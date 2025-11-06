package com.example.bikeredlights.data.repository

import com.example.bikeredlights.domain.model.history.SortPreference
import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user settings persistence and retrieval.
 *
 * All settings are stored locally using DataStore Preferences.
 * Reads are reactive (Flow) for automatic UI updates.
 * Writes are async (suspend) for non-blocking persistence.
 *
 * Implementation: [SettingsRepositoryImpl]
 */
interface SettingsRepository {
    /**
     * Reactive stream of user's preferred units system.
     * Emits default value (METRIC) on first read if not yet set.
     * Emits new values whenever setting changes.
     */
    val unitsSystem: Flow<UnitsSystem>

    /**
     * Reactive stream of user's preferred GPS accuracy mode.
     * Emits default value (HIGH_ACCURACY) on first read if not yet set.
     * Emits new values whenever setting changes.
     */
    val gpsAccuracy: Flow<GpsAccuracy>

    /**
     * Reactive stream of user's auto-pause configuration.
     * Emits default value (disabled, 5 minutes) on first read if not yet set.
     * Emits new values whenever setting changes.
     */
    val autoPauseConfig: Flow<AutoPauseConfig>

    /**
     * Update user's preferred units system.
     * Change persists immediately to DataStore.
     *
     * @param units New units system (METRIC or IMPERIAL)
     */
    suspend fun setUnitsSystem(units: UnitsSystem)

    /**
     * Update user's preferred GPS accuracy mode.
     * Change persists immediately to DataStore.
     *
     * @param accuracy New GPS accuracy (BATTERY_SAVER or HIGH_ACCURACY)
     */
    suspend fun setGpsAccuracy(accuracy: GpsAccuracy)

    /**
     * Update user's auto-pause configuration.
     * Change persists immediately to DataStore.
     *
     * @param config New auto-pause configuration
     * @throws IllegalArgumentException if config.thresholdMinutes is invalid
     */
    suspend fun setAutoPauseConfig(config: AutoPauseConfig)

    /**
     * Reactive stream of user's ride list sort preference.
     * Emits default value (NEWEST_FIRST) on first read if not yet set.
     * Emits new values whenever setting changes.
     */
    val rideSortPreference: Flow<SortPreference>

    /**
     * Update user's ride list sort preference.
     * Change persists immediately to DataStore.
     *
     * @param sortPreference New sort preference
     */
    suspend fun setRideSortPreference(sortPreference: SortPreference)
}
