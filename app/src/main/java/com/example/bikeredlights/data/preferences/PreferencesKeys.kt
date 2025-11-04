package com.example.bikeredlights.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * DataStore Preferences keys for user settings.
 *
 * All settings are stored locally on the device using DataStore Preferences.
 * Default values are defined in domain models (UnitsSystem, GpsAccuracy, AutoPauseConfig).
 */
object PreferencesKeys {
    /**
     * User's preferred measurement system.
     * Allowed values: "metric" (default), "imperial"
     * Maps to: UnitsSystem enum
     */
    val UNITS_SYSTEM = stringPreferencesKey("units_system")

    /**
     * User's preferred GPS accuracy mode.
     * Allowed values: "high_accuracy" (default), "battery_saver"
     * Maps to: GpsAccuracy enum
     */
    val GPS_ACCURACY = stringPreferencesKey("gps_accuracy")

    /**
     * Whether auto-pause is enabled for ride recording.
     * Default: false (opt-in feature)
     * Maps to: AutoPauseConfig.enabled
     */
    val AUTO_PAUSE_ENABLED = booleanPreferencesKey("auto_pause_enabled")

    /**
     * Auto-pause threshold in minutes.
     * Allowed values: 1, 2, 3, 5, 10, 15
     * Default: 5
     * Maps to: AutoPauseConfig.thresholdMinutes
     */
    val AUTO_PAUSE_MINUTES = intPreferencesKey("auto_pause_minutes")
}
