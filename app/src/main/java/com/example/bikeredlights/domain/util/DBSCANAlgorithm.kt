package com.example.bikeredlights.domain.util

/**
 * DBSCAN (Density-Based Spatial Clustering of Applications with Noise) algorithm implementation.
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
 * @property clusters Map from cluster ID (1-based) to list of point indices
 * @property clusterCount Total number of clusters (including singleton noise clusters)
 * @property noiseCount Number of noise points (points in singleton clusters)
 */
data class ClusteringResult(
    val clusters: Map<Int, List<Int>>,
    val clusterCount: Int,
    val noiseCount: Int
)

/**
 * Naive O(n²) implementation of DBSCAN algorithm (Feature 010).
 *
 * Implementation Details:
 * - No spatial indexing (e.g., R-tree, K-D tree) for MVP
 * - Distance function called O(n²) times in worst case
 * - Acceptable performance for 1000 stops: 100-200ms
 *
 * Future Optimization:
 * - Add spatial indexing if profiling shows performance bottleneck
 * - Could reduce to O(n log n) average case with R-tree
 */
class DBSCANAlgorithmImpl : DBSCANAlgorithm {

    override fun cluster(
        pointCount: Int,
        epsilon: Float,
        minPts: Int,
        distanceFunction: (Int, Int) -> Float
    ): ClusteringResult {
        // Validate parameters
        require(pointCount >= 0) { "pointCount must be >= 0, got $pointCount" }
        require(epsilon > 0.0f) { "epsilon must be > 0, got $epsilon" }
        require(minPts >= 1) { "minPts must be >= 1, got $minPts" }

        // Edge case: empty dataset
        if (pointCount == 0) {
            return ClusteringResult(
                clusters = emptyMap(),
                clusterCount = 0,
                noiseCount = 0
            )
        }

        // Tracking structures
        val visited = BooleanArray(pointCount) { false }
        val clusterAssignment = IntArray(pointCount) { -1 } // -1 = unassigned
        var nextClusterId = 1 // 1-based cluster IDs
        var noiseCount = 0

        // Main DBSCAN loop
        for (pointIdx in 0 until pointCount) {
            if (visited[pointIdx]) continue

            visited[pointIdx] = true

            // Find neighbors within epsilon
            val neighbors = findNeighbors(pointIdx, pointCount, epsilon, distanceFunction)

            // Check if core point (>= minPts neighbors including self)
            if (neighbors.size >= minPts) {
                // Core point - expand cluster
                expandCluster(
                    pointIdx = pointIdx,
                    neighbors = neighbors.toMutableList(),
                    clusterId = nextClusterId,
                    visited = visited,
                    clusterAssignment = clusterAssignment,
                    pointCount = pointCount,
                    epsilon = epsilon,
                    minPts = minPts,
                    distanceFunction = distanceFunction
                )
                nextClusterId++
            } else {
                // Noise point (temporarily) - will be assigned singleton cluster later
                // Leave clusterAssignment[pointIdx] = -1
            }
        }

        // Assign noise points to singleton clusters (FR-012: all stops must have cluster_id)
        for (pointIdx in 0 until pointCount) {
            if (clusterAssignment[pointIdx] == -1) {
                clusterAssignment[pointIdx] = nextClusterId
                nextClusterId++
                noiseCount++
            }
        }

        // Build result map
        val clusters = clusterAssignment
            .mapIndexed { index, clusterId -> clusterId to index }
            .groupBy({ it.first }, { it.second })

        return ClusteringResult(
            clusters = clusters,
            clusterCount = clusters.size,
            noiseCount = noiseCount
        )
    }

    /**
     * Find all neighbors of a point within epsilon distance.
     *
     * **Semantics**: Inclusive comparison (distance <= epsilon, not <)
     *
     * @param pointIdx Index of the point to find neighbors for
     * @param pointCount Total number of points
     * @param epsilon Maximum distance for neighborhood
     * @param distanceFunction Distance calculation function
     * @return List of neighbor indices (including the point itself if distance=0)
     */
    private fun findNeighbors(
        pointIdx: Int,
        pointCount: Int,
        epsilon: Float,
        distanceFunction: (Int, Int) -> Float
    ): List<Int> {
        val neighbors = mutableListOf<Int>()

        for (candidateIdx in 0 until pointCount) {
            val distance = distanceFunction(pointIdx, candidateIdx)
            if (distance <= epsilon) { // INCLUSIVE comparison
                neighbors.add(candidateIdx)
            }
        }

        return neighbors
    }

    /**
     * Expand cluster from a core point by adding all density-reachable points.
     *
     * **Algorithm**:
     * 1. Assign core point to cluster
     * 2. For each neighbor in seed set:
     *    - Mark visited if unvisited
     *    - Assign to cluster if not already in a cluster
     *    - If neighbor is a core point, add its neighbors to seed set
     * 3. Repeat until seed set is exhausted
     *
     * @param pointIdx Index of the core point starting the cluster
     * @param neighbors Initial seed set of neighbors (mutable, will be expanded)
     * @param clusterId Cluster ID to assign
     * @param visited Array tracking which points have been visited
     * @param clusterAssignment Array tracking cluster assignments
     * @param pointCount Total number of points
     * @param epsilon Maximum distance for neighborhood
     * @param minPts Minimum points for core point
     * @param distanceFunction Distance calculation function
     */
    private fun expandCluster(
        pointIdx: Int,
        neighbors: MutableList<Int>,
        clusterId: Int,
        visited: BooleanArray,
        clusterAssignment: IntArray,
        pointCount: Int,
        epsilon: Float,
        minPts: Int,
        distanceFunction: (Int, Int) -> Float
    ) {
        // Assign core point to cluster
        clusterAssignment[pointIdx] = clusterId

        // Process seed set (neighbors list grows as we discover new density-reachable points)
        var seedIdx = 0
        while (seedIdx < neighbors.size) {
            val neighborIdx = neighbors[seedIdx]
            seedIdx++

            // Mark as visited if not already
            if (!visited[neighborIdx]) {
                visited[neighborIdx] = true

                // Find this neighbor's neighbors
                val neighborNeighbors = findNeighbors(neighborIdx, pointCount, epsilon, distanceFunction)

                // If this neighbor is a core point, add its neighbors to seed set
                if (neighborNeighbors.size >= minPts) {
                    for (nn in neighborNeighbors) {
                        if (!neighbors.contains(nn)) {
                            neighbors.add(nn)
                        }
                    }
                }
            }

            // Assign to cluster if not already in a cluster
            if (clusterAssignment[neighborIdx] == -1) {
                clusterAssignment[neighborIdx] = clusterId
            }
        }
    }
}
