package com.dragon.fm.ui.screens



import android.content.res.Configuration

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.animateColorAsState

import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.LinearEasing

import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.tween

import androidx.compose.animation.fadeIn

import androidx.compose.animation.fadeOut

import androidx.compose.animation.scaleIn

import androidx.compose.animation.scaleOut

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.gestures.detectTapGestures

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

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.pager.VerticalPager

import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.outlined.QueueMusic

import androidx.compose.material.icons.filled.Bolt

import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.DirectionsCar

import androidx.compose.material.icons.filled.Favorite

import androidx.compose.material.icons.filled.FavoriteBorder

import androidx.compose.material.icons.filled.FiberManualRecord

import androidx.compose.material.icons.filled.GraphicEq

import androidx.compose.material.icons.filled.Headphones

import androidx.compose.material.icons.filled.Menu

import androidx.compose.material.icons.filled.NightlightRound

import androidx.compose.material.icons.filled.Pause

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material.icons.filled.Radio

import androidx.compose.material.icons.filled.SkipNext

import androidx.compose.material.icons.filled.SkipPrevious

import androidx.compose.material.icons.filled.Speed

import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material.icons.outlined.BookmarkBorder

import androidx.compose.material.icons.outlined.LocationOn

import androidx.compose.material.icons.outlined.PowerSettingsNew

import androidx.compose.material.icons.outlined.Search

import androidx.compose.material.icons.outlined.TravelExplore

import androidx.compose.material.icons.outlined.Tune

import android.app.Activity

import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateMapOf

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.rememberUpdatedState

import androidx.compose.runtime.setValue

import androidx.compose.runtime.snapshotFlow

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.alpha

import androidx.compose.ui.draw.blur

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.Dp

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage

import com.dragon.fm.R

import com.dragon.fm.data.model.ChineseTagHelper

import com.dragon.fm.data.model.PlaybackStatus

import com.dragon.fm.data.model.RadioStation

import com.dragon.fm.ui.components.AddToScenarioBottomSheet

import com.dragon.fm.ui.components.FloatingRecognitionResultBanner

import com.dragon.fm.ui.components.WaveFlowRhythmVisualizer

import androidx.compose.ui.zIndex

import com.dragon.fm.ui.theme.DarkBackground

import com.dragon.fm.ui.theme.DragonCyan

import com.dragon.fm.ui.theme.DragonGold

import com.dragon.fm.ui.theme.DragonTheme

import com.dragon.fm.ui.theme.LiveRed

import com.dragon.fm.ui.theme.StationColorExtractor

import com.dragon.fm.ui.viewmodel.MainViewModel

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch



/**

 * Animated 3-bar Audio Equalizer Waveform indicator for live playing stations

 */

@Composable

fun AudioEqualizerWave(

    isPlaying: Boolean,

    color: Color = DragonCyan,

    modifier: Modifier = Modifier

) {

    if (!isPlaying) {

        Canvas(modifier = modifier.size(width = 13.dp, height = 18.dp)) {

            val barWidth = 2.5.dp.toPx()

            val spacing = 2.5.dp.toPx()

            val cornerRadius = CornerRadius(1.dp.toPx())

            val staticHeights = listOf(6.dp.toPx(), 8.dp.toPx(), 5.dp.toPx())

            val inactiveColor = color.copy(alpha = 0.35f)

            for (i in 0..2) {

                val left = i * (barWidth + spacing)

                val h = staticHeights[i]

                val top = size.height - h

                drawRoundRect(

                    color = inactiveColor,

                    topLeft = Offset(left, top),

                    size = Size(barWidth, h),

                    cornerRadius = cornerRadius

                )

            }

        }

    } else {

        val transition = rememberInfiniteTransition(label = "equalizer")

        val h1 by transition.animateFloat(

            initialValue = 4f,

            targetValue = 16f,

            animationSpec = infiniteRepeatable(

                animation = tween(durationMillis = 380, easing = LinearEasing),

                repeatMode = RepeatMode.Reverse

            ),

            label = "bar1"

        )

        val h2 by transition.animateFloat(

            initialValue = 16f,

            targetValue = 6f,

            animationSpec = infiniteRepeatable(

                animation = tween(durationMillis = 480, easing = LinearEasing),

                repeatMode = RepeatMode.Reverse

            ),

            label = "bar2"

        )

        val h3 by transition.animateFloat(

            initialValue = 8f,

            targetValue = 18f,

            animationSpec = infiniteRepeatable(

                animation = tween(durationMillis = 340, easing = LinearEasing),

                repeatMode = RepeatMode.Reverse

            ),

            label = "bar3"

        )



        Canvas(modifier = modifier.size(width = 13.dp, height = 18.dp)) {

            val barWidth = 2.5.dp.toPx()

            val spacing = 2.5.dp.toPx()

            val cornerRadius = CornerRadius(1.dp.toPx())

            val dynamicHeights = listOf(h1.dp.toPx(), h2.dp.toPx(), h3.dp.toPx())

            for (i in 0..2) {

                val left = i * (barWidth + spacing)

                val h = dynamicHeights[i]

                val top = size.height - h

                drawRoundRect(

                    color = color,

                    topLeft = Offset(left, top),

                    size = Size(barWidth, h),

                    cornerRadius = cornerRadius

                )

            }

        }

    }

}



@Composable

fun DragonFlowScreen(

    viewModel: MainViewModel,

    onOpenRadar: () -> Unit,

    onOpenLibrary: () -> Unit,

    onOpenStudio: () -> Unit,

    onOpenCarMode: () -> Unit,

    onToggleSleepMode: () -> Unit,

    onOpenTag: (String) -> Unit = {}

) {

    val playlist by viewModel.playlist.collectAsState()

    val currentIndex by viewModel.currentIndex.collectAsState()

    val playerState by viewModel.playerState.collectAsState()

    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()

    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()

    val sleepTimerSec by viewModel.sleepTimerSeconds.collectAsState()

    val selectedScenario by viewModel.selectedScenario.collectAsState()

    val detectedLocation by viewModel.detectedLocation.collectAsState()

    val latencies by viewModel.latencies.collectAsState()

    val isSpeedTesting by viewModel.isSpeedTesting.collectAsState()

    val fontScale by viewModel.fontScale.collectAsState()

    val appBackground by viewModel.appBackground.collectAsState()
    val isDefaultBg = appBackground.type == com.dragon.fm.data.model.AppBackgroundType.DEFAULT

    val isDark = DragonTheme.isDark



    val configuration = LocalConfiguration.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isTablet = configuration.screenWidthDp >= 600

    val isExpandedLayout = isLandscape || isTablet



    // Ambient extension color cache for stations (sampled from icon/cover art)

    val stationAmbientColors = remember { mutableStateMapOf<String, Color>() }

    val currentStation = playlist.getOrNull(currentIndex)

    val fallbackHarmonic = currentStation?.let {

        remember(it.id, isDark) { StationColorExtractor.getHarmonicColor(it, isDark) }

    } ?: DragonTheme.primary

    val targetAmbientColor = currentStation?.let {

        stationAmbientColors[it.id] ?: fallbackHarmonic

    } ?: DragonTheme.primary



    val animatedAmbientColor by animateColorAsState(

        targetValue = targetAmbientColor,

        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),

        label = "ambientColor"

    )



    val floatingSong by viewModel.floatingRecognizedSong.collectAsState()

    val recognitionState by viewModel.recognitionState.collectAsState()



    var showQuitDialog by remember { mutableStateOf(false) }

    var stationToAddToScenario by remember { mutableStateOf<RadioStation?>(null) }



    Box(modifier = Modifier.fillMaxSize()) {

        if (isExpandedLayout) {

            // Tablet / Foldable / Landscape console mode

            LandscapeTabletFlowContent(

                playlist = playlist,

                currentIndex = currentIndex,

                playerState = playerState,

                favoriteIds = favoriteIds,

                isRecording = isRecording,

                recordingDurationSec = recordingDurationSec,

                sleepTimerSec = sleepTimerSec,

                selectedScenario = selectedScenario,

                latencies = latencies,

                animatedAmbientColor = animatedAmbientColor,

                stationAmbientColors = stationAmbientColors,

                isDark = isDark,

                isDefaultBg = isDefaultBg,

                recognitionState = recognitionState,

                onSelectStation = { index -> viewModel.playStationAtIndex(index) },

                onTogglePlayPause = { viewModel.togglePlayPause() },

                onPrevious = { viewModel.previousStation() },

                onNext = { viewModel.nextStation() },

                onToggleFavorite = { station -> viewModel.toggleFavorite(station) },

                onToggleRecording = { viewModel.toggleRecording() },

                onOpenRecognition = { viewModel.startSilentRecognition() },

                onToggleSleepMode = onToggleSleepMode,

                onRunSpeedTest = { viewModel.runPlaylistSpeedTest() },

                onSortByLatency = { viewModel.sortPlaylistByLatency() },

                onOpenCarMode = onOpenCarMode,

                onOpenStudio = onOpenStudio,

                onOpenRadar = onOpenRadar,

                onOpenLibrary = onOpenLibrary,

                onOpenTag = onOpenTag,

                onOpenQuitDialog = { showQuitDialog = true },

                onRetryPlayback = { viewModel.retryPlayback() },

                onSwitchLine = { lineIdx -> viewModel.switchLine(lineIdx) }

            )

        } else {

            // Smartphone portrait TikTok/Soda Music single-screen swipe flow

            PortraitFlowContent(

                playlist = playlist,

                currentIndex = currentIndex,

                playerState = playerState,

                favoriteIds = favoriteIds,

                isRecording = isRecording,

                recordingDurationSec = recordingDurationSec,

                sleepTimerSec = sleepTimerSec,

                selectedScenario = selectedScenario,

                latencies = latencies,

                isSpeedTesting = isSpeedTesting,

                animatedAmbientColor = animatedAmbientColor,

                stationAmbientColors = stationAmbientColors,

                isDark = isDark,

                isDefaultBg = isDefaultBg,

                recognitionState = recognitionState,

                fontScale = fontScale,

                onSelectStation = { index -> viewModel.playStationAtIndex(index) },

                onTogglePlayPause = { viewModel.togglePlayPause() },

                onPrevious = { viewModel.previousStation() },

                onNext = { viewModel.nextStation() },

                onToggleFavorite = { station -> viewModel.toggleFavorite(station) },

                onToggleRecording = { viewModel.toggleRecording() },

                onOpenRecognition = { viewModel.startSilentRecognition() },

                onToggleSleepMode = onToggleSleepMode,

                onRunSpeedTest = { viewModel.runPlaylistSpeedTest() },

                onOpenCarMode = onOpenCarMode,

                onOpenStudio = onOpenStudio,

                onOpenRadar = onOpenRadar,

                onOpenLibrary = onOpenLibrary,

                onOpenTag = onOpenTag,

                onOpenQuitDialog = { showQuitDialog = true },

                onRetryPlayback = { viewModel.retryPlayback() },

                onSwitchLine = { lineIdx -> viewModel.switchLine(lineIdx) },

                onSortByLatency = { viewModel.sortPlaylistByLatency() },

                onRemoveStation = { id -> viewModel.removeStationFromScenario(selectedScenario, id) },

                onMoveStation = { from, to -> viewModel.moveStationInScenario(selectedScenario, from, to) },

                onReorderPlaylist = { list -> viewModel.reorderPlaylist(list) },

                onAddToScenario = { station -> stationToAddToScenario = station }

            )

        }



        // Silent Recognition Floating Result Banner

        FloatingRecognitionResultBanner(

            song = floatingSong,

            onDismiss = { viewModel.dismissFloatingRecognizedSong() },

            modifier = Modifier

                .statusBarsPadding()

                .zIndex(250f)

                .align(Alignment.TopCenter)

        )

    }



    if (showQuitDialog) {

        val context = LocalContext.current

        val activity = context as? Activity

        AlertDialog(

            onDismissRequest = { showQuitDialog = false },

            title = {

                Text(

                    text = "退出 LX FM",

                    color = DragonTheme.textPrimary,

                    fontWeight = FontWeight.Bold

                )

            },

            text = {

                Text(

                    text = "确定要停止电台播放并完全退出应用吗？",

                    color = DragonTheme.textSecondary

                )

            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showQuitDialog = false

                        viewModel.quitApplication(activity)

                    }

                ) {

                    Text("退出应用", color = LiveRed, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showQuitDialog = false }) {

                    Text("继续收听", color = DragonTheme.textSecondary)

                }

            },

            containerColor = DragonTheme.surface,

            shape = RoundedCornerShape(16.dp)

        )

    }



    stationToAddToScenario?.let { station ->

        val customScenarios by viewModel.customScenarios.collectAsState()

        val scenarioOverrides by viewModel.scenarioStationOverrides.collectAsState()

        AddToScenarioBottomSheet(

            station = station,

            customScenarios = customScenarios,

            scenarioOverrides = scenarioOverrides,

            favoriteIds = favoriteIds,

            onToggleInScenario = { scenarioName ->

                viewModel.toggleStationInScenario(scenarioName, station)

            },

            onCreateScenarioWithStation = { title, iconKey ->

                viewModel.createCustomScenario(title, iconKey, listOf(station))

            },

            onDismiss = { stationToAddToScenario = null }

        )

    }

}



/**

 * Tablet & Landscape Dual-Pane Adaptive Console Layout

 */

@Composable

private fun LandscapeTabletFlowContent(

    playlist: List<RadioStation>,

    currentIndex: Int,

    playerState: com.dragon.fm.data.model.PlayerState,

    favoriteIds: Set<String>,

    isRecording: Boolean,

    recordingDurationSec: Int,

    sleepTimerSec: Int?,

    selectedScenario: String,

    latencies: Map<String, Long>,

    animatedAmbientColor: Color,

    stationAmbientColors: MutableMap<String, Color>,

    isDark: Boolean,

    isDefaultBg: Boolean = true,

    recognitionState: com.dragon.fm.data.model.RecognitionState = com.dragon.fm.data.model.RecognitionState.Idle,

    fontScale: Float = 1.0f,

    onSelectStation: (Int) -> Unit,

    onTogglePlayPause: () -> Unit,

    onPrevious: () -> Unit,

    onNext: () -> Unit,

    onToggleFavorite: (RadioStation) -> Unit,

    onToggleRecording: () -> Unit,

    onOpenRecognition: () -> Unit = {},

    onToggleSleepMode: () -> Unit,

    onRunSpeedTest: () -> Unit,

    onSortByLatency: () -> Unit,

    onOpenCarMode: () -> Unit,

    onOpenStudio: () -> Unit,

    onOpenRadar: () -> Unit,

    onOpenLibrary: () -> Unit,

    onOpenTag: (String) -> Unit,

    onOpenQuitDialog: () -> Unit = {},

    onRetryPlayback: () -> Unit = {},

    onSwitchLine: (Int) -> Unit = {}

) {

    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp

    val screenWidth = configuration.screenWidthDp.dp

    val scope = rememberCoroutineScope()

    var showHeartPop by remember { mutableStateOf(false) }



    val currentStation = playlist.getOrNull(currentIndex)

    val latency = currentStation?.let { latencies[it.id] }

    val isPlaying = playerState.status == PlaybackStatus.PLAYING

    val isBuffering = playerState.status == PlaybackStatus.BUFFERING

    val isFav = currentStation?.id?.let { favoriteIds.contains(it) } == true



    val fallbackStationColor = currentStation?.let {

        remember(it.id, isDark) { StationColorExtractor.getHarmonicColor(it, isDark) }

    } ?: animatedAmbientColor

    val stationAmbient = currentStation?.let {

        stationAmbientColors[it.id] ?: fallbackStationColor

    } ?: animatedAmbientColor



    val listState = rememberLazyListState()



    // Smoothly scroll station playlist to current playing station

    LaunchedEffect(currentIndex) {

        if (currentIndex in playlist.indices) {

            val target = (currentIndex - 1).coerceAtLeast(0)

            listState.animateScrollToItem(target)

        }

    }



    Box(

        modifier = Modifier

            .fillMaxSize()

            .then(
                if (isDefaultBg) Modifier.background(DragonTheme.background)
                else Modifier.background(Color.Transparent)
            )
            .pointerInput(Unit) {
                var dragSum = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragSum > 80f) {
                            onOpenStudio()
                        }
                        dragSum = 0f
                    },
                    onDragCancel = { dragSum = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 0) dragSum += dragAmount
                    }
                )
            }

    ) {

        // Atmospheric dynamic blurred backdrop (64px thumbnail for fluid rendering)

        if (currentStation != null && !currentStation.favicon.isNullOrBlank()) {

            val context = LocalContext.current

            val backdropReq = remember(currentStation.favicon) {

                coil.request.ImageRequest.Builder(context)

                    .data(currentStation.favicon)

                    .size(64, 64)

                    .crossfade(false)

                    .build()

            }

            AsyncImage(

                model = backdropReq,

                contentDescription = null,

                contentScale = ContentScale.Crop,

                modifier = Modifier

                    .fillMaxSize()

                    .blur(20.dp)

                    .alpha(if (isDark) 0.22f else 0.12f)

            )

        }



        // Left Ambient Radial Diffusion Glow (matches the station's cover art)

        Box(

            modifier = Modifier

                .fillMaxSize()

                .drawBehind {

                    drawCircle(

                        brush = Brush.radialGradient(

                            colors = listOf(

                                animatedAmbientColor.copy(alpha = if (isDark) 0.35f else 0.20f),

                                animatedAmbientColor.copy(alpha = if (isDark) 0.10f else 0.05f),

                                Color.Transparent

                            ),

                            center = Offset(size.width * 0.22f, size.height * 0.45f),

                            radius = size.width * 0.45f

                        )

                    )

                }

        )



        val isCompactLandscape = screenHeight < 480.dp



        // Main Dual-Pane Structure

        Row(

            modifier = Modifier

                .fillMaxSize()

                .statusBarsPadding()

                .navigationBarsPadding()

                .padding(

                    horizontal = if (isCompactLandscape) 10.dp else 16.dp,

                    vertical = if (isCompactLandscape) 4.dp else 8.dp

                ),

            horizontalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 10.dp else 16.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            // ==========================================

            // LEFT PANE: Immersive Master Turntable & Player (42% Width)

            // ==========================================

            Column(

                modifier = Modifier

                    .weight(0.42f)

                    .fillMaxHeight()

                    .clip(RoundedCornerShape(18.dp))

                    .background(DragonTheme.surface.copy(alpha = if (isDark) 0.55f else 0.75f))

                    .border(0.5.dp, DragonTheme.glassBorder, RoundedCornerShape(18.dp))

                    .padding(

                        horizontal = if (isCompactLandscape) 10.dp else 14.dp,

                        vertical = if (isCompactLandscape) 6.dp else 10.dp

                    ),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.SpaceBetween

            ) {

                // Top Tag Indicator (Tablet / Landscape Mode branding capsule)

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Surface(

                            shape = RoundedCornerShape(8.dp),

                            color = animatedAmbientColor.copy(alpha = 0.18f),

                            border = BorderStroke(0.5.dp, animatedAmbientColor.copy(alpha = 0.4f))

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Box(

                                    modifier = Modifier

                                        .size(5.dp)

                                        .clip(CircleShape)

                                        .background(if (isPlaying) LiveRed else Color.Gray)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = if (isCompactLandscape) "LX FM • 横屏沉浸" else "大屏平板 • LX FM 沉浸",

                                    color = DragonTheme.textPrimary,

                                    fontSize = 10.sp,

                                    fontWeight = FontWeight.Bold

                                )

                            }

                        }



                    }



                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // Car HUD quick entry

                        IconButton(

                            onClick = onOpenCarMode,

                            modifier = Modifier.size(24.dp)

                        ) {

                            Icon(

                                imageVector = Icons.Default.DirectionsCar,

                                contentDescription = "Car HUD",

                                tint = DragonTheme.textSecondary,

                                modifier = Modifier.size(16.dp)

                            )

                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Power Exit quick entry

                        IconButton(

                            onClick = onOpenQuitDialog,

                            modifier = Modifier.size(24.dp)

                        ) {

                            Icon(

                                imageVector = Icons.Outlined.PowerSettingsNew,

                                contentDescription = "Quit",

                                tint = LiveRed.copy(alpha = 0.85f),

                                modifier = Modifier.size(16.dp)

                            )

                        }

                    }

                }



                // Adaptive Album Cover Dimension (compact for landscape phone, expansive for tablet)

                val coverDimension = if (isCompactLandscape) {

                    (screenHeight * 0.22f).coerceIn(64.dp, 82.dp)

                } else {

                    minOf(screenWidth * 0.28f, screenHeight * 0.42f, 260.dp)

                }



                // Squircle Album Art Box (Clean and pure, no floating badge)

                Box(

                    modifier = Modifier

                        .size(coverDimension)

                        .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = stationAmbient.copy(alpha = 0.35f))

                        .clip(RoundedCornerShape(16.dp))

                        .background(DragonTheme.surfaceVariant)

                        .border(0.5.dp, DragonTheme.glassBorder, RoundedCornerShape(16.dp))

                        .pointerInput(currentStation?.id) {

                            detectTapGestures(

                                onDoubleTap = {

                                    currentStation?.let {

                                        onToggleFavorite(it)

                                        showHeartPop = true

                                        scope.launch {

                                            delay(800)

                                            showHeartPop = false

                                        }

                                    }

                                }

                            )

                        },

                    contentAlignment = Alignment.Center

                ) {

                    if (currentStation != null && !currentStation.favicon.isNullOrBlank()) {

                        AsyncImage(

                            model = currentStation.favicon,

                            contentDescription = currentStation.name,

                            contentScale = ContentScale.Crop,

                            modifier = Modifier.fillMaxSize(),

                            onSuccess = { state ->

                                val color = StationColorExtractor.extractFromDrawable(

                                    currentStation.id,

                                    state.result.drawable,

                                    isDark

                                )

                                stationAmbientColors[currentStation.id] = color

                            }

                        )

                    } else {

                        Icon(

                            painter = painterResource(id = R.drawable.app_icon),

                            contentDescription = "App Icon",

                            tint = Color.Unspecified,

                            modifier = Modifier.size(coverDimension * 0.65f)

                        )

                    }

                }



                // Track & Station Metadata

                Column(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalAlignment = Alignment.CenterHorizontally

                ) {

                    val actualSong = playerState.nowPlayingTitle.takeIf {

                        it.isNotBlank() && !it.equals(currentStation?.name, ignoreCase = true) && !it.contains("正在载入", ignoreCase = true)

                    } ?: currentStation?.currentProgram?.let { "正在直播：$it" }

                    if (actualSong != null) {

                        Text(

                            text = actualSong,

                            color = DragonTheme.primary,

                            fontSize = if (isCompactLandscape) 10.sp else 12.5.sp,

                            fontWeight = FontWeight.Medium,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                        Spacer(modifier = Modifier.height(2.dp))

                    }



                    Text(

                        text = currentStation?.name ?: "LX FM",

                        color = DragonTheme.textPrimary,

                        fontSize = if (isCompactLandscape) 16.sp else 21.sp,

                        fontWeight = FontWeight.Bold,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis

                    )



                    Spacer(modifier = Modifier.height(if (isCompactLandscape) 2.dp else 4.dp))



                    if (currentStation != null) {

                        val isError = playerState.status == PlaybackStatus.ERROR

                        val allTags = remember(currentStation.id) { ChineseTagHelper.extractChineseTags(currentStation) }

                        val regionTag = remember(currentStation.id) { allTags.firstOrNull { it.isRegion } }

                        val regionStr = regionTag?.name ?: currentStation.formattedRegion

                        val categoryTags = remember(currentStation.id) { allTags.filter { !it.isRegion } }

                        val regionQuery = regionTag?.searchQuery ?: regionStr



                        if (isCompactLandscape) {

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.Center

                            ) {

                                if (isError) {

                                    Surface(

                                        onClick = onRetryPlayback,

                                        shape = RoundedCornerShape(6.dp),

                                        color = LiveRed.copy(alpha = 0.16f),

                                        border = BorderStroke(0.5.dp, LiveRed.copy(alpha = 0.5f))

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = LiveRed, modifier = Modifier.size(10.dp))

                                            Spacer(modifier = Modifier.width(2.dp))

                                            Text("重试", color = LiveRed, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)

                                        }

                                    }

                                } else {

                                    Surface(

                                        shape = RoundedCornerShape(6.dp),

                                        color = if (isPlaying) LiveRed.copy(alpha = 0.12f) else DragonTheme.glassSurface,

                                        border = BorderStroke(0.5.dp, if (isPlaying) LiveRed.copy(alpha = 0.35f) else DragonTheme.glassBorder)

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isPlaying) LiveRed else Color.Gray))

                                            Spacer(modifier = Modifier.width(3.dp))

                                            Text(if (isBuffering) "缓冲中" else (currentStation.state?.ifBlank { "在线电台" } ?: "在线电台"), color = if (isPlaying) LiveRed else DragonTheme.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)

                                        }

                                    }

                                }



                                Spacer(modifier = Modifier.width(5.dp))



                                Surface(

                                    onClick = { onOpenTag(regionQuery) },

                                    shape = RoundedCornerShape(6.dp),

                                    color = DragonTheme.primary.copy(alpha = 0.12f),

                                    border = BorderStroke(0.5.dp, DragonTheme.primary.copy(alpha = 0.35f))

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = DragonTheme.primary, modifier = Modifier.size(9.dp))

                                        Spacer(modifier = Modifier.width(2.dp))

                                        Text(regionStr, color = DragonTheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Medium)

                                    }

                                }
                                if (currentStation.hasDedicatedLine || currentStation.effectiveLines.size > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val lines = currentStation.effectiveLines
                                    val activeIdx = playerState.currentStation?.activeLineIndex ?: currentStation.activeLineIndex
                                    val lineName = playerState.activeLineName ?: lines.getOrNull(activeIdx)?.name ?: (if (currentStation.hasDedicatedLine) "高品质专线" else "默认主线")
                                    Surface(
                                        onClick = {
                                            if (lines.size > 1) {
                                                onSwitchLine((activeIdx + 1) % lines.size)
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(9.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(if (lines.size > 1) "$lineName ▾" else lineName, color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                            }

                        } else {

                            if (isError) {

                                Surface(

                                    onClick = onRetryPlayback,

                                    shape = RoundedCornerShape(8.dp),

                                    color = LiveRed.copy(alpha = 0.16f),

                                    border = BorderStroke(0.5.dp, LiveRed.copy(alpha = 0.5f))

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = LiveRed, modifier = Modifier.size(12.dp))

                                        Spacer(modifier = Modifier.width(3.dp))

                                        Text("连接受阻 • 点击重试", color = LiveRed, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)

                                    }

                                }

                            } else {

                                Surface(

                                    shape = RoundedCornerShape(8.dp),

                                    color = if (isPlaying) LiveRed.copy(alpha = 0.12f) else DragonTheme.glassSurface,

                                    border = BorderStroke(0.5.dp, if (isPlaying) LiveRed.copy(alpha = 0.35f) else DragonTheme.glassBorder)

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isPlaying) LiveRed else Color.Gray))

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(if (isBuffering) "正在缓冲" else "LIVE 直播", color = if (isPlaying) LiveRed else DragonTheme.textSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)

                                    }

                                }

                            }



                            Spacer(modifier = Modifier.height(5.dp))



                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.Center

                            ) {

                                Surface(

                                    onClick = { onOpenTag(regionQuery) },

                                    shape = RoundedCornerShape(6.dp),

                                    color = DragonTheme.primary.copy(alpha = 0.12f),

                                    border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.35f))

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null, tint = DragonTheme.primary, modifier = Modifier.size(10.dp))

                                        Spacer(modifier = Modifier.width(3.dp))

                                        Text(regionStr, color = DragonTheme.primary, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)

                                    }

                                }
                                if (currentStation.hasDedicatedLine || currentStation.effectiveLines.size > 1) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    val lines = currentStation.effectiveLines
                                    val activeIdx = playerState.currentStation?.activeLineIndex ?: currentStation.activeLineIndex
                                    val lineName = playerState.activeLineName ?: lines.getOrNull(activeIdx)?.name ?: (if (currentStation.hasDedicatedLine) "高品质专线" else "默认主线")
                                    Surface(
                                        onClick = {
                                            if (lines.size > 1) {
                                                onSwitchLine((activeIdx + 1) % lines.size)
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(if (lines.size > 1) "$lineName ▾" else lineName, color = Color(0xFF00E5FF), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }



                                if (categoryTags.isNotEmpty()) {

                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                                        items(categoryTags.take(3)) { tag ->

                                            Surface(

                                                onClick = { onOpenTag(tag.searchQuery) },

                                                shape = RoundedCornerShape(6.dp),

                                                color = DragonTheme.glassSurface,

                                                border = BorderStroke(0.5.dp, DragonTheme.glassBorder)

                                            ) {

                                                Text(tag.name, color = DragonTheme.textPrimary.copy(alpha = 0.85f), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp))

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                }







                Spacer(modifier = Modifier.height(if (isCompactLandscape) 4.dp else 8.dp))



                // Core Playback Controls (Previous, Large Play/Pause Ring, Next, Favorite)

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceEvenly,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    val sideBtnSize = if (isCompactLandscape) 34.dp else 42.dp

                    val sideIconSize = if (isCompactLandscape) 18.dp else 22.dp

                    val playBtnSize = if (isCompactLandscape) 44.dp else 54.dp

                    val playIconSize = if (isCompactLandscape) 22.dp else 28.dp



                    // Previous Station

                    IconButton(

                        onClick = onPrevious,

                        modifier = Modifier

                            .size(sideBtnSize)

                            .clip(CircleShape)

                            .background(DragonTheme.glassSurface)

                    ) {

                        Icon(

                            imageVector = Icons.Default.SkipPrevious,

                            contentDescription = "Previous Station",

                            tint = DragonTheme.textPrimary,

                            modifier = Modifier.size(sideIconSize)

                        )

                    }



                    // Main Play / Pause Capsule

                    Box(

                        modifier = Modifier

                            .size(playBtnSize)

                            .shadow(8.dp, CircleShape)

                            .clip(CircleShape)

                            .background(

                                Brush.linearGradient(listOf(animatedAmbientColor, DragonTheme.secondary))

                            ),

                        contentAlignment = Alignment.Center

                    ) {

                        IconButton(

                            onClick = onTogglePlayPause,

                            modifier = Modifier.fillMaxSize()

                        ) {

                            val iconColor = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White

                            when {

                                isBuffering -> {

                                    CircularProgressIndicator(

                                        modifier = Modifier.size(18.dp),

                                        color = iconColor,

                                        strokeWidth = 2.dp

                                    )

                                }

                                isPlaying -> {

                                    Icon(

                                        imageVector = Icons.Default.Pause,

                                        contentDescription = "Pause",

                                        tint = iconColor,

                                        modifier = Modifier.size(if (isCompactLandscape) 22.dp else 26.dp)

                                    )

                                }

                                else -> {

                                    Icon(

                                        imageVector = Icons.Default.PlayArrow,

                                        contentDescription = "Play",

                                        tint = iconColor,

                                        modifier = Modifier.size(if (isCompactLandscape) 22.dp else 26.dp)

                                    )

                                }

                            }

                        }

                    }



                    // Next Station

                    IconButton(

                        onClick = onNext,

                        modifier = Modifier

                            .size(sideBtnSize)

                            .clip(CircleShape)

                            .background(DragonTheme.glassSurface)

                    ) {

                        Icon(

                            imageVector = Icons.Default.SkipNext,

                            contentDescription = "Next Station",

                            tint = DragonTheme.textPrimary,

                            modifier = Modifier.size(sideIconSize)

                        )

                    }



                    // Favorite Button

                    IconButton(

                        onClick = { currentStation?.let { onToggleFavorite(it) } },

                        modifier = Modifier

                            .size(sideBtnSize)

                            .clip(CircleShape)

                            .background(DragonTheme.glassSurface)

                    ) {

                        Icon(

                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,

                            contentDescription = "Favorite",

                            tint = if (isFav) LiveRed else DragonTheme.textSecondary,

                            modifier = Modifier.size(if (isCompactLandscape) 17.dp else 19.dp)

                        )

                    }

                }



                // Audio Stream Gradient Bar

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(2.dp)

                        .clip(RoundedCornerShape(1.dp))

                        .background(Color(0x22334155))

                ) {

                    Box(

                        modifier = Modifier

                            .fillMaxWidth(if (isPlaying) 0.70f else 0.15f)

                            .height(2.dp)

                            .background(

                                Brush.horizontalGradient(

                                    listOf(animatedAmbientColor.copy(alpha = 0.3f), animatedAmbientColor)

                                )

                            )

                    )

                }

            }



            // ==========================================

            // RIGHT PANE: Control Center & Live Station Playlist (58% Width)

            // ==========================================

            Column(

                modifier = Modifier

                    .weight(0.58f)

                    .fillMaxHeight(),

                verticalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 5.dp else 8.dp)

            ) {

                // 1. Tool Capsule Bar (Horizontal Scrollable for Quick Operations)

                LazyRow(

                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                    verticalAlignment = Alignment.CenterVertically,

                    modifier = Modifier.fillMaxWidth()

                ) {

                    // Scenario Picker

                    item {

                        Surface(

                            onClick = onOpenStudio,

                            shape = RoundedCornerShape(14.dp),

                            color = DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.Menu,

                                    contentDescription = "Scenario",

                                    tint = DragonTheme.primary,

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(

                                    text = selectedScenario,

                                    color = DragonTheme.textPrimary,

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.Bold

                                )

                            }

                        }

                    }



                    // Recording

                    item {

                        Surface(

                            onClick = onToggleRecording,

                            shape = RoundedCornerShape(14.dp),

                            color = if (isRecording) LiveRed.copy(alpha = 0.18f) else DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, if (isRecording) LiveRed else DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.FiberManualRecord,

                                    contentDescription = "Record",

                                    tint = if (isRecording) LiveRed else DragonTheme.textSecondary,

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = if (isRecording) "${recordingDurationSec}s" else "录音",

                                    color = if (isRecording) LiveRed else DragonTheme.textPrimary,

                                    fontSize = 12.sp

                                )

                            }

                        }

                    }



                    // Music Recognition (听歌识曲)

                    item {

                        val isRecognizing = recognitionState is com.dragon.fm.data.model.RecognitionState.Sampling ||

                                            recognitionState is com.dragon.fm.data.model.RecognitionState.Fingerprinting ||

                                            recognitionState is com.dragon.fm.data.model.RecognitionState.QueryingCloud

                        val recogRotation by if (isRecognizing) {

                            val transition = rememberInfiniteTransition(label = "recog_spin_land")

                            transition.animateFloat(

                                initialValue = 0f,

                                targetValue = 360f,

                                animationSpec = infiniteRepeatable(

                                    animation = tween(1200, easing = LinearEasing),

                                    repeatMode = RepeatMode.Restart

                                ),

                                label = "spin_land"

                            )

                        } else {

                            remember { mutableStateOf(0f) }

                        }



                        Surface(

                            onClick = onOpenRecognition,

                            shape = RoundedCornerShape(14.dp),

                            color = if (isRecognizing) DragonCyan.copy(alpha = 0.22f) else DragonTheme.primary.copy(alpha = 0.12f),

                            border = BorderStroke(1.dp, if (isRecognizing) DragonCyan else DragonTheme.primary.copy(alpha = 0.35f))

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.GraphicEq,

                                    contentDescription = "Recognize",

                                    tint = if (isRecognizing) DragonCyan else DragonTheme.primary,

                                    modifier = Modifier

                                        .size(16.dp)

                                        .graphicsLayer { rotationZ = recogRotation }

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = if (isRecognizing) "识别中..." else "识曲",

                                    color = if (isRecognizing) DragonCyan else DragonTheme.primary,

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.Bold

                                )

                            }

                        }

                    }



                    // Sleep Timer

                    item {

                        Surface(

                            onClick = onToggleSleepMode,

                            shape = RoundedCornerShape(14.dp),

                            color = if (sleepTimerSec != null) DragonGold.copy(alpha = 0.18f) else DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, if (sleepTimerSec != null) DragonGold else DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.NightlightRound,

                                    contentDescription = "Sleep",

                                    tint = if (sleepTimerSec != null) DragonGold else DragonTheme.textSecondary,

                                    modifier = Modifier.size(15.dp)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = if (sleepTimerSec != null) "${sleepTimerSec / 60}分" else "睡眠",

                                    color = if (sleepTimerSec != null) DragonGold else DragonTheme.textPrimary,

                                    fontSize = 12.sp

                                )

                            }

                        }

                    }



                    // Radar Search Entry

                    item {

                        Surface(

                            onClick = onOpenRadar,

                            shape = RoundedCornerShape(14.dp),

                            color = DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Outlined.Search,

                                    contentDescription = "Search",

                                    tint = DragonCyan,

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = "搜索/发现",

                                    color = DragonTheme.textPrimary,

                                    fontSize = 12.sp

                                )

                            }

                        }

                    }



                    // Library Entry

                    item {

                        Surface(

                            onClick = onOpenLibrary,

                            shape = RoundedCornerShape(14.dp),

                            color = DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.Person,

                                    contentDescription = "Library",

                                    tint = DragonTheme.textSecondary,

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = "我的收藏",

                                    color = DragonTheme.textPrimary,

                                    fontSize = 12.sp

                                )

                            }

                        }

                    }

                }



                // Subheader

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = 4.dp, vertical = 2.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(

                            imageVector = Icons.AutoMirrored.Outlined.QueueMusic,

                            contentDescription = null,

                            tint = DragonTheme.primary,

                            modifier = Modifier.size(14.dp)

                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(

                            text = "频道流漫游 • ${playlist.size} 台",

                            color = DragonTheme.textSecondary,

                            fontSize = 12.sp,

                            fontWeight = FontWeight.Medium

                        )

                    }

                }



                // Live Station Playlist LazyColumn

                LazyColumn(

                    state = listState,

                    verticalArrangement = Arrangement.spacedBy(6.dp),

                    modifier = Modifier

                        .fillMaxWidth()

                        .weight(1f)

                ) {

                    itemsIndexed(playlist) { index, station ->

                        val isItemCurrent = index == currentIndex

                        val isItemPlaying = isItemCurrent && isPlaying



                        Surface(

                            onClick = { onSelectStation(index) },

                            shape = RoundedCornerShape(12.dp),

                            color = if (isItemCurrent) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.surface.copy(alpha = 0.60f),

                            border = BorderStroke(

                                if (isItemCurrent) 1.dp else 0.5.dp,

                                if (isItemCurrent) DragonTheme.primary.copy(alpha = 0.70f) else DragonTheme.glassBorder

                            ),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                // Station Favicon

                                Box(

                                    modifier = Modifier

                                        .size(38.dp)

                                        .clip(RoundedCornerShape(8.dp))

                                        .background(DragonTheme.surfaceVariant),

                                    contentAlignment = Alignment.Center

                                ) {

                                    if (!station.favicon.isNullOrBlank()) {

                                        AsyncImage(

                                            model = station.favicon,

                                            contentDescription = station.name,

                                            contentScale = ContentScale.Crop,

                                            modifier = Modifier.fillMaxSize()

                                        )

                                    } else {

                                        Icon(

                                            imageVector = Icons.Default.Radio,

                                            contentDescription = "Station",

                                            tint = DragonCyan,

                                            modifier = Modifier.size(18.dp)

                                        )

                                    }

                                }



                                Spacer(modifier = Modifier.width(10.dp))



                                // Station Name & Details

                                Column(modifier = Modifier.weight(1f)) {

                                    Text(

                                        text = station.name,

                                        color = if (isItemCurrent) DragonTheme.primary else DragonTheme.textPrimary,

                                        fontSize = 13.5.sp,

                                        fontWeight = if (isItemCurrent) FontWeight.Bold else FontWeight.Medium,

                                        maxLines = 1,

                                        overflow = TextOverflow.Ellipsis

                                    )

                                    Spacer(modifier = Modifier.height(1.5.dp))

                                    val region = station.state?.takeIf { it.isNotBlank() } ?: station.country

                                    Text(

                                        text = if (station.tags.isNotBlank()) "$region · ${station.tags.take(15)}" else region,

                                        color = DragonTheme.textSecondary,

                                        fontSize = 11.sp,

                                        maxLines = 1,

                                        overflow = TextOverflow.Ellipsis

                                    )

                                }



                                // Right status: Equalizer wave

                                if (isItemCurrent) {

                                    AudioEqualizerWave(

                                        isPlaying = isItemPlaying,

                                        color = DragonTheme.primary,

                                        modifier = Modifier.height(18.dp)

                                    )

                                }

                            }

                        }

                    }

                }

            }

        }



        // Double-tap heart pop up overlay

        AnimatedVisibility(

            visible = showHeartPop,

            enter = scaleIn() + fadeIn(),

            exit = scaleOut() + fadeOut(),

            modifier = Modifier.align(Alignment.Center)

        ) {

            Icon(

                imageVector = Icons.Default.Favorite,

                contentDescription = "Favorite Heart",

                tint = LiveRed,

                modifier = Modifier.size(96.dp)

            )

        }

    }

}



/**

 * Smartphone Portrait TikTok / Soda Music Single Screen Vertical Swipe Flow

 */

@OptIn(ExperimentalFoundationApi::class)

@Composable

private fun PortraitFlowContent(

    playlist: List<RadioStation>,

    currentIndex: Int,

    playerState: com.dragon.fm.data.model.PlayerState,

    favoriteIds: Set<String>,

    isRecording: Boolean,

    recordingDurationSec: Int,

    sleepTimerSec: Int?,

    selectedScenario: String,

    latencies: Map<String, Long>,

    isSpeedTesting: Boolean = false,

    animatedAmbientColor: Color,

    stationAmbientColors: MutableMap<String, Color>,

    isDark: Boolean,

    isDefaultBg: Boolean = true,

    recognitionState: com.dragon.fm.data.model.RecognitionState = com.dragon.fm.data.model.RecognitionState.Idle,

    fontScale: Float = 1.0f,

    onReorderPlaylist: (List<RadioStation>) -> Unit = {},

    onSelectStation: (Int) -> Unit,

    onTogglePlayPause: () -> Unit,

    onPrevious: () -> Unit = {},

    onNext: () -> Unit = {},

    onToggleFavorite: (RadioStation) -> Unit,

    onToggleRecording: () -> Unit,

    onOpenRecognition: () -> Unit = {},

    onToggleSleepMode: () -> Unit,

    onRunSpeedTest: () -> Unit,

    onOpenCarMode: () -> Unit,

    onOpenStudio: () -> Unit,

    onOpenRadar: () -> Unit,

    onOpenLibrary: () -> Unit,

    onOpenTag: (String) -> Unit,

    onOpenQuitDialog: () -> Unit = {},

    onRetryPlayback: () -> Unit = {},

    onSwitchLine: (Int) -> Unit = {},

    onSortByLatency: () -> Unit = {},

    onRemoveStation: (String) -> Unit = {},

    onMoveStation: (Int, Int) -> Unit = { _, _ -> },

    onAddToScenario: (RadioStation) -> Unit = {}

) {

    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(

        initialPage = currentIndex,

        pageCount = { playlist.size }

    )



    var showHeartPop by remember { mutableStateOf(false) }

    var showPlaylistSheet by remember { mutableStateOf(false) }



    val currentStationIndex by rememberUpdatedState(currentIndex)

    val currentPlaylist by rememberUpdatedState(playlist)

    val currentOnSelectStation by rememberUpdatedState(onSelectStation)



    // Synchronize pager swipe with playback (ONLY on settledPage to avoid intermediate animation loops)

    LaunchedEffect(pagerState) {

        snapshotFlow { pagerState.settledPage }.collect { page ->

            if (page != currentStationIndex && page in currentPlaylist.indices) {

                currentOnSelectStation(page)

            }

        }

    }



    // Synchronize programmatic index changes with pager smoothly

    LaunchedEffect(currentIndex) {

        if (currentIndex in playlist.indices &&

            (pagerState.currentPage != currentIndex || pagerState.targetPage != currentIndex)

        ) {

            if (kotlin.math.abs(pagerState.currentPage - currentIndex) > 1) {

                pagerState.scrollToPage(currentIndex)

            } else {

                pagerState.animateScrollToPage(currentIndex)

            }

        }

    }



    // When playlist changes (e.g. mode / scenario switch), instantly sync pager to target index

    LaunchedEffect(playlist) {

        if (currentIndex in playlist.indices && pagerState.currentPage != currentIndex) {

            pagerState.scrollToPage(currentIndex)

        }

    }



    Box(

        modifier = Modifier

            .fillMaxSize()

            .then(
                if (isDefaultBg) Modifier.background(DragonTheme.background)
                else Modifier.background(Color.Transparent)
            )
            .pointerInput(Unit) {
                var dragSum = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragSum > 80f) {
                            onOpenStudio()
                        }
                        dragSum = 0f
                    },
                    onDragCancel = { dragSum = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 0) dragSum += dragAmount
                    }
                )
            }

    ) {

        // Dynamic Ambient Radial Lighting

        Box(

            modifier = Modifier

                .fillMaxSize()

                .drawBehind {

                    drawCircle(

                        brush = Brush.radialGradient(

                            colors = listOf(

                                animatedAmbientColor.copy(alpha = if (isDark) 0.38f else 0.22f),

                                animatedAmbientColor.copy(alpha = if (isDark) 0.12f else 0.06f),

                                Color.Transparent

                            ),

                            center = Offset(size.width / 2f, size.height * 0.28f),

                            radius = size.width * 0.90f

                        )

                    )

                }

        )



        // Vertical ambient wash extending color downwards smoothly

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .height(420.dp)

                .background(

                    Brush.verticalGradient(

                        colors = listOf(

                            animatedAmbientColor.copy(alpha = if (isDark) 0.28f else 0.16f),

                            animatedAmbientColor.copy(alpha = if (isDark) 0.08f else 0.04f),

                            Color.Transparent

                        )

                    )

                )

        )



        // Vertical Pager (TikTok / Soda Music style swipe-to-switch stations)

        VerticalPager(

            state = pagerState,

            modifier = Modifier.fillMaxSize()

        ) { page ->

            val station = playlist.getOrNull(page)

            if (station != null) {

                val isCurrent = page == currentIndex

                val isPlaying = isCurrent && playerState.status == PlaybackStatus.PLAYING

                val isBuffering = isCurrent && playerState.status == PlaybackStatus.BUFFERING

                val latency = latencies[station.id]



                val defaultHarmonic = remember(station.id, isDark) {

                    StationColorExtractor.getHarmonicColor(station, isDark)

                }

                val stationAmbient = stationAmbientColors[station.id] ?: defaultHarmonic



                Box(

                    modifier = Modifier.fillMaxSize()

                ) {

                    // Atmospheric Blurred Backdrop (Lightweight 64px downsampled thumbnail for 60fps+ swipe)

                    if (!station.favicon.isNullOrBlank()) {

                        val context = LocalContext.current

                        val backdropReq = remember(station.favicon) {

                            coil.request.ImageRequest.Builder(context)

                                .data(station.favicon)

                                .size(64, 64)

                                .crossfade(false)

                                .build()

                        }

                        AsyncImage(

                            model = backdropReq,

                            contentDescription = null,

                            contentScale = ContentScale.Crop,

                            modifier = Modifier

                                .fillMaxSize()

                                .blur(20.dp)

                                .alpha(if (isDark) 0.25f else 0.15f)

                        )

                    }



                    // Dynamic Station Ambient Glow Mesh (Radiates the station's harmonic color)

                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .drawBehind {

                                // Primary upper halo centered behind album art

                                drawCircle(

                                    brush = Brush.radialGradient(

                                        colors = listOf(

                                            stationAmbient.copy(alpha = if (isDark) 0.52f else 0.35f),

                                            stationAmbient.copy(alpha = if (isDark) 0.18f else 0.10f),

                                            Color.Transparent

                                        ),

                                        center = Offset(size.width * 0.50f, size.height * 0.25f),

                                        radius = size.width * 0.92f

                                    )

                                )

                                // Secondary accent wash on upper-right

                                drawCircle(

                                    brush = Brush.radialGradient(

                                        colors = listOf(

                                            stationAmbient.copy(alpha = if (isDark) 0.26f else 0.15f),

                                            Color.Transparent

                                        ),

                                        center = Offset(size.width * 0.85f, size.height * 0.10f),

                                        radius = size.width * 0.55f

                                    )

                                )

                            }

                    )



                    // Scrim Gradient (Keep top translucent so ambient glow shines through)

                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .background(

                                Brush.verticalGradient(

                                    colors = listOf(

                                        Color.Transparent,

                                        DragonTheme.background.copy(alpha = if (isDark) 0.38f else 0.25f),

                                        DragonTheme.background.copy(alpha = if (isDark) 0.88f else 0.78f),

                                        DragonTheme.background

                                    )

                                )

                            )

                    )



                    val configuration = LocalConfiguration.current

                    val screenHeight = configuration.screenHeightDp.dp

                    val screenWidth = configuration.screenWidthDp.dp

                    val isTallScreen = screenHeight >= 760.dp

                    val coverRatio = if (isTallScreen) 0.38f else 0.33f

                    val maxCoverByHeight = (screenHeight - 380.dp).coerceIn(150.dp, screenHeight * coverRatio)

                    val maxCoverByWidth = (screenWidth - 40.dp).coerceAtLeast(140.dp)

                    val coverDimension = minOf(maxCoverByHeight, maxCoverByWidth, 310.dp)



                    Column(

                        modifier = Modifier

                            .fillMaxSize()

                            .padding(horizontal = 20.dp)

                            .statusBarsPadding()

                            .padding(

                                top = if (isTallScreen) 58.dp else 48.dp,

                                bottom = if (isTallScreen) 112.dp else 98.dp

                            ),

                        horizontalAlignment = Alignment.CenterHorizontally,

                        verticalArrangement = Arrangement.SpaceBetween

                    ) {

                        // ===============================================

                        // 1. TOP HERO GROUP: Album Art + Station Title & Tags (Tight & cohesive)

                        // ===============================================

                        Column(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalAlignment = Alignment.Start

                        ) {

                            // Center Large Album Art (Soda Music wide squircle)

                            Box(

                                contentAlignment = Alignment.Center,

                                modifier = Modifier

                                    .size(coverDimension)

                                    .align(Alignment.CenterHorizontally)

                            ) {

                                // Ambient Halo Aura (Luminous pulse extending beyond cover boundaries)

                                val haloPulse by if (isPlaying) {

                                    val haloTransition = rememberInfiniteTransition(label = "halo")

                                    haloTransition.animateFloat(

                                        initialValue = 0.85f,

                                        targetValue = 1.0f,

                                        animationSpec = infiniteRepeatable(

                                            animation = tween(2200, easing = FastOutSlowInEasing),

                                            repeatMode = RepeatMode.Reverse

                                        ),

                                        label = "pulse"

                                    )

                                } else {

                                    remember { mutableStateOf(0.5f) }

                                }



                                Box(

                                    modifier = Modifier

                                        .size(coverDimension + 40.dp)

                                        .graphicsLayer {

                                            alpha = if (isPlaying) haloPulse else 0.40f

                                        }

                                        .drawBehind {

                                            drawRoundRect(

                                                brush = Brush.radialGradient(

                                                    colors = listOf(

                                                        stationAmbient.copy(alpha = if (isDark) 0.52f else 0.38f),

                                                        stationAmbient.copy(alpha = if (isDark) 0.22f else 0.14f),

                                                        Color.Transparent

                                                    ),

                                                    radius = size.width * 0.65f

                                                ),

                                                cornerRadius = CornerRadius(36.dp.toPx(), 36.dp.toPx())

                                            )

                                        }

                                )



                                Box(

                                    modifier = Modifier

                                        .fillMaxSize()

                                        .shadow(

                                            elevation = 18.dp,

                                            shape = RoundedCornerShape(16.dp),

                                            ambientColor = stationAmbient.copy(alpha = 0.50f),

                                            spotColor = Color.Black.copy(alpha = 0.65f)

                                        )

                                        .clip(RoundedCornerShape(16.dp))

                                        .background(DragonTheme.surface)

                                        .border(

                                            width = 0.5.dp,

                                            color = DragonTheme.glassBorder,

                                            shape = RoundedCornerShape(16.dp)

                                        )

                                        .pointerInput(station.id) {

                                            detectTapGestures(

                                                onDoubleTap = {

                                                    onToggleFavorite(station)

                                                    showHeartPop = true

                                                    scope.launch {

                                                        delay(800)

                                                        showHeartPop = false

                                                    }

                                                }

                                            )

                                        },

                                    contentAlignment = Alignment.Center

                                ) {

                                    if (!station.favicon.isNullOrBlank()) {

                                        AsyncImage(

                                            model = station.favicon,

                                            contentDescription = station.name,

                                            contentScale = ContentScale.Crop,

                                            modifier = Modifier.fillMaxSize(),

                                            onSuccess = { state ->

                                                val color = StationColorExtractor.extractFromDrawable(

                                                    station.id,

                                                    state.result.drawable,

                                                    isDark

                                                )

                                                stationAmbientColors[station.id] = color

                                            }

                                        )

                                    } else {

                                        Box(

                                            modifier = Modifier

                                                .fillMaxSize()

                                                .background(

                                                    Brush.radialGradient(

                                                        colors = listOf(

                                                            stationAmbient.copy(alpha = 0.40f),

                                                            DragonTheme.surface

                                                        )

                                                    )

                                                ),

                                            contentAlignment = Alignment.Center

                                        ) {

                                            Icon(

                                                painter = painterResource(id = R.drawable.app_icon),

                                                contentDescription = "App Icon",

                                                tint = Color.Unspecified,

                                                modifier = Modifier.size(140.dp)

                                            )

                                        }

                                    }

                                }



                                }



                            Spacer(modifier = Modifier.height(if (isTallScreen) 16.dp else 10.dp))



                            // Actual playing song (ONLY displayed if genuine broadcast metadata exists, NO 24-hour filler!)

                            val actualPlaying = playerState.nowPlayingTitle.takeIf {

                                it.isNotBlank() &&

                                !it.equals(station.name, ignoreCase = true) &&

                                !it.contains("正在载入", ignoreCase = true)

                            } ?: station.currentProgram?.let { "正在直播：$it" }

                            if (actualPlaying != null) {

                                Text(

                                    text = actualPlaying,

                                    color = DragonTheme.primary,

                                    fontSize = 13.5.sp,

                                    fontWeight = FontWeight.Medium,

                                    maxLines = 1,

                                    overflow = TextOverflow.Ellipsis

                                )

                                Spacer(modifier = Modifier.height(4.dp))

                            }



                            // Main Station Title

                            Text(

                                text = station.name,

                                color = DragonTheme.textPrimary,

                                fontSize = 23.sp,

                                fontWeight = FontWeight.Bold,

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis

                            )



                            Spacer(modifier = Modifier.height(if (isTallScreen) 10.dp else 8.dp))



                            // Live Status Pulse Pill + Region & Chinese Tags (only animate when actively playing)

                            val pulseAlpha by if (isPlaying) {

                                val infiniteTransition = rememberInfiniteTransition(label = "livePulse")

                                infiniteTransition.animateFloat(

                                    initialValue = 1f,

                                    targetValue = 0.35f,

                                    animationSpec = infiniteRepeatable(

                                        animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),

                                        repeatMode = RepeatMode.Reverse

                                    ),

                                    label = "livePulseAlpha"

                                )

                            } else {

                                remember { mutableStateOf(1f) }

                            }



                            val isError = isCurrent && playerState.status == PlaybackStatus.ERROR



                            // 1. LIVE Status Breathing Capsule (Independent row between title and tags)

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                if (isError) {

                                    Surface(

                                        onClick = onRetryPlayback,

                                        shape = RoundedCornerShape(8.dp),

                                        color = LiveRed.copy(alpha = 0.16f),

                                        border = BorderStroke(0.5.dp, LiveRed.copy(alpha = 0.5f))

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Icon(

                                                imageVector = Icons.Default.Refresh,

                                                contentDescription = "Retry",

                                                tint = LiveRed,

                                                modifier = Modifier.size(13.dp)

                                            )

                                            Spacer(modifier = Modifier.width(4.dp))

                                            Text(

                                                text = "连接受阻 • 点击重试",

                                                color = LiveRed,

                                                fontSize = 11.sp,

                                                fontWeight = FontWeight.SemiBold

                                            )

                                        }

                                    }

                                } else {

                                    Surface(

                                        shape = RoundedCornerShape(8.dp),

                                        color = if (isPlaying) LiveRed.copy(alpha = 0.14f) else DragonTheme.glassSurface,

                                        border = BorderStroke(0.5.dp, if (isPlaying) LiveRed.copy(alpha = 0.40f) else DragonTheme.glassBorder)

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Box(

                                                modifier = Modifier

                                                    .size(6.dp)

                                                    .clip(CircleShape)

                                                    .graphicsLayer {

                                                        alpha = if (isPlaying) pulseAlpha else 1f

                                                    }

                                                    .background(if (isPlaying) LiveRed else Color.Gray)

                                            )

                                            Spacer(modifier = Modifier.width(5.dp))

                                            Text(

                                                text = if (isBuffering) "正在缓冲" else "LIVE 直播",

                                                color = if (isPlaying) LiveRed else DragonTheme.textSecondary,

                                                maxLines = 1,

                                                softWrap = false,

                                                fontSize = 11.sp,

                                                fontWeight = FontWeight.SemiBold

                                            )

                                        }

                                    }

                                }

                                if (station.hasDedicatedLine || station.effectiveLines.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val lines = station.effectiveLines
                                    val isCur = (page == currentIndex && playerState.currentStation?.id == station.id)
                                    val activeIdx = if (isCur) (playerState.currentStation?.activeLineIndex ?: station.activeLineIndex) else station.activeLineIndex
                                    val lineName = if (isCur && playerState.activeLineName != null) playerState.activeLineName!! else (lines.getOrNull(activeIdx)?.name ?: (if (station.hasDedicatedLine) "高品质专线" else "默认主线"))
                                    Surface(
                                        onClick = {
                                            if (lines.size > 1) {
                                                onSwitchLine((activeIdx + 1) % lines.size)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bolt,
                                                contentDescription = null,
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (lines.size > 1) "$lineName ▾" else lineName,
                                                color = Color(0xFF00E5FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(

                                    onClick = { showPlaylistSheet = true },

                                    shape = RoundedCornerShape(8.dp),

                                    color = DragonTheme.glassSurface,

                                    border = BorderStroke(0.5.dp, DragonTheme.glassBorder)

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Icon(

                                            imageVector = Icons.AutoMirrored.Outlined.QueueMusic,

                                            contentDescription = "Queue",

                                            tint = DragonCyan,

                                            modifier = Modifier.size(12.dp)

                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(

                                            text = "列表 • ${page + 1}/${playlist.size}", maxLines = 1, softWrap = false,

                                            color = DragonTheme.textPrimary,

                                            fontSize = 11.sp,

                                            fontWeight = FontWeight.Medium

                                        )

                                    }

                                }

                            }



                            Spacer(modifier = Modifier.height(if (isTallScreen) 8.dp else 6.dp))



                            // 2. Region & Aligned Category Tags Row

                            val allTags = remember(station.id) { ChineseTagHelper.extractChineseTags(station) }

                            val regionTag = remember(station.id) { allTags.firstOrNull { it.isRegion } }

                            val regionStr = regionTag?.name ?: station.formattedRegion

                            val categoryTags = remember(station.id) { allTags.filter { !it.isRegion } }

                            val regionQuery = regionTag?.searchQuery ?: regionStr



                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                if (regionStr.isNotBlank()) {

                                    Surface(

                                        onClick = { onOpenTag(regionQuery) },

                                        shape = RoundedCornerShape(8.dp),

                                        color = DragonTheme.primary.copy(alpha = 0.14f),

                                        border = BorderStroke(0.5.dp, DragonTheme.primary.copy(alpha = 0.40f))

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Icon(

                                                imageVector = Icons.Outlined.LocationOn,

                                                contentDescription = null,

                                                tint = DragonTheme.primary,

                                                modifier = Modifier.size(11.dp)

                                            )

                                            Spacer(modifier = Modifier.width(3.5.dp))

                                            Text(

                                                text = regionStr,

                                                color = DragonTheme.primary,

                                                fontSize = 11.5.sp,

                                                fontWeight = FontWeight.Medium

                                            )

                                        }

                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                }



                                LazyRow(

                                    horizontalArrangement = Arrangement.spacedBy(6.dp),

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    items(categoryTags.take(4)) { tag ->

                                        Surface(

                                            onClick = { onOpenTag(tag.searchQuery) },

                                            shape = RoundedCornerShape(8.dp),

                                            color = DragonTheme.glassSurface,

                                            border = BorderStroke(0.5.dp, DragonTheme.glassBorder)

                                        ) {

                                            Text(

                                                text = tag.name,

                                                color = DragonTheme.textPrimary.copy(alpha = 0.85f),

                                                fontSize = 11.sp,

                                                fontWeight = FontWeight.Normal,

                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)

                                            )

                                        }

                                    }

                                }

                            }

                        }



                        // ===============================================

                        // 2. BOTTOM ACTION & STREAM BAR GROUP (Tight & unified)

                        // ===============================================

                        Column(

                            modifier = Modifier.fillMaxWidth(),

                            verticalArrangement = Arrangement.spacedBy(if (isTallScreen) 12.dp else 8.dp)

                        ) {

                            // Interactive Actions Row (Soda Music style 4 core buttons: Heart, Record, Sleep, Car)

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(horizontal = 6.dp, vertical = if (isTallScreen) 4.dp else 0.dp),

                                horizontalArrangement = Arrangement.SpaceAround,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                val isFav = favoriteIds.contains(station.id)

                                val voteDisplay = if (station.votes > 1000) "${station.votes / 1000}k" else "${station.votes}"



                                // 1) Favorite Heart

                                Column(

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(12.dp))

                                        .combinedClickable(

                                            onClick = { onToggleFavorite(station) },

                                            onLongClick = { onAddToScenario(station) }

                                        )

                                        .padding(horizontal = 10.dp, vertical = 4.dp)

                                ) {

                                    Icon(

                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,

                                        contentDescription = "Favorite",

                                        tint = if (isFav) LiveRed else DragonTheme.textSecondary,

                                        modifier = Modifier.size(26.dp)

                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(

                                        text = voteDisplay,

                                        color = DragonTheme.textSecondary,

                                        fontSize = 11.sp

                                    )

                                }



                                // 2) Recording Button

                                Column(

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(12.dp))

                                        .clickable { onToggleRecording() }

                                        .padding(horizontal = 10.dp, vertical = 4.dp)

                                ) {

                                    Icon(

                                        imageVector = Icons.Default.FiberManualRecord,

                                        contentDescription = "Record",

                                        tint = if (isRecording && isCurrent) LiveRed else DragonTheme.textSecondary,

                                        modifier = Modifier.size(26.dp)

                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(

                                        text = if (isRecording && isCurrent) "${recordingDurationSec}s" else "录音",

                                        color = if (isRecording && isCurrent) LiveRed else DragonTheme.textSecondary,

                                        fontSize = 11.sp

                                    )

                                }



                                // 2.5) Recognition Button (In-place dynamic listening status)

                                val isRecognizing = recognitionState is com.dragon.fm.data.model.RecognitionState.Sampling ||

                                                    recognitionState is com.dragon.fm.data.model.RecognitionState.Fingerprinting ||

                                                    recognitionState is com.dragon.fm.data.model.RecognitionState.QueryingCloud

                                val recogRotation by if (isRecognizing) {

                                    val transition = rememberInfiniteTransition(label = "recog_spin_port")

                                    transition.animateFloat(

                                        initialValue = 0f,

                                        targetValue = 360f,

                                        animationSpec = infiniteRepeatable(

                                            animation = tween(1200, easing = LinearEasing),

                                            repeatMode = RepeatMode.Restart

                                        ),

                                        label = "spin_port"

                                    )

                                } else {

                                    remember { mutableStateOf(0f) }

                                }



                                Column(

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(12.dp))

                                        .clickable { onOpenRecognition() }

                                        .padding(horizontal = 10.dp, vertical = 4.dp)

                                ) {

                                    Icon(

                                        imageVector = Icons.Default.GraphicEq,

                                        contentDescription = "Recognize",

                                        tint = if (isRecognizing) DragonCyan else DragonTheme.primary,

                                        modifier = Modifier

                                            .size(26.dp)

                                            .graphicsLayer { rotationZ = recogRotation }

                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(

                                        text = if (isRecognizing) "识别中" else "识曲",

                                        color = if (isRecognizing) DragonCyan else DragonTheme.primary,

                                        fontSize = 11.sp,

                                        fontWeight = FontWeight.Bold

                                    )

                                }



                                // 3) Sleep Mode Button

                                Column(

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(12.dp))

                                        .clickable { onToggleSleepMode() }

                                        .padding(horizontal = 10.dp, vertical = 4.dp)

                                ) {

                                    Icon(

                                        imageVector = Icons.Default.NightlightRound,

                                        contentDescription = "Sleep",

                                        tint = if (sleepTimerSec != null) DragonGold else DragonTheme.textSecondary,

                                        modifier = Modifier.size(26.dp)

                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(

                                        text = if (sleepTimerSec != null) "${sleepTimerSec / 60}分" else "睡眠",

                                        color = if (sleepTimerSec != null) DragonGold else DragonTheme.textSecondary,

                                        fontSize = 11.sp

                                    )

                                }



                                // 4) Car HUD Mode

                                Column(

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(12.dp))

                                        .clickable { onOpenCarMode() }

                                        .padding(horizontal = 10.dp, vertical = 4.dp)

                                ) {

                                    Icon(

                                        imageVector = Icons.Default.DirectionsCar,

                                        contentDescription = "Car Mode",

                                        tint = DragonTheme.textSecondary,

                                        modifier = Modifier.size(26.dp)

                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(

                                        text = "车载",

                                        color = DragonTheme.textSecondary,

                                        fontSize = 11.sp

                                    )

                                }



                                // 5) Playlist Queue Button

                                Column(

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(12.dp))

                                        .clickable { showPlaylistSheet = true }

                                        .padding(horizontal = 8.dp, vertical = 4.dp)

                                ) {

                                    Icon(

                                        imageVector = Icons.AutoMirrored.Outlined.QueueMusic,

                                        contentDescription = "Playlist",

                                        tint = DragonCyan,

                                        modifier = Modifier.size(26.dp)

                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(

                                        text = "列表",

                                        color = DragonCyan,

                                        fontSize = 11.sp,

                                        fontWeight = FontWeight.Medium

                                    )

                                }

                            }





                        }

                    }

                }

            }

        }



        // Top Navigation Bar (≡ 模式选择 on left, 🔍 Search on right)

        Column(

            modifier = Modifier

                .align(Alignment.TopCenter)

                .fillMaxWidth()

                .statusBarsPadding()

        ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp, vertical = 6.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically

            ) {

                val cleanScenario = selectedScenario.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()

                Row(

                    modifier = Modifier

                        .clip(RoundedCornerShape(12.dp))

                        .background(DragonTheme.glassSurface)

                        .border(1.dp, DragonTheme.glassBorder, RoundedCornerShape(12.dp))

                        .clickable { onOpenStudio() }

                        .padding(horizontal = 10.dp, vertical = 6.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Icon(

                        imageVector = Icons.Outlined.Tune,

                        contentDescription = "Mode Menu",

                        tint = DragonTheme.primary,

                        modifier = Modifier.size(18.dp)

                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(

                        text = cleanScenario,

                        color = DragonTheme.textPrimary,

                        fontSize = 15.sp,

                        fontWeight = FontWeight.Bold

                    )

                }



                Surface(

                    shape = RoundedCornerShape(12.dp),

                    color = DragonTheme.glassSurface,

                    border = BorderStroke(1.dp, DragonTheme.glassBorder)

                ) {

                    Row(

                        verticalAlignment = Alignment.CenterVertically,

                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)

                    ) {

                        Box(

                            modifier = Modifier

                                .size(32.dp)

                                .clip(RoundedCornerShape(9.dp))

                                .clickable { onOpenRadar() },

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(

                                imageVector = Icons.Outlined.Search,

                                contentDescription = "Search",

                                tint = DragonTheme.textPrimary,

                                modifier = Modifier.size(17.dp)

                            )

                        }

                        Box(

                            modifier = Modifier

                                .width(1.dp)

                                .height(14.dp)

                                .background(DragonTheme.glassBorder)

                        )

                        Box(

                            modifier = Modifier

                                .size(32.dp)

                                .clip(RoundedCornerShape(9.dp))

                                .clickable { onOpenQuitDialog() },

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(

                                imageVector = Icons.Outlined.PowerSettingsNew,

                                contentDescription = "Quit",

                                tint = LiveRed.copy(alpha = 0.88f),

                                modifier = Modifier.size(17.dp)

                            )

                        }

                    }

                }

            }

        }



        // Bottom Navigation Bar (🎧 发现 | ⚪ 播放键 | 👤 我的)

        Box(

            modifier = Modifier

                .align(Alignment.BottomCenter)

                .fillMaxWidth()

                .navigationBarsPadding()

                .padding(horizontal = 24.dp, vertical = 18.dp)

        ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .clip(RoundedCornerShape(999.dp))

                    .background(if (DragonTheme.isDark) Color(0xF2090B10) else DragonTheme.surface.copy(alpha = 0.94f))

                    .border(

                        1.dp,

                        DragonTheme.glassBorder,

                        RoundedCornerShape(999.dp)

                    )

                    .padding(horizontal = 14.dp, vertical = 6.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically

            ) {

                // 1) 发现 Tab

                Column(

                    horizontalAlignment = Alignment.CenterHorizontally,

                    modifier = Modifier

                        .clip(RoundedCornerShape(16.dp))

                        .clickable { onOpenRadar() }

                        .padding(horizontal = 10.dp, vertical = 6.dp)

                ) {

                    Icon(

                        imageVector = Icons.Outlined.TravelExplore,

                        contentDescription = "Discover",

                        tint = DragonTheme.primary,

                        modifier = Modifier.size(20.dp)

                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(

                        text = "发现",

                        color = DragonTheme.primary,

                        fontSize = 11.sp,

                        fontWeight = FontWeight.Bold

                    )

                }



                // 2) 切上一台

                IconButton(

                    onClick = onPrevious,

                    enabled = playlist.size > 1,

                    modifier = Modifier.size(36.dp)

                ) {

                    Icon(

                        imageVector = Icons.Default.SkipPrevious,

                        contentDescription = "Previous",

                        tint = if (playlist.size > 1) DragonTheme.textPrimary else DragonTheme.textSecondary.copy(alpha = 0.35f),

                        modifier = Modifier.size(22.dp)

                    )

                }



                // 3) 居中核心播放/暂停键

                Box(

                    modifier = Modifier

                        .size(54.dp)

                        .shadow(12.dp, CircleShape, ambientColor = DragonTheme.primary.copy(alpha = 0.35f))

                        .clip(CircleShape)

                        .background(

                            Brush.linearGradient(listOf(DragonTheme.primary, DragonTheme.secondary))

                        ),

                    contentAlignment = Alignment.Center

                ) {

                    IconButton(

                        onClick = onTogglePlayPause,

                        modifier = Modifier.fillMaxSize()

                    ) {

                        val iconColor = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White

                        when (playerState.status) {

                            PlaybackStatus.BUFFERING -> {

                                CircularProgressIndicator(

                                    modifier = Modifier.size(24.dp),

                                    color = iconColor,

                                    strokeWidth = 3.dp

                                )

                            }

                            PlaybackStatus.PLAYING -> {

                                Icon(

                                    imageVector = Icons.Default.Pause,

                                    contentDescription = "Pause",

                                    tint = iconColor,

                                    modifier = Modifier.size(28.dp)

                                )

                            }

                            else -> {

                                Icon(

                                    imageVector = Icons.Default.PlayArrow,

                                    contentDescription = "Play",

                                    tint = iconColor,

                                    modifier = Modifier.size(28.dp)

                                )

                            }

                        }

                    }

                }



                // 4) 切下一台

                IconButton(

                    onClick = onNext,

                    enabled = playlist.size > 1,

                    modifier = Modifier.size(36.dp)

                ) {

                    Icon(

                        imageVector = Icons.Default.SkipNext,

                        contentDescription = "Next",

                        tint = if (playlist.size > 1) DragonTheme.textPrimary else DragonTheme.textSecondary.copy(alpha = 0.35f),

                        modifier = Modifier.size(22.dp)

                    )

                }



                // 5) 我的 Tab

                Column(

                    horizontalAlignment = Alignment.CenterHorizontally,

                    modifier = Modifier

                        .clip(RoundedCornerShape(16.dp))

                        .clickable { onOpenLibrary() }

                        .padding(horizontal = 10.dp, vertical = 6.dp)

                ) {

                    Icon(

                        imageVector = Icons.Outlined.BookmarkBorder,

                        contentDescription = "My Library",

                        tint = DragonTheme.textSecondary,

                        modifier = Modifier.size(20.dp)

                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(

                        text = "我的",

                        color = DragonTheme.textSecondary,

                        fontSize = 11.sp,

                        fontWeight = FontWeight.Medium

                    )

                }

            }

        }



        // Double-tap heart pop up overlay

        AnimatedVisibility(

            visible = showHeartPop,

            enter = scaleIn() + fadeIn(),

            exit = scaleOut() + fadeOut(),

            modifier = Modifier.align(Alignment.Center)

        ) {

            Column(

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center,

                modifier = Modifier

                    .clip(RoundedCornerShape(24.dp))

                    .background(Color.Black.copy(alpha = 0.65f))

                    .padding(horizontal = 24.dp, vertical = 18.dp)

            ) {

                Icon(

                    imageVector = Icons.Default.Favorite,

                    contentDescription = "Favorite Heart",

                    tint = LiveRed,

                    modifier = Modifier.size(76.dp)

                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(

                    text = "已加入收藏",

                    color = Color.White,

                    fontSize = 14.sp,

                    fontWeight = FontWeight.SemiBold

                )

            }

        }



        if (showPlaylistSheet) {

            PlaylistDrawerSheet(

                playlist = playlist,

                currentIndex = currentIndex,

                isPlaying = playerState.status == PlaybackStatus.PLAYING,

                selectedScenario = selectedScenario,

                latencies = latencies,

                isSpeedTesting = isSpeedTesting,

                fontScale = fontScale,

                onSelectStation = { idx ->

                    onSelectStation(idx)

                    scope.launch {

                        pagerState.scrollToPage(idx)

                    }

                },

                onRunSpeedTest = onRunSpeedTest,

                onSortByLatency = onSortByLatency,

                onRemoveStation = onRemoveStation,

                onMoveStation = onMoveStation,

                onReorderPlaylist = onReorderPlaylist,

                onAddToScenario = onAddToScenario,

                onDismiss = { showPlaylistSheet = false }

            )

        }

    }

}

