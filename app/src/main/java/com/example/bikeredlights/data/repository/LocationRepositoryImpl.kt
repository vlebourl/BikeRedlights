package com.example.bikeredlights.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.LocationData
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Implementation of LocationRepository using Google Play Services Location API.
 *
 * This class wraps FusedLocationProviderClient to provide a Flow-based interface
 * for location tracking. It configures GPS for cycling speed tracking with:
 * - High accuracy GPS updates
 * - Configurable update interval based on GPS accuracy setting (v0.2.0):
 *   - High Accuracy: 1000ms (1 second) for real-time speed display
 *   - Battery Saver: 4000ms (4 seconds) for battery optimization
 * - Automatic cleanup when Flow is cancelled
 *
 * v0.2.0 Update: Integrated with SettingsRepository to support dynamic GPS accuracy modes.
 *
 * @param context Android context for accessing location services
 * @param settingsRepository Repository providing user preferences (v0.2.0)
 */
class LocationRepositoryImpl(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationRepository"
    }

    /**
     * Emits continuous location updates as a cold Flow.
     *
     * Uses callbackFlow to convert FusedLocationProviderClient callbacks into a
     * reactive Flow. The Flow automatically starts location tracking when collected
     * and stops tracking when cancelled (e.g., app backgrounds).
     *
     * v0.2.0 Configuration (dynamic based on GPS accuracy setting):
     * - PRIORITY_HIGH_ACCURACY: GPS-based positioning for accurate speed
     * - High Accuracy mode: 1000ms interval (1 second) for real-time updates
     * - Battery Saver mode: 4000ms interval (4 seconds) for battery optimization
     * - Min interval: Half of desired interval for faster updates when available
     *
     * @return Flow<LocationData> emitting location updates at configured rate
     * @throws SecurityException if ACCESS_FINE_LOCATION permission not granted
     */
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        // Read GPS accuracy setting from SettingsRepository (v0.2.0)
        val gpsAccuracy = try {
            settingsRepository.gpsAccuracy.first()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading GPS accuracy setting, using default HIGH_ACCURACY", e)
            GpsAccuracy.DEFAULT
        }

        // Configure update interval based on GPS accuracy setting
        val updateIntervalMs = gpsAccuracy.getUpdateIntervalMs()
        val minUpdateIntervalMs = updateIntervalMs / 2  // Allow faster updates

        Log.d(TAG, "Starting location updates with ${gpsAccuracy.name} mode (${updateIntervalMs}ms interval)")

        // Configure location request for cycling speed tracking
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,  // GPS-based for accurate speed
            updateIntervalMs  // Configured interval based on GPS accuracy
        ).apply {
            setMinUpdateIntervalMillis(minUpdateIntervalMs)  // Allow faster updates
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
