package com.example.bikeredlights.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.SpeedMeasurement
import kotlin.math.roundToInt

/**
 * Composable that displays current cycling speed in km/h.
 *
 * This is the primary display component for User Story 1 (View Current Speed While Riding).
 * Shows speed in large, readable typography optimized for glancing while cycling.
 *
 * Behavior:
 * - Displays "---" when speedMeasurement is null (no GPS data yet)
 * - Shows speed rounded to nearest integer (e.g., "25 km/h")
 * - Includes accessibility content description for TalkBack
 *
 * @param speedMeasurement Current speed data from GPS, null before first location fix
 * @param modifier Optional modifier for layout customization
 */
@Composable
fun SpeedDisplay(
    speedMeasurement: SpeedMeasurement?,
    modifier: Modifier = Modifier
) {
    val speedText = if (speedMeasurement != null) {
        "${speedMeasurement.speedKmh.roundToInt()} km/h"
    } else {
        "--- km/h"
    }

    val accessibilityDescription = if (speedMeasurement != null) {
        "Current speed: ${speedMeasurement.speedKmh.roundToInt()} kilometers per hour"
    } else {
        "Current speed: Not available"
    }

    Column(
        modifier = modifier.semantics {
            contentDescription = accessibilityDescription
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = speedText,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
