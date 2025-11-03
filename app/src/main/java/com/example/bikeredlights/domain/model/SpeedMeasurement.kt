package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents calculated cycling speed with accuracy metadata and stationary detection.
 *
 * This value object contains processed speed data ready for UI display, with contextual
 * information about how the speed was determined and whether the cyclist is moving.
 *
 * @property speedKmh Current speed in kilometers per hour (≥0, clamped to max 100 km/h)
 * @property timestamp Unix epoch time in milliseconds when measurement was taken
 * @property accuracyKmh Speed accuracy in km/h if available from GPS (API 26+), null otherwise
 * @property isStationary True if speed is below stationary threshold (<1 km/h), indicating GPS jitter
 * @property source How the speed was determined (GPS, calculated from position, or unknown)
 */
@Immutable
data class SpeedMeasurement(
    val speedKmh: Float,
    val timestamp: Long,
    val accuracyKmh: Float?,
    val isStationary: Boolean,
    val source: SpeedSource
)

/**
 * Indicates how speed measurement was obtained.
 *
 * GPS is preferred (most accurate via Doppler shift), with fallback to calculated
 * speed from position changes, or unknown when no data is available.
 */
enum class SpeedSource {
    /** Speed obtained from Location.getSpeed() (GPS Doppler shift) */
    GPS,

    /** Speed calculated from distance / time between location updates */
    CALCULATED,

    /** No speed data available (first location fix or GPS unavailable) */
    UNKNOWN
}
