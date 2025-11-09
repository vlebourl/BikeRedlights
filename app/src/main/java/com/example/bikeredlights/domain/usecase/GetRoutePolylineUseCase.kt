package com.example.bikeredlights.domain.usecase

import androidx.compose.ui.graphics.Color
import com.example.bikeredlights.domain.model.PolylineData
import com.example.bikeredlights.domain.model.TrackPoint
import com.example.bikeredlights.domain.util.simplifyRoute
import com.example.bikeredlights.domain.util.shouldSimplify
import com.example.bikeredlights.domain.util.toLatLngList
import javax.inject.Inject

/**
 * Use case to convert a list of TrackPoints into a PolylineData object ready for map rendering.
 *
 * **Responsibilities**:
 * - Convert TrackPoint domain models to LatLng coordinates
 * - Apply Douglas-Peucker simplification for performance optimization
 * - Configure polyline visual properties (color, width, geodesic)
 *
 * **Simplification Logic**:
 * - Routes with < 100 points: No simplification (minimal performance impact)
 * - Routes with >= 100 points: Apply Douglas-Peucker algorithm with 3m tolerance
 * - Result: ~90% reduction in points with negligible visual difference
 *
 * @property No dependencies required (pure function)
 */
class GetRoutePolylineUseCase @Inject constructor() {

    /**
     * Converts TrackPoints to PolylineData for map rendering.
     *
     * @param trackPoints The list of GPS track points from the ride
     * @param color The color of the polyline (e.g., Color.Red for live route, Color.Blue for completed route)
     * @param width The width of the polyline in pixels (default 10f)
     * @return PolylineData ready for rendering, or null if trackPoints is empty
     *
     * Example usage in ViewModel:
     * ```
     * val trackPoints: List<TrackPoint> = trackPointRepository.getTrackPoints(rideId)
     * val polylineData: PolylineData? = getRoutePolylineUseCase(
     *     trackPoints = trackPoints,
     *     color = Color.Red,
     *     width = 10f
     * )
     * ```
     */
    operator fun invoke(
        trackPoints: List<TrackPoint>,
        color: Color,
        width: Float = 10f
    ): PolylineData? {
        if (trackPoints.isEmpty()) return null

        // Convert TrackPoints to LatLng coordinates
        val latLngList = trackPoints.toLatLngList()

        // Apply simplification only if route is long enough to benefit
        val optimizedPoints = if (latLngList.shouldSimplify()) {
            latLngList.simplifyRoute(toleranceMeters = 3.0)
        } else {
            latLngList
        }

        return PolylineData(
            points = optimizedPoints,
            color = color,
            width = width,
            geodesic = true // Follow Earth's curvature for GPS routes
        )
    }
}
