package com.dragon.fm.data.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dragon.fm.data.model.AppUpdateInfo
import com.dragon.fm.data.model.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class AppUpdateManager(private val context: Context) {

    companion object {
        const val GITHUB_RELEASE_API =
            "https://api.github.com/repos/zzg341125/QY-FM/releases/latest"
        const val GITHUB_LATEST_VERSION_URL =
            "https://github.com/zzg341125/QY-FM/releases/latest/download/version.json"
        const val GITHUB_RAW_VERSION_URL =
            "https://raw.githubusercontent.com/zzg341125/QY-FM/main/version.json"
        const val GITHUB_REPO_URL = "https://github.com/zzg341125/QY-FM"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkUpdate(isManual: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentCode = getCurrentVersionCode()
        val currentName = getCurrentVersionName()
        val headers = mapOf(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
            "User-Agent" to "QY-FM-Android/$currentName"
        )

        var lastError = "网络连接异常"
        for (url in listOf(GITHUB_RELEASE_API, GITHUB_LATEST_VERSION_URL, GITHUB_RAW_VERSION_URL)) {
            try {
                val body = fetchText(url, headers)
                val updateInfo = if (url == GITHUB_RELEASE_API) {
                    GitHubReleaseParser.parseRelease(body)
                } else {
                    json.decodeFromString<AppUpdateInfo>(body)
                }

                if (updateInfo.downloadUrl.isBlank()) {
                    lastError = "更新信息缺少 APK 下载地址"
                    continue
                }

                return@withContext if (
                    GitHubReleaseParser.isNewer(
                        candidateName = updateInfo.versionName,
                        candidateCode = updateInfo.versionCode,
                        currentName = currentName,
                        currentCode = currentCode
                    )
                ) {
                    UpdateCheckResult.HasUpdate(updateInfo, isManual)
                } else {
                    UpdateCheckResult.UpToDate(currentName, isManual)
                }
            } catch (e: Exception) {
                lastError = e.message ?: "网络连接异常"
            }
        }
        UpdateCheckResult.Error(lastError, isManual)
    }

    private fun fetchText(url: String, headers: Map<String, String>): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Cache-Control", "no-cache")
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("检测更新失败 HTTP ${response.code}")
            }
            return response.body?.string() ?: throw Exception("获取更新信息为空")
        }
    }

    suspend fun downloadApk(
        updateInfo: AppUpdateInfo,
        onProgress: (progress: Float, currentBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir

            val safeVersion = updateInfo.versionName
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "latest" }
            val finalFile = File(downloadDir, "QY-FM_v$safeVersion.apk")
            val tempFile = File(downloadDir, "QY-FM_v$safeVersion.apk.tmp")

            if (tempFile.exists()) tempFile.delete()

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .header("User-Agent", "QY-FM-Android/${getCurrentVersionName()}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("下载请求失败 HTTP ${response.code}"))
                }

                val body = response.body
                    ?: return@withContext Result.failure(Exception("下载内容为空"))

                val totalLength = body.contentLength()
                var downloadedLength = 0L

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var lastProgressUpdate = 0L

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedLength += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 60 || downloadedLength == totalLength) {
                                lastProgressUpdate = now
                                val progress = if (totalLength > 0) {
                                    (downloadedLength.toFloat() / totalLength).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                onProgress(progress, downloadedLength, totalLength)
                            }
                        }
                    }
                }

                if (finalFile.exists()) finalFile.delete()
                if (!tempFile.renameTo(finalFile)) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }

                Result.success(finalFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File): Boolean {
        return try {
            // Check unknown sources installation permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    // Continue to fire install intent
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

internal object GitHubReleaseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val versionCodeFormatter =
        DateTimeFormatter.ofPattern("yyMMddHH").withZone(ZoneOffset.UTC)

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String = "",
        val body: String = "",
        @SerialName("published_at") val publishedAt: String = "",
        @SerialName("draft") val draft: Boolean = false,
        @SerialName("prerelease") val prerelease: Boolean = false,
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
        val name: String = "",
        @SerialName("browser_download_url") val downloadUrl: String = "",
        val size: Long = 0L,
    )

    fun parseRelease(payload: String): AppUpdateInfo {
        val release = json.decodeFromString<GitHubRelease>(payload)
        if (release.draft || release.prerelease) return AppUpdateInfo()
        val apk = release.assets
            .asSequence()
            .filter { it.name.endsWith(".apk", ignoreCase = true) }
            .filterNot { it.name.endsWith(".idsig", ignoreCase = true) }
            .filter { it.downloadUrl.isNotBlank() }
            .maxByOrNull { it.size }
            ?: return AppUpdateInfo()

        return AppUpdateInfo(
            versionCode = versionCodeFromPublishedAt(release.publishedAt),
            versionName = release.tagName.trim().removePrefix("v").ifBlank { "latest" },
            changelog = release.body.trim(),
            downloadUrl = apk.downloadUrl,
            releaseDate = release.publishedAt.take(10),
        )
    }

    fun isNewer(
        candidateName: String,
        candidateCode: Int,
        currentName: String,
        currentCode: Int,
    ): Boolean {
        if (candidateCode > currentCode) return true
        val candidate = candidateName.toVersionParts()
        val current = currentName.toVersionParts()
        return candidate != null && current != null && candidate > current
    }

    private fun versionCodeFromPublishedAt(value: String): Int {
        val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return 0
        return versionCodeFormatter.format(instant).toIntOrNull() ?: 0
    }

    private fun String.toVersionParts(): VersionParts? {
        val numbers = trim()
            .removePrefix("v")
            .split('.')
            .take(3)
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: return null }
        if (numbers.isEmpty()) return null
        return VersionParts(
            numbers.getOrElse(0) { 0 },
            numbers.getOrElse(1) { 0 },
            numbers.getOrElse(2) { 0 },
        )
    }

    private data class VersionParts(
        val major: Int,
        val minor: Int,
        val patch: Int,
    ) : Comparable<VersionParts> {
        override fun compareTo(other: VersionParts): Int =
            compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    }
}
