# Data Model: Auto-Resume Bug Fix (Feature 004)

**Date**: 2025-11-07
**Feature**: Fix Auto-Resume Not Working After Auto-Pause
**Spec**: [spec.md](./spec.md)
**Plan**: [plan.md](./plan.md)

## Overview

This bug fix **does not require any schema changes** or new data models. All necessary entities and fields already exist in the v0.3.0 database schema. This document describes the existing data model that supports auto-pause/auto-resume functionality to ensure the fix preserves data integrity.

---

## Existing Entities

### 1. Ride Entity

**Table**: `rides`
**Purpose**: Stores aggregate statistics for completed cycling rides

**Schema** (Room entity):
```kotlin
@Entity(
    tableName = "rides",
    indices = [Index(value = ["start_time"], name = "idx_rides_start_time")]
)
data class Ride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "start_time")
    val startTime: Long,  // Unix timestamp in milliseconds

    @ColumnInfo(name = "end_time")
    val endTime: Long?,   // Nullable for incomplete rides

    @ColumnInfo(name = "elapsed_duration_millis")
    val elapsedDurationMillis: Long,  // Total elapsed time

    @ColumnInfo(name = "moving_duration_millis")
    val movingDurationMillis: Long,  // Time spent moving (excludes all pauses)

    @ColumnInfo(name = "manual_paused_duration_millis")
    val manualPausedDurationMillis: Long = 0,  // Time in manual pause

    @ColumnInfo(name = "auto_paused_duration_millis")
    val autoPausedDurationMillis: Long = 0,  // Time in auto-pause ✅ **CRITICAL FIELD**

    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double,

    @ColumnInfo(name = "avg_speed_mps")
    val avgSpeedMetersPerSec: Double,

    @ColumnInfo(name = "max_speed_mps")
    val maxSpeedMetersPerSec: Double
)
```

**Fields Relevant to Auto-Resume Fix**:
- `autoPausedDurationMillis`: Accumulates time spent in AutoPaused state
  - **Bug Impact**: Currently not accumulating correctly because auto-resume logic never executes
  - **Fix Impact**: Properly accumulates duration when `checkAutoResume()` is called

**Relationships**:
- One Ride has many TrackPoints (one-to-many with CASCADE delete)

**Validation Rules**:
- `startTime` must be > 0
- `endTime` must be null (incomplete ride) or > `startTime` (completed ride)
- `elapsedDurationMillis` = `endTime - startTime` (when completed)
- `movingDurationMillis` = `elapsedDurationMillis - manualPausedDurationMillis - autoPausedDurationMillis`
- `avgSpeedMetersPerSec` = `distanceMeters / (movingDurationMillis / 1000.0)`
- `maxSpeedMetersPerSec` >= 0.0

---

### 2. TrackPoint Entity

**Table**: `track_points`
**Purpose**: Stores GPS coordinate points captured during rides

**Schema** (Room entity):
```kotlin
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = Ride::class,
            parentColumns = ["id"],
            childColumns = ["ride_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ride_id"], name = "idx_track_points_ride_id"),
        Index(value = ["timestamp"], name = "idx_track_points_timestamp")
    ]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ride_id")
    val rideId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,  // Unix timestamp in milliseconds

    @ColumnInfo(name = "latitude")
    val latitude: Double,  // Decimal degrees, range: -90.0 to 90.0

    @ColumnInfo(name = "longitude")
    val longitude: Double,  // Decimal degrees, range: -180.0 to 180.0

    @ColumnInfo(name = "speed_mps")
    val speedMetersPerSec: Double,  // Meters per second, >= 0.0

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,  // GPS accuracy radius in meters, must be <= 50.0

    @ColumnInfo(name = "is_manually_paused")
    val isManuallyPaused: Boolean = false,  // True if manual pause active

    @ColumnInfo(name = "is_auto_paused")
    val isAutoPaused: Boolean = false  // True if auto-pause active ✅ **CRITICAL FIELD**
)
```

**Fields Relevant to Auto-Resume Fix**:
- `isAutoPaused`: Flags track points captured during auto-pause
  - **Current Behavior**: Set to `true` when ride is AutoPaused ✅ Works correctly
  - **Fix Impact**: No changes needed; flag continues to work as designed
- `speedMetersPerSec`: Used for auto-resume detection
  - **Current Behavior**: Recorded from GPS `Location.speed` ✅ Works correctly
  - **Fix Impact**: `checkAutoResume()` reads this value to detect movement > 1 km/h

**Relationships**:
- Many TrackPoints belong to one Ride (foreign key with CASCADE delete)

**Validation Rules**:
- `rideId` must reference an existing `Ride.id`
- `latitude` in range [-90.0, 90.0]
- `longitude` in range [-180.0, 180.0]
- `speedMetersPerSec` >= 0.0
- `accuracy` <= 50.0 meters (points with poorer accuracy are rejected before insertion)
- `isManuallyPaused` and `isAutoPaused` cannot both be `true` simultaneously

**Indices**:
- `idx_track_points_ride_id`: Optimizes queries filtering by ride (e.g., `getAllPointsForRide()`)
- `idx_track_points_timestamp`: Optimizes chronological queries (e.g., distance calculation)

---

## 3. RideRecordingState (In-Memory State, not persisted to Room)

**Purpose**: Represents current state of ride recording service

**Schema** (Kotlin sealed class):
```kotlin
sealed class RideRecordingState {
    data object Idle : RideRecordingState()
    data class Recording(val rideId: Long) : RideRecordingState()
    data class ManuallyPaused(val rideId: Long) : RideRecordingState()
    data class AutoPaused(val rideId: Long) : RideRecordingState()  // ✅ **CRITICAL STATE**
    data class Stopped(val rideId: Long) : RideRecordingState()
}
```

**States Relevant to Auto-Resume Fix**:
- `AutoPaused(rideId)`: Represents ride in auto-paused state
  - **Current Bug**: Service enters this state but never exits it automatically
  - **Fix Impact**: `checkAutoResume()` transitions from `AutoPaused` → `Recording` when speed > 1 km/h

**State Transitions** (Fixed by this bug fix):
```
Recording → AutoPaused  (auto-pause logic, works ✅)
AutoPaused → Recording  (auto-resume logic, BROKEN ❌ → FIXED ✅)
```

**Persistence**:
- Stored in DataStore Preferences as JSON string for process death recovery
- Repository: `RideRecordingStateRepositoryImpl` manages in-memory StateFlow + DataStore sync

---

## 4. AutoPauseConfig (Settings Model)

**Purpose**: User-configurable auto-pause settings

**Schema** (Kotlin data class):
```kotlin
data class AutoPauseConfig(
    val enabled: Boolean = false,  // Feature toggle
    val thresholdSeconds: Int = 30  // Duration threshold (5-60 seconds)
) {
    companion object {
        val VALID_THRESHOLDS = listOf(5, 10, 15, 20, 30, 45, 60)
        val DEFAULT = AutoPauseConfig(enabled = false, thresholdSeconds = 30)
    }
}
```

**Fields Relevant to Auto-Resume Fix**:
- `enabled`: Must be `true` for auto-resume to execute
  - **Fix Impact**: `checkAutoResume()` checks this flag before proceeding
- `thresholdSeconds`: Not used by auto-resume (only affects auto-pause detection)

**Persistence**:
- Stored in DataStore Preferences (key-value pairs)
- Repository: `SettingsRepository` provides `Flow<AutoPauseConfig>`

**Constants**:
- **Auto-Pause Speed Threshold**: < 1 km/h (0.278 m/s) - hardcoded in service
- **Auto-Resume Speed Threshold**: > 1 km/h (0.278 m/s) - hardcoded in service
  - **Rationale**: Using same threshold for symmetry (1 km/h is effective boundary between stationary and moving)

---

## Data Flow During Auto-Resume

### Step-by-Step Data Operations

**1. GPS Location Update Received**:
```
LocationRepository.getLocationUpdates() emits LocationData(
    latitude = 37.422,
    longitude = -122.084,
    speedMetersPerSec = 1.5,  // > 1 km/h → should trigger auto-resume
    accuracy = 12.0
)
```

**2. Track Point Recorded** (existing behavior, works correctly):
```kotlin
recordTrackPointUseCase(
    rideId = currentRideId,
    locationData = locationData,
    isManuallyPaused = false,
    isAutoPaused = true  // ✅ Flag set correctly
)
```
→ `TrackPoint` inserted with `isAutoPaused=true`, `speedMetersPerSec=1.5`

**3. Auto-Resume Check** (**NEW** - added by this fix):
```kotlin
if (isAutoPaused) {
    checkAutoResume(rideId, locationData.speedMetersPerSec)
}
```

**4. Inside `checkAutoResume()`**:

a. **Read Auto-Pause Config**:
```kotlin
val autoPauseConfig = settingsRepository.autoPauseConfig.first()
if (!autoPauseConfig.enabled) return  // Feature disabled, exit
```

b. **Check Speed Threshold**:
```kotlin
val resumeThreshold = 0.278  // 1 km/h in m/s
if (currentSpeed >= resumeThreshold) {  // 1.5 >= 0.278 → true
    // Proceed to auto-resume
}
```

c. **Accumulate Auto-Pause Duration** (database update):
```kotlin
if (autoPauseStartTime > 0) {
    val autoPausedDuration = System.currentTimeMillis() - autoPauseStartTime
    val ride = rideRepository.getRideById(rideId)
    if (ride != null) {
        val updatedRide = ride.copy(
            autoPausedDurationMillis = ride.autoPausedDurationMillis + autoPausedDuration
        )
        rideRepository.updateRide(updatedRide)  // ✅ Room UPDATE operation
    }
}
```

d. **Transition State** (in-memory + DataStore update):
```kotlin
currentState = RideRecordingState.Recording(rideId)
rideRecordingStateRepository.updateRecordingState(currentState)
```
→ `RideRecordingState` changes from `AutoPaused(rideId)` to `Recording(rideId)`
→ StateFlow emits new state → ViewModel observes → UI updates

e. **Update Notification**:
```kotlin
val notification = buildNotification("Recording...")
notificationManager.notify(NOTIFICATION_ID, notification)
```

**5. Distance Calculation Resumes** (existing behavior):
```kotlin
if (!isManuallyPaused && !isAutoPaused) {  // Now isAutoPaused=false after auto-resume
    updateRideDistance(rideId)  // ✅ Executes normally
}
```

---

## Database Queries

### Queries Used During Auto-Resume (all existing, no new queries)

**1. Get Current Ride**:
```sql
SELECT * FROM rides WHERE id = ?
```
Used to fetch current `autoPausedDurationMillis` before updating.

**2. Update Ride Duration**:
```sql
UPDATE rides
SET auto_paused_duration_millis = ?
WHERE id = ?
```
Used to accumulate auto-paused duration when auto-resuming.

**3. Get Latest Track Point** (not directly used by auto-resume, but related):
```sql
SELECT * FROM track_points
WHERE ride_id = ?
ORDER BY timestamp DESC
LIMIT 1
```
Used by distance calculation to get previous point for Haversine formula.

---

## Data Integrity Guarantees

### Invariants Preserved by Fix

1. **Duration Arithmetic**:
   ```
   elapsedDurationMillis = endTime - startTime
   movingDurationMillis = elapsedDurationMillis - manualPausedDurationMillis - autoPausedDurationMillis
   ```
   ✅ **Preserved**: Auto-resume correctly accumulates `autoPausedDurationMillis` before transitioning

2. **Pause State Exclusivity**:
   ```
   !(isManuallyPaused && isAutoPaused)  // Both cannot be true
   ```
   ✅ **Preserved**: Auto-resume only triggers when `isManuallyPaused=false` and `isAutoPaused=true`

3. **Track Point Chronology**:
   ```
   timestamp[n] < timestamp[n+1]  // Track points are strictly increasing in time
   ```
   ✅ **Preserved**: GPS updates continue during auto-pause; chronological order maintained

4. **Foreign Key Integrity**:
   ```
   TrackPoint.rideId → Ride.id (CASCADE delete)
   ```
   ✅ **Preserved**: No schema changes; CASCADE behavior unaffected

---

## Schema Version

**Current Version**: 1 (v0.3.0)
**Changes Required for Auto-Resume Fix**: **NONE**

All necessary fields (`autoPausedDurationMillis`, `isAutoPaused`) already exist. No migrations needed.

---

## Testing Data Scenarios

### Test Case 1: Single Auto-Pause/Resume Cycle

**Setup**:
1. Start ride at `t=0`
2. Record at normal speed for 60 seconds
3. Stop at `t=60`, trigger auto-pause after 5 seconds (`t=65`)
4. Resume movement at `t=75`, trigger auto-resume immediately

**Expected Data**:
- `autoPausedDurationMillis` = 10,000ms (from `t=65` to `t=75`)
- Track points from `t=60` to `t=75` have `isAutoPaused=true`
- Track points after `t=75` have `isAutoPaused=false`

### Test Case 2: Multiple Auto-Pause/Resume Cycles

**Setup**:
1. Auto-pause for 10 seconds
2. Auto-resume and ride for 30 seconds
3. Auto-pause for 15 seconds
4. Auto-resume and ride for 20 seconds

**Expected Data**:
- `autoPausedDurationMillis` = 25,000ms (10s + 15s)
- Two distinct sequences of `isAutoPaused=true` track points

### Test Case 3: Manual Resume Overrides Auto-Resume

**Setup**:
1. Ride enters auto-pause at `t=100`
2. User manually taps Resume at `t=105` (before auto-resume would trigger)

**Expected Data**:
- `autoPausedDurationMillis` = 5,000ms (from `t=100` to `t=105`)
- State transitions: `AutoPaused → Recording` via manual action (not auto-resume)
- Grace period starts at `t=105` (prevents immediate re-auto-pause)

---

## Summary

This bug fix is **data-model neutral**. No schema changes, no migrations, no new entities. The fix operates entirely within the existing data model by:

1. Reading `AutoPauseConfig` from DataStore ✅
2. Reading `TrackPoint.speedMetersPerSec` from GPS updates ✅
3. Updating `Ride.autoPausedDurationMillis` via existing DAO methods ✅
4. Transitioning `RideRecordingState` via existing repository ✅

All database operations use existing Room DAOs. All data integrity constraints are preserved.
