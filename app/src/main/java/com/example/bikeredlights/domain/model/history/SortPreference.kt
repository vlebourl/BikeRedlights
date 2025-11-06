package com.example.bikeredlights.domain.model.history

/**
 * User preference for ride list sort order.
 *
 * Persisted to DataStore to maintain sort preference across app sessions.
 * Default sorting is NEWEST_FIRST for typical chronological browsing.
 *
 * Sorting behavior:
 * - Primary sort: By the selected criterion
 * - Secondary sort: By startTime DESC (for stable ordering when primary values are equal)
 *
 * @property displayName Human-readable label for UI display
 */
enum class SortPreference(val displayName: String) {
    /**
     * Most recent rides first (default).
     * Sort: startTime DESC
     */
    NEWEST_FIRST("Newest First"),

    /**
     * Oldest rides first.
     * Sort: startTime ASC
     */
    OLDEST_FIRST("Oldest First"),

    /**
     * Longest distance rides first.
     * Sort: distanceMeters DESC, startTime DESC
     */
    LONGEST_DISTANCE("Longest Distance"),

    /**
     * Longest duration rides first.
     * Sort: movingDurationMillis DESC, startTime DESC
     */
    LONGEST_DURATION("Longest Duration");

    companion object {
        /**
         * Default sort preference when none is set.
         */
        val DEFAULT = NEWEST_FIRST

        /**
         * Converts a string name to enum value, with fallback to default.
         *
         * @param name Enum name string (e.g., "NEWEST_FIRST")
         * @return Matching enum value or DEFAULT if not found
         */
        fun fromString(name: String?): SortPreference {
            return entries.find { it.name == name } ?: DEFAULT
        }
    }
}
