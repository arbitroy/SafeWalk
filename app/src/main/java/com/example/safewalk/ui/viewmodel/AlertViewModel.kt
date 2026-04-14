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
import com.example.safewalk.data.model.UserProfile
import com.example.safewalk.location.LocationResult
import com.example.safewalk.location.LocationService
import com.example.safewalk.sms.SmsAlertSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AlertUiState(
    val alertType: AlertType = AlertType.SOS,
    val contacts: List<EmergencyContact> = emptyList(),
    val selectedContactIds: Set<String> = emptySet(),
    val alertedContactIds: Set<String> = emptySet(),
    /** Contacts that were auto-sent via SmsManager on screen open (starred contacts). */
    val autoAlertedContactIds: Set<String> = emptySet(),
    val location: LocationResult? = null,
    val isLoadingLocation: Boolean = true,
    val senderName: String = "",
    val userProfile: UserProfile? = null,
    val timestamp: String = ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_INSTANT),
)

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
    private val locationService: LocationService,
    private val smsAlertSender: SmsAlertSender,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()

    init {
        val alertTypeArg = savedStateHandle.get<String>("alertType") ?: "SOS"
        val alertType = if (alertTypeArg == "MISSED") AlertType.MISSED else AlertType.SOS
        _uiState.value = _uiState.value.copy(alertType = alertType)

        viewModelScope.launch {
            // 1. Load contacts, user info, and profile in parallel
            val contacts = repository.getAllContacts().first()
            val currentUser = repository.getCurrentUser().first()
            val profile = repository.getUserProfile().first()

            _uiState.value = _uiState.value.copy(
                contacts = contacts,
                selectedContactIds = contacts.map { it.id }.toSet(),
                senderName = currentUser?.name ?: "",
                userProfile = profile,
            )

            // 2. Fetch location — give it up to 5 seconds, proceed anyway if slower
            val location = withTimeoutOrNull(5_000) { locationService.getCurrentLocation() }
            _uiState.value = _uiState.value.copy(location = location, isLoadingLocation = false)

            // 3. Auto-send to starred (isPrimary) contacts via SmsManager — no tap needed
            val primaryContacts = contacts.filter { it.isPrimary }
            if (primaryContacts.isNotEmpty() && smsAlertSender.canSendSms()) {
                val message = buildSmsMessage()
                val autoSent = mutableSetOf<String>()
                primaryContacts.forEach { contact ->
                    if (smsAlertSender.sendToContact(contact.phone, message)) {
                        autoSent += contact.id
                    }
                }
                if (autoSent.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        alertedContactIds = _uiState.value.alertedContactIds + autoSent,
                        autoAlertedContactIds = autoSent,
                    )
                }
            }
        }
    }

    fun toggleContact(contactId: String) {
        val current = _uiState.value.selectedContactIds
        _uiState.value = _uiState.value.copy(
            selectedContactIds = if (contactId in current) current - contactId else current + contactId,
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
     * Opens the native SMS app for the first selected, unalerted contact.
     * Returns true if an intent was launched.
     */
    fun sendToNextPending(context: Context): Boolean {
        val state = _uiState.value
        val next = state.contacts.firstOrNull { contact ->
            contact.id in state.selectedContactIds && contact.id !in state.alertedContactIds
        } ?: return false
        launchSmsForContact(context, next)
        return true
    }

    fun buildSmsMessage(): String {
        val state = _uiState.value
        val profile = state.userProfile

        // Custom emergency message takes precedence over the default text
        val headline = if (!profile?.emergencyMessage.isNullOrBlank()) {
            profile!!.emergencyMessage.trim()
        } else {
            "I need immediate help!"
        }

        val locationPart = state.location?.let { loc ->
            buildString {
                if (loc.address.isNotEmpty()) append(" Location: ${loc.address}.")
                append(" GPS: ${loc.formattedGps}.")
                append(" Map: ${loc.googleMapsUrl}")
            }
        } ?: " Location unavailable."

        val fromPart = if (state.senderName.isNotBlank()) " From: ${state.senderName}." else ""

        val medicalPart = if (!profile?.medicalInfo.isNullOrBlank()) {
            " Medical info: ${profile!!.medicalInfo.trim()}."
        } else {
            ""
        }

        return "SAFE WALK ALERT: $headline" +
            "$fromPart" +
            "$locationPart" +
            "$medicalPart" +
            " Time: ${state.timestamp}"
    }

    private fun markContactAlerted(contactId: String) {
        _uiState.value = _uiState.value.copy(
            alertedContactIds = _uiState.value.alertedContactIds + contactId,
        )
    }
}
