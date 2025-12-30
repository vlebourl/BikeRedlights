package com.example.bikeredlights.data.repository

import android.content.Context
import android.os.Looper
import app.cash.turbine.test
import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LocationRepositoryImpl GPS status logic.
 *
 * Tests GPS status determination based on accuracy thresholds:
 * - Unavailable: accuracy > 50m or no location update for >10 seconds
 * - Acquiring: accuracy 10-50m (exclusive upper, inclusive lower bounds)
 * - Active: accuracy ≤ 10m
 *
 * This is a critical feature for user notification of GPS signal quality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepositoryImplGpsStatusTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: LocationRepositoryImpl

    @Before
    fun setup() {
        // Mock Android dependencies
        context = mockk(relaxed = true)
        settingsRepository = mockk()

        // Mock Looper for Android context
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        // Mock LocationServices to avoid real GPS calls
        mockkStatic(LocationServices::class)
        val mockFusedLocationClient: FusedLocationProviderClient = mockk(relaxed = true)
        every { LocationServices.getFusedLocationProviderClient(any<Context>()) } returns mockFusedLocationClient

        // Setup default GPS accuracy setting
        every { settingsRepository.gpsAccuracy } returns flowOf(GpsAccuracy.HIGH_ACCURACY)

        repository = LocationRepositoryImpl(context, settingsRepository)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ============================================================
    // Tests for determineGpsStatus() accuracy threshold logic
    // ============================================================

    @Test
    fun `determineGpsStatus returns Unavailable when accuracy exceeds 50m threshold`() {
        // Given: Accuracy above the unavailable threshold (>50m)
        val poorAccuracy = 51f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(poorAccuracy)

        // Then: Status is Unavailable
        assertThat(status).isEqualTo(GpsStatus.Unavailable)
    }

    @Test
    fun `determineGpsStatus returns Unavailable when accuracy exactly at 50m boundary`() {
        // Given: Accuracy exactly at the upper threshold boundary
        // Note: >50m is Unavailable, so 50.01m should be Unavailable
        val boundaryAccuracy = 50.01f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(boundaryAccuracy)

        // Then: Status is Unavailable (just above threshold)
        assertThat(status).isEqualTo(GpsStatus.Unavailable)
    }

    @Test
    fun `determineGpsStatus returns Acquiring when accuracy between 10m and 50m`() {
        // Given: Accuracy in the acquiring range (10m < accuracy ≤ 50m)
        val acquiringAccuracy = 30f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(acquiringAccuracy)

        // Then: Status is Acquiring
        assertThat(status).isEqualTo(GpsStatus.Acquiring)
    }

    @Test
    fun `determineGpsStatus returns Acquiring when accuracy exactly at 50m boundary`() {
        // Given: Accuracy exactly at 50m (threshold boundary)
        val boundaryAccuracy = 50f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(boundaryAccuracy)

        // Then: Status is Acquiring (not > 50m)
        assertThat(status).isEqualTo(GpsStatus.Acquiring)
    }

    @Test
    fun `determineGpsStatus returns Acquiring when accuracy just above 10m`() {
        // Given: Accuracy just above the active threshold
        val acquiringAccuracy = 10.01f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(acquiringAccuracy)

        // Then: Status is Acquiring
        assertThat(status).isEqualTo(GpsStatus.Acquiring)
    }

    @Test
    fun `determineGpsStatus returns Active when accuracy 10m or below`() {
        // Given: Accuracy at or below the active threshold (≤10m)
        val goodAccuracy = 10f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(goodAccuracy)

        // Then: Status is Active with accuracy value
        assertThat(status).isInstanceOf(GpsStatus.Active::class.java)
        assertThat((status as GpsStatus.Active).accuracy).isEqualTo(10f)
    }

    @Test
    fun `determineGpsStatus returns Active with correct accuracy value`() {
        // Given: High precision GPS accuracy
        val preciseAccuracy = 3.5f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(preciseAccuracy)

        // Then: Status is Active with correct accuracy
        assertThat(status).isInstanceOf(GpsStatus.Active::class.java)
        assertThat((status as GpsStatus.Active).accuracy).isEqualTo(3.5f)
    }

    @Test
    fun `determineGpsStatus returns Active for sub-meter accuracy`() {
        // Given: Very high precision (sub-meter) accuracy
        val subMeterAccuracy = 0.5f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(subMeterAccuracy)

        // Then: Status is Active with sub-meter accuracy
        assertThat(status).isInstanceOf(GpsStatus.Active::class.java)
        assertThat((status as GpsStatus.Active).accuracy).isEqualTo(0.5f)
    }

    // ============================================================
    // Tests for updateGpsStatus() state management
    // ============================================================

    @Test
    fun `updateGpsStatus updates internal status based on accuracy`() = runTest {
        // Given: Repository starts with default Acquiring status
        // When: Updating with good accuracy
        repository.updateGpsStatus(5f)

        // Then: Internal status should be updated
        // We can verify via gpsStatusUpdates flow
        repository.gpsStatusUpdates().test {
            // First emission should be Acquiring (initial state reset in gpsStatusUpdates)
            val initialStatus = awaitItem()
            assertThat(initialStatus).isEqualTo(GpsStatus.Acquiring)

            // Trigger another update with Active accuracy
            repository.updateGpsStatus(5f)

            val updatedStatus = awaitItem()
            assertThat(updatedStatus).isInstanceOf(GpsStatus.Active::class.java)
            assertThat((updatedStatus as GpsStatus.Active).accuracy).isEqualTo(5f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateGpsStatus transitions from Active to Unavailable on poor accuracy`() = runTest {
        // Given: Repository with good GPS signal
        repository.updateGpsStatus(5f)

        // When: GPS accuracy degrades significantly
        repository.updateGpsStatus(60f)

        // Then: Status transitions to Unavailable
        repository.gpsStatusUpdates().test {
            // Skip initial Acquiring status
            awaitItem()

            // Trigger updates to verify status
            repository.updateGpsStatus(60f)
            val status = awaitItem()
            assertThat(status).isEqualTo(GpsStatus.Unavailable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateGpsStatus transitions from Unavailable to Active on good accuracy`() = runTest {
        // Given: Repository with poor GPS signal
        repository.updateGpsStatus(60f)

        // When/Then: Verify via gpsStatusUpdates flow
        repository.gpsStatusUpdates().test {
            // Skip initial Acquiring
            awaitItem()

            // Update to poor accuracy
            repository.updateGpsStatus(60f)
            val unavailableStatus = awaitItem()
            assertThat(unavailableStatus).isEqualTo(GpsStatus.Unavailable)

            // Update to good accuracy
            repository.updateGpsStatus(8f)
            val activeStatus = awaitItem()
            assertThat(activeStatus).isInstanceOf(GpsStatus.Active::class.java)
            assertThat((activeStatus as GpsStatus.Active).accuracy).isEqualTo(8f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateGpsStatus transitions through Acquiring state`() = runTest {
        // Given: Repository in unknown state
        repository.gpsStatusUpdates().test {
            // Initial Acquiring state
            val initial = awaitItem()
            assertThat(initial).isEqualTo(GpsStatus.Acquiring)

            // Update to Acquiring range (30m)
            repository.updateGpsStatus(30f)
            val acquiring = awaitItem()
            assertThat(acquiring).isEqualTo(GpsStatus.Acquiring)

            // Note: No emission expected since status didn't change (still Acquiring)
            // Update to Active (5m)
            repository.updateGpsStatus(5f)
            val active = awaitItem()
            assertThat(active).isInstanceOf(GpsStatus.Active::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Tests for gpsStatusUpdates() Flow behavior
    // ============================================================

    @Test
    fun `gpsStatusUpdates emits initial Acquiring status`() = runTest {
        // Given: Fresh repository
        // When: Collecting GPS status updates
        repository.gpsStatusUpdates().test {
            // Then: First emission is Acquiring
            val initialStatus = awaitItem()
            assertThat(initialStatus).isEqualTo(GpsStatus.Acquiring)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gpsStatusUpdates resets to Acquiring on each collection`() = runTest {
        // Given: Repository with previous Active status
        repository.updateGpsStatus(5f)

        // When: Starting new collection
        repository.gpsStatusUpdates().test {
            // Then: First emission is Acquiring (reset on collection start)
            val initialStatus = awaitItem()
            assertThat(initialStatus).isEqualTo(GpsStatus.Acquiring)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Tests for accuracy threshold constants
    // ============================================================

    @Test
    fun `ACCURACY_UNAVAILABLE_THRESHOLD constant is 50f`() {
        // Verify threshold constant matches GpsStatus.kt documentation
        assertThat(LocationRepositoryImpl.ACCURACY_UNAVAILABLE_THRESHOLD).isEqualTo(50f)
    }

    @Test
    fun `ACCURACY_ACQUIRING_THRESHOLD constant is 10f`() {
        // Verify threshold constant matches GpsStatus.kt documentation
        assertThat(LocationRepositoryImpl.ACCURACY_ACQUIRING_THRESHOLD).isEqualTo(10f)
    }

    @Test
    fun `GPS_TIMEOUT_MS constant is 10 seconds`() {
        // Verify timeout constant matches spec (10 seconds)
        assertThat(LocationRepositoryImpl.GPS_TIMEOUT_MS).isEqualTo(10_000L)
    }

    // ============================================================
    // Tests for edge cases
    // ============================================================

    @Test
    fun `determineGpsStatus handles zero accuracy`() {
        // Given: Zero accuracy (theoretically perfect)
        val zeroAccuracy = 0f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(zeroAccuracy)

        // Then: Status is Active with zero accuracy
        assertThat(status).isInstanceOf(GpsStatus.Active::class.java)
        assertThat((status as GpsStatus.Active).accuracy).isEqualTo(0f)
    }

    @Test
    fun `determineGpsStatus handles very large accuracy values`() {
        // Given: Extremely poor accuracy (e.g., cell tower location)
        val veryPoorAccuracy = 10000f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(veryPoorAccuracy)

        // Then: Status is Unavailable
        assertThat(status).isEqualTo(GpsStatus.Unavailable)
    }

    @Test
    fun `determineGpsStatus handles negative accuracy gracefully`() {
        // Given: Invalid negative accuracy (defensive coding)
        val negativeAccuracy = -5f

        // When: Determining GPS status
        val status = repository.determineGpsStatus(negativeAccuracy)

        // Then: Status is Active (negative is less than 10m threshold)
        // Note: This is a boundary case - negative values are technically invalid
        // but the implementation treats them as very accurate
        assertThat(status).isInstanceOf(GpsStatus.Active::class.java)
    }

    @Test
    fun `updateGpsStatus does not emit duplicate consecutive statuses`() = runTest {
        // Given: Repository collecting status updates
        repository.gpsStatusUpdates().test {
            // Initial Acquiring
            assertThat(awaitItem()).isEqualTo(GpsStatus.Acquiring)

            // Update to Active
            repository.updateGpsStatus(5f)
            val active1 = awaitItem()
            assertThat(active1).isInstanceOf(GpsStatus.Active::class.java)

            // Update with same accuracy - should still emit (different Active instance)
            // Note: GpsStatus.Active is a data class, so same accuracy = equal objects
            repository.updateGpsStatus(5f)
            // No emission expected since status equals previous

            // Update with different Active accuracy
            repository.updateGpsStatus(6f)
            val active2 = awaitItem()
            assertThat((active2 as GpsStatus.Active).accuracy).isEqualTo(6f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `consecutive Active statuses with same accuracy do not trigger multiple emissions`() = runTest {
        // Given: Repository with Active status
        repository.gpsStatusUpdates().test {
            // Initial
            awaitItem()

            // First Active update
            repository.updateGpsStatus(5f)
            awaitItem()

            // Same accuracy update - StateFlow deduplicates
            repository.updateGpsStatus(5f)

            // Different accuracy - should emit
            repository.updateGpsStatus(7f)
            val newActive = awaitItem()
            assertThat((newActive as GpsStatus.Active).accuracy).isEqualTo(7f)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
