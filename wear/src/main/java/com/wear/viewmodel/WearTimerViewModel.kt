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
import kotlinx.coroutines.launch

@HiltViewModel
class WearTimerViewModel @Inject constructor(
    private val dataLayerManager: WearDataLayerManager,
    private val repository: WearRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SafeWalkSession>(SafeWalkSession.Idle)
    val session: StateFlow<SafeWalkSession> = _session.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    init {
        // Start listening for data from the phone
        dataLayerManager.startListening(viewModelScope)

        // Mirror phone-authoritative timer state into local UI state
        viewModelScope.launch {
            repository.timerState.collect { timerData ->
                _session.value = timerData.session
                _remainingSeconds.value = timerData.remainingSeconds
            }
        }

        // Local 1-second countdown so the display keeps ticking even when disconnected
        startLocalCountdown()
    }

    fun checkIn(status: String) {
        viewModelScope.launch {
            dataLayerManager.sendCheckIn(status)
        }
    }

    fun triggerSOS() {
        viewModelScope.launch {
            dataLayerManager.sendCheckIn("SOS")
        }
    }

    /**
     * Asks the phone (source of truth) to start a new SafeWalk session.
     * The phone will send the authoritative timer state back once started.
     */
    fun requestStart(durationMinutes: Int = 30) {
        viewModelScope.launch {
            dataLayerManager.sendTimerStartRequest(durationMinutes)
        }
    }

    private fun startLocalCountdown() {
        viewModelScope.launch {
            while (true) {
                if (_session.value is SafeWalkSession.Active) {
                    delay(1000)
                    // Re-check after delay — phone may have ended the session during the sleep,
                    // which resets remainingSeconds to 0 via the repository collector above.
                    // Without this guard, 0 - 1 = -1 would corrupt the display.
                    if (_session.value !is SafeWalkSession.Active) continue
                    _remainingSeconds.value = maxOf(0, _remainingSeconds.value - 1)
                } else {
                    delay(100)
                }
            }
        }
    }
}
