package com.example.bikeredlights.ui.screens.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.ui.components.ride.RideStatistics
import com.example.bikeredlights.ui.components.ride.formatDuration
import com.example.bikeredlights.ui.viewmodel.RideReviewUiState
import com.example.bikeredlights.ui.viewmodel.RideReviewViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Ride Review Screen for displaying completed ride statistics.
 *
 * **Features**:
 * - Display ride name and date
 * - Show comprehensive statistics (duration, distance, speeds)
 * - Map placeholder message (for v0.4.0)
 * - Back navigation to Live tab
 *
 * **UI Layout**:
 * - Top app bar with back button and ride name
 * - Map placeholder section
 * - Statistics card (reuses RideStatistics composable)
 * - Bottom padding for safe area
 *
 * **State Management**:
 * - ViewModel fetches ride from database using rideId
 * - Loading state while fetching
 * - Error state if ride not found
 *
 * @param rideId ID of the ride to display (passed as navigation argument)
 * @param onNavigateBack Callback when back button clicked
 * @param viewModel RideReviewViewModel injected by Hilt
 * @param modifier Modifier for this composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideReviewScreen(
    rideId: Long,
    onNavigateBack: () -> Unit,
    viewModel: RideReviewViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Fetch ride when screen appears
    LaunchedEffect(rideId) {
        viewModel.loadRide(rideId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val state = uiState) {
                            is RideReviewUiState.Success -> state.ride.name
                            else -> "Ride Review"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is RideReviewUiState.Loading -> {
                    LoadingContent()
                }
                is RideReviewUiState.Success -> {
                    RideReviewContent(
                        ride = state.ride,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is RideReviewUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }
}

/**
 * Loading indicator while fetching ride.
 */
@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error message when ride not found.
 */
@Composable
private fun ErrorContent(
    message: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = onNavigateBack) {
            Text("Back to Live")
        }
    }
}

/**
 * Main content displaying ride statistics.
 */
@Composable
private fun RideReviewContent(
    ride: Ride,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Ride date
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val rideDate = dateFormat.format(Date(ride.startTime))
        Text(
            text = rideDate,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Map placeholder
        MapPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        // Ride statistics (reuse component from live screen)
        RideStatistics(
            ride = ride,
            currentSpeed = 0.0,  // Not applicable for completed ride
            modifier = Modifier.fillMaxWidth()
        )

        // Summary section
        SummarySection(ride)
    }
}

/**
 * Map placeholder message.
 * Map visualization will be added in v0.4.0.
 */
@Composable
private fun MapPlaceholder(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Map visualization\ncoming in v0.4.0",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Summary section with additional ride details.
 */
@Composable
private fun SummarySection(
    ride: Ride,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // Ride duration
            val totalDuration = ride.endTime?.let { it - ride.startTime } ?: 0L
            SummaryRow(
                label = "Total Duration",
                value = formatDuration(totalDuration)
            )

            // Moving duration
            SummaryRow(
                label = "Moving Time",
                value = formatDuration(ride.movingDurationMillis)
            )

            // Distance
            val distanceKm = ride.distanceMeters / 1000.0
            SummaryRow(
                label = "Distance",
                value = String.format("%.2f km", distanceKm)
            )

            // Average speed
            val avgSpeedKmh = ride.avgSpeedMetersPerSec * 3.6
            SummaryRow(
                label = "Average Speed",
                value = String.format("%.1f km/h", avgSpeedKmh)
            )

            // Max speed
            val maxSpeedKmh = ride.maxSpeedMetersPerSec * 3.6
            SummaryRow(
                label = "Max Speed",
                value = String.format("%.1f km/h", maxSpeedKmh)
            )
        }
    }
}

/**
 * Single row in summary section.
 */
@Composable
private fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
