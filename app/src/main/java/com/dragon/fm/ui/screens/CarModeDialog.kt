package com.dragon.fm.ui.screens

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.PlayerState
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.LiveRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CarModeDialog(
    playerState: PlayerState,
    fontScale: Float = 1.0f,
    quickStations: List<RadioStation> = emptyList(),
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onSelectStation: (RadioStation) -> Unit = {},
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    fun triggerHaptic() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    // 1. Fullscreen Immersive & Auto-Hide Notification Bar (StatusBar)
    val activity = context as? Activity
    val actWindow = activity?.window
    DisposableEffect(actWindow) {
        actWindow?.let { win ->
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controller = WindowCompat.getInsetsController(win, win.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            actWindow?.let { win ->
                win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    // 2. High-precision Digital HUD Clock & Date
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateSdf = SimpleDateFormat("MM月dd日 EEEE", Locale.CHINA)
        while (isActive) {
            val now = Date()
            timeStr = timeSdf.format(now)
            dateStr = dateSdf.format(now)
            delay(1000)
        }
    }

    // 3. Cockpit Master Volume Control with Hardware Sync
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
    }
    var currentVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2))
    }
    var isMuted by remember {
        mutableStateOf(currentVolume == 0)
    }
    var lastNonZeroVolume by remember {
        mutableIntStateOf(if (currentVolume > 0) currentVolume else (maxVolume / 2))
    }

    fun applyVolume(target: Int) {
        val clamped = target.coerceIn(0, maxVolume)
        currentVolume = clamped
        isMuted = (clamped == 0)
        if (clamped > 0) lastNonZeroVolume = clamped
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        } catch (_: Exception) {}
        triggerHaptic()
    }

    fun toggleMuteVolume() {
        if (isMuted || currentVolume == 0) {
            val restore = if (lastNonZeroVolume > 0) lastNonZeroVolume else (maxVolume / 2)
            applyVolume(restore)
        } else {
            lastNonZeroVolume = currentVolume
            applyVolume(0)
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            val vol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: currentVolume
            if (vol != currentVolume) {
                currentVolume = vol
                isMuted = (vol == 0)
                if (vol > 0) lastNonZeroVolume = vol
            }
            delay(400)
        }
    }

    // Breathing pulse animation for Car Cockpit HUD
    val infiniteTransition = rememberInfiniteTransition(label = "CarHUDPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, fontScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScale
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || configuration.screenWidthDp > configuration.screenHeightDp
    val screenHeight = configuration.screenHeightDp.dp

    val isPlaying = playerState.status == PlaybackStatus.PLAYING
    val isBuffering = playerState.status == PlaybackStatus.BUFFERING
    val currentStation = playerState.currentStation

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Also ensure Dialog window hides statusBars
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            dialogWindow?.let { dWin ->
                dWin.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val dController = WindowCompat.getInsetsController(dWin, dWin.decorView)
                dController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                dController.hide(WindowInsetsCompat.Type.statusBars())
            }
            onDispose {
                dialogWindow?.let { dWin ->
                    val dController = WindowCompat.getInsetsController(dWin, dWin.decorView)
                    dController.show(WindowInsetsCompat.Type.statusBars())
                }
            }
        }

        CompositionLocalProvider(LocalDensity provides customDensity) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF070A0F) // Ultra-deep anti-glare cockpit dark
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // HUD Cybernetic Ambient Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        DragonCyan.copy(alpha = 0.12f),
                                        Color(0xFF0C1322).copy(alpha = 0.6f),
                                        Color(0xFF070A0F)
                                    ),
                                    radius = 1200f
                                )
                            )
                    )

                    if (isLandscape) {
                        // ==========================================
                        // LANDSCAPE HUD DUAL-PANE COCKPIT LAYOUT
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            // Top Bar: Clock + Live Status + Exit Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DragonCyan.copy(alpha = 0.16f),
                                        border = BorderStroke(0.8.dp, DragonCyan.copy(alpha = 0.55f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = DragonCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "车载 HUD 巡航模式",
                                                color = DragonCyan,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (timeStr.isNotBlank()) {
                                        Text(
                                            text = timeStr,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = dateStr,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Large Tactile Exit Button
                                Surface(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x331E293B),
                                    border = BorderStroke(0.8.dp, DragonCyan.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Exit Car Mode",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "退出车载",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Middle Area: Left Station Showcase (50%) + Right Big Controls & Volume (50%)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Pane: Station Cover + Title + Program
                                Row(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // High-Def Station Logo with HUD border
                                    Box(
                                        modifier = Modifier
                                            .size(if (screenHeight < 400.dp) 100.dp else 126.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF131B2A))
                                            .border(
                                                width = 1.5.dp,
                                                color = DragonCyan.copy(alpha = pulseGlow),
                                                shape = RoundedCornerShape(20.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!currentStation?.favicon.isNullOrBlank()) {
                                            AsyncImage(
                                                model = currentStation!!.favicon,
                                                contentDescription = currentStation.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Radio,
                                                contentDescription = null,
                                                tint = DragonCyan,
                                                modifier = Modifier.size(46.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Station Name + Favorite Button
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = currentStation?.name ?: "LX FM 在线广播",
                                                color = Color.White,
                                                fontSize = if (screenHeight < 400.dp) 24.sp else 30.sp,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    triggerHaptic()
                                                    onToggleFavorite()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Favorite",
                                                    tint = if (isFavorite) LiveRed else Color.LightGray.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Badges: Dedicated Line + Live Show Title
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (currentStation?.hasDedicatedLine == true) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = DragonCyan.copy(alpha = 0.16f),
                                                    border = BorderStroke(0.6.dp, DragonCyan.copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(DragonCyan)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.5.dp))
                                                        Text(
                                                            text = "高品质专线",
                                                            color = DragonCyan,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            Text(
                                                text = currentStation?.currentProgram?.let { "正在直播: $it" }
                                                    ?: (currentStation?.state?.let { "$it · 在线广播" } ?: "高品质立体声广播"),
                                                color = DragonCyan,
                                                fontSize = if (screenHeight < 400.dp) 14.sp else 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (playerState.nowPlayingTitle.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = playerState.nowPlayingTitle,
                                                color = DragonGold,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Right Pane: Tactical Chunky Controls + Volume Bar
                                Column(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 3 Big Chunky Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val btnSize = if (screenHeight < 400.dp) 62.dp else 74.dp
                                        val playBtnSize = if (screenHeight < 400.dp) 80.dp else 92.dp

                                        // Previous
                                        Box(
                                            modifier = Modifier
                                                .size(btnSize)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF162032))
                                                .border(1.2.dp, Color(0xFF2C3E5B), RoundedCornerShape(20.dp))
                                                .clickable {
                                                    triggerHaptic()
                                                    onPrevious()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SkipPrevious,
                                                contentDescription = "Previous",
                                                tint = Color.White,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }

                                        // Huge Play/Pause
                                        Box(
                                            modifier = Modifier
                                                .size(playBtnSize)
                                                .shadow(16.dp, CircleShape, ambientColor = DragonCyan)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(DragonCyan, Color(0xFF0284C7))
                                                    )
                                                )
                                                .clickable {
                                                    triggerHaptic()
                                                    onTogglePlayPause()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isBuffering) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(36.dp),
                                                    color = Color(0xFF070A0F),
                                                    strokeWidth = 3.5.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Toggle Play",
                                                    tint = Color(0xFF070A0F),
                                                    modifier = Modifier.size(44.dp)
                                                )
                                            }
                                        }

                                        // Next
                                        Box(
                                            modifier = Modifier
                                                .size(btnSize)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF162032))
                                                .border(1.2.dp, Color(0xFF2C3E5B), RoundedCornerShape(20.dp))
                                                .clickable {
                                                    triggerHaptic()
                                                    onNext()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SkipNext,
                                                contentDescription = "Next",
                                                tint = Color.White,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Tactile Master Volume Bar (Mute + Dec + Bar + Inc)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { toggleMuteVolume() },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted || currentVolume == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                                contentDescription = "Mute",
                                                tint = if (isMuted || currentVolume == 0) LiveRed else DragonCyan,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { applyVolume(currentVolume - 1) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x331E293B))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Vol-",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF131B2A))
                                                .border(0.8.dp, Color(0xFF233146), RoundedCornerShape(14.dp))
                                                .clickable {
                                                    // Quick tap center toggles medium-high volume
                                                    applyVolume(maxVolume * 2 / 3)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { currentVolume.toFloat() / maxVolume.toFloat() },
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(14.dp)),
                                                color = DragonCyan,
                                                trackColor = Color.Transparent
                                            )
                                            Text(
                                                text = "音量 $currentVolume / $maxVolume",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(
                                            onClick = { applyVolume(currentVolume + 1) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x331E293B))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Vol+",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Bottom Dock: Quick Stations Switcher
                            if (quickStations.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x330D1420),
                                    border = BorderStroke(0.6.dp, Color(0xFF1E2B3E))
                                ) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(quickStations) { st ->
                                            val isSelected = st.id == currentStation?.id
                                            Surface(
                                                onClick = {
                                                    triggerHaptic()
                                                    onSelectStation(st)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) DragonCyan.copy(alpha = 0.22f) else Color(0x331B2638),
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    if (isSelected) DragonCyan else Color(0xFF2A3A52)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(DragonCyan)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    Text(
                                                        text = st.name,
                                                        color = if (isSelected) DragonCyan else Color.White,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // PORTRAIT HUD BALANCED COCKPIT LAYOUT
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 1. Top HUD Info Bar: Time + Date + Exit Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DragonCyan.copy(alpha = 0.16f),
                                            border = BorderStroke(0.6.dp, DragonCyan.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsCar,
                                                    contentDescription = null,
                                                    tint = DragonCyan,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "HUD 巡航",
                                                    color = DragonCyan,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dateStr,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = timeStr,
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Surface(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x331E293B),
                                    border = BorderStroke(0.8.dp, DragonCyan.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Exit Car Mode",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "退出",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // 2. Middle Station Hero Card (Cover + Station Name + Live Program + Favorite)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                // Station High-Res Cover with Breathing Pulse Ring
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = DragonCyan)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(Color(0xFF131B2A))
                                        .border(
                                            width = 1.8.dp,
                                            color = DragonCyan.copy(alpha = pulseGlow),
                                            shape = RoundedCornerShape(26.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!currentStation?.favicon.isNullOrBlank()) {
                                        AsyncImage(
                                            model = currentStation!!.favicon,
                                            contentDescription = currentStation.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Radio,
                                            contentDescription = null,
                                            tint = DragonCyan,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Station Name + Favorite Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = currentStation?.name ?: "LX FM 在线广播",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            triggerHaptic()
                                            onToggleFavorite()
                                        },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFavorite) LiveRed else Color.LightGray.copy(alpha = 0.6f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Live Show Title + Dedicated Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                                ) {
                                    if (currentStation?.hasDedicatedLine == true) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DragonCyan.copy(alpha = 0.16f),
                                            border = BorderStroke(0.6.dp, DragonCyan.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(DragonCyan)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "高品质专线",
                                                    color = DragonCyan,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Text(
                                        text = currentStation?.currentProgram?.let { "正在直播: $it" }
                                            ?: (currentStation?.state?.let { "$it · 在线广播" } ?: "高品质立体声广播"),
                                        color = DragonCyan,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Now Playing Track
                                if (playerState.nowPlayingTitle.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = playerState.nowPlayingTitle,
                                        color = DragonGold,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }

                            // 3. Tactile Controls & Cockpit Volume Console
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Chunky 3-Button Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Prev
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color(0xFF162032))
                                            .border(1.2.dp, Color(0xFF2C3E5B), RoundedCornerShape(22.dp))
                                            .clickable {
                                                triggerHaptic()
                                                onPrevious()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "Previous",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }

                                    // Giant Play/Pause
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .shadow(20.dp, CircleShape, ambientColor = DragonCyan)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(DragonCyan, Color(0xFF0284C7))
                                                )
                                            )
                                            .clickable {
                                                triggerHaptic()
                                                onTogglePlayPause()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isBuffering) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(42.dp),
                                                color = Color(0xFF070A0F),
                                                strokeWidth = 4.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Toggle Play",
                                                tint = Color(0xFF070A0F),
                                                modifier = Modifier.size(52.dp)
                                            )
                                        }
                                    }

                                    // Next
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color(0xFF162032))
                                            .border(1.2.dp, Color(0xFF2C3E5B), RoundedCornerShape(22.dp))
                                            .clickable {
                                                triggerHaptic()
                                                onNext()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Next",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tactile Master Volume Bar (Mute + Dec + Bar + Inc)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { toggleMuteVolume() },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMuted || currentVolume == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Mute",
                                            tint = if (isMuted || currentVolume == 0) LiveRed else DragonCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { applyVolume(currentVolume - 1) },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x331E293B))
                                            .border(0.8.dp, Color(0xFF2C3E5B), RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Vol-",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(Color(0xFF131B2A))
                                            .border(0.8.dp, Color(0xFF233146), RoundedCornerShape(15.dp))
                                            .clickable {
                                                applyVolume(maxVolume * 2 / 3)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { currentVolume.toFloat() / maxVolume.toFloat() },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(15.dp)),
                                            color = DragonCyan,
                                            trackColor = Color.Transparent
                                        )
                                        Text(
                                            text = "音量 $currentVolume / $maxVolume",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { applyVolume(currentVolume + 1) },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x331E293B))
                                            .border(0.8.dp, Color(0xFF2C3E5B), RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Vol+",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // 4. Bottom Dock: Quick Stations Switcher
                            if (quickStations.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "常听与收藏电台",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${quickStations.size} 个电台",
                                            color = Color.White.copy(alpha = 0.35f),
                                            fontSize = 10.5.sp
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0x330D1420),
                                        border = BorderStroke(0.6.dp, Color(0xFF1E2B3E))
                                    ) {
                                        LazyRow(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            items(quickStations) { st ->
                                                val isSelected = st.id == currentStation?.id
                                                Surface(
                                                    onClick = {
                                                        triggerHaptic()
                                                        onSelectStation(st)
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) DragonCyan.copy(alpha = 0.22f) else Color(0x331B2638),
                                                    border = BorderStroke(
                                                        0.8.dp,
                                                        if (isSelected) DragonCyan else Color(0xFF2A3A52)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(DragonCyan)
                                                            )
                                                            Spacer(modifier = Modifier.width(5.dp))
                                                        }
                                                        Text(
                                                            text = st.name,
                                                            color = if (isSelected) DragonCyan else Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
