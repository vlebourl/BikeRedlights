package com.example.bikeredlights.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bikeredlights.domain.model.display.RideDetailData
import com.example.bikeredlights.ui.components.history.DetailStatCard
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import com.example.bikeredlights.ui.viewmodel.RideDetailUiState
import com.example.bikeredlights.ui.viewmodel.RideDetailViewModel

/**
 * Ride Detail screen displaying comprehensive ride statistics.
 *
 * **Features**:
 * - Full ride statistics in grid layout
 * - Responsive 2-column grid
 * - Loading state during data fetch
 * - Not found state for invalid ride IDs
 * - Error state with error message
 * - Back button navigation
 *
 * **Architecture**:
 * - Stateless screen composable
 * - ViewModel handles business logic
 * - Reactive UI updates via StateFlow
 *
 * **Performance**:
 * - LazyVerticalGrid for efficient rendering
 * - Minimal recompositions with stable data
 *
 * @param onNavigateBack Callback when back button is pressed
 * @param viewModel ViewModel managing ride detail state
 * @param modifier Optional modifier for layout customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is RideDetailUiState.Success -> Text(state.rideDetail.name)
                        else -> Text("Ride Details")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)  // Remove status bar padding
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is RideDetailUiState.Loading -> {
                LoadingView(modifier = Modifier.padding(paddingValues))
            }

            is RideDetailUiState.NotFound -> {
                NotFoundView(modifier = Modifier.padding(paddingValues))
            }

            is RideDetailUiState.Success -> {
                RideDetailContent(
                    rideDetail = state.rideDetail,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is RideDetailUiState.Error -> {
                ErrorView(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Loading indicator centered on screen.
 */
@Composable
private fun LoadingView(
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
 * Not found message for invalid ride IDs.
 */
@Composable
private fun NotFoundView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ride not found",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error message centered on screen.
 */
@Composable
private fun ErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Main content showing ride statistics in grid layout.
 *
 * **Layout**:
 * - Ride name header
 * - 2-column grid of stat cards
 * - Responsive to screen width
 *
 * @param rideDetail Display-ready ride detail data
 * @param modifier Optional modifier
 */
@Composable
private fun RideDetailContent(
    rideDetail: RideDetailData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Distance
            item {
                DetailStatCard(
                    label = "Distance",
                    value = rideDetail.distanceFormatted
                )
            }

            // Duration
            item {
                DetailStatCard(
                    label = "Duration",
                    value = rideDetail.durationFormatted
                )
            }

            // Average Speed
            item {
                DetailStatCard(
                    label = "Avg Speed",
                    value = rideDetail.avgSpeedFormatted
                )
            }

            // Max Speed
            item {
                DetailStatCard(
                    label = "Max Speed",
                    value = rideDetail.maxSpeedFormatted
                )
            }

            // Start Time
            item {
                DetailStatCard(
                    label = "Start Time",
                    value = rideDetail.startTimeFormatted
                )
            }

            // End Time
            item {
                DetailStatCard(
                    label = "End Time",
                    value = rideDetail.endTimeFormatted
                )
            }

            // Paused Time (only if ride has pauses)
            if (rideDetail.hasPauses) {
                item {
                    DetailStatCard(
                        label = "Paused Time",
                        value = rideDetail.pausedTimeFormatted
                    )
                }
            }
        }
    }
}

// ===== Previews =====

@Preview(name = "Success State", showBackground = true)
@Composable
private fun RideDetailScreenSuccessPreview() {
    BikeRedlightsTheme {
        RideDetailContent(
            rideDetail = RideDetailData(
                id = 1,
                name = "Morning Commute",
                startTimeFormatted = "Nov 6, 2025, 08:15 AM",
                endTimeFormatted = "Nov 6, 2025, 09:05 AM",
                durationFormatted = "00:42:15",
                distanceFormatted = "12.5 km",
                avgSpeedFormatted = "17.7 km/h",
                maxSpeedFormatted = "32.4 km/h",
                pausedTimeFormatted = "00:07:45",
                hasPauses = true
            )
        )
    }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun RideDetailScreenLoadingPreview() {
    BikeRedlightsTheme {
        LoadingView()
    }
}

@Preview(name = "Not Found State", showBackground = true)
@Composable
private fun RideDetailScreenNotFoundPreview() {
    BikeRedlightsTheme {
        NotFoundView()
    }
}

@Preview(name = "Error State", showBackground = true)
@Composable
private fun RideDetailScreenErrorPreview() {
    BikeRedlightsTheme {
        ErrorView(message = "Failed to load ride details")
    }
}
