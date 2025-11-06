# Data Model: Core Ride Recording

**Feature**: F1A - Core Ride Recording
**Date**: 2025-11-04
**Branch**: 002-core-ride-recording

---

## Overview

This document defines the data entities for persistent ride storage in Room database. All entities follow Clean Architecture principles with domain models separate from data layer DTOs.

---

## Entity Relationships

```
Ride (1) ----< TrackPoint (many)
  id           └─ rideId (FK, CASCADE)
```

**Relationship Type**: One-to-Many
- One Ride has many TrackPoints (GPS coordinates)
- Foreign key with CASCADE delete (deleting Ride auto-deletes its TrackPoints)
- Indexed on rideId for query performance

---

## 1. Ride Entity

### Purpose
Represents a single cycling session with aggregate statistics and metadata.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | Primary Key, Auto-generate | Unique ride identifier |
| `name` | String | NOT NULL | Auto-generated: "Ride on Nov 4, 2025" |
| `startTime` | Long | NOT NULL | Unix timestamp (milliseconds) when ride started |
| `endTime` | Long | Nullable | Unix timestamp when ride ended; NULL = incomplete/active |
| `elapsedDurationMillis` | Long | NOT NULL, >= 0 | Total time from start to end (includes pauses) |
| `movingDurationMillis` | Long | NOT NULL, >= 0 | Active riding time (excludes all pauses) |
| `manualPausedDurationMillis` | Long | NOT NULL, >= 0 | Total time spent manually paused |
| `autoPausedDurationMillis` | Long | NOT NULL, >= 0 | Total time spent auto-paused |
| `distanceMeters` | Double | NOT NULL, >= 0.0 | Total distance traveled in meters |
| `avgSpeedMetersPerSec` | Double | NOT NULL, >= 0.0 | Average speed = distance / movingDuration |
| `maxSpeedMetersPerSec` | Double | NOT NULL, >= 0.0 | Maximum speed achieved during ride |

### Validation Rules

```kotlin
// Name generation
fun generateRideName(startTime: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val date = startTime.toLocalDateTime(TimeZone.currentSystemDefault())
    return "Ride on ${date.format(formatter)}"
}

// Duration constraints
require(movingDurationMillis <= elapsedDurationMillis) {
    "Moving time cannot exceed elapsed time"
}
require(movingDurationMillis == elapsedDurationMillis - manualPausedDurationMillis - autoPausedDurationMillis) {
    "Moving time must equal elapsed time minus pause durations"
}

// Speed constraints
require(avgSpeedMetersPerSec >= 0.0) { "Average speed cannot be negative" }
require(maxSpeedMetersPerSec >= avgSpeedMetersPerSec) {
    "Max speed must be >= average speed"
}

// Distance constraints
require(distanceMeters >= 0.0) { "Distance cannot be negative" }
```

### State Transitions

```
NULL (doesn't exist)
  ↓ startRide()
INCOMPLETE (endTime = null, elapsedDuration = 0, distance = 0)
  ↓ GPS updates accumulate
IN_PROGRESS (endTime = null, elapsedDuration > 0, distance > 0)
  ↓ stopRide() + save
COMPLETE (endTime != null, all stats finalized)
  ↓ deleteRide()
DELETED (CASCADE deletes TrackPoints)
```

### Indices

- **Primary Index**: `id` (auto-generated)
- **Secondary Index**: `startTime` (for date-range queries in future history screen)

### Room Entity Definition

```kotlin
@Entity(
    tableName = "rides",
    indices = [
        Index(value = ["startTime"], name = "idx_rides_start_time")
    ]
)
data class Ride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "start_time")
    val startTime: Long,  // Unix millis

    @ColumnInfo(name = "end_time")
    val endTime: Long?,   // Nullable for incomplete rides

    @ColumnInfo(name = "elapsed_duration_millis")
    val elapsedDurationMillis: Long,

    @ColumnInfo(name = "moving_duration_millis")
    val movingDurationMillis: Long,

    @ColumnInfo(name = "manual_paused_duration_millis")
    val manualPausedDurationMillis: Long = 0,

    @ColumnInfo(name = "auto_paused_duration_millis")
    val autoPausedDurationMillis: Long = 0,

    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double,

    @ColumnInfo(name = "avg_speed_mps")
    val avgSpeedMetersPerSec: Double,

    @ColumnInfo(name = "max_speed_mps")
    val maxSpeedMetersPerSec: Double
)
```

---

## 2. TrackPoint Entity

### Purpose
Represents a single GPS coordinate captured during a ride at a specific timestamp.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | Primary Key, Auto-generate | Unique track point identifier |
| `rideId` | Long | Foreign Key → rides(id), NOT NULL | Parent ride reference |
| `timestamp` | Long | NOT NULL | Unix timestamp (milliseconds) when point captured |
| `latitude` | Double | NOT NULL, -90.0 to 90.0 | GPS latitude in decimal degrees |
| `longitude` | Double | NOT NULL, -180.0 to 180.0 | GPS longitude in decimal degrees |
| `speedMetersPerSec` | Double | NOT NULL, >= 0.0 | Speed at this point (from Location.getSpeed()) |
| `accuracy` | Float | NOT NULL, > 0.0 | GPS accuracy radius in meters |
| `isManuallyPaused` | Boolean | NOT NULL | Was ride manually paused at this point? |
| `isAutoPaused` | Boolean | NOT NULL | Was ride auto-paused at this point? |

### Validation Rules

```kotlin
// Coordinate validation
require(latitude in -90.0..90.0) {
    "Latitude must be between -90.0 and 90.0"
}
require(longitude in -180.0..180.0) {
    "Longitude must be between -180.0 and 180.0"
}

// Accuracy threshold (don't insert poor-quality points)
require(accuracy <= 50.0f) {
    "GPS accuracy must be <= 50 meters (spec FR-022)"
}

// Speed validation
require(speedMetersPerSec >= 0.0) {
    "Speed cannot be negative"
}

// Pause state validation
require(!(isManuallyPaused && isAutoPaused)) {
    "Cannot be both manually paused and auto-paused simultaneously"
}
```

### Indices

- **Primary Index**: `id` (auto-generated)
- **Foreign Key Index**: `rideId` (for "get all points for ride X" queries)
- **Timestamp Index**: `timestamp` (for time-range queries, chronological ordering)

### Room Entity Definition

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
    indices = [
        Index(value = ["rideId"], name = "idx_track_points_ride_id"),
        Index(value = ["timestamp"], name = "idx_track_points_timestamp")
    ]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ride_id")
    val rideId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,  // Unix millis

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "speed_mps")
    val speedMetersPerSec: Double,

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,

    @ColumnInfo(name = "is_manually_paused")
    val isManuallyPaused: Boolean = false,

    @ColumnInfo(name = "is_auto_paused")
    val isAutoPaused: Boolean = false
)
```

---

## 3. Domain Models (Non-Persisted)

### RideRecordingState

**Purpose**: Runtime state for active recording session (not stored in database).

```kotlin
data class RideRecordingState(
    val rideId: Long,
    val startTime: Instant,

    // Manual pause state
    val isManuallyPaused: Boolean = false,
    val manualPauseStartTime: Instant? = null,
    val accumulatedManualPausedDuration: Duration = Duration.ZERO,

    // Auto-pause state
    val isAutoPaused: Boolean = false,
    val autoPauseStartTime: Instant? = null,
    val accumulatedAutoPausedDuration: Duration = Duration.ZERO,

    // Live statistics
    val currentDistanceMeters: Double = 0.0,
    val currentSpeedMetersPerSec: Float = 0f,
    val maxSpeedMetersPerSec: Float = 0f,

    // Last known location
    val lastTrackPoint: TrackPoint? = null
) {
    val effectivelyPaused: Boolean
        get() = isManuallyPaused || isAutoPaused

    val elapsedDuration: Duration
        get() = Clock.System.now() - startTime

    val movingDuration: Duration
        get() = elapsedDuration - accumulatedManualPausedDuration - accumulatedAutoPausedDuration

    val avgSpeedMetersPerSec: Float
        get() = if (movingDuration.inWholeSeconds > 0) {
            (currentDistanceMeters / movingDuration.inWholeSeconds).toFloat()
        } else 0f
}
```

---

## 4. Data Access Patterns

### Read Operations

**Get Ride by ID**:
```kotlin
@Query("SELECT * FROM rides WHERE id = :rideId")
suspend fun getRideById(rideId: Long): Ride?
```

**Get All Rides** (for future history screen):
```kotlin
@Query("SELECT * FROM rides ORDER BY start_time DESC")
fun getAllRidesFlow(): Flow<List<Ride>>
```

**Get Incomplete Rides** (recovery on app launch):
```kotlin
@Query("SELECT * FROM rides WHERE end_time IS NULL")
suspend fun getIncompleteRides(): List<Ride>
```

**Get TrackPoints for Ride**:
```kotlin
@Query("SELECT * FROM track_points WHERE ride_id = :rideId ORDER BY timestamp ASC")
suspend fun getTrackPointsForRide(rideId: Long): List<TrackPoint>
```

**Get TrackPoints Flow** (live updates during recording):
```kotlin
@Query("SELECT * FROM track_points WHERE ride_id = :rideId ORDER BY timestamp ASC")
fun getTrackPointsForRideFlow(rideId: Long): Flow<List<TrackPoint>>
```

**Get Last TrackPoint** (for distance calculation):
```kotlin
@Query("SELECT * FROM track_points WHERE ride_id = :rideId ORDER BY timestamp DESC LIMIT 1")
suspend fun getLastTrackPoint(rideId: Long): TrackPoint?
```

### Write Operations

**Insert Ride** (returns generated ID):
```kotlin
@Insert(onConflict = OnConflictStrategy.ABORT)
suspend fun insertRide(ride: Ride): Long
```

**Update Ride** (for setting endTime and final stats):
```kotlin
@Update
suspend fun updateRide(ride: Ride)
```

**Insert TrackPoint** (single):
```kotlin
@Insert(onConflict = OnConflictStrategy.ABORT)
suspend fun insertTrackPoint(trackPoint: TrackPoint): Long
```

**Insert TrackPoints** (batch - performance optimization):
```kotlin
@Insert(onConflict = OnConflictStrategy.ABORT)
suspend fun insertAllTrackPoints(trackPoints: List<TrackPoint>)
```

**Delete Ride** (CASCADE deletes TrackPoints automatically):
```kotlin
@Delete
suspend fun deleteRide(ride: Ride)
```

---

## 5. Data Conversion & Units

### Base Units (Stored in Database)

All measurements stored in base SI units for consistency:

- **Distance**: Meters (Double)
- **Speed**: Meters per second (Double)
- **Duration**: Milliseconds (Long)
- **Timestamp**: Unix milliseconds (Long)
- **Coordinates**: Decimal degrees (Double)

### Display Units (UI Layer)

Convert to user's preferred units from F2A settings:

**Metric**:
- Distance: meters → kilometers (÷ 1000)
- Speed: m/s → km/h (× 3.6)

**Imperial**:
- Distance: meters → miles (÷ 1609.34)
- Speed: m/s → mph (× 2.23694)

**Utility Functions**:
```kotlin
object UnitsConverter {
    fun Double.metersToKilometers() = this / 1000.0
    fun Double.metersToMiles() = this / 1609.34
    fun Double.metersPerSecToKmh() = this * 3.6
    fun Double.metersPerSecToMph() = this * 2.23694

    fun Long.millisToDurationString(): String {
        val duration = Duration.milliseconds(this)
        return String.format(
            "%02d:%02d:%02d",
            duration.inWholeHours,
            duration.inWholeMinutes % 60,
            duration.inWholeSeconds % 60
        )
    }
}
```

---

## 6. Database Schema Export

**Location**: `app/schemas/com.example.bikeredlights.data.local.BikeRedlightsDatabase/1.json`

Room automatically exports schema to this directory (configured in build.gradle.kts lines 85-87). This file:
- Must be committed to version control
- Used by MigrationTestHelper for testing future migrations
- Documents database structure history

**Schema Version**: 1
- This is the first database version for BikeRedlights
- Future features requiring schema changes will increment to version 2, 3, etc.
- Migrations required for version 2+ (not for v1)

---

## 7. Data Integrity Constraints

### Foreign Key Enforcement

```kotlin
@Database(
    entities = [Ride::class, TrackPoint::class],
    version = 1,
    exportSchema = true
)
abstract class BikeRedlightsDatabase : RoomDatabase() {
    // ...

    companion object {
        fun build(context: Context): BikeRedlightsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BikeRedlightsDatabase::class.java,
                "bike_redlights.db"
            )
            .setJournalMode(RoomDatabase.JournalMode.WAL)  // Write-Ahead Logging for performance
            .fallbackToDestructiveMigration()  // v1 only - remove for v2+
            .build()
        }
    }
}
```

### Cascade Delete Behavior

**Scenario**: User deletes a ride from history screen (future F3)

**Database Actions**:
1. `rideDao.deleteRide(ride)` called
2. Room executes `DELETE FROM rides WHERE id = ?`
3. Foreign key cascade automatically executes `DELETE FROM track_points WHERE ride_id = ?`
4. Both operations wrapped in transaction (atomic)
5. TrackPoints deleted **before** Ride deleted (foreign key order)

**No application code needed** - handled by database constraint.

### Transaction Handling

**Implicit Transactions** (automatic):
- Single `@Insert`, `@Update`, `@Delete` operations
- `@Query` with multiple statements

**Explicit Transactions** (when needed):
```kotlin
@Transaction
@Query("SELECT * FROM rides WHERE id = :rideId")
suspend fun getRideWithTrackPoints(rideId: Long): RideWithTrackPoints
```

---

## 8. Performance Considerations

### Batch Insert Optimization

**Bad** (3600 disk writes per hour at 1 sample/second):
```kotlin
for (trackPoint in trackPoints) {
    trackPointDao.insertTrackPoint(trackPoint)  // Individual transaction each time
}
```

**Good** (1 disk write per batch):
```kotlin
trackPointDao.insertAllTrackPoints(trackPoints)  // Single transaction
```

**Best** (chunked batching):
```kotlin
trackPoints.chunked(100).forEach { chunk ->
    trackPointDao.insertAllTrackPoints(chunk)  // Balance memory and disk writes
}
```

### Query Optimization

**Indices automatically optimize**:
- Foreign key lookups: `WHERE ride_id = ?` uses `idx_track_points_ride_id`
- Time-range queries: `WHERE start_time > ?` uses `idx_rides_start_time`
- Chronological ordering: `ORDER BY timestamp` uses `idx_track_points_timestamp`

### Memory Management

**Large Datasets**:
- 1-hour ride at 1 sample/second = 3,600 TrackPoints
- Each TrackPoint ≈ 64 bytes = ~230KB total (acceptable)
- Use Flow for large lists to avoid loading all into memory at once

---

## 9. Migration Strategy (Future)

**Version 1 → Version 2** (example for future features):

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Example: Add weather data column
        database.execSQL("ALTER TABLE rides ADD COLUMN temperature_celsius REAL")
    }
}

// In database builder
.addMigrations(MIGRATION_1_2)
```

**Testing Migrations**:
```kotlin
@Test
fun migrate1To2_preservesRideData() {
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BikeRedlightsDatabase::class.java
    )

    val db = helper.createDatabase(TEST_DB, 1).apply {
        execSQL("INSERT INTO rides VALUES (...)")
        close()
    }

    helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

    // Verify data preserved
}
```

---

## 10. Data Retention & Privacy

**Storage Location**: Local device only (no cloud sync in v0.3.0)
- Database file: `/data/data/com.example.bikeredlights/databases/bike_redlights.db`
- Rides stored indefinitely until user deletes
- No automatic data deletion in v0.3.0

**Privacy Considerations**:
- GPS coordinates stored at high precision (6 decimal places)
- Necessary for accurate route reconstruction (future F1B)
- User controls when recording starts/stops (explicit consent)
- Foreground notification ensures awareness of tracking

**Future Enhancements** (F3+):
- User-configurable retention policy (auto-delete rides older than X months)
- Export to GPX (anonymize by reducing coordinate precision)
- Data anonymization options

---

**Data Model Complete**: Ready for contract generation and implementation.
