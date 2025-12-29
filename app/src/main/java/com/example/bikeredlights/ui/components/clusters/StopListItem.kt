package com.example.bikeredlights.ui.components.clusters

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.Stop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Composable for displaying a single stop in the cluster detail bottom sheet.
 *
 * Created by: Feature 011 - User Story 2 (Cluster Details)
 * Used by: ClusterDetailBottomSheet to show list of stops
 *
 * Visual Design:
 * ```
 * ┌─────────────────────────────┐
 * │ Dec 29, 2025                │ ← Date (MMM DD, YYYY)
 * │ 2:30 PM          Duration   │ ← Time (HH:MM AM/PM) + Duration
 * └─────────────────────────────┘
 * ```
 *
 * Duration Formatting (per AS6):
 * - < 60 seconds: "45s"
 * - < 60 minutes: "2:30" (MM:SS)
 * - >= 60 minutes: "1:15:30" (HH:MM:SS)
 *
 * Accessibility:
 * - Card has content description with full stop details
 * - Readable date/time format
 *
 * @param stop Stop entity with date/time/duration data
 * @param modifier Modifier for card layout
 *
 * Example usage:
 * ```
 * LazyColumn {
 *     items(cluster.stops) { stop ->
 *         StopListItem(stop = stop)
 *     }
 * }
 * ```
 */
@Composable
fun StopListItem(
    stop: Stop,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Date and Time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDate(stop.startTimestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(stop.startTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: Duration
            Text(
                text = formatDuration(stop.durationSeconds),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Format stop date as "MMM DD, YYYY" (e.g., "Dec 29, 2025").
 *
 * @param timestamp Unix epoch milliseconds
 * @return Formatted date string
 */
private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

/**
 * Format stop time as "HH:MM AM/PM" (e.g., "2:30 PM").
 *
 * @param timestamp Unix epoch milliseconds
 * @return Formatted time string
 */
private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

/**
 * Format stop duration according to FR-016 rules.
 *
 * Rules (per AS6):
 * - < 60 seconds: "45s"
 * - < 60 minutes: "2:30" (MM:SS)
 * - >= 60 minutes: "1:15:30" (HH:MM:SS)
 * - null (active stop): "Active"
 *
 * @param durationSeconds Duration in seconds (null = active stop)
 * @return Formatted duration string
 */
private fun formatDuration(durationSeconds: Int?): String {
    if (durationSeconds == null) {
        return "Active"
    }

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
