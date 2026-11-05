package com.dragon.fm.data.local

import android.content.Context
import android.content.SharedPreferences
import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dragon_fm_data", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _favoriteStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val favoriteStations: StateFlow<List<RadioStation>> = _favoriteStations.asStateFlow()

    private val _history = MutableStateFlow<List<RadioStation>>(emptyList())
    val history: StateFlow<List<RadioStation>> = _history.asStateFlow()

    private val _customStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val customStations: StateFlow<List<RadioStation>> = _customStations.asStateFlow()

    private val _customNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val customNames: StateFlow<Map<String, String>> = _customNames.asStateFlow()

    init {
        loadCustomNames()
        loadHistory()
        loadFavorites()
        loadCustomStations()
    }

    private fun loadCustomNames() {
        val raw = prefs.getString(KEY_CUSTOM_NAMES, null) ?: return
        try {
            _customNames.value = json.decodeFromString(raw)
        } catch (_: Exception) {}
    }

    fun setCustomStationName(stationId: String, newName: String?) {
        val current = _customNames.value.toMutableMap()
        if (newName.isNullOrBlank()) {
            current.remove(stationId)
        } else {
            current[stationId] = newName.trim()
        }
        _customNames.value = current
        try {
            val encoded = json.encodeToString(current)
            prefs.edit().putString(KEY_CUSTOM_NAMES, encoded).apply()
        } catch (_: Exception) {}

        // Also update name in favorites and history lists if present
        loadFavorites()
        loadHistory()
    }

    fun getCustomStationName(stationId: String): String? {
        return _customNames.value[stationId]
    }

    fun applyCustomName(station: RadioStation): RadioStation {
        val custom = _customNames.value[station.id]
        return if (!custom.isNullOrBlank()) {
            station.copy(name = custom)
        } else {
            station
        }
    }

    private fun loadFavorites() {
        val rawSet = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoriteIds.value = rawSet

        val currentList = mutableListOf<RadioStation>()
        val rawList = prefs.getString(KEY_FAVORITE_STATIONS, null)
        if (rawList != null) {
            try {
                val list = json.decodeFromString<List<RadioStation>>(rawList)
                currentList.addAll(list.map { applyCustomName(it) })
            } catch (_: Exception) {}
        }

        // Reconcile: If any ID in rawSet is missing from currentList, restore it from presets or history
        val presentIds = currentList.map { it.id }.toSet()
        val missingIds = rawSet - presentIds
        if (missingIds.isNotEmpty()) {
            val presetsMap = PresetsData.defaultStations.associateBy { it.id }
            val historyMap = _history.value.associateBy { it.id }
            for (id in missingIds) {
                val found = presetsMap[id] ?: historyMap[id]
                if (found != null) {
                    currentList.add(applyCustomName(found.copy(isFavorite = true)))
                }
            }
            saveStations(KEY_FAVORITE_STATIONS, currentList)
        }
        _favoriteStations.value = currentList
    }

    fun isFavorite(stationId: String): Boolean {
        return _favoriteIds.value.contains(stationId)
    }

    fun toggleFavorite(station: RadioStation): Boolean {
        val currentIds = _favoriteIds.value.toMutableSet()
        val currentList = _favoriteStations.value.toMutableList()
        val isFav: Boolean

        if (currentIds.contains(station.id)) {
            currentIds.remove(station.id)
            currentList.removeAll { it.id == station.id }
            isFav = false
        } else {
            currentIds.add(station.id)
            currentList.removeAll { it.id == station.id }
            currentList.add(0, applyCustomName(station.copy(isFavorite = true)))
            isFav = true
        }

        _favoriteIds.value = currentIds
        _favoriteStations.value = currentList

        prefs.edit().putStringSet(KEY_FAVORITES, currentIds).apply()
        saveStations(KEY_FAVORITE_STATIONS, currentList)
        return isFav
    }

    // Overload for toggleFavorite by ID
    fun toggleFavorite(stationId: String, fallbackStation: RadioStation? = null): Boolean {
        val currentIds = _favoriteIds.value.toMutableSet()
        val currentList = _favoriteStations.value.toMutableList()
        val isFav: Boolean

        if (currentIds.contains(stationId)) {
            currentIds.remove(stationId)
            currentList.removeAll { it.id == stationId }
            isFav = false
        } else {
            currentIds.add(stationId)
            val station = fallbackStation
                ?: _history.value.find { it.id == stationId }
                ?: PresetsData.defaultStations.find { it.id == stationId }
            if (station != null) {
                currentList.removeAll { it.id == stationId }
                currentList.add(0, applyCustomName(station.copy(isFavorite = true)))
            }
            isFav = true
        }

        _favoriteIds.value = currentIds
        _favoriteStations.value = currentList
        prefs.edit().putStringSet(KEY_FAVORITES, currentIds).apply()
        saveStations(KEY_FAVORITE_STATIONS, currentList)
        return isFav
    }

    fun removeFavorite(stationId: String) {
        val currentIds = _favoriteIds.value.toMutableSet()
        currentIds.remove(stationId)
        val currentList = _favoriteStations.value.toMutableList()
        currentList.removeAll { it.id == stationId }

        _favoriteIds.value = currentIds
        _favoriteStations.value = currentList
        prefs.edit().putStringSet(KEY_FAVORITES, currentIds).apply()
        saveStations(KEY_FAVORITE_STATIONS, currentList)
    }

    fun pinFavorite(stationId: String) {
        val currentList = _favoriteStations.value.toMutableList()
        val target = currentList.find { it.id == stationId } ?: return
        currentList.remove(target)
        currentList.add(0, target)
        _favoriteStations.value = currentList
        saveStations(KEY_FAVORITE_STATIONS, currentList)
    }

    fun addToHistory(station: RadioStation) {
        val customStation = applyCustomName(station)
        val current = _history.value.toMutableList()
        current.removeAll { it.id == station.id }
        current.add(0, customStation)
        val trimmed = current.take(60)
        _history.value = trimmed
        saveStations(KEY_HISTORY, trimmed)
    }

    fun removeFromHistory(stationId: String) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == stationId }
        _history.value = current
        saveStations(KEY_HISTORY, current)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun loadHistory() {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return
        try {
            val list = json.decodeFromString<List<RadioStation>>(raw)
            _history.value = list.map { applyCustomName(it) }
        } catch (_: Exception) {}
    }

    fun addCustomStation(station: RadioStation) {
        val current = _customStations.value.toMutableList()
        current.removeAll { it.id == station.id }
        current.add(0, station)
        _customStations.value = current
        saveStations(KEY_CUSTOM_STATIONS, current)
    }

    fun removeCustomStation(stationId: String) {
        val current = _customStations.value.toMutableList()
        current.removeAll { it.id == stationId }
        _customStations.value = current
        saveStations(KEY_CUSTOM_STATIONS, current)
    }

    fun clearCustomStations() {
        _customStations.value = emptyList()
        prefs.edit().remove(KEY_CUSTOM_STATIONS).apply()
    }

    private fun loadCustomStations() {
        val raw = prefs.getString(KEY_CUSTOM_STATIONS, null) ?: return
        try {
            _customStations.value = json.decodeFromString(raw)
        } catch (_: Exception) {}
    }

    private fun saveStations(key: String, list: List<RadioStation>) {
        try {
            val encoded = json.encodeToString(list)
            prefs.edit().putString(key, encoded).apply()
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_FAVORITES = "fav_station_ids"
        private const val KEY_FAVORITE_STATIONS = "fav_station_objects"
        private const val KEY_HISTORY = "history_stations"
        private const val KEY_CUSTOM_STATIONS = "custom_stations"
        private const val KEY_CUSTOM_NAMES = "custom_station_names"
    }
}
