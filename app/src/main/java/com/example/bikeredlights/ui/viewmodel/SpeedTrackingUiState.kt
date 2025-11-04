package com.example.bikeredlights.ui.viewmodel

import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.domain.model.LocationData
import com.example.bikeredlights.domain.model.SpeedMeasurement
import com.example.bikeredlights.domain.model.settings.UnitsSystem

/**
 * UI state for the speed tracking screen.
 *
 * This data class aggregates all domain models needed for rendering the tracking UI.
 * It follows the unidirectional data flow pattern where state flows down from ViewModel
 * to UI composables.
 *
 * @property speedMeasurement Current speed data, null when no GPS data yet (show "---")
 * @property locationData Current GPS coordinates, null before first fix (show "Acquiring...")
 * @property gpsStatus GPS signal quality state (Unavailable, Acquiring, or Active)
 * @property hasLocationPermission Whether location permission is granted
 * @property errorMessage User-facing error message, null when no error
 * @property unitsSystem User's preferred measurement system (Metric/Imperial) - v0.2.0
 */
data class SpeedTrackingUiState(
    val speedMeasurement: SpeedMeasurement? = null,
    val locationData: LocationData? = null,
    val gpsStatus: GpsStatus = GpsStatus.Acquiring,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null,
    val unitsSystem: UnitsSystem = UnitsSystem.DEFAULT
)
