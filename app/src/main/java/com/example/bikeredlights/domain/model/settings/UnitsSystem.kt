package com.example.bikeredlights.domain.model.settings

/**
 * User's preferred measurement system for speed and distance display.
 *
 * This setting affects all speed/distance displays throughout the app:
 * - Current speed on Live tab
 * - Ride statistics (distance, average speed)
 * - Historical ride data
 *
 * Default: METRIC (km/h, km)
 */
enum class UnitsSystem {
    /**
     * Metric system: kilometers per hour (km/h) and kilometers (km).
     * Default value for all users.
     */
    METRIC,

    /**
     * Imperial system: miles per hour (mph) and miles (mi).
     * User must explicitly select this option.
     */
    IMPERIAL;

    companion object {
        /**
         * Default units system for new users.
         */
        val DEFAULT = METRIC

        /**
         * Parse units system from DataStore string value.
         * Returns DEFAULT if value is unrecognized.
         */
        fun fromString(value: String?): UnitsSystem {
            return when (value?.lowercase()) {
                "metric" -> METRIC
                "imperial" -> IMPERIAL
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
}
