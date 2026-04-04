package com.example.safewalk.data.service

import com.google.firebase.messaging.RemoteMessage

interface NotificationManager {
    fun setupFCM(onTokenRefresh: (String) -> Unit)
    fun sendLocalNotification(title: String, message: String)
    fun registerFCMToken(): Result<String>
    fun handleRemoteNotification(remoteMessage: RemoteMessage)
}
