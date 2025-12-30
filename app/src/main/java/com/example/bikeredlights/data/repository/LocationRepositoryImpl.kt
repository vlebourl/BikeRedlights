package com.example.bikeredlights.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.GpsStatus
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

        /**
         * GPS status accuracy thresholds (from GpsStatus.kt documentation):
         * - UNAVAILABLE_THRESHOLD: accuracy > 50m indicates GPS unavailable
         * - ACQUIRING_THRESHOLD: accuracy 10-50m indicates acquiring signal
         * - ACTIVE: accuracy ≤ 10m indicates good GPS signal
         */
        internal const val ACCURACY_UNAVAILABLE_THRESHOLD = 50f
        internal const val ACCURACY_ACQUIRING_THRESHOLD = 10f

        /**
         * Timeout threshold for GPS signal loss detection.
         * If no location update is received for >10 seconds, GPS is considered unavailable.
         */
        internal const val GPS_TIMEOUT_MS = 10_000L

        /**
         * Interval for checking GPS timeout in the monitoring coroutine.
         */
        private const val GPS_TIMEOUT_CHECK_INTERVAL_MS = 1_000L
    }

    /**
     * Internal state for GPS status tracking.
     * Starts as Acquiring since we're waiting for the first location fix.
     */
    private val _gpsStatus = MutableStateFlow<GpsStatus>(GpsStatus.Acquiring)

    /**
     * Timestamp of the last received location update.
     * Used to detect GPS timeout (no updates for >10 seconds).
     */
    @Volatile
    private var lastLocationTimestamp: Long = System.currentTimeMillis()

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
                    // Update GPS status based on location accuracy
                    updateGpsStatus(location.accuracy)

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
                // Update GPS status based on last known location accuracy
                updateGpsStatus(lastLocation.accuracy)

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

    /**
     * Emits GPS status updates as a Flow.
     *
     * The Flow monitors location updates and emits GPS status changes based on:
     * - Location accuracy thresholds (50m, 10m)
     * - Timeout detection (no updates for >10 seconds)
     *
     * This is a cold Flow that starts monitoring when collected and uses
     * a coroutine to periodically check for GPS timeout conditions.
     *
     * @return Flow<GpsStatus> that emits GPS status changes
     */
    override fun gpsStatusUpdates(): Flow<GpsStatus> = callbackFlow {
        // Reset last timestamp when starting to monitor
        lastLocationTimestamp = System.currentTimeMillis()
        _gpsStatus.value = GpsStatus.Acquiring

        // Launch a coroutine to periodically check for GPS timeout
        val timeoutJob = launch {
            while (isActive) {
                delay(GPS_TIMEOUT_CHECK_INTERVAL_MS)
                val currentTime = System.currentTimeMillis()
                val timeSinceLastUpdate = currentTime - lastLocationTimestamp

                if (timeSinceLastUpdate > GPS_TIMEOUT_MS) {
                    val previousStatus = _gpsStatus.value
                    if (previousStatus !is GpsStatus.Unavailable) {
                        Log.w(TAG, "GPS timeout: no location update for ${timeSinceLastUpdate}ms")
                        _gpsStatus.value = GpsStatus.Unavailable
                    }
                }
            }
        }

        // Collect and forward GPS status changes
        val statusJob = launch {
            _gpsStatus.collect { status ->
                trySend(status)
            }
        }

        // Cleanup when Flow is cancelled
        awaitClose {
            timeoutJob.cancel()
            statusJob.cancel()
        }
    }

    /**
     * Determines GPS status based on location accuracy.
     *
     * Thresholds (from GpsStatus.kt documentation):
     * - Unavailable: accuracy > 50m
     * - Acquiring: accuracy > 10m but ≤ 50m
     * - Active: accuracy ≤ 10m
     *
     * @param accuracy Horizontal accuracy in meters from the location update
     * @return Appropriate GpsStatus based on accuracy threshold
     */
    internal fun determineGpsStatus(accuracy: Float): GpsStatus {
        return when {
            accuracy > ACCURACY_UNAVAILABLE_THRESHOLD -> GpsStatus.Unavailable
            accuracy > ACCURACY_ACQUIRING_THRESHOLD -> GpsStatus.Acquiring
            else -> GpsStatus.Active(accuracy)
        }
    }

    /**
     * Updates GPS status based on a new location update.
     *
     * Called internally when a new location is received to:
     * 1. Update the last location timestamp (for timeout detection)
     * 2. Determine and emit the new GPS status based on accuracy
     *
     * @param accuracy Horizontal accuracy in meters from the location update
     */
    internal fun updateGpsStatus(accuracy: Float) {
        lastLocationTimestamp = System.currentTimeMillis()
        val newStatus = determineGpsStatus(accuracy)
        val previousStatus = _gpsStatus.value

        // Only log and update if status changed
        if (newStatus != previousStatus) {
            Log.d(TAG, "GPS status changed: $previousStatus -> $newStatus (accuracy: ${accuracy}m)")
        }
        _gpsStatus.value = newStatus
    }
}
