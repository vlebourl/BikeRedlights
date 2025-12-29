# Data Model: Stop Clustering

**Feature**: Stop Clustering (Feature 010)
**Date**: 2025-12-29
**Status**: Phase 1 Design

## Overview

Feature 010 extends the existing `stops` table schema (Feature 009) to support geospatial clustering. No new database tables required - clustering leverages the existing `cluster_id` column already present in the stops schema.

**Key Principle**: Clustering is computed, not stored as separate entities. cluster_id is a grouping field, not a foreign key to a clusters table. Cluster statistics (count, centroid, avg duration) are derived via SQL aggregation queries.

---

## Domain Models

### StopCluster (NEW)

**Package**: `com.example.bikeredlights.domain.model`

**Purpose**: Represents aggregated statistics for a group of stops clustered at the same geographic location.

**Lifecycle**: Computed on-demand from database queries, not persisted as entity.

```kotlin
/**
 * Domain model representing aggregated statistics for a cluster of stops.
 *
 * Purpose: Provide analytics for "you stopped at this intersection N times" feature.
 * Not persisted to database - computed via SQL aggregation queries.
 *
 * Immutability:
 * - All properties are val (immutable)
 * - Represents a snapshot of cluster state at query time
 *
 * Validation:
 * - stopCount must be ≥ 1 (cluster must contain at least 1 stop)
 * - centroidLatitude in range [-90.0, 90.0]
 * - centroidLongitude in range [-180.0, 180.0]
 * - averageDuration must be > 0 if stopCount > 0
 * - totalDuration = sum of all stop durations in cluster
 *
 * @property clusterId Unique identifier for this cluster (same as stops.cluster_id)
 * @property stopCount Number of stops assigned to this cluster
 * @property centroidLatitude Geographic center latitude (average of all stop latitudes)
 * @property centroidLongitude Geographic center longitude (average of all stop longitudes)
 * @property averageDuration Average stop duration in seconds across all stops in cluster
 * @property totalDuration Total time spent at this cluster in seconds (sum of all durations)
 * @property earliestStop Timestamp of first stop ever recorded at this cluster (milliseconds)
 * @property latestStop Timestamp of most recent stop at this cluster (milliseconds)
 */
data class StopCluster(
    val clusterId: Long,
    val stopCount: Int,
    val centroidLatitude: Double,
    val centroidLongitude: Double,
    val averageDuration: Int,
    val totalDuration: Int,
    val earliestStop: Long,
    val latestStop: Long
) {
    init {
        require(stopCount >= 1) { "Stop count must be >= 1, got $stopCount" }
        require(centroidLatitude in -90.0..90.0) {
            "Centroid latitude must be in range [-90.0, 90.0], got $centroidLatitude"
        }
        require(centroidLongitude in -180.0..180.0) {
            "Centroid longitude must be in range [-180.0, 180.0], got $centroidLongitude"
        }
        require(averageDuration > 0) {
            "Average duration must be positive, got $averageDuration"
        }
        require(totalDuration > 0) {
            "Total duration must be positive, got $totalDuration"
        }
        require(earliestStop <= latestStop) {
            "Earliest stop ($earliestStop) must be <= latest stop ($latestStop)"
        }
    }

    /**
     * Check if this cluster is "frequent" (stopped here many times).
     * Threshold: 5+ stops considered frequent.
     */
    val isFrequent: Boolean
        get() = stopCount >= 5

    /**
     * Get formatted centroid coordinates (for display).
     * Example: "37.4220, -122.0841"
     */
    fun getFormattedCentroid(): String {
        return "%.4f, %.4f".format(centroidLatitude, centroidLongitude)
    }
}
```

---

## Database Schema (No Changes Required)

### stops Table (Existing from Feature 009)

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
    cluster_id INTEGER,  -- <-- Used by Feature 010 (already exists!)
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE
);

-- Existing indexes (already created in Migration 1→2)
CREATE INDEX index_stops_ride_id ON stops(ride_id);
CREATE INDEX index_stops_cluster_id ON stops(cluster_id);  -- <-- Critical for clustering queries
CREATE INDEX index_stops_start_timestamp ON stops(start_timestamp);
CREATE UNIQUE INDEX index_stops_ride_id_stop_number ON stops(ride_id, stop_number);
```

**cluster_id Column**:
- **Type**: `INTEGER` (nullable)
- **Semantics**: Grouping identifier for stops at same geographic location
- **Nullability**: NULL for unclustered stops (should never happen after clustering runs)
- **Generation**: Assigned by DBSCAN algorithm, auto-incremented per cluster
- **Stability**: May change when re-clustering runs (not a stable foreign key)

**No Migration Needed**: cluster_id column already exists in database version 2 (added in Feature 009 for forward compatibility).

---

## Data Access Layer

### StopDao Extensions (EXTEND existing file)

**File**: `app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt`

**New Methods to Add**:

```kotlin
/**
 * Get all stops for clustering operation.
 *
 * Returns stops ordered by timestamp (oldest first) for deterministic clustering.
 * Includes stops without cluster_id (unclustered) and with cluster_id (already clustered).
 *
 * @return List of all stop entities
 */
@Query("SELECT * FROM stops ORDER BY start_timestamp ASC")
suspend fun getAllStops(): List<StopEntity>

/**
 * Batch update cluster_id for multiple stops.
 *
 * Used by clustering algorithm to assign stops to clusters atomically.
 * More efficient than individual updates for large datasets.
 *
 * @param clusterId Cluster identifier to assign
 * @param stopIds List of stop IDs to update
 */
@Query("""
    UPDATE stops
    SET cluster_id = :clusterId
    WHERE id IN (:stopIds)
""")
suspend fun updateClusterIds(clusterId: Long, stopIds: List<Long>)

/**
 * Get all stops belonging to a specific cluster.
 *
 * Used for cluster details view and manual split/merge operations (P3).
 *
 * @param clusterId Cluster identifier
 * @return List of stops in this cluster
 */
@Query("SELECT * FROM stops WHERE cluster_id = :clusterId ORDER BY start_timestamp ASC")
suspend fun getStopsByClusterId(clusterId: Long): List<StopEntity>

/**
 * Get cluster statistics for a specific cluster.
 *
 * Calculates aggregate metrics via SQL for efficiency.
 * Returns null if cluster doesn't exist or has no stops.
 *
 * @param clusterId Cluster identifier
 * @return Cluster statistics or null
 */
@Query("""
    SELECT
        cluster_id,
        COUNT(*) as stop_count,
        AVG(latitude) as centroid_lat,
        AVG(longitude) as centroid_lon,
        AVG(duration_seconds) as avg_duration,
        SUM(duration_seconds) as total_duration,
        MIN(start_timestamp) as earliest_stop,
        MAX(start_timestamp) as latest_stop
    FROM stops
    WHERE cluster_id = :clusterId
    GROUP BY cluster_id
""")
suspend fun getClusterStats(clusterId: Long): ClusterStatsDto?

/**
 * Get all cluster statistics, ordered by frequency (most stops first).
 *
 * Returns reactive Flow that updates when stops table changes.
 * Used for "Top Intersections" analytics screen.
 *
 * @return Flow of cluster statistics ordered by stop count descending
 */
@Query("""
    SELECT
        cluster_id,
        COUNT(*) as stop_count,
        AVG(latitude) as centroid_lat,
        AVG(longitude) as centroid_lon,
        AVG(duration_seconds) as avg_duration,
        SUM(duration_seconds) as total_duration,
        MIN(start_timestamp) as earliest_stop,
        MAX(start_timestamp) as latest_stop
    FROM stops
    WHERE cluster_id IS NOT NULL
    GROUP BY cluster_id
    ORDER BY stop_count DESC
""")
fun getAllClusterStatsFlow(): Flow<List<ClusterStatsDto>>

/**
 * Reset all cluster_id values to null (clear clustering).
 *
 * Used before full re-clustering to start fresh.
 * Should be called within @Transaction to ensure atomicity.
 */
@Query("UPDATE stops SET cluster_id = NULL")
suspend fun clearAllClusterAssignments()
```

**Data Transfer Object (Internal)**:

```kotlin
/**
 * DTO for cluster statistics query results.
 *
 * Internal to data layer - maps to StopCluster domain model via repository.
 */
data class ClusterStatsDto(
    @ColumnInfo(name = "cluster_id") val clusterId: Long,
    @ColumnInfo(name = "stop_count") val stopCount: Int,
    @ColumnInfo(name = "centroid_lat") val centroidLat: Double,
    @ColumnInfo(name = "centroid_lon") val centroidLon: Double,
    @ColumnInfo(name = "avg_duration") val avgDuration: Double,  // SQL AVG returns REAL
    @ColumnInfo(name = "total_duration") val totalDuration: Int,
    @ColumnInfo(name = "earliest_stop") val earliestStop: Long,
    @ColumnInfo(name = "latest_stop") val latestStop: Long
)

/**
 * Map DTO to domain model.
 */
fun ClusterStatsDto.toDomain(): StopCluster {
    return StopCluster(
        clusterId = clusterId,
        stopCount = stopCount,
        centroidLatitude = centroidLat,
        centroidLongitude = centroidLon,
        averageDuration = avgDuration.toInt(),  // Round to nearest second
        totalDuration = totalDuration,
        earliestStop = earliestStop,
        latestStop = latestStop
    )
}
```

---

## Repository Contract Extensions

### StopRepository (EXTEND interface)

**File**: `app/src/main/java/com/example/bikeredlights/domain/repository/StopRepository.kt`

**New Methods to Add**:

```kotlin
/**
 * Get all stops for clustering operation.
 *
 * Returns domain models ordered by timestamp.
 * Used by ClusterStopsUseCase to fetch data for DBSCAN algorithm.
 *
 * @return List of all stops (domain models)
 */
suspend fun getAllStops(): List<Stop>

/**
 * Update cluster_id assignments for multiple stops atomically.
 *
 * Used by ClusterStopsUseCase after DBSCAN algorithm completes.
 * Transaction ensures all-or-nothing update (prevents partial clustering).
 *
 * @param clusterAssignments Map of cluster_id to list of stop IDs
 */
suspend fun updateClusterAssignments(clusterAssignments: Map<Long, List<Long>>)

/**
 * Get cluster statistics for a specific cluster.
 *
 * Returns aggregated metrics (count, centroid, durations).
 * Returns null if cluster doesn't exist.
 *
 * @param clusterId Cluster identifier
 * @return Cluster statistics or null
 */
suspend fun getClusterStats(clusterId: Long): StopCluster?

/**
 * Get all cluster statistics as reactive Flow.
 *
 * Emits updated list whenever stops table changes.
 * Sorted by frequency (most stops first).
 *
 * @return Flow of cluster statistics
 */
fun getAllClustersFlow(): Flow<List<StopCluster>>

/**
 * Clear all cluster_id assignments (prepare for re-clustering).
 *
 * Sets all cluster_id values to NULL.
 * Used before full re-clustering to start from clean state.
 */
suspend fun clearAllClusterAssignments()
```

---

## State Transitions

### Clustering State Machine

```
┌─────────────────────────────────────────────────┐
│ INITIAL STATE: All stops have cluster_id = NULL │
└─────────────────┬───────────────────────────────┘
                  │
                  │ ClusterStopsUseCase.clusterAllStops()
                  ▼
┌─────────────────────────────────────────────────┐
│ CLUSTERING IN PROGRESS                          │
│ - Fetch all stops from database                 │
│ - Run DBSCAN algorithm (domain layer)           │
│ - Generate cluster_id assignments               │
└─────────────────┬───────────────────────────────┘
                  │
                  │ updateClusterAssignments()
                  ▼
┌─────────────────────────────────────────────────┐
│ CLUSTERED STATE: All stops have cluster_id set  │
│ - Core points: cluster_id = 1, 2, 3, ...        │
│ - Noise points: cluster_id = unique singleton   │
└─────────────────┬───────────────────────────────┘
                  │
                  │ Settings change (radius update)
                  │ OR Manual re-cluster trigger
                  ▼
┌─────────────────────────────────────────────────┐
│ CLEAR CLUSTERING: cluster_id = NULL for all     │
└─────────────────┬───────────────────────────────┘
                  │
                  └──────► (Back to CLUSTERING IN PROGRESS)
```

**State Invariants**:
1. After clustering completes: All stops MUST have cluster_id != NULL
2. During clustering: cluster_id values may be inconsistent (transaction in progress)
3. On re-clustering: Clear all cluster_id first, then re-cluster from scratch

**Atomicity**: All cluster_id updates wrapped in @Transaction to prevent partial updates.

---

## Data Validation Rules

### Stop Entity Validation (Existing)

From Feature 009, stops already validate:
- ✅ Latitude in range [-90.0, 90.0]
- ✅ Longitude in range [-180.0, 180.0]
- ✅ startTimestamp ≤ endTimestamp
- ✅ durationSeconds = (endTimestamp - startTimestamp) / 1000

### Cluster Assignment Validation (New)

**cluster_id Constraints**:
- **Non-null after clustering**: All stops MUST have cluster_id set after ClusterStopsUseCase runs
- **Positive values**: cluster_id > 0 (using auto-increment pattern)
- **No orphaned clusters**: Every cluster_id in stops table MUST have >= 1 stop
- **Stability (optional)**: cluster_id values may change on re-clustering (not guaranteed stable)

**Cluster Statistics Validation**:
- stopCount MUST match actual COUNT(*) from database
- centroidLatitude/Longitude MUST be within bounds of constituent stops
- averageDuration MUST equal totalDuration / stopCount

---

## Performance Considerations

### Clustering Query Performance

**Expected Dataset Sizes**:
- 100 stops: ~10ms query time
- 500 stops: ~30ms query time
- 1000 stops: ~50ms query time

**Index Usage**:
- ✅ `index_stops_cluster_id` used for `WHERE cluster_id = ?` queries
- ✅ `index_stops_start_timestamp` used for `ORDER BY start_timestamp`
- ✅ Full table scan acceptable for `SELECT * FROM stops` (small dataset)

**Batch Update Performance**:
- `updateClusterIds()` with 100 stop IDs: ~5ms
- Using `IN (:stopIds)` clause with prepared statement (efficient)
- Room batches updates automatically for optimal performance

**Aggregation Query Performance**:
- `getClusterStats()` with GROUP BY: ~2-5ms
- SQL aggregation (COUNT, AVG, SUM) is faster than fetching + computing in Kotlin
- No N+1 query problem (single query per cluster)

---

## Testing Strategy

### Unit Tests (Domain Models)

**StopCluster Validation Tests**:
- ✅ Valid cluster with stopCount=5, valid coordinates → success
- ❌ Invalid cluster with stopCount=0 → IllegalArgumentException
- ❌ Invalid cluster with latitude=100.0 → IllegalArgumentException
- ✅ Cluster with earliestStop=latestStop (single stop over time) → success

### Integration Tests (Database)

**StopDao Clustering Query Tests**:
- Insert 50 stops → `getAllStops()` returns 50 stops ordered by timestamp
- Insert 3 clusters (IDs 1,2,3) → `getAllClusterStatsFlow()` emits 3 items
- Update 10 stops with `updateClusterIds(clusterId=5, stopIds=[...])` → verify cluster_id=5
- `getClusterStats(clusterId=2)` → verify COUNT, AVG, SUM calculations correct
- DELETE ride → verify CASCADE deletes stops + cluster_id

**Transaction Tests**:
- Call `updateClusterAssignments()` with 3 clusters → verify atomicity
- Simulate crash mid-transaction → verify no partial updates persisted

### Performance Tests

**Large Dataset Tests**:
- Insert 1000 stops → measure `getAllStops()` query time (< 50ms)
- Batch update 1000 stops → measure `updateClusterIds()` time (< 100ms)
- Query cluster stats for 50 clusters → measure `getAllClusterStatsFlow()` time (< 20ms)

---

## Summary

**Database Changes**: ✅ **NONE** - cluster_id column already exists
**New Domain Models**: 1 - `StopCluster`
**DAO Extensions**: 7 new methods in `StopDao`
**Repository Extensions**: 5 new methods in `StopRepository`

**Key Design Decisions**:
1. No separate clusters table (cluster_id is computed grouping field, not entity)
2. SQL aggregation for cluster statistics (faster than fetching all stops)
3. Batch updates for cluster_id assignments (efficient, atomic)
4. Reactive Flow queries for analytics (UI auto-updates on data changes)

**Ready for Phase 2**: Contract generation and quickstart guide.
