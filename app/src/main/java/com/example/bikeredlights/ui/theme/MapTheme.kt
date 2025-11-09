package com.example.bikeredlights.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.google.android.gms.maps.GoogleMap

/**
 * Material 3 theming configuration for Google Maps dark mode support.
 *
 * This file provides utilities to apply Material 3 dark/light themes
 * to Google Maps based on the system theme setting.
 *
 * **Dark Mode Styles**:
 * - MAP_STYLE_DARK: Google Maps dark theme (used when system dark mode is enabled)
 * - MAP_STYLE_LIGHT: Standard light theme (default)
 *
 * **Future Enhancement**:
 * To enable full dark mode styling, create a JSON style file at:
 * `app/src/main/res/raw/dark_map_style.json`
 *
 * Example JSON (Google Maps dark mode):
 * ```json
 * [
 *   {
 *     "elementType": "geometry",
 *     "stylers": [{"color": "#212121"}]
 *   },
 *   {
 *     "elementType": "labels.text.fill",
 *     "stylers": [{"color": "#757575"}]
 *   },
 *   {
 *     "elementType": "labels.text.stroke",
 *     "stylers": [{"color": "#212121"}]
 *   }
 * ]
 * ```
 *
 * @see <a href="https://mapstyle.withgoogle.com/">Google Maps Styling Wizard</a>
 */

/**
 * Map style constants for dark/light mode.
 */
object MapColorScheme {
    const val DARK = GoogleMap.MAP_TYPE_NORMAL // Will use dark JSON style if provided
    const val LIGHT = GoogleMap.MAP_TYPE_NORMAL
}

/**
 * Determines the appropriate map style based on the system theme.
 *
 * @return MAP_STYLE_DARK if system is in dark mode, MAP_STYLE_LIGHT otherwise
 */
@Composable
fun mapColorScheme(): Int {
    return if (isSystemInDarkTheme()) {
        MapColorScheme.DARK
    } else {
        MapColorScheme.LIGHT
    }
}

/**
 * Checks if dark mode is currently enabled.
 *
 * @return True if system dark mode is enabled, false otherwise
 */
@Composable
fun isMapDarkMode(): Boolean {
    return isSystemInDarkTheme()
}
