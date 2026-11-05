package com.dragon.fm.ui.components

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

enum class RhythmVisualizerMode(val key: String, val label: String) {
    SPECTRUM("spectrum", "均衡频谱"),
    WAVE("wave", "流体波浪"),
    HYBRID("hybrid", "复合极光"),
    PULSE("pulse", "声学光环");

    fun next(): RhythmVisualizerMode {
        val entries = entries
        return entries[(ordinal + 1) % entries.size]
    }

    companion object {
        fun fromKey(key: String): RhythmVisualizerMode {
            return entries.find { it.key == key } ?: SPECTRUM
        }
    }
}

/**
 * LX FM Signature WaveFlow Dynamic Audio Visualizer
 * Inspired by LX FM PC's WaveFlow Visualizer Engine:
 * - Precision Dancing Frequency Spectrum with Peak Hold (均衡频谱)
 * - Fluid Organic Sine Waves (流体波浪)
 * - Acoustic Breathing Halos (声学光环)
 * - Hybrid Luminous Aurora (复合极光)
 *
 * Fully autonomous frame-ticking engine: Runs 100% reliably even if system "Remove Animations"
 * or Power Saving Mode is enabled on the device.
 */
@Composable
fun WaveFlowRhythmVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = DragonTheme.primary,
    secondaryColor: Color = DragonCyan,
    showModeSelector: Boolean = true
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dragon_fm_prefs", Context.MODE_PRIVATE) }
    var currentMode by remember {
        mutableStateOf(RhythmVisualizerMode.fromKey(prefs.getString("lxfm_rhythm_mode", RhythmVisualizerMode.SPECTRUM.key) ?: "spectrum"))
    }

    var animClock by remember { mutableFloatStateOf(0f) }

    // Robust autonomous frame clock: completely immune to system animator duration scaling & battery saver pause
    LaunchedEffect(isPlaying) {
        var lastTime = SystemClock.uptimeMillis()
        while (isActive) {
            val now = SystemClock.uptimeMillis()
            val dt = ((now - lastTime).coerceIn(1L, 100L)) / 1000f
            lastTime = now
            val speed = if (isPlaying) 2.2f else 0.6f
            animClock = (animClock + dt * speed) % 6283.18f
            delay(16) // ~60fps solid animation
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                val next = currentMode.next()
                currentMode = next
                prefs.edit().putString("lxfm_rhythm_mode", next.key).apply()
                Toast.makeText(context, "律动模式: ${next.label}", Toast.LENGTH_SHORT).show()
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            when (currentMode) {
                RhythmVisualizerMode.SPECTRUM -> {
                    drawPrecisionSpectrum(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        isHybrid = false
                    )
                }
                RhythmVisualizerMode.WAVE -> {
                    drawFluidWaves(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                RhythmVisualizerMode.HYBRID -> {
                    drawFluidWaves(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        alphaScale = 0.55f
                    )
                    drawPrecisionSpectrum(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        isHybrid = true
                    )
                }
                RhythmVisualizerMode.PULSE -> {
                    drawAcousticPulse(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
            }
        }

        if (showModeSelector) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = currentMode.label,
                    fontSize = 8.5.sp,
                    color = secondaryColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 1. Fluid Wave Dynamics
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFluidWaves(
    w: Float,
    h: Float,
    time: Float,
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    alphaScale: Float = 1.0f
) {
    val midY = h * 0.58f
    val playFactor = if (isPlaying) 1.0f else 0.22f

    // Layer 1: Ambient filled gradient wave
    val wavePath = Path()
    wavePath.moveTo(0f, h)

    val stepX = 4f
    var x = 0f
    while (x <= w) {
        val p = x / w
        val harmonic1 = sin(p * 6.0f + time * 2.6f) * (h * 0.28f)
        val harmonic2 = cos(p * 12.0f - time * 1.7f) * (h * 0.16f)
        val beat = (sin(time * 3.4f) * 0.5f + 0.5f) * (h * 0.12f)
        val y = midY - (harmonic1 + harmonic2 + beat) * playFactor
        wavePath.lineTo(x, y.coerceIn(2f, h - 2f))
        x += stepX
    }
    wavePath.lineTo(w, h)
    wavePath.close()

    drawPath(
        path = wavePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                secondaryColor.copy(alpha = 0.40f * alphaScale),
                primaryColor.copy(alpha = 0.20f * alphaScale),
                Color.Transparent
            ),
            startY = 0f,
            endY = h
        )
    )

    // Layer 2: Glowing Crest Stroke
    val crestPath = Path()
    x = 0f
    var isFirst = true
    while (x <= w) {
        val p = x / w
        val harmonic1 = sin(p * 8.0f + time * 3.2f) * (h * 0.25f)
        val harmonic2 = cos(p * 15.0f - time * 2.0f) * (h * 0.14f)
        val y = (midY - (harmonic1 + harmonic2) * playFactor).coerceIn(2f, h - 2f)
        if (isFirst) {
            crestPath.moveTo(x, y)
            isFirst = false
        } else {
            crestPath.lineTo(x, y)
        }
        x += stepX
    }

    drawPath(
        path = crestPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                secondaryColor.copy(alpha = 0.90f * alphaScale),
                primaryColor.copy(alpha = 0.98f * alphaScale),
                secondaryColor.copy(alpha = 0.90f * alphaScale)
            )
        ),
        style = Stroke(width = 2.5.dp.toPx())
    )
}

// 2. Precision Jumping Frequency Spectrum with Peak Hold
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPrecisionSpectrum(
    w: Float,
    h: Float,
    time: Float,
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    isHybrid: Boolean
) {
    val barCount = 38
    val barWidth = (w / barCount) * (if (isHybrid) 0.48f else 0.62f)
    val gap = (w / barCount) - barWidth
    val playFactor = if (isPlaying) 1.0f else 0.18f

    val brush = Brush.verticalGradient(
        colors = listOf(
            secondaryColor.copy(alpha = if (isHybrid) 0.95f else 1.0f),
            primaryColor.copy(alpha = if (isHybrid) 0.70f else 0.85f)
        ),
        startY = 0f,
        endY = h
    )

    for (i in 0 until barCount) {
        val normalizedIdx = i.toFloat() / barCount
        // Audio frequency spectrum simulation driven by multiple harmonics and beat kicks
        val freq1 = sin(normalizedIdx * 9f + time * 4.8f) * 0.45f + 0.55f
        val freq2 = cos(normalizedIdx * 21f - time * 3.2f) * 0.35f + 0.5f
        val beatKick = (sin(time * 6.5f) * 0.5f + 0.5f) * (if (i < 10) 0.35f else 0.15f)
        val bassBoost = if (i < 10) 1.35f else 1.0f
        val rawHeight = ((freq1 * freq2 + beatKick) * bassBoost).coerceIn(0.10f, 1.0f) * playFactor
        val barHeight = (rawHeight * (h * if (isHybrid) 0.75f else 0.92f)).coerceAtLeast(3f)

        val x = i * (barWidth + gap) + gap / 2f
        val y = h - barHeight

        drawRoundRect(
            brush = brush,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )

        // Peak Hold Falling Dot
        if (isPlaying && rawHeight > 0.22f) {
            val peakPhase = (sin(time * 5f + i * 0.7f) * 0.5f + 0.5f)
            val peakY = (y - 3f - peakPhase * 4f).coerceAtLeast(1f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(x, peakY),
                size = Size(barWidth, 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

// 3. Acoustic Concentric Pulse Halos
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAcousticPulse(
    w: Float,
    h: Float,
    time: Float,
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color
) {
    val center = Offset(w / 2f, h / 2f)
    val maxRadius = minOf(w, h) * 0.48f
    val playFactor = if (isPlaying) 1.0f else 0.2f

    val ringCount = 3
    for (i in 0 until ringCount) {
        val ringPhase = (time * 1.5f + i * (6.283f / ringCount)) % 6.283f
        val progress = (ringPhase / 6.283f)
        val radius = maxRadius * progress * playFactor
        val alpha = (1f - progress) * 0.45f * playFactor

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = alpha * 0.5f),
                    primaryColor.copy(alpha = alpha),
                    Color.Transparent
                ),
                center = center,
                radius = (radius + 12f).coerceAtLeast(1f)
            ),
            center = center,
            radius = radius.coerceAtLeast(2f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
