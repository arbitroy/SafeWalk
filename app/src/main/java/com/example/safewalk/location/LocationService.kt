package com.example.safewalk.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
) {
    val googleMapsUrl: String
        get() = "https://maps.google.com/?q=$latitude,$longitude"

    val formattedGps: String
        get() = "%.4f, %.4f".format(latitude, longitude)
}

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationSource = CancellationTokenSource()

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationSource.token,
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val address = reverseGeocode(location.latitude, location.longitude)
                    continuation.resume(
                        LocationResult(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            address = address,
                        )
                    )
                } else {
                    // Fall back to last known location
                    fusedClient.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            val address = reverseGeocode(last.latitude, last.longitude)
                            continuation.resume(
                                LocationResult(
                                    latitude = last.latitude,
                                    longitude = last.longitude,
                                    address = address,
                                )
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation { cancellationSource.cancel() }
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                buildString {
                    addr.thoroughfare?.let { append(it).append(", ") }
                    addr.locality?.let { append(it).append(", ") }
                    addr.postalCode?.let { append(it) }
                }.trimEnd(',', ' ')
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
