package com.example.bikeredlights.domain.model.display

import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.UnitsSystem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Display model for a ride item in the history list.
 *
 * Contains pre-formatted strings optimized for list rendering performance.
 * All values respect user's preferred units (Metric or Imperial).
 *
 * @property id Unique ride identifier for navigation
 * @property name User-assigned ride name (e.g., "Morning Commute")
 * @property dateFormatted Human-readable date (e.g., "Nov 4, 2025")
 * @property durationFormatted Active riding time in HH:MM:SS format
 * @property distanceFormatted Total distance with unit (e.g., "12.5 km" or "7.8 mi")
 * @property avgSpeedFormatted Average speed with unit (e.g., "18.2 km/h" or "11.3 mph")
 * @property startTimeMillis Original timestamp for sorting operations
 */
data class RideListItem(
    val id: Long,
    val name: String,
    val dateFormatted: String,
    val durationFormatted: String,
    val distanceFormatted: String,
    val avgSpeedFormatted: String,
    val startTimeMillis: Long
)

/**
 * Converts a domain Ride model to a list display model.
 *
 * Performs all formatting calculations once at conversion time to avoid
 * repeated calculations during list rendering/recomposition.
 *
 * @param unitPreference User's unit system preference (Metric or Imperial)
 * @return Formatted display model ready for UI rendering
 */
fun Ride.toListItem(unitPreference: UnitsSystem): RideListItem {
    val dateFormatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

    return RideListItem(
        id = id,
        name = name,
        dateFormatted = Instant.ofEpochMilli(startTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter),
        durationFormatted = formatDuration(movingDurationMillis / 1000), // Convert to seconds
        distanceFormatted = when (unitPreference) {
            UnitsSystem.METRIC -> "%.1f km".format(distanceMeters / 1000.0)
            UnitsSystem.IMPERIAL -> "%.1f mi".format(distanceMeters / 1609.34)
        },
        avgSpeedFormatted = when (unitPreference) {
            UnitsSystem.METRIC -> "%.1f km/h".format(avgSpeedMetersPerSec * 3.6)
            UnitsSystem.IMPERIAL -> "%.1f mph".format(avgSpeedMetersPerSec * 2.23694)
        },
        startTimeMillis = startTime
    )
}

/**
 * Formats duration in seconds to HH:MM:SS string.
 *
 * Examples:
 * - 65 seconds → "00:01:05"
 * - 3661 seconds → "01:01:01"
 * - 0 seconds → "00:00:00"
 *
 * @param durationSeconds Total duration in seconds
 * @return Formatted time string
 */
private fun formatDuration(durationSeconds: Long): String {
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
