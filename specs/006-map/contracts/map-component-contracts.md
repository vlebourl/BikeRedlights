# API Contracts: Map Components

**Feature**: 006-map (Maps Integration)
**Date**: 2025-11-08
**Branch**: `006-map`

## Overview

This document defines the API contracts (interfaces, composable signatures, and use case contracts) for all map-related components in BikeRedlights. These contracts ensure clean separation between UI, domain, and data layers.

---

## Use Case Contracts (Domain Layer)

### GetRoutePolylineUseCase

**Location**: `app/src/main/java/com/example/bikeredlights/domain/usecase/GetRoutePolylineUseCase.kt`

**Purpose**: Convert list of TrackPoints to simplified PolylineData ready for rendering.

**Contract**:
```kotlin
interface GetRoutePolylineUseCase {
    /**
     * Converts track points to simplified polyline data.
     *
     * @param trackPoints List of GPS track points from database
     * @param toleranceMeters Simplification tolerance in meters (default: 10m)
     * @param color Polyline color (default: Material 3 primary)
     * @return PolylineData ready for rendering, or null if no points
     */
    suspend operator fun invoke(
        trackPoints: List<TrackPoint>,
        toleranceMeters: Double = 10.0,
        color: Color = Color.Red
    ): PolylineData?
}
```

**Implementation Requirements**:
1. Convert `List<TrackPoint>` → `List<LatLng>`
2. Apply Douglas-Peucker simplification with tolerance
3. Return `null` if trackPoints is empty
4. Handle edge case: < 3 points (return unsimplified)

**Example Usage**:
```kotlin
class GetRoutePolylineUseCaseImpl @Inject constructor() : GetRoutePolylineUseCase {
    override suspend operator fun invoke(
        trackPoints: List<TrackPoint>,
        toleranceMeters: Double,
        color: Color
    ): PolylineData? {
        if (trackPoints.isEmpty()) return null

        val latLngPoints = trackPoints.toLatLngList()
        val simplified = if (latLngPoints.size > 2) {
            latLngPoints.simplifyRoute(toleranceMeters)
        } else {
            latLngPoints
        }

        return PolylineData(
            points = simplified,
            color = color,
            width = 10f,
            geodesic = true
        )
    }
}
```

---

### CalculateMapBoundsUseCase

**Location**: `app/src/main/java/com/example/bikeredlights/domain/usecase/CalculateMapBoundsUseCase.kt`

**Purpose**: Calculate LatLngBounds for auto-zoom to fit entire route.

**Contract**:
```kotlin
interface CalculateMapBoundsUseCase {
    /**
     * Calculates map bounds to fit all track points with padding.
     *
     * @param trackPoints List of GPS track points
     * @param padding Padding in pixels from screen edges (default: 100)
     * @return MapBounds for auto-zoom, or null if insufficient points
     */
    suspend operator fun invoke(
        trackPoints: List<TrackPoint>,
        padding: Int = 100
    ): MapBounds?
}
```

**Implementation Requirements**:
1. Return `null` if trackPoints is empty or contains single point
2. Build `LatLngBounds` including all track points
3. Return `MapBounds` with calculated bounds and padding
4. Handle very short routes (< 100m): Return null (caller uses fixed zoom)
5. Handle very long routes (> 100km): Auto-bounds handles appropriately

**Example Usage**:
```kotlin
class CalculateMapBoundsUseCaseImpl @Inject constructor() : CalculateMapBoundsUseCase {
    override suspend operator fun invoke(
        trackPoints: List<TrackPoint>,
        padding: Int
    ): MapBounds? {
        if (trackPoints.size < 2) return null  // Can't create bounds with < 2 points

        val builder = LatLngBounds.Builder()
        trackPoints.forEach { point ->
            builder.include(LatLng(point.latitude, point.longitude))
        }

        return MapBounds(
            bounds = builder.build(),
            padding = padding,
            animationDurationMs = 1000
        )
    }
}
```

---

### FormatMapMarkersUseCase

**Location**: `app/src/main/java/com/example/bikeredlights/domain/usecase/FormatMapMarkersUseCase.kt`

**Purpose**: Generate start/end marker data from track points.

**Contract**:
```kotlin
interface FormatMapMarkersUseCase {
    /**
     * Creates start and end marker data from track points.
     *
     * @param trackPoints List of GPS track points
     * @return List of MarkerData (empty if no points, 1 if single point, 2 if multiple)
     */
    suspend operator fun invoke(
        trackPoints: List<TrackPoint>
    ): List<MarkerData>
}
```

**Implementation Requirements**:
1. Return empty list if trackPoints is empty
2. Return single START marker if trackPoints.size == 1
3. Return START + END markers if trackPoints.size > 1
4. Format timestamp for marker snippets
5. Set appropriate title and snippet for each marker

**Example Usage**:
```kotlin
class FormatMapMarkersUseCaseImpl @Inject constructor() : FormatMapMarkersUseCase {
    override suspend operator fun invoke(
        trackPoints: List<TrackPoint>
    ): List<MarkerData> {
        if (trackPoints.isEmpty()) return emptyList()

        val markers = mutableListOf<MarkerData>()

        // Start marker
        val startPoint = trackPoints.first()
        markers.add(
            MarkerData(
                position = LatLng(startPoint.latitude, startPoint.longitude),
                type = MarkerType.START,
                title = "Start",
                snippet = formatTimestamp(startPoint.timestamp)
            )
        )

        // End marker (if different from start)
        if (trackPoints.size > 1) {
            val endPoint = trackPoints.last()
            markers.add(
                MarkerData(
                    position = LatLng(endPoint.latitude, endPoint.longitude),
                    type = MarkerType.END,
                    title = "End",
                    snippet = formatTimestamp(endPoint.timestamp)
                )
            )
        }

        return markers
    }
}
```

---

## ViewModel Contracts

### RideRecordingViewModel (Updated)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`

**New Map-Related State**:
```kotlin
// Current ride track points (for Live tab polyline)
val currentRideTrackPoints: StateFlow<List<TrackPoint>>

// Polyline data for rendering
val polylineData: StateFlow<PolylineData?>

// User's current location
val userLocation: StateFlow<LatLng?>

// Map camera following enabled
val isCameraFollowing: StateFlow<Boolean>
```

**New Methods**:
```kotlin
/**
 * Toggle whether map camera follows user location.
 */
fun toggleCameraFollowing()

/**
 * Update map view state (camera position, zoom, etc.)
 */
fun updateMapViewState(newState: MapViewState)
```

---

### RideDetailViewModel (New)

**Location**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideDetailViewModel.kt`

**Purpose**: Manages state for Review Screen with complete route visualization.

**State**:
```kotlin
// Selected ride details
val ride: StateFlow<Ride?>

// All track points for the ride
val trackPoints: StateFlow<List<TrackPoint>>

// Polyline data (simplified)
val polylineData: StateFlow<PolylineData?>

// Map bounds for auto-zoom
val mapBounds: StateFlow<MapBounds?>

// Start/end markers
val markers: StateFlow<List<MarkerData>>

// Loading state
val isLoading: StateFlow<Boolean>
```

**Methods**:
```kotlin
/**
 * Load ride details and track points by ID.
 *
 * @param rideId Ride database ID
 */
fun loadRide(rideId: Long)
```

---

## Composable Contracts (UI Layer)

### BikeMap Composable

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`

**Purpose**: Reusable Google Maps wrapper with Material 3 theming.

**Signature**:
```kotlin
@Composable
fun BikeMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    mapColorScheme: Int = MapColorScheme.FOLLOW_SYSTEM,
    mapType: Int = MapType.NORMAL,
    uiSettings: MapUiSettings = MapUiSettings(
        compassEnabled = true,
        mapToolbarEnabled = true,
        zoomControlsEnabled = true
    ),
    onMapClick: ((LatLng) -> Unit)? = null,
    content: (@Composable @GoogleMapComposable () -> Unit)? = null
)
```

**Required Parameters**:
- None (all have defaults)

**Optional Parameters**:
- `modifier`: Layout modifier
- `cameraPositionState`: Camera position state (hoisted)
- `mapColorScheme`: Light/dark theme mode
- `mapType`: Map type (NORMAL, SATELLITE, etc.)
- `uiSettings`: UI controls configuration
- `onMapClick`: Click event handler
- `content`: Slot for markers, polylines, etc.

**Example Usage**:
```kotlin
BikeMap(
    modifier = Modifier.fillMaxSize(),
    cameraPositionState = cameraPositionState,
    mapColorScheme = if (isSystemInDarkTheme()) {
        MapColorScheme.DARK
    } else {
        MapColorScheme.LIGHT
    }
) {
    // Content: markers, polylines, etc.
    RoutePolyline(polylineData = polylineData)
    StartEndMarkers(markers = markers)
}
```

---

### RoutePolyline Composable

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/map/RoutePolyline.kt`

**Purpose**: Render route polyline on map.

**Signature**:
```kotlin
@Composable
@GoogleMapComposable
fun RoutePolyline(
    polylineData: PolylineData?,
    modifier: Modifier = Modifier
)
```

**Required Parameters**:
- `polylineData`: Processed polyline data (null if no route)

**Optional Parameters**:
- `modifier`: Layout modifier (unused for map child)

**Behavior**:
- Renders nothing if `polylineData` is null
- Renders `Polyline` composable with data from `polylineData`

**Example Usage**:
```kotlin
GoogleMap(...) {
    RoutePolyline(polylineData = viewModel.polylineData.collectAsState().value)
}
```

---

### StartEndMarkers Composable

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/map/StartEndMarkers.kt`

**Purpose**: Render start and end markers on map.

**Signature**:
```kotlin
@Composable
@GoogleMapComposable
fun StartEndMarkers(
    markers: List<MarkerData>,
    modifier: Modifier = Modifier
)
```

**Required Parameters**:
- `markers`: List of marker data (empty list if no markers)

**Optional Parameters**:
- `modifier`: Layout modifier (unused for map children)

**Behavior**:
- Renders nothing if `markers` is empty
- Renders `Marker` composable for each item in `markers`
- Resolves icon based on `MarkerType` (START/END/CURRENT)

**Example Usage**:
```kotlin
GoogleMap(...) {
    StartEndMarkers(markers = viewModel.markers.collectAsState().value)
}
```

---

### LocationMarker Composable

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt`

**Purpose**: Render current location marker (blue dot) on Live tab.

**Signature**:
```kotlin
@Composable
@GoogleMapComposable
fun LocationMarker(
    location: LatLng?,
    modifier: Modifier = Modifier,
    title: String = "Current Location"
)
```

**Required Parameters**:
- `location`: Current GPS location (null if unavailable)

**Optional Parameters**:
- `modifier`: Layout modifier (unused for map child)
- `title`: Marker title for info window

**Behavior**:
- Renders nothing if `location` is null
- Renders blue marker at `location` with title

**Example Usage**:
```kotlin
GoogleMap(...) {
    LocationMarker(location = viewModel.userLocation.collectAsState().value)
}
```

---

### MapControls Composable

**Location**: `app/src/main/java/com/example/bikeredlights/ui/components/map/MapControls.kt`

**Purpose**: Overlay UI controls for map interaction (zoom, location centering).

**Signature**:
```kotlin
@Composable
fun MapControls(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenterLocation: () -> Unit,
    isCameraFollowing: Boolean = false
)
```

**Required Parameters**:
- `onZoomIn`: Callback to zoom in
- `onZoomOut`: Callback to zoom out
- `onCenterLocation`: Callback to center on location

**Optional Parameters**:
- `modifier`: Layout modifier for positioning
- `isCameraFollowing`: Whether camera is currently following user

**Behavior**:
- Renders zoom +/- buttons (48dp touch targets)
- Renders location FAB (56dp, changes icon based on `isCameraFollowing`)
- Positions controls in bottom-right corner of map

**Example Usage**:
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    BikeMap(...) { /* map content */ }

    MapControls(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        onZoomIn = { /* cameraPositionState.move(zoomIn) */ },
        onZoomOut = { /* cameraPositionState.move(zoomOut) */ },
        onCenterLocation = { viewModel.toggleCameraFollowing() },
        isCameraFollowing = viewModel.isCameraFollowing.collectAsState().value
    )
}
```

---

## Extension Function Contracts

### TrackPoint Extensions

**Location**: `app/src/main/java/com/example/bikeredlights/domain/util/TrackPointExtensions.kt`

**Purpose**: Convert TrackPoint lists to map-compatible types.

**Functions**:
```kotlin
/**
 * Converts list of TrackPoints to LatLng list.
 */
fun List<TrackPoint>.toLatLngList(): List<LatLng>

/**
 * Gets first track point as LatLng (start location).
 * Returns null if list is empty.
 */
fun List<TrackPoint>.startLocation(): LatLng?

/**
 * Gets last track point as LatLng (end location).
 * Returns null if list is empty.
 */
fun List<TrackPoint>.endLocation(): LatLng?
```

---

### LatLng Extensions

**Location**: `app/src/main/java/com/example/bikeredlights/domain/util/LatLngExtensions.kt`

**Purpose**: Simplification and utility functions for LatLng lists.

**Functions**:
```kotlin
/**
 * Simplifies LatLng list using Douglas-Peucker algorithm.
 *
 * @param toleranceMeters Tolerance in meters (default: 10m)
 * @return Simplified list (90% reduction for 10m tolerance)
 */
fun List<LatLng>.simplifyRoute(toleranceMeters: Double = 10.0): List<LatLng>
```

---

### MarkerType Extensions

**Location**: `app/src/main/java/com/example/bikeredlights/domain/model/MarkerType.kt`

**Purpose**: Convert MarkerType to BitmapDescriptor icon.

**Function**:
```kotlin
/**
 * Converts MarkerType to Google Maps icon.
 *
 * @return BitmapDescriptor for the marker type
 */
fun MarkerType.toIcon(): BitmapDescriptor
```

**Implementation**:
```kotlin
fun MarkerType.toIcon(): BitmapDescriptor {
    return when (this) {
        MarkerType.START -> BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_GREEN
        )
        MarkerType.END -> BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_RED
        )
        MarkerType.CURRENT -> BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_BLUE
        )
    }
}
```

---

## Dependency Injection Contracts (Hilt)

### Module: MapModule

**Location**: `app/src/main/java/com/example/bikeredlights/di/MapModule.kt`

**Purpose**: Provide use case dependencies for map functionality.

**Contract**:
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object MapModule {

    @Provides
    fun provideGetRoutePolylineUseCase(): GetRoutePolylineUseCase {
        return GetRoutePolylineUseCaseImpl()
    }

    @Provides
    fun provideCalculateMapBoundsUseCase(): CalculateMapBoundsUseCase {
        return CalculateMapBoundsUseCaseImpl()
    }

    @Provides
    fun provideFormatMapMarkersUseCase(): FormatMapMarkersUseCase {
        return FormatMapMarkersUseCaseImpl()
    }
}
```

---

## Testing Contracts

### Unit Tests

**GetRoutePolylineUseCaseTest**:
```kotlin
class GetRoutePolylineUseCaseTest {
    @Test
    fun `returns null when track points is empty`()

    @Test
    fun `converts track points to LatLng list`()

    @Test
    fun `simplifies polyline with tolerance`()

    @Test
    fun `handles single track point (no simplification)`()

    @Test
    fun `applies correct color and width`()
}
```

**CalculateMapBoundsUseCaseTest**:
```kotlin
class CalculateMapBoundsUseCaseTest {
    @Test
    fun `returns null when track points is empty`()

    @Test
    fun `returns null when single track point`()

    @Test
    fun `calculates bounds for multiple points`()

    @Test
    fun `applies correct padding value`()
}
```

**FormatMapMarkersUseCaseTest**:
```kotlin
class FormatMapMarkersUseCaseTest {
    @Test
    fun `returns empty list when track points is empty`()

    @Test
    fun `returns single START marker for single point`()

    @Test
    fun `returns START and END markers for multiple points`()

    @Test
    fun `formats timestamps correctly in snippets`()
}
```

---

### Instrumented Tests

**BikeMapTest**:
```kotlin
class BikeMapTest {
    @Test
    fun `map renders successfully`()

    @Test
    fun `map applies dark mode style`()

    @Test
    fun `map camera position updates`()
}
```

**RoutePolylineTest**:
```kotlin
class RoutePolylineTest {
    @Test
    fun `polyline renders with correct color`()

    @Test
    fun `polyline renders nothing when data is null`()

    @Test
    fun `polyline applies simplification correctly`()
}
```

**StartEndMarkersTest**:
```kotlin
class StartEndMarkersTest {
    @Test
    fun `markers render at correct positions`()

    @Test
    fun `start marker uses green icon`()

    @Test
    fun `end marker uses red icon`()

    @Test
    fun `markers render nothing when list is empty`()
}
```

---

## Summary

### Use Cases (Domain Layer)
- ✅ **GetRoutePolylineUseCase**: Convert track points → simplified polyline
- ✅ **CalculateMapBoundsUseCase**: Calculate auto-zoom bounds
- ✅ **FormatMapMarkersUseCase**: Generate start/end marker data

### ViewModels
- ✅ **RideRecordingViewModel** (updated): Add map state for Live tab
- ✅ **RideDetailViewModel** (new): Manage Review Screen map state

### Composables (UI Layer)
- ✅ **BikeMap**: Reusable Google Maps wrapper
- ✅ **RoutePolyline**: Render route polyline
- ✅ **StartEndMarkers**: Render start/end markers
- ✅ **LocationMarker**: Render current location marker
- ✅ **MapControls**: Overlay zoom/location controls

### Extensions
- ✅ **TrackPoint.toLatLngList()**: Convert to map coordinates
- ✅ **LatLng.simplifyRoute()**: Douglas-Peucker simplification
- ✅ **MarkerType.toIcon()**: Convert type to Google Maps icon

### Dependency Injection
- ✅ **MapModule**: Provide use case dependencies

All contracts follow Clean Architecture principles with clear separation between UI, domain, and data layers.
