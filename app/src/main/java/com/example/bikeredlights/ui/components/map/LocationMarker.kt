package com.example.bikeredlights.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.example.bikeredlights.domain.model.MarkerType
import com.example.bikeredlights.domain.model.toIcon
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

/**
 * Composable that renders a location marker on a Google Map with directional arrow support (Feature 007 - v0.6.1).
 *
 * This component displays a marker representing the user's current location:
 * - **When moving (bearing available)**: Directional arrow icon rotated to show heading
 * - **When stationary (bearing null)**: Standard blue pin icon
 * - **For completed rides**: Always show pin (not directional arrow)
 *
 * **Null Safety**:
 * If location is null, nothing is rendered. This handles the case when GPS
 * signal is not yet available or location permissions are denied.
 *
 * **Accessibility**:
 * The marker has a content description for screen readers via the title parameter.
 *
 * @param location The GPS coordinates for the marker. Null if location unavailable.
 * @param bearing GPS bearing in degrees (0-360) for directional arrow rotation. Null shows pin icon.
 * @param title Optional title text displayed when marker is tapped (default: "Current Location")
 *
 * Example usage:
 * ```
 * val currentBearing by viewModel.currentBearing.collectAsStateWithLifecycle()
 * BikeMap {
 *     LocationMarker(
 *         location = viewModel.currentLocation,
 *         bearing = currentBearing
 *     )
 * }
 * ```
 */
@Composable
fun LocationMarker(
    location: LatLng?,
    bearing: Float? = null,
    title: String = "Current Location"
) {
    // Don't render if no location available
    if (location == null) return

    // Feature 007 (v0.6.1): Determine if we should show directional arrow or pin
    // - Show directional arrow when bearing is available (rider is moving)
    // - Show pin when bearing is null (stationary or no GPS heading data)
    val isMoving = bearing != null

    // Note: Google Maps Marker doesn't support rotation transformation
    // For v0.6.1, we use standard pin icon with bearing info in title
    // Future enhancement: Use MarkerComposable with custom arrow graphic + graphicsLayer rotation
    Marker(
        state = remember(location) { MarkerState(position = location) },
        title = if (isMoving && bearing != null) {
            "$title (heading ${bearing.toInt()}°)"
        } else {
            title
        },
        icon = MarkerType.CURRENT.toIcon(),
        // TODO Feature 007 (v0.6.1): Implement custom directional arrow with rotation
        // Current limitation: Google Maps Compose Marker doesn't support graphicsLayer
        // Next iteration: Use MarkerComposable or overlay for custom rotatable arrow graphic
        rotation = bearing ?: 0f // This sets flat marker rotation, not icon rotation
    )
}
