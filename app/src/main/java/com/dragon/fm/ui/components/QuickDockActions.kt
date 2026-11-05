package com.dragon.fm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed

@Composable
fun QuickDockActions(
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    isRecording: Boolean,
    recordingDurationSec: Int,
    sleepTimerMinutes: Int?,
    onToggleFavorite: () -> Unit,
    onToggleRecording: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenCarMode: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Column(
        modifier = modifier
            .padding(end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Favorite
        ActionButtonItem(
            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            iconTint = if (isFavorite) LiveRed else DragonTheme.textPrimary,
            label = if (isFavorite) "已收藏" else "收藏",
            onClick = onToggleFavorite
        )

        // 2. Airwaves Recording
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isRecording) LiveRed.copy(alpha = 0.22f) else DragonTheme.glassSurface)
                    .border(
                        1.dp,
                        if (isRecording) LiveRed.copy(alpha = 0.6f) else DragonTheme.glassBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onToggleRecording() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FiberManualRecord,
                    contentDescription = "Record",
                    tint = if (isRecording) LiveRed.copy(alpha = recAlpha) else DragonTheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (isRecording) {
                val min = recordingDurationSec / 60
                val sec = recordingDurationSec % 60
                Text(
                    text = String.format("%02d:%02d", min, sec),
                    color = LiveRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 3.dp)
                )
            } else {
                Text(
                    text = "录音",
                    color = DragonTheme.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        // 3. Sleep Timer
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ActionButtonItem(
                icon = Icons.Outlined.Bedtime,
                iconTint = if (sleepTimerMinutes != null) DragonTheme.primary else DragonTheme.textPrimary,
                label = if (sleepTimerMinutes != null) "${sleepTimerMinutes}m" else "睡眠",
                onClick = onOpenSleepTimer
            )
        }

        // 4. Radio Studio Drawer
        ActionButtonItem(
            icon = Icons.Outlined.Tune,
            iconTint = DragonTheme.primary,
            label = "演播室",
            onClick = onOpenStudio
        )

        // 5. Car HUD Mode
        ActionButtonItem(
            icon = Icons.Outlined.DirectionsCar,
            iconTint = DragonTheme.textPrimary,
            label = "车载",
            onClick = onOpenCarMode
        )
    }
}

@Composable
private fun ActionButtonItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DragonTheme.glassSurface)
                .border(
                    1.dp,
                    DragonTheme.glassBorder,
                    RoundedCornerShape(14.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = DragonTheme.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
