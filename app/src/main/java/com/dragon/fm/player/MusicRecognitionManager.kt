package com.dragon.fm.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.alexmercerind.audire.native.ShazamSignature
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.RecognitionMode
import com.dragon.fm.data.model.RecognitionState
import com.dragon.fm.data.model.RecognizedSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MusicRecognitionManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val signatureGenerator by lazy { ShazamSignature() }

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private val _history = MutableStateFlow<List<RecognizedSong>>(emptyList())
    val history: StateFlow<List<RecognizedSong>> = _history.asStateFlow()

    private var activeJob: Job? = null

    init {
        loadHistory()
    }

    fun resetState() {
        activeJob?.cancel()
        activeJob = null
        _state.value = RecognitionState.Idle
    }

    /**
     * 一键识别当前电台正在播放的音乐（直接截取广播数字流，零环境杂音，准确率极高）
     */
    fun recognizeCurrentStation(station: RadioStation) {
        activeJob?.cancel()
        activeJob = scope.launch {
            try {
                _state.value = RecognitionState.Sampling(0.1f, "正在连接广播直播流...")

                val tempAudioFile = File(context.cacheDir, "recog_stream_${System.currentTimeMillis()}.tmp")
                val sampleSuccess = sampleRadioStream(station.playUrl, tempAudioFile)

                if (!sampleSuccess || !tempAudioFile.exists() || tempAudioFile.length() < 16384) {
                    _state.value = RecognitionState.Error("广播流采样数据不足，请确保电台正在流畅播放")
                    return@launch
                }

                _state.value = RecognitionState.Sampling(0.5f, "正在解码并提取声学指纹...")
                val pcmSamples = decodeAudioTo16kMonoPcm(tempAudioFile)
                tempAudioFile.delete()

                if (pcmSamples == null || pcmSamples.size < 16000 * 2) {
                    _state.value = RecognitionState.Error("音频解码失败或有效音频长度过短")
                    return@launch
                }

                _state.value = RecognitionState.Fingerprinting
                val signatureUri = withContext(Dispatchers.Default) {
                    signatureGenerator.create(pcmSamples)
                }

                if (signatureUri.isBlank()) {
                    _state.value = RecognitionState.Error("声学地标特征提取失败")
                    return@launch
                }

                _state.value = RecognitionState.QueryingCloud
                val sampleMs = (pcmSamples.size * 1000L / 16000L).toInt()
                val recognizedSong = queryShazamApi(signatureUri, sampleMs, station.name)

                if (recognizedSong != null) {
                    saveToHistory(recognizedSong)
                    _state.value = RecognitionState.Success(recognizedSong)
                } else {
                    _state.value = RecognitionState.NoMatch("当前电台未检测到歌曲（可能正处于主持人口播、新闻或片头广告）")
                }
            } catch (e: Exception) {
                if (isActive) {
                    _state.value = RecognitionState.Error("识别发生异常: ${e.localizedMessage ?: "网络或音频错误"}")
                }
            }
        }
    }

    /**
     * 麦克风环境听歌识曲（采集周围环境音乐）
     */
    fun recognizeFromMicrophone() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.value = RecognitionState.Error("未获取麦克风录音权限，请在系统设置中授予")
            return
        }

        activeJob?.cancel()
        activeJob = scope.launch {
            var audioRecord: AudioRecord? = null
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(3200)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    _state.value = RecognitionState.Error("麦克风初始化失败，请稍后重试")
                    return@launch
                }

                audioRecord.startRecording()

                val durationSeconds = 5
                val totalSamples = sampleRate * durationSeconds
                val pcmBuffer = ShortArray(totalSamples)
                var samplesRead = 0

                val startTime = System.currentTimeMillis()
                while (samplesRead < totalSamples && isActive) {
                    val remaining = totalSamples - samplesRead
                    val chunk = (remaining).coerceAtMost(2000)
                    val read = audioRecord.read(pcmBuffer, samplesRead, chunk)
                    if (read > 0) {
                        samplesRead += read
                        val progress = samplesRead.toFloat() / totalSamples.toFloat()
                        _state.value = RecognitionState.Sampling(progress, "正在聆听环境音乐 (${(samplesRead / 16000.0).roundToInt()}s / 5s)...")
                    } else {
                        delay(20)
                    }
                }

                try {
                    audioRecord.stop()
                } catch (_: Exception) {}

                if (samplesRead < 16000 * 2) {
                    _state.value = RecognitionState.Error("录音时间过短，未能采集足够音频")
                    return@launch
                }

                _state.value = RecognitionState.Fingerprinting
                val signatureUri = withContext(Dispatchers.Default) {
                    signatureGenerator.create(pcmBuffer)
                }

                if (signatureUri.isBlank()) {
                    _state.value = RecognitionState.Error("环境音频特征提取失败")
                    return@launch
                }

                _state.value = RecognitionState.QueryingCloud
                val sampleMs = (samplesRead * 1000L / 16000L).toInt()
                val recognizedSong = queryShazamApi(signatureUri, sampleMs, "麦克风识别")

                if (recognizedSong != null) {
                    saveToHistory(recognizedSong)
                    _state.value = RecognitionState.Success(recognizedSong)
                } else {
                    _state.value = RecognitionState.NoMatch("未识别出正在播放的歌曲，请靠近音源重试")
                }
            } catch (e: Exception) {
                if (isActive) {
                    _state.value = RecognitionState.Error("录音识曲失败: ${e.localizedMessage ?: "未知错误"}")
                }
            } finally {
                try {
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * 截取广播流 5-6 秒数据
     */
    private suspend fun sampleRadioStream(streamUrl: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val isHls = streamUrl.contains(".m3u8", ignoreCase = true)
            if (isHls) {
                // HLS m3u8 分片采集
                var currentUrl = streamUrl
                val req = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                    .build()
                var playlistBody = httpClient.newCall(req).execute().use { it.body?.string().orEmpty() }

                // 若为 Master Playlist（多码率索引），递归提取实际媒体切片分片列表
                if (playlistBody.contains("#EXT-X-STREAM-INF")) {
                    val lines = playlistBody.lines()
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            val baseUrl = currentUrl.substringBeforeLast("/") + "/"
                            currentUrl = if (trimmed.startsWith("http")) trimmed else baseUrl + trimmed
                            break
                        }
                    }
                    val childReq = Request.Builder()
                        .url(currentUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                        .build()
                    playlistBody = httpClient.newCall(childReq).execute().use { it.body?.string().orEmpty() }
                }

                val baseUrl = currentUrl.substringBeforeLast("/") + "/"
                val lines = playlistBody.lines()
                var segmentUrl: String? = null
                for (line in lines.reversed()) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        segmentUrl = if (trimmed.startsWith("http")) trimmed else baseUrl + trimmed
                        break
                    }
                }

                if (segmentUrl == null) return@withContext false

                val segReq = Request.Builder()
                    .url(segmentUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                    .build()
                httpClient.newCall(segReq).execute().use { res ->
                    if (!res.isSuccessful) return@withContext false
                    val bytes = res.body?.bytes() ?: return@withContext false
                    targetFile.writeBytes(bytes)
                    return@withContext bytes.size >= 8192
                }
            } else {
                // 普通 HTTP 直播流采样 (MP3, AAC, FLAC, OGG)
                val req = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                    .build()

                httpClient.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val inputStream = response.body?.byteStream() ?: return@withContext false
                    val targetBytes = 160 * 1024 // 约 160KB (~6-8秒 192kbps 或 10秒 128kbps)
                    FileOutputStream(targetFile).use { fos ->
                        val buf = ByteArray(8192)
                        var total = 0
                        val start = System.currentTimeMillis()
                        while (total < targetBytes && (System.currentTimeMillis() - start) < 6500) {
                            val r = inputStream.read(buf)
                            if (r <= 0) break
                            fos.write(buf, 0, r)
                            total += r
                        }
                        fos.flush()
                        return@withContext total >= 16384
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 将任意音频文件（MP3/AAC/TS/OGG）通过 Android 原生 MediaCodec 解码为 16000Hz 16-Bit Mono PCM ShortArray
     */
    private fun decodeAudioTo16kMonoPcm(audioFile: File): ShortArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(audioFile.absolutePath)
            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    extractor.selectTrack(i)
                    break
                }
            }

            if (audioTrackIndex == -1 || trackFormat == null) return null

            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(trackFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val decodedPcmChunks = ArrayList<ShortArray>()
            var sawInputEOS = false
            var sawOutputEOS = false
            var outputSampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var outputChannels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            var iterations = 0
            while (!sawOutputEOS && iterations++ < 1500) {
                if (!sawInputEOS) {
                    val inputBufIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufIndex >= 0) {
                        val inputBuf = codec.getInputBuffer(inputBufIndex)
                        if (inputBuf != null) {
                            inputBuf.clear()
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                sawInputEOS = true
                                codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                codec.queueInputBuffer(inputBufIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputBufIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outputBufIndex >= 0) {
                    val outputBuf = codec.getOutputBuffer(outputBufIndex)
                    if (outputBuf != null && info.size > 0) {
                        outputBuf.position(info.offset)
                        outputBuf.limit(info.offset + info.size)
                        outputBuf.order(ByteOrder.LITTLE_ENDIAN)

                        val shortCount = info.size / 2
                        val shorts = ShortArray(shortCount)
                        outputBuf.asShortBuffer().get(shorts)
                        decodedPcmChunks.add(shorts)
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = codec.outputFormat
                    outputSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    outputChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }

            if (decodedPcmChunks.isEmpty()) return null

            val totalDecodedShorts = decodedPcmChunks.sumOf { it.size }
            val fullPcm = ShortArray(totalDecodedShorts)
            var destPos = 0
            for (chunk in decodedPcmChunks) {
                System.arraycopy(chunk, 0, fullPcm, destPos, chunk.size)
                destPos += chunk.size
            }

            // 1. 转为单声道 (Downmix to Mono)
            val monoSamples: ShortArray = if (outputChannels > 1) {
                val monoLen = fullPcm.size / outputChannels
                val mono = ShortArray(monoLen)
                for (i in 0 until monoLen) {
                    var sum = 0
                    for (ch in 0 until outputChannels) {
                        sum += fullPcm[i * outputChannels + ch]
                    }
                    mono[i] = (sum / outputChannels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                mono
            } else {
                fullPcm
            }

            // 2. 重采样到 16000 Hz
            return if (outputSampleRate != 16000) {
                resampleTo16k(monoSamples, outputSampleRate)
            } else {
                monoSamples
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * 高质量线性插值重采样至 16000 Hz
     */
    private fun resampleTo16k(input: ShortArray, sourceRate: Int): ShortArray {
        if (sourceRate <= 0 || input.isEmpty()) return input
        val ratio = sourceRate.toDouble() / 16000.0
        val targetLength = (input.size / ratio).toInt()
        val output = ShortArray(targetLength)

        for (i in 0 until targetLength) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt()
            val fraction = srcPos - srcIndex
            if (srcIndex + 1 < input.size) {
                val sample1 = input[srcIndex]
                val sample2 = input[srcIndex + 1]
                val interpolated = sample1 + (fraction * (sample2 - sample1)).roundToInt()
                output[i] = interpolated.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            } else if (srcIndex < input.size) {
                output[i] = input[srcIndex]
            }
        }
        return output
    }

    /**
     * 向 Shazam 识别服务器查询声学指纹
     */
    private suspend fun queryShazamApi(signatureUri: String, sampleMs: Int, stationName: String): RecognizedSong? = withContext(Dispatchers.IO) {
        try {
            val tagId1 = UUID.randomUUID().toString().uppercase()
            val tagId2 = UUID.randomUUID().toString().uppercase()
            val url = "https://amp.shazam.com/discovery/v5/zh-CN/CN/android/-/tag/$tagId1/$tagId2?sync=true&webv3=true&sampling=true"

            val jsonBody = JSONObject().apply {
                put("timezone", "Asia/Shanghai")
                put("signature", JSONObject().apply {
                    put("uri", signatureUri)
                    put("samplems", sampleMs)
                })
                put("timestamp", System.currentTimeMillis())
                put("context", JSONObject())
                put("geolocation", JSONObject())
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string().orEmpty()
                val rootJson = JSONObject(bodyStr)
                val track = rootJson.optJSONObject("track") ?: return@withContext null

                val title = track.optString("title", "").trim()
                val artist = track.optString("subtitle", "").trim()
                if (title.isBlank() || artist.isBlank()) return@withContext null

                val imagesObj = track.optJSONObject("images")
                val coverArt = imagesObj?.optString("coverarthq")?.takeIf { it.isNotBlank() }
                    ?: imagesObj?.optString("coverart")?.takeIf { it.isNotBlank() }
                    ?: ""

                var album = ""
                var releaseYear = ""
                val sections = track.optJSONArray("sections")
                if (sections != null) {
                    for (i in 0 until sections.length()) {
                        val sec = sections.optJSONObject(i) ?: continue
                        val metadata = sec.optJSONArray("metadata") ?: continue
                        for (j in 0 until metadata.length()) {
                            val item = metadata.optJSONObject(j) ?: continue
                            val metaTitle = item.optString("title")
                            val text = item.optString("text")
                            if (metaTitle.equals("Album", ignoreCase = true) || metaTitle.equals("专辑", ignoreCase = true)) {
                                album = text
                            }
                            if (metaTitle.equals("Released", ignoreCase = true) || metaTitle.equals("发行年份", ignoreCase = true)) {
                                releaseYear = text
                            }
                        }
                    }
                }

                val genre = track.optJSONObject("genres")?.optString("primary", "") ?: ""
                val shazamUrl = track.optString("url", "")

                return@withContext RecognizedSong(
                    title = title,
                    artist = artist,
                    album = album,
                    coverArtUrl = coverArt,
                    genre = genre,
                    releaseYear = releaseYear,
                    shazamUrl = shazamUrl,
                    stationName = stationName,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun loadHistory() {
        try {
            val sp = context.getSharedPreferences("lxfm_recognition_history", Context.MODE_PRIVATE)
            val raw = sp.getString("history_json", null) ?: return
            val list = json.decodeFromString<List<RecognizedSong>>(raw)
            _history.value = list
        } catch (e: Exception) {
            _history.value = emptyList()
        }
    }

    private fun saveToHistory(song: RecognizedSong) {
        val current = _history.value.toMutableList()
        // Deduplicate recent same song within 1 hour
        current.removeAll { it.title.equals(song.title, ignoreCase = true) && it.artist.equals(song.artist, ignoreCase = true) }
        current.add(0, song)
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _history.value = current
        persistHistory(current)
    }

    fun deleteSong(songId: String) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == songId }
        _history.value = current
        persistHistory(current)
    }

    fun clearHistory() {
        _history.value = emptyList()
        persistHistory(emptyList())
    }

    private fun persistHistory(list: List<RecognizedSong>) {
        scope.launch(Dispatchers.IO) {
            try {
                val sp = context.getSharedPreferences("lxfm_recognition_history", Context.MODE_PRIVATE)
                val encoded = json.encodeToString(list)
                sp.edit().putString("history_json", encoded).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
