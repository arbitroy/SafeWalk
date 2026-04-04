package com.example.safewalk.di

import com.example.safewalk.data.service.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    @Provides
    @Singleton
    fun provideLocationManager(
        impl: LocationManagerImpl
    ): LocationManager = impl
    
    @Provides
    @Singleton
    fun provideBluetoothService(
        impl: BluetoothServiceImpl
    ): BluetoothService = impl
    
    @Provides
    @Singleton
    fun provideNotificationManager(
        impl: NotificationManagerImpl
    ): NotificationManager = impl
    
    @Provides
    @Singleton
    fun provideSyncService(
        impl: SyncServiceImpl
    ): SyncService = impl
    
    @Provides
    @Singleton
    fun providePermissionChecker(
        impl: PermissionCheckerImpl
    ): PermissionChecker = impl
}
