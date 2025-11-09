package com.example.bikeredlights.ui.components.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import androidx.compose.ui.platform.LocalContext
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Reusable Material 3-styled Google Maps composable wrapper for BikeRedlights.
 *
 * This composable provides a consistent map experience across the app with:
 * - Material 3 dark mode support (automatically switches based on system theme)
 * - Optimized UI controls for cycling use case
 * - Custom camera position management
 * - Slot pattern for flexible content injection (markers, polylines, etc.)
 *
 * **Material 3 Integration**:
 * - Dark mode: Uses Google Maps dark theme when system dark mode is enabled
 * - Light mode: Uses standard Google Maps light theme
 *
 * **Accessibility**:
 * - Map controls (zoom, compass, location) have 48dp minimum touch targets
 * - All interactive elements have content descriptions (set by Google Maps SDK)
 *
 * @param modifier Modifier for the map container
 * @param cameraPositionState State holder for camera position (use rememberCameraPositionState())
 * @param mapType Type of map tiles (NORMAL, SATELLITE, HYBRID, TERRAIN)
 * @param showMyLocationButton Whether to show the "My Location" FAB (default true)
 * @param showZoomControls Whether to show zoom +/- buttons (default true)
 * @param content Slot for map content (Markers, Polylines, etc.)
 *
 * Example usage:
 * ```
 * val cameraPosition = rememberCameraPositionState {
 *     position = CameraPosition.fromLatLngZoom(LatLng(37.422, -122.084), 17f)
 * }
 * BikeMap(
 *     cameraPositionState = cameraPosition,
 *     modifier = Modifier.fillMaxSize()
 * ) {
 *     // Map content (markers, polylines)
 *     Marker(position = LatLng(37.422, -122.084), title = "Current Location")
 *     Polyline(points = routePoints, color = Color.Red)
 * }
 * ```
 */
@Composable
fun BikeMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(37.422, -122.084), // Default to Google campus
            17f // City block level zoom (50-200m radius)
        )
    },
    mapType: MapType = MapType.NORMAL,
    showMyLocationButton: Boolean = true,
    showZoomControls: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    val isDarkMode = isSystemInDarkTheme()
    val context = LocalContext.current

    // Map properties for Material 3 theming with dark mode support
    // Using Google's predefined dark mode style JSON
    val mapProperties = remember(isDarkMode, mapType) {
        MapProperties(
            mapType = mapType,
            isMyLocationEnabled = false, // We'll handle location markers manually for better control
            mapStyleOptions = if (isDarkMode) {
                // Simple dark mode JSON style
                MapStyleOptions("""
                    [
                      {"elementType": "geometry", "stylers": [{"color": "#242f3e"}]},
                      {"elementType": "labels.text.fill", "stylers": [{"color": "#746855"}]},
                      {"elementType": "labels.text.stroke", "stylers": [{"color": "#242f3e"}]},
                      {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#38414e"}]},
                      {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#212a37"}]},
                      {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9ca5b3"}]},
                      {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#17263c"}]}
                    ]
                """.trimIndent())
            } else {
                null // Use default light style
            }
        )
    }

    // Map UI settings optimized for cycling use case
    val mapUiSettings = remember(showMyLocationButton, showZoomControls) {
        MapUiSettings(
            zoomControlsEnabled = showZoomControls,
            myLocationButtonEnabled = showMyLocationButton,
            compassEnabled = true, // Helpful for navigation
            rotationGesturesEnabled = true, // Allow map rotation
            tiltGesturesEnabled = false, // Disable 3D tilt (not useful for cycling)
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        content = content
    )
}
