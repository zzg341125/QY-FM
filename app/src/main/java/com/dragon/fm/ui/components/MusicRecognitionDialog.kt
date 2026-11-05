package com.dragon.fm.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dragon.fm.data.model.RecognitionMode
import com.dragon.fm.data.model.RecognitionState
import com.dragon.fm.data.model.RecognizedSong
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed

@Composable
fun MusicRecognitionDialog(
    state: RecognitionState,
    currentMode: RecognitionMode,
    isRadioPlaying: Boolean,
    currentStationName: String?,
    onSwitchMode: (RecognitionMode) -> Unit,
    onRetry: () -> Unit,
    onApplyToTrackTitle: (RecognizedSong) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = DragonTheme.surface,
            border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.35f)),
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Top Header: Title & Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DragonTheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Recognition",
                                tint = DragonTheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "听歌识曲",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = DragonTheme.textPrimary
                            )
                            Text(
                                text = "地标声学指纹秒级识别",
                                fontSize = 11.sp,
                                color = DragonTheme.textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DragonTheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector Tabs: Radio Stream vs Microphone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DragonTheme.glassSurface)
                        .border(1.dp, DragonTheme.glassBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val radioActive = currentMode == RecognitionMode.RADIO_STREAM
                    Surface(
                        onClick = { onSwitchMode(RecognitionMode.RADIO_STREAM) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (radioActive) DragonTheme.primary else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = if (radioActive) Color.White else DragonTheme.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "电台直播识曲",
                                fontSize = 12.sp,
                                fontWeight = if (radioActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (radioActive) Color.White else DragonTheme.textSecondary
                            )
                        }
                    }

                    val micActive = currentMode == RecognitionMode.MICROPHONE
                    Surface(
                        onClick = { onSwitchMode(RecognitionMode.MICROPHONE) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (micActive) DragonTheme.primary else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (micActive) Color.White else DragonTheme.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "麦克风环境识曲",
                                fontSize = 12.sp,
                                fontWeight = if (micActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (micActive) Color.White else DragonTheme.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dynamic Body Content based on State
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "recog_state_anim"
                ) { targetState ->
                    when (targetState) {
                        is RecognitionState.Idle,
                        is RecognitionState.Sampling,
                        is RecognitionState.Fingerprinting,
                        is RecognitionState.QueryingCloud -> {
                            ScanningView(
                                state = targetState,
                                mode = currentMode,
                                stationName = currentStationName
                            )
                        }

                        is RecognitionState.Success -> {
                            SuccessResultView(
                                song = targetState.song,
                                onCopy = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("song", "${targetState.song.title} - ${targetState.song.artist}"))
                                    Toast.makeText(context, "已复制歌名: ${targetState.song.title} - ${targetState.song.artist}", Toast.LENGTH_SHORT).show()
                                },
                                onSearch = {
                                    try {
                                        val targetUrl = if (targetState.song.shazamUrl.isNotBlank()) {
                                            targetState.song.shazamUrl
                                        } else {
                                            "https://www.shazam.com/search?q=" + Uri.encode("${targetState.song.title} ${targetState.song.artist}")
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "无法打开 Shazam 对应页面", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onApplyToTitle = {
                                    onApplyToTrackTitle(targetState.song)
                                    Toast.makeText(context, "已同步为当前播放曲目！", Toast.LENGTH_SHORT).show()
                                },
                                onRetry = onRetry
                            )
                        }

                        is RecognitionState.NoMatch -> {
                            NoMatchView(
                                message = targetState.message,
                                currentMode = currentMode,
                                onRetry = onRetry,
                                onSwitchMode = onSwitchMode
                            )
                        }

                        is RecognitionState.Error -> {
                            ErrorView(
                                error = targetState.message,
                                onRetry = onRetry
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningView(
    state: RecognitionState,
    mode: RecognitionMode,
    stationName: String?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val stepText = when (state) {
        is RecognitionState.Sampling -> state.message
        is RecognitionState.Fingerprinting -> "正在提取地标声学指纹特征..."
        is RecognitionState.QueryingCloud -> "正在连接云端百万级曲库比对..."
        else -> if (mode == RecognitionMode.RADIO_STREAM) "正在连接 ${stationName ?: "当前电台"}..." else "正在准备麦克风..."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulsing Radar Sphere
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer Wave Ring
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(DragonTheme.primary.copy(alpha = pulseAlpha))
            )
            // Mid Ring
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(DragonTheme.primary.copy(alpha = 0.22f))
                    .border(1.5.dp, DragonTheme.primary.copy(alpha = 0.5f), CircleShape)
            )
            // Center Core Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(DragonTheme.primary, DragonTheme.primary.copy(alpha = 0.8f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (mode == RecognitionMode.RADIO_STREAM) Icons.Default.Radio else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stepText,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DragonTheme.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state is RecognitionState.Sampling && state.progress > 0f) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = DragonTheme.primary,
                trackColor = DragonTheme.glassBorder
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = DragonTheme.primary,
                trackColor = DragonTheme.glassBorder
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (mode == RecognitionMode.RADIO_STREAM) "数字信号直连提取，不受外界杂音干扰" else "请保持手机靠近扬声器或音源",
            fontSize = 11.sp,
            color = DragonTheme.textSecondary
        )
    }
}

@Composable
private fun SuccessResultView(
    song: RecognizedSong,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
    onApplyToTitle: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Song Detail Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DragonTheme.glassSurface)
                .border(1.dp, DragonTheme.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Cover
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DragonTheme.background)
                    .border(1.dp, DragonTheme.glassBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (song.coverArtUrl.isNotBlank()) {
                    AsyncImage(
                        model = song.coverArtUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = DragonTheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DragonTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DragonTheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.album.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "专辑: ${song.album}" + if (song.releaseYear.isNotBlank()) " (${song.releaseYear})" else "",
                        fontSize = 11.sp,
                        color = DragonTheme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (song.genre.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DragonTheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.genre,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DragonTheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row 1: Copy Song & Apply to Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCopy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DragonTheme.primary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("复制歌名", fontSize = 12.5.sp, color = Color.White)
            }

            Button(
                onClick = onApplyToTitle,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DragonTheme.glassSurface),
                border = BorderStroke(1.dp, DragonTheme.glassBorder),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = DragonTheme.textPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("设为当前曲目", fontSize = 12.5.sp, color = DragonTheme.textPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons Row 2: Search on Web & Retry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSearch,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DragonTheme.glassBorder),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = DragonTheme.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("去音乐平台搜索", fontSize = 12.sp, color = DragonTheme.textSecondary)
            }

            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DragonTheme.glassBorder),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = DragonTheme.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("重新识别", fontSize = 12.sp, color = DragonTheme.textSecondary)
            }
        }
    }
}

@Composable
private fun NoMatchView(
    message: String,
    currentMode: RecognitionMode,
    onRetry: () -> Unit,
    onSwitchMode: (RecognitionMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DragonTheme.glassSurface)
                .border(1.dp, DragonTheme.glassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = DragonTheme.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "未识别到歌曲旋律",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DragonTheme.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            fontSize = 12.sp,
            color = DragonTheme.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DragonTheme.primary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("再次尝试", fontSize = 12.5.sp, color = Color.White)
            }

            if (currentMode == RecognitionMode.RADIO_STREAM) {
                Button(
                    onClick = { onSwitchMode(RecognitionMode.MICROPHONE) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DragonTheme.glassSurface),
                    border = BorderStroke(1.dp, DragonTheme.glassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = DragonTheme.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("切为麦克风识别", fontSize = 12.sp, color = DragonTheme.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(LiveRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = LiveRed,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "识别遇到问题",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DragonTheme.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = error,
            fontSize = 12.sp,
            color = LiveRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DragonTheme.primary),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("重试", fontSize = 13.sp, color = Color.White)
        }
    }
}
