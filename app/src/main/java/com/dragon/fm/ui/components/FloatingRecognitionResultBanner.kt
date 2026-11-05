package com.dragon.fm.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dragon.fm.data.model.RecognizedSong
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme
import kotlinx.coroutines.delay

/**
 * LX FM Silent Recognition Result Floating Banner
 * Displays song art, title, artist, one-click copy, web search,
 * and an 8-second auto-dismiss progress bar (similar to LX FM PC).
 */
@Composable
fun FloatingRecognitionResultBanner(
    song: RecognizedSong?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isTimerActive by remember { androidx.compose.runtime.mutableStateOf(true) }

    LaunchedEffect(song, isTimerActive) {
        if (song != null && isTimerActive) {
            delay(8500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (song != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DragonTheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.6f)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { isTimerActive = false }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art or Fallback Icon
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DragonTheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!song.coverArtUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = song.coverArtUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = DragonCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Metadata Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DragonTheme.primary.copy(alpha = 0.16f),
                                    border = BorderStroke(0.5.dp, DragonTheme.primary.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "已识别曲目",
                                        color = DragonTheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "已同步到电台",
                                    color = DragonTheme.textSecondary,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = song.title,
                                color = DragonTheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = song.artist,
                                color = DragonTheme.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Actions
                        IconButton(
                            onClick = {
                                val text = "${song.title} - ${song.artist}"
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("song", text))
                                Toast.makeText(context, "已复制「$text」", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Song",
                                tint = DragonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val targetUrl = if (song.shazamUrl.isNotBlank()) {
                                        song.shazamUrl
                                    } else {
                                        "https://www.shazam.com/search?q=" + Uri.encode("${song.title} ${song.artist}")
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "无法打开 Shazam 页面", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open in Shazam",
                                tint = DragonCyan,
                                modifier = Modifier.size(18.dp)
                            )
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

                    // Progress indicator line at bottom
                    var progress by remember { mutableFloatStateOf(1.0f) }
                    LaunchedEffect(song, isTimerActive) {
                        if (isTimerActive) {
                            val duration = 8000L
                            val interval = 100L
                            val steps = duration / interval
                            for (i in steps downTo 0) {
                                progress = i.toFloat() / steps.toFloat()
                                delay(interval)
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = DragonTheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}
