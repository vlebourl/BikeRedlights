package com.example.bikeredlights.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.example.bikeredlights.domain.model.settings.StopDetectionConfig
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.domain.usecase.ClusterStopsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screens.
 *
 * Manages settings state and provides methods for updating preferences.
 * State is reactive (StateFlow) for automatic UI updates.
 *
 * @param settingsRepository Repository for settings persistence
 * @param clusterStopsUseCase Use case for manual clustering trigger (Feature 010 testing)
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val clusterStopsUseCase: ClusterStopsUseCase
) : ViewModel() {

    // Feature 010 - Clustering test status
    private val _clusteringStatus = MutableStateFlow<ClusteringStatus>(ClusteringStatus.Idle)
    val clusteringStatus: StateFlow<ClusteringStatus> = _clusteringStatus.asStateFlow()

    /**
     * Combined settings UI state.
     * Emits new state whenever any setting changes.
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.unitsSystem,
        settingsRepository.gpsAccuracy,
        settingsRepository.autoPauseConfig
    ) { units, accuracy, autoPause ->
        SettingsUiState(
            unitsSystem = units,
            gpsAccuracy = accuracy,
            autoPauseConfig = autoPause
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    /**
     * Update user's preferred units system.
     *
     * @param units New units system (METRIC or IMPERIAL)
     */
    fun setUnitsSystem(units: UnitsSystem) {
        viewModelScope.launch {
            settingsRepository.setUnitsSystem(units)
        }
    }

    /**
     * Update user's preferred GPS accuracy mode.
     *
     * @param accuracy New GPS accuracy (BATTERY_SAVER or HIGH_ACCURACY)
     */
    fun setGpsAccuracy(accuracy: GpsAccuracy) {
        viewModelScope.launch {
            settingsRepository.setGpsAccuracy(accuracy)
        }
    }

    /**
     * Update user's auto-pause enabled state.
     *
     * @param enabled Whether auto-pause is active
     */
    fun setAutoPauseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentConfig = uiState.value.autoPauseConfig
            val newConfig = currentConfig.copy(enabled = enabled)
            settingsRepository.setAutoPauseConfig(newConfig)
        }
    }

    /**
     * Update user's auto-pause threshold (Feature 007 - v0.6.1).
     *
     * Bug #10: Changed from minutes to seconds.
     * v0.6.1: Updated to 6 granular timing options per user request.
     *
     * @param thresholdSeconds Threshold in seconds (must be valid: 1, 2, 5, 10, 15, 30)
     * @throws IllegalArgumentException if thresholdSeconds is not in AutoPauseConfig.VALID_THRESHOLDS
     */
    fun setAutoPauseThreshold(thresholdSeconds: Int) {
        viewModelScope.launch {
            val currentConfig = uiState.value.autoPauseConfig
            val newConfig = AutoPauseConfig(
                enabled = currentConfig.enabled,
                thresholdSeconds = thresholdSeconds
            )
            settingsRepository.setAutoPauseConfig(newConfig)
        }
    }

    /**
     * Update user's auto-pause configuration atomically.
     *
     * Use this method when updating both enabled state and threshold simultaneously
     * to avoid race conditions.
     *
     * @param config New auto-pause configuration
     */
    fun setAutoPauseConfig(config: AutoPauseConfig) {
        viewModelScope.launch {
            settingsRepository.setAutoPauseConfig(config)
        }
    }

    // Feature 008 - Stop Detection Settings (v0.8.0)

    /**
     * Reactive stream of stop detection configuration.
     * Emits default values (3 km/h, 15s, 20m) on first subscription if not yet set.
     * Emits new values whenever setting changes.
     *
     * Consumed by StopDetectionSettingsScreen for displaying current values and
     * handling user selections.
     */
    val stopDetectionConfig: StateFlow<StopDetectionConfig> = settingsRepository.stopDetectionConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StopDetectionConfig()
        )

    /**
     * Update user's stop detection configuration.
     *
     * All 3 values (speed threshold, duration threshold, clustering radius) are
     * persisted atomically to DataStore in a single transaction.
     *
     * @param config New stop detection configuration
     * @throws IllegalArgumentException if any config value is invalid
     *         (should not occur with proper UI validation using SegmentedButtonSetting)
     */
    fun updateStopDetectionConfig(config: StopDetectionConfig) {
        viewModelScope.launch {
            settingsRepository.setStopDetectionConfig(config)
        }
    }

    // Feature 010 - Manual Clustering Trigger (Testing)

    /**
     * Manually trigger stop clustering (Feature 010 testing).
     *
     * Runs ClusterStopsUseCase with current clustering radius from settings.
     * Updates clusteringStatus StateFlow for UI feedback.
     *
     * This is a TEST FEATURE for validating clustering logic with existing stops.
     */
    fun runClustering() {
        viewModelScope.launch {
            try {
                _clusteringStatus.value = ClusteringStatus.Running

                // Fetch current radius from settings
                val radiusMeters = stopDetectionConfig.value.clusteringRadiusMeters

                // Run clustering
                clusterStopsUseCase.invoke(
                    epsilonMeters = radiusMeters.toFloat(),
                    minPts = 3
                )

                _clusteringStatus.value = ClusteringStatus.Success
            } catch (e: Exception) {
                _clusteringStatus.value = ClusteringStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Reset clustering status back to Idle.
     * Call after user dismisses success/error message.
     */
    fun resetClusteringStatus() {
        _clusteringStatus.value = ClusteringStatus.Idle
    }
}

/**
 * Status of manual clustering operation (Feature 010 testing).
 */
sealed class ClusteringStatus {
    object Idle : ClusteringStatus()
    object Running : ClusteringStatus()
    object Success : ClusteringStatus()
    data class Error(val message: String) : ClusteringStatus()
}

/**
 * UI state for Settings screens.
 *
 * Represents current values of all settings.
 * Default values match domain model defaults.
 *
 * @property unitsSystem User's preferred measurement system
 * @property gpsAccuracy User's preferred GPS update frequency
 * @property autoPauseConfig User's auto-pause configuration
 */
data class SettingsUiState(
    val unitsSystem: UnitsSystem = UnitsSystem.DEFAULT,
    val gpsAccuracy: GpsAccuracy = GpsAccuracy.DEFAULT,
    val autoPauseConfig: AutoPauseConfig = AutoPauseConfig.default()
)
