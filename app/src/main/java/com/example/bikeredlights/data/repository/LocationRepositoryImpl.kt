package com.example.bikeredlights.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.example.bikeredlights.domain.model.LocationData
import com.example.bikeredlights.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementation of LocationRepository using Google Play Services Location API.
 *
 * This class wraps FusedLocationProviderClient to provide a Flow-based interface
 * for location tracking. It configures GPS for cycling speed tracking with:
 * - High accuracy GPS updates
 * - 1-second update interval for real-time speed display
 * - Automatic cleanup when Flow is cancelled
 *
 * @param context Android context for accessing location services
 */
class LocationRepositoryImpl(
    private val context: Context
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Emits continuous location updates as a cold Flow.
     *
     * Uses callbackFlow to convert FusedLocationProviderClient callbacks into a
     * reactive Flow. The Flow automatically starts location tracking when collected
     * and stops tracking when cancelled (e.g., app backgrounds).
     *
     * Configuration:
     * - PRIORITY_HIGH_ACCURACY: GPS-based positioning for accurate speed
     * - 1000ms interval: Real-time updates for cycling speedometer
     * - 500ms min interval: Can update faster if GPS provides more frequent fixes
     *
     * @return Flow<LocationData> emitting location updates ~1/second
     * @throws SecurityException if ACCESS_FINE_LOCATION permission not granted
     */
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        // Configure location request for cycling speed tracking
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,  // GPS-based for accurate speed
            1000L  // Desired interval: 1 second for real-time updates
        ).apply {
            setMinUpdateIntervalMillis(500L)  // Fastest interval: 0.5 seconds
            setWaitForAccurateLocation(false) // Don't wait indefinitely for high accuracy
        }.build()

        // Create callback for location updates
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    // trySend: non-blocking, returns failure if channel closed
                    trySend(
                        LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time,
                            speedMps = if (location.hasSpeed()) location.speed else null,
                            bearing = if (location.hasBearing()) location.bearing else null
                        )
                    )
                }
            }
        }

        // Get last known location immediately if available
        try {
            fusedLocationClient.lastLocation.await()?.let { lastLocation ->
                send(
                    LocationData(
                        latitude = lastLocation.latitude,
                        longitude = lastLocation.longitude,
                        accuracy = lastLocation.accuracy,
                        timestamp = lastLocation.time,
                        speedMps = if (lastLocation.hasSpeed()) lastLocation.speed else null,
                        bearing = if (lastLocation.hasBearing()) lastLocation.bearing else null
                    )
                )
            }
        } catch (e: Exception) {
            // Last location unavailable, continue with real-time updates
        }

        // Start location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            context.mainLooper
        ).await()

        // Suspend until Flow is cancelled, then cleanup
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
