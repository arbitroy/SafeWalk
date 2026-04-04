package com.example.safewalk.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.safewalk.data.model.*

@Database(
    entities = [
        UserEntity::class,
        EmergencyContactEntity::class,
        AlertEntity::class,
        AlertLocationEntity::class,
        WearableDeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SafeWalkDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun alertDao(): AlertDao
    abstract fun alertLocationDao(): AlertLocationDao
    abstract fun wearableDeviceDao(): WearableDeviceDao
}
