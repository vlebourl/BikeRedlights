package com.example.bikeredlights.ui.components.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Card component displaying a single statistic with label and value.
 *
 * **Layout**:
 * - Label on top (secondary color)
 * - Value below (primary color, large text)
 *
 * **Design**:
 * - Material 3 elevated card
 * - Consistent padding and spacing
 * - Flexible width for grid layout
 *
 * **Accessibility**:
 * - Semantic structure for screen readers
 * - High contrast colors
 *
 * @param label Descriptive label for the statistic
 * @param value Formatted value with units
 * @param modifier Optional modifier for layout customization
 */
@Composable
fun DetailStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Value
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ===== Previews =====

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailStatCardPreview() {
    BikeRedlightsTheme {
        DetailStatCard(
            label = "Distance",
            value = "12.5 km"
        )
    }
}

@Preview(name = "Long Value", showBackground = true)
@Composable
private fun DetailStatCardLongValuePreview() {
    BikeRedlightsTheme {
        DetailStatCard(
            label = "Elapsed Duration",
            value = "02:34:15"
        )
    }
}
