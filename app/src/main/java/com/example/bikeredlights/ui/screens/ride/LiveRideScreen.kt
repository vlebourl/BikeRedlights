package com.example.bikeredlights.ui.screens.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bikeredlights.ui.components.ride.SaveRideDialog
import com.example.bikeredlights.ui.viewmodel.RideRecordingUiState
import com.example.bikeredlights.ui.viewmodel.RideRecordingViewModel

/**
 * Live Ride Screen for starting and stopping ride recording.
 *
 * **Features**:
 * - Start Ride button (when idle)
 * - Stop Ride button (when recording)
 * - Show elapsed time (when recording)
 * - Show distance traveled (when recording)
 * - Save/Discard dialog (when stopped)
 *
 * **UI States**:
 * - Idle: Show "Start Ride" button
 * - Recording: Show elapsed time, distance, "Stop Ride" button
 * - Paused: Show "Paused" indicator, "Resume" and "Stop" buttons
 * - ShowingSaveDialog: Show save/discard dialog
 *
 * @param viewModel RideRecordingViewModel injected by Hilt
 * @param modifier Modifier for this composable
 */
@Composable
fun LiveRideScreen(
    viewModel: RideRecordingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Show save dialog if needed
    if (uiState is RideRecordingUiState.ShowingSaveDialog) {
        val ride = (uiState as RideRecordingUiState.ShowingSaveDialog).ride
        SaveRideDialog(
            ride = ride,
            onSave = { viewModel.saveRide() },
            onDiscard = { viewModel.discardRide() }
        )
    }

    // Main content
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is RideRecordingUiState.Idle -> {
                IdleContent(
                    onStartRide = { viewModel.startRide() }
                )
            }
            is RideRecordingUiState.Recording -> {
                val ride = (uiState as RideRecordingUiState.Recording).ride
                RecordingContent(
                    ride = ride,
                    onStopRide = { viewModel.stopRide() }
                )
            }
            is RideRecordingUiState.Paused -> {
                val ride = (uiState as RideRecordingUiState.Paused).ride
                PausedContent(
                    ride = ride,
                    onStopRide = { viewModel.stopRide() }
                )
            }
            is RideRecordingUiState.AutoPaused -> {
                val ride = (uiState as RideRecordingUiState.AutoPaused).ride
                AutoPausedContent(
                    ride = ride,
                    onStopRide = { viewModel.stopRide() }
                )
            }
            is RideRecordingUiState.ShowingSaveDialog -> {
                // Dialog is shown above, show recording content underneath
                val ride = (uiState as RideRecordingUiState.ShowingSaveDialog).ride
                RecordingContent(
                    ride = ride,
                    onStopRide = { } // No action while dialog is shown
                )
            }
        }
    }
}

/**
 * Content shown when idle (no active ride).
 */
@Composable
private fun IdleContent(
    onStartRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ready to ride?",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(
            onClick = onStartRide,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Start Ride")
        }
    }
}

/**
 * Content shown when recording is active.
 */
@Composable
private fun RecordingContent(
    ride: com.example.bikeredlights.domain.model.Ride,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Recording",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Elapsed time
        val elapsedSeconds = (System.currentTimeMillis() - ride.startTime) / 1000
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Distance
        val distanceKm = ride.distanceMeters / 1000.0
        Text(
            text = String.format("%.2f km", distanceKm),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Stop button
        Button(
            onClick = onStopRide,
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Stop Ride")
        }
    }
}

/**
 * Content shown when manually paused.
 */
@Composable
private fun PausedContent(
    ride: com.example.bikeredlights.domain.model.Ride,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Paused",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Elapsed time (frozen)
        val elapsedSeconds = ride.elapsedDurationMillis / 1000
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Distance
        val distanceKm = ride.distanceMeters / 1000.0
        Text(
            text = String.format("%.2f km", distanceKm),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Stop button
        Button(
            onClick = onStopRide,
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Stop Ride")
        }
    }
}

/**
 * Content shown when auto-paused (low speed).
 */
@Composable
private fun AutoPausedContent(
    ride: com.example.bikeredlights.domain.model.Ride,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Same as paused for now
    PausedContent(ride, onStopRide, modifier)
}
