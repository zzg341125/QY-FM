package com.dragon.fm.data.local

import com.dragon.fm.data.model.RadioStation

/**
 * 极简版 M3U / M3U8 播放列表读写工具。
 */
object M3uPlaylistIO {

    data class Entry(val name: String, val url: String, val logo: String? = null)

    private val logoRegex = Regex("""tvg-logo\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)

    /** 解析 M3U / M3U8 文本，返回（名称, 流地址, 台标）列表 */
    fun parse(content: String): List<Entry> {
        val entries = mutableListOf<Entry>()
        var pendingName: String? = null
        var pendingLogo: String? = null

        content.lineSequence().forEach { raw ->
            val line = raw.trim().removePrefix("\uFEFF")
            when {
                line.isEmpty() -> Unit

                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = line.lastIndexOf(',')
                    pendingName = if (comma >= 0 && comma < line.lastIndex) {
                        line.substring(comma + 1).trim().ifBlank { null }
                    } else null
                    pendingLogo = logoRegex.find(line)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
                }

                line.startsWith("#") -> Unit

                line.startsWith("http://", ignoreCase = true) ||
                        line.startsWith("https://", ignoreCase = true) -> {
                    val fallbackName = line.substringAfterLast('/')
                        .substringBefore('?')
                        .ifBlank { line }
                    entries.add(Entry(pendingName ?: fallbackName, line, pendingLogo))
                    pendingName = null
                    pendingLogo = null
                }
            }
        }

        return entries.distinctBy { it.url }
    }

    /** 导出为 M3U 文本 */
    fun build(stations: List<RadioStation>): String = buildString {
        appendLine("#EXTM3U")
        stations.forEach { station ->
            appendLine("#EXTINF:-1,${station.name}")
            appendLine(station.playUrl)
        }
    }
}
