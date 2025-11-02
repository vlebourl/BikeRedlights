package com.example.bikeredlights

import android.app.Application

/**
 * BikeRedlights Application Class
 *
 * Entry point for the application.
 * This class is responsible for:
 * - Application-level configuration
 * - Global application state (if needed)
 *
 * TODO v0.1.0: Add Hilt dependency injection (@HiltAndroidApp)
 */
class BikeRedlightsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Future: Initialize Hilt DI, logging, crash reporting, etc.
    }
}
