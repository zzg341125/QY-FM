package com.dragon.fm.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import kotlin.math.abs

@Composable
fun FrequencyRuler(
    modifier: Modifier = Modifier,
    currentFreqMhz: Float = 98.7f
) {
    val animatedFreq by animateFloatAsState(
        targetValue = currentFreqMhz,
        animationSpec = spring(stiffness = 300f),
        label = "freq_anim"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(Color(0x330F172A))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Ticks rail on the left
        Canvas(
            modifier = Modifier
                .width(18.dp)
                .fillMaxHeight(0.32f)
        ) {
            val totalTicks = 15
            val tickStepY = size.height / (totalTicks - 1)
            val centerIndex = totalTicks / 2

            for (i in 0 until totalTicks) {
                val y = i * tickStepY
                val distFromCenter = abs(i - centerIndex)
                val isCenter = distFromCenter == 0

                val tickLength = when {
                    isCenter -> size.width
                    distFromCenter % 2 == 0 -> size.width * 0.65f
                    else -> size.width * 0.35f
                }

                val tickColor = when {
                    isCenter -> DragonGold
                    distFromCenter <= 2 -> DragonCyan.copy(alpha = 0.85f)
                    else -> Color.White.copy(alpha = 0.2f)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(0f, y),
                    end = Offset(tickLength, y),
                    strokeWidth = if (isCenter) 3.5f else 2f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Frequency readout badge on the right of ticks
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(start = 6.dp)
        ) {
            Text(
                text = String.format("%.1f", animatedFreq),
                color = DragonGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "MHz",
                color = DragonCyan.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
