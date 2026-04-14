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

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingManager: PairingManager,
) : ViewModel() {

    val pairingState: StateFlow<PairingState> = pairingManager.pairingState
    val pairedDevice = pairingManager.pairedDevice

    private val _pairingEvent = MutableSharedFlow<PairingEvent>()
    val pairingEvent = _pairingEvent.asSharedFlow()

    /**
     * Query the Wearable Data Layer for connected nodes.
     * Replaces the old Bluetooth-based scan — no custom handshake required.
     */
    fun checkConnection() {
        viewModelScope.launch {
            _pairingEvent.emit(PairingEvent.PairingInitiated)
            val connected = pairingManager.checkWearableConnection()
            if (connected) {
                _pairingEvent.emit(PairingEvent.PairingSuccess)
            } else {
                _pairingEvent.emit(
                    PairingEvent.PairingFailed("No wearable found. Make sure your watch is paired and nearby.")
                )
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
