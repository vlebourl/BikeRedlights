package com.example.bikeredlights.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.bikeredlights.data.preferences.PreferencesKeys
import com.example.bikeredlights.domain.model.history.SortPreference
import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.example.bikeredlights.domain.model.settings.StopDetectionConfig
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.worker.ReclusterStopsWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore extension property for Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Implementation of SettingsRepository using DataStore Preferences.
 *
 * All settings are stored locally on the device.
 * Read failures return default values.
 * Write failures are logged but don't crash the app (graceful degradation).
 *
 * **Feature 010: Stop Clustering**:
 * - Monitors clustering radius changes and triggers re-clustering via WorkManager
 * - WorkManager ensures background re-clustering completes even if app is killed
 *
 * @param context Android context for DataStore access and WorkManager
 */
class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    companion object {
        private const val TAG = "SettingsRepository"
    }

    // WorkManager instance for background re-clustering (Feature 010)
    private val workManager = WorkManager.getInstance(context)

    override val unitsSystem: Flow<UnitsSystem> = context.dataStore.data
        .catch { exception ->
            // Handle read errors gracefully
            if (exception is IOException) {
                Log.e(TAG, "Error reading units system preference", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = preferences[PreferencesKeys.UNITS_SYSTEM]
            UnitsSystem.fromString(value)
        }

    override val gpsAccuracy: Flow<GpsAccuracy> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading GPS accuracy preference", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = preferences[PreferencesKeys.GPS_ACCURACY]
            GpsAccuracy.fromString(value)
        }

    override val autoPauseConfig: Flow<AutoPauseConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading auto-pause preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val enabled = preferences[PreferencesKeys.AUTO_PAUSE_ENABLED] ?: false
            val seconds = preferences[PreferencesKeys.AUTO_PAUSE_SECONDS] ?: 30
            AutoPauseConfig.fromDataStore(enabled, seconds)
        }

    override suspend fun setUnitsSystem(units: UnitsSystem) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.UNITS_SYSTEM] = units.toDataStoreValue()
            }
            Log.d(TAG, "Units system updated to: $units")
        } catch (e: IOException) {
            Log.e(TAG, "Error writing units system preference", e)
            // Don't rethrow - graceful degradation
        }
    }

    override suspend fun setGpsAccuracy(accuracy: GpsAccuracy) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GPS_ACCURACY] = accuracy.toDataStoreValue()
            }
            Log.d(TAG, "GPS accuracy updated to: $accuracy")
        } catch (e: IOException) {
            Log.e(TAG, "Error writing GPS accuracy preference", e)
        }
    }

    override suspend fun setAutoPauseConfig(config: AutoPauseConfig) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_PAUSE_ENABLED] = config.enabled
                preferences[PreferencesKeys.AUTO_PAUSE_SECONDS] = config.thresholdSeconds
            }
            Log.d(TAG, "Auto-pause config updated: enabled=${config.enabled}, threshold=${config.thresholdSeconds}s")
        } catch (e: IOException) {
            Log.e(TAG, "Error writing auto-pause preferences", e)
        }
    }

    override val rideSortPreference: Flow<SortPreference> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading ride sort preference", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = preferences[PreferencesKeys.RIDE_SORT_PREFERENCE]
            SortPreference.fromString(value)
        }

    override suspend fun setRideSortPreference(sortPreference: SortPreference) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RIDE_SORT_PREFERENCE] = sortPreference.name
            }
            Log.d(TAG, "Ride sort preference updated to: $sortPreference")
        } catch (e: IOException) {
            Log.e(TAG, "Error writing ride sort preference", e)
        }
    }

    // Feature 008 - Stop Detection Settings (v0.8.0)

    override val stopDetectionConfig: Flow<StopDetectionConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading stop detection preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val speedThresholdKmh = preferences[PreferencesKeys.STOP_DETECTION_SPEED_THRESHOLD_KMH]
                ?: StopDetectionConfig.DEFAULT_SPEED_THRESHOLD_KMH
            val durationThresholdSeconds = preferences[PreferencesKeys.STOP_DETECTION_DURATION_THRESHOLD_SECONDS]
                ?: StopDetectionConfig.DEFAULT_DURATION_THRESHOLD_SECONDS
            val clusteringRadiusMeters = preferences[PreferencesKeys.STOP_DETECTION_CLUSTERING_RADIUS_METERS]
                ?: StopDetectionConfig.DEFAULT_CLUSTERING_RADIUS_METERS

            // StopDetectionConfig validates values in init block
            StopDetectionConfig(
                speedThresholdKmh = speedThresholdKmh,
                durationThresholdSeconds = durationThresholdSeconds,
                clusteringRadiusMeters = clusteringRadiusMeters
            )
        }

    override suspend fun setStopDetectionConfig(config: StopDetectionConfig) {
        try {
            // Check if clustering radius changed (Feature 010: triggers re-clustering)
            val oldRadius = stopDetectionConfig.first().clusteringRadiusMeters
            val radiusChanged = oldRadius != config.clusteringRadiusMeters

            // Atomic write of all 3 settings in single transaction
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.STOP_DETECTION_SPEED_THRESHOLD_KMH] = config.speedThresholdKmh
                preferences[PreferencesKeys.STOP_DETECTION_DURATION_THRESHOLD_SECONDS] = config.durationThresholdSeconds
                preferences[PreferencesKeys.STOP_DETECTION_CLUSTERING_RADIUS_METERS] = config.clusteringRadiusMeters
            }
            Log.d(TAG, "Stop detection config updated: speed=${config.speedThresholdKmh}km/h, " +
                    "duration=${config.durationThresholdSeconds}s, radius=${config.clusteringRadiusMeters}m")

            // Feature 010: Trigger background re-clustering if radius changed
            if (radiusChanged) {
                triggerReclusteringWorker()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error writing stop detection preferences", e)
        }
    }

    /**
     * Enqueue WorkManager task to re-cluster all stops (Feature 010).
     *
     * **Work Policy**: REPLACE (cancel old work if pending, replace with new)
     * - If work already queued → cancel and start fresh
     * - If work running → let it finish (ignore new request)
     *
     * **Constraints**: None (runs immediately on WiFi/cellular)
     * **Backoff**: EXPONENTIAL (30s, 60s, 120s... on retry)
     *
     * Private helper method called by setStopDetectionConfig when radius changes.
     */
    private fun triggerReclusteringWorker() {
        val reclusterWork = OneTimeWorkRequestBuilder<ReclusterStopsWorker>().build()

        workManager.enqueueUniqueWork(
            ReclusterStopsWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            reclusterWork
        )

        Log.d(TAG, "Enqueued background re-clustering worker")
    }
}
