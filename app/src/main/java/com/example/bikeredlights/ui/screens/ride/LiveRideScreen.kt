package com.example.bikeredlights.ui.screens.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bikeredlights.ui.components.ride.RideControls
import com.example.bikeredlights.ui.components.ride.RideStatistics
import com.example.bikeredlights.ui.components.ride.SaveRideDialog
import com.example.bikeredlights.ui.viewmodel.NavigationEvent
import com.example.bikeredlights.ui.viewmodel.RideRecordingUiState
import com.example.bikeredlights.ui.viewmodel.RideRecordingViewModel
import kotlinx.coroutines.flow.collectLatest

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
    onNavigateToReview: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle navigation events (one-time events via Channel)
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToReview -> {
                    onNavigateToReview(event.rideId)
                }
            }
        }
    }

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
                    onPauseRide = { viewModel.pauseRide() },
                    onStopRide = { viewModel.stopRide() }
                )
            }
            is RideRecordingUiState.Paused -> {
                val ride = (uiState as RideRecordingUiState.Paused).ride
                PausedContent(
                    ride = ride,
                    onResumeRide = { viewModel.resumeRide() },
                    onStopRide = { viewModel.stopRide() }
                )
            }
            is RideRecordingUiState.AutoPaused -> {
                val ride = (uiState as RideRecordingUiState.AutoPaused).ride
                AutoPausedContent(
                    ride = ride,
                    onResumeRide = { viewModel.resumeRide() },
                    onStopRide = { viewModel.stopRide() }
                )
            }
            is RideRecordingUiState.ShowingSaveDialog -> {
                // Dialog is shown above, show recording content underneath
                val ride = (uiState as RideRecordingUiState.ShowingSaveDialog).ride
                RecordingContent(
                    ride = ride,
                    onPauseRide = { }, // No action while dialog is shown
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
    onPauseRide: () -> Unit,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Status indicator
        Text(
            text = "Recording",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // Ride statistics (duration, distance, speeds)
        RideStatistics(
            ride = ride,
            currentSpeed = 0.0, // TODO: Expose current speed from service
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Control buttons (pause, stop)
        RideControls(
            isPaused = false,
            onPauseClick = onPauseRide,
            onResumeClick = { }, // Not used when not paused
            onStopClick = onStopRide,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Content shown when manually paused.
 */
@Composable
private fun PausedContent(
    ride: com.example.bikeredlights.domain.model.Ride,
    onResumeRide: () -> Unit,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Status indicator
        Text(
            text = "Paused",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // Ride statistics (frozen at pause time)
        RideStatistics(
            ride = ride,
            currentSpeed = 0.0, // Zero when paused
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Control buttons (resume, stop)
        RideControls(
            isPaused = true,
            onPauseClick = { }, // Not used when paused
            onResumeClick = onResumeRide,
            onStopClick = onStopRide,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Content shown when auto-paused (low speed).
 */
@Composable
private fun AutoPausedContent(
    ride: com.example.bikeredlights.domain.model.Ride,
    onResumeRide: () -> Unit,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Same as paused, but with "Auto-paused" label
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Status indicator
        Text(
            text = "Auto-paused",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // Ride statistics (frozen at auto-pause)
        RideStatistics(
            ride = ride,
            currentSpeed = 0.0, // Zero when auto-paused
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Control buttons (resume, stop)
        RideControls(
            isPaused = true,
            onPauseClick = { }, // Not used when auto-paused
            onResumeClick = onResumeRide,
            onStopClick = onStopRide,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
