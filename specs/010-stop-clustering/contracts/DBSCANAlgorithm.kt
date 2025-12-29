/**
 * CONTRACT: DBSCANAlgorithm (Feature 010)
 *
 * This file documents the contract for the DBSCAN clustering algorithm.
 * Implementation: app/src/main/java/com/example/bikeredlights/domain/util/DBSCANAlgorithm.kt
 *
 * Purpose: Pure function implementation of DBSCAN density-based clustering.
 */

package com.example.bikeredlights.domain.util

/**
 * DBSCAN (Density-Based Spatial Clustering of Applications with Noise) algorithm.
 *
 * **Algorithm Overview**:
 * 1. For each unvisited point:
 *    - Find all neighbors within epsilon distance
 *    - If neighbors.size < minPts: mark as noise (temporarily)
 *    - Else: Create new cluster, expand by adding density-reachable points
 * 2. Border points join nearest core point's cluster
 * 3. Noise points remain unclustered (or assigned to singleton clusters)
 *
 * **Key Concepts**:
 * - **Core point**: Has >= minPts neighbors within epsilon (forms cluster center)
 * - **Border point**: Within epsilon of core point, but has < minPts neighbors
 * - **Noise point**: Not core or border (isolated)
 * - **Density-reachable**: Point P is density-reachable from Q if there's a chain of core points connecting them
 *
 * **Parameters**:
 * - **epsilon (ε)**: Maximum distance between two points to be neighbors (in meters for GPS)
 * - **minPts**: Minimum points required to form a cluster (typically 3 for 2D data)
 *
 * **Reference**: Ester, M., Kriegel, H. P., Sander, J., & Xu, X. (1996).
 *               "A density-based algorithm for discovering clusters in large spatial databases with noise."
 *               KDD-96 Proceedings.
 */
interface DBSCANAlgorithm {

    /**
     * Cluster 2D points using DBSCAN algorithm.
     *
     * **Contract**:
     * - MUST be a pure function (no side effects)
     * - MUST return deterministic results for same input
     * - MUST handle empty input (0 points) → empty clusters
     * - MUST handle single point → 1 cluster with 1 point (if minPts=1) or noise
     * - MUST use provided distance function for neighbor queries
     * - SHOULD be O(n²) time complexity for naive implementation
     * - MAY be optimized to O(n log n) with spatial indexing (future)
     *
     * **Epsilon Semantics**:
     * - Distance comparison is INCLUSIVE: distance <= epsilon (not <)
     * - Example: If epsilon=20.0m and distance=20.0m exactly, points are neighbors
     *
     * **MinPts Semantics**:
     * - Comparison is INCLUSIVE: neighborCount >= minPts (not >)
     * - Example: If minPts=3 and point has 3 neighbors (including itself), it's a core point
     * - Self-counting: Point is always its own neighbor (distance=0)
     *
     * **Noise Handling**:
     * - Noise points (isolated) are assigned to singleton clusters
     * - Each noise point gets unique cluster ID
     * - Rationale: Spec requires all stops have cluster_id (FR-012)
     *
     * **Algorithm Steps**:
     * 1. Initialize all points as unvisited
     * 2. For each unvisited point P:
     *    a. Mark P as visited
     *    b. Find neighbors N = {Q | distance(P, Q) <= epsilon}
     *    c. If |N| < minPts: mark P as noise (temp)
     *    d. Else: Create cluster C, add P to C, expand cluster:
     *       - For each neighbor Q in N:
     *         * If Q unvisited: mark visited, find Q's neighbors, merge with N
     *         * If Q not in any cluster: add Q to C
     * 3. Assign noise points to singleton clusters
     *
     * **Performance**:
     * - Time: O(n²) where n = points.size (naive implementation)
     * - Space: O(n) for visited set + cluster assignments
     * - Typical: 10-20ms for 1000 points on modern mobile CPU
     *
     * **Example Usage**:
     * ```kotlin
     * val stops = listOf(
     *     Stop(lat=37.422, lon=-122.084, ...),  // Stop 0
     *     Stop(lat=37.423, lon=-122.085, ...),  // Stop 1 (within 20m of 0)
     *     Stop(lat=37.424, lon=-122.086, ...)   // Stop 2 (within 20m of 1)
     * )
     * val distanceFn = { i: Int, j: Int ->
     *     haversineDistance(stops[i].latitude, stops[i].longitude,
     *                       stops[j].latitude, stops[j].longitude)
     * }
     * val result = dbscan(stops.size, epsilon=20.0f, minPts=3, distanceFn)
     * // result.clusters = {1: [0, 1, 2]}  // All in cluster 1
     * ```
     *
     * @param pointCount Total number of points to cluster
     * @param epsilon Maximum distance for two points to be neighbors (in same units as distanceFunction)
     * @param minPts Minimum points required to form a dense cluster (typically 3 for 2D)
     * @param distanceFunction Function returning distance between point i and point j
     * @return ClusteringResult with cluster assignments
     * @throws IllegalArgumentException if pointCount < 0, epsilon <= 0, or minPts < 1
     */
    fun cluster(
        pointCount: Int,
        epsilon: Float,
        minPts: Int,
        distanceFunction: (Int, Int) -> Float
    ): ClusteringResult
}

/**
 * Result of DBSCAN clustering.
 *
 * See ClusterStopsUseCase.kt contract for full documentation.
 */
data class ClusteringResult(
    val clusters: Map<Int, List<Int>>,
    val clusterCount: Int,
    val noiseCount: Int
)

/**
 * Point classification in DBSCAN.
 *
 * Internal enum for algorithm implementation (not exposed in public API).
 */
internal enum class PointType {
    /** Unvisited point (initial state) */
    UNVISITED,

    /** Core point (has >= minPts neighbors) */
    CORE,

    /** Border point (within epsilon of core, but < minPts neighbors) */
    BORDER,

    /** Noise point (isolated, assigned to singleton cluster) */
    NOISE
}
