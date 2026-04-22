package com.wear.data

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class WearFirebaseSyncManager @Inject constructor(
    @ApplicationContext val context: Context,
    private val repository: WearRepository,
) {
    private val database = FirebaseDatabase.getInstance()
    private val prefs = context.getSharedPreferences("wear_session", Context.MODE_PRIVATE)

    val sessionCode: String?
        get() = prefs.getString("session_code", null)

    private fun sessionRef(code: String) = database.getReference("sessions/$code")

    fun pairWithCode(code: String, scope: CoroutineScope, onResult: (Boolean) -> Unit) {
        if (code.length != 6 || code.any { !it.isDigit() }) {
            onResult(false)
            return
        }
        // Write a watch-present marker so the phone can see the watch joined
        sessionRef(code).child("watch_present").setValue(
            mapOf("joined_at" to System.currentTimeMillis())
        ).addOnSuccessListener {
            prefs.edit().putString("session_code", code).apply()
            startListening(scope)
            Log.d("SW_WEAR_FB", "paired with session code $code")
            onResult(true)
        }.addOnFailureListener {
            Log.e("SW_WEAR_FB", "pairWithCode failed for code=$code", it)
            onResult(false)
        }
    }

    fun unpair() {
        prefs.edit().remove("session_code").apply()
    }

    fun startListening(scope: CoroutineScope) {
        val code = sessionCode ?: return
        listenForTimerState(code)
        listenForContacts(code)
    }

    private fun listenForTimerState(code: String) {
        sessionRef(code).child("timer_state").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val isActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: false
                val durationMinutes = snapshot.child("durationMinutes").getValue(Int::class.java) ?: 0
                val remainingSeconds = snapshot.child("remainingSeconds").getValue(Int::class.java) ?: 0
                val startTime = snapshot.child("startTime").getValue(Long::class.java) ?: 0L
                val startLocation = snapshot.child("startLocation").getValue(String::class.java)
                Log.d("SW_WEAR_FB", "timer_state: isActive=$isActive remaining=${remainingSeconds}s")
                repository.updateTimerState(
                    TimerStateData(isActive, startTime, durationMinutes, remainingSeconds, startLocation)
                )
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("SW_WEAR_FB", "timer_state cancelled: ${error.message}")
            }
        })
    }

    private fun listenForContacts(code: String) {
        sessionRef(code).child("contacts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val contacts = snapshot.children.mapNotNull { child ->
                    try {
                        EmergencyContactData(
                            id = child.child("id").getValue(String::class.java) ?: return@mapNotNull null,
                            name = child.child("name").getValue(String::class.java) ?: return@mapNotNull null,
                            phone = child.child("phone").getValue(String::class.java) ?: return@mapNotNull null,
                            relationship = child.child("relationship").getValue(String::class.java) ?: "",
                            isPrimary = child.child("isPrimary").getValue(Boolean::class.java) ?: false,
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                repository.updateContacts(contacts)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("SW_WEAR_FB", "contacts cancelled: ${error.message}")
            }
        })
    }

    suspend fun sendCheckIn(status: String) {
        val code = sessionCode ?: return
        try {
            sessionRef(code).child("check_in").setValue(
                mapOf("status" to status, "timestamp" to System.currentTimeMillis(), "device" to "wear")
            ).await()
            Log.d("SW_WEAR_FB", "check_in sent: status=$status")
        } catch (e: Exception) {
            Log.e("SW_WEAR_FB", "sendCheckIn failed", e)
        }
    }

    suspend fun sendTimerStartRequest(durationMinutes: Int = 30) {
        val code = sessionCode ?: return
        try {
            sessionRef(code).child("timer_start").setValue(
                mapOf("durationMinutes" to durationMinutes, "timestamp" to System.currentTimeMillis())
            ).await()
            Log.d("SW_WEAR_FB", "timer_start request sent (${durationMinutes}min)")
        } catch (e: Exception) {
            Log.e("SW_WEAR_FB", "sendTimerStartRequest failed", e)
        }
    }
}
