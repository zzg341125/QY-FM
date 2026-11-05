package com.dragon.fm.player

import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object StationSpeedTester {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3000, TimeUnit.MILLISECONDS)
        .readTimeout(3000, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Cache latency in ms for station id
    private val latencyMap = ConcurrentHashMap<String, Long>()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _latencies = MutableStateFlow<Map<String, Long>>(emptyMap())
    val latencies: StateFlow<Map<String, Long>> = _latencies.asStateFlow()

    fun getLatency(stationId: String): Long? = latencyMap[stationId]

    suspend fun testLatency(url: String): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android) DragonFM-SpeedTest/1.0")
                .header("Range", "bytes=0-1024")
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful || response.code in 200..399) {
                    val stream = response.body?.byteStream()
                    val buf = ByteArray(256)
                    stream?.read(buf)
                    val elapsed = System.currentTimeMillis() - startTime
                    elapsed.coerceAtLeast(10)
                } else {
                    -1L
                }
            }
        } catch (_: Exception) {
            -1L
        }
    }

    suspend fun batchTestStations(stations: List<RadioStation>) = withContext(Dispatchers.IO) {
        if (_isTesting.value) return@withContext
        _isTesting.value = true

        try {
            // Test in batches of 6 concurrently to avoid overwhelming network
            stations.chunked(6).forEach { batch ->
                val deferreds = batch.map { station ->
                    async {
                        val latency = testLatency(station.playUrl)
                        latencyMap[station.id] = latency
                        station.id to latency
                    }
                }
                deferreds.awaitAll()
                _latencies.value = HashMap(latencyMap)
            }
        } finally {
            _isTesting.value = false
            _latencies.value = HashMap(latencyMap)
        }
    }

    fun getSpeedBadge(latency: Long?): String {
        return when {
            latency == null -> "未测速"
            latency < 0 -> "离线"
            latency < 300 -> "${latency}ms 极速"
            latency < 800 -> "${latency}ms 良好"
            else -> "${latency}ms"
        }
    }
}
