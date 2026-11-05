package com.dragon.fm.data.local

import android.content.Context
import android.content.SharedPreferences
import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages exclusion preferences:
 * 1. Domestic-only mode (exclude foreign stations)
 * 2. Station blacklist (manually excluded stations)
 * 3. Tag blacklist (manually excluded tags or genres)
 */
class ExclusionFilterManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dragon_fm_exclusions", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    // 1. Domestic only toggle
    private val _isDomesticOnly = MutableStateFlow(prefs.getBoolean(KEY_DOMESTIC_ONLY, true))
    val isDomesticOnly: StateFlow<Boolean> = _isDomesticOnly.asStateFlow()

    // 2. Blocked Stations: Map of Station ID to Station Name for clean UI display
    private val _blockedStations = MutableStateFlow<Map<String, String>>(emptyMap())
    val blockedStations: StateFlow<Map<String, String>> = _blockedStations.asStateFlow()

    // 3. Blocked Tags (e.g. "宗教", "religious", "talk", etc.)
    private val _blockedTags = MutableStateFlow<Set<String>>(emptySet())
    val blockedTags: StateFlow<Set<String>> = _blockedTags.asStateFlow()

    init {
        loadBlockedStations()
        loadBlockedTags()
    }

    fun setDomesticOnly(enabled: Boolean) {
        _isDomesticOnly.value = enabled
        prefs.edit().putBoolean(KEY_DOMESTIC_ONLY, enabled).apply()
    }

    fun blockStation(station: RadioStation) {
        val current = _blockedStations.value.toMutableMap()
        current[station.id] = station.name
        _blockedStations.value = current
        saveBlockedStations(current)
    }

    fun blockStationByName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = _blockedStations.value.toMutableMap()
        current["manual_${trimmed.hashCode()}"] = trimmed
        _blockedStations.value = current
        saveBlockedStations(current)
    }

    fun unblockStation(stationId: String) {
        val current = _blockedStations.value.toMutableMap()
        current.remove(stationId)
        _blockedStations.value = current
        saveBlockedStations(current)
    }

    fun clearAllBlockedStations() {
        _blockedStations.value = emptyMap()
        prefs.edit().remove(KEY_BLOCKED_STATIONS).apply()
    }

    fun blockTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) return
        val current = _blockedTags.value.toMutableSet()
        current.add(trimmed)
        _blockedTags.value = current
        prefs.edit().putStringSet(KEY_BLOCKED_TAGS, current).apply()
    }

    fun unblockTag(tag: String) {
        val current = _blockedTags.value.toMutableSet()
        current.remove(tag)
        _blockedTags.value = current
        prefs.edit().putStringSet(KEY_BLOCKED_TAGS, current).apply()
    }

    fun isStationBlocked(station: RadioStation): Boolean {
        if (_blockedStations.value.containsKey(station.id)) return true
        if (_blockedStations.value.values.any { it.equals(station.name, ignoreCase = true) }) return true

        if (_isDomesticOnly.value) {
            val c = station.country.lowercase()
            val isDomestic = c == "china" || c.contains("中国") || c.contains("hong kong") ||
                    c.contains("taiwan") || c.contains("macau") || c.contains("cn")
            if (!isDomestic) return true
        }

        val tagsLower = station.tags.lowercase()
        val nameLower = station.name.lowercase()
        for (bt in _blockedTags.value) {
            val btLower = bt.lowercase().trim()
            if (btLower.isNotBlank() && (tagsLower.contains(btLower) || nameLower.contains(btLower))) {
                return true
            }
        }

        return false
    }

    fun filterStations(stations: List<RadioStation>): List<RadioStation> {
        return stations.filter { !isStationBlocked(it) }
    }

    private fun loadBlockedStations() {
        val raw = prefs.getString(KEY_BLOCKED_STATIONS, null) ?: return
        try {
            _blockedStations.value = json.decodeFromString(raw)
        } catch (_: Exception) {}
    }

    private fun saveBlockedStations(map: Map<String, String>) {
        try {
            val encoded = json.encodeToString(map)
            prefs.edit().putString(KEY_BLOCKED_STATIONS, encoded).apply()
        } catch (_: Exception) {}
    }

    private fun loadBlockedTags() {
        val raw = prefs.getStringSet(KEY_BLOCKED_TAGS, emptySet()) ?: emptySet()
        _blockedTags.value = raw
    }

    companion object {
        private const val KEY_DOMESTIC_ONLY = "pref_domestic_only"
        private const val KEY_BLOCKED_STATIONS = "pref_blocked_stations_map"
        private const val KEY_BLOCKED_TAGS = "pref_blocked_tags_set"
    }
}
