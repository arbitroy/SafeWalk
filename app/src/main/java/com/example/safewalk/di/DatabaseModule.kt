package com.example.safewalk.di

import android.content.Context
import androidx.room.Room
import com.example.safewalk.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideSafeWalkDatabase(context: Context): SafeWalkDatabase {
        return Room.databaseBuilder(
            context,
            SafeWalkDatabase::class.java,
            "safewalk_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideUserDao(database: SafeWalkDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    @Singleton
    fun provideEmergencyContactDao(database: SafeWalkDatabase): EmergencyContactDao {
        return database.emergencyContactDao()
    }
    
    @Provides
    @Singleton
    fun provideAlertDao(database: SafeWalkDatabase): AlertDao {
        return database.alertDao()
    }
    
    @Provides
    @Singleton
    fun provideAlertLocationDao(database: SafeWalkDatabase): AlertLocationDao {
        return database.alertLocationDao()
    }
    
    @Provides
    @Singleton
    fun provideWearableDeviceDao(database: SafeWalkDatabase): WearableDeviceDao {
        return database.wearableDeviceDao()
    }
}
