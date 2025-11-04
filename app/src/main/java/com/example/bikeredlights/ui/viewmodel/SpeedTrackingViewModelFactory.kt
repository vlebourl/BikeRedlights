package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.repository.LocationRepository
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase

/**
 * Factory for creating SpeedTrackingViewModel with constructor dependencies.
 *
 * This factory is required for manual dependency injection (used until
 * Hilt is re-enabled in v0.3.0). It provides the LocationRepository,
 * TrackLocationUseCase, and SettingsRepository dependencies to the ViewModel constructor.
 *
 * v0.2.0 Update: Added SettingsRepository parameter for units conversion support.
 *
 * Usage in MainActivity:
 * ```kotlin
 * val factory = SpeedTrackingViewModelFactory(
 *     locationRepository,
 *     trackLocationUseCase,
 *     settingsRepository
 * )
 * val viewModel = ViewModelProvider(this, factory)[SpeedTrackingViewModel::class.java]
 * ```
 *
 * @param locationRepository Repository for location updates
 * @param trackLocationUseCase Use case for tracking location and calculating speed
 * @param settingsRepository Repository for user preferences (v0.2.0)
 */
class SpeedTrackingViewModelFactory(
    private val locationRepository: LocationRepository,
    private val trackLocationUseCase: TrackLocationUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpeedTrackingViewModel::class.java)) {
            return SpeedTrackingViewModel(
                locationRepository,
                trackLocationUseCase,
                settingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
