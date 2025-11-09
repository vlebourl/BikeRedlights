package com.example.bikeredlights.domain.model

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng

/**
 * Represents the data needed to render a polyline (route path) on a map.
 *
 * This model encapsulates all the visual properties and coordinate data
 * for drawing a connected line path on a map, typically used to show
 * a bike ride route.
 *
 * @property points The list of GPS coordinates that make up the polyline path
 * @property color The color of the polyline (e.g., Red for live route, primary for completed route)
 * @property width The width of the polyline in pixels
 * @property geodesic Whether the polyline should follow the curvature of the Earth (true for GPS routes)
 *
 * Example usage:
 * ```
 * val routePolyline = PolylineData(
 *     points = listOf(LatLng(37.422, -122.084), LatLng(37.423, -122.083)),
 *     color = Color.Red,
 *     width = 10f,
 *     geodesic = true
 * )
 * ```
 */
data class PolylineData(
    val points: List<LatLng>,
    val color: Color,
    val width: Float = 10f,
    val geodesic: Boolean = true
) {
    /**
     * Whether this polyline is empty (has no points to render).
     */
    val isEmpty: Boolean
        get() = points.isEmpty()

    /**
     * Whether this polyline has at least one point.
     */
    val isNotEmpty: Boolean
        get() = points.isNotEmpty()
}
