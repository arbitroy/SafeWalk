package com.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wear.data.SafeWalkSession
import com.wear.data.WearDataLayerManager
import com.wear.data.WearRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WearPairingViewModel @Inject constructor(
    private val dataLayerManager: WearDataLayerManager,
    private val repository: WearRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SafeWalkSession>(SafeWalkSession.Idle)
    val session: StateFlow<SafeWalkSession> = _session.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Unpaired)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _showPairingMenu = MutableStateFlow(false)
    val showPairingMenu: StateFlow<Boolean> = _showPairingMenu.asStateFlow()

    init {
        loadPairingState()
        startTimerUpdates()
        dataLayerManager.startListening(viewModelScope)
    }

    private fun loadPairingState() {
        viewModelScope.launch {
            val prefs = dataLayerManager.context.getSharedPreferences("wear_pairing", 0)
            val pairedDeviceId = prefs.getString("paired_device_id", null)

            if (pairedDeviceId != null) {
                _pairingState.value = PairingState.Paired
            } else {
                _pairingState.value = PairingState.Unpaired
            }
        }
    }

    fun togglePairingMenu() {
        _showPairingMenu.update { !it }
    }

    fun confirmPairing(code: String) {
        if (code.length != 8) return

        viewModelScope.launch {
            _pairingState.value = PairingState.Pairing

            // Validate code format
            if (!code.all { it.isLetterOrDigit() }) {
                _pairingState.value = PairingState.Error
                delay(1500)
                _pairingState.value = PairingState.Unpaired
                return@launch
            }

            // Wait 3 seconds for phone confirmation
            delay(3000)

            // In production, phone would send confirmation via Data Layer
            // For emulator: simulate successful pairing
            val prefs = dataLayerManager.context.getSharedPreferences("wear_pairing", 0)
            prefs.edit().apply {
                putString("paired_device_id", "emulator_phone_001")
                putString("pairing_code", code)
                putLong("pairing_time", System.currentTimeMillis())
                apply()
            }

            _pairingState.value = PairingState.Paired
            delay(1500)
            _showPairingMenu.value = false
        }
    }

    fun cancelPairing() {
        _pairingState.value = PairingState.Unpaired
    }

    fun unpair() {
        viewModelScope.launch {
            val prefs = dataLayerManager.context.getSharedPreferences("wear_pairing", 0)
            prefs.edit().apply {
                remove("paired_device_id")
                remove("pairing_code")
                remove("pairing_time")
                apply()
            }

            _pairingState.value = PairingState.Unpaired
            _showPairingMenu.value = false
        }
    }

    fun checkIn(status: String) {
        viewModelScope.launch {
            dataLayerManager.sendCheckIn(status)
            _session.value = SafeWalkSession.Idle
            _remainingSeconds.value = 0
        }
    }

    fun triggerSOS() {
        viewModelScope.launch {
            dataLayerManager.sendCheckIn("SOS")
        }
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            while (true) {
                if (_session.value is SafeWalkSession.Active) {
                    delay(1000)
                    _remainingSeconds.update { current ->
                        if (current > 0) current - 1 else 0
                    }
                } else {
                    delay(100)
                }
            }
        }
    }

    enum class PairingState {
        Unpaired,
        Pairing,
        Paired,
        Error,
    }
}