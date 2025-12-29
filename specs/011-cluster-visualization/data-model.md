# Data Model: Stop Cluster Visualization

**Feature**: 011-cluster-visualization
**Date**: 2025-12-29
**Phase**: Phase 1 - Design & Contracts

This document defines all domain entities, data models, and value objects required for Feature 011.

---

## Domain Layer Entities

### 1. ClusterSummary

**Purpose**: Aggregate representation of a stop cluster with calculated statistics for map display and analytics.

**Package**: `com.example.bikeredlights.domain.model`

**Properties**:

```kotlin
package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.model.LatLng

/**
 * Aggregate summary of a stop cluster for UI display.
 *
 * This entity represents a group of stops that have been clustered together by Feature 010
 * (DBSCAN algorithm). Contains pre-calculated statistics and analytics for efficient rendering.
 *
 * @property clusterId Unique identifier for this cluster (matches cluster_id in stops table)
 * @property centerPosition GPS coordinates for cluster marker placement (arithmetic mean of all stop coordinates)
 * @property stopCount Total number of stops in this cluster
 * @property averageDuration Average duration of all stops in cluster (in seconds)
 * @property frequencyText Human-readable analytics text (e.g., "15 times this month", "12 times in last 7 days")
 * @property stops List of individual Stop entities belonging to this cluster
 */
@Immutable
data class ClusterSummary(
    val clusterId: Long,
    val centerPosition: LatLng,
    val stopCount: Int,
    val averageDuration: Long,  // in seconds
    val frequencyText: String,
    val stops: List<Stop>
)
```

**Validation Rules**:
- `clusterId` must be > 0 (matches database foreign key)
- `stopCount` must equal `stops.size`
- `stopCount` must be >= 2 (minimum for a cluster per DBSCAN)
- `averageDuration` must be >= 0
- `centerPosition` lat/lng must be valid GPS coordinates (-90 to 90, -180 to 180)

**Usage Context**:
- Returned by `GetClusteredStopsUseCase` and `CalculateClusterStatsUseCase`
- Consumed by `ClusterMapViewModel` for StateFlow emission
- Rendered in `StopsMapScreen` as map markers
- Displayed in `ClusterDetailBottomSheet` as popup content

---

### 2. ClusterMarkerData

**Purpose**: Visual properties for rendering a color-coded cluster marker on the map.

**Package**: `com.example.bikeredlights.domain.model`

**Properties**:

```kotlin
package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.model.LatLng

/**
 * Visual representation data for a cluster marker on the map.
 *
 * Encapsulates all properties needed to render a cluster marker with color-coding based on
 * cluster size per Feature 011 requirements (FR-003).
 *
 * @property clusterId Unique identifier for this cluster
 * @property position GPS coordinates for marker placement
 * @property markerColor Color code based on cluster size: GREEN (2-5), YELLOW (6-10), RED (11+)
 * @property stopCount Total stops in cluster (displayed in marker title/snippet)
 */
@Immutable
data class ClusterMarkerData(
    val clusterId: Long,
    val position: LatLng,
    val markerColor: MarkerColor,
    val stopCount: Int
)

/**
 * Enum representing color-coding scheme for cluster markers per FR-003.
 */
enum class MarkerColor(val hue: Float) {
    /** 2-5 stops in cluster */
    GREEN(BitmapDescriptorFactory.HUE_GREEN),  // 120.0f

    /** 6-10 stops in cluster */
    YELLOW(BitmapDescriptorFactory.HUE_YELLOW), // 60.0f

    /** 11+ stops in cluster */
    RED(BitmapDescriptorFactory.HUE_RED)  // 0.0f
}
```

**Business Logic**:
```kotlin
/**
 * Determines marker color based on cluster size per FR-003.
 */
fun determineMarkerColor(stopCount: Int): MarkerColor = when (stopCount) {
    in 2..5 -> MarkerColor.GREEN
    in 6..10 -> MarkerColor.YELLOW
    else -> MarkerColor.RED  // 11+
}
```

**Usage Context**:
- Created by `CalculateClusterStatsUseCase` or directly in ViewModel
- Used in `ClusterMarker` composable for map rendering
- Binds domain logic (color rules) to UI presentation

---

### 3. StopClusterFilter

**Purpose**: User-selected filter criteria for narrowing down visible clusters on map.

**Package**: `com.example.bikeredlights.domain.model`

**Properties**:

```kotlin
package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable

/**
 * Filter criteria for stop cluster visualization.
 *
 * Encapsulates user-selected filters per FR-007 (date range) and FR-008 (minimum cluster size).
 * Default state shows all clusters (no filtering).
 *
 * @property dateRange Time window for stop inclusion (null = all time)
 * @property minClusterSize Minimum number of stops required for cluster to be displayed
 */
@Immutable
data class StopClusterFilter(
    val dateRange: DateRange? = null,  // null = "All Time"
    val minClusterSize: Int = 2  // Default: show all clusters (minimum valid cluster size)
)

/**
 * Represents a time window for filtering stops by date.
 *
 * @property startMillis Start of time window (inclusive) in epoch milliseconds
 * @property endMillis End of time window (inclusive) in epoch milliseconds
 * @property label Human-readable label for UI display (e.g., "Last 7 Days", "Last 30 Days")
 */
@Immutable
data class DateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String
) {
    init {
        require(startMillis <= endMillis) {
            "Start time must be before or equal to end time"
        }
    }
}

/**
 * Predefined date range presets per FR-007.
 */
object DateRangePresets {
    fun last7Days(): DateRange {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        return DateRange(
            startMillis = sevenDaysAgo,
            endMillis = now,
            label = "Last 7 Days"
        )
    }

    fun last30Days(): DateRange {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)
        return DateRange(
            startMillis = thirtyDaysAgo,
            endMillis = now,
            label = "Last 30 Days"
        )
    }

    fun custom(startMillis: Long, endMillis: Long): DateRange {
        return DateRange(
            startMillis = startMillis,
            endMillis = endMillis,
            label = "Custom Range"
        )
    }
}

/**
 * Predefined minimum cluster size presets per FR-008.
 */
object ClusterSizePresets {
    const val TWO_PLUS = 2
    const val THREE_PLUS = 3
    const val FIVE_PLUS = 5
    const val TEN_PLUS = 10
}
```

**Usage Context**:
- Stored in `ClusterMapViewModel` state
- Updated by `ClusterFilterControls` composable user input
- Passed to `GetClusteredStopsUseCase` to filter query results
- Displayed in filter status indicator (FR-016)

---

## Existing Entities (Reference)

### Stop (from Feature 009)

**Package**: `com.example.bikeredlights.domain.model`

**Relevant Properties**:
```kotlin
data class Stop(
    val id: Long = 0,
    val rideId: Long,
    val startTime: Long,         // Epoch milliseconds
    val endTime: Long?,          // Null if stop not yet ended
    val latitude: Double,        // GPS coordinate
    val longitude: Double,       // GPS coordinate
    val clusterId: Long? = null  // Populated by Feature 010, null = noise
)
```

**Usage in Feature 011**:
- Source data for cluster aggregation
- Filtered by `clusterId NOT NULL` to exclude noise points
- Grouped by `clusterId` to form clusters
- Individual stops displayed in cluster detail bottom sheet list

---

## UI State Models

### ClusterMapUiState

**Purpose**: Immutable state representation for `StopsMapScreen`.

**Package**: `com.example.bikeredlights.ui.viewmodel`

**Properties**:

```kotlin
package com.example.bikeredlights.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.bikeredlights.domain.model.ClusterSummary
import com.example.bikeredlights.domain.model.StopClusterFilter

/**
 * UI state for Stops map screen.
 *
 * @property clusters List of cluster summaries to display on map
 * @property activeFilter Currently applied filter criteria
 * @property isLoading True if cluster data is being loaded
 * @property errorMessage Error message if cluster loading failed (null if no error)
 * @property selectedCluster Cluster currently selected for detail view (null if none selected)
 */
@Immutable
data class ClusterMapUiState(
    val clusters: List<ClusterSummary> = emptyList(),
    val activeFilter: StopClusterFilter = StopClusterFilter(),  // Default: no filtering
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedCluster: ClusterSummary? = null
)
```

**State Transitions**:
- **Initial**: `isLoading = true, clusters = emptyList()`
- **Loaded**: `isLoading = false, clusters = [data], errorMessage = null`
- **Error**: `isLoading = false, clusters = emptyList(), errorMessage = "Error text"`
- **Filtered**: `clusters = [filtered data], activeFilter = [user selection]`
- **Cluster Selected**: `selectedCluster = [tapped cluster]`

**Usage Context**:
- Emitted by `ClusterMapViewModel` via StateFlow
- Collected in `StopsMapScreen` with `collectAsStateWithLifecycle()`
- Drives UI rendering: markers, bottom sheet, loading indicators

---

## Value Objects & Helpers

### ClusterAnalytics

**Purpose**: Helper for generating frequency analytics text (FR-015).

**Package**: `com.example.bikeredlights.domain.model`

**Implementation**:

```kotlin
package com.example.bikeredlights.domain.model

import java.util.concurrent.TimeUnit

/**
 * Helper for calculating and formatting cluster frequency analytics.
 */
object ClusterAnalytics {
    /**
     * Generates frequency text based on stop timestamps and current time.
     *
     * Logic (per FR-015):
     * - "X times this week" if all stops in last 7 days
     * - "X times this month" if all stops in last 30 days
     * - "X total stops" otherwise
     *
     * @param stopCount Total number of stops in cluster
     * @param stopTimestamps List of stop start times (epoch milliseconds)
     * @param currentTimeMillis Current time for relative calculation (default: System.currentTimeMillis())
     * @return Formatted frequency text for UI display
     */
    fun formatFrequency(
        stopCount: Int,
        stopTimestamps: List<Long>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): String {
        val sevenDaysAgo = currentTimeMillis - TimeUnit.DAYS.toMillis(7)
        val thirtyDaysAgo = currentTimeMillis - TimeUnit.DAYS.toMillis(30)

        val stopsInLastWeek = stopTimestamps.count { it >= sevenDaysAgo }
        val stopsInLastMonth = stopTimestamps.count { it >= thirtyDaysAgo }

        return when {
            stopsInLastWeek == stopCount -> {
                "You stopped here $stopCount time${if (stopCount > 1) "s" else ""} this week"
            }
            stopsInLastMonth == stopCount -> {
                "You stopped here $stopCount time${if (stopCount > 1) "s" else ""} this month"
            }
            else -> {
                "$stopCount total stop${if (stopCount > 1) "s" else ""}"
            }
        }
    }
}
```

**Test Cases**:
- All stops within 7 days → "X times this week"
- All stops within 30 days (but not 7) → "X times this month"
- Stops older than 30 days → "X total stops"
- Single stop → "1 time" (singular form)

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ Data Layer (Room Database)                                     │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ stops table                                                 │ │
│ │ - id, ride_id, start_time, end_time, latitude, longitude   │ │
│ │ - cluster_id (populated by Feature 010)                     │ │
│ └─────────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Query: WHERE cluster_id IS NOT NULL
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ Repository Layer                                                │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ StopRepository.getClusteredStops(filter)                    │ │
│ │ Returns: Flow<List<Stop>>                                   │ │
│ └─────────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ List<Stop> (raw data)
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ Domain Layer (Use Cases)                                        │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 1. GetClusteredStopsUseCase                                 │ │
│ │    Input: StopClusterFilter                                 │ │
│ │    Output: Flow<List<Stop>> (filtered)                      │ │
│ │                                                              │ │
│ │ 2. CalculateClusterStatsUseCase                             │ │
│ │    Input: List<Stop>                                        │ │
│ │    Process:                                                  │ │
│ │      - Group by cluster_id                                   │ │
│ │      - Calculate center (CalculateClusterCenterUseCase)     │ │
│ │      - Calculate avg duration                                │ │
│ │      - Format analytics (FormatClusterAnalyticsUseCase)     │ │
│ │    Output: List<ClusterSummary>                             │ │
│ │                                                              │ │
│ │ 3. FormatClusterAnalyticsUseCase                            │ │
│ │    Input: stopCount, timestamps                              │ │
│ │    Output: String (frequency text)                           │ │
│ │                                                              │ │
│ │ 4. CalculateClusterCenterUseCase                            │ │
│ │    Input: List<Stop>                                        │ │
│ │    Output: LatLng (arithmetic mean)                          │ │
│ └─────────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ List<ClusterSummary>
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ ViewModel Layer                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ ClusterMapViewModel                                         │ │
│ │                                                              │ │
│ │ State: StateFlow<ClusterMapUiState>                         │ │
│ │   - clusters: List<ClusterSummary>                          │ │
│ │   - activeFilter: StopClusterFilter                         │ │
│ │   - selectedCluster: ClusterSummary?                        │ │
│ │                                                              │ │
│ │ Events:                                                      │ │
│ │   - applyFilter(StopClusterFilter)                          │ │
│ │   - selectCluster(ClusterSummary)                           │ │
│ │   - clearSelection()                                         │ │
│ │   - clearFilters()                                           │ │
│ └─────────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ ClusterMapUiState
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ UI Layer (Jetpack Compose)                                      │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ StopsMapScreen                                              │ │
│ │   ├── BikeMap (existing from Feature 006)                   │ │
│ │   │   └── ClusterMarkers (forEach cluster → Marker)         │ │
│ │   │                                                          │ │
│ │   └── if (selectedCluster != null) {                        │ │
│ │         ModalBottomSheet {                                   │ │
│ │           ClusterDetailBottomSheet(selectedCluster)         │ │
│ │             ├── Summary cards (count, avg duration)          │ │
│ │             ├── Frequency analytics text                     │ │
│ │             └── LazyColumn { StopListItem(stop) }           │ │
│ │         }                                                    │ │
│ │       }                                                      │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Database Schema Changes

**No schema changes required**. Feature 011 uses existing `stops` table from Feature 009 with `cluster_id` column added by Feature 010.

**Relevant Queries**:

```sql
-- Get all clustered stops (exclude noise)
SELECT * FROM stops WHERE cluster_id IS NOT NULL;

-- Get stops for specific cluster
SELECT * FROM stops WHERE cluster_id = ?;

-- Get clustered stops within date range
SELECT * FROM stops
WHERE cluster_id IS NOT NULL
AND start_time BETWEEN ? AND ?;

-- Count stops per cluster (for filtering by min size)
SELECT cluster_id, COUNT(*) as stop_count
FROM stops
WHERE cluster_id IS NOT NULL
GROUP BY cluster_id
HAVING stop_count >= ?;
```

---

## Immutability & Stability

All domain models are marked with `@Immutable` or use immutable data structures:
- ✅ `ClusterSummary`: All properties are `val`, List is read-only
- ✅ `ClusterMarkerData`: All properties are `val`
- ✅ `StopClusterFilter`: All properties are `val`, DateRange is immutable
- ✅ `ClusterMapUiState`: All properties are `val`, Lists are read-only

**Why**: Immutability enables Compose to skip unnecessary recompositions, critical for smooth 60fps map performance per SC-006.

---

## Summary

**New Domain Entities**: 3
- ClusterSummary (aggregate cluster data)
- ClusterMarkerData (visual marker properties)
- StopClusterFilter (filter criteria)

**New UI State**: 1
- ClusterMapUiState (screen state)

**Helper Objects**: 2
- ClusterAnalytics (frequency text generation)
- DateRangePresets / ClusterSizePresets (filter presets)

**Existing Entities Referenced**: 1
- Stop (from Feature 009, with cluster_id from Feature 010)

All models follow BikeRedlights MVVM + Clean Architecture patterns with immutable data structures for optimal Compose performance.
