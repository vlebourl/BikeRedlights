# Data Model: Real-Time Speed and Location Tracking

**Feature**: 001-speed-tracking
**Date**: 2025-11-02
**Phase**: Phase 1 - Design & Contracts

## Overview

This document defines the domain models for real-time GPS location tracking and speed measurement. All models are immutable value objects following Kotlin data class best practices and marked with `@Immutable` for Jetpack Compose optimization.

---

## Domain Models

### 1. LocationData

**Purpose**: Represents a single GPS location reading with all relevant metadata.

**Package**: `com.example.bikeredlights.domain.model`

**Definition**:

```kotlin
package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val speedMps: Float? = null,
    val bearing: Float? = null
)
```

**Field Descriptions**:

| Field | Type | Unit | Description | Constraints |
|-------|------|------|-------------|-------------|
| `latitude` | Double | degrees | GPS latitude coordinate | -90.0 to 90.0 |
| `longitude` | Double | degrees | GPS longitude coordinate | -180.0 to 180.0 |
| `accuracy` | Float | meters | Horizontal accuracy radius (68% confidence) | ≥0 |
| `timestamp` | Long | milliseconds | Unix epoch time when location was acquired | >0 |
| `speedMps` | Float? | m/s | Speed from GPS (if available), null if unavailable | ≥0 or null |
| `bearing` | Float? | degrees | Direction of travel (0° = North, clockwise) | 0.0 to 360.0 or null |

**Validation Rules**:
- Latitude must be in range [-90, 90]
- Longitude must be in range [-180, 180]
- Accuracy must be non-negative
- Timestamp must be positive (after Unix epoch)
- Speed, if present, must be non-negative
- Bearing, if present, must be in range [0, 360)

**Usage Context**:
- Emitted by `LocationRepository.getLocationUpdates(): Flow<LocationData>`
- Consumed by `TrackLocationUseCase` to calculate speed
- Displayed in UI for coordinate visualization
- Used for GPS status determination based on accuracy

**Nullability Decisions**:
- `speedMps` is nullable because GPS may not provide speed on first fix or with poor signal
- `bearing` is nullable because bearing unavailable when stationary or with single location fix

---

### 2. SpeedMeasurement

**Purpose**: Represents calculated cycling speed with accuracy metadata and stationary detection.

**Package**: `com.example.bikeredlights.domain.model`

**Definition**:

```kotlin
package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SpeedMeasurement(
    val speedKmh: Float,
    val timestamp: Long,
    val accuracyKmh: Float?,
    val isStationary: Boolean,
    val source: SpeedSource
)

enum class SpeedSource {
    GPS,        // From Location.getSpeed()
    CALCULATED, // From position change (distance / time)
    UNKNOWN     // No speed data available
}
```

**Field Descriptions**:

| Field | Type | Unit | Description | Constraints |
|-------|------|------|-------------|-------------|
| `speedKmh` | Float | km/h | Current speed in kilometers per hour | ≥0 |
| `timestamp` | Long | milliseconds | Unix epoch time of measurement | >0 |
| `accuracyKmh` | Float? | km/h | Speed accuracy (if available from GPS) | ≥0 or null |
| `isStationary` | Boolean | - | True if speed below stationary threshold (<1 km/h) | true/false |
| `source` | SpeedSource | - | How speed was determined | GPS, CALCULATED, or UNKNOWN |

**Validation Rules**:
- Speed must be non-negative (0 km/h minimum)
- Speed clamped to max 100 km/h for cycling (unrealistic values filtered)
- Stationary threshold: <1 km/h considered stationary (filters GPS jitter)
- Timestamp must be positive

**State Transitions**:

```
GPS Signal Lost → source: UNKNOWN, speedKmh: 0
GPS Signal Acquiring → source: CALCULATED (if previous location available)
GPS Signal Active → source: GPS (preferred)
Speed < 1 km/h → isStationary: true, speedKmh: 0
Speed ≥ 1 km/h → isStationary: false, speedKmh: actual value
```

**Usage Context**:
- Produced by `TrackLocationUseCase` from `LocationData`
- Consumed by `SpeedTrackingViewModel` for UI state
- Displayed prominently in main tracking screen
- Used for speed history recording (future feature)

**Business Logic**:
- Conversion: `speedMps * 3.6f = speedKmh`
- Stationary detection prevents showing GPS jitter when cyclist stops
- Source tracking enables displaying accuracy warnings to user

---

### 3. GpsStatus

**Purpose**: Represents the current state of GPS signal availability and quality.

**Package**: `com.example.bikeredlights.domain.model`

**Definition**:

```kotlin
package com.example.bikeredlights.domain.model

sealed interface GpsStatus {
    /**
     * GPS is unavailable (no permission, disabled, or severe signal loss)
     */
    data object Unavailable : GpsStatus

    /**
     * GPS is acquiring signal (no fix yet or low accuracy)
     */
    data object Acquiring : GpsStatus

    /**
     * GPS signal is active with acceptable accuracy
     * @param accuracy Horizontal accuracy in meters
     */
    data class Active(val accuracy: Float) : GpsStatus
}
```

**State Descriptions**:

| State | Meaning | UI Indication | Conditions |
|-------|---------|---------------|------------|
| `Unavailable` | No GPS signal or permission denied | Red indicator, error message | `!hasPermission` OR `accuracy > 50m` OR no location for >10s |
| `Acquiring` | Searching for GPS fix | Yellow/orange indicator, "Acquiring..." | `accuracy` 10-50m OR first location |
| `Active` | GPS working with good accuracy | Green indicator, show accuracy value | `accuracy ≤ 10m` AND regular updates |

**State Transition Rules**:

```
Initial → Acquiring (on app start)
Acquiring → Active (when accuracy ≤ 10m AND updates flowing)
Acquiring → Unavailable (if no fix after 30s timeout)
Active → Acquiring (if accuracy degrades to 10-50m)
Active → Unavailable (if signal lost or accuracy > 50m)
Any → Unavailable (if permission revoked)
```

**Usage Context**:
- Determined by `SpeedTrackingViewModel` from `LocationData.accuracy`
- Displayed in UI as status indicator (icon + text)
- Used to show/hide warning messages about GPS quality
- Blocks speed display when `Unavailable` (shows "GPS Required" instead)

**Accuracy Thresholds** (subject to tuning based on testing):
- **Excellent**: ≤5m (typical outdoor GPS)
- **Good**: 5-10m (acceptable for cycling)
- **Fair**: 10-50m (acquiring or degraded signal)
- **Poor**: >50m (considered unavailable)

---

## Entity Relationships

```
┌─────────────────┐
│ LocationData    │ (from GPS hardware)
│ - lat/lng       │
│ - accuracy      │
│ - speedMps      │
└────────┬────────┘
         │
         │ emitted by Repository Flow
         │
         ▼
┌─────────────────────┐
│ TrackLocationUseCase│ (business logic)
│ - converts m/s → km/h
│ - detects stationary
│ - tracks speed source
└────────┬────────────┘
         │
         │ outputs
         │
         ▼
┌──────────────────┐
│ SpeedMeasurement │ (domain model)
│ - speedKmh       │
│ - isStationary   │
└──────────────────┘
         │
         │ + LocationData.accuracy
         │
         ▼
┌──────────────────┐
│ GpsStatus        │ (derived state)
│ - Unavailable    │
│ - Acquiring      │
│ - Active(acc)    │
└──────────────────┘
```

**Data Flow**:
1. GPS hardware → Android Location Services → FusedLocationProviderClient
2. FusedLocationProviderClient → Repository → `Flow<LocationData>`
3. ViewModel collects Flow → transforms to `StateFlow<UiState>`
4. UiState contains: `SpeedMeasurement` + `LocationData` + `GpsStatus`
5. Compose UI observes `StateFlow` → renders speed/location/status

---

## Conversion Extensions

### LocationData → SpeedMeasurement

```kotlin
fun LocationData.toSpeedMeasurement(previousLocation: LocationData?): SpeedMeasurement {
    val (speedMs, source) = when {
        // Prefer GPS-provided speed
        speedMps != null && speedMps > 0 -> speedMps to SpeedSource.GPS

        // Fallback: calculate from position change
        previousLocation != null -> {
            val elapsedTimeSeconds = (timestamp - previousLocation.timestamp) / 1000.0
            val distanceMeters = distanceTo(previousLocation)
            if (elapsedTimeSeconds > 0) {
                (distanceMeters / elapsedTimeSeconds).toFloat() to SpeedSource.CALCULATED
            } else 0f to SpeedSource.UNKNOWN
        }

        // No speed data available
        else -> 0f to SpeedSource.UNKNOWN
    }

    // Sanitize and convert
    val sanitizedSpeedMs = speedMs.coerceIn(0f, 100f / 3.6f) // Max 100 km/h
    val stationaryThresholdMs = 1f / 3.6f // 1 km/h
    val isStationary = sanitizedSpeedMs < stationaryThresholdMs

    return SpeedMeasurement(
        speedKmh = if (isStationary) 0f else sanitizedSpeedMs * 3.6f,
        timestamp = timestamp,
        accuracyKmh = null, // TODO: Add speedAccuracyMetersPerSecond from Location (API 26+)
        isStationary = isStationary,
        source = source
    )
}

private fun LocationData.distanceTo(other: LocationData): Float {
    val earthRadius = 6371000f // meters
    val dLat = Math.toRadians(other.latitude - latitude)
    val dLon = Math.toRadians(other.longitude - longitude)

    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return (earthRadius * c).toFloat()
}
```

### LocationData → GpsStatus

```kotlin
fun LocationData.toGpsStatus(): GpsStatus {
    return when {
        accuracy > 50f -> GpsStatus.Unavailable // Poor accuracy
        accuracy > 10f -> GpsStatus.Acquiring   // Fair accuracy, still acquiring
        else -> GpsStatus.Active(accuracy)      // Good accuracy
    }
}
```

---

## UI State Model

**Purpose**: Aggregates all domain models for UI rendering.

**Package**: `com.example.bikeredlights.ui.viewmodel`

**Definition**:

```kotlin
package com.example.bikeredlights.ui.viewmodel

import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.domain.model.LocationData
import com.example.bikeredlights.domain.model.SpeedMeasurement

data class SpeedTrackingUiState(
    val speedMeasurement: SpeedMeasurement? = null,
    val locationData: LocationData? = null,
    val gpsStatus: GpsStatus = GpsStatus.Acquiring,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null
)
```

**Field Descriptions**:

| Field | Type | Description | Null Handling |
|-------|------|-------------|---------------|
| `speedMeasurement` | SpeedMeasurement? | Current speed data | null = no GPS data yet, show "---" |
| `locationData` | LocationData? | Current GPS coordinates | null = no GPS data yet, show "Acquiring..." |
| `gpsStatus` | GpsStatus | GPS signal state | Default: Acquiring on app start |
| `hasLocationPermission` | Boolean | Permission granted state | false = show permission request UI |
| `errorMessage` | String? | User-facing error (e.g., "GPS timeout") | null = no error, show normal UI |

---

## Persistence (Out of Scope for MVP)

**Note**: Per spec requirement, MVP has no persistence. Location data is ephemeral and discarded when app backgrounds.

**Future Considerations** (v0.2.0+):
- Add Room entity `LocationEntity` for trip history
- Map `LocationData` → `LocationEntity` for database storage
- Add `TripData` model to group location points into trips
- Consider retention policy (24 hours max per constitution)

---

## Testing Fixtures

### Test Data Builders

```kotlin
// Test fixtures for unit tests
object LocationDataFixtures {
    fun createStationaryLocation(
        latitude: Double = 37.7749,
        longitude: Double = -122.4194,
        accuracy: Float = 5f
    ) = LocationData(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = System.currentTimeMillis(),
        speedMps = 0f,
        bearing = null
    )

    fun createMovingLocation(
        speedKmh: Float = 20f,
        accuracy: Float = 5f
    ) = LocationData(
        latitude = 37.7749,
        longitude = -122.4194,
        accuracy = accuracy,
        timestamp = System.currentTimeMillis(),
        speedMps = speedKmh / 3.6f,
        bearing = 90f
    )

    fun createInaccurateLocation(accuracy: Float = 100f) = LocationData(
        latitude = 37.7749,
        longitude = -122.4194,
        accuracy = accuracy,
        timestamp = System.currentTimeMillis(),
        speedMps = null,
        bearing = null
    )
}

object SpeedMeasurementFixtures {
    fun createStationary() = SpeedMeasurement(
        speedKmh = 0f,
        timestamp = System.currentTimeMillis(),
        accuracyKmh = null,
        isStationary = true,
        source = SpeedSource.GPS
    )

    fun createCyclingSpeed(speedKmh: Float = 25f) = SpeedMeasurement(
        speedKmh = speedKmh,
        timestamp = System.currentTimeMillis(),
        accuracyKmh = 2f,
        isStationary = false,
        source = SpeedSource.GPS
    )
}
```

---

## Validation & Invariants

### Compile-Time Guarantees
- All models are `data class` (structural equality, copy, toString)
- All models are `@Immutable` (Compose optimization)
- Sealed interface for `GpsStatus` (exhaustive when expressions)

### Runtime Validation
- Speed coercion: `coerceIn(0f, 100f)` before displaying
- Coordinate validation: Check ranges before using in map APIs (future)
- Timestamp validation: Ensure positive values

### Defensive Programming
```kotlin
// Example: Safe speed display
fun SpeedMeasurement.displaySpeed(): String {
    return when {
        speedKmh < 0 -> "0 km/h" // Should never happen, but defensive
        speedKmh > 100 -> "-- km/h" // Unrealistic, show error indicator
        isStationary -> "0 km/h"
        else -> "${speedKmh.roundToInt()} km/h"
    }
}
```

---

## Summary

**Total Domain Models**: 3 core + 1 UI state
- **LocationData**: Raw GPS data from Android Location Services
- **SpeedMeasurement**: Calculated cycling speed with metadata
- **GpsStatus**: GPS signal quality state machine
- **SpeedTrackingUiState**: Aggregate for UI rendering

**Key Design Decisions**:
- Immutable value objects for thread safety and Compose optimization
- Sealed interface for type-safe state management
- Nullable fields only where Android API doesn't guarantee values
- Conversion extensions for clear data transformation boundaries
- No persistence for MVP (future enhancement)

**Files to Create**:
1. `/app/src/main/java/com/example/bikeredlights/domain/model/LocationData.kt`
2. `/app/src/main/java/com/example/bikeredlights/domain/model/SpeedMeasurement.kt`
3. `/app/src/main/java/com/example/bikeredlights/domain/model/GpsStatus.kt`
4. `/app/src/main/java/com/example/bikeredlights/ui/viewmodel/SpeedTrackingUiState.kt`
5. `/app/src/test/java/com/example/bikeredlights/domain/model/LocationDataTest.kt`
6. `/app/src/test/java/com/example/bikeredlights/domain/model/SpeedMeasurementTest.kt`
