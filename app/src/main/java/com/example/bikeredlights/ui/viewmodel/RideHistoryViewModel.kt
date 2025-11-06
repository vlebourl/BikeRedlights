package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.domain.model.display.RideListItem
import com.example.bikeredlights.domain.model.history.SortPreference
import com.example.bikeredlights.domain.usecase.GetAllRidesUseCase
import com.example.bikeredlights.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Ride History screen.
 *
 * Manages UI state for displaying list of saved rides with:
 * - Reactive ride list from database
 * - Sort preference persistence
 * - Unit system preference
 * - Loading/error states
 * - Empty state handling
 *
 * **State Management**:
 * - Collects rides, sort, and units from repositories
 * - Combines streams for consistent UI state
 * - Emits RideHistoryUiState sealed class
 *
 * **Architecture**:
 * - Uses GetAllRidesUseCase for business logic
 * - Observes SettingsRepository for user preferences
 * - Follows MVI pattern (Model-View-Intent)
 *
 * @property getAllRidesUseCase Use case for fetching rides
 * @property settingsRepository Repository for user preferences
 */
@HiltViewModel
class RideHistoryViewModel @Inject constructor(
    private val getAllRidesUseCase: GetAllRidesUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RideHistoryUiState>(RideHistoryUiState.Loading)
    val uiState: StateFlow<RideHistoryUiState> = _uiState.asStateFlow()

    private val _currentSort = MutableStateFlow(SortPreference.NEWEST_FIRST)
    val currentSort: StateFlow<SortPreference> = _currentSort.asStateFlow()

    init {
        observeRides()
    }

    /**
     * Observe rides with combined sort and unit preferences.
     *
     * Combines three reactive streams:
     * 1. Ride sort preference (from SettingsRepository)
     * 2. Units preference (from SettingsRepository)
     * 3. Rides (from GetAllRidesUseCase, depends on sort)
     *
     * Emits updated UI state whenever any stream changes.
     */
    private fun observeRides() {
        combine(
            settingsRepository.rideSortPreference,
            settingsRepository.unitsSystem
        ) { sortPreference, unitsSystem ->
            _currentSort.value = sortPreference
            Pair(sortPreference, unitsSystem)
        }
            .onEach { (sortPreference, unitsSystem) ->
                // Switch to appropriate sorted flow when preferences change
                getAllRidesUseCase(sortPreference, unitsSystem)
                    .catch { error ->
                        _uiState.value = RideHistoryUiState.Error(
                            message = error.message ?: "Failed to load rides"
                        )
                    }
                    .collect { rides ->
                        _uiState.value = if (rides.isEmpty()) {
                            RideHistoryUiState.Empty
                        } else {
                            RideHistoryUiState.Success(rides = rides)
                        }
                    }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Update sort preference.
     *
     * Persists to DataStore and triggers ride list requery.
     *
     * @param sortPreference New sort preference
     */
    fun updateSortPreference(sortPreference: SortPreference) {
        viewModelScope.launch {
            settingsRepository.setRideSortPreference(sortPreference)
        }
    }
}

/**
 * UI state for Ride History screen.
 *
 * Sealed class representing all possible UI states:
 * - Loading: Initial state while fetching data
 * - Empty: No rides saved yet
 * - Success: Rides loaded and displayed
 * - Error: Failed to load rides
 */
sealed class RideHistoryUiState {
    /**
     * Loading state - show progress indicator.
     */
    object Loading : RideHistoryUiState()

    /**
     * Empty state - no rides saved yet.
     * Show helpful message and CTA.
     */
    object Empty : RideHistoryUiState()

    /**
     * Success state - rides loaded.
     *
     * @property rides List of display-ready ride items
     */
    data class Success(
        val rides: List<RideListItem>
    ) : RideHistoryUiState()

    /**
     * Error state - failed to load rides.
     *
     * @property message Error message for user display
     */
    data class Error(
        val message: String
    ) : RideHistoryUiState()
}
