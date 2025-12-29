/**
 * CONTRACT: StopRepository Extensions for Clustering (Feature 010)
 *
 * This file documents the contract extensions to StopRepository.
 * Implementation: app/src/main/java/com/example/bikeredlights/domain/repository/StopRepository.kt
 *
 * Purpose: Provide clustering-specific data access methods for DBSCAN algorithm.
 */

package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.Stop
import com.example.bikeredlights.domain.model.StopCluster
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for stop clustering operations.
 *
 * Extends existing StopRepository with clustering-specific methods.
 * Maintains separation of concerns: repository handles data access, use case handles business logic.
 */
interface StopRepositoryClusteringContract {

    /**
     * Get all stops for clustering operation.
     *
     * **Contract**:
     * - MUST return ALL stops from database (no filtering)
     * - MUST order by start_timestamp ASC for deterministic clustering
     * - MUST return domain models (Stop), not database entities
     * - MAY return empty list if no stops exist
     *
     * **Performance**:
     * - Expected: O(n) where n = total stops in database
     * - Typical: 10-50ms for 1000 stops
     *
     * **Use Case**: Called by ClusterStopsUseCase to fetch data for DBSCAN algorithm
     *
     * @return List of all stops ordered by timestamp
     */
    suspend fun getAllStops(): List<Stop>

    /**
     * Update cluster_id assignments for multiple stops atomically.
     *
     * **Contract**:
     * - MUST update ALL stops in clusterAssignments map
     * - MUST be atomic (all updates succeed or all fail)
     * - MUST handle transaction boundaries (@Transaction in DAO)
     * - MUST preserve stop data (only cluster_id changes)
     * - SHOULD batch updates for efficiency (not 1 UPDATE per stop)
     *
     * **Validation**:
     * - cluster_id values MUST be positive (> 0)
     * - stop IDs MUST exist in database (foreign key constraint)
     * - No validation of cluster assignments (business logic in use case)
     *
     * **Performance**:
     * - Expected: O(c * log(s)) where c = clusters, s = stops per cluster
     * - Typical: 5-10ms for 100 stops across 10 clusters
     *
     * **Use Case**: Called by ClusterStopsUseCase after DBSCAN algorithm completes
     *
     * @param clusterAssignments Map of cluster_id to list of stop IDs
     *   Example: {1: [101, 102, 103], 2: [104, 105], 3: [106]}
     * @throws IllegalArgumentException if cluster_id <= 0
     * @throws IllegalStateException if stop IDs don't exist
     */
    suspend fun updateClusterAssignments(clusterAssignments: Map<Long, List<Long>>)

    /**
     * Get cluster statistics for a specific cluster.
     *
     * **Contract**:
     * - MUST return null if cluster doesn't exist
     * - MUST calculate aggregated statistics (count, centroid, durations)
     * - MUST use SQL aggregation (AVG, SUM, MIN, MAX) for efficiency
     * - MUST NOT fetch all stops and compute in Kotlin (performance anti-pattern)
     *
     * **Statistics Calculated**:
     * - stopCount: COUNT(*) of stops with this cluster_id
     * - centroidLatitude/Longitude: AVG(latitude), AVG(longitude)
     * - averageDuration: AVG(duration_seconds)
     * - totalDuration: SUM(duration_seconds)
     * - earliestStop: MIN(start_timestamp)
     * - latestStop: MAX(start_timestamp)
     *
     * **Performance**:
     * - Expected: O(s) where s = stops in cluster
     * - Typical: 2-5ms per cluster
     *
     * **Use Case**: Called by analytics UI to display cluster details
     *
     * @param clusterId Cluster identifier
     * @return Cluster statistics or null if cluster doesn't exist
     */
    suspend fun getClusterStats(clusterId: Long): StopCluster?

    /**
     * Get all cluster statistics as reactive Flow.
     *
     * **Contract**:
     * - MUST return Flow that emits on stops table changes
     * - MUST order by stopCount DESC (most frequent clusters first)
     * - MUST calculate statistics via SQL GROUP BY (efficient)
     * - MUST emit empty list if no clusters exist
     * - MAY emit duplicate values if stops table updates rapidly (Flow behavior)
     *
     * **Flow Lifecycle**:
     * - Emits initial value immediately upon collection
     * - Emits new value when ANY stop's cluster_id changes
     * - Emits new value when stops are inserted/deleted
     * - Terminates when collector is cancelled
     *
     * **Performance**:
     * - Expected: O(n) where n = total stops (GROUP BY scan)
     * - Typical: 10-20ms for 1000 stops grouped into 50 clusters
     *
     * **Use Case**: Called by ClusterAnalyticsViewModel to display top intersections
     *
     * @return Flow of cluster statistics ordered by frequency (descending)
     */
    fun getAllClustersFlow(): Flow<List<StopCluster>>

    /**
     * Clear all cluster_id assignments (prepare for re-clustering).
     *
     * **Contract**:
     * - MUST set cluster_id = NULL for ALL stops
     * - MUST be atomic (transaction boundary)
     * - MUST preserve all other stop data (coordinates, timestamps, etc.)
     * - SHOULD be called before full re-clustering to avoid orphaned clusters
     *
     * **Performance**:
     * - Expected: O(n) where n = total stops
     * - Typical: 5-10ms for 1000 stops
     *
     * **Use Case**: Called by ClusterStopsUseCase before full re-clustering
     */
    suspend fun clearAllClusterAssignments()
}
