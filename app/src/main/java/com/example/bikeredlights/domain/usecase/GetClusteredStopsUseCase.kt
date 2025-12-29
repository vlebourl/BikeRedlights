package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Stop
import com.example.bikeredlights.domain.model.StopClusterFilter
import com.example.bikeredlights.domain.repository.StopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Retrieves all clustered stops from repository with optional filtering.
 *
 * Created by Feature 011: Stop Cluster Visualization.
 * Used by ClusterMapViewModel to fetch stops for map display.
 *
 * Business Rules:
 * - Only returns stops where cluster_id IS NOT NULL (excludes noise points from DBSCAN)
 * - Applies date range filter if specified
 * - Applies minimum cluster size filter if specified
 * - Returns empty list if no clusters match criteria
 *
 * Dependencies:
 * - StopRepository: Data source for clustered stops
 *
 * Performance:
 * - Query executes on IO dispatcher (Room default)
 * - Reactive: UI automatically updates when database changes
 * - Expected query time: <500ms for 500 stops
 *
 * @param filter Filter criteria (default: no filtering - show all clusters)
 * @return Flow emitting list of Stop entities belonging to clusters
 */
class GetClusteredStopsUseCase @Inject constructor(
    private val stopRepository: StopRepository
) {
    /**
     * Invoke use case with optional filtering.
     *
     * Filter Logic:
     * 1. If filter.dateRange is set: Query repository with date range
     * 2. Else: Query all clustered stops
     * 3. If filter.minClusterSize > 2: Filter by cluster size in-memory
     * 4. Return filtered Flow
     *
     * Note: Minimum cluster size filtering happens in-memory (not SQL) because
     * it requires grouping by cluster_id and counting, which is complex in SQL.
     * For typical use (50-100 clusters), in-memory filtering is fast (<50ms).
     *
     * @param filter Filter criteria (default = StopClusterFilter())
     * @return Flow emitting list of clustered stops matching filter
     */
    operator fun invoke(
        filter: StopClusterFilter = StopClusterFilter()
    ): Flow<List<Stop>> {
        // Step 1: Get clustered stops (with optional date range filter)
        val baseFlow: Flow<List<Stop>> = if (filter.dateRange != null) {
            stopRepository.getClusteredStopsByDateRange(
                startMillis = filter.dateRange.startMillis,
                endMillis = filter.dateRange.endMillis
            )
        } else {
            stopRepository.getClusteredStops()
        }

        // Step 2: Apply minimum cluster size filter (in-memory)
        return if (filter.minClusterSize > 2) {
            baseFlow.map { stops ->
                // Group by cluster_id and filter by size
                val grouped = stops.groupBy { it.clusterId!! }
                val filteredGroups = grouped.filter { (_, clusterStops) ->
                    clusterStops.size >= filter.minClusterSize
                }
                // Flatten back to list of stops
                filteredGroups.values.flatten()
            }
        } else {
            // No minimum size filter needed
            baseFlow
        }
    }
}
