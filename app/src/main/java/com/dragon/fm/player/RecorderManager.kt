package com.dragon.fm.player

import android.content.Context
import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RecordedFile(
    val file: File,
    val stationName: String,
    val dateString: String,
    val sizeBytes: Long
)

class RecorderManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Dedicated client with infinite readTimeout for continuous live radio streams
    private val streamClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Infinite read timeout for continuous stream
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var recordJob: Job? = null
    private var currentFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _recordingsList = MutableStateFlow<List<RecordedFile>>(emptyList())
    val recordingsList: StateFlow<List<RecordedFile>> = _recordingsList.asStateFlow()

    init {
        refreshRecordings()
    }

    fun startRecording(station: RadioStation) {
        if (_isRecording.value) return

        val dir = File(context.getExternalFilesDir(null), "Recordings")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val cleanName = station.name.replace("[/\\\\:*?\"<>|]".toRegex(), "_").trim().ifBlank { "Radio" }
        val isHls = station.playUrl.contains(".m3u8")
        val ext = if (isHls) "ts" else if (station.codec.uppercase().contains("AAC")) "aac" else "mp3"
        // PC-compatible standard recording filename format
        val targetFile = File(dir, "${cleanName}_$timestamp.$ext")
        currentFile = targetFile

        _isRecording.value = true
        _recordingDurationSec.value = 0

        recordJob = scope.launch {
            // Seconds ticker
            val tickerJob = launch {
                while (isActive) {
                    delay(1000)
                    _recordingDurationSec.value += 1
                }
            }

            try {
                if (isHls) {
                    recordHlsStream(station.playUrl, targetFile)
                } else {
                    recordDirectStream(station.playUrl, targetFile)
                }
            } catch (_: Exception) {
            } finally {
                tickerJob.cancel()
                _isRecording.value = false
                refreshRecordings()
            }
        }
    }

    private suspend fun recordDirectStream(url: String, targetFile: File) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DragonFM-Recorder/1.0 (Android)")
            .build()

        streamClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return
            val inputStream = response.body?.byteStream() ?: return
            FileOutputStream(targetFile, true).use { output ->
                val buffer = ByteArray(16384)
                var read = 0
                while (recordJob?.isActive == true && inputStream.read(buffer).also { read = it } != -1) {
                    if (read > 0) {
                        output.write(buffer, 0, read)
                    }
                }
                output.flush()
            }
        }
    }

    private suspend fun recordHlsStream(m3u8Url: String, targetFile: File) {
        val downloadedSegments = mutableSetOf<String>()
        val baseUrl = m3u8Url.substringBeforeLast("/") + "/"

        FileOutputStream(targetFile, true).use { output ->
            while (recordJob?.isActive == true) {
                try {
                    val req = Request.Builder().url(m3u8Url).build()
                    val playlistBody = httpClient.newCall(req).execute().use { it.body?.string().orEmpty() }

                    // Parse segment URLs from m3u8
                    val lines = playlistBody.lines()
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            val segmentUrl = if (trimmed.startsWith("http")) trimmed else baseUrl + trimmed
                            if (!downloadedSegments.contains(segmentUrl) && recordJob?.isActive == true) {
                                downloadedSegments.add(segmentUrl)
                                downloadSegment(segmentUrl, output)
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                delay(3000) // Poll for next live chunks
            }
            output.flush()
        }
    }

    private fun downloadSegment(segmentUrl: String, output: FileOutputStream) {
        try {
            val req = Request.Builder().url(segmentUrl).build()
            httpClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val bytes = res.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        output.write(bytes)
                        output.flush()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun stopRecording(): File? {
        recordJob?.cancel()
        recordJob = null
        _isRecording.value = false
        val saved = currentFile
        currentFile = null
        refreshRecordings()
        return saved
    }

    fun refreshRecordings() {
        val dir = File(context.getExternalFilesDir(null), "Recordings")
        if (!dir.exists()) {
            _recordingsList.value = emptyList()
            return
        }
        val files = dir.listFiles { f -> f.isFile && (f.name.endsWith(".mp3") || f.name.endsWith(".aac") || f.name.endsWith(".ts") || f.name.endsWith(".wav") || f.name.endsWith(".m4a")) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val base = file.nameWithoutExtension.removePrefix("DragonFM_")
                val dateRegex = "_(?:\\d{4}[-_]?\\d{2}[-_]?\\d{2})".toRegex()
                val match = dateRegex.find(base)
                val stationName = if (match != null) {
                    base.substring(0, match.range.first).ifBlank { base }
                } else {
                    val lastIdx = base.lastIndexOf('_')
                    if (lastIdx > 0) base.substring(0, lastIdx) else base
                }
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                RecordedFile(
                    file = file,
                    stationName = stationName,
                    dateString = sdf.format(Date(file.lastModified())),
                    sizeBytes = file.length()
                )
            }.orEmpty()

        _recordingsList.value = files
    }

    fun deleteRecording(file: File) {
        if (file.exists()) {
            file.delete()
            refreshRecordings()
        }
    }
}
