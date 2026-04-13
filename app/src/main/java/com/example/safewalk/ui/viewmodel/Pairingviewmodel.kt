package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.pairing.PairingManager
import com.example.safewalk.pairing.PairingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simplified pairing ViewModel using unidirectional flow.
 *
 * No code verification needed - Android's native pairing handles authentication.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingManager: PairingManager,
) : ViewModel() {

    val pairingState: StateFlow<PairingState> = pairingManager.pairingState
    val pairedDevice = pairingManager.pairedDevice

    private val _pairingEvent = MutableSharedFlow<PairingEvent>()
    val pairingEvent = _pairingEvent.asSharedFlow()

    /**
     * Scan for nearby Bluetooth devices and initiate pairing.
     * In production, this would use BLE scanning with proper discovery.
     */
    fun scanAndPair(remoteDeviceAddress: String, remoteDeviceName: String) {
        viewModelScope.launch {
            val success = pairingManager.startBluetoothPairing(remoteDeviceAddress, remoteDeviceName)

            if (success) {
                _pairingEvent.emit(PairingEvent.PairingInitiated)
            } else {
                _pairingEvent.emit(PairingEvent.PairingFailed("Failed to initiate pairing"))
            }
        }
    }

    /**
     * Confirm pairing after user accepts on wearable.
     * Called when Android system notifies pairing is complete.
     */
    fun confirmPairingComplete(remoteDeviceAddress: String, remoteDeviceName: String) {
        viewModelScope.launch {
            val success = pairingManager.completePairing(remoteDeviceAddress, remoteDeviceName)

            if (success) {
                _pairingEvent.emit(PairingEvent.PairingSuccess)
            } else {
                _pairingEvent.emit(PairingEvent.PairingFailed("Failed to complete pairing"))
            }
        }
    }

    fun unpair() {
        viewModelScope.launch {
            val success = pairingManager.unpair()

            if (success) {
                _pairingEvent.emit(PairingEvent.Unpaired)
            } else {
                _pairingEvent.emit(PairingEvent.PairingFailed("Failed to unpair"))
            }
        }
    }

    sealed class PairingEvent {
        data object PairingInitiated : PairingEvent()
        data object PairingSuccess : PairingEvent()
        data object Unpaired : PairingEvent()
        data class PairingFailed(val reason: String) : PairingEvent()
    }
}