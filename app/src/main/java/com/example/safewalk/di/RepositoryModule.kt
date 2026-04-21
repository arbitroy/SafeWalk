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
    // DataStore and Repository are provided via @Inject constructor
}