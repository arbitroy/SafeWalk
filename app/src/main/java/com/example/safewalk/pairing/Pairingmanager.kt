package com.example.safewalk.pairing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
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

@Singleton
class PairingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bluetoothManager = context.getSystemService<BluetoothManager>()
    private val bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    private val wifiManager = context.getSystemService<WifiManager>()

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Unpaired)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _pairedDevice = MutableStateFlow<PairedDevice?>(null)
    val pairedDevice: StateFlow<PairedDevice?> = _pairedDevice.asStateFlow()

    // Unique device identifier generated on first run
    val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("pairing", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        id!!
    }

    // Generate pairing code (8 char alphanumeric)
    fun generatePairingCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }

    // Validate pairing code format
    fun validatePairingCode(code: String): Boolean {
        return code.length == 8 && code.all { it.isLetterOrDigit() }
    }

    // Initiate Bluetooth pairing
    fun startBluetoothPairing(pairingCode: String): Boolean {
        if (!bluetoothAdapter?.isEnabled!!) {
            Log.e("PairingManager", "Bluetooth not enabled")
            return false
        }

        if (!validatePairingCode(pairingCode)) {
            Log.e("PairingManager", "Invalid pairing code: $pairingCode")
            return false
        }

        _pairingState.value = PairingState.Pairing(pairingCode, TransportType.BLUETOOTH)

        // In production, discovery would happen here via BluetoothAdapter.startDiscovery()
        // For emulator testing, use known device addresses
        return true
    }

    // Initiate Wi-Fi Direct pairing
    fun startWiFiPairing(pairingCode: String): Boolean {
        if (!validatePairingCode(pairingCode)) {
            Log.e("PairingManager", "Invalid pairing code: $pairingCode")
            return false
        }

        _pairingState.value = PairingState.Pairing(pairingCode, TransportType.WIFI)

        // In production, Wi-Fi Direct discovery would happen here
        return true
    }

    // Complete pairing after successful authentication
    fun completePairing(
        remoteDeviceId: String,
        remoteDeviceName: String,
        transportType: TransportType,
        pairingCode: String,
    ): Boolean {
        val currentState = _pairingState.value
        if (currentState !is PairingState.Pairing || currentState.code != pairingCode) {
            Log.e("PairingManager", "Pairing code mismatch or invalid state")
            return false
        }

        val pairedDevice = PairedDevice(
            localDeviceId = deviceId,
            remoteDeviceId = remoteDeviceId,
            remoteDeviceName = remoteDeviceName,
            transportType = transportType,
            pairingTimestamp = System.currentTimeMillis(),
            pairingCode = pairingCode,
        )

        // Persist pairing to SharedPreferences
        val prefs = context.getSharedPreferences("pairing", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("remote_device_id", pairedDevice.remoteDeviceId)
            putString("remote_device_name", pairedDevice.remoteDeviceName)
            putString("transport_type", pairedDevice.transportType.name)
            putLong("pairing_timestamp", pairedDevice.pairingTimestamp)
            apply()
        }

        _pairedDevice.value = pairedDevice
        _pairingState.value = PairingState.Paired(pairedDevice)
        return true
    }

    // Load previously paired device
    fun loadPairedDevice(): Boolean {
        val prefs = context.getSharedPreferences("pairing", Context.MODE_PRIVATE)
        val remoteId = prefs.getString("remote_device_id", null)

        if (remoteId == null) {
            _pairingState.value = PairingState.Unpaired
            return false
        }

        val device = PairedDevice(
            localDeviceId = deviceId,
            remoteDeviceId = remoteId,
            remoteDeviceName = prefs.getString("remote_device_name", "Unknown") ?: "Unknown",
            transportType = TransportType.valueOf(
                prefs.getString("transport_type", "BLUETOOTH") ?: "BLUETOOTH"
            ),
            pairingTimestamp = prefs.getLong("pairing_timestamp", 0),
            pairingCode = prefs.getString("pairing_code", "") ?: "",
        )

        _pairedDevice.value = device
        _pairingState.value = PairingState.Paired(device)
        return true
    }

    // Unpair current device
    fun unpair() {
        val prefs = context.getSharedPreferences("pairing", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("remote_device_id")
            remove("remote_device_name")
            remove("transport_type")
            remove("pairing_timestamp")
            apply()
        }

        _pairedDevice.value = null
        _pairingState.value = PairingState.Unpaired
    }
}

data class PairedDevice(
    val localDeviceId: String,
    val remoteDeviceId: String,
    val remoteDeviceName: String,
    val transportType: TransportType,
    val pairingTimestamp: Long,
    val pairingCode: String,
) {
    fun isValid(): Boolean = remoteDeviceId.isNotEmpty() && remoteDeviceName.isNotEmpty()
}

sealed class PairingState {
    data object Unpaired : PairingState()
    data class Pairing(val code: String, val transportType: TransportType) : PairingState()
    data class Paired(val device: PairedDevice) : PairingState()
    data class Error(val message: String) : PairingState()
}

enum class TransportType {
    BLUETOOTH,
    WIFI,
}