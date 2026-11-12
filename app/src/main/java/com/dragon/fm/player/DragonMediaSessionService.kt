package com.dragon.fm.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.dragon.fm.DragonFmApp
import com.dragon.fm.MainActivity
import com.dragon.fm.R
import com.dragon.fm.data.model.PlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class DragonMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playerStateCollectJob: Job? = null
    private var cachedArtwork: Pair<String, Bitmap>? = null

    private fun getCleanStationArtwork(context: Context, rawBitmap: Bitmap?): Bitmap {
        if (rawBitmap != null) {
            return rawBitmap
        }
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.app_icon)
        } catch (_: Exception) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "[Lifecycle] DragonMediaSessionService onCreate")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val app = applicationContext as? DragonFmApp
        val radioPlayer = app?.radioPlayer
        val player = radioPlayer?.exoPlayer

        if (player != null) {
            val forwardingPlayer = object : ForwardingPlayer(player) {
                override fun isCommandAvailable(command: Int): Boolean {
                    return when (command) {
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_STOP -> true
                        else -> super.isCommandAvailable(command)
                    }
                }

                override fun getAvailableCommands(): Player.Commands {
                    return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_STOP)
                        .build()
                }

                override fun play() {
                    Log.i(TAG, "[Command] play triggered from MediaSession")
                    radioPlayer.play()
                }

                override fun pause() {
                    Log.i(TAG, "[Command] pause triggered from MediaSession")
                    radioPlayer.pause()
                }

                override fun stop() {
                    Log.i(TAG, "[Command] stop triggered from MediaSession")
                    stopPlaybackAndService(this@DragonMediaSessionService)
                }

                override fun seekToNext() {
                    Log.i(TAG, "[Command] seekToNext triggered from MediaSession")
                    onSkipNextListener?.invoke()
                }

                override fun seekToNextMediaItem() {
                    Log.i(TAG, "[Command] seekToNextMediaItem triggered from MediaSession")
                    onSkipNextListener?.invoke()
                }

                override fun seekToPrevious() {
                    Log.i(TAG, "[Command] seekToPrevious triggered from MediaSession")
                    onSkipPreviousListener?.invoke()
                }

                override fun seekToPreviousMediaItem() {
                    Log.i(TAG, "[Command] seekToPreviousMediaItem triggered from MediaSession")
                    onSkipPreviousListener?.invoke()
                }
            }

            mediaSession = MediaSession.Builder(this, forwardingPlayer)
                .setSessionActivity(pendingIntent)
                .build()

            // Directly observe radioPlayer's state flow for millisecond-precision synchronization
            playerStateCollectJob?.cancel()
            playerStateCollectJob = serviceScope.launch(Dispatchers.Main) {
                var lastPostTime = 0L
                radioPlayer.playerState.collect { state ->
                    Log.d(TAG, "[StateSync] status=${state.status}, station=${state.currentStation?.name}")
                    val now = android.os.SystemClock.elapsedRealtime()
                    // Instant update on definitive status changes (IDLE, PAUSED, ERROR, PLAYING)
                    if (state.status != PlaybackStatus.BUFFERING || (now - lastPostTime) >= 120L) {
                        lastPostTime = now
                        updateNotification()
                    } else {
                        // Conflate intermediate buffering updates during rapid channel swiping
                        launch {
                            kotlinx.coroutines.delay(120L)
                            if (radioPlayer.playerState.value == state) {
                                lastPostTime = android.os.SystemClock.elapsedRealtime()
                                updateNotification()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as? DragonFmApp
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                Log.d(TAG, "[Action] PLAY_PAUSE")
                app?.radioPlayer?.togglePlayPause()
            }
            ACTION_PREV -> {
                Log.d(TAG, "[Action] PREV")
                onSkipPreviousListener?.invoke()
            }
            ACTION_NEXT -> {
                Log.d(TAG, "[Action] NEXT")
                onSkipNextListener?.invoke()
            }
            ACTION_STOP -> {
                Log.d(TAG, "[Action] STOP")
                playerStateCollectJob?.cancel()
                app?.radioPlayer?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationManager?.cancel(NOTIFICATION_ID)
                stopSelf()
            }
            ACTION_UPDATE -> {
                updateNotification()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun updateNotification() {
        val app = applicationContext as? DragonFmApp ?: return
        val playerState = app.radioPlayer.playerState.value
        if (playerState.status == PlaybackStatus.IDLE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager?.cancel(NOTIFICATION_ID)
            return
        }
        val isPlaying = playerState.status == PlaybackStatus.PLAYING
        val isBuffering = playerState.status == PlaybackStatus.BUFFERING
        val awaitingBluetoothReconnect = app.radioPlayer.isAwaitingBluetoothReconnect()
        val station = playerState.currentStation ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager?.cancel(NOTIFICATION_ID)
            return
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, DragonMediaSessionService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, DragonMediaSessionService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, DragonMediaSessionService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, DragonMediaSessionService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val subtitle = when {
            playerState.nowPlayingTitle.isNotBlank() && !playerState.nowPlayingTitle.contains("正在载入") -> playerState.nowPlayingTitle
            isBuffering -> "正在载入电台流..."
            playerState.status == PlaybackStatus.ERROR -> "连接受阻 • 可点击重试"
            else -> station.formattedRegion
        }

        // Clean station artwork without fake canvas frames so Android SystemUI can render it natively
        val artworkBitmap = if (cachedArtwork?.first == station.id) {
            cachedArtwork?.second
        } else {
            val defaultArt = getCleanStationArtwork(this, null)
            cachedArtwork = station.id to defaultArt

            if (!station.favicon.isNullOrBlank()) {
                val currentStationId = station.id
                val faviconUrl = station.favicon
                serviceScope.launch {
                    try {
                        val imageLoader = ImageLoader(this@DragonMediaSessionService)
                        val req = ImageRequest.Builder(this@DragonMediaSessionService)
                            .data(faviconUrl)
                            .allowHardware(false)
                            .build()
                        val res = (imageLoader.execute(req) as? SuccessResult)?.drawable
                        val bmp = (res as? BitmapDrawable)?.bitmap
                        if (bmp != null) {
                            val cleanBmp = getCleanStationArtwork(this@DragonMediaSessionService, bmp)
                            cachedArtwork = currentStationId to cleanBmp
                            withContext(Dispatchers.Main) {
                                if (app.radioPlayer.playerState.value.currentStation?.id == currentStationId) {
                                    updateNotification()
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Keep default artwork
                    }
                }
            }
            defaultArt
        }

        // Native Android System Media Control Notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .apply {
                if (artworkBitmap != null) {
                    setLargeIcon(artworkBitmap)
                }
            }
            .setContentTitle(station.name)
            .setContentText(subtitle)
            .setSubText("LIVE • ${station.formattedRegion} • LX FM")
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying || isBuffering || awaitingBluetoothReconnect)
            .addAction(android.R.drawable.ic_media_previous, "上一台", prevIntent)
            .addAction(
                if (isPlaying || isBuffering) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying || isBuffering) "暂停" else "播放",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_next, "下一台", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionCompatToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        val notification = builder.build()

        val isForegroundNeeded = isPlaying || isBuffering || awaitingBluetoothReconnect
        try {
            if (isForegroundNeeded) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                stopForeground(STOP_FOREGROUND_DETACH)
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "[Notification] startForeground failed, falling back to notify: ${e.message}")
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val app = applicationContext as? DragonFmApp
        val status = app?.radioPlayer?.playerState?.value?.status
        val awaitingBluetoothReconnect = app?.radioPlayer?.isAwaitingBluetoothReconnect() == true
        if (
            status != PlaybackStatus.PLAYING &&
            status != PlaybackStatus.BUFFERING &&
            !awaitingBluetoothReconnect
        ) {
            stopPlaybackAndService(this)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.i(TAG, "[Lifecycle] DragonMediaSessionService onDestroy")
        playerStateCollectJob?.cancel()
        playerStateCollectJob = null
        notificationManager?.cancel(NOTIFICATION_ID)
        instance = null
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "DragonMediaSessionService"
        const val CHANNEL_ID = "dragon_fm_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.dragon.fm.action.PLAY_PAUSE"
        const val ACTION_PREV = "com.dragon.fm.action.PREV"
        const val ACTION_NEXT = "com.dragon.fm.action.NEXT"
        const val ACTION_STOP = "com.dragon.fm.action.STOP"
        const val ACTION_UPDATE = "com.dragon.fm.action.UPDATE"

        var instance: DragonMediaSessionService? = null
            private set

        var onSkipPreviousListener: (() -> Unit)? = null
        var onSkipNextListener: (() -> Unit)? = null

        fun ensureRunning(context: Context) {
            if (instance == null) {
                try {
                    val intent = Intent(context, DragonMediaSessionService::class.java)
                    context.startService(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to ensure service running: ${e.message}")
                }
            }
        }

        fun requestNotificationUpdate(context: Context) {
            if (instance == null) return
            try {
                context.startService(
                    Intent(context, DragonMediaSessionService::class.java).apply {
                        action = ACTION_UPDATE
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update notification: ${e.message}")
            }
        }

        fun stopPlaybackAndService(context: Context) {
            val app = context.applicationContext as? DragonFmApp
            app?.radioPlayer?.stop()
            instance?.run {
                playerStateCollectJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationManager?.cancel(NOTIFICATION_ID)
                stopSelf()
            }
            try {
                context.stopService(Intent(context, DragonMediaSessionService::class.java))
            } catch (_: Exception) {}
        }
    }
}
