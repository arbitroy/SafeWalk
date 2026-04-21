package com.example.safewalk.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.safewalk.data.model.CheckIn
import com.example.safewalk.data.model.EmergencyContact

/**
 * Room database for SafeWalk
 */
@Database(
    entities = [CheckIn::class, EmergencyContact::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SafeWalkDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInDao
    abstract fun contactDao(): ContactDao
}
