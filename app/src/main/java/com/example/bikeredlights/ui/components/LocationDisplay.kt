package com.example.bikeredlights.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.LocationData

/**
 * Composable that displays current GPS coordinates and accuracy.
 *
 * This component implements User Story 2 (View Current GPS Position While Riding).
 * Shows latitude/longitude with 6 decimal places for precise location verification
 * and GPS accuracy indicator.
 *
 * Behavior:
 * - Displays "Acquiring GPS..." when locationData is null (no fix yet)
 * - Shows coordinates with 6 decimal precision (e.g., "Lat: 37.774929")
 * - Shows accuracy as "±X.X m" when available
 * - Includes accessibility content descriptions for TalkBack
 *
 * @param locationData Current GPS location data, null before first fix
 * @param modifier Optional modifier for layout customization
 */
@Composable
fun LocationDisplay(
    locationData: LocationData?,
    modifier: Modifier = Modifier
) {
    val accessibilityDescription = if (locationData != null) {
        val latText = "Latitude: %.6f".format(locationData.latitude)
        val lngText = "Longitude: %.6f".format(locationData.longitude)
        val accuracyText = if (locationData.accuracy > 0) {
            ", Accuracy: ${locationData.accuracy.toInt()} meters"
        } else ""
        "$latText, $lngText$accuracyText"
    } else {
        "GPS position: Acquiring signal"
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics {
                contentDescription = accessibilityDescription
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (locationData != null) {
            // Latitude with 6 decimal places
            Text(
                text = "Lat: %.6f".format(locationData.latitude),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Longitude with 6 decimal places
            Text(
                text = "Lng: %.6f".format(locationData.longitude),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Accuracy indicator (if available)
            if (locationData.accuracy > 0) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "±%.1f m".format(locationData.accuracy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // No GPS fix yet
            Text(
                text = "Acquiring GPS...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
