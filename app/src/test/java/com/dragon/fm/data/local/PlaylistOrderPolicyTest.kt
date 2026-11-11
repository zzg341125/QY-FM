package com.dragon.fm.data.local

import com.dragon.fm.data.model.RadioStation
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistOrderPolicyTest {

    @Test
    fun restoresExplicitOrderAndAppendsNewStations() {
        val stations = listOf(
            station("c"),
            station("a"),
            station("d"),
            station("b"),
        )

        val result = PlaylistOrderPolicy.apply(stations, listOf("b", "a", "c"))

        assertEquals(listOf("b", "a", "c", "d"), result.map { it.id })
    }

    @Test
    fun removesDuplicateIdsWithoutLosingOrder() {
        val stations = listOf(station("a"), station("b"), station("a"))

        val result = PlaylistOrderPolicy.apply(stations, listOf("b", "a", "b"))

        assertEquals(listOf("b", "a"), result.map { it.id })
    }

    private fun station(id: String) = RadioStation(
        id = id,
        name = id,
        url = "https://example.com/$id",
    )
}
