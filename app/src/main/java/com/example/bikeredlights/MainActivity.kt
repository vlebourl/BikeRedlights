package com.example.bikeredlights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.bikeredlights.data.repository.LocationRepositoryImpl
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase
import com.example.bikeredlights.ui.screens.SpeedTrackingScreen
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModel
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModelFactory

/**
 * MainActivity
 *
 * Main entry point for the BikeRedlights app.
 * Implements User Story 1: View Current Speed While Riding
 *
 * Uses manual dependency injection (v0.1.0 will migrate to Hilt):
 * 1. Create LocationRepositoryImpl with FusedLocationProviderClient
 * 2. Create TrackLocationUseCase with repository
 * 3. Create SpeedTrackingViewModel with use case via factory
 * 4. Render SpeedTrackingScreen with ViewModel
 *
 * TODO v0.1.0: Add Hilt dependency injection (@AndroidEntryPoint)
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: SpeedTrackingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manual dependency injection (until Hilt is re-enabled)
        val locationRepository = LocationRepositoryImpl(
            context = applicationContext
        )
        val trackLocationUseCase = TrackLocationUseCase(
            locationRepository = locationRepository
        )
        val factory = SpeedTrackingViewModelFactory(locationRepository, trackLocationUseCase)
        viewModel = ViewModelProvider(this, factory)[SpeedTrackingViewModel::class.java]

        setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
