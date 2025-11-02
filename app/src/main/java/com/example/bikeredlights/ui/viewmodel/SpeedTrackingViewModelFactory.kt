package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase

/**
 * Factory for creating SpeedTrackingViewModel with constructor dependencies.
 *
 * This factory is required for manual dependency injection (used in MVP until
 * Hilt is re-enabled in v0.1.0). It provides the TrackLocationUseCase dependency
 * to the ViewModel constructor.
 *
 * Usage in MainActivity:
 * ```kotlin
 * val factory = SpeedTrackingViewModelFactory(trackLocationUseCase)
 * val viewModel = ViewModelProvider(this, factory)[SpeedTrackingViewModel::class.java]
 * ```
 *
 * @param trackLocationUseCase Use case for tracking location and calculating speed
 */
class SpeedTrackingViewModelFactory(
    private val trackLocationUseCase: TrackLocationUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpeedTrackingViewModel::class.java)) {
            return SpeedTrackingViewModel(trackLocationUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
