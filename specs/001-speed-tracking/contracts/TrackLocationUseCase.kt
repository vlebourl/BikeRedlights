package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.SpeedMeasurement
import com.example.bikeredlights.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for tracking location and calculating cycling speed.
 *
 * This use case encapsulates the business logic for converting raw GPS location
 * data into meaningful speed measurements for cyclists.
 *
 * Contract: TrackLocationUseCase
 * Feature: 001-speed-tracking (Real-Time Speed and Location Tracking)
 * Date: 2025-11-02
 */
class TrackLocationUseCase(
    private val locationRepository: LocationRepository
) {

    /**
     * Tracks location updates and emits calculated speed measurements.
     *
     * Business Logic:
     * 1. Collects location updates from LocationRepository
     * 2. Converts m/s speed to km/h (×3.6)
     * 3. Applies stationary threshold (<1 km/h = 0 km/h)
     * 4. Determines speed source (GPS vs calculated)
     * 5. Calculates fallback speed from position change if GPS speed unavailable
     * 6. Clamps speed to realistic range (0-100 km/h)
     *
     * Speed Calculation Priority:
     * 1. GPS-provided speed (Location.getSpeed()) - most accurate
     * 2. Calculated from position change (distance / time) - fallback
     * 3. Zero speed - when no previous location or GPS unavailable
     *
     * Stationary Detection:
     * - Threshold: 1 km/h (~0.28 m/s)
     * - Purpose: Filter GPS jitter when cyclist is stopped
     * - Behavior: Speed < 1 km/h → SpeedMeasurement(speedKmh = 0, isStationary = true)
     *
     * Edge Cases Handled:
     * - Negative speed values → coerced to 0
     * - Unrealistic speeds (>100 km/h) → clamped to max
     * - Missing GPS speed → fallback to calculated speed
     * - First location (no previous) → zero speed
     * - Zero elapsed time → zero speed (avoid division by zero)
     *
     * Flow Behavior:
     * - Cold Flow: Starts when collected, stops when cancelled
     * - Transformation: LocationData → SpeedMeasurement
     * - Stateful: Maintains previousLocation for calculated speed
     * - Error Propagation: Errors from repository flow through
     *
     * Performance:
     * - Emissions: ~1/second (matches location update interval)
     * - Computation: Lightweight (simple math, no heavy processing)
     * - Memory: Stores only last LocationData (~100 bytes)
     *
     * Thread Safety:
     * - All operations on Flow collection dispatcher
     * - No shared mutable state (previousLocation scoped to Flow)
     * - Safe to collect from multiple scopes (each gets own previousLocation)
     *
     * Usage Example:
     * ```kotlin
     * class SpeedTrackingViewModel(
     *     private val trackLocationUseCase: TrackLocationUseCase
     * ) : ViewModel() {
     *
     *     private val _speedState = MutableStateFlow<SpeedMeasurement?>(null)
     *     val speedState = _speedState.asStateFlow()
     *
     *     fun startTracking() {
     *         viewModelScope.launch {
     *             trackLocationUseCase()
     *                 .catch { e -> handleError(e) }
     *                 .collect { measurement ->
     *                     _speedState.value = measurement
     *                 }
     *         }
     *     }
     * }
     * ```
     *
     * Testing:
     * ```kotlin
     * @Test
     * fun `converts GPS speed from m/s to km/h`() = runTest {
     *     val fakeRepo = FakeLocationRepository()
     *     val useCase = TrackLocationUseCase(fakeRepo)
     *
     *     fakeRepo.emitLocation(LocationData(speedMps = 10f)) // 10 m/s
     *
     *     useCase().test {
     *         val measurement = awaitItem()
     *         assertEquals(36f, measurement.speedKmh) // 10 * 3.6 = 36 km/h
     *     }
     * }
     * ```
     *
     * @return Flow<SpeedMeasurement> emitting calculated speed measurements
     * @throws SecurityException if location permission not granted (propagated from repository)
     * @see SpeedMeasurement for output data structure
     * @see LocationRepository for input data source
     * @see com.example.bikeredlights.domain.model.LocationData for conversion details
     */
    operator fun invoke(): Flow<SpeedMeasurement>

    // Alternative naming (if not using operator fun):
    // fun trackSpeed(): Flow<SpeedMeasurement>
}

/**
 * Internal helper: Converts LocationData to SpeedMeasurement
 *
 * This is a pure function with no Android dependencies, enabling
 * comprehensive unit testing without instrumentation tests.
 *
 * @param currentLocation Current GPS location reading
 * @param previousLocation Previous GPS location (for fallback calculation)
 * @return SpeedMeasurement with calculated speed in km/h
 */
internal fun calculateSpeed(
    currentLocation: com.example.bikeredlights.domain.model.LocationData,
    previousLocation: com.example.bikeredlights.domain.model.LocationData?
): com.example.bikeredlights.domain.model.SpeedMeasurement {
    // Implementation follows data-model.md specification
    // See: specs/001-speed-tracking/data-model.md#conversion-extensions
}
