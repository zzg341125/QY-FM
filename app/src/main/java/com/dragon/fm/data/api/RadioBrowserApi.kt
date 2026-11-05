package com.dragon.fm.data.api

import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class RadioBrowserApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(4500, TimeUnit.MILLISECONDS)
        .build()
) {
    private val servers = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val responseCache = ConcurrentHashMap<String, List<RadioStation>>()
    private var currentServerIndex = 0

    fun clearCache() {
        responseCache.clear()
    }

    private fun getBaseUrl(): String = servers[currentServerIndex % servers.size]

    private fun switchServer() {
        currentServerIndex = (currentServerIndex + 1) % servers.size
    }

    private fun encode(str: String): String = URLEncoder.encode(str, "UTF-8")

    /**
     * Fetch top voted stations from China (CN)
     */
    suspend fun getChinaStations(limit: Int = 200): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        val endpoint = "${getBaseUrl()}/json/stations/search?countrycode=CN&hidebroken=true&order=votes&reverse=true&limit=$limit"
        fetchStations(endpoint)
    }

    /**
     * Fetch Chinese stations by province or city keyword (e.g. 广东, 河南, 北京, 四川, etc.)
     */
    /**
     * Fetch Chinese stations by province or city keyword (e.g. 广东, 河南, 北京, 四川, etc.)
     */
    suspend fun getChinaStationsByProvince(province: String, limit: Int = 100): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        val trimmed = province.trim().removeSuffix("省").removeSuffix("市").removeSuffix("自治区").removeSuffix("壮族自治区").removeSuffix("回族自治区").removeSuffix("维吾尔自治区").removeSuffix("特别行政区")
        val encoded = encode(trimmed)

        // Complete 34 provincial level administrative regions mapping
        val pinyin = when (trimmed) {
            "北京" -> "Beijing"
            "天津" -> "Tianjin"
            "河北" -> "Hebei"
            "山西" -> "Shanxi"
            "内蒙古" -> "Inner Mongolia"
            "辽宁" -> "Liaoning"
            "吉林" -> "Jilin"
            "黑龙江" -> "Heilongjiang"
            "上海" -> "Shanghai"
            "江苏" -> "Jiangsu"
            "浙江" -> "Zhejiang"
            "安徽" -> "Anhui"
            "福建" -> "Fujian"
            "江西" -> "Jiangxi"
            "山东" -> "Shandong"
            "河南" -> "Henan"
            "湖北" -> "Hubei"
            "湖南" -> "Hunan"
            "广东" -> "Guangdong"
            "广西" -> "Guangxi"
            "海南" -> "Hainan"
            "重庆" -> "Chongqing"
            "四川" -> "Sichuan"
            "贵州" -> "Guizhou"
            "云南" -> "Yunnan"
            "西藏" -> "Tibet"
            "陕西" -> "Shaanxi"
            "甘肃" -> "Gansu"
            "青海" -> "Qinghai"
            "宁夏" -> "Ningxia"
            "新疆" -> "Xinjiang"
            "香港" -> "Hong Kong"
            "澳门" -> "Macao"
            "台湾" -> "Taiwan"
            else -> null
        }

        // Parallel dispatch for name search and state pinyin search
        val (nameList, stateList) = coroutineScope {
            val nameDef = async {
                fetchStations("${getBaseUrl()}/json/stations/search?countrycode=CN&name=$encoded&hidebroken=true&order=votes&reverse=true&limit=$limit").getOrNull().orEmpty()
            }
            val stateDef = async {
                if (pinyin != null) {
                    fetchStations("${getBaseUrl()}/json/stations/search?countrycode=CN&state=${encode(pinyin)}&hidebroken=true&order=votes&reverse=true&limit=$limit").getOrNull().orEmpty()
                } else emptyList()
            }
            Pair(nameDef.await(), stateDef.await())
        }

        // Merge and deduplicate by station id
        val merged = (nameList + stateList).distinctBy { it.id }
        Result.success(merged)
    }

    suspend fun getTopStations(limit: Int = 200): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        val endpoint = "${getBaseUrl()}/json/stations/topvote/$limit?hidebroken=true"
        fetchStations(endpoint)
    }

    suspend fun getStationsByTag(tag: String, limit: Int = 200): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        val encoded = encode(tag)
        val endpoint = "${getBaseUrl()}/json/stations/search?tag=$encoded&hidebroken=true&order=votes&reverse=true&limit=$limit"
        fetchStations(endpoint)
    }

    suspend fun getStationsByCountry(country: String, limit: Int = 200): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        val countryCode = when {
            country.contains("china", ignoreCase = true) || country.contains("中国") || country.equals("cn", ignoreCase = true) -> "CN"
            country.contains("japan", ignoreCase = true) || country.contains("日本") || country.equals("jp", ignoreCase = true) -> "JP"
            country.contains("united states", ignoreCase = true) || country.contains("美国") || country.equals("us", ignoreCase = true) -> "US"
            country.contains("united kingdom", ignoreCase = true) || country.contains("英国") || country.equals("gb", ignoreCase = true) || country.equals("uk", ignoreCase = true) -> "GB"
            country.contains("germany", ignoreCase = true) || country.contains("德国") || country.equals("de", ignoreCase = true) -> "DE"
            country.contains("france", ignoreCase = true) || country.contains("法国") || country.equals("fr", ignoreCase = true) -> "FR"
            country.contains("russia", ignoreCase = true) || country.contains("俄罗斯") || country.equals("ru", ignoreCase = true) -> "RU"
            country.contains("korea", ignoreCase = true) || country.contains("韩国") || country.equals("kr", ignoreCase = true) -> "KR"
            country.contains("canada", ignoreCase = true) || country.contains("加拿大") || country.equals("ca", ignoreCase = true) -> "CA"
            country.contains("australia", ignoreCase = true) || country.contains("澳大利亚") || country.equals("au", ignoreCase = true) -> "AU"
            else -> null
        }

        val endpoint = if (countryCode != null) {
            "${getBaseUrl()}/json/stations/search?countrycode=$countryCode&hidebroken=true&order=votes&reverse=true&limit=$limit"
        } else {
            val encoded = encode(country)
            "${getBaseUrl()}/json/stations/search?country=$encoded&hidebroken=true&order=votes&reverse=true&limit=$limit"
        }
        fetchStations(endpoint)
    }

    suspend fun searchStationsWithTerms(
        terms: List<String>,
        isDomestic: Boolean = false,
        limit: Int = 100
    ): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        val distinct = coroutineScope {
            terms.take(4).map { term ->
                async {
                    searchStations(term, isDomestic = isDomestic, limit = limit / 2).getOrNull().orEmpty()
                }
            }.awaitAll().flatten().distinctBy { it.id }
        }
        if (distinct.isNotEmpty()) {
            Result.success(distinct)
        } else {
            searchStations(terms.firstOrNull() ?: "", isDomestic = isDomestic, limit = limit)
        }
    }

    suspend fun searchStations(
        query: String,
        isDomestic: Boolean = false,
        limit: Int = 200
    ): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(emptyList())

        val countryParam = if (isDomestic) "&countrycode=CN" else ""

        // 1. If it matches a known tag synonym, search by tag with proper country scope
        val tagSynonym = when (trimmed.lowercase()) {
            "流行", "华语流行", "热门流行", "流行音乐", "pop", "cpop" -> "pop"
            "摇滚", "摇滚力量", "摇滚民谣", "rock" -> "rock"
            "爵士", "爵士慢调", "jazz" -> "jazz"
            "古典", "古典音乐", "古典交响", "classical" -> "classical"
            "新闻", "实时新闻", "新闻广播", "新闻资讯", "news" -> "news"
            "轻音乐", "轻音助眠", "助眠", "ambient", "chill" -> "ambient"
            "电子", "电子律动", "电子流行", "舞曲", "edm", "dance" -> "dance"
            "民谣", "民谣音乐", "folk" -> "folk"
            "经典", "经典老歌", "经典怀旧", "oldies", "classic" -> "classic"
            "粤语", "粤语流行", "大湾区", "cantopop" -> "cantopop"
            "交通", "交通广播", "交通路况", "traffic" -> "traffic"
            "经济", "经济财经", "财经", "finance", "business" -> "finance"
            "动漫", "二次元", "acg", "acg动漫", "anime" -> "anime"
            "体育", "体育赛事", "sport", "sports" -> "sports"
            "文化", "文化科教", "评书", "评书曲艺", "culture", "talk" -> "talk"
            else -> null
        }
        if (tagSynonym != null) {
            val tagRes = fetchStations("${getBaseUrl()}/json/stations/search?tag=${encode(tagSynonym)}$countryParam&hidebroken=true&order=votes&reverse=true&limit=$limit")
            if (tagRes.isSuccess && tagRes.getOrNull()?.isNotEmpty() == true) {
                return@withContext tagRes
            }
        }

        val encoded = encode(trimmed)
        // 2. Search by name with country filter
        val endpoint = "${getBaseUrl()}/json/stations/search?name=$encoded$countryParam&hidebroken=true&order=votes&reverse=true&limit=$limit"
        val nameResult = fetchStations(endpoint)
        if (nameResult.isSuccess && nameResult.getOrNull()?.isNotEmpty() == true) {
            return@withContext nameResult
        }

        // 3. Fallback: search by tag directly with country filter
        val tagEndpoint = "${getBaseUrl()}/json/stations/search?tag=$encoded$countryParam&hidebroken=true&order=votes&reverse=true&limit=$limit"
        val tagResult = fetchStations(tagEndpoint)
        if (tagResult.isSuccess && tagResult.getOrNull()?.isNotEmpty() == true) {
            return@withContext tagResult
        }

        // 4. Fallback: If query is a Chinese province/region, search CN state or name
        val pinyin = when (trimmed) {
            "北京" -> "Beijing"
            "广东" -> "Guangdong"
            "上海" -> "Shanghai"
            "四川" -> "Sichuan"
            "河南" -> "Henan"
            "浙江" -> "Zhejiang"
            "江苏" -> "Jiangsu"
            "山东" -> "Shandong"
            "湖北" -> "Hubei"
            "陕西" -> "Shaanxi"
            "湖南" -> "Hunan"
            "辽宁" -> "Liaoning"
            "重庆" -> "Chongqing"
            "香港" -> "Hong Kong"
            "台湾" -> "Taiwan"
            "福建" -> "Fujian"
            "云南" -> "Yunnan"
            "贵州" -> "Guizhou"
            "安徽" -> "Anhui"
            "江西" -> "Jiangxi"
            "河北" -> "Hebei"
            "天津" -> "Tianjin"
            "海南" -> "Hainan"
            "广西" -> "Guangxi"
            "新疆" -> "Xinjiang"
            "西藏" -> "Tibet"
            "内蒙古" -> "Inner Mongolia"
            "吉林" -> "Jilin"
            "黑龙江" -> "Heilongjiang"
            "山西" -> "Shanxi"
            "甘肃" -> "Gansu"
            "青海" -> "Qinghai"
            "宁夏" -> "Ningxia"
            "澳门" -> "Macao"
            else -> null
        }
        if (pinyin != null) {
            val stateEndpoint = "${getBaseUrl()}/json/stations/search?countrycode=CN&state=${encode(pinyin)}&hidebroken=true&order=votes&reverse=true&limit=$limit"
            val stateRes = fetchStations(stateEndpoint)
            if (stateRes.isSuccess && stateRes.getOrNull()?.isNotEmpty() == true) {
                return@withContext stateRes
            }
        }

        nameResult
    }

    private fun fetchStations(url: String): Result<List<RadioStation>> {
        responseCache[url]?.let {
            return Result.success(it)
        }
        for (attempt in 0..servers.size) {
            try {
                val currentBase = getBaseUrl()
                val targetServer = servers[(currentServerIndex + attempt) % servers.size]
                val requestUrl = url.replace(currentBase, targetServer)

                val request = Request.Builder()
                    .url(requestUrl)
                    .header("User-Agent", "DragonFM/1.0.0 (Android)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        val list = json.decodeFromString<List<RadioStation>>(responseBody)
                        if (list.isNotEmpty()) {
                            responseCache[url] = list
                        }
                        return Result.success(list)
                    }
                }
            } catch (e: Exception) {
                switchServer()
            }
        }
        return Result.failure(Exception("Failed to fetch radio stations after trying all mirrors."))
    }
}
