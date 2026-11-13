package com.dragon.fm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.dragon.fm.data.model.AppBackgroundType
import com.dragon.fm.data.model.BackgroundPresets
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.UpdateCheckResult
import com.dragon.fm.ui.components.AppUpdateDialog
import com.dragon.fm.ui.components.MusicRecognitionDialog
import com.dragon.fm.ui.screens.CarModeDialog
import com.dragon.fm.ui.screens.DragonFlowScreen
import com.dragon.fm.ui.screens.LibraryScreen
import com.dragon.fm.ui.screens.RadarExploreScreen
import com.dragon.fm.ui.screens.SettingsScreen
import com.dragon.fm.ui.screens.SimpleHomeScreen
import com.dragon.fm.ui.screens.SleepModeOverlay
import com.dragon.fm.ui.screens.StudioDrawerSheet
import com.dragon.fm.ui.theme.DragonFMTheme
import com.dragon.fm.ui.theme.ThemePreset
import com.dragon.fm.ui.viewmodel.MainViewModel
import java.io.File

enum class ScreenRoute {
    FLOW,
    RADAR,
    LIBRARY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(
            this,
            if (granted) {
                "已授权蓝牙设备识别，重连续播可靠性已提升"
            } else {
                "未授权蓝牙设备识别，锁屏重连续播可能受限"
            },
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val stationId = intent?.getStringExtra(EXTRA_PLAY_STATION_ID) ?: return
        intent.removeExtra(EXTRA_PLAY_STATION_ID)
        viewModel.playStationById(stationId)
    }

    companion object {
        const val EXTRA_PLAY_STATION_ID = "extra_play_station_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissionIfNeeded()

        // 桌面快捷方式：直接播放对应电台
        handleShortcutIntent(intent)

        // Enable edge-to-edge full screen experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 极简版为浅色界面，强制状态栏/导航栏使用深色图标
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()
            val isFollowSystemTheme by viewModel.isFollowSystemTheme.collectAsState()
            val selectedLightThemeId by viewModel.selectedLightThemeId.collectAsState()
            val selectedDarkThemeId by viewModel.selectedDarkThemeId.collectAsState()
            val isSystemDark = isSystemInDarkTheme()

            val effectiveTheme = remember(currentTheme, isFollowSystemTheme, isSystemDark, selectedLightThemeId, selectedDarkThemeId) {
                if (isFollowSystemTheme) {
                    if (isSystemDark) {
                        ThemePreset.fromId(selectedDarkThemeId)
                    } else {
                        ThemePreset.fromId(selectedLightThemeId)
                    }
                } else {
                    currentTheme
                }
            }

            val appBackground by viewModel.appBackground.collectAsState()
            val fontScale by viewModel.fontScale.collectAsState()
            val currentDensity = androidx.compose.ui.platform.LocalDensity.current
            val customDensity = remember(currentDensity, fontScale) {
                androidx.compose.ui.unit.Density(
                    density = currentDensity.density,
                    fontScale = currentDensity.fontScale * fontScale
                )
            }

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides customDensity
            ) {
                DragonFMTheme(preset = effectiveTheme) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Layer: Default Solid, Selected Fluid Preset, or User Album Wallpaper
                        when (appBackground.type) {
                            AppBackgroundType.DEFAULT -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(effectiveTheme.background)
                                )
                            }
                            AppBackgroundType.PRESET -> {
                                val preset = remember(appBackground.presetId) {
                                    BackgroundPresets.getById(appBackground.presetId)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(preset.brush)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(effectiveTheme.background.copy(alpha = appBackground.overlayAlpha))
                                )
                            }
                            AppBackgroundType.CUSTOM -> {
                                val imagePath = appBackground.customImagePath
                                if (!imagePath.isNullOrBlank() && File(imagePath).exists()) {
                                    AsyncImage(
                                        model = File(imagePath),
                                        contentDescription = "Background Wallpaper",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(effectiveTheme.background.copy(alpha = appBackground.overlayAlpha))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(effectiveTheme.background)
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent
                        ) {
                            DragonAppContent(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun requestBluetoothPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val prefs = getSharedPreferences("dragon_fm_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("pref_pause_on_unplug", true)) return
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (prefs.getBoolean("pref_bluetooth_permission_prompted", false)) return
        prefs.edit().putBoolean("pref_bluetooth_permission_prompted", true).apply()
        window.decorView.post {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragonAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    var currentScreen by remember { mutableStateOf(ScreenRoute.FLOW) }
    var showStudioSheet by remember { mutableStateOf(false) }
    var showCarMode by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val playerState by viewModel.playerState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val sleepTimerSec by viewModel.sleepTimerSeconds.collectAsState()
    val selectedSleepMinutes by viewModel.selectedSleepMinutes.collectAsState()
    val isSleepModeActive by viewModel.isSleepModeActive.collectAsState()
    val selectedScenario by viewModel.selectedScenario.collectAsState()
    val customScenarios by viewModel.customScenarios.collectAsState()
    val deletedDefaultScenarios by viewModel.deletedDefaultScenarios.collectAsState()
    val favorites by viewModel.favoriteStations.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val scenarioOrder by viewModel.scenarioOrder.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val updateCheckResult by viewModel.updateCheckResult.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    // Robust Android system back button handler
    BackHandler(enabled = true) {
        when {
            isSleepModeActive -> {
                viewModel.toggleSleepMode()
            }
            showCarMode -> {
                showCarMode = false
            }
            showStudioSheet -> {
                showStudioSheet = false
            }
            currentScreen != ScreenRoute.FLOW -> {
                when (currentScreen) {
                    ScreenRoute.RADAR -> {
                        viewModel.clearRadarQuery()
                        currentScreen = ScreenRoute.FLOW
                    }
                    ScreenRoute.SETTINGS -> {
                        currentScreen = ScreenRoute.LIBRARY
                    }
                    else -> {
                        currentScreen = ScreenRoute.FLOW
                    }
                }
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(context, "再按一次返回桌面", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (isSleepModeActive) {
        // Bedtime Ultra-Dark Pure Sleep Mode
        SleepModeOverlay(
            playerState = playerState,
            sleepTimerSeconds = sleepTimerSec,
            selectedMinutes = selectedSleepMinutes,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSetTimer = { mins -> viewModel.setSleepTimer(mins) },
            onExit = { viewModel.toggleSleepMode() }
        )
    } else {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (fadeIn(animationSpec = tween(240)) + scaleIn(initialScale = 0.96f, animationSpec = tween(240)))
                    .togetherWith(fadeOut(animationSpec = tween(180)))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                ScreenRoute.FLOW -> {
                    // 极简纯净版首页：打开即播放列表，仅保留列表播放 / 添加电台 / 设置
                    SimpleHomeScreen(viewModel = viewModel)
                }
                ScreenRoute.RADAR -> {
                    RadarExploreScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenRoute.FLOW }
                    )
                }
                ScreenRoute.LIBRARY -> {
                    LibraryScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenRoute.FLOW },
                        onOpenSettings = { currentScreen = ScreenRoute.SETTINGS }
                    )
                }
                ScreenRoute.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenRoute.LIBRARY }
                    )
                }
            }
        }

        // Studio Bottom Sheet
        if (showStudioSheet) {
            StudioDrawerSheet(
                station = playerState.currentStation,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                sleepTimerMinutes = selectedSleepMinutes,
                selectedScenario = selectedScenario,
                customScenarios = customScenarios,
                deletedDefaultScenarios = deletedDefaultScenarios,
                scenarioOrder = scenarioOrder,
                favoriteCount = favorites.size,
                currentTheme = currentTheme,
                fontScale = fontScale,
                onDismiss = {
                    showStudioSheet = false
                },
                onSelectScenario = { scenario ->
                    viewModel.selectScenario(scenario)
                    showStudioSheet = false
                },
                onDeleteCustomScenario = { id ->
                    viewModel.deleteCustomScenario(id)
                },
                onDeleteDefaultScenario = { key ->
                    viewModel.deleteDefaultScenario(key)
                },
                onRestoreDefaultScenarios = {
                    viewModel.restoreDefaultScenarios()
                },
                onUpdateCustomScenario = { id, title, icon ->
                    viewModel.updateCustomScenario(id, title, icon)
                },
                onReorderCustomScenarios = { list ->
                    viewModel.reorderCustomScenarios(list)
                },
                onReorderAllScenarios = { list ->
                    viewModel.reorderAllScenarios(list)
                },
                onCreateCustomScenario = { title, icon ->
                    viewModel.createCustomScenario(title, icon, emptyList())
                },
                onTriggerSpeedSort = {
                    viewModel.sortPlaylistByLatency()
                },
                onSelectTheme = { theme ->
                    viewModel.setTheme(theme)
                },
                onToggleRecording = { viewModel.toggleRecording() },
                onSetSleepTimer = { mins -> viewModel.setSleepTimer(mins) }
            )
        }

        // Car HUD Mode Dialog
        if (showCarMode) {
            val quickStations = if (favorites.isNotEmpty()) favorites else playlist
            val isCurrentFav = favorites.any { it.id == playerState.currentStation?.id }
            CarModeDialog(
                playerState = playerState,
                fontScale = fontScale,
                quickStations = quickStations,
                isFavorite = isCurrentFav,
                onToggleFavorite = {
                    playerState.currentStation?.let { viewModel.toggleFavorite(it) }
                },
                onSelectStation = { station ->
                    viewModel.playStation(station)
                },
                onPrevious = { viewModel.previousStation() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.nextStation() },
                onDismiss = { showCarMode = false }
            )
        }

        // Self-Update Dialog (GitHub Releases auto/manual update)
        if (showUpdateDialog && updateCheckResult is UpdateCheckResult.HasUpdate) {
            val updateInfo = (updateCheckResult as UpdateCheckResult.HasUpdate).info
            AppUpdateDialog(
                updateInfo = updateInfo,
                downloadState = downloadState,
                onStartDownload = { viewModel.startDownloadUpdate(updateInfo) },
                onInstallApk = { file -> viewModel.installApk(file) },
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }

        // Music Recognition Dialog (听歌识曲)
        val showRecognitionDialog by viewModel.showRecognitionDialog.collectAsState()
        val recognitionState by viewModel.recognitionState.collectAsState()
        val currentRecognitionMode by viewModel.currentRecognitionMode.collectAsState()

        if (showRecognitionDialog) {
            MusicRecognitionDialog(
                state = recognitionState,
                currentMode = currentRecognitionMode,
                isRadioPlaying = playerState.status == PlaybackStatus.PLAYING,
                currentStationName = playerState.currentStation?.name,
                onSwitchMode = { mode -> viewModel.startMusicRecognition(mode) },
                onRetry = { viewModel.startMusicRecognition(currentRecognitionMode) },
                onApplyToTrackTitle = { song -> viewModel.applyRecognizedSongToNowPlaying(song) },
                onDismiss = { viewModel.closeMusicRecognition() }
            )
        }
    }
}
