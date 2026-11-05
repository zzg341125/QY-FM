package com.dragon.fm.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class AppBackgroundType {
    DEFAULT,
    PRESET,
    CUSTOM
}

data class BackgroundPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val isDark: Boolean = true
) {
    val brush: Brush
        get() = Brush.verticalGradient(gradientColors)
}

object BackgroundPresets {
    val PRESETS = listOf(
        BackgroundPreset(
            id = "twilight",
            title = "流光暮霭",
            subtitle = "深邃靛蓝与暮光暖金交织的沉浸氛围",
            gradientColors = listOf(
                Color(0xFF070E1A),
                Color(0xFF0F2035),
                Color(0xFF1E293B),
                Color(0xFF1A1F2C)
            ),
            isDark = true
        ),
        BackgroundPreset(
            id = "nebula",
            title = "深空星轨",
            subtitle = "暗夜深空与星云脉冲紫韵，极客视听",
            gradientColors = listOf(
                Color(0xFF090615),
                Color(0xFF160E2E),
                Color(0xFF241544),
                Color(0xFF0E0B1A)
            ),
            isDark = true
        ),
        BackgroundPreset(
            id = "daybreak",
            title = "晨曦微露",
            subtitle = "晨曦柔光与通透天青，清新舒适",
            gradientColors = listOf(
                Color(0xFFF1F5F9),
                Color(0xFFE0F2FE),
                Color(0xFFF0FDF4),
                Color(0xFFF8FAFC)
            ),
            isDark = false
        ),
        BackgroundPreset(
            id = "alpine",
            title = "雪岭冷杉",
            subtitle = "高山薄雾与深林静谧青翠，自然纯粹",
            gradientColors = listOf(
                Color(0xFF061412),
                Color(0xFF0E2722),
                Color(0xFF163C34),
                Color(0xFF091714)
            ),
            isDark = true
        ),
        BackgroundPreset(
            id = "acoustic",
            title = "声学光环",
            subtitle = "暖调黑胶胡桃木与光影质感，老电台怀旧美学",
            gradientColors = listOf(
                Color(0xFF140D0A),
                Color(0xFF281810),
                Color(0xFF382318),
                Color(0xFF190F0B)
            ),
            isDark = true
        ),
        BackgroundPreset(
            id = "frosted_slate",
            title = "霜白微光",
            subtitle = "轻盈素雅微光与珍珠岩白，柔和不刺眼",
            gradientColors = listOf(
                Color(0xFFFAF9F6),
                Color(0xFFF3EFEA),
                Color(0xFFEAE5DE),
                Color(0xFFF8F6F2)
            ),
            isDark = false
        )
    )

    fun getById(id: String): BackgroundPreset =
        PRESETS.firstOrNull { it.id == id } ?: PRESETS[0]
}

data class AppBackgroundState(
    val type: AppBackgroundType = AppBackgroundType.DEFAULT,
    val presetId: String = "twilight",
    val customImagePath: String? = null,
    val overlayAlpha: Float = 0.65f
)
