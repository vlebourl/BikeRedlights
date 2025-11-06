package com.example.bikeredlights.ui.components.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.history.SortPreference
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Dialog for selecting ride list sort order.
 *
 * **Features**:
 * - Radio button list of all sort options
 * - Current selection highlighted
 * - Confirm/Cancel buttons
 * - Material 3 AlertDialog
 *
 * **Design**:
 * - Uses SortPreference enum values
 * - Displays user-friendly labels
 * - Dismisses on selection
 *
 * **Accessibility**:
 * - Radio buttons have clear labels
 * - Full row clickable for easier tapping
 * - Semantic content descriptions
 *
 * @param currentSort Currently selected sort preference
 * @param onSortSelected Callback when user confirms new sort
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun SortDialog(
    currentSort: SortPreference,
    onSortSelected: (SortPreference) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSort) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sort Rides")
        },
        text = {
            Column {
                SortPreference.values().forEach { sortOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSort = sortOption }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSort == sortOption,
                            onClick = { selectedSort = sortOption }
                        )
                        Text(
                            text = sortOption.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSortSelected(selectedSort)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ===== Previews =====

@Preview(name = "Sort Dialog - Light Mode", showBackground = true)
@Preview(name = "Sort Dialog - Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SortDialogPreview() {
    BikeRedlightsTheme {
        SortDialog(
            currentSort = SortPreference.NEWEST_FIRST,
            onSortSelected = {},
            onDismiss = {}
        )
    }
}
