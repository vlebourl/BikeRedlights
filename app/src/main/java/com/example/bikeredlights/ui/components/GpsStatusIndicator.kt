package com.example.bikeredlights.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.GpsStatus

/**
 * Composable that displays GPS signal status indicator.
 *
 * This component implements User Story 3 (Understand GPS Signal Status).
 * Shows visual feedback about GPS signal quality with color-coded indicators.
 *
 * Status Colors:
 * - Unavailable: Red (MaterialTheme.colorScheme.error)
 * - Acquiring: Yellow/Orange (MaterialTheme.colorScheme.tertiary)
 * - Active: Green (MaterialTheme.colorScheme.primary)
 *
 * Behavior:
 * - Shows icon and text indicating current GPS state
 * - Displays accuracy value when GPS is active (e.g., "GPS Active (±5.2m)")
 * - Includes accessibility content descriptions for TalkBack
 *
 * @param gpsStatus Current GPS signal status
 * @param modifier Optional modifier for layout customization
 */
@Composable
fun GpsStatusIndicator(
    gpsStatus: GpsStatus,
    modifier: Modifier = Modifier
) {
    val (icon, text, color, accessibilityDesc) = when (gpsStatus) {
        is GpsStatus.Unavailable -> {
            GpsIndicatorData(
                icon = Icons.Default.GpsOff,
                text = "GPS Unavailable",
                color = MaterialTheme.colorScheme.error,
                accessibilityDesc = "GPS signal unavailable"
            )
        }
        is GpsStatus.Acquiring -> {
            GpsIndicatorData(
                icon = Icons.Default.GpsNotFixed,
                text = "Acquiring GPS...",
                color = MaterialTheme.colorScheme.tertiary,
                accessibilityDesc = "Acquiring GPS signal"
            )
        }
        is GpsStatus.Active -> {
            val accuracyText = if (gpsStatus.accuracy > 0) {
                " (±%.1fm)".format(gpsStatus.accuracy)
            } else ""
            GpsIndicatorData(
                icon = Icons.Default.GpsFixed,
                text = "GPS Active$accuracyText",
                color = MaterialTheme.colorScheme.primary,
                accessibilityDesc = "GPS signal active${if (accuracyText.isNotEmpty()) " with accuracy ${gpsStatus.accuracy.toInt()} meters" else ""}"
            )
        }
    }

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics {
                contentDescription = accessibilityDesc
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Handled by row semantics
            tint = color
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}

/**
 * Data class holding GPS indicator display properties.
 */
private data class GpsIndicatorData(
    val icon: ImageVector,
    val text: String,
    val color: Color,
    val accessibilityDesc: String
)
