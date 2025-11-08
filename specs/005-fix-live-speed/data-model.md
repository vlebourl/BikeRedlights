# Data Model: Current Speed State

**Date**: 2025-11-07
**Feature**: [spec.md](./spec.md)
**Branch**: `005-fix-live-speed`

## Overview

This document defines the `CurrentSpeed` state entity and its lifecycle within the BikeRedlights reactive state architecture. Current speed is an **ephemeral real-time value** that updates with every GPS location fix and resets when ride recording stops.

## Entity Definition

### CurrentSpeed (State)

**Type**: Primitive `Double` (meters per second)
**Storage**: In-memory StateFlow (not persisted to database/DataStore)
**Lifecycle**: Created when service starts, destroyed when service stops
**Update Frequency**: Every GPS location update (1-4 seconds based on accuracy setting)

**Value Range**:
- **Minimum**: `0.0` (stationary or no GPS data)
- **Maximum**: Unlimited (determined by GPS hardware limits, typically up to ~250 m/s)
- **Typical Range**: `0.0` to `22.0` m/s (0-80 km/h for cycling)

**Units**:
- **Internal Representation**: meters per second (m/s) - matches GPS LocationData format
- **Display Units**: km/h (metric) or mph (imperial) based on user settings
- **Conversion**:
  - m/s → km/h: `speed * 3.6`
  - m/s → mph: `speed * 2.23694`

**Nullability**: Non-nullable (`Double`, not `Double?`)
- GPS unavailable → `0.0` (not null)
- Service stopped → `0.0` (reset value)

---

## State Lifecycle

```
┌────────────────────────────────────────────────────────────────┐
│ State Lifecycle of Current Speed                               │
└────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ Service      │
│ Starts       │──▶ currentSpeed = 0.0  (Initial state)
└──────────────┘

┌──────────────┐
│ Ride         │
│ Recording    │──▶ currentSpeed = 0.0  (No movement yet)
│ Starts       │
└──────────────┘

┌──────────────┐
│ GPS Update   │
│ Arrives      │──▶ currentSpeed = location.speedMps ?: 0.0
│ (1-4 sec)    │    (Updates continuously during ride)
└──────────────┘

┌──────────────┐
│ User Moves   │
│ at 15 km/h   │──▶ currentSpeed = 4.17 m/s  (15 / 3.6)
└──────────────┘

┌──────────────┐
│ User Stops   │
│ (< 1 km/h)   │──▶ currentSpeed = 0.0  (Stationary detection)
└──────────────┘

┌──────────────┐
│ Ride Paused  │
│ (Manual or   │──▶ currentSpeed = 0.0  (No GPS tracking)
│ Auto-pause)  │
└──────────────┘

┌──────────────┐
│ Ride Resumed │──▶ currentSpeed = location.speedMps  (Resumes updating)
└──────────────┘

┌──────────────┐
│ Ride Stops   │──▶ currentSpeed = 0.0  (Reset)
└──────────────┘

┌──────────────┐
│ Service      │
│ Stops        │──▶ currentSpeed = 0.0  (StateFlow garbage collected)
└──────────────┘
```

---

## StateFlow Contract

### Repository Layer

**Interface**: `domain/repository/RideRecordingStateRepository.kt`

```kotlin
interface RideRecordingStateRepository {
    // Existing methods...

    /**
     * Observe current speed in meters per second.
     *
     * Returns a StateFlow that emits speed updates from GPS location fixes.
     * Value is 0.0 when:
     * - No ride is recording
     * - Ride is paused (manual or auto-pause)
     * - User is stationary (< 1 km/h / 0.278 m/s)
     * - GPS signal is unavailable
     *
     * @return StateFlow<Double> emitting speed in m/s (never null)
     */
    fun getCurrentSpeed(): StateFlow<Double>
}
```

**Implementation**: `data/repository/RideRecordingStateRepositoryImpl.kt`

```kotlin
class RideRecordingStateRepositoryImpl @Inject constructor(
    // Existing dependencies...
) : RideRecordingStateRepository {

    // Private mutable state (internal updates only)
    private val _currentSpeed = MutableStateFlow(0.0)

    // Public read-only accessor
    override fun getCurrentSpeed(): StateFlow<Double> = _currentSpeed.asStateFlow()

    /**
     * Update current speed from GPS location fix.
     * Called by RideRecordingService on every location update.
     *
     * @param speedMps Speed in meters per second (0.0 if null or stationary)
     */
    internal suspend fun updateCurrentSpeed(speedMps: Double) {
        _currentSpeed.value = speedMps
    }

    /**
     * Reset current speed to 0.0.
     * Called when ride stops or pauses.
     */
    internal suspend fun resetCurrentSpeed() {
        _currentSpeed.value = 0.0
    }
}
```

---

### ViewModel Layer

**File**: `ui/viewmodel/RideRecordingViewModel.kt`

```kotlin
@HiltViewModel
class RideRecordingViewModel @Inject constructor(
    private val rideRecordingStateRepository: RideRecordingStateRepository,
    // Existing dependencies...
) : ViewModel() {

    /**
     * Current speed in meters per second.
     *
     * Lifecycle-aware StateFlow that:
     * - Stops collecting when no subscribers (WhileSubscribed with 5s timeout)
     * - Resets to 0.0 when ViewModel is cleared
     * - Survives configuration changes (screen rotation)
     *
     * UI should convert to km/h or mph using convertSpeed() before display.
     */
    val currentSpeed: StateFlow<Double> =
        rideRecordingStateRepository.getCurrentSpeed()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0.0
            )

    /**
     * Convert speed from m/s to display units (km/h or mph).
     *
     * @param metersPerSec Speed in meters per second
     * @param unitsSystem User's preferred units (METRIC or IMPERIAL)
     * @return Speed in km/h (METRIC) or mph (IMPERIAL)
     */
    companion object {
        fun convertSpeed(metersPerSec: Double, unitsSystem: UnitsSystem): Double {
            return when (unitsSystem) {
                UnitsSystem.METRIC -> metersPerSec * 3.6      // km/h
                UnitsSystem.IMPERIAL -> metersPerSec * 2.23694  // mph
            }
        }
    }
}
```

---

### UI Layer

**File**: `ui/screens/ride/LiveRideScreen.kt`

```kotlin
@Composable
fun LiveRideScreen(
    viewModel: RideRecordingViewModel = hiltViewModel()
) {
    // Collect current speed from ViewModel
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()

    // Collect other state...
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unitsSystem by viewModel.unitsSystem.collectAsStateWithLifecycle()

    // Pass to composable (NO conversion here - RideStatistics handles it)
    when (val state = uiState) {
        is RideRecordingUiState.Recording -> {
            RideStatistics(
                ride = state.ride,
                currentSpeed = currentSpeed,  // ✅ FIXED (was hardcoded 0.0)
                unitsSystem = unitsSystem,
            )
        }
        // Other states...
    }
}
```

**File**: `ui/components/ride/RideStatistics.kt` (NO CHANGES)

```kotlin
@Composable
fun RideStatistics(
    ride: Ride,
    currentSpeed: Double,  // In meters per second
    unitsSystem: UnitsSystem,
    modifier: Modifier = Modifier
) {
    // Convert to display units
    val displaySpeed = RideRecordingViewModel.convertSpeed(currentSpeed, unitsSystem)
    val speedUnit = if (unitsSystem == UnitsSystem.METRIC) "km/h" else "mph"

    // Display in UI (existing code already handles this correctly)
    Text(
        text = "${displaySpeed.format(1)} $speedUnit",
        style = MaterialTheme.typography.headlineMedium
    )
}
```

---

## Data Flow Sequence

### Scenario: User Accelerates from 0 to 20 km/h

```
Time    GPS Event                    StateFlow Value     UI Display
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
T+0s    Ride starts                  0.0 m/s            "0.0 km/h"
T+1s    GPS: 2.0 m/s                 2.0 m/s            "7.2 km/h"
T+2s    GPS: 3.5 m/s                 3.5 m/s            "12.6 km/h"
T+3s    GPS: 5.0 m/s                 5.0 m/s            "18.0 km/h"
T+4s    GPS: 5.5 m/s (20 km/h)       5.5 m/s            "19.8 km/h"
T+5s    GPS: 5.6 m/s                 5.6 m/s            "20.2 km/h"
```

**Flow**:
1. Service receives `LocationData(speedMps = 5.5f)`
2. Service calls `repository.updateCurrentSpeed(5.5)`
3. Repository emits `5.5` to `_currentSpeed` StateFlow
4. ViewModel's `currentSpeed` StateFlow updates (via `stateIn`)
5. LiveRideScreen collects new value (`collectAsStateWithLifecycle`)
6. RideStatistics converts `5.5 * 3.6 = 19.8 km/h`
7. UI recomposes and displays "19.8 km/h"

**Latency**: ~50ms (StateFlow emission + Compose recomposition)

---

## State Reset Conditions

Current speed automatically resets to `0.0` in these scenarios:

| Condition | Trigger | Reset Location | Reason |
|-----------|---------|----------------|---------|
| Ride stops | User taps "Stop Ride" | Service.stopRideRecording() | No tracking active |
| Ride paused | User taps "Pause" or auto-pause | Service.pauseRecording() | GPS updates stop |
| Service stops | System kills service | StateFlow garbage collected | Process terminated |
| Stationary detection | GPS speed < 0.278 m/s (1 km/h) | Service.startLocationTracking() | User stopped moving |
| GPS unavailable | Location.speedMps == null | Service.startLocationTracking() | No GPS signal |

**Configuration Changes** (screen rotation):
- Current speed **does NOT reset**
- StateFlow survives via ViewModel
- UI recomposes with latest value

---

## Testing Scenarios

### Unit Tests (Repository)

```kotlin
@Test
fun `getCurrentSpeed returns initial value of 0`() {
    val repository = RideRecordingStateRepositoryImpl(...)
    assertEquals(0.0, repository.getCurrentSpeed().value)
}

@Test
fun `updateCurrentSpeed updates StateFlow`() = runTest {
    val repository = RideRecordingStateRepositoryImpl(...)
    repository.updateCurrentSpeed(5.5)
    assertEquals(5.5, repository.getCurrentSpeed().value)
}

@Test
fun `resetCurrentSpeed sets value to 0`() = runTest {
    val repository = RideRecordingStateRepositoryImpl(...)
    repository.updateCurrentSpeed(10.0)
    repository.resetCurrentSpeed()
    assertEquals(0.0, repository.getCurrentSpeed().value)
}
```

### Unit Tests (ViewModel)

```kotlin
@Test
fun `currentSpeed exposes repository StateFlow`() = runTest {
    val mockRepository = mockk<RideRecordingStateRepository>()
    every { mockRepository.getCurrentSpeed() } returns MutableStateFlow(8.0).asStateFlow()

    val viewModel = RideRecordingViewModel(mockRepository, ...)
    assertEquals(8.0, viewModel.currentSpeed.value)
}

@Test
fun `convertSpeed converts m_s to km_h`() {
    val result = RideRecordingViewModel.convertSpeed(10.0, UnitsSystem.METRIC)
    assertEquals(36.0, result, 0.01)
}

@Test
fun `convertSpeed converts m_s to mph`() {
    val result = RideRecordingViewModel.convertSpeed(10.0, UnitsSystem.IMPERIAL)
    assertEquals(22.37, result, 0.01)
}
```

### UI Tests (LiveRideScreen)

```kotlin
@Test
fun `displays current speed during ride`() {
    val mockViewModel = mockk<RideRecordingViewModel>()
    every { mockViewModel.currentSpeed } returns MutableStateFlow(5.5).asStateFlow()
    every { mockViewModel.unitsSystem } returns MutableStateFlow(UnitsSystem.METRIC).asStateFlow()

    composeTestRule.setContent {
        LiveRideScreen(viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithText("19.8 km/h").assertExists()
}

@Test
fun `speed updates when StateFlow changes`() = runTest {
    val speedFlow = MutableStateFlow(0.0)
    val mockViewModel = mockk<RideRecordingViewModel>()
    every { mockViewModel.currentSpeed } returns speedFlow.asStateFlow()

    composeTestRule.setContent {
        LiveRideScreen(viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithText("0.0 km/h").assertExists()

    speedFlow.value = 8.33  // 30 km/h
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("30.0 km/h").assertExists()
}
```

---

## Performance Considerations

**Memory**: StateFlow overhead is minimal (~40 bytes per StateFlow instance)

**CPU**: Updates trigger Compose recomposition
- **Best case**: 1-4 recompositions per second (GPS update rate)
- **Worst case**: RideStatistics composable recomposition (~0.5ms per recomposition)
- **Total overhead**: < 2ms/second for current speed updates

**Battery**: No impact beyond existing GPS tracking (speed comes from same LocationData already being captured)

**Threading**: All StateFlow operations are main-thread safe (Kotlin coroutines handle dispatching)

---

## Next Steps

Proceed to [contracts/](./contracts/) for detailed interface specifications and [quickstart.md](./quickstart.md) for developer testing guide.
