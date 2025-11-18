package com.example.bikeredlights.ui.components.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Material 3 segmented button setting component for N mutually exclusive options.
 *
 * Extends SegmentedButtonSetting to support more than 2 options using horizontal
 * scrolling when needed. Used for settings with 3-7 discrete values (e.g., speed
 * thresholds with 5 options: 1, 2, 3, 4, 5 km/h).
 *
 * Each option is formatted using the provided valueFormatter function. The component
 * maintains accessibility by providing clear content descriptions.
 *
 * @param T Type of value (Int, Float, or other comparable types)
 * @param label Primary text displayed above the segmented button row (e.g., "Speed Threshold")
 * @param options List of available values (e.g., [1f, 2f, 3f, 4f, 5f] for km/h)
 * @param selectedValue Currently selected value from options
 * @param onValueChange Callback when user taps an option, receives selected value
 * @param valueFormatter Function to format values for display (e.g., "3 km/h")
 * @param modifier Modifier for customizing layout and behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiOptionSegmentedButtonSetting(
    label: String,
    options: List<T>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    valueFormatter: (T) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "$label setting. " +
                        "Options: ${options.joinToString(", ") { valueFormatter(it) }}. " +
                        "Currently selected: ${valueFormatter(selectedValue)}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label text
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Segmented button row with horizontal scroll if options don't fit
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp) // WCAG minimum touch target height
                .then(
                    // Enable horizontal scroll if more than 3 options
                    if (options.size > 3) {
                        Modifier.horizontalScroll(rememberScrollState())
                    } else {
                        Modifier
                    }
                )
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selectedValue,
                    onClick = { onValueChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = if (options.size <= 3) {
                        Modifier.weight(1f) // Equal width for ≤3 options
                    } else {
                        Modifier // Natural width for >3 options (scrollable)
                    }
                ) {
                    Text(
                        text = valueFormatter(option),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Preview for MultiOptionSegmentedButtonSetting with 5 options (light theme).
 * Demonstrates speed threshold use case.
 */
@Preview(showBackground = true)
@Composable
private fun MultiOptionSegmentedButtonSettingPreview5Options() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MultiOptionSegmentedButtonSetting(
                label = "Speed Threshold",
                options = listOf(1f, 2f, 3f, 4f, 5f),
                selectedValue = 3f,
                onValueChange = {},
                valueFormatter = { "${it.toInt()} km/h" }
            )
        }
    }
}

/**
 * Preview for MultiOptionSegmentedButtonSetting with 6 options (light theme).
 * Demonstrates duration threshold use case.
 */
@Preview(showBackground = true)
@Composable
private fun MultiOptionSegmentedButtonSettingPreview6Options() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MultiOptionSegmentedButtonSetting(
                label = "Duration Threshold",
                options = listOf(5, 10, 15, 20, 25, 30),
                selectedValue = 15,
                onValueChange = {},
                valueFormatter = { "${it}s" }
            )
        }
    }
}

/**
 * Preview for MultiOptionSegmentedButtonSetting with 3 options (light theme).
 * Demonstrates small option count (no scrolling needed).
 */
@Preview(showBackground = true)
@Composable
private fun MultiOptionSegmentedButtonSettingPreview3Options() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MultiOptionSegmentedButtonSetting(
                label = "Sample Setting",
                options = listOf(10, 20, 30),
                selectedValue = 20,
                onValueChange = {},
                valueFormatter = { "${it}m" }
            )
        }
    }
}

/**
 * Preview for MultiOptionSegmentedButtonSetting in dark theme.
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MultiOptionSegmentedButtonSettingPreviewDark() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MultiOptionSegmentedButtonSetting(
                label = "Speed Threshold",
                options = listOf(1f, 2f, 3f, 4f, 5f),
                selectedValue = 3f,
                onValueChange = {},
                valueFormatter = { "${it.toInt()} km/h" }
            )
        }
    }
}
