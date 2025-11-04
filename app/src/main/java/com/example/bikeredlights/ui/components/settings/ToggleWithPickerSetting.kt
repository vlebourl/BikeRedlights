package com.example.bikeredlights.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Material 3 toggle with picker setting component.
 *
 * Used for settings that can be enabled/disabled with a configurable numeric value
 * (e.g., Auto-Pause with threshold in minutes). When toggle is enabled, shows a
 * dropdown picker for selecting from predefined options.
 *
 * Provides large touch targets (48dp minimum) and clear visual feedback.
 *
 * @param label Primary text displayed for the setting (e.g., "Auto-Pause Rides")
 * @param enabled Whether the toggle is currently ON
 * @param selectedValue Currently selected value from options (e.g., 5 for 5 minutes)
 * @param options List of available values for the picker (e.g., [1, 2, 3, 5, 10, 15])
 * @param valueFormatter Function to format picker values for display (e.g., "5 minutes")
 * @param onEnabledChange Callback when user toggles the switch
 * @param onValueChange Callback when user selects a new value from picker
 * @param modifier Modifier for customizing layout and behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToggleWithPickerSetting(
    label: String,
    enabled: Boolean,
    selectedValue: Int,
    options: List<Int>,
    valueFormatter: (Int) -> String,
    onEnabledChange: (Boolean) -> Unit,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "$label setting. " +
                        "Currently ${if (enabled) "enabled with ${valueFormatter(selectedValue)}" else "disabled"}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Toggle row with label and switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics {
                    contentDescription = if (enabled) {
                        "$label enabled"
                    } else {
                        "$label disabled"
                    }
                }
            )
        }

        // Picker dropdown (only visible when enabled)
        if (enabled) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = valueFormatter(selectedValue),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Threshold") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .semantics {
                            contentDescription = "Select threshold. Currently ${valueFormatter(selectedValue)}"
                        }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(valueFormatter(option)) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            },
                            modifier = Modifier.semantics {
                                contentDescription = valueFormatter(option)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview for ToggleWithPickerSetting with toggle disabled (light theme).
 */
@Preview(showBackground = true)
@Composable
private fun ToggleWithPickerSettingPreviewDisabled() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ToggleWithPickerSetting(
                label = "Auto-Pause Rides",
                enabled = false,
                selectedValue = 5,
                options = listOf(1, 2, 3, 5, 10, 15),
                valueFormatter = { "$it minutes" },
                onEnabledChange = {},
                onValueChange = {}
            )
        }
    }
}

/**
 * Preview for ToggleWithPickerSetting with toggle enabled (light theme).
 */
@Preview(showBackground = true)
@Composable
private fun ToggleWithPickerSettingPreviewEnabled() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ToggleWithPickerSetting(
                label = "Auto-Pause Rides",
                enabled = true,
                selectedValue = 5,
                options = listOf(1, 2, 3, 5, 10, 15),
                valueFormatter = { "$it minutes" },
                onEnabledChange = {},
                onValueChange = {}
            )
        }
    }
}

/**
 * Preview for ToggleWithPickerSetting in dark theme.
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleWithPickerSettingPreviewDark() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ToggleWithPickerSetting(
                label = "Auto-Pause Rides",
                enabled = true,
                selectedValue = 10,
                options = listOf(1, 2, 3, 5, 10, 15),
                valueFormatter = { "$it minutes" },
                onEnabledChange = {},
                onValueChange = {}
            )
        }
    }
}
