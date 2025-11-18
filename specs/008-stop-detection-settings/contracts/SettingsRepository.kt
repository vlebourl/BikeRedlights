/**
 * Settings Repository Contract Extension
 *
 * Feature: 008-stop-detection-settings
 * Phase: 1 - Design
 *
 * This contract defines the interface extension for SettingsRepository
 * to support stop detection configuration persistence.
 *
 * IMPORTANT: This is a CONTRACT DEFINITION, not implementation code.
 * It documents the expected interface for implementation.
 */

package com.example.bikeredlights.data.repository

import com.example.bikeredlights.domain.model.settings.StopDetectionConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user settings persistence and retrieval.
 *
 * ** NEW in Feature 008: Stop Detection Settings **
 */
interface SettingsRepository {

    // ========== EXISTING METHODS (Feature 002) ==========
    // val unitsSystem: Flow<UnitsSystem>
    // val gpsAccuracy: Flow<GpsAccuracy>
    // val autoPauseConfig: Flow<AutoPauseConfig>
    // val rideSortPreference: Flow<SortPreference>
    // suspend fun setUnitsSystem(units: UnitsSystem)
    // suspend fun setGpsAccuracy(accuracy: GpsAccuracy)
    // suspend fun setAutoPauseConfig(config: AutoPauseConfig)
    // suspend fun setRideSortPreference(sortPreference: SortPreference)

    // ========== NEW METHODS (Feature 008) ==========

    /**
     * Reactive stream of stop detection configuration.
     *
     * **Behavior**:
     * - Emits default values (3 km/h, 15s, 20m) on first read if not yet set
     * - Emits new values immediately when [setStopDetectionConfig] is called
     * - Never completes (hot flow)
     * - Shares same DataStore instance across all subscribers
     *
     * **Default Values** (when no prior settings exist):
     * - speedThresholdKmh: 3.0f
     * - durationThresholdSeconds: 15
     * - clusteringRadiusMeters: 20
     *
     * **Thread Safety**: Flow is thread-safe, safe to collect from any coroutine
     *
     * **Usage Example**:
     * ```kotlin
     * viewModelScope.launch {
     *     settingsRepository.stopDetectionConfig.collect { config ->
     *         _uiState.update { it.copy(stopDetectionConfig = config) }
     *     }
     * }
     * ```
     */
    val stopDetectionConfig: Flow<StopDetectionConfig>

    /**
     * Update stop detection configuration.
     *
     * **Behavior**:
     * - Persists all three values atomically to DataStore
     * - Triggers emission on [stopDetectionConfig] Flow
     * - Validates config via [StopDetectionConfig] constructor
     * - Non-blocking (suspend function)
     *
     * **Validation**:
     * - speedThresholdKmh must be in [1, 2, 3, 4, 5] km/h
     * - durationThresholdSeconds must be in [5, 10, 15, 20, 25, 30] seconds
     * - clusteringRadiusMeters must be in [10, 15, 20, 25, 30, 40, 50] meters
     *
     * **Error Handling**:
     * - Throws [IllegalArgumentException] if config contains invalid values
     * - Exception is propagated from [StopDetectionConfig] constructor
     * - DataStore write is atomic (all or nothing)
     *
     * **Thread Safety**: Safe to call from any coroutine context
     *
     * **Usage Example**:
     * ```kotlin
     * viewModelScope.launch {
     *     try {
     *         val newConfig = StopDetectionConfig(
     *             speedThresholdKmh = 2f,
     *             durationThresholdSeconds = 10,
     *             clusteringRadiusMeters = 15
     *         )
     *         settingsRepository.setStopDetectionConfig(newConfig)
     *         // UI updates automatically via stopDetectionConfig Flow
     *     } catch (e: IllegalArgumentException) {
     *         // Handle validation error (UI should prevent this)
     *         Log.e("Settings", "Invalid config: ${e.message}")
     *     }
     * }
     * ```
     *
     * @param config New stop detection configuration (must be valid)
     * @throws IllegalArgumentException if config contains invalid values
     */
    suspend fun setStopDetectionConfig(config: StopDetectionConfig)
}

/**
 * Contract Guarantees (Testing Checklist)
 *
 * ✅ stopDetectionConfig Flow emits defaults when no DataStore keys exist
 * ✅ stopDetectionConfig Flow emits persisted values when keys exist
 * ✅ setStopDetectionConfig() writes all 3 DataStore keys atomically
 * ✅ setStopDetectionConfig() triggers Flow emission immediately
 * ✅ setStopDetectionConfig() throws IllegalArgumentException for invalid config
 * ✅ Multiple subscribers to stopDetectionConfig receive same values
 * ✅ Flow emission happens on background thread (safe for main thread collection)
 */
