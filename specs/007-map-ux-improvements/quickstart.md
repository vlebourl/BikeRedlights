# Quickstart Guide: Map UX Improvements (v0.6.1 Patch)

**Feature**: 007-map-ux-improvements
**Date**: 2025-11-10
**Purpose**: Step-by-step implementation guide for developers

---

## Prerequisites

- [ ] Branch `007-map-ux-improvements` checked out
- [ ] Latest `main` merged into feature branch
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Emulator/device with GPS capabilities available for testing

---

## Implementation Order

**Recommended sequence** (most independent to most dependent):

1. **Settings** (P3) - Auto-pause timing options (fully independent)
2. **Pause Counter** (P2) - Real-time updates (independent of map changes)
3. **Map Bearing** (P1) - Map rotation (depends on bearing data flow)
4. **Location Marker** (P1) - Directional marker (depends on bearing data flow + map setup)

**Rationale**: Settings and pause counter can be implemented and tested independently. Map features share common bearing data flow, so implement together.

---

## Phase 1: Auto-Pause Timing Settings (P3)

**User Story**: Granular Auto-Pause Timing Options

### 1.1 Add DataStore Key

**File**: `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepository.kt`

```kotlin
// Add private key
private val AUTO_PAUSE_THRESHOLD_KEY = intPreferencesKey("auto_pause_threshold_seconds")

// Add accessor methods
fun getAutoPauseThreshold(): Flow<Int> = dataStore.data
    .map { preferences -> preferences[AUTO_PAUSE_THRESHOLD_KEY] ?: 5 } // Default 5s

suspend fun setAutoPauseThreshold(seconds: Int) {
    require(seconds in listOf(1, 2, 5, 10, 15, 30)) {
        "Invalid auto-pause threshold: $seconds"
    }
    dataStore.edit { preferences ->
        preferences[AUTO_PAUSE_THRESHOLD_KEY] = seconds
    }
}
```

**Test**:
```kotlin
// In SettingsRepositoryTest.kt
@Test
fun `getAutoPauseThreshold returns default 5 when not set`() = runTest {
    val threshold = settingsRepository.getAutoPauseThreshold().first()
    assertEquals(5, threshold)
}

@Test
fun `setAutoPauseThreshold persists value`() = runTest {
    settingsRepository.setAutoPauseThreshold(10)
    val threshold = settingsRepository.getAutoPauseThreshold().first()
    assertEquals(10, threshold)
}
```

### 1.2 Update SettingsViewModel

**File**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/SettingsViewModel.kt`

```kotlin
val autoPauseThreshold: StateFlow<Int> = settingsRepository
    .getAutoPauseThreshold()
    .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

fun setAutoPauseThreshold(seconds: Int) {
    viewModelScope.launch {
        try {
            settingsRepository.setAutoPauseThreshold(seconds)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid auto-pause threshold: $seconds, using default 5")
            settingsRepository.setAutoPauseThreshold(5)
        }
    }
}
```

**Test**:
```kotlin
// In SettingsViewModelTest.kt
@Test
fun `autoPauseThreshold emits persisted value`() = runTest {
    settingsRepository.setAutoPauseThreshold(15)
    val viewModel = SettingsViewModel(settingsRepository)

    assertEquals(15, viewModel.autoPauseThreshold.value)
}

@Test
fun `setAutoPauseThreshold updates StateFlow`() = runTest {
    val viewModel = SettingsViewModel(settingsRepository)
    viewModel.setAutoPauseThreshold(30)
    advanceUntilIdle()

    assertEquals(30, viewModel.autoPauseThreshold.value)
}
```

### 1.3 Create AutoPauseSettingsScreen

**File**: `app/src/main/java/com/example/bikeredlights/ui/screens/settings/AutoPauseSettingsScreen.kt`

```kotlin
@Composable
fun AutoPauseSettingsScreen(
    currentThreshold: Int,
    onThresholdSelected: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember {
        listOf(
            1 to "1 second",
            2 to "2 seconds",
            5 to "5 seconds",
            10 to "10 seconds",
            15 to "15 seconds",
            30 to "30 seconds"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Pause Timing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Pause when stationary for:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            options.forEach { (seconds, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThresholdSelected(seconds) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentThreshold == seconds,
                        onClick = { onThresholdSelected(seconds) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = label)
                }
            }
        }
    }
}
```

**Test**: See `contracts/component-contracts.md` for Compose UI tests

### 1.4 Add Navigation Route

**File**: Navigation graph (e.g., `SettingsNavigation.kt` or `NavGraph.kt`)

```kotlin
composable("auto_pause_settings") {
    val viewModel: SettingsViewModel = hiltViewModel()
    val currentThreshold by viewModel.autoPauseThreshold.collectAsStateWithLifecycle()

    AutoPauseSettingsScreen(
        currentThreshold = currentThreshold,
        onThresholdSelected = viewModel::setAutoPauseThreshold,
        onNavigateBack = { navController.navigateUp() }
    )
}
```

**Integration**: Add navigation call from SettingsHomeScreen

### 1.5 Manual Testing

1. Navigate to Settings > Auto-Pause Timing
2. Select each option (1s, 2s, 5s, 10s, 15s, 30s)
3. Back out and re-enter → verify selection persists
4. Start a ride, remain stationary → verify auto-pause triggers at selected threshold

---

## Phase 2: Real-Time Pause Counter (P2)

**User Story**: Real-Time Pause Counter

### 2.1 Add pausedDuration StateFlow to RideRecordingViewModel

**File**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`

```kotlin
// Add private state
private val _pauseStartTime = MutableStateFlow<Instant?>(null)

// Add public StateFlow
val pausedDuration: StateFlow<Duration> = _pauseStartTime
    .flatMapLatest { startTime ->
        if (startTime == null) {
            flowOf(Duration.ZERO)
        } else {
            flow {
                while (true) {
                    val elapsed = Duration.between(startTime, Instant.now())
                    emit(elapsed)
                    delay(1000) // Update every second
                }
            }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Duration.ZERO)

// Update pause/resume methods
fun pauseRide() {
    _isPaused.value = true
    _pauseStartTime.value = Instant.now() // Start timer
}

fun resumeRide() {
    val pausedTime = _pauseStartTime.value?.let {
        Duration.between(it, Instant.now())
    } ?: Duration.ZERO

    // Add to cumulative paused time
    _totalPausedDuration.value += pausedTime

    _isPaused.value = false
    _pauseStartTime.value = null // Stop timer
}
```

**Test**:
```kotlin
// In RideRecordingViewModelTest.kt
@Test
fun `pausedDuration emits updated duration every second`() = runTest {
    viewModel.startRide()
    viewModel.pauseRide()

    val emissions = viewModel.pausedDuration
        .take(4) // 0s, ~1s, ~2s, ~3s
        .toList()

    assertEquals(Duration.ZERO, emissions[0])
    assertTrue(emissions[1].seconds in 0..1)
    assertTrue(emissions[2].seconds in 1..2)
    assertTrue(emissions[3].seconds in 2..3)
}

@Test
fun `pausedDuration resets to ZERO when resumed`() = runTest {
    viewModel.startRide()
    viewModel.pauseRide()
    advanceTimeBy(5000)
    viewModel.resumeRide()

    assertEquals(Duration.ZERO, viewModel.pausedDuration.value)
}
```

### 2.2 Update RideStatistics Composable

**File**: `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt`

```kotlin
@Composable
fun RideStatistics(
    distance: Double,
    duration: Duration,
    averageSpeed: Float,
    isPaused: Boolean,
    pausedDuration: Duration, // NEW parameter
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // Existing stats...
        Text("Distance: ${"%.2f".format(distance)} km")
        Text("Duration: ${duration.toMinutes()} min")
        Text("Avg Speed: ${"%.1f".format(averageSpeed)} km/h")

        // NEW: Real-time pause counter
        if (isPaused) {
            val minutes = pausedDuration.toMinutes()
            val seconds = pausedDuration.seconds % 60
            Text(
                text = "Paused: $minutes:${"%02d".format(seconds)}",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

### 2.3 Update LiveRideScreen

**File**: `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt`

```kotlin
@Composable
fun LiveRideScreen(
    viewModel: RideRecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pausedDuration by viewModel.pausedDuration.collectAsStateWithLifecycle() // NEW

    // Pass to RideStatistics
    RideStatistics(
        distance = uiState.distance,
        duration = uiState.duration,
        averageSpeed = uiState.averageSpeed,
        isPaused = uiState.isPaused,
        pausedDuration = pausedDuration // NEW
    )
}
```

### 2.4 Manual Testing

1. Start a ride on emulator/device
2. Trigger auto-pause (remain stationary for threshold duration)
3. Observe pause counter incrementing every second: "Paused: 0:01", "Paused: 0:02", etc.
4. Resume riding → verify counter resets to "Paused: 0:00" or disappears
5. Lock device, wait 10 seconds, unlock → verify counter shows accurate elapsed time

---

## Phase 3: Map Bearing Rotation (P1)

**User Story**: Directional Map Orientation

### 3.1 Add bearing field to MapViewState

**File**: `app/src/main/java/com/example/bikeredlights/domain/model/MapViewState.kt`

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

### 3.2 Add currentBearing StateFlow to RideRecordingViewModel

**File**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`

```kotlin
// Add private state
private val _currentBearing = MutableStateFlow<Float?>(null)

// Add public StateFlow
val currentBearing: StateFlow<Float?> = _currentBearing.asStateFlow()

// Update location callback (in service integration)
fun onLocationUpdate(location: Location) {
    // Existing location handling...

    // NEW: Extract bearing if available
    if (location.hasBearing()) {
        _currentBearing.value = location.bearing // 0-360 degrees
    } else {
        // Optional: Retain last known bearing or set null after timeout
        // For now, set null immediately when bearing unavailable
        _currentBearing.value = null
    }

    // Update map state with bearing
    _mapViewState.update { currentState ->
        currentState.copy(
            bearing = _currentBearing.value,
            center = LatLng(location.latitude, location.longitude)
        )
    }
}
```

**Test**:
```kotlin
@Test
fun `currentBearing emits GPS bearing when available`() = runTest {
    val location = mockk<Location>().apply {
        every { hasBearing() } returns true
        every { bearing } returns 45f
    }

    viewModel.onLocationUpdate(location)

    assertEquals(45f, viewModel.currentBearing.value)
}

@Test
fun `currentBearing emits null when bearing unavailable`() = runTest {
    val location = mockk<Location>().apply {
        every { hasBearing() } returns false
    }

    viewModel.onLocationUpdate(location)

    assertNull(viewModel.currentBearing.value)
}
```

### 3.3 Update BikeMap to handle bearing rotation

**File**: `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`

```kotlin
@Composable
fun BikeMap(
    mapViewState: MapViewState,
    currentLocation: LatLng?,
    currentBearing: Float?, // NEW parameter
    onMapClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(
            target = mapViewState.center ?: LatLng(0.0, 0.0),
            zoom = mapViewState.zoom,
            bearing = mapViewState.bearing ?: 0f, // NEW: Initial bearing
            tilt = 0f
        )
    }

    // NEW: Animate bearing changes
    LaunchedEffect(mapViewState.bearing) {
        val targetBearing = mapViewState.bearing ?: 0f
        val currentBearingValue = cameraPositionState.position.bearing

        // Debounce: Only animate if delta > 5 degrees
        if (abs(targetBearing - currentBearingValue) > 5f) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    cameraPositionState.position.copy(bearing = targetBearing)
                ),
                durationMs = 300
            )
        }
    }

    GoogleMap(
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = false),
        modifier = modifier
    ) {
        // Existing markers, polyline...

        // Location marker will be updated next
    }
}
```

### 3.4 Update LiveRideScreen to pass bearing

**File**: `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt`

```kotlin
val currentBearing by viewModel.currentBearing.collectAsStateWithLifecycle() // NEW

BikeMap(
    mapViewState = mapViewState,
    currentLocation = currentLocation,
    currentBearing = currentBearing // NEW
)
```

### 3.5 Manual Testing

1. Start a ride with GPS enabled
2. Move in a specific direction (e.g., walk/bike north)
3. Observe map rotating so your direction points upward
4. Turn 90 degrees (e.g., now heading east)
5. Verify map smoothly rotates to keep your direction pointing up
6. Stand still → verify map retains last bearing or reverts to north-up

---

## Phase 4: Directional Location Marker (P1)

**User Story**: Directional Location Marker

### 4.1 Create LocationMarker composable with rotation

**File**: `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt`

```kotlin
@Composable
fun LocationMarker(
    position: LatLng,
    bearing: Float?,
    isMoving: Boolean,
    modifier: Modifier = Modifier
) {
    if (isMoving) {
        // Directional arrow
        Canvas(
            modifier = modifier
                .size(32.dp)
                .graphicsLayer { rotationZ = bearing ?: 0f }
        ) {
            val arrowColor = Color.Blue
            val path = Path().apply {
                // Arrow pointing north (before rotation)
                moveTo(size.width / 2, 0f) // Tip
                lineTo(size.width * 0.7f, size.height * 0.6f) // Right
                lineTo(size.width / 2, size.height * 0.4f) // Neck
                lineTo(size.width * 0.3f, size.height * 0.6f) // Left
                close()
            }
            drawPath(path, color = arrowColor, style = Fill)

            // Optional: Add outline for visibility
            drawPath(path, color = Color.White, style = Stroke(width = 2.dp.toPx()))
        }
    } else {
        // Pin icon (stationary)
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = "Current location",
            tint = Color.Red,
            modifier = modifier.size(32.dp)
        )
    }
}
```

### 4.2 Integrate LocationMarker into BikeMap

**File**: `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`

```kotlin
GoogleMap(
    cameraPositionState = cameraPositionState,
    uiSettings = MapUiSettings(zoomControlsEnabled = false),
    modifier = modifier
) {
    // Existing polyline, markers...

    // NEW: Directional location marker
    if (currentLocation != null) {
        MarkerInfoWindowContent(
            state = rememberMarkerState(position = currentLocation)
        ) {
            LocationMarker(
                position = currentLocation,
                bearing = currentBearing,
                isMoving = currentBearing != null // Show arrow if bearing available
            )
        }
    }
}
```

**Alternative (Overlay approach)**:
If using MarkerInfoWindowContent is problematic, overlay LocationMarker directly:

```kotlin
Box(modifier = modifier) {
    GoogleMap(...) { /* map content */ }

    // Overlay location marker
    if (currentLocation != null) {
        // Calculate screen position from LatLng (requires projection utilities)
        // For simplicity, use marker overlay or wait for Maps Compose updates
    }
}
```

**Note**: Google Maps Compose custom markers can be tricky. If MarkerInfoWindowContent doesn't support rotation, consider filing upstream issue or using Marker with custom icon bitmap.

### 4.3 Manual Testing

1. Start a ride with GPS enabled
2. Move in a specific direction
3. Observe blue arrow marker pointing in your direction of travel
4. Turn 90 degrees → verify arrow rotates to match new heading
5. Stand still → verify marker switches to red pin or static arrow
6. Resume movement → verify marker returns to rotated arrow

---

## Integration Testing

### End-to-End Test Scenario

1. **Settings**: Change auto-pause timing to 2 seconds, verify persistence
2. **Start Ride**: Begin ride with GPS enabled
3. **Map Bearing**: Move north, verify map rotates to north-up; turn east, verify map rotates
4. **Location Marker**: Verify arrow points in direction of travel, rotates with turns
5. **Pause Counter**: Remain stationary for 2 seconds, verify auto-pause triggers and counter updates every second (0:01, 0:02, 0:03...)
6. **Resume**: Start moving, verify pause counter resets and map bearing resumes updating
7. **Settings Change**: Mid-ride, change auto-pause to 10 seconds, verify new threshold applies to future pauses (not current pause state)

---

## Emulator GPS Simulation

### Setup Extended Controls

1. Open emulator
2. Click "..." (Extended Controls)
3. Navigate to "Location" tab

### Bearing Simulation

**Option 1: GPX Route**:
- Create GPX file with waypoints in sequence (e.g., north to east turn)
- Load GPX file in emulator
- Play route at cycling speed (10-20 km/h)
- Maps Compose will receive bearing from route direction

**Option 2: Manual Location Points**:
```bash
# Send location updates with bearing via ADB
adb emu geo fix -122.084 37.422 100 45 # lon, lat, alt, bearing(degrees)
# Bearing: 0=north, 90=east, 180=south, 270=west
```

**Option 3: Use Emulator GUI**:
- In Location tab, select "Route"
- Set multiple points forming a path
- Emulator calculates bearing between points
- Adjust playback speed to simulate cycling

---

## Commit Strategy

**Recommended commits** (following CLAUDE.md <200 LOC guideline):

1. `feat(settings): add auto-pause timing options to DataStore`
   - SettingsRepository changes
   - SettingsViewModel changes
2. `feat(ui): create AutoPauseSettingsScreen with 6 timing options`
   - AutoPauseSettingsScreen composable
   - Navigation integration
3. `test(settings): add unit tests for auto-pause timing settings`
   - SettingsRepository tests
   - SettingsViewModel tests
4. `feat(viewmodel): add real-time pause counter StateFlow`
   - RideRecordingViewModel pausedDuration flow
5. `feat(ui): update RideStatistics with real-time pause counter`
   - RideStatistics composable changes
   - LiveRideScreen integration
6. `test(viewmodel): add tests for real-time pause counter flow`
   - RideRecordingViewModel tests
7. `feat(domain): add bearing field to MapViewState`
   - MapViewState data class update
8. `feat(viewmodel): expose currentBearing StateFlow from GPS updates`
   - RideRecordingViewModel bearing tracking
9. `feat(ui): implement map bearing rotation in BikeMap`
   - BikeMap composable bearing animation
10. `feat(ui): create directional LocationMarker with rotation`
    - LocationMarker composable with Canvas drawing
    - BikeMap integration
11. `test(ui): add Compose UI tests for map and marker rotation`
    - BikeMap tests
    - LocationMarker tests
12. `docs: update TODO.md and RELEASE.md for v0.6.1 patch`
    - Documentation updates

**Total**: 12 commits, each focused on single component/feature

---

## Troubleshooting

### Issue: Map bearing rotation is jittery

**Solution**: Increase debounce threshold to 10 degrees instead of 5, or add exponential smoothing to bearing updates

### Issue: Pause counter stops updating when app backgrounded

**Solution**: Verify using `collectAsStateWithLifecycle()` in composable, not `collectAsState()`. Lifecycle-aware collection pauses UI updates but calculation continues (based on wall-clock time).

### Issue: Location marker doesn't rotate

**Solution**: Check `graphicsLayer` is applied correctly. Ensure `bearing` parameter is not null. Use Layout Inspector to verify rotation transformation.

### Issue: GPS bearing unavailable on emulator

**Solution**: Use GPX route playback or manual ADB commands with bearing parameter. Emulator's single-point location doesn't provide bearing.

### Issue: Settings don't persist after app restart

**Solution**: Verify DataStore Preferences file is not corrupted. Check `settingsRepository.setAutoPauseThreshold()` completes successfully (no exceptions).

---

## Performance Validation

### Metrics to Monitor

- **Map Rotation**: Frame time during bearing animation (target: <16ms, 60fps)
- **Marker Rotation**: Recomposition count (target: marker scope only, not full map)
- **Pause Counter**: UI update frequency (target: ~1 second intervals, no dropped frames)
- **Settings Persistence**: DataStore write latency (target: <50ms)

### Tools

- Android Profiler → CPU profiler (measure frame times)
- Compose Layout Inspector → Recomposition counts
- Logcat → Search for "Recomposition" or add custom logs

---

## Definition of Done

- [ ] All 4 user stories implemented and tested
- [ ] Unit tests passing (RideRecordingViewModel, SettingsViewModel, repositories)
- [ ] Compose UI tests passing (BikeMap, LocationMarker, RideStatistics, AutoPauseSettingsScreen)
- [ ] Emulator testing completed with GPS simulation (bearing rotation, pause counter, settings)
- [ ] No lint warnings or errors
- [ ] Code committed in small, logical chunks (<200 LOC per commit)
- [ ] TODO.md updated with feature status
- [ ] RELEASE.md updated with v0.6.1 patch notes
- [ ] Ready for pull request creation

---

## Next Steps

After implementation complete:

1. Run `/speckit.tasks` to generate detailed task breakdown for implementation
2. Execute tasks in sequence, testing after each task
3. Create pull request with emulator test results
4. After merge, tag release v0.6.1 and build signed APK
