package com.example.bikeredlights.ui.screens.clusters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.ClusterSummary
import com.example.bikeredlights.ui.components.clusters.StopListItem

/**
 * Material 3 bottom sheet displaying detailed cluster information (Feature 011 - User Story 2).
 *
 * Shows comprehensive cluster analytics when user taps a cluster marker on the map:
 * - Summary statistics (total stops, average duration)
 * - Frequency analytics text (e.g., "You stopped here 15 times this month")
 * - Scrollable list of all individual stops in the cluster
 *
 * Visual Structure:
 * ```
 * ┌─────────────────────────────┐
 * │ [Close Button]              │
 * │                             │
 * │ ┌───────┐  ┌──────────────┐ │ ← Summary Cards
 * │ │15 Stops│  │ Avg: 2:30   │ │
 * │ └───────┘  └──────────────┘ │
 * │                             │
 * │ "You stopped here 15 times  │ ← Frequency Analytics
 * │  this month"                │
 * │                             │
 * │ ─── Individual Stops ───    │ ← Divider
 * │                             │
 * │ ┌─────────────────────────┐ │ ← Scrollable Stop List
 * │ │ Dec 29, 2025            │ │
 * │ │ 2:30 PM        2:30     │ │
 * │ └─────────────────────────┘ │
 * │ ┌─────────────────────────┐ │
 * │ │ Dec 28, 2025            │ │
 * │ │ 5:15 PM        1:45     │ │
 * │ └─────────────────────────┘ │
 * │         ...                 │
 * └─────────────────────────────┘
 * ```
 *
 * Accessibility:
 * - Close button has content description
 * - Summary cards clearly labeled
 * - Scrollable list has proper semantics
 *
 * Performance:
 * - LazyColumn for efficient rendering of large stop lists
 * - Recomposition optimized with stable parameters
 *
 * @param cluster ClusterSummary with all cluster data (stops, stats, analytics)
 * @param onDismiss Callback when bottom sheet is dismissed
 * @param sheetState ModalBottomSheet state for animations
 * @param modifier Modifier for layout customization
 *
 * Example usage:
 * ```
 * val sheetState = rememberModalBottomSheetState()
 *
 * if (selectedCluster != null) {
 *     ClusterDetailBottomSheet(
 *         cluster = selectedCluster,
 *         onDismiss = { viewModel.deselectCluster() },
 *         sheetState = sheetState
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClusterDetailBottomSheet(
    cluster: ClusterSummary,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close cluster details"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary Statistics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Stops Card
                SummaryCard(
                    title = "Total Stops",
                    value = "${cluster.stopCount}",
                    modifier = Modifier.weight(1f)
                )

                // Average Duration Card
                SummaryCard(
                    title = "Avg Duration",
                    value = formatAverageDuration(cluster.averageDuration),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency Analytics Text
            Text(
                text = cluster.frequencyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            Text(
                text = "Individual Stops",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Stop List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = cluster.stops,
                    key = { stop -> stop.id }
                ) { stop ->
                    StopListItem(stop = stop)
                }
            }
        }
    }
}

/**
 * Summary card displaying a single statistic (stop count or average duration).
 *
 * @param title Card title (e.g., "Total Stops")
 * @param value Statistic value to display
 * @param modifier Modifier for card layout
 */
@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Format average duration in seconds to human-readable string.
 *
 * Rules (same as StopListItem formatDuration):
 * - < 60 seconds: "45s"
 * - < 60 minutes: "2:30" (MM:SS)
 * - >= 60 minutes: "1:15:30" (HH:MM:SS)
 *
 * @param durationSeconds Duration in seconds
 * @return Formatted duration string
 */
private fun formatAverageDuration(durationSeconds: Long): String {
    return when {
        // Less than 60 seconds: show as "45s"
        durationSeconds < 60 -> "${durationSeconds}s"

        // Less than 60 minutes: show as "MM:SS"
        durationSeconds < 3600 -> {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            String.format("%d:%02d", minutes, seconds)
        }

        // 60 minutes or more: show as "HH:MM:SS"
        else -> {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        }
    }
}
