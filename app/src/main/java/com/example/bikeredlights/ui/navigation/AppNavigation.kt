package com.example.bikeredlights.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.bikeredlights.ui.screens.history.RideDetailScreen
import com.example.bikeredlights.ui.screens.history.RideHistoryScreen
import com.example.bikeredlights.ui.screens.ride.LiveRideScreen
import com.example.bikeredlights.ui.screens.ride.RideReviewScreen
import com.example.bikeredlights.ui.screens.settings.RideTrackingSettingsScreen
import com.example.bikeredlights.ui.screens.settings.SettingsHomeScreen
import com.example.bikeredlights.ui.screens.settings.SettingsViewModel
import com.example.bikeredlights.ui.screens.MapTestScreen
import com.example.bikeredlights.ui.viewmodel.RideRecordingViewModel

/**
 * Main navigation graph for the BikeRedlights app.
 *
 * Handles routing between:
 * - Live tab (ride recording)
 * - Rides tab (placeholder for Feature 3)
 * - Settings tab (Feature 2A)
 * - Settings detail screens (Ride & Tracking)
 * - Ride review screen (after saving ride)
 *
 * @param navController Navigation controller for managing navigation state
 * @param rideRecordingViewModel ViewModel for ride recording (Feature 002)
 * @param settingsViewModel ViewModel for settings (Feature 2A)
 * @param modifier Modifier for NavHost
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    rideRecordingViewModel: RideRecordingViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavDestination.DEFAULT.route,
        modifier = modifier
    ) {
        // Live tab - Ride recording (Feature 002)
        composable(BottomNavDestination.LIVE.route) {
            LiveRideScreen(
                viewModel = rideRecordingViewModel,
                onNavigateToReview = { rideId ->
                    navController.navigate("ride_review/$rideId")
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Ride review screen (Feature 002) - shows completed ride statistics
        composable(
            route = "ride_review/{rideId}",
            arguments = listOf(
                navArgument("rideId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getLong("rideId") ?: 0L
            RideReviewScreen(
                rideId = rideId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Rides tab - Feature 3 (Ride History)
        composable(BottomNavDestination.RIDES.route) {
            RideHistoryScreen(
                onRideClick = { rideId ->
                    navController.navigate("ride_detail/$rideId")
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Ride detail screen (Feature 003) - shows detailed ride statistics
        composable(
            route = "ride_detail/{rideId}",
            arguments = listOf(
                navArgument("rideId") {
                    type = NavType.LongType
                }
            )
        ) {
            RideDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings tab - Feature 2A home screen
        composable(BottomNavDestination.SETTINGS.route) {
            SettingsHomeScreen(
                onRideTrackingClick = {
                    navController.navigate(SettingsDestination.RIDE_TRACKING.route)
                }
            )
        }

        // Settings detail screen - Ride & Tracking
        composable(SettingsDestination.RIDE_TRACKING.route) {
            val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            RideTrackingSettingsScreen(
                unitsSystem = uiState.unitsSystem,
                gpsAccuracy = uiState.gpsAccuracy,
                autoPauseConfig = uiState.autoPauseConfig,
                onUnitsChange = { units ->
                    settingsViewModel.setUnitsSystem(units)
                },
                onGpsAccuracyChange = { accuracy ->
                    settingsViewModel.setGpsAccuracy(accuracy)
                },
                onAutoPauseChange = { config ->
                    // Use atomic update to avoid race condition
                    settingsViewModel.setAutoPauseConfig(config)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // TEMPORARY: Map SDK Integration Test Screen (Phase 3 - US3)
        // This route will be removed after testing is complete
        composable("map_test") {
            MapTestScreen()
        }
    }
}

