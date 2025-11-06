package com.example.bikeredlights.ui.components.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.display.RideListItem
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Card component displaying summary information for a single ride in the history list.
 *
 * **Layout**:
 * - Top row: Ride name + date
 * - Middle row: Duration + distance
 * - Bottom row: Average speed
 *
 * **Interaction**:
 * - Clickable card navigates to ride detail screen
 * - Material 3 elevation and ripple effect
 *
 * **Accessibility**:
 * - Ride name truncates with ellipsis if too long (max 2 lines)
 * - All statistics displayed with units
 * - Semantic content description for screen readers
 *
 * @param ride Display model containing formatted ride data
 * @param onClick Callback invoked when card is tapped
 * @param modifier Optional modifier for layout customization
 */
@Composable
fun RideListItemCard(
    ride: RideListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: Ride name and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = ride.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ride.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Statistics row: Duration, distance, average speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Duration
                StatisticItem(
                    label = "Duration",
                    value = ride.durationFormatted,
                    modifier = Modifier.weight(1f)
                )

                // Distance
                StatisticItem(
                    label = "Distance",
                    value = ride.distanceFormatted,
                    modifier = Modifier.weight(1f)
                )

                // Average speed
                StatisticItem(
                    label = "Avg Speed",
                    value = ride.avgSpeedFormatted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Helper composable for displaying a label + value statistic.
 *
 * @param label Descriptive label (e.g., "Duration", "Distance")
 * @param value Formatted value with units (e.g., "01:23:45", "12.5 km")
 * @param modifier Optional modifier
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ===== Previews =====

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RideListItemCardPreview() {
    BikeRedlightsTheme {
        RideListItemCard(
            ride = RideListItem(
                id = 1,
                name = "Morning Commute",
                dateFormatted = "Nov 6, 2025",
                durationFormatted = "00:42:15",
                distanceFormatted = "12.5 km",
                avgSpeedFormatted = "17.7 km/h",
                startTimeMillis = System.currentTimeMillis()
            ),
            onClick = {}
        )
    }
}

@Preview(name = "Long Ride Name", showBackground = true)
@Composable
private fun RideListItemCardLongNamePreview() {
    BikeRedlightsTheme {
        RideListItemCard(
            ride = RideListItem(
                id = 2,
                name = "Epic Century Ride to the Mountains and Back with Amazing Weather and Perfect Roads",
                dateFormatted = "Nov 5, 2025",
                durationFormatted = "05:12:30",
                distanceFormatted = "102.4 km",
                avgSpeedFormatted = "19.6 km/h",
                startTimeMillis = System.currentTimeMillis()
            ),
            onClick = {}
        )
    }
}
