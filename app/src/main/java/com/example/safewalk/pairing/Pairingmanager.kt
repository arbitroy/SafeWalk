package com.example.safewalk.pairing

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pairing manager using the Wearable Data Layer NodeClient.
 *
 * "Paired" means Android has already connected the watch at the OS level —
 * NodeClient.connectedNodes reflects that truth directly. No custom Bluetooth
 * handshake is needed; we just query what the platform already knows.
 */
@Singleton
class PairingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nodeClient = Wearable.getNodeClient(context)

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
     * Restore last-known pairing state from cache for instant UI on cold start.
     * Always follow up with checkWearableConnection() for an authoritative check.
     */
    fun loadPairedDevice(): Boolean {
        val prefs = context.getSharedPreferences("safewalk_pairing", Context.MODE_PRIVATE)
        val remoteNodeId = prefs.getString("remote_device_address", null) ?: return false

        val device = PairedDevice(
            localDeviceId = deviceId,
            remoteDeviceId = remoteNodeId,
            remoteDeviceName = prefs.getString("remote_device_name", "SafeWalk Watch") ?: "SafeWalk Watch",
            pairingTimestamp = prefs.getLong("pairing_timestamp", 0),
        )
        _pairedDevice.value = device
        _pairingState.value = PairingState.Paired(device)
        return true
    }

    /**
     * Query the Wearable Data Layer for connected nodes.
     *
     * This is the authoritative check — it reflects whether the OS considers
     * the watch connected, which is exactly what Android Studio / emulator
     * pairing establishes. No custom Bluetooth handshake is required.
     */
    suspend fun checkWearableConnection(): Boolean {
        Log.d("SW_PAIRING", "checkWearableConnection() — querying NodeClient for connected nodes")
        return try {
            val nodes = nodeClient.connectedNodes.await()
            Log.d("SW_PAIRING", "NodeClient returned ${nodes.size} node(s): ${nodes.map { "${it.displayName}(id=${it.id} nearby=${it.isNearby})" }}")
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                Log.d("SW_PAIRING", "Using first node: displayName='${node.displayName}'  id='${node.id}'  isNearby=${node.isNearby}")
                val device = PairedDevice(
                    localDeviceId = deviceId,
                    remoteDeviceId = node.id,
                    remoteDeviceName = node.displayName,
                    pairingTimestamp = System.currentTimeMillis(),
                )
                val prefs = context.getSharedPreferences("safewalk_pairing", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("remote_device_address", node.id)
                    putString("remote_device_name", node.displayName)
                    putLong("pairing_timestamp", device.pairingTimestamp)
                    apply()
                }
                _pairedDevice.value = device
                _pairingState.value = PairingState.Paired(device)
                Log.d("SW_PAIRING", "Pairing cached ✓ remoteId='${node.id}' name='${node.displayName}'")
                true
            } else {
                Log.w("SW_PAIRING", "No connected nodes found — check that watch is paired in Wear OS / BT settings and nearby")
                _pairedDevice.value = null
                _pairingState.value = PairingState.Unpaired
                val prefs = context.getSharedPreferences("safewalk_pairing", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    remove("remote_device_address")
                    remove("remote_device_name")
                    remove("pairing_timestamp")
                    apply()
                }
                false
            }
        } catch (e: Exception) {
            Log.e("SW_PAIRING", "Error checking wearable connection", e)
            _pairingState.value = PairingState.Error("Failed to check connection: ${e.message}")
            false
        }
    }

    /**
     * Clear app-level pairing state. Note: this does not remove the OS-level
     * Wear pairing — use system Bluetooth settings for that.
     */
    fun unpair(): Boolean {
        return try {
            val prefs = context.getSharedPreferences("safewalk_pairing", Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove("remote_device_address")
                remove("remote_device_name")
                remove("pairing_timestamp")
                apply()
            }
            _pairedDevice.value = null
            _pairingState.value = PairingState.Unpaired
            Log.d("PairingManager", "App-level pairing cleared")
            true
        } catch (e: Exception) {
            Log.e("PairingManager", "Error clearing pairing", e)
            _pairingState.value = PairingState.Error(e.message ?: "Unpair failed")
            false
        }
    }
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
