package com.example.safewalk.data.service

interface PermissionChecker {
    fun hasLocationPermission(): Boolean
    fun hasBluetoothPermission(): Boolean
    fun hasNotificationPermission(): Boolean
    fun needsLocationPermission(): Boolean
    fun needsBluetoothPermission(): Boolean
    fun needsNotificationPermission(): Boolean
}
