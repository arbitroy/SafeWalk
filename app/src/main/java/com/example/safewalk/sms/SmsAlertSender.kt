package com.example.safewalk.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsAlertSender @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun canSendSms(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Sends an SMS directly via SmsManager — no user interaction required.
     * Falls back silently if permission is not granted.
     * Returns true if the send was attempted.
     */
    fun sendToContact(phone: String, message: String): Boolean {
        if (!canSendSms()) {
            Log.w("SmsAlertSender", "SEND_SMS permission not granted — skipping $phone")
            return false
        }
        return try {
            val smsManager = getSmsManager()
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phone, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            }
            Log.d("SmsAlertSender", "Alert SMS sent to $phone")
            true
        } catch (e: Exception) {
            Log.e("SmsAlertSender", "Failed to send SMS to $phone", e)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun getSmsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
}
