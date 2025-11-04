package com.example.bikeredlights.ui.components.ride

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bikeredlights.domain.model.Ride

/**
 * Dialog for saving or discarding a finished ride.
 *
 * Shown after user taps "Stop Ride" button.
 *
 * **Actions**:
 * - Save: Keeps ride in database, navigates to Review screen
 * - Discard: Deletes ride from database, returns to Live screen
 *
 * **UI Design**:
 * - Material 3 AlertDialog
 * - Shows ride name and elapsed duration
 * - Two buttons: Save (primary) and Discard (secondary)
 *
 * @param ride Finished ride to save or discard
 * @param onSave Callback when user taps "Save"
 * @param onDiscard Callback when user taps "Discard"
 * @param onDismissRequest Callback when user dismisses dialog (same as discard)
 */
@Composable
fun SaveRideDialog(
    ride: Ride,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismissRequest: () -> Unit = onDiscard,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Save Ride?")
        },
        text = {
            val durationSeconds = ride.elapsedDurationMillis / 1000
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            Text("Would you like to save \"${ride.name}\"?\n\nDuration: ${minutes}m ${seconds}s")
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        },
        modifier = modifier
    )
}
