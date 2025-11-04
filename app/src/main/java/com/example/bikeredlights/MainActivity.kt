package com.example.bikeredlights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bikeredlights.data.repository.LocationRepositoryImpl
import com.example.bikeredlights.data.repository.SettingsRepositoryImpl
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase
import com.example.bikeredlights.ui.navigation.AppNavigation
import com.example.bikeredlights.ui.navigation.BottomNavDestination
import com.example.bikeredlights.ui.screens.settings.SettingsViewModel
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModel
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModelFactory

/**
 * MainActivity
 *
 * Main entry point for the BikeRedlights app.
 *
 * v0.2.0 Update: Added bottom navigation with 3 tabs (Live, Rides, Settings)
 *
 * Uses manual dependency injection (Hilt deferred per constitution exception):
 * 1. Create LocationRepositoryImpl with FusedLocationProviderClient
 * 2. Create SettingsRepositoryImpl with DataStore
 * 3. Create TrackLocationUseCase with location repository
 * 4. Create SpeedTrackingViewModel with use case via factory
 * 5. Create SettingsViewModel with settings repository
 * 6. Render Scaffold with NavigationBar and AppNavigation
 *
 * TODO v0.3.0: Migrate to Hilt dependency injection (@AndroidEntryPoint)
 */
class MainActivity : ComponentActivity() {

    private lateinit var speedTrackingViewModel: SpeedTrackingViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manual dependency injection (until Hilt is re-enabled in v0.3.0)

        // Settings dependencies (v0.2.0) - created first since used by SpeedTrackingViewModel
        val settingsRepository = SettingsRepositoryImpl(
            context = applicationContext
        )
        settingsViewModel = SettingsViewModel(settingsRepository)

        // Location tracking dependencies (v0.1.0, v0.2.0 update with SettingsRepository)
        val locationRepository = LocationRepositoryImpl(
            context = applicationContext
        )
        val trackLocationUseCase = TrackLocationUseCase(
            locationRepository = locationRepository
        )
        val speedTrackingFactory = SpeedTrackingViewModelFactory(
            locationRepository,
            trackLocationUseCase,
            settingsRepository  // v0.2.0: Pass settings repository for units conversion
        )
        speedTrackingViewModel = ViewModelProvider(this, speedTrackingFactory)[SpeedTrackingViewModel::class.java]

        setContent {
            BikeRedlightsTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            BottomNavDestination.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            // Pop up to start destination to avoid building backstack
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        when (destination) {
                                            BottomNavDestination.LIVE -> Icon(
                                                painter = painterResource(android.R.drawable.ic_menu_compass),
                                                contentDescription = destination.label
                                            )
                                            BottomNavDestination.RIDES -> Icon(
                                                imageVector = Icons.Default.List,
                                                contentDescription = destination.label
                                            )
                                            BottomNavDestination.SETTINGS -> Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = destination.label
                                            )
                                        }
                                    },
                                    label = { Text(destination.label) }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    AppNavigation(
                        navController = navController,
                        speedTrackingViewModel = speedTrackingViewModel,
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
