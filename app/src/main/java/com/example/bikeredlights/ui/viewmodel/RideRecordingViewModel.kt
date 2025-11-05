package com.example.bikeredlights.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.RideRecordingState
import com.example.bikeredlights.domain.repository.RideRecordingStateRepository
import com.example.bikeredlights.domain.repository.RideRepository
import com.example.bikeredlights.domain.usecase.FinishRideResult
import com.example.bikeredlights.domain.usecase.FinishRideUseCase
import com.example.bikeredlights.service.RideRecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ride recording UI.
 *
 * **Responsibilities**:
 * - Manage ride recording UI state
 * - Start/stop ride recording service
 * - Handle save/discard dialog
 * - Expose recording state to UI
 *
 * **State Management**:
 * - Observes RideRecordingStateRepository for state changes
 * - Emits UI state to composables
 * - Handles user actions (start, stop, save, discard)
 *
 * **Architecture**:
 * - MVVM pattern: ViewModel + StateFlow
 * - Hilt dependency injection
 * - Unidirectional data flow (events down, state up)
 */
@HiltViewModel
class RideRecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rideRecordingStateRepository: RideRecordingStateRepository,
    private val rideRepository: RideRepository,
    private val finishRideUseCase: FinishRideUseCase,
    private val settingsRepository: com.example.bikeredlights.data.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RideRecordingUiState>(RideRecordingUiState.Idle)
    val uiState: StateFlow<RideRecordingUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    // Expose settings for UI (T076)
    val unitsSystem: StateFlow<com.example.bikeredlights.domain.model.settings.UnitsSystem> =
        settingsRepository.unitsSystem
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.bikeredlights.domain.model.settings.UnitsSystem.METRIC)

    init {
        // Observe recording state from repository
        viewModelScope.launch {
            rideRecordingStateRepository.getRecordingState().collect { recordingState ->
                updateUiStateFromRecordingState(recordingState)
            }
        }
    }

    /**
     * Start ride recording.
     *
     * - Starts RideRecordingService (foreground service)
     * - Service creates ride and begins GPS tracking
     * - UI state will update automatically via repository observer
     */
    fun startRide() {
        RideRecordingService.startRecording(context)
    }

    /**
     * Pause ride recording (manual pause).
     *
     * - Sends pause action to RideRecordingService
     * - Service updates state to ManuallyPaused
     * - UI state will update automatically via repository observer
     */
    fun pauseRide() {
        val intent = android.content.Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_PAUSE_RECORDING
        }
        context.startService(intent)
    }

    /**
     * Resume ride recording from manual pause.
     *
     * - Sends resume action to RideRecordingService
     * - Service updates state to Recording
     * - UI state will update automatically via repository observer
     */
    fun resumeRide() {
        val intent = android.content.Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_RESUME_RECORDING
        }
        context.startService(intent)
    }

    /**
     * Stop ride recording.
     *
     * - Stops GPS tracking
     * - Finishes ride (sets endTime)
     * - Shows save/discard dialog if ride is valid (>= 5 seconds)
     * - Auto-discards if ride is too short (< 5 seconds)
     */
    fun stopRide() {
        viewModelScope.launch {
            val state = rideRecordingStateRepository.getCurrentState()
            val rideId = state.currentRideId ?: return@launch

            // Stop service (stops GPS tracking)
            RideRecordingService.stopRecording(context)

            // Finish ride (sets endTime, validates duration)
            when (val result = finishRideUseCase(rideId)) {
                is FinishRideResult.Success -> {
                    // Show save/discard dialog
                    _uiState.value = RideRecordingUiState.ShowingSaveDialog(result.ride)
                }
                is FinishRideResult.TooShort -> {
                    // Auto-discard ride (too short)
                    discardRide(rideId)
                    _uiState.value = RideRecordingUiState.Idle
                }
                is FinishRideResult.RideNotFound -> {
                    // Error: ride not found
                    _uiState.value = RideRecordingUiState.Idle
                }
            }
        }
    }

    /**
     * Save ride (user tapped "Save" in dialog).
     *
     * - Keeps ride in database
     * - Clears recording state
     * - Returns to Idle state
     * - Emits navigation event to Review screen
     */
    fun saveRide() {
        val state = _uiState.value
        if (state is RideRecordingUiState.ShowingSaveDialog) {
            viewModelScope.launch {
                val rideId = state.ride.id

                // Clear recording state
                rideRecordingStateRepository.clearRecordingState()

                // Return to Idle state
                _uiState.value = RideRecordingUiState.Idle

                // Emit navigation event to Review screen
                _navigationEvents.send(NavigationEvent.NavigateToReview(rideId))
            }
        }
    }

    /**
     * Discard ride (user tapped "Discard" in dialog).
     *
     * - Deletes ride from database
     * - Clears recording state
     * - Returns to Idle state
     */
    fun discardRide() {
        val state = _uiState.value
        if (state is RideRecordingUiState.ShowingSaveDialog) {
            viewModelScope.launch {
                discardRide(state.ride.id)
            }
        }
    }

    /**
     * Discard ride by ID (internal helper).
     */
    private suspend fun discardRide(rideId: Long) {
        // Delete ride from database (CASCADE deletes track points)
        val ride = rideRepository.getRideById(rideId)
        if (ride != null) {
            rideRepository.deleteRide(ride)
        }

        // Clear recording state
        rideRecordingStateRepository.clearRecordingState()

        // Return to Idle state
        _uiState.value = RideRecordingUiState.Idle
    }

    /**
     * Update UI state based on recording state from repository.
     */
    private suspend fun updateUiStateFromRecordingState(recordingState: RideRecordingState) {
        when (recordingState) {
            is RideRecordingState.Idle -> {
                _uiState.value = RideRecordingUiState.Idle
            }
            is RideRecordingState.Recording -> {
                val ride = rideRepository.getRideById(recordingState.rideId)
                _uiState.value = if (ride != null) {
                    RideRecordingUiState.Recording(ride)
                } else {
                    RideRecordingUiState.Idle
                }
            }
            is RideRecordingState.ManuallyPaused -> {
                val ride = rideRepository.getRideById(recordingState.rideId)
                _uiState.value = if (ride != null) {
                    RideRecordingUiState.Paused(ride)
                } else {
                    RideRecordingUiState.Idle
                }
            }
            is RideRecordingState.AutoPaused -> {
                val ride = rideRepository.getRideById(recordingState.rideId)
                _uiState.value = if (ride != null) {
                    RideRecordingUiState.AutoPaused(ride)
                } else {
                    RideRecordingUiState.Idle
                }
            }
            is RideRecordingState.Stopped -> {
                // State will transition to ShowingSaveDialog via stopRide()
                // or Idle if auto-discarded
            }
        }
    }

    companion object {
        /**
         * Convert distance from meters to kilometers or miles.
         *
         * @param meters Distance in meters
         * @param unitsSystem Units system (Metric or Imperial)
         * @return Distance in km (Metric) or miles (Imperial)
         */
        fun convertDistance(meters: Double, unitsSystem: com.example.bikeredlights.domain.model.settings.UnitsSystem): Double {
            return when (unitsSystem) {
                com.example.bikeredlights.domain.model.settings.UnitsSystem.METRIC -> meters / 1000.0  // km
                com.example.bikeredlights.domain.model.settings.UnitsSystem.IMPERIAL -> meters / 1609.34  // miles
            }
        }

        /**
         * Convert speed from meters/second to km/h or mph.
         *
         * @param metersPerSec Speed in meters per second
         * @param unitsSystem Units system (Metric or Imperial)
         * @return Speed in km/h (Metric) or mph (Imperial)
         */
        fun convertSpeed(metersPerSec: Double, unitsSystem: com.example.bikeredlights.domain.model.settings.UnitsSystem): Double {
            return when (unitsSystem) {
                com.example.bikeredlights.domain.model.settings.UnitsSystem.METRIC -> metersPerSec * 3.6  // km/h
                com.example.bikeredlights.domain.model.settings.UnitsSystem.IMPERIAL -> metersPerSec * 2.23694  // mph
            }
        }

        /**
         * Get distance unit label.
         *
         * @param unitsSystem Units system (Metric or Imperial)
         * @return Unit label ("km" or "mi")
         */
        fun getDistanceUnit(unitsSystem: com.example.bikeredlights.domain.model.settings.UnitsSystem): String {
            return when (unitsSystem) {
                com.example.bikeredlights.domain.model.settings.UnitsSystem.METRIC -> "km"
                com.example.bikeredlights.domain.model.settings.UnitsSystem.IMPERIAL -> "mi"
            }
        }

        /**
         * Get speed unit label.
         *
         * @param unitsSystem Units system (Metric or Imperial)
         * @return Unit label ("km/h" or "mph")
         */
        fun getSpeedUnit(unitsSystem: com.example.bikeredlights.domain.model.settings.UnitsSystem): String {
            return when (unitsSystem) {
                com.example.bikeredlights.domain.model.settings.UnitsSystem.METRIC -> "km/h"
                com.example.bikeredlights.domain.model.settings.UnitsSystem.IMPERIAL -> "mph"
            }
        }
    }
}

/**
 * UI state for ride recording screen.
 */
sealed class RideRecordingUiState {
    /**
     * No active ride recording.
     */
    data object Idle : RideRecordingUiState()

    /**
     * Ride is actively recording.
     *
     * @property ride Current ride being recorded
     */
    data class Recording(val ride: Ride) : RideRecordingUiState()

    /**
     * Ride is manually paused.
     *
     * @property ride Current ride (paused)
     */
    data class Paused(val ride: Ride) : RideRecordingUiState()

    /**
     * Ride is auto-paused (low speed).
     *
     * @property ride Current ride (auto-paused)
     */
    data class AutoPaused(val ride: Ride) : RideRecordingUiState()

    /**
     * Showing save/discard dialog after stopping ride.
     *
     * @property ride Finished ride (endTime set)
     */
    data class ShowingSaveDialog(val ride: Ride) : RideRecordingUiState()
}

/**
 * Navigation events emitted by ViewModel.
 *
 * Used to trigger one-time navigation actions from the screen.
 * Consumed via Channel to prevent duplicate navigation on recomposition.
 */
sealed class NavigationEvent {
    /**
     * Navigate to Ride Review screen.
     *
     * @property rideId ID of the ride to review
     */
    data class NavigateToReview(val rideId: Long) : NavigationEvent()
}
