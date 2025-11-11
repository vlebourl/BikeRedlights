package com.example.bikeredlights.ui.components.map

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.bikeredlights.domain.model.MarkerType
import com.example.bikeredlights.domain.model.toIcon
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

/**
 * Helper function to create a navigation arrow bitmap from drawable resource.
 *
 * Loads the navigation arrow icon from drawable resources and converts it to a BitmapDescriptor.
 * The arrow points upward (north) and will be rotated via the Marker's rotation parameter.
 * Scales the icon to a reasonable size for map display (64dp).
 *
 * @param context Android context for accessing drawable resources
 * @return BitmapDescriptor of the navigation arrow icon
 */
private fun createNavigationArrowIcon(context: android.content.Context): BitmapDescriptor {
    // Load the navigation arrow drawable
    val drawable = androidx.core.content.ContextCompat.getDrawable(
        context,
        com.example.bikeredlights.R.drawable.ic_navigation_arrow
    )

    if (drawable != null) {
        // Target size in pixels (64dp * density for high-DPI screens)
        val density = context.resources.displayMetrics.density
        val targetSize = (64 * density).toInt() // 64dp in pixels

        // Create bitmap at target size (scaled from original)
        val bitmap = Bitmap.createBitmap(
            targetSize,
            targetSize,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Fallback to default blue marker if drawable not found
    return MarkerType.CURRENT.toIcon()
}

/**
 * Composable that renders a location marker on a Google Map with directional arrow support (Feature 007 - v0.6.1).
 *
 * This component displays a marker representing the user's current location:
 * - **Always shows navigation arrow icon** (even when stationary)
 * - **When moving (bearing available)**: Arrow rotates to show heading direction
 * - **When stationary (bearing null)**: Arrow points north (0°)
 *
 * **Null Safety**:
 * If location is null, nothing is rendered. This handles the case when GPS
 * signal is not yet available or location permissions are denied.
 *
 * **Accessibility**:
 * The marker has a content description for screen readers via the title parameter.
 *
 * @param location The GPS coordinates for the marker. Null if location unavailable.
 * @param bearing GPS bearing in degrees (0-360) for directional arrow rotation. Null defaults to 0° (north).
 * @param title Optional title text displayed when marker is tapped (default: "Current Location")
 *
 * Example usage:
 * ```
 * val currentBearing by viewModel.currentBearing.collectAsStateWithLifecycle()
 * BikeMap {
 *     LocationMarker(
 *         location = viewModel.currentLocation,
 *         bearing = currentBearing
 *     )
 * }
 * ```
 */
@Composable
fun LocationMarker(
    location: LatLng?,
    bearing: Float? = null,
    title: String = "Current Location"
) {
    // Don't render if no location available
    if (location == null) return

    val context = androidx.compose.ui.platform.LocalContext.current

    // Create navigation arrow icon (cached via remember)
    val navigationArrow = remember { createNavigationArrowIcon(context) }

    Marker(
        state = remember(location) { MarkerState(position = location) },
        title = if (bearing != null) {
            "$title (heading ${bearing.toInt()}°)"
        } else {
            title
        },
        icon = navigationArrow, // Always use navigation arrow
        rotation = bearing ?: 0f, // Rotate to bearing, or point north (0°) when stationary
        flat = true // Makes marker stick to map plane (important for rotation visibility)
    )
}
