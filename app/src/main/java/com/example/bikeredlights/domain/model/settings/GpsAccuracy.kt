package com.example.bikeredlights.domain.model.settings

/**
 * User's preferred GPS update frequency, balancing accuracy vs battery life.
 *
 * This setting configures the LocationRepository update interval:
 * - HIGH_ACCURACY: 1000ms (1 second) - best precision, higher battery drain
 * - BATTERY_SAVER: 4000ms (4 seconds) - good precision, lower battery drain
 *
 * Default: HIGH_ACCURACY
 */
enum class GpsAccuracy {
    /**
     * Battery Saver mode: location updates every 4 seconds.
     * Optimizes battery life for long delivery shifts.
     * Track precision is still good for ride recording.
     */
    BATTERY_SAVER,

    /**
     * High Accuracy mode: location updates every 1 second.
     * Best precision for speed tracking and route recording.
     * Default mode for most users.
     */
    HIGH_ACCURACY;

    companion object {
        /**
         * Default GPS accuracy for new users.
         */
        val DEFAULT = HIGH_ACCURACY

        /**
         * Parse GPS accuracy from DataStore string value.
         * Returns DEFAULT if value is unrecognized.
         */
        fun fromString(value: String?): GpsAccuracy {
            return when (value?.lowercase()) {
                "battery_saver" -> BATTERY_SAVER
                "high_accuracy" -> HIGH_ACCURACY
                else -> DEFAULT
            }
        }
    }

    /**
     * Convert to DataStore string value.
     */
    fun toDataStoreValue(): String {
        return name.lowercase()
    }

    /**
     * Get location update interval in milliseconds.
     */
    fun getUpdateIntervalMs(): Long {
        return when (this) {
            BATTERY_SAVER -> 4000L  // 4 seconds
            HIGH_ACCURACY -> 1000L   // 1 second
        }
    }
}
