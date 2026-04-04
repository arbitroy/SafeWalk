package com.example.safewalk.data.repository

import com.example.safewalk.data.local.EmergencyContactDao
import com.example.safewalk.data.model.AddContactRequest
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.data.model.toDomain
import com.example.safewalk.data.model.toEntity
import com.example.safewalk.data.remote.SafeWalkApiService
import com.example.safewalk.domain.repository.EmergencyContactRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactRepositoryImpl @Inject constructor(
    private val apiService: SafeWalkApiService,
    private val contactDao: EmergencyContactDao
) : EmergencyContactRepository {

    override suspend fun addContact(contact: EmergencyContact): Result<EmergencyContact> {
        return try {
            val request = AddContactRequest(
                contact.name, contact.phone, contact.email,
                contact.notificationPreference.name
            )
            val newContact = apiService.addContact(request)
            contactDao.insert(newContact.toEntity())
            Result.success(newContact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContact(contact: EmergencyContact): Result<Unit> {
        return try {
            contactDao.update(contact.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContact(contactId: String): Result<Unit> {
        return try {
            val contact = contactDao.getContactById(contactId)
            if (contact != null) {
                contactDao.delete(contact)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Contact not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveContacts(userId: String): Result<List<EmergencyContact>> {
        return try {
            val contacts = contactDao.getActiveContactsByUserId(userId)
            Result.success(contacts.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContact(contactId: String): Result<EmergencyContact?> {
        return try {
            val contact = contactDao.getContactById(contactId)
            Result.success(contact?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
