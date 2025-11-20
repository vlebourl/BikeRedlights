package com.example.bikeredlights.domain.util

import com.example.bikeredlights.domain.model.Stop
import com.example.bikeredlights.domain.model.StopDetectionState
import com.example.bikeredlights.domain.repository.StopRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StopDetectionStateMachine (Feature 009, Phase 2, Task T012).
 *
 * Test Coverage:
 * - State transitions (Moving → Detecting → Confirmed → Moving)
 * - Consecutive seconds filtering (3-second requirement)
 * - Duration threshold validation
 * - Database persistence on confirmation
 * - Edge cases (GPS noise, rapid speed changes, manual pause)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StopDetectionStateMachineTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var testScope: TestScope
    private lateinit var stateMachine: StopDetectionStateMachine

    private val speedThreshold = 3f // km/h
    private val durationThreshold = 15 // seconds
    private val rideId = 100L
    private val testLatitude = 37.422
    private val testLongitude = -122.084

    @Before
    fun setup() {
        stopRepository = mockk(relaxed = true)
        testScope = TestScope(UnconfinedTestDispatcher())

        // Mock database insert to return stopId
        coEvery { stopRepository.insertStop(any()) } returns 1L

        stateMachine = StopDetectionStateMachine(
            stopRepository = stopRepository,
            speedThresholdKmh = speedThreshold,
            durationThresholdSeconds = durationThreshold,
            scope = testScope
        )
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `initial state is Moving`() = runTest {
        stateMachine.startRide(rideId)

        val state = stateMachine.getCurrentState()

        assertTrue("Should be in Moving state", state.isMoving)
        assertFalse("Should not be detecting", state.isDetecting)
        assertFalse("Should not be stopped", state.isStopped)
        assertEquals(0f, state.currentSpeed, 0.01f)
        assertEquals(0, state.speedBelowThresholdCount)
        assertEquals(1, state.currentStopNumber)
        assertNull(state.activeStopId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid speed threshold throws exception`() {
        StopDetectionStateMachine(
            stopRepository = stopRepository,
            speedThresholdKmh = 10f, // Invalid: must be 1-5 km/h
            durationThresholdSeconds = durationThreshold,
            scope = testScope
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid duration threshold throws exception`() {
        StopDetectionStateMachine(
            stopRepository = stopRepository,
            speedThresholdKmh = speedThreshold,
            durationThresholdSeconds = 50, // Invalid: must be 5-30 seconds
            scope = testScope
        )
    }

    // ==================== State Transition Tests ====================

    @Test
    fun `Moving to Detecting on first speed below threshold`() = runTest {
        stateMachine.startRide(rideId)

        stateMachine.processSpeed(
            speedKmh = 2f, // Below 3 km/h threshold
            latitude = testLatitude,
            longitude = testLongitude,
            timestamp = 1000L
        )

        val state = stateMachine.getCurrentState()

        assertFalse("Should not be moving", state.isMoving)
        assertTrue("Should be detecting", state.isDetecting)
        assertEquals(1, state.speedBelowThresholdCount)
        assertNotNull("Detection start time should be set", state.detectionStartTime)
    }

    @Test
    fun `Detecting resets to Moving if speed rises before 3 seconds`() = runTest {
        stateMachine.startRide(rideId)

        // First speed below threshold
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L)

        // Second speed below threshold
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 2000L)

        // Speed rises above threshold (reset to moving)
        stateMachine.processSpeed(5f, testLatitude, testLongitude, 3000L)

        val state = stateMachine.getCurrentState()

        assertTrue("Should be moving", state.isMoving)
        assertFalse("Should not be detecting", state.isDetecting)
        assertEquals(0, state.speedBelowThresholdCount)
        assertNull("Detection start time should be reset", state.detectionStartTime)
    }

    @Test
    fun `Detecting transitions to Confirmed after 3 consecutive seconds and duration met`() = runTest {
        stateMachine.startRide(rideId)

        val startTime = 1000L

        // Simulate 15 seconds at 2 km/h (below threshold)
        for (i in 0..14) {
            stateMachine.processSpeed(
                speedKmh = 2f,
                latitude = testLatitude,
                longitude = testLongitude,
                timestamp = startTime + (i * 1000)
            )
        }

        // Wait for async database insert
        testScope.testScheduler.advanceUntilIdle()

        val state = stateMachine.getCurrentState()

        assertTrue("Should be stopped", state.isStopped)
        assertFalse("Should not be moving", state.isMoving)
        assertNotNull("Active stop ID should be set", state.activeStopId)
        assertEquals(1L, state.activeStopId)

        // Verify database insert was called
        coVerify(exactly = 1) {
            stopRepository.insertStop(match {
                it.rideId == rideId &&
                        it.stopNumber == 1 &&
                        it.latitude == testLatitude &&
                        it.longitude == testLongitude &&
                        it.startTimestamp == startTime &&
                        it.endTimestamp == null &&
                        it.durationSeconds == null
            })
        }
    }

    @Test
    fun `does not confirm stop if duration threshold not met`() = runTest {
        stateMachine.startRide(rideId)

        val startTime = 1000L

        // Simulate only 10 seconds at 2 km/h (below 15-second duration threshold)
        for (i in 0..9) {
            stateMachine.processSpeed(
                speedKmh = 2f,
                latitude = testLatitude,
                longitude = testLongitude,
                timestamp = startTime + (i * 1000)
            )
        }

        testScope.testScheduler.advanceUntilIdle()

        val state = stateMachine.getCurrentState()

        assertFalse("Should not be stopped", state.isStopped)
        assertTrue("Should still be detecting", state.isDetecting)
        assertNull("Active stop ID should be null", state.activeStopId)

        // Verify database insert was NOT called
        coVerify(exactly = 0) { stopRepository.insertStop(any()) }
    }

    @Test
    fun `Confirmed transitions to Moving after 3 consecutive seconds above threshold`() = runTest {
        stateMachine.startRide(rideId)

        val startTime = 1000L

        // Confirm stop (15 seconds at 2 km/h)
        for (i in 0..14) {
            stateMachine.processSpeed(2f, testLatitude, testLongitude, startTime + (i * 1000))
        }

        testScope.testScheduler.advanceUntilIdle()

        // Verify stop is confirmed
        assertTrue("Stop should be confirmed", stateMachine.getCurrentState().isStopped)

        // Resume movement (3 consecutive seconds above threshold)
        val resumeTime = startTime + 16000L
        for (i in 0..2) {
            stateMachine.processSpeed(
                speedKmh = 5f, // Above 3 km/h threshold
                latitude = testLatitude,
                longitude = testLongitude,
                timestamp = resumeTime + (i * 1000)
            )
        }

        testScope.testScheduler.advanceUntilIdle()

        val state = stateMachine.getCurrentState()

        assertTrue("Should be moving", state.isMoving)
        assertFalse("Should not be stopped", state.isStopped)
        assertNull("Active stop ID should be reset", state.activeStopId)
        assertEquals(2, state.currentStopNumber) // Incremented for next stop

        // Verify database update was called
        coVerify(exactly = 1) {
            stopRepository.updateStopEnd(
                stopId = 1L,
                endTimestamp = any(),
                durationSeconds = any()
            )
        }
    }

    // ==================== Consecutive Seconds Filtering Tests ====================

    @Test
    fun `consecutive seconds counter increments correctly`() = runTest {
        stateMachine.startRide(rideId)

        // First reading below threshold
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L)
        assertEquals(1, stateMachine.getCurrentState().speedBelowThresholdCount)

        // Second reading below threshold
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 2000L)
        assertEquals(2, stateMachine.getCurrentState().speedBelowThresholdCount)

        // Third reading below threshold
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 3000L)
        assertEquals(3, stateMachine.getCurrentState().speedBelowThresholdCount)
    }

    @Test
    fun `consecutive seconds counter caps at 3`() = runTest {
        stateMachine.startRide(rideId)

        // 5 readings below threshold
        for (i in 0..4) {
            stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L + (i * 1000))
        }

        // Counter should cap at 3 (MAX_CONSECUTIVE_SECONDS)
        val count = stateMachine.getCurrentState().speedBelowThresholdCount
        assertTrue("Counter should be capped at 3", count <= 3)
    }

    @Test
    fun `consecutive seconds counter resets on speed above threshold`() = runTest {
        stateMachine.startRide(rideId)

        // Two readings below threshold
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L)
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 2000L)
        assertEquals(2, stateMachine.getCurrentState().speedBelowThresholdCount)

        // Speed rises above threshold (reset counter)
        stateMachine.processSpeed(5f, testLatitude, testLongitude, 3000L)
        assertEquals(0, stateMachine.getCurrentState().speedBelowThresholdCount)
    }

    @Test
    fun `resume counter increments during stop`() = runTest {
        stateMachine.startRide(rideId)

        // Confirm stop
        for (i in 0..14) {
            stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L + (i * 1000))
        }
        testScope.testScheduler.advanceUntilIdle()

        assertTrue("Stop should be confirmed", stateMachine.getCurrentState().isStopped)

        // First reading above threshold
        stateMachine.processSpeed(5f, testLatitude, testLongitude, 20000L)
        assertEquals(1, stateMachine.getCurrentState().speedAboveThresholdCount)

        // Second reading above threshold
        stateMachine.processSpeed(5f, testLatitude, testLongitude, 21000L)
        assertEquals(2, stateMachine.getCurrentState().speedAboveThresholdCount)
    }

    @Test
    fun `resume counter resets if speed drops during stop`() = runTest {
        stateMachine.startRide(rideId)

        // Confirm stop
        for (i in 0..14) {
            stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L + (i * 1000))
        }
        testScope.testScheduler.advanceUntilIdle()

        // Two readings above threshold
        stateMachine.processSpeed(5f, testLatitude, testLongitude, 20000L)
        stateMachine.processSpeed(5f, testLatitude, testLongitude, 21000L)
        assertEquals(2, stateMachine.getCurrentState().speedAboveThresholdCount)

        // Speed drops below threshold again (reset resume counter)
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 22000L)
        assertEquals(0, stateMachine.getCurrentState().speedAboveThresholdCount)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `multiple stops increment stop number correctly`() = runTest {
        stateMachine.startRide(rideId)

        // First stop
        for (i in 0..14) {
            stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L + (i * 1000))
        }
        testScope.testScheduler.advanceUntilIdle()

        // Resume
        for (i in 0..2) {
            stateMachine.processSpeed(5f, testLatitude, testLongitude, 20000L + (i * 1000))
        }
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(2, stateMachine.getCurrentState().currentStopNumber)

        // Second stop
        for (i in 0..14) {
            stateMachine.processSpeed(2f, testLatitude, testLongitude, 30000L + (i * 1000))
        }
        testScope.testScheduler.advanceUntilIdle()

        // Resume
        for (i in 0..2) {
            stateMachine.processSpeed(5f, testLatitude, testLongitude, 50000L + (i * 1000))
        }
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(3, stateMachine.getCurrentState().currentStopNumber)

        // Verify 2 stops were inserted
        coVerify(exactly = 2) { stopRepository.insertStop(any()) }
    }

    @Test
    fun `manual pause ends active stop immediately`() = runTest {
        stateMachine.startRide(rideId)

        // Confirm stop
        for (i in 0..14) {
            stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L + (i * 1000))
        }
        testScope.testScheduler.advanceUntilIdle()

        assertTrue("Stop should be confirmed", stateMachine.getCurrentState().isStopped)

        // Manual pause
        stateMachine.handleManualPause()
        testScope.testScheduler.advanceUntilIdle()

        assertFalse("Stop should be ended", stateMachine.getCurrentState().isStopped)
        assertNull("Active stop ID should be null", stateMachine.getCurrentState().activeStopId)

        // Verify database update was called
        coVerify(exactly = 1) { stopRepository.updateStopEnd(1L, any(), any()) }
    }

    @Test
    fun `manual pause during detection resets state`() = runTest {
        stateMachine.startRide(rideId)

        // Start detection (2 seconds below threshold)
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L)
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 2000L)

        assertTrue("Should be detecting", stateMachine.getCurrentState().isDetecting)

        // Manual pause
        stateMachine.handleManualPause()

        assertFalse("Should not be detecting", stateMachine.getCurrentState().isDetecting)
        assertEquals(0, stateMachine.getCurrentState().speedBelowThresholdCount)
        assertNull("Detection start time should be reset", stateMachine.getCurrentState().detectionStartTime)
    }

    @Test
    fun `stopRide ends active stop`() = runTest {
        stateMachine.startRide(rideId)

        // Confirm stop
        for (i in 0..14) {
            stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L + (i * 1000))
        }
        testScope.testScheduler.advanceUntilIdle()

        assertTrue("Stop should be confirmed", stateMachine.getCurrentState().isStopped)

        // Stop ride
        stateMachine.stopRide()
        testScope.testScheduler.advanceUntilIdle()

        assertFalse("Stop should be ended", stateMachine.getCurrentState().isStopped)
        assertNull("Active stop ID should be null", stateMachine.getCurrentState().activeStopId)

        // Verify database update was called
        coVerify(exactly = 1) { stopRepository.updateStopEnd(1L, any(), any()) }
    }

    @Test
    fun `processSpeed ignores readings when no ride active`() = runTest {
        // Don't call startRide()

        stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L)

        // State should remain initial
        val state = stateMachine.getCurrentState()
        assertTrue("Should be in initial state", state.isMoving)
        assertEquals(0, state.speedBelowThresholdCount)
    }

    @Test
    fun `rapid speed oscillations around threshold filtered correctly`() = runTest {
        stateMachine.startRide(rideId)

        // Simulate GPS noise: speed oscillates around threshold
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 1000L) // Below
        stateMachine.processSpeed(4f, testLatitude, testLongitude, 2000L) // Above (reset)
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 3000L) // Below
        stateMachine.processSpeed(4f, testLatitude, testLongitude, 4000L) // Above (reset)
        stateMachine.processSpeed(2f, testLatitude, testLongitude, 5000L) // Below

        // Should be detecting but counter should be 1 (not 3)
        val state = stateMachine.getCurrentState()
        assertTrue("Should be detecting", state.isDetecting)
        assertEquals(1, state.speedBelowThresholdCount)

        // Never reached 3 consecutive seconds, so no stop confirmed
        testScope.testScheduler.advanceUntilIdle()
        coVerify(exactly = 0) { stopRepository.insertStop(any()) }
    }
}
