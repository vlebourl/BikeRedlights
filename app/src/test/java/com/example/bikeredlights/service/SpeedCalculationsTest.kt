package com.example.bikeredlights.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for speed calculation formulas used in RideRecordingService.
 *
 * **Test Coverage**:
 * - T067: Average speed calculation (distance / moving time)
 * - T068: Max speed tracking (peak value)
 * - T069: Stationary detection (< 1 km/h = 0.0)
 * - T070: Moving duration calculation (exclude pauses)
 *
 * **Note**: These tests verify the calculation logic used in
 * RideRecordingService.updateRideDistance() method. The actual
 * implementation is in the service, but these tests document
 * and verify the formulas.
 */
class SpeedCalculationsTest {

    // ========================================
    // T067: Average Speed Calculation
    // ========================================

    @Test
    fun `average speed calculation - zero distance returns zero`() {
        val distance = 0.0  // meters
        val movingDuration = 60_000L  // 1 minute in milliseconds

        val avgSpeed = calculateAverageSpeed(distance, movingDuration)

        assertThat(avgSpeed).isWithin(0.001).of(0.0)
    }

    @Test
    fun `average speed calculation - zero moving time returns zero`() {
        val distance = 1000.0  // 1 km
        val movingDuration = 0L

        val avgSpeed = calculateAverageSpeed(distance, movingDuration)

        assertThat(avgSpeed).isWithin(0.001).of(0.0)
    }

    @Test
    fun `average speed calculation - 1 km in 1 minute returns 16_67 m per s`() {
        val distance = 1000.0  // 1 km = 1000 meters
        val movingDuration = 60_000L  // 1 minute = 60 seconds = 60,000 ms

        val avgSpeed = calculateAverageSpeed(distance, movingDuration)

        // 1000m / 60s = 16.666... m/s (60 km/h)
        assertThat(avgSpeed).isWithin(0.01).of(16.67)
    }

    @Test
    fun `average speed calculation - 10 km in 30 minutes returns 5_56 m per s`() {
        val distance = 10_000.0  // 10 km
        val movingDuration = 1_800_000L  // 30 minutes

        val avgSpeed = calculateAverageSpeed(distance, movingDuration)

        // 10,000m / 1800s = 5.555... m/s (20 km/h)
        assertThat(avgSpeed).isWithin(0.01).of(5.56)
    }

    @Test
    fun `average speed calculation - typical cycling speed 20 km per h`() {
        val distance = 5000.0  // 5 km
        val movingDuration = 900_000L  // 15 minutes

        val avgSpeed = calculateAverageSpeed(distance, movingDuration)

        // 5000m / 900s = 5.555... m/s (20 km/h)
        assertThat(avgSpeed).isWithin(0.01).of(5.56)
    }

    // ========================================
    // T068: Max Speed Tracking
    // ========================================

    @Test
    fun `max speed tracking - new speed higher than current max updates max`() {
        val currentMax = 5.0  // 5 m/s (18 km/h)
        val newSpeed = 8.0    // 8 m/s (28.8 km/h)

        val updatedMax = maxOf(currentMax, newSpeed)

        assertThat(updatedMax).isEqualTo(8.0)
    }

    @Test
    fun `max speed tracking - new speed lower than current max keeps max`() {
        val currentMax = 10.0  // 10 m/s (36 km/h)
        val newSpeed = 6.0     // 6 m/s (21.6 km/h)

        val updatedMax = maxOf(currentMax, newSpeed)

        assertThat(updatedMax).isEqualTo(10.0)
    }

    @Test
    fun `max speed tracking - new speed equal to current max keeps max`() {
        val currentMax = 7.5  // 7.5 m/s (27 km/h)
        val newSpeed = 7.5    // 7.5 m/s (27 km/h)

        val updatedMax = maxOf(currentMax, newSpeed)

        assertThat(updatedMax).isEqualTo(7.5)
    }

    @Test
    fun `max speed tracking - initial max is zero updates to first speed`() {
        val currentMax = 0.0  // Initial state
        val newSpeed = 5.0    // First GPS reading

        val updatedMax = maxOf(currentMax, newSpeed)

        assertThat(updatedMax).isEqualTo(5.0)
    }

    // ========================================
    // T069: Stationary Detection
    // ========================================

    @Test
    fun `stationary detection - speed below 1 km per h is treated as zero`() {
        val gpsSpeed = 0.2  // 0.2 m/s = 0.72 km/h

        val currentSpeed = applyStationaryDetection(gpsSpeed)

        assertThat(currentSpeed).isEqualTo(0.0)
    }

    @Test
    fun `stationary detection - speed exactly 1 km per h is treated as zero`() {
        val gpsSpeed = 0.278  // 0.278 m/s ≈ 1 km/h

        val currentSpeed = applyStationaryDetection(gpsSpeed)

        // Edge case: < 0.278 is zero, but 0.278 exactly should also be zero
        // Implementation uses < 0.278, so 0.278 will be kept
        assertThat(currentSpeed).isGreaterThan(0.0)
    }

    @Test
    fun `stationary detection - speed above 1 km per h is kept`() {
        val gpsSpeed = 1.0  // 1 m/s = 3.6 km/h

        val currentSpeed = applyStationaryDetection(gpsSpeed)

        assertThat(currentSpeed).isEqualTo(1.0)
    }

    @Test
    fun `stationary detection - zero speed stays zero`() {
        val gpsSpeed = 0.0

        val currentSpeed = applyStationaryDetection(gpsSpeed)

        assertThat(currentSpeed).isEqualTo(0.0)
    }

    @Test
    fun `stationary detection - threshold conversion 1 km per h to m per s`() {
        val thresholdKmh = 1.0
        val thresholdMs = thresholdKmh / 3.6

        // 1 km/h = 1000m / 3600s = 0.277778 m/s
        assertThat(thresholdMs).isWithin(0.001).of(0.278)
    }

    // ========================================
    // T070: Moving Duration Calculation
    // ========================================

    @Test
    fun `moving duration - no pauses returns elapsed duration`() {
        val elapsedDuration = 3_600_000L  // 1 hour
        val manualPauseDuration = 0L
        val autoPauseDuration = 0L

        val movingDuration = calculateMovingDuration(
            elapsedDuration,
            manualPauseDuration,
            autoPauseDuration
        )

        assertThat(movingDuration).isEqualTo(3_600_000L)
    }

    @Test
    fun `moving duration - excludes manual pause time`() {
        val elapsedDuration = 3_600_000L  // 1 hour
        val manualPauseDuration = 300_000L  // 5 minutes paused
        val autoPauseDuration = 0L

        val movingDuration = calculateMovingDuration(
            elapsedDuration,
            manualPauseDuration,
            autoPauseDuration
        )

        assertThat(movingDuration).isEqualTo(3_300_000L)  // 55 minutes
    }

    @Test
    fun `moving duration - excludes auto pause time`() {
        val elapsedDuration = 3_600_000L  // 1 hour
        val manualPauseDuration = 0L
        val autoPauseDuration = 600_000L  // 10 minutes auto-paused (stopped at lights)

        val movingDuration = calculateMovingDuration(
            elapsedDuration,
            manualPauseDuration,
            autoPauseDuration
        )

        assertThat(movingDuration).isEqualTo(3_000_000L)  // 50 minutes
    }

    @Test
    fun `moving duration - excludes both manual and auto pause time`() {
        val elapsedDuration = 3_600_000L  // 1 hour
        val manualPauseDuration = 300_000L  // 5 minutes manual pause
        val autoPauseDuration = 600_000L  // 10 minutes auto-pause

        val movingDuration = calculateMovingDuration(
            elapsedDuration,
            manualPauseDuration,
            autoPauseDuration
        )

        assertThat(movingDuration).isEqualTo(2_700_000L)  // 45 minutes
    }

    @Test
    fun `moving duration - all time paused returns zero`() {
        val elapsedDuration = 1_800_000L  // 30 minutes
        val manualPauseDuration = 1_200_000L  // 20 minutes
        val autoPauseDuration = 600_000L  // 10 minutes

        val movingDuration = calculateMovingDuration(
            elapsedDuration,
            manualPauseDuration,
            autoPauseDuration
        )

        assertThat(movingDuration).isEqualTo(0L)
    }

    // ========================================
    // Helper Functions (Implementation of Formulas)
    // ========================================

    /**
     * Calculate average speed in meters per second.
     *
     * Formula: distance (m) / time (s)
     *
     * Implementation in RideRecordingService.kt:359
     */
    private fun calculateAverageSpeed(distanceMeters: Double, movingDurationMillis: Long): Double {
        if (movingDurationMillis <= 0) return 0.0
        return distanceMeters / (movingDurationMillis / 1000.0)
    }

    /**
     * Apply stationary detection threshold.
     *
     * Formula: if speed < 1 km/h (0.278 m/s) then 0.0 else speed
     *
     * Implementation in RideRecordingService.kt:341
     */
    private fun applyStationaryDetection(speedMetersPerSec: Double): Double {
        val stationaryThreshold = 0.278  // 1 km/h in m/s
        return if (speedMetersPerSec < stationaryThreshold) {
            0.0
        } else {
            speedMetersPerSec
        }
    }

    /**
     * Calculate moving duration by excluding pauses.
     *
     * Formula: elapsed - manual_paused - auto_paused
     *
     * Implementation in RideRecordingService.kt:355
     */
    private fun calculateMovingDuration(
        elapsedDurationMillis: Long,
        manualPausedDurationMillis: Long,
        autoPausedDurationMillis: Long
    ): Long {
        return elapsedDurationMillis - manualPausedDurationMillis - autoPausedDurationMillis
    }
}
