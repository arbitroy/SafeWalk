package com.example.safewalk.di

import android.content.Context
import com.example.safewalk.communication.BluetoothCommunicationManager
import com.example.safewalk.permissions.PermissionsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommunicationModule {
    // BluetoothCommunicationManager and PermissionsManager are provided via @Inject constructor
}
