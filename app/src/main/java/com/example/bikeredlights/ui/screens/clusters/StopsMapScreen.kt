package com.example.bikeredlights.ui.screens.clusters

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bikeredlights.domain.model.ClusterMarkerData
import com.example.bikeredlights.domain.model.MarkerColor
import com.example.bikeredlights.ui.components.clusters.ClusterMarker
import com.example.bikeredlights.ui.components.map.BikeMap
import com.example.bikeredlights.ui.viewmodel.ClusterMapUiState
import com.example.bikeredlights.ui.viewmodel.ClusterMapViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Main screen for viewing stop clusters on Google Maps (Feature 011 - User Story 1).
 *
 * Displays all clustered stops as color-coded markers on an interactive map.
 * Users can pan, zoom, and tap markers to view cluster details.
 *
 * Visual Structure:
 * ```
 * ┌─────────────────────────┐
 * │                         │
 * │   Google Maps           │
 * │   with cluster markers  │ ← Color-coded (green/yellow/red)
 * │   (pan, zoom enabled)   │
 * │                         │
 * └─────────────────────────┘
 * ```
 *
 * States:
 * - Loading: Shows centered loading spinner overlay
 * - Loaded: Shows map with cluster markers
 * - Empty: Shows "No clusters found" message with centered text
 * - Error: Shows error message with retry button
 *
 * Accessibility:
 * - Map controls have 48dp minimum touch targets (Google Maps default)
 * - Cluster markers have content descriptions
 * - Loading state announces "Loading clusters"
 * - Error state is clearly communicated
 *
 * Performance:
 * - Map renders up to 100 cluster markers efficiently
 * - Recomposition only when uiState.clusters changes
 * - Camera position preserved across recompositions
 *
 * @param viewModel ClusterMapViewModel for state management (injected via Hilt)
 * @param modifier Modifier for screen layout
 *
 * Example navigation:
 * ```
 * // In AppNavigation.kt
 * composable("stops_temp") {
 *     StopsMapScreen()
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopsMapScreen(
    viewModel: ClusterMapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    StopsMapContent(
        uiState = uiState,
        onClusterClick = viewModel::selectCluster,
        onRetry = viewModel::retry,
        modifier = modifier
    )

    // Show cluster detail bottom sheet when a cluster is selected (User Story 2)
    if (uiState.selectedCluster != null) {
        ClusterDetailBottomSheet(
            cluster = uiState.selectedCluster!!,
            onDismiss = viewModel::deselectCluster,
            sheetState = sheetState
        )
    }
}

/**
 * Content composable for Stops Map Screen (stateless).
 *
 * Separates state management from UI composition for easier testing.
 *
 * @param uiState Current UI state from ViewModel
 * @param onClusterClick Callback when cluster marker is tapped (receives cluster ID)
 * @param onRetry Callback when retry button is tapped (error state)
 * @param modifier Modifier for layout
 */
@Composable
private fun StopsMapContent(
    uiState: ClusterMapUiState,
    onClusterClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            // Loading state: Show loading indicator
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.align(Alignment.Center))
            }

            // Error state: Show error message with retry
            uiState.shouldShowErrorState -> {
                ErrorState(
                    errorMessage = uiState.errorMessage ?: "Unknown error",
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Empty state: Show "No clusters found" message
            uiState.shouldShowEmptyState -> {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }

            // Loaded state: Show map with cluster markers
            uiState.hasClusters -> {
                MapWithClusters(
                    uiState = uiState,
                    onClusterClick = onClusterClick
                )
            }
        }
    }
}

/**
 * Map with cluster markers (loaded state).
 *
 * @param uiState Current UI state with clusters
 * @param onClusterClick Callback when marker is tapped
 */
@Composable
private fun MapWithClusters(
    uiState: ClusterMapUiState,
    onClusterClick: (Long) -> Unit
) {
    // Calculate initial camera position from first cluster (if available)
    val initialPosition = if (uiState.clusters.isNotEmpty()) {
        uiState.clusters.first().centerPosition
    } else {
        LatLng(37.422, -122.084) // Default to Google campus
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            initialPosition,
            14f // Zoom level showing ~2km radius (good for cluster overview)
        )
    }

    BikeMap(
        cameraPositionState = cameraPositionState,
        currentBearing = null, // Static map (no bearing rotation for cluster view)
        navigationMode = false, // Centered map (not navigation mode)
        modifier = Modifier.fillMaxSize()
    ) {
        // Render cluster markers
        uiState.clusters.forEach { cluster ->
            ClusterMarker(
                markerData = ClusterMarkerData(
                    clusterId = cluster.clusterId,
                    position = cluster.centerPosition,
                    color = MarkerColor.fromStopCount(cluster.stopCount),
                    stopCount = cluster.stopCount
                ),
                onClick = onClusterClick
            )
        }
    }
}

/**
 * Loading state composable (centered spinner).
 *
 * @param modifier Modifier for positioning
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Empty state composable (no clusters message).
 *
 * @param modifier Modifier for positioning
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No clusters found",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete some rides with stops to see clusters appear here",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error state composable (error message with retry).
 *
 * @param errorMessage User-facing error message
 * @param onRetry Callback when retry button is tapped
 * @param modifier Modifier for positioning
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Failed to load clusters",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
