package com.example.bikeredlights.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bikeredlights.domain.model.display.RideListItem
import com.example.bikeredlights.ui.components.history.EmptyStateView
import com.example.bikeredlights.ui.components.history.RideListItemCard
import com.example.bikeredlights.ui.components.history.SortDialog
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import com.example.bikeredlights.ui.viewmodel.RideHistoryUiState
import com.example.bikeredlights.ui.viewmodel.RideHistoryViewModel

/**
 * Ride History screen displaying list of all saved rides.
 *
 * **Features**:
 * - Reactive list of rides from database
 * - Sort rides with dialog (User Story 3)
 * - Empty state when no rides exist
 * - Loading state during data fetch
 * - Error state with snackbar message
 * - Smooth scrolling with LazyColumn
 * - Click to navigate to ride details
 *
 * **Architecture**:
 * - Stateless screen composable
 * - ViewModel handles business logic
 * - Reactive UI updates via StateFlow
 *
 * **Performance**:
 * - LazyColumn provides automatic item windowing
 * - Only visible items are composed
 * - Smooth 60fps scrolling for 100+ rides
 *
 * @param onRideClick Callback when ride item is tapped (navigate to detail)
 * @param viewModel ViewModel managing ride list state
 * @param modifier Optional modifier for layout customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryScreen(
    onRideClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSort by viewModel.currentSort.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Rides") },
                actions = {
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort rides"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            is RideHistoryUiState.Loading -> {
                LoadingView(modifier = Modifier.padding(paddingValues))
            }

            is RideHistoryUiState.Empty -> {
                EmptyStateView(modifier = Modifier.padding(paddingValues))
            }

            is RideHistoryUiState.Success -> {
                RideListContent(
                    rides = state.rides,
                    onRideClick = onRideClick,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is RideHistoryUiState.Error -> {
                ErrorView(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Sort dialog
    if (showSortDialog) {
        SortDialog(
            currentSort = currentSort,
            onSortSelected = { sortPreference ->
                viewModel.updateSortPreference(sortPreference)
            },
            onDismiss = { showSortDialog = false }
        )
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
 * Scrollable list of ride items using LazyColumn.
 *
 * **Performance**:
 * - LazyColumn only composes visible items
 * - Automatic item windowing for large lists
 * - Efficient item reuse with key = ride.id
 *
 * @param rides List of rides to display
 * @param onRideClick Callback when ride is tapped
 * @param modifier Optional modifier
 */
@Composable
private fun RideListContent(
    rides: List<RideListItem>,
    onRideClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = rides,
            key = { ride -> ride.id }
        ) { ride ->
            RideListItemCard(
                ride = ride,
                onClick = { onRideClick(ride.id) }
            )
        }
    }
}

/**
 * Error message centered on screen.
 *
 * @param message Error message to display
 * @param modifier Optional modifier
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
        Text(text = "Error: $message")
    }
}

// ===== Previews =====

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun RideHistoryScreenLoadingPreview() {
    BikeRedlightsTheme {
        LoadingView()
    }
}

@Preview(name = "Empty State", showBackground = true)
@Composable
private fun RideHistoryScreenEmptyPreview() {
    BikeRedlightsTheme {
        EmptyStateView()
    }
}

@Preview(name = "Success State", showBackground = true)
@Composable
private fun RideHistoryScreenSuccessPreview() {
    BikeRedlightsTheme {
        RideListContent(
            rides = listOf(
                RideListItem(
                    id = 1,
                    name = "Morning Commute",
                    dateFormatted = "Nov 6, 2025",
                    durationFormatted = "00:42:15",
                    distanceFormatted = "12.5 km",
                    avgSpeedFormatted = "17.7 km/h",
                    startTimeMillis = System.currentTimeMillis()
                ),
                RideListItem(
                    id = 2,
                    name = "Evening Ride",
                    dateFormatted = "Nov 5, 2025",
                    durationFormatted = "01:15:30",
                    distanceFormatted = "25.8 km",
                    avgSpeedFormatted = "20.5 km/h",
                    startTimeMillis = System.currentTimeMillis() - 86400000
                ),
                RideListItem(
                    id = 3,
                    name = "Weekend Adventure",
                    dateFormatted = "Nov 4, 2025",
                    durationFormatted = "03:22:45",
                    distanceFormatted = "68.2 km",
                    avgSpeedFormatted = "19.8 km/h",
                    startTimeMillis = System.currentTimeMillis() - 172800000
                )
            ),
            onRideClick = {}
        )
    }
}

@Preview(name = "Error State", showBackground = true)
@Composable
private fun RideHistoryScreenErrorPreview() {
    BikeRedlightsTheme {
        ErrorView(message = "Failed to load rides")
    }
}
