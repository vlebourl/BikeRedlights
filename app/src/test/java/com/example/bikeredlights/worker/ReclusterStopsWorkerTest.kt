package com.example.bikeredlights.worker

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.settings.StopDetectionConfig
import com.example.bikeredlights.domain.usecase.ClusterStopsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ReclusterStopsWorker (Feature 010).
 *
 * Tests verify WorkManager worker integrates correctly with use case.
 */
class ReclusterStopsWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var clusterStopsUseCase: ClusterStopsUseCase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var worker: ReclusterStopsWorker

    @Before
    fun setup() {
        // Mock android.util.Log for unit tests
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
        every { Log.d(any(), any()) } returns 0

        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        clusterStopsUseCase = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        worker = ReclusterStopsWorker(
            context = context,
            params = workerParams,
            clusterStopsUseCase = clusterStopsUseCase,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `doWork fetches current radius and triggers clustering`() = runTest {
        // Given: Current clustering radius is 30m
        val config = StopDetectionConfig(
            speedThresholdKmh = 2f,
            durationThresholdSeconds = 10,
            clusteringRadiusMeters = 30
        )
        every { settingsRepository.stopDetectionConfig } returns flowOf(config)
        coEvery { clusterStopsUseCase.invoke(any(), any()) } just Runs

        // When: Worker executes
        val result = worker.doWork()

        // Then: Use case called with current radius
        coVerify(exactly = 1) {
            clusterStopsUseCase.invoke(
                epsilonMeters = 30f,
                minPts = 3
            )
        }

        // And: Worker returns success
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork returns retry on clustering failure`() = runTest {
        // Given: Clustering will fail
        val config = StopDetectionConfig(
            speedThresholdKmh = 2f,
            durationThresholdSeconds = 10,
            clusteringRadiusMeters = 20
        )
        every { settingsRepository.stopDetectionConfig } returns flowOf(config)
        coEvery { clusterStopsUseCase.invoke(any(), any()) } throws RuntimeException("Database locked")

        // When: Worker executes
        val result = worker.doWork()

        // Then: Worker returns retry (for WorkManager exponential backoff)
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `doWork uses default radius from config`() = runTest {
        // Given: Config with default radius (20m)
        val config = StopDetectionConfig(
            speedThresholdKmh = 2f,
            durationThresholdSeconds = 10,
            clusteringRadiusMeters = StopDetectionConfig.DEFAULT_CLUSTERING_RADIUS_METERS
        )
        every { settingsRepository.stopDetectionConfig } returns flowOf(config)
        coEvery { clusterStopsUseCase.invoke(any(), any()) } just Runs

        // When
        val result = worker.doWork()

        // Then: Use case called with default 20m radius
        coVerify(exactly = 1) {
            clusterStopsUseCase.invoke(
                epsilonMeters = 20f,
                minPts = 3
            )
        }
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }
}
