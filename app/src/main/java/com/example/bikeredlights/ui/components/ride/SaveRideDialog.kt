package com.example.bikeredlights.ui.components.ride

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.bikeredlights.domain.model.MarkerData
import com.example.bikeredlights.domain.model.PolylineData
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.ui.components.map.BikeMap
import com.example.bikeredlights.ui.components.map.RoutePolyline
import com.example.bikeredlights.ui.components.map.StartEndMarkers
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Dialog for saving or discarding a finished ride with map preview.
 *
 * Shown after user taps "Stop Ride" button.
 *
 * **Actions**:
 * - Save: Keeps ride in database, navigates to Review screen
 * - Discard: Deletes ride from database, returns to Live screen
 *
 * **UI Design**:
 * - Custom Dialog with map preview at top
 * - Shows ride route with start/end markers
 * - Displays ride name and elapsed duration
 * - Two buttons: Save (primary) and Discard (secondary)
 *
 * @param ride Finished ride to save or discard
 * @param polylineData Route polyline for map display (nullable if no GPS data)
 * @param markers Start and end markers for the route
 * @param onSave Callback when user taps "Save"
 * @param onDiscard Callback when user taps "Discard"
 * @param onDismissRequest Callback when user dismisses dialog (same as discard)
 */
@Composable
fun SaveRideDialog(
    ride: Ride,
    polylineData: PolylineData?,
    markers: List<MarkerData>,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismissRequest: () -> Unit = onDiscard,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()

    // Auto-zoom to fit route if polyline data exists
    LaunchedEffect(polylineData) {
        polylineData?.let { data ->
            if (data.points.isNotEmpty()) {
                try {
                    // Calculate bounds from polyline points
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.builder().apply {
                        data.points.forEach { include(it) }
                    }.build()

                    // Animate camera to show entire route
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 100), // 100dp padding
                        durationMs = 800
                    )
                } catch (_: Exception) {
                    // Ignore bounds calculation errors
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Save Ride?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Map preview (if GPS data available)
                if (polylineData != null) {
                    BikeMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        cameraPositionState = cameraPositionState,
                        showMyLocationButton = false,
                        showZoomControls = false
                    ) {
                        RoutePolyline(polylineData = polylineData)
                        StartEndMarkers(markers = markers)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Ride details
                val durationSeconds = ride.movingDurationMillis / 1000
                val minutes = durationSeconds / 60
                val seconds = durationSeconds % 60

                Text(
                    text = "Would you like to save \"${ride.name}\"?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Duration: ${minutes}m ${seconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = onDiscard) {
                        Text("Discard")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = onSave) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
