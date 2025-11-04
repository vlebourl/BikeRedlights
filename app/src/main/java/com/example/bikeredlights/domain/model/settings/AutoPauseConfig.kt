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
 * @property thresholdMinutes Duration (in minutes) rider must be stationary before auto-pause triggers
 *
 * @throws IllegalArgumentException if thresholdMinutes is not in VALID_THRESHOLDS
 */
data class AutoPauseConfig(
    val enabled: Boolean,
    val thresholdMinutes: Int
) {
    init {
        require(thresholdMinutes in VALID_THRESHOLDS) {
            "thresholdMinutes must be one of: ${VALID_THRESHOLDS.joinToString()}, got: $thresholdMinutes"
        }
    }

    companion object {
        /**
         * Valid threshold values (in minutes).
         * Only discrete values are allowed for UI picker simplicity.
         */
        val VALID_THRESHOLDS = listOf(1, 2, 3, 5, 10, 15)

        /**
         * Default configuration for new users.
         * Auto-pause is opt-in (disabled by default).
         */
        fun default() = AutoPauseConfig(
            enabled = false,
            thresholdMinutes = 5
        )

        /**
         * Create AutoPauseConfig from DataStore values.
         * Returns default if thresholdMinutes is invalid.
         */
        fun fromDataStore(enabled: Boolean, thresholdMinutes: Int): AutoPauseConfig {
            return if (thresholdMinutes in VALID_THRESHOLDS) {
                AutoPauseConfig(enabled, thresholdMinutes)
            } else {
                default()
            }
        }
    }

    /**
     * Get threshold duration in milliseconds for use in TrackLocationUseCase.
     */
    fun getThresholdMs(): Long {
        return thresholdMinutes * 60 * 1000L
    }
}
