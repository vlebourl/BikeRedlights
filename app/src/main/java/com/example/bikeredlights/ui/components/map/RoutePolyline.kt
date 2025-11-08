package com.example.bikeredlights.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.bikeredlights.domain.model.PolylineData
import com.google.maps.android.compose.Polyline

/**
 * Composable that renders a route polyline on a Google Map.
 *
 * This component displays a connected line path representing a bike ride route,
 * with customizable color and width. It's used for both:
 * - Real-time route visualization on the Live tab (growing as the rider moves)
 * - Complete route display on the Review Screen (showing the full saved route)
 *
 * **Null Safety**:
 * If polylineData is null or empty, nothing is rendered. This handles the case
 * when there's no active ride recording or no saved route data.
 *
 * **Performance**:
 * The polyline points are pre-simplified using the Douglas-Peucker algorithm
 * in GetRoutePolylineUseCase, reducing rendering overhead for long routes.
 *
 * @param polylineData The route data containing points, color, width, and geodesic setting.
 *                     Null if no route to display.
 * @param color Optional color override (defaults to polylineData.color)
 *
 * Example usage:
 * ```
 * BikeMap {
 *     RoutePolyline(polylineData = viewModel.polylineData)
 * }
 * ```
 */
@Composable
fun RoutePolyline(
    polylineData: PolylineData?,
    color: Color? = null
) {
    // Don't render if no data or empty points
    if (polylineData == null || polylineData.isEmpty) return

    Polyline(
        points = polylineData.points,
        color = color ?: polylineData.color,
        width = polylineData.width,
        geodesic = polylineData.geodesic
    )
}
