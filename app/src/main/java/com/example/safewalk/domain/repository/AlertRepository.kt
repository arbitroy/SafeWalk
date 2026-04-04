package com.example.safewalk.domain.repository

import com.example.safewalk.data.model.Alert
import com.example.safewalk.data.model.AlertLocation
import com.example.safewalk.data.model.AlertType
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    suspend fun triggerAlert(alertType: AlertType, notes: String?): Result<Alert>
    suspend fun resolveAlert(alertId: String): Result<Alert>
    suspend fun cancelAlert(alertId: String): Result<Alert>
    suspend fun addAlertLocation(alertId: String, location: AlertLocation): Result<Unit>
    suspend fun getAlertHistory(userId: String, limit: Int = 20): Result<List<Alert>>
    fun observeActiveAlert(userId: String): Flow<Alert?>
}
