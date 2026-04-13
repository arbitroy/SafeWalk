package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.contacts.ContactPickerManager
import com.example.safewalk.contacts.SystemContact
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.EmergencyContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyContactsViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
    private val contactPickerManager: ContactPickerManager,
) : ViewModel() {

    val contacts: Flow<List<EmergencyContact>> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _systemContacts = MutableStateFlow<List<SystemContact>>(emptyList())
    val systemContacts: StateFlow<List<SystemContact>> = _systemContacts

    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts

    private val _uiEvent = MutableSharedFlow<ContactsEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun loadSystemContacts() {
        viewModelScope.launch {
            _isLoadingContacts.value = true
            try {
                val contacts = contactPickerManager.getSystemContacts()
                _systemContacts.value = contacts
            } catch (e: Exception) {
                _systemContacts.value = emptyList()
            } finally {
                _isLoadingContacts.value = false
            }
        }
    }

    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.addContact(contact)
            _uiEvent.emit(ContactsEvent.ContactAdded)
        }
    }

    fun updateContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.updateContact(contact)
            _uiEvent.emit(ContactsEvent.ContactUpdated)
        }
    }

    fun deleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            _uiEvent.emit(ContactsEvent.ContactDeleted)
        }
    }

    fun togglePrimary(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.updateContact(
                contact.copy(isPrimary = !contact.isPrimary)
            )
        }
    }
}

sealed class ContactsEvent {
    data object ContactAdded : ContactsEvent()
    data object ContactUpdated : ContactsEvent()
    data object ContactDeleted : ContactsEvent()
}