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
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.domain.util.UnitConversions
import kotlin.math.roundToInt

/**
 * Composable that displays current cycling speed with units conversion support.
 *
 * This is the primary display component for User Story 1 (View Current Speed While Riding).
 * Shows speed in large, readable typography optimized for glancing while cycling.
 *
 * v0.2.0 Update: Supports both Metric (km/h) and Imperial (mph) units based on user preference.
 *
 * Behavior:
 * - Displays "---" when speedMeasurement is null (no GPS data yet)
 * - Shows speed rounded to nearest integer (e.g., "25 km/h" or "16 mph")
 * - Converts km/h to mph when Imperial units selected (using 0.621371 factor)
 * - Includes accessibility content description for TalkBack
 *
 * @param speedMeasurement Current speed data from GPS (always in km/h), null before first location fix
 * @param unitsSystem User's preferred measurement system (Metric/Imperial)
 * @param modifier Optional modifier for layout customization
 */
@Composable
fun SpeedDisplay(
    speedMeasurement: SpeedMeasurement?,
    unitsSystem: UnitsSystem,
    modifier: Modifier = Modifier
) {
    val speedText = if (speedMeasurement != null) {
        val displaySpeed = when (unitsSystem) {
            UnitsSystem.METRIC -> speedMeasurement.speedKmh
            UnitsSystem.IMPERIAL -> UnitConversions.toMph(speedMeasurement.speedKmh)
        }
        val unit = if (unitsSystem == UnitsSystem.METRIC) "km/h" else "mph"
        "${displaySpeed.roundToInt()} $unit"
    } else {
        val unit = if (unitsSystem == UnitsSystem.METRIC) "km/h" else "mph"
        "--- $unit"
    }

    val accessibilityDescription = if (speedMeasurement != null) {
        val displaySpeed = when (unitsSystem) {
            UnitsSystem.METRIC -> speedMeasurement.speedKmh
            UnitsSystem.IMPERIAL -> UnitConversions.toMph(speedMeasurement.speedKmh)
        }
        val unitText = when (unitsSystem) {
            UnitsSystem.METRIC -> "kilometers per hour"
            UnitsSystem.IMPERIAL -> "miles per hour"
        }
        "Current speed: ${displaySpeed.roundToInt()} $unitText"
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
