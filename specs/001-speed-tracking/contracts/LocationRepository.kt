package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.LocationData
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for location tracking operations.
 *
 * This interface abstracts the Android Location Services implementation,
 * enabling Clean Architecture separation and testability.
 *
 * Contract: LocationRepository
 * Feature: 001-speed-tracking (Real-Time Speed and Location Tracking)
 * Date: 2025-11-02
 */
interface LocationRepository {

    /**
     * Emits continuous location updates as a Flow.
     *
     * Flow Behavior:
     * - Cold Flow: Starts tracking when collected, stops when cancelled
     * - Emits LocationData on each GPS update (typically 1/second)
     * - May emit last known location immediately if available
     * - Continues emitting until Flow is cancelled or error occurs
     *
     * Lifecycle:
     * - Start: When Flow is collected (e.g., in ViewModel's viewModelScope)
     * - Stop: When Flow collector is cancelled (e.g., app backgrounds)
     * - Automatic cleanup via callbackFlow's awaitClose block
     *
     * Location Configuration:
     * - Priority: PRIORITY_HIGH_ACCURACY (GPS-based)
     * - Interval: 1000ms (1 second updates)
     * - Min Interval: 500ms (can update faster if GPS provides)
     * - Max Delay: 2000ms (allows batching for battery efficiency)
     *
     * Error Handling:
     * - @throws SecurityException if location permission not granted
     * - Emits error via Flow.catch if GPS unavailable or disabled
     * - Closes Flow on fatal errors (e.g., FusedLocationProviderClient failure)
     *
     * Thread Safety:
     * - All emissions happen on the caller's dispatcher
     * - FusedLocationProviderClient callback runs on main looper
     * - Safe to collect from any CoroutineScope
     *
     * Usage Example:
     * ```kotlin
     * class MyViewModel(private val repo: LocationRepository) : ViewModel() {
     *     fun startTracking() {
     *         viewModelScope.launch {
     *             repo.getLocationUpdates()
     *                 .catch { exception ->
     *                     // Handle SecurityException, GPS errors, etc.
     *                     handleError(exception)
     *                 }
     *                 .collect { locationData ->
     *                     // Process location update
     *                     updateUiState(locationData)
     *                 }
     *         }
     *     }
     * }
     * ```
     *
     * Testing:
     * - Mock this interface in unit tests to provide fake location data
     * - Use Turbine library for testing Flow emissions
     * - Create FakeLocationRepository that emits predetermined LocationData
     *
     * Performance Considerations:
     * - Battery drain: ~5-10% per hour (foreground tracking with PRIORITY_HIGH_ACCURACY)
     * - Memory: Minimal (Flow is cold, no buffering)
     * - CPU: Low (GPS handled by system, emissions are lightweight)
     *
     * @return Flow<LocationData> that emits location updates continuously
     * @throws SecurityException if ACCESS_FINE_LOCATION permission not granted
     * @see LocationData for the data model structure
     * @see com.example.bikeredlights.data.repository.LocationRepositoryImpl for implementation
     */
    fun getLocationUpdates(): Flow<LocationData>

    // Future methods (out of scope for MVP):
    // fun getLastKnownLocation(): LocationData?
    // suspend fun requestSingleLocationUpdate(): LocationData
    // fun isLocationEnabled(): Boolean
}
