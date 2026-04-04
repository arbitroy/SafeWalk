package com.example.safewalk.domain.repository

import com.example.safewalk.data.model.EmergencyContact

interface EmergencyContactRepository {
    suspend fun addContact(contact: EmergencyContact): Result<EmergencyContact>
    suspend fun updateContact(contact: EmergencyContact): Result<Unit>
    suspend fun deleteContact(contactId: String): Result<Unit>
    suspend fun getActiveContacts(userId: String): Result<List<EmergencyContact>>
    suspend fun getContact(contactId: String): Result<EmergencyContact?>
}
