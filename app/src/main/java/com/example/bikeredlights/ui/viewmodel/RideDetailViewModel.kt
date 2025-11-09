package com.example.bikeredlights.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.MapBounds
import com.example.bikeredlights.domain.model.MarkerData
import com.example.bikeredlights.domain.model.PolylineData
import com.example.bikeredlights.domain.model.TrackPoint
import com.example.bikeredlights.domain.model.display.RideDetailData
import com.example.bikeredlights.domain.repository.TrackPointRepository
import com.example.bikeredlights.domain.usecase.CalculateMapBoundsUseCase
import com.example.bikeredlights.domain.usecase.FormatMapMarkersUseCase
import com.example.bikeredlights.domain.usecase.GetRideByIdUseCase
import com.example.bikeredlights.domain.usecase.GetRoutePolylineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for Ride Detail screen.
 *
 * Manages UI state for displaying detailed ride information with:
 * - Reactive ride data from database
 * - Unit system preference
 * - Loading/error states
 * - Not found handling
 *
 * **State Management**:
 * - Collects ride by ID and units from repositories
 * - Combines streams for consistent UI state
 * - Emits RideDetailUiState sealed class
 *
 * **Architecture**:
 * - Uses GetRideByIdUseCase for business logic
 * - Observes SettingsRepository for user preferences
 * - Follows MVI pattern (Model-View-Intent)
 * - Receives rideId from navigation via SavedStateHandle
 *
 * @property getRideByIdUseCase Use case for fetching ride
 * @property settingsRepository Repository for user preferences
 * @property savedStateHandle Navigation arguments container
 */
@HiltViewModel
class RideDetailViewModel @Inject constructor(
    private val getRideByIdUseCase: GetRideByIdUseCase,
    private val settingsRepository: SettingsRepository,
    private val trackPointRepository: TrackPointRepository,
    private val getRoutePolylineUseCase: GetRoutePolylineUseCase,
    private val calculateMapBoundsUseCase: CalculateMapBoundsUseCase,
    private val formatMapMarkersUseCase: FormatMapMarkersUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val rideId: Long = checkNotNull(savedStateHandle["rideId"]) {
        "RideDetailViewModel requires rideId argument"
    }

    private val _uiState = MutableStateFlow<RideDetailUiState>(RideDetailUiState.Loading)
    val uiState: StateFlow<RideDetailUiState> = _uiState.asStateFlow()

    /**
     * Track points for the ride, used to render the route on the map.
     */
    val trackPoints: StateFlow<List<TrackPoint>> = flow {
        val points = trackPointRepository.getTrackPointsForRide(rideId)
        emit(points)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Polyline data for rendering the route on the map.
     * Derived from trackPoints with simplification applied.
     */
    val polylineData: StateFlow<PolylineData?> = trackPoints.map { points ->
        getRoutePolylineUseCase(
            trackPoints = points,
            color = Color.Blue, // Blue for completed rides
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

    init {
        observeRideDetail()
    }

    /**
     * Observe ride detail with unit preferences.
     *
     * Combines two reactive streams:
     * 1. Ride data (from GetRideByIdUseCase)
     * 2. Units preference (from SettingsRepository)
     *
     * Emits updated UI state whenever any stream changes.
     */
    private fun observeRideDetail() {
        settingsRepository.unitsSystem
            .onEach { unitsSystem ->
                getRideByIdUseCase(rideId, unitsSystem)
                    .catch { error ->
                        _uiState.value = RideDetailUiState.Error(
                            message = error.message ?: "Failed to load ride"
                        )
                    }
                    .collect { rideDetail ->
                        _uiState.value = if (rideDetail == null) {
                            RideDetailUiState.NotFound
                        } else {
                            RideDetailUiState.Success(rideDetail = rideDetail)
                        }
                    }
            }
            .launchIn(viewModelScope)
    }
}

/**
 * UI state for Ride Detail screen.
 *
 * Sealed class representing all possible UI states:
 * - Loading: Initial state while fetching data
 * - NotFound: Ride ID doesn't exist
 * - Success: Ride loaded and displayed
 * - Error: Failed to load ride
 */
sealed class RideDetailUiState {
    /**
     * Loading state - show progress indicator.
     */
    object Loading : RideDetailUiState()

    /**
     * Not found state - ride ID doesn't exist.
     * Show helpful message with back button.
     */
    object NotFound : RideDetailUiState()

    /**
     * Success state - ride loaded.
     *
     * @property rideDetail Display-ready ride detail data
     */
    data class Success(
        val rideDetail: RideDetailData
    ) : RideDetailUiState()

    /**
     * Error state - failed to load ride.
     *
     * @property message Error message for user display
     */
    data class Error(
        val message: String
    ) : RideDetailUiState()
}
