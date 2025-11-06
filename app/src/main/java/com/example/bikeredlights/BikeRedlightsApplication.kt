package com.example.bikeredlights

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * BikeRedlights Application Class
 *
 * Entry point for the application.
 * This class is responsible for:
 * - Application-level configuration
 * - Hilt dependency injection initialization
 * - Global application state (if needed)
 *
 * @HiltAndroidApp triggers Hilt code generation for the application.
 * This annotation is required for all Hilt-injected components to function.
 */
@HiltAndroidApp
class BikeRedlightsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Hilt DI is automatically initialized via @HiltAndroidApp annotation
        // Future: Add logging, crash reporting, etc.
    }
}
