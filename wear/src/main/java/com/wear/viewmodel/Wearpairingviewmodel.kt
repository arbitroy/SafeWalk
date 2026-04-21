package com.wear.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wear.data.SafeWalkSession
import com.wear.data.WearFirebaseSyncManager
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
    private val syncManager: WearFirebaseSyncManager,
    private val repository: WearRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SafeWalkSession>(SafeWalkSession.Idle)
    val session: StateFlow<SafeWalkSession> = _session.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _pairingState = MutableStateFlow(
        if (syncManager.sessionCode != null) PairingState.Paired else PairingState.Unpaired
    )
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _showPairingMenu = MutableStateFlow(false)
    val showPairingMenu: StateFlow<Boolean> = _showPairingMenu.asStateFlow()

    private val _pairingError = MutableStateFlow<String?>(null)
    val pairingError: StateFlow<String?> = _pairingError.asStateFlow()

    init {
        Log.d("SW_WEAR_VM", "WearPairingViewModel init")
        if (syncManager.sessionCode != null) {
            syncManager.startListening(viewModelScope)
        }

        viewModelScope.launch {
            Log.d("SW_WEAR_VM", "Starting repository.timerState collector")
            repository.timerState.collect { timerData ->
                _session.value = timerData.session
                _remainingSeconds.value = timerData.remainingSeconds
            }
        }

        startTimerUpdates()
    }

    fun pairWithCode(code: String) {
        _pairingError.value = null
        syncManager.pairWithCode(code, viewModelScope) { success ->
            if (success) {
                Log.d("SW_WEAR_VM", "Paired with code=$code")
                _pairingState.value = PairingState.Paired
                _showPairingMenu.value = false
            } else {
                Log.w("SW_WEAR_VM", "Failed to pair with code=$code")
                _pairingError.value = "Could not connect. Check the code and try again."
            }
        }
    }

    fun togglePairingMenu() {
        _showPairingMenu.update { !it }
        _pairingError.value = null
    }

    fun unpair() {
        syncManager.unpair()
        _pairingState.value = PairingState.Unpaired
        _showPairingMenu.value = false
        _session.value = SafeWalkSession.Idle
        _remainingSeconds.value = 0
    }

    fun requestStart(durationMinutes: Int = 30) {
        Log.d("SW_WEAR_VM", "requestStart($durationMinutes min)")
        viewModelScope.launch {
            syncManager.sendTimerStartRequest(durationMinutes)
        }
    }

    fun checkIn(status: String) {
        Log.d("SW_WEAR_VM", "checkIn($status)")
        viewModelScope.launch {
            syncManager.sendCheckIn(status)
            _session.value = SafeWalkSession.Idle
            _remainingSeconds.value = 0
        }
    }

    fun triggerSOS() {
        Log.d("SW_WEAR_VM", "triggerSOS()")
        viewModelScope.launch {
            syncManager.sendCheckIn("SOS")
        }
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            while (true) {
                if (_session.value is SafeWalkSession.Active) {
                    delay(1000)
                    _remainingSeconds.update { current -> if (current > 0) current - 1 else 0 }
                } else {
                    delay(100)
                }
            }
        }
    }

    enum class PairingState {
        Unpaired,
        Paired,
    }
}
