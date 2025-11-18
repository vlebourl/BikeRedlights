/**
 * Settings ViewModel Contract Extension
 *
 * Feature: 008-stop-detection-settings
 * Phase: 1 - Design
 *
 * This contract defines the ViewModel interface extension for exposing
 * stop detection configuration to the UI layer.
 *
 * IMPORTANT: This is a CONTRACT DEFINITION, not implementation code.
 * It documents the expected ViewModel behavior for implementation.
 */

package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.settings.StopDetectionConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for Settings screens.
 *
 * ** NEW in Feature 008: Stop Detection Settings **
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ========== EXISTING STATE FLOWS (Feature 002) ==========
    // val unitsSystem: StateFlow<UnitsSystem>
    // val gpsAccuracy: StateFlow<GpsAccuracy>
    // val autoPauseConfig: StateFlow<AutoPauseConfig>
    // val rideSortPreference: StateFlow<SortPreference>

    // ========== NEW STATE FLOW (Feature 008) ==========

    /**
     * Stop detection configuration state.
     *
     * **Behavior**:
     * - Emits default values (3 km/h, 15s, 20m) on first subscription if not yet set
     * - Emits new values immediately when [updateStopDetectionConfig] is called
     * - Survives configuration changes (ViewModel lifecycle)
     * - Shared across all UI subscribers (hot flow)
     * - Collects on viewModelScope (survives screen composition/recomposition)
     *
     * **Initial Value**: StopDetectionConfig() with defaults
     *
     * **Thread Safety**: Safe to collect from UI layer (main thread)
     *
     * **Lifecycle**:
     * - Starts when ViewModel created
     * - Active while ViewModel alive
     * - Cancelled when ViewModel cleared
     *
     * **Usage in Composable**:
     * ```kotlin
     * @Composable
     * fun StopDetectionSettingsScreen(
     *     viewModel: SettingsViewModel = hiltViewModel()
     * ) {
     *     val stopDetectionConfig by viewModel.stopDetectionConfig.collectAsStateWithLifecycle()
     *
     *     // UI displays current config
     *     Text("Speed Threshold: ${stopDetectionConfig.speedThresholdKmh} km/h")
     * }
     * ```
     *
     * **Type**: StateFlow<StopDetectionConfig>
     * **Scope**: viewModelScope (WhileSubscribed with 5-second timeout)
     * **Initial Value**: StopDetectionConfig() (defaults: 3 km/h, 15s, 20m)
     */
    val stopDetectionConfig: StateFlow<StopDetectionConfig>
        get() = TODO("Implementation in SettingsViewModel.kt")

    // ========== NEW METHODS (Feature 008) ==========

    /**
     * Update stop detection configuration.
     *
     * **Behavior**:
     * - Validates config via [StopDetectionConfig] constructor
     * - Persists to repository (async, non-blocking)
     * - Updates [stopDetectionConfig] StateFlow automatically via repository Flow
     * - Launches in viewModelScope (cancelled if ViewModel cleared)
     *
     * **Validation**:
     * - speedThresholdKmh must be in [1, 2, 3, 4, 5] km/h
     * - durationThresholdSeconds must be in [5, 10, 15, 20, 25, 30] seconds
     * - clusteringRadiusMeters must be in [10, 15, 20, 25, 30, 40, 50] meters
     *
     * **Error Handling**:
     * - Throws [IllegalArgumentException] if config invalid
     * - UI should prevent invalid input (SegmentedButtonSetting only shows valid options)
     * - If error occurs, exception logged but app doesn't crash
     *
     * **Thread Safety**: Safe to call from UI layer (main thread)
     *
     * **Usage in Composable**:
     * ```kotlin
     * SegmentedButtonSetting(
     *     label = "Speed Threshold",
     *     options = StopDetectionConfig.VALID_SPEED_THRESHOLDS,
     *     selectedOption = stopDetectionConfig.speedThresholdKmh,
     *     onOptionSelected = { newSpeed ->
     *         viewModel.updateStopDetectionConfig(
     *             stopDetectionConfig.copy(speedThresholdKmh = newSpeed)
     *         )
     *     }
     * )
     * ```
     *
     * @param config New stop detection configuration (must be valid)
     * @throws IllegalArgumentException if config contains invalid values (should not happen with proper UI)
     */
    fun updateStopDetectionConfig(config: StopDetectionConfig) {
        TODO("Implementation in SettingsViewModel.kt")
    }
}

/**
 * Contract Guarantees (Testing Checklist)
 *
 * ✅ stopDetectionConfig StateFlow emits defaults on first subscription
 * ✅ stopDetectionConfig StateFlow emits updated values after updateStopDetectionConfig()
 * ✅ updateStopDetectionConfig() persists to repository
 * ✅ updateStopDetectionConfig() throws IllegalArgumentException for invalid config
 * ✅ StateFlow survives configuration changes (rotation)
 * ✅ StateFlow shares same instance across multiple collectors
 * ✅ StateFlow collects on viewModelScope (background thread)
 * ✅ ViewModel lifecycle properly managed (cleared when not needed)
 */

/**
 * Implementation Pattern (Reference)
 *
 * ```kotlin
 * @HiltViewModel
 * class SettingsViewModel @Inject constructor(
 *     private val settingsRepository: SettingsRepository
 * ) : ViewModel() {
 *
 *     val stopDetectionConfig = settingsRepository.stopDetectionConfig
 *         .stateIn(
 *             scope = viewModelScope,
 *             started = SharingStarted.WhileSubscribed(5000),
 *             initialValue = StopDetectionConfig()
 *         )
 *
 *     fun updateStopDetectionConfig(config: StopDetectionConfig) {
 *         viewModelScope.launch {
 *             try {
 *                 settingsRepository.setStopDetectionConfig(config)
 *             } catch (e: IllegalArgumentException) {
 *                 Log.e("SettingsViewModel", "Invalid config: ${e.message}")
 *                 // UI should prevent this, but log for debugging
 *             }
 *         }
 *     }
 * }
 * ```
 */
