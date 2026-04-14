package com.wear.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Wearable
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
import kotlinx.coroutines.tasks.await

/**
 * Wearable pairing ViewModel.
 *
 * Pairing state is derived from NodeClient.connectedNodes — this directly
 * reflects whether the phone is connected at the Wearable platform level,
 * which is what Android Studio / emulator pairing establishes.
 */
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
            try {
                val nodes = Wearable.getNodeClient(dataLayerManager.context)
                    .connectedNodes
                    .await()
                _pairingState.value = if (nodes.isNotEmpty()) {
                    Log.d("WearPairingVM", "Connected to phone: ${nodes.first().displayName}")
                    PairingState.Paired
                } else {
                    Log.d("WearPairingVM", "No phone node connected")
                    PairingState.Unpaired
                }
            } catch (e: Exception) {
                Log.e("WearPairingVM", "Error checking connection state", e)
                _pairingState.value = PairingState.Unpaired
            }
        }
    }

    fun togglePairingMenu() {
        _showPairingMenu.update { !it }
        // Re-check live connection state when the user opens the pairing menu
        if (_showPairingMenu.value) loadPairingState()
    }

    /**
     * Clear app-level pairing indicator. Note: the OS-level Wear pairing
     * remains; use Wear OS / Bluetooth settings to fully unpair.
     */
    fun unpair() {
        viewModelScope.launch {
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
        Paired,
    }
}
