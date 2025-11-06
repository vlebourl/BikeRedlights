package com.example.bikeredlights.domain.model.display

import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Display model for detailed ride information screen.
 *
 * Contains all ride statistics formatted for display, including expanded
 * information not shown in the list view (start/end times, max speed, pauses).
 *
 * @property id Unique ride identifier
 * @property name User-assigned ride name
 * @property startTimeFormatted Start date/time (e.g., "Nov 4, 2025, 9:30 AM")
 * @property endTimeFormatted End date/time or "Incomplete" if ride ongoing
 * @property durationFormatted Active riding time in HH:MM:SS format
 * @property distanceFormatted Total distance with unit
 * @property avgSpeedFormatted Average speed with unit
 * @property maxSpeedFormatted Maximum speed reached with unit
 * @property pausedTimeFormatted Total paused time (manual + auto) in HH:MM:SS format
 * @property hasPauses True if ride has any pause time (shows/hides pause section)
 */
data class RideDetailData(
    val id: Long,
    val name: String,
    val startTimeFormatted: String,
    val endTimeFormatted: String,
    val durationFormatted: String,
    val distanceFormatted: String,
    val avgSpeedFormatted: String,
    val maxSpeedFormatted: String,
    val pausedTimeFormatted: String,
    val hasPauses: Boolean
)

/**
 * Converts a domain Ride model to a detail display model.
 *
 * Calculates total paused time from manual and auto pause durations.
 * Formats all timestamps and measurements according to user's preferences.
 *
 * @param unitPreference User's unit system preference (Metric or Imperial)
 * @return Formatted display model ready for detail screen
 */
fun Ride.toDetailData(unitPreference: UnitsSystem): RideDetailData {
    val dateTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())

    val totalPausedMillis = manualPausedDurationMillis + autoPausedDurationMillis

    return RideDetailData(
        id = id,
        name = name,
        startTimeFormatted = Instant.ofEpochMilli(startTime)
            .atZone(ZoneId.systemDefault())
            .format(dateTimeFormatter),
        endTimeFormatted = endTime?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .format(dateTimeFormatter)
        } ?: "Incomplete",
        durationFormatted = formatDuration(movingDurationMillis / 1000),
        distanceFormatted = formatDistance(distanceMeters, unitPreference),
        avgSpeedFormatted = formatSpeed(avgSpeedMetersPerSec, unitPreference),
        maxSpeedFormatted = formatSpeed(maxSpeedMetersPerSec, unitPreference),
        pausedTimeFormatted = formatDuration(totalPausedMillis / 1000),
        hasPauses = totalPausedMillis > 0
    )
}

/**
 * Formats duration in seconds to HH:MM:SS string.
 *
 * @param durationSeconds Total duration in seconds
 * @return Formatted time string (e.g., "01:23:45")
 */
private fun formatDuration(durationSeconds: Long): String {
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

/**
 * Formats distance in meters according to unit preference.
 *
 * @param distanceMeters Distance in meters
 * @param unitPreference User's unit system preference
 * @return Formatted distance string (e.g., "12.5 km" or "7.8 mi")
 */
private fun formatDistance(distanceMeters: Double, unitPreference: UnitsSystem): String {
    return when (unitPreference) {
        UnitsSystem.METRIC -> "%.1f km".format(distanceMeters / 1000.0)
        UnitsSystem.IMPERIAL -> "%.1f mi".format(distanceMeters / 1609.34)
    }
}

/**
 * Formats speed in meters per second according to unit preference.
 *
 * @param speedMetersPerSec Speed in meters per second
 * @param unitPreference User's unit system preference
 * @return Formatted speed string (e.g., "18.2 km/h" or "11.3 mph")
 */
private fun formatSpeed(speedMetersPerSec: Double, unitPreference: UnitsSystem): String {
    return when (unitPreference) {
        UnitsSystem.METRIC -> "%.1f km/h".format(speedMetersPerSec * 3.6)
        UnitsSystem.IMPERIAL -> "%.1f mph".format(speedMetersPerSec * 2.23694)
    }
}
