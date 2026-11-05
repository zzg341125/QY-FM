package com.dragon.fm.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed
import kotlin.math.abs

/**
 * Dragon FM 2.0 Horizontal FM Radio Frequency Dial (87.5 ~ 108.0 MHz)
 * Allows users to scrub or tap frequencies with vintage tuner needle aesthetics.
 */
@Composable
fun HorizontalFrequencyDial(
    currentFreqMhz: Float = 99.3f,
    onFrequencyChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val minFreq = 87.5f
    val maxFreq = 108.0f
    val freqSpan = maxFreq - minFreq

    var targetFreq by remember(currentFreqMhz) { mutableFloatStateOf(currentFreqMhz.coerceIn(minFreq, maxFreq)) }

    val animatedFreq by animateFloatAsState(
        targetValue = targetFreq,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.85f),
        label = "tuner_needle_anim"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DragonTheme.glassSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DragonTheme.glassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header Row: Tuner Title + Frequency Value Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Radio,
                        contentDescription = "Tuner Dial",
                        tint = DragonTheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FM 调频电波刻度标尺",
                        color = DragonTheme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DragonTheme.primary.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DragonTheme.primary.copy(alpha = 0.40f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(LiveRed)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = String.format("%.1f", animatedFreq),
                            color = DragonTheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "MHz",
                            color = DragonTheme.primary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Tuner Scale
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (DragonTheme.isDark) Color(0xF206080D) else Color(0x180F172A)
                    )
                    .border(
                        0.5.dp,
                        DragonTheme.glassBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val pct = (offset.x / size.width).coerceIn(0f, 1f)
                            val selectedFreq = minFreq + pct * freqSpan
                            val roundedFreq = kotlin.math.round(selectedFreq * 10f) / 10f
                            targetFreq = roundedFreq
                            onFrequencyChanged(roundedFreq)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val pct = (change.position.x / size.width).coerceIn(0f, 1f)
                            val selectedFreq = minFreq + pct * freqSpan
                            val roundedFreq = kotlin.math.round(selectedFreq * 10f) / 10f
                            targetFreq = roundedFreq
                            onFrequencyChanged(roundedFreq)
                        }
                    }
            ) {
                // Ticks Canvas
                val primaryColor = DragonTheme.primary
                val needleColor = LiveRed
                val textColor = DragonTheme.textSecondary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val totalTicks = 41 // Every 0.5 MHz

                    // Draw ticks
                    for (i in 0 until totalTicks) {
                        val pct = i.toFloat() / (totalTicks - 1)
                        val x = pct * canvasWidth
                        val isMajor = i % 4 == 0
                        val isMedium = i % 2 == 0

                        val tickHeight = when {
                            isMajor -> canvasHeight * 0.55f
                            isMedium -> canvasHeight * 0.35f
                            else -> canvasHeight * 0.20f
                        }

                        val strokeWidth = when {
                            isMajor -> 2.dp.toPx()
                            isMedium -> 1.2.dp.toPx()
                            else -> 0.8.dp.toPx()
                        }

                        val tickColor = when {
                            isMajor -> primaryColor.copy(alpha = 0.75f)
                            isMedium -> primaryColor.copy(alpha = 0.40f)
                            else -> textColor.copy(alpha = 0.25f)
                        }

                        drawLine(
                            color = tickColor,
                            start = Offset(x, 0f),
                            end = Offset(x, tickHeight),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw Red Needle Indicator
                    val needlePct = ((animatedFreq - minFreq) / freqSpan).coerceIn(0f, 1f)
                    val needleX = needlePct * canvasWidth

                    // Needle glow
                    drawLine(
                        color = needleColor.copy(alpha = 0.45f),
                        start = Offset(needleX, 0f),
                        end = Offset(needleX, canvasHeight),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Sharp center needle
                    drawLine(
                        color = needleColor,
                        start = Offset(needleX, 0f),
                        end = Offset(needleX, canvasHeight),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Bottom frequency markers (88, 93, 98, 103, 108)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("88", "92", "96", "100", "104", "108").forEach { label ->
                        Text(
                            text = label,
                            color = DragonTheme.textSecondary.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
