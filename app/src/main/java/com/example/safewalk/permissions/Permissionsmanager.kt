package com.example.safewalk.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _permissionsState = MutableStateFlow<PermissionState>(PermissionState.Idle)
    val permissionsState: StateFlow<PermissionState> = _permissionsState.asStateFlow()

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    fun registerPermissionLauncher(activity: ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val allGranted = results.all { it.value }
            _permissionsState.value = if (allGranted) {
                PermissionState.AllGranted
            } else {
                PermissionState.PartiallyGranted(results)
            }
        }
    }

    fun requestBluetoothPermissions(activity: ComponentActivity) {
        registerPermissionLauncher(activity)

        val requiredPermissions = buildList {
            add(Manifest.permission.BLUETOOTH)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }.toTypedArray()

        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            _permissionsState.value = PermissionState.AllGranted
        } else {
            _permissionsState.value = PermissionState.Requesting
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    fun requestWiFiPermissions(activity: ComponentActivity) {
        registerPermissionLauncher(activity)

        val requiredPermissions = buildList {
            add(Manifest.permission.CHANGE_NETWORK_STATE)
            add(Manifest.permission.ACCESS_NETWORK_STATE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()

        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            _permissionsState.value = PermissionState.AllGranted
        } else {
            _permissionsState.value = PermissionState.Requesting
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    fun requestContactsPermission(activity: ComponentActivity) {
        registerPermissionLauncher(activity)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            _permissionsState.value = PermissionState.AllGranted
        } else {
            _permissionsState.value = PermissionState.Requesting
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
        }
    }

    fun requestAllPairingPermissions(activity: ComponentActivity) {
        registerPermissionLauncher(activity)

        val requiredPermissions = buildList {
            // Bluetooth
            add(Manifest.permission.BLUETOOTH)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            // Location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            // WiFi
            add(Manifest.permission.CHANGE_NETWORK_STATE)
            add(Manifest.permission.ACCESS_NETWORK_STATE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }

            // Contacts
            add(Manifest.permission.READ_CONTACTS)
        }.toTypedArray()

        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            _permissionsState.value = PermissionState.AllGranted
        } else {
            _permissionsState.value = PermissionState.Requesting
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasWiFiPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CHANGE_NETWORK_STATE) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun hasAllRequiredPermissions(): Boolean {
        return hasBluetoothPermissions() && hasWiFiPermissions() && hasContactsPermission()
    }
}

sealed class PermissionState {
    data object Idle : PermissionState()
    data object Requesting : PermissionState()
    data object AllGranted : PermissionState()
    data class PartiallyGranted(val results: Map<String, Boolean>) : PermissionState()
    data class Denied(val permissions: List<String>) : PermissionState()
}