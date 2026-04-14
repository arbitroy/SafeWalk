package com.example.safewalk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.safewalk.data.model.CheckIn
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Insert
    suspend fun insert(checkIn: CheckIn)

    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC")
    fun getAllCheckIns(): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins WHERE id = :id")
    suspend fun getCheckInById(id: String): CheckIn?

    @Query("SELECT COUNT(*) FROM check_ins WHERE status = 'COMPLETED'")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM check_ins WHERE status = 'MISSED'")
    fun getMissedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM check_ins WHERE status = 'SOS'")
    fun getSosCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM check_ins")
    fun getTotalCount(): Flow<Int>

    @Query("DELETE FROM check_ins")
    suspend fun clearAll()
}