package com.example.bikeredlights.domain.model

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

/**
 * Represents the current visual state of a map view.
 *
 * This is a domain model that encapsulates all the necessary information
 * to display and control a map, including camera position, zoom level,
 * user tracking state, and display preferences.
 *
 * @property cameraPosition The current camera position including target location, zoom, bearing, and tilt
 * @property isFollowingUser Whether the camera should automatically follow the user's current location
 * @property mapType The type of map tiles to display (NORMAL, SATELLITE, HYBRID, TERRAIN)
 * @property isDarkMode Whether to use dark mode styling for the map
 *
 * @see CameraPosition for camera positioning details
 */
data class MapViewState(
    val cameraPosition: CameraPosition = CameraPosition.fromLatLngZoom(
        LatLng(37.422, -122.084), // Default to Google campus
        17f // City block level zoom (50-200m radius)
    ),
    val isFollowingUser: Boolean = true,
    val mapType: Int = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL,
    val isDarkMode: Boolean = false
)
