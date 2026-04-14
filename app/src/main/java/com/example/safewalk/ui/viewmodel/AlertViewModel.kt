package com.example.safewalk.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.AlertType
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.location.LocationResult
import com.example.safewalk.location.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class AlertUiState(
    val alertType: AlertType = AlertType.SOS,
    val contacts: List<EmergencyContact> = emptyList(),
    val selectedContactIds: Set<String> = emptySet(),
    val alertedContactIds: Set<String> = emptySet(),
    val location: LocationResult? = null,
    val isLoadingLocation: Boolean = true,
    val alertId: String = UUID.randomUUID().toString().take(12),
    val timestamp: String = ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_INSTANT),
)

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
    private val locationService: LocationService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()

    init {
        val alertTypeArg = savedStateHandle.get<String>("alertType") ?: "SOS"
        val alertType = if (alertTypeArg == "MISSED") AlertType.MISSED else AlertType.SOS

        _uiState.value = _uiState.value.copy(alertType = alertType)

        viewModelScope.launch {
            val contacts = repository.getAllContacts().first()
            _uiState.value = _uiState.value.copy(
                contacts = contacts,
                selectedContactIds = contacts.map { it.id }.toSet(),
            )
        }

        viewModelScope.launch {
            val location = locationService.getCurrentLocation()
            _uiState.value = _uiState.value.copy(
                location = location,
                isLoadingLocation = false,
            )
        }
    }

    fun toggleContact(contactId: String) {
        val current = _uiState.value.selectedContactIds
        _uiState.value = _uiState.value.copy(
            selectedContactIds = if (contactId in current) {
                current - contactId
            } else {
                current + contactId
            },
        )
    }

    fun launchSmsForContact(context: Context, contact: EmergencyContact) {
        val message = buildSmsMessage()
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${contact.phone}")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        markContactAlerted(contact.id)
    }

    /**
     * Opens SMS for the first selected contact that hasn't been alerted yet.
     * Returns true if an SMS intent was launched, false if all are alerted.
     */
    fun sendToNextPending(context: Context): Boolean {
        val state = _uiState.value
        val nextContact = state.contacts.firstOrNull { contact ->
            contact.id in state.selectedContactIds && contact.id !in state.alertedContactIds
        } ?: return false
        launchSmsForContact(context, nextContact)
        return true
    }

    fun buildSmsMessage(): String {
        val state = _uiState.value
        val location = state.location
        val locationPart = if (location != null) {
            buildString {
                if (location.address.isNotEmpty()) append(" Location: ${location.address}.")
                append(" GPS: ${location.formattedGps}.")
                append(" Map: ${location.googleMapsUrl}")
            }
        } else {
            " Location unavailable."
        }
        return "SAFE WALK ALERT: I need immediate help!" +
            "$locationPart" +
            " Time: ${state.timestamp}" +
            " Track me: https://safewalk.app/tracking/${state.alertId}"
    }

    private fun markContactAlerted(contactId: String) {
        _uiState.value = _uiState.value.copy(
            alertedContactIds = _uiState.value.alertedContactIds + contactId,
        )
    }
}
