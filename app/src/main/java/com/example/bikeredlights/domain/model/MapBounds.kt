package com.example.bikeredlights.domain.model

import com.google.android.gms.maps.model.LatLngBounds

/**
 * Represents the data needed to auto-zoom a map to fit a specific geographic area.
 *
 * This model encapsulates the geographic bounds and animation parameters
 * required to adjust the map camera to show a complete route or area of interest.
 *
 * @property bounds The geographic bounds (southwest and northeast corners) to fit in the viewport
 * @property padding The padding in pixels to apply around the bounds (ensures markers don't touch edges)
 * @property animationDurationMs The duration in milliseconds for the camera animation to the new bounds
 *
 * Example usage:
 * ```
 * val routeBounds = MapBounds(
 *     bounds = LatLngBounds(
 *         LatLng(37.420, -122.085),  // Southwest corner
 *         LatLng(37.425, -122.080)   // Northeast corner
 *     ),
 *     padding = 100, // 100px padding
 *     animationDurationMs = 500
 * )
 * ```
 */
data class MapBounds(
    val bounds: LatLngBounds,
    val padding: Int = 100, // Default 100px padding around bounds
    val animationDurationMs: Int = 500 // Default 500ms animation
)
