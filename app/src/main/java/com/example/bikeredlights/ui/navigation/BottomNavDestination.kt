package com.example.bikeredlights.ui.navigation

/**
 * Bottom navigation destinations for the BikeRedlights app.
 *
 * Tab order (v0.11.0+):
 * 1. LIVE - Real-time speed tracking (default landing screen)
 * 2. RIDES - Ride history and statistics (added in Feature 3)
 * 3. STOPS - Stop cluster visualization (added in Feature 011)
 * 4. SETTINGS - App settings and preferences
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
     * Stop clusters map screen (Feature 011).
     * Shows clustered stops on interactive map with filtering.
     */
    STOPS(
        route = "stops",
        label = "Stops",
        icon = "stop_circle"  // Material Icons: stop_circle (outlined)
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

/**
 * Settings detail destinations (nested under Settings tab).
 *
 * These are the detail screens accessible from the Settings home screen.
 */
enum class SettingsDestination(
    val route: String
) {
    /**
     * Ride & Tracking settings detail screen.
     * Contains: Units, GPS Accuracy, Auto-Pause settings.
     */
    RIDE_TRACKING(route = "settings/ride_tracking"),

    /**
     * Stop Detection settings detail screen (Feature 008 - v0.8.0).
     * Contains: Speed threshold, Duration threshold, Clustering radius.
     */
    STOP_DETECTION(route = "settings/stop_detection");

    companion object {
        /**
         * Get destination from route string.
         * Returns null if route doesn't match any destination.
         */
        fun fromRoute(route: String?): SettingsDestination? {
            return entries.find { it.route == route }
        }
    }
}
