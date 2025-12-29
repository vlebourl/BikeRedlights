package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.domain.model.ClusterSummary
import com.example.bikeredlights.domain.model.StopClusterFilter
import com.example.bikeredlights.domain.usecase.CalculateClusterStatsUseCase
import com.example.bikeredlights.domain.usecase.GetClusteredStopsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for cluster map screen (Feature 011).
 *
 * Created by: Phase 3 - User Story 1 (View Clusters on Map)
 * Used by: StopsMapScreen composable
 *
 * Responsibilities:
 * - Load clustered stops from repository
 * - Calculate cluster statistics (center, count, duration, frequency)
 * - Apply filters (date range, minimum cluster size)
 * - Manage UI state (loading, error, empty)
 * - Handle cluster selection for bottom sheet (US2)
 *
 * Dependencies:
 * - GetClusteredStopsUseCase: Fetch stops from database
 * - CalculateClusterStatsUseCase: Aggregate stops into ClusterSummary
 *
 * State Flow:
 * 1. Initial: isLoading=true
 * 2. Fetch clustered stops via GetClusteredStopsUseCase
 * 3. Aggregate into ClusterSummary via CalculateClusterStatsUseCase
 * 4. Emit updated state: isLoading=false, clusters=data
 * 5. Handle errors: isLoading=false, errorMessage=error
 *
 * @property getClusteredStopsUseCase Use case to fetch clustered stops
 * @property calculateClusterStatsUseCase Use case to aggregate cluster statistics
 */
@HiltViewModel
class ClusterMapViewModel @Inject constructor(
    private val getClusteredStopsUseCase: GetClusteredStopsUseCase,
    private val calculateClusterStatsUseCase: CalculateClusterStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClusterMapUiState())
    val uiState: StateFlow<ClusterMapUiState> = _uiState.asStateFlow()

    init {
        loadClusters()
    }

    /**
     * Load clusters from repository with optional filter.
     *
     * Flow:
     * 1. Set isLoading=true
     * 2. Call GetClusteredStopsUseCase with current filter
     * 3. Map stops to ClusterSummary via CalculateClusterStatsUseCase
     * 4. Emit clusters to UI state
     * 5. Set isLoading=false
     * 6. Handle errors gracefully
     *
     * Reactivity: Uses Flow.collect for automatic updates when database changes
     *
     * @param filter Filter criteria (default = current active filter)
     */
    fun loadClusters(filter: StopClusterFilter = _uiState.value.activeFilter) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, activeFilter = filter) }

        viewModelScope.launch {
            getClusteredStopsUseCase(filter)
                .map { stops ->
                    // Aggregate stops into ClusterSummary
                    calculateClusterStatsUseCase(stops)
                }
                .catch { exception ->
                    // Handle errors gracefully
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = "Failed to load clusters: ${exception.message}"
                        )
                    }
                }
                .collect { clusterSummaries ->
                    // Emit clusters to UI
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            clusters = clusterSummaries,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * Apply filter to cluster display (User Story 3).
     *
     * Updates activeFilter and reloads clusters from repository.
     *
     * @param filter New filter criteria
     */
    fun applyFilter(filter: StopClusterFilter) {
        loadClusters(filter)
    }

    /**
     * Clear all filters and reload all clusters.
     *
     * Resets activeFilter to default (no date range, minSize=2).
     */
    fun clearFilters() {
        loadClusters(StopClusterFilter())
    }

    /**
     * Select a cluster for detailed view (User Story 2).
     *
     * Sets selectedCluster in state, triggering bottom sheet display.
     *
     * @param cluster ClusterSummary to select
     */
    fun selectCluster(cluster: ClusterSummary) {
        _uiState.update { it.copy(selectedCluster = cluster) }
    }

    /**
     * Deselect cluster and close bottom sheet (User Story 2).
     *
     * Clears selectedCluster in state, dismissing bottom sheet.
     */
    fun deselectCluster() {
        _uiState.update { it.copy(selectedCluster = null) }
    }

    /**
     * Retry loading clusters after error.
     *
     * Clears error message and reloads with current filter.
     */
    fun retry() {
        loadClusters()
    }
}
