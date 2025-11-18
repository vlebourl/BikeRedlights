package com.example.bikeredlights.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Material 3 exposed dropdown menu setting component for N mutually exclusive options.
 *
 * Uses Material 3's ExposedDropdownMenuBox to provide a space-efficient dropdown
 * selection for 5+ options. This is the recommended Material Design 3 approach for
 * larger option sets, providing better UX than horizontal scrolling.
 *
 * Benefits over segmented buttons with scrolling:
 * - All options visible in dropdown (no hidden options requiring discovery)
 * - Clear affordance via dropdown icon
 * - More compact (saves vertical space)
 * - Better accessibility
 * - Material 3 standard component (consistent with Google apps)
 *
 * @param T Type of value (Int, Float, or other comparable types)
 * @param label Primary text displayed above the dropdown (e.g., "Speed Threshold")
 * @param options List of available values (e.g., [1f, 2f, 3f, 4f, 5f] for km/h)
 * @param selectedValue Currently selected value from options
 * @param onValueChange Callback when user selects an option, receives selected value
 * @param valueFormatter Function to format values for display (e.g., "3 km/h")
 * @param modifier Modifier for customizing layout and behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExposedDropdownMenuSetting(
    label: String,
    options: List<T>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    valueFormatter: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "$label setting. " +
                        "Options: ${options.joinToString(", ") { valueFormatter(it) }}. " +
                        "Currently selected: ${valueFormatter(selectedValue)}"
            }
    ) {
        // Label text
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Exposed dropdown menu
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = valueFormatter(selectedValue),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

/**
 * Preview for ExposedDropdownMenuSetting with 5 options (light theme).
 * Demonstrates speed threshold use case.
 */
@Preview(showBackground = true)
@Composable
private fun ExposedDropdownMenuSettingPreview5Options() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ExposedDropdownMenuSetting(
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
 * Preview for ExposedDropdownMenuSetting with 6 options (light theme).
 * Demonstrates duration threshold use case.
 */
@Preview(showBackground = true)
@Composable
private fun ExposedDropdownMenuSettingPreview6Options() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ExposedDropdownMenuSetting(
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
 * Preview for ExposedDropdownMenuSetting with 7 options (light theme).
 * Demonstrates clustering radius use case.
 */
@Preview(showBackground = true)
@Composable
private fun ExposedDropdownMenuSettingPreview7Options() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ExposedDropdownMenuSetting(
                label = "Clustering Radius",
                options = listOf(10, 15, 20, 25, 30, 40, 50),
                selectedValue = 20,
                onValueChange = {},
                valueFormatter = { "${it}m" }
            )
        }
    }
}

/**
 * Preview for ExposedDropdownMenuSetting in dark theme.
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExposedDropdownMenuSettingPreviewDark() {
    BikeRedlightsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ExposedDropdownMenuSetting(
                label = "Speed Threshold",
                options = listOf(1f, 2f, 3f, 4f, 5f),
                selectedValue = 3f,
                onValueChange = {},
                valueFormatter = { "${it.toInt()} km/h" }
            )
        }
    }
}
