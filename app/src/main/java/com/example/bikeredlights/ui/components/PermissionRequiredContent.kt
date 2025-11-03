package com.example.bikeredlights.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Composable that displays permission required state to the user.
 *
 * This component is shown when location permission has not been granted.
 * It provides clear visual feedback with an icon, explanatory text, and
 * an optional action button to request permission.
 *
 * Usage:
 * ```kotlin
 * PermissionRequiredContent(
 *     onRequestPermission = { permissionLauncher.launch(...) }
 * )
 * ```
 *
 * @param onRequestPermission Optional callback triggered when user taps "Grant Permission" button.
 *                            If null, button is not displayed.
 * @param modifier Optional modifier for layout customization
 */
@Composable
fun PermissionRequiredContent(
    onRequestPermission: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // LocationOff icon
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = "Location permission required",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Headline
        Text(
            text = "Location Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Explanation text
        Text(
            text = "BikeRedlights needs access to your location to track your cycling speed. " +
                    "Your location data is only used while the app is active and is never shared.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Optional "Grant Permission" button
        if (onRequestPermission != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermission
            ) {
                Text("Grant Permission")
            }
        }
    }
}
