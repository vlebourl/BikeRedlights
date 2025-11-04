package com.example.bikeredlights.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.bikeredlights.ui.screens.SpeedTrackingScreen
import com.example.bikeredlights.ui.screens.settings.SettingsHomeScreen
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModel

/**
 * Main navigation graph for the BikeRedlights app.
 *
 * Handles routing between:
 * - Live tab (speed tracking)
 * - Rides tab (placeholder for Feature 3)
 * - Settings tab (Feature 2A)
 *
 * @param navController Navigation controller for managing navigation state
 * @param speedTrackingViewModel ViewModel for speed tracking (existing v0.1.0)
 * @param modifier Modifier for NavHost
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    speedTrackingViewModel: SpeedTrackingViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavDestination.DEFAULT.route,
        modifier = modifier
    ) {
        // Live tab - Speed tracking (v0.1.0)
        composable(BottomNavDestination.LIVE.route) {
            SpeedTrackingScreen(
                viewModel = speedTrackingViewModel,
                modifier = Modifier
            )
        }

        // Rides tab - Placeholder for Feature 3 (Core Ride Recording)
        composable(BottomNavDestination.RIDES.route) {
            RidesPlaceholderScreen()
        }

        // Settings tab - Feature 2A
        composable(BottomNavDestination.SETTINGS.route) {
            SettingsHomeScreen()
        }
    }
}

/**
 * Placeholder screen for Rides tab.
 * Will be replaced by actual Ride History screen in Feature 3.
 */
@Composable
private fun RidesPlaceholderScreen() {
    Surface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Rides History\n\nComing in Feature 3",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
