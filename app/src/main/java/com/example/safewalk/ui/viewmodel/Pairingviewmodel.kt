package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.pairing.PairingManager
import com.example.safewalk.pairing.PairingState
import com.example.safewalk.pairing.TransportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
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

    private val _pairingEvent = kotlinx.coroutines.flow.MutableSharedFlow<PairingEvent>()
    val pairingEvent: SharedFlow<PairingEvent> = _pairingEvent.asSharedFlow()

    fun generatePairingCode() {
        viewModelScope.launch {
            val code = pairingManager.generatePairingCode()
            pairingManager.startBluetoothPairing(code)
            _pairingEvent.emit(PairingEvent.CodeGenerated(code))
        }
    }

    fun confirmPairingCode(wearableCode: String) {
        viewModelScope.launch {
            val currentState = pairingManager.pairingState.value
            if (currentState !is PairingState.Pairing) {
                _pairingEvent.emit(PairingEvent.PairingFailed("Invalid pairing state"))
                return@launch
            }

            val mobileCode = currentState.code
            if (wearableCode.uppercase() != mobileCode.uppercase()) {
                _pairingEvent.emit(PairingEvent.PairingFailed("Code mismatch. Check and try again."))
                return@launch
            }

            try {
                val success = pairingManager.completePairing(
                    remoteDeviceId = "wear_emulator_001",
                    remoteDeviceName = "SafeWalk Watch",
                    transportType = TransportType.BLUETOOTH,
                    pairingCode = mobileCode,
                )

                if (success) {
                    _pairingEvent.emit(PairingEvent.PairingSuccess("Bluetooth"))
                } else {
                    _pairingEvent.emit(PairingEvent.PairingFailed("Authentication failed"))
                }
            } catch (e: Exception) {
                _pairingEvent.emit(PairingEvent.PairingFailed(e.message ?: "Unknown error"))
            }
        }
    }

    fun unpair() {
        viewModelScope.launch {
            pairingManager.unpair()
            _pairingEvent.emit(PairingEvent.Unpaired)
        }
    }

    fun cancelPairing() {
        pairingManager.unpair()
    }

    fun resetPairingState() {
        pairingManager.loadPairedDevice()
    }

    sealed class PairingEvent {
        data class CodeGenerated(val code: String) : PairingEvent()
        data class PairingSuccess(val transportType: String) : PairingEvent()
        data class PairingFailed(val reason: String) : PairingEvent()
        data object Unpaired : PairingEvent()
    }
}