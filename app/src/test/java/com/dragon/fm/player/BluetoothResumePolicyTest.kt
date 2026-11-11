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
    fun expiresAfterTheResumeWindow() {
        val policy = BluetoothResumePolicy(resumeWindowMs = 5_000L)
        val headphones = BluetoothAudioDevice(id = 9, type = 8, productName = "Headphones")

        policy.rememberDisconnected(listOf(headphones), nowMs = 1_000L)

        assertFalse(policy.consumeReconnect(listOf(headphones), nowMs = 6_001L))
        assertFalse(policy.consumeReconnect(listOf(headphones), nowMs = 6_002L))
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
