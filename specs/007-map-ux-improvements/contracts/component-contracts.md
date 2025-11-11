# Component Contracts: Map UX Improvements (v0.6.1 Patch)

**Feature**: 007-map-ux-improvements
**Date**: 2025-11-10
**Purpose**: Define Compose UI component API contracts for map, marker, statistics, and settings

---

## 1. BikeMap Composable (Modified)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`

### Function Signature

```kotlin
@Composable
fun BikeMap(
    mapViewState: MapViewState, // Modified to include bearing field
    currentLocation: LatLng?,
    currentBearing: Float?, // NEW - For location marker rotation
    onMapClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

### New Parameter

#### `currentBearing: Float?`

**Type**: Nullable `Float` (0-360 degrees or null)

**Purpose**: Bearing for directional location marker rotation

**Values**:
- `0.0f - 360.0f`: Valid GPS bearing (degrees clockwise from north)
- `null`: No bearing available → display non-rotated marker or pin fallback

**Behavior**:
- Passed to `LocationMarker` composable for icon rotation
- Independent of `mapViewState.bearing` (map rotation vs. marker rotation can differ, though typically synced)

### Modified Behavior

**Map Bearing Rotation**:
- When `mapViewState.bearing` changes → animate `CameraPositionState.position.bearing`
- Use `CameraPositionState.animate()` with 300ms duration for smooth rotation
- If `mapViewState.bearing == null` → use bearing = 0 (north-up)

**Implementation Pattern**:
```kotlin
val cameraPositionState = rememberCameraPositionState()

LaunchedEffect(mapViewState.bearing) {
    val targetBearing = mapViewState.bearing ?: 0f
    cameraPositionState.animate(
        CameraUpdateFactory.newCameraPosition(
            cameraPositionState.position.copy(bearing = targetBearing)
        ),
        durationMs = 300
    )
}

GoogleMap(
    cameraPositionState = cameraPositionState,
    modifier = modifier
) {
    if (currentLocation != null) {
        LocationMarker(
            position = currentLocation,
            bearing = currentBearing,
            isMoving = currentBearing != null // Show directional arrow if bearing available
        )
    }
    // ... other markers, polylines ...
}
```

### Performance Considerations

- **Debouncing**: Only animate bearing if `abs(newBearing - oldBearing) > 5 degrees` to avoid jittery rotations
- **Recomposition**: `CameraPositionState` updates do NOT trigger full GoogleMap recomposition (optimized by Maps Compose)
- **Animation Duration**: 300ms is fast enough for responsive feel but slow enough to avoid disorienting jumps

### Testing Contract

**Test**: Map rotates when bearing changes
```kotlin
@Test
fun `map rotates to bearing 90 degrees when mapViewState bearing is 90`() {
    composeTestRule.setContent {
        BikeMap(
            mapViewState = MapViewState(bearing = 90f, ...),
            currentLocation = LatLng(0.0, 0.0),
            currentBearing = 90f
        )
    }

    // Assert: CameraPosition bearing is 90f (requires Maps Compose test utilities)
    // Note: Direct testing of CameraPositionState may require instrumented test
}
```

---

## 2. LocationMarker Composable (Modified)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt`

### Function Signature

```kotlin
@Composable
fun LocationMarker(
    position: LatLng,
    bearing: Float?, // NEW - Rotation angle (0-360 or null)
    isMoving: Boolean, // NEW - true = directional arrow, false = pin
    modifier: Modifier = Modifier
)
```

### New Parameters

#### `bearing: Float?`

**Type**: Nullable `Float` (0-360 degrees or null)

**Purpose**: Rotation angle for directional marker icon

**Values**:
- `0.0f - 360.0f`: Rotate marker to this bearing (0 = north, 90 = east, 180 = south, 270 = west)
- `null`: No rotation (display north-pointing arrow or pin fallback)

**Behavior**:
- Applied via `Modifier.graphicsLayer { rotationZ = bearing ?: 0f }`
- Rotation is clockwise from north (standard GPS bearing convention)

#### `isMoving: Boolean`

**Type**: `Boolean`

**Purpose**: Determines marker icon type

**Values**:
- `true`: Display directional arrow (bike is moving, show heading direction)
- `false`: Display pin icon or static marker (bike is stationary)

**Behavior**:
- If `isMoving == true && bearing != null` → Rotated directional arrow
- If `isMoving == true && bearing == null` → Non-rotated arrow pointing north
- If `isMoving == false` → Pin icon (no rotation applied)

### Rendering Logic

**Directional Arrow (isMoving = true)**:
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
            val arrowPath = Path().apply {
                moveTo(size.width / 2, 0f) // Tip (pointing north before rotation)
                lineTo(size.width * 0.7f, size.height * 0.6f) // Right wing
                lineTo(size.width / 2, size.height * 0.4f) // Neck
                lineTo(size.width * 0.3f, size.height * 0.6f) // Left wing
                close()
            }
            drawPath(arrowPath, color = Color.Blue, style = Fill)
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

**Alternative (Bike Icon)**:
- If arrow is insufficient, replace with bike icon SVG path
- Same rotation logic applies: `graphicsLayer { rotationZ = bearing ?: 0f }`

### Performance Considerations

- **graphicsLayer**: Efficient rotation (hardware-accelerated, does NOT trigger recomposition)
- **Canvas Drawing**: Drawn once per bearing change, not every frame
- **Icon Size**: 32.dp is visible but not obtrusive on map (adjust if needed)

### Testing Contract

**Test**: Marker rotates to bearing
```kotlin
@Test
fun `location marker rotates to 180 degrees when bearing is 180`() {
    composeTestRule.setContent {
        LocationMarker(
            position = LatLng(0.0, 0.0),
            bearing = 180f,
            isMoving = true
        )
    }

    // Assert: Canvas element has graphicsLayer rotation of 180f
    // (Use Compose UI test semantics or screenshot testing)
}
```

**Test**: Marker displays pin when not moving
```kotlin
@Test
fun `location marker displays pin icon when isMoving is false`() {
    composeTestRule.setContent {
        LocationMarker(
            position = LatLng(0.0, 0.0),
            bearing = 90f, // Ignored when not moving
            isMoving = false
        )
    }

    composeTestRule.onNodeWithContentDescription("Current location").assertExists()
}
```

---

## 3. RideStatistics Composable (Modified)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt`

### Function Signature (Modified)

```kotlin
@Composable
fun RideStatistics(
    distance: Double,
    duration: Duration,
    averageSpeed: Float,
    isPaused: Boolean,
    pausedDuration: Duration, // NEW - Real-time updating while paused
    modifier: Modifier = Modifier
)
```

### New Parameter

#### `pausedDuration: Duration`

**Type**: `Duration` (non-nullable, always valid)

**Purpose**: Displays real-time elapsed pause time (updates every ~1 second)

**Values**:
- `Duration.ZERO`: Not currently paused (or pause just ended)
- `Duration.ofSeconds(N)`: Elapsed pause time (N seconds)

**Behavior**:
- Displayed when `isPaused == true`
- Formatted as "MM:SS" or human-readable string (e.g., "2 min 15 sec")
- Updates automatically via StateFlow collection in parent composable

### Rendering Logic

```kotlin
@Composable
fun RideStatistics(
    distance: Double,
    duration: Duration,
    averageSpeed: Float,
    isPaused: Boolean,
    pausedDuration: Duration,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Distance: ${"%.2f".format(distance)} km")
        Text("Duration: ${duration.toMinutes()} min")
        Text("Avg Speed: ${"%.1f".format(averageSpeed)} km/h")

        if (isPaused) {
            // NEW: Real-time pause counter
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

### Performance Considerations

- **Recomposition Scope**: Only the pause counter `Text` recomposes every second (not entire `RideStatistics`)
- **StateFlow Collection**: Use `collectAsStateWithLifecycle()` in parent to pause updates when app backgrounded
- **Formatting Efficiency**: String formatting (`"%02d"`) is lightweight, no performance concern

### Testing Contract

**Test**: Pause counter updates every second
```kotlin
@Test
fun `pause counter displays updated duration when paused`() {
    val pausedDuration = mutableStateOf(Duration.ofSeconds(0))

    composeTestRule.setContent {
        RideStatistics(
            distance = 5.0,
            duration = Duration.ofMinutes(20),
            averageSpeed = 15f,
            isPaused = true,
            pausedDuration = pausedDuration.value
        )
    }

    // Assert: Initial display "Paused: 0:00"
    composeTestRule.onNodeWithText("Paused: 0:00").assertExists()

    // When: Duration updates to 75 seconds
    pausedDuration.value = Duration.ofSeconds(75)
    composeTestRule.waitForIdle()

    // Then: Display updates to "Paused: 1:15"
    composeTestRule.onNodeWithText("Paused: 1:15").assertExists()
}
```

---

## 4. AutoPauseSettingsScreen Composable (New)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/screens/settings/AutoPauseSettingsScreen.kt`

### Function Signature

```kotlin
@Composable
fun AutoPauseSettingsScreen(
    currentThreshold: Int, // Current selection (1, 2, 5, 10, 15, 30)
    onThresholdSelected: (Int) -> Unit, // Callback to SettingsViewModel
    modifier: Modifier = Modifier
)
```

### Parameters

#### `currentThreshold: Int`

**Type**: `Int` (seconds)

**Purpose**: Currently selected auto-pause timing threshold

**Values**: One of `{1, 2, 5, 10, 15, 30}` seconds

**Behavior**: Determines which RadioButton is selected in the UI

#### `onThresholdSelected: (Int) -> Unit`

**Type**: Function callback

**Purpose**: Invoked when user selects a new threshold option

**Behavior**:
- Called with selected threshold value (1, 2, 5, 10, 15, or 30)
- Parent screen/ViewModel handles persistence via `SettingsViewModel.setAutoPauseThreshold()`

### Rendering Logic

```kotlin
@Composable
fun AutoPauseSettingsScreen(
    currentThreshold: Int,
    onThresholdSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        1 to "1 second",
        2 to "2 seconds",
        5 to "5 seconds",
        10 to "10 seconds",
        15 to "15 seconds",
        30 to "30 seconds"
    )

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Auto-Pause Timing",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Automatically pause ride when speed is below threshold for this duration:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        options.forEach { (seconds, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThresholdSelected(seconds) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentThreshold == seconds,
                    onClick = { onThresholdSelected(seconds) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label)
            }
        }
    }
}
```

### Testing Contract

**Test**: Displays all 6 options
```kotlin
@Test
fun `settings screen displays all 6 auto-pause timing options`() {
    composeTestRule.setContent {
        AutoPauseSettingsScreen(
            currentThreshold = 5,
            onThresholdSelected = {}
        )
    }

    composeTestRule.onNodeWithText("1 second").assertExists()
    composeTestRule.onNodeWithText("2 seconds").assertExists()
    composeTestRule.onNodeWithText("5 seconds").assertExists()
    composeTestRule.onNodeWithText("10 seconds").assertExists()
    composeTestRule.onNodeWithText("15 seconds").assertExists()
    composeTestRule.onNodeWithText("30 seconds").assertExists()
}
```

**Test**: Selecting option invokes callback
```kotlin
@Test
fun `clicking option invokes onThresholdSelected callback`() {
    val selectedThreshold = mutableStateOf<Int?>(null)

    composeTestRule.setContent {
        AutoPauseSettingsScreen(
            currentThreshold = 5,
            onThresholdSelected = { selectedThreshold.value = it }
        )
    }

    // When: Click "10 seconds" option
    composeTestRule.onNodeWithText("10 seconds").performClick()

    // Then: Callback invoked with 10
    assertEquals(10, selectedThreshold.value)
}
```

---

## Component Integration Summary

| Component | Modified/New | Key Changes | Integration Point |
|-----------|-------------|-------------|-------------------|
| BikeMap | Modified | Add `currentBearing` parameter, handle `mapViewState.bearing` rotation | LiveRideScreen, RideDetailScreen |
| LocationMarker | Modified | Add `bearing` and `isMoving` parameters, directional icon rendering | BikeMap composable |
| RideStatistics | Modified | Add `pausedDuration` parameter, real-time counter display | LiveRideScreen |
| AutoPauseSettingsScreen | New | Radio button list for 6 timing options | SettingsScreen navigation |

---

## Accessibility Considerations

**BikeMap**:
- No changes to accessibility - map rotation is visual-only
- Existing map accessibility features (zoom, pan) unaffected

**LocationMarker**:
- Content description should indicate direction if bearing is available
- Example: `"Current location, heading northeast"` vs. `"Current location"`

**RideStatistics**:
- Pause counter should have semantic label: `"Paused for {duration}"`
- Screen reader announces updates when counter increments

**AutoPauseSettingsScreen**:
- RadioButtons have standard accessibility support (selected state announced)
- Each option has clear text label (no icon-only buttons)

---

## Performance Benchmarks

| Component | Operation | Target Performance | Measurement Method |
|-----------|-----------|-------------------|-------------------|
| BikeMap | Bearing rotation animation | 60 fps, <300ms duration | Frame time profiler |
| LocationMarker | Icon rotation | <16ms recomposition | Compose layout inspector |
| RideStatistics | Counter update | <16ms recomposition (local scope) | Recomposition counts |
| AutoPauseSettingsScreen | Option selection | Instant (<50ms callback) | Touch latency |
