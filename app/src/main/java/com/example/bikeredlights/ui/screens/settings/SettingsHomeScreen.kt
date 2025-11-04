package com.example.bikeredlights.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * Settings home screen - placeholder for Phase 3 implementation.
 *
 * This will be replaced with the full Settings UI in Phase 3 (User Story 1).
 * The full implementation will include:
 * - SettingsViewModel integration
 * - "Ride & Tracking" card
 * - Navigation to detail screens
 */
@Composable
fun SettingsHomeScreen(
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Settings\n\nComing in Phase 3\n(User Story 1)",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
