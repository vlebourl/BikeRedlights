/**
 * CONTRACT: ClusterStopsUseCase (Feature 010)
 *
 * This file documents the contract for the clustering use case.
 * Implementation: app/src/main/java/com/example/bikeredlights/domain/usecase/ClusterStopsUseCase.kt
 *
 * Purpose: Orchestrate DBSCAN clustering algorithm on GPS stops.
 */

package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Stop

/**
 * Use case for clustering stops using DBSCAN algorithm.
 *
 * Responsibilities:
 * 1. Fetch all stops from repository
 * 2. Run DBSCAN clustering algorithm with configured epsilon/minPts
 * 3. Assign cluster_id to each stop (core, border, noise)
 * 4. Persist cluster assignments back to repository
 *
 * Dependencies:
 * - StopRepository: Data access for stops
 * - DBSCANAlgorithm: Clustering algorithm implementation
 * - SettingsRepository: Read clustering radius (epsilon) setting
 *
 * Thread Safety:
 * - All methods are suspend functions (coroutine-safe)
 * - Repository handles database transaction atomicity
 * - No shared mutable state (pure function pattern)
 */
interface ClusterStopsUseCase {

    /**
     * Cluster all stops in database using DBSCAN algorithm.
     *
     * **Algorithm**: DBSCAN (Density-Based Spatial Clustering of Applications with Noise)
     * **Distance Metric**: Haversine formula for GPS coordinates
     * **Parameters**:
     * - epsilon: Clustering radius from settings (10-50m, default 20m)
     * - minPts: 3 stops per cluster (standard for 2D data)
     *
     * **Contract**:
     * - MUST fetch all stops from repository (getAllStops())
     * - MUST run DBSCAN algorithm on fetched stops
     * - MUST assign cluster_id to all stops (core + border + noise)
     * - MUST persist cluster assignments (updateClusterAssignments())
     * - MUST be idempotent (safe to call multiple times with same data)
     * - SHOULD clear existing cluster_id values before re-clustering
     *
     * **Cluster Assignment Rules**:
     * - Core points: Stops with >= minPts neighbors within epsilon → assigned to cluster 1, 2, 3, ...
     * - Border points: Stops within epsilon of core point → assigned to nearest core point's cluster
     * - Noise points: Isolated stops (< minPts neighbors) → assigned to unique singleton clusters
     *
     * **Performance**:
     * - Time Complexity: O(n²) where n = total stops (naive DBSCAN)
     * - Expected Runtime:
     *   - 100 stops: 10-20ms
     *   - 500 stops: 50-75ms
     *   - 1000 stops: 100-200ms
     * - Memory: O(n) for stops list + O(n) for cluster assignments
     *
     * **Error Handling**:
     * - Empty database (0 stops): Returns 0 (no clusters created)
     * - Single stop: Assigns cluster_id=1 (singleton cluster)
     * - Database error: Propagates exception from repository
     *
     * **Use Cases**:
     * - Called after each ride completes (incremental clustering - future optimization)
     * - Called when clustering radius setting changes (full re-clustering)
     * - Called manually by user via "Re-cluster" button (if UI added)
     *
     * @return Number of stops successfully clustered
     * @throws IllegalStateException if clustering fails
     */
    suspend fun clusterAllStops(): Int

    /**
     * Get current clustering configuration.
     *
     * **Contract**:
     * - MUST return current epsilon (radius) from settings
     * - MUST return current minPts value (hardcoded to 3)
     * - SHOULD be used by UI to display current settings
     *
     * **Use Case**: Display clustering parameters in settings or debug UI
     *
     * @return Pair of (epsilon in meters, minPts value)
     */
    suspend fun getClusteringConfig(): Pair<Float, Int>

    /**
     * Cluster only new stops (incremental clustering - DEFERRED TO POST-MVP).
     *
     * **Contract**:
     * - MUST fetch only stops with cluster_id = NULL
     * - MUST assign to existing clusters or create new clusters
     * - MUST NOT re-cluster existing stops (preserve cluster_id)
     * - SHOULD be faster than full re-clustering (O(k * m) vs O(n²))
     *
     * **Trade-offs**:
     * - Faster: Only clusters new stops against existing cluster centroids
     * - Less accurate: May miss cluster merges/splits caused by new stops
     * - Drift: Cluster centroids not recalculated (accuracy degrades over time)
     *
     * **Status**: DEFERRED - Use clusterAllStops() for MVP
     *
     * @param newStopIds List of stop IDs to cluster (with cluster_id = NULL)
     * @return Number of new stops clustered
     */
    // suspend fun clusterNewStops(newStopIds: List<Long>): Int  // FUTURE
}

/**
 * Result of DBSCAN clustering algorithm.
 *
 * Internal data structure returned by DBSCANAlgorithm, consumed by ClusterStopsUseCase.
 */
data class ClusteringResult(
    /**
     * Map of cluster_id to list of stop indices.
     *
     * Example: {1: [0, 1, 5], 2: [2, 3], 3: [4]}
     * - Cluster 1 contains stops at indices 0, 1, 5
     * - Cluster 2 contains stops at indices 2, 3
     * - Cluster 3 contains stop at index 4 (singleton noise point)
     */
    val clusters: Map<Int, List<Int>>,

    /**
     * Number of clusters created (excluding noise).
     *
     * Equals clusters.keys.size - noiseClusterCount
     */
    val clusterCount: Int,

    /**
     * Number of noise points (isolated stops).
     *
     * Each noise point gets its own singleton cluster.
     */
    val noiseCount: Int
) {
    /**
     * Total number of points clustered.
     */
    val totalPoints: Int
        get() = clusters.values.sumOf { it.size }

    /**
     * Check if all points are noise (no meaningful clusters).
     */
    val allNoise: Boolean
        get() = noiseCount == totalPoints
}
