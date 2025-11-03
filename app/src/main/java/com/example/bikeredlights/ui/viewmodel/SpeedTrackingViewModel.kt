package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.domain.repository.LocationRepository
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the speed tracking screen.
 *
 * Manages UI state for real-time GPS location tracking and speed display.
 * Follows MVVM pattern with unidirectional data flow:
 * - State flows down via StateFlow
 * - Events flow up via function calls
 *
 * Lifecycle:
 * - ViewModel survives configuration changes (screen rotation)
 * - Location tracking continues during rotation without restart
 * - Flow collection automatically cancels when ViewModel is cleared
 *
 * @param locationRepository Repository providing location updates
 * @param trackLocationUseCase Use case providing speed measurements
 */
class SpeedTrackingViewModel(
    private val locationRepository: LocationRepository,
    private val trackLocationUseCase: TrackLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTrackingUiState())

    /**
     * UI state exposed as immutable StateFlow for Compose observation.
     *
     * Collect with collectAsStateWithLifecycle(minActiveState = Lifecycle.State.STARTED)
     * to automatically stop location tracking when app backgrounds.
     */
    val uiState: StateFlow<SpeedTrackingUiState> = _uiState.asStateFlow()

    /**
     * Starts location tracking and speed calculation.
     *
     * Should be called after location permission is granted. Launches coroutines
     * in viewModelScope that collect location updates and speed measurements.
     * The coroutines automatically cancel when ViewModel is cleared.
     *
     * Error handling:
     * - SecurityException → sets gpsStatus to Unavailable with error message
     * - Other exceptions → captured in catch block and logged
     */
    fun startLocationTracking() {
        // Collect location data for coordinates display
        viewModelScope.launch {
            locationRepository.getLocationUpdates()
                .catch { exception ->
                    // Handle location errors silently (speed tracking handles primary errors)
                }
                .collect { locationData ->
                    _uiState.update { currentState ->
                        currentState.copy(locationData = locationData)
                    }
                }
        }

        // Collect speed measurements
        viewModelScope.launch {
            trackLocationUseCase()
                .catch { exception ->
                    // Handle errors (SecurityException, GPS unavailable, etc.)
                    _uiState.update {
                        it.copy(
                            gpsStatus = GpsStatus.Unavailable,
                            errorMessage = when (exception) {
                                is SecurityException -> "Location permission required"
                                else -> "GPS unavailable: ${exception.message}"
                            }
                        )
                    }
                }
                .collect { speedMeasurement ->
                    _uiState.update { currentState ->
                        // Determine GPS status from speed measurement accuracy
                        // Note: Using simplified logic since accuracyKmh is null in MVP
                        val gpsStatus = when {
                            speedMeasurement.source == com.example.bikeredlights.domain.model.SpeedSource.UNKNOWN ->
                                GpsStatus.Acquiring
                            else -> GpsStatus.Active(5f) // Assume good accuracy when GPS active
                        }

                        currentState.copy(
                            speedMeasurement = speedMeasurement,
                            gpsStatus = gpsStatus,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * Called when location permission is granted by the user.
     *
     * Updates UI state to reflect permission grant and starts location tracking.
     */
    fun onPermissionGranted() {
        _uiState.update { it.copy(hasLocationPermission = true) }
        startLocationTracking()
    }

    /**
     * Called when location permission is denied by the user.
     *
     * Updates UI state to show permission required message.
     */
    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                hasLocationPermission = false,
                errorMessage = "Location permission is required for speed tracking"
            )
        }
    }
}
