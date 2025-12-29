# Repository Contract: Stop Cluster Visualization

**Feature**: 011-cluster-visualization
**Date**: 2025-12-29
**Phase**: Phase 1 - Design & Contracts

This document defines the repository and DAO method contracts required for Feature 011.

---

## StopRepository Extensions

**Existing Interface**: `com.example.bikeredlights.domain.repository.StopRepository`

**New Methods Required**:

```kotlin
package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.Stop
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Stop entity operations.
 *
 * Extended by Feature 011 to support clustered stop queries.
 */
interface StopRepository {
    // ... existing methods from Feature 009 ...

    /**
     * Get all stops that belong to clusters (cluster_id IS NOT NULL).
     *
     * Excludes noise points identified by DBSCAN clustering (Feature 010).
     * Returns reactive flow that emits updates when stops table changes.
     *
     * @return Flow emitting list of clustered stops, ordered by start_time descending
     */
    fun getClusteredStops(): Flow<List<Stop>>

    /**
     * Get clustered stops within a specific date range.
     *
     * Combines cluster_id filtering with date range filtering.
     * Boundaries are inclusive.
     *
     * @param startMillis Start of date range (inclusive) in epoch milliseconds
     * @param endMillis End of date range (inclusive) in epoch milliseconds
     * @return Flow emitting list of clustered stops within date range
     */
    fun getClusteredStopsByDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<Stop>>

    /**
     * Get stops grouped by cluster ID.
     *
     * Returns map where key = cluster_id, value = list of stops in that cluster.
     * Excludes noise points (cluster_id == null).
     *
     * @return Flow emitting map of cluster ID to stops
     */
    fun getStopsGroupedByCluster(): Flow<Map<Long, List<Stop>>>
}
```

---

## StopDao Extensions

**Existing DAO**: `com.example.bikeredlights.data.local.dao.StopDao`

**New Query Methods**:

```kotlin
package com.example.bikeredlights.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.bikeredlights.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Stop entity database operations.
 *
 * Extended by Feature 011 to support cluster visualization queries.
 */
@Dao
interface StopDao {
    // ... existing methods from Feature 009 ...

    /**
     * Query all stops that belong to clusters.
     *
     * SQL Logic:
     * - WHERE cluster_id IS NOT NULL (exclude noise)
     * - ORDER BY start_time DESC (most recent first)
     *
     * @return Flow emitting list of StopEntity with cluster assignments
     */
    @Query("""
        SELECT * FROM stops
        WHERE cluster_id IS NOT NULL
        ORDER BY start_time DESC
    """)
    fun getClusteredStops(): Flow<List<StopEntity>>

    /**
     * Query clustered stops within date range.
     *
     * SQL Logic:
     * - WHERE cluster_id IS NOT NULL AND start_time BETWEEN :startMillis AND :endMillis
     * - ORDER BY start_time DESC
     *
     * @param startMillis Start of date range (inclusive)
     * @param endMillis End of date range (inclusive)
     * @return Flow emitting list of StopEntity matching criteria
     */
    @Query("""
        SELECT * FROM stops
        WHERE cluster_id IS NOT NULL
        AND start_time BETWEEN :startMillis AND :endMillis
        ORDER BY start_time DESC
    """)
    fun getClusteredStopsByDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<StopEntity>>

    /**
     * Query cluster IDs with stop counts (for minimum size filtering).
     *
     * SQL Logic:
     * - GROUP BY cluster_id
     * - HAVING COUNT(*) >= :minSize
     * - Returns cluster IDs that meet minimum size threshold
     *
     * Usage: First query cluster IDs, then query stops for those clusters.
     *
     * @param minSize Minimum number of stops required in cluster
     * @return Flow emitting list of cluster IDs meeting threshold
     */
    @Query("""
        SELECT cluster_id FROM stops
        WHERE cluster_id IS NOT NULL
        GROUP BY cluster_id
        HAVING COUNT(*) >= :minSize
    """)
    fun getClusterIdsWithMinSize(minSize: Int): Flow<List<Long>>

    /**
     * Query stops for specific cluster IDs.
     *
     * Used in combination with getClusterIdsWithMinSize for two-step filtering.
     *
     * @param clusterIds List of cluster IDs to fetch stops for
     * @return Flow emitting list of StopEntity belonging to specified clusters
     */
    @Query("""
        SELECT * FROM stops
        WHERE cluster_id IN (:clusterIds)
        ORDER BY start_time DESC
    """)
    fun getStopsByClusterIds(clusterIds: List<Long>): Flow<List<StopEntity>>
}
```

---

## StopRepositoryImpl Extensions

**Existing Implementation**: `com.example.bikeredlights.data.repository.StopRepositoryImpl`

**Implementation of New Methods**:

```kotlin
package com.example.bikeredlights.data.repository

import com.example.bikeredlights.data.local.dao.StopDao
import com.example.bikeredlights.data.local.entity.StopEntity
import com.example.bikeredlights.domain.model.Stop
import com.example.bikeredlights.domain.repository.StopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of StopRepository using Room database.
 *
 * Extended by Feature 011 to support cluster visualization queries.
 */
class StopRepositoryImpl @Inject constructor(
    private val stopDao: StopDao
) : StopRepository {
    // ... existing methods ...

    override fun getClusteredStops(): Flow<List<Stop>> {
        return stopDao.getClusteredStops()
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getClusteredStopsByDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<Stop>> {
        return stopDao.getClusteredStopsByDateRange(startMillis, endMillis)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getStopsGroupedByCluster(): Flow<Map<Long, List<Stop>>> {
        return stopDao.getClusteredStops()
            .map { entities ->
                entities
                    .map { it.toDomainModel() }
                    .groupBy { it.clusterId!! }  // Safe: already filtered cluster_id NOT NULL
            }
    }

    /**
     * Get clustered stops filtered by minimum cluster size.
     *
     * Two-step query:
     * 1. Find cluster IDs meeting minimum size threshold
     * 2. Fetch all stops for those cluster IDs
     *
     * @param minClusterSize Minimum number of stops required in cluster
     * @return Flow emitting list of stops in clusters >= minClusterSize
     */
    fun getClusteredStopsByMinSize(minClusterSize: Int): Flow<List<Stop>> {
        return stopDao.getClusterIdsWithMinSize(minClusterSize)
            .flatMapLatest { clusterIds ->
                if (clusterIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    stopDao.getStopsByClusterIds(clusterIds)
                        .map { entities -> entities.map { it.toDomainModel() } }
                }
            }
    }
}

/**
 * Extension function to convert StopEntity to domain model.
 *
 * Existing from Feature 009, no changes required.
 */
private fun StopEntity.toDomainModel(): Stop {
    return Stop(
        id = id,
        rideId = rideId,
        startTime = startTime,
        endTime = endTime,
        latitude = latitude,
        longitude = longitude,
        clusterId = clusterId  // Populated by Feature 010
    )
}
```

---

## Query Performance Considerations

### Indexing

**Existing Indexes** (from Feature 009/010):
- Primary key on `id` (auto-indexed)
- Foreign key on `ride_id` (auto-indexed by Room)
- Index on `cluster_id` (added by Feature 010 for clustering queries)

**New Index Recommended**:
```kotlin
@Entity(
    tableName = "stops",
    indices = [
        Index(value = ["ride_id"]),
        Index(value = ["cluster_id"]),
        Index(value = ["start_time"]),  // NEW: for date range queries
        Index(value = ["cluster_id", "start_time"])  // NEW: composite for combined filtering
    ]
)
```

**Rationale**:
- `start_time` index: Speeds up date range filtering (FR-007)
- Composite index: Optimizes combined cluster + date queries
- Expected query time: <100ms for 500 stops with proper indexing

### Query Optimization

**Good Practices**:
- ✅ Use `Flow<List<T>>` for reactive data (Room default)
- ✅ Filter in SQL WHERE clause (not in Kotlin)
- ✅ ORDER BY in SQL (not Kotlin sorting)
- ✅ Use IN clause for batch ID queries (efficient)

**Avoid**:
- ❌ Loading all stops then filtering in memory
- ❌ Multiple sequential queries when one JOIN would suffice
- ❌ Collecting Flow then emitting again (use map/flatMap)

---

## Data Flow Examples

### Example 1: Get All Clusters

```kotlin
// ViewModel
viewModelScope.launch {
    getClusteredStopsUseCase()
        .map { stops -> calculateClusterStatsUseCase(stops) }
        .collect { clusterSummaries ->
            _uiState.update { it.copy(clusters = clusterSummaries, isLoading = false) }
        }
}

// Data flow:
// 1. GetClusteredStopsUseCase calls StopRepository.getClusteredStops()
// 2. Repository calls StopDao.getClusteredStops()
// 3. Room executes: SELECT * FROM stops WHERE cluster_id IS NOT NULL ORDER BY start_time DESC
// 4. Results flow back as List<Stop>
// 5. CalculateClusterStatsUseCase aggregates into List<ClusterSummary>
// 6. ViewModel emits to UI
```

### Example 2: Filter by Date Range

```kotlin
// User selects "Last 7 Days" filter
val filter = StopClusterFilter(dateRange = DateRangePresets.last7Days())

viewModelScope.launch {
    getClusteredStopsUseCase(filter)
        .map { stops -> calculateClusterStatsUseCase(stops) }
        .collect { clusterSummaries ->
            _uiState.update {
                it.copy(
                    clusters = clusterSummaries,
                    activeFilter = filter,
                    isLoading = false
                )
            }
        }
}

// Data flow:
// 1. GetClusteredStopsUseCase extracts startMillis/endMillis from filter.dateRange
// 2. Calls StopRepository.getClusteredStopsByDateRange(startMillis, endMillis)
// 3. Room executes: SELECT * FROM stops WHERE cluster_id IS NOT NULL AND start_time BETWEEN ? AND ?
// 4. Filtered results aggregated and displayed
```

### Example 3: Filter by Minimum Cluster Size

```kotlin
// User selects "10+ stops" filter
val filter = StopClusterFilter(minClusterSize = ClusterSizePresets.TEN_PLUS)

viewModelScope.launch {
    getClusteredStopsUseCase(filter)
        .map { stops -> calculateClusterStatsUseCase(stops) }
        .collect { clusterSummaries ->
            _uiState.update {
                it.copy(
                    clusters = clusterSummaries,
                    activeFilter = filter,
                    isLoading = false
                )
            }
        }
}

// Data flow:
// 1. GetClusteredStopsUseCase checks filter.minClusterSize > 2
// 2. Calls StopRepository.getClusteredStopsByMinSize(10)
// 3. Repository:
//    a. Calls StopDao.getClusterIdsWithMinSize(10)
//       SQL: SELECT cluster_id FROM stops WHERE cluster_id IS NOT NULL GROUP BY cluster_id HAVING COUNT(*) >= 10
//    b. Gets cluster IDs: [5, 12, 23]
//    c. Calls StopDao.getStopsByClusterIds([5, 12, 23])
//       SQL: SELECT * FROM stops WHERE cluster_id IN (5, 12, 23) ORDER BY start_time DESC
// 4. Results aggregated and displayed
```

---

## Testing Recommendations

### Unit Tests (StopRepositoryImplTest)

```kotlin
@Test
fun `getClusteredStops returns only stops with cluster_id not null`() = runTest {
    // Given: Database has 5 stops, 3 clustered and 2 noise
    stopDao.insert(createStopEntity(id = 1, clusterId = 5))
    stopDao.insert(createStopEntity(id = 2, clusterId = 5))
    stopDao.insert(createStopEntity(id = 3, clusterId = null))  // noise
    stopDao.insert(createStopEntity(id = 4, clusterId = 7))
    stopDao.insert(createStopEntity(id = 5, clusterId = null))  // noise

    // When: Query clustered stops
    val result = repository.getClusteredStops().first()

    // Then: Only 3 clustered stops returned
    assertThat(result).hasSize(3)
    assertThat(result.map { it.id }).containsExactly(1L, 2L, 4L)
}

@Test
fun `getClusteredStopsByDateRange filters correctly`() = runTest {
    val now = System.currentTimeMillis()
    val tenDaysAgo = now - TimeUnit.DAYS.toMillis(10)
    val twentyDaysAgo = now - TimeUnit.DAYS.toMillis(20)

    // Given: 3 clustered stops at different times
    stopDao.insert(createStopEntity(id = 1, startTime = now, clusterId = 5))
    stopDao.insert(createStopEntity(id = 2, startTime = tenDaysAgo, clusterId = 5))
    stopDao.insert(createStopEntity(id = 3, startTime = twentyDaysAgo, clusterId = 7))

    // When: Query last 15 days
    val fifteenDaysAgo = now - TimeUnit.DAYS.toMillis(15)
    val result = repository.getClusteredStopsByDateRange(fifteenDaysAgo, now).first()

    // Then: Only stops from last 15 days returned
    assertThat(result).hasSize(2)
    assertThat(result.map { it.id }).containsExactly(1L, 2L)
}
```

### Integration Tests (DAO Tests)

```kotlin
@Test
fun `getClusterIdsWithMinSize returns correct cluster IDs`() = runTest {
    // Given: Cluster 5 has 3 stops, Cluster 7 has 2 stops, Cluster 9 has 5 stops
    stopDao.insertAll(
        createStopEntity(clusterId = 5),
        createStopEntity(clusterId = 5),
        createStopEntity(clusterId = 5),
        createStopEntity(clusterId = 7),
        createStopEntity(clusterId = 7),
        createStopEntity(clusterId = 9),
        createStopEntity(clusterId = 9),
        createStopEntity(clusterId = 9),
        createStopEntity(clusterId = 9),
        createStopEntity(clusterId = 9)
    )

    // When: Query clusters with minimum 3 stops
    val result = stopDao.getClusterIdsWithMinSize(3).first()

    // Then: Only clusters 5 and 9 returned (7 has only 2 stops)
    assertThat(result).containsExactly(5L, 9L)
}
```

---

## Summary

**Repository Methods Added**: 3
- `getClusteredStops()`: Basic cluster query
- `getClusteredStopsByDateRange()`: Date range filtering
- `getStopsGroupedByCluster()`: Pre-grouped for aggregation

**DAO Methods Added**: 4
- `getClusteredStops()`: SQL query with cluster_id filter
- `getClusteredStopsByDateRange()`: Combined cluster + date filter
- `getClusterIdsWithMinSize()`: Minimum size threshold query
- `getStopsByClusterIds()`: Batch ID lookup

**Database Changes**: None (uses existing stops table from Feature 009)

**Index Recommendations**: 2 new indexes for performance
- Single column: `start_time`
- Composite: `(cluster_id, start_time)`

All repository contracts follow BikeRedlights patterns:
- ✅ Flow-based reactive data
- ✅ Room database queries
- ✅ Entity-to-domain model mapping
- ✅ Hilt dependency injection
- ✅ Testable with in-memory database
