package com.example.safewalk.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.safewalk.data.model.WearableDeviceEntity

@Dao
interface WearableDeviceDao {
    @Insert
    suspend fun insert(device: WearableDeviceEntity)

    @Update
    suspend fun update(device: WearableDeviceEntity)

    @Query("SELECT * FROM devices WHERE userId = :userId")
    suspend fun getDevicesByUserId(userId: String): List<WearableDeviceEntity>

    @Query("SELECT * FROM devices WHERE bluetoothAddress = :address")
    suspend fun getDeviceByAddress(address: String): WearableDeviceEntity?

    @Delete
    suspend fun delete(device: WearableDeviceEntity)
}
