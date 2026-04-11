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
class WearTimerViewModel @Inject constructor(
    private val dataLayerManager: WearDataLayerManager,
    private val repository: WearRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SafeWalkSession>(SafeWalkSession.Idle)
    val session: StateFlow<SafeWalkSession> = _session.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    init {
        // Listen for updates from phone
        dataLayerManager.startListening(viewModelScope)

        // When phone sends timer update, update watch display
        viewModelScope.launch {
            repository.timerState.collect { timerData ->
                _session.value = timerData.session
                _remainingSeconds.value = timerData.remainingSeconds
            }
        }

        // Local timer countdown (even when disconnected)
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

    private fun startLocalCountdown() {
        viewModelScope.launch {
            while (true) {
                if (_session.value is SafeWalkSession.Active) {
                    delay(1000)
                    _remainingSeconds.update { it - 1 }
                } else {
                    delay(100)
                }
            }
        }
    }
}