package com.example.bikeredlights.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Control buttons for ride recording.
 *
 * **Features**:
 * - Pause button (when recording)
 * - Resume button (when paused)
 * - Stop button (always visible)
 * - Material 3 design with icons
 * - Responsive button sizing
 *
 * **State Management**:
 * - Stateless component
 * - Callbacks for all actions
 * - Parent controls visibility via isPaused flag
 *
 * @param isPaused True if ride is currently paused
 * @param onPauseClick Callback when pause button clicked
 * @param onResumeClick Callback when resume button clicked
 * @param onStopClick Callback when stop button clicked
 * @param modifier Modifier for this composable
 */
@Composable
fun RideControls(
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pause/Resume button
        if (isPaused) {
            // Show Resume button when paused
            FilledTonalButton(
                onClick = onResumeClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resume")
            }
        } else {
            // Show Pause button when recording
            FilledTonalButton(
                onClick = onPauseClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pause")
            }
        }

        // Stop button (always visible)
        Button(
            onClick = onStopClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stop")
        }
    }
}
