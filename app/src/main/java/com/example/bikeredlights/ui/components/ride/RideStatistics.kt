package com.example.bikeredlights.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.Ride

/**
 * Display ride statistics during active recording.
 *
 * **Features**:
 * - Duration in HH:MM:SS format
 * - Distance with one decimal place
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
 * @param ride Current ride with statistics
 * @param currentSpeed Current speed in m/s from latest GPS update
 * @param modifier Modifier for this composable
 */
@Composable
fun RideStatistics(
    ride: Ride,
    currentSpeed: Double = 0.0,
    modifier: Modifier = Modifier
) {
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
            // Duration (primary metric)
            val duration = System.currentTimeMillis() - ride.startTime
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Distance
            val distanceKm = ride.distanceMeters / 1000.0
            Text(
                text = String.format("%.1f km", distanceKm),
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
                    modifier = Modifier.weight(1f)
                )

                // Average speed
                SpeedMetric(
                    label = "Average",
                    speed = ride.avgSpeedMetersPerSec,
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
                    modifier = Modifier.weight(1f)
                )

                // Moving time
                val movingTimeSeconds = ride.movingDurationMillis / 1000
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Moving",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(ride.movingDurationMillis),
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
 */
@Composable
private fun SpeedMetric(
    label: String,
    speed: Double,
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

        // Convert m/s to km/h (multiply by 3.6)
        val speedKmh = speed * 3.6
        Text(
            text = String.format("%.1f", speedKmh),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "km/h",
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
