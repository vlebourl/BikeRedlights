package com.example.bikeredlights.domain.model

/**
 * Domain model representing a single stationary period detected during a ride.
 *
 * Purpose: Technology-agnostic representation of stop data for business logic and UI layers.
 * No dependencies on Android framework or Room database.
 *
 * Immutability:
 * - Stop records are immutable after creation
 * - Only endTimestamp and durationSeconds are updated once when stop ends
 * - clusterId is set by Feature 010 clustering algorithm, never by stop detection
 *
 * Validation:
 * - endTimestamp must be ≥ startTimestamp if not null
 * - durationSeconds = (endTimestamp - startTimestamp) / 1000 when both present
 * - stopNumber must be unique per rideId (enforced at database layer)
 * - latitude in range [-90.0, 90.0]
 * - longitude in range [-180.0, 180.0]
 *
 * @property id Database ID (0 for unsaved stops, auto-generated on insert)
 * @property rideId Foreign key to parent ride
 * @property stopNumber Sequential number within ride (1, 2, 3...)
 * @property latitude GPS latitude at stop confirmation (decimal degrees)
 * @property longitude GPS longitude at stop confirmation (decimal degrees)
 * @property startTimestamp Unix epoch milliseconds when stop confirmed
 * @property endTimestamp Unix epoch milliseconds when movement resumed (null during active stop)
 * @property durationSeconds Calculated duration (null during active stop)
 * @property clusterId Cluster assignment from Feature 010 (null initially)
 */
data class Stop(
    val id: Long = 0,
    val rideId: Long,
    val stopNumber: Int,
    val latitude: Double,
    val longitude: Double,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val durationSeconds: Int? = null,
    val clusterId: Long? = null
) {
    /**
     * Check if this stop is currently active (not ended yet).
     */
    val isActive: Boolean
        get() = endTimestamp == null

    /**
     * Check if this stop has been ended (movement resumed).
     */
    val isEnded: Boolean
        get() = endTimestamp != null

    /**
     * Check if this stop has been assigned to a cluster (Feature 010).
     */
    val isClustered: Boolean
        get() = clusterId != null

    init {
        require(latitude in -90.0..90.0) { "Latitude must be in range [-90.0, 90.0], got $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be in range [-180.0, 180.0], got $longitude" }
        require(stopNumber > 0) { "Stop number must be positive, got $stopNumber" }

        if (endTimestamp != null) {
            require(endTimestamp >= startTimestamp) {
                "End timestamp ($endTimestamp) must be >= start timestamp ($startTimestamp)"
            }
        }

        if (endTimestamp != null && durationSeconds != null) {
            val expectedDuration = ((endTimestamp - startTimestamp) / 1000).toInt()
            require(durationSeconds == expectedDuration) {
                "Duration ($durationSeconds s) does not match timestamps (expected $expectedDuration s)"
            }
        }
    }

    companion object {
        /**
         * Calculate duration in seconds from start and end timestamps.
         *
         * @param startTimestamp Unix epoch milliseconds when stop started
         * @param endTimestamp Unix epoch milliseconds when stop ended
         * @return Duration in seconds
         */
        fun calculateDuration(startTimestamp: Long, endTimestamp: Long): Int {
            require(endTimestamp >= startTimestamp) {
                "End timestamp must be >= start timestamp"
            }
            return ((endTimestamp - startTimestamp) / 1000).toInt()
        }
    }
}
