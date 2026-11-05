package com.dragon.fm.data.model

data class ChineseTag(
    val name: String,
    val searchQuery: String,
    val isRegion: Boolean
)

object ChineseTagHelper {

    private val regionMap = mapOf(
        "beijing" to "北京",
        "guangdong" to "广东",
        "shanghai" to "上海",
        "sichuan" to "四川",
        "henan" to "河南",
        "zhejiang" to "浙江",
        "jiangsu" to "江苏",
        "shandong" to "山东",
        "hubei" to "湖北",
        "shaanxi" to "陕西",
        "shanxi" to "山西",
        "hunan" to "湖南",
        "fujian" to "福建",
        "chongqing" to "重庆",
        "tianjin" to "天津",
        "hebei" to "河北",
        "liaoning" to "辽宁",
        "jilin" to "吉林",
        "heilongjiang" to "黑龙江",
        "anhui" to "安徽",
        "jiangxi" to "江西",
        "guangxi" to "广西",
        "hainan" to "海南",
        "guizhou" to "贵州",
        "yunnan" to "云南",
        "xizang" to "西藏",
        "tibet" to "西藏",
        "gansu" to "甘肃",
        "qinghai" to "青海",
        "ningxia" to "宁夏",
        "xinjiang" to "新疆",
        "neimenggu" to "内蒙古",
        "inner mongolia" to "内蒙古",
        "hong kong" to "香港",
        "hongkong" to "香港",
        "hk" to "香港",
        "taiwan" to "台湾",
        "macau" to "澳门",
        "macao" to "澳门",
        "china" to "中国",
        "prc" to "中国",
        "the people's republic of china" to "中国",
        "guangzhou" to "广州",
        "shenzhen" to "深圳",
        "hangzhou" to "杭州",
        "nanjing" to "南京",
        "wuhan" to "武汉",
        "chengdu" to "成都",
        "xi'an" to "西安",
        "xian" to "西安",
        "suzhou" to "苏州",
        "changsha" to "长沙",
        "zhengzhou" to "郑州",
        "jinan" to "济南",
        "qingdao" to "青岛",
        "dalian" to "大连",
        "shenyang" to "沈阳",
        "ningbo" to "宁波",
        "xiamen" to "厦门",
        "fuzhou" to "福州",
        "kunming" to "昆明",
        "harbin" to "哈尔滨",
        "changchun" to "长春",
        "nanchang" to "南昌",
        "hefei" to "合肥",
        "guiyang" to "贵阳",
        "nanning" to "南宁",
        "haikou" to "海口",
        "sanya" to "三亚",
        "lanzhou" to "兰州",
        "yinchuan" to "银川",
        "xining" to "西宁",
        "urumqi" to "乌鲁木齐",
        "lhasa" to "拉萨",
        "united states" to "美国",
        "usa" to "美国",
        "united kingdom" to "英国",
        "uk" to "英国",
        "japan" to "日本",
        "germany" to "德国",
        "france" to "法国",
        "korea" to "韩国",
        "south korea" to "韩国",
        "canada" to "加拿大",
        "australia" to "澳大利亚",
        "russia" to "俄罗斯",
        "italy" to "意大利",
        "spain" to "西班牙",
        "hungary" to "匈牙利",
        "singapore" to "新加坡",
        "malaysia" to "马来西亚",
        "new zealand" to "新西兰",
        "switzerland" to "瑞士",
        "sweden" to "瑞典",
        "norway" to "挪威",
        "netherlands" to "荷兰",
        "austria" to "奥地利",
        "belgium" to "比利时",
        "brazil" to "巴西",
        "international" to "环球"
    )

    private val categoryMap = mapOf(
        "news" to ("实时新闻" to "新闻"),
        "talk" to ("谈话互动" to "谈话"),
        "pop" to ("流行音乐" to "流行"),
        "cpop" to ("华语流行" to "流行"),
        "c-pop" to ("华语流行" to "流行"),
        "chinese pop" to ("华语流行" to "流行"),
        "mandopop" to ("华语流行" to "流行"),
        "cantopop" to ("粤语流行" to "粤语"),
        "music" to ("音乐之声" to "音乐"),
        "traffic" to ("交通广播" to "交通"),
        "classical" to ("古典音乐" to "古典"),
        "jazz" to ("爵士慢调" to "jazz"),
        "rock" to ("摇滚力量" to "rock"),
        "folk" to ("民谣民乐" to "民谣"),
        "ambient" to ("轻音助眠" to "轻音乐"),
        "chill" to ("轻音助眠" to "轻音乐"),
        "sleep" to ("轻音助眠" to "轻音乐"),
        "relax" to ("轻音助眠" to "轻音乐"),
        "easy listening" to ("轻松慢调" to "轻音乐"),
        "hits" to ("热播金曲" to "流行"),
        "top40" to ("热播金曲" to "流行"),
        "oldies" to ("经典老歌" to "经典"),
        "classic" to ("经典怀旧" to "经典"),
        "retro" to ("复古金曲" to "经典"),
        "golden" to ("流金岁月" to "经典"),
        "culture" to ("文化科教" to "文化"),
        "education" to ("文化教育" to "教育"),
        "economy" to ("经济财经" to "经济"),
        "finance" to ("经济财经" to "财经"),
        "business" to ("商业金融" to "财经"),
        "sports" to ("体育动态" to "体育"),
        "anime" to ("动漫原声" to "anime"),
        "acg" to ("动漫原声" to "anime"),
        "drama" to ("评书故事" to "评书"),
        "story" to ("评书故事" to "故事"),
        "theatre" to ("文艺剧场" to "文艺"),
        "electronic" to ("电子律动" to "电子"),
        "edm" to ("电子律动" to "电子"),
        "dance" to ("舞曲律动" to "舞曲"),
        "hip hop" to ("说唱嘻哈" to "说唱"),
        "rap" to ("说唱嘻哈" to "说唱"),
        "instrumental" to ("纯音独奏" to "纯音乐"),
        "piano" to ("钢琴独奏" to "钢琴"),
        "opera" to ("戏曲曲艺" to "戏曲"),
        "traditional" to ("传统民乐" to "民乐"),
        "youth" to ("青春活力" to "青年"),
        "international" to ("环球资讯" to "国际"),
        "variety" to ("综合广播" to "综合"),
        "life" to ("城市生活" to "生活")
    )

    fun isChinese(text: String): Boolean {
        return text.any { it in '\u4e00'..'\u9fa5' }
    }

    fun formatRegionName(rawState: String?, rawCountry: String): String {
        val s = rawState?.trim().orEmpty()
        val c = rawCountry.trim()

        fun lookup(raw: String): String? {
            if (raw.isBlank()) return null
            if (isChinese(raw)) {
                return raw.replace("省", "").replace("市", "").trim().ifBlank { raw }
            }
            val lower = raw.lowercase().trim()
            regionMap[lower]?.let { return it }

            val cleaned = lower
                .replace("province", "")
                .replace("sheng", "")
                .replace("city", "")
                .replace("shi", "")
                .trim()
            regionMap[cleaned]?.let { return it }
            return null
        }

        lookup(s)?.let { return it }
        lookup(c)?.let { return it }

        if (s.isBlank() && (c.equals("China", ignoreCase = true) || c == "中国" || c.isBlank())) {
            return "中国"
        }
        return if (s.isNotBlank()) s else (if (c.isNotBlank()) c else "全国")
    }

    fun extractChineseTags(station: RadioStation): List<ChineseTag> {
        val result = mutableListOf<ChineseTag>()
        val seenNames = mutableSetOf<String>()

        // 1. Extract Region Tag (100% Chinese)
        var regionName: String? = null
        var regionQuery: String? = null

        val formatted = formatRegionName(station.state, station.country)
        if (formatted.isNotBlank() && isChinese(formatted)) {
            regionName = formatted
            regionQuery = formatted
        }

        // If station name implies region (e.g. "北京新闻", "广东音乐", "上海交通")
        if (regionName == null) {
            for ((en, cn) in regionMap) {
                if (station.name.contains(cn)) {
                    regionName = cn
                    regionQuery = cn
                    break
                }
            }
        }

        // National / CNR station detection
        if (regionName == null || regionName == "中国") {
            if (station.name.contains("中国之声") || station.name.contains("CNR") || station.name.contains("中央")) {
                regionName = "央广国台"
                regionQuery = "CNR"
            } else if (regionName == null) {
                regionName = "精选电台"
                regionQuery = "全国"
            }
        }

        regionName?.let { name ->
            val query = regionQuery ?: name
            result.add(ChineseTag(name = name, searchQuery = query, isRegion = true))
            seenNames.add(name)
        }

        // 2. Extract Category Tags (100% Chinese, NO English)
        val rawTags = station.tags.split(",", ";", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (raw in rawTags) {
            val lower = raw.lowercase()
            if (raw in listOf("官方专线", "高品质广播", "专线", "广播")) continue
            if (categoryMap.containsKey(lower)) {
                val (cnTitle, query) = categoryMap[lower]!!
                if (!seenNames.contains(cnTitle)) {
                    result.add(ChineseTag(name = cnTitle, searchQuery = query, isRegion = false))
                    seenNames.add(cnTitle)
                }
            } else if (isChinese(raw)) {
                // If it's already Chinese, ensure it's not too long
                val cleaned = raw.take(6)
                if (cleaned.length in 2..6 && !seenNames.contains(cleaned) && cleaned !in listOf("官方专线", "高品质广播", "专线", "广播")) {
                    result.add(ChineseTag(name = cleaned, searchQuery = cleaned, isRegion = false))
                    seenNames.add(cleaned)
                }
            }
            if (result.size >= 4) break
        }

        // 3. Fallback: inspect Station Name for smart category enrichment if needed
        if (result.count { !it.isRegion } < 2) {
            val name = station.name
            val nameRules = listOf(
                ("新闻" to ("实时新闻" to "新闻")),
                ("综合" to ("综合广播" to "综合")),
                ("交通" to ("交通广播" to "交通")),
                ("音乐" to ("华语流行" to "流行")),
                ("流行" to ("华语流行" to "流行")),
                ("经济" to ("经济财经" to "经济")),
                ("财经" to ("经济财经" to "经济")),
                ("文艺" to ("文艺生活" to "文艺")),
                ("故事" to ("评书故事" to "故事")),
                ("经典" to ("经典老歌" to "经典")),
                ("生活" to ("城市生活" to "生活")),
                ("体育" to ("体育动态" to "体育")),
                ("戏曲" to ("戏曲精粹" to "戏曲")),
                ("动感" to ("流行流行" to "流行")),
                ("爱乐" to ("经典爱乐" to "古典")),
                ("都市" to ("都市声波" to "生活"))
            )

            for ((keyword, tagPair) in nameRules) {
                if (name.contains(keyword) && !seenNames.contains(tagPair.first)) {
                    result.add(ChineseTag(name = tagPair.first, searchQuery = tagPair.second, isRegion = false))
                    seenNames.add(tagPair.first)
                    if (result.count { !it.isRegion } >= 2) break
                }
            }
        }

        // 4. Ultimate fallback if still no category tags
        if (result.count { !it.isRegion } == 0) {
            val defaultTag = ChineseTag(name = "综合广播", searchQuery = "综合", isRegion = false)
            result.add(defaultTag)
        }

        return result
    }
}
