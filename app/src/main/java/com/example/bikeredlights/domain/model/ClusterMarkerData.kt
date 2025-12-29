package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng

/**
 * Domain model for cluster marker visual properties.
 *
 * Created by Feature 011: Stop Cluster Visualization.
 * Maps cluster size to visual representation (color) for map markers.
 *
 * Business Rules (FR-003):
 * - GREEN: Small clusters (2-5 stops)
 * - YELLOW: Medium clusters (6-10 stops)
 * - RED: Large clusters (11+ stops)
 *
 * @property clusterId Cluster identifier for marker key
 * @property position Marker position on map (cluster center)
 * @property color Marker color based on cluster size
 * @property stopCount Number of stops for accessibility description
 */
@Immutable
data class ClusterMarkerData(
    val clusterId: Long,
    val position: LatLng,
    val color: MarkerColor,
    val stopCount: Int
)

/**
 * Enum representing marker colors based on cluster size.
 *
 * Uses Google Maps BitmapDescriptorFactory hue constants for marker tinting.
 *
 * @property hue Float value for Google Maps marker hue (0-360 degrees on color wheel)
 */
enum class MarkerColor(val hue: Float) {
    /**
     * Green marker for small clusters (2-5 stops).
     * Hue value: 120 degrees (green on HSV color wheel).
     */
    GREEN(BitmapDescriptorFactory.HUE_GREEN),

    /**
     * Yellow marker for medium clusters (6-10 stops).
     * Hue value: 60 degrees (yellow on HSV color wheel).
     */
    YELLOW(BitmapDescriptorFactory.HUE_YELLOW),

    /**
     * Red marker for large clusters (11+ stops).
     * Hue value: 0 degrees (red on HSV color wheel).
     */
    RED(BitmapDescriptorFactory.HUE_RED);

    companion object {
        /**
         * Determine marker color based on cluster size.
         *
         * @param stopCount Number of stops in cluster
         * @return Appropriate MarkerColor based on size thresholds
         */
        fun fromStopCount(stopCount: Int): MarkerColor {
            return when {
                stopCount <= 5 -> GREEN
                stopCount <= 10 -> YELLOW
                else -> RED
            }
        }
    }
}
