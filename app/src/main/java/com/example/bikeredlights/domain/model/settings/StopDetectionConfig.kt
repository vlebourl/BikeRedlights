package com.example.bikeredlights.domain.model.settings

/**
 * Configuration for stop detection parameters.
 *
 * Used by:
 * - Feature 009 (Stop Detection): speed and duration thresholds
 * - Feature 010 (Clustering): clustering radius
 *
 * @param speedThresholdKmh Speed below which rider is considered stopped (1-5 km/h)
 * @param durationThresholdSeconds Minimum time stationary to count as stop (5-30 seconds)
 * @param clusteringRadiusMeters Distance to group stops as same location (10-50 meters)
 * @throws IllegalArgumentException if any value is not in the valid set
 */
data class StopDetectionConfig(
    val speedThresholdKmh: Float = DEFAULT_SPEED_THRESHOLD_KMH,
    val durationThresholdSeconds: Int = DEFAULT_DURATION_THRESHOLD_SECONDS,
    val clusteringRadiusMeters: Int = DEFAULT_CLUSTERING_RADIUS_METERS
) {
    companion object {
        const val DEFAULT_SPEED_THRESHOLD_KMH = 3f
        const val DEFAULT_DURATION_THRESHOLD_SECONDS = 15
        const val DEFAULT_CLUSTERING_RADIUS_METERS = 20

        val VALID_SPEED_THRESHOLDS = listOf(1f, 2f, 3f, 4f, 5f)
        val VALID_DURATION_THRESHOLDS = listOf(5, 10, 15, 20, 25, 30)
        val VALID_CLUSTERING_RADII = listOf(10, 15, 20, 25, 30, 40, 50)
    }

    init {
        require(speedThresholdKmh in VALID_SPEED_THRESHOLDS) {
            "Invalid speed threshold: $speedThresholdKmh km/h. Must be one of: ${VALID_SPEED_THRESHOLDS.joinToString()}"
        }
        require(durationThresholdSeconds in VALID_DURATION_THRESHOLDS) {
            "Invalid duration threshold: $durationThresholdSeconds seconds. Must be one of: ${VALID_DURATION_THRESHOLDS.joinToString()}"
        }
        require(clusteringRadiusMeters in VALID_CLUSTERING_RADII) {
            "Invalid clustering radius: $clusteringRadiusMeters meters. Must be one of: ${VALID_CLUSTERING_RADII.joinToString()}"
        }
    }
}
