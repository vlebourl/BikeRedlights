# Research: Map UX Improvements (v0.6.1 Patch)

**Feature**: 007-map-ux-improvements
**Date**: 2025-11-10
**Purpose**: Investigate technical approaches for map bearing rotation, directional markers, real-time pause counter updates, and settings configuration

## Research Areas

### 1. Google Maps Compose Bearing/Rotation API

**Decision**: Use `CameraPositionState.position.bearing` property with `CameraPosition` updates to rotate map orientation

**Rationale**:
- Maps Compose 4.4.1 provides built-in bearing support via `CameraPosition` data class
- `CameraPositionState` is already used in existing BikeMap.kt for camera control
- Bearing is a Float (0-360 degrees) representing map rotation clockwise from north
- Smooth animation achieved via `animate()` method on `CameraPositionState`
- No custom drawing/transformation required - handled natively by Google Maps SDK

**Implementation Pattern**:
```kotlin
val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition(
        target = LatLng(...),
        zoom = 15f,
        bearing = bearingDegrees, // NEW - 0-360 rotation
        tilt = 0f
    )
}

// Update bearing when heading changes
LaunchedEffect(currentBearing) {
    cameraPositionState.animate(
        CameraUpdateFactory.newCameraPosition(
            cameraPositionState.position.copy(bearing = currentBearing)
        ),
        durationMs = 300 // Smooth 300ms rotation
    )
}
```

**Alternatives Considered**:
- Manual map rotation via Canvas transformations: Rejected - more complex, not native Google Maps support, worse performance
- Third-party map libraries (Mapbox, OpenStreetMap): Rejected - requires dependency change, migration cost, existing Maps SDK is sufficient

**Bearing Data Source**:
- `Location.bearing` from Fused Location Provider API (already in use via RideRecordingService)
- Bearing represents direction of travel (0-360 degrees from north)
- May be unavailable when stationary (Location.hasBearing() returns false)
- Fallback strategy: retain last known bearing or default to 0 (north-up)

**Performance Considerations**:
- Debounce bearing updates to avoid excessive camera animations (e.g., only update if delta > 5 degrees)
- Use `animate()` instead of instant position updates to avoid jarring rotations
- Monitor recomposition frequency - bearing should not cause full map recomposition

---

### 2. Directional Location Marker with Rotation

**Decision**: Custom `Canvas`-based composable for arrow/bike icon with rotation transformation using Compose `graphicsLayer`

**Rationale**:
- Compose `Canvas` API allows custom drawing of directional shapes (arrows, triangles)
- `Modifier.graphicsLayer { rotationZ = bearing }` provides efficient rotation without triggering recomposition
- Can be overlaid on map using `OverlayComposable` or direct Compose stacking
- Avoids need for multiple pre-rotated bitmap assets (one dynamic asset, rotated in code)
- Consistent with Compose-first approach (no XML vector drawables with programmatic rotation)

**Implementation Pattern**:
```kotlin
@Composable
fun DirectionalMarker(
    bearing: Float?, // Nullable - may not have bearing when stationary
    modifier: Modifier = Modifier
) {
    val rotation = bearing ?: 0f // Default to north if no bearing

    Canvas(
        modifier = modifier
            .size(32.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        // Draw arrow pointing up (north)
        val path = Path().apply {
            moveTo(size.width / 2, 0f) // Tip
            lineTo(size.width * 0.7f, size.height * 0.6f) // Right base
            lineTo(size.width / 2, size.height * 0.4f) // Neck
            lineTo(size.width * 0.3f, size.height * 0.6f) // Left base
            close()
        }
        drawPath(path, color = Color.Blue, style = Fill)
    }
}
```

**Alternatives Considered**:
- Vector drawable with programmatic rotation: Rejected - less Compose-idiomatic, requires XML asset
- Bitmap rotation via Matrix: Rejected - more complex, worse performance than graphicsLayer
- Google Maps native marker rotation: Rejected - Maps Compose marker rotation is less flexible for custom shapes
- Use bike icon instead of arrow: DEFERRED to implementation - arrow is simpler, bike icon can be substituted if desired

**Fallback Strategy**:
- When `bearing == null` (no GPS bearing available), display non-rotated marker or fallback to pin icon
- User Story 2 acceptance scenario 3: "Given I am stationary, When bearing data is unavailable, Then the marker displays without directional indication"

**Icon Choice (Arrow vs. Bike)**:
- **Recommendation**: Start with simple arrow (triangle/chevron shape)
- Bike icon option can be added later if arrow is insufficient
- Arrow advantages: universally understood, simple to draw, clear directionality
- Bike icon advantages: thematic consistency, potentially more recognizable at small sizes
- **Decision**: Implement arrow first, evaluate during testing, swap to bike icon if needed

---

### 3. Real-Time Pause Counter Updates

**Decision**: Use `Flow<Long>` emitting elapsed pause time every second, collected via `collectAsStateWithLifecycle` in UI

**Rationale**:
- Kotlin Flow is the standard reactive primitive in modern Android/Compose
- `flow { ... }` builder with `while(true)` loop + `delay(1000)` emits updates every second
- `collectAsStateWithLifecycle` ensures counter stops updating when app is backgrounded (lifecycle-aware)
- StateFlow in ViewModel ensures single source of truth for pause state
- No manual timer management (Handler/Timer) - Flow handles lifecycle and cancellation automatically

**Implementation Pattern**:
```kotlin
// In RideRecordingViewModel
private val _pauseStartTime = MutableStateFlow<Long?>(null)

val pausedDuration: StateFlow<Duration> = _pauseStartTime
    .flatMapLatest { startTime ->
        if (startTime == null) {
            flowOf(Duration.ZERO)
        } else {
            flow {
                while (true) {
                    val elapsed = Duration.between(
                        Instant.ofEpochMilli(startTime),
                        Instant.now()
                    )
                    emit(elapsed)
                    delay(1000) // Update every second
                }
            }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Duration.ZERO)

// In UI composable
val pausedTime by viewModel.pausedDuration.collectAsStateWithLifecycle()
Text("Paused: ${pausedTime.toMinutes()}:${pausedTime.seconds % 60}")
```

**Alternatives Considered**:
- `Handler.postDelayed()`: Rejected - requires manual lifecycle management, not Compose-idiomatic
- `Timer` or `ScheduledExecutorService`: Rejected - heavyweight, manual cancellation needed
- `LaunchedEffect` with `while(true)`: Rejected - ties timer to composable lifecycle, should be in ViewModel
- Update only on resume: Rejected - violates FR-008 (real-time updates required)

**Background/Lock Screen Handling**:
- `collectAsStateWithLifecycle()` pauses collection when app is not in foreground (Lifecycle.State.STARTED)
- Counter calculation based on wall-clock time (`Instant.now()`) ensures accuracy when returning to app
- When user unlocks phone, Flow resumes and immediately emits current elapsed time
- Satisfies FR-009: "Paused time counter MUST continue updating accurately even when the app is backgrounded"

**Performance Considerations**:
- `delay(1000)` is non-blocking (suspends coroutine, doesn't block thread)
- StateFlow ensures only one active collector (UI doesn't create duplicate timers)
- `SharingStarted.WhileSubscribed(5000)` stops Flow 5 seconds after last subscriber (battery-efficient)

---

### 4. Auto-Pause Timing Settings (DataStore Preferences)

**Decision**: Add new `AUTO_PAUSE_THRESHOLD_SECONDS` key to existing DataStore Preferences with Int value, default 5 seconds

**Rationale**:
- DataStore Preferences is already in use for settings (established in Feature 001-settings-infrastructure per CLAUDE.md Active Technologies)
- Int value (1, 2, 5, 10, 15, 30) stored directly as seconds - simple, type-safe
- No schema migration needed (new key, not modifying existing)
- SettingsRepository pattern already established - add new accessor method
- UI uses existing preferences infrastructure (no new storage layer)

**Implementation Pattern**:
```kotlin
// In SettingsRepository
private val AUTO_PAUSE_THRESHOLD_KEY = intPreferencesKey("auto_pause_threshold_seconds")

fun getAutoPauseThreshold(): Flow<Int> = dataStore.data
    .map { preferences -> preferences[AUTO_PAUSE_THRESHOLD_KEY] ?: 5 } // Default 5s

suspend fun setAutoPauseThreshold(seconds: Int) {
    dataStore.edit { preferences ->
        preferences[AUTO_PAUSE_THRESHOLD_KEY] = seconds
    }
}

// In SettingsViewModel
val autoPauseOptions = listOf(1, 2, 5, 10, 15, 30) // seconds
val selectedThreshold: StateFlow<Int> = settingsRepository
    .getAutoPauseThreshold()
    .stateIn(viewModelScope, SharingStarted.Eagerly, 5)
```

**UI Pattern** (Settings Screen):
```kotlin
// RadioButton group or Dropdown for 6 options
val options = listOf(
    1 to "1 second",
    2 to "2 seconds",
    5 to "5 seconds",
    10 to "10 seconds",
    15 to "15 seconds",
    30 to "30 seconds"
)

options.forEach { (seconds, label) ->
    Row(
        modifier = Modifier.clickable { viewModel.setAutoPauseThreshold(seconds) }
    ) {
        RadioButton(
            selected = selectedThreshold == seconds,
            onClick = { viewModel.setAutoPauseThreshold(seconds) }
        )
        Text(label)
    }
}
```

**Alternatives Considered**:
- Store as enum: Rejected - Int is simpler, directly usable in threshold comparisons
- Store as milliseconds: Rejected - seconds are more human-readable, avoids large numbers
- Use SharedPreferences: Rejected - CLAUDE.md explicitly forbids SharedPreferences (use DataStore)
- Store as Duration string: Rejected - unnecessary complexity, Int is sufficient

**Migration Strategy**:
- No migration needed - new key with sensible default (5 seconds)
- Existing users will see 5s default on first launch of v0.6.1
- Users can immediately change to preferred value
- No risk of breaking existing settings

---

## Technology Stack Summary

| Component | Technology | Justification |
|-----------|-----------|---------------|
| Map Bearing Rotation | Google Maps Compose `CameraPosition.bearing` | Native SDK support, smooth animations, already integrated |
| Directional Marker | Compose `Canvas` + `graphicsLayer` rotation | Compose-idiomatic, efficient, dynamic rotation without assets |
| Pause Counter | Kotlin Flow + `collectAsStateWithLifecycle` | Reactive, lifecycle-aware, standard Android approach |
| Settings Storage | DataStore Preferences (Int key) | Established pattern, type-safe, no migration needed |
| UI Framework | Jetpack Compose (existing BOM 2024.11.00) | Consistent with v0.6.0, no new dependencies |
| Testing | JUnit 5 + Turbine (Flow testing) | Standard testing stack, Turbine for Flow assertions |

**New Dependencies**: None - all technologies are already in use in v0.6.0

**Removed Dependencies**: None

---

## Open Questions / Decisions Deferred to Implementation

1. **Arrow vs. Bike Icon**: Start with arrow (simpler), evaluate during testing, can swap to bike icon if arrow is insufficient for user clarity
2. **Bearing Debounce Threshold**: Likely 5-10 degrees delta to trigger map rotation update - tune during emulator testing to balance smoothness vs. performance
3. **Pause Counter Format**: Display as "MM:SS" or "X minutes Y seconds"? - Defer to UI implementation, both are acceptable per spec
4. **Bearing Staleness Timeout**: How long to retain last known bearing before reverting to north-up? - Defer to implementation (suggest 30-60 seconds)
5. **Marker Fallback Icon**: When no bearing, display pin or static (non-rotated) arrow? - Defer to implementation, either is acceptable per FR-006

**Note**: All open questions are implementation details that do not block task generation. They can be resolved during development with quick testing iterations.
