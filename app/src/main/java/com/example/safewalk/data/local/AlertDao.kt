package com.example.safewalk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.safewalk.data.model.AlertEntity

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(alert: AlertEntity)

    @Update
    suspend fun update(alert: AlertEntity)

    @Query("SELECT * FROM alerts WHERE userId = :userId ORDER BY triggeredAt DESC LIMIT :limit")
    suspend fun getAlertHistory(userId: String, limit: Int): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getAlertById(id: String): AlertEntity?

    @Query("SELECT * FROM alerts WHERE status = 'ACTIVE' AND userId = :userId LIMIT 1")
    suspend fun getActiveAlert(userId: String): AlertEntity?

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteAlert(id: String)
}
