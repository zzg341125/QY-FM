package com.dragon.fm.data.local

import com.dragon.fm.data.model.RadioStation

internal object PlaylistOrderPolicy {
    /**
     * Applies an explicit saved order while keeping unknown/new stations.
     * The output always contains each station ID at most once.
     */
    fun apply(stations: List<RadioStation>, orderedIds: List<String>): List<RadioStation> {
        if (stations.isEmpty()) return emptyList()
        val stationsById = stations
            .distinctBy { it.id }
            .associateBy { it.id }
        val ordered = orderedIds
            .asSequence()
            .distinct()
            .mapNotNull { stationsById[it] }
            .toMutableList()
        val includedIds = ordered.mapTo(mutableSetOf()) { it.id }
        ordered += stations.filter { it.id !in includedIds }
        return ordered
    }
}
