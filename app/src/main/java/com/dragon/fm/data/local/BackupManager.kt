package com.dragon.fm.data.local

import android.content.Context
import com.dragon.fm.data.model.RadioStation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 曲阳收音机在线备份：把播放列表、设置、上传的台标打包为 zip。
 */
object BackupManager {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 需要随备份保存的设置项 */
    val SETTINGS_KEYS = listOf(
        "pref_simple_theme_id",
        "pref_dynamic_color",
        "pref_transparent_ui",
        "pref_click_station_play",
        "pref_edit_station",
        "pref_edit_stream_url",
        "pref_auto_play_on_launch",
        "pref_playback_memory",
        "pref_fast_start",
        "pref_large_buffer",
        "pref_pause_on_unplug",
        "pref_auto_update_enabled",
        "pref_webdav_url",
        "pref_webdav_user",
        "pref_webdav_file"
    )

    data class RestoreResult(
        val stations: List<RadioStation>,
        val settings: Map<String, Any?>
    )

    /** 生成备份 zip，返回文件 */
    fun createBackup(
        context: Context,
        stations: List<RadioStation>,
        settings: Map<String, Any?>
    ): File {
        val outDir = File(context.cacheDir, "webdav").apply { mkdirs() }
        val outFile = File(outDir, "quyang_backup.zip")

        val logoEntries = linkedMapOf<String, String>() // 本地路径 -> zip 内路径
        val backupStations = stations.mapIndexed { index, station ->
            val favicon = station.favicon
            if (!favicon.isNullOrBlank() && favicon.startsWith("/")) {
                val logoFile = File(favicon)
                if (logoFile.isFile) {
                    val entryName = logoEntries.getOrPut(favicon) {
                        "logos/${index}_${logoFile.name}"
                    }
                    station.copy(favicon = "logo:${entryName.removePrefix("logos/")}")
                } else {
                    station
                }
            } else {
                station
            }
        }

        ZipOutputStream(BufferedOutputStream(outFile.outputStream())).use { zos ->
            putText(zos, "manifest.json", JSONObject().apply {
                put("app", "收音机")
                put("version", "1.0.0")
                put("createdAt", System.currentTimeMillis())
                put("stationCount", stations.size)
                put("stationOrder", JSONArray(backupStations.map { it.id }))
            }.toString())

            putText(zos, "playlist.json", json.encodeToString(backupStations))
            putText(zos, "playlist.m3u", M3uPlaylistIO.build(stations))
            putText(zos, "settings.json", settingsToJson(settings))

            logoEntries.forEach { (localPath, entryName) ->
                val file = File(localPath)
                if (!file.isFile) return@forEach
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { input -> input.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return outFile
    }

    /** 读取备份 zip，恢复台标到应用私有目录，并返回电台与设置 */
    fun restoreBackup(context: Context, zipFile: File): RestoreResult {
        val entries = readZipEntries(zipFile)

        val playlistText = entries["playlist.json"]?.toString(Charsets.UTF_8)
            ?: return RestoreResult(emptyList(), emptyMap())

        val stations = runCatching {
            json.decodeFromString<List<RadioStation>>(playlistText)
        }.getOrElse { return RestoreResult(emptyList(), emptyMap()) }

        val logoDir = File(context.filesDir, "station_logos").apply { mkdirs() }
        val restoredWithIcons = stations.map { station ->
            val favicon = station.favicon
            if (!favicon.isNullOrBlank() && favicon.startsWith("logo:")) {
                val name = favicon.removePrefix("logo:")
                val bytes = entries["logos/$name"]
                if (bytes != null) {
                    val target = File(logoDir, File(name).name)
                    target.outputStream().use { it.write(bytes) }
                    station.copy(favicon = target.absolutePath)
                } else {
                    station.copy(favicon = null)
                }
            } else {
                station
            }
        }
        val orderedIds = entries["manifest.json"]
            ?.toString(Charsets.UTF_8)
            ?.let { text ->
                runCatching {
                    val array = JSONObject(text).optJSONArray("stationOrder") ?: JSONArray()
                    buildList {
                        for (index in 0 until array.length()) {
                            array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }.getOrDefault(emptyList())
            }
            .orEmpty()
        val restoredStations = PlaylistOrderPolicy.apply(restoredWithIcons, orderedIds)

        val settings = entries["settings.json"]?.toString(Charsets.UTF_8)
            ?.let { runCatching { jsonToSettings(it) }.getOrDefault(emptyMap()) }
            ?: emptyMap()

        return RestoreResult(restoredStations, settings)
    }

    private fun readZipEntries(zipFile: File): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        var totalBytes = 0L
        val maxBytes = 100L * 1024 * 1024

        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && isValidEntry(name)) {
                    val bytes = zis.readBytes()
                    totalBytes += bytes.size
                    if (totalBytes > maxBytes) break
                    result[name] = bytes
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return result
    }

    private fun isValidEntry(name: String): Boolean {
        if (name.startsWith("/") || name.contains("..")) return false
        return name == "manifest.json" ||
                name == "playlist.json" ||
                name == "playlist.m3u" ||
                name == "settings.json" ||
                name.startsWith("logos/")
    }

    private fun putText(zos: ZipOutputStream, name: String, text: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(text.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun settingsToJson(settings: Map<String, Any?>): String {
        val obj = JSONObject()
        settings.forEach { (key, value) ->
            if (value != null) obj.put(key, value)
        }
        return obj.toString()
    }

    private fun jsonToSettings(text: String): Map<String, Any?> {
        val obj = JSONObject(text)
        val map = mutableMapOf<String, Any?>()
        obj.keys().forEach { key -> map[key] = obj.opt(key) }
        return map
    }
}
