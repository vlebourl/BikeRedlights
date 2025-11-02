package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.LocationData
import com.example.bikeredlights.domain.model.SpeedMeasurement
import com.example.bikeredlights.domain.model.SpeedSource
import com.example.bikeredlights.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Use case for tracking location and calculating cycling speed.
 *
 * This use case encapsulates the business logic for converting raw GPS location
 * data into meaningful speed measurements for cyclists. It handles:
 * - m/s to km/h conversion (×3.6)
 * - Stationary threshold detection (<1 km/h = 0 km/h)
 * - Speed source determination (GPS vs calculated from position)
 * - Fallback speed calculation using Haversine formula
 * - Edge case handling (negative speeds, unrealistic values)
 *
 * @param locationRepository Repository providing location updates
 */
class TrackLocationUseCase(
    private val locationRepository: LocationRepository
) {

    /**
     * Tracks location updates and emits calculated speed measurements.
     *
     * This operator function enables calling the use case as: `trackLocationUseCase()`
     * The Flow maintains previousLocation state to calculate fallback speed from
     * position changes when GPS speed is unavailable.
     *
     * @return Flow<SpeedMeasurement> emitting calculated speed in km/h
     * @throws SecurityException if location permission not granted (propagated from repository)
     */
    operator fun invoke(): Flow<SpeedMeasurement> {
        return locationRepository.getLocationUpdates()
            .scan(Pair<LocationData?, LocationData?>(null, null)) { acc, location ->
                val previousLocation = acc.second
                Pair(previousLocation, location)
            }
            .drop(1)  // Drop initial (null, null) emission from scan
            .map { (previousLocation, currentLocation) ->
                requireNotNull(currentLocation) { "Current location should not be null in scan output" }
                calculateSpeed(currentLocation, previousLocation)
            }
    }

    /**
     * Calculates speed measurement from GPS location data.
     *
     * Speed calculation priority:
     * 1. GPS-provided speed (Location.getSpeed()) - most accurate via Doppler shift
     * 2. Calculated from position change (distance / time) - fallback when GPS speed unavailable
     * 3. Zero speed - when no previous location or invalid data
     *
     * Applies stationary threshold (<1 km/h) to filter GPS jitter when stopped.
     * Clamps speed to realistic cycling range (0-100 km/h).
     *
     * @param current Current GPS location reading
     * @param previous Previous GPS location (for fallback calculation)
     * @return SpeedMeasurement with speed in km/h and metadata
     */
    private fun calculateSpeed(
        current: LocationData,
        previous: LocationData?
    ): SpeedMeasurement {
        // Determine speed and source
        val (speedMs, source) = when {
            // Prefer GPS-provided speed (most accurate)
            current.speedMps != null && current.speedMps > 0 ->
                current.speedMps to SpeedSource.GPS

            // Fallback: calculate from position change
            previous != null -> {
                val elapsedSeconds = (current.timestamp - previous.timestamp) / 1000.0
                if (elapsedSeconds > 0) {
                    val distanceMeters = distanceBetween(current, previous)
                    (distanceMeters / elapsedSeconds).toFloat() to SpeedSource.CALCULATED
                } else {
                    0f to SpeedSource.UNKNOWN
                }
            }

            // No speed data available
            else -> 0f to SpeedSource.UNKNOWN
        }

        // Sanitize and convert speed
        val sanitizedSpeedMs = speedMs.coerceIn(0f, 100f / 3.6f) // Clamp to 0-100 km/h
        val stationaryThresholdMs = 1f / 3.6f // 1 km/h threshold
        val isStationary = sanitizedSpeedMs < stationaryThresholdMs

        return SpeedMeasurement(
            speedKmh = if (isStationary) 0f else sanitizedSpeedMs * 3.6f,
            timestamp = current.timestamp,
            accuracyKmh = null, // TODO: Add speedAccuracyMetersPerSecond from Location (API 26+)
            isStationary = isStationary,
            source = source
        )
    }

    /**
     * Calculates distance between two GPS coordinates using Haversine formula.
     *
     * The Haversine formula computes great-circle distance between two points on
     * a sphere, accounting for Earth's curvature. Accurate for cycling distances
     * (typically <100km between location updates).
     *
     * @param loc1 First location point
     * @param loc2 Second location point
     * @return Distance in meters
     */
    private fun distanceBetween(loc1: LocationData, loc2: LocationData): Float {
        val earthRadius = 6371000f // meters

        val dLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val dLon = Math.toRadians(loc2.longitude - loc1.longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(loc1.latitude)) *
                cos(Math.toRadians(loc2.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }
}
