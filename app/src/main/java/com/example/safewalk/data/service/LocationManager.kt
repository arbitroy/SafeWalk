package com.example.safewalk.data.service

import com.example.safewalk.data.model.LocationData

interface LocationManager {
    suspend fun getCurrentLocation(): Result<LocationData>
    fun startLocationUpdates(callback: (LocationData) -> Unit)
    fun stopLocationUpdates()
    fun hasLocationPermission(): Boolean
}
