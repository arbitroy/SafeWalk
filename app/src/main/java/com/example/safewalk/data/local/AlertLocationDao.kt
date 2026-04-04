package com.example.safewalk.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.safewalk.data.model.AlertLocationEntity

@Dao
interface AlertLocationDao {
    @Insert
    suspend fun insert(location: AlertLocationEntity)

    @Query("SELECT * FROM alert_locations WHERE alertId = :alertId ORDER BY timestamp DESC")
    suspend fun getLocationsByAlertId(alertId: String): List<AlertLocationEntity>

    @Delete
    suspend fun delete(location: AlertLocationEntity)
}
