package com.example.bikeredlights.ui.components.history

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Confirmation dialog for deleting a ride.
 *
 * **Features**:
 * - Clear warning message about permanent deletion
 * - Destructive action styling (red text)
 * - Cancel and Delete buttons
 * - Material 3 AlertDialog
 *
 * **Design**:
 * - Follows Material Design confirmation pattern
 * - Delete button uses error color to indicate destructive action
 * - Blocks accidental deletion with explicit confirmation
 *
 * **Accessibility**:
 * - Clear action labels
 * - Semantic content descriptions
 * - High contrast destructive button
 *
 * @param rideName Name of ride being deleted (shown in message)
 * @param onConfirm Callback when user confirms deletion
 * @param onDismiss Callback when dialog is dismissed/cancelled
 */
@Composable
fun DeleteConfirmationDialog(
    rideName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Ride?")
        },
        text = {
            Text(
                "Are you sure you want to delete \"$rideName\"? This action cannot be undone."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
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

@Preview(name = "Delete Confirmation Dialog - Light Mode", showBackground = true)
@Preview(name = "Delete Confirmation Dialog - Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeleteConfirmationDialogPreview() {
    BikeRedlightsTheme {
        DeleteConfirmationDialog(
            rideName = "Morning Commute",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
