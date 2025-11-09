# Data Model: Maps Integration

**Feature**: 006-map (Maps Integration)
**Date**: 2025-11-08
**Branch**: `006-map`

## Overview

This document defines the data models and entities used for Google Maps integration in BikeRedlights. The feature reuses existing `Ride` and `TrackPoint` entities from Feature 1A (Core Ride Recording) and introduces new domain models for map state management.

---

## Existing Entities (No Changes)

### Ride Entity (Room Database)

**Location**: `app/src/main/java/com/example/bikeredlights/data/local/entity/Ride.kt`

**Purpose**: Represents a completed ride session with metadata (already exists from Feature 1A).

**Fields**:
```kotlin
@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val startTime: Long,      // Unix timestamp (milliseconds)
    val endTime: Long?,       // Unix timestamp (nullable if ongoing)
    val duration: Long,       // Duration in seconds
    val distance: Double,     // Distance in meters
    val avgSpeed: Float,      // Average speed in km/h or mph
    val maxSpeed: Float,      // Maximum speed in km/h or mph

    // Pause tracking
    val totalPausedTime: Long,  // Total paused duration in seconds
    val autoPauseCount: Int,    // Number of auto-pause events
    val manualPauseCount: Int,  // Number of manual pause events

    // Metadata
    val createdAt: Long,        // Creation timestamp
    val updatedAt: Long         // Last update timestamp
)
```

**Used For**:
- Accessing start/end timestamps for ride metadata
- Querying ride list for Review Screen selection
- No map-specific fields needed

**Note**: Maps integration does NOT modify this entity.

---

### TrackPoint Entity (Room Database)

**Location**: `app/src/main/java/com/example/bikeredlights/data/local/entity/TrackPoint.kt`

**Purpose**: Represents a single GPS coordinate recorded during a ride (already exists from Feature 1A).

**Fields**:
```kotlin
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = Ride::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rideId")]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "rideId")
    val rideId: Long,         // Foreign key to rides table

    val latitude: Double,     // GPS latitude
    val longitude: Double,    // GPS longitude
    val timestamp: Long,      // Unix timestamp (milliseconds)
    val speed: Float,         // Speed at this point (km/h or mph)
    val accuracy: Float,      // GPS accuracy in meters

    // State flags
    val isPaused: Boolean,    // True if recorded during pause
    val isManualPause: Boolean, // True if manual pause, false if auto-pause

    // Metadata
    val createdAt: Long       // Creation timestamp
)
```

**Used For**:
- Converting to `LatLng` list for polyline rendering
- Calculating `LatLngBounds` for auto-zoom
- Extracting start/end locations for markers

**Conversion to LatLng**:
```kotlin
// Extension function (implemented in domain layer)
fun List<TrackPoint>.toLatLngList(): List<LatLng> {
    return map { LatLng(it.latitude, it.longitude) }
}
```

**Note**: Maps integration reads this entity (read-only, no modifications).

---

## New Domain Models

### MapViewState (In-Memory State)

**Location**: `app/src/main/java/com/example/bikeredlights/domain/model/MapViewState.kt`

**Purpose**: Represents the current state of the map view, including camera position, zoom level, and viewport bounds. This is ephemeral state (not persisted to database).

**Fields**:
```kotlin
data class MapViewState(
    val cameraPosition: CameraPosition,  // Google Maps CameraPosition
    val isFollowingUser: Boolean = true, // Whether camera follows user location
    val mapType: Int = MapType.NORMAL,   // Map type (NORMAL, SATELLITE, etc.)
    val isDarkMode: Boolean = false      // Dark mode enabled
)
```

**Usage**:
```kotlin
// In ViewModel
private val _mapViewState = MutableStateFlow(
    MapViewState(
        cameraPosition = CameraPosition.fromLatLngZoom(
            LatLng(0.0, 0.0),
            12f
        )
    )
)
val mapViewState: StateFlow<MapViewState> = _mapViewState.asStateFlow()
```

**State Management**:
- Created by `RideRecordingViewModel` for Live tab
- Created by `RideDetailViewModel` for Review Screen
- Updated reactively via StateFlow
- Not persisted (destroyed on app restart)

---

### PolylineData (Processed Route Data)

**Location**: `app/src/main/java/com/example/bikeredlights/domain/model/PolylineData.kt`

**Purpose**: Represents a processed polyline ready for rendering, including simplified coordinates, color, and styling.

**Fields**:
```kotlin
data class PolylineData(
    val points: List<LatLng>,          // Simplified LatLng coordinates
    val color: Color,                  // Polyline color (Material 3 dynamic or static)
    val width: Float = 10f,            // Line width in pixels
    val geodesic: Boolean = true       // Follow Earth's curvature (recommended for long routes)
)
```

**Creation**:
```kotlin
// From TrackPoints via use case
fun createPolylineData(
    trackPoints: List<TrackPoint>,
    color: Color = Color.Red,
    toleranceMeters: Double = 10.0
): PolylineData {
    val simplified = trackPoints
        .toLatLngList()
        .simplifyRoute(toleranceMeters)

    return PolylineData(
        points = simplified,
        color = color,
        width = 10f,
        geodesic = true
    )
}
```

**Usage**:
```kotlin
// In Composable
@Composable
fun RoutePolyline(polylineData: PolylineData) {
    Polyline(
        points = polylineData.points,
        color = polylineData.color,
        width = polylineData.width,
        geodesic = polylineData.geodesic
    )
}
```

---

### MarkerData (Marker Configuration)

**Location**: `app/src/main/java/com/example/bikeredlights/domain/model/MarkerData.kt`

**Purpose**: Represents configuration for a map marker, including position, icon, title, and type.

**Fields**:
```kotlin
enum class MarkerType {
    START,      // Green pin for ride start
    END,        // Red flag for ride end
    CURRENT     // Blue dot for current location
}

data class MarkerData(
    val position: LatLng,
    val type: MarkerType,
    val title: String = "",
    val snippet: String = "",
    val visible: Boolean = true
)
```

**Icon Resolution** (in UI layer):
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

**Creation** (from TrackPoints):
```kotlin
fun createStartEndMarkers(trackPoints: List<TrackPoint>): List<MarkerData> {
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
    val endPoint = trackPoints.last()
    if (trackPoints.size > 1) {
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
```

**Usage**:
```kotlin
// In Composable
@Composable
fun MapMarkers(markers: List<MarkerData>) {
    markers.forEach { markerData ->
        if (markerData.visible) {
            Marker(
                state = MarkerState(position = markerData.position),
                title = markerData.title,
                snippet = markerData.snippet,
                icon = markerData.type.toIcon()
            )
        }
    }
}
```

---

### MapBounds (Auto-Zoom Calculation Result)

**Location**: `app/src/main/java/com/example/bikeredlights/domain/model/MapBounds.kt`

**Purpose**: Represents calculated map bounds for auto-zoom functionality.

**Fields**:
```kotlin
data class MapBounds(
    val bounds: LatLngBounds,      // Google Maps LatLngBounds
    val padding: Int = 100,        // Padding in pixels from screen edges
    val animationDurationMs: Int = 1000  // Animation duration
)
```

**Creation**:
```kotlin
fun calculateMapBounds(
    trackPoints: List<TrackPoint>,
    padding: Int = 100
): MapBounds? {
    if (trackPoints.isEmpty()) return null

    // Handle single point (can't create bounds with identical points)
    if (trackPoints.size == 1) {
        return null  // Caller should use fixed zoom instead
    }

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
```

**Usage**:
```kotlin
// In Composable
LaunchedEffect(mapBounds) {
    mapBounds?.let {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngBounds(
                it.bounds,
                it.padding
            ),
            durationMs = it.animationDurationMs
        )
    }
}
```

---

## Data Flow Diagrams

### Live Tab Real-Time Route Rendering

```
┌─────────────────────────────────┐
│ RideRecordingService            │
│ (GPS location updates)          │
└────────────┬────────────────────┘
             │ Emit new TrackPoint
             ▼
┌─────────────────────────────────┐
│ TrackPointRepository            │
│ (Insert to Room + emit via Flow)│
└────────────┬────────────────────┘
             │ StateFlow<List<TrackPoint>>
             ▼
┌─────────────────────────────────┐
│ GetRoutePolylineUseCase         │
│ (Convert to LatLng + simplify)  │
└────────────┬────────────────────┘
             │ PolylineData
             ▼
┌─────────────────────────────────┐
│ RideRecordingViewModel          │
│ (Expose polylineData StateFlow) │
└────────────┬────────────────────┘
             │ collectAsStateWithLifecycle()
             ▼
┌─────────────────────────────────┐
│ LiveRideScreen Composable       │
│ (Render GoogleMap + Polyline)   │
└─────────────────────────────────┘
```

### Review Screen Complete Route Display

```
┌─────────────────────────────────┐
│ User taps ride in history list  │
└────────────┬────────────────────┘
             │ Navigate with rideId
             ▼
┌─────────────────────────────────┐
│ RideDetailViewModel             │
│ (Load ride + track points)      │
└────────────┬────────────────────┘
             │ Query Room database
             ▼
┌─────────────────────────────────┐
│ RideRepository                  │
│ (Fetch Ride + TrackPoints)      │
└────────────┬────────────────────┘
             │ Ride + List<TrackPoint>
             ▼
┌─────────────────────────────────┐
│ GetRoutePolylineUseCase         │
│ CalculateMapBoundsUseCase       │
│ FormatMapMarkersUseCase         │
└────────────┬────────────────────┘
             │ PolylineData + MapBounds + List<MarkerData>
             ▼
┌─────────────────────────────────┐
│ RideDetailViewModel             │
│ (Expose StateFlows)             │
└────────────┬────────────────────┘
             │ collectAsStateWithLifecycle()
             ▼
┌─────────────────────────────────┐
│ RideDetailScreen Composable     │
│ (Render GoogleMap + Polyline +  │
│  Start/End Markers + Auto-Zoom) │
└─────────────────────────────────┘
```

---

## Database Schema (No Changes)

The existing Room database schema from Feature 1A remains unchanged:

```sql
CREATE TABLE rides (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    startTime INTEGER NOT NULL,
    endTime INTEGER,
    duration INTEGER NOT NULL,
    distance REAL NOT NULL,
    avgSpeed REAL NOT NULL,
    maxSpeed REAL NOT NULL,
    totalPausedTime INTEGER NOT NULL,
    autoPauseCount INTEGER NOT NULL,
    manualPauseCount INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

CREATE TABLE track_points (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rideId INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    timestamp INTEGER NOT NULL,
    speed REAL NOT NULL,
    accuracy REAL NOT NULL,
    isPaused INTEGER NOT NULL,  -- Boolean as INTEGER (0/1)
    isManualPause INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    FOREIGN KEY (rideId) REFERENCES rides(id) ON DELETE CASCADE
);

CREATE INDEX idx_track_points_rideId ON track_points(rideId);
```

**Key Points**:
- ✅ No new tables needed
- ✅ No schema migrations required
- ✅ Existing indices support efficient map queries
- ✅ CASCADE DELETE ensures data consistency

---

## Repository Interfaces (No Changes)

### RideRepository (Existing)

**Location**: `app/src/main/java/com/example/bikeredlights/domain/repository/RideRepository.kt`

**Methods Used by Maps Integration**:
```kotlin
interface RideRepository {
    // Get single ride by ID (for Review Screen)
    suspend fun getRideById(rideId: Long): Ride?

    // Get all track points for a ride (for polyline rendering)
    suspend fun getTrackPointsForRide(rideId: Long): List<TrackPoint>

    // Real-time track points for current ride (Live tab)
    fun getCurrentRideTrackPoints(): Flow<List<TrackPoint>>
}
```

**Note**: These methods already exist from Feature 1A. Maps integration uses them read-only.

---

## Type Aliases & Utility Extensions

### Type Aliases

**Location**: `app/src/main/java/com/example/bikeredlights/domain/model/MapTypeAliases.kt`

```kotlin
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.CameraPosition

// Type aliases for clarity (optional, if desired)
typealias Coordinate = LatLng
typealias ViewportBounds = LatLngBounds
typealias MapCamera = CameraPosition
```

### Extension Functions

**Location**: `app/src/main/java/com/example/bikeredlights/domain/util/MapExtensions.kt`

```kotlin
import com.google.android.gms.maps.model.LatLng

// Convert TrackPoint list to LatLng list
fun List<TrackPoint>.toLatLngList(): List<LatLng> {
    return map { LatLng(it.latitude, it.longitude) }
}

// Simplify polyline using Douglas-Peucker algorithm
fun List<LatLng>.simplifyRoute(toleranceMeters: Double = 10.0): List<LatLng> {
    if (size <= 2) return this  // Can't simplify < 3 points

    // Convert meters to degrees (approximate: 1 degree ≈ 111km)
    val toleranceDegrees = toleranceMeters / 111000.0

    return PolyUtil.simplify(this, toleranceDegrees)
}

// Get start location
fun List<TrackPoint>.startLocation(): LatLng? {
    return firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
}

// Get end location
fun List<TrackPoint>.endLocation(): LatLng? {
    return lastOrNull()?.let { LatLng(it.latitude, it.longitude) }
}

// Format timestamp for marker snippet
fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
```

---

## Summary

### Existing Entities (Unchanged)
- ✅ **Ride**: Room entity with ride metadata
- ✅ **TrackPoint**: Room entity with GPS coordinates

### New Domain Models (Maps-Specific)
- ✅ **MapViewState**: Camera position and map configuration (ephemeral)
- ✅ **PolylineData**: Processed route ready for rendering
- ✅ **MarkerData**: Marker configuration with type/position/title
- ✅ **MapBounds**: Auto-zoom calculation result

### No Database Changes
- ✅ No new tables
- ✅ No schema migrations
- ✅ No modifications to existing entities
- ✅ Read-only access to `rides` and `track_points` tables

### Clean Separation
- **Data Layer**: Room entities (Ride, TrackPoint)
- **Domain Layer**: Business models (MapViewState, PolylineData, MarkerData, MapBounds)
- **UI Layer**: Google Maps SDK types (CameraPosition, LatLng, BitmapDescriptor)

This design maintains Feature 1A's database schema while adding lightweight domain models for map-specific functionality.
