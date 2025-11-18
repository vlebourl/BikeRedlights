# Data Model: Stop Detection Settings

**Feature**: 008-stop-detection-settings
**Date**: 2025-11-18
**Phase**: 1 - Design

## Overview

This document defines the data model for Stop Detection Settings. The feature introduces one new domain entity (StopDetectionConfig) and extends existing repository interfaces for persistence.

## Domain Model

### StopDetectionConfig

**Purpose**: Configuration bundle for stop detection parameters that will be consumed by future features (Feature 009: Stop Detection, Feature 010: Clustering).

**Type**: Immutable data class with validation

**Package**: `com.example.bikeredlights.domain.model.settings`

**Fields**:

| Field Name | Type | Range/Values | Default | Description |
|------------|------|--------------|---------|-------------|
| `speedThresholdKmh` | `Float` | 1.0, 2.0, 3.0, 4.0, 5.0 | 3.0 | Minimum speed (km/h) below which rider is considered stopped |
| `durationThresholdSeconds` | `Int` | 5, 10, 15, 20, 25, 30 | 15 | Minimum time (seconds) stationary required to count as a stop |
| `clusteringRadiusMeters` | `Int` | 10, 15, 20, 25, 30, 40, 50 | 20 | Distance (meters) within which stops are grouped as same location |

**Validation Rules**:
- `speedThresholdKmh` MUST be one of the valid values (enforced in `init` block)
- `durationThresholdSeconds` MUST be one of the valid values (enforced in `init` block)
- `clusteringRadiusMeters` MUST be one of the valid values (enforced in `init` block)
- Invalid values throw `IllegalArgumentException` with descriptive message

**Kotlin Representation**:

```kotlin
package com.example.bikeredlights.domain.model.settings

/**
 * Configuration for stop detection parameters.
 *
 * Used by:
 * - Feature 009 (Stop Detection): speed and duration thresholds
 * - Feature 010 (Clustering): clustering radius
 *
 * @param speedThresholdKmh Speed below which rider is considered stopped (1-5 km/h)
 * @param durationThresholdSeconds Minimum time stationary to count as stop (5-30 seconds)
 * @param clusteringRadiusMeters Distance to group stops as same location (10-50 meters)
 * @throws IllegalArgumentException if any value is not in the valid set
 */
data class StopDetectionConfig(
    val speedThresholdKmh: Float = DEFAULT_SPEED_THRESHOLD_KMH,
    val durationThresholdSeconds: Int = DEFAULT_DURATION_THRESHOLD_SECONDS,
    val clusteringRadiusMeters: Int = DEFAULT_CLUSTERING_RADIUS_METERS
) {
    companion object {
        /** Default speed threshold: 3 km/h (~1.9 mph, typical walking pace) */
        const val DEFAULT_SPEED_THRESHOLD_KMH = 3f

        /** Default duration threshold: 15 seconds (filters brief slow-downs) */
        const val DEFAULT_DURATION_THRESHOLD_SECONDS = 15

        /** Default clustering radius: 20 meters (typical intersection size) */
        const val DEFAULT_CLUSTERING_RADIUS_METERS = 20

        /** Valid speed threshold options (km/h) */
        val VALID_SPEED_THRESHOLDS = listOf(1f, 2f, 3f, 4f, 5f)

        /** Valid duration threshold options (seconds) */
        val VALID_DURATION_THRESHOLDS = listOf(5, 10, 15, 20, 25, 30)

        /** Valid clustering radius options (meters) */
        val VALID_CLUSTERING_RADII = listOf(10, 15, 20, 25, 30, 40, 50)
    }

    init {
        require(speedThresholdKmh in VALID_SPEED_THRESHOLDS) {
            "Speed threshold must be one of $VALID_SPEED_THRESHOLDS km/h, got $speedThresholdKmh"
        }
        require(durationThresholdSeconds in VALID_DURATION_THRESHOLDS) {
            "Duration threshold must be one of $VALID_DURATION_THRESHOLDS seconds, got $durationThresholdSeconds"
        }
        require(clusteringRadiusMeters in VALID_CLUSTERING_RADII) {
            "Clustering radius must be one of $VALID_CLUSTERING_RADII meters, got $clusteringRadiusMeters"
        }
    }
}
```

**Design Rationale**:
- **Immutable data class**: Thread-safe, predictable state
- **Validation in init block**: Fail-fast on invalid configuration (domain layer responsibility)
- **Companion object constants**: Type-safe defaults, centralized valid values
- **Descriptive exceptions**: Clear error messages for debugging

---

## Repository Contract

### SettingsRepository (Interface Extension)

**Location**: `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepository.kt`

**Existing Interface**: Already has methods for `unitsSystem`, `gpsAccuracy`, `autoPauseConfig`, `rideSortPreference`

**New Methods**:

```kotlin
interface SettingsRepository {
    // ... existing methods ...

    /**
     * Reactive stream of stop detection configuration.
     * Emits default values on first read if not yet set.
     * Emits new values whenever configuration changes.
     */
    val stopDetectionConfig: Flow<StopDetectionConfig>

    /**
     * Update stop detection configuration.
     * Change persists immediately to DataStore.
     *
     * @param config New stop detection configuration
     * @throws IllegalArgumentException if config contains invalid values (validated by StopDetectionConfig)
     */
    suspend fun setStopDetectionConfig(config: StopDetectionConfig)
}
```

**Contract Guarantees**:
1. **stopDetectionConfig Flow** emits default values (3 km/h, 15s, 20m) if no prior settings exist
2. **stopDetectionConfig Flow** emits immediately when `setStopDetectionConfig()` is called
3. **setStopDetectionConfig()** persists to DataStore atomically (all 3 values or none)
4. **setStopDetectionConfig()** validates via `StopDetectionConfig` constructor (throws on invalid input)
5. **Reads are non-blocking** (Flow-based)
6. **Writes are async** (suspend function, non-blocking)

---

## Persistence Model (DataStore)

### PreferencesKeys

**Location**: `app/src/main/java/com/example/bikeredlights/data/local/datastore/PreferencesKeys.kt`

**New Keys**:

```kotlin
object PreferencesKeys {
    // ... existing keys ...

    /** Speed threshold for stop detection (km/h) */
    val STOP_DETECTION_SPEED_THRESHOLD_KMH = floatPreferencesKey("stop_detection_speed_threshold_kmh")

    /** Duration threshold for stop detection (seconds) */
    val STOP_DETECTION_DURATION_THRESHOLD_SECONDS = intPreferencesKey("stop_detection_duration_threshold_seconds")

    /** Clustering radius for grouping stops (meters) */
    val STOP_DETECTION_CLUSTERING_RADIUS_METERS = intPreferencesKey("stop_detection_clustering_radius_meters")
}
```

**Storage Format**:
- Keys are independent (no grouping/nesting in DataStore)
- Values stored as primitives (Float, Int, Int)
- Default values applied in repository read logic (not stored explicitly)

**Mapping to Domain Model**:

| DataStore Key | Domain Field | Type | Storage Type |
|---------------|--------------|------|--------------|
| `stop_detection_speed_threshold_kmh` | `speedThresholdKmh` | Float | `floatPreferencesKey` |
| `stop_detection_duration_threshold_seconds` | `durationThresholdSeconds` | Int | `intPreferencesKey` |
| `stop_detection_clustering_radius_meters` | `clusteringRadiusMeters` | Int | `intPreferencesKey` |

---

## Data Flow

### Read Flow (ViewModel → UI)

```text
1. UI subscribes to ViewModel.stopDetectionConfig (StateFlow)
2. ViewModel exposes SettingsRepository.stopDetectionConfig (Flow)
3. Repository reads DataStore Preferences
4. If keys exist: map to StopDetectionConfig
5. If keys missing: return StopDetectionConfig() with defaults
6. Flow emits to ViewModel StateFlow
7. UI updates with current config
```

### Write Flow (UI → Repository)

```text
1. User selects new value in StopDetectionSettingsScreen
2. UI calls ViewModel.updateStopDetectionConfig(newConfig)
3. ViewModel calls SettingsRepository.setStopDetectionConfig(newConfig)
4. Repository validates via StopDetectionConfig constructor
5. Repository writes all 3 values to DataStore atomically
6. DataStore emits change event
7. Flow propagates to ViewModel StateFlow
8. UI updates to reflect saved value
```

### First Launch (No Prior Settings)

```text
1. UI loads StopDetectionSettingsScreen
2. Repository reads DataStore (keys don't exist)
3. Repository returns StopDetectionConfig() with defaults
   - speedThresholdKmh = 3f
   - durationThresholdSeconds = 15
   - clusteringRadiusMeters = 20
4. UI displays default values
5. User changes value
6. Write flow persists new values to DataStore
```

---

## Relationships to Other Models

### Dependencies

| Model | Relationship | Purpose |
|-------|--------------|---------|
| `UnitsSystem` | Used for display conversion | StopDetectionSettingsScreen converts km/h ↔ mph based on UnitsSystem setting |

**Note**: StopDetectionConfig is **independent** from other settings models. It has no foreign key relationships or bidirectional dependencies.

### Future Consumption

| Feature | Consumes | Purpose |
|---------|----------|---------|
| Feature 009 (Stop Detection) | `speedThresholdKmh`, `durationThresholdSeconds` | Detect when rider is stopped at red lights |
| Feature 010 (Clustering) | `clusteringRadiusMeters` | Group nearby stops as same intersection |

---

## Validation & Error Handling

### Domain Layer Validation

**Location**: `StopDetectionConfig.init` block

**Rules**:
```kotlin
require(speedThresholdKmh in VALID_SPEED_THRESHOLDS) { "..." }
require(durationThresholdSeconds in VALID_DURATION_THRESHOLDS) { "..." }
require(clusteringRadiusMeters in VALID_CLUSTERING_RADII) { "..." }
```

**Error Type**: `IllegalArgumentException`

**When Thrown**: On construction of `StopDetectionConfig` with invalid values

### UI Layer Prevention

**Controls**: SegmentedButtonSetting only allows selection from valid options

**Guarantees**: UI **cannot** send invalid values to ViewModel (no free-form input)

**Defense in Depth**: Even if UI bug occurs, domain layer validation catches invalid data

### Repository Layer

**Responsibility**: Persistence only (no additional validation)

**Assumption**: `StopDetectionConfig` already validated before reaching repository

**Error Propagation**: If domain validation fails, exception propagates to ViewModel and UI

---

## State Transitions

**Stop Detection Config has no internal state machine** - it's a simple value object. State transitions occur at the **settings level** (not persisted → persisted):

```text
State 1: No settings exist
- DataStore keys absent
- Repository returns defaults (3 km/h, 15s, 20m)

Transition: User saves configuration
- ViewModel calls setStopDetectionConfig(newConfig)
- Repository writes keys to DataStore

State 2: Settings persisted
- DataStore keys present
- Repository returns saved values
- Future app launches read persisted values
```

**No transitional states** - writes are atomic (all 3 keys written together).

---

## Type Safety & Nullability

**All fields are non-nullable**:
- `speedThresholdKmh: Float` (not `Float?`)
- `durationThresholdSeconds: Int` (not `Int?`)
- `clusteringRadiusMeters: Int` (not `Int?`)

**Rationale**: Defaults always available, no "uninitialized" state

**DataStore Handling**:
```kotlin
// Repository read logic
val speed = preferences[STOP_DETECTION_SPEED_THRESHOLD_KMH]
    ?: StopDetectionConfig.DEFAULT_SPEED_THRESHOLD_KMH
```

**No null checks in UI or ViewModel** - config is always complete.

---

## Testing Considerations

### Unit Tests (Domain Layer)

**File**: `app/src/test/java/com/example/bikeredlights/domain/model/settings/StopDetectionConfigTest.kt`

**Test Cases**:
- ✅ Default constructor creates valid config with defaults
- ✅ Valid values accepted (e.g., 2 km/h, 10s, 30m)
- ✅ Invalid speed threshold throws IllegalArgumentException
- ✅ Invalid duration threshold throws IllegalArgumentException
- ✅ Invalid clustering radius throws IllegalArgumentException
- ✅ Boundary values handled (1 km/h, 5 km/h, 5s, 30s, 10m, 50m)

### Repository Tests (Mock DataStore)

**Test Cases**:
- ✅ stopDetectionConfig Flow emits defaults when no keys exist
- ✅ stopDetectionConfig Flow emits saved values when keys exist
- ✅ setStopDetectionConfig() persists all 3 keys
- ✅ setStopDetectionConfig() triggers Flow emission
- ✅ Invalid config throws exception (propagated from domain layer)

### UI Tests (Compose)

**Test Cases**:
- ✅ Default values displayed on first launch
- ✅ Selecting new value updates UI immediately
- ✅ Values persist across configuration changes (rotation)
- ✅ Imperial units display correct mph conversion

---

## Summary

**New Entities**: 1 domain model (StopDetectionConfig)

**Modified Entities**: 2 interfaces (SettingsRepository + SettingsRepositoryImpl)

**Storage Keys**: 3 DataStore preferences

**Validation**: Domain layer (init block)

**Relationships**: None (independent model)

**Future Integration**: Read-only by Features 009 and 010

---

**Data Model Complete**. Ready for contracts generation.
