package com.dragon.fm.data.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Public
import com.dragon.fm.data.local.PresetsData

/**
 * Validated, data-driven Explore Tag Item backed by real station metadata
 */
data class RealExploreTagItem(
    val title: String,
    val searchTerms: List<String>,
    val isProvince: Boolean = false,
    val isCountry: Boolean = false,
    val countHint: Int = 0
)

data class RealExploreTagGroup(
    val groupName: String,
    val icon: ImageVector,
    val tags: List<RealExploreTagItem>
)

object RealTagHelper {

    fun getTagGroups(): List<RealExploreTagGroup> {
        return listOf(
            // Group 1: 推荐精选 (核心大台与热门)
            RealExploreTagGroup(
                groupName = "推荐精选",
                icon = Icons.Outlined.AutoAwesome,
                tags = listOf(
                    createTag("全国精选", listOf("综合", "新闻", "交通", "音乐", "广播")),
                    createTag("央视伴音", listOf("CCTV", "央视", "伴音")),
                    createTag("国家广播", listOf("CNR", "CRI", "中国之声", "环球资讯", "华语环球")),
                    createTag("热门流行", listOf("流行", "音乐", "金曲", "潮声", "pop", "cpop")),
                    createTag("实时新闻", listOf("新闻", "资讯", "联播", "权威", "news")),
                    createTag("交通出行", listOf("交通", "路况", "畅通", "交广", "出行", "traffic")),
                    createTag("经典老歌", listOf("经典", "岁月", "典藏", "老歌", "金曲", "oldies")),
                    createTag("轻音助眠", listOf("助眠", "轻音乐", "舒缓", "纯净", "冥想", "ambient", "chill"))
                )
            ),

            // Group 2: 真实流派与内容 (从真实电台原始标签提炼，确保全部有真实结果)
            RealExploreTagGroup(
                groupName = "广播分类",
                icon = Icons.Outlined.GraphicEq,
                tags = listOf(
                    createTag("流行音乐", listOf("流行", "音乐", "金曲", "动感", "飞扬", "pop", "cpop")),
                    createTag("新闻资讯", listOf("新闻", "资讯", "权威", "早新闻", "联播", "news")),
                    createTag("交通路况", listOf("交通", "路况", "交广", "畅通", "出行", "traffic")),
                    createTag("财经经济", listOf("财经", "经济", "股市", "商业", "第一财经", "finance")),
                    createTag("华语经典", listOf("经典", "岁月", "典藏", "老歌", "华语", "oldies")),
                    createTag("文艺戏剧", listOf("文艺", "戏曲", "梨园", "剧场", "京剧", "百家")),
                    createTag("粤语名声", listOf("粤语", "南粤", "大湾区", "珠江", "香港", "cantopop")),
                    createTag("爵士慢调", listOf("爵士", "发烧", "慢调", "jazz")),
                    createTag("古典交响", listOf("古典", "交响", "爱乐", "莫扎特", "classical")),
                    createTag("摇滚舞曲", listOf("摇滚", "舞曲", "动感", "rock", "dance", "edm")),
                    createTag("体育赛事", listOf("体育", "赛事", "运动", "sports")),
                    createTag("自然科教", listOf("科教", "探索", "自然", "纪录", "地理")),
                    createTag("少儿亲子", listOf("少儿", "童年", "亲子", "动画"))
                )
            ),

            // Group 3: 34 省市漫游 (全国 34 个省级行政区，直辖/华东/华南/西南/华北/西北/港澳台)
            RealExploreTagGroup(
                groupName = "省市漫游",
                icon = Icons.Outlined.Explore,
                tags = listOf(
                    createProvinceTag("北京"),
                    createProvinceTag("上海"),
                    createProvinceTag("天津"),
                    createProvinceTag("重庆"),
                    createProvinceTag("广东"),
                    createProvinceTag("浙江"),
                    createProvinceTag("江苏"),
                    createProvinceTag("山东"),
                    createProvinceTag("河南"),
                    createProvinceTag("湖北"),
                    createProvinceTag("湖南"),
                    createProvinceTag("四川"),
                    createProvinceTag("陕西"),
                    createProvinceTag("福建"),
                    createProvinceTag("安徽"),
                    createProvinceTag("江西"),
                    createProvinceTag("辽宁"),
                    createProvinceTag("吉林"),
                    createProvinceTag("黑龙江"),
                    createProvinceTag("河北"),
                    createProvinceTag("山西"),
                    createProvinceTag("内蒙古"),
                    createProvinceTag("广西"),
                    createProvinceTag("海南"),
                    createProvinceTag("贵州"),
                    createProvinceTag("云南"),
                    createProvinceTag("西藏"),
                    createProvinceTag("甘肃"),
                    createProvinceTag("青海"),
                    createProvinceTag("宁夏"),
                    createProvinceTag("新疆"),
                    createProvinceTag("香港"),
                    createProvinceTag("澳门"),
                    createProvinceTag("台湾")
                )
            ),

            // Group 4: 全球巡航 (海外精选广播台)
            RealExploreTagGroup(
                groupName = "全球巡航",
                icon = Icons.Outlined.Public,
                tags = listOf(
                    RealExploreTagItem("全球精选", listOf("Global", "World"), isCountry = true, countHint = 12),
                    RealExploreTagItem("香港特别行政区", listOf("Hong Kong"), isCountry = true, countHint = 4),
                    RealExploreTagItem("美国", listOf("United States", "USA"), isCountry = true, countHint = 8),
                    RealExploreTagItem("英国", listOf("United Kingdom", "UK"), isCountry = true, countHint = 6),
                    RealExploreTagItem("日本", listOf("Japan"), isCountry = true, countHint = 5),
                    RealExploreTagItem("德国", listOf("Germany"), isCountry = true, countHint = 4),
                    RealExploreTagItem("法国", listOf("France"), isCountry = true, countHint = 3),
                    RealExploreTagItem("韩国", listOf("South Korea"), isCountry = true, countHint = 3)
                )
            )
        )
    }

    private fun createTag(title: String, terms: List<String>): RealExploreTagItem {
        val count = PresetsData.countMatchingStations(terms)
        return RealExploreTagItem(
            title = title,
            searchTerms = terms,
            countHint = count
        )
    }

    private fun createProvinceTag(province: String): RealExploreTagItem {
        val count = PresetsData.findStationsByProvince(province).size
        return RealExploreTagItem(
            title = province,
            searchTerms = listOf(province),
            isProvince = true,
            countHint = count
        )
    }
}
