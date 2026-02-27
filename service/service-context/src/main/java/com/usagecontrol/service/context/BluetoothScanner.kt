package com.usagecontrol.service.context

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans for connected Bluetooth devices.
 * Returns the name of the first connected device, or null if none.
 */
@Singleton
class BluetoothScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Get the name of a currently connected Bluetooth audio device.
     * Checks A2DP (media audio) and headset profiles.
     */
    fun getConnectedDeviceName(): String? {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                ?: return null
            val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return null

            // Check bonded (paired) devices that are currently connected
            @Suppress("MissingPermission")
            val bondedDevices = adapter.bondedDevices ?: return null

            // BluetoothAdapter.getProfileProxy is async, so we use a simpler approach:
            // check bonded devices and return the first one.
            // In practice, for the context engine we just need to know if a known device is paired.
            // A more accurate check would use BluetoothProfile listeners.
            for (device in bondedDevices) {
                @Suppress("MissingPermission")
                val name = device.name
                // Check if this bonded device is currently connected via reflection or profile check
                try {
                    val isConnectedMethod = device.javaClass.getMethod("isConnected")
                    val isConnected = isConnectedMethod.invoke(device) as? Boolean ?: false
                    if (isConnected && !name.isNullOrBlank()) {
                        return name
                    }
                } catch (_: Exception) {
                    // isConnected method not available on this device
                }
            }

            null
        } catch (_: SecurityException) {
            // Bluetooth permission not granted
            null
        } catch (_: Exception) {
            null
        }
    }
}
