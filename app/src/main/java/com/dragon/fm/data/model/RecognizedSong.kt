package com.dragon.fm.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RecognizedSong(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String,
    val album: String = "",
    val coverArtUrl: String = "",
    val genre: String = "",
    val releaseYear: String = "",
    val shazamUrl: String = "",
    val stationName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class RecognitionMode {
    RADIO_STREAM,
    MICROPHONE
}

sealed class RecognitionState {
    object Idle : RecognitionState()
    data class Sampling(val progress: Float = 0f, val message: String = "正在采集音频...") : RecognitionState()
    object Fingerprinting : RecognitionState()
    object QueryingCloud : RecognitionState()
    data class Success(val song: RecognizedSong) : RecognitionState()
    data class NoMatch(val message: String = "未识别到歌曲（可能正处于主持人口播、新闻或片头广告）") : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}
