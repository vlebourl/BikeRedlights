package com.example.bikeredlights.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for generating default ride names.
 *
 * **Naming Format**: "Ride on MMM d, yyyy"
 *
 * **Examples**:
 * - "Ride on Jan 15, 2025"
 * - "Ride on Dec 3, 2024"
 * - "Ride on Jul 4, 2025"
 *
 * **Design Principles**:
 * - Thread-safe (uses function-scoped SimpleDateFormat)
 * - Locale-aware (uses Locale.getDefault())
 * - Immutable (pure function)
 *
 * **Usage**:
 * ```kotlin
 * val rideName = RideNameGenerator.generateDefaultName(System.currentTimeMillis())
 * // Returns: "Ride on Nov 4, 2025"
 * ```
 */
object RideNameGenerator {

    /**
     * Date format pattern for ride names.
     */
    private const val DATE_FORMAT_PATTERN = "MMM d, yyyy"

    /**
     * Generate a default ride name from a timestamp.
     *
     * @param timestamp Unix epoch milliseconds
     * @return Formatted ride name (e.g., "Ride on Jan 15, 2025")
     */
    fun generateDefaultName(timestamp: Long): String {
        val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))
        return "Ride on $formattedDate"
    }
}
