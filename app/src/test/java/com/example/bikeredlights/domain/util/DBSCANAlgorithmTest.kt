package com.example.bikeredlights.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DBSCANAlgorithm implementation (Feature 010).
 *
 * Tests verify correctness of DBSCAN (Density-Based Spatial Clustering) algorithm
 * for geospatial stop clustering.
 *
 * Test Strategy:
 * - Empty/minimal datasets
 * - Core clustering behavior (dense regions)
 * - Noise detection (isolated points)
 * - Border points (density-reachable from core)
 * - Parameter sensitivity (epsilon, minPts)
 * - Edge cases (all noise, all one cluster)
 *
 * DBSCAN Parameters for Stop Clustering:
 * - epsilon = 20.0m (clustering radius from settings)
 * - minPts = 3 (standard for 2D geospatial data)
 */
class DBSCANAlgorithmTest {

    private lateinit var algorithm: DBSCANAlgorithm

    @Before
    fun setup() {
        algorithm = DBSCANAlgorithmImpl()
    }

    /**
     * Test: Empty dataset returns empty clusters.
     *
     * Validates: Edge case with zero points
     */
    @Test
    fun `empty dataset returns empty clusters`() {
        val result = algorithm.cluster(
            pointCount = 0,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { _, _ -> 0.0f }
        )

        assertThat(result.clusters).isEmpty()
        assertThat(result.clusterCount).isEqualTo(0)
        assertThat(result.noiseCount).isEqualTo(0)
    }

    /**
     * Test: Single point forms singleton cluster.
     *
     * Validates: Noise points get unique cluster IDs (spec FR-012)
     */
    @Test
    fun `single point forms singleton cluster`() {
        val result = algorithm.cluster(
            pointCount = 1,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> if (i == j) 0.0f else Float.MAX_VALUE }
        )

        assertThat(result.clusters).hasSize(1)
        assertThat(result.clusterCount).isEqualTo(1)
        assertThat(result.noiseCount).isEqualTo(1) // Isolated point is noise
        assertThat(result.clusters.values.flatten()).containsExactly(0)
    }

    /**
     * Test: Three points within epsilon form one cluster.
     *
     * Validates: Core clustering behavior with minPts=3
     *
     * Scenario:
     * - Points 0, 1, 2 all within 10m of each other
     * - With epsilon=20m and minPts=3, they form a dense cluster
     * - All three are core points (each has 2 neighbors + self = 3)
     */
    @Test
    fun `three points within epsilon form one cluster`() {
        // Distance matrix: all points within 10m
        val distances = arrayOf(
            arrayOf(0.0f, 10.0f, 10.0f), // Point 0 distances
            arrayOf(10.0f, 0.0f, 10.0f), // Point 1 distances
            arrayOf(10.0f, 10.0f, 0.0f)  // Point 2 distances
        )

        val result = algorithm.cluster(
            pointCount = 3,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        assertThat(result.clusters).hasSize(1)
        assertThat(result.clusterCount).isEqualTo(1)
        assertThat(result.noiseCount).isEqualTo(0)
        assertThat(result.clusters.values.first()).containsExactly(0, 1, 2)
    }

    /**
     * Test: Four points in a line - middle two are core, outer two are border.
     *
     * Validates: Border point detection (density-reachable from core)
     *
     * Scenario: Points arranged as: P0 --- P1 --- P2 --- P3
     * - Distance between neighbors: 10m
     * - Epsilon: 20m (reaches 1 neighbor away, not 2)
     * - MinPts: 3
     *
     * Expected:
     * - P1 is core (has P0, P1, P2 within epsilon = 3 points)
     * - P2 is core (has P1, P2, P3 within epsilon = 3 points)
     * - P0 is border (within epsilon of P1, but only 2 neighbors)
     * - P3 is border (within epsilon of P2, but only 2 neighbors)
     * - All in one cluster (density-connected via P1-P2 core chain)
     */
    @Test
    fun `four points in a line form one cluster`() {
        // P0 --- P1 --- P2 --- P3 (10m between each)
        val distances = arrayOf(
            arrayOf(0.0f, 10.0f, 20.0f, 30.0f), // P0: only P1 within epsilon=20
            arrayOf(10.0f, 0.0f, 10.0f, 20.0f), // P1: P0, P2 within epsilon (core!)
            arrayOf(20.0f, 10.0f, 0.0f, 10.0f), // P2: P1, P3 within epsilon (core!)
            arrayOf(30.0f, 20.0f, 10.0f, 0.0f)  // P3: only P2 within epsilon
        )

        val result = algorithm.cluster(
            pointCount = 4,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        assertThat(result.clusters).hasSize(1)
        assertThat(result.clusterCount).isEqualTo(1)
        assertThat(result.noiseCount).isEqualTo(0)
        assertThat(result.clusters.values.first()).containsExactly(0, 1, 2, 3)
    }

    /**
     * Test: Two separate clusters (well-separated groups).
     *
     * Validates: Multiple cluster detection
     *
     * Scenario: Two groups of 3 points each, 100m apart
     * - Group 1: P0, P1, P2 (all within 10m)
     * - Group 2: P3, P4, P5 (all within 10m)
     * - Groups are 100m apart (> epsilon=20m)
     */
    @Test
    fun `two separate clusters detected`() {
        // Group 1: P0, P1, P2 (within 10m)
        // Group 2: P3, P4, P5 (within 10m)
        // Groups separated by 100m
        val distances = arrayOf(
            arrayOf(0.0f, 10.0f, 10.0f, 100.0f, 100.0f, 100.0f), // P0
            arrayOf(10.0f, 0.0f, 10.0f, 100.0f, 100.0f, 100.0f), // P1
            arrayOf(10.0f, 10.0f, 0.0f, 100.0f, 100.0f, 100.0f), // P2
            arrayOf(100.0f, 100.0f, 100.0f, 0.0f, 10.0f, 10.0f), // P3
            arrayOf(100.0f, 100.0f, 100.0f, 10.0f, 0.0f, 10.0f), // P4
            arrayOf(100.0f, 100.0f, 100.0f, 10.0f, 10.0f, 0.0f)  // P5
        )

        val result = algorithm.cluster(
            pointCount = 6,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        assertThat(result.clusters).hasSize(2)
        assertThat(result.clusterCount).isEqualTo(2)
        assertThat(result.noiseCount).isEqualTo(0)

        // Verify both groups are clustered
        val allPoints = result.clusters.values.flatten().sorted()
        assertThat(allPoints).containsExactly(0, 1, 2, 3, 4, 5)
    }

    /**
     * Test: Isolated noise points get singleton clusters.
     *
     * Validates: Noise handling (FR-012: all stops must have cluster_id)
     *
     * Scenario:
     * - 3 points, all > 50m apart (epsilon=20m)
     * - No point has >= minPts neighbors
     * - All are noise, each gets unique cluster ID
     */
    @Test
    fun `isolated noise points get singleton clusters`() {
        // All points > 50m apart (epsilon=20m)
        val distances = arrayOf(
            arrayOf(0.0f, 50.0f, 50.0f),
            arrayOf(50.0f, 0.0f, 50.0f),
            arrayOf(50.0f, 50.0f, 0.0f)
        )

        val result = algorithm.cluster(
            pointCount = 3,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        assertThat(result.clusters).hasSize(3)
        assertThat(result.clusterCount).isEqualTo(3)
        assertThat(result.noiseCount).isEqualTo(3)

        // Each noise point gets its own cluster
        assertThat(result.clusters.values.map { it.size }).containsExactly(1, 1, 1)
    }

    /**
     * Test: Mixed cluster and noise.
     *
     * Validates: Noise detection when some points form clusters
     *
     * Scenario:
     * - P0, P1, P2 form a cluster (within 10m)
     * - P3 is isolated (50m away)
     * - P3 becomes noise (singleton cluster)
     */
    @Test
    fun `cluster with isolated noise point`() {
        val distances = arrayOf(
            arrayOf(0.0f, 10.0f, 10.0f, 50.0f), // P0
            arrayOf(10.0f, 0.0f, 10.0f, 50.0f), // P1
            arrayOf(10.0f, 10.0f, 0.0f, 50.0f), // P2
            arrayOf(50.0f, 50.0f, 50.0f, 0.0f)  // P3 (isolated)
        )

        val result = algorithm.cluster(
            pointCount = 4,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        assertThat(result.clusters).hasSize(2)
        assertThat(result.clusterCount).isEqualTo(2)
        assertThat(result.noiseCount).isEqualTo(1)

        // One cluster with 3 points, one singleton cluster
        val clusterSizes = result.clusters.values.map { it.size }.sorted()
        assertThat(clusterSizes).containsExactly(1, 3).inOrder()
    }

    /**
     * Test: Epsilon boundary condition (distance exactly equals epsilon).
     *
     * Validates: Inclusive epsilon comparison (distance <= epsilon, not <)
     *
     * Scenario: Three points exactly 20m apart with epsilon=20m
     * - Per contract, distance=20.0 should be WITHIN epsilon
     * - All three should form one cluster
     */
    @Test
    fun `epsilon boundary - points exactly at epsilon distance`() {
        // All points exactly 20m apart (epsilon=20m)
        val distances = arrayOf(
            arrayOf(0.0f, 20.0f, 20.0f),
            arrayOf(20.0f, 0.0f, 20.0f),
            arrayOf(20.0f, 20.0f, 0.0f)
        )

        val result = algorithm.cluster(
            pointCount = 3,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        // All points should be neighbors (inclusive comparison)
        assertThat(result.clusters).hasSize(1)
        assertThat(result.clusterCount).isEqualTo(1)
        assertThat(result.noiseCount).isEqualTo(0)
        assertThat(result.clusters.values.first()).containsExactly(0, 1, 2)
    }

    /**
     * Test: MinPts boundary condition (exactly minPts neighbors).
     *
     * Validates: Inclusive minPts comparison (neighborCount >= minPts, not >)
     *
     * Scenario: Point with exactly 2 neighbors (+ self = 3 total) with minPts=3
     * - Per contract, neighborCount=3 should be a CORE point
     */
    @Test
    fun `minPts boundary - point with exactly minPts neighbors`() {
        // P0 has exactly 2 neighbors: P1, P2 (+ self = 3 total, meets minPts=3)
        val distances = arrayOf(
            arrayOf(0.0f, 10.0f, 10.0f, 50.0f), // P0: 2 neighbors (+ self = 3)
            arrayOf(10.0f, 0.0f, 10.0f, 10.0f), // P1: 3 neighbors (+ self = 4, core!)
            arrayOf(10.0f, 10.0f, 0.0f, 10.0f), // P2: 3 neighbors (+ self = 4, core!)
            arrayOf(50.0f, 10.0f, 10.0f, 0.0f)  // P3: 2 neighbors (+ self = 3)
        )

        val result = algorithm.cluster(
            pointCount = 4,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        // P1 and P2 are core, P0 and P3 join their cluster
        assertThat(result.clusters).hasSize(1)
        assertThat(result.clusterCount).isEqualTo(1)
        assertThat(result.noiseCount).isEqualTo(0)
    }

    /**
     * Test: Deterministic results for same input.
     *
     * Validates: Algorithm produces consistent clustering for identical inputs
     */
    @Test
    fun `deterministic results for same input`() {
        val distances = arrayOf(
            arrayOf(0.0f, 10.0f, 10.0f),
            arrayOf(10.0f, 0.0f, 10.0f),
            arrayOf(10.0f, 10.0f, 0.0f)
        )

        val result1 = algorithm.cluster(
            pointCount = 3,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        val result2 = algorithm.cluster(
            pointCount = 3,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        // Results should be identical
        assertThat(result1.clusters).isEqualTo(result2.clusters)
        assertThat(result1.clusterCount).isEqualTo(result2.clusterCount)
        assertThat(result1.noiseCount).isEqualTo(result2.noiseCount)
    }

    /**
     * Test: Large dataset performance (1000 points).
     *
     * Validates: O(n²) naive implementation completes in reasonable time
     *
     * Expected: <200ms for 1000 points (per quickstart.md performance target)
     */
    @Test(timeout = 500) // 500ms timeout (2.5x target for safety margin)
    fun `large dataset performance - 1000 points`() {
        // Create 10 clusters of 100 points each
        val pointCount = 1000
        val clusterSize = 100
        val clusterCount = 10

        val distances = Array(pointCount) { FloatArray(pointCount) }
        for (i in 0 until pointCount) {
            for (j in 0 until pointCount) {
                val clusterI = i / clusterSize
                val clusterJ = j / clusterSize

                distances[i][j] = if (i == j) {
                    0.0f
                } else if (clusterI == clusterJ) {
                    10.0f // Same cluster: within epsilon
                } else {
                    100.0f // Different cluster: outside epsilon
                }
            }
        }

        val startTime = System.currentTimeMillis()

        val result = algorithm.cluster(
            pointCount = pointCount,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { i, j -> distances[i][j] }
        )

        val elapsed = System.currentTimeMillis() - startTime

        assertThat(result.clusterCount).isEqualTo(clusterCount)
        assertThat(result.noiseCount).isEqualTo(0)
        println("1000 points clustered in ${elapsed}ms (target: <200ms)")
    }

    /**
     * Test: Invalid parameters throw IllegalArgumentException.
     *
     * Validates: Contract preconditions are enforced
     */
    @Test(expected = IllegalArgumentException::class)
    fun `negative pointCount throws exception`() {
        algorithm.cluster(
            pointCount = -1,
            epsilon = 20.0f,
            minPts = 3,
            distanceFunction = { _, _ -> 0.0f }
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero epsilon throws exception`() {
        algorithm.cluster(
            pointCount = 5,
            epsilon = 0.0f,
            minPts = 3,
            distanceFunction = { _, _ -> 0.0f }
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative epsilon throws exception`() {
        algorithm.cluster(
            pointCount = 5,
            epsilon = -10.0f,
            minPts = 3,
            distanceFunction = { _, _ -> 0.0f }
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero minPts throws exception`() {
        algorithm.cluster(
            pointCount = 5,
            epsilon = 20.0f,
            minPts = 0,
            distanceFunction = { _, _ -> 0.0f }
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative minPts throws exception`() {
        algorithm.cluster(
            pointCount = 5,
            epsilon = 20.0f,
            minPts = -1,
            distanceFunction = { _, _ -> 0.0f }
        )
    }
}
