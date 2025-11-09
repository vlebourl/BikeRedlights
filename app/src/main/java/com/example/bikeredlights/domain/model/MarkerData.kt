package com.example.bikeredlights.domain.model

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng

/**
 * Enum representing the different types of markers that can be displayed on a map.
 *
 * @property START Green pin marker indicating the start location of a ride
 * @property END Red flag marker indicating the end location of a ride
 * @property CURRENT Blue dot marker indicating the user's current location
 */
enum class MarkerType {
    START,
    END,
    CURRENT
}

/**
 * Extension function to convert a MarkerType to a BitmapDescriptor icon.
 *
 * @return BitmapDescriptor with the appropriate color for the marker type
 */
fun MarkerType.toIcon(): BitmapDescriptor = when (this) {
    MarkerType.START -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
    MarkerType.END -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    MarkerType.CURRENT -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
}

/**
 * Represents the data needed to render a marker (pin/icon) on a map.
 *
 * This model encapsulates all the information required to display a location
 * marker on a map, including its position, type (which determines icon color),
 * and optional metadata like title and description.
 *
 * @property position The GPS coordinates where the marker should be placed
 * @property type The type of marker (START, END, or CURRENT) which determines the icon color
 * @property title Optional title text displayed when the marker is tapped
 * @property snippet Optional description text displayed below the title when the marker is tapped
 * @property visible Whether the marker should be visible on the map
 *
 * Example usage:
 * ```
 * val startMarker = MarkerData(
 *     position = LatLng(37.422, -122.084),
 *     type = MarkerType.START,
 *     title = "Ride Start",
 *     snippet = "Started at 10:00 AM",
 *     visible = true
 * )
 * ```
 */
data class MarkerData(
    val position: LatLng,
    val type: MarkerType,
    val title: String? = null,
    val snippet: String? = null,
    val visible: Boolean = true
)
