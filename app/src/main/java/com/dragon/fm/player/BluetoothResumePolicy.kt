package com.dragon.fm.player

internal data class BluetoothAudioDevice(
    val id: Int,
    val type: Int,
    val productName: String,
    val address: String = "",
)

/**
 * Remembers the Bluetooth output that was active before a playback pause.
 * The same output is eligible for auto-resume for up to 120 minutes.
 * Reconnects may change profile or device identity, so the Bluetooth address,
 * device ID, or product name can all be used for matching.
 */
internal class BluetoothResumePolicy {
    private var disconnectedDevices: Set<BluetoothAudioDevice> = emptySet()
    private var expiresAtMs: Long? = null

    fun rememberDisconnected(
        devices: Collection<BluetoothAudioDevice>,
        nowMs: Long,
    ) {
        val knownDevices = devices.filter { it.id >= 0 }.toSet()
        if (knownDevices.isEmpty()) {
            clear()
            return
        }
        disconnectedDevices = knownDevices
        expiresAtMs = nowMs + RESUME_WINDOW_MS
    }

    fun mergeDisconnected(
        devices: Collection<BluetoothAudioDevice>,
        nowMs: Long,
    ) {
        if (!hasPending(nowMs) || devices.isEmpty()) return
        disconnectedDevices = disconnectedDevices + devices
    }

    fun consumeReconnect(
        addedDevices: Collection<BluetoothAudioDevice>,
        nowMs: Long,
    ): Boolean {
        if (!hasPending(nowMs)) return false
        val matchedKnownDevice = addedDevices.any { added ->
            disconnectedDevices.any { disconnected -> added.matches(disconnected) }
        }
        val unknownDisconnected = disconnectedDevices.any { it.hasNoStableIdentity }
        val unknownAdded = addedDevices.any { it.hasNoStableIdentity }
        val resumed = matchedKnownDevice || (unknownDisconnected && unknownAdded)
        if (resumed) clear()
        return resumed
    }

    fun consumeRouteRecovery(nowMs: Long): Boolean {
        if (!hasPending(nowMs)) return false
        clear()
        return true
    }

    fun hasPending(nowMs: Long): Boolean {
        val deadline = expiresAtMs
        if (deadline == null) return false
        if (nowMs > deadline) {
            clear()
            return false
        }
        return true
    }

    fun clear() {
        disconnectedDevices = emptySet()
        expiresAtMs = null
    }

    private fun BluetoothAudioDevice.matches(other: BluetoothAudioDevice): Boolean {
        if (address.isNotBlank() && address.equals(other.address, ignoreCase = true)) return true
        if (id >= 0 && id == other.id) return true
        return productName.isNotBlank() &&
            productName.equals(other.productName, ignoreCase = true)
    }

    private val BluetoothAudioDevice.hasNoStableIdentity: Boolean
        get() = address.isBlank() && productName.isBlank()

    companion object {
        const val RESUME_WINDOW_MS = 120L * 60L * 1_000L
    }
}
