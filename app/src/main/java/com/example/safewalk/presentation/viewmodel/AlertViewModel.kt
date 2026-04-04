package com.example.safewalk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.model.Alert
import com.example.safewalk.data.model.AlertLocation
import com.example.safewalk.data.model.AlertUiState
import com.example.safewalk.data.model.AlertType
import com.example.safewalk.data.service.LocationManager
import com.example.safewalk.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val locationManager: LocationManager
) : ViewModel() {

    private val _activeAlert = MutableStateFlow<Alert?>(null)
    val activeAlert: StateFlow<Alert?> = _activeAlert.asStateFlow()

    private val _alertHistory = MutableStateFlow<List<Alert>>(emptyList())
    val alertHistory: StateFlow<List<Alert>> = _alertHistory.asStateFlow()

    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()

    fun triggerAlert(alertType: AlertType, notes: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = alertRepository.triggerAlert(alertType, notes)
                result.onSuccess { alert ->
                    _activeAlert.value = alert
                    _uiState.value = _uiState.value.copy(
                        currentAlert = alert,
                        isLoading = false,
                        alertSent = true
                    )
                    recordAlertLocation(alert.id)
                }
                result.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun resolveAlert(alertId: String) {
        viewModelScope.launch {
            try {
                val result = alertRepository.resolveAlert(alertId)
                result.onSuccess { alert ->
                    _activeAlert.value = null
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun loadAlertHistory(userId: String, limit: Int = 20) {
        viewModelScope.launch {
            try {
                val result = alertRepository.getAlertHistory(userId, limit)
                result.onSuccess { alerts ->
                    _alertHistory.value = alerts
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    private fun recordAlertLocation(alertId: String) {
        viewModelScope.launch {
            try {
                locationManager.getCurrentLocation().onSuccess { locationData ->
                    val alertLocation = AlertLocation(
                        id = System.currentTimeMillis().toString(),
                        alertId = alertId,
                        latitude = locationData.latitude,
                        longitude = locationData.longitude,
                        accuracy = locationData.accuracy,
                        timestamp = System.currentTimeMillis()
                    )
                    alertRepository.addAlertLocation(alertId, alertLocation)
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}
