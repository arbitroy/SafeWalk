package com.example.safewalk.data.repository

import com.example.safewalk.data.local.AlertDao
import com.example.safewalk.data.local.AlertLocationDao
import com.example.safewalk.data.model.*
import com.example.safewalk.data.remote.SafeWalkApiService
import com.example.safewalk.domain.repository.AlertRepository
import java.util.concurrent.Flow


@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val apiService: SafeWalkApiService,
    private val alertDao: AlertDao,
    private val locationDao: AlertLocationDao
) : AlertRepository {

    override suspend fun triggerAlert(alertType: AlertType, notes: String?): Result<Alert> {
        return try {
            val request = TriggerAlertRequest(alertType.name, notes)
            val alert = apiService.triggerAlert(request)
            alertDao.insert(alert.toEntity())
            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveAlert(alertId: String): Result<Alert> {
        return try {
            val request = UpdateAlertRequest("RESOLVED", System.currentTimeMillis())
            val alert = apiService.updateAlertStatus(alertId, request)
            alertDao.update(alert.toEntity())
            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelAlert(alertId: String): Result<Alert> {
        return try {
            val request = UpdateAlertRequest("CANCELLED", null)
            val alert = apiService.updateAlertStatus(alertId, request)
            alertDao.update(alert.toEntity())
            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addAlertLocation(alertId: String, location: AlertLocation): Result<Unit> {
        return try {
            val request = RecordLocationRequest(
                alertId, location.latitude, location.longitude,
                location.accuracy, location.timestamp
            )
            apiService.recordAlertLocation(request)
            locationDao.insert(location.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAlertHistory(userId: String, limit: Int): Result<List<Alert>> {
        return try {
            val alerts = apiService.getAlertHistory(limit)
            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeActiveAlert(userId: String): Flow<Alert?> {
        return MutableStateFlow(null)
    }
}
