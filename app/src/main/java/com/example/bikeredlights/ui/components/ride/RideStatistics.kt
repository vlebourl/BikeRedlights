package com.example.bikeredlights.ui.components.ride

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.ui.viewmodel.RideRecordingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Display ride statistics during active recording.
 *
 * **Features**:
 * - Duration in HH:MM:SS format
 * - Distance with one decimal place (km or miles based on settings)
 * - Current speed (from latest GPS reading)
 * - Average speed (distance / moving time)
 * - Max speed (peak value)
 *
 * **Layout**:
 * - Material 3 Card with elevation
 * - Grid layout: 2 columns x 3 rows
 * - Large primary metric (duration)
 * - Secondary metrics in grid
 *
 * **Units Support**:
 * - Metric: km, km/h
 * - Imperial: miles, mph
 *
 * @param ride Current ride with statistics
 * @param currentSpeed Current speed in m/s from latest GPS update
 * @param unitsSystem Units system for display (Metric or Imperial)
 * @param modifier Modifier for this composable
 */
@Composable
fun RideStatistics(
    ride: Ride,
    currentSpeed: Double = 0.0,
    unitsSystem: UnitsSystem = UnitsSystem.METRIC,
    modifier: Modifier = Modifier
) {
    // SIMPLE TIMER: Independent of GPS, uses SystemClock for accurate timing
    // Starts at 0, counts up based on actual elapsed time (prevents drift)
    // Resets per ride using ride.id as key
    var elapsedMillis by remember(ride.id) { mutableLongStateOf(0L) }
    val isTimerRunning = ride.endTime == null && ride.startTime != 0L

    LaunchedEffect(ride.id, isTimerRunning) {
        if (isTimerRunning) {
            // Timer is running: calculate elapsed time from SystemClock
            val timerStartTime = SystemClock.uptimeMillis()
            while (isActive) {
                // Calculate actual elapsed time (prevents drift from delay inaccuracies)
                elapsedMillis = SystemClock.uptimeMillis() - timerStartTime
                delay(100L)  // Update every 100ms for smooth display
            }
        } else if (ride.endTime != null) {
            // Finished ride: show stored duration from database
            elapsedMillis = ride.movingDurationMillis
        }
        // If waiting for GPS (startTime==0): elapsedMillis stays at 0
    }

    val displayDuration = elapsedMillis

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duration (primary metric) - simple timer counting seconds
            Text(
                text = formatDuration(displayDuration),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Distance (converted based on units system)
            val distance = RideRecordingViewModel.convertDistance(ride.distanceMeters, unitsSystem)
            val distanceUnit = RideRecordingViewModel.getDistanceUnit(unitsSystem)
            Text(
                text = String.format("%.1f %s", distance, distanceUnit),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Speed metrics grid (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Current speed
                SpeedMetric(
                    label = "Current",
                    speed = currentSpeed,
                    unitsSystem = unitsSystem,
                    modifier = Modifier.weight(1f)
                )

                // Average speed
                SpeedMetric(
                    label = "Average",
                    speed = ride.avgSpeedMetersPerSec,
                    unitsSystem = unitsSystem,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Max speed
                SpeedMetric(
                    label = "Max",
                    speed = ride.maxSpeedMetersPerSec,
                    unitsSystem = unitsSystem,
                    modifier = Modifier.weight(1f)
                )

                // Current time of day
                val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Display a single speed metric.
 *
 * @param label Label for the metric (e.g., "Current", "Average", "Max")
 * @param speed Speed in meters per second
 * @param unitsSystem Units system for conversion (Metric or Imperial)
 * @param modifier Modifier for this composable
 */
@Composable
private fun SpeedMetric(
    label: String,
    speed: Double,
    unitsSystem: UnitsSystem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Convert m/s to km/h or mph based on units system
        val convertedSpeed = RideRecordingViewModel.convertSpeed(speed, unitsSystem)
        val speedUnit = RideRecordingViewModel.getSpeedUnit(unitsSystem)

        Text(
            text = String.format("%.1f", convertedSpeed),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = speedUnit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Format duration in milliseconds to HH:MM:SS.
 *
 * **Examples**:
 * - 0 ms → "00:00:00"
 * - 65000 ms → "00:01:05"
 * - 3661000 ms → "01:01:01"
 * - 86400000 ms → "24:00:00"
 *
 * @param durationMillis Duration in milliseconds
 * @return Formatted string in HH:MM:SS format
 */
fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
