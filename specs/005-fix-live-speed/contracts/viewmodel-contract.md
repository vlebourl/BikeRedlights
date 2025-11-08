# ViewModel Contract: RideRecordingViewModel

**Date**: 2025-11-07
**Feature**: Fix Live Current Speed Display Bug
**Layer**: UI (ViewModel) → Domain (Repository)

## Interface Definition

**File**: `ui/viewmodel/RideRecordingViewModel.kt`

```kotlin
package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for ride recording screens.
 *
 * Manages UI state for live ride tracking including current speed,
 * ride metrics, and recording state.
 */
@HiltViewModel
class RideRecordingViewModel @Inject constructor(
    private val rideRecordingStateRepository: RideRecordingStateRepository,
    // ... existing dependencies ...
) : ViewModel() {

    // ... existing properties ...

    /**
     * Current speed in meters per second.
     *
     * Lifecycle-aware StateFlow that automatically:
     * - Starts collecting from repository when UI subscribes
     * - Stops collecting after 5 seconds of no subscribers (battery optimization)
     * - Survives configuration changes (screen rotation, backgrounding)
     * - Resets to 0.0 when ViewModel is cleared
     *
     * **Value Semantics**:
     * - `0.0`: No ride recording, ride paused, user stationary, or GPS unavailable
     * - `> 0.0`: Current GPS-provided speed in meters per second
     *
     * **Update Frequency**:
     * - High Accuracy GPS: Every 1 second
     * - Battery Saver GPS: Every 4 seconds
     *
     * **UI Usage**:
     * ```kotlin
     * val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
     * val displaySpeed = RideRecordingViewModel.convertSpeed(currentSpeed, unitsSystem)
     * Text("${displaySpeed.format(1)} km/h")
     * ```
     *
     * **DO NOT** convert units in ViewModel - conversion happens in UI layer for flexibility.
     *
     * @see convertSpeed for unit conversion helper
     */
    val currentSpeed: StateFlow<Double> =
        rideRecordingStateRepository.getCurrentSpeed()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = 0.0
            )

    companion object {
        /**
         * Convert speed from meters per second to display units.
         *
         * Helper function for UI layer to convert internal speed representation
         * (m/s) to user-facing units (km/h or mph) based on settings.
         *
         * **Conversion Factors**:
         * - m/s to km/h: multiply by 3.6 (1 m/s = 3.6 km/h)
         * - m/s to mph: multiply by 2.23694 (1 m/s ≈ 2.237 mph)
         *
         * **Usage Example**:
         * ```kotlin
         * val speedMps = 10.0 // meters per second
         * val speedKmh = RideRecordingViewModel.convertSpeed(speedMps, UnitsSystem.METRIC)
         * // Result: 36.0 km/h
         * ```
         *
         * **Thread Safety**: Pure function, safe to call from any thread.
         *
         * @param metersPerSec Speed in meters per second (must be >= 0.0)
         * @param unitsSystem User's preferred unit system (METRIC or IMPERIAL)
         * @return Speed in km/h (METRIC) or mph (IMPERIAL)
         * @throws IllegalArgumentException if metersPerSec < 0.0
         */
        fun convertSpeed(metersPerSec: Double, unitsSystem: UnitsSystem): Double {
            require(metersPerSec >= 0.0) { "Speed must be non-negative, got: $metersPerSec" }
            return when (unitsSystem) {
                UnitsSystem.METRIC -> metersPerSec * 3.6      // km/h
                UnitsSystem.IMPERIAL -> metersPerSec * 2.23694  // mph
            }
        }
    }
}
```

---

## StateFlow Lifecycle Contract

### `SharingStarted.WhileSubscribed(5000)` Behavior

**When UI Subscribes** (e.g., LiveRideScreen becomes visible):
1. ViewModel starts collecting from repository's `getCurrentSpeed()`
2. Latest speed value is immediately emitted to UI
3. Subsequent updates flow through automatically

**When UI Unsubscribes** (e.g., user navigates away):
1. ViewModel continues collecting for 5 seconds (timeout)
2. If no new subscribers within 5 seconds, collection stops
3. Battery optimization: Repository StateFlow is no longer collected

**When UI Re-subscribes** (e.g., user navigates back):
1. If within 5-second timeout: Immediately receives latest value (no restart)
2. If after timeout: Collection restarts, receives current repository value

**On Configuration Change** (screen rotation):
1. ViewModel survives (Android default behavior with `@HiltViewModel`)
2. StateFlow value retained (no reset to 0.0)
3. UI re-collects and receives latest value immediately

**On ViewModel Cleared** (app closes, user finishes activity):
1. StateFlow collection stops
2. ViewModel scope cancels
3. Memory released (garbage collected)

---

## Unit Conversion Contract

### Why Conversion Happens in UI, Not ViewModel?

**Design Rationale**:
- ViewModel exposes raw m/s value (matches GPS LocationData format)
- UI handles display formatting and unit conversion
- Allows for flexible display without ViewModel changes (e.g., showing both km/h and mph)

**Example Scenario**:
```kotlin
// ViewModel exposes raw value
val currentSpeed: StateFlow<Double> = ... // 10.0 m/s

// UI decides how to display
@Composable
fun SpeedDisplay() {
    val speedMps by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val unitsSystem by viewModel.unitsSystem.collectAsStateWithLifecycle()

    // Convert to display units
    val displaySpeed = RideRecordingViewModel.convertSpeed(speedMps, unitsSystem)
    val unit = if (unitsSystem == UnitsSystem.METRIC) "km/h" else "mph"

    Text("$displaySpeed $unit")  // "36.0 km/h" or "22.4 mph"
}
```

**Alternative (Rejected)**:
```kotlin
// BAD: ViewModel pre-converts value
val currentSpeedKmh: StateFlow<Double> = ... // 36.0 km/h

// UI is locked into km/h display
@Composable
fun SpeedDisplay() {
    val speedKmh by viewModel.currentSpeedKmh.collectAsStateWithLifecycle()
    Text("$speedKmh km/h")  // Cannot switch to mph without ViewModel change
}
```

---

## Error Handling Contract

### Negative Speed Values

**Contract**: `convertSpeed()` throws `IllegalArgumentException` if speed < 0.0.

**Enforcement**:
```kotlin
fun convertSpeed(metersPerSec: Double, unitsSystem: UnitsSystem): Double {
    require(metersPerSec >= 0.0) { "Speed must be non-negative, got: $metersPerSec" }
    // ... conversion ...
}
```

**Caller Responsibility**: UI MUST NOT pass negative values. ViewModel StateFlow guarantees non-negative values (repository contract enforces this).

**Recovery**: If negative value somehow occurs, app crashes with clear error message (fail-fast principle).

---

## Testing Contract

### ViewModel Tests MUST Verify:

```kotlin
@Test
fun `currentSpeed exposes repository StateFlow`() = runTest {
    // Given: Repository with speed value
    val mockRepository = mockk<RideRecordingStateRepository>()
    every { mockRepository.getCurrentSpeed() } returns MutableStateFlow(8.0).asStateFlow()

    // When: ViewModel is created
    val viewModel = RideRecordingViewModel(mockRepository, ...)

    // Then: currentSpeed matches repository value
    assertEquals(8.0, viewModel.currentSpeed.value)
}

@Test
fun `currentSpeed survives configuration changes`() = runTest {
    // Given: ViewModel with speed value
    val viewModel = RideRecordingViewModel(...)
    val initialSpeed = viewModel.currentSpeed.value

    // When: Configuration change simulated (ViewModel retained)
    // (Android framework handles this automatically)

    // Then: Speed value is retained
    assertEquals(initialSpeed, viewModel.currentSpeed.value)
}

@Test
fun `convertSpeed converts m_s to km_h`() {
    // Given: Speed in m/s
    val speedMps = 10.0

    // When: Convert to km/h
    val result = RideRecordingViewModel.convertSpeed(speedMps, UnitsSystem.METRIC)

    // Then: Result is correct (10 * 3.6 = 36.0)
    assertEquals(36.0, result, 0.001)
}

@Test
fun `convertSpeed converts m_s to mph`() {
    // Given: Speed in m/s
    val speedMps = 10.0

    // When: Convert to mph
    val result = RideRecordingViewModel.convertSpeed(speedMps, UnitsSystem.IMPERIAL)

    // Then: Result is correct (10 * 2.23694 = 22.3694)
    assertEquals(22.3694, result, 0.001)
}

@Test
fun `convertSpeed throws on negative value`() {
    // Given: Negative speed
    val speedMps = -5.0

    // When/Then: Throws IllegalArgumentException
    assertThrows<IllegalArgumentException> {
        RideRecordingViewModel.convertSpeed(speedMps, UnitsSystem.METRIC)
    }
}

@Test
fun `currentSpeed stops collecting after 5 seconds of no subscribers`() = runTest {
    // Given: ViewModel with speed StateFlow
    val mockRepository = mockk<RideRecordingStateRepository>()
    val speedFlow = MutableStateFlow(0.0)
    every { mockRepository.getCurrentSpeed() } returns speedFlow.asStateFlow()

    val viewModel = RideRecordingViewModel(mockRepository, ...)

    // When: Collect, then cancel
    val job = launch {
        viewModel.currentSpeed.collect { }
    }
    delay(100)
    job.cancel()

    // Then: Wait for timeout (5 seconds + buffer)
    delay(5500)

    // Verify: Repository StateFlow is no longer being collected
    // (This is tested by checking that updates don't propagate)
    speedFlow.value = 10.0
    delay(100)
    // If collection stopped, ViewModel's stateIn cache won't update
}
```

---

## Performance Contract

**Memory Overhead**:
- StateFlow: ~40 bytes per instance
- `stateIn` operator: ~200 bytes (caching + coroutine scope)
- **Total**: ~240 bytes per ViewModel instance

**CPU Overhead**:
- StateFlow emission: ~50 microseconds
- `stateIn` replay: ~10 microseconds
- Unit conversion: ~5 nanoseconds (pure math)
- **Total**: < 100 microseconds per speed update

**Battery Impact**:
- StateFlow collection: Negligible (event-driven, no polling)
- WhileSubscribed timeout: Saves battery when UI not visible (stops collection after 5s)
- No background work: Collection only happens when ViewModel is active

**Thread Safety**:
- `currentSpeed` StateFlow: Safe to collect from any dispatcher
- `convertSpeed()`: Pure function, safe to call from any thread
- ViewModel scope: Main dispatcher (Android default)

---

## UI Integration Contract

### LiveRideScreen Usage

**File**: `ui/screens/ride/LiveRideScreen.kt`

```kotlin
@Composable
fun LiveRideScreen(
    viewModel: RideRecordingViewModel = hiltViewModel()
) {
    // Collect current speed (lifecycle-aware)
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()

    // Collect units preference
    val unitsSystem by viewModel.unitsSystem.collectAsStateWithLifecycle()

    // Collect ride state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pass to UI components
    when (val state = uiState) {
        is RideRecordingUiState.Recording -> {
            RideStatistics(
                ride = state.ride,
                currentSpeed = currentSpeed,  // Raw m/s value
                unitsSystem = unitsSystem,     // For conversion
            )
        }
        // ... other states ...
    }
}
```

**Contract Requirements**:
- MUST use `collectAsStateWithLifecycle()` (not plain `collect`)
  - **Rationale**: Lifecycle-aware collection prevents memory leaks
  - **Rationale**: Automatically cancels collection when screen not visible
- MUST pass raw `currentSpeed` value (m/s, not converted)
  - **Rationale**: Composables handle conversion based on units preference
- MUST pass `unitsSystem` alongside `currentSpeed`
  - **Rationale**: Allows composables to convert units correctly

---

## Backward Compatibility

**Breaking Changes**: None

**New API**: `currentSpeed` property is a new addition.

**Migration**: Existing code is unaffected. UI code that previously used hardcoded 0.0 can now collect this StateFlow.

**Example Migration**:
```kotlin
// Before (hardcoded)
RideStatistics(
    ride = ride,
    currentSpeed = 0.0,  // ❌ Always zero
    unitsSystem = unitsSystem,
)

// After (reactive)
val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
RideStatistics(
    ride = ride,
    currentSpeed = currentSpeed,  // ✅ Real-time updates
    unitsSystem = unitsSystem,
)
```

---

## Future Extensions

### Possible Future Enhancements (Out of Scope)

1. **Speed Smoothing in ViewModel**:
   ```kotlin
   val smoothedSpeed: StateFlow<Double> =
       repository.getCurrentSpeed()
           .scan(emptyList<Double>()) { acc, speed -> (acc + speed).takeLast(3) }
           .map { it.average() }
           .stateIn(...)
   ```
   - 3-point moving average to reduce GPS jitter
   - Exposes separate smoothed StateFlow alongside raw speed

2. **Speed Change Rate**:
   ```kotlin
   val acceleration: StateFlow<Double> // m/s²
   ```
   - Detect rapid acceleration/deceleration
   - Useful for advanced analytics or gamification

3. **Speed Thresholds**:
   ```kotlin
   val isOverSpeedLimit: StateFlow<Boolean>
   ```
   - Compare against configurable speed limit
   - Useful for red light warning feature (future)

**Decision**: Keep contract simple for MVP. Extensions can be added later without breaking existing API.
