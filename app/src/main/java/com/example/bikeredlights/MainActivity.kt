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
import androidx.activity.viewModels
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bikeredlights.ui.navigation.AppNavigation
import com.example.bikeredlights.ui.navigation.BottomNavDestination
import com.example.bikeredlights.ui.screens.settings.SettingsViewModel
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import com.example.bikeredlights.ui.viewmodel.RideRecordingViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity
 *
 * Main entry point for the BikeRedlights app.
 *
 * v0.2.0 Update: Added bottom navigation with 3 tabs (Live, Rides, Settings)
 * v0.3.0 Update: Migrated to Hilt dependency injection
 *
 * **Dependency Injection**: Uses Hilt (@AndroidEntryPoint)
 * - RideRecordingViewModel: Injected for ride recording features
 * - SettingsViewModel: Injected for settings management
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val rideRecordingViewModel: RideRecordingViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                        rideRecordingViewModel = rideRecordingViewModel,
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
