package com.example.bikeredlights.domain.model.history

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Date range filter for ride history list.
 *
 * Supports preset filters (Last 7 Days, Last 30 Days, etc.) and custom date ranges.
 * Persisted to DataStore to maintain filter preference across app sessions.
 *
 * @property type Filter type (preset or custom)
 * @property customStartDate Start date for CUSTOM filter (nullable, epoch millis)
 * @property customEndDate End date for CUSTOM filter (nullable, epoch millis)
 */
data class DateRangeFilter(
    val type: FilterType = FilterType.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null
) {
    /**
     * Preset and custom filter types.
     *
     * @property displayName Human-readable label for UI display
     * @property requiresCustomDates True if filter requires customStartDate/customEndDate
     */
    enum class FilterType(
        val displayName: String,
        val requiresCustomDates: Boolean = false
    ) {
        /** Show all rides (no filtering) */
        ALL_TIME("All Time", requiresCustomDates = false),

        /** Show rides from last 7 days */
        LAST_7_DAYS("Last 7 Days", requiresCustomDates = false),

        /** Show rides from last 30 days */
        LAST_30_DAYS("Last 30 Days", requiresCustomDates = false),

        /** Show rides from current calendar year */
        THIS_YEAR("This Year", requiresCustomDates = false),

        /** Show rides within user-specified date range */
        CUSTOM("Custom Range", requiresCustomDates = true);

        companion object {
            /**
             * Default filter type when none is set.
             */
            val DEFAULT = ALL_TIME

            /**
             * Converts a string name to enum value, with fallback to default.
             *
             * @param name Enum name string (e.g., "LAST_7_DAYS")
             * @return Matching enum value or DEFAULT if not found
             */
            fun fromString(name: String?): FilterType {
                return entries.find { it.name == name } ?: DEFAULT
            }
        }
    }

    /**
     * Calculates the effective date range for filtering rides.
     *
     * Returns a pair of (startMillis, endMillis) representing the filter bounds.
     * For preset filters, calculates relative to current time.
     * For custom filters, uses provided dates.
     *
     * @return Pair of start/end timestamps in epoch millis, or null if ALL_TIME
     */
    fun getEffectiveDateRange(): Pair<Long, Long>? {
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()

        return when (type) {
            FilterType.ALL_TIME -> null

            FilterType.LAST_7_DAYS -> {
                val start = now.minus(7, ChronoUnit.DAYS).toEpochMilli()
                val end = now.toEpochMilli()
                Pair(start, end)
            }

            FilterType.LAST_30_DAYS -> {
                val start = now.minus(30, ChronoUnit.DAYS).toEpochMilli()
                val end = now.toEpochMilli()
                Pair(start, end)
            }

            FilterType.THIS_YEAR -> {
                val currentYear = LocalDate.now(zoneId).year
                val startOfYear = LocalDate.of(currentYear, 1, 1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
                val endOfYear = LocalDate.of(currentYear, 12, 31)
                    .atTime(23, 59, 59)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
                Pair(startOfYear, endOfYear)
            }

            FilterType.CUSTOM -> {
                if (customStartDate != null && customEndDate != null) {
                    Pair(customStartDate, customEndDate)
                } else {
                    // Invalid custom filter, treat as ALL_TIME
                    null
                }
            }
        }
    }

    /**
     * Checks if the filter is currently active (not ALL_TIME).
     *
     * @return True if filter should be applied to ride list
     */
    fun isActive(): Boolean = type != FilterType.ALL_TIME

    companion object {
        /**
         * Default filter (show all rides).
         */
        val DEFAULT = DateRangeFilter(type = FilterType.ALL_TIME)
    }
}
