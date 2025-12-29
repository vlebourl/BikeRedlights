package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.repository.StopRepository
import com.example.bikeredlights.domain.util.DBSCANAlgorithm
import com.example.bikeredlights.domain.util.haversineDistance
import javax.inject.Inject

/**
 * Use case for clustering stops using DBSCAN algorithm (Feature 010).
 *
 * **Responsibility**: Orchestrate stop clustering workflow
 * - Fetch all stops from repository
 * - Apply DBSCAN clustering with Haversine distance
 * - Update cluster IDs in database
 *
 * **Dependencies**:
 * - StopRepository: Fetch stops and persist cluster assignments
 * - DBSCANAlgorithm: Clustering logic
 * - haversineDistance: GPS distance calculation
 *
 * **Parameters**:
 * - epsilonMeters: Clustering radius (from settings, default 20m)
 * - minPts: Minimum points per cluster (default 3 for 2D data)
 */
class ClusterStopsUseCase @Inject constructor(
    private val stopRepository: StopRepository,
    private val dbscanAlgorithm: DBSCANAlgorithm
) {

    /**
     * Cluster all stops across all rides.
     *
     * **Algorithm**:
     * 1. Fetch all stops from database
     * 2. Build distance matrix using Haversine formula
     * 3. Run DBSCAN clustering
     * 4. Update cluster_id in database for each cluster
     *
     * **Performance**: O(n²) for n stops due to distance calculations
     *
     * @param epsilonMeters Clustering radius in meters (default: 20m from settings)
     * @param minPts Minimum points to form a cluster (default: 3)
     */
    suspend operator fun invoke(
        epsilonMeters: Float = 20.0f,
        minPts: Int = 3
    ) {
        // Step 1: Fetch all stops
        val stops = stopRepository.getAllStops()

        if (stops.isEmpty()) {
            return // Nothing to cluster
        }

        // Step 2: Create distance function using Haversine
        val distanceFunction = { i: Int, j: Int ->
            haversineDistance(
                lat1 = stops[i].latitude,
                lon1 = stops[i].longitude,
                lat2 = stops[j].latitude,
                lon2 = stops[j].longitude
            )
        }

        // Step 3: Run DBSCAN clustering
        val clusteringResult = dbscanAlgorithm.cluster(
            pointCount = stops.size,
            epsilon = epsilonMeters,
            minPts = minPts,
            distanceFunction = distanceFunction
        )

        // Step 4: Update cluster IDs in database
        for ((clusterId, pointIndices) in clusteringResult.clusters) {
            val stopIds = pointIndices.map { stops[it].id }
            stopRepository.updateClusterIds(clusterId.toLong(), stopIds)
        }
    }
}
