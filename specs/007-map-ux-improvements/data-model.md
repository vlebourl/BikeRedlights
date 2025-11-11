# Data Model: Map UX Improvements (v0.6.1 Patch)

**Feature**: 007-map-ux-improvements
**Date**: 2025-11-10
**Purpose**: Define state models, UI contracts, and data flows for map UX enhancements

## Overview

This feature does not introduce new database entities or persistent data structures. All changes are **UI state models** and **transient runtime state** for presentation layer enhancements. No Room schema changes, no new repositories (except adding one key to existing SettingsRepository).

---

## State Models

### 1. MapViewState (Modified)

**Location**: `app/src/main/java/com/example/bikeredlights/domain/model/MapViewState.kt`

**Purpose**: Represents the current state of the map display, including camera position, markers, polylines, and **NEW: bearing rotation**

**Existing Fields** (unchanged):
```kotlin
data class MapViewState(
    val center: LatLng?,
    val zoom: Float,
    val markers: List<MarkerData>,
    val polyline: PolylineData?,
    val bounds: MapBounds?
)
```

**New Field** (added):
```kotlin
data class MapViewState(
    // ... existing fields ...
    val bearing: Float? = null // NEW - Map rotation in degrees (0-360), null = north-up
)
```

**Validation Rules**:
- `bearing` must be in range 0-360 degrees (or null)
- If `bearing` is non-null and outside 0-360 range, normalize to 0-360 using modulo operation
- `bearing = null` indicates north-up orientation (default fallback)

**State Transitions**:
- **Initial**: `bearing = null` (north-up)
- **Moving with valid GPS bearing**: `bearing = Location.bearing` (0-360)
- **Stationary or bearing unavailable**: `bearing` retains last known value or reverts to null after staleness timeout

---

### 2. LocationMarkerState (New)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt` (inline data class or parameters)

**Purpose**: Encapsulates the display state for the current location marker (icon type, rotation, position)

**Fields**:
```kotlin
data class LocationMarkerState(
    val position: LatLng,
    val bearing: Float?, // Marker rotation (0-360 degrees), null = no bearing available
    val isMoving: Boolean // true = show directional arrow, false = fallback to pin/static icon
)
```

**Validation Rules**:
- `bearing` must be in range 0-360 degrees (or null)
- `isMoving` determines icon type: true → directional arrow, false → pin or static marker
- If `bearing == null` and `isMoving == true`, display static arrow pointing north or fallback to pin

**State Transitions**:
- **Stationary (speed < threshold)**: `isMoving = false`, `bearing = null` → Display pin icon
- **Moving with bearing**: `isMoving = true`, `bearing = [0-360]` → Display rotated directional arrow
- **Moving without bearing**: `isMoving = true`, `bearing = null` → Display non-rotated arrow or pin

**Icon Selection Logic**:
```
if (isMoving && bearing != null) → Display directional arrow rotated to `bearing`
else if (isMoving && bearing == null) → Display static arrow pointing north (or pin fallback)
else → Display pin icon (stationary)
```

---

### 3. PauseTimerState (Modified ViewModel State)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`

**Purpose**: Tracks real-time elapsed pause duration for live UI updates

**Existing State** (inferred from v0.6.0):
```kotlin
// Existing: Pause state tracked but not real-time
data class RideState(
    val isRecording: Boolean,
    val isPaused: Boolean,
    val pausedAt: Instant?,
    // ... other ride state ...
)
```

**New State** (added to ViewModel):
```kotlin
// NEW: Real-time pause duration flow
val pausedDuration: StateFlow<Duration> // Emits updated Duration every second while paused
```

**Flow Emission Logic**:
```kotlin
pausedDuration = _pauseStartTime.flatMapLatest { startTime ->
    if (startTime == null) {
        flowOf(Duration.ZERO) // Not paused
    } else {
        flow {
            while (true) {
                emit(Duration.between(startTime, Instant.now()))
                delay(1000) // Update every second
            }
        }
    }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Duration.ZERO)
```

**Validation Rules**:
- `pausedDuration` must never be negative (Instant.now() >= startTime)
- When pause ends (resume), `pausedDuration` resets to Duration.ZERO or transitions to "total paused time" accumulator

**State Transitions**:
- **Active riding**: `pausedDuration = Duration.ZERO`
- **Auto-pause triggered**: `pausedDuration` starts incrementing from 0 every second
- **Resume riding**: `pausedDuration` freezes at final value, added to cumulative pause time, then resets flow to ZERO

---

### 4. AutoPauseSettingsState (New DataStore Key)

**Location**: `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepository.kt`

**Purpose**: Persistent storage of user-selected auto-pause timing threshold

**DataStore Key**:
```kotlin
private val AUTO_PAUSE_THRESHOLD_KEY = intPreferencesKey("auto_pause_threshold_seconds")
```

**Storage Type**: Int (seconds)

**Valid Values**: 1, 2, 5, 10, 15, 30 (seconds)
- Any other value is considered invalid and should fall back to default (5 seconds)

**Default Value**: 5 seconds (medium sensitivity)

**Validation Rules**:
- Must be one of the six allowed values: {1, 2, 5, 10, 15, 30}
- If stored value is not in allowed set, use default (5)
- Negative values are invalid → use default

**Repository API**:
```kotlin
fun getAutoPauseThreshold(): Flow<Int> // Emits current threshold (default 5)
suspend fun setAutoPauseThreshold(seconds: Int) // Validates and stores
```

---

## Data Flows

### Flow 1: Map Bearing Update

```
[GPS Location Update]
    → Location.bearing (0-360 or null)
    → RideRecordingViewModel._currentBearing (StateFlow<Float?>)
    → MapViewState.bearing
    → LiveRideScreen observes MapViewState
    → BikeMap composable receives bearing parameter
    → CameraPositionState.animate(bearing = newBearing)
    → Map rotates smoothly
```

**Debouncing/Smoothing**:
- Only update bearing if `abs(newBearing - oldBearing) > 5 degrees` (avoid jitter)
- Animate over 300ms to prevent disorienting jumps

### Flow 2: Directional Marker Rotation

```
[GPS Location Update]
    → Location.bearing (0-360 or null)
    → RideRecordingViewModel._currentBearing (StateFlow<Float?>)
    → LocationMarkerState(position, bearing, isMoving)
    → LocationMarker composable receives state
    → graphicsLayer { rotationZ = bearing ?: 0f }
    → Marker rotates to match heading
```

**Fallback Flow** (no bearing):
```
[Stationary or No Bearing]
    → bearing = null
    → LocationMarkerState(bearing = null)
    → Display pin icon or static arrow (no rotation)
```

### Flow 3: Real-Time Pause Counter

```
[Auto-Pause Triggered]
    → RideRecordingViewModel._pauseStartTime = Instant.now()
    → pausedDuration Flow starts emitting (Duration.ZERO, then +1s every second)
    → RideStatistics composable collects pausedDuration via collectAsStateWithLifecycle()
    → Text("Paused: ${duration.toMinutes()}:${duration.seconds % 60}")
    → Updates every second in UI
```

**Resume Flow**:
```
[Resume Riding]
    → _pauseStartTime = null
    → pausedDuration Flow emits Duration.ZERO
    → Final paused duration added to cumulative ride stats
```

### Flow 4: Auto-Pause Settings Change

```
[User selects timing option in Settings]
    → SettingsViewModel.setAutoPauseThreshold(seconds)
    → SettingsRepository.setAutoPauseThreshold(seconds)
    → DataStore.edit { preferences[AUTO_PAUSE_THRESHOLD_KEY] = seconds }
    → Setting persisted
```

**Application to Active Ride**:
```
[RideRecordingViewModel starts or restarts]
    → Observes SettingsRepository.getAutoPauseThreshold()
    → Uses threshold for speed monitoring
    → When speed < threshold for [threshold] seconds → trigger auto-pause
```

---

## UI Component State Contracts

### BikeMap Composable

**Input Parameters**:
```kotlin
@Composable
fun BikeMap(
    mapViewState: MapViewState, // Includes bearing field
    currentLocation: LatLng?,
    currentBearing: Float?, // NEW - for location marker rotation
    modifier: Modifier = Modifier
)
```

**State Management**:
- `CameraPositionState` (local state) - updated via LaunchedEffect when mapViewState.bearing changes
- `MarkerState` (local state) - position + rotation for current location marker

**Behavior**:
- When `mapViewState.bearing` changes → animate camera rotation
- When `currentBearing` changes → rotate location marker icon

### LocationMarker Composable

**Input Parameters**:
```kotlin
@Composable
fun LocationMarker(
    position: LatLng,
    bearing: Float?, // null = no directional rotation
    isMoving: Boolean, // true = arrow, false = pin
    modifier: Modifier = Modifier
)
```

**Rendering Logic**:
- Draw directional arrow using Canvas API
- Apply `Modifier.graphicsLayer { rotationZ = bearing ?: 0f }`
- If `!isMoving`, render pin icon instead of arrow

### RideStatistics Composable (Pause Counter)

**Input Parameters**:
```kotlin
@Composable
fun RideStatistics(
    isPaused: Boolean,
    pausedDuration: Duration, // NEW - real-time updating
    // ... other stats ...
)
```

**Rendering Logic**:
- Format `pausedDuration` as "MM:SS" or "X min Y sec"
- Update automatically when `pausedDuration` Flow emits new value
- No manual timer needed in composable - ViewModel handles Flow emission

### AutoPauseSettingsScreen Composable

**Input Parameters**:
```kotlin
@Composable
fun AutoPauseSettingsScreen(
    currentThreshold: Int, // Current selection (1, 2, 5, 10, 15, 30)
    onThresholdSelected: (Int) -> Unit, // Callback to SettingsViewModel
    modifier: Modifier = Modifier
)
```

**UI Elements**:
- RadioButton group or Dropdown with 6 options
- Each option: (seconds, label) pairs
  - 1 → "1 second"
  - 2 → "2 seconds"
  - 5 → "5 seconds"
  - 10 → "10 seconds"
  - 15 → "15 seconds"
  - 30 → "30 seconds"
- Selected state: `currentThreshold == option.seconds`

---

## State Validation Summary

| State | Validation Rules | Invalid Handling |
|-------|------------------|------------------|
| MapViewState.bearing | 0-360 or null | Normalize to 0-360 via modulo, or set null |
| LocationMarkerState.bearing | 0-360 or null | Display non-rotated marker if invalid |
| PauseTimerState.pausedDuration | >= Duration.ZERO | Clamp to ZERO if negative (should never happen) |
| AutoPauseSettingsState.threshold | {1,2,5,10,15,30} | Fallback to default (5) if not in set |

---

## Testing Considerations

### Unit Testing (ViewModels)

**RideRecordingViewModel**:
- Test `pausedDuration` Flow emits correctly every second
- Verify Flow stops emitting when pause ends
- Ensure bearing updates propagate to MapViewState

**SettingsViewModel**:
- Test setting auto-pause threshold persists to DataStore
- Verify invalid values fall back to default (5)
- Ensure Flow emits updated threshold immediately

### Compose UI Testing

**BikeMap**:
- Verify map rotates when bearing changes
- Test smooth animation (no jarring jumps)
- Confirm fallback to north-up when bearing is null

**LocationMarker**:
- Test marker rotates to match bearing
- Verify fallback to pin icon when bearing unavailable
- Ensure graphicsLayer rotation is efficient (no recomposition)

**RideStatistics**:
- Verify pause counter updates every second
- Test counter freezes correctly on resume
- Ensure no UI lag during updates

### Integration Testing

**Auto-Pause Settings**:
- Change setting → verify new threshold applies to next ride
- Test mid-ride setting change doesn't affect current pause state
- Verify persistence across app restarts

---

## Migration Notes

**Schema Changes**: None - all state is transient or stored in existing DataStore

**Data Migration**: Not applicable - new DataStore key with default value (5 seconds)

**Backward Compatibility**: Full compatibility with v0.6.0 rides (no database schema changes)

**Rollback Plan**: If needed, revert code changes - no data corruption risk (UI-only changes)
