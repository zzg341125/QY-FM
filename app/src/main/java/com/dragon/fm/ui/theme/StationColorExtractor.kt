package com.dragon.fm.ui.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import com.dragon.fm.data.model.RadioStation
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Extracts and manages ambient extension colors from station icons/covers.
 * Provides modern ambient mesh lighting inspired by Soda Music and Apple Music.
 */
object StationColorExtractor {

    private val colorCache = ConcurrentHashMap<String, Color>()

    /**
     * Fallback harmonic palettes tailored for Dark and Light themes.
     */
    private val darkHarmonicPalette = listOf(
        Color(0xFF00C6FF), // Neon Cyan
        Color(0xFF6366F1), // Royal Indigo
        Color(0xFF8B5CF6), // Electric Violet
        Color(0xFFEC4899), // Neon Pink
        Color(0xFFF43F5E), // Crimson Rose
        Color(0xFFF59E0B), // Warm Amber
        Color(0xFF10B981), // Emerald Green
        Color(0xFF06B6D4), // Deep Sea Teal
        Color(0xFFF97316), // Sunset Orange
        Color(0xFF3B82F6)  // Vivid Blue
    )

    private val lightHarmonicPalette = listOf(
        Color(0xFF0284C7), // Sky Blue
        Color(0xFF4F46E5), // Indigo
        Color(0xFF7C3AED), // Purple
        Color(0xFFDB2777), // Deep Rose
        Color(0xFFE11D48), // Ruby
        Color(0xFFD97706), // Warm Gold
        Color(0xFF059669), // Emerald
        Color(0xFF0891B2), // Cyan
        Color(0xFFEA580C), // Orange
        Color(0xFF2563EB)  // Cobalt
    )

    /**
     * Gets a deterministic, pleasant harmonic ambient color for a station
     * when an icon is not yet loaded or doesn't exist.
     */
    fun getHarmonicColor(station: RadioStation, isDark: Boolean): Color {
        val cached = colorCache[station.id]
        if (cached != null) return cached

        val palette = if (isDark) darkHarmonicPalette else lightHarmonicPalette
        val hash = abs((station.name + station.country).hashCode())
        val color = palette[hash % palette.size]
        return color
    }

    /**
     * Extracts a dominant, vibrant ambient color from a loaded Drawable.
     */
    fun extractFromDrawable(stationId: String, drawable: Drawable, isDark: Boolean): Color {
        val cached = colorCache[stationId]
        if (cached != null) return cached

        try {
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> drawable.toBitmap(width = 96, height = 96)
            }
            val extracted = extractFromBitmap(bitmap, isDark)
            colorCache[stationId] = extracted
            return extracted
        } catch (_: Throwable) {
            val fallback = if (isDark) DragonCyan else Color(0xFF0284C7)
            colorCache[stationId] = fallback
            return fallback
        }
    }

    /**
     * Fast pixel sampling algorithm:
     * Samples pixels in the center 70% of the image to find the most vibrant,
     * non-washed-out accent color that represents the station icon.
     */
    private fun extractFromBitmap(bitmap: Bitmap, isDark: Boolean): Color {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return if (isDark) DragonCyan else Color(0xFF0284C7)
        }

        var bestColor: Color? = null
        var maxVibrancyScore = -1f

        val startX = (width * 0.15f).toInt()
        val endX = (width * 0.85f).toInt()
        val startY = (height * 0.15f).toInt()
        val endY = (height * 0.85f).toInt()

        val stepX = ((endX - startX) / 8).coerceAtLeast(1)
        val stepY = ((endY - startY) / 8).coerceAtLeast(1)

        for (x in startX..endX step stepX) {
            for (y in startY..endY step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = android.graphics.Color.alpha(pixel)
                if (alpha < 128) continue

                val r = android.graphics.Color.red(pixel) / 255f
                val g = android.graphics.Color.green(pixel) / 255f
                val b = android.graphics.Color.blue(pixel) / 255f

                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val delta = max - min
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b

                // Filter out extremes: near-pure black, near-pure white, or pure neutral gray
                if (luminance in 0.12f..0.88f && delta > 0.10f) {
                    val saturation = delta / max
                    // Vibrancy score favors colorful and saturated tones
                    val score = saturation * 1.5f + (1f - abs(luminance - 0.5f))
                    if (score > maxVibrancyScore) {
                        maxVibrancyScore = score
                        bestColor = Color(r, g, b, 1f)
                    }
                }
            }
        }

        return bestColor ?: if (isDark) DragonCyan else Color(0xFF0284C7)
    }

    /**
     * Clears cache if needed
     */
    fun clearCache() {
        colorCache.clear()
    }
}
