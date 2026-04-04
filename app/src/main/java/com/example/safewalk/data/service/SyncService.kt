package com.example.safewalk.data.service

interface SyncService {
    suspend fun syncPendingAlerts(): Result<Unit>
    suspend fun syncContacts(): Result<Unit>
    fun schedulePeriodicSync()
}
