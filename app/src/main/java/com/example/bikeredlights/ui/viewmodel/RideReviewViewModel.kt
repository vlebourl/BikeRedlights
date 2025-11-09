package com.example.bikeredlights.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.MapBounds
import com.example.bikeredlights.domain.model.MarkerData
import com.example.bikeredlights.domain.model.PolylineData
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.TrackPoint
import com.example.bikeredlights.domain.repository.RideRepository
import com.example.bikeredlights.domain.repository.TrackPointRepository
import com.example.bikeredlights.domain.usecase.CalculateMapBoundsUseCase
import com.example.bikeredlights.domain.usecase.FormatMapMarkersUseCase
import com.example.bikeredlights.domain.usecase.GetRoutePolylineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Ride Review screen.
 *
 * **Responsibilities**:
 * - Load ride from database by ID
 * - Expose ride as UI state
 * - Handle loading and error states
 * - Expose settings (units system) for UI
 *
 * **State Management**:
 * - Emits RideReviewUiState (Loading, Success, Error)
 * - Fetches ride once on screen load via loadRide()
 * - Uses StateFlow for reactive UI updates
 *
 * **Architecture**:
 * - MVVM pattern: ViewModel + StateFlow
 * - Hilt dependency injection
 * - Unidirectional data flow
 */
@HiltViewModel
class RideReviewViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val settingsRepository: SettingsRepository,
    private val trackPointRepository: TrackPointRepository,
    private val getRoutePolylineUseCase: GetRoutePolylineUseCase,
    private val calculateMapBoundsUseCase: CalculateMapBoundsUseCase,
    private val formatMapMarkersUseCase: FormatMapMarkersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RideReviewUiState>(RideReviewUiState.Loading)
    val uiState: StateFlow<RideReviewUiState> = _uiState.asStateFlow()

    private val _currentRideId = MutableStateFlow<Long?>(null)

    // Expose settings for UI (units conversion)
    val unitsSystem: StateFlow<com.example.bikeredlights.domain.model.settings.UnitsSystem> =
        settingsRepository.unitsSystem
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.bikeredlights.domain.model.settings.UnitsSystem.METRIC)

    /**
     * Track points for the ride, used to render the route on the map.
     */
    val trackPoints: StateFlow<List<TrackPoint>> = _currentRideId.map { rideId ->
        rideId?.let { trackPointRepository.getTrackPointsForRide(it) } ?: emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Polyline data for rendering the route on the map.
     */
    val polylineData: StateFlow<PolylineData?> = trackPoints.map { points ->
        getRoutePolylineUseCase(
            trackPoints = points,
            color = Color.Blue,
            width = 10f
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Map bounds to auto-zoom the map to fit the entire route.
     */
    val mapBounds: StateFlow<MapBounds?> = trackPoints.map { points ->
        calculateMapBoundsUseCase(points)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Start and end markers for the ride route.
     */
    val markers: StateFlow<List<MarkerData>> = trackPoints.map { points ->
        formatMapMarkersUseCase(points)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Load ride from database by ID.
     *
     * - Fetches ride from repository
     * - Updates UI state to Success with ride data
     * - Updates UI state to Error if ride not found
     * - Sets current ride ID for map data loading
     *
     * @param rideId ID of the ride to load
     */
    fun loadRide(rideId: Long) {
        viewModelScope.launch {
            _uiState.value = RideReviewUiState.Loading
            _currentRideId.value = rideId

            val ride = rideRepository.getRideById(rideId)
            if (ride != null) {
                _uiState.value = RideReviewUiState.Success(ride)
            } else {
                _uiState.value = RideReviewUiState.Error("Ride not found (ID: $rideId)")
            }
        }
    }
}

/**
 * UI state for Ride Review screen.
 */
sealed class RideReviewUiState {
    /**
     * Loading ride from database.
     */
    data object Loading : RideReviewUiState()

    /**
     * Ride loaded successfully.
     *
     * @property ride Ride data to display
     */
    data class Success(val ride: Ride) : RideReviewUiState()

    /**
     * Error loading ride (e.g., ride not found).
     *
     * @property message Error message to display
     */
    data class Error(val message: String) : RideReviewUiState()
}
