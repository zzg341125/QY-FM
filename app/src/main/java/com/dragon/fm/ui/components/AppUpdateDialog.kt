package com.dragon.fm.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dragon.fm.data.model.AppUpdateInfo
import com.dragon.fm.data.model.DownloadState
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadState: DownloadState,
    onStartDownload: () -> Unit,
    onInstallApk: (java.io.File) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!updateInfo.forceUpdate && downloadState !is DownloadState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.forceUpdate && downloadState !is DownloadState.Downloading,
            dismissOnClickOutside = !updateInfo.forceUpdate && downloadState !is DownloadState.Downloading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DragonTheme.surface,
            border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.4f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header with Badge & Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DragonTheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Update",
                            tint = DragonTheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "发现新版本",
                                color = DragonTheme.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DragonTheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "v${updateInfo.versionName}",
                                    color = DragonTheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (updateInfo.releaseDate.isNotBlank()) "发布于 ${updateInfo.releaseDate} · GitHub Release" else "GitHub 官方开源同步更新",
                            color = DragonTheme.textSecondary,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Changelog content area
                Text(
                    text = "更新亮点与说明：",
                    color = DragonTheme.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DragonTheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    val scrollState = rememberScrollState()
                    val logText = updateInfo.changelog.ifBlank {
                        "• 优化播放核心引擎与电台流缓冲稳定性\n• 提升电台切换流畅度与音质表现\n• 修复已知问题并改善用户界面交互细节"
                    }
                    Text(
                        text = logText,
                        color = DragonTheme.textSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Download Progress or State
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        val progressPercent = (downloadState.progress * 100).toInt()
                        val currentMb = downloadState.currentBytes / (1024f * 1024f)
                        val totalMb = downloadState.totalBytes / (1024f * 1024f)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "正在下载新版 APK...",
                                    color = DragonTheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (downloadState.totalBytes > 0) {
                                        String.format("%d%% (%.1f MB / %.1f MB)", progressPercent, currentMb, totalMb)
                                    } else {
                                        String.format("%.1f MB", currentMb)
                                    },
                                    color = DragonTheme.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = DragonTheme.primary,
                                trackColor = DragonTheme.primary.copy(alpha = 0.15f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    is DownloadState.Completed -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DragonCyan.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownloadDone,
                                    contentDescription = null,
                                    tint = DragonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "下载已完成，正在准备安装升级包",
                                    color = DragonCyan,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    is DownloadState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LiveRed.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "下载出错: ${downloadState.message}",
                                color = LiveRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    else -> {}
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!updateInfo.forceUpdate && downloadState !is DownloadState.Downloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DragonTheme.glassBorder)
                        ) {
                            Text(
                                text = "稍后再说",
                                color = DragonTheme.textSecondary,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    val buttonModifier = if (!updateInfo.forceUpdate && downloadState !is DownloadState.Downloading) {
                        Modifier.weight(1.3f)
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    when (downloadState) {
                        is DownloadState.Downloading -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = buttonModifier,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = DragonTheme.primary.copy(alpha = 0.35f),
                                    disabledContentColor = Color.White
                                )
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在下载...", fontSize = 13.5.sp)
                            }
                        }

                        is DownloadState.Completed -> {
                            Button(
                                onClick = { onInstallApk(downloadState.apkFile) },
                                modifier = buttonModifier,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DragonTheme.primary,
                                    contentColor = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownloadDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("立即安装", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }

                        is DownloadState.Error -> {
                            Button(
                                onClick = onStartDownload,
                                modifier = buttonModifier,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LiveRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("重试下载", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }

                        else -> {
                            Button(
                                onClick = onStartDownload,
                                modifier = buttonModifier,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DragonTheme.primary,
                                    contentColor = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("立即更新", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
