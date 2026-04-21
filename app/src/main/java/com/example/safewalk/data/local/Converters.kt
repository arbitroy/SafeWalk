package com.example.safewalk.data.local

import androidx.room.TypeConverter
import com.example.safewalk.data.model.CheckInStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: CheckInStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): CheckInStatus {
        return CheckInStatus.valueOf(value)
    }
}
