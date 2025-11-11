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
 * Darkens blue colors in a bitmap for better visibility on light map backgrounds.
 *
 * Applies a darkening filter to blue-ish pixels while preserving transparency.
 * Makes light blues darker by reducing RGB values proportionally.
 *
 * @param bitmap Original bitmap to darken
 * @return New bitmap with darkened blue colors
 */
private fun darkenBlueColors(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val darkenedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel)

            if (alpha > 0) { // Only process non-transparent pixels
                val red = android.graphics.Color.red(pixel)
                val green = android.graphics.Color.green(pixel)
                val blue = android.graphics.Color.blue(pixel)

                // Check if pixel is blue-ish (blue channel is dominant)
                if (blue > red && blue > green) {
                    // Darken by reducing all RGB values by 30%
                    val darkenedRed = (red * 0.7f).toInt()
                    val darkenedGreen = (green * 0.7f).toInt()
                    val darkenedBlue = (blue * 0.7f).toInt()

                    darkenedBitmap.setPixel(x, y, android.graphics.Color.argb(
                        alpha, darkenedRed, darkenedGreen, darkenedBlue
                    ))
                } else {
                    // Keep non-blue pixels unchanged (white outline, etc.)
                    darkenedBitmap.setPixel(x, y, pixel)
                }
            }
        }
    }

    return darkenedBitmap
}

/**
 * Helper function to create a navigation arrow bitmap from drawable resource.
 *
 * Loads the navigation arrow icon from drawable resources and converts it to a BitmapDescriptor.
 * The arrow points upward (north) and will be rotated via the Marker's rotation parameter.
 * Scales the icon to a reasonable size for map display and darkens blue colors for visibility.
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
        // Target size in pixels (36dp * density for high-DPI screens)
        val density = context.resources.displayMetrics.density
        val targetSize = (36 * density).toInt() // 36dp in pixels

        // Create bitmap at target size (scaled from original)
        val originalBitmap = Bitmap.createBitmap(
            targetSize,
            targetSize,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(originalBitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)

        // Darken the blue color for better visibility on light backgrounds
        val darkenedBitmap = darkenBlueColors(originalBitmap)
        return BitmapDescriptorFactory.fromBitmap(darkenedBitmap)
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
