package com.example.bikeredlights.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.ui.components.settings.SettingCard
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Settings home screen displaying setting categories as cards.
 *
 * Currently displays:
 * - "Ride & Tracking" card (Units, GPS Accuracy, Auto-pause settings)
 * - "Stop Detection" card (Speed threshold, Duration threshold, Clustering radius)
 *
 * Future cards (deferred to later features):
 * - "App Behavior" (theme, language, notifications)
 * - "About" (version, licenses, privacy policy)
 *
 * @param onRideTrackingClick Callback when user taps "Ride & Tracking" card
 * @param onStopDetectionClick Callback when user taps "Stop Detection" card
 * @param modifier Modifier for customizing layout and behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(
    onRideTrackingClick: () -> Unit,
    onStopDetectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)  // Remove status bar padding
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ride & Tracking card (User Story 1)
                SettingCard(
                    title = "Ride & Tracking",
                    subtitle = "Units, GPS, Auto-pause",
                    icon = Icons.Default.DirectionsBike,
                    onClick = onRideTrackingClick
                )

                // Stop Detection card (Feature 008 - v0.8.0)
                SettingCard(
                    title = "Stop Detection",
                    subtitle = "Speed, Duration, Clustering",
                    icon = Icons.Default.PinDrop,
                    onClick = onStopDetectionClick
                )

                // Future cards will be added here:
                // - "App Behavior" card (future)
                // - "About" card (future)
            }
        }
    }
}

/**
 * Preview for SettingsHomeScreen in light theme.
 */
@Preview(showBackground = true)
@Composable
private fun SettingsHomeScreenPreview() {
    BikeRedlightsTheme {
        SettingsHomeScreen(
            onRideTrackingClick = {},
            onStopDetectionClick = {}
        )
    }
}

/**
 * Preview for SettingsHomeScreen in dark theme.
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsHomeScreenPreviewDark() {
    BikeRedlightsTheme {
        SettingsHomeScreen(
            onRideTrackingClick = {},
            onStopDetectionClick = {}
        )
    }
}
