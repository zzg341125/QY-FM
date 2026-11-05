package com.dragon.fm.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed

@Composable
fun LiveAirwavesHeader(
    modifier: Modifier = Modifier,
    station: RadioStation?,
    onOpenRadar: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_anim"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radar / Explore Button
        IconButton(
            onClick = onOpenRadar,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DragonTheme.glassSurface)
                .border(
                    width = 1.dp,
                    color = DragonTheme.glassBorder,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Outlined.TravelExplore,
                contentDescription = "Radar Explore",
                tint = DragonTheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Live Airwaves Badge & Telemetry
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(DragonTheme.glassSurface)
                    .border(
                        width = 1.dp,
                        color = DragonTheme.glassBorder,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                // Red glowing On-Air dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(LiveRed.copy(alpha = alphaAnim))
                )

                Text(
                    text = " LIVE",
                    color = LiveRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )

                Text(
                    text = " • ",
                    color = DragonTheme.textSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )

                // Codec & Bitrate
                Text(
                    text = "${station?.codec ?: "AAC"} ${station?.bitrate?.let { "${it}K" } ?: "HD"}",
                    color = DragonTheme.textPrimary.copy(alpha = 0.9f),
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "  ",
                    fontSize = 10.sp
                )

                // 5-bar signal indicator
                SignalBarsCanvas(bars = 5)
            }
        }

        // Library / Favorites Button
        IconButton(
            onClick = onOpenLibrary,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DragonTheme.glassSurface)
                .border(
                    width = 1.dp,
                    color = DragonTheme.glassBorder,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = "My Library",
                tint = DragonTheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SignalBarsCanvas(
    modifier: Modifier = Modifier,
    bars: Int = 5
) {
    Canvas(
        modifier = modifier
            .size(width = 16.dp, height = 11.dp)
    ) {
        val barWidth = 2.2.dp.toPx()
        val spacing = 1.2.dp.toPx()
        for (i in 0 until 5) {
            val barHeight = ((i + 1) / 5f) * size.height
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight
            drawRoundRect(
                color = if (i < bars) DragonCyan else Color.White.copy(alpha = 0.2f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}
