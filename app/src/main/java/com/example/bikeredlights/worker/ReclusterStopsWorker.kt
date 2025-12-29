package com.example.bikeredlights.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bikeredlights.data.preferences.PreferencesKeys
import com.example.bikeredlights.domain.usecase.ClusterStopsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker for background stop re-clustering (Feature 010).
 *
 * **Purpose**: Re-cluster all stops when clustering radius setting changes.
 *
 * **Trigger**: SettingsRepository observes STOP_DETECTION_CLUSTERING_RADIUS_METERS
 * and enqueues this worker when value changes.
 *
 * **Work Type**: One-time work (not periodic)
 * - Constraints: None (runs immediately on WiFi/cellular)
 * - Backoff policy: EXPONENTIAL (30s, 60s, 120s... on failure)
 * - Cancellation: User can't cancel (invisible to user)
 *
 * **Performance**:
 * - Expected: <2s for 100 stops, <10s for 1000 stops
 * - Battery impact: Minimal (one-shot operation)
 * - Network: None (all local processing)
 *
 * **Error Handling**:
 * - Success: Returns Result.success()
 * - Failure: Returns Result.retry() (WorkManager handles backoff)
 * - Permanent failure after 3 retries: Returns Result.failure()
 *
 * **Thread Safety**:
 * - Runs on background thread (CoroutineWorker)
 * - Database access is safe (Room + coroutines)
 * - No UI updates (fire-and-forget)
 *
 * **Testing**:
 * - Unit tests: Mock ClusterStopsUseCase
 * - Integration tests: WorkManager TestDriver
 *
 * @param context Application context (provided by WorkManager)
 * @param params Worker parameters (provided by WorkManager)
 * @param clusterStopsUseCase Domain use case for clustering logic (Hilt injected)
 */
@HiltWorker
class ReclusterStopsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val clusterStopsUseCase: ClusterStopsUseCase,
    private val settingsRepository: com.example.bikeredlights.data.repository.SettingsRepository
) : CoroutineWorker(context, params) {

    /**
     * Execute background re-clustering work.
     *
     * **Algorithm**:
     * 1. Fetch current clustering radius from settings
     * 2. Call ClusterStopsUseCase with updated epsilon
     * 3. Use case handles: fetch all stops → cluster → update cluster IDs
     *
     * **Performance**: Synchronous execution (blocks worker thread)
     * - Acceptable: Worker is designed for background execution
     * - No progress updates (invisible to user)
     *
     * @return Result.success() if clustering completes successfully
     * @return Result.retry() if transient error (e.g., database locked)
     * @return Result.failure() if permanent error after retries
     */
    override suspend fun doWork(): Result {
        return try {
            // Step 1: Fetch current clustering radius from settings
            val radiusMeters = settingsRepository.stopDetectionConfig.first().clusteringRadiusMeters

            // Step 2: Re-cluster all stops with updated radius
            clusterStopsUseCase.invoke(
                epsilonMeters = radiusMeters.toFloat(),
                minPts = 3 // Fixed minPts for 2D geospatial clustering
            )

            // Success: All stops re-clustered
            Result.success()
        } catch (e: Exception) {
            // Log error for debugging (visible in Logcat)
            android.util.Log.e("ReclusterStopsWorker", "Re-clustering failed", e)

            // Retry on transient errors (database lock, out of memory, etc.)
            // WorkManager will apply exponential backoff: 30s, 60s, 120s...
            Result.retry()
        }
    }

    companion object {
        /**
         * Unique work name for re-clustering (ensures only one instance queued).
         *
         * SettingsRepository uses enqueueUniqueWork() with REPLACE policy:
         * - If work already queued → cancel old, start new
         * - If work running → let finish, ignore new request
         */
        const val WORK_NAME = "recluster_stops"
    }
}
