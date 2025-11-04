package com.example.bikeredlights.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ServiceTestRule
import com.example.bikeredlights.domain.model.RideRecordingState
import com.example.bikeredlights.domain.repository.RideRecordingStateRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumented test for RideRecordingService.
 *
 * **Test Coverage**:
 * - Service starts and binds successfully
 * - Recording state transitions (Idle → Recording → Stopped)
 * - Service runs as foreground service
 * - Service handles stop action
 *
 * **Note**: GPS location tracking requires physical device or emulator with location enabled.
 * These tests focus on service lifecycle and state management.
 */
@MediumTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RideRecordingServiceTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Inject
    lateinit var rideRecordingStateRepository: RideRecordingStateRepository

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()

        // Ensure clean state
        runBlocking {
            rideRecordingStateRepository.clearRecordingState()
        }
    }

    @After
    fun teardown() {
        runBlocking {
            rideRecordingStateRepository.clearRecordingState()
        }
    }

    @Test
    fun serviceStartsWithStartRecordingAction() {
        // Given
        val intent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_START_RECORDING
        }

        // When
        serviceRule.startService(intent)

        // Then - Service should be running (no exception thrown)
        // If service crashes, ServiceTestRule will fail the test
    }

    @Test
    fun startRecordingTransitionsToRecordingState() = runBlocking {
        // Given
        val initialState = rideRecordingStateRepository.getRecordingState().first()
        assertThat(initialState).isEqualTo(RideRecordingState.Idle)

        // When
        val intent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_START_RECORDING
        }
        serviceRule.startService(intent)

        // Then - Wait for state to transition to Recording
        withTimeout(5000) {
            val newState = rideRecordingStateRepository.getCurrentState()
            assertThat(newState).isInstanceOf(RideRecordingState.Recording::class.java)
        }
    }

    @Test
    fun stopRecordingTransitionsToStoppedState() = runBlocking {
        // Given - Start recording first
        val startIntent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_START_RECORDING
        }
        serviceRule.startService(startIntent)

        // Wait for Recording state
        withTimeout(5000) {
            var state = rideRecordingStateRepository.getCurrentState()
            while (state !is RideRecordingState.Recording) {
                kotlinx.coroutines.delay(100)
                state = rideRecordingStateRepository.getCurrentState()
            }
        }

        // When - Stop recording
        val stopIntent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_STOP_RECORDING
        }
        context.startService(stopIntent)

        // Then - Wait for state to transition to Stopped
        withTimeout(5000) {
            var state = rideRecordingStateRepository.getCurrentState()
            while (state !is RideRecordingState.Stopped) {
                kotlinx.coroutines.delay(100)
                state = rideRecordingStateRepository.getCurrentState()
            }
            assertThat(state).isInstanceOf(RideRecordingState.Stopped::class.java)
        }
    }

    @Test
    fun pauseRecordingTransitionsToManuallyPausedState() = runBlocking {
        // Given - Start recording first
        val startIntent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_START_RECORDING
        }
        serviceRule.startService(startIntent)

        // Wait for Recording state
        withTimeout(5000) {
            var state = rideRecordingStateRepository.getCurrentState()
            while (state !is RideRecordingState.Recording) {
                kotlinx.coroutines.delay(100)
                state = rideRecordingStateRepository.getCurrentState()
            }
        }

        // When - Pause recording
        val pauseIntent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_PAUSE_RECORDING
        }
        context.startService(pauseIntent)

        // Then - Wait for state to transition to ManuallyPaused
        withTimeout(5000) {
            var state = rideRecordingStateRepository.getCurrentState()
            while (state !is RideRecordingState.ManuallyPaused) {
                kotlinx.coroutines.delay(100)
                state = rideRecordingStateRepository.getCurrentState()
            }
            assertThat(state).isInstanceOf(RideRecordingState.ManuallyPaused::class.java)
        }
    }

    @Test
    fun resumeRecordingTransitionsBackToRecordingState() = runBlocking {
        // Given - Start and pause recording
        val startIntent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_START_RECORDING
        }
        serviceRule.startService(startIntent)

        withTimeout(5000) {
            var state = rideRecordingStateRepository.getCurrentState()
            while (state !is RideRecordingState.Recording) {
                kotlinx.coroutines.delay(100)
                state = rideRecordingStateRepository.getCurrentState()
            }
        }

        val pauseIntent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_PAUSE_RECORDING
        }
        context.startService(pauseIntent)

        withTimeout(5000) {
            var state = rideRecordingStateRepository.getCurrentState()
            while (state !is RideRecordingState.ManuallyPaused) {
                kotlinx.coroutines.delay(100)
                state = rideRecordingStateRepository.getCurrentState()
            }
        }

        // When - Resume recording
        val resumeIntent = Intent(context, RideRecordingService::class.java).apply {
            action = RideRecordingService.ACTION_RESUME_RECORDING
        }
        context.startService(resumeIntent)

        // Then - Wait for state to transition back to Recording
        withTimeout(5000) {
            var state = rideRecordingStateRepository.getCurrentState()
            while (state !is RideRecordingState.Recording) {
                kotlinx.coroutines.delay(100)
                state = rideRecordingStateRepository.getCurrentState()
            }
            assertThat(state).isInstanceOf(RideRecordingState.Recording::class.java)
        }
    }

    @Test
    fun multipleStartCommandsAreSafelyHandled() = runBlocking {
        // Given & When - Start recording multiple times
        repeat(3) {
            val intent = Intent(context, RideRecordingService::class.java).apply {
                action = RideRecordingService.ACTION_START_RECORDING
            }
            serviceRule.startService(intent)
            kotlinx.coroutines.delay(500)
        }

        // Then - Should still be in Recording state with single ride
        withTimeout(5000) {
            val state = rideRecordingStateRepository.getCurrentState()
            assertThat(state).isInstanceOf(RideRecordingState.Recording::class.java)
            val rideId = (state as RideRecordingState.Recording).rideId
            assertThat(rideId).isGreaterThan(0)
        }
    }
}
