package com.example.bikeredlights.ui.components.ride

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
 * - Current speed (hero metric - safety critical)
 * - Duration in HH:MM:SS format (moving time)
 * - Distance with one decimal place (km or miles based on settings)
 * - Average speed (distance / moving time)
 * - Max speed (peak value)
 * - Paused time (manual + auto-pause combined)
 *
 * **Layout** (Safety-Focused):
 * - Material 3 Card with elevation
 * - Row 1: Current speed (displayLarge) - HERO METRIC
 * - Row 2: Duration + Distance (headlineMedium) - SECONDARY
 * - Row 3: Average + Max speed (titleLarge) - SUPPORTING
 * - Row 4: Paused time (titleLarge) - INFORMATIONAL
 *
 * **Design Rationale**:
 * - Current speed is most prominent (safety-critical for red light warnings)
 * - Duration shows active riding time (excludes pauses)
 * - Paused time shows total pause duration (useful for understanding ride patterns)
 * - Aligns UI priority with app safety mission
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
    // Timer display: Use movingDurationMillis from database
    // The service updates this field every second, accounting for pauses in real-time
    // - Active ride: Shows moving time (excludes pauses)
    // - Finished ride: Shows final moving duration
    // - Waiting for GPS: Shows 0
    val currentDuration = ride.movingDurationMillis

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
            // Current speed (hero metric - safety critical)
            val convertedSpeed = RideRecordingViewModel.convertSpeed(currentSpeed, unitsSystem)
            val speedUnit = RideRecordingViewModel.getSpeedUnit(unitsSystem)

            Text(
                text = String.format("%.1f", convertedSpeed),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = speedUnit,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Duration and Distance (secondary metrics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Duration
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(currentDuration),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                // Distance
                val distance = RideRecordingViewModel.convertDistance(ride.distanceMeters, unitsSystem)
                val distanceUnit = RideRecordingViewModel.getDistanceUnit(unitsSystem)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.1f %s", distance, distanceUnit),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Speed metrics grid (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Average speed
                SpeedMetric(
                    label = "Average",
                    speed = ride.avgSpeedMetersPerSec,
                    unitsSystem = unitsSystem,
                    modifier = Modifier.weight(1f)
                )

                // Max speed
                SpeedMetric(
                    label = "Max",
                    speed = ride.maxSpeedMetersPerSec,
                    unitsSystem = unitsSystem,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Paused time (manual + auto-pause combined)
            val totalPausedDuration = ride.manualPausedDurationMillis + ride.autoPausedDurationMillis
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(totalPausedDuration),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
