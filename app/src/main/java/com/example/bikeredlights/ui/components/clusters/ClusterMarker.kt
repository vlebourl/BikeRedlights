package com.example.bikeredlights.ui.components.clusters

import androidx.compose.runtime.Composable
import com.example.bikeredlights.domain.model.ClusterMarkerData
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

/**
 * Composable for rendering a cluster marker on Google Maps.
 *
 * Created by: Feature 011 - User Story 1 (View Clusters on Map)
 * Used by: StopsMapScreen to display cluster locations
 *
 * Visual Design:
 * - Default Google Maps pin marker with custom color
 * - Color indicates cluster size:
 *   - GREEN: Small clusters (2-5 stops)
 *   - YELLOW: Medium clusters (6-10 stops)
 *   - RED: Large clusters (11+ stops)
 * - Marker title shows stop count
 * - Marker snippet shows cluster ID (for debugging)
 *
 * Accessibility:
 * - Marker has content description: "Cluster marker: {stopCount} stops"
 * - Tappable area: 48dp minimum (Google Maps default)
 *
 * Interaction:
 * - Tap marker: Triggers onClick callback with cluster ID
 * - Info window: Shows stop count
 *
 * Performance:
 * - Stateless composable (no internal state)
 * - Recomposition only when markerData changes
 * - Google Maps SDK handles marker rendering efficiently
 *
 * @param markerData Cluster marker data (position, color, stop count)
 * @param onClick Callback when marker is tapped (passes cluster ID)
 *
 * Example usage:
 * ```
 * BikeMap(cameraPositionState = cameraState) {
 *     clusters.forEach { cluster ->
 *         ClusterMarker(
 *             markerData = ClusterMarkerData(
 *                 clusterId = cluster.clusterId,
 *                 position = cluster.centerPosition,
 *                 color = MarkerColor.fromStopCount(cluster.stopCount),
 *                 stopCount = cluster.stopCount
 *             ),
 *             onClick = { clusterId -> viewModel.selectCluster(clusterId) }
 *         )
 *     }
 * }
 * ```
 */
@Composable
fun ClusterMarker(
    markerData: ClusterMarkerData,
    onClick: (Long) -> Unit
) {
    Marker(
        state = MarkerState(position = markerData.position),
        title = "${markerData.stopCount} stops",
        snippet = "Cluster #${markerData.clusterId}",
        // Set marker color based on cluster size
        icon = BitmapDescriptorFactory.defaultMarker(markerData.color.hue),
        // Handle marker tap
        onClick = {
            onClick(markerData.clusterId)
            true // Consume the click event
        },
        // Accessibility
        contentDescription = "Cluster marker: ${markerData.stopCount} stops at this location"
    )
}
