package com.example.bikeredlights.ui.components.map

import androidx.compose.runtime.Composable
import com.example.bikeredlights.domain.model.MarkerType
import com.example.bikeredlights.domain.model.toIcon
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

/**
 * Composable that renders a location marker on a Google Map.
 *
 * This component displays a single marker representing the user's current location
 * during ride recording. It uses a blue marker icon to distinguish it from
 * start/end markers (green/red).
 *
 * **Null Safety**:
 * If location is null, nothing is rendered. This handles the case when GPS
 * signal is not yet available or location permissions are denied.
 *
 * **Accessibility**:
 * The marker has a content description for screen readers via the title parameter.
 *
 * @param location The GPS coordinates for the marker. Null if location unavailable.
 * @param title Optional title text displayed when marker is tapped (default: "Current Location")
 *
 * Example usage:
 * ```
 * BikeMap {
 *     LocationMarker(location = viewModel.currentLocation)
 * }
 * ```
 */
@Composable
fun LocationMarker(
    location: LatLng?,
    title: String = "Current Location"
) {
    // Don't render if no location available
    if (location == null) return

    Marker(
        state = MarkerState(position = location),
        title = title,
        icon = MarkerType.CURRENT.toIcon()
    )
}
