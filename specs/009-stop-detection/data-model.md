# Data Model: Stop Detection & Recording

**Feature**: 009-stop-detection
**Date**: 2025-11-18
**Source**: Extracted from [spec.md](spec.md) and [research.md](research.md)

## Overview

This document defines the data entities, relationships, validation rules, and state transitions for stop detection during rides. The model supports real-time stop detection, database persistence, and future clustering (Feature 010).

## Core Entities

### 1. Stop (Domain Model)

**Purpose**: Represents a single stationary period detected during a ride.

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | Auto-generated, unique | Primary key, database ID |
| `rideId` | Long | Foreign key to Ride, NOT NULL | Parent ride this stop belongs to |
| `stopNumber` | Int | 1-999, NOT NULL | Sequential number within ride (1, 2, 3...) |
| `latitude` | Double | -90.0 to 90.0, NOT NULL | GPS latitude at stop confirmation (decimal degrees) |
| `longitude` | Double | -180.0 to 180.0, NOT NULL | GPS longitude at stop confirmation (decimal degrees) |
| `startTimestamp` | Long | Unix epoch milliseconds, NOT NULL | When stop was confirmed (duration threshold met) |
| `endTimestamp` | Long? | Unix epoch milliseconds, nullable | When movement resumed (null during active stop) |
| `durationSeconds` | Int? | 0-2147483647, nullable | Calculated duration (null during active stop) |
| `clusterId` | Long? | Foreign key to Cluster, nullable | Assigned by clustering algorithm (Feature 010), NULL initially |

**Validation Rules**:
- `endTimestamp` must be ≥ `startTimestamp` if not null
- `durationSeconds` = `(endTimestamp - startTimestamp) / 1000` when both present
- `stopNumber` must be unique per `rideId` (database unique constraint on pair)
- `latitude` and `longitude` must be valid GPS coordinates
- `clusterId` can only be set by clustering algorithm, never by stop detection

**Relationships**:
- **Belongs To**: One Ride (many-to-one) → CASCADE delete when ride deleted
- **Belongs To**: Zero or One Cluster (many-to-one, optional) → SET NULL when cluster deleted

**Immutability**: Stop records are immutable after creation except:
- `endTimestamp` updated once when stop ends
- `durationSeconds` calculated when `endTimestamp` set
- `clusterId` set by Feature 010 clustering algorithm

---

### 2. StopEntity (Room Entity)

**Purpose**: Room database representation of Stop.

**Table Name**: `stops`

**Schema**:

```sql
CREATE TABLE stops (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    ride_id INTEGER NOT NULL,
    stop_number INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    start_timestamp INTEGER NOT NULL,
    end_timestamp INTEGER,
    duration_seconds INTEGER,
    cluster_id INTEGER,
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    FOREIGN KEY (cluster_id) REFERENCES clusters(id) ON DELETE SET NULL,
    UNIQUE (ride_id, stop_number)
);

CREATE INDEX idx_stops_ride_id ON stops(ride_id);
CREATE INDEX idx_stops_cluster_id ON stops(cluster_id);
CREATE INDEX idx_stops_start_timestamp ON stops(start_timestamp);
```

**Migration Strategy** (v4 → v5):
- Add `stops` table with foreign keys
- Create indexes for query performance
- No data migration needed (new table)

**Mapping**: Room auto-converts between StopEntity ↔ Stop domain model via repository.

---

### 3. StopDetectionState (Runtime State)

**Purpose**: Transient in-memory state for stop detection logic. NOT persisted to database.

**Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `currentSpeed` | Float | Latest GPS speed in km/h (from Location.getSpeed() * 3.6) |
| `speedBelowThresholdCount` | Int | Consecutive seconds speed < threshold (0-3) |
| `speedAboveThresholdCount` | Int | Consecutive seconds speed > threshold during active stop (0-3) |
| `stopTimer` | Long? | Duration in milliseconds since stop confirmed (null when not stopped) |
| `currentStopNumber` | Int | Next stop number to assign (1, 2, 3... per ride) |
| `activeStopId` | Long? | Database ID of current in-progress stop (null when not stopped) |
| `isStopConfirmed` | Boolean | True when duration threshold met, false during detection phase |
| `detectionStartTime` | Long? | Timestamp when speed first dropped below threshold (for duration calculation) |

**State Machine Transitions**:

```
┌──────────┐  speed < threshold     ┌───────────┐  duration >= threshold   ┌───────────┐
│  Moving  │ ──────────────────────>│ Detecting │ ────────────────────────>│ Confirmed │
│          │  (start counting)      │           │  (save to DB, show popup)│           │
└──────────┘                         └───────────┘                          └───────────┘
     ▲                                    │                                       │
     │                                    │ speed > threshold                     │
     │                                    │ OR count < 3                          │
     │                                    ▼                                       │
     │                               ┌──────────┐                                │
     │                               │  Reset   │                                │
     │                               └──────────┘                                │
     │                                                                            │
     │                                 speed > threshold (3 consecutive)         │
     └────────────────────────────────────────────────────────────────────────────┘
                                  (update DB end time, hide popup)
```

**Lifecycle**: Lives in RideRecordingService scope (survives app backgrounding). Reset on ride start/stop.

---

## Entity Relationships

```
┌────────────┐
│   Ride     │
│  (existing)│
└─────┬──────┘
      │ 1
      │
      │ has many
      │
      ▼ *
┌────────────┐       ┌──────────────┐
│   Stop     │──────>│   Cluster    │
│            │ *   0..1 (Feature 010)
└────────────┘       └──────────────┘

CASCADE delete       SET NULL delete
```

**Cascade Rules**:
- Delete Ride → Deletes all associated Stops (CASCADE)
- Delete Cluster → Sets Stop.clusterId to NULL (SET NULL, preserves stop data)

---

## Validation Rules Summary

### Stop Creation (Insert)

**Required**:
- ✅ `rideId` must exist in rides table
- ✅ `stopNumber` must be unique per rideId
- ✅ `latitude` in [-90.0, 90.0]
- ✅ `longitude` in [-180.0, 180.0]
- ✅ `startTimestamp` must be current or recent (within 24 hours of ride start)
- ✅ `endTimestamp` = NULL on creation
- ✅ `durationSeconds` = NULL on creation
- ✅ `clusterId` = NULL on creation

**Rejected**:
- ❌ Cannot create stop with `endTimestamp` already set (must update separately)
- ❌ Cannot create stop with future `startTimestamp`
- ❌ Cannot create stop for non-existent ride

### Stop Update (Ending Stop)

**Required**:
- ✅ `endTimestamp` ≥ `startTimestamp` (cannot end before start)
- ✅ `durationSeconds` = `(endTimestamp - startTimestamp) / 1000`
- ✅ Only update allowed: setting `endTimestamp` and `durationSeconds` (immutable otherwise)

**Rejected**:
- ❌ Cannot update `latitude`, `longitude`, `startTimestamp`, `rideId`, `stopNumber` after creation
- ❌ Cannot set `endTimestamp` = NULL after it's been set (stops are permanent)

---

## State Transitions

### StopDetectionState State Machine

**States**:
1. **Moving**: Default state, speed > threshold, no stop in progress
2. **Detecting**: Speed < threshold, counting consecutive seconds (0-3)
3. **Confirmed**: Duration threshold met, stop persisted, popup visible
4. **Reset**: Intermediate state, counters reset, return to Moving

**Transitions**:

| From State | Event | To State | Side Effects |
|------------|-------|----------|--------------|
| Moving | `speed < threshold` | Detecting | `speedBelowThresholdCount = 1`, `detectionStartTime = now` |
| Detecting | `speed < threshold` (consecutive) | Detecting | `speedBelowThresholdCount++` |
| Detecting | `speed > threshold` | Reset → Moving | Reset counters, clear `detectionStartTime` |
| Detecting | `speedBelowThresholdCount >= 3 AND duration >= threshold` | Confirmed | Insert stop to DB, `activeStopId = newId`, show popup, `currentStopNumber++` |
| Confirmed | `speed > threshold` (3 consecutive) | Moving | Update stop `endTimestamp`, calculate `durationSeconds`, hide popup, reset counters |
| Confirmed | `speed < threshold` | Confirmed | Increment `stopTimer`, update popup duration display |
| Confirmed | Manual pause ride | Moving | Update stop `endTimestamp`, save to DB, reset state |

**Invariants**:
- `speedBelowThresholdCount` and `speedAboveThresholdCount` are never > 3 (capped)
- Only one of the counters can be non-zero at a time (mutually exclusive)
- `activeStopId` is only non-null in Confirmed state
- `isStopConfirmed` = true only in Confirmed state

---

## Database Queries

### StopDao Operations

**Insert** (Create new stop):
```kotlin
@Insert
suspend fun insertStop(stop: StopEntity): Long
```

**Update** (End stop):
```kotlin
@Query("UPDATE stops SET end_timestamp = :endTimestamp, duration_seconds = :durationSeconds WHERE id = :stopId")
suspend fun updateStopEnd(stopId: Long, endTimestamp: Long, durationSeconds: Int)
```

**Query by Ride** (For ride history):
```kotlin
@Query("SELECT * FROM stops WHERE ride_id = :rideId ORDER BY stop_number ASC")
suspend fun getStopsByRideId(rideId: Long): List<StopEntity>
```

**Count Stops in Ride** (For live display):
```kotlin
@Query("SELECT COUNT(*) FROM stops WHERE ride_id = :rideId")
fun getStopCountByRideId(rideId: Long): Flow<Int>
```

**Delete by Ride** (Triggered by CASCADE):
```kotlin
// Automatic via foreign key CASCADE - no explicit DAO method needed
```

**Get Unclustered Stops** (For Feature 010 clustering):
```kotlin
@Query("SELECT * FROM stops WHERE cluster_id IS NULL ORDER BY start_timestamp ASC")
suspend fun getUnclusteredStops(): List<StopEntity>
```

---

## Performance Considerations

### Indexes

**Required Indexes**:
- `idx_stops_ride_id` (foreign key) → Fast CASCADE delete, ride history queries
- `idx_stops_cluster_id` (foreign key) → Fast clustering queries (Feature 010)
- `idx_stops_start_timestamp` → Chronological sorting for clustering

**Query Performance Targets**:
- Insert stop: <50ms (single row)
- Update stop end: <30ms (WHERE id = ?)
- Get stops by ride: <100ms (typical 5-20 stops per ride)
- Count stops by ride: <50ms (indexed query)

### Memory Management

**In-Memory State** (StopDetectionState):
- Size: ~200 bytes (8 primitive fields)
- Lifecycle: Lives in Service scope, reset on ride start
- No Location objects stored (leak risk) → Extract primitives only

**Database Growth**:
- Typical: 50 stops/week/user → 2,600 stops/year/user
- Storage per stop: ~64 bytes → 166KB/year/user
- Manageable growth, no auto-cleanup needed

---

## Data Flow

### Stop Detection → Persistence Flow

```
GPS Location Update (every 1-3s)
    ↓
Extract speed, lat, long
    ↓
StopDetectionStateMachine.processSpeed(speed)
    ↓
State transition (Moving → Detecting → Confirmed)
    ↓
[if Confirmed]
    ↓
StopRepository.insertStop(rideId, stopNumber, lat, long, startTimestamp)
    ↓
Room Database (stops table)
    ↓
Return stopId
    ↓
Update StopDetectionState.activeStopId
    ↓
Emit stop event to ViewModel
    ↓
UI displays stop popup
```

### Stop End Flow

```
GPS Location Update (speed > threshold)
    ↓
StopDetectionStateMachine.processSpeed(speed)
    ↓
speedAboveThresholdCount++
    ↓
[if count >= 3]
    ↓
Calculate endTimestamp, durationSeconds
    ↓
StopRepository.updateStopEnd(activeStopId, endTimestamp, durationSeconds)
    ↓
Room Database UPDATE
    ↓
Reset StopDetectionState (activeStopId = null)
    ↓
Emit stop ended event to ViewModel
    ↓
UI hides stop popup
```

---

## Assumptions

- GPS location updates arrive every 1-3 seconds during ride recording (existing behavior)
- Stop numbers are sequential and gaps are acceptable if stops are deleted
- Clustering (Feature 010) will handle `clusterId` population, never set by this feature
- Room auto-increments `id` reliably (no manual ID assignment)
- Foreign key CASCADE delete is enabled in Room database (must verify in migration)
- Database UNIQUE constraint on (ride_id, stop_number) enforced by SQLite

---

## Future Enhancements (Out of Scope for v0.9.0)

- Accuracy-based filtering (ignore stops with GPS accuracy > 50m)
- Stop merging (if two stops at same location within 1 minute, merge them)
- Stop deletion UI (allow manual deletion of individual stops)
- Stop editing (adjust start/end times, lat/long corrections)
- Export stop data to GPX/KML formats
