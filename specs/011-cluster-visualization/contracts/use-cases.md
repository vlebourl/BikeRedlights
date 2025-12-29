# Use Case Contracts: Stop Cluster Visualization

**Feature**: 011-cluster-visualization
**Date**: 2025-12-29
**Phase**: Phase 1 - Design & Contracts

This document defines the contracts (inputs, outputs, behavior) for all domain layer use cases.

---

## 1. GetClusteredStopsUseCase

**Purpose**: Retrieve all stops that belong to clusters (exclude noise), with optional filtering.

**Package**: `com.example.bikeredlights.domain.usecase`

**Contract**:

```kotlin
package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Stop
import com.example.bikeredlights.domain.model.StopClusterFilter
import kotlinx.coroutines.flow.Flow

/**
 * Retrieves all clustered stops from repository with optional filtering.
 *
 * Business Rules:
 * - Only returns stops where cluster_id IS NOT NULL (excludes noise points from DBSCAN)
 * - Applies date range filter if specified
 * - Applies minimum cluster size filter if specified
 * - Returns empty list if no clusters match criteria
 *
 * @param filter Filter criteria (default: no filtering - show all clusters)
 * @return Flow emitting list of Stop entities belonging to clusters
 */
class GetClusteredStopsUseCase(
    private val stopRepository: StopRepository
) {
    operator fun invoke(
        filter: StopClusterFilter = StopClusterFilter()
    ): Flow<List<Stop>>
}
```

**Input**:
- `filter: StopClusterFilter` (optional, default = no filtering)
  - `dateRange: DateRange?` (null = all time)
  - `minClusterSize: Int` (default = 2)

**Output**:
- `Flow<List<Stop>>` - Reactive stream of clustered stops

**Behavior**:
1. Query repository for stops with `cluster_id NOT NULL`
2. If `filter.dateRange` is set:
   - Filter stops where `startTime >= dateRange.startMillis AND startTime <= dateRange.endMillis`
3. If `filter.minClusterSize > 2`:
   - Group stops by `cluster_id`
   - Keep only clusters with `count >= minClusterSize`
4. Return filtered list as Flow
5. Return empty list if no stops match criteria

**Error Handling**:
- Database errors propagated to ViewModel
- Empty result is valid (not an error)

**Performance**:
- Query executes on IO dispatcher (Room default)
- Reactive: UI automatically updates when database changes
- Expected query time: <500ms for 500 stops

**Example Usage**:
```kotlin
// No filtering - all clusters
val allClusters = getClusteredStopsUseCase()

// Last 7 days only
val recentClusters = getClusteredStopsUseCase(
    filter = StopClusterFilter(
        dateRange = DateRangePresets.last7Days()
    )
)

// Large clusters only (10+ stops)
val largeClusters = getClusteredStopsUseCase(
    filter = StopClusterFilter(
        minClusterSize = ClusterSizePresets.TEN_PLUS
    )
)
```

**Test Cases**:
- ✅ Returns empty list if no clustered stops exist
- ✅ Excludes stops with cluster_id = NULL (noise points)
- ✅ Filters by date range correctly (inclusive boundaries)
- ✅ Filters by minimum cluster size correctly
- ✅ Combines date range + min size filters correctly
- ✅ Emits updates when new stops are clustered

---

## 2. CalculateClusterStatsUseCase

**Purpose**: Transform raw list of stops into aggregated cluster summaries with statistics.

**Package**: `com.example.bikeredlights.domain.usecase`

**Contract**:

```kotlin
package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.ClusterSummary
import com.example.bikeredlights.domain.model.Stop

/**
 * Calculates aggregate statistics for stop clusters.
 *
 * Business Rules:
 * - Groups stops by cluster_id
 * - Calculates cluster center as arithmetic mean of GPS coordinates
 * - Calculates average duration (only for completed stops with endTime != null)
 * - Generates frequency analytics text based on stop timestamps
 * - Each cluster must have >= 2 stops (DBSCAN minimum)
 *
 * @param stops List of stops to aggregate (must have cluster_id != null)
 * @return List of ClusterSummary entities with calculated statistics
 * @throws IllegalArgumentException if any stop has cluster_id == null
 */
class CalculateClusterStatsUseCase(
    private val calculateClusterCenterUseCase: CalculateClusterCenterUseCase,
    private val formatClusterAnalyticsUseCase: FormatClusterAnalyticsUseCase
) {
    operator fun invoke(stops: List<Stop>): List<ClusterSummary>
}
```

**Input**:
- `stops: List<Stop>` - Stops with cluster_id NOT NULL

**Output**:
- `List<ClusterSummary>` - Aggregated cluster data

**Behavior**:
1. Validate all stops have `cluster_id != null` (throw IllegalArgumentException if any null)
2. Group stops by `cluster_id`: `Map<Long, List<Stop>>`
3. For each cluster group:
   - Calculate center using `CalculateClusterCenterUseCase`
   - Calculate average duration:
     - Filter stops where `endTime != null`
     - Compute: `(endTime - startTime) / 1000` (convert ms to seconds)
     - Average all durations
     - If no completed stops, use 0L
   - Generate frequency text using `FormatClusterAnalyticsUseCase`
   - Create `ClusterSummary` with all calculated values
4. Sort clusters by `stopCount` descending (most frequent first)
5. Return list

**Error Handling**:
- Throws `IllegalArgumentException` if any stop has `cluster_id == null`
- Returns empty list if input is empty (not an error)

**Performance**:
- Pure computation, runs on default dispatcher
- Expected execution time: <100ms for 100 clusters

**Example Usage**:
```kotlin
val stops = getClusteredStopsUseCase().first()
val clusterSummaries = calculateClusterStatsUseCase(stops)

// Result: List<ClusterSummary> sorted by stop count (largest clusters first)
```

**Test Cases**:
- ✅ Groups stops by cluster_id correctly
- ✅ Calculates center as mean of coordinates
- ✅ Calculates average duration excluding active stops (endTime == null)
- ✅ Generates correct frequency text ("X times this week/month")
- ✅ Sorts clusters by stop count descending
- ✅ Throws exception if any stop has cluster_id == null
- ✅ Returns empty list for empty input

---

## 3. FormatClusterAnalyticsUseCase

**Purpose**: Generate human-readable frequency analytics text (FR-015).

**Package**: `com.example.bikeredlights.domain.usecase`

**Contract**:

```kotlin
package com.example.bikeredlights.domain.usecase

/**
 * Formats cluster frequency analytics for UI display.
 *
 * Business Rules (per FR-015):
 * - "X times this week" if all stops are within last 7 days
 * - "X times this month" if all stops are within last 30 days
 * - "X total stops" otherwise
 * - Uses singular "time" for count == 1
 *
 * @param stopCount Total number of stops in cluster
 * @param stopTimestamps List of stop start times (epoch milliseconds)
 * @param currentTimeMillis Current time for relative calculation (injectable for testing)
 * @return Formatted frequency text (e.g., "15 times this month")
 */
class FormatClusterAnalyticsUseCase {
    operator fun invoke(
        stopCount: Int,
        stopTimestamps: List<Long>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): String
}
```

**Input**:
- `stopCount: Int` - Total stops in cluster
- `stopTimestamps: List<Long>` - Stop start times (epoch ms)
- `currentTimeMillis: Long` - Current time (default = now)

**Output**:
- `String` - Formatted frequency text

**Behavior**:
1. Calculate time boundaries:
   - `sevenDaysAgo = currentTimeMillis - (7 * 24 * 60 * 60 * 1000)`
   - `thirtyDaysAgo = currentTimeMillis - (30 * 24 * 60 * 60 * 1000)`
2. Count stops in each time window:
   - `stopsInLastWeek = stopTimestamps.count { it >= sevenDaysAgo }`
   - `stopsInLastMonth = stopTimestamps.count { it >= thirtyDaysAgo }`
3. Determine text:
   - If `stopsInLastWeek == stopCount`: "You stopped here {count} time(s) this week"
   - Else if `stopsInLastMonth == stopCount`: "You stopped here {count} time(s) this month"
   - Else: "{count} total stop(s)"
4. Handle singular/plural:
   - Use "time" if count == 1
   - Use "times" if count > 1

**Error Handling**:
- Empty timestamps list → "0 total stops"
- stopCount != timestamps.size → Warn but use stopCount

**Example Outputs**:
- `"You stopped here 15 times this month"` (all stops in last 30 days)
- `"You stopped here 3 times this week"` (all stops in last 7 days)
- `"12 total stops"` (stops span > 30 days)
- `"You stopped here 1 time this week"` (singular form)

**Test Cases**:
- ✅ All stops in last 7 days → "X times this week"
- ✅ All stops in last 30 days (but not 7) → "X times this month"
- ✅ Stops older than 30 days → "X total stops"
- ✅ Single stop → "1 time" (singular)
- ✅ Multiple stops → "X times" (plural)
- ✅ Empty timestamps → "0 total stops"

---

## 4. CalculateClusterCenterUseCase

**Purpose**: Compute geographic center of a cluster for marker placement.

**Package**: `com.example.bikeredlights.domain.usecase`

**Contract**:

```kotlin
package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Stop
import com.google.android.gms.maps.model.LatLng

/**
 * Calculates cluster center as arithmetic mean of GPS coordinates.
 *
 * Business Rules:
 * - Center = (mean latitude, mean longitude)
 * - Arithmetic mean is sufficient for small clusters (30m radius per Feature 008)
 * - Input must not be empty
 *
 * @param stops List of stops in cluster (must be non-empty)
 * @return LatLng representing cluster center for marker placement
 * @throws IllegalArgumentException if stops list is empty
 */
class CalculateClusterCenterUseCase {
    operator fun invoke(stops: List<Stop>): LatLng
}
```

**Input**:
- `stops: List<Stop>` - Stops in cluster (non-empty)

**Output**:
- `LatLng` - Cluster center coordinates

**Behavior**:
1. Validate `stops.isNotEmpty()` (throw IllegalArgumentException if empty)
2. Extract latitudes: `stops.map { it.latitude }`
3. Extract longitudes: `stops.map { it.longitude }`
4. Calculate means:
   - `avgLat = latitudes.average()`
   - `avgLng = longitudes.average()`
5. Return `LatLng(avgLat, avgLng)`

**Error Handling**:
- Throws `IllegalArgumentException` if input is empty
- No validation of coordinate bounds (assumes Stop entities already validated)

**Performance**:
- O(n) where n = number of stops in cluster
- Expected execution time: <1ms for 50 stops

**Example Usage**:
```kotlin
val clusterStops = listOf(
    Stop(latitude = 37.422, longitude = -122.084),
    Stop(latitude = 37.423, longitude = -122.085),
    Stop(latitude = 37.421, longitude = -122.083)
)

val center = calculateClusterCenterUseCase(clusterStops)
// Result: LatLng(37.422, -122.084) - arithmetic mean
```

**Test Cases**:
- ✅ Single stop → returns that stop's exact coordinates
- ✅ Multiple stops → returns arithmetic mean
- ✅ Throws exception for empty list
- ✅ Handles stops with identical coordinates
- ✅ Validates result is valid LatLng (-90 to 90, -180 to 180)

---

## Use Case Dependencies

```
GetClusteredStopsUseCase
└── StopRepository (data layer)

CalculateClusterStatsUseCase
├── CalculateClusterCenterUseCase
└── FormatClusterAnalyticsUseCase

FormatClusterAnalyticsUseCase
└── (no dependencies - pure function)

CalculateClusterCenterUseCase
└── (no dependencies - pure computation)
```

**Dependency Injection** (Hilt):
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object ClusterUseCaseModule {
    @Provides
    fun provideGetClusteredStopsUseCase(
        stopRepository: StopRepository
    ): GetClusteredStopsUseCase = GetClusteredStopsUseCase(stopRepository)

    @Provides
    fun provideCalculateClusterCenterUseCase(): CalculateClusterCenterUseCase =
        CalculateClusterCenterUseCase()

    @Provides
    fun provideFormatClusterAnalyticsUseCase(): FormatClusterAnalyticsUseCase =
        FormatClusterAnalyticsUseCase()

    @Provides
    fun provideCalculateClusterStatsUseCase(
        calculateCenterUseCase: CalculateClusterCenterUseCase,
        formatAnalyticsUseCase: FormatClusterAnalyticsUseCase
    ): CalculateClusterStatsUseCase = CalculateClusterStatsUseCase(
        calculateCenterUseCase,
        formatAnalyticsUseCase
    )
}
```

---

## Summary

**Total Use Cases**: 4

| Use Case | Type | Dependencies | Complexity |
|----------|------|--------------|------------|
| GetClusteredStopsUseCase | Repository Query | StopRepository | Low |
| CalculateClusterStatsUseCase | Aggregation | 2 other use cases | Medium |
| FormatClusterAnalyticsUseCase | Formatting | None | Low |
| CalculateClusterCenterUseCase | Computation | None | Low |

All use cases follow BikeRedlights patterns:
- ✅ Single responsibility
- ✅ Pure business logic (no Android dependencies except LatLng)
- ✅ Testable with unit tests
- ✅ Hilt dependency injection
- ✅ Kotlin operator fun invoke() pattern
