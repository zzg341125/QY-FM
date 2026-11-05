package com.dragon.fm.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * 极简 WebDAV 客户端：测试连接、上传备份、下载备份。
 * 使用 Basic Auth（用户名 + 密码），适合坚果云、Nextcloud、群晖等 WebDAV。
 */
object WebDavManager {

    private const val USER_AGENT = "QuYangRadio/1.0 (Android)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun authHeader(user: String, password: String): String =
        Credentials.basic(user, password, Charsets.UTF_8)

    /** 支持“目录地址”或“完整 .zip 文件地址” */
    fun buildFileUrl(baseUrl: String, fileName: String): String {
        val base = baseUrl.trim().replace("\n", "").replace("\r", "")
        if (base.isEmpty()) return base
        if (base.lowercase().endsWith(".zip")) return base
        val safeName = fileName.trim().ifBlank { "quyang_backup.zip" }
        return base.trimEnd('/') + "/" + safeName
    }

    private fun describe(e: Exception): String {
        val msg = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
        return "请求失败：$msg"
    }

    /** 拼接服务器返回码、原因与响应体片段，便于定位问题 */
    private fun detail(response: Response): String {
        val body = try {
            response.peekBody(1024).string().replace(Regex("\\s+"), " ").trim()
        } catch (_: Exception) {
            ""
        }
        val head = "HTTP ${response.code}" + if (response.message.isBlank()) "" else " ${response.message}"
        return if (body.isBlank()) head else "$head\n${body.take(300)}"
    }

    /** 用 PROPFIND 测试 WebDAV 目录是否可用、账号是否正确 */
    suspend fun testConnection(baseUrl: String, user: String, password: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(baseUrl.trim())
                    .header("Authorization", authHeader(user, password))
                    .header("User-Agent", USER_AGENT)
                    .header("Depth", "0")
                    .method("PROPFIND", null)
                    .build()
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 401 || response.code == 403 ->
                            Result.failure(Exception("用户名或密码错误（${response.code}）"))
                        response.isSuccessful || response.code == 207 ->
                            Result.success(response.code)
                        else -> Result.failure(Exception("服务器返回 ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(Exception(describe(e), e))
            }
        }

    /** 上传备份文件（PUT），先试 octet-stream，失败再试 zip 类型 */
    suspend fun upload(
        fileUrl: String,
        user: String,
        password: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (mediaType in listOf("application/octet-stream", "application/zip")) {
            try {
                val body = bytes.toRequestBody(mediaType.toMediaType())
                val request = Request.Builder()
                    .url(fileUrl)
                    .header("Authorization", authHeader(user, password))
                    .header("User-Agent", USER_AGENT)
                    .header("Overwrite", "T")
                    .put(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code == 201 || response.code == 204) {
                        return@withContext Result.success(Unit)
                    }
                    val hint = when (response.code) {
                        301, 302, 307, 308 -> "\n（服务器要求跳转，请把地址换成跳转后的最终地址）"
                        400 -> "\n（服务器拒绝该请求：可能该路径不是 WebDAV 目录，或路径中的中文需在服务器端可用）"
                        401, 403 -> "\n（账号无写入权限，请确认地址是 WebDAV 目录且可写）"
                        404 -> "\n（目录不存在，请检查服务器地址）"
                        409 -> "\n（目标目录不存在或重名冲突）"
                        405 -> "\n（服务器不允许 PUT，可能不是 WebDAV 目录）"
                        else -> ""
                    }
                    lastError = Exception("上传失败：${detail(response)}$hint")
                    // 类型不被接受时换一种 Content-Type 再试
                    if (response.code != 400 && response.code != 415 && response.code != 405) {
                        return@withContext Result.failure(lastError!!)
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception(describe(e), e))
            }
        }
        Result.failure(lastError ?: Exception("上传失败：未知错误"))
    }

    /** 下载备份文件（GET） */
    suspend fun download(
        fileUrl: String,
        user: String,
        password: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(fileUrl)
                .header("Authorization", authHeader(user, password))
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    val hint = when (response.code) {
                        401, 403 -> "\n（账号无读取权限）"
                        404 -> "\n（云端还没有这个备份文件，请先上传）"
                        else -> ""
                    }
                    Result.failure(Exception("下载失败：${detail(response)}$hint"))
                } else {
                    Result.success(body.bytes())
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(describe(e), e))
        }
    }

    data class DavInfo(
        val server: String,
        val allow: Set<String>,
        val dav: String,
        val hrefs: List<String>
    )

    /** OPTIONS + PROPFIND 探测：服务器软件、支持的方法、目录内 href */
    suspend fun inspect(dirUrl: String, user: String, password: String): Result<DavInfo> =
        withContext(Dispatchers.IO) {
            try {
                var server = ""
                var allow = emptySet<String>()
                var dav = ""
                val options = Request.Builder()
                    .url(dirUrl)
                    .header("Authorization", authHeader(user, password))
                    .header("User-Agent", USER_AGENT)
                    .method("OPTIONS", null)
                    .build()
                client.newCall(options).execute().use { r ->
                    server = r.header("Server").orEmpty()
                    allow = r.header("Allow").orEmpty()
                        .split(',')
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                    dav = r.header("DAV").orEmpty()
                }

                val hrefs = mutableListOf<String>()
                val propfind = Request.Builder()
                    .url(dirUrl)
                    .header("Authorization", authHeader(user, password))
                    .header("User-Agent", USER_AGENT)
                    .header("Depth", "1")
                    .method("PROPFIND", null)
                    .build()
                client.newCall(propfind).execute().use { r ->
                    val text = r.body?.string().orEmpty()
                    Regex("<[^>]*href[^>]*>([^<]+)</[^>]*href>", RegexOption.IGNORE_CASE)
                        .findAll(text)
                        .forEach { hrefs += it.groupValues[1].trim() }
                }
                Result.success(DavInfo(server, allow, dav, hrefs))
            } catch (e: Exception) {
                Result.failure(Exception(describe(e), e))
            }
        }

    data class RemoteFile(val name: String, val url: String, val size: Long)

    /** 生成带时间戳的备份文件名：前缀_20251104_230512.zip */
    fun backupFileName(prefix: String, timestamp: Long = System.currentTimeMillis()): String {
        val base = prefix.trim()
            .removeSuffix(".zip")
            .removeSuffix(".ZIP")
            .ifBlank { "quyang_backup" }
        val fmt = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
        return "${base}_${fmt.format(java.util.Date(timestamp))}.zip"
    }

    /** 列出目录下的 .zip 备份文件（按文件名倒序，最新的在前） */
    suspend fun listFiles(dirUrl: String, user: String, password: String): Result<List<RemoteFile>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(dirUrl)
                    .header("Authorization", authHeader(user, password))
                    .header("User-Agent", USER_AGENT)
                    .header("Depth", "1")
                    .method("PROPFIND", null)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.code != 207 && !response.isSuccessful) {
                        return@withContext Result.failure(Exception("获取备份列表失败：${detail(response)}"))
                    }
                    val body = response.body?.string().orEmpty()
                    val entries = Regex(
                        "<[^>]*:?response[^>]*>(.*?)</[^>]*:?response>",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                    ).findAll(body)

                    val files = mutableListOf<RemoteFile>()
                    for (entry in entries) {
                        val chunk = entry.groupValues[1]
                        val href = Regex(":?href[^>]*>([^<]+)<", RegexOption.IGNORE_CASE)
                            .find(chunk)?.groupValues?.get(1)?.trim() ?: continue
                        val rawName = href.trimEnd('/').substringAfterLast('/')
                        if (rawName.isBlank()) continue
                        val name = percentDecode(rawName)
                        if (!name.endsWith(".zip", ignoreCase = true)) continue
                        val size = Regex(
                            ":?getcontentlength[^>]*>([0-9]+)<",
                            RegexOption.IGNORE_CASE
                        ).find(chunk)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                        val url = resolveHref(dirUrl, href) ?: continue
                        files += RemoteFile(name, url, size)
                    }
                    Result.success(files.sortedByDescending { it.name })
                }
            } catch (e: Exception) {
                Result.failure(Exception(describe(e), e))
            }
        }

    private fun resolveHref(dirUrl: String, href: String): String? =
        runCatching { dirUrl.toHttpUrlOrNull()?.resolve(href)?.toString() }.getOrNull()

    /** 只解码 %XX，避免把 + 当成空格 */
    private fun percentDecode(input: String): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '%' && i + 2 < input.length) {
                val hex = input.substring(i + 1, i + 3)
                val value = hex.toIntOrNull(16)
                if (value != null) {
                    sb.append(value.toChar())
                    i += 3
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    /**
     * 裸 HTTP PUT：直接把（已解码的）中文路径按 UTF-8 写进请求行。
     * 部分国产 NAS（如极空间）的 WebDAV 对结构化 URL 的规范化路径返回 400，
     * 但对原始 UTF-8 请求行可以正常写入。
     * 返回 HTTP 状态码，失败返回 -1。
     */
    private fun rawPut(fileUrl: String, user: String, password: String, bytes: ByteArray): Int {
        return try {
            val uri = java.net.URI(fileUrl)
            val scheme = uri.scheme?.lowercase() ?: return -1
            if (scheme != "http") return -1
            val host = uri.host ?: return -1
            val port = if (uri.port != -1) uri.port else 80
            val rawPath = percentDecode(uri.rawPath ?: "/")
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), 8000)
                socket.soTimeout = 45000
                val out = socket.getOutputStream()
                val header = buildString {
                    append("PUT ").append(rawPath).append(" HTTP/1.1\r\n")
                    append("Host: ").append(host)
                    if (uri.port != -1) append(":").append(uri.port)
                    append("\r\n")
                    append("Authorization: ")
                    append(Credentials.basic(user, password, Charsets.UTF_8))
                    append("\r\n")
                    append("Content-Type: application/octet-stream\r\n")
                    append("Content-Length: ").append(bytes.size).append("\r\n")
                    append("Connection: close\r\n\r\n")
                }
                out.write(header.toByteArray(Charsets.UTF_8))
                out.write(bytes)
                out.flush()
                val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                val statusLine = reader.readLine() ?: return -1
                statusLine.split(" ").getOrNull(1)?.toIntOrNull() ?: -1
            }
        } catch (_: Exception) {
            -1
        }
    }

    /** 目录自身在服务器上的规范地址（PROPFIND 返回的首个 href） */
    private fun canonicalDirUrl(dirUrl: String, info: DavInfo?): String? {
        val href = info?.hrefs?.firstOrNull() ?: return null
        if (!href.endsWith("/")) return null
        val resolved = resolveHref(dirUrl, href) ?: return null
        return resolved.takeIf { it != dirUrl.trimEnd('/') + "/" }
    }

    /**
     * 上传：优先用输入地址，失败时再试服务器返回的规范地址，并附带服务器信息。
     */
    suspend fun uploadSmart(
        dirUrl: String,
        fileName: String,
        user: String,
        password: String,
        bytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        val safeName = fileName.trim().ifBlank { "quyang_backup.zip" }
        val info = inspect(dirUrl, user, password).getOrNull()
        val candidates = linkedSetOf<String>()
        candidates += buildFileUrl(dirUrl, safeName)
        canonicalDirUrl(dirUrl, info)?.let { candidates += buildFileUrl(it, safeName) }

        var last: Exception? = null
        for (url in candidates) {
            val result = upload(url, user, password, bytes)
            if (result.isSuccess) return@withContext Result.success(url)
            last = result.exceptionOrNull() as? Exception
        }

        // 兜底：裸 HTTP PUT（中文路径直接以 UTF-8 写入请求行，绕开 URL 规范化）
        var rawCode = 0
        for (url in candidates) {
            val code = rawPut(url, user, password, bytes)
            if (code == 200 || code == 201 || code == 204) {
                return@withContext Result.success(url)
            }
            if (code > 0) rawCode = code
        }

        val extra = buildString {
            append("\n服务器：").append(info?.server?.takeIf { it.isNotBlank() } ?: "未知")
            if (info != null && info.allow.isNotEmpty()) {
                append("\n支持方法：").append(info.allow.joinToString(", "))
            }
            if (info != null && info.dav.isNotBlank()) append("\nDAV：").append(info.dav)
            if (info != null && info.allow.isNotEmpty() && !info.allow.contains("PUT")) {
                append("\n该地址未声明支持 PUT，可能只读或不是 WebDAV 目录")
            }
            if (rawCode > 0) append("\n裸 PUT 返回：HTTP ").append(rawCode)
            append("\n已尝试 ").append(candidates.size).append(" 个地址")
        }
        Result.failure(Exception((last?.message ?: "上传失败：未知错误") + extra))
    }

    /** 下载：同样在失败时尝试规范地址 */
    suspend fun downloadSmart(
        dirUrl: String,
        fileName: String,
        user: String,
        password: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val safeName = fileName.trim().ifBlank { "quyang_backup.zip" }
        val urls = linkedSetOf<String>()
        urls += buildFileUrl(dirUrl, safeName)
        val info = inspect(dirUrl, user, password).getOrNull()
        canonicalDirUrl(dirUrl, info)?.let { urls += buildFileUrl(it, safeName) }

        var last: Exception? = null
        for (url in urls) {
            val result = download(url, user, password)
            if (result.isSuccess) return@withContext result
            last = result.exceptionOrNull() as? Exception
        }
        Result.failure(last ?: Exception("下载失败：未知错误"))
    }
}
