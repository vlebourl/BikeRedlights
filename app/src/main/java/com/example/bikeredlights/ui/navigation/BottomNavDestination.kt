package com.example.bikeredlights.ui.navigation

/**
 * Bottom navigation destinations for the BikeRedlights app.
 *
 * Tab order (v0.2.0 - v0.5.0):
 * 1. LIVE - Real-time speed tracking (default landing screen)
 * 2. RIDES - Ride history and statistics (added in Feature 3)
 * 3. SETTINGS - App settings and preferences
 *
 * Future (v0.6.0+):
 * 4. STOPS - Red light stop history (added in Feature 6)
 *
 * Each destination corresponds to a top-level screen in the app.
 */
enum class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: String  // Material Icon name
) {
    /**
     * Live speed tracking screen (default).
     * Shows current speed, ride duration, distance.
     */
    LIVE(
        route = "live",
        label = "Live",
        icon = "directions_bike"  // Material Icons: directions_bike
    ),

    /**
     * Ride history screen (Feature 3).
     * Shows list of past rides with statistics.
     */
    RIDES(
        route = "rides",
        label = "Rides",
        icon = "list"  // Material Icons: list or history
    ),

    /**
     * Settings screen (Feature 2A).
     * App preferences: units, GPS accuracy, auto-pause, etc.
     */
    SETTINGS(
        route = "settings",
        label = "Settings",
        icon = "settings"  // Material Icons: settings
    );

    companion object {
        /**
         * Default landing screen when app launches.
         */
        val DEFAULT = LIVE

        /**
         * Get destination from route string.
         * Returns null if route doesn't match any destination.
         */
        fun fromRoute(route: String?): BottomNavDestination? {
            return entries.find { it.route == route }
        }
    }
}
