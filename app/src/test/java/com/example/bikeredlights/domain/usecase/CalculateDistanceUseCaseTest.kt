package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.TrackPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for CalculateDistanceUseCase.
 *
 * **Test Coverage**:
 * - Distance calculation accuracy
 * - Haversine formula verification
 * - Edge cases (same location, different hemispheres)
 * - Known distances (e.g., SF to LA)
 */
class CalculateDistanceUseCaseTest {

    private lateinit var useCase: CalculateDistanceUseCase

    @Before
    fun setup() {
        useCase = CalculateDistanceUseCase()
    }

    @Test
    fun `calculate distance between same location returns zero`() {
        // Given - Same location
        val point1 = createTrackPoint(latitude = 37.7749, longitude = -122.4194)
        val point2 = createTrackPoint(latitude = 37.7749, longitude = -122.4194)

        // When
        val distance = useCase(point1, point2)

        // Then
        assertThat(distance).isWithin(0.1).of(0.0)
    }

    @Test
    fun `calculate distance between nearby points is accurate`() {
        // Given - Two points ~100m apart in San Francisco
        val point1 = createTrackPoint(latitude = 37.7749, longitude = -122.4194)
        val point2 = createTrackPoint(latitude = 37.7759, longitude = -122.4194)

        // When
        val distance = useCase(point1, point2)

        // Then - Approximately 111 meters (1 degree latitude ≈ 111km)
        assertThat(distance).isWithin(5.0).of(111.0)
    }

    @Test
    fun `calculate distance SF to LA is approximately correct`() {
        // Given - San Francisco to Los Angeles (known distance ~559 km)
        val sanFrancisco = createTrackPoint(latitude = 37.7749, longitude = -122.4194)
        val losAngeles = createTrackPoint(latitude = 34.0522, longitude = -118.2437)

        // When
        val distance = useCase(sanFrancisco, losAngeles)

        // Then - Allow 5km margin for Haversine approximation
        assertThat(distance).isWithin(5000.0).of(559000.0)
    }

    @Test
    fun `calculate distance across equator works correctly`() {
        // Given - Points across equator
        val northPoint = createTrackPoint(latitude = 1.0, longitude = 0.0)
        val southPoint = createTrackPoint(latitude = -1.0, longitude = 0.0)

        // When
        val distance = useCase(northPoint, southPoint)

        // Then - Approximately 222 km (2 degrees latitude)
        assertThat(distance).isWithin(1000.0).of(222000.0)
    }

    @Test
    fun `calculate distance across prime meridian works correctly`() {
        // Given - Points across prime meridian
        val westPoint = createTrackPoint(latitude = 0.0, longitude = -1.0)
        val eastPoint = createTrackPoint(latitude = 0.0, longitude = 1.0)

        // When
        val distance = useCase(westPoint, eastPoint)

        // Then - Approximately 222 km (2 degrees longitude at equator)
        assertThat(distance).isWithin(1000.0).of(222000.0)
    }

    @Test
    fun `calculate distance is symmetric`() {
        // Given - Two arbitrary points
        val point1 = createTrackPoint(latitude = 40.7128, longitude = -74.0060)  // NYC
        val point2 = createTrackPoint(latitude = 51.5074, longitude = -0.1278)   // London

        // When
        val distance1to2 = useCase(point1, point2)
        val distance2to1 = useCase(point2, point1)

        // Then - Distance should be same in both directions
        assertThat(distance1to2).isWithin(0.1).of(distance2to1)
    }

    @Test
    fun `calculate distance for 1km cycling is accurate`() {
        // Given - Simulate 1km cycling trip (north direction)
        val start = createTrackPoint(latitude = 37.7749, longitude = -122.4194)
        val end = createTrackPoint(latitude = 37.7839, longitude = -122.4194)  // ~1km north

        // When
        val distance = useCase(start, end)

        // Then - Should be approximately 1000 meters
        assertThat(distance).isWithin(50.0).of(1000.0)
    }

    @Test
    fun `calculate distance for short cycling distance is accurate`() {
        // Given - Simulate 50m cycling (realistic GPS update interval)
        val start = createTrackPoint(latitude = 37.7749, longitude = -122.4194)
        val end = createTrackPoint(latitude = 37.7753, longitude = -122.4194)  // ~50m north

        // When
        val distance = useCase(start, end)

        // Then - Should be approximately 50 meters
        assertThat(distance).isWithin(5.0).of(44.0)  // Actual calculated value
    }

    @Test
    fun `calculate distance is always positive`() {
        // Given - Any two different points
        val point1 = createTrackPoint(latitude = 10.0, longitude = 20.0)
        val point2 = createTrackPoint(latitude = -10.0, longitude = -20.0)

        // When
        val distance = useCase(point1, point2)

        // Then
        assertThat(distance).isGreaterThan(0.0)
    }

    // Helper function

    private fun createTrackPoint(
        latitude: Double,
        longitude: Double,
        rideId: Long = 1L
    ): TrackPoint {
        return TrackPoint(
            id = 0,
            rideId = rideId,
            timestamp = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            speedMetersPerSec = 5.0,
            accuracy = 10.0f,
            isManuallyPaused = false,
            isAutoPaused = false
        )
    }
}
