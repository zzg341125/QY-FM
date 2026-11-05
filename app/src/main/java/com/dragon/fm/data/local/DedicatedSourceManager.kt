package com.dragon.fm.data.local

import com.dragon.fm.data.api.DedicatedRadioApi
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.StreamLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages high-reliability official dedicated broadcast streams.
 * Handles province-based fetching, in-memory caching, name normalization,
 * and dual-source line merging (Line 1: 官方专线, Line 2: 公网备用).
 */
object DedicatedSourceManager {

    private val api = DedicatedRadioApi()
    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    @Volatile
    var currentSourceMode: com.dragon.fm.data.model.StreamSourceMode = com.dragon.fm.data.model.StreamSourceMode.HYBRID

    // In-memory cache of dedicated stations: provinceCode -> list
    private val provinceCache = ConcurrentHashMap<Int, List<RadioStation>>()

    // Province Name -> Province Code mapping
    val provinceCodeMap = mapOf(
        "全国" to 0, "国家" to 0, "央广" to 0,
        "北京" to 110000,
        "河北" to 130000,
        "山西" to 140000,
        "内蒙古" to 150000,
        "辽宁" to 210000,
        "吉林" to 220000,
        "黑龙江" to 230000,
        "上海" to 310000,
        "江苏" to 320000,
        "浙江" to 330000,
        "安徽" to 340000,
        "福建" to 350000,
        "江西" to 360000,
        "山东" to 370000,
        "河南" to 410000,
        "湖北" to 420000,
        "湖南" to 430000,
        "广东" to 440000,
        "广西" to 450000,
        "海南" to 460000,
        "重庆" to 500000,
        "四川" to 510000,
        "贵州" to 520000,
        "云南" to 530000,
        "西藏" to 540000,
        "陕西" to 610000,
        "甘肃" to 620000,
        "青海" to 630000,
        "宁夏" to 640000,
        "新疆" to 650000,
        "新疆兵团" to 660000
    )

    private val cityToProvinceMap = mapOf(
        "广州" to "广东", "深圳" to "广东", "珠海" to "广东", "佛山" to "广东", "东莞" to "广东",
        "成都" to "四川", "绵阳" to "四川",
        "武汉" to "湖北", "宜昌" to "湖北",
        "长沙" to "湖南", "株洲" to "湖南",
        "杭州" to "浙江", "宁波" to "浙江", "温州" to "浙江",
        "南京" to "江苏", "苏州" to "江苏", "无锡" to "江苏", "常州" to "江苏",
        "西安" to "陕西",
        "济南" to "山东", "青岛" to "山东",
        "郑州" to "河南", "洛阳" to "河南",
        "沈阳" to "辽宁", "大连" to "辽宁",
        "哈尔滨" to "黑龙江",
        "长春" to "吉林",
        "太原" to "山西", "大同" to "山西",
        "石家庄" to "河北", "唐山" to "河北",
        "福州" to "福建", "厦门" to "福建",
        "南昌" to "江西",
        "合肥" to "安徽",
        "海口" to "海南", "三亚" to "海南",
        "南宁" to "广西", "桂林" to "广西",
        "贵阳" to "贵州",
        "昆明" to "云南",
        "拉萨" to "西藏",
        "兰州" to "甘肃",
        "西宁" to "青海",
        "银川" to "宁夏",
        "乌鲁木齐" to "新疆",
        "呼和浩特" to "内蒙古", "包头" to "内蒙古"
    )

    init {
        startBackgroundWarmup()
    }

    /**
     * Asynchronously pre-warms national and core provincial broadcast stations.
     * Replicates LX FM PC background warmup mechanism to provide 0ms instant discovery.
     */
    fun startBackgroundWarmup() {
        scope.launch {
            try {
                getNationalStations()
                val popularProvinces = listOf(
                    "北京", "广东", "上海", "浙江", "江苏", "四川", "河南", "湖北", "湖南", "山东", "山西", "陕西", "河北"
                )
                for (p in popularProvinces) {
                    if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) break
                    delay(120)
                    getStationsByProvince(p)
                }
            } catch (_: Exception) {}
        }
    }

    fun resolveProvinceCode(provinceName: String?): Int? {
        if (provinceName.isNullOrBlank()) return null
        val clean = provinceName.trim()
            .removeSuffix("省")
            .removeSuffix("市")
            .removeSuffix("自治区")
            .removeSuffix("特别行政区")
            .removeSuffix("壮族自治区")
            .removeSuffix("回族自治区")
            .removeSuffix("维吾尔自治区")
        return provinceCodeMap[clean]
    }

    /**
     * Get dedicated stations for national channels (中国之声, 经济之声, 音乐之声, etc.)
     */
    suspend fun getNationalStations(): List<RadioStation> = withContext(Dispatchers.IO) {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return@withContext emptyList()
        provinceCache[0]?.let { return@withContext it }
        val res = api.getBroadcastList(categoryId = 0, provinceCode = 0, provinceName = "全国")
        val list = res.getOrDefault(emptyList())
        if (list.isNotEmpty()) {
            provinceCache[0] = list
        }
        list
    }

    /**
     * Instant synchronous access to in-memory cached national stations (0ms).
     */
    fun getCachedNationalStations(): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return emptyList()
        return provinceCache[0] ?: emptyList()
    }

    /**
     * Instant synchronous access to in-memory cached province stations (0ms).
     */
    fun getCachedStationsByProvince(provinceName: String): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return emptyList()
        val code = resolveProvinceCode(provinceName) ?: return emptyList()
        return provinceCache[code] ?: emptyList()
    }

    /**
     * Instant synchronous search in cached dedicated station pool by multi-terms (0ms).
     * Supports matching by title keywords, tags, or current program.
     */
    fun searchCachedDedicatedStations(terms: List<String>): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return emptyList()
        val cleanTerms = terms.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (cleanTerms.isEmpty()) return emptyList()
        val candidatePool = provinceCache.values.flatten().distinctBy { it.id }
        return candidatePool.filter { st ->
            val name = st.name.lowercase()
            val norm = normalizeName(st.name).lowercase()
            val tags = st.tags.lowercase()
            val prog = (st.currentProgram ?: "").lowercase()
            cleanTerms.any { t ->
                name.contains(t) || norm.contains(t) || tags.contains(t) || prog.contains(t)
            }
        }
    }

    /**
     * Instant synchronous search in cached dedicated station pool by single query (0ms).
     */
    fun searchCachedDedicatedStations(query: String): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return emptyList()
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return emptyList()
        return searchCachedDedicatedStations(listOf(cleanQ))
    }

    fun clearCache() {
        provinceCache.clear()
    }

    /**
     * Fetch dedicated stations for a specific province (e.g. 河南 -> 410000, 广东 -> 440000).
     */
    suspend fun getStationsByProvince(provinceName: String): List<RadioStation> = withContext(Dispatchers.IO) {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return@withContext emptyList()
        val code = resolveProvinceCode(provinceName) ?: return@withContext emptyList()
        provinceCache[code]?.let { return@withContext it }

        val res = api.getBroadcastList(categoryId = 0, provinceCode = code, provinceName = provinceName)
        val list = res.getOrDefault(emptyList())
        if (list.isNotEmpty()) {
            provinceCache[code] = list
        }
        list
    }

    /**
     * Search among all loaded/cached dedicated stations or fetch on-demand.
     * Auto-detects province/city keywords in the search query and proactively fetches them.
     */
    suspend fun searchDedicatedStations(query: String, currentProvince: String? = null): List<RadioStation> = withContext(Dispatchers.IO) {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return@withContext emptyList()
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return@withContext emptyList()

        // 1. Ensure national stations are loaded
        val national = getNationalStations()

        // 2. Identify target provinces mentioned in query or specified by user
        val targetProvinces = mutableSetOf<String>()
        currentProvince?.takeIf { it.isNotBlank() }?.let { targetProvinces.add(it) }

        for (prov in provinceCodeMap.keys) {
            if (prov != "全国" && (cleanQ.contains(prov) || prov.contains(cleanQ))) {
                targetProvinces.add(prov)
            }
        }
        for ((city, prov) in cityToProvinceMap) {
            if (cleanQ.contains(city)) {
                targetProvinces.add(prov)
            }
        }

        // Proactively fetch any detected province
        for (p in targetProvinces) {
            getStationsByProvince(p)
        }

        // 3. Match against the unified candidate pool
        val candidatePool = (national + provinceCache.values.flatten()).distinctBy { it.id }
        candidatePool.filter { st ->
            st.name.contains(cleanQ, ignoreCase = true) ||
            cleanQ.contains(normalizeName(st.name), ignoreCase = true) ||
            normalizeName(st.name).contains(normalizeName(cleanQ), ignoreCase = true) ||
            st.tags.contains(cleanQ, ignoreCase = true) ||
            (st.state?.contains(cleanQ, ignoreCase = true) == true) ||
            (st.currentProgram?.contains(cleanQ, ignoreCase = true) == true)
        }
    }

    /**
     * Strips noise words, prefixes, frequencies to allow fuzzy matching
     * (e.g. "CNR-1 中国之声" -> "中国之声", "河南广播电视台交通广播 FM104.1" -> "河南交通广播")
     */
    fun normalizeName(raw: String): String {
        return raw.uppercase()
            .replace(Regex("CNR[-\\s]*\\d*"), "")
            .replace(Regex("CCTV[-\\s]*\\d*"), "")
            .replace(Regex("FM\\s*\\d+(\\.\\d+)?"), "")
            .replace(Regex("AM\\s*\\d+"), "")
            .replace(Regex("广播电视台|人民广播电台|广播电台|人民广播"), "")
            .replace(Regex("[()（）\\-\\s•·]"), "")
            .trim()
    }

    /**
     * Checks if two station names refer to the same station
     */
    fun isSameStation(nameA: String, nameB: String): Boolean {
        val normA = normalizeName(nameA)
        val normB = normalizeName(nameB)
        if (normA.isEmpty() || normB.isEmpty()) return false
        return normA == normB || normA.contains(normB) || normB.contains(normA)
    }

    /**
     * Merges a base station list (from presets or RadioBrowser) with dedicated high-availability stations.
     * When a match is found:
     * - The dedicated stream becomes Line 1 (isStable = true, priority = 1)
     * - The original stream becomes Line 2 (backup line, priority = 2)
     * - Retains HD artwork and real-time EPG program title
     * - Appends unmatched dedicated stations to the top/front of the list for instant visibility
     */
    fun mergeWithDedicatedStations(
        baseStations: List<RadioStation>,
        dedicatedStations: List<RadioStation>
    ): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) {
            return baseStations
        }
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
            if (dedicatedStations.isEmpty()) return emptyList()
            val matchedDedicatedIds = mutableSetOf<String>()
            val resultList = mutableListOf<RadioStation>()
            for (base in baseStations) {
                val matchingDedicated = dedicatedStations.firstOrNull { dedicated ->
                    isSameStation(base.name, dedicated.name)
                }
                if (matchingDedicated != null) {
                    matchedDedicatedIds.add(matchingDedicated.id)
                    resultList.add(base.copy(
                        favicon = matchingDedicated.favicon?.takeIf { it.isNotBlank() } ?: base.favicon,
                        lines = matchingDedicated.lines,
                        activeLineIndex = 0,
                        currentProgram = matchingDedicated.currentProgram ?: base.currentProgram,
                        hasDedicatedLine = true
                    ))
                }
            }
            val unmatchedDedicated = dedicatedStations.filter { it.id !in matchedDedicatedIds }
            resultList.addAll(0, unmatchedDedicated)
            return resultList.distinctBy { it.id }
        }

        if (dedicatedStations.isEmpty()) return baseStations
        if (baseStations.isEmpty()) return dedicatedStations

        val matchedDedicatedIds = mutableSetOf<String>()
        val resultList = mutableListOf<RadioStation>()

        for (base in baseStations) {
            val matchingDedicated = dedicatedStations.firstOrNull { dedicated ->
                isSameStation(base.name, dedicated.name)
            }

            if (matchingDedicated != null) {
                matchedDedicatedIds.add(matchingDedicated.id)

                // Build unified redundant lines
                val combinedLines = mutableListOf<StreamLine>()
                // Line 1: Primary Dedicated official high-quality stream (from dedicated station)
                matchingDedicated.lines.firstOrNull()?.let { dedicatedLine ->
                    combinedLines.add(dedicatedLine.copy(name = "高品质专线", priority = 1))
                } ?: run {
                    combinedLines.add(StreamLine("line-dedicated-1", "高品质专线", matchingDedicated.playUrl, isStable = true, priority = 1))
                }

                // Line 2: Original public stream from base station
                val baseLineUrl = base.playUrl
                if (baseLineUrl.isNotBlank() && baseLineUrl != matchingDedicated.playUrl) {
                    combinedLines.add(StreamLine("line-backup-public", "公网备用", baseLineUrl, isStable = false, priority = 2))
                }

                // Additional lines if present
                for (extra in matchingDedicated.lines.drop(1)) {
                    if (combinedLines.none { it.url == extra.url }) {
                        combinedLines.add(extra.copy(priority = combinedLines.size + 1))
                    }
                }

                val merged = base.copy(
                    favicon = matchingDedicated.favicon?.takeIf { it.isNotBlank() } ?: base.favicon,
                    lines = combinedLines,
                    activeLineIndex = 0,
                    currentProgram = matchingDedicated.currentProgram ?: base.currentProgram,
                    hasDedicatedLine = true
                )
                resultList.add(merged)
            } else {
                resultList.add(base)
            }
        }

        // Dedicated stations that were not in baseStations can be placed at the top (e.g. key official channels)
        val unmatchedDedicated = dedicatedStations.filter { it.id !in matchedDedicatedIds }
        if (unmatchedDedicated.isNotEmpty()) {
            // Prepend key unmatched dedicated stations
            resultList.addAll(0, unmatchedDedicated)
        }

        return resultList.distinctBy { it.id }
    }
}
