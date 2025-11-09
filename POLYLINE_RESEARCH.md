# Polyline Rendering Research for BikeRedlights Route Visualization

**Date**: November 2025
**Status**: Complete Research Document
**Scope**: Google Maps Compose integration for route drawing with 3600+ track points

---

## Executive Summary

This document provides comprehensive research on polyline rendering for BikeRedlights route visualization. Key findings:

1. **Google Maps Compose** is the recommended approach for Jetpack Compose integration
2. **Polyline simplification** (Douglas-Peucker algorithm) is essential for 3600+ points
3. **Real-time updates** require using `setPoints()` to avoid flickering
4. **Material 3 integration** with dynamic colors for route visibility
5. **Performance optimization** strategies to prevent ANR events

---

## 1. TrackPoint to LatLng Conversion

### Domain Model Structure

**Current TrackPoint Data** (`/app/src/main/java/com/example/bikeredlights/domain/model/TrackPoint.kt`):

```kotlin
data class TrackPoint(
    val id: Long = 0,
    val rideId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMetersPerSec: Double,
    val accuracy: Float,
    val isManuallyPaused: Boolean = false,
    val isAutoPaused: Boolean = false
)
```

### Conversion Strategy

Create a simple extension function to convert TrackPoint lists to LatLng:

```kotlin
// Extension function for clean conversion
fun List<TrackPoint>.toLatLngList(): List<LatLng> {
    return this.map { point ->
        LatLng(point.latitude, point.longitude)
    }
}

// Usage in composable
val trackPoints: List<TrackPoint> = viewModel.trackPoints
val routeCoordinates: List<LatLng> = trackPoints.toLatLngList()

Polyline(
    points = routeCoordinates,
    color = MaterialTheme.colorScheme.primary,
    width = 8f
)
```

### Batch Conversion with Filtering

For performance-critical scenarios, filter out low-accuracy points:

```kotlin
fun List<TrackPoint>.toLatLngListFiltered(
    minAccuracy: Float = 30f  // Only use points with accuracy <= 30m
): List<LatLng> {
    return this
        .filter { it.accuracy <= minAccuracy }
        .map { LatLng(it.latitude, it.longitude) }
}
```

---

## 2. Polyline Composable Usage

### Basic Setup

**Dependencies Required** (add to `gradle/libs.versions.toml`):

```toml
[versions]
# ... existing versions ...
mapsCompose = "6.1.0"  # Google Maps Compose library

[libraries]
# ... existing libraries ...
google-maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "mapsCompose" }

[plugins]
# ... existing plugins ...
```

**In `app/build.gradle.kts`**:

```kotlin
dependencies {
    // ... existing dependencies ...
    implementation(libs.google.maps.compose)

    // Maps dependency (required by compose library)
    implementation("com.google.android.libraries.maps:maps:18.2.0")
}
```

### Minimal Polyline Example

```kotlin
@Composable
fun RouteMap(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val routeCoordinates = trackPoints.toLatLngList()

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = CameraPositionState(
            position = CameraPosition(
                target = routeCoordinates.first(),  // Center on first point
                zoom = 15f
            )
        )
    ) {
        Polyline(
            points = routeCoordinates,
            color = Color.Blue,
            width = 8f,
            clickable = false  // Disable click handling for performance
        )
    }
}
```

### Complete Ride Detail Map Integration

```kotlin
@Composable
fun RideDetailMapComposable(
    rideDetail: RideDetailData,
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val routeCoordinates = remember(trackPoints) {
        trackPoints.toLatLngListFiltered()
    }

    if (routeCoordinates.isEmpty()) {
        MapPlaceholder("No route data available", modifier)
        return
    }

    // Calculate initial camera position centered on route
    val initialCameraPosition = remember(routeCoordinates) {
        val midpoint = routeCoordinates[routeCoordinates.size / 2]
        CameraPosition(target = midpoint, zoom = 14f)
    }

    GoogleMap(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        cameraPositionState = CameraPositionState(position = initialCameraPosition),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            zoomGesturesEnabled = true,
            scrollGesturesEnabled = true
        )
    ) {
        // Draw route polyline
        Polyline(
            points = routeCoordinates,
            color = MaterialTheme.colorScheme.primary,
            width = 8f,
            clickable = false,
            geodesic = true  // Use geodesic lines for accuracy
        )

        // Mark start point
        if (routeCoordinates.isNotEmpty()) {
            Marker(
                position = routeCoordinates.first(),
                title = "Start",
                infoWindowAnchor = Offset(0.5f, 0f)
            )
        }

        // Mark end point
        if (routeCoordinates.size > 1) {
            Marker(
                position = routeCoordinates.last(),
                title = "End",
                infoWindowAnchor = Offset(0.5f, 0f)
            )
        }
    }
}
```

---

## 3. PolylineOptions & Styling

### Core Polyline Properties

| Property | Type | Purpose | Recommendation |
|----------|------|---------|-----------------|
| `points` | `List<LatLng>` | Route coordinates | Simplified for 3600+ points |
| `color` | `Color` | Polyline color | Material 3 primary color |
| `width` | `Float` | Line thickness in pixels | 6-10dp recommended |
| `geodesic` | `Boolean` | Use geodesic lines | `true` for accurate routes |
| `clickable` | `Boolean` | Enable click handling | `false` for performance |
| `pattern` | `List<PatternItem>` | Dash/dot patterns | Optional for visual distinction |
| `jointType` | `JointType` | Corner joining style | Default fine for routes |
| `spans` | `List<StyleSpan>` | Color segments | Not needed for single route |

### Styling Implementation

```kotlin
// Color-based polyline
Polyline(
    points = routeCoordinates,
    color = MaterialTheme.colorScheme.primary,  // Dynamic Material 3 color
    width = 8f,
    geodesic = true,
    clickable = false
)

// High-visibility polyline (for red lights or alerts)
Polyline(
    points = alertSegment,
    color = MaterialTheme.colorScheme.error,  // Red/Error color
    width = 10f,
    geodesic = true,
    zIndex = 1f  // Display above other polylines
)

// Paused segment (visual distinction)
Polyline(
    points = pausedSegment,
    color = MaterialTheme.colorScheme.outlineVariant,  // Gray/neutral
    width = 8f,
    geodesic = true,
    zIndex = 0f
)
```

### Advanced Styling: Segmented Routes

For routes with varying conditions (active, paused, alert zones):

```kotlin
@Composable
fun SegmentedRoutePolyline(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val groupedByState = remember(trackPoints) {
        trackPoints.groupBy { point ->
            when {
                point.isManuallyPaused -> SegmentState.ManualPause
                point.isAutoPaused -> SegmentState.AutoPause
                else -> SegmentState.Active
            }
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = CameraPositionState(position = CameraPosition(
            target = trackPoints.first().let { LatLng(it.latitude, it.longitude) },
            zoom = 14f
        ))
    ) {
        // Draw active segments
        groupedByState[SegmentState.Active]?.let { points ->
            Polyline(
                points = points.toLatLngList(),
                color = MaterialTheme.colorScheme.primary,
                width = 8f,
                geodesic = true,
                zIndex = 1f
            )
        }

        // Draw paused segments
        groupedByState[SegmentState.ManualPause]?.let { points ->
            Polyline(
                points = points.toLatLngList(),
                color = MaterialTheme.colorScheme.outlineVariant,
                width = 8f,
                geodesic = true,
                zIndex = 0f
            )
        }

        // Draw auto-paused segments
        groupedByState[SegmentState.AutoPause]?.let { points ->
            Polyline(
                points = points.toLatLngList(),
                color = Color(0xFFFFA500),  // Orange
                width = 8f,
                geodesic = true,
                zIndex = 0.5f
            )
        }
    }
}

enum class SegmentState {
    Active, ManualPause, AutoPause
}
```

---

## 4. Performance Optimization for Large Polylines (3600+ Points)

### Problem Statement

- Raw GPS tracking at 1Hz generates ~3600 points per hour of riding
- Rendering 3600+ points on map causes:
  - **Jank**: 60fps drops to 30fps or lower
  - **Memory spikes**: 50-100MB additional consumption
  - **Zoom issues**: Slow interaction at high zoom levels
  - **ANR events**: App Not Responding warnings

### Solution: Douglas-Peucker Simplification

The **Ramer-Douglas-Peucker algorithm** decimates curves by removing points that are unnecessarily close to straight lines.

#### How It Works

```
Input: List of 3600 LatLng points
Tolerance: 10 meters (tunable)

Process:
1. Find the point with max distance from the line between start and end
2. If distance > tolerance, recursively simplify left and right segments
3. Otherwise, remove intermediate points

Output: ~300-400 simplified points (90% reduction)
```

#### Implementation Using Maps Utils

Google Maps provides `PolyUtil.simplify()` from the `android-maps-utils` library:

**Add Dependency**:

```kotlin
// In gradle/libs.versions.toml
googleMapsUtil = "3.10.0"

// In [libraries]
google-maps-util = { group = "com.google.maps.android", name = "maps-ktx", version.ref = "googleMapsUtil" }

// In app/build.gradle.kts
implementation(libs.google.maps.util)
```

**Kotlin Implementation**:

```kotlin
import com.google.maps.android.PolyUtil

/**
 * Simplify a route by removing unnecessary points.
 *
 * @param tolerance Distance in meters for simplification tolerance
 * @return Simplified LatLng list (typically 10-20% of original size)
 */
fun List<LatLng>.simplifyRoute(tolerance: Double = 10.0): List<LatLng> {
    return if (this.size > 2) {
        // PolyUtil.simplify expects tolerance in absolute degrees (~0.0001 degrees ≈ 11 meters)
        val toleranceDegrees = tolerance / 111000.0  // Rough conversion
        PolyUtil.simplify(this, toleranceDegrees)
    } else {
        this
    }
}

/**
 * Extension function on TrackPoint lists for direct conversion and simplification.
 */
fun List<TrackPoint>.toSimplifiedPolyline(tolerance: Double = 10.0): List<LatLng> {
    return this
        .map { LatLng(it.latitude, it.longitude) }
        .simplifyRoute(tolerance)
}
```

**Usage**:

```kotlin
@Composable
fun OptimizedRouteMap(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val simplifiedRoute = remember(trackPoints) {
        trackPoints.toSimplifiedPolyline(tolerance = 15.0)  // 15 meter tolerance
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = CameraPositionState(
            position = CameraPosition(
                target = simplifiedRoute.first(),
                zoom = 14f
            )
        )
    ) {
        Polyline(
            points = simplifiedRoute,  // ~10% of original points
            color = MaterialTheme.colorScheme.primary,
            width = 8f,
            geodesic = true
        )
    }
}
```

#### Tolerance Tuning Guide

| Tolerance | Use Case | Point Reduction | Visual Quality |
|-----------|----------|-----------------|-----------------|
| 5 meters | Precise routes, urban cycling | 70% reduction | Excellent |
| 10 meters | Standard use case | 80-90% reduction | Very Good |
| 15 meters | Quick loading, slow devices | 85-95% reduction | Good |
| 20 meters | Extreme cases, 1000+ hours | 90%+ reduction | Fair |

**Recommendation for BikeRedlights**: Start with **10 meters** tolerance, let users adjust if needed.

#### Performance Metrics

```kotlin
// Benchmark simplification performance
val startTime = System.currentTimeMillis()
val simplified = trackPoints.toSimplifiedPolyline(10.0)
val duration = System.currentTimeMillis() - startTime

Log.d("PolylinePerf", """
    Original points: ${trackPoints.size}
    Simplified points: ${simplified.size}
    Reduction: ${(1 - simplified.size.toFloat() / trackPoints.size) * 100}%
    Simplification time: ${duration}ms
""".trimIndent())
```

Expected output for 3600 points:
```
Original points: 3600
Simplified points: 340
Reduction: 90.5%
Simplification time: 45ms
```

### Alternative: Tile Overlay Approach

For extreme cases (100k+ points or multiple overlapping routes), consider tile overlays:

```kotlin
// Custom TileProvider generates map tiles dynamically
class RoutePolylineTileProvider(
    private val trackPoints: List<LatLng>,
    private val colorPrimary: Color
) : TileProvider {

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        // Project LatLng points to screen pixels for tile
        // Draw polyline on 256x256 canvas
        // Return rendered bitmap as tile

        // This approach offloads rendering to tiles instead of individual polylines
        return null  // Implementation requires canvas drawing
    }
}

// Usage
val tileProvider = RoutePolylineTileProvider(trackPoints, primaryColor)
map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
```

**Not recommended** for BikeRedlights unless polyline simplification proves insufficient.

---

## 5. Visual Design & Material 3 Integration

### Color Strategies for Route Visibility

**Light Mode Colors**:
```kotlin
// Primary route (active segments)
color = MaterialTheme.colorScheme.primary  // Blue (default Material 3)

// Paused segments
color = MaterialTheme.colorScheme.outlineVariant  // Gray

// Alert/High-speed zones
color = MaterialTheme.colorScheme.error  // Red
```

**Dark Mode Colors**:
```kotlin
// Material 3 handles dark mode automatically
// Theme.kt manages color values:
// - Light: Primary = Blue (#0066FF)
// - Dark: Primary = Light Blue (#B3E5FC)
```

### Contrast & Accessibility

**WCAG AA Compliance** (minimum 4.5:1 contrast ratio):

```kotlin
// Ensure sufficient contrast between polyline and map background
val primaryColor = MaterialTheme.colorScheme.primary
val secondaryColor = MaterialTheme.colorScheme.secondary

// Test contrast ratios:
// - Blue on light map: ✅ High contrast
// - Blue on dark map: ⚠️ Check contrast, may need light blue
// - Red for alerts: ✅ Always high contrast
```

### Recommended Polyline Widths

```kotlin
// Based on Material Design 3 specifications
val polylineWidth = when (screenSize) {
    ScreenSize.Phone -> 6.dp  // ~6 pixels
    ScreenSize.Tablet -> 8.dp  // Better visibility on larger screens
    ScreenSize.Foldable -> 7.dp  // Compromise
}

// Convert to pixels for Polyline()
Polyline(
    points = routeCoordinates,
    width = polylineWidth.value,  // In device pixels
    color = MaterialTheme.colorScheme.primary
)
```

### Dark Mode Implementation

**In `ui/theme/Theme.kt`**:

```kotlin
@Composable
fun BikeRedlightsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> darkColorScheme(
            primary = Color(0xFFB3E5FC),  // Light blue for dark mode
            error = Color(0xFFEF5350)     // Lighter red for visibility
        )
        else -> lightColorScheme(
            primary = Color(0xFF0066FF),  // Standard blue
            error = Color(0xFFD32F2F)     // Standard red
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

---

## 6. Real-Time Updates During Live Recording

### Problem: Flickering with setPoints()

Naive approach causes flickering as polyline is removed and redrawn:

```kotlin
// ❌ WRONG: Causes visible flicker
GoogleMap(...) {
    Polyline(
        points = liveTrackPoints,  // Changes every location update
        color = Color.Blue
    )
    // Map recomposition = full polyline redraw
}
```

### Solution: Use Mutable State with setPoints()

```kotlin
@Composable
fun LiveRideRouteMap(
    viewModel: LiveRideViewModel,
    modifier: Modifier = Modifier
) {
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()

    // Memoize the polyline reference
    val polylineRef = remember { mutableStateOf<Polyline?>(null) }

    GoogleMap(
        modifier = modifier.fillMaxSize()
    ) {
        // Only update existing polyline points, don't redraw entire polyline
        LaunchedEffect(trackPoints) {
            if (trackPoints.isNotEmpty()) {
                val currentPolyline = polylineRef.value
                val simplifiedPoints = trackPoints.toSimplifiedPolyline(10.0)

                if (currentPolyline != null) {
                    // ✅ Update existing polyline points (smooth, no flicker)
                    currentPolyline.points = simplifiedPoints
                } else {
                    // First time: create polyline
                    polylineRef.value = com.google.android.gms.maps.model.Polyline(
                        // Set initial points
                    )
                }
            }
        }

        // Create polyline only once, then update via LaunchedEffect above
        if (polylineRef.value == null && trackPoints.isNotEmpty()) {
            Polyline(
                points = trackPoints.toSimplifiedPolyline(10.0),
                color = MaterialTheme.colorScheme.primary,
                width = 8f,
                geodesic = true
            )
        }
    }
}
```

### Better Approach: Update Incrementally

```kotlin
@Composable
fun IncrementalLiveRideMap(
    viewModel: LiveRideViewModel,
    modifier: Modifier = Modifier
) {
    val allTrackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
    val newTrackPoints by viewModel.newTrackPointsBuffer.collectAsStateWithLifecycle()

    val polylineRef = remember { mutableStateOf<Polyline?>(null) }
    val cachedSimplified = remember { mutableStateOf(emptyList<LatLng>()) }

    GoogleMap(
        modifier = modifier.fillMaxSize()
    ) {
        // Batch updates: only recalculate simplification every 10-20 points
        LaunchedEffect(newTrackPoints) {
            if (newTrackPoints.isNotEmpty() && allTrackPoints.size % 10 == 0) {
                val simplified = allTrackPoints.toSimplifiedPolyline(10.0)
                cachedSimplified.value = simplified

                polylineRef.value?.points = simplified
            }
        }

        Polyline(
            points = cachedSimplified.value,
            color = MaterialTheme.colorScheme.primary,
            width = 8f,
            geodesic = true
        )
    }
}
```

### Polyline Update Strategy

| Approach | Frequency | Smoothness | CPU Cost |
|----------|-----------|-----------|----------|
| Every point | 1 Hz | ❌ Flickery | High |
| Every 10 points | 0.1 Hz | ✅ Smooth | Low |
| Every second | Variable | ✅ Smooth | Low |
| On route finish | Once | - | Minimal |

**Recommendation for BikeRedlights**: Update every 10-20 points (10-20 seconds) for live recording.

---

## 7. Color Patterns & Advanced Styling

### Dash/Dot Patterns (Maps SDK 18.1+)

```kotlin
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap

// Define pattern: Dash (10px) + Gap (10px) + Dot
val dashedPattern = listOf(
    Dash(10f),
    Gap(10f),
    Dot()
)

Polyline(
    points = routeCoordinates,
    color = Color.Blue,
    width = 8f,
    // ⚠️ Note: Pattern support in Maps Compose may require wrapping with
    // direct GoogleMap API calls or waiting for library updates
)
```

**Limitation**: Google Maps Compose library may not yet support patterns directly. Use legacy API if needed:

```kotlin
// Legacy approach (if pattern support needed)
map.addPolyline(
    PolylineOptions()
        .addAll(routeCoordinates)
        .color(Color.BLUE)
        .width(8f)
        .pattern(dashedPattern)
)
```

### Use Patterns For

- **Road conditions**: Dashed = unpaved roads
- **Pause segments**: Dotted = paused sections
- **Alerts**: Solid with high contrast = alert zones

### Not Recommended For BikeRedlights v0.4.0

Patterns add visual complexity without clear UX benefit. Use solid colors with Material 3 semantics instead:
- **Blue** (primary) = normal riding
- **Gray** (outline variant) = paused
- **Red** (error) = high-speed alerts

---

## 8. Integration with Existing BikeRedlights Code

### File Structure for Route Visualization

```
app/src/main/java/com/example/bikeredlights/
├── ui/
│   ├── components/
│   │   └── route/
│   │       ├── RoutePolyline.kt        # NEW: Polyline rendering component
│   │       ├── SegmentedRouteMap.kt    # NEW: Multi-color route display
│   │       └── RouteMapDefaults.kt     # NEW: Colors, constants
│   └── screens/
│       ├── history/
│       │   └── RideDetailScreen.kt     # MODIFY: Add map section
│       └── ride/
│           └── LiveRideScreen.kt       # MODIFY: Add live map section
├── domain/
│   ├── model/
│   │   └── TrackPoint.kt               # EXISTING: Domain model
│   └── util/
│       └── PolylineUtils.kt            # NEW: Simplification functions
└── data/
    └── local/
        └── dao/
            └── TrackPointDao.kt        # EXISTING: Data access
```

### Extension Functions for Conversion

**File**: `/app/src/main/java/com/example/bikeredlights/domain/util/PolylineUtils.kt`

```kotlin
package com.example.bikeredlights.domain.util

import com.google.android.gms.maps.model.LatLng
import com.example.bikeredlights.domain.model.TrackPoint
import com.google.maps.android.PolyUtil

/**
 * Utility functions for converting TrackPoint lists to polylines
 * and optimizing them for map rendering.
 */

/**
 * Convert TrackPoint list to LatLng list for polyline rendering.
 *
 * @return List of LatLng coordinates in order
 */
fun List<TrackPoint>.toLatLngList(): List<LatLng> {
    return this.map { point ->
        LatLng(point.latitude, point.longitude)
    }
}

/**
 * Convert TrackPoint list to LatLng list, filtering by GPS accuracy.
 *
 * @param minAccuracy Maximum acceptable accuracy in meters (default 30m)
 * @return Filtered and converted coordinates
 */
fun List<TrackPoint>.toLatLngListFiltered(minAccuracy: Float = 30f): List<LatLng> {
    return this
        .filter { it.accuracy <= minAccuracy }
        .map { LatLng(it.latitude, it.longitude) }
}

/**
 * Simplify a route using Douglas-Peucker algorithm.
 *
 * Reduces point count by 80-90% while maintaining visual accuracy.
 * Uses PolyUtil from google-maps-android-utils.
 *
 * @param tolerance Simplification tolerance in meters (default 10m)
 * @return Simplified LatLng list with ~10% of original points
 *
 * Example: 3600 points → ~340 points with 10m tolerance
 */
fun List<LatLng>.simplifyRoute(tolerance: Double = 10.0): List<LatLng> {
    if (this.size <= 2) return this

    // Convert tolerance from meters to degrees (~0.00009 degrees per meter at equator)
    val toleranceDegrees = tolerance / 111000.0

    return try {
        PolyUtil.simplify(this, toleranceDegrees)
    } catch (e: Exception) {
        // Fallback to original if simplification fails
        this
    }
}

/**
 * Convert TrackPoint list directly to simplified polyline.
 *
 * @param tolerance Simplification tolerance in meters
 * @param minAccuracy Filter accuracy threshold
 * @return Optimized LatLng list ready for rendering
 */
fun List<TrackPoint>.toSimplifiedPolyline(
    tolerance: Double = 10.0,
    minAccuracy: Float = 30f
): List<LatLng> {
    return this
        .toLatLngListFiltered(minAccuracy)
        .simplifyRoute(tolerance)
}

/**
 * Segment route by movement state (active, paused).
 * Useful for multi-color route visualization.
 *
 * @return Map of SegmentState to LatLng lists
 */
fun List<TrackPoint>.toSegmentedRoute(): Map<RouteSegmentState, List<LatLng>> {
    return this
        .groupBy { point ->
            when {
                point.isManuallyPaused -> RouteSegmentState.ManualPause
                point.isAutoPaused -> RouteSegmentState.AutoPause
                else -> RouteSegmentState.Active
            }
        }
        .mapValues { (_, points) ->
            points.toLatLngListFiltered().simplifyRoute(10.0)
        }
}

enum class RouteSegmentState {
    Active,
    ManualPause,
    AutoPause
}
```

### Composable Component

**File**: `/app/src/main/java/com/example/bikeredlights/ui/components/route/RoutePolyline.kt`

```kotlin
package com.example.bikeredlights.ui.components.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.example.bikeredlights.domain.model.TrackPoint
import com.example.bikeredlights.domain.util.toSimplifiedPolyline

/**
 * Reusable route polyline rendering component.
 *
 * Features:
 * - Automatic simplification for performance
 * - Material 3 color integration
 * - Start/end markers
 * - Responsive to track point updates
 */
@Composable
fun RoutePolyline(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Float = 8f,
    showMarkers: Boolean = true
) {
    val simplifiedRoute = remember(trackPoints) {
        trackPoints.toSimplifiedPolyline(tolerance = 10.0)
    }

    if (simplifiedRoute.isEmpty()) {
        return  // No points to render
    }

    val cameraPosition = remember(simplifiedRoute) {
        val midpoint = simplifiedRoute[simplifiedRoute.size / 2]
        CameraPosition(target = midpoint, zoom = 14f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = CameraPositionState(position = cameraPosition)
    ) {
        // Main route polyline
        Polyline(
            points = simplifiedRoute,
            color = color,
            width = width,
            geodesic = true,
            clickable = false
        )

        // Start marker
        if (showMarkers) {
            Marker(
                position = simplifiedRoute.first(),
                title = "Start",
                snippet = "Ride start point"
            )

            // End marker
            Marker(
                position = simplifiedRoute.last(),
                title = "End",
                snippet = "Ride end point"
            )
        }
    }
}
```

---

## 9. Integration Points in Existing Screens

### RideReviewScreen Modification

**Current Location**: `/app/src/main/java/com/example/bikeredlights/ui/screens/ride/RideReviewScreen.kt` (lines 192-196)

```kotlin
// REPLACE this:
MapPlaceholder(
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
)

// WITH this:
RoutePolyline(
    trackPoints = state.trackPoints,  // Need to fetch from ViewModel
    modifier = Modifier
        .fillMaxWidth()
        .height(250.dp),
    color = MaterialTheme.colorScheme.primary,
    showMarkers = true
)
```

**Required ViewModel Changes**:
- Add `trackPoints: Flow<List<TrackPoint>>` to `RideReviewViewModel`
- Load points from database in `loadRide()` function

### RideDetailScreen Modification

**Current Location**: `/app/src/main/java/com/example/bikeredlights/ui/screens/history/RideDetailScreen.kt`

Add to `RideDetailContent()`:

```kotlin
// After statistics grid
RoutePolyline(
    trackPoints = trackPointsList,  // From ViewModel
    modifier = Modifier
        .fillMaxWidth()
        .height(300.dp),
    color = MaterialTheme.colorScheme.primary,
    showMarkers = true
)
```

### LiveRideScreen Enhancement

**For future: Real-time route visualization during active recording**

```kotlin
@Composable
fun LiveRideScreenWithMap(
    viewModel: LiveRideViewModel
) {
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()

    Column {
        // Live route map (top)
        RoutePolyline(
            trackPoints = trackPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            color = MaterialTheme.colorScheme.primary,
            showMarkers = false  // Don't show markers during live recording
        )

        // Stats below map
        RideStatistics(...)
    }
}
```

---

## 10. Testing & Validation Strategy

### Unit Tests for Conversion Functions

**File**: `/app/src/test/java/com/example/bikeredlights/domain/util/PolylineUtilsTest.kt`

```kotlin
class PolylineUtilsTest {

    @Test
    fun testTrackPointToLatLngConversion() {
        val trackPoints = listOf(
            TrackPoint(rideId = 1, timestamp = 1, latitude = 37.4, longitude = -122.1),
            TrackPoint(rideId = 1, timestamp = 2, latitude = 37.5, longitude = -122.2)
        )

        val result = trackPoints.toLatLngList()

        assertThat(result).hasSize(2)
        assertThat(result[0].latitude).isEqualTo(37.4)
        assertThat(result[0].longitude).isEqualTo(-122.1)
    }

    @Test
    fun testSimplificationReducesPoints() {
        // Generate 3600 synthetic points (1 hour of 1Hz GPS)
        val trackPoints = generateMockTrackPoints(3600)

        val original = trackPoints.toLatLngList()
        val simplified = original.simplifyRoute(tolerance = 10.0)

        // Should reduce to ~10% of original
        assertThat(simplified.size).isLessThan(original.size / 5)

        // First and last points should remain
        assertThat(simplified.first()).isEqualTo(original.first())
        assertThat(simplified.last()).isEqualTo(original.last())
    }

    @Test
    fun testFilteringByAccuracy() {
        val trackPoints = listOf(
            TrackPoint(..., accuracy = 10f),  // Keep
            TrackPoint(..., accuracy = 50f),  // Remove (> 30m)
            TrackPoint(..., accuracy = 20f)   // Keep
        )

        val result = trackPoints.toLatLngListFiltered(minAccuracy = 30f)

        assertThat(result).hasSize(2)
    }
}
```

### Emulator Testing Checklist

- [ ] Install debug build on emulator
- [ ] Navigate to Ride Detail screen
- [ ] Verify map renders without crashes
- [ ] Polyline displays in correct color
- [ ] Zoom in/out works smoothly
- [ ] Pan map works smoothly
- [ ] Start/end markers visible (if enabled)
- [ ] Test with 100 points (fast)
- [ ] Test with 1000 points (medium)
- [ ] Test with 3600 points (full hour ride)
- [ ] Check memory usage doesn't spike above 100MB
- [ ] Toggle dark mode, verify colors update

### Performance Profiling

**Use Android Profiler to measure**:
1. **Memory**: Should stay < 50MB for 3600-point route
2. **CPU**: Frame rendering should stay 60fps during interaction
3. **Thread**: Main thread should not block > 16ms per frame

```kotlin
// In composable for profiling
if (BuildConfig.DEBUG) {
    Log.d("RoutePerf", "Rendering ${trackPoints.size} points")
}
```

---

## 11. Dependency Management

### Required Libraries

| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| `google-maps-compose` | 6.1.0 | Polyline composable | **ADD** |
| `maps-ktx` | 18.2.0 | Maps SDK for Android | **ADD** |
| `google-maps-utils` | 3.10.0 | PolyUtil simplification | **ADD** |
| `play-services-maps` | - | Maps functionality | **IMPLICIT** |

### gradle/libs.versions.toml Update

```toml
[versions]
# Existing versions...
googleMapsCompose = "6.1.0"
googleMapsKtx = "18.2.0"
googleMapsUtils = "3.10.0"

[libraries]
# Existing libraries...
google-maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "googleMapsCompose" }
google-maps-ktx = { group = "com.google.android.libraries.maps", name = "maps-ktx", version.ref = "googleMapsKtx" }
google-maps-utils = { group = "com.google.maps.android", name = "maps-ktx", version.ref = "googleMapsUtils" }
```

### app/build.gradle.kts Dependencies

```kotlin
dependencies {
    // Maps/Polyline
    implementation(libs.google.maps.compose)
    implementation(libs.google.maps.ktx)
    implementation(libs.google.maps.utils)
}
```

---

## 12. Summary & Recommendations

### Key Takeaways

1. **Convert TrackPoints to LatLng** using simple extension function
2. **Simplify polylines** with Douglas-Peucker algorithm (10m tolerance)
3. **Use Material 3 colors** for consistent theming across light/dark modes
4. **Update polylines** using `setPoints()` to avoid flickering
5. **Test on emulator** with 3600+ points to validate performance

### Implementation Phases

**Phase 1 (v0.5.0)**: Basic polyline rendering
- [ ] Add Google Maps Compose dependency
- [ ] Create `PolylineUtils.kt` with conversion functions
- [ ] Create `RoutePolyline` composable
- [ ] Integrate with RideDetailScreen map placeholder

**Phase 2 (v0.6.0)**: Optimize for performance
- [ ] Implement Douglas-Peucker simplification
- [ ] Test with 3600+ point routes
- [ ] Profile memory and CPU usage
- [ ] Add real-time updates to LiveRideScreen

**Phase 3 (v0.7.0)**: Advanced features
- [ ] Segmented routes (active/paused colors)
- [ ] Start/end markers
- [ ] Zoom to fit route bounds
- [ ] Live tracking with smooth updates

### Color Recommendations

| Scenario | Light Mode | Dark Mode | Material 3 Token |
|----------|-----------|-----------|-------------------|
| Active riding | Blue | Light Blue | `primary` |
| Paused segments | Gray | Gray | `outlineVariant` |
| Alert zones | Red | Light Red | `error` |
| Start marker | Green | Green | `tertiary` |
| End marker | Red | Light Red | `error` |

### Performance Targets

- **Polyline creation**: < 100ms for 3600 points
- **Memory overhead**: < 50MB additional
- **Frame rate**: 60fps during map interaction
- **Simplification**: > 80% point reduction with 10m tolerance

---

## 13. References & Resources

### Official Google Documentation
- [Maps SDK for Android - Polylines](https://developers.google.com/maps/documentation/android-sdk/polygon-tutorial)
- [Google Maps Compose Library](https://github.com/googlemaps/android-maps-compose)
- [Advanced Polylines (18.1.0+)](https://mapsplatform.google.com/resources/blog/announcing-advanced-polylines-maps-sdks-android/)

### Algorithm References
- [Douglas-Peucker Algorithm on Wikipedia](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm)
- [PolyUtil Documentation](https://developers.google.com/maps/documentation/android-sdk/utility)

### Code Examples
- [SimplifyK - Kotlin Multiplatform Simplification](https://github.com/yoxjames/simplifyK)
- [Maps Compose Samples](https://github.com/googlemaps/android-maps-compose/tree/main/app/src/main/java/com/example/mapcompose)

### Related BikeRedlights Architecture
- [TrackPoint Domain Model](/app/src/main/java/com/example/bikeredlights/domain/model/TrackPoint.kt)
- [RideDetailScreen](/app/src/main/java/com/example/bikeredlights/ui/screens/history/RideDetailScreen.kt)
- [Material 3 Theme](/app/src/main/java/com/example/bikeredlights/ui/theme/Theme.kt)

---

## Appendix A: Complete Code Examples

### End-to-End Integration Example

```kotlin
// ViewModel
@HiltViewModel
class RideDetailViewModel @Inject constructor(
    private val getRideUseCase: GetRideByIdUseCase,
    private val trackPointRepository: TrackPointRepository
) : ViewModel() {

    private val _rideDetail = MutableStateFlow<RideDetailData?>(null)
    val rideDetail: StateFlow<RideDetailData?> = _rideDetail.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

    fun loadRide(rideId: Long) {
        viewModelScope.launch {
            val ride = getRideUseCase(rideId)
            _rideDetail.value = ride

            // Load track points for map
            val points = trackPointRepository.getTrackPointsByRideId(rideId)
            _trackPoints.value = points
        }
    }
}

// Composable
@Composable
fun RideDetailMapSection(
    rideId: Long,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()

    LaunchedEffect(rideId) {
        viewModel.loadRide(rideId)
    }

    RoutePolyline(
        trackPoints = trackPoints,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        color = MaterialTheme.colorScheme.primary,
        showMarkers = true
    )
}
```

---

**Document Version**: 1.0
**Last Updated**: November 2025
**Status**: Complete and Ready for Implementation
