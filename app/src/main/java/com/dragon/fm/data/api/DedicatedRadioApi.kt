package com.dragon.fm.data.api

import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.StreamLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * High-reliability dedicated broadcast streams network client.
 * Provides official provincial and national broadcast channels with real-time EPG program data.
 */
class DedicatedRadioApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val API_BASE = "https://ytmsout.radio.cn/web"
        private const val SIGN_KEY = "f0fc4c668392f9f9a447e48584c214ee"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Serializable
    data class ApiResponse<T>(
        val code: Int = -1,
        val message: String? = null,
        val data: T? = null
    )

    @Serializable
    data class CategoryItem(
        val id: String = "",
        val categoryName: String = ""
    )

    @Serializable
    data class ProvinceItem(
        val provinceCode: Int = 0,
        val provinceName: String = ""
    )

    @Serializable
    data class BroadcastItem(
        val contentId: String = "",
        val title: String = "",
        val subtitle: String? = null,
        val image: String? = null,
        val playUrlLow: String? = null,
        val mp3PlayUrlLow: String? = null,
        val mp3PlayUrlHigh: String? = null,
        val playUrlMulti: String? = null
    )

    /**
     * Compute MD5 signature required by the dedicated stream service.
     * Rule: uppercase hex of MD5(sorted_params + "&timestamp=" + ts + "&key=" + KEY)
     */
    private fun generateSign(params: Map<String, Any>, timestamp: String): String {
        val sortedString = if (params.isNotEmpty()) {
            params.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" }
        } else ""

        val raw = if (sortedString.isNotBlank()) {
            "$sortedString&timestamp=$timestamp&key=$SIGN_KEY"
        } else {
            "timestamp=$timestamp&key=$SIGN_KEY"
        }

        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }

    /**
     * Fetch list of broadcast categories (News, Music, Traffic, etc.)
     */
    suspend fun getCategories(): Result<List<CategoryItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$API_BASE/appCategory/list/all"
            val ts = System.currentTimeMillis().toString()
            val sign = generateSign(emptyMap(), ts)

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("sign", sign)
                .header("timestamp", ts)
                .header("equipmentId", "0000")
                .header("platformCode", "WEB")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val body = resp.body?.string().orEmpty()
                val parsed = json.decodeFromString<ApiResponse<List<CategoryItem>>>(body)
                if (parsed.code == 0 && parsed.data != null) {
                    Result.success(parsed.data)
                } else {
                    Result.failure(Exception(parsed.message ?: "Failed with code ${parsed.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch list of supported administrative provinces
     */
    suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$API_BASE/appProvince/list/all"
            val ts = System.currentTimeMillis().toString()
            val sign = generateSign(emptyMap(), ts)

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("sign", sign)
                .header("timestamp", ts)
                .header("equipmentId", "0000")
                .header("platformCode", "WEB")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val body = resp.body?.string().orEmpty()
                val parsed = json.decodeFromString<ApiResponse<List<ProvinceItem>>>(body)
                if (parsed.code == 0 && parsed.data != null) {
                    Result.success(parsed.data)
                } else {
                    Result.failure(Exception(parsed.message ?: "Failed with code ${parsed.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch broadcasts for a given category and province code.
     * @param categoryId Category ID (0 for all)
     * @param provinceCode Province code (0 for national, 410000 for Henan, 440000 for Guangdong, etc.)
     */
    suspend fun getBroadcastList(
        categoryId: Int = 0,
        provinceCode: Int = 0,
        provinceName: String? = null
    ): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "categoryId" to categoryId,
                "provinceCode" to provinceCode
            )
            val ts = System.currentTimeMillis().toString()
            val sign = generateSign(params, ts)

            val fullUrl = "$API_BASE/appBroadcast/list?categoryId=$categoryId&provinceCode=$provinceCode"

            val req = Request.Builder()
                .url(fullUrl)
                .header("User-Agent", USER_AGENT)
                .header("sign", sign)
                .header("timestamp", ts)
                .header("equipmentId", "0000")
                .header("platformCode", "WEB")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val body = resp.body?.string().orEmpty()
                val parsed = json.decodeFromString<ApiResponse<List<BroadcastItem>>>(body)
                if (parsed.code == 0 && parsed.data != null) {
                    val stations = parsed.data.mapNotNull { item ->
                        itemToRadioStation(item, provinceName)
                    }
                    Result.success(stations)
                } else {
                    Result.failure(Exception(parsed.message ?: "Failed with code ${parsed.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts a dedicated broadcast item to our unified RadioStation model with redundant lines.
     * Direct replication of LX FM PC: Default to official high-quality 192kbps MP3 stream directly.
     */
    private fun itemToRadioStation(item: BroadcastItem, provinceName: String?): RadioStation? {
        val mp3High = item.mp3PlayUrlHigh?.takeIf { it.isNotBlank() }
        val mp3Low = item.mp3PlayUrlLow?.takeIf { it.isNotBlank() }
        val playLow = item.playUrlLow?.takeIf { it.isNotBlank() }

        // 默认直接采用官方高品质直播流 (192kbps MP3)，无需用户再做低质量线路选择
        val streamUrl = mp3High ?: mp3Low ?: playLow ?: return null

        val id = "dedicated_${item.contentId}"

        val lines = mutableListOf<StreamLine>()
        // 核心主线：高品质专线 (192kbps 纯净数字流)
        lines.add(StreamLine("line-dedicated-hq", "高品质专线", streamUrl, isStable = true, priority = 1))

        // 备用线路：若有不同格式或备用地址，提供备用容灾
        if (playLow != null && playLow != streamUrl) {
            lines.add(StreamLine("line-dedicated-backup", "极速备用", playLow, isStable = true, priority = 2))
        }

        val cleanProgram = item.subtitle
            ?.takeIf { it.isNotBlank() && it != "暂无节目单" }
            ?.replace("正在直播：", "")
            ?.replace("正在直播:", "")
            ?.trim()

        val isMp3 = streamUrl.contains(".mp3") || mp3High != null
        return RadioStation(
            id = id,
            name = item.title.trim(),
            url = streamUrl,
            urlResolved = streamUrl,
            favicon = item.image?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it },
            country = "China",
            state = provinceName ?: "全国",
            tags = "官方专线, 高品质广播",
            codec = if (isMp3) "MP3" else "AAC",
            bitrate = if (mp3High != null) 192 else 128,
            votes = 99999,
            lines = lines,
            activeLineIndex = 0,
            currentProgram = cleanProgram,
            hasDedicatedLine = true
        )
    }
}
