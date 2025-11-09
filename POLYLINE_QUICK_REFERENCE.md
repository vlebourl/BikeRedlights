# Polyline Rendering - Quick Reference Guide

**For**: Implementing route drawing in BikeRedlights route visualization feature
**Last Updated**: November 2025

---

## One-Page Summary

### What Problem Does This Solve?

BikeRedlights needs to display bike routes on a map with 3600+ GPS track points collected during rides. Rendering raw points causes:
- **Jank**: Frame rate drops from 60fps to 15-30fps
- **Memory spikes**: 50-100MB additional consumption
- **ANR events**: App Not Responding warnings

### The Solution: 3-Part Approach

1. **Simplify polylines** (Douglas-Peucker algorithm)
2. **Use Material 3 colors** for consistent theming
3. **Update incrementally** during live recording

---

## Implementation Quick Start

### Step 1: Add Dependencies

**gradle/libs.versions.toml**:
```toml
[versions]
googleMapsCompose = "6.1.0"

[libraries]
google-maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "googleMapsCompose" }
```

**app/build.gradle.kts**:
```kotlin
implementation(libs.google.maps.compose)
```

### Step 2: Create Utility Functions

**File**: `domain/util/PolylineUtils.kt`

```kotlin
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.example.bikeredlights.domain.model.TrackPoint

// Convert TrackPoint → LatLng
fun List<TrackPoint>.toLatLngList(): List<LatLng> {
    return map { LatLng(it.latitude, it.longitude) }
}

// Simplify polyline (reduce 3600 points → ~340 points)
fun List<LatLng>.simplifyRoute(tolerance: Double = 10.0): List<LatLng> {
    val toleranceDegrees = tolerance / 111000.0
    return PolyUtil.simplify(this, toleranceDegrees)
}

// One-liner conversion + simplification
fun List<TrackPoint>.toSimplifiedPolyline(
    tolerance: Double = 10.0
): List<LatLng> {
    return toLatLngList().simplifyRoute(tolerance)
}
```

### Step 3: Create Composable Component

**File**: `ui/components/route/RoutePolyline.kt`

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import com.example.bikeredlights.domain.model.TrackPoint
import com.example.bikeredlights.domain.util.toSimplifiedPolyline

@Composable
fun RoutePolyline(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val simplified = remember(trackPoints) {
        trackPoints.toSimplifiedPolyline(tolerance = 10.0)
    }

    if (simplified.isEmpty()) return

    GoogleMap(
        modifier = modifier,
        cameraPositionState = CameraPositionState(
            position = CameraPosition(
                target = simplified[simplified.size / 2],
                zoom = 14f
            )
        )
    ) {
        Polyline(
            points = simplified,
            color = MaterialTheme.colorScheme.primary,
            width = 8f,
            geodesic = true
        )
    }
}
```

### Step 4: Integrate with Screens

**In RideDetailScreen** or **RideReviewScreen**:

```kotlin
RoutePolyline(
    trackPoints = trackPoints,
    modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
)
```

---

## Key Concepts at a Glance

### Simplification Algorithm

| Metric | Before | After |
|--------|--------|-------|
| Points | 3,600 | ~340 |
| Reduction | - | 90% |
| Memory | 144 KB | 14 KB |
| Render Time | ~500ms | ~50ms |
| Visual Accuracy | - | 99% |

**Tolerance = 10 meters** is the sweet spot for bike routes.

### Color Strategy (Material 3)

```kotlin
// Primary route
color = MaterialTheme.colorScheme.primary  // Blue / Light Blue

// Alert zones
color = MaterialTheme.colorScheme.error  // Red / Light Red

// Paused segments
color = MaterialTheme.colorScheme.outlineVariant  // Gray
```

Automatically respects light/dark mode.

### Real-Time Updates (Live Recording)

```kotlin
// Update polyline every 10-20 points (smooth, no flicker)
LaunchedEffect(trackPoints) {
    if (trackPoints.size % 10 == 0) {  // Every 10 points
        polylineRef.value?.points = trackPoints.toSimplifiedPolyline()
    }
}
```

### Performance Targets

- **Polyline rendering**: < 100ms
- **Memory overhead**: < 50MB
- **Frame rate**: 60fps during interaction
- **Simplification time**: < 50ms

---

## Code Snippets by Use Case

### Case 1: Show Route on Ride Detail Screen

```kotlin
@Composable
fun RideDetailContent(rideDetail: RideDetailData) {
    Column {
        // Map with route
        RoutePolyline(
            trackPoints = rideDetail.trackPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        // Stats below
        Text("Distance: ${rideDetail.distanceFormatted}")
    }
}
```

### Case 2: Color-Coded Segments

```kotlin
@Composable
fun SegmentedRoute(trackPoints: List<TrackPoint>) {
    GoogleMap {
        // Active riding
        Polyline(
            points = trackPoints
                .filter { !it.isManuallyPaused && !it.isAutoPaused }
                .toSimplifiedPolyline(),
            color = Color.Blue
        )

        // Paused segments
        Polyline(
            points = trackPoints
                .filter { it.isManuallyPaused || it.isAutoPaused }
                .toSimplifiedPolyline(),
            color = Color.Gray
        )
    }
}
```

### Case 3: Live Recording Update

```kotlin
@Composable
fun LiveRouteMap(viewModel: LiveRideViewModel) {
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
    val polylineRef = remember { mutableStateOf<Polyline?>(null) }

    GoogleMap {
        LaunchedEffect(trackPoints) {
            if (trackPoints.size % 10 == 0) {  // Update every 10 points
                polylineRef.value?.points = trackPoints.toSimplifiedPolyline()
            }
        }

        Polyline(
            points = trackPoints.toSimplifiedPolyline(),
            color = MaterialTheme.colorScheme.primary,
            width = 8f
        )
    }
}
```

---

## Common Pitfalls & Solutions

### Pitfall 1: Rendering All 3600 Points

```kotlin
// ❌ WRONG: Causes jank
Polyline(points = trackPoints.toLatLngList())

// ✅ CORRECT: Simplify first
Polyline(points = trackPoints.toSimplifiedPolyline())
```

### Pitfall 2: Flickering During Live Recording

```kotlin
// ❌ WRONG: Entire polyline redraws (flicker)
Polyline(points = liveTrackPoints)  // Changes every location update

// ✅ CORRECT: Update existing polyline points
LaunchedEffect(liveTrackPoints) {
    polylineRef.value?.points = liveTrackPoints.toSimplifiedPolyline()
}
```

### Pitfall 3: Ignoring GPS Accuracy

```kotlin
// ❌ WRONG: Includes inaccurate points
points = trackPoints.toLatLngList()

// ✅ CORRECT: Filter by accuracy
points = trackPoints
    .filter { it.accuracy <= 30f }  // Only use points ≤ 30m accuracy
    .toLatLngList()
```

### Pitfall 4: Hardcoded Colors

```kotlin
// ❌ WRONG: Not compatible with dark mode
color = Color.Blue

// ✅ CORRECT: Use Material 3 dynamic colors
color = MaterialTheme.colorScheme.primary
```

---

## Testing Checklist

- [ ] **Unit Test**: Conversion functions work correctly
- [ ] **Unit Test**: Simplification reduces points by 80%+
- [ ] **Integration Test**: ViewModel provides track points correctly
- [ ] **Emulator Test**: 100-point route renders (fast)
- [ ] **Emulator Test**: 1000-point route renders (medium)
- [ ] **Emulator Test**: 3600-point route renders (full hour)
- [ ] **Performance Test**: Memory stays < 50MB
- [ ] **Performance Test**: 60fps during pan/zoom
- [ ] **Dark Mode Test**: Colors visible in both themes
- [ ] **Crash Test**: No ANR on 3600+ point routes

---

## File Locations to Create/Modify

### New Files
```
app/src/main/java/com/example/bikeredlights/
├── domain/util/PolylineUtils.kt                    (NEW)
└── ui/components/route/
    ├── RoutePolyline.kt                           (NEW)
    └── RouteMapDefaults.kt                        (NEW, optional)
```

### Modified Files
```
app/src/main/java/com/example/bikeredlights/
├── ui/screens/history/RideDetailScreen.kt        (MODIFY: Add map section)
├── ui/screens/ride/RideReviewScreen.kt           (MODIFY: Replace placeholder)
├── gradle/libs.versions.toml                     (MODIFY: Add dependencies)
└── app/build.gradle.kts                          (MODIFY: Add implementation)
```

### Test Files
```
app/src/test/java/com/example/bikeredlights/
└── domain/util/PolylineUtilsTest.kt             (NEW)
```

---

## Tolerance Values Reference

| Use Case | Tolerance | Reduction | Visual Quality |
|----------|-----------|-----------|-----------------|
| High precision (mountainous) | 5m | 70% | Excellent |
| **Standard (recommended)** | **10m** | **85%** | **Very Good** |
| Quick loading | 15m | 90% | Good |
| Extreme optimization | 20m | 95% | Fair |

**Start with 10m, tune if needed.**

---

## Performance Expectations

### 3600-point route (1 hour of riding at 1 Hz)

**Before optimization**:
- Raw LatLng list: 3,600 points
- Memory: ~144 KB
- Render time: ~500ms
- Frame rate impact: 60fps → 15fps

**After optimization (with 10m tolerance)**:
- Simplified list: ~340 points
- Memory: ~14 KB
- Render time: ~50ms
- Frame rate impact: 60fps → 60fps ✅

---

## Dependency Versions

| Library | Version | Release | Status |
|---------|---------|---------|--------|
| Google Maps Compose | 6.1.0 | Sep 2024 | ✅ Stable |
| Maps SDK Android | 18.2.0 | Jan 2024 | ✅ Stable |
| maps-ktx | 18.2.0 | Jan 2024 | ✅ Stable |

---

## Next Steps

1. **Add dependencies** to `gradle/libs.versions.toml` and `app/build.gradle.kts`
2. **Create PolylineUtils.kt** with conversion and simplification functions
3. **Create RoutePolyline composable** for reusability
4. **Update RideDetailScreen** to use new map component
5. **Test on emulator** with 3600-point routes
6. **Profile memory** to confirm < 50MB overhead
7. **Verify dark mode** colors work correctly

---

## Resources

- **Full Research Document**: `/POLYLINE_RESEARCH.md`
- **Google Maps Compose Docs**: https://github.com/googlemaps/android-maps-compose
- **Maps SDK Android Docs**: https://developers.google.com/maps/documentation/android-sdk/
- **Douglas-Peucker Algorithm**: https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm

---

**Version**: 1.0 | **Last Updated**: November 2025
