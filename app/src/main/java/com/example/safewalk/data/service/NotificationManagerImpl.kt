package com.example.safewalk.data.service

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseMessaging: FirebaseMessaging
) : NotificationManager {

    override fun setupFCM(onTokenRefresh: (String) -> Unit) {
        firebaseMessaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                onTokenRefresh(token)
            }
        }
    }

    override fun sendLocalNotification(title: String, message: String) {
        // Implementation would use Android notification channels
    }

    override fun registerFCMToken(): Result<String> {
        return try {
            var token = ""
            firebaseMessaging.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    token = task.result
                }
            }
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun handleRemoteNotification(remoteMessage: RemoteMessage) {
        // Handle remote notification
        remoteMessage.notification?.let {
            sendLocalNotification(it.title ?: "", it.body ?: "")
        }
    }
}
