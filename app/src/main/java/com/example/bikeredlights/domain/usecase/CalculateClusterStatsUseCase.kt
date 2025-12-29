package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.ClusterSummary
import com.example.bikeredlights.domain.model.Stop
import javax.inject.Inject

/**
 * Calculates aggregate statistics for stop clusters.
 *
 * Created by Feature 011: Stop Cluster Visualization.
 * Transforms raw list of stops into ClusterSummary objects with calculated metrics.
 *
 * Business Rules:
 * - Groups stops by cluster_id
 * - Calculates cluster center as arithmetic mean of GPS coordinates
 * - Calculates average duration (only for completed stops with endTime != null)
 * - Generates frequency analytics text based on stop timestamps
 * - Each cluster must have >= 2 stops (DBSCAN minimum)
 *
 * Dependencies:
 * - CalculateClusterCenterUseCase: GPS coordinate averaging
 * - FormatClusterAnalyticsUseCase: Frequency text generation
 *
 * Algorithm:
 * 1. Validate all stops have cluster_id != null
 * 2. Group stops by cluster_id
 * 3. For each cluster group:
 *    a. Calculate center using CalculateClusterCenterUseCase
 *    b. Calculate average duration (exclude active stops)
 *    c. Generate frequency text using FormatClusterAnalyticsUseCase
 *    d. Create ClusterSummary with all calculated values
 * 4. Sort clusters by stopCount descending
 *
 * Performance: <100ms for 100 clusters (pure computation, default dispatcher)
 *
 * @param stops List of stops to aggregate (must have cluster_id != null)
 * @return List of ClusterSummary entities with calculated statistics, sorted by stop count
 * @throws IllegalArgumentException if any stop has cluster_id == null
 */
class CalculateClusterStatsUseCase @Inject constructor(
    private val calculateClusterCenterUseCase: CalculateClusterCenterUseCase,
    private val formatClusterAnalyticsUseCase: FormatClusterAnalyticsUseCase
) {
    operator fun invoke(stops: List<Stop>): List<ClusterSummary> {
        // Validate all stops have cluster_id
        require(stops.all { it.clusterId != null }) {
            "Cannot calculate stats for stops without cluster_id. All stops must be clustered."
        }

        if (stops.isEmpty()) {
            return emptyList()
        }

        // Group stops by cluster_id (safe cast: validated above)
        val grouped: Map<Long, List<Stop>> = stops.groupBy { it.clusterId!! }

        // Calculate stats for each cluster
        val clusterSummaries = grouped.map { (clusterId, clusterStops) ->
            val center = calculateClusterCenterUseCase(clusterStops)
            val stopCount = clusterStops.size

            // Calculate average duration (only completed stops with endTimestamp != null)
            val completedStops = clusterStops.filter { it.endTimestamp != null }
            val averageDuration = if (completedStops.isNotEmpty()) {
                completedStops.map { stop ->
                    val duration = (stop.endTimestamp!! - stop.startTimestamp) / 1000 // Convert ms to seconds
                    duration
                }.average().toLong()
            } else {
                0L
            }

            // Generate frequency text
            val stopTimestamps = clusterStops.map { it.startTimestamp }
            val frequencyText = formatClusterAnalyticsUseCase(
                stopCount = stopCount,
                stopTimestamps = stopTimestamps
            )

            ClusterSummary(
                clusterId = clusterId,
                centerPosition = center,
                stopCount = stopCount,
                averageDuration = averageDuration,
                frequencyText = frequencyText,
                stops = clusterStops
            )
        }

        // Sort by stop count descending (most frequent clusters first)
        return clusterSummaries.sortedByDescending { it.stopCount }
    }
}
