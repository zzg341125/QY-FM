package com.dragon.fm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun DragonFMTheme(
    preset: ThemePreset = ThemePreset.SODA_OBSIDIAN,
    content: @Composable () -> Unit
) {
    val colorScheme = if (preset.isDark) {
        darkColorScheme(
            primary = preset.primary,
            secondary = preset.secondary,
            tertiary = preset.accent,
            background = preset.background,
            surface = preset.surface,
            surfaceVariant = preset.surfaceVariant,
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = preset.textPrimary,
            onSurface = preset.textPrimary
        )
    } else {
        lightColorScheme(
            primary = preset.primary,
            secondary = preset.secondary,
            tertiary = preset.accent,
            background = preset.background,
            surface = preset.surface,
            surfaceVariant = preset.surfaceVariant,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = preset.textPrimary,
            onSurface = preset.textPrimary
        )
    }

    CompositionLocalProvider(
        LocalDragonTheme provides preset
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
