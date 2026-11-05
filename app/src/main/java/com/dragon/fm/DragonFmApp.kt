package com.dragon.fm

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dragon.fm.data.api.RadioBrowserApi
import com.dragon.fm.data.local.FavoritesRepository
import com.dragon.fm.player.DragonMediaSessionService
import com.dragon.fm.player.DragonRadioPlayer
import com.dragon.fm.player.MusicRecognitionManager
import com.dragon.fm.player.RecorderManager
import com.dragon.fm.player.SleepTimerManager

class DragonFmApp : Application() {

    lateinit var radioPlayer: DragonRadioPlayer
        private set

    lateinit var favoritesRepository: FavoritesRepository
        private set

    lateinit var recorderManager: RecorderManager
        private set

    lateinit var musicRecognitionManager: MusicRecognitionManager
        private set

    lateinit var sleepTimerManager: SleepTimerManager
        private set

    lateinit var radioBrowserApi: RadioBrowserApi
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 极简版：读取“使用更大缓冲”设置，必须在创建播放器之前
        val prefs = getSharedPreferences("dragon_fm_prefs", Context.MODE_PRIVATE)
        DragonRadioPlayer.useLargeBuffer = prefs.getBoolean("pref_large_buffer", false)
        DragonRadioPlayer.fastStart = prefs.getBoolean("pref_fast_start", true)
        DragonRadioPlayer.pauseOnHeadsetUnplug = prefs.getBoolean("pref_pause_on_unplug", true)

        radioPlayer = DragonRadioPlayer(this)
        favoritesRepository = FavoritesRepository(this)
        recorderManager = RecorderManager(this)
        musicRecognitionManager = MusicRecognitionManager(this)
        radioBrowserApi = RadioBrowserApi()

        sleepTimerManager = SleepTimerManager {
            radioPlayer.fadeOutAndPause()
        }

        startPlaybackService()
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(this, DragonMediaSessionService::class.java)
            startService(intent)
        } catch (_: Exception) {
            // Handled gracefully
        }
    }

    companion object {
        lateinit var instance: DragonFmApp
            private set
    }
}
