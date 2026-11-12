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

        policy.rememberDisconnected(listOf(headphones))

        assertFalse(policy.consumeReconnect(listOf(speaker)))
        assertTrue(policy.consumeReconnect(listOf(headphones)))
        assertFalse(policy.consumeReconnect(listOf(headphones)))
    }

    @Test
    fun matchesAReissuedDeviceIdByTypeAndProductName() {
        val policy = BluetoothResumePolicy()
        val beforeReconnect = BluetoothAudioDevice(id = 1, type = 8, productName = "Car Audio")
        val afterReconnect = BluetoothAudioDevice(id = 2, type = 8, productName = "car audio")

        policy.rememberDisconnected(listOf(beforeReconnect))

        assertTrue(policy.consumeReconnect(listOf(afterReconnect)))
    }

    @Test
    fun resumesWhenReconnectUsesADifferentBluetoothProfile() {
        val policy = BluetoothResumePolicy()
        val a2dpCarAudio = BluetoothAudioDevice(id = 1, type = 8, productName = "Car Audio")
        val scoCarAudio = BluetoothAudioDevice(id = 2, type = 7, productName = "Car Audio")

        policy.rememberDisconnected(listOf(a2dpCarAudio))

        assertTrue(policy.consumeReconnect(listOf(scoCarAudio)))
    }

    @Test
    fun keepsResumePendingUntilTheUserConnectsBluetoothAgain() {
        val policy = BluetoothResumePolicy()
        val headphones = BluetoothAudioDevice(id = 9, type = 8, productName = "Headphones")

        policy.rememberDisconnected(listOf(headphones))

        assertTrue(policy.consumeReconnect(listOf(headphones)))
    }

    @Test
    fun resumesWhenReconnectHasNoStableDeviceIdentity() {
        val policy = BluetoothResumePolicy()
        val beforeReconnect = BluetoothAudioDevice(id = 3, type = 8, productName = "")
        val afterReconnect = BluetoothAudioDevice(id = 19, type = 7, productName = "")

        policy.rememberDisconnected(listOf(beforeReconnect))

        assertTrue(policy.consumeReconnect(listOf(afterReconnect)))
    }

    @Test
    fun mergesAConcurrentNoiseBroadcastWithTheRemovalCallback() {
        val policy = BluetoothResumePolicy()
        val headphones = BluetoothAudioDevice(id = 9, type = 8, productName = "Headphones")

        policy.rememberDisconnected(listOf(headphones.copy(id = 10)))
        policy.mergeDisconnected(listOf(headphones))

        assertTrue(policy.consumeReconnect(listOf(headphones)))
    }
}
