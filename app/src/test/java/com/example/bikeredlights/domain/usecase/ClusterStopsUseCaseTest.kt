package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Stop
import com.example.bikeredlights.domain.repository.StopRepository
import com.example.bikeredlights.domain.util.DBSCANAlgorithm
import com.example.bikeredlights.domain.util.ClusteringResult
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ClusterStopsUseCase (Feature 010).
 *
 * Tests verify clustering logic integrates haversineDistance + DBSCAN + repository.
 */
class ClusterStopsUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var dbscanAlgorithm: DBSCANAlgorithm
    private lateinit var useCase: ClusterStopsUseCase

    @Before
    fun setup() {
        stopRepository = mockk(relaxed = true)
        dbscanAlgorithm = mockk()
        useCase = ClusterStopsUseCase(stopRepository, dbscanAlgorithm)
    }

    @Test
    fun `cluster stops with epsilon from parameters`() = runTest {
        // Given: 3 stops at same intersection
        val stops = listOf(
            Stop(id = 1, rideId = 1, stopNumber = 1, latitude = 37.422, longitude = -122.084,
                startTimestamp = 1000, endTimestamp = 11000, durationSeconds = 10, clusterId = null),
            Stop(id = 2, rideId = 2, stopNumber = 1, latitude = 37.4221, longitude = -122.0841,
                startTimestamp = 2000, endTimestamp = 17000, durationSeconds = 15, clusterId = null),
            Stop(id = 3, rideId = 3, stopNumber = 1, latitude = 37.4222, longitude = -122.0842,
                startTimestamp = 3000, endTimestamp = 23000, durationSeconds = 20, clusterId = null)
        )

        coEvery { stopRepository.getAllStops() } returns stops

        // Mock DBSCAN to return one cluster
        every {
            dbscanAlgorithm.cluster(
                pointCount = 3,
                epsilon = 20.0f,
                minPts = 3,
                distanceFunction = any()
            )
        } returns ClusteringResult(
            clusters = mapOf(1 to listOf(0, 1, 2)),
            clusterCount = 1,
            noiseCount = 0
        )

        // When: cluster with epsilon=20m, minPts=3
        useCase.invoke(epsilonMeters = 20.0f, minPts = 3)

        // Then: verify repository methods called
        coVerify(exactly = 1) { stopRepository.getAllStops() }
        coVerify(exactly = 1) {
            stopRepository.updateClusterIds(1, listOf(1L, 2L, 3L))
        }
    }

    @Test
    fun `empty stops returns without error`() = runTest {
        // Given: no stops
        coEvery { stopRepository.getAllStops() } returns emptyList()

        // When
        useCase.invoke(epsilonMeters = 20.0f, minPts = 3)

        // Then: no crash, no repository updates
        coVerify(exactly = 1) { stopRepository.getAllStops() }
        coVerify(exactly = 0) { stopRepository.updateClusterIds(any(), any()) }
    }
}
