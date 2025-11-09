package com.example.bikeredlights.domain.util

import com.example.bikeredlights.domain.model.TrackPoint
import com.google.android.gms.maps.model.LatLng

/**
 * Extension functions for TrackPoint collections to facilitate map rendering.
 *
 * These functions convert TrackPoint domain models into Google Maps-compatible
 * data structures (LatLng) for polyline rendering and marker placement.
 */

/**
 * Converts a list of TrackPoints to a list of LatLng coordinates.
 *
 * This function extracts latitude and longitude from each TrackPoint
 * and creates Google Maps LatLng objects suitable for polyline rendering.
 *
 * @return List of LatLng coordinates in the same order as the input TrackPoints
 *
 * Example usage:
 * ```
 * val trackPoints: List<TrackPoint> = repository.getTrackPoints(rideId)
 * val latLngList: List<LatLng> = trackPoints.toLatLngList()
 * Polyline(points = latLngList, color = Color.Red)
 * ```
 */
fun List<TrackPoint>.toLatLngList(): List<LatLng> {
    return map { point ->
        LatLng(point.latitude, point.longitude)
    }
}

/**
 * Gets the start location (first track point's coordinates) from a list of TrackPoints.
 *
 * @return LatLng of the first track point, or null if the list is empty
 *
 * Example usage:
 * ```
 * val startLocation = trackPoints.startLocation()
 * if (startLocation != null) {
 *     Marker(position = startLocation, icon = BitmapDescriptorFactory.defaultMarker(GREEN))
 * }
 * ```
 */
fun List<TrackPoint>.startLocation(): LatLng? {
    return firstOrNull()?.let { point ->
        LatLng(point.latitude, point.longitude)
    }
}

/**
 * Gets the end location (last track point's coordinates) from a list of TrackPoints.
 *
 * @return LatLng of the last track point, or null if the list is empty
 *
 * Example usage:
 * ```
 * val endLocation = trackPoints.endLocation()
 * if (endLocation != null) {
 *     Marker(position = endLocation, icon = BitmapDescriptorFactory.defaultMarker(RED))
 * }
 * ```
 */
fun List<TrackPoint>.endLocation(): LatLng? {
    return lastOrNull()?.let { point ->
        LatLng(point.latitude, point.longitude)
    }
}
