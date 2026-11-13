package com.dragon.fm.player

import android.Manifest
import android.animation.ValueAnimator
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.PlayerState
import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DragonRadioPlayer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // High performance OkHttpClient with warm connection pool, resilient timeouts, and auto-redirect caching
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS) // HLS 分片偶发慢时不要过早中断
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(12, 60, TimeUnit.SECONDS))
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val req = chain.request()
            val resp = chain.proceed(req)
            val originalUrl = req.url.toString()
            val finalUrl = resp.request.url.toString()
            if (finalUrl != originalUrl && resp.isSuccessful) {
                resolvedUrlCache[originalUrl] = finalUrl
                Log.d(TAG, "[RedirectInterceptor] Auto-cached redirect: $originalUrl -> $finalUrl")
            }
            resp
        }
        .build()

    private val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        .setUserAgent("LXFM/1.0.0 (Linux; Android; ExoPlayer)")
        .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

    // 强制选择最高码率 / 最高清晰度轨道（HLS 多码率时取最高档）
    private val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(
            buildUponParameters()
                .setForceHighestSupportedBitrate(true)
                .setExceedAudioConstraintsIfNecessary(true)
                .setMaxAudioBitrate(Integer.MAX_VALUE)
                .setMaxVideoBitrate(Integer.MAX_VALUE)
                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                .build()
        )
    }

    // 允许解码器回退，并优先使用扩展渲染器（若安装了扩展）
    private val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

    // 加大缓冲，缓解 HLS 直播卡顿（电台音频码率低，内存占用可控）
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            if (useLargeBuffer) 20000 else 8000,    // minBufferMs：始终保持 8~20 秒待播数据
            if (useLargeBuffer) 180000 else 60000,  // maxBufferMs
            if (useLargeBuffer) 2000 else if (fastStart) 500 else 1500, // 起播阈值
            if (useLargeBuffer) 8000 else 4000      // 重缓冲后继续播放阈值
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(0, false) // 0 backbuffer saves memory on continuous radio
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory)
        )
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(false)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // Pre-resolved redirect URL cache
    private val resolvedUrlCache = ConcurrentHashMap<String, String>()

    private var currentSessionId = 0L
    private var retryCount = 0
    private var retryJob: Job? = null
    private var timeoutWatchdogJob: Job? = null
    private var warmUpJob: Job? = null
    private var volumeAnimator: ValueAnimator? = null
    private var activeStationId: String? = null
    private var playStartTimeMs: Long = 0L
    private var lastAudioBecomingNoisyAtMs: Long = 0L
    private var bluetoothReconnectMonitorJob: Job? = null
    private var debugBluetoothResumeDevices: List<BluetoothAudioDevice>? = null

    /** 拔出耳机 / 断开蓝牙时，根据设置决定是否自动暂停 */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && pauseOnHeadsetUnplug) {
                lastAudioBecomingNoisyAtMs = SystemClock.elapsedRealtime()
                val activeBluetoothDevices = bluetoothOutputDevices()
                if (activeBluetoothDevices.isNotEmpty()) {
                    bluetoothResumePolicy.rememberDisconnected(
                        activeBluetoothDevices.map { it.toResumeDevice() },
                        lastAudioBecomingNoisyAtMs
                    )
                    ensureBluetoothReconnectMonitor()
                }
                pauseInternal(clearBluetoothResume = false)
            }
        }
    }

    private val bluetoothProfileReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            if (!pauseOnHeadsetUnplug || intent == null) return
            val device = intent.bluetoothDeviceExtra() ?: return
            val aclConnected = intent.action == BluetoothDevice.ACTION_ACL_CONNECTED
            val aclDisconnected = intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED
            val type = when (intent.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED ->
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                else -> return
            }
            val state = intent.getIntExtra(
                BluetoothProfile.EXTRA_STATE,
                BluetoothProfile.STATE_DISCONNECTED
            )
            val resumeDevice = device.toResumeDevice(type)
            when {
                aclConnected || state == BluetoothProfile.STATE_CONNECTED -> {
                    scope.launch {
                        delay(if (aclConnected) 1_000L else 400L)
                        handleBluetoothDevicesAdded(listOf(resumeDevice))
                    }
                }
                aclDisconnected || state == BluetoothProfile.STATE_DISCONNECTED -> {
                    handleBluetoothDevicesRemoved(listOf(resumeDevice))
                }
            }
        }
    }

    /** 蓝牙断开时是否由我们主动暂停（用于重连后自动续播） */
    private val bluetoothResumePolicy = BluetoothResumePolicy()

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
    private var a2dpProfile: BluetoothA2dp? = null
    private var headsetProfile: BluetoothHeadset? = null
    private val bluetoothProfileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProfile = proxy as? BluetoothA2dp
                BluetoothProfile.HEADSET -> headsetProfile = proxy as? BluetoothHeadset
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProfile = null
                BluetoothProfile.HEADSET -> headsetProfile = null
            }
        }
    }

    private fun AudioDeviceInfo.isBluetoothAudio(): Boolean {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER
            } else {
                false
            }
        }
    }

    private fun AudioDeviceInfo.toResumeDevice() = BluetoothAudioDevice(
        id = id,
        type = type,
        productName = productName.toString(),
        address = try {
            address.orEmpty()
        } catch (_: Exception) {
            ""
        },
    )

    private fun Intent.bluetoothDeviceExtra(): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun bluetoothOutputDevices(): List<AudioDeviceInfo> {
        return try {
            audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                ?.filter { it.isBluetoothAudio() }
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun BluetoothDevice.toResumeDevice(type: Int): BluetoothAudioDevice {
        if (!hasBluetoothConnectPermission()) {
            return BluetoothAudioDevice(id = -1, type = type, productName = "")
        }
        val safeAddress = runCatching { address.orEmpty() }.getOrDefault("")
        val safeName = runCatching { name.orEmpty() }.getOrDefault("")
        return BluetoothAudioDevice(
            id = if (safeAddress.isBlank()) -1 else safeAddress.hashCode(),
            type = type,
            productName = safeName,
            address = safeAddress,
        )
    }

    private fun ensureBluetoothProfileProxies() {
        if (!hasBluetoothConnectPermission()) return
        val adapter = bluetoothAdapter ?: return
        if (a2dpProfile == null) {
            runCatching { adapter.getProfileProxy(context, bluetoothProfileListener, BluetoothProfile.A2DP) }
        }
        if (headsetProfile == null) {
            runCatching { adapter.getProfileProxy(context, bluetoothProfileListener, BluetoothProfile.HEADSET) }
        }
    }

    private fun connectedBluetoothProfileDevices(): List<BluetoothAudioDevice> {
        if (bluetoothAdapter == null || !hasBluetoothConnectPermission()) return emptyList()
        ensureBluetoothProfileProxies()
        return try {
            buildList {
                a2dpProfile?.connectedDevices.orEmpty().forEach { device ->
                    add(device.toResumeDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
                }
                headsetProfile?.connectedDevices.orEmpty().forEach { device ->
                    add(device.toResumeDevice(AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
                }
            }.distinctBy { it.deviceKey() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun currentBluetoothResumeDevices(): List<BluetoothAudioDevice> {
        debugBluetoothResumeDevices?.let { return it }
        return (bluetoothOutputDevices().map { it.toResumeDevice() } + connectedBluetoothProfileDevices())
            .distinctBy { it.deviceKey() }
    }

    private fun BluetoothAudioDevice.deviceKey(): String {
        return when {
            address.isNotBlank() -> "address:$address"
            productName.isNotBlank() -> "name:$type:$productName"
            id >= 0 -> "id:$id"
            else -> "type:$type"
        }
    }

    private fun isPlaybackActive(): Boolean {
        return exoPlayer.playWhenReady || _playerState.value.status == PlaybackStatus.PLAYING
    }

    fun isAwaitingBluetoothReconnect(): Boolean {
        return bluetoothResumePolicy.hasPending(SystemClock.elapsedRealtime())
    }

    internal fun setDebugBluetoothResumeDevices(devices: List<BluetoothAudioDevice>?) {
        debugBluetoothResumeDevices = devices
    }

    private fun clearBluetoothResume() {
        bluetoothReconnectMonitorJob?.cancel()
        bluetoothReconnectMonitorJob = null
        bluetoothResumePolicy.clear()
    }

    private fun ensureBluetoothReconnectMonitor() {
        if (bluetoothReconnectMonitorJob?.isActive == true) return
        bluetoothReconnectMonitorJob = scope.launch {
            val startedAtMs = SystemClock.elapsedRealtime()
            var firstPoll = true
            var bluetoothRouteGapSeen = !isLegacyBluetoothRouteActive()
            while (bluetoothResumePolicy.hasPending(SystemClock.elapsedRealtime())) {
                val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs
                val pollDelayMs = when {
                    firstPoll -> 1_500L
                    elapsedMs < 30_000L -> 1_000L
                    elapsedMs < 5 * 60_000L -> 3_000L
                    else -> 5_000L
                }
                delay(pollDelayMs)
                firstPoll = false

                if (!pauseOnHeadsetUnplug) {
                    clearBluetoothResume()
                    DragonMediaSessionService.requestNotificationUpdate()
                    return@launch
                }

                val resumedDevices = debugBluetoothResumeDevices
                    ?: bluetoothOutputDevices().map { it.toResumeDevice() }
                if (resumedDevices.isNotEmpty()) {
                    Log.i(TAG, "[Bluetooth] 轮询检测到蓝牙输出恢复，尝试续播")
                    if (handleBluetoothDevicesAdded(resumedDevices)) return@launch
                }
                val routeActive = isLegacyBluetoothRouteActive()
                if (!routeActive) {
                    bluetoothRouteGapSeen = true
                } else if (
                    bluetoothRouteGapSeen &&
                    bluetoothResumePolicy.consumeRouteRecovery(SystemClock.elapsedRealtime())
                ) {
                    Log.i(TAG, "[Bluetooth] 兼容路由状态恢复，自动继续播放")
                    play()
                    return@launch
                }
            }
            clearBluetoothResume()
            DragonMediaSessionService.requestNotificationUpdate()
        }
    }

    @Suppress("DEPRECATION")
    private fun isLegacyBluetoothRouteActive(): Boolean {
        return try {
            audioManager?.isBluetoothA2dpOn == true || audioManager?.isBluetoothScoOn == true
        } catch (_: Exception) {
            false
        }
    }

    internal fun handleBluetoothDevicesRemoved(
        removedDevices: Collection<BluetoothAudioDevice>,
        nowMs: Long = SystemClock.elapsedRealtime(),
    ) {
        if (!pauseOnHeadsetUnplug || removedDevices.isEmpty()) return
        if (isPlaybackActive()) {
            Log.i(TAG, "[Bluetooth] 蓝牙音频输出断开，自动暂停")
            bluetoothResumePolicy.rememberDisconnected(removedDevices, nowMs)
                ensureBluetoothReconnectMonitor()
            pauseInternal(clearBluetoothResume = false)
                DragonMediaSessionService.requestNotificationUpdate()
        } else {
            // ACTION_AUDIO_BECOMING_NOISY may arrive first and pause playback.
            if (nowMs - lastAudioBecomingNoisyAtMs <= NOISY_CALLBACK_COALESCE_MS) {
                bluetoothResumePolicy.rememberDisconnected(removedDevices, nowMs)
            } else {
                bluetoothResumePolicy.mergeDisconnected(removedDevices, nowMs)
            }
                ensureBluetoothReconnectMonitor()
            DragonMediaSessionService.requestNotificationUpdate()
        }
    }

    internal fun handleBluetoothDevicesAdded(
        addedDevices: Collection<BluetoothAudioDevice>,
        nowMs: Long = SystemClock.elapsedRealtime(),
    ): Boolean {
        if (!pauseOnHeadsetUnplug) {
            clearBluetoothResume()
            return false
        }
        if (addedDevices.isEmpty()) return false
        val shouldResume = bluetoothResumePolicy.consumeReconnect(addedDevices, nowMs)
        if (!shouldResume) return false
        Log.i(TAG, "[Bluetooth] 蓝牙已重新连接，自动继续播放")
        play()
        return true
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            handleBluetoothDevicesRemoved(
                removedDevices
                    .filter { it.isBluetoothAudio() }
                    .map { it.toResumeDevice() }
            )
        }

        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            handleBluetoothDevicesAdded(
                addedDevices
                    .filter { it.isBluetoothAudio() }
                    .map { it.toResumeDevice() }
            )
        }
    }

    init {
        setupPlayerListener()
        try {
            ContextCompat.registerReceiver(
                context,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {
            // Ignore registration failures on restricted ROMs
        }
        try {
            ContextCompat.registerReceiver(
                context,
                bluetoothProfileReceiver,
                IntentFilter().apply {
                    addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                    addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {
            // Bluetooth profile broadcasts may require nearby-device permission.
        }
        try {
            audioManager?.registerAudioDeviceCallback(
                audioDeviceCallback,
                Handler(Looper.getMainLooper())
            )
        } catch (_: Exception) {
            // Ignore registration failures on restricted ROMs
        }
    }

    private fun cancelVolumeAnimation() {
        volumeAnimator?.removeAllListeners()
        volumeAnimator?.cancel()
        volumeAnimator = null
    }

    private fun isEventStale(mediaItem: MediaItem? = exoPlayer.currentMediaItem): Boolean {
        val targetId = activeStationId ?: return true
        val eventId = mediaItem?.mediaId ?: return true
        return eventId != targetId
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (isEventStale()) return
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        if (exoPlayer.playWhenReady) {
                            _playerState.value = _playerState.value.copy(
                                status = PlaybackStatus.BUFFERING,
                                errorMessage = null
                            )
                            val current = _playerState.value.currentStation
                            if (current != null && timeoutWatchdogJob == null) {
                                startTimeoutWatchdog(currentSessionId, current, isMidStream = true)
                            }
                        }
                    }
                    Player.STATE_READY -> {
                        timeoutWatchdogJob?.cancel()
                        timeoutWatchdogJob = null
                        retryJob?.cancel()
                        retryJob = null
                        retryCount = 0

                        val isPlaying = exoPlayer.playWhenReady
                        val elapsed = SystemClock.elapsedRealtime() - playStartTimeMs
                        val currentStation = _playerState.value.currentStation
                        val currentStationName = currentStation?.name ?: "Unknown"
                        Log.i(TAG, "[FirstAudioReady] Station: $currentStationName, TTFB/FirstFrame: ${elapsed}ms")

                        currentStation?.id?.let {
                            com.dragon.fm.data.local.StationHealthManager.markHealthy(it)
                        }

                        _playerState.value = _playerState.value.copy(
                            status = if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED,
                            errorMessage = null
                        )

                        // Smooth anti-pop fade in when playback begins (guard against duplicate runs)
                        if (isPlaying && (volumeAnimator == null || !volumeAnimator!!.isRunning)) {
                            fadeInVolume()
                        }
                    }
                    Player.STATE_ENDED -> {
                        timeoutWatchdogJob?.cancel()
                        timeoutWatchdogJob = null
                        _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                    }
                    Player.STATE_IDLE -> {
                        if (_playerState.value.status != PlaybackStatus.ERROR &&
                            _playerState.value.status != PlaybackStatus.BUFFERING) {
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.IDLE)
                        }
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (isEventStale()) return
                Log.d(TAG, "[Player] onPlayWhenReadyChanged: playWhenReady=$playWhenReady, reason=$reason, state=${exoPlayer.playbackState}")
                if (!playWhenReady) {
                    timeoutWatchdogJob?.cancel()
                    timeoutWatchdogJob = null
                    retryJob?.cancel()
                    retryJob = null
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                } else {
                    when (exoPlayer.playbackState) {
                        Player.STATE_READY -> {
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.PLAYING)
                            if (exoPlayer.volume < 1.0f && (volumeAnimator == null || !volumeAnimator!!.isRunning)) {
                                fadeInVolume()
                            }
                        }
                        Player.STATE_BUFFERING -> {
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.BUFFERING)
                            val current = _playerState.value.currentStation
                            if (current != null && timeoutWatchdogJob == null) {
                                startTimeoutWatchdog(currentSessionId, current, isMidStream = true)
                            }
                        }
                        Player.STATE_IDLE -> {
                            val current = _playerState.value.currentStation
                            if (current != null) {
                                playStation(current)
                            }
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isEventStale()) return
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    _playerState.value = _playerState.value.copy(
                        status = if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
                    )
                    if (isPlaying && exoPlayer.volume < 1.0f && (volumeAnimator == null || !volumeAnimator!!.isRunning)) {
                        fadeInVolume()
                    }
                } else if (!isPlaying && !exoPlayer.playWhenReady) {
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                if (isEventStale()) return
                val title = mediaMetadata.title?.toString()
                    ?: mediaMetadata.displayTitle?.toString()
                    ?: mediaMetadata.artist?.toString().orEmpty()
                if (title.isNotBlank()) {
                    _playerState.value = _playerState.value.copy(nowPlayingTitle = title)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (isEventStale()) return
                val current = _playerState.value.currentStation ?: return
                if (current.id != activeStationId) return

                timeoutWatchdogJob?.cancel()
                timeoutWatchdogJob = null
                retryJob?.cancel()

                // Fast recovery for HLS live window expiration (resync directly to live edge)
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    Log.i(TAG, "[SelfHealing] Behind live window on ${current.name}, seeking to live edge")
                    try {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                        return
                    } catch (_: Exception) {}
                }

                // Evict potentially stale or invalid redirect URL from cache
                resolvedUrlCache.remove(current.playUrl)

                // 1. Multi-line failover: if current line failed and alternative lines exist, failover immediately
                val activeLineIdx = current.activeLineIndex
                val lines = current.effectiveLines
                if (lines.size > 1 && (activeLineIdx + 1) < lines.size) {
                    val nextIdx = activeLineIdx + 1
                    val nextLine = lines[nextIdx]
                    Log.w(TAG, "[Failover] Line $activeLineIdx failed on ${current.name}, auto-switching to line $nextIdx (${nextLine.name})")
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.BUFFERING,
                        nowPlayingTitle = "正在无感切换备用线路 (${nextLine.name})..."
                    )
                    val switched = current.copy(activeLineIndex = nextIdx)
                    playStation(switched, isRetry = true)
                    return
                }

                if (retryCount < MAX_AUTO_RETRIES) {
                    retryCount++
                    Log.w(TAG, "[SelfHealing] Error on ${current.name} (${error.errorCodeName}), auto-retry $retryCount/$MAX_AUTO_RETRIES in 700ms")
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.BUFFERING,
                        nowPlayingTitle = "网络波动，正在重试连接 ($retryCount/$MAX_AUTO_RETRIES)..."
                    )
                    val expectedSessionId = currentSessionId
                    retryJob = scope.launch {
                        delay(700L)
                        if (expectedSessionId == currentSessionId && activeStationId == current.id) {
                            playStation(current, isRetry = true)
                        }
                    }
                } else {
                    Log.e(TAG, "[SelfHealing] Station ${current.name} retries exhausted, marking error: ${error.message}")
                    com.dragon.fm.data.local.StationHealthManager.markUnhealthy(current.id)
                    try {
                        exoPlayer.stop()
                    } catch (_: Exception) {}
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "该电台暂时无法连接，可点击重试或切换其他频道"
                    )
                }
            }
        })
    }

    companion object {
        private const val TAG = "DragonPlayer"
        private const val MAX_AUTO_RETRIES = 2
        private const val NOISY_CALLBACK_COALESCE_MS = 1_500L
        var onAutoSkipListener: (() -> Unit)? = null

        /** 极简版设置：“使用更大缓冲”，需在创建播放器前设置，重启后生效 */
        var useLargeBuffer: Boolean = false

        /** “0.25s 极速起播优化”，需在创建播放器前设置，重启后生效 */
        var fastStart: Boolean = true

        /** 拔出耳机 / 蓝牙断开时是否自动暂停（实时生效） */
        var pauseOnHeadsetUnplug: Boolean = true
    }

    /**
     * Pre-resolves DNS and HTTP redirects for upcoming or adjacent radio stations
     * to eliminate TTFB delays when swiping channels.
     * Uses debounce and job cancellation to prevent network saturation during rapid swiping.
     */
    fun warmUpAdjacentStations(stations: List<RadioStation>) {
        if (stations.isEmpty()) return
        warmUpJob?.cancel()
        warmUpJob = scope.launch(Dispatchers.IO) {
            delay(500L) // Debounce rapid swiping to conserve bandwidth for active stream
            for (st in stations) {
                try {
                    val rawUrl = st.playUrl
                    if (rawUrl.isBlank()) continue
                    val uri = Uri.parse(rawUrl)
                    val host = uri.host ?: continue

                    // 1. Pre-warm DNS cache
                    Dns.SYSTEM.lookup(host)

                    // 2. Pre-resolve HTTP 301/302 redirects if not already cached
                    if (!resolvedUrlCache.containsKey(rawUrl)) {
                        var resolved = false
                        val headReq = Request.Builder()
                            .url(rawUrl)
                            .header("User-Agent", "DragonFM/1.0.0 (Linux; Android; ExoPlayer)")
                            .head()
                            .build()
                        try {
                            okHttpClient.newCall(headReq).execute().use { resp ->
                                val finalUrl = resp.request.url.toString()
                                if (finalUrl != rawUrl && resp.isSuccessful) {
                                    resolvedUrlCache[rawUrl] = finalUrl
                                    resolved = true
                                    Log.d(TAG, "[PreResolve] Cached redirect for ${st.name}: $rawUrl -> $finalUrl")
                                }
                            }
                        } catch (_: Exception) {
                            // Head request might not be supported on some shoutcast/CDN servers; fallback below
                        }

                        // Fallback to lightweight Range GET if HEAD failed or was unsupported
                        if (!resolved && !resolvedUrlCache.containsKey(rawUrl)) {
                            try {
                                val getReq = Request.Builder()
                                    .url(rawUrl)
                                    .header("User-Agent", "DragonFM/1.0.0 (Linux; Android; ExoPlayer)")
                                    .header("Range", "bytes=0-1")
                                    .build()
                                okHttpClient.newCall(getReq).execute().use { resp ->
                                    val finalUrl = resp.request.url.toString()
                                    if (finalUrl != rawUrl && (resp.isSuccessful || resp.code == 206)) {
                                        resolvedUrlCache[rawUrl] = finalUrl
                                        Log.d(TAG, "[PreResolve] Cached redirect via GET for ${st.name}: $rawUrl -> $finalUrl")
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "[PreResolve] Pre-warm failed for ${st.name}: ${e.message}")
                }
            }
        }
    }

    private fun startTimeoutWatchdog(sessionId: Long, station: RadioStation, isMidStream: Boolean = false) {
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = scope.launch {
            // 直播分段之间短暂进入 BUFFERING 属正常现象，不能像首连那样 6.5 秒就重连
            val timeoutMs = if (isMidStream) 45_000L else 6_500L
            val startBuffered = try { exoPlayer.bufferedPosition } catch (_: Exception) { 0L }
            val startPosition = try { exoPlayer.currentPosition } catch (_: Exception) { 0L }

            delay(timeoutMs)

            if (sessionId != currentSessionId || activeStationId != station.id) return@launch
            if (_playerState.value.status != PlaybackStatus.BUFFERING) return@launch

            val nowBuffered = try { exoPlayer.bufferedPosition } catch (_: Exception) { 0L }
            val nowPosition = try { exoPlayer.currentPosition } catch (_: Exception) { 0L }
            if (isMidStream && (nowBuffered > startBuffered || nowPosition != startPosition)) {
                // 缓冲仍在推进，只是节奏慢：重新计时，不打断播放
                Log.d(TAG, "[Watchdog] Buffering but progressing for ${station.name}, keep waiting")
                startTimeoutWatchdog(sessionId, station, isMidStream = true)
                return@launch
            }

            Log.w(TAG, "[Watchdog] Connection timeout for ${station.name} (isMidStream=$isMidStream)")
            resolvedUrlCache.remove(station.playUrl)

            // 1. Multi-line failover on timeout
            val activeLineIdx = station.activeLineIndex
            val lines = station.effectiveLines
            if (lines.size > 1 && (activeLineIdx + 1) < lines.size) {
                val nextIdx = activeLineIdx + 1
                val nextLine = lines[nextIdx]
                Log.w(TAG, "[Watchdog Failover] Line $activeLineIdx timed out on ${station.name}, switching to line $nextIdx (${nextLine.name})")
                _playerState.value = _playerState.value.copy(
                    status = PlaybackStatus.BUFFERING,
                    nowPlayingTitle = "连接超时，正在切换备用线路 (${nextLine.name})..."
                )
                val switched = station.copy(activeLineIndex = nextIdx)
                playStation(switched, isRetry = true)
                return@launch
            }

            if (retryCount < MAX_AUTO_RETRIES) {
                retryCount++
                Log.w(TAG, "[Watchdog] Attempting auto-retry $retryCount/$MAX_AUTO_RETRIES")
                _playerState.value = _playerState.value.copy(
                    status = PlaybackStatus.BUFFERING,
                    nowPlayingTitle = "网络波动，正在重试 ($retryCount/$MAX_AUTO_RETRIES)..."
                )
                playStation(station, isRetry = true)
            } else {
                try {
                    exoPlayer.stop()
                } catch (_: Exception) {}
                _playerState.value = _playerState.value.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = "电台连接超时，请点击重试或切换频道"
                )
            }
        }
    }

    fun playStation(station: RadioStation, isRetry: Boolean = false) {
        retryJob?.cancel()
        retryJob = null
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = null

        // Ensure playback service is running and active
        DragonMediaSessionService.ensureRunning(context)

        val thisSessionId = ++currentSessionId
        activeStationId = station.id
        if (!isRetry) {
            retryCount = 0
            clearBluetoothResume()
        }
        playStartTimeMs = SystemClock.elapsedRealtime()

        // Zero-pop: immediately mute hardware volume and cancel prior animation before stopping old stream
        cancelVolumeAnimation()
        exoPlayer.volume = 0f

        val finalPlayUrl = resolvedUrlCache[station.playUrl] ?: station.playUrl
        Log.i(TAG, "[Switch] Station: ${station.name} (${station.id}), session: $thisSessionId, isRetry: $isRetry, url: $finalPlayUrl")

        val mediaItem = buildMediaItem(station, finalPlayUrl)

        val activeLine = station.currentLine
        val lineName = activeLine?.name ?: (if (station.hasDedicatedLine) "官方专线" else "默认主线")
        val initialTitle = station.currentProgram?.let { "正在直播：$it" }
            ?: if (isRetry) "正在连接 $lineName..." else "正在载入电台流..."

        _playerState.value = _playerState.value.copy(
            currentStation = station,
            status = PlaybackStatus.BUFFERING,
            nowPlayingTitle = initialTitle,
            errorMessage = null,
            activeLineName = lineName,
            availableLines = station.effectiveLines
        )

        // Resilient Stream Watchdog (handles initial connection and reconnection timeouts)
        startTimeoutWatchdog(thisSessionId, station, isMidStream = false)

        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            timeoutWatchdogJob?.cancel()
            _playerState.value = _playerState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "该电台流暂无法播放: ${e.message}"
            )
        }
    }

    /** 构建 MediaItem；HLS 直播使用较小的直播偏移，避免反复滑出直播窗口导致重连 */
    private fun buildMediaItem(station: RadioStation, url: String): MediaItem {
        val artworkUri = station.favicon?.takeIf {
            it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://") || it.startsWith("content://"))
        }?.let { Uri.parse(it) }

        val builder = MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(Uri.parse(url))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.formattedRegion)
                    .apply {
                        if (artworkUri != null) {
                            setArtworkUri(artworkUri)
                        }
                    }
                    .build()
            )

        if (url.contains(".m3u8", ignoreCase = true)) {
            builder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(12_000L)
                    .setMinPlaybackSpeed(0.95f)
                    .setMaxPlaybackSpeed(1.05f)
                    .build()
            )
        }
        return builder.build()
    }

    fun prepareStation(station: RadioStation) {
        clearBluetoothResume()
        retryJob?.cancel()
        retryJob = null
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = null

        DragonMediaSessionService.ensureRunning(context)

        val thisSessionId = ++currentSessionId
        activeStationId = station.id
        retryCount = 0

        cancelVolumeAnimation()
        exoPlayer.volume = 1f

        val finalPlayUrl = resolvedUrlCache[station.playUrl] ?: station.playUrl
        val mediaItem = buildMediaItem(station, finalPlayUrl)

        _playerState.value = _playerState.value.copy(
            currentStation = station,
            status = PlaybackStatus.PAUSED,
            nowPlayingTitle = "准备就绪",
            errorMessage = null
        )

        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
        } catch (_: Exception) {}
    }

    fun retryCurrentStation() {
        val current = _playerState.value.currentStation ?: return
        resolvedUrlCache.remove(current.playUrl)
        retryCount = 0
        playStation(current, isRetry = false)
    }

    /**
     * Switch playback to a specific line index for the current station (e.g. 0 for 官方专线, 1 for 公网备用)
     */
    fun switchLine(lineIndex: Int) {
        val current = _playerState.value.currentStation ?: return
        val lines = current.effectiveLines
        if (lineIndex in lines.indices && lineIndex != current.activeLineIndex) {
            resolvedUrlCache.remove(current.playUrl)
            retryCount = 0
            val switched = current.copy(activeLineIndex = lineIndex)
            playStation(switched, isRetry = false)
        }
    }

    fun play() {
        cancelVolumeAnimation()
        clearBluetoothResume()
        DragonMediaSessionService.ensureRunning(context)
        if (exoPlayer.playbackState == Player.STATE_IDLE && _playerState.value.currentStation != null) {
            playStation(_playerState.value.currentStation!!)
        } else {
            exoPlayer.volume = 0f
            exoPlayer.play()
            fadeInVolume(180L)
        }
    }

    fun pause() {
        pauseInternal(clearBluetoothResume = true)
    }

    private fun pauseInternal(clearBluetoothResume: Boolean) {
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = null
        retryJob?.cancel()
        retryJob = null
        if (clearBluetoothResume) {
            clearBluetoothResume()
        }

        if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.volume > 0f) {
            // Fast 60ms mute ramp before pause to eliminate sudden cut-off pop
            cancelVolumeAnimation()
            val anim = ValueAnimator.ofFloat(exoPlayer.volume, 0.0f).apply {
                duration = 60
                addUpdateListener {
                    exoPlayer.volume = it.animatedValue as Float
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        exoPlayer.pause()
                        exoPlayer.volume = 1.0f
                        _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                    }
                })
            }
            volumeAnimator = anim
            anim.start()
        } else {
            // Paused while buffering or zero volume: pause immediately
            cancelVolumeAnimation()
            exoPlayer.pause()
            exoPlayer.volume = 1.0f
            _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.playWhenReady) {
            pause()
        } else {
            play()
        }
    }

    fun updateFavoriteStatus(stationId: String, isFavorite: Boolean) {
        val current = _playerState.value.currentStation
        if (current?.id == stationId) {
            _playerState.value = _playerState.value.copy(
                currentStation = current.copy(isFavorite = isFavorite)
            )
        }
    }

    fun updateStationName(stationId: String, newName: String) {
        val current = _playerState.value.currentStation
        if (current?.id == stationId) {
            _playerState.value = _playerState.value.copy(
                currentStation = current.copy(name = newName)
            )
        }
    }

    fun updateNowPlayingTitle(title: String) {
        if (title.isNotBlank()) {
            _playerState.value = _playerState.value.copy(nowPlayingTitle = title)
        }
    }

    fun stop() {
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = null
        retryJob?.cancel()
        retryJob = null
        warmUpJob?.cancel()
        warmUpJob = null
        activeStationId = null
        clearBluetoothResume()

        cancelVolumeAnimation()
        exoPlayer.volume = 0f

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playerState.value = _playerState.value.copy(status = PlaybackStatus.IDLE)
    }

    private fun fadeInVolume(durationMs: Long = 180L) {
        cancelVolumeAnimation()
        val startVol = exoPlayer.volume
        if (startVol >= 1.0f) return
        val anim = ValueAnimator.ofFloat(startVol, 1.0f).apply {
            duration = durationMs
            addUpdateListener {
                exoPlayer.volume = it.animatedValue as Float
            }
            start()
        }
        volumeAnimator = anim
    }

    fun fadeOutAndPause(onComplete: () -> Unit = {}) {
        cancelVolumeAnimation()
        clearBluetoothResume()
        val animator = ValueAnimator.ofFloat(exoPlayer.volume, 0.0f).apply {
            duration = 1500
            addUpdateListener {
                exoPlayer.volume = it.animatedValue as Float
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    exoPlayer.pause()
                    exoPlayer.volume = 1.0f
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                    onComplete()
                }
            })
            start()
        }
        volumeAnimator = animator
    }

    fun release() {
        stop()
        try {
            context.unregisterReceiver(bluetoothProfileReceiver)
        } catch (_: Exception) {
        }
        try {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (_: Exception) {
        }
        try {
            a2dpProfile?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
            headsetProfile?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) }
        } catch (_: Exception) {
        }
        exoPlayer.release()
    }
}
