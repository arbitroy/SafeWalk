package com.example.safewalk.data.local

import com.example.safewalk.data.model.AppSettings
import com.example.safewalk.data.model.CheckIn
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.data.model.User
import com.example.safewalk.data.model.UserProfile
import com.example.safewalk.data.preferences.SafeWalkDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow


/**
 * Single source of truth for all data operations
 */
@Singleton
class SafeWalkRepository @Inject constructor(
    private val checkInDao: CheckInDao,
    private val contactDao: ContactDao,
    private val dataStore: SafeWalkDataStore,
) {
    // ========== Check-Ins ==========

    fun getAllCheckIns(): Flow<List<CheckIn>> = checkInDao.getAllCheckIns()

    suspend fun addCheckIn(checkIn: CheckIn) = checkInDao.insert(checkIn)

    fun getCompletedCheckInCount(): Flow<Int> = checkInDao.getCompletedCount()

    fun getMissedCheckInCount(): Flow<Int> = checkInDao.getMissedCount()

    fun getSosCheckInCount(): Flow<Int> = checkInDao.getSosCount()

    fun getTotalCheckInCount(): Flow<Int> = checkInDao.getTotalCount()

    suspend fun clearCheckInHistory() = checkInDao.clearAll()

    // ========== Contacts ==========

    fun getAllContacts(): Flow<List<EmergencyContact>> = contactDao.getAllContacts()

    suspend fun addContact(contact: EmergencyContact) = contactDao.insert(contact)

    suspend fun updateContact(contact: EmergencyContact) = contactDao.update(contact)

    suspend fun deleteContact(contact: EmergencyContact) = contactDao.delete(contact)

    suspend fun getPrimaryContact(): EmergencyContact? = contactDao.getPrimaryContact()

    suspend fun clearContacts() = contactDao.clearAll()

    // ========== User Management ==========

    fun getCurrentUser(): Flow<User?> = dataStore.currentUser

    suspend fun setCurrentUser(user: User) = dataStore.setCurrentUser(user)

    suspend fun logout() = dataStore.logout()

    fun isGuestUser(): Flow<Boolean> = dataStore.isGuestUser

    suspend fun createGuestUser(): User = dataStore.createGuestUser()

    suspend fun addUser(user: User) = dataStore.addUser(user)

    suspend fun updateUser(user: User) = dataStore.updateUser(user)

    suspend fun findUserByEmail(email: String): User? = dataStore.findUserByEmail(email)

    // ========== Settings ==========

    fun getSettings(): Flow<AppSettings> = dataStore.settings

    suspend fun saveSettings(settings: AppSettings) = dataStore.saveSettings(settings)

    // ========== Profile ==========

    fun getUserProfile(): Flow<UserProfile?> = dataStore.userProfile

    suspend fun saveProfile(profile: UserProfile) = dataStore.saveProfile(profile)
}