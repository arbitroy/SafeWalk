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
        Log.d("SW_WEAR_VM", "WearPairingViewModel init — starting up")
        loadPairingState()
        dataLayerManager.startListening(viewModelScope)

        // Mirror phone-authoritative timer state into local UI state
        viewModelScope.launch {
            Log.d("SW_WEAR_VM", "Starting repository.timerState collector")
            repository.timerState.collect { timerData ->
                val sessionName = timerData.session::class.simpleName
                Log.d("SW_WEAR_VM", "repository.timerState emitted → session=$sessionName  remaining=${timerData.remainingSeconds}s")
                _session.value = timerData.session
                _remainingSeconds.value = timerData.remainingSeconds
                Log.d("SW_WEAR_VM", "_session and _remainingSeconds updated — UI should refresh")
            }
        }

        startTimerUpdates()
    }

    private fun loadPairingState() {
        viewModelScope.launch {
            try {
                val nodes = Wearable.getNodeClient(dataLayerManager.context)
                    .connectedNodes
                    .await()
                Log.d("SW_WEAR_VM", "loadPairingState: found ${nodes.size} connected node(s): ${nodes.map { "${it.displayName}(${it.id})" }}")
                _pairingState.value = if (nodes.isNotEmpty()) {
                    Log.d("SW_WEAR_VM", "Paired — phone is ${nodes.first().displayName} id=${nodes.first().id}")
                    PairingState.Paired
                } else {
                    Log.w("SW_WEAR_VM", "No phone node found — showing Unpaired")
                    PairingState.Unpaired
                }
            } catch (e: Exception) {
                Log.e("SW_WEAR_VM", "Error checking connection state", e)
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

    fun requestStart(durationMinutes: Int = 30) {
        Log.d("SW_WEAR_VM", "requestStart($durationMinutes min) — asking phone to start walk")
        viewModelScope.launch {
            dataLayerManager.sendTimerStartRequest(durationMinutes)
        }
    }

    fun checkIn(status: String) {
        Log.d("SW_WEAR_VM", "checkIn($status) — sending to phone, resetting local state")
        viewModelScope.launch {
            dataLayerManager.sendCheckIn(status)
            _session.value = SafeWalkSession.Idle
            _remainingSeconds.value = 0
        }
    }

    fun triggerSOS() {
        Log.d("SW_WEAR_VM", "triggerSOS() — sending SOS check-in to phone")
        viewModelScope.launch {
            dataLayerManager.sendCheckIn("SOS")
        }
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            var logTick = 0
            while (true) {
                if (_session.value is SafeWalkSession.Active) {
                    delay(1000)
                    _remainingSeconds.update { current ->
                        val next = if (current > 0) current - 1 else 0
                        logTick++
                        // Log every 5 seconds to avoid flooding logcat
                        if (logTick % 5 == 0) {
                            Log.d("SW_WEAR_VM", "Local countdown tick: $current → $next")
                        }
                        next
                    }
                } else {
                    logTick = 0
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
