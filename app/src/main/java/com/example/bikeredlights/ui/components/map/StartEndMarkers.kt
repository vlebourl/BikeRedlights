package com.example.bikeredlights.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bikeredlights.domain.model.MarkerData
import com.example.bikeredlights.domain.model.toIcon
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

/**
 * Composable that renders start and end markers on a Google Map.
 *
 * This component displays location markers for the beginning and end of a ride:
 * - Green marker for ride start location
 * - Red marker for ride end location
 *
 * **Accessibility**:
 * - Each marker has a title and snippet for TalkBack support
 * - Markers use distinct colors (green/red) for visual differentiation
 *
 * @param markers List of MarkerData containing position, type, title, and snippet
 * @param modifier Modifier for the markers
 *
 * Example usage:
 * ```
 * GoogleMap(...) {
 *     StartEndMarkers(
 *         markers = listOf(
 *             MarkerData(
 *                 position = LatLng(37.422, -122.084),
 *                 type = MarkerType.START,
 *                 title = "Start",
 *                 snippet = "8:30 AM"
 *             ),
 *             MarkerData(
 *                 position = LatLng(37.423, -122.083),
 *                 type = MarkerType.END,
 *                 title = "End",
 *                 snippet = "9:15 AM"
 *             )
 *         )
 *     )
 * }
 * ```
 */
@Composable
@GoogleMapComposable
fun StartEndMarkers(
    markers: List<MarkerData>,
    modifier: Modifier = Modifier
) {
    markers.forEach { markerData ->
        if (markerData.visible) {
            Marker(
                state = MarkerState(position = markerData.position),
                title = markerData.title ?: "",
                snippet = markerData.snippet ?: "",
                icon = markerData.type.toIcon()
            )
        }
    }
}
