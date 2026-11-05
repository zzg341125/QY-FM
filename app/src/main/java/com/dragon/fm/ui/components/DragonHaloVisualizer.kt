package com.dragon.fm.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.LiveRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DragonHaloVisualizer(
    modifier: Modifier = Modifier,
    imageUrl: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    size: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "halo_transition")

    // Slow rotation for disc vinyl effect
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Wave pulsation factor
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Radial Audio Halo Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.72f
            val barCount = 48
            val angleStep = (2f * PI.toFloat()) / barCount

            for (i in 0 until barCount) {
                val angle = i * angleStep
                // Multi-frequency wave synthesis for organic sound visualizer
                val wave1 = sin(angle * 3f + pulse)
                val wave2 = cos(angle * 5f - pulse * 1.5f)
                val rawIntensity = (wave1 * 0.5f + wave2 * 0.5f + 1f) / 2f

                val barLength = when {
                    isBuffering -> ((sin(pulse * 3f + i * 0.2f) + 1f) / 2f) * 22f + 4f
                    isPlaying -> rawIntensity * 32f + 6f
                    else -> 4f
                }

                val startRadius = baseRadius + 4f
                val endRadius = startRadius + barLength

                val startX = center.x + startRadius * cos(angle)
                val startY = center.y + startRadius * sin(angle)
                val endX = center.x + endRadius * cos(angle)
                val endY = center.y + endRadius * sin(angle)

                val barColor = when {
                    isBuffering -> DragonGold.copy(alpha = 0.8f)
                    isPlaying -> {
                        if (i % 6 == 0) LiveRed.copy(alpha = 0.9f)
                        else if (i % 2 == 0) DragonCyan.copy(alpha = 0.85f)
                        else DragonGold.copy(alpha = 0.7f)
                    }
                    else -> Color.White.copy(alpha = 0.2f)
                }

                drawLine(
                    color = barColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Vinyl Disc / Station Emblem
        val innerDiscSize = size * 0.68f
        Box(
            modifier = Modifier
                .size(innerDiscSize)
                .rotate(if (isPlaying) rotation else 0f)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0A0F1D))
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(DragonCyan, DragonGold, LiveRed, DragonCyan)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Station Emblem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Radio",
                    tint = DragonCyan,
                    modifier = Modifier.size(innerDiscSize * 0.45f)
                )
            }

            // Center spindle dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(1.5.dp, DragonGold, CircleShape)
            )
        }
    }
}
