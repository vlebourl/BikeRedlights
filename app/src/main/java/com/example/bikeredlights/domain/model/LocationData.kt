package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents a single GPS location reading with all relevant metadata.
 *
 * This immutable value object encapsulates raw location data from Android Location Services.
 * All fields use standard units (degrees for coordinates, meters for accuracy, milliseconds for time).
 *
 * @property latitude GPS latitude coordinate in degrees (-90.0 to 90.0)
 * @property longitude GPS longitude coordinate in degrees (-180.0 to 180.0)
 * @property accuracy Horizontal accuracy radius in meters (68% confidence)
 * @property timestamp Unix epoch time in milliseconds when location was acquired
 * @property speedMps Speed from GPS in meters per second (null if unavailable)
 * @property bearing Direction of travel in degrees (0° = North, clockwise), null if unavailable or stationary
 */
@Immutable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val speedMps: Float? = null,
    val bearing: Float? = null
)
