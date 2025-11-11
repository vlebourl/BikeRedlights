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
 * - Paused time (manual + auto-pause combined) with real-time counter (Feature 007 - v0.6.1)
 *
 * **Layout** (Safety-Focused):
 * - Material 3 Card with elevation
 * - Row 1: Current speed (displayLarge) - HERO METRIC
 * - Row 2: Duration + Distance (headlineMedium) - SECONDARY
 * - Row 3: Average + Max speed (titleLarge) - SUPPORTING
 * - Row 4: Paused + Immobile time (titleLarge) - INFORMATIONAL
 *
 * **Design Rationale**:
 * - Current speed is most prominent (safety-critical for red light warnings)
 * - Duration shows active riding time (excludes pauses)
 * - Paused time shows total pause duration (useful for understanding ride patterns)
 * - Real-time pause counter updates every second when actively paused (v0.6.1)
 * - Aligns UI priority with app safety mission
 *
 * **Units Support**:
 * - Metric: km, km/h
 * - Imperial: miles, mph
 *
 * @param ride Current ride with statistics
 * @param currentSpeed Current speed in m/s from latest GPS update
 * @param pausedDuration Real-time pause duration (ZERO when not paused) (Feature 007 - v0.6.1)
 * @param unitsSystem Units system for display (Metric or Imperial)
 * @param modifier Modifier for this composable
 */
@Composable
fun RideStatistics(
    ride: Ride,
    currentSpeed: Double = 0.0,
    pausedDuration: java.time.Duration = java.time.Duration.ZERO,
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
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Current speed (hero metric - safety critical)
            val convertedSpeed = RideRecordingViewModel.convertSpeed(currentSpeed, unitsSystem)
            val speedUnit = RideRecordingViewModel.getSpeedUnit(unitsSystem)

            Text(
                text = String.format("%.1f %s", convertedSpeed, speedUnit),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

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

            // Paused and Immobile time (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Paused time (manual + auto-pause combined) with real-time counter (Feature 007 - v0.6.1)
                // Logic:
                // - If pausedDuration > ZERO (actively paused): show real-time counter
                // - Otherwise (not paused): show accumulated value from database
                val displayedPausedDuration = if (pausedDuration > java.time.Duration.ZERO) {
                    // Real-time counter: show current pause session + previous accumulated pauses
                    val previousPauses = ride.manualPausedDurationMillis + ride.autoPausedDurationMillis
                    previousPauses + pausedDuration.toMillis()
                } else {
                    // Not paused: show accumulated value from database
                    ride.manualPausedDurationMillis + ride.autoPausedDurationMillis
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(displayedPausedDuration),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Immobile time (placeholder - not yet tracked)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Immobile",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "00:00:00", // TODO: Track immobile time (stopped at lights while recording)
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) // De-emphasized
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

        // Convert m/s to km/h or mph based on units system
        val convertedSpeed = RideRecordingViewModel.convertSpeed(speed, unitsSystem)
        val speedUnit = RideRecordingViewModel.getSpeedUnit(unitsSystem)

        Text(
            text = String.format("%.1f %s", convertedSpeed, speedUnit),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
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
