package com.example.bikeredlights.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * Material 3 segmented button setting component for 2-option mutually exclusive choices.
 *
 * Uses Material 3 SegmentedButton API for binary settings like Metric/Imperial or
 * High Accuracy/Battery Saver. Provides large touch targets (48dp height) and clear
 * visual feedback for selected state.
 *
 * @param label Primary text displayed above the segmented button row (e.g., "Units")
 * @param option1Label Text for first option (e.g., "Metric")
 * @param option2Label Text for second option (e.g., "Imperial")
 * @param selectedOption Currently selected option (0 for option1, 1 for option2)
 * @param onOptionSelected Callback when user taps an option, receives selected index (0 or 1)
 * @param modifier Modifier for customizing layout and behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButtonSetting(
    label: String,
    option1Label: String,
    option2Label: String,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "$label setting. $option1Label or $option2Label. " +
                        "Currently selected: ${if (selectedOption == 0) option1Label else option2Label}"
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

        // Segmented button row (2 options)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp) // WCAG minimum touch target height
        ) {
            // Option 1
            SegmentedButton(
                selected = selectedOption == 0,
                onClick = { onOptionSelected(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option1Label,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Option 2
            SegmentedButton(
                selected = selectedOption == 1,
                onClick = { onOptionSelected(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option2Label,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Preview for SegmentedButtonSetting with first option selected (light theme).
 */
@Preview(showBackground = true)
@Composable
private fun SegmentedButtonSettingPreview() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SegmentedButtonSetting(
                label = "Units",
                option1Label = "Metric",
                option2Label = "Imperial",
                selectedOption = 0,
                onOptionSelected = {}
            )
        }
    }
}

/**
 * Preview for SegmentedButtonSetting with second option selected (light theme).
 */
@Preview(showBackground = true)
@Composable
private fun SegmentedButtonSettingPreviewOption2() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SegmentedButtonSetting(
                label = "GPS Accuracy",
                option1Label = "High Accuracy",
                option2Label = "Battery Saver",
                selectedOption = 1,
                onOptionSelected = {}
            )
        }
    }
}

/**
 * Preview for SegmentedButtonSetting in dark theme.
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SegmentedButtonSettingPreviewDark() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SegmentedButtonSetting(
                label = "Units",
                option1Label = "Metric",
                option2Label = "Imperial",
                selectedOption = 0,
                onOptionSelected = {}
            )
        }
    }
}
