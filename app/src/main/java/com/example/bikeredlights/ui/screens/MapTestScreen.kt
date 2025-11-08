package com.example.bikeredlights.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bikeredlights.ui.components.map.BikeMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Temporary test screen to verify Google Maps SDK integration.
 *
 * This screen will be removed after Phase 3 (US3) testing is complete.
 * It's used to validate:
 * - API key is configured correctly
 * - Map tiles load without errors
 * - Touch gestures work (pan, zoom, rotate)
 * - Dark mode styling applies correctly
 *
 * **Testing Steps**:
 * 1. Build and install: ./gradlew assembleDebug && ./gradlew installDebug
 * 2. Navigate to this screen
 * 3. Verify map displays (no blank gray screen or API key errors)
 * 4. Check logcat for any errors: adb logcat | grep -E "Maps|ERROR|Exception"
 * 5. Test gestures: pan, pinch-zoom, rotate
 * 6. Toggle dark mode and verify map style changes
 * 7. Rotate device and verify map state persists
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTestScreen() {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(37.422, -122.084), // Google campus
            17f // City block level zoom
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Map SDK Test") })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            BikeMap(
                cameraPositionState = cameraPositionState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
