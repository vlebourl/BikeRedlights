package com.example.bikeredlights.domain.model.settings

/**
 * User's auto-pause configuration for ride recording.
 *
 * When enabled, ride recording automatically pauses when the rider remains
 * stationary (speed < 1 km/h) for the configured threshold duration.
 *
 * Auto-resume occurs immediately when rider starts moving again (speed > 1 km/h).
 *
 * @property enabled Whether auto-pause is active
 * @property thresholdSeconds Duration (in seconds) rider must be stationary before auto-pause triggers
 *
 * @throws IllegalArgumentException if thresholdSeconds is not in VALID_THRESHOLDS
 */
data class AutoPauseConfig(
    val enabled: Boolean,
    val thresholdSeconds: Int
) {
    init {
        require(thresholdSeconds in VALID_THRESHOLDS) {
            "thresholdSeconds must be one of: ${VALID_THRESHOLDS.joinToString()}, got: $thresholdSeconds"
        }
    }

    companion object {
        /**
         * Valid threshold values (in seconds) - Feature 007 (v0.6.1).
         *
         * Granular options to accommodate different riding styles:
         * - 1s, 2s: Quick traffic light stops (urban cycling)
         * - 5s: Default, general purpose
         * - 10s, 15s: Longer stops at intersections
         * - 30s: Rest breaks, navigation checks
         *
         * Bug #10: Changed from minutes (1-15 min) to seconds for better UX.
         * v0.6.1: Updated to 6 specific timing options per user request.
         */
        val VALID_THRESHOLDS = listOf(1, 2, 5, 10, 15, 30)

        /**
         * Default configuration for new users.
         * Auto-pause is opt-in (disabled by default).
         * Default threshold: 5 seconds (general purpose, not too sensitive).
         */
        fun default() = AutoPauseConfig(
            enabled = false,
            thresholdSeconds = 5
        )

        /**
         * Create AutoPauseConfig from DataStore values.
         * Returns default if thresholdSeconds is invalid.
         */
        fun fromDataStore(enabled: Boolean, thresholdSeconds: Int): AutoPauseConfig {
            return if (thresholdSeconds in VALID_THRESHOLDS) {
                AutoPauseConfig(enabled, thresholdSeconds)
            } else {
                default()
            }
        }
    }

    /**
     * Get threshold duration in milliseconds for use in RideRecordingService.
     */
    fun getThresholdMs(): Long {
        return thresholdSeconds * 1000L
    }
}
