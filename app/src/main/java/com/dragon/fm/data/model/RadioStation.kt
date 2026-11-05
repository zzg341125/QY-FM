package com.dragon.fm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamLine(
    val lineId: String = "line-1",
    val name: String = "官方专线",
    val url: String,
    val isStable: Boolean = true,
    val priority: Int = 1
)

enum class StreamSourceMode(val code: String, val title: String, val desc: String) {
    HYBRID("HYBRID", "智能极速模式 (推荐)", "官方专线首选起播，遇网络阻断自动无感容灾热切至公网备用"),
    DEDICATED_ONLY("DEDICATED_ONLY", "纯净官方专线", "仅检索与播放高可用官方专线，享受毫秒级秒开与高稳定度广播"),
    PUBLIC_ONLY("PUBLIC_ONLY", "全球公网广播", "仅使用 Radio-Browser 开放网络广播源，涵盖全球多国多流派频道")
}

@Serializable
data class RadioStation(
    @SerialName("stationuuid")
    val id: String,
    val name: String,
    val url: String,
    @SerialName("url_resolved")
    val urlResolved: String? = null,
    val favicon: String? = null,
    val country: String = "",
    val state: String? = null,
    val tags: String = "",
    val codec: String = "MP3",
    val bitrate: Int = 128,
    val votes: Int = 0,
    val homepage: String? = null,
    // Dragon FM exclusive cyber-tuner frequency (e.g. 98.7 MHz)
    val frequencyMhz: Float = 98.7f,
    val isFavorite: Boolean = false,
    // Multi-line redundancy support
    val lines: List<StreamLine> = emptyList(),
    val activeLineIndex: Int = 0,
    // Real-time EPG program name (e.g. "正在直播：新闻纵横")
    val currentProgram: String? = null,
    val hasDedicatedLine: Boolean = false
) {
    val currentLine: StreamLine?
        get() = lines.getOrNull(activeLineIndex) ?: lines.firstOrNull()

    val playUrl: String
        get() = currentLine?.url?.takeIf { it.isNotBlank() }
            ?: if (!urlResolved.isNullOrBlank()) urlResolved else url

    val tagList: List<String>
        get() = tags.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    @Deprecated("Frequency is obsolete in modern digital network radio")
    val formattedFrequency: String
        get() = ""

    val formattedRegion: String
        get() = ChineseTagHelper.formatRegionName(state, country)

    val effectiveLines: List<StreamLine>
        get() {
            if (lines.isNotEmpty()) return lines
            val fallbackUrl = if (!urlResolved.isNullOrBlank()) urlResolved else url
            return if (fallbackUrl.isNotBlank()) {
                listOf(StreamLine("default", "公网主线", fallbackUrl, isStable = false, priority = 2))
            } else emptyList()
        }
}

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

data class PlayerState(
    val currentStation: RadioStation? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val nowPlayingTitle: String = "",
    val errorMessage: String? = null,
    val signalBars: Int = 5,
    val isRecording: Boolean = false,
    val recordingDurationSec: Int = 0,
    val sleepTimerRemainingSec: Int? = null,
    val activeLineName: String? = null,
    val availableLines: List<StreamLine> = emptyList()
)
