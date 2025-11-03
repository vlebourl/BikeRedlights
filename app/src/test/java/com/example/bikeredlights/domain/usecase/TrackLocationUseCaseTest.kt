package com.example.bikeredlights.domain.usecase

import app.cash.turbine.test
import com.example.bikeredlights.domain.model.LocationData
import com.example.bikeredlights.domain.model.SpeedMeasurement
import com.example.bikeredlights.domain.model.SpeedSource
import com.example.bikeredlights.domain.repository.LocationRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TrackLocationUseCase.
 *
 * Tests critical speed calculation logic including:
 * - m/s to km/h conversion
 * - Stationary threshold detection
 * - Speed clamping for unrealistic values
 * - Speed source determination (GPS vs calculated)
 * - Haversine distance calculation accuracy
 *
 * This is a safety-critical feature requiring 90%+ test coverage.
 */
class TrackLocationUseCaseTest {

    private lateinit var locationRepository: LocationRepository
    private lateinit var useCase: TrackLocationUseCase

    @Before
    fun setup() {
        locationRepository = mockk()
        useCase = TrackLocationUseCase(locationRepository)
    }

    @Test
    fun `invoke converts GPS speed from m per s to km per h correctly`() = runTest {
        // Given: Location with 10 m/s GPS speed
        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = System.currentTimeMillis(),
            speedMps = 10f,  // 10 m/s = 36 km/h
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(locationData)

        // When: Collecting speed measurements
        useCase().test {
            val emission = awaitItem()

            // Then: Speed is converted to km/h (10 * 3.6 = 36)
            assertThat(emission.speedKmh).isWithin(0.1f).of(36f)
            assertThat(emission.source).isEqualTo(SpeedSource.GPS)
            assertThat(emission.isStationary).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `invoke applies stationary threshold for speeds below 1 km per h`() = runTest {
        // Given: Location with 0.2 km/h speed (0.2 / 3.6 ≈ 0.056 m/s)
        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = System.currentTimeMillis(),
            speedMps = 0.056f,  // Below 1 km/h threshold
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(locationData)

        // When: Collecting speed measurements
        useCase().test {
            val emission = awaitItem()

            // Then: Speed is clamped to 0 km/h
            assertThat(emission.speedKmh).isEqualTo(0f)
            assertThat(emission.isStationary).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `invoke handles negative GPS speed by coercing to 0`() = runTest {
        // Given: Location with negative GPS speed (should never happen but defensive coding)
        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = System.currentTimeMillis(),
            speedMps = -5f,  // Invalid negative speed
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(locationData)

        // When: Collecting speed measurements
        useCase().test {
            val emission = awaitItem()

            // Then: Speed is coerced to 0 km/h
            assertThat(emission.speedKmh).isEqualTo(0f)
            assertThat(emission.isStationary).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `invoke clamps unrealistic speeds above 100 km per h`() = runTest {
        // Given: Location with unrealistic 150 km/h speed (150 / 3.6 ≈ 41.67 m/s)
        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = System.currentTimeMillis(),
            speedMps = 45f,  // ~162 km/h - too fast for cycling
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(locationData)

        // When: Collecting speed measurements
        useCase().test {
            val emission = awaitItem()

            // Then: Speed is clamped to 100 km/h maximum
            assertThat(emission.speedKmh).isEqualTo(100f)
            assertThat(emission.source).isEqualTo(SpeedSource.GPS)
            awaitComplete()
        }
    }

    @Test
    fun `invoke calculates speed from position change when GPS speed unavailable`() = runTest {
        // Given: Two locations 100 meters apart with 1 second elapsed
        val location1 = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = 1000L,
            speedMps = null,  // No GPS speed
            bearing = null
        )
        val location2 = LocationData(
            latitude = 37.7758,  // ~100m north
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = 2000L,  // 1 second later
            speedMps = null,  // No GPS speed
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(location1, location2)

        // When: Collecting speed measurements
        useCase().test {
            // First emission: no previous location
            val emission1 = awaitItem()
            assertThat(emission1.speedKmh).isEqualTo(0f)
            assertThat(emission1.source).isEqualTo(SpeedSource.UNKNOWN)

            // Second emission: calculated from distance
            val emission2 = awaitItem()
            // 100m in 1s = 100 m/s = 360 km/h (clamped to 100)
            // Actually ~100m in 1s, so clamped
            assertThat(emission2.speedKmh).isGreaterThan(0f)
            assertThat(emission2.source).isEqualTo(SpeedSource.CALCULATED)
            awaitComplete()
        }
    }

    @Test
    fun `invoke prefers GPS speed over calculated speed when available`() = runTest {
        // Given: Two locations with GPS speed provided
        val location1 = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = 1000L,
            speedMps = 5f,  // 18 km/h GPS speed
            bearing = null
        )
        val location2 = LocationData(
            latitude = 37.7758,  // Different position
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = 2000L,
            speedMps = 8f,  // 28.8 km/h GPS speed
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(location1, location2)

        // When: Collecting speed measurements
        useCase().test {
            // First emission: uses GPS speed
            val emission1 = awaitItem()
            assertThat(emission1.speedKmh).isWithin(0.1f).of(18f)
            assertThat(emission1.source).isEqualTo(SpeedSource.GPS)

            // Second emission: still uses GPS speed (not calculated)
            val emission2 = awaitItem()
            assertThat(emission2.speedKmh).isWithin(0.1f).of(28.8f)
            assertThat(emission2.source).isEqualTo(SpeedSource.GPS)
            awaitComplete()
        }
    }

    @Test
    fun `invoke returns UNKNOWN source when no GPS speed and no previous location`() = runTest {
        // Given: Single location with no GPS speed
        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = System.currentTimeMillis(),
            speedMps = null,
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(locationData)

        // When: Collecting speed measurements
        useCase().test {
            val emission = awaitItem()

            // Then: Speed is 0 with UNKNOWN source
            assertThat(emission.speedKmh).isEqualTo(0f)
            assertThat(emission.source).isEqualTo(SpeedSource.UNKNOWN)
            assertThat(emission.isStationary).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `invoke handles zero elapsed time between location updates`() = runTest {
        // Given: Two locations with same timestamp
        val timestamp = System.currentTimeMillis()
        val location1 = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = timestamp,
            speedMps = null,
            bearing = null
        )
        val location2 = LocationData(
            latitude = 37.7758,
            longitude = -122.4195,
            accuracy = 5f,
            timestamp = timestamp,  // Same timestamp
            speedMps = null,
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(location1, location2)

        // When: Collecting speed measurements
        useCase().test {
            awaitItem()  // First emission

            // Second emission: zero elapsed time should result in UNKNOWN
            val emission2 = awaitItem()
            assertThat(emission2.speedKmh).isEqualTo(0f)
            assertThat(emission2.source).isEqualTo(SpeedSource.UNKNOWN)
            awaitComplete()
        }
    }

    @Test
    fun `invoke calculates realistic cycling speed from position changes`() = runTest {
        // Given: Two locations ~20 meters apart with 1 second elapsed
        // Simulates cycling at 72 km/h (20 m/s)
        val location1 = LocationData(
            latitude = 37.774900,
            longitude = -122.419400,
            accuracy = 5f,
            timestamp = 1000L,
            speedMps = null,
            bearing = null
        )
        val location2 = LocationData(
            latitude = 37.775080,  // ~20m north (0.0018 degrees ≈ 20m)
            longitude = -122.419400,
            accuracy = 5f,
            timestamp = 2000L,  // 1 second later
            speedMps = null,
            bearing = null
        )
        every { locationRepository.getLocationUpdates() } returns flowOf(location1, location2)

        // When: Collecting speed measurements
        useCase().test {
            awaitItem()  // First emission (no previous location)

            // Second emission: calculated from ~20m in 1s
            val emission2 = awaitItem()
            // 20 m/s * 3.6 = 72 km/h
            assertThat(emission2.speedKmh).isGreaterThan(50f)
            assertThat(emission2.speedKmh).isLessThan(100f)
            assertThat(emission2.source).isEqualTo(SpeedSource.CALCULATED)
            assertThat(emission2.isStationary).isFalse()
            awaitComplete()
        }
    }
}
