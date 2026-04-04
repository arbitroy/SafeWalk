package com.example.safewalk.di

import com.example.safewalk.data.repository.AlertRepositoryImpl
import com.example.safewalk.data.repository.EmergencyContactRepositoryImpl
import com.example.safewalk.domain.repository.AlertRepository
import com.example.safewalk.domain.repository.EmergencyContactRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideAlertRepository(
        impl: AlertRepositoryImpl
    ): AlertRepository = impl
    
    @Provides
    @Singleton
    fun provideEmergencyContactRepository(
        impl: EmergencyContactRepositoryImpl
    ): EmergencyContactRepository = impl
}
