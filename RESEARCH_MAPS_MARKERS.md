# Google Maps Compose: Markers, Custom Icons & Auto-Zoom Research

**Date:** November 8, 2025
**Purpose:** Comprehensive research on implementing markers, custom icons, and auto-zoom functionality in Google Maps Compose for the BikeRedlights ride history/detail view.

---

## 1. Marker Composable & Configuration

### 1.1 Basic Marker Implementation

The `Marker` composable from `com.google.maps.android.compose` is the primary component for displaying location pins:

```kotlin
@Composable
fun Marker(
    state: MarkerState,                          // Position, dragging state
    alpha: Float = 1.0f,                         // Opacity (0-1)
    anchor: Offset = Offset(0.5f, 1f),          // Icon anchor point (default: bottom-center)
    draggable: Boolean = false,                 // User can drag marker
    flat: Boolean = false,                      // Flat vs billboard rotation
    icon: BitmapDescriptor? = null,             // Custom icon (use BitmapDescriptorFactory)
    infoWindowEnabled: Boolean = true,          // Show info window on click
    rotation: Float = 0f,                       // Rotation in degrees
    snippet: String? = null,                    // Info window subtitle
    title: String? = null,                      // Info window title
    visible: Boolean = true,                    // Marker visibility
    zIndex: Float = 0f,                         // Layer ordering
    onClick: (Marker) -> Boolean = { false },   // Click handler (return true to consume)
    onInfoWindowClick: (Marker) -> Unit = {},   // Info window click
    onInfoWindowClose: (Marker) -> Unit = {},   // Info window close
    onInfoWindowLongClick: (Marker) -> Unit = {}, // Info window long click
    contentDescription: String? = null,         // Accessibility label
)
```

### 1.2 MarkerState Management

Controls marker position and dragging state:

```kotlin
// Create marker state with position
val markerState = rememberMarkerState(position = LatLng(37.422, -122.084))

// Or with custom initialization
val markerState = rememberUpdatedMarkerState(
    position = currentLocation,
    title = "Current Location"
)

// MarkerState properties
markerState.position           // LatLng - updatable
markerState.dragState          // DragState - IDLE, STARTED, DRAGGING, ENDED
markerState.isDragging         // Boolean - true during drag

// Methods
markerState.hideInfoWindow()   // Close info window
markerState.showInfoWindow()   // Open info window
```

### 1.3 Marker Variants for Info Windows

Different composables for different customization levels:

```kotlin
// Standard marker with default info window
Marker(
    state = markerState,
    title = "Location Name",
    snippet = "Subtitle text"
)

// Custom info window content
MarkerInfoWindowContent(
    state = markerState,
    title = "Location",
) {
    Box(modifier = Modifier.padding(8.dp)) {
        Column {
            Text("Custom content here")
            Button(onClick = { /* ... */ }) {
                Text("Action")
            }
        }
    }
}

// Fully custom info window
MarkerInfoWindow(
    state = markerState,
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Custom Info Window")
            // Any composable content
        }
    }
}

// Custom composable as marker icon (recommended for complex markers)
MarkerComposable(
    state = rememberMarkerState(position = latLng),
    title = "Marker Title"
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color.Red, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Location",
            tint = Color.White,
            modifier = Modifier.size(50.dp)
        )
    }
}
```

---

## 2. Custom Icons & BitmapDescriptor

### 2.1 Icon Resource Loading

Using `BitmapDescriptorFactory` to create icon sources:

```kotlin
// From drawable resource
val greenPinIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_start)

// From bitmap
val customBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_custom_marker)
val bitmapIcon = BitmapDescriptorFactory.fromBitmap(customBitmap)

// Default marker with hue color tinting
val redMarker = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
val blueMarker = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)

// Custom hue (0-360 degrees on color wheel)
val hueGreen = 120f // Green
val customHueMarker = BitmapDescriptorFactory.defaultMarker(hueGreen)
```

### 2.2 Recommended Icon Setup for BikeRedlights Route View

For a ride history detail view, implement three marker types:

```kotlin
// In ViewModel or utility object
object RideMapMarkers {

    fun getStartMarker(context: Context): BitmapDescriptor {
        return BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_GREEN  // Green for start
        )
    }

    fun getEndMarker(context: Context): BitmapDescriptor {
        return BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_RED    // Red for end
        )
    }

    fun getCurrentMarker(context: Context): BitmapDescriptor {
        return BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_BLUE   // Blue for current location
        )
    }
}

// Usage in composable
Marker(
    state = rememberMarkerState(position = ride.startLocation),
    icon = RideMapMarkers.getStartMarker(context),
    title = "Start",
    snippet = ride.startTime.format(dateFormatter)
)

Marker(
    state = rememberMarkerState(position = ride.endLocation),
    icon = RideMapMarkers.getEndMarker(context),
    title = "End",
    snippet = ride.endTime.format(dateFormatter)
)
```

### 2.3 Converting Vector Drawables to BitmapDescriptor

Vector drawables (VectorDrawables) cannot be directly converted to `BitmapDescriptor`. Current workaround:

```kotlin
// Option 1: Use .toBitmap() extension (requires androidx.core.graphics)
val vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_location)
val bitmap = (vectorDrawable as? VectorDrawable)?.toBitmap()
val icon = bitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }

// Option 2: Create custom bitmap from Composable (best for complex icons)
fun composableToBitmapDescriptor(
    context: Context,
    content: @Composable () -> Unit
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val width = (100 * density).toInt()
    val height = (100 * density).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Render composable to canvas
    val view = ComposeView(context).apply {
        setContent(content)
    }
    view.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )
    view.layout(0, 0, width, height)
    view.draw(canvas)

    return BitmapDescriptor.fromBitmap(bitmap)
}

// Usage
val customIcon = composableToBitmapDescriptor(context) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color.Red, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("S", color = Color.White, fontWeight = FontWeight.Bold)
    }
}
```

### 2.4 Color Hue Reference

Default marker hue values (0-360 degrees):

```kotlin
object MarkerHues {
    const val HUE_RED = 0f
    const val HUE_ORANGE = 30f
    const val HUE_YELLOW = 60f
    const val HUE_GREEN = 120f
    const val HUE_CYAN = 180f
    const val HUE_BLUE = 240f
    const val HUE_MAGENTA = 300f
}

// Convert hex color to hue
fun hexColorToHue(hexColor: String): Float {
    val color = Color.parseColor(hexColor)
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)
    return hsv[0]  // Return hue component (0-360)
}
```

---

## 3. Auto-Zoom with LatLngBounds

### 3.1 LatLngBounds.Builder Pattern

Build bounds from a list of coordinates:

```kotlin
// Build bounds from list of locations
fun buildBoundsFromLocations(locations: List<LatLng>): LatLngBounds {
    val boundsBuilder = LatLngBounds.Builder()
    locations.forEach { boundsBuilder.include(it) }
    return boundsBuilder.build()
}

// Usage
val trackPoints = ride.trackPoints.map { LatLng(it.latitude, it.longitude) }
val bounds = buildBoundsFromLocations(trackPoints)
```

### 3.2 CameraPositionState Animation

Two approaches to fit bounds in viewport:

#### 3.2a Instant Movement

```kotlin
val cameraPositionState = rememberCameraPositionState()

LaunchedEffect(bounds) {
    cameraPositionState.move(
        update = CameraUpdateFactory.newLatLngBounds(
            bounds,
            paddingPx = 100  // Padding in pixels around bounds
        )
    )
}
```

#### 3.2b Smooth Animation (Preferred)

```kotlin
val cameraPositionState = rememberCameraPositionState()

LaunchedEffect(bounds) {
    cameraPositionState.animate(
        update = CameraUpdateFactory.newLatLngBounds(
            bounds,
            paddingPx = 100
        ),
        durationMs = 1000  // Animation duration in milliseconds
    )
}
```

### 3.3 Padding Considerations

Padding is used to inset the bounds from map edges:

```kotlin
// Pixel-based padding (recommended)
// Insets the bounds by 100 pixels on all sides
CameraUpdateFactory.newLatLngBounds(bounds, 100)

// Width/height variant (useful during layout)
CameraUpdateFactory.newLatLngBounds(
    bounds,
    mapWidthPx = 1080,
    mapHeightPx = 1920,
    paddingPx = 100
)

// Responsive padding based on screen size
val screenWidth = context.resources.displayMetrics.widthPixels
val paddingPercent = 0.1f  // 10% padding
val paddingPx = (screenWidth * paddingPercent).toInt()
```

### 3.4 Complete Auto-Zoom Implementation

```kotlin
@Composable
fun RideDetailMap(
    ride: Ride,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()
    val context = LocalContext.current

    // Build bounds whenever track points change
    LaunchedEffect(ride.trackPoints) {
        if (ride.trackPoints.isNotEmpty()) {
            val locations = ride.trackPoints.map {
                LatLng(it.latitude, it.longitude)
            }

            val boundsBuilder = LatLngBounds.Builder()
            locations.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()

            // Animate to bounds
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
                durationMs = 1000
            )
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Start marker
        Marker(
            state = rememberMarkerState(position = LatLng(ride.startLat, ride.startLng)),
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
            title = "Start",
            snippet = ride.startTime.toString()
        )

        // End marker
        Marker(
            state = rememberMarkerState(position = LatLng(ride.endLat, ride.endLng)),
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
            title = "End",
            snippet = ride.endTime.toString()
        )

        // Track polyline
        Polyline(
            points = ride.trackPoints.map { LatLng(it.latitude, it.longitude) },
            color = Color.Blue,
            width = 5f
        )
    }
}
```

---

## 4. Edge Cases & Handling

### 4.1 Single Point Routes

When start and end locations are identical, LatLngBounds fails:

```kotlin
fun calculateBoundsForRoute(
    startPoint: LatLng,
    endPoint: LatLng,
    trackPoints: List<LatLng>
): CameraUpdate {
    val allPoints = listOfNotNull(startPoint, endPoint) + trackPoints

    return if (allPoints.isEmpty()) {
        // Default: world view
        CameraUpdateFactory.newCameraPosition(
            CameraPosition(LatLng(0.0, 0.0), 2f, 0f, 0f)
        )
    } else if (allPoints.size == 1) {
        // Single point: center on it with fixed zoom
        CameraUpdateFactory.newCameraPosition(
            CameraPosition(allPoints[0], 17f, 0f, 0f)  // Zoom level 17 for street detail
        )
    } else if (allPoints[0] == allPoints[allPoints.size - 1]) {
        // Same start/end: single point
        CameraUpdateFactory.newCameraPosition(
            CameraPosition(allPoints[0], 17f, 0f, 0f)
        )
    } else {
        // Multiple distinct points: fit bounds
        val boundsBuilder = LatLngBounds.Builder()
        allPoints.forEach { boundsBuilder.include(it) }
        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
    }
}

// Usage
LaunchedEffect(ride.trackPoints) {
    cameraPositionState.animate(
        update = calculateBoundsForRoute(
            startPoint = LatLng(ride.startLat, ride.startLng),
            endPoint = LatLng(ride.endLat, ride.endLng),
            trackPoints = ride.trackPoints.map { LatLng(it.latitude, it.longitude) }
        ),
        durationMs = 1000
    )
}
```

### 4.2 Very Short Routes (< 100m)

Short routes may cause excessive zoom:

```kotlin
fun calculateZoomLevelForDistance(distanceKm: Float): Float {
    return when {
        distanceKm < 0.1f -> 18f    // Street detail
        distanceKm < 1f -> 16f      // Block view
        distanceKm < 5f -> 15f      // Neighborhood
        distanceKm < 20f -> 13f     // City view
        else -> 11f                 // Regional view
    }
}

// Constrain zoom during bounds animation
LaunchedEffect(ride) {
    val bounds = buildBoundsFromLocations(trackPoints)
    val zoomLimit = calculateZoomLevelForDistance(ride.distanceKm)

    // Get camera update with bounds
    val boundsUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)

    // Calculate camera position from bounds, then constrain zoom
    val position = CameraPosition(
        target = bounds.center,
        zoom = minOf(boundsUpdate.zoom, zoomLimit),  // Cap zoom level
        bearing = 0f,
        tilt = 0f
    )

    cameraPositionState.animate(
        update = CameraUpdateFactory.newCameraPosition(position),
        durationMs = 1000
    )
}
```

### 4.3 Very Long Routes (100+ km)

Constrain zoom to avoid looking at entire world:

```kotlin
fun calculateBoundsForLongRoute(
    trackPoints: List<LatLng>,
    maxZoom: Float = 12f
): CameraUpdate {
    val boundsBuilder = LatLngBounds.Builder()
    trackPoints.forEach { boundsBuilder.include(it) }
    val bounds = boundsBuilder.build()

    // newLatLngBounds automatically calculates appropriate zoom
    // This natural zoom respects the bounds
    return CameraUpdateFactory.newLatLngBounds(bounds, 100)
}
```

### 4.4 Handling Empty or Missing Data

```kotlin
@Composable
fun RideDetailMapSafe(
    ride: Ride?,
    modifier: Modifier = Modifier
) {
    if (ride == null || ride.trackPoints.isEmpty()) {
        // Show placeholder
        Box(
            modifier = modifier.fillMaxSize().background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text("No route data available")
        }
        return
    }

    RideDetailMap(ride = ride, modifier = modifier)
}
```

---

## 5. Camera Animation Details

### 5.1 Move vs. Animate

```kotlin
// Instant movement (no animation)
cameraPositionState.move(
    update = CameraUpdateFactory.newLatLngBounds(bounds, 100)
)

// Smooth animation (preferred for UX)
cameraPositionState.animate(
    update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
    durationMs = 1000  // Duration in milliseconds (default ~1000)
)
```

### 5.2 Camera Position Properties

```kotlin
data class CameraPosition(
    val target: LatLng,          // Latitude/longitude center
    val zoom: Float,             // Zoom level (0=world, 21=max)
    val bearing: Float,          // Rotation (0-360 degrees from true north)
    val tilt: Float              // Perspective tilt (0-90 degrees, 0=overhead)
)

// Examples
CameraPosition(
    target = LatLng(37.422, -122.084),
    zoom = 15f,
    bearing = 0f,        // North pointing up
    tilt = 0f            // Overhead view
)

CameraPosition(
    target = LatLng(37.422, -122.084),
    zoom = 18f,
    bearing = 45f,       // Rotated 45° clockwise
    tilt = 45f           // 45° perspective view (3D effect)
)
```

### 5.3 Animation Duration Recommendations

```kotlin
// Based on distance
fun getAnimationDuration(distanceKm: Float): Int {
    return when {
        distanceKm < 1f -> 500        // Quick pan (< 0.5 sec)
        distanceKm < 10f -> 1000      // Normal pan (1 sec)
        distanceKm < 100f -> 1500     // Slower pan (1.5 sec)
        else -> 2000                  // Smooth global transition (2 sec)
    }
}
```

---

## 6. Marker Performance Considerations

### 6.1 When to Use MarkerComposable vs. Marker

| Feature | Marker | MarkerComposable | MarkerInfoWindow |
|---------|--------|------------------|------------------|
| **Best for** | Simple pins with icons | Custom UI (badges, text) | Fully custom windows |
| **Performance** | Fastest | Good (composables rendered as bitmap) | Good |
| **Flexibility** | Icons only | Full Compose | Full UI design |
| **Use case** | Start/end/current pins | Numbered clusters, speed indicators | Custom ride info |

```kotlin
// For simple colored pins
Marker(
    state = markerState,
    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
)

// For markers with text/badges
MarkerComposable(
    state = markerState
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.Red, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("1", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// For rich info windows
MarkerInfoWindow(
    state = markerState
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Start: ${ride.startTime}")
            Text("Elevation: ${ride.maxElevation}m")
        }
    }
}
```

### 6.2 Large Route Optimization

For routes with 1000+ track points:

```kotlin
// Sample points for polyline rendering (1000+ points slow rendering)
fun sampleTrackPoints(
    points: List<LatLng>,
    maxPoints: Int = 1000
): List<LatLng> {
    if (points.size <= maxPoints) return points

    val samplingRate = points.size / maxPoints
    return points.filterIndexed { index, _ -> index % samplingRate == 0 }
}

// Usage
val sampledPoints = sampleTrackPoints(ride.trackPoints.map { LatLng(it.lat, it.lng) })
Polyline(points = sampledPoints, color = Color.Blue, width = 5f)
```

### 6.3 Marker Clustering (Future: For 50+ Markers)

For future expansion with multiple routes:

```kotlin
// Import clustering library
// implementation("com.google.maps.android:maps-compose-utils:1.4.0")

// Cluster markers when displaying multiple routes
val markerClusterManager = MarkerClusterManager()

// Alternative: Server-side clustering for even better performance
// Group markers on server, send summary clusters to app
```

---

## 7. Implementation Checklist for BikeRedlights

### For Ride Detail/History Screen

- [ ] Create three marker types (start/green, end/red, optional current/blue)
- [ ] Implement LatLngBounds auto-zoom with smooth animation
- [ ] Handle single-point routes (start == end)
- [ ] Handle empty track data gracefully
- [ ] Add polyline for ride path
- [ ] Test with various route lengths (100m to 100+ km)
- [ ] Add info windows or snippets to markers
- [ ] Test on emulator with different zoom levels
- [ ] Verify smooth animation on different devices
- [ ] Add accessibility labels to markers

### Recommended Code Structure

```kotlin
// In ui/screens/ride_detail/RideDetailMapScreen.kt
@Composable
fun RideDetailMapScreen(
    rideId: String,
    viewModel: RideDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ride by viewModel.ride.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            ride == null -> LoadingScreen()
            ride?.trackPoints.isEmpty() -> ErrorScreen("No route data")
            else -> RideDetailMap(ride!!)
        }
    }
}

@Composable
fun RideDetailMap(ride: Ride) {
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(ride.trackPoints) {
        animateToRouteBounds(
            cameraPositionState = cameraPositionState,
            ride = ride
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Markers
        StartMarker(ride)
        EndMarker(ride)

        // Route polyline
        if (ride.trackPoints.isNotEmpty()) {
            Polyline(
                points = ride.trackPoints.map { LatLng(it.latitude, it.longitude) },
                color = Color.Blue,
                width = 5f
            )
        }
    }
}

private suspend fun animateToRouteBounds(
    cameraPositionState: CameraPositionState,
    ride: Ride
) {
    // Implementation from section 4.1
}

@Composable
private fun StartMarker(ride: Ride) {
    Marker(
        state = rememberMarkerState(position = LatLng(ride.startLat, ride.startLng)),
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
        title = "Start",
        snippet = ride.startTime.format(dateTimeFormatter)
    )
}

@Composable
private fun EndMarker(ride: Ride) {
    Marker(
        state = rememberMarkerState(position = LatLng(ride.endLat, ride.endLng)),
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
        title = "End",
        snippet = ride.endTime.format(dateTimeFormatter)
    )
}
```

---

## 8. Dependencies Required

Add to `build.gradle.kts`:

```kotlin
dependencies {
    // Maps Compose library
    implementation("com.google.maps.android:maps-compose:4.3.0")

    // Google Play Services Location (for location types)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Utilities (optional, for clustering future)
    implementation("com.google.maps.android:maps-compose-utils:1.4.0")

    // Core Android/Compose dependencies (likely already present)
    implementation("androidx.compose.ui:ui:${composeBom}")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core:1.12.0")
}
```

---

## 9. Key Learnings & Best Practices

1. **Use `animate()` not `move()`** for smooth camera transitions (better UX)
2. **Handle single-point routes** explicitly to avoid LatLngBounds errors
3. **Add padding to bounds** (100px recommended) to avoid markers at screen edges
4. **MarkerComposable over Marker** when custom icon rendering needed
5. **Sample large polylines** (1000+ points) to prevent performance degradation
6. **Test on emulator** with various route lengths before production
7. **Provide accessibility labels** on all markers for screen readers
8. **Keep animation duration** between 500-2000ms depending on distance
9. **Use HUE values** for simple colored markers (green/red/blue)
10. **Test configuration changes** - marker state doesn't persist across rotation

---

## 10. References

- [Google Maps Compose Library](https://github.com/googlemaps/android-maps-compose)
- [Marker API Documentation](https://googlemaps.github.io/android-maps-compose/maps-compose/com.google.maps.android.compose/-marker.html)
- [BitmapDescriptorFactory Reference](https://developers.google.com/maps/documentation/android-sdk/reference/com/google/android/libraries/maps/model/BitmapDescriptorFactory)
- [Camera and View Control](https://developers.google.com/maps/documentation/android-sdk/views)
- [Maps SDK for Android Documentation](https://developers.google.com/maps/documentation/android-sdk)

---

**Research completed:** November 8, 2025
**Status:** Ready for implementation in ride detail/history screens
