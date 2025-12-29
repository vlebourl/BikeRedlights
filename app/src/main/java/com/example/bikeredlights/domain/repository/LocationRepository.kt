package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.domain.model.LocationData
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for location tracking operations.
 *
 * This interface abstracts Android Location Services implementation, enabling
 * Clean Architecture separation and testability. The repository provides a
 * cold Flow of location updates that starts tracking when collected and stops
 * when cancelled.
 *
 * ## Flow Behavior
 * - **Cold Flow**: Starts tracking when collected, stops when cancelled
 * - **Emissions**: ~1/second (1000ms interval) while collected
 * - **Last Known Location**: May emit immediately if available
 * - **Automatic Cleanup**: Removes location callbacks on Flow cancellation
 *
 * ## Location Configuration
 * - **Priority**: PRIORITY_HIGH_ACCURACY (GPS-based)
 * - **Interval**: 1000ms (1 second updates)
 * - **Min Interval**: 500ms (can update faster if GPS provides)
 * - **Max Delay**: 2000ms (allows batching for battery efficiency)
 *
 * ## Error Handling
 * - Throws [SecurityException] if location permission not granted
 * - Emits error via Flow.catch if GPS unavailable or disabled
 * - Closes Flow on fatal errors (e.g., FusedLocationProviderClient failure)
 *
 * ## Thread Safety
 * - All emissions happen on the caller's dispatcher
 * - FusedLocationProviderClient callback runs on main looper
 * - Safe to collect from any CoroutineScope
 *
 * @see LocationData for the emitted data model
 */
interface LocationRepository {

    /**
     * Emits continuous location updates as a Flow.
     *
     * The Flow starts location tracking when collected and automatically stops
     * when the collector is cancelled (e.g., when app backgrounds). This provides
     * lifecycle-aware tracking with minimal battery drain.
     *
     * Example usage:
     * ```kotlin
     * viewModelScope.launch {
     *     locationRepository.getLocationUpdates()
     *         .catch { exception ->
     *             // Handle SecurityException, GPS errors, etc.
     *             handleError(exception)
     *         }
     *         .collect { locationData ->
     *             // Process location update
     *             updateUiState(locationData)
     *         }
     * }
     * ```
     *
     * @return Flow<LocationData> that emits location updates continuously
     * @throws SecurityException if ACCESS_FINE_LOCATION permission not granted
     */
    fun getLocationUpdates(): Flow<LocationData>

    /**
     * Emits GPS status updates as a Flow.
     *
     * The Flow emits the current GPS signal status based on accuracy thresholds
     * and update frequency. Status is derived from location updates:
     * - [GpsStatus.Unavailable]: No permission, GPS disabled, or no updates for >10s
     * - [GpsStatus.Acquiring]: First fix or accuracy >10m but ≤50m
     * - [GpsStatus.Active]: Accuracy ≤10m with regular updates
     *
     * This Flow is typically collected alongside [getLocationUpdates] to provide
     * real-time feedback about GPS signal quality to the user.
     *
     * Example usage:
     * ```kotlin
     * viewModelScope.launch {
     *     locationRepository.gpsStatusUpdates()
     *         .collect { status ->
     *             when (status) {
     *                 is GpsStatus.Unavailable -> showError()
     *                 is GpsStatus.Acquiring -> showAcquiring()
     *                 is GpsStatus.Active -> showActive(status.accuracy)
     *             }
     *         }
     * }
     * ```
     *
     * @return Flow<GpsStatus> that emits GPS status changes
     * @see GpsStatus for the status states and thresholds
     */
    fun gpsStatusUpdates(): Flow<GpsStatus>
}
