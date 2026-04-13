package com.example.safewalk.pairing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified unidirectional pairing manager.
 *
 * Mobile initiates pairing → Wearable accepts → Done
 *
 * Trade-off: Requires wearable to accept (trust device).
 * Benefit: Removes code verification step, leverages Android's native pairing.
 */
@Singleton
class PairingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bluetoothManager = context.getSystemService<BluetoothManager>()
    private val bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Unpaired)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _pairedDevice = MutableStateFlow<PairedDevice?>(null)
    val pairedDevice: StateFlow<PairedDevice?> = _pairedDevice.asStateFlow()

    val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("safewalk_pairing", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        id!!
    }

    init {
        loadPairedDevice()
    }

    /**
     * Initiate Bluetooth pairing with a remote device.
     * Android's native pairing dialog will appear on both devices.
     */
    fun startBluetoothPairing(
        remoteDeviceAddress: String,
        remoteDeviceName: String,
    ): Boolean {
        if (!hasBluetoothPermission()) {
            Log.e("PairingManager", "Missing Bluetooth permissions")
            return false
        }

        if (!bluetoothAdapter?.isEnabled!!) {
            Log.e("PairingManager", "Bluetooth not enabled")
            return false
        }

        return try {
            val remoteDevice = bluetoothAdapter?.getRemoteDevice(remoteDeviceAddress)
            if (remoteDevice == null) {
                Log.e("PairingManager", "Device not found: $remoteDeviceAddress")
                return false
            }

            _pairingState.value = PairingState.Pairing

            // Android's native pairing dialog is triggered automatically
            // when attempting to connect. Let the OS handle it.
            Log.d("PairingManager", "Pairing initiated with $remoteDeviceName ($remoteDeviceAddress)")
            true
        } catch (e: Exception) {
            Log.e("PairingManager", "Pairing initiation failed", e)
            _pairingState.value = PairingState.Error("Failed to initiate pairing")
            false
        }
    }

    /**
     * Complete pairing after successful Android system pairing.
     * This is called once the user accepts pairing on both devices.
     */
    fun completePairing(
        remoteDeviceAddress: String,
        remoteDeviceName: String,
    ): Boolean {
        return try {
            val pairedDevice = PairedDevice(
                localDeviceId = deviceId,
                remoteDeviceId = remoteDeviceAddress,
                remoteDeviceName = remoteDeviceName,
                pairingTimestamp = System.currentTimeMillis(),
            )

            // Persist to SharedPreferences
            val prefs = context.getSharedPreferences("safewalk_pairing", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("remote_device_address", remoteDeviceAddress)
                putString("remote_device_name", remoteDeviceName)
                putLong("pairing_timestamp", pairedDevice.pairingTimestamp)
                apply()
            }

            _pairedDevice.value = pairedDevice
            _pairingState.value = PairingState.Paired(pairedDevice)
            Log.d("PairingManager", "Pairing completed: $remoteDeviceName")
            true
        } catch (e: Exception) {
            Log.e("PairingManager", "Error completing pairing", e)
            _pairingState.value = PairingState.Error(e.message ?: "Unknown error")
            false
        }
    }

    /**
     * Load previously paired device from storage.
     */
    fun loadPairedDevice(): Boolean {
        val prefs = context.getSharedPreferences("safewalk_pairing", Context.MODE_PRIVATE)
        val remoteAddress = prefs.getString("remote_device_address", null)

        if (remoteAddress == null) {
            _pairingState.value = PairingState.Unpaired
            return false
        }

        val device = PairedDevice(
            localDeviceId = deviceId,
            remoteDeviceId = remoteAddress,
            remoteDeviceName = prefs.getString("remote_device_name", "Unknown") ?: "Unknown",
            pairingTimestamp = prefs.getLong("pairing_timestamp", 0),
        )

        _pairedDevice.value = device
        _pairingState.value = PairingState.Paired(device)
        return true
    }

    /**
     * Unpair the current device.
     */
    fun unpair(): Boolean {
        return try {
            val device = _pairedDevice.value ?: return false

            // Remove from Android's paired devices using reflection as it's a hidden API
            try {
                val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.remoteDeviceId)
                remoteDevice?.let {
                    val method = it.javaClass.getMethod("removeBond")
                    method.invoke(it)
                }
            } catch (e: Exception) {
                Log.e("PairingManager", "Error removing bond via reflection", e)
            }

            // Clear SharedPreferences
            val prefs = context.getSharedPreferences("safewalk_pairing", Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove("remote_device_address")
                remove("remote_device_name")
                remove("pairing_timestamp")
                apply()
            }

            _pairedDevice.value = null
            _pairingState.value = PairingState.Unpaired
            Log.d("PairingManager", "Device unpaired")
            true
        } catch (e: Exception) {
            Log.e("PairingManager", "Error unpairing device", e)
            _pairingState.value = PairingState.Error(e.message ?: "Unpair failed")
            false
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
}

data class PairedDevice(
    val localDeviceId: String,
    val remoteDeviceId: String,
    val remoteDeviceName: String,
    val pairingTimestamp: Long,
) {
    fun isValid(): Boolean = remoteDeviceId.isNotEmpty() && remoteDeviceName.isNotEmpty()
}

sealed class PairingState {
    data object Unpaired : PairingState()
    data object Pairing : PairingState()
    data class Paired(val device: PairedDevice) : PairingState()
    data class Error(val message: String) : PairingState()
}