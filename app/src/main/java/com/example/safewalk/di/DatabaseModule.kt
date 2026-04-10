package com.example.safewalk.di

import android.content.Context
import androidx.room.Room
import com.example.safewalk.data.local.SafeWalkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SafeWalkDatabase {
        return Room.databaseBuilder(
            context,
            SafeWalkDatabase::class.java,
            "safewalk.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCheckInDao(database: SafeWalkDatabase) = database.checkInDao()

    @Provides
    @Singleton
    fun provideContactDao(database: SafeWalkDatabase) = database.contactDao()
}