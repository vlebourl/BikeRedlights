# Data Model: Maps Integration

**Feature**: 006-maps-integration
**Created**: 2025-11-08

---

## Overview

Maps Integration reuses existing data models from Feature 1A (Core Ride Recording). No new database tables or entities are required. This feature only adds UI visualization of existing track point data.

---

## Existing Data Models (No Changes)

### TrackPoint Entity (Feature 1A)

**Already exists** in `app/src/main/java/com/example/bikeredlights/data/local/entity/TrackPoint.kt`

```kotlin
@Entity(tableName = "track_points")
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ride_id")
    val rideId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,  // Unix timestamp in milliseconds

    @ColumnInfo(name = "latitude")
    val latitude: Double,  // Decimal degrees, WGS 84

    @ColumnInfo(name = "longitude")
    val longitude: Double,  // Decimal degrees, WGS 84

    @ColumnInfo(name = "speed_mps")
    val speedMetersPerSec: Double,

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,  // Meters

    @ColumnInfo(name = "is_manually_paused")
    val isManuallyPaused: Boolean = false,

    @ColumnInfo(name = "is_auto_paused")
    val isAutoPaused: Boolean = false
)
```

**Usage in Maps**:
- `latitude` and `longitude` → Converted to `LatLng` coordinates for map display
- `timestamp` → Used for chronological polyline drawing
- Other fields → Not used for visualization (used for ride statistics)

---

## UI Data Models (New for v0.5.0)

### LatLng (Google Maps SDK)

**Third-party model** from `com.google.android.gms.maps.model.LatLng`

```kotlin
data class LatLng(
    val latitude: Double,  // -90.0 to 90.0
    val longitude: Double  // -180.0 to 180.0
)
```

**Usage**:
- Represents a point on the map
- Used for polyline points, marker positions, camera center

**Conversion**:
```kotlin
// TrackPoint → LatLng
val latLng = LatLng(trackPoint.latitude, trackPoint.longitude)

// List<TrackPoint> → List<LatLng>
val latLngList = trackPoints.map { LatLng(it.latitude, it.longitude) }
```

---

### LatLngBounds (Google Maps SDK)

**Third-party model** from `com.google.android.gms.maps.model.LatLngBounds`

```kotlin
data class LatLngBounds(
    val southwest: LatLng,
    val northeast: LatLng
)
```

**Usage**:
- Represents a rectangular area on the map
- Used for auto-zooming to fit entire route
- Calculated from all track points in a ride

**Construction**:
```kotlin
val builder = LatLngBounds.Builder()
trackPoints.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
val bounds = builder.build()
```

---

### CameraPosition (Google Maps SDK)

**Third-party model** from `com.google.android.gms.maps.model.CameraPosition`

```kotlin
data class CameraPosition(
    val target: LatLng,   // Center point
    val zoom: Float,       // Zoom level (1-20)
    val tilt: Float,       // Camera angle (0-90 degrees, not used)
    val bearing: Float     // Rotation (0-360 degrees, not used)
)
```

**Usage**:
- Defines where the map camera is pointing
- Updated in real-time to follow user location
- Animated smoothly during transitions

**Default Values**:
- **Zoom**: 15f (city block level, appropriate for cycling)
- **Tilt**: 0f (north-up orientation)
- **Bearing**: 0f (no rotation)

---

## State Models (ViewModels)

### RideRecordingViewModel State

**New StateFlows added**:
```kotlin
val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()
val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()
```

**Data Flow**:
```
Service → Repository → ViewModel → UI
 (GPS)   (Database)   (StateFlow)  (Compose)
```

**Update Frequency**:
- **Track points**: Updated when new point inserted to database (every 1-4s based on GPS accuracy setting)
- **Current location**: Updated on every GPS location update from service

---

### RideReviewViewModel State

**New StateFlow added**:
```kotlin
val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()
```

**Data Flow**:
```
Database → Repository → ViewModel → UI
 (Query)   (Flow)      (StateFlow)  (Compose)
```

**Loading**:
- Track points loaded once when ride opens
- Not updated in real-time (ride is completed)

---

## Map Component State

### BikeMap Component Parameters

```kotlin
data class BikeMapParams(
    val currentLocation: LatLng?,              // Blue marker position
    val trackPoints: List<LatLng>,             // Polyline coordinates
    val showPolyline: Boolean,                 // Toggle polyline visibility
    val showCurrentLocationMarker: Boolean,    // Toggle current location marker
    val startMarker: LatLng?,                  // Green pin (Review screen only)
    val endMarker: LatLng?,                    // Red flag (Review screen only)
    val cameraFollowsLocation: Boolean,        // Auto-center on currentLocation
    val gesturesEnabled: Boolean,              // Allow zoom/pan
    val onRecenterClick: (() -> Unit)?         // Re-center FAB callback
)
```

**Usage by Screen**:

| Parameter | Live Tab (Idle) | Live Tab (Recording) | Review Screen |
|-----------|----------------|---------------------|---------------|
| currentLocation | ✅ User's location | ✅ User's location | ❌ null |
| trackPoints | ❌ Empty list | ✅ Growing list | ✅ Complete list |
| showPolyline | ❌ false | ✅ true | ✅ true |
| showCurrentLocationMarker | ✅ true | ✅ true | ❌ false |
| startMarker | ❌ null | ❌ null | ✅ First point |
| endMarker | ❌ null | ❌ null | ✅ Last point |
| cameraFollowsLocation | ❌ false | ✅ true | ❌ false |
| gesturesEnabled | ✅ true | ❌ false | ✅ true |
| onRecenterClick | ❌ null | ✅ Callback | ❌ null |

---

## Data Transformations

### TrackPoint → LatLng Conversion

**Utility Function** (`MapUtils.kt`):
```kotlin
fun trackPointsToLatLng(trackPoints: List<TrackPoint>): List<LatLng> {
    return trackPoints.map { LatLng(it.latitude, it.longitude) }
}
```

**Performance**:
- O(n) time complexity
- Constant space (in-place map)
- Memoized in Compose with `remember(trackPoints) { ... }`

**Optimization for Large Lists**:
```kotlin
// For 1000+ points, consider decimation for display
fun simplifyPolyline(points: List<LatLng>, tolerance: Double = 0.0001): List<LatLng> {
    // Douglas-Peucker algorithm (optional for v0.5.0, defer to v0.6.0)
    return points  // No simplification in v0.5.0
}
```

---

### Bounds Calculation

**Utility Function** (`MapUtils.kt`):
```kotlin
fun calculateBounds(points: List<LatLng>): LatLngBounds? {
    if (points.isEmpty()) return null

    val builder = LatLngBounds.Builder()
    points.forEach { builder.include(it) }
    return builder.build()
}
```

**Usage**:
- Auto-zoom Review screen to fit entire route
- Calculate padding (100px) around bounds
- Animate camera to bounds with 1-second duration

---

### Center Calculation

**Utility Function** (`MapUtils.kt`):
```kotlin
fun calculateCenter(points: List<LatLng>): LatLng? {
    if (points.isEmpty()) return null

    var lat = 0.0
    var lng = 0.0

    points.forEach {
        lat += it.latitude
        lng += it.longitude
    }

    return LatLng(lat / points.size, lng / points.size)
}
```

**Usage**:
- Initial camera position when no current location available
- Fallback center for Review screen

---

## Database Queries (No Changes)

Feature 1A already provides all necessary repository methods:

```kotlin
interface TrackPointRepository {
    fun getTrackPointsForRide(rideId: Long): Flow<List<TrackPoint>>
}
```

**Existing Implementation**:
```kotlin
override fun getTrackPointsForRide(rideId: Long): Flow<List<TrackPoint>> {
    return trackPointDao.getTrackPointsForRide(rideId)
}

// Room DAO
@Query("SELECT * FROM track_points WHERE ride_id = :rideId ORDER BY timestamp ASC")
fun getTrackPointsForRide(rideId: Long): Flow<List<TrackPoint>>
```

**No additional queries needed** - maps simply consume existing data.

---

## Memory Considerations

**Estimated Memory Usage**:

**Per Track Point** (Java object overhead):
- TrackPoint entity: ~80 bytes
- LatLng coordinate: ~24 bytes
- Total per point: ~104 bytes

**For Typical Rides**:
- Short ride (100 points): ~10 KB
- Medium ride (500 points): ~50 KB
- Long ride (1000 points): ~100 KB
- Very long ride (2000 points): ~200 KB

**Total Memory Budget**:
- Track points in memory: < 500 KB
- Map tiles (Google SDK): ~20-50 MB (cached by system)
- Total additional: < 100 MB vs. v0.4.2

**Optimization Strategies** (if needed in future):
- Limit visible polyline to recent N points (e.g., last 1000)
- Use polyline simplification (Douglas-Peucker) for display
- Defer to v0.6.0 if performance issues arise

---

## No Schema Changes Required

✅ **No database migrations needed**
✅ **No new tables or columns**
✅ **No breaking changes to existing data**

This feature is purely additive - it only visualizes data that already exists from Feature 1A.

---

**Version**: 1.0 | **Created**: 2025-11-08 | **Last Updated**: 2025-11-08
