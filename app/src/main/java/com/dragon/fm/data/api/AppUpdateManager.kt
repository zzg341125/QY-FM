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
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class AppUpdateManager(private val context: Context) {

    companion object {
        const val GITEE_VERSION_URL = "https://gitee.com/app-finder/LX-FM/raw/master/version.json"
        const val GITEE_REPO_URL = "https://gitee.com/app-finder/LX-FM"
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
        try {
            val request = Request.Builder()
                .url(GITEE_VERSION_URL)
                .header("User-Agent", "LX-FM-Android/${getCurrentVersionName()}")
                .header("Cache-Control", "no-cache")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("检测更新失败 HTTP ${response.code}", isManual)
                }

                val bodyStr = response.body?.string()
                    ?: return@withContext UpdateCheckResult.Error("获取更新信息为空", isManual)

                val updateInfo = json.decodeFromString<AppUpdateInfo>(bodyStr)
                val currentCode = getCurrentVersionCode()

                if (updateInfo.versionCode > currentCode) {
                    UpdateCheckResult.HasUpdate(updateInfo, isManual)
                } else {
                    UpdateCheckResult.UpToDate(getCurrentVersionName(), isManual)
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "网络连接异常", isManual)
        }
    }

    suspend fun downloadApk(
        updateInfo: AppUpdateInfo,
        onProgress: (progress: Float, currentBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir

            val finalFile = File(downloadDir, "LX_FM_v${updateInfo.versionName}.apk")
            val tempFile = File(downloadDir, "LX_FM_v${updateInfo.versionName}.apk.tmp")

            if (tempFile.exists()) tempFile.delete()

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .header("User-Agent", "LX-FM-Android/${getCurrentVersionName()}")
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
