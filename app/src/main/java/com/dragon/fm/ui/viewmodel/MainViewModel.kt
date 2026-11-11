package com.dragon.fm.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dragon.fm.DragonFmApp
import com.dragon.fm.data.api.IpLocationService
import com.dragon.fm.data.api.RadioBrowserApi
import com.dragon.fm.data.local.FavoritesRepository
import com.dragon.fm.data.local.BackupManager
import com.dragon.fm.data.local.M3uPlaylistIO
import com.dragon.fm.data.local.PresetsData
import com.dragon.fm.data.local.DedicatedSourceManager
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.PlayerState
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.StreamLine
import com.dragon.fm.data.model.RecognitionMode
import com.dragon.fm.data.model.RecognitionState
import com.dragon.fm.data.model.RecognizedSong
import com.dragon.fm.player.DragonRadioPlayer
import com.dragon.fm.player.MusicRecognitionManager
import com.dragon.fm.player.RecordedFile
import com.dragon.fm.player.RecorderManager
import com.dragon.fm.player.SleepTimerManager
import com.dragon.fm.ui.theme.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dragon.fm.data.api.AppUpdateManager
import com.dragon.fm.data.local.ExclusionFilterManager
import com.dragon.fm.data.model.AppUpdateInfo
import com.dragon.fm.data.model.CustomScenarioMode
import com.dragon.fm.data.model.DownloadState
import com.dragon.fm.data.model.UpdateCheckResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DragonFmApp
    val player: DragonRadioPlayer = app.radioPlayer
    val favoritesRepo: FavoritesRepository = app.favoritesRepository
    val recorderManager: RecorderManager = app.recorderManager
    val recognitionManager: MusicRecognitionManager = app.musicRecognitionManager
    val sleepTimer: SleepTimerManager = app.sleepTimerManager
    val localRecordingPlayer = com.dragon.fm.player.LocalRecordingPlayer(application)
    private val api = RadioBrowserApi()
    val exclusionManager = ExclusionFilterManager(application)
    private val json = Json { ignoreUnknownKeys = true }

    val playerState: StateFlow<PlayerState> = player.playerState
    val favoriteIds: StateFlow<Set<String>> = favoritesRepo.favoriteIds
    val favoriteStations: StateFlow<List<RadioStation>> = favoritesRepo.favoriteStations
    val historyList: StateFlow<List<RadioStation>> = favoritesRepo.history
    val historyStations: StateFlow<List<RadioStation>> = historyList
    val customStations: StateFlow<List<RadioStation>> = favoritesRepo.customStations
    val recordedFiles: StateFlow<List<RecordedFile>> = recorderManager.recordingsList
    val playingRecordedFile: StateFlow<RecordedFile?> = localRecordingPlayer.currentPlayingFile
    val isLocalRecordingPlaying: StateFlow<Boolean> = localRecordingPlayer.isPlaying
    val localRecordingPositionMs: StateFlow<Long> = localRecordingPlayer.currentPositionMs
    val localRecordingDurationMs: StateFlow<Long> = localRecordingPlayer.durationMs
    val localRecordingSpeed: StateFlow<Float> = localRecordingPlayer.playbackSpeed
    val recognitionState: StateFlow<RecognitionState> = recognitionManager.state
    val recognitionHistory: StateFlow<List<RecognizedSong>> = recognitionManager.history

    // Music recognition dialog visibility
    private val _showRecognitionDialog = MutableStateFlow(false)
    val showRecognitionDialog: StateFlow<Boolean> = _showRecognitionDialog.asStateFlow()

    private val _currentRecognitionMode = MutableStateFlow(RecognitionMode.RADIO_STREAM)
    val currentRecognitionMode: StateFlow<RecognitionMode> = _currentRecognitionMode.asStateFlow()

    // Exclusion filter states
    val isDomesticOnly: StateFlow<Boolean> = exclusionManager.isDomesticOnly
    val blockedStations: StateFlow<Map<String, String>> = exclusionManager.blockedStations
    val blockedTags: StateFlow<Set<String>> = exclusionManager.blockedTags

    val isRecording: StateFlow<Boolean> = recorderManager.isRecording
    val recordingDurationSec: StateFlow<Int> = recorderManager.recordingDurationSec
    val sleepTimerSeconds: StateFlow<Int?> = sleepTimer.remainingSeconds
    val selectedSleepMinutes: StateFlow<Int?> = sleepTimer.selectedMinutes

    private val _playlist = MutableStateFlow<List<RadioStation>>(PresetsData.defaultStations)
    val playlist: StateFlow<List<RadioStation>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Scenarios (汽水音乐场景与分类标签)
    private val _selectedScenario = MutableStateFlow("精选推荐")
    val selectedScenario: StateFlow<String> = _selectedScenario.asStateFlow()

    // Custom Scenario Modes
    private val _customScenarios = MutableStateFlow<List<CustomScenarioMode>>(emptyList())
    val customScenarios: StateFlow<List<CustomScenarioMode>> = _customScenarios.asStateFlow()

    // Deleted Default Scenario Keys (allows user to remove default modes and restore them)
    private val _deletedDefaultScenarios = MutableStateFlow<Set<String>>(emptySet())
    val deletedDefaultScenarios: StateFlow<Set<String>> = _deletedDefaultScenarios.asStateFlow()

    // Infinite Random Roaming deduplication queue
    private val recentRoamingIds = java.util.ArrayDeque<String>(200)

    private val _detectedLocation = MutableStateFlow("本地电台")
    val detectedLocation: StateFlow<String> = _detectedLocation.asStateFlow()

    private val _detectedProvince = MutableStateFlow<String?>("河南")
    val detectedProvince: StateFlow<String?> = _detectedProvince.asStateFlow()

    private val _detectedCity = MutableStateFlow<String?>("郑州")
    val detectedCity: StateFlow<String?> = _detectedCity.asStateFlow()

    // Pure Sleep Mode state (纯净睡眠模式)
    private val _isSleepModeActive = MutableStateFlow(false)
    val isSleepModeActive: StateFlow<Boolean> = _isSleepModeActive.asStateFlow()

    // Explore & Search State
    private val _exploreStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val exploreStations: StateFlow<List<RadioStation>> = _exploreStations.asStateFlow()

    private val _radarSearchQuery = MutableStateFlow("")
    val radarSearchQuery: StateFlow<String> = _radarSearchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // High-speed in-memory cache for explore tabs & provinces (tag/province -> list)
    private val exploreStationCache = java.util.concurrent.ConcurrentHashMap<String, List<RadioStation>>()

    private val _currentExploreTag = MutableStateFlow("全国精选")
    val currentExploreTag: StateFlow<String> = _currentExploreTag.asStateFlow()

    private val _currentExploreGroupIndex = MutableStateFlow(0)
    val currentExploreGroupIndex: StateFlow<Int> = _currentExploreGroupIndex.asStateFlow()

    fun setExploreTag(tag: String) {
        _currentExploreTag.value = tag
    }

    fun setExploreGroupIndex(index: Int) {
        _currentExploreGroupIndex.value = index
    }

    fun clearExploreCache() {
        exploreStationCache.clear()
    }

    // Speed test state & low latency sorting
    val latencies: StateFlow<Map<String, Long>> = com.dragon.fm.player.StationSpeedTester.latencies
    val isSpeedTesting: StateFlow<Boolean> = com.dragon.fm.player.StationSpeedTester.isTesting

    private val _isSortByLatency = MutableStateFlow(false)
    val isSortByLatency: StateFlow<Boolean> = _isSortByLatency.asStateFlow()

    // Multi-style Theme Engine (跟随系统 / 11款精美浅色与深色主题)
    private val prefs = application.getSharedPreferences("dragon_fm_prefs", Context.MODE_PRIVATE)
    private val _isFollowSystemTheme = MutableStateFlow(prefs.getBoolean("pref_follow_system_theme", true))
    val isFollowSystemTheme: StateFlow<Boolean> = _isFollowSystemTheme.asStateFlow()

    private val _selectedLightThemeId = MutableStateFlow(
        prefs.getString("pref_selected_light_theme", ThemePreset.PORCELAIN_JADE.id) ?: ThemePreset.PORCELAIN_JADE.id
    )
    val selectedLightThemeId: StateFlow<String> = _selectedLightThemeId.asStateFlow()

    private val _selectedDarkThemeId = MutableStateFlow(
        prefs.getString("pref_selected_dark_theme", ThemePreset.SODA_OBSIDIAN.id) ?: ThemePreset.SODA_OBSIDIAN.id
    )
    val selectedDarkThemeId: StateFlow<String> = _selectedDarkThemeId.asStateFlow()

    private val _currentTheme = MutableStateFlow(
        ThemePreset.fromId(prefs.getString("saved_theme_id", ThemePreset.PORCELAIN_JADE.id) ?: ThemePreset.PORCELAIN_JADE.id)
    )
    val currentTheme: StateFlow<ThemePreset> = _currentTheme.asStateFlow()

    fun setFollowSystemTheme(enabled: Boolean) {
        _isFollowSystemTheme.value = enabled
        prefs.edit().putBoolean("pref_follow_system_theme", enabled).apply()
    }

    fun setTheme(theme: ThemePreset) {
        _currentTheme.value = theme
        prefs.edit().putString("saved_theme_id", theme.id).apply()
        if (theme.isDark) {
            _selectedDarkThemeId.value = theme.id
            prefs.edit().putString("pref_selected_dark_theme", theme.id).apply()
        } else {
            _selectedLightThemeId.value = theme.id
            prefs.edit().putString("pref_selected_light_theme", theme.id).apply()
        }
    }

    // App Background Wallpapers & Presets
    private val _appBackground = MutableStateFlow(
        com.dragon.fm.data.model.AppBackgroundState(
            type = try {
                com.dragon.fm.data.model.AppBackgroundType.valueOf(
                    prefs.getString("pref_bg_type", com.dragon.fm.data.model.AppBackgroundType.DEFAULT.name)
                        ?: com.dragon.fm.data.model.AppBackgroundType.DEFAULT.name
                )
            } catch (_: Exception) { com.dragon.fm.data.model.AppBackgroundType.DEFAULT },
            presetId = prefs.getString("pref_bg_preset_id", "twilight") ?: "twilight",
            customImagePath = prefs.getString("pref_bg_custom_path", null),
            overlayAlpha = prefs.getFloat("pref_bg_overlay_alpha", 0.65f)
        )
    )
    val appBackground: StateFlow<com.dragon.fm.data.model.AppBackgroundState> = _appBackground.asStateFlow()

    fun setBackgroundType(type: com.dragon.fm.data.model.AppBackgroundType) {
        _appBackground.value = _appBackground.value.copy(type = type)
        prefs.edit().putString("pref_bg_type", type.name).apply()
    }

    fun setBackgroundPreset(presetId: String) {
        _appBackground.value = _appBackground.value.copy(
            type = com.dragon.fm.data.model.AppBackgroundType.PRESET,
            presetId = presetId
        )
        prefs.edit()
            .putString("pref_bg_type", com.dragon.fm.data.model.AppBackgroundType.PRESET.name)
            .putString("pref_bg_preset_id", presetId)
            .apply()
    }

    fun setCustomBackgroundImage(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(uriString))
                val targetFile = java.io.File(context.filesDir, "custom_bg_wallpaper.jpg")
                inputStream?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val path = targetFile.absolutePath
                _appBackground.value = _appBackground.value.copy(
                    type = com.dragon.fm.data.model.AppBackgroundType.CUSTOM,
                    customImagePath = path
                )
                prefs.edit()
                    .putString("pref_bg_type", com.dragon.fm.data.model.AppBackgroundType.CUSTOM.name)
                    .putString("pref_bg_custom_path", path)
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetBackgroundToDefault() {
        _appBackground.value = _appBackground.value.copy(type = com.dragon.fm.data.model.AppBackgroundType.DEFAULT)
        prefs.edit().putString("pref_bg_type", com.dragon.fm.data.model.AppBackgroundType.DEFAULT.name).apply()
    }

    // Stream Source & Line Preference (智能极速模式 / 纯净官方专线 / 全球公网广播)
    private val _streamSourceMode = MutableStateFlow(
        com.dragon.fm.data.model.StreamSourceMode.values().find {
            it.code == prefs.getString("pref_stream_source_mode", com.dragon.fm.data.model.StreamSourceMode.HYBRID.code)
        } ?: com.dragon.fm.data.model.StreamSourceMode.HYBRID
    )
    val streamSourceMode: StateFlow<com.dragon.fm.data.model.StreamSourceMode> = _streamSourceMode.asStateFlow()

    fun setStreamSourceMode(mode: com.dragon.fm.data.model.StreamSourceMode) {
        _streamSourceMode.value = mode
        prefs.edit().putString("pref_stream_source_mode", mode.code).apply()
        DedicatedSourceManager.currentSourceMode = mode
        api.clearCache()
        DedicatedSourceManager.clearCache()
        exploreStationCache.clear()
        loadExploreInitial(forceRefresh = true)
        selectScenario(selectedScenario.value)
    }

    // User Region Customization (支持手动指定省份与二级市)
    private val _userSelectedProvince = MutableStateFlow<String?>(prefs.getString("saved_user_province", null))
    val userSelectedProvince: StateFlow<String?> = _userSelectedProvince.asStateFlow()

    private val _userSelectedCity = MutableStateFlow<String?>(prefs.getString("saved_user_city", null))
    val userSelectedCity: StateFlow<String?> = _userSelectedCity.asStateFlow()

    private val _isCustomRegionEnabled = MutableStateFlow(prefs.getBoolean("saved_user_custom_region", false))
    val isCustomRegionEnabled: StateFlow<Boolean> = _isCustomRegionEnabled.asStateFlow()

    fun setUserRegion(province: String, city: String) {
        _userSelectedProvince.value = province
        _userSelectedCity.value = city
        _isCustomRegionEnabled.value = true
        prefs.edit()
            .putString("saved_user_province", province)
            .putString("saved_user_city", city)
            .putBoolean("saved_user_custom_region", true)
            .apply()

        val display = if (city.isBlank() || city == "全城通用" || city == "全省通用" || city == "全区通用") province else "$province · $city"
        _detectedLocation.value = "本地 ($display)"

        if (_selectedScenario.value.contains("本地")) {
            loadLocalScenarioStations(province, city)
        }
    }

    fun resetToAutoLocation() {
        _userSelectedProvince.value = null
        _userSelectedCity.value = null
        _isCustomRegionEnabled.value = false
        prefs.edit()
            .remove("saved_user_province")
            .remove("saved_user_city")
            .putBoolean("saved_user_custom_region", false)
            .apply()
        detectLocalLocation()
    }

    // Fast Buffer Setting (0.25s 极速缓冲模式)
    private val _isFastBuffer = MutableStateFlow(prefs.getBoolean("pref_fast_buffer", true))
    val isFastBuffer: StateFlow<Boolean> = _isFastBuffer.asStateFlow()

    fun setFastBuffer(enabled: Boolean) {
        _isFastBuffer.value = enabled
        prefs.edit().putBoolean("pref_fast_buffer", enabled).apply()
    }

    // Auto-play on launch setting (启动时自动播放)
    private val _isAutoPlayOnLaunch = MutableStateFlow(prefs.getBoolean("pref_auto_play_on_launch", true))
    val isAutoPlayOnLaunch: StateFlow<Boolean> = _isAutoPlayOnLaunch.asStateFlow()

    fun setAutoPlayOnLaunch(enabled: Boolean) {
        _isAutoPlayOnLaunch.value = enabled
        prefs.edit().putBoolean("pref_auto_play_on_launch", enabled).apply()
    }

    // Font Scale setting (界面字体大小调节: 0.88f 紧凑, 1.0f 标准, 1.12f 舒适, 1.25f 特大)
    private val _fontScale = MutableStateFlow(prefs.getFloat("pref_font_scale", 1.0f))
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.85f, 1.30f)
        _fontScale.value = clamped
        prefs.edit().putFloat("pref_font_scale", clamped).apply()
    }

    // Playback Memory Setting (记忆最后收听电台、模式与播放列表)
    private val _isPlaybackMemoryEnabled = MutableStateFlow(prefs.getBoolean("pref_playback_memory", true))
    val isPlaybackMemoryEnabled: StateFlow<Boolean> = _isPlaybackMemoryEnabled.asStateFlow()

    fun setPlaybackMemoryEnabled(enabled: Boolean) {
        _isPlaybackMemoryEnabled.value = enabled
        prefs.edit().putBoolean("pref_playback_memory", enabled).apply()
        if (!enabled) {
            prefs.edit()
                .remove("saved_last_station")
                .remove("saved_last_scenario")
                .remove("saved_last_playlist")
                .remove("saved_last_index")
                .apply()
        } else {
            val curList = _playlist.value
            val idx = _currentIndex.value
            if (idx in curList.indices) {
                savePlaybackMemory(curList[idx], _selectedScenario.value, curList, idx)
            }
        }
    }

    private fun savePlaybackMemory(
        station: RadioStation,
        scenario: String,
        playlist: List<RadioStation>,
        index: Int
    ) {
        if (!_isPlaybackMemoryEnabled.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val stationJson = json.encodeToString(station)
                val trimmedPlaylist = playlist.take(120)
                val playlistJson = json.encodeToString(trimmedPlaylist)
                prefs.edit()
                    .putString("saved_last_station", stationJson)
                    .putString("saved_last_scenario", scenario)
                    .putString("saved_last_playlist", playlistJson)
                    .putInt("saved_last_index", index)
                    .apply()
            } catch (_: Exception) {}
        }
    }

    private fun updateSavedPlaylistIfMemoryEnabled() {
        if (!_isPlaybackMemoryEnabled.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val curList = _playlist.value
                val curIndex = _currentIndex.value
                val curStation = curList.getOrNull(curIndex) ?: return@launch
                val stationJson = json.encodeToString(curStation)
                val trimmedPlaylist = curList.take(120)
                val playlistJson = json.encodeToString(trimmedPlaylist)
                prefs.edit()
                    .putString("saved_last_station", stationJson)
                    .putString("saved_last_scenario", _selectedScenario.value)
                    .putString("saved_last_playlist", playlistJson)
                    .putInt("saved_last_index", curIndex)
                    .apply()
            } catch (_: Exception) {}
        }
    }

    // Image Cache Size Calculation & Clearing
    private val _imageCacheSizeBytes = MutableStateFlow(0L)
    val imageCacheSizeBytes: StateFlow<Long> = _imageCacheSizeBytes.asStateFlow()

    fun refreshCacheSize() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val dir = java.io.File(getApplication<Application>().cacheDir, "image_cache")
            val size = if (dir.exists()) {
                dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else 0L
            _imageCacheSizeBytes.value = size
        }
    }

    fun clearImageCache(): Long {
        val dir = java.io.File(getApplication<Application>().cacheDir, "image_cache")
        val size = if (dir.exists()) {
            dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
        if (dir.exists()) {
            dir.deleteRecursively()
            dir.mkdirs()
        }
        _imageCacheSizeBytes.value = 0L
        return size
    }

    // 启动前是否已存在播放记忆（用于判断是否为“首次启动”，首启直接加载本地电台列表）
    private val hadSavedPlaybackAtLaunch: Boolean =
        prefs.getBoolean("pref_playback_memory", true) &&
                !prefs.getString("saved_last_station", null).isNullOrBlank()

    init {
        // Initialize stream source mode
        DedicatedSourceManager.currentSourceMode = _streamSourceMode.value

        // Disable auto-skip on radio error to prevent unexpected channel jumping
        DragonRadioPlayer.onAutoSkipListener = null
        com.dragon.fm.player.DragonMediaSessionService.onSkipPreviousListener = {
            viewModelScope.launch {
                previousStation()
            }
        }
        com.dragon.fm.player.DragonMediaSessionService.onSkipNextListener = {
            viewModelScope.launch {
                nextStation()
            }
        }

        loadCustomScenarios()
        loadScenarioStationOverrides()
        loadScenarioOrder()
        loadDeletedDefaultScenarios()

        // 曲阳收音机 1.0.0：首次升级清空播放列表，交由用户自行导入 M3U
        if (!prefs.getBoolean("pref_qy_v1_empty_migrated", false)) {
            prefs.edit()
                .remove("saved_last_station")
                .remove("saved_last_playlist")
                .remove("saved_last_scenario")
                .remove("saved_last_index")
                .putBoolean("pref_qy_v1_empty_migrated", true)
                .apply()
            favoritesRepo.clearCustomStations()
        }

        viewModelScope.launch {
            val memoryEnabled = prefs.getBoolean("pref_playback_memory", true)
            var restored = false

            if (memoryEnabled) {
                val savedStationJson = prefs.getString("saved_last_station", null)
                val savedScenario = prefs.getString("saved_last_scenario", null)
                val savedPlaylistJson = prefs.getString("saved_last_playlist", null)
                val savedIndex = prefs.getInt("saved_last_index", 0)

                if (!savedStationJson.isNullOrBlank()) {
                    try {
                        val lastStation = json.decodeFromString<RadioStation>(savedStationJson)
                        val lastPlaylist = if (!savedPlaylistJson.isNullOrBlank()) {
                            json.decodeFromString<List<RadioStation>>(savedPlaylistJson)
                        } else emptyList()

                        val filteredPlaylist = exclusionManager.filterStations(lastPlaylist).toMutableList()

                        // Determine target index
                        var targetIndex = -1
                        if (savedIndex in filteredPlaylist.indices && filteredPlaylist[savedIndex].id == lastStation.id) {
                            targetIndex = savedIndex
                        } else {
                            targetIndex = filteredPlaylist.indexOfFirst { it.id == lastStation.id }
                        }

                        // If not found in filtered list but not blocked, insert back at saved position
                        if (targetIndex == -1 && !exclusionManager.isStationBlocked(lastStation)) {
                            val insertPos = savedIndex.coerceIn(0, filteredPlaylist.size)
                            filteredPlaylist.add(insertPos, lastStation)
                            targetIndex = insertPos
                        }

                        if (filteredPlaylist.isNotEmpty() && targetIndex in filteredPlaylist.indices) {
                            val scenarioName = savedScenario ?: "精选推荐"
                            val combined = mergeCustomStations(filteredPlaylist)
                            val playIndex = combined.indexOfFirst { it.id == lastStation.id }
                            _selectedScenario.value = scenarioName
                            _playlist.value = combined
                            if (playIndex >= 0) {
                                if (_isAutoPlayOnLaunch.value) {
                                    playStationAtIndex(playIndex)
                                } else {
                                    prepareStationAtIndex(playIndex)
                                }
                            }
                            restored = true
                        }
                    } catch (_: Exception) {
                        restored = false
                    }
                }
            }

            if (!restored) {
                // 首页默认保持为空；仅显示用户自己导入/添加的电台集合
                _playlist.value = favoritesRepo.customStations.value
                if (_playlist.value.isNotEmpty()) {
                    if (_isAutoPlayOnLaunch.value) {
                        playStationAtIndex(0)
                    } else {
                        prepareStationAtIndex(0)
                    }
                }
            }
        }

        // Set initial location from custom region or auto IP
        if (_isCustomRegionEnabled.value && !_userSelectedProvince.value.isNullOrBlank()) {
            val p = _userSelectedProvince.value ?: ""
            val c = _userSelectedCity.value ?: ""
            val display = if (c.isBlank() || c == "全城通用" || c == "全省通用" || c == "全区通用") p else c
            _detectedLocation.value = "本地 ($display)"
        } else {
            detectLocalLocation()
        }
        refreshCacheSize()

        scheduleAutomaticUpdateCheck()

        // Listen to music recognition results to sync Now Playing track & trigger floating banner / failure toasts
        viewModelScope.launch {
            recognitionState.collect { state ->
                when (state) {
                    is RecognitionState.Success -> {
                        applyRecognizedSongToNowPlaying(state.song)
                        _floatingRecognizedSong.value = state.song
                    }
                    is RecognitionState.NoMatch -> {
                        val tip = state.message.ifBlank { "当前广播未识别到歌曲（可能正处于主持人口播、新闻或广告）" }
                        showToast(tip)
                        delay(2500)
                        if (recognitionState.value is RecognitionState.NoMatch) {
                            recognitionManager.resetState()
                        }
                    }
                    is RecognitionState.Error -> {
                        val tip = if (state.message.contains("采样数据不足") || state.message.contains("有效音频长度")) {
                            "电台流采样数据不足，请确保电台播放流畅后重试"
                        } else {
                            "识曲未完成: ${state.message}"
                        }
                        showToast(tip)
                        delay(2500)
                        if (recognitionState.value is RecognitionState.Error) {
                            recognitionManager.resetState()
                        }
                    }
                    else -> {}
                }
            }
        }

        // Prewarm high-speed explore cache and instant stations on startup
        prewarmExploreCache()
    }

    // GitHub Releases in-app update engine
    val updateManager = AppUpdateManager(application)

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateCheckResult: StateFlow<UpdateCheckResult> = _updateCheckResult.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _autoUpdateEnabled =
        MutableStateFlow(prefs.getBoolean("pref_auto_update_enabled", true))
    val autoUpdateEnabled: StateFlow<Boolean> = _autoUpdateEnabled.asStateFlow()

    val currentVersionName: String get() = updateManager.getCurrentVersionName()
    val currentVersionCode: Int get() = updateManager.getCurrentVersionCode()

    fun setAutoUpdateEnabled(enabled: Boolean) {
        _autoUpdateEnabled.value = enabled
        prefs.edit().putBoolean("pref_auto_update_enabled", enabled).apply()
        if (enabled) {
            checkForUpdates(isManual = false)
        }
    }

    private fun scheduleAutomaticUpdateCheck() {
        viewModelScope.launch {
            delay(2_500)
            if (!_autoUpdateEnabled.value) return@launch
            val now = System.currentTimeMillis()
            val lastCheck = prefs.getLong("last_auto_update_check", 0L)
            if (now - lastCheck < AUTO_UPDATE_CHECK_INTERVAL_MS) return@launch
            prefs.edit().putLong("last_auto_update_check", now).apply()
            checkForUpdates(isManual = false)
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        if (_updateCheckResult.value is UpdateCheckResult.Checking) return
        viewModelScope.launch {
            _updateCheckResult.value = UpdateCheckResult.Checking
            val result = updateManager.checkUpdate(isManual = isManual)
            _updateCheckResult.value = result
            if (result is UpdateCheckResult.HasUpdate) {
                _showUpdateDialog.value = true
                if (!isManual && _autoUpdateEnabled.value) {
                    startDownloadUpdate(result.info)
                }
            }
        }
    }

    fun startDownloadUpdate(info: AppUpdateInfo) {
        if (_downloadState.value is DownloadState.Downloading) return
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0f, 0L, 0L)
            val result = updateManager.downloadApk(info) { progress, cur, total ->
                _downloadState.value = DownloadState.Downloading(progress, cur, total)
            }
            result.fold(
                onSuccess = { file ->
                    _downloadState.value = DownloadState.Completed(file)
                },
                onFailure = { err ->
                    _downloadState.value = DownloadState.Error(err.message ?: "下载失败")
                }
            )
        }
    }

    fun installApk(file: java.io.File) {
        updateManager.installApk(file)
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    private companion object {
        const val AUTO_UPDATE_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }

    // Scenario Station Overrides & Customization (场景内电台收藏、删除与排序)
    private val _scenarioStationOverrides = MutableStateFlow<Map<String, List<RadioStation>>>(emptyMap())
    val scenarioStationOverrides: StateFlow<Map<String, List<RadioStation>>> = _scenarioStationOverrides.asStateFlow()

    // Overall Scenario Order (支持全场景展示排序调整)
    private val _scenarioOrder = MutableStateFlow<List<String>>(emptyList())
    val scenarioOrder: StateFlow<List<String>> = _scenarioOrder.asStateFlow()

    private fun loadScenarioStationOverrides() {
        val raw = prefs.getString("saved_scenario_stations", null) ?: return
        try {
            _scenarioStationOverrides.value = json.decodeFromString(raw)
        } catch (_: Exception) {}
    }

    private fun saveScenarioStationOverridesToPrefs() {
        try {
            val encoded = json.encodeToString(_scenarioStationOverrides.value)
            prefs.edit().putString("saved_scenario_stations", encoded).apply()
        } catch (_: Exception) {}
    }

    private fun loadScenarioOrder() {
        val raw = prefs.getString("saved_scenario_order", null) ?: return
        try {
            _scenarioOrder.value = json.decodeFromString(raw)
        } catch (_: Exception) {}
    }

    fun reorderAllScenarios(orderedKeys: List<String>) {
        _scenarioOrder.value = orderedKeys
        try {
            prefs.edit().putString("saved_scenario_order", json.encodeToString(orderedKeys)).apply()
        } catch (_: Exception) {}
    }

    private fun loadCustomScenarios() {
        val raw = prefs.getString("saved_custom_scenarios", null) ?: return
        try {
            val decoded: List<CustomScenarioMode> = json.decodeFromString(raw)
            val filtered = decoded.filterNot {
                it.id.contains("canton", ignoreCase = true) ||
                it.title.contains("canton", ignoreCase = true) ||
                it.title.contains("粤语", ignoreCase = true)
            }
            _customScenarios.value = filtered
            if (filtered.size != decoded.size) {
                saveCustomScenariosToPrefs()
            }
        } catch (_: Exception) {}
    }

    private fun saveCustomScenariosToPrefs() {
        try {
            prefs.edit().putString("saved_custom_scenarios", json.encodeToString(_customScenarios.value)).apply()
        } catch (_: Exception) {}
    }

    fun saveCustomScenario(
        title: String,
        iconEmoji: String,
        query: String = "",
        replaceId: String? = null,
        initialStation: RadioStation? = null
    ): String {
        val current = _customScenarios.value.toMutableList()
        val id = replaceId ?: "mode_${System.currentTimeMillis()}"
        val existingIndex = current.indexOfFirst { it.id == id }
        val stations = if (existingIndex >= 0) {
            val list = current[existingIndex].stations.toMutableList()
            if (initialStation != null && list.none { it.id == initialStation.id }) {
                list.add(0, initialStation)
            }
            list
        } else {
            if (initialStation != null) listOf(initialStation) else emptyList()
        }

        val newMode = CustomScenarioMode(
            id = id,
            title = title,
            iconEmoji = iconEmoji,
            query = query,
            stations = stations
        )

        if (existingIndex >= 0) {
            current[existingIndex] = newMode
        } else {
            current.add(0, newMode)
        }
        _customScenarios.value = current
        saveCustomScenariosToPrefs()
        return id
    }

    fun createCustomScenario(
        title: String,
        iconKey: String,
        initialStations: List<RadioStation> = emptyList()
    ): String {
        val id = saveCustomScenario(
            title = title,
            iconEmoji = iconKey,
            initialStation = initialStations.firstOrNull()
        )
        if (initialStations.size > 1) {
            val current = _customScenarios.value.toMutableList()
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) {
                current[idx] = current[idx].copy(stations = initialStations)
                _customScenarios.value = current
                saveCustomScenariosToPrefs()
            }
        }
        return id
    }

    fun updateCustomScenario(id: String, newTitle: String, newIconKey: String) {
        val current = _customScenarios.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(title = newTitle, iconEmoji = newIconKey)
            _customScenarios.value = current
            saveCustomScenariosToPrefs()
        }
    }

    fun reorderCustomScenarios(newList: List<CustomScenarioMode>) {
        _customScenarios.value = newList
        saveCustomScenariosToPrefs()
    }

    fun deleteCustomScenario(id: String) {
        val current = _customScenarios.value.toMutableList()
        current.removeAll { it.id == id }
        _customScenarios.value = current
        saveCustomScenariosToPrefs()
    }

    private fun loadDeletedDefaultScenarios() {
        val set = prefs.getStringSet("deleted_default_scenarios", emptySet()) ?: emptySet()
        _deletedDefaultScenarios.value = set
    }

    fun deleteDefaultScenario(key: String) {
        val current = _deletedDefaultScenarios.value.toMutableSet()
        current.add(key)
        _deletedDefaultScenarios.value = current
        prefs.edit().putStringSet("deleted_default_scenarios", current).apply()
    }

    fun restoreDefaultScenarios() {
        _deletedDefaultScenarios.value = emptySet()
        prefs.edit().remove("deleted_default_scenarios").apply()
    }

    fun getBaseStationsForScenario(cleanName: String): List<RadioStation> {
        return when {
            cleanName.contains("交通") || cleanName.contains("路况") -> PresetsData.trafficStations
            cleanName.contains("通勤") -> PresetsData.commuteStations
            cleanName.contains("助眠") -> PresetsData.sleepStations
            cleanName.contains("专注") -> PresetsData.focusStations
            cleanName.contains("新闻") -> PresetsData.newsStations
            cleanName.contains("摇滚") -> PresetsData.rockStations
            cleanName.contains("经典") -> PresetsData.classicStations
            cleanName.contains("本地") -> {
                val prov = if (_isCustomRegionEnabled.value) _userSelectedProvince.value else _detectedProvince.value
                val city = if (_isCustomRegionEnabled.value) _userSelectedCity.value else _detectedCity.value
                val list = mutableListOf<RadioStation>()
                if (!city.isNullOrBlank()) list.addAll(PresetsData.findStationsByCity(city))
                if (!prov.isNullOrBlank()) {
                    val cityIds = list.map { it.id }.toSet()
                    list.addAll(PresetsData.findStationsByProvince(prov).filter { it.id !in cityIds })
                }
                if (list.isEmpty()) list.addAll(PresetsData.nationalStations)
                list
            }
            else -> PresetsData.featuredStations
        }
    }

    fun getScenariosContainingStation(stationId: String): Set<String> {
        val set = mutableSetOf<String>()
        // 1. Custom scenarios
        _customScenarios.value.forEach { mode ->
            if (mode.stations.any { it.id == stationId }) {
                set.add(mode.title)
            }
        }
        // 2. Builtin scenario overrides
        _scenarioStationOverrides.value.forEach { (name, list) ->
            if (list.any { it.id == stationId }) {
                set.add(name)
            }
        }
        // 3. Favorites mode
        if (favoritesRepo.isFavorite(stationId)) {
            set.add("我的收藏")
        }
        return set
    }

    fun toggleStationInScenario(scenarioName: String, station: RadioStation): Boolean {
        val cleanName = scenarioName.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        val isAlreadyIn = getScenariosContainingStation(station.id).contains(cleanName)
        if (isAlreadyIn) {
            removeStationFromScenario(cleanName, station.id)
            return false
        } else {
            addStationToScenario(cleanName, station)
            return true
        }
    }

    fun addStationToScenario(scenarioName: String, station: RadioStation) {
        val cleanName = scenarioName.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        if (cleanName == "我的收藏" || cleanName.contains("收藏")) {
            if (!favoritesRepo.isFavorite(station.id)) {
                favoritesRepo.toggleFavorite(station)
            }
            if (_selectedScenario.value.contains("收藏")) {
                _playlist.value = favoritesRepo.favoriteStations.value
            }
            return
        }

        val customIndex = _customScenarios.value.indexOfFirst {
            val title = it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
            title == cleanName || it.id == scenarioName
        }

        if (customIndex >= 0) {
            val list = _customScenarios.value.toMutableList()
            val mode = list[customIndex]
            val newStations = mode.stations.toMutableList()
            newStations.removeAll { it.id == station.id }
            newStations.add(0, station)
            list[customIndex] = mode.copy(stations = newStations)
            _customScenarios.value = list
            saveCustomScenariosToPrefs()

            if (_selectedScenario.value == mode.title || _selectedScenario.value == cleanName) {
                _playlist.value = newStations
                updateSavedPlaylistIfMemoryEnabled()
            }
        } else {
            val currentOverrides = _scenarioStationOverrides.value.toMutableMap()
            val existingList = (currentOverrides[cleanName] ?: getBaseStationsForScenario(cleanName)).toMutableList()
            existingList.removeAll { it.id == station.id }
            existingList.add(0, station)
            currentOverrides[cleanName] = existingList
            _scenarioStationOverrides.value = currentOverrides
            saveScenarioStationOverridesToPrefs()

            if (_selectedScenario.value == cleanName) {
                _playlist.value = existingList
                updateSavedPlaylistIfMemoryEnabled()
            }
        }

        // Also sync into favorites so heart lights up
        if (!favoritesRepo.isFavorite(station.id)) {
            favoritesRepo.toggleFavorite(station)
        }
    }

    fun removeStationFromScenario(scenarioName: String, stationId: String) {
        val cleanName = scenarioName.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        if (cleanName == "我的收藏" || cleanName.contains("收藏")) {
            favoritesRepo.removeFavorite(stationId)
            if (_selectedScenario.value.contains("收藏")) {
                _playlist.value = favoritesRepo.favoriteStations.value
                val curIdx = _currentIndex.value.coerceIn(0, (_playlist.value.size - 1).coerceAtLeast(0))
                if (_playlist.value.isNotEmpty()) {
                    _currentIndex.value = curIdx
                }
            }
            return
        }

        val customIndex = _customScenarios.value.indexOfFirst {
            val title = it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
            title == cleanName || it.id == scenarioName
        }

        if (customIndex >= 0) {
            val list = _customScenarios.value.toMutableList()
            val mode = list[customIndex]
            val newStations = mode.stations.filter { it.id != stationId }
            list[customIndex] = mode.copy(stations = newStations)
            _customScenarios.value = list
            saveCustomScenariosToPrefs()

            if (_selectedScenario.value == mode.title || _selectedScenario.value == cleanName) {
                val updated = _playlist.value.filter { it.id != stationId }
                _playlist.value = updated
                val curIdx = _currentIndex.value.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
                if (updated.isNotEmpty()) {
                    _currentIndex.value = curIdx
                }
                updateSavedPlaylistIfMemoryEnabled()
            }
        } else {
            val currentOverrides = _scenarioStationOverrides.value.toMutableMap()
            val existingList = (currentOverrides[cleanName] ?: getBaseStationsForScenario(cleanName)).filter { it.id != stationId }
            currentOverrides[cleanName] = existingList
            _scenarioStationOverrides.value = currentOverrides
            saveScenarioStationOverridesToPrefs()

            if (_selectedScenario.value == cleanName) {
                val updated = _playlist.value.filter { it.id != stationId }
                _playlist.value = updated
                val curIdx = _currentIndex.value.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
                if (updated.isNotEmpty()) {
                    _currentIndex.value = curIdx
                }
                updateSavedPlaylistIfMemoryEnabled()
            }
        }
    }

    fun moveStationInScenario(scenarioName: String, fromIndex: Int, toIndex: Int) {
        val cleanName = scenarioName.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        val currentList = _playlist.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _playlist.value = currentList

            val cur = _currentIndex.value
            _currentIndex.value = when {
                cur == fromIndex -> toIndex
                fromIndex < toIndex && cur in (fromIndex + 1)..toIndex -> cur - 1
                fromIndex > toIndex && cur in toIndex until fromIndex -> cur + 1
                else -> cur
            }

            val customIndex = _customScenarios.value.indexOfFirst {
                it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim() == cleanName
            }
            if (customIndex >= 0) {
                val list = _customScenarios.value.toMutableList()
                list[customIndex] = list[customIndex].copy(stations = currentList)
                _customScenarios.value = list
                saveCustomScenariosToPrefs()
            } else {
                val currentOverrides = _scenarioStationOverrides.value.toMutableMap()
                currentOverrides[cleanName] = currentList
                _scenarioStationOverrides.value = currentOverrides
                saveScenarioStationOverridesToPrefs()
            }
            updateSavedPlaylistIfMemoryEnabled()
        }
    }

    fun setDomesticOnly(enabled: Boolean) {
        exclusionManager.setDomesticOnly(enabled)
        val filtered = exclusionManager.filterStations(_playlist.value)
        _playlist.value = filtered.ifEmpty { exclusionManager.filterStations(PresetsData.defaultStations) }
    }

    fun blockStation(station: RadioStation) {
        exclusionManager.blockStation(station)
        val filtered = exclusionManager.filterStations(_playlist.value)
        _playlist.value = filtered.ifEmpty { exclusionManager.filterStations(PresetsData.defaultStations) }
    }

    fun blockStationByName(name: String) {
        exclusionManager.blockStationByName(name)
        val filtered = exclusionManager.filterStations(_playlist.value)
        _playlist.value = filtered.ifEmpty { exclusionManager.filterStations(PresetsData.defaultStations) }
    }

    fun unblockStation(stationId: String) {
        exclusionManager.unblockStation(stationId)
    }

    fun clearAllBlockedStations() {
        exclusionManager.clearAllBlockedStations()
    }

    fun blockTag(tag: String) {
        exclusionManager.blockTag(tag)
        val filtered = exclusionManager.filterStations(_playlist.value)
        _playlist.value = filtered.ifEmpty { exclusionManager.filterStations(PresetsData.defaultStations) }
    }

    fun unblockTag(tag: String) {
        exclusionManager.unblockTag(tag)
    }

    fun removeFavorite(stationId: String) {
        favoritesRepo.removeFavorite(stationId)
    }

    fun pinFavorite(stationId: String) {
        favoritesRepo.pinFavorite(stationId)
    }

    fun removeFromHistory(stationId: String) {
        favoritesRepo.removeFromHistory(stationId)
    }

    fun clearHistory() {
        favoritesRepo.clearHistory()
    }

    /**
     * 极简版：把用户手动“添加新电台”的自建电台固定放在列表最前面
     */
    private fun mergeCustomStations(baseList: List<RadioStation>): List<RadioStation> {
        val customs = favoritesRepo.customStations.value
        if (customs.isEmpty()) return baseList
        val existingIds = baseList.map { it.id }.toSet()
        val pendingCustoms = customs.filter { it.id !in existingIds }
        return pendingCustoms + baseList
    }

    private fun expandPlaylistWithChinaStations() {
        viewModelScope.launch {
            api.getChinaStations(limit = 80).onSuccess { chinaList ->
                val filteredChina = exclusionManager.filterStations(chinaList)
                if (filteredChina.isNotEmpty()) {
                    val existingIds = _playlist.value.map { it.id }.toSet()
                    val existingNames = _playlist.value.map { it.name.trim() }.toSet()
                    val newStations = filteredChina.filter { it.id !in existingIds && it.name.trim() !in existingNames }
                    if (newStations.isNotEmpty()) {
                        _playlist.value = _playlist.value + newStations
                        updateSavedPlaylistIfMemoryEnabled()
                    }
                }
            }
        }
    }

    private fun detectLocalLocation() {
        viewModelScope.launch {
            val loc = IpLocationService.detectLocation()
            if (loc != null) {
                _detectedProvince.value = loc.province
                _detectedCity.value = loc.city
                val label = if (loc.city.isNotBlank() && loc.city != loc.province) {
                    "${loc.province} · ${loc.city}"
                } else {
                    loc.province
                }
                _detectedLocation.value = "本地 ($label)"

                // If user is currently listening to local scenario and not on custom region, reload local stations!
                if (_selectedScenario.value.contains("本地") && !_isCustomRegionEnabled.value) {
                    loadLocalScenarioStations(loc.province, loc.city)
                }
            } else {
                _detectedLocation.value = "本地电台"
            }
        }
    }

    fun loadLocalScenarioStations(targetProvince: String?, targetCity: String?) {
        val prov = targetProvince?.trim()
            ?.removeSuffix("省")?.removeSuffix("市")?.removeSuffix("自治区")?.removeSuffix("特别行政区")
        val ct = targetCity?.trim()
            ?.removeSuffix("市")?.removeSuffix("区")?.removeSuffix("县")?.removeSuffix("地区")
            ?.takeIf { it != "全城通用" && it != "全省通用" && it != "全区通用" }

        val resultStations = mutableListOf<RadioStation>()

        if (!ct.isNullOrBlank()) {
            // Level 1: City level (范围由小到大: 最优先展示本地地级市相关广播，如郑州新闻、郑州音乐)
            val cityStations = PresetsData.findStationsByCity(ct)
            resultStations.addAll(cityStations)
        }

        if (!prov.isNullOrBlank()) {
            // Level 2: Province level (其次展示省份广播，如河南广播，严格排除国内其他省市广播)
            val existingIds = resultStations.map { it.id }.toSet()
            val provinceStations = PresetsData.findStationsByProvince(prov).filter { it.id !in existingIds }
            resultStations.addAll(provinceStations)
        }

        // Level 3: Fallback if neither city nor province broadcasts exist
        if (resultStations.isEmpty()) {
            // 如没有郑州且没有河南广播，或没有选择地区，就从国家广播展示 (CNR, CCTV)
            resultStations.addAll(PresetsData.nationalStations)
        }

        val filtered = exclusionManager.filterStations(resultStations)
        _playlist.value = filtered
        if (filtered.isNotEmpty()) {
            playStationAtIndex(0)
            updateSavedPlaylistIfMemoryEnabled()
        }

        // High-availability dedicated stream integration: fetch province dedicated channels and merge redundant lines
        viewModelScope.launch {
            val dedicatedList = if (!prov.isNullOrBlank()) {
                DedicatedSourceManager.getStationsByProvince(prov)
            } else {
                DedicatedSourceManager.getNationalStations()
            }
            if (dedicatedList.isNotEmpty()) {
                val merged = DedicatedSourceManager.mergeWithDedicatedStations(_playlist.value, dedicatedList)
                _playlist.value = exclusionManager.filterStations(merged)
                updateSavedPlaylistIfMemoryEnabled()
            }

            if (!prov.isNullOrBlank()) {
                api.getChinaStationsByProvince(prov, limit = 40).onSuccess { list ->
                    val cleanOnline = exclusionManager.filterStations(list).filter { st ->
                        val text = "${st.name} ${st.tags} ${st.state ?: ""}".lowercase()
                        text.contains(prov.lowercase()) || (!ct.isNullOrBlank() && text.contains(ct.lowercase()))
                    }
                    if (cleanOnline.isNotEmpty()) {
                        val currentList = _playlist.value
                        val currentIds = currentList.map { it.id }.toSet()
                        val toAdd = cleanOnline.filter { it.id !in currentIds }
                        if (toAdd.isNotEmpty()) {
                            val combined = currentList + toAdd
                            val finalMerged = if (dedicatedList.isNotEmpty()) {
                                DedicatedSourceManager.mergeWithDedicatedStations(combined, dedicatedList)
                            } else combined
                            _playlist.value = exclusionManager.filterStations(finalMerged)
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
        }
    }

    fun startRandomRoaming() {
        viewModelScope.launch {
            _selectedScenario.value = "随机漫游"
            _isSearching.value = true

            // Local preset pool shuffled without recently played
            val localPool = PresetsData.allStations.filter { it.id !in recentRoamingIds }.shuffled()
            val initialBatch = (if (localPool.isNotEmpty()) localPool.take(20) else PresetsData.defaultStations).toMutableList()

            _playlist.value = exclusionManager.filterStations(initialBatch)
            if (_playlist.value.isNotEmpty()) {
                playStationAtIndex(0)
            }
            _isSearching.value = false

            // Append online diverse batch ahead of user
            appendRandomRoamingBatch()
        }
    }

    fun appendRandomRoamingBatch() {
        viewModelScope.launch {
            val tagsPool = listOf("流行", "rock", "jazz", "民谣", "ambient", "古典", "chill", "classic", "hits", "cpop")
            val randomTag = tagsPool.random()
            val domestic = exclusionManager.isDomesticOnly.value

            val cloudResult = if (domestic) {
                api.searchStations(randomTag, limit = 40)
            } else {
                api.getStationsByTag(randomTag, limit = 40)
            }

            val cloudList = cloudResult.getOrNull()?.let { exclusionManager.filterStations(it) } ?: emptyList()
            val currentIds = _playlist.value.map { it.id }.toSet()

            // Merge unplayed presets + cloud stations
            val candidatePool = (PresetsData.allStations.filter { it.id !in recentRoamingIds && it.id !in currentIds } +
                    cloudList.filter { it.id !in recentRoamingIds && it.id !in currentIds }).shuffled().take(15)

            if (candidatePool.isNotEmpty()) {
                _playlist.value = _playlist.value + candidatePool
            }
        }
    }

    fun playFavoritesPlaylist(startStation: RadioStation? = null) {
        val favs = favoriteStations.value
        if (favs.isNotEmpty()) {
            _selectedScenario.value = "我的收藏"
            _playlist.value = favs
            val targetIndex = if (startStation != null) {
                favs.indexOfFirst { it.id == startStation.id }.coerceAtLeast(0)
            } else 0
            playStationAtIndex(targetIndex)
        } else {
            _selectedScenario.value = "精选推荐"
            _playlist.value = exclusionManager.filterStations(PresetsData.defaultStations)
            playStationAtIndex(0)
        }
    }

    fun selectScenario(scenario: String) {
        val cleanScenario = scenario.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        _selectedScenario.value = cleanScenario

        if (cleanScenario == "我的收藏" || cleanScenario.contains("收藏")) {
            playFavoritesPlaylist()
            return
        }

        // Check if custom scenario
        val customMatch = _customScenarios.value.find { 
            val cleanTitle = it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
            cleanTitle == cleanScenario || it.id == scenario || it.title == scenario
        }
        if (customMatch != null) {
            if (customMatch.stations.isNotEmpty()) {
                val filtered = exclusionManager.filterStations(customMatch.stations)
                if (filtered.isNotEmpty()) {
                    _playlist.value = filtered
                    playStationAtIndex(0)
                    return
                }
            }
            viewModelScope.launch {
                _isSearching.value = true
                val query = customMatch.query.ifBlank { customMatch.title }.trim()
                // 1. Instant local preset matching with query (guaranteed fast zero-latency response)
                val localMatches = exclusionManager.filterStations(PresetsData.findStationsByKeyword(query))
                if (localMatches.isNotEmpty()) {
                    _playlist.value = localMatches
                    playStationAtIndex(0)
                }

                // 2. Online search with domestic filter respected
                val isDomestic = exclusionManager.isDomesticOnly.value
                api.searchStations(query, isDomestic = isDomestic, limit = 60).onSuccess { list ->
                    val filtered = exclusionManager.filterStations(list)
                    val combined = (localMatches + filtered).distinctBy { it.id }
                    if (combined.isNotEmpty()) {
                        _playlist.value = combined
                        updateSavedPlaylistIfMemoryEnabled()
                        if (localMatches.isEmpty()) {
                            playStationAtIndex(0)
                        }
                    }
                }
                _isSearching.value = false
            }
            return
        }

        // Check if builtin scenario has custom user overrides
        val overriddenList = _scenarioStationOverrides.value[cleanScenario]
        if (!overriddenList.isNullOrEmpty()) {
            val filtered = exclusionManager.filterStations(overriddenList)
            if (filtered.isNotEmpty()) {
                _playlist.value = filtered
                playStationAtIndex(0)
                return
            }
        }

        when {
            cleanScenario.contains("随机漫游") || cleanScenario == "每日发现" -> {
                startRandomRoaming()
            }
            cleanScenario.contains("路况") || cleanScenario.contains("交通") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.trafficStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("交通", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            lower.contains("交通") || lower.contains("路况") || lower.contains("traffic")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("潮流通勤") || cleanScenario.contains("通勤") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.commuteStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("流行", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news") && !lower.contains("助眠") &&
                            !lower.contains("交通") && !lower.contains("路况")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("晚间助眠") || cleanScenario.contains("助眠") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.sleepStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("轻音乐", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news") && !lower.contains("rock") &&
                            !lower.contains("戏曲") && !lower.contains("京剧") && !lower.contains("talk")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("咖啡专注") || cleanScenario.contains("专注") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.focusStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    api.getStationsByTag("jazz", limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news") && !lower.contains("talk")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("资讯谈话") || cleanScenario.contains("实时新闻") || cleanScenario.contains("新闻") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.newsStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("新闻", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list)
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("摇滚") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.rockStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    api.searchStations("rock", limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news") && !lower.contains("talk") &&
                                    !lower.contains("助眠") && !lower.contains("ambient")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("经典") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.classicStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("经典", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("环球") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.worldStations)
                playStationAtIndex(0)
            }
            cleanScenario.contains("本地") -> {
                val customEnabled = _isCustomRegionEnabled.value
                val prov = if (customEnabled) _userSelectedProvince.value else _detectedProvince.value
                val city = if (customEnabled) _userSelectedCity.value else _detectedCity.value
                loadLocalScenarioStations(prov, city)
            }
            else -> {
                // "精选推荐"
                _playlist.value = exclusionManager.filterStations(PresetsData.featuredStations)
                playStationAtIndex(0)
                expandPlaylistWithChinaStations()
            }
        }
    }

    fun selectCountry(country: String) {
        viewModelScope.launch {
            _selectedScenario.value = country
            _isSearching.value = true
            api.getStationsByCountry(country, limit = 100).onSuccess { list ->
                if (list.isNotEmpty()) {
                    _playlist.value = list
                    playStationAtIndex(0)
                }
            }
            _isSearching.value = false
        }
    }

    fun toggleSleepMode() {
        _isSleepModeActive.value = !_isSleepModeActive.value
        if (_isSleepModeActive.value && sleepTimerSeconds.value == null) {
            // Auto start 30 min timer on sleep mode by default
            setSleepTimer(30)
        }
    }

    fun playStationAtIndex(index: Int) {
        val list = _playlist.value
        if (index in list.indices) {
            localRecordingPlayer.stop()
            _currentIndex.value = index
            val station = list[index]
            val isFav = favoritesRepo.isFavorite(station.id)
            val updated = favoritesRepo.applyCustomName(station.copy(isFavorite = isFav))
            player.playStation(updated)
            favoritesRepo.addToHistory(updated)

            // Deduplicate in recent roaming queue
            recentRoamingIds.remove(station.id)
            recentRoamingIds.addLast(station.id)
            if (recentRoamingIds.size > 200) {
                recentRoamingIds.removeFirst()
            }

            // Infinite Roaming: Automatically extend playlist ahead when near end
            if (_selectedScenario.value.contains("随机漫游") && index >= list.size - 4) {
                appendRandomRoamingBatch()
            }

            // Warm up adjacent stations (i-1 and i+1) to slash channel-switch TTFB latency
            val prevStation = list.getOrNull(index - 1)
            val nextStation = list.getOrNull(index + 1)
            player.warmUpAdjacentStations(listOfNotNull(prevStation, nextStation))

            // Save playback memory (station, scenario, playlist, index)
            savePlaybackMemory(updated, _selectedScenario.value, list, index)
        }
    }

    fun prepareStationAtIndex(index: Int) {
        val list = _playlist.value
        if (index in list.indices) {
            _currentIndex.value = index
            val station = list[index]
            val isFav = favoritesRepo.isFavorite(station.id)
            val updated = favoritesRepo.applyCustomName(station.copy(isFavorite = isFav))
            player.prepareStation(updated)
            favoritesRepo.addToHistory(updated)

            val prevStation = list.getOrNull(index - 1)
            val nextStation = list.getOrNull(index + 1)
            player.warmUpAdjacentStations(listOfNotNull(prevStation, nextStation))

            savePlaybackMemory(updated, _selectedScenario.value, list, index)
        }
    }

    fun renameFavoriteStation(stationId: String, newName: String?) {
        favoritesRepo.setCustomStationName(stationId, newName)
        val currentList = _playlist.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == stationId }
        if (index != -1) {
            val updatedStation = favoritesRepo.applyCustomName(currentList[index])
            currentList[index] = updatedStation
            _playlist.value = currentList
            player.updateStationName(stationId, updatedStation.name)
        }
    }

    fun reorderPlaylist(newList: List<RadioStation>) {
        _playlist.value = newList
        favoritesRepo.replaceCustomStations(newList)
        val curStation = playerState.value.currentStation
        if (curStation != null) {
            val newIdx = newList.indexOfFirst { it.id == curStation.id }
            if (newIdx != -1) {
                _currentIndex.value = newIdx
            }
        }
        updateSavedPlaylistIfMemoryEnabled()
    }

    /** 拖动排序：把 fromIndex 处的电台移动到 toIndex 处 */
    fun moveStation(fromIndex: Int, toIndex: Int) {
        val list = _playlist.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        reorderPlaylist(list)
    }

    /** 桌面快捷方式启动：按电台 ID 播放（列表尚未加载时短暂重试） */
    fun playStationById(stationId: String) {
        viewModelScope.launch {
            repeat(15) {
                val station = _playlist.value.firstOrNull { it.id == stationId }
                if (station != null) {
                    playStation(station)
                    return@launch
                }
                delay(200)
            }
        }
    }

    fun playStation(station: RadioStation) {
        val list = _playlist.value.toMutableList()
        var targetIndex = list.indexOfFirst { it.id == station.id }
        if (targetIndex == -1) {
            list.add(station)
            _playlist.value = list
            targetIndex = list.lastIndex
        }
        playStationAtIndex(targetIndex)
    }

    /**
     * Seamlessly adopts the entire explore/search list into the vertical flow pager
     */
    fun playStationFromExplore(station: RadioStation) {
        val exploreList = _exploreStations.value
        if (exploreList.isNotEmpty()) {
            _playlist.value = exploreList
            val targetIndex = exploreList.indexOfFirst { it.id == station.id }
            playStationAtIndex(if (targetIndex >= 0) targetIndex else 0)
        } else {
            playStation(station)
        }
    }

    fun nextStation() {
        val list = _playlist.value
        if (list.isNotEmpty()) {
            if (_selectedScenario.value.contains("随机漫游") && _currentIndex.value >= list.size - 2) {
                appendRandomRoamingBatch()
            }
            val next = (_currentIndex.value + 1) % list.size
            playStationAtIndex(next)
        }
    }

    fun previousStation() {
        val list = _playlist.value
        if (list.isNotEmpty()) {
            val prev = if (_currentIndex.value - 1 < 0) list.size - 1 else _currentIndex.value - 1
            playStationAtIndex(prev)
        }
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    /** 从后台回到前台时，若之前是暂停状态则自动继续播放 */
    fun resumeIfPaused() {
        val state = playerState.value
        if (state.currentStation != null && state.status == PlaybackStatus.PAUSED) {
            player.play()
        }
    }

    fun toggleFavoriteCurrent() {
        val current = playerState.value.currentStation ?: return
        toggleFavorite(current)
    }

    fun toggleFavorite(station: RadioStation) {
        val isNowFav = favoritesRepo.toggleFavorite(station)
        player.updateFavoriteStatus(station.id, isNowFav)
        val list = _playlist.value
        val idx = list.indexOfFirst { it.id == station.id }
        if (idx >= 0) {
            val updated = list.toMutableList()
            updated[idx] = updated[idx].copy(isFavorite = isNowFav)
            _playlist.value = updated
        }
    }

    fun toggleRecording() {
        val current = playerState.value.currentStation ?: return
        if (recorderManager.isRecording.value) {
            recorderManager.stopRecording()
        } else {
            recorderManager.startRecording(current)
        }
    }

    // ================= Music Recognition (听歌识曲) =================
    private val _floatingRecognizedSong = MutableStateFlow<RecognizedSong?>(null)
    val floatingRecognizedSong: StateFlow<RecognizedSong?> = _floatingRecognizedSong.asStateFlow()

    fun showToast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                android.widget.Toast.makeText(getApplication(), message, android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    fun dismissFloatingRecognizedSong() {
        _floatingRecognizedSong.value = null
    }

    fun startSilentRecognition() {
        val curState = recognitionState.value
        if (curState is RecognitionState.Sampling || curState is RecognitionState.Fingerprinting || curState is RecognitionState.QueryingCloud) {
            recognitionManager.resetState()
            showToast("已停止识曲")
            return
        }

        // Clean reset in case of previous Error/NoMatch to avoid instant re-failure
        recognitionManager.resetState()

        val isStationPlaying = playerState.value.status == PlaybackStatus.PLAYING && playerState.value.currentStation != null
        val mode = if (isStationPlaying) {
            RecognitionMode.RADIO_STREAM
        } else {
            showToast("当前广播未播放，正在启用麦克风环境识曲")
            RecognitionMode.MICROPHONE
        }
        _currentRecognitionMode.value = mode
        startMusicRecognition(mode)
    }

    fun openMusicRecognition(preferredMode: RecognitionMode? = null) {
        val mode = preferredMode ?: if (playerState.value.status == PlaybackStatus.PLAYING && playerState.value.currentStation != null) {
            RecognitionMode.RADIO_STREAM
        } else {
            RecognitionMode.MICROPHONE
        }
        _currentRecognitionMode.value = mode
        _showRecognitionDialog.value = true
        startMusicRecognition(mode)
    }

    fun closeMusicRecognition() {
        _showRecognitionDialog.value = false
        recognitionManager.resetState()
    }

    fun startMusicRecognition(mode: RecognitionMode) {
        _currentRecognitionMode.value = mode
        if (mode == RecognitionMode.RADIO_STREAM) {
            val station = playerState.value.currentStation
            if (station != null) {
                recognitionManager.recognizeCurrentStation(station)
            } else {
                _currentRecognitionMode.value = RecognitionMode.MICROPHONE
                recognitionManager.recognizeFromMicrophone()
            }
        } else {
            recognitionManager.recognizeFromMicrophone()
        }
    }

    fun applyRecognizedSongToNowPlaying(song: RecognizedSong) {
        val title = "${song.title} - ${song.artist}"
        player.updateNowPlayingTitle(title)
    }

    fun deleteRecognizedSong(id: String) {
        recognitionManager.deleteSong(id)
    }

    fun clearRecognitionHistory() {
        recognitionManager.clearHistory()
    }

    fun setSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            sleepTimer.cancelTimer()
        } else {
            sleepTimer.startTimer(minutes)
        }
    }

    private fun prewarmExploreCache() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Prewarm "全国精选"
            val initialPresets = exclusionManager.filterStations(PresetsData.defaultStations)
            val nationalDedicated = DedicatedSourceManager.getCachedNationalStations().ifEmpty {
                DedicatedSourceManager.getNationalStations()
            }
            val mergedPresets = if (nationalDedicated.isNotEmpty()) {
                DedicatedSourceManager.mergeWithDedicatedStations(initialPresets, nationalDedicated)
            } else initialPresets
            exploreStationCache["全国精选"] = mergedPresets
            if (_exploreStations.value.isEmpty()) {
                _exploreStations.value = mergedPresets
            }

            // 2. Prewarm top categories from local presets
            val categoriesToPrewarm = listOf(
                "央视伴音" to listOf("CCTV", "央视", "伴音"),
                "国家广播" to listOf("CNR", "CRI", "中国之声", "环球资讯", "华语环球"),
                "热门流行" to listOf("流行", "音乐", "金曲", "潮声", "pop", "cpop"),
                "实时新闻" to listOf("新闻", "资讯", "联播", "权威", "news"),
                "交通出行" to listOf("交通", "路况", "畅通", "交广", "出行", "traffic"),
                "经典老歌" to listOf("经典", "岁月", "典藏", "老歌", "金曲", "oldies"),
                "轻音助眠" to listOf("助眠", "轻音乐", "舒缓", "纯净", "冥想", "ambient", "chill"),
                "流行音乐" to listOf("流行", "音乐", "金曲", "动感", "飞扬", "pop", "cpop"),
                "新闻资讯" to listOf("新闻", "资讯", "权威", "早新闻", "联播", "news"),
                "交通路况" to listOf("交通", "路况", "交广", "畅通", "出行", "traffic"),
                "财经经济" to listOf("财经", "经济", "股市", "商业", "第一财经", "finance"),
                "华语经典" to listOf("经典", "岁月", "典藏", "老歌", "华语", "oldies"),
                "文艺戏剧" to listOf("文艺", "戏曲", "梨园", "剧场", "京剧", "百家"),
                "粤语名声" to listOf("粤语", "南粤", "大湾区", "珠江", "香港", "cantopop")
            )
            for ((catTitle, terms) in categoriesToPrewarm) {
                val matches = exclusionManager.filterStations(PresetsData.findStationsByKeywords(terms))
                val dedicatedMatches = DedicatedSourceManager.searchCachedDedicatedStations(terms)
                val merged = if (dedicatedMatches.isNotEmpty()) {
                    DedicatedSourceManager.mergeWithDedicatedStations(matches, dedicatedMatches)
                } else matches
                if (merged.isNotEmpty()) {
                    exploreStationCache[catTitle] = merged
                }
            }

            // 3. Prewarm user's local province if available
            val userProv = _userSelectedProvince.value ?: _detectedProvince.value
            if (!userProv.isNullOrBlank()) {
                val provMatches = exclusionManager.filterStations(PresetsData.findStationsByProvince(userProv))
                if (provMatches.isNotEmpty()) {
                    exploreStationCache[userProv] = provMatches
                }
            }
        }
    }

    fun loadExploreInitial(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _currentExploreTag.value = "全国精选"

            // Check cache first for 0ms instantaneous load
            if (!forceRefresh) {
                val cached = exploreStationCache["全国精选"]
                if (!cached.isNullOrEmpty()) {
                    _exploreStations.value = cached
                    _isSearching.value = false
                    return@launch
                }
            }

            // Phase 1: Instant local display (< 5ms)
            val initialPresets = exclusionManager.filterStations(PresetsData.defaultStations)
            val nationalDedicated = DedicatedSourceManager.getCachedNationalStations()
            val mergedPresets = if (nationalDedicated.isNotEmpty()) {
                DedicatedSourceManager.mergeWithDedicatedStations(initialPresets, nationalDedicated)
            } else initialPresets

            if (mergedPresets.isNotEmpty()) {
                _exploreStations.value = mergedPresets
                exploreStationCache["全国精选"] = mergedPresets
                _isSearching.value = false
            } else {
                _isSearching.value = true
            }

            // Phase 2: If national dedicated wasn't cached, fetch asynchronously
            if (nationalDedicated.isEmpty() && DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) {
                val freshNational = DedicatedSourceManager.getNationalStations()
                if (freshNational.isNotEmpty()) {
                    val updated = DedicatedSourceManager.mergeWithDedicatedStations(_exploreStations.value, freshNational)
                    _exploreStations.value = updated
                    exploreStationCache["全国精选"] = updated
                    _isSearching.value = false
                }
            }

            // Phase 3: Cloud enrichment (only if mode allows public stations)
            if (DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
                api.getChinaStations(limit = 100).onSuccess { cloudList ->
                    val filtered = exclusionManager.filterStations(cloudList)
                    if (filtered.isNotEmpty()) {
                        val current = _exploreStations.value
                        val existingIds = current.map { it.id }.toSet()
                        val combined = current + filtered.filter { it.id !in existingIds }
                        val finalMerged = if (nationalDedicated.isNotEmpty()) {
                            DedicatedSourceManager.mergeWithDedicatedStations(combined, nationalDedicated)
                        } else combined
                        _exploreStations.value = finalMerged
                        exploreStationCache["全国精选"] = finalMerged
                    }
                }
            }
            _isSearching.value = false
        }
    }

    fun searchByRealTag(tagItem: com.dragon.fm.data.model.RealExploreTagItem) {
        viewModelScope.launch {
            _currentExploreTag.value = tagItem.title
            _radarSearchQuery.value = tagItem.title

            // Check cache first for 0ms instantaneous switch!
            val cached = exploreStationCache[tagItem.title]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }

            when {
                tagItem.isProvince -> {
                    fetchProvinceStations(tagItem.title)
                }
                tagItem.isCountry -> {
                    fetchCountryStations(tagItem.searchTerms.firstOrNull() ?: tagItem.title)
                }
                tagItem.title == "全国精选" -> {
                    loadExploreInitial()
                }
                else -> {
                    // 1. Instant local preset matching with multi-term coverage (guaranteed 0ms result)
                    val localMatches = exclusionManager.filterStations(
                        PresetsData.findStationsByKeywords(tagItem.searchTerms)
                    )
                    val dedicatedMatches = DedicatedSourceManager.searchCachedDedicatedStations(tagItem.searchTerms)
                    val mergedLocal = if (dedicatedMatches.isNotEmpty()) {
                        DedicatedSourceManager.mergeWithDedicatedStations(localMatches, dedicatedMatches)
                    } else localMatches

                    if (mergedLocal.isNotEmpty()) {
                        _exploreStations.value = mergedLocal
                        exploreStationCache[tagItem.title] = mergedLocal
                        // If we already have items, don't block the UI with a full-screen spinner
                        _isSearching.value = false
                    } else {
                        _isSearching.value = true
                    }

                    // 2. Query cloud stations only if mode is HYBRID or PUBLIC_ONLY
                    if (DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
                        val isDomestic = exclusionManager.isDomesticOnly.value
                        api.searchStationsWithTerms(tagItem.searchTerms, isDomestic = isDomestic, limit = 80).onSuccess { cloudList ->
                            val filteredCloud = exclusionManager.filterStations(cloudList)
                            val combined = (mergedLocal + filteredCloud).distinctBy { it.id }
                            val finalMerged = if (dedicatedMatches.isNotEmpty()) {
                                DedicatedSourceManager.mergeWithDedicatedStations(combined, dedicatedMatches)
                            } else combined
                            if (finalMerged.isNotEmpty()) {
                                _exploreStations.value = finalMerged
                                exploreStationCache[tagItem.title] = finalMerged
                            }
                        }
                    }
                    _isSearching.value = false
                }
            }
        }
    }

    fun searchOnline(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            val cacheKey = "query:${query.trim()}"
            val cached = exploreStationCache[cacheKey]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }

            _isSearching.value = true
            val isDomestic = exclusionManager.isDomesticOnly.value
            // Step 1: Instant local preset matching (zero latency display)
            val localMatches = exclusionManager.filterStations(PresetsData.findStationsByKeyword(query))
            if (localMatches.isNotEmpty()) {
                _exploreStations.value = localMatches
                _isSearching.value = false
            }

            // Step 2: Query dedicated high-availability official streams
            val dedicatedMatches = DedicatedSourceManager.searchDedicatedStations(query, _userSelectedProvince.value ?: _detectedProvince.value)
            if (dedicatedMatches.isNotEmpty()) {
                val merged = DedicatedSourceManager.mergeWithDedicatedStations(_exploreStations.value, dedicatedMatches)
                _exploreStations.value = exclusionManager.filterStations(merged)
                _isSearching.value = false
            }

            // Step 3: Query cloud stations with domestic scoping (only if mode allows)
            if (DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
                api.searchStations(query, isDomestic = isDomestic, limit = 100).onSuccess { cloudList ->
                    val filteredCloud = exclusionManager.filterStations(cloudList)
                    val current = _exploreStations.value
                    val combined = (current + filteredCloud).distinctBy { it.id }
                    val finalMerged = if (dedicatedMatches.isNotEmpty()) {
                        DedicatedSourceManager.mergeWithDedicatedStations(combined, dedicatedMatches)
                    } else combined
                    if (finalMerged.isNotEmpty()) {
                        _exploreStations.value = finalMerged
                        exploreStationCache[cacheKey] = finalMerged
                    }
                }.onFailure {
                    if (_exploreStations.value.isEmpty()) {
                        _exploreStations.value = emptyList()
                    }
                }
            }
            if (_exploreStations.value.isNotEmpty()) {
                exploreStationCache[cacheKey] = _exploreStations.value
            }
            _isSearching.value = false
        }
    }

    fun openTagExplore(tagQuery: String) {
        _radarSearchQuery.value = tagQuery
        _currentExploreTag.value = tagQuery
        searchOnline(tagQuery)
    }

    fun clearRadarQuery() {
        _radarSearchQuery.value = ""
    }

    fun fetchTagStations(tag: String) {
        viewModelScope.launch {
            val cached = exploreStationCache[tag]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }
            _isSearching.value = true
            val localMatches = exclusionManager.filterStations(PresetsData.findStationsByKeyword(tag))
            if (localMatches.isNotEmpty()) {
                _exploreStations.value = localMatches
                _isSearching.value = false
            }

            api.getStationsByTag(tag, limit = 100).onSuccess { cloudList ->
                val filteredCloud = exclusionManager.filterStations(cloudList)
                val combined = (localMatches + filteredCloud).distinctBy { it.id }
                if (combined.isNotEmpty()) {
                    _exploreStations.value = combined
                    exploreStationCache[tag] = combined
                }
            }
            _isSearching.value = false
        }
    }

    fun fetchCountryStations(country: String) {
        viewModelScope.launch {
            val cached = exploreStationCache[country]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }
            _isSearching.value = true
            api.getStationsByCountry(country, limit = 100).onSuccess {
                val filtered = exclusionManager.filterStations(it)
                _exploreStations.value = filtered
                exploreStationCache[country] = filtered
            }
            _isSearching.value = false
        }
    }

    fun fetchProvinceStations(province: String) {
        viewModelScope.launch {
            // Check cache first for 0ms switch
            val cached = exploreStationCache[province]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }

            // Step 1: Instant local preset matching for province (< 5ms)
            val localMatches = exclusionManager.filterStations(PresetsData.findStationsByProvince(province))
            val cachedDedicated = DedicatedSourceManager.getCachedStationsByProvince(province)
            val initial = if (cachedDedicated.isNotEmpty()) {
                DedicatedSourceManager.mergeWithDedicatedStations(localMatches, cachedDedicated)
            } else localMatches

            if (initial.isNotEmpty()) {
                _exploreStations.value = initial
                _isSearching.value = false
            } else {
                _isSearching.value = true
            }

            // Step 2: Fetch dedicated official streams for province (domestic, ~100-200ms)
            var currentList = initial
            if (cachedDedicated.isEmpty() && DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) {
                val dedicated = DedicatedSourceManager.getStationsByProvince(province)
                if (dedicated.isNotEmpty()) {
                    currentList = DedicatedSourceManager.mergeWithDedicatedStations(currentList, dedicated)
                    _exploreStations.value = exclusionManager.filterStations(currentList)
                    exploreStationCache[province] = _exploreStations.value
                    _isSearching.value = false
                }
            }

            // Step 3: Query province cloud stations (only if not DEDICATED_ONLY)
            if (DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
                api.getChinaStationsByProvince(province, limit = 80).onSuccess { cloudList ->
                    val filteredCloud = exclusionManager.filterStations(cloudList)
                    val combined = (currentList + filteredCloud).distinctBy { it.id }
                    if (combined.isNotEmpty()) {
                        _exploreStations.value = combined
                        exploreStationCache[province] = combined
                    }
                }
            }
            if (_exploreStations.value.isNotEmpty()) {
                exploreStationCache[province] = _exploreStations.value
            }
            _isSearching.value = false
        }
    }

    fun switchLine(lineIndex: Int) {
        player.switchLine(lineIndex)
        val cur = playerState.value.currentStation ?: return
        val curList = _playlist.value
        val idx = curList.indexOfFirst { it.id == cur.id }
        if (idx != -1) {
            val updated = curList.toMutableList()
            updated[idx] = updated[idx].copy(activeLineIndex = lineIndex)
            _playlist.value = updated
        }
        val exploreList = _exploreStations.value
        val exploreIdx = exploreList.indexOfFirst { it.id == cur.id }
        if (exploreIdx != -1) {
            val updatedExplore = exploreList.toMutableList()
            updatedExplore[exploreIdx] = updatedExplore[exploreIdx].copy(activeLineIndex = lineIndex)
            _exploreStations.value = updatedExplore
        }
    }

    fun retryPlayback() {
        player.retryCurrentStation()
    }

    fun quitApplication(activity: android.app.Activity?) {
        player.stop()
        sleepTimer.cancelTimer()
        recorderManager.stopRecording()
        com.dragon.fm.player.DragonMediaSessionService.stopPlaybackAndService(getApplication())
        activity?.finishAffinity()
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            val list = if (_exploreStations.value.isNotEmpty()) _exploreStations.value else _playlist.value
            com.dragon.fm.player.StationSpeedTester.batchTestStations(list)
            if (_isSortByLatency.value) {
                sortPlaylistByLatency()
            }
        }
    }

    fun runPlaylistSpeedTest() {
        viewModelScope.launch {
            val list = _playlist.value
            com.dragon.fm.player.StationSpeedTester.batchTestStations(list)
            sortPlaylistByLatency()
        }
    }

    fun toggleSortByLatency() {
        val next = !_isSortByLatency.value
        _isSortByLatency.value = next
        if (next) {
            viewModelScope.launch {
                val list = _playlist.value
                com.dragon.fm.player.StationSpeedTester.batchTestStations(list)
                sortPlaylistByLatency()
            }
        }
    }

    fun sortPlaylistByLatency() {
        val lMap = latencies.value
        val sorted = _playlist.value.sortedWith(
            compareBy(
                { val l = lMap[it.id]; if (l != null && l > 0) 0 else 1 },
                { lMap[it.id] ?: Long.MAX_VALUE }
            )
        )
        _playlist.value = sorted
    }

    fun sortExploreByLatency() {
        val lMap = latencies.value
        val sorted = _exploreStations.value.sortedWith(
            compareBy(
                { val l = lMap[it.id]; if (l != null && l > 0) 0 else 1 },
                { lMap[it.id] ?: Long.MAX_VALUE }
            )
        )
        _exploreStations.value = sorted
    }

    fun addCustomStation(name: String, url: String) {
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        if (cleanName.isBlank() || cleanUrl.isBlank()) return

        val station = RadioStation(
            id = "custom-${System.currentTimeMillis()}",
            name = cleanName,
            url = cleanUrl,
            urlResolved = cleanUrl,
            country = "自定义频段",
            state = "自定义",
            tags = "User Stream",
            frequencyMhz = 95.5f
        )
        // 极简版：新增电台固定插到列表最前并立即播放
        val updatedPlaylist = listOf(station) + _playlist.value.filter { it.id != station.id }
        _playlist.value = updatedPlaylist
        favoritesRepo.replaceCustomStations(updatedPlaylist)
        playStationAtIndex(0)
        updateSavedPlaylistIfMemoryEnabled()
    }

    // ================= 极简版设置项 =================
    private val _themeMode = MutableStateFlow(
        prefs.getString("pref_simple_theme_mode", "system") ?: "system"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("pref_simple_theme_mode", mode).apply()
    }

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean("pref_dynamic_color", true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean("pref_dynamic_color", enabled).apply()
    }

    private val _transparentUi = MutableStateFlow(prefs.getBoolean("pref_transparent_ui", false))
    val transparentUi: StateFlow<Boolean> = _transparentUi.asStateFlow()

    fun setTransparentUi(enabled: Boolean) {
        _transparentUi.value = enabled
        prefs.edit().putBoolean("pref_transparent_ui", enabled).apply()
    }

    private val _clickStationToPlay = MutableStateFlow(prefs.getBoolean("pref_click_station_play", true))
    val clickStationToPlay: StateFlow<Boolean> = _clickStationToPlay.asStateFlow()

    fun setClickStationToPlay(enabled: Boolean) {
        _clickStationToPlay.value = enabled
        prefs.edit().putBoolean("pref_click_station_play", enabled).apply()
    }

    private val _largeBuffer = MutableStateFlow(prefs.getBoolean("pref_large_buffer", false))
    val largeBuffer: StateFlow<Boolean> = _largeBuffer.asStateFlow()

    fun setLargeBuffer(enabled: Boolean) {
        _largeBuffer.value = enabled
        prefs.edit().putBoolean("pref_large_buffer", enabled).apply()
        DragonRadioPlayer.useLargeBuffer = enabled
        showToast(if (enabled) "已启用更大缓冲，重启应用后生效" else "已关闭更大缓冲，重启应用后生效")
    }

    private val _editStationEnabled = MutableStateFlow(prefs.getBoolean("pref_edit_station", true))
    val editStationEnabled: StateFlow<Boolean> = _editStationEnabled.asStateFlow()

    fun setEditStationEnabled(enabled: Boolean) {
        _editStationEnabled.value = enabled
        prefs.edit().putBoolean("pref_edit_station", enabled).apply()
    }

    private val _editStreamUrlEnabled = MutableStateFlow(prefs.getBoolean("pref_edit_stream_url", true))
    val editStreamUrlEnabled: StateFlow<Boolean> = _editStreamUrlEnabled.asStateFlow()

    fun setEditStreamUrlEnabled(enabled: Boolean) {
        _editStreamUrlEnabled.value = enabled
        prefs.edit().putBoolean("pref_edit_stream_url", enabled).apply()
    }

    // 电台图标刷新版本号（用于强制重新加载图标）
    private val _iconRefreshVersion = MutableStateFlow(0)
    val iconRefreshVersion: StateFlow<Int> = _iconRefreshVersion.asStateFlow()

    fun refreshStationIcons() {
        _iconRefreshVersion.value += 1
        showToast("已刷新电台图标")
    }

    // 曲阳收音机：LXFM 主题配色选择
    private val _simpleThemeId = MutableStateFlow(
        prefs.getString("pref_simple_theme_id", "lunar_light") ?: "lunar_light"
    )
    val simpleThemeId: StateFlow<String> = _simpleThemeId.asStateFlow()

    fun setSimpleThemeId(themeId: String) {
        _simpleThemeId.value = themeId
        prefs.edit().putString("pref_simple_theme_id", themeId).apply()
    }

    // 记忆播放列表与播放进度
    private val _playbackMemory = MutableStateFlow(prefs.getBoolean("pref_playback_memory", true))
    val playbackMemory: StateFlow<Boolean> = _playbackMemory.asStateFlow()

    fun setPlaybackMemory(enabled: Boolean) {
        _playbackMemory.value = enabled
        prefs.edit().putBoolean("pref_playback_memory", enabled).apply()
        if (!enabled) {
            prefs.edit()
                .remove("saved_last_station")
                .remove("saved_last_playlist")
                .remove("saved_last_scenario")
                .remove("saved_last_index")
                .apply()
        }
    }

    // 0.25s 极速起播优化
    private val _fastStart = MutableStateFlow(prefs.getBoolean("pref_fast_start", true))
    val fastStart: StateFlow<Boolean> = _fastStart.asStateFlow()

    fun setFastStart(enabled: Boolean) {
        _fastStart.value = enabled
        prefs.edit().putBoolean("pref_fast_start", enabled).apply()
        DragonRadioPlayer.fastStart = enabled
        showToast(if (enabled) "已启用极速起播，重启应用后生效" else "已关闭极速起播，重启应用后生效")
    }

    // 拔出耳机 / 蓝牙断开自动暂停
    private val _pauseOnUnplug = MutableStateFlow(prefs.getBoolean("pref_pause_on_unplug", true))
    val pauseOnUnplug: StateFlow<Boolean> = _pauseOnUnplug.asStateFlow()

    fun setPauseOnUnplug(enabled: Boolean) {
        _pauseOnUnplug.value = enabled
        prefs.edit().putBoolean("pref_pause_on_unplug", enabled).apply()
        DragonRadioPlayer.pauseOnHeadsetUnplug = enabled
    }

    /** 更新电台台标（从相册选择图片后复制到应用私有目录） */
    fun updateStationLogo(stationId: String, uri: Uri) {
        val app = getApplication<Application>()
        try {
            val dir = File(app.filesDir, "station_logos").apply { mkdirs() }
            val target = File(dir, stationId.hashCode().toString() + ".img")
            app.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                showToast("无法读取所选图片")
                return
            }

            val list = _playlist.value.toMutableList()
            val index = list.indexOfFirst { it.id == stationId }
            if (index < 0) return
            val updated = list[index].copy(favicon = target.absolutePath)
            list[index] = updated
            _playlist.value = list
            favoritesRepo.replaceCustomStations(list)
            updateSavedPlaylistIfMemoryEnabled()
            showToast("台标已更新")
        } catch (_: Exception) {
            showToast("台标更新失败")
        }
    }

    // ================= 设置页搜索电台 =================
    private val _stationSearchResults = MutableStateFlow<List<RadioStation>>(emptyList())
    val stationSearchResults: StateFlow<List<RadioStation>> = _stationSearchResults.asStateFlow()

    private val _isStationSearching = MutableStateFlow(false)
    val isStationSearching: StateFlow<Boolean> = _isStationSearching.asStateFlow()

    private var stationSearchJob: kotlinx.coroutines.Job? = null

    fun searchStationsForAdd(query: String) {
        stationSearchJob?.cancel()
        val keyword = query.trim()
        if (keyword.isBlank()) {
            _stationSearchResults.value = emptyList()
            _isStationSearching.value = false
            return
        }
        stationSearchJob = viewModelScope.launch {
            _isStationSearching.value = true
            val localMatches = exclusionManager.filterStations(PresetsData.findStationsByKeyword(keyword))
            if (localMatches.isNotEmpty()) {
                _stationSearchResults.value = localMatches
            }
            val remote = try {
                api.searchStations(keyword, isDomestic = true, limit = 60).getOrNull().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            val merged = (localMatches + exclusionManager.filterStations(remote))
                .distinctBy { it.id }
                .take(80)
            _stationSearchResults.value = merged
            _isStationSearching.value = false
        }
    }

    /** 把搜索结果中的电台加入播放主界面列表 */
    fun addStationToPlaylist(station: RadioStation, playNow: Boolean = false) {
        val list = _playlist.value
        val existingIndex = list.indexOfFirst { it.id == station.id }
        if (existingIndex >= 0) {
            if (playNow) playStationAtIndex(existingIndex) else showToast("该电台已在播放列表中")
            return
        }
        val added = favoritesRepo.applyCustomName(station)
        val newList = list.toMutableList()
        newList.add(0, added)
        _playlist.value = newList
        favoritesRepo.replaceCustomStations(newList)
        updateSavedPlaylistIfMemoryEnabled()
        if (playNow) playStationAtIndex(0) else showToast("已添加：${added.name}")
    }

    /** 从播放主界面移除电台 */
    fun removeStationFromPlaylist(stationId: String) {
        val list = _playlist.value
        val index = list.indexOfFirst { it.id == stationId }
        if (index < 0) return
        val currentId = playerState.value.currentStation?.id
        val newList = list.toMutableList().also { it.removeAt(index) }
        _playlist.value = newList
        favoritesRepo.replaceCustomStations(newList)
        when {
            newList.isEmpty() -> {
                _currentIndex.value = 0
                player.pause()
            }
            currentId == stationId -> playStationAtIndex(index.coerceAtMost(newList.lastIndex))
            else -> {
                val newIdx = newList.indexOfFirst { it.id == currentId }
                if (newIdx >= 0) _currentIndex.value = newIdx
            }
        }
        updateSavedPlaylistIfMemoryEnabled()
        showToast("已移除电台")
    }

    /** 编辑电台名称 / 流媒体地址 */
    fun updateStationInfo(stationId: String, newName: String, newUrl: String) {
        val list = _playlist.value.toMutableList()
        val index = list.indexOfFirst { it.id == stationId }
        if (index < 0) return
        val old = list[index]
        val name = newName.trim().ifBlank { old.name }
        val url = newUrl.trim().ifBlank { old.url }
        val updated = old.copy(
            name = name,
            url = url,
            urlResolved = url,
            lines = emptyList(),
            activeLineIndex = 0,
            hasDedicatedLine = false
        )
        list[index] = updated
        _playlist.value = list
        favoritesRepo.replaceCustomStations(list)
        favoritesRepo.setCustomStationName(stationId, name)
        if (playerState.value.currentStation?.id == stationId) {
            player.updateStationName(stationId, name)
            if (old.playUrl != updated.playUrl) player.playStation(updated)
        }
        updateSavedPlaylistIfMemoryEnabled()
        showToast("已保存电台信息")
    }

    // ================= M3U 导入 / 导出、备份 / 恢复 =================
    fun importM3uPlaylist(content: String): Int {
        val entries = M3uPlaylistIO.parse(content)
        if (entries.isEmpty()) return 0

        val added = mutableListOf<RadioStation>()
        entries.forEach { entry ->
            val stableId = "m3u-" + Integer.toHexString(
                (entry.url.trim().lowercase() + "|" + entry.name).hashCode()
            )
            if (_playlist.value.any { it.id == stableId }) return@forEach
            val station = RadioStation(
                id = stableId,
                name = entry.name,
                url = entry.url,
                urlResolved = entry.url,
                favicon = entry.logo?.takeIf { it.isNotBlank() },
                country = "M3U导入",
                state = "M3U",
                tags = "M3U",
                codec = "MP3",
                bitrate = 128
            )
            added.add(station)
        }

        if (added.isNotEmpty()) {
            val addedIds = added.map { it.id }.toSet()
            val updatedPlaylist = added + _playlist.value.filter { it.id !in addedIds }
            _playlist.value = updatedPlaylist
            favoritesRepo.replaceCustomStations(updatedPlaylist)
            updateSavedPlaylistIfMemoryEnabled()
        }
        return added.size
    }

    fun buildM3uPlaylist(): String = M3uPlaylistIO.build(_playlist.value)

    fun buildBackupJson(): String? = try {
        json.encodeToString(_playlist.value)
    } catch (_: Exception) {
        null
    }

    fun restoreFromBackupJson(content: String): Int {
        val restored = try {
            json.decodeFromString<List<RadioStation>>(content)
        } catch (_: Exception) {
            return -1
        }
        if (restored.isEmpty()) return 0
        _playlist.value = restored
        favoritesRepo.replaceCustomStations(restored)
        playStationAtIndex(0)
        updateSavedPlaylistIfMemoryEnabled()
        return restored.size
    }

    // ================= WebDAV 在线备份 =================
    fun webDavUrl(): String = prefs.getString("pref_webdav_url", "") ?: ""
    fun webDavUser(): String = prefs.getString("pref_webdav_user", "") ?: ""
    fun webDavPassword(): String = prefs.getString("pref_webdav_pass", "") ?: ""
    fun webDavFileName(): String =
        prefs.getString("pref_webdav_file", "quyang_backup.zip") ?: "quyang_backup.zip"

    fun saveWebDavConfig(url: String, user: String, password: String, fileName: String) {
        prefs.edit()
            .putString("pref_webdav_url", url.trim())
            .putString("pref_webdav_user", user.trim())
            .putString("pref_webdav_pass", password)
            .putString("pref_webdav_file", fileName.trim().ifBlank { "quyang_backup.zip" })
            .apply()
    }

    fun settingsSnapshot(): Map<String, Any?> =
        BackupManager.SETTINGS_KEYS.associateWith { prefs.all[it] }

    /** 打包播放列表 + 设置 + 本地台标 */
    fun buildBackupZip(): File? = try {
        BackupManager.createBackup(getApplication(), _playlist.value, settingsSnapshot())
    } catch (_: Exception) {
        null
    }

    /** 从 WebDAV 下载的备份 zip 恢复（返回恢复的电台数量） */
    suspend fun restoreBackupZip(zipFile: File): Int = withContext(Dispatchers.IO) {
        val result = BackupManager.restoreBackup(getApplication(), zipFile)
        withContext(Dispatchers.Main) {
            _playlist.value = result.stations
            favoritesRepo.replaceCustomStations(result.stations)
            applyRestoredSettings(result.settings)
            if (result.stations.isNotEmpty()) {
                if (_isAutoPlayOnLaunch.value) playStationAtIndex(0) else prepareStationAtIndex(0)
            } else {
                _currentIndex.value = 0
                player.pause()
            }
            updateSavedPlaylistIfMemoryEnabled()
        }
        result.stations.size
    }

    private fun applyRestoredSettings(settings: Map<String, Any?>) {
        fun bool(key: String): Boolean? = when (val v = settings[key]) {
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true)
            else -> null
        }
        fun text(key: String): String? = settings[key]?.toString()

        text("pref_simple_theme_id")?.let { setSimpleThemeId(it) }
        bool("pref_dynamic_color")?.let { setDynamicColor(it) }
        bool("pref_transparent_ui")?.let { setTransparentUi(it) }
        bool("pref_click_station_play")?.let { setClickStationToPlay(it) }
        bool("pref_edit_station")?.let { setEditStationEnabled(it) }
        bool("pref_edit_stream_url")?.let { setEditStreamUrlEnabled(it) }
        bool("pref_auto_play_on_launch")?.let { setAutoPlayOnLaunch(it) }
        bool("pref_playback_memory")?.let { setPlaybackMemory(it) }
        bool("pref_fast_start")?.let { setFastStart(it) }
        bool("pref_large_buffer")?.let { setLargeBuffer(it) }
        bool("pref_pause_on_unplug")?.let { setPauseOnUnplug(it) }
        bool("pref_auto_update_enabled")?.let { setAutoUpdateEnabled(it) }

        prefs.edit().apply {
            text("pref_webdav_url")?.let { putString("pref_webdav_url", it) }
            text("pref_webdav_user")?.let { putString("pref_webdav_user", it) }
            text("pref_webdav_file")?.let { putString("pref_webdav_file", it) }
        }.apply()
    }

    fun deleteRecordedFile(file: RecordedFile) {
        if (playingRecordedFile.value?.file?.absolutePath == file.file.absolutePath) {
            localRecordingPlayer.stop()
        }
        recorderManager.deleteRecording(file.file)
    }

    fun playRecordedFile(file: RecordedFile) {
        player.pause()
        localRecordingPlayer.playFile(file)
    }

    fun toggleRecordedFilePlayPause() {
        localRecordingPlayer.togglePlayPause()
    }

    fun seekRecordedFile(posMs: Long) {
        localRecordingPlayer.seekTo(posMs)
    }

    fun setRecordedFileSpeed(speed: Float) {
        localRecordingPlayer.setSpeed(speed)
    }

    fun cycleRecordedFileSpeed(): Float {
        return localRecordingPlayer.cycleSpeed()
    }

    fun stopRecordedFile() {
        localRecordingPlayer.stop()
    }

    fun refreshRecordings() {
        recorderManager.refreshRecordings()
    }

    override fun onCleared() {
        super.onCleared()
        localRecordingPlayer.release()
    }
}
