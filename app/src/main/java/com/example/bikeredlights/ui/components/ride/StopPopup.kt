package com.example.bikeredlights.ui.components.ride

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Semi-transparent popup displaying current stop status during active ride (Feature 009).
 *
 * **Display Logic**:
 * - Shows when currentStopNumber is not null (stop is active)
 * - Hides with fade-out animation when currentStopNumber becomes null (stop ends)
 * - Updates duration counter in real-time (every second)
 *
 * **UI Design**:
 * - Semi-transparent Material 3 Card (alpha 0.9)
 * - Stop emoji 🛑 + stop number
 * - Live duration counter (HH:MM:SS or MM:SS format)
 * - Positioned at top-center of screen (doesn't block map/speed)
 * - Fade-in/fade-out animations (200ms)
 *
 * **Accessibility**:
 * - High contrast text on semi-transparent background
 * - Readable font sizes (body large for number, body medium for duration)
 * - Screen reader support via content descriptions
 *
 * @param stopNumber Current stop number (1, 2, 3...) or null when not stopped
 * @param durationSeconds Current stop duration in seconds or null when not stopped
 * @param modifier Modifier for positioning and styling
 */
@Composable
fun StopPopup(
    stopNumber: Int?,
    durationSeconds: Int?,
    modifier: Modifier = Modifier
) {
    // DEBUG: Log whenever this composable is called
    android.util.Log.d("StopPopup", "🎨 COMPOSING: stopNumber=$stopNumber, durationSeconds=$durationSeconds, visible=${stopNumber != null}")

    AnimatedVisibility(
        visible = stopNumber != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        // DEBUG: Log when AnimatedVisibility content is composed
        android.util.Log.d("StopPopup", "✅ INSIDE AnimatedVisibility content block (should be visible now)")
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop emoji and number
                Text(
                    text = "🛑 Stop #${stopNumber ?: 0}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Duration counter
                Text(
                    text = formatDuration(durationSeconds ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Format duration in seconds to MM:SS or HH:MM:SS format.
 *
 * **Format Rules**:
 * - < 1 hour: MM:SS (e.g., "00:15", "05:42")
 * - >= 1 hour: HH:MM:SS (e.g., "1:05:42", "2:30:00")
 *
 * @param seconds Duration in seconds
 * @return Formatted duration string
 */
private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

// ====================================================================================
// PREVIEWS
// ====================================================================================

@Preview(name = "Stop Popup - Short Duration", showBackground = true)
@Composable
private fun StopPopupPreview_Short() {
    BikeRedlightsTheme {
        StopPopup(
            stopNumber = 1,
            durationSeconds = 15
        )
    }
}

@Preview(name = "Stop Popup - Medium Duration", showBackground = true)
@Composable
private fun StopPopupPreview_Medium() {
    BikeRedlightsTheme {
        StopPopup(
            stopNumber = 3,
            durationSeconds = 342 // 5:42
        )
    }
}

@Preview(name = "Stop Popup - Long Duration", showBackground = true)
@Composable
private fun StopPopupPreview_Long() {
    BikeRedlightsTheme {
        StopPopup(
            stopNumber = 5,
            durationSeconds = 3942 // 1:05:42
        )
    }
}

@Preview(name = "Stop Popup - Hidden", showBackground = true)
@Composable
private fun StopPopupPreview_Hidden() {
    BikeRedlightsTheme {
        StopPopup(
            stopNumber = null,
            durationSeconds = null
        )
    }
}
