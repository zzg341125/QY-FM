package com.dragon.fm.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.PlayerState
import com.dragon.fm.ui.components.DragonHaloVisualizer
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.LiveRed
import com.dragon.fm.ui.theme.SurfaceDark
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SleepModeOverlay(
    playerState: PlayerState,
    sleepTimerSeconds: Int?,
    selectedMinutes: Int?,
    onTogglePlayPause: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onExit: () -> Unit
) {
    var currentTimeStr by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeStr = LocalTime.now().format(formatter)
            delay(1000)
        }
    }

    val remainingSec = sleepTimerSeconds
    val isTimerRunning = remainingSec != null && remainingSec > 0
    val formattedCountdown = if (remainingSec != null && remainingSec > 0) {
        val m = remainingSec / 60
        val s = remainingSec % 60
        String.format("%02d:%02d", m, s)
    } else ""

    val isOffSelected = !isTimerRunning || selectedMinutes == null || selectedMinutes == 0
    val isCustomActive = isTimerRunning && selectedMinutes != null && selectedMinutes !in listOf(15, 30, 60)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030508))
    ) {
        if (isLandscape) {
            // ========================================================
            // LANDSCAPE DUAL-PANE BEDSIDE NIGHT CLOCK LAYOUT
            // ========================================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Bedside Glowing Clock & Station Aura (44% Width)
                Column(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Sleep",
                            tint = DragonGold.copy(alpha = 0.85f),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "夜间伴听模式",
                            color = DragonGold.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentTimeStr,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = playerState.currentStation?.name ?: "LX FM",
                        color = DragonCyan.copy(alpha = 0.90f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DragonHaloVisualizer(
                        imageUrl = playerState.currentStation?.favicon,
                        isPlaying = playerState.status == PlaybackStatus.PLAYING,
                        isBuffering = false,
                        size = 96.dp
                    )
                }

                // Right Column: Timer Configuration & Play Controls (56% Width)
                Column(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: Exit Capsule Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x331E293B),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onExit() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit Sleep Mode",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "退出睡眠模式",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Middle: Countdown Status Capsule
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isTimerRunning) Color(0x24FFB703) else Color(0x1F334155),
                        border = BorderStroke(0.5.dp, if (isTimerRunning) DragonGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Timer else Icons.Default.TimerOff,
                                contentDescription = "Timer",
                                tint = if (isTimerRunning) DragonGold else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTimerRunning) "将在 $formattedCountdown 后平滑停止" else "伴听定时已关闭 (持续播送)",
                                color = if (isTimerRunning) DragonGold else Color.White.copy(alpha = 0.7f),
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Preset Duration Buttons (Horizontal Grid of 5 Pills)
                    SleepTimerPillsRow(
                        isOffSelected = isOffSelected,
                        isTimerRunning = isTimerRunning,
                        selectedMinutes = selectedMinutes,
                        isCustomActive = isCustomActive,
                        onSetTimer = onSetTimer,
                        onCustomClick = { showCustomDialog = true }
                    )

                    // Master Play / Pause Capsule
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x331E293B))
                            .border(1.dp, DragonCyan.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (playerState.status == PlaybackStatus.PLAYING)
                                Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle",
                            tint = DragonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        } else {
            // ========================================================
            // PORTRAIT COMPACT ELEGANT BEDSIDE LAYOUT
            // ========================================================
            // Exit icon top-right
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x331E293B),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.clickable { onExit() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Sleep Mode",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "退出睡眠模式",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NightlightRound,
                    contentDescription = "Sleep",
                    tint = DragonGold.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Soft glowing digital bedside clock
                Text(
                    text = currentTimeStr,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Station Name
                Text(
                    text = playerState.currentStation?.name ?: "LX FM",
                    color = DragonCyan.copy(alpha = 0.88f),
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Soft Halo Visualizer (Gentle Pulse)
                DragonHaloVisualizer(
                    imageUrl = playerState.currentStation?.favicon,
                    isPlaying = playerState.status == PlaybackStatus.PLAYING,
                    isBuffering = false,
                    size = 150.dp
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Sleep Timer countdown banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isTimerRunning) Color(0x24FFB703) else Color(0x1F334155),
                    border = BorderStroke(0.5.dp, if (isTimerRunning) DragonGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Timer else Icons.Default.TimerOff,
                            contentDescription = "Timer",
                            tint = if (isTimerRunning) DragonGold else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTimerRunning) "将在 $formattedCountdown 后平滑停止" else "伴听定时已关闭 (持续播送)",
                            color = if (isTimerRunning) DragonGold else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preset Timer Pills
                SleepTimerPillsRow(
                    isOffSelected = isOffSelected,
                    isTimerRunning = isTimerRunning,
                    selectedMinutes = selectedMinutes,
                    isCustomActive = isCustomActive,
                    onSetTimer = onSetTimer,
                    onCustomClick = { showCustomDialog = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Simple Play/Pause
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x331E293B))
                        .border(1.dp, DragonCyan.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (playerState.status == PlaybackStatus.PLAYING)
                            Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle",
                        tint = DragonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Custom Minutes Dialog
        if (showCustomDialog) {
            var inputMinutes by remember { mutableStateOf("45") }
            AlertDialog(
                onDismissRequest = { showCustomDialog = false },
                containerColor = SurfaceDark,
                title = { Text("自定义睡眠伴听时长", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "请输入定时关闭的分钟数 (1 ~ 180 分钟):",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputMinutes,
                            onValueChange = { inputMinutes = it.filter { c -> c.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val mins = inputMinutes.toIntOrNull() ?: 45
                            val clamped = mins.coerceIn(1, 180)
                            onSetTimer(clamped)
                            showCustomDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DragonGold, contentColor = Color(0xFF0F172A))
                    ) {
                        Text("确定设定", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showCustomDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White.copy(alpha = 0.6f))
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun SleepTimerPillsRow(
    isOffSelected: Boolean,
    isTimerRunning: Boolean,
    selectedMinutes: Int?,
    isCustomActive: Boolean,
    onSetTimer: (Int) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Off
        SleepTimerPresetPill(
            text = "关闭",
            isSelected = isOffSelected,
            containerColor = if (isOffSelected) LiveRed.copy(alpha = 0.22f) else Color(0x1F334155),
            textColor = if (isOffSelected) LiveRed else Color.White.copy(alpha = 0.75f),
            borderColor = if (isOffSelected) LiveRed else Color.White.copy(alpha = 0.08f),
            modifier = Modifier.weight(1f),
            onClick = { onSetTimer(0) }
        )

        listOf(15, 30, 60).forEach { mins ->
            val isSelected = isTimerRunning && selectedMinutes == mins
            SleepTimerPresetPill(
                text = "${mins}分",
                isSelected = isSelected,
                containerColor = if (isSelected) DragonGold else Color(0x1F334155),
                textColor = if (isSelected) Color(0xFF0F172A) else Color.White.copy(alpha = 0.75f),
                borderColor = if (isSelected) DragonGold else Color.White.copy(alpha = 0.08f),
                modifier = Modifier.weight(1f),
                onClick = { onSetTimer(mins) }
            )
        }

        val customLabel = if (isCustomActive) "${selectedMinutes}分" else "自定义"
        SleepTimerPresetPill(
            text = customLabel,
            isSelected = isCustomActive,
            containerColor = if (isCustomActive) DragonCyan else Color(0x1F334155),
            textColor = if (isCustomActive) Color(0xFF0F172A) else Color.White.copy(alpha = 0.75f),
            borderColor = if (isCustomActive) DragonCyan else Color.White.copy(alpha = 0.08f),
            modifier = Modifier.weight(1.1f),
            onClick = onCustomClick
        )
    }
}

@Composable
private fun SleepTimerPresetPill(
    text: String,
    isSelected: Boolean,
    containerColor: Color,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 1.2.dp else 0.5.dp, borderColor)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
