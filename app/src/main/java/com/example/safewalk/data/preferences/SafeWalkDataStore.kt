package com.example.safewalk.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.safewalk.data.model.AppSettings
import com.example.safewalk.data.model.User
import com.example.safewalk.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


/**
 * DataStore-backed secure preferences for SafeWalk
 * Handles user authentication, settings, and profile data
 */
@Singleton
class SafeWalkDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    // ========== Preference Keys ==========

    companion object {
        private val CURRENT_USER = stringPreferencesKey("current_user")
        private val USERS_JSON = stringPreferencesKey("users_json")
        private val IS_GUEST = booleanPreferencesKey("is_guest")
        private val DEFAULT_DURATION = intPreferencesKey("default_duration")
        private val NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound")
        private val AUTO_START_LOCATION = booleanPreferencesKey("auto_start_location")
        private val USER_PROFILE = stringPreferencesKey("user_profile")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ========== Current User ==========

    val currentUser: Flow<User?> = dataStore.data.map { preferences ->
        preferences[CURRENT_USER]?.let {
            try { json.decodeFromString<User>(it) } catch (e: Exception) { null }
        }
    }

    suspend fun setCurrentUser(user: User) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER] = json.encodeToString(user)
            preferences[IS_GUEST] = false
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER)
            preferences[IS_GUEST] = true
        }
    }

    // ========== Guest Mode ==========

    val isGuestUser: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_GUEST] ?: false
    }

    suspend fun createGuestUser(): User {
        val guestUser = User(
            id = "guest_${System.currentTimeMillis()}",
            email = "guest@safewalk.local",
            name = "Guest User",
        )
        dataStore.edit { preferences ->
            preferences[CURRENT_USER] = json.encodeToString(guestUser)
            preferences[IS_GUEST] = true
        }
        return guestUser
    }

    // ========== User Management ==========

    suspend fun addUser(user: User) {
        dataStore.edit { preferences ->
            val usersJson = preferences[USERS_JSON] ?: "[]"
            val users = try {
                json.decodeFromString<MutableList<User>>(usersJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            // Avoid duplicates
            users.removeAll { it.email == user.email }
            users.add(user)

            preferences[USERS_JSON] = json.encodeToString(users)
        }
    }

    suspend fun updateUser(user: User) {
        dataStore.edit { preferences ->
            val usersJson = preferences[USERS_JSON] ?: "[]"
            val users = try {
                json.decodeFromString<MutableList<User>>(usersJson)
            } catch (e: Exception) {
                mutableListOf()
            }
            users.removeAll { it.id == user.id }
            users.add(user)
            preferences[USERS_JSON] = json.encodeToString(users)
            // Keep current user in sync if it's the same user
            preferences[CURRENT_USER]?.let { currentJson ->
                val current = try { json.decodeFromString<User>(currentJson) } catch (e: Exception) { null }
                if (current?.id == user.id) {
                    preferences[CURRENT_USER] = json.encodeToString(user)
                }
            }
        }
    }

    suspend fun findUserByEmail(email: String): User? {
        val usersJson = dataStore.data.map { preferences ->
            preferences[USERS_JSON] ?: "[]"
        }.first()

        return try {
            val users = json.decodeFromString<List<User>>(usersJson)
            users.find { it.email == email }
        } catch (e: Exception) {
            null
        }
    }

    // ========== Settings ==========

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            defaultDuration = preferences[DEFAULT_DURATION] ?: 30,
            notificationSound = preferences[NOTIFICATION_SOUND] ?: true,
            autoStartLocation = preferences[AUTO_START_LOCATION] ?: false,
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_DURATION] = settings.defaultDuration
            preferences[NOTIFICATION_SOUND] = settings.notificationSound
            preferences[AUTO_START_LOCATION] = settings.autoStartLocation
        }
    }

    // ========== User Profile ==========

    val userProfile: Flow<UserProfile?> = dataStore.data.map { preferences ->
        preferences[USER_PROFILE]?.let {
            try { json.decodeFromString<UserProfile>(it) } catch (e: Exception) { null }
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[USER_PROFILE] = json.encodeToString(profile)
        }
    }
}

// ========== DataStore Extension ==========

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "safewalk_preferences"
)