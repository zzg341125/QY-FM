package com.dragon.fm.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val isDark: Boolean = true,
    val indicatorColor: Color = primary
) {
    LUNAR_LIGHT(
        id = "lunar_light",
        title = "钛空雅素",
        subtitle = "清透典雅素白瓷与晨光深空天青，清爽通透的现代极简风",
        primary = Color(0xFF0284C7),
        secondary = Color(0xFF0D9488),
        accent = Color(0xFFEF4444),
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1F5F9),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        glassSurface = Color(0x140F172A),
        glassBorder = Color(0xFFE2E8F0),
        isDark = false,
        indicatorColor = Color(0xFF7DD3FC)
    ),
    PORCELAIN_JADE(
        id = "porcelain_jade",
        title = "温润白玉",
        subtitle = "温润白瓷与澄澈青玉，典雅宁静的高级古典东方声学美学",
        primary = Color(0xFF059669),
        secondary = Color(0xFF10B981),
        accent = Color(0xFFD97706),
        background = Color(0xFFF9FAF8),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF0F4F1),
        textPrimary = Color(0xFF132A1C),
        textSecondary = Color(0xFF4B6354),
        glassSurface = Color(0x1A132A1C),
        glassBorder = Color(0xFFDCE7DF),
        isDark = false,
        indicatorColor = Color(0xFF6EE7B7)
    ),
    SUNLIT_AMBER(
        id = "sunlit_amber",
        title = "晨曦暖杏",
        subtitle = "晨光暖金与柔润杏色，如同老式木质收音机的早晨微光",
        primary = Color(0xFFD97706),
        secondary = Color(0xFFEA580C),
        accent = Color(0xFFE11D48),
        background = Color(0xFFFFFDF9),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFFBF4EB),
        textPrimary = Color(0xFF291B0D),
        textSecondary = Color(0xFF785635),
        glassSurface = Color(0x1A291B0D),
        glassBorder = Color(0xFFEDE0D1),
        isDark = false,
        indicatorColor = Color(0xFFFBBF24)
    ),
    CHERRY_BLOSSOM(
        id = "cherry_blossom",
        title = "春樱落粉",
        subtitle = "落樱花瓣与柔雾淡粉，温婉明净的现代浪漫微风色调",
        primary = Color(0xFFE11D48),
        secondary = Color(0xFFFB7185),
        accent = Color(0xFF8B5CF6),
        background = Color(0xFFFDF8F9),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFFCEEF1),
        textPrimary = Color(0xFF33131D),
        textSecondary = Color(0xFF7C4355),
        glassSurface = Color(0x1A33131D),
        glassBorder = Color(0xFFF4D4DC),
        isDark = false,
        indicatorColor = Color(0xFFFDA4AF)
    ),
    FROSTED_SILVER(
        id = "frosted_silver",
        title = "霜银极简",
        subtitle = "冷调金属铝银与深空岩灰，德国博朗 Braun 与 Studio Hi-Fi 极简工艺",
        primary = Color(0xFF2563EB),
        secondary = Color(0xFF0284C7),
        accent = Color(0xFFF59E0B),
        background = Color(0xFFF1F5F9),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE2E8F0),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        glassSurface = Color(0x140F172A),
        glassBorder = Color(0xFFCBD5E1),
        isDark = false,
        indicatorColor = Color(0xFF93C5FD)
    ),
    MATCHA_MIST(
        id = "matcha_mist",
        title = "薄雾青茶",
        subtitle = "清晨山岚与冷泡初茶，心旷神怡的治愈系自然雅韵",
        primary = Color(0xFF16A34A),
        secondary = Color(0xFF65A30D),
        accent = Color(0xFFCA8A04),
        background = Color(0xFFF4F9F5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE8F3EA),
        textPrimary = Color(0xFF142918),
        textSecondary = Color(0xFF49674E),
        glassSurface = Color(0x1A142918),
        glassBorder = Color(0xFFD4E6D7),
        isDark = false,
        indicatorColor = Color(0xFF86EFAC)
    ),
    SODA_OBSIDIAN(
        id = "soda_obsidian",
        title = "黑曜极光",
        subtitle = "深空曜黑与现代薄荷绿，Spotify 与 Cursor 级工业暗黑美学",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF06B6D4),
        accent = Color(0xFFF43F5E),
        background = Color(0xFF090B11),
        surface = Color(0xFF11141E),
        surfaceVariant = Color(0xFF181C29),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        glassSurface = Color(0xCC11141E),
        glassBorder = Color(0xFF1E2638)
    ),
    VINTAGE_AMBER(
        id = "vintage_amber",
        title = "胡桃琥珀",
        subtitle = "低噪暖木与金琥珀微光，经典德生/猫王电台工业指针质感",
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFD97706),
        accent = Color(0xFFF43F5E),
        background = Color(0xFF0C0A08),
        surface = Color(0xFF161310),
        surfaceVariant = Color(0xFF221C16),
        textPrimary = Color(0xFFFEF3C7),
        textSecondary = Color(0xFFD97706),
        glassSurface = Color(0xCC161310),
        glassBorder = Color(0xFF2A221C)
    ),
    AURORA_EMERALD(
        id = "aurora_emerald",
        title = "静谧苍翠",
        subtitle = "北欧冷杉与静谧翡翠碧波，山林溪流呼吸般的通透感",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF34D399),
        accent = Color(0xFFF59E0B),
        background = Color(0xFF07120F),
        surface = Color(0xFF0D1C18),
        surfaceVariant = Color(0xFF142923),
        textPrimary = Color(0xFFECFDF5),
        textSecondary = Color(0xFF6EE7B7),
        glassSurface = Color(0xCC0D1C18),
        glassBorder = Color(0xFF1B362F)
    ),
    CYBER_NEON(
        id = "cyber_neon",
        title = "未来霓虹",
        subtitle = "电子罗兰紫与脉冲深蓝，跃动未来感低音电波",
        primary = Color(0xFF8B5CF6),
        secondary = Color(0xFFEC4899),
        accent = Color(0xFF06B6D4),
        background = Color(0xFF0B0914),
        surface = Color(0xFF131022),
        surfaceVariant = Color(0xFF1B162E),
        textPrimary = Color(0xFFFDF4FF),
        textSecondary = Color(0xFFC084FC),
        glassSurface = Color(0xCC131022),
        glassBorder = Color(0xFF262040)
    ),
    SUNSET_GLOW(
        id = "sunset_glow",
        title = "落日余晖",
        subtitle = "加州暮色珊瑚暖橙与微醺晚霞，温暖治愈的情绪陪伴",
        primary = Color(0xFFFB923C),
        secondary = Color(0xFFF43F5E),
        accent = Color(0xFFFBBF24),
        background = Color(0xFF12090F),
        surface = Color(0xFF1D1019),
        surfaceVariant = Color(0xFF2A1724),
        textPrimary = Color(0xFFFFF1F2),
        textSecondary = Color(0xFFFDA4AF),
        glassSurface = Color(0xCC1D1019),
        glassBorder = Color(0xFF382030)
    );

    companion object {
        fun fromId(id: String): ThemePreset = entries.firstOrNull { it.id == id } ?: LUNAR_LIGHT
        val lightPresets: List<ThemePreset> get() = entries.filter { !it.isDark }
        val darkPresets: List<ThemePreset> get() = entries.filter { it.isDark }
    }
}

val LocalDragonTheme = staticCompositionLocalOf { ThemePreset.LUNAR_LIGHT }

object DragonTheme {
    val current: ThemePreset
        @Composable
        get() = LocalDragonTheme.current

    val primary: Color
        @Composable
        get() = LocalDragonTheme.current.primary

    val secondary: Color
        @Composable
        get() = LocalDragonTheme.current.secondary

    val accent: Color
        @Composable
        get() = LocalDragonTheme.current.accent

    val background: Color
        @Composable
        get() = LocalDragonTheme.current.background

    val surface: Color
        @Composable
        get() = LocalDragonTheme.current.surface

    val surfaceVariant: Color
        @Composable
        get() = LocalDragonTheme.current.surfaceVariant

    val textPrimary: Color
        @Composable
        get() = LocalDragonTheme.current.textPrimary

    val textSecondary: Color
        @Composable
        get() = LocalDragonTheme.current.textSecondary

    val glassSurface: Color
        @Composable
        get() = LocalDragonTheme.current.glassSurface

    val glassBorder: Color
        @Composable
        get() = LocalDragonTheme.current.glassBorder

    val isDark: Boolean
        @Composable
        get() = LocalDragonTheme.current.isDark
}
