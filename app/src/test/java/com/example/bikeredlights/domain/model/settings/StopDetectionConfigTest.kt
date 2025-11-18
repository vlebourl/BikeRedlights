package com.example.bikeredlights.domain.model.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StopDetectionConfigTest {

    // T003: Test default values validation
    @Test
    fun `default values should be valid`() {
        val config = StopDetectionConfig()

        assertEquals(3f, config.speedThresholdKmh)
        assertEquals(15, config.durationThresholdSeconds)
        assertEquals(20, config.clusteringRadiusMeters)
    }

    // T004: Test invalid speed threshold rejection
    @Test
    fun `invalid speed threshold 0 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(speedThresholdKmh = 0f)
        }
    }

    @Test
    fun `invalid speed threshold 6 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(speedThresholdKmh = 6f)
        }
    }

    @Test
    fun `invalid speed threshold 2_5 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(speedThresholdKmh = 2.5f)
        }
    }

    // T005: Test invalid duration threshold rejection
    @Test
    fun `invalid duration threshold 0 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(durationThresholdSeconds = 0)
        }
    }

    @Test
    fun `invalid duration threshold 35 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(durationThresholdSeconds = 35)
        }
    }

    @Test
    fun `invalid duration threshold 12 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(durationThresholdSeconds = 12)
        }
    }

    // T006: Test invalid clustering radius rejection
    @Test
    fun `invalid clustering radius 5 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(clusteringRadiusMeters = 5)
        }
    }

    @Test
    fun `invalid clustering radius 60 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(clusteringRadiusMeters = 60)
        }
    }

    @Test
    fun `invalid clustering radius 35 should throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            StopDetectionConfig(clusteringRadiusMeters = 35)
        }
    }

    @Test
    fun `valid values should create config successfully`() {
        val config = StopDetectionConfig(
            speedThresholdKmh = 5f,
            durationThresholdSeconds = 30,
            clusteringRadiusMeters = 50
        )

        assertEquals(5f, config.speedThresholdKmh)
        assertEquals(30, config.durationThresholdSeconds)
        assertEquals(50, config.clusteringRadiusMeters)
    }

    @Test
    fun `all valid speed thresholds should be accepted`() {
        StopDetectionConfig.VALID_SPEED_THRESHOLDS.forEach { speed ->
            val config = StopDetectionConfig(speedThresholdKmh = speed)
            assertEquals(speed, config.speedThresholdKmh)
        }
    }

    @Test
    fun `all valid duration thresholds should be accepted`() {
        StopDetectionConfig.VALID_DURATION_THRESHOLDS.forEach { duration ->
            val config = StopDetectionConfig(durationThresholdSeconds = duration)
            assertEquals(duration, config.durationThresholdSeconds)
        }
    }

    @Test
    fun `all valid clustering radii should be accepted`() {
        StopDetectionConfig.VALID_CLUSTERING_RADII.forEach { radius ->
            val config = StopDetectionConfig(clusteringRadiusMeters = radius)
            assertEquals(radius, config.clusteringRadiusMeters)
        }
    }
}
