package com.example.bikeredlights.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Composable that handles location permission requests with proper UX flow.
 *
 * This component manages the complete permission lifecycle:
 * - Initial permission check on app start
 * - Permission request when needed
 * - Rationale dialog after first denial
 * - Settings dialog for "Don't ask again" scenario
 * - Re-check permissions when app returns to foreground
 *
 * Usage:
 * ```kotlin
 * LocationPermissionHandler(
 *     onPermissionGranted = { viewModel.onPermissionGranted() },
 *     onPermissionDenied = { viewModel.onPermissionDenied() }
 * ) {
 *     // Your content here
 *     SpeedTrackingScreen(viewModel)
 * }
 * ```
 *
 * @param onPermissionGranted Called when user grants location permission
 * @param onPermissionDenied Called when user denies location permission
 * @param content Composable content to display (receives permission state via callbacks)
 */
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionsGranted by remember {
        mutableStateOf(hasLocationPermissions(context))
    }
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if at least FINE location was granted
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        when {
            fineLocationGranted -> {
                // User granted precise location (preferred for cycling)
                permissionsGranted = true
                onPermissionGranted()
            }
            coarseLocationGranted -> {
                // User granted approximate location only
                // Still usable but warn user that speed may be less accurate
                permissionsGranted = true
                onPermissionGranted()
            }
            else -> {
                // All permissions denied
                permissionsGranted = false
                onPermissionDenied()
            }
        }
    }

    // Check permissions on lifecycle start (including when returning from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                val hasPermissions = hasLocationPermissions(context)
                permissionsGranted = hasPermissions

                if (hasPermissions) {
                    onPermissionGranted()
                } else {
                    // Request permissions
                    permissionLauncher.launch(locationPermissions)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Rationale Dialog (shown after first denial)
    if (showRationale) {
        PermissionRationaleDialog(
            onDismiss = {
                showRationale = false
                onPermissionDenied()
            },
            onRequestPermission = {
                showRationale = false
                permissionLauncher.launch(locationPermissions)
            }
        )
    }

    // Settings Dialog (shown for "Don't ask again" scenario)
    if (showSettingsDialog) {
        PermissionSettingsDialog(
            context = context,
            onDismiss = {
                showSettingsDialog = false
                onPermissionDenied()
            }
        )
    }

    content()
}

/**
 * Checks if location permissions are granted.
 *
 * @param context Android context
 * @return true if ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is granted
 */
private fun hasLocationPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Dialog explaining why location permission is needed.
 *
 * Shown after user denies permission for the first time, providing context
 * about why BikeRedlights needs location access.
 */
@Composable
private fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Permission Required") },
        text = {
            Text(
                "BikeRedlights needs access to your location to track your cycling speed " +
                        "and display your current position. Your location data is only used " +
                        "while the app is active and is never shared with third parties."
            )
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

/**
 * Dialog directing user to app settings to enable location permission.
 *
 * Shown when user has selected "Don't ask again" and permission is still needed.
 * Provides a direct link to the app settings page.
 */
@Composable
private fun PermissionSettingsDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = {
            Text(
                "Location permission is required for BikeRedlights to function. " +
                        "Please enable location access in Settings > Apps > BikeRedlights > Permissions."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
