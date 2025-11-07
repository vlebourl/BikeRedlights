# Repository Contract: RideRecordingStateRepository

**Date**: 2025-11-07
**Feature**: Fix Live Current Speed Display Bug
**Layer**: Domain → Data

## Interface Definition

**File**: `domain/repository/RideRecordingStateRepository.kt`

```kotlin
package com.example.bikeredlights.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing ride recording state and real-time metrics.
 *
 * This repository provides reactive access to ride state, current speed, and other
 * real-time metrics during active ride recording.
 */
interface RideRecordingStateRepository {

    // ... existing methods ...

    /**
     * Observe current speed in meters per second.
     *
     * Returns a StateFlow that emits real-time speed updates from GPS location fixes.
     * The speed value updates with every GPS location callback during active ride recording.
     *
     * **Value Semantics**:
     * - `0.0`: No ride recording, ride paused, user stationary, or GPS unavailable
     * - `> 0.0`: Current GPS-provided speed in meters per second
     *
     * **Update Frequency**:
     * - High Accuracy GPS: Every 1 second
     * - Battery Saver GPS: Every 4 seconds
     *
     * **Lifecycle**:
     * - Created when service starts
     * - Resets to 0.0 when ride stops or pauses
     * - Survives configuration changes (ViewModel retains StateFlow)
     * - Destroyed when service stops (garbage collected)
     *
     * **Thread Safety**: StateFlow is thread-safe and can be collected from any dispatcher.
     *
     * **Usage Example**:
     * ```kotlin
     * viewModelScope.launch {
     *     repository.getCurrentSpeed()
     *         .collect { speedMps ->
     *             val speedKmh = speedMps * 3.6
     *             _uiState.value = UiState.Speed(speedKmh)
     *         }
     * }
     * ```
     *
     * @return StateFlow<Double> emitting speed in meters per second (never null, minimum 0.0)
     */
    fun getCurrentSpeed(): StateFlow<Double>
}
```

---

## Implementation Contract

**File**: `data/repository/RideRecordingStateRepositoryImpl.kt`

```kotlin
package com.example.bikeredlights.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ride recording state repository.
 *
 * Manages in-memory reactive state for ride recording session including
 * current speed from GPS location updates.
 */
@Singleton
class RideRecordingStateRepositoryImpl @Inject constructor(
    // ... existing dependencies ...
) : RideRecordingStateRepository {

    /**
     * Private mutable state for current speed.
     *
     * INVARIANT: Value is always >= 0.0 (never negative or null)
     * INVARIANT: Value is in meters per second (m/s)
     */
    private val _currentSpeed = MutableStateFlow(0.0)

    /**
     * Public read-only accessor for current speed.
     *
     * Implements interface contract. Returns immutable StateFlow to prevent
     * external mutation of state.
     */
    override fun getCurrentSpeed(): StateFlow<Double> = _currentSpeed.asStateFlow()

    /**
     * Update current speed from GPS location fix.
     *
     * INTERNAL USE ONLY - Called by RideRecordingService on location updates.
     *
     * **Preconditions**:
     * - speedMps >= 0.0 (negative speeds are invalid, use 0.0 instead)
     * - Called only during active ride recording (not during pause)
     *
     * **Postconditions**:
     * - _currentSpeed.value == speedMps
     * - All collectors receive new value emission
     *
     * **Thread Safety**: Coroutine-safe, can be called from any dispatcher.
     *
     * @param speedMps Speed in meters per second (0.0 if null or stationary)
     */
    internal suspend fun updateCurrentSpeed(speedMps: Double) {
        require(speedMps >= 0.0) { "Speed must be non-negative, got: $speedMps" }
        _currentSpeed.value = speedMps
    }

    /**
     * Reset current speed to 0.0.
     *
     * INTERNAL USE ONLY - Called when ride stops or pauses.
     *
     * **Postconditions**:
     * - _currentSpeed.value == 0.0
     * - All collectors receive 0.0 emission
     */
    internal suspend fun resetCurrentSpeed() {
        _currentSpeed.value = 0.0
    }
}
```

---

## Service Contract

**File**: `service/RideRecordingService.kt`

```kotlin
/**
 * Service responsibility: Emit current speed to repository on every GPS update.
 *
 * Location: Inside startLocationTracking() method, after receiving LocationData.
 */

// Pseudo-code showing required integration:
private fun startLocationTracking() {
    locationRepository.observeLocationUpdates()
        .collect { locationData ->
            // ... existing track point recording ...

            // NEW: Update current speed StateFlow
            val currentSpeed = (locationData.speedMps ?: 0f).toDouble()
            rideRecordingStateRepository.updateCurrentSpeed(currentSpeed)
        }
}

// On ride stop/pause:
private fun stopOrPauseRide() {
    // ... existing stop/pause logic ...

    // NEW: Reset current speed
    rideRecordingStateRepository.resetCurrentSpeed()
}
```

**Contract Requirements**:
- Service MUST call `updateCurrentSpeed()` on every LocationData emission
- Service MUST call `resetCurrentSpeed()` on ride stop or pause
- Speed value MUST be >= 0.0 (convert null to 0.0)
- Service MUST handle GPS unavailability gracefully (pass 0.0)

---

## Error Handling

### Invalid Speed Values

**Scenario**: GPS provides negative speed (hardware error)

**Contract**:
```kotlin
internal suspend fun updateCurrentSpeed(speedMps: Double) {
    require(speedMps >= 0.0) { "Speed must be non-negative, got: $speedMps" }
    // ... implementation ...
}
```

**Enforcement**: Repository throws `IllegalArgumentException` if contract violated.

**Service Responsibility**: Sanitize speed before passing to repository:
```kotlin
val currentSpeed = maxOf(0.0, (locationData.speedMps ?: 0f).toDouble())
rideRecordingStateRepository.updateCurrentSpeed(currentSpeed)
```

### GPS Signal Loss

**Scenario**: Location updates stop arriving (GPS signal lost)

**Contract**: Service MUST call `resetCurrentSpeed()` or continue with last known value.

**Design Decision**: Continue with last known value until location updates resume or ride stops.
- **Rationale**: Sudden speed reset to 0.0 during brief signal loss would be jarring for users
- **Rationale**: Last known speed is more useful than 0.0 for brief interruptions
- **Fallback**: If signal loss exceeds 10 seconds, consider showing "No GPS" indicator (future enhancement, out of scope)

### Repository Initialization Race Condition

**Scenario**: ViewModel collects `getCurrentSpeed()` before repository is fully initialized.

**Contract**: `_currentSpeed` is initialized to `0.0` at class instantiation.

**Guarantee**: StateFlow always has a valid value (no null emissions).

---

## Testing Contract

### Repository Tests MUST Verify:

```kotlin
@Test
fun `getCurrentSpeed returns initial value of 0_0`() {
    // Verify initial state contract
    assertEquals(0.0, repository.getCurrentSpeed().value)
}

@Test
fun `updateCurrentSpeed updates StateFlow`() = runTest {
    // Verify state mutation contract
    repository.updateCurrentSpeed(5.5)
    assertEquals(5.5, repository.getCurrentSpeed().value)
}

@Test
fun `updateCurrentSpeed throws on negative value`() = runTest {
    // Verify precondition enforcement
    assertThrows<IllegalArgumentException> {
        repository.updateCurrentSpeed(-1.0)
    }
}

@Test
fun `resetCurrentSpeed sets value to 0_0`() = runTest {
    // Verify reset behavior contract
    repository.updateCurrentSpeed(10.0)
    repository.resetCurrentSpeed()
    assertEquals(0.0, repository.getCurrentSpeed().value)
}

@Test
fun `getCurrentSpeed emits to multiple collectors`() = runTest {
    // Verify reactive contract (StateFlow multicasting)
    val collector1 = async {
        repository.getCurrentSpeed().take(2).toList()
    }
    val collector2 = async {
        repository.getCurrentSpeed().take(2).toList()
    }

    repository.updateCurrentSpeed(5.0)

    assertEquals(listOf(0.0, 5.0), collector1.await())
    assertEquals(listOf(0.0, 5.0), collector2.await())
}
```

---

## Performance Contract

**Memory**: StateFlow overhead ~40 bytes per instance.

**CPU**: StateFlow update is O(1) operation:
- Value assignment: ~10 nanoseconds
- Emission to collectors: ~50 microseconds per collector

**Threading**: All operations are main-thread safe:
- `updateCurrentSpeed()`: Can be called from any dispatcher
- `getCurrentSpeed()`: Collection can happen on any dispatcher
- StateFlow internally handles thread safety via atomic operations

**Latency**: From GPS update to UI display:
- Service to Repository: < 1 millisecond
- Repository to ViewModel: < 1 millisecond (StateFlow emission)
- ViewModel to UI: < 50 milliseconds (Compose recomposition)
- **Total**: < 52 milliseconds (imperceptible to users)

---

## Backward Compatibility

**Breaking Changes**: None

**New API**: `getCurrentSpeed()` is a new method addition to existing interface.

**Migration**: Existing code is unaffected. Only new UI code needs to collect the StateFlow.

**Deprecation**: N/A (no existing speed API to deprecate)

---

## Future Extensions

### Possible Future Enhancements (Out of Scope)

1. **Speed Smoothing**:
   ```kotlin
   fun getCurrentSpeed(smoothingWindow: Int = 3): StateFlow<Double>
   ```
   - Moving average over N GPS updates to reduce jitter
   - Requires buffering last N speed values

2. **Speed History**:
   ```kotlin
   fun getSpeedHistory(durationMillis: Long): Flow<List<Double>>
   ```
   - Last N seconds of speed data for graphing
   - Requires circular buffer storage

3. **Anomaly Detection**:
   ```kotlin
   fun getSpeedAnomalies(): Flow<SpeedAnomaly>
   ```
   - Detect impossible speed changes (GPS errors)
   - Requires heuristics for valid acceleration ranges

**Decision**: Keep contract simple for MVP. Extensions can be added later without breaking existing API.
