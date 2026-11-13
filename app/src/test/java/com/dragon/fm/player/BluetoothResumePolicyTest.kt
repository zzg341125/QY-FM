package com.dragon.fm.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothResumePolicyTest {

    @Test
    fun resumesOnlyForTheSameDisconnectedDevice() {
        val policy = BluetoothResumePolicy()
        val headphones = BluetoothAudioDevice(id = 42, type = 8, productName = "Car Audio")
        val speaker = BluetoothAudioDevice(id = 77, type = 8, productName = "Kitchen Speaker")

        policy.rememberDisconnected(listOf(headphones), nowMs = 1_000L)

        assertFalse(policy.consumeReconnect(listOf(speaker), nowMs = 2_000L))
        assertTrue(policy.consumeReconnect(listOf(headphones), nowMs = 3_000L))
        assertFalse(policy.consumeReconnect(listOf(headphones), nowMs = 4_000L))
    }

    @Test
    fun matchesAReissuedDeviceIdByTypeAndProductName() {
        val policy = BluetoothResumePolicy()
        val beforeReconnect = BluetoothAudioDevice(id = 1, type = 8, productName = "Car Audio")
        val afterReconnect = BluetoothAudioDevice(id = 2, type = 8, productName = "car audio")

        policy.rememberDisconnected(listOf(beforeReconnect), nowMs = 1_000L)

        assertTrue(policy.consumeReconnect(listOf(afterReconnect), nowMs = 2_000L))
    }

    @Test
    fun resumesWhenReconnectUsesADifferentBluetoothProfile() {
        val policy = BluetoothResumePolicy()
        val a2dpCarAudio = BluetoothAudioDevice(id = 1, type = 8, productName = "Car Audio")
        val scoCarAudio = BluetoothAudioDevice(id = 2, type = 7, productName = "Car Audio")

        policy.rememberDisconnected(listOf(a2dpCarAudio), nowMs = 1_000L)

        assertTrue(policy.consumeReconnect(listOf(scoCarAudio), nowMs = 2_000L))
    }

    @Test
    fun resumesAtTheEndOfTheTwoHourWindow() {
        val policy = BluetoothResumePolicy()
        val headphones = BluetoothAudioDevice(id = 9, type = 8, productName = "Headphones")
        val deadline = 120 * 60 * 1_000L

        policy.rememberDisconnected(listOf(headphones), nowMs = 0L)

        assertTrue(policy.consumeReconnect(listOf(headphones), nowMs = deadline))
    }

    @Test
    fun doesNotResumeAfterTheTwoHourWindow() {
        val policy = BluetoothResumePolicy()
        val headphones = BluetoothAudioDevice(id = 9, type = 8, productName = "Headphones")
        val justAfterDeadline = (120 * 60 * 1_000L) + 1L

        policy.rememberDisconnected(listOf(headphones), nowMs = 0L)

        assertFalse(policy.consumeReconnect(listOf(headphones), nowMs = justAfterDeadline))
    }

    @Test
    fun matchesTheSameBluetoothAddressWhenNameAndProfileChange() {
        val policy = BluetoothResumePolicy()
        val beforeReconnect = BluetoothAudioDevice(
            id = 1,
            type = 8,
            productName = "Car Audio",
            address = "AA:BB:CC:DD:EE:FF",
        )
        val afterReconnect = BluetoothAudioDevice(
            id = 19,
            type = 7,
            productName = "Bluetooth device",
            address = "AA:BB:CC:DD:EE:FF",
        )

        policy.rememberDisconnected(listOf(beforeReconnect), nowMs = 1_000L)

        assertTrue(policy.consumeReconnect(listOf(afterReconnect), nowMs = 2_000L))
    }

    @Test
    fun resumesWhenReconnectHasNoStableDeviceIdentity() {
        val policy = BluetoothResumePolicy()
        val beforeReconnect = BluetoothAudioDevice(id = 3, type = 8, productName = "")
        val afterReconnect = BluetoothAudioDevice(id = 19, type = 7, productName = "")

        policy.rememberDisconnected(listOf(beforeReconnect), nowMs = 1_000L)

        assertTrue(policy.consumeReconnect(listOf(afterReconnect), nowMs = 2_000L))
    }

    @Test
    fun resumesWhenOnlyTheBluetoothRouteStateRecovers() {
        val policy = BluetoothResumePolicy()
        val headphones = BluetoothAudioDevice(id = 9, type = 8, productName = "Headphones")

        policy.rememberDisconnected(listOf(headphones), nowMs = 1_000L)

        assertTrue(policy.consumeRouteRecovery(nowMs = 2_000L))
        assertFalse(policy.consumeRouteRecovery(nowMs = 3_000L))
    }

    @Test
    fun mergesAConcurrentNoiseBroadcastWithTheRemovalCallback() {
        val policy = BluetoothResumePolicy()
        val headphones = BluetoothAudioDevice(id = 9, type = 8, productName = "Headphones")

        policy.rememberDisconnected(listOf(headphones.copy(id = 10)), nowMs = 1_000L)
        policy.mergeDisconnected(listOf(headphones), nowMs = 1_100L)

        assertTrue(policy.consumeReconnect(listOf(headphones), nowMs = 2_000L))
    }
}
