package com.dragon.fm.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
data class CustomScenarioMode(
    val id: String,
    val title: String,
    val iconEmoji: String = "radio", // Stores icon key or legacy emoji
    val query: String = "",
    val stations: List<RadioStation> = emptyList(), // Custom added stations in this scenario
    val sortOrder: Int = 0
) {
    val iconVector: ImageVector
        get() = resolveScenarioIcon(iconEmoji)
}

data class ModernIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

val AvailableModernIcons = listOf(
    ModernIconOption("sparkles", "精选", Icons.Outlined.AutoAwesome),
    ModernIconOption("music", "音乐", Icons.Outlined.MusicNote),
    ModernIconOption("car", "通勤", Icons.Outlined.DirectionsCar),
    ModernIconOption("bed", "助眠", Icons.Outlined.Bedtime),
    ModernIconOption("coffee", "专注", Icons.Outlined.LocalCafe),
    ModernIconOption("mic", "资讯", Icons.Outlined.Mic),
    ModernIconOption("radio", "电台", Icons.Outlined.Radio),
    ModernIconOption("star", "收藏", Icons.Outlined.Star),
    ModernIconOption("heart", "挚爱", Icons.Outlined.Favorite),
    ModernIconOption("fire", "热度", Icons.Outlined.Whatshot),
    ModernIconOption("globe", "环球", Icons.Outlined.Public),
    ModernIconOption("bolt", "电波", Icons.Outlined.Bolt),
    ModernIconOption("headphones", "发烧", Icons.Outlined.Headphones),
    ModernIconOption("book", "人文", Icons.Outlined.MenuBook),
    ModernIconOption("spa", "治愈", Icons.Outlined.Spa),
    ModernIconOption("pin", "本土", Icons.Outlined.NearMe),
    ModernIconOption("rocket", "先锋", Icons.Outlined.RocketLaunch),
    ModernIconOption("explore", "雷达", Icons.Outlined.Explore)
)

fun resolveScenarioIcon(iconKey: String?): ImageVector {
    if (iconKey.isNullOrBlank()) return Icons.Outlined.Radio
    return when (iconKey.lowercase().trim()) {
        "sparkles", "auto_awesome", "recommend", "featured", "✨" -> Icons.Outlined.AutoAwesome
        "bolt", "⚡", "lightning" -> Icons.Outlined.Bolt
        "music", "song", "note", "🎵", "🎧" -> Icons.Outlined.MusicNote
        "radio", "broadcast", "fm", "📻" -> Icons.Outlined.Radio
        "car", "commute", "transit", "traffic", "🚗" -> Icons.Outlined.DirectionsCar
        "coffee", "cafe", "tea", "focus", "☕" -> Icons.Outlined.LocalCafe
        "bed", "sleep", "moon", "night", "🌙", "💤" -> Icons.Outlined.Bedtime
        "mic", "talk", "news", "voice", "🎙️", "🎙" -> Icons.Outlined.Mic
        "fire", "flame", "hot", "trend", "🔥" -> Icons.Outlined.Whatshot
        "star", "fav", "favorite", "🌟", "⭐" -> Icons.Outlined.Star
        "globe", "world", "earth", "country", "🌍", "🌏", "🌎" -> Icons.Outlined.Public
        "heart", "love", "like", "❤️", "🌸" -> Icons.Outlined.Favorite
        "headphones", "headset", "audio" -> Icons.Outlined.Headphones
        "book", "read", "story", "literature" -> Icons.Outlined.MenuBook
        "spa", "relax", "nature", "meditation" -> Icons.Outlined.Spa
        "rocket", "fast", "launch", "🚀" -> Icons.Outlined.RocketLaunch
        "explore", "compass", "radar", "🧭" -> Icons.Outlined.Explore
        "dice", "casino", "shuffle", "random", "🎲" -> Icons.Outlined.Casino
        "pin", "location", "place", "local", "📍" -> Icons.Outlined.NearMe
        else -> Icons.Outlined.Radio
    }
}
