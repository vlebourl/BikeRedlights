package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.TrackPoint
import javax.inject.Inject
import kotlin.math.*

/**
 * Use case for calculating distance between two GPS track points.
 *
 * **Algorithm**: Haversine Formula
 * - Calculates great-circle distance between two points on a sphere
 * - Accurate for short distances (< 500km)
 * - Earth radius: 6371 km
 *
 * **Business Logic**:
 * - Takes two consecutive track points
 * - Calculates distance in meters
 * - Used to accumulate total ride distance
 *
 * **Use Case Flow**:
 * 1. RideRecordingService receives new GPS update
 * 2. Service retrieves last track point from database
 * 3. Service calls CalculateDistanceUseCase(lastPoint, newPoint)
 * 4. Use case returns distance in meters
 * 5. Service accumulates distance to ride total
 *
 * **Performance**:
 * - Pure computation (no I/O)
 * - Fast: ~1-2 microseconds per call
 * - Can be called on any thread
 *
 * **Accuracy**:
 * - Accurate within 0.5% for distances < 500km
 * - GPS accuracy (5-10m) is limiting factor, not formula
 *
 * @see <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine Formula</a>
 */
class CalculateDistanceUseCase @Inject constructor() {
    /**
     * Calculate distance between two track points using Haversine formula.
     *
     * @param from Starting track point
     * @param to Ending track point
     * @return Distance in meters
     */
    operator fun invoke(from: TrackPoint, to: TrackPoint): Double {
        return calculateHaversineDistance(
            lat1 = from.latitude,
            lon1 = from.longitude,
            lat2 = to.latitude,
            lon2 = to.longitude
        )
    }

    /**
     * Calculate distance between two GPS coordinates using Haversine formula.
     *
     * Formula:
     * ```
     * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
     * c = 2 * atan2(√a, √(1−a))
     * distance = R * c
     * ```
     *
     * @param lat1 Latitude of first point (decimal degrees)
     * @param lon1 Longitude of first point (decimal degrees)
     * @param lat2 Latitude of second point (decimal degrees)
     * @param lon2 Longitude of second point (decimal degrees)
     * @return Distance in meters
     */
    private fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        // Convert degrees to radians
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Differences
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        // Haversine formula
        val a = sin(dLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Distance in meters
        return EARTH_RADIUS_METERS * c
    }

    companion object {
        /**
         * Earth radius in meters (mean radius).
         */
        private const val EARTH_RADIUS_METERS = 6371000.0
    }
}
