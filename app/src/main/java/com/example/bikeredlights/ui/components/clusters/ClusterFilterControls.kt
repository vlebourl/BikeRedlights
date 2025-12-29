package com.example.bikeredlights.ui.components.clusters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.ClusterSizePresets
import com.example.bikeredlights.domain.model.DateRangePresets
import com.example.bikeredlights.domain.model.StopClusterFilter

/**
 * Filter controls for stop cluster map (Feature 011 - User Story 3).
 *
 * Provides UI controls to filter displayed clusters by:
 * - Date range (All Time, Last 7 Days, Last 30 Days, Custom Range)
 * - Minimum cluster size (2+, 3+, 5+, 10+ stops)
 *
 * Visual Structure:
 * ```
 * ┌─────────────────────────────────────────────┐
 * │ Date Range ▼    Min Size ▼   [Filter: 2]  X│ ← Dropdowns + Indicator + Clear
 * └─────────────────────────────────────────────┘
 * ```
 *
 * Filter Indicator:
 * - Shows chip with active filter count when filters applied
 * - Hidden when no filters active (default state)
 * - Tappable to show filter summary
 *
 * Accessibility:
 * - Dropdowns have proper labels
 * - Clear button has content description
 * - Filter count announced for screen readers
 *
 * Performance:
 * - Recomposition optimized with stable parameters
 * - Dropdown state managed locally
 *
 * @param currentFilter Current active filter state
 * @param onFilterChange Callback when user changes filter (emits new StopClusterFilter)
 * @param onClearFilters Callback when user taps clear filters button
 * @param modifier Modifier for layout customization
 *
 * Example usage:
 * ```
 * ClusterFilterControls(
 *     currentFilter = uiState.activeFilter,
 *     onFilterChange = { newFilter -> viewModel.applyFilter(newFilter) },
 *     onClearFilters = { viewModel.clearFilters() }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClusterFilterControls(
    currentFilter: StopClusterFilter,
    onFilterChange: (StopClusterFilter) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine if filters are active (non-default)
    val hasActiveFilters = currentFilter.dateRange != null || currentFilter.minClusterSize > 2
    val activeFilterCount = listOfNotNull(
        if (currentFilter.dateRange != null) 1 else null,
        if (currentFilter.minClusterSize > 2) 1 else null
    ).sum()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Range Dropdown
                DateRangeDropdown(
                    currentDateRange = currentFilter.dateRange,
                    onDateRangeSelected = { dateRange ->
                        onFilterChange(currentFilter.copy(dateRange = dateRange))
                    },
                    modifier = Modifier.weight(1f)
                )

                // Min Cluster Size Dropdown
                MinClusterSizeDropdown(
                    currentMinSize = currentFilter.minClusterSize,
                    onMinSizeSelected = { minSize ->
                        onFilterChange(currentFilter.copy(minClusterSize = minSize))
                    },
                    modifier = Modifier.weight(1f)
                )

                // Filter Indicator Chip (only visible when filters active)
                if (hasActiveFilters) {
                    AssistChip(
                        onClick = { /* Could show filter summary dialog */ },
                        label = { Text("Filters: $activeFilterCount") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Active filters"
                            )
                        }
                    )

                    // Clear Filters Button
                    IconButton(onClick = onClearFilters) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear all filters"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dropdown for selecting date range filter.
 *
 * Options: All Time (default), Last 7 Days, Last 30 Days
 * Note: Custom Range deferred to future enhancement
 *
 * @param currentDateRange Currently selected date range (null = All Time)
 * @param onDateRangeSelected Callback when user selects new date range
 * @param modifier Modifier for dropdown layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDropdown(
    currentDateRange: com.example.bikeredlights.domain.model.DateRange?,
    onDateRangeSelected: (com.example.bikeredlights.domain.model.DateRange?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Determine current selection label
    // Note: DateRange comparison is by value equality (data class)
    val currentLabel = when {
        currentDateRange == null -> "All Time"
        currentDateRange.startMillis == DateRangePresets.last7Days().startMillis -> "Last 7 Days"
        currentDateRange.startMillis == DateRangePresets.last30Days().startMillis -> "Last 30 Days"
        else -> "Custom Range"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date Range") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Time") },
                onClick = {
                    onDateRangeSelected(null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Last 7 Days") },
                onClick = {
                    onDateRangeSelected(DateRangePresets.last7Days())
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Last 30 Days") },
                onClick = {
                    onDateRangeSelected(DateRangePresets.last30Days())
                    expanded = false
                }
            )
            // TODO: Add "Custom Range" option with date picker dialog (future enhancement)
        }
    }
}

/**
 * Dropdown for selecting minimum cluster size filter.
 *
 * Options: 2+ (default), 3+, 5+, 10+ stops
 *
 * @param currentMinSize Currently selected minimum cluster size
 * @param onMinSizeSelected Callback when user selects new min size
 * @param modifier Modifier for dropdown layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinClusterSizeDropdown(
    currentMinSize: Int,
    onMinSizeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Current selection label
    val currentLabel = "${currentMinSize}+ stops"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Min Size") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Cluster size options: 2+ (default), 3+, 5+, 6+ (medium), 10+, 11+ (large)
            listOf(
                ClusterSizePresets.SMALL,         // 2+ (default)
                3,                                 // 3+
                5,                                 // 5+
                ClusterSizePresets.MEDIUM_PLUS,   // 6+
                10,                                // 10+
                ClusterSizePresets.LARGE          // 11+ (large clusters only)
            ).forEach { minSize ->
                DropdownMenuItem(
                    text = { Text("${minSize}+ stops") },
                    onClick = {
                        onMinSizeSelected(minSize)
                        expanded = false
                    }
                )
            }
        }
    }
}
