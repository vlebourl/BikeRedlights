package com.example.bikeredlights.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Material 3 settings card component with icon, title, subtitle, and click handler.
 *
 * Used in Settings home screen to group related settings by category.
 * Follows Material 3 Card design guidelines with appropriate elevation and touch feedback.
 *
 * @param title Primary text displayed at top of card (e.g., "Ride & Tracking")
 * @param subtitle Secondary text displayed below title (e.g., "Units, GPS, Auto-pause")
 * @param icon Leading icon to visually identify the category
 * @param onClick Callback when user taps the card
 * @param modifier Modifier for customizing layout and behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "$title settings card. $subtitle. Tap to configure."
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon (48dp size for visual weight)
            Icon(
                imageVector = icon,
                contentDescription = null, // Described in card semantics
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Title and subtitle column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Preview for SettingCard component in light theme.
 */
@Preview(showBackground = true)
@Composable
private fun SettingCardPreview() {
    BikeRedlightsTheme {
        SettingCard(
            title = "Ride & Tracking",
            subtitle = "Units, GPS, Auto-pause",
            icon = Icons.Default.Settings,
            onClick = {}
        )
    }
}

/**
 * Preview for SettingCard component in dark theme.
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingCardPreviewDark() {
    BikeRedlightsTheme {
        SettingCard(
            title = "Ride & Tracking",
            subtitle = "Units, GPS, Auto-pause",
            icon = Icons.Default.Settings,
            onClick = {}
        )
    }
}
