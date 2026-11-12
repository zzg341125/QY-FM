package com.dragon.fm.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dragon.fm.DragonFmApp
import com.dragon.fm.player.BluetoothAudioDevice

/**
 * Debug-only transport simulator used by the emulator QA flow.
 * This receiver is not included in release APKs.
 */
class BluetoothResumeDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? DragonFmApp ?: return
        val device = BluetoothAudioDevice(
            id = DEBUG_DEVICE_ID,
            type = 8,
            productName = DEBUG_DEVICE_NAME,
            address = DEBUG_DEVICE_ADDRESS,
        )
        when (intent.action) {
            ACTION_DISCONNECT -> app.radioPlayer.handleBluetoothDevicesRemoved(listOf(device))
            ACTION_RECONNECT -> app.radioPlayer.handleBluetoothDevicesAdded(listOf(device))
            else -> return
        }
        Log.i(TAG, "Simulated Bluetooth event: ${intent.action}")
    }

    companion object {
        const val ACTION_DISCONNECT = "com.dragon.fm.debug.BLUETOOTH_DISCONNECT"
        const val ACTION_RECONNECT = "com.dragon.fm.debug.BLUETOOTH_RECONNECT"

        private const val TAG = "BluetoothResumeDebug"
        private const val DEBUG_DEVICE_ID = 99001
        private const val DEBUG_DEVICE_NAME = "QY Debug Bluetooth"
        private const val DEBUG_DEVICE_ADDRESS = "AA:BB:CC:DD:EE:FF"
    }
}
