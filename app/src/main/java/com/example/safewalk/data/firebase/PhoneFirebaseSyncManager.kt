package com.example.safewalk.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneFirebaseSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val database = FirebaseDatabase.getInstance()
    private val prefs = context.getSharedPreferences("safewalk_session", Context.MODE_PRIVATE)

    val sessionCode: String
        get() = prefs.getString("session_code", null) ?: generateCode()

    private fun generateCode(): String {
        val code = (100000..999999).random().toString()
        prefs.edit().putString("session_code", code).apply()
        return code
    }

    fun regenerateSessionCode(): String {
        val code = (100000..999999).random().toString()
        prefs.edit().putString("session_code", code).apply()
        return code
    }

    private fun sessionRef() = database.getReference("sessions/$sessionCode")

    private val _wearableEvents = MutableSharedFlow<WearableEvent>()
    val wearableEvents: Flow<WearableEvent> = _wearableEvents.asSharedFlow()

    // Only process events written after this listener was registered
    private var listenStartTime = 0L

    fun startListening(scope: CoroutineScope) {
        listenStartTime = System.currentTimeMillis()
        listenForCheckIn(scope)
        listenForTimerStart(scope)
    }

    private fun listenForCheckIn(scope: CoroutineScope) {
        sessionRef().child("check_in").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: return
                val status = snapshot.child("status").getValue(String::class.java) ?: return
                if (timestamp < listenStartTime) return
                Log.d("SW_PHONE_FB", "check_in received: status=$status")
                scope.launch { _wearableEvents.emit(WearableEvent.CheckInReceived(status)) }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("SW_PHONE_FB", "check_in listener cancelled: ${error.message}")
            }
        })
    }

    private fun listenForTimerStart(scope: CoroutineScope) {
        sessionRef().child("timer_start").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: return
                if (timestamp < listenStartTime) return
                Log.d("SW_PHONE_FB", "timer_start received from watch")
                scope.launch { _wearableEvents.emit(WearableEvent.TimerStartRequest) }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("SW_PHONE_FB", "timer_start listener cancelled: ${error.message}")
            }
        })
    }

    suspend fun sendTimerUpdate(
        isActive: Boolean,
        durationMinutes: Int,
        remainingSeconds: Int,
        startLocation: String? = null,
    ): Boolean {
        return try {
            val data = mapOf(
                "isActive" to isActive,
                "durationMinutes" to durationMinutes,
                "remainingSeconds" to remainingSeconds,
                "startTime" to System.currentTimeMillis(),
                "startLocation" to startLocation,
                "updatedAt" to System.currentTimeMillis(),
            )
            sessionRef().child("timer_state").setValue(data).await()
            Log.d("SW_PHONE_FB", "timer_state sent: isActive=$isActive remaining=${remainingSeconds}s")
            _wearableEvents.emit(WearableEvent.TimerSent)
            true
        } catch (e: Exception) {
            Log.e("SW_PHONE_FB", "sendTimerUpdate failed", e)
            _wearableEvents.emit(WearableEvent.SendError(e.message ?: "Unknown error"))
            false
        }
    }

    suspend fun sendContactsList(contacts: List<ContactData>): Boolean {
        return try {
            val data = contacts.map { c ->
                mapOf(
                    "id" to c.id,
                    "name" to c.name,
                    "phone" to c.phone,
                    "relationship" to c.relationship,
                    "isPrimary" to c.isPrimary,
                )
            }
            sessionRef().child("contacts").setValue(data).await()
            true
        } catch (e: Exception) {
            Log.e("SW_PHONE_FB", "sendContactsList failed", e)
            false
        }
    }

    suspend fun sendCheckInResponse(status: String, timestamp: Long): Boolean {
        return try {
            sessionRef().child("check_in_response").setValue(
                mapOf("status" to status, "timestamp" to timestamp)
            ).await()
            true
        } catch (e: Exception) {
            Log.e("SW_PHONE_FB", "sendCheckInResponse failed", e)
            false
        }
    }
}

sealed class WearableEvent {
    data object TimerSent : WearableEvent()
    data object TimerStartRequest : WearableEvent()
    data class CheckInReceived(val status: String) : WearableEvent()
    data class SendError(val error: String) : WearableEvent()
}

data class ContactData(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String = "",
    val isPrimary: Boolean = false,
)
