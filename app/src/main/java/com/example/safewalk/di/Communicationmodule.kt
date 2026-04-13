package com.example.safewalk.di

import android.content.Context
import com.example.safewalk.communication.BluetoothCommunicationManager
import com.example.safewalk.data.wearable.WearDataLayerManager
import com.example.safewalk.pairing.PairingManager
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

    @Provides
    @Singleton
    fun providePairingManager(
        @ApplicationContext context: Context,
    ): PairingManager = PairingManager(context)

    @Provides
    @Singleton
    fun provideBluetoothCommunicationManager(
        @ApplicationContext context: Context,
    ): BluetoothCommunicationManager = BluetoothCommunicationManager(context)

    @Provides
    @Singleton
    fun provideWearDataLayerManager(
        @ApplicationContext context: Context,
        pairingManager: PairingManager,
    ): WearDataLayerManager = WearDataLayerManager(context, pairingManager)

    @Provides
    @Singleton
    fun providePermissionsManager(
        @ApplicationContext context: Context,
    ): PermissionsManager = PermissionsManager(context)
}