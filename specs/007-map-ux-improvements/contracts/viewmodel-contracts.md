# ViewModel Contracts: Map UX Improvements (v0.6.1 Patch)

**Feature**: 007-map-ux-improvements
**Date**: 2025-11-10
**Purpose**: Define ViewModel API contracts for map bearing, real-time pause counter, and settings

---

## 1. RideRecordingViewModel (Modified)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`

### New/Modified State Properties

#### `currentBearing: StateFlow<Float?>`

**Purpose**: Exposes current GPS bearing (heading direction) for map rotation and marker orientation

**Type**: `StateFlow<Float?>` (nullable - bearing may be unavailable)

**Values**:
- `0.0f - 360.0f`: Valid bearing in degrees clockwise from north
- `null`: Bearing unavailable (stationary, GPS signal lost, or device doesn't support bearing)

**Emission Behavior**:
- Emits new value when GPS location update includes valid bearing (`Location.hasBearing() == true`)
- Retains last known bearing for up to 60 seconds of no updates (configurable)
- Emits `null` if bearing becomes stale or device is stationary for extended period

**Lifecycle**:
- Active during ride recording
- Resets to `null` when ride ends
- Persists through pause/resume cycles (bearing continues updating if moving)

**Example Usage**:
```kotlin
// In LiveRideScreen composable
val currentBearing by viewModel.currentBearing.collectAsStateWithLifecycle()

BikeMap(
    mapViewState = mapViewState,
    currentBearing = currentBearing // Map rotates to this bearing
)
```

---

#### `pausedDuration: StateFlow<Duration>`

**Purpose**: Provides real-time elapsed pause duration for live UI updates (updates every ~1 second while paused)

**Type**: `StateFlow<Duration>` (non-nullable, emits `Duration.ZERO` when not paused)

**Values**:
- `Duration.ZERO`: Not currently paused
- `Duration.ofSeconds(N)`: Elapsed pause time (N seconds), updated every second

**Emission Behavior**:
- When pause starts: Begins emitting `Duration.ofSeconds(0)`, then increments every ~1000ms
- While paused: Emits updated duration every second (0s → 1s → 2s → 3s → ...)
- On resume: Emits `Duration.ZERO` immediately
- Background behavior: Continues calculating elapsed time even when app is backgrounded (uses wall-clock time)

**Implementation Pattern**:
```kotlin
val pausedDuration: StateFlow<Duration> = _pauseStartTime
    .flatMapLatest { startTime ->
        if (startTime == null) {
            flowOf(Duration.ZERO)
        } else {
            flow {
                while (true) {
                    emit(Duration.between(startTime, Instant.now()))
                    delay(1000)
                }
            }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Duration.ZERO)
```

**Lifecycle**:
- Active during entire ride (always emitting, either ZERO or elapsed time)
- Resets to ZERO when ride ends
- Final pause duration added to cumulative stats when resumed

**Example Usage**:
```kotlin
// In RideStatistics composable
val pausedTime by viewModel.pausedDuration.collectAsStateWithLifecycle()

if (isPaused) {
    Text("Paused: ${pausedTime.toMinutes()}:${pausedTime.seconds % 60}")
}
```

**Performance Considerations**:
- Uses `SharingStarted.WhileSubscribed(5000)` to stop emissions when no subscribers
- `delay(1000)` is non-blocking (suspends coroutine)
- UI collection pauses when app is not in foreground (lifecycle-aware via `collectAsStateWithLifecycle`)

---

### Modified State Properties

#### `mapViewState: StateFlow<MapViewState>`

**Modification**: Add `bearing` field to `MapViewState` data class

**Before**:
```kotlin
data class MapViewState(
    val center: LatLng?,
    val zoom: Float,
    val markers: List<MarkerData>,
    val polyline: PolylineData?,
    val bounds: MapBounds?
)
```

**After**:
```kotlin
data class MapViewState(
    val center: LatLng?,
    val zoom: Float,
    val markers: List<MarkerData>,
    val polyline: PolylineData?,
    val bounds: MapBounds?,
    val bearing: Float? = null // NEW - Map rotation (0-360 degrees), null = north-up
)
```

**Update Logic**:
```kotlin
_mapViewState.update { currentState ->
    currentState.copy(
        bearing = _currentBearing.value // Sync map bearing with GPS bearing
    )
}
```

---

## 2. SettingsViewModel (Modified)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/SettingsViewModel.kt`

### New State Properties

#### `autoPauseThreshold: StateFlow<Int>`

**Purpose**: Exposes current auto-pause timing threshold for UI display and modification

**Type**: `StateFlow<Int>` (seconds)

**Values**:
- One of: `1, 2, 5, 10, 15, 30` (valid options)
- Default: `5` (if no preference set)

**Emission Behavior**:
- Emits immediately on collection with current persisted value (or default)
- Updates whenever user changes setting via `setAutoPauseThreshold()`
- Persists to DataStore Preferences automatically

**Lifecycle**:
- Active for entire app lifetime (settings are global)
- Survives app restarts (persisted in DataStore)

**Example Usage**:
```kotlin
// In AutoPauseSettingsScreen composable
val currentThreshold by viewModel.autoPauseThreshold.collectAsStateWithLifecycle()

val options = listOf(1, 2, 5, 10, 15, 30)
options.forEach { seconds ->
    RadioButton(
        selected = currentThreshold == seconds,
        onClick = { viewModel.setAutoPauseThreshold(seconds) }
    )
}
```

---

### New Methods

#### `setAutoPauseThreshold(seconds: Int)`

**Purpose**: Updates and persists user-selected auto-pause timing threshold

**Signature**:
```kotlin
fun setAutoPauseThreshold(seconds: Int)
```

**Parameters**:
- `seconds: Int` - Auto-pause threshold in seconds (must be one of: 1, 2, 5, 10, 15, 30)

**Behavior**:
- Validates `seconds` is in allowed set `{1, 2, 5, 10, 15, 30}`
- If invalid, logs warning and falls back to default (5)
- Writes to DataStore Preferences via `SettingsRepository.setAutoPauseThreshold()`
- Updates `autoPauseThreshold` StateFlow immediately

**Side Effects**:
- New threshold applies to future rides started after the setting change
- Mid-ride setting changes do NOT retroactively affect current pause detection (FR-014)

**Example Usage**:
```kotlin
// In Settings screen onClick handler
viewModel.setAutoPauseThreshold(10) // Set to 10 seconds
```

**Error Handling**:
```kotlin
fun setAutoPauseThreshold(seconds: Int) {
    if (seconds !in listOf(1, 2, 5, 10, 15, 30)) {
        Log.w(TAG, "Invalid auto-pause threshold: $seconds, using default 5")
        viewModelScope.launch {
            settingsRepository.setAutoPauseThreshold(5)
        }
        return
    }
    viewModelScope.launch {
        settingsRepository.setAutoPauseThreshold(seconds)
    }
}
```

---

## Contract Summary Table

| ViewModel | Property/Method | Type | Purpose | Lifecycle |
|-----------|----------------|------|---------|-----------|
| RideRecordingViewModel | `currentBearing` | `StateFlow<Float?>` | GPS bearing for map/marker rotation | Active during ride |
| RideRecordingViewModel | `pausedDuration` | `StateFlow<Duration>` | Real-time pause counter (updates every 1s) | Active during ride |
| RideRecordingViewModel | `mapViewState` | `StateFlow<MapViewState>` | Modified to include `bearing` field | Active during ride |
| SettingsViewModel | `autoPauseThreshold` | `StateFlow<Int>` | Current auto-pause timing (1-30s options) | Always active |
| SettingsViewModel | `setAutoPauseThreshold(Int)` | Method | Update and persist auto-pause timing | N/A |

---

## Testing Contracts

### RideRecordingViewModel Tests

#### Test: `currentBearing` emits GPS bearing

```kotlin
@Test
fun `currentBearing emits valid bearing from GPS updates`() = runTest {
    // Given: Mock location with bearing 45 degrees (northeast)
    val location = mockLocation(bearing = 45f, hasBearing = true)

    // When: Location update received
    viewModel.onLocationUpdate(location)

    // Then: currentBearing emits 45f
    assertEquals(45f, viewModel.currentBearing.value)
}
```

#### Test: `currentBearing` emits null when bearing unavailable

```kotlin
@Test
fun `currentBearing emits null when GPS bearing unavailable`() = runTest {
    // Given: Mock location without bearing (stationary)
    val location = mockLocation(bearing = 0f, hasBearing = false)

    // When: Location update received
    viewModel.onLocationUpdate(location)

    // Then: currentBearing emits null
    assertNull(viewModel.currentBearing.value)
}
```

#### Test: `pausedDuration` updates every second

```kotlin
@Test
fun `pausedDuration emits updated duration every second while paused`() = runTest {
    // Given: Ride is paused
    viewModel.pauseRide()

    // When: Collecting pausedDuration over 3 seconds
    val emissions = viewModel.pausedDuration
        .take(4) // Initial ZERO + 3 updates
        .toList()

    // Then: Emissions are approximately [0s, 1s, 2s, 3s]
    assertEquals(Duration.ZERO, emissions[0])
    assertTrue(emissions[1].seconds in 0..1) // ~1 second
    assertTrue(emissions[2].seconds in 1..2) // ~2 seconds
    assertTrue(emissions[3].seconds in 2..3) // ~3 seconds
}
```

#### Test: `pausedDuration` resets on resume

```kotlin
@Test
fun `pausedDuration resets to ZERO when ride resumed`() = runTest {
    // Given: Ride is paused for 5 seconds
    viewModel.pauseRide()
    advanceTimeBy(5000)

    // When: Ride is resumed
    viewModel.resumeRide()

    // Then: pausedDuration emits ZERO
    assertEquals(Duration.ZERO, viewModel.pausedDuration.value)
}
```

### SettingsViewModel Tests

#### Test: `autoPauseThreshold` emits persisted value

```kotlin
@Test
fun `autoPauseThreshold emits persisted setting`() = runTest {
    // Given: Setting is persisted as 10 seconds
    settingsRepository.setAutoPauseThreshold(10)

    // When: Collecting autoPauseThreshold
    val threshold = viewModel.autoPauseThreshold.first()

    // Then: Emits 10
    assertEquals(10, threshold)
}
```

#### Test: `setAutoPauseThreshold` persists new value

```kotlin
@Test
fun `setAutoPauseThreshold persists and emits new value`() = runTest {
    // When: Setting threshold to 15 seconds
    viewModel.setAutoPauseThreshold(15)
    advanceUntilIdle()

    // Then: autoPauseThreshold emits 15
    assertEquals(15, viewModel.autoPauseThreshold.value)

    // And: Value is persisted in DataStore
    assertEquals(15, settingsRepository.getAutoPauseThreshold().first())
}
```

#### Test: Invalid threshold falls back to default

```kotlin
@Test
fun `setAutoPauseThreshold rejects invalid value and uses default`() = runTest {
    // When: Setting invalid threshold (42 seconds)
    viewModel.setAutoPauseThreshold(42)
    advanceUntilIdle()

    // Then: Falls back to default (5)
    assertEquals(5, viewModel.autoPauseThreshold.value)
}
```

---

## Integration Points

### RideRecordingViewModel → LiveRideScreen

**Data Flow**:
```
RideRecordingViewModel.currentBearing (StateFlow<Float?>)
    ↓
LiveRideScreen.collectAsStateWithLifecycle()
    ↓
BikeMap(currentBearing = bearing) + LocationMarker(bearing = bearing)
```

**Contract**:
- LiveRideScreen MUST collect `currentBearing` and pass to BikeMap
- BikeMap MUST handle `null` bearing gracefully (north-up fallback)

### RideRecordingViewModel → RideStatistics

**Data Flow**:
```
RideRecordingViewModel.pausedDuration (StateFlow<Duration>)
    ↓
RideStatistics.collectAsStateWithLifecycle()
    ↓
Text("Paused: ${duration.formatted}")
```

**Contract**:
- RideStatistics MUST collect `pausedDuration` using `collectAsStateWithLifecycle` (lifecycle-aware)
- Format duration as "MM:SS" or human-readable string

### SettingsViewModel → AutoPauseSettingsScreen

**Data Flow**:
```
SettingsViewModel.autoPauseThreshold (StateFlow<Int>)
    ↓
AutoPauseSettingsScreen.collectAsStateWithLifecycle()
    ↓
RadioButton(selected = threshold == option)
    ↓
onClick → SettingsViewModel.setAutoPauseThreshold(option)
```

**Contract**:
- Settings screen MUST display all 6 options (1, 2, 5, 10, 15, 30)
- Selected option MUST match `autoPauseThreshold.value`
- Clicking option MUST call `setAutoPauseThreshold` immediately
