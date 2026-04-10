package com.example.safewalk.di

import android.content.Context
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.preferences.SafeWalkDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): SafeWalkDataStore {
        return SafeWalkDataStore(context)
    }

    @Provides
    @Singleton
    fun provideRepository(
        checkInDao: com.example.safewalk.data.local.CheckInDao,
        contactDao: com.example.safewalk.data.local.ContactDao,
        dataStore: SafeWalkDataStore,
    ): SafeWalkRepository {
        return SafeWalkRepository(checkInDao, contactDao, dataStore)
    }
}