package com.dragon.fm.player

internal data class BluetoothAudioDevice(
    val id: Int,
    val type: Int,
    val productName: String,
)

/**
 * Remembers the Bluetooth output that was active before a playback pause.
 * Auto-resume is allowed only for the same physical output and within a
 * bounded window, so connecting an unrelated device cannot start the radio.
 */
internal class BluetoothResumePolicy(
    private val resumeWindowMs: Long = 2 * 60 * 1000L,
) {
    private var disconnectedDevices: Set<BluetoothAudioDevice> = emptySet()
    private var pausedAtMs: Long? = null

    fun rememberDisconnected(devices: Collection<BluetoothAudioDevice>, nowMs: Long) {
        val knownDevices = devices.filter { it.id >= 0 }.toSet()
        if (knownDevices.isEmpty()) {
            clear()
            return
        }
        disconnectedDevices = knownDevices
        pausedAtMs = nowMs
    }

    fun mergeDisconnected(devices: Collection<BluetoothAudioDevice>, nowMs: Long) {
        if (pausedAtMs == null || devices.isEmpty()) return
        disconnectedDevices = disconnectedDevices + devices
        pausedAtMs = nowMs
    }

    fun consumeReconnect(addedDevices: Collection<BluetoothAudioDevice>, nowMs: Long): Boolean {
        val pausedAt = pausedAtMs ?: return false
        if (nowMs - pausedAt > resumeWindowMs) {
            clear()
            return false
        }

        val resumed = addedDevices.any { added ->
            disconnectedDevices.any { disconnected -> added.matches(disconnected) }
        }
        if (resumed) clear()
        return resumed
    }

    fun clear() {
        disconnectedDevices = emptySet()
        pausedAtMs = null
    }

    private fun BluetoothAudioDevice.matches(other: BluetoothAudioDevice): Boolean {
        if (id >= 0 && id == other.id) return true
        return type == other.type &&
            productName.isNotBlank() &&
            productName.equals(other.productName, ignoreCase = true)
    }
}
