package com.dragon.fm.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dragon.fm.ui.theme.ThemePreset

/** 极简版统一配色（支持浅色 / 深色） */
internal data class SimpleColors(
    val background: Color,
    val bottomBar: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val outline: Color,
    val nowPlayingBg: Color,
    val playButton: Color,
    val fieldBg: Color,
    val isDark: Boolean
)

internal fun simpleColors(dark: Boolean): SimpleColors = if (dark) {
    SimpleColors(
        background = Color(0xFF111214),
        bottomBar = Color(0xFF18191C),
        textPrimary = Color(0xFFF2F3F5),
        textSecondary = Color(0xFF9AA0AA),
        divider = Color(0xFF2A2C31),
        outline = Color(0xFFE4E6EA),
        nowPlayingBg = Color(0xFF232B3A),
        playButton = Color(0xFF8397C2),
        fieldBg = Color(0xFF1D1F24),
        isDark = true
    )
} else {
    SimpleColors(
        background = Color(0xFFF7F8FA),
        bottomBar = Color(0xFFF2F3F6),
        textPrimary = Color(0xFF17181A),
        textSecondary = Color(0xFF6B7078),
        divider = Color(0xFFE8EAEE),
        outline = Color(0xFF2B2C2E),
        nowPlayingBg = Color(0xFFE7EEF9),
        playButton = Color(0xFF56698C),
        fieldBg = Color(0xFFFFFFFF),
        isDark = false
    )
}

/** 将 LXFM 内置主题配色映射到极简版界面 */
internal fun simpleColors(preset: ThemePreset): SimpleColors = SimpleColors(
    background = preset.background,
    bottomBar = if (preset.isDark) preset.surface else preset.surfaceVariant,
    textPrimary = preset.textPrimary,
    textSecondary = preset.textSecondary,
    divider = preset.glassBorder,
    outline = if (preset.isDark) Color(0xFFE4E6EA) else Color(0xFF2B2C2E),
    nowPlayingBg = preset.primary.copy(alpha = if (preset.isDark) 0.30f else 0.14f),
    playButton = preset.primary,
    fieldBg = preset.surface,
    isDark = preset.isDark
)

/** 根据浅色 / 深色主题自动切换状态栏、导航栏图标颜色 */
@Composable
internal fun ApplySystemBarAppearance(dark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
