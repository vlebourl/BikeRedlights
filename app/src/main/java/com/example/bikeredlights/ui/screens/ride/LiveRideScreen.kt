package com.example.bikeredlights.ui.screens.ride

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bikeredlights.ui.components.map.BikeMap
import com.example.bikeredlights.ui.components.map.LocationMarker
import com.example.bikeredlights.ui.components.map.RoutePolyline
import com.example.bikeredlights.ui.components.ride.KeepScreenOn
import com.example.bikeredlights.ui.components.ride.RideControls
import com.example.bikeredlights.ui.components.ride.RideStatistics
import com.example.bikeredlights.ui.components.ride.SaveRideDialog
import com.example.bikeredlights.ui.viewmodel.NavigationEvent
import com.example.bikeredlights.ui.viewmodel.RideRecordingUiState
import com.example.bikeredlights.ui.viewmodel.RideRecordingViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Live Ride Screen for starting and stopping ride recording.
 *
 * **Features**:
 * - Start Ride button (when idle)
 * - Stop Ride button (when recording)
 * - Show elapsed time (when recording)
 * - Show distance traveled (when recording)
 * - Save/Discard dialog (when stopped)
 *
 * **UI States**:
 * - Idle: Show "Start Ride" button
 * - Recording: Show elapsed time, distance, "Stop Ride" button
 * - Paused: Show "Paused" indicator, "Resume" and "Stop" buttons
 * - ShowingSaveDialog: Show save/discard dialog
 *
 * @param viewModel RideRecordingViewModel injected by Hilt
 * @param modifier Modifier for this composable
 */
@Composable
fun LiveRideScreen(
    viewModel: RideRecordingViewModel = hiltViewModel(),
    onNavigateToReview: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unitsSystem by viewModel.unitsSystem.collectAsStateWithLifecycle()
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()

    // Real-time pause counter (Feature 007 - v0.6.1)
    val pausedDuration by viewModel.pausedDuration.collectAsStateWithLifecycle()

    // Map bearing for directional orientation (Feature 007 - v0.6.1)
    val currentBearing by viewModel.currentBearing.collectAsStateWithLifecycle()

    // Map state (Feature 006)
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val polylineData by viewModel.polylineData.collectAsStateWithLifecycle()

    // Track device's current GPS location (even when not recording)
    var currentDeviceLocation by remember { mutableStateOf<com.google.android.gms.maps.model.LatLng?>(null) }

    // Camera position state for map (zoom level 17f = city block level, 50-200m radius)
    // Use userLocation (from recording) or currentDeviceLocation (from GPS when idle)
    val locationForCamera = userLocation ?: currentDeviceLocation
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            locationForCamera ?: com.google.android.gms.maps.model.LatLng(46.1942, 6.2347),
            17f
        )
    }

    // Camera following: Animate to location when it changes (Feature 006: FR-002)
    // Tracks both recording location (userLocation) and idle device location (currentDeviceLocation)
    LaunchedEffect(locationForCamera) {
        locationForCamera?.let { location ->
            cameraPositionState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(location, 17f),
                durationMs = 500 // Smooth 500ms animation (meets FR-002: <500ms requirement)
            )
        }
    }

    // Handle navigation events (one-time events via Channel)
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToReview -> {
                    onNavigateToReview(event.rideId)
                }
            }
        }
    }

    // GPS pre-warming: Request single location when screen is shown in Idle state
    // This "wakes up" the GPS so it's ready faster when user taps "Start Ride"
    // Only runs if location permissions are granted
    val context = LocalContext.current
    LaunchedEffect(uiState) {
        if (uiState is RideRecordingUiState.Idle && hasLocationPermissions(context)) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val cancellationTokenSource = CancellationTokenSource()

                // Request single high-accuracy location to warm up GPS
                // This is a one-time request, not continuous tracking
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            // Update map camera to device's current location
                            currentDeviceLocation = com.google.android.gms.maps.model.LatLng(
                                location.latitude,
                                location.longitude
                            )
                            android.util.Log.d("LiveRideScreen",
                                "GPS pre-warmed successfully: lat=${location.latitude}, lon=${location.longitude}")
                        }
                    }.addOnFailureListener { exception ->
                        android.util.Log.w("LiveRideScreen",
                            "GPS pre-warming failed (this is OK): ${exception.message}")
                    }
                }
            } catch (e: Exception) {
                // Silently fail - pre-warming is optional enhancement
                android.util.Log.w("LiveRideScreen", "GPS pre-warming exception: ${e.message}")
            }
        }
    }

    // Keep screen on during recording (US6)
    // Only applies when recording is active (not idle, not showing save dialog)
    val isRecording = uiState is RideRecordingUiState.WaitingForGps ||
                      uiState is RideRecordingUiState.Recording ||
                      uiState is RideRecordingUiState.Paused ||
                      uiState is RideRecordingUiState.AutoPaused
    if (isRecording) {
        KeepScreenOn()
    }

    // Show save dialog if needed
    if (uiState is RideRecordingUiState.ShowingSaveDialog) {
        val ride = (uiState as RideRecordingUiState.ShowingSaveDialog).ride

        // Calculate markers from polyline data for map preview
        val markers = remember(polylineData) {
            polylineData?.let { data ->
                if (data.points.isNotEmpty()) {
                    listOf(
                        com.example.bikeredlights.domain.model.MarkerData(
                            position = data.points.first(),
                            type = com.example.bikeredlights.domain.model.MarkerType.START,
                            title = "Start",
                            visible = true
                        ),
                        com.example.bikeredlights.domain.model.MarkerData(
                            position = data.points.last(),
                            type = com.example.bikeredlights.domain.model.MarkerType.END,
                            title = "End",
                            visible = true
                        )
                    )
                } else {
                    emptyList()
                }
            } ?: emptyList()
        }

        SaveRideDialog(
            ride = ride,
            polylineData = polylineData,
            markers = markers,
            onSave = { viewModel.saveRide() },
            onDiscard = { viewModel.discardRide() }
        )
    }

    // Main content with GPS status indicator
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // GPS Status Indicator (always visible at top-right to avoid overlap)
        GpsStatusIndicator(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        )

        // Main content with split-screen layout (Feature 006)
        // Map on top, stats/controls on bottom - Material 3 design
        when (uiState) {
            is RideRecordingUiState.Idle -> {
                // Show map even in Idle state with "Ready to ride?" message
                // Show device's current location (not recording track points)
                SplitScreenMapContent(
                    cameraPositionState = cameraPositionState,
                    userLocation = currentDeviceLocation, // Show device location when idle
                    polylineData = null // No route when idle
                ) {
                    IdleContent(
                        onStartRide = { viewModel.startRide() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is RideRecordingUiState.WaitingForGps -> {
                WaitingForGpsContent(
                    modifier = Modifier.fillMaxSize()
                )
            }
            is RideRecordingUiState.Recording -> {
                val ride = (uiState as RideRecordingUiState.Recording).ride
                SplitScreenMapContent(
                    cameraPositionState = cameraPositionState,
                    userLocation = userLocation,
                    polylineData = polylineData
                ) {
                    RecordingContent(
                        ride = ride,
                        currentSpeed = currentSpeed,
                        pausedDuration = pausedDuration,
                        unitsSystem = unitsSystem,
                        onPauseRide = { viewModel.pauseRide() },
                        onStopRide = { viewModel.stopRide() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is RideRecordingUiState.Paused -> {
                val ride = (uiState as RideRecordingUiState.Paused).ride
                SplitScreenMapContent(
                    cameraPositionState = cameraPositionState,
                    userLocation = userLocation,
                    polylineData = polylineData
                ) {
                    PausedContent(
                        ride = ride,
                        currentSpeed = currentSpeed,
                        pausedDuration = pausedDuration,
                        unitsSystem = unitsSystem,
                        onResumeRide = { viewModel.resumeRide() },
                        onStopRide = { viewModel.stopRide() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is RideRecordingUiState.AutoPaused -> {
                val ride = (uiState as RideRecordingUiState.AutoPaused).ride
                SplitScreenMapContent(
                    cameraPositionState = cameraPositionState,
                    userLocation = userLocation,
                    polylineData = polylineData
                ) {
                    AutoPausedContent(
                        ride = ride,
                        currentSpeed = currentSpeed,
                        pausedDuration = pausedDuration,
                        unitsSystem = unitsSystem,
                        onResumeRide = { viewModel.resumeRide() },
                        onStopRide = { viewModel.stopRide() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is RideRecordingUiState.ShowingSaveDialog -> {
                // Dialog is shown above, show split-screen underneath
                val ride = (uiState as RideRecordingUiState.ShowingSaveDialog).ride
                SplitScreenMapContent(
                    cameraPositionState = cameraPositionState,
                    userLocation = userLocation,
                    polylineData = polylineData
                ) {
                    RecordingContent(
                        ride = ride,
                        currentSpeed = currentSpeed,
                        pausedDuration = pausedDuration,
                        unitsSystem = unitsSystem,
                        onPauseRide = { }, // No action while dialog is shown
                        onStopRide = { }, // No action while dialog is shown
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Content shown when idle (no active ride).
 *
 * **Permission Handling**:
 * - Checks for location permissions before starting ride
 * - Requests permissions if not granted
 * - Shows rationale dialog if permissions are denied
 * - Handles "Don't ask again" scenario with settings prompt
 */
@Composable
private fun IdleContent(
    onStartRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Permission state
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var permissionsGranted by remember {
        mutableStateOf(hasLocationPermissions(context))
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // At least one location permission granted, start ride
            permissionsGranted = true
            onStartRide()
        } else {
            // Permissions denied, show dialog
            showPermissionDeniedDialog = true
        }
    }

    // Permission denied dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Location Permission Required") },
            text = {
                Text(
                    "BikeRedlights needs location permission to track your ride. " +
                    "Please grant location permission in Settings to use ride recording."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Main idle UI
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ready to ride?",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(
            onClick = {
                if (hasLocationPermissions(context)) {
                    // Permissions already granted, start ride immediately
                    onStartRide()
                } else {
                    // Request permissions
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Start Ride")
        }
    }
}

/**
 * Check if location permissions are granted.
 *
 * @param context Android context
 * @return true if at least one location permission is granted
 */
private fun hasLocationPermissions(context: Context): Boolean {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fineLocationGranted || coarseLocationGranted
}

/**
 * Split-screen layout with map on top and content on bottom.
 * Material 3 design with proper spacing and elevation.
 *
 * @param cameraPositionState Camera state for map
 * @param userLocation Current GPS location for marker
 * @param polylineData Route polyline data
 * @param content Bottom half content (stats and controls)
 */
@Composable
private fun SplitScreenMapContent(
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    userLocation: com.google.android.gms.maps.model.LatLng?,
    polylineData: com.example.bikeredlights.domain.model.PolylineData?,
    content: @Composable () -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map section (flexible, shares space equally with stats)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BikeMap(
                    cameraPositionState = cameraPositionState,
                    currentBearing = currentBearing, // Directional map orientation (Feature 007)
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Current location marker with directional arrow (Feature 007)
                    LocationMarker(
                        location = userLocation,
                        bearing = currentBearing // Rotates marker to show heading direction
                    )

                    // Route polyline (red, growing in real-time)
                    RoutePolyline(polylineData = polylineData)
                }

                // Center button (Material 3 FAB) - positioned top right to avoid zoom controls
                FloatingActionButton(
                    onClick = {
                        // Recenter camera on current location
                        userLocation?.let { location ->
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(location, 17f),
                                    durationMs = 300
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Center on current location"
                    )
                }
            }
        }

        // Stats and controls section (flexible, shares space equally with map)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            content()
        }
    }
}

/**
 * Content shown when recording is active.
 */
@Composable
private fun RecordingContent(
    ride: com.example.bikeredlights.domain.model.Ride,
    currentSpeed: Double,
    pausedDuration: java.time.Duration,
    unitsSystem: com.example.bikeredlights.domain.model.settings.UnitsSystem,
    onPauseRide: () -> Unit,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status indicator - compact icon instead of text to save space
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        // Ride statistics (duration, distance, speeds) - expands to fill available space
        RideStatistics(
            ride = ride,
            currentSpeed = currentSpeed, // Real-time GPS speed (Feature 005)
            pausedDuration = pausedDuration, // Real-time pause counter (Feature 007)
            unitsSystem = unitsSystem,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Control buttons section (fixed height at bottom, just enough for buttons)
        RideControls(
            isPaused = false,
            onPauseClick = onPauseRide,
            onResumeClick = { }, // Not used when not paused
            onStopClick = onStopRide,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

/**
 * Content shown when manually paused.
 */
@Composable
private fun PausedContent(
    ride: com.example.bikeredlights.domain.model.Ride,
    currentSpeed: Double,
    pausedDuration: java.time.Duration,
    unitsSystem: com.example.bikeredlights.domain.model.settings.UnitsSystem,
    onResumeRide: () -> Unit,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status indicator - compact to save space
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        ) {
            Text(
                text = "⏸",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PAUSED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }

        // Ride statistics (frozen at pause time) - expands to fill available space
        RideStatistics(
            ride = ride,
            currentSpeed = currentSpeed, // 0.0 when paused (reset by service)
            pausedDuration = pausedDuration, // Real-time pause counter (Feature 007)
            unitsSystem = unitsSystem,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Control buttons section (fixed height at bottom, just enough for buttons)
        RideControls(
            isPaused = true,
            onPauseClick = { }, // Not used when paused
            onResumeClick = onResumeRide,
            onStopClick = onStopRide,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

/**
 * Content shown when auto-paused (low speed).
 */
@Composable
private fun AutoPausedContent(
    ride: com.example.bikeredlights.domain.model.Ride,
    currentSpeed: Double,
    pausedDuration: java.time.Duration,
    unitsSystem: com.example.bikeredlights.domain.model.settings.UnitsSystem,
    onResumeRide: () -> Unit,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Same as paused, but with "Auto-paused" label
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status indicator - compact to save space
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        ) {
            Text(
                text = "⏸",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "AUTO-PAUSED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )
        }

        // Ride statistics (frozen at auto-pause) - expands to fill available space
        RideStatistics(
            ride = ride,
            currentSpeed = currentSpeed, // Real-time (may trigger auto-resume if > 1 km/h)
            pausedDuration = pausedDuration, // Real-time pause counter (Feature 007)
            unitsSystem = unitsSystem,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Control buttons section (fixed height at bottom, just enough for buttons)
        RideControls(
            isPaused = true,
            onPauseClick = { }, // Not used when auto-paused
            onResumeClick = onResumeRide,
            onStopClick = onStopRide,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

/**
 * Content shown while waiting for GPS to initialize.
 *
 * **Bug #14 Enhancement**:
 * - Shows loading spinner while GPS acquires first fix
 * - Displayed when ride.startTime == 0 (GPS not yet ready)
 * - Automatically transitions to Recording when first GPS fix arrives
 */
@Composable
private fun WaitingForGpsContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Loading spinner
        CircularProgressIndicator(
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.primary
        )

        // Status text
        Text(
            text = "Waiting for GPS...",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Helper text
        Text(
            text = "Make sure you're outdoors with a clear view of the sky",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

/**
 * GPS Status Indicator - Always visible at top of Live Ride screen.
 *
 * Shows GPS state:
 * - Idle: "GPS Off" (gray)
 * - WaitingForGps: "GPS Searching..." (orange, pulsing)
 * - Recording/Paused/AutoPaused: "GPS Ready" (green)
 *
 * @param uiState Current UI state to determine GPS status
 * @param modifier Modifier for this composable
 */
@Composable
private fun GpsStatusIndicator(
    uiState: RideRecordingUiState,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (uiState) {
        is RideRecordingUiState.Idle -> "GPS Off" to MaterialTheme.colorScheme.onSurfaceVariant
        is RideRecordingUiState.WaitingForGps -> "GPS Searching..." to MaterialTheme.colorScheme.tertiary
        is RideRecordingUiState.Recording,
        is RideRecordingUiState.Paused,
        is RideRecordingUiState.AutoPaused -> "GPS Ready" to MaterialTheme.colorScheme.primary
        is RideRecordingUiState.ShowingSaveDialog -> "GPS Off" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        color = statusColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = statusColor,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Status text
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
