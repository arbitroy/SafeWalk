package com.example.safewalk.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.safewalk.data.model.EmergencyContactEntity

@Dao
interface EmergencyContactDao {
    @Insert
    suspend fun insert(contact: EmergencyContactEntity)

    @Update
    suspend fun update(contact: EmergencyContactEntity)

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveContactsByUserId(userId: String): List<EmergencyContactEntity>

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId")
    suspend fun getAllContactsByUserId(userId: String): List<EmergencyContactEntity>

    @Query("SELECT * FROM emergency_contacts WHERE id = :id")
    suspend fun getContactById(id: String): EmergencyContactEntity?

    @Delete
    suspend fun delete(contact: EmergencyContactEntity)
}
