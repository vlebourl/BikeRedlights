package com.example.bikeredlights.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings screens.
 *
 * Manages settings state and provides methods for updating preferences.
 * State is reactive (StateFlow) for automatic UI updates.
 *
 * @param settingsRepository Repository for settings persistence
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
     * Update user's auto-pause threshold.
     *
     * @param thresholdMinutes Threshold in minutes (must be valid: 1, 2, 3, 5, 10, 15)
     */
    fun setAutoPauseThreshold(thresholdMinutes: Int) {
        viewModelScope.launch {
            val currentConfig = uiState.value.autoPauseConfig
            val newConfig = AutoPauseConfig(
                enabled = currentConfig.enabled,
                thresholdMinutes = thresholdMinutes
            )
            settingsRepository.setAutoPauseConfig(newConfig)
        }
    }
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
