package com.dragon.fm.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object StationHealthManager {
    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(3500, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Cache of probe results: stationId -> isWorking
    private val healthCache = ConcurrentHashMap<String, Boolean>()

    suspend fun isStationAvailable(stationId: String, url: String): Boolean = withContext(Dispatchers.IO) {
        // Check cache first
        healthCache[stationId]?.let { return@withContext it }

        val isHealthy = try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14) DragonFM/1.0")
                .header("Range", "bytes=0-2048")
                .build()

            probeClient.newCall(req).execute().use { response ->
                response.isSuccessful || response.code in 200..399
            }
        } catch (_: Exception) {
            false
        }

        healthCache[stationId] = isHealthy
        isHealthy
    }

    fun markUnhealthy(stationId: String) {
        healthCache[stationId] = false
    }

    fun markHealthy(stationId: String) {
        healthCache[stationId] = true
    }

    fun getHealthStatus(stationId: String): Boolean? {
        return healthCache[stationId]
    }
}
