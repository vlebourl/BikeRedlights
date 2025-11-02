package com.example.bikeredlights.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import com.example.bikeredlights.ui.components.PermissionRequiredContent
import com.example.bikeredlights.ui.components.SpeedDisplay
import com.example.bikeredlights.ui.permissions.LocationPermissionHandler
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModel

/**
 * Main screen for real-time speed tracking.
 *
 * This screen implements User Story 1 (View Current Speed While Riding) by displaying
 * the current cycling speed in km/h. It handles permission requests via
 * LocationPermissionHandler and conditionally renders either the speed display
 * or permission required content based on permission state.
 *
 * Architecture:
 * - ViewModel provides UI state via StateFlow
 * - collectAsStateWithLifecycle stops location tracking when app backgrounds
 *   (minActiveState = STARTED means collection pauses at ON_STOP)
 * - LocationPermissionHandler wraps screen to manage permission lifecycle
 *
 * Lifecycle behavior:
 * - ON_START: Location tracking resumes (if permission granted)
 * - ON_STOP: Location tracking pauses (battery optimization)
 * - Configuration changes: ViewModel survives, state preserved
 *
 * Usage:
 * ```kotlin
 * val viewModel = viewModel<SpeedTrackingViewModel>(
 *     factory = SpeedTrackingViewModelFactory(trackLocationUseCase)
 * )
 * SpeedTrackingScreen(viewModel = viewModel)
 * ```
 *
 * @param viewModel ViewModel managing speed tracking state
 * @param modifier Optional modifier for layout customization
 */
@Composable
fun SpeedTrackingScreen(
    viewModel: SpeedTrackingViewModel,
    modifier: Modifier = Modifier
) {
    // Collect UI state with lifecycle awareness (stops collection at ON_STOP)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(
        minActiveState = Lifecycle.State.STARTED
    )

    // Wrap screen with permission handler for location permission management
    LocationPermissionHandler(
        onPermissionGranted = { viewModel.onPermissionGranted() },
        onPermissionDenied = { viewModel.onPermissionDenied() }
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.hasLocationPermission) {
                    // Permission granted: show speed display
                    SpeedTrackingContent(
                        speedMeasurement = uiState.speedMeasurement
                    )
                } else {
                    // Permission not granted: show permission required content
                    PermissionRequiredContent()
                }
            }
        }
    }
}

/**
 * Content composable for speed tracking screen when permission is granted.
 *
 * Displays the current speed in a centered layout. This composable is separated
 * to allow for future expansion with additional UI elements (GPS status, location
 * coordinates) in subsequent user stories.
 *
 * @param speedMeasurement Current speed measurement, null before first GPS fix
 */
@Composable
private fun SpeedTrackingContent(
    speedMeasurement: com.example.bikeredlights.domain.model.SpeedMeasurement?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SpeedDisplay(
            speedMeasurement = speedMeasurement
        )
    }
}
