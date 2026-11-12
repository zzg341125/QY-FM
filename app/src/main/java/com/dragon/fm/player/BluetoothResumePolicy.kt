package com.dragon.fm.player

internal data class BluetoothAudioDevice(
    val id: Int,
    val type: Int,
    val productName: String,
)

/**
 * Remembers the Bluetooth output that was active before a playback pause.
 * Reconnects may change profile or device identity, so known devices and
 * devices without stable metadata are accepted while unrelated named devices
 * are still rejected.
 */
internal class BluetoothResumePolicy {
    private var disconnectedDevices: Set<BluetoothAudioDevice> = emptySet()
    private var hasPendingReconnect = false

    fun rememberDisconnected(devices: Collection<BluetoothAudioDevice>) {
        val knownDevices = devices.filter { it.id >= 0 }.toSet()
        if (knownDevices.isEmpty()) {
            clear()
            return
        }
        disconnectedDevices = knownDevices
        hasPendingReconnect = true
    }

    fun mergeDisconnected(devices: Collection<BluetoothAudioDevice>) {
        if (!hasPendingReconnect || devices.isEmpty()) return
        disconnectedDevices = disconnectedDevices + devices
    }

    fun consumeReconnect(addedDevices: Collection<BluetoothAudioDevice>): Boolean {
        if (!hasPendingReconnect) return false
        val matchedKnownDevice = addedDevices.any { added ->
            disconnectedDevices.any { disconnected -> added.matches(disconnected) }
        }
        val resumed = matchedKnownDevice || addedDevices.any { it.productName.isBlank() }
        if (resumed) clear()
        return resumed
    }

    fun clear() {
        disconnectedDevices = emptySet()
        hasPendingReconnect = false
    }

    private fun BluetoothAudioDevice.matches(other: BluetoothAudioDevice): Boolean {
        if (id >= 0 && id == other.id) return true
        return productName.isNotBlank() &&
            productName.equals(other.productName, ignoreCase = true)
    }
}
