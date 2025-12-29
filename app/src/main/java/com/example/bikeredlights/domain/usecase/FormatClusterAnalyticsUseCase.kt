package com.example.bikeredlights.domain.usecase

import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Formats cluster frequency analytics for UI display.
 *
 * Created by Feature 011: Stop Cluster Visualization (FR-015).
 * Generates human-readable text like "You stopped here 15 times this month".
 *
 * Business Rules (per FR-015):
 * - "X times this week" if all stops are within last 7 days
 * - "X times this month" if all stops are within last 30 days
 * - "X total stops" otherwise
 * - Uses singular "time" for count == 1
 *
 * Algorithm:
 * 1. Calculate time boundaries (7 days ago, 30 days ago)
 * 2. Count stops in each time window
 * 3. Determine appropriate text based on coverage
 * 4. Handle singular/plural forms
 *
 * @param stopCount Total number of stops in cluster
 * @param stopTimestamps List of stop start times (epoch milliseconds)
 * @param currentTimeMillis Current time for relative calculation (injectable for testing)
 * @return Formatted frequency text (e.g., "You stopped here 15 times this month")
 */
class FormatClusterAnalyticsUseCase @Inject constructor() {
    operator fun invoke(
        stopCount: Int,
        stopTimestamps: List<Long>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): String {
        if (stopCount == 0 || stopTimestamps.isEmpty()) {
            return "0 total stops"
        }

        val sevenDaysAgo = currentTimeMillis - TimeUnit.DAYS.toMillis(7)
        val thirtyDaysAgo = currentTimeMillis - TimeUnit.DAYS.toMillis(30)

        val stopsInLastWeek = stopTimestamps.count { it >= sevenDaysAgo }
        val stopsInLastMonth = stopTimestamps.count { it >= thirtyDaysAgo }

        return when {
            // All stops within last 7 days
            stopsInLastWeek == stopCount -> {
                "You stopped here ${formatCount(stopCount)} this week"
            }
            // All stops within last 30 days (but not all in last 7)
            stopsInLastMonth == stopCount -> {
                "You stopped here ${formatCount(stopCount)} this month"
            }
            // Stops span > 30 days
            else -> {
                "${formatCount(stopCount, includePrefix = false)}"
            }
        }
    }

    /**
     * Format count with correct singular/plural form.
     *
     * @param count Number of stops
     * @param includePrefix If true, format as "X time(s)", else just count
     * @return Formatted string (e.g., "1 time", "15 times", "12 total stops")
     */
    private fun formatCount(count: Int, includePrefix: Boolean = true): String {
        return if (includePrefix) {
            if (count == 1) "1 time" else "$count times"
        } else {
            if (count == 1) "1 total stop" else "$count total stops"
        }
    }
}
