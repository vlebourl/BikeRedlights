package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.MapBounds
import com.example.bikeredlights.domain.model.TrackPoint
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import javax.inject.Inject

/**
 * Use case to calculate geographic bounds for auto-zooming a map to fit a complete route.
 *
 * **Responsibilities**:
 * - Calculate the smallest bounding box that contains all track points
 * - Apply padding to ensure markers don't touch screen edges
 * - Configure animation duration for smooth camera transitions
 *
 * **Edge Cases**:
 * - Empty route: Returns null (no bounds to calculate)
 * - Single point: Returns null (use fixed zoom level instead)
 * - Two+ points: Returns bounds with 100px padding
 *
 * @property No dependencies required (pure function)
 */
class CalculateMapBoundsUseCase @Inject constructor() {

    /**
     * Calculates map bounds to fit the entire route in the viewport.
     *
     * @param trackPoints The list of GPS track points from the ride
     * @param padding The padding in pixels to apply around the bounds (default 100px)
     * @param animationDurationMs The duration in milliseconds for the camera animation (default 500ms)
     * @return MapBounds with calculated bounding box, or null if insufficient points (< 2)
     *
     * Example usage in ViewModel:
     * ```
     * val trackPoints: List<TrackPoint> = trackPointRepository.getTrackPoints(rideId)
     * val mapBounds: MapBounds? = calculateMapBoundsUseCase(
     *     trackPoints = trackPoints,
     *     padding = 100,
     *     animationDurationMs = 500
     * )
     * if (mapBounds != null) {
     *     cameraPositionState.animate(
     *         CameraUpdateFactory.newLatLngBounds(mapBounds.bounds, mapBounds.padding),
     *         mapBounds.animationDurationMs
     *     )
     * }
     * ```
     */
    operator fun invoke(
        trackPoints: List<TrackPoint>,
        padding: Int = 100,
        animationDurationMs: Int = 500
    ): MapBounds? {
        // Need at least 2 points to calculate bounds
        if (trackPoints.size < 2) return null

        // Build bounding box from all track points
        val boundsBuilder = LatLngBounds.Builder()
        trackPoints.forEach { point ->
            boundsBuilder.include(LatLng(point.latitude, point.longitude))
        }

        return MapBounds(
            bounds = boundsBuilder.build(),
            padding = padding,
            animationDurationMs = animationDurationMs
        )
    }
}
