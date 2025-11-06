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
         * Valid threshold values (in seconds).
         * Bug #10: Changed from minutes (1-15 min) to seconds (5-60 sec) for better UX.
         * Only discrete values are allowed for UI picker simplicity.
         */
        val VALID_THRESHOLDS = listOf(5, 10, 15, 20, 30, 45, 60)

        /**
         * Default configuration for new users.
         * Auto-pause is opt-in (disabled by default).
         * Default threshold: 30 seconds (reasonable for typical bike stops).
         */
        fun default() = AutoPauseConfig(
            enabled = false,
            thresholdSeconds = 30
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
