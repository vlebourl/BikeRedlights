package com.example.bikeredlights.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.bikeredlights.domain.model.ClusterSummary
import com.example.bikeredlights.domain.model.StopClusterFilter

/**
 * UI state for cluster map screen (Feature 011).
 *
 * Created by: Phase 3 - User Story 1 (View Clusters on Map)
 * Used by: ClusterMapViewModel → StopsMapScreen
 *
 * State Properties:
 * - clusters: Aggregated cluster data for map markers
 * - activeFilter: Current filter settings (date range, minimum size)
 * - isLoading: True during initial load or filter changes
 * - errorMessage: User-facing error message (null if no error)
 * - selectedCluster: Currently selected cluster for bottom sheet (US2)
 *
 * State Transitions:
 * 1. Initial: isLoading=true, clusters=empty
 * 2. Loaded: isLoading=false, clusters=data
 * 3. Empty: isLoading=false, clusters=empty (show empty state)
 * 4. Error: isLoading=false, errorMessage=error (show error state)
 * 5. Filtering: isLoading=true, activeFilter=new (refetch data)
 * 6. Cluster Selected (US2): selectedCluster=ClusterSummary (open bottom sheet)
 *
 * @property clusters List of cluster summaries for map display (empty = no clusters)
 * @property activeFilter Current filter criteria (null = no filtering)
 * @property isLoading True during data fetch, false when complete
 * @property errorMessage User-facing error message (null = no error)
 * @property selectedCluster Selected cluster for detail popup (null = no selection)
 */
@Immutable
data class ClusterMapUiState(
    val clusters: List<ClusterSummary> = emptyList(),
    val activeFilter: StopClusterFilter = StopClusterFilter(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedCluster: ClusterSummary? = null
) {
    /**
     * Check if the map has clusters to display.
     *
     * @return True if clusters list is not empty
     */
    val hasClusters: Boolean
        get() = clusters.isNotEmpty()

    /**
     * Check if the map should show empty state.
     *
     * Empty state conditions:
     * - Not loading
     * - No error
     * - No clusters
     *
     * @return True if empty state should be displayed
     */
    val shouldShowEmptyState: Boolean
        get() = !isLoading && errorMessage == null && !hasClusters

    /**
     * Check if the map should show error state.
     *
     * Error state conditions:
     * - Not loading
     * - Error message present
     *
     * @return True if error state should be displayed
     */
    val shouldShowErrorState: Boolean
        get() = !isLoading && errorMessage != null

    /**
     * Check if bottom sheet should be shown (US2).
     *
     * Bottom sheet conditions:
     * - Cluster is selected
     * - Not loading
     * - No error
     *
     * @return True if bottom sheet should be displayed
     */
    val shouldShowBottomSheet: Boolean
        get() = selectedCluster != null && !isLoading && errorMessage == null
}
