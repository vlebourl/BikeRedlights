package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable
import java.util.concurrent.TimeUnit

/**
 * Domain model for cluster filtering criteria.
 *
 * Created by Feature 011: Stop Cluster Visualization (User Story 3).
 * Used to filter displayed clusters by date range and minimum cluster size.
 *
 * Default Values:
 * - dateRange = null (show all time)
 * - minClusterSize = 2 (DBSCAN minimum, show all clusters)
 *
 * @property dateRange Optional time window filter (null = all time)
 * @property minClusterSize Minimum number of stops required in cluster (default = 2)
 */
@Immutable
data class StopClusterFilter(
    val dateRange: DateRange? = null,
    val minClusterSize: Int = 2
) {
    init {
        require(minClusterSize >= 2) { "Minimum cluster size must be at least 2 (DBSCAN constraint)" }
    }
}

/**
 * Represents a date range for filtering stops.
 *
 * Boundaries are inclusive (startMillis <= stop.startTime <= endMillis).
 *
 * @property startMillis Start of range (epoch milliseconds, inclusive)
 * @property endMillis End of range (epoch milliseconds, inclusive)
 */
@Immutable
data class DateRange(
    val startMillis: Long,
    val endMillis: Long
) {
    init {
        require(startMillis <= endMillis) { "Start time must be before or equal to end time" }
    }
}

/**
 * Preset date ranges for common filtering scenarios.
 *
 * All ranges end at current time and calculate start time based on duration.
 */
object DateRangePresets {
    /**
     * Last 7 days (week filter).
     *
     * @param currentTimeMillis Current time (defaults to now, injectable for testing)
     * @return DateRange for last 7 days
     */
    fun last7Days(currentTimeMillis: Long = System.currentTimeMillis()): DateRange {
        val startMillis = currentTimeMillis - TimeUnit.DAYS.toMillis(7)
        return DateRange(startMillis, currentTimeMillis)
    }

    /**
     * Last 30 days (month filter).
     *
     * @param currentTimeMillis Current time (defaults to now, injectable for testing)
     * @return DateRange for last 30 days
     */
    fun last30Days(currentTimeMillis: Long = System.currentTimeMillis()): DateRange {
        val startMillis = currentTimeMillis - TimeUnit.DAYS.toMillis(30)
        return DateRange(startMillis, currentTimeMillis)
    }

    /**
     * Last 90 days (quarter filter).
     *
     * @param currentTimeMillis Current time (defaults to now, injectable for testing)
     * @return DateRange for last 90 days
     */
    fun last90Days(currentTimeMillis: Long = System.currentTimeMillis()): DateRange {
        val startMillis = currentTimeMillis - TimeUnit.DAYS.toMillis(90)
        return DateRange(startMillis, currentTimeMillis)
    }

    /**
     * Last 365 days (year filter).
     *
     * @param currentTimeMillis Current time (defaults to now, injectable for testing)
     * @return DateRange for last 365 days
     */
    fun lastYear(currentTimeMillis: Long = System.currentTimeMillis()): DateRange {
        val startMillis = currentTimeMillis - TimeUnit.DAYS.toMillis(365)
        return DateRange(startMillis, currentTimeMillis)
    }
}

/**
 * Preset minimum cluster sizes for filtering.
 *
 * Aligns with MarkerColor thresholds for UI consistency.
 */
object ClusterSizePresets {
    /** Small clusters only (2-5 stops, green markers) */
    const val SMALL = 2

    /** Medium+ clusters (6+ stops, yellow and red markers) */
    const val MEDIUM_PLUS = 6

    /** Large clusters only (11+ stops, red markers) */
    const val LARGE = 11

    /** Very large clusters (20+ stops) */
    const val VERY_LARGE = 20
}
