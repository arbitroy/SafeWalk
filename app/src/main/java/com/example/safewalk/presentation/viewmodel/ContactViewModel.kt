package com.example.safewalk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.domain.repository.EmergencyContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: EmergencyContactRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadContacts(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = contactRepository.getActiveContacts(userId)
                result.onSuccess { contacts ->
                    _contacts.value = contacts
                }
                result.onFailure { error ->
                    _errorMessage.value = error.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = contactRepository.addContact(contact)
                result.onSuccess { newContact ->
                    _contacts.value = _contacts.value + newContact
                }
                result.onFailure { error ->
                    _errorMessage.value = error.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                val result = contactRepository.updateContact(contact)
                result.onSuccess {
                    _contacts.value = _contacts.value.map {
                        if (it.id == contact.id) contact else it
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                val result = contactRepository.deleteContact(contactId)
                result.onSuccess {
                    _contacts.value = _contacts.value.filter { it.id != contactId }
                }
                result.onFailure { error ->
                    _errorMessage.value = error.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deactivateContact(contactId: String) {
        viewModelScope.launch {
            try {
                val contact = _contacts.value.find { it.id == contactId }
                if (contact != null) {
                    val deactivated = contact.copy(isActive = false)
                    contactRepository.updateContact(deactivated)
                    _contacts.value = _contacts.value.map {
                        if (it.id == contactId) deactivated else it
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
