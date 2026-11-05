package com.dragon.fm.data.model

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class AppUpdateInfo(
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val changelog: String = "",
    val downloadUrl: String = "",
    val forceUpdate: Boolean = false,
    val releaseDate: String = ""
)

sealed class UpdateCheckResult {
    object Idle : UpdateCheckResult()
    object Checking : UpdateCheckResult()
    data class HasUpdate(val info: AppUpdateInfo, val isManual: Boolean = false) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val isManual: Boolean = false) : UpdateCheckResult()
    data class Error(val message: String, val isManual: Boolean = false) : UpdateCheckResult()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val currentBytes: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Completed(val apkFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
