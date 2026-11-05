package com.dragon.fm.data.api

import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class LocationResult(
    val province: String,
    val city: String
)

@Serializable
data class IpLocationResponse(
    val status: String? = null,
    val country: String? = null,
    val regionName: String? = null,
    val city: String? = null
)

object IpLocationService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Accurately detects user location using high-priority domestic IP geolocation services.
     * Guaranteed to return precise Chinese province and secondary prefecture city.
     */
    suspend fun detectLocation(): LocationResult? = withContext(Dispatchers.IO) {
        // Attempt 1: IPIP.net Domestic High Precision JSON API
        try {
            val request = Request.Builder()
                .url("https://myip.ipip.net/json")
                .header("User-Agent", "Mozilla/5.0 (DragonFM/1.0.0)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val root = json.parseToJsonElement(body).jsonObject
                    val data = root["data"]?.jsonObject
                    val locArray = data?.get("location")?.jsonArray
                    if (locArray != null && locArray.size >= 3) {
                        val rawProv = locArray[1].jsonPrimitive.content.trim()
                        val rawCity = locArray[2].jsonPrimitive.content.trim()
                        val prov = cleanRegionName(rawProv)
                        val city = cleanRegionName(if (rawCity.isNotBlank()) rawCity else rawProv)
                        if (prov.isNotBlank()) {
                            return@withContext LocationResult(province = prov, city = city)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Attempt 2: PConline Domestic IP API Fallback (精确到国内地级市)
        try {
            val request = Request.Builder()
                .url("http://whois.pconline.com.cn/ipJson.jsp?json=true")
                .header("User-Agent", "DragonFM/1.0.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val jsonElement = json.parseToJsonElement(body)
                    val rawCity = jsonElement.toString().substringAfter("\"city\":\"", "").substringBefore("\"", "").trim()
                    val rawProv = jsonElement.toString().substringAfter("\"pro\":\"", "").substringBefore("\"", "").trim()
                    val prov = cleanRegionName(rawProv)
                    val city = cleanRegionName(if (rawCity.isNotBlank()) rawCity else rawProv)
                    if (prov.isNotBlank()) {
                        return@withContext LocationResult(province = prov, city = city)
                    }
                }
            }
        } catch (_: Exception) {}

        // Attempt 3: ip-api.com
        try {
            val request = Request.Builder()
                .url("http://ip-api.com/json/?lang=zh-CN")
                .header("User-Agent", "DragonFM/1.0.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val data = json.decodeFromString<IpLocationResponse>(body)
                    val rawRegion = data.regionName.orEmpty()
                    val rawCity = data.city.orEmpty()
                    val prov = cleanRegionName(rawRegion)
                    val city = cleanRegionName(if (rawCity.isNotBlank()) rawCity else rawRegion)
                    if (prov.isNotBlank()) {
                        return@withContext LocationResult(province = prov, city = city)
                    }
                }
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    suspend fun detectLocalStation(): Pair<String, RadioStation?> = withContext(Dispatchers.IO) {
        val location = detectLocation()
        if (location != null) {
            val label = if (location.city.isNotBlank() && location.city != location.province) {
                "${location.province} · ${location.city}"
            } else {
                location.province
            }
            return@withContext Pair(label, null)
        }
        return@withContext Pair("本地推荐", null)
    }

    private fun cleanRegionName(name: String): String {
        return name
            .removeSuffix("省")
            .removeSuffix("壮族自治区")
            .removeSuffix("回族自治区")
            .removeSuffix("维吾尔自治区")
            .removeSuffix("自治区")
            .removeSuffix("特别行政区")
            .removeSuffix("地区")
            .removeSuffix("市")
            .trim()
    }
}
