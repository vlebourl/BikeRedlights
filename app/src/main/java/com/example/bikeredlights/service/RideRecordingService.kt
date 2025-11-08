package com.example.bikeredlights.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.bikeredlights.MainActivity
import com.example.bikeredlights.R
import com.example.bikeredlights.domain.model.RideRecordingState
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.repository.LocationRepository
import com.example.bikeredlights.domain.repository.RideRecordingStateRepository
import com.example.bikeredlights.domain.repository.RideRepository
import com.example.bikeredlights.domain.repository.TrackPointRepository
import com.example.bikeredlights.domain.usecase.CalculateDistanceUseCase
import com.example.bikeredlights.domain.usecase.RecordTrackPointUseCase
import com.example.bikeredlights.domain.usecase.StartRideUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for ride recording with GPS tracking.
 *
 * **Responsibilities**:
 * - Run as foreground service with notification
 * - Collect GPS location updates
 * - Record track points to database
 * - Calculate distance traveled
 * - Update ride statistics
 * - Handle pause/resume/stop actions
 * - Survive process death (START_STICKY)
 *
 * **Lifecycle**:
 * - START: User taps "Start Ride" button
 * - RUNNING: GPS tracking, recording track points
 * - PAUSED: User manually pauses or auto-paused (low speed)
 * - STOPPED: User taps "Stop Ride", shows save/discard dialog
 * - DESTROYED: Service stopped after ride saved/discarded
 *
 * **Notification**:
 * - Foreground notification with elapsed time
 * - Actions: Pause/Resume, Stop
 * - Channel: "Ride Recording" (importance HIGH)
 *
 * **Process Death Recovery**:
 * - START_STICKY: System restarts service after process death
 * - Recovers state from RideRecordingStateRepository (DataStore)
 * - Resumes GPS tracking for incomplete ride
 *
 * **Permissions**:
 * - ACCESS_FINE_LOCATION: Required for GPS
 * - POST_NOTIFICATIONS: Required for Android 13+ (Tiramisu)
 * - FOREGROUND_SERVICE_LOCATION: Required for foreground service
 */
@AndroidEntryPoint
class RideRecordingService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var rideRepository: RideRepository

    @Inject
    lateinit var trackPointRepository: TrackPointRepository

    @Inject
    lateinit var rideRecordingStateRepository: RideRecordingStateRepository

    @Inject
    lateinit var startRideUseCase: StartRideUseCase

    @Inject
    lateinit var recordTrackPointUseCase: RecordTrackPointUseCase

    @Inject
    lateinit var calculateDistanceUseCase: CalculateDistanceUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var locationJob: Job? = null
    private var durationUpdateJob: Job? = null
    private var gpsAccuracyObserverJob: Job? = null  // T082: Observe GPS accuracy changes
    private var currentRideId: Long? = null
    private var currentState: RideRecordingState = RideRecordingState.Idle
    private var pauseStartTime: Long = 0  // Timestamp when manual pause started (Bug #2 fix)
    private var autoPauseStartTime: Long = 0  // Bug #8: Track auto-pause start time
    private var lastManualResumeTime: Long = 0  // Bug #5: Track manual resume to prevent immediate auto-pause
    private var lowSpeedStartTime: Long = 0  // Bug #9: Track when speed first dropped below threshold

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        // Initialize notification manager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        createNotificationChannel()

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted - notification may not show
                // This should be handled by MainActivity before starting service
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                startRecording()
            }
            ACTION_PAUSE_RECORDING -> {
                pauseRecording()
            }
            ACTION_RESUME_RECORDING -> {
                resumeRecording()
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
            else -> {
                // Handle process death recovery
                recoverFromProcessDeath()
            }
        }

        // START_STICKY: System will restart service if killed
        // Intent will be null on restart (handled by recoverFromProcessDeath)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't support binding
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        locationJob?.cancel()
        durationUpdateJob?.cancel()
        gpsAccuracyObserverJob?.cancel()
    }

    /**
     * Start ride recording.
     *
     * - Creates new ride in database
     * - Starts foreground service with notification
     * - Begins GPS tracking
     * - Updates state to Recording
     */
    private fun startRecording() {
        serviceScope.launch {
            // Create new ride
            val rideId = startRideUseCase()
            currentRideId = rideId

            // Update state to Recording
            currentState = RideRecordingState.Recording(rideId)
            rideRecordingStateRepository.updateRecordingState(currentState)

            // Start foreground service
            val notification = buildNotification("Recording...")
            startForegroundService(notification)

            // Start GPS tracking
            startLocationTracking(rideId)

            // Start duration updates (Bug #1 fix)
            startDurationUpdates(rideId)

            // Start GPS accuracy observer (T082)
            startGpsAccuracyObserver(rideId)
        }
    }

    /**
     * Pause ride recording (manual pause).
     */
    private fun pauseRecording() {
        val rideId = currentRideId ?: return

        serviceScope.launch {
            // Stop duration updates while paused
            durationUpdateJob?.cancel()

            // Stop GPS accuracy observer while paused (T082)
            gpsAccuracyObserverJob?.cancel()

            // Record pause start time (Bug #2 fix)
            pauseStartTime = System.currentTimeMillis()

            // Reset current speed to 0.0 on pause (Feature 005)
            (rideRecordingStateRepository as? com.example.bikeredlights.data.repository.RideRecordingStateRepositoryImpl)
                ?.resetCurrentSpeed()

            currentState = RideRecordingState.ManuallyPaused(rideId)
            rideRecordingStateRepository.updateRecordingState(currentState)

            // Update notification
            val notification = buildNotification("Paused")
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Resume ride recording from manual or auto pause.
     */
    private fun resumeRecording() {
        val rideId = currentRideId ?: return

        serviceScope.launch {
            // Track if resuming from auto-pause (Bug #5)
            val wasAutoPaused = currentState is RideRecordingState.AutoPaused

            // Bug #8: If resuming from auto-pause, accumulate auto-pause duration
            if (wasAutoPaused && autoPauseStartTime > 0) {
                val autoPausedDuration = System.currentTimeMillis() - autoPauseStartTime
                val ride = rideRepository.getRideById(rideId)
                if (ride != null) {
                    val updatedRide = ride.copy(
                        autoPausedDurationMillis = ride.autoPausedDurationMillis + autoPausedDuration
                    )
                    rideRepository.updateRide(updatedRide)
                    android.util.Log.d("RideRecordingService",
                        "Manual resume from auto-pause: accumulated ${autoPausedDuration}ms, total=${updatedRide.autoPausedDurationMillis}ms")
                }
                autoPauseStartTime = 0  // Reset
            }

            // Calculate manual paused duration and accumulate it (Bug #2 fix)
            if (pauseStartTime > 0) {
                val pausedDuration = System.currentTimeMillis() - pauseStartTime
                val ride = rideRepository.getRideById(rideId)
                if (ride != null) {
                    val updatedRide = ride.copy(
                        manualPausedDurationMillis = ride.manualPausedDurationMillis + pausedDuration
                    )
                    rideRepository.updateRide(updatedRide)
                }
                pauseStartTime = 0  // Reset
            }

            currentState = RideRecordingState.Recording(rideId)
            rideRecordingStateRepository.updateRecordingState(currentState)

            // If resuming from auto-pause, set grace period to prevent immediate re-trigger (Bug #5)
            if (wasAutoPaused) {
                lastManualResumeTime = System.currentTimeMillis()
                lowSpeedStartTime = 0  // Bug #9: Reset low speed tracking on manual resume
                android.util.Log.d("RideRecordingService",
                    "Manual resume from auto-pause - grace period active for 30s")
            }

            // Resume duration updates (Bug #1 fix)
            startDurationUpdates(rideId)

            // Resume GPS accuracy observer (T082)
            startGpsAccuracyObserver(rideId)

            // Update notification
            val notification = buildNotification("Recording...")
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Stop ride recording.
     *
     * - Stops GPS tracking
     * - Updates state to Stopped
     * - Stops foreground service
     * - Shows save/discard dialog (handled by ViewModel)
     */
    private fun stopRecording() {
        val rideId = currentRideId ?: return

        serviceScope.launch {
            // Bug #12: Accumulate any current pause duration before stopping
            // If stopping from ManuallyPaused state, accumulate manual pause duration
            if (currentState is RideRecordingState.ManuallyPaused && pauseStartTime > 0) {
                val pausedDuration = System.currentTimeMillis() - pauseStartTime
                val ride = rideRepository.getRideById(rideId)
                if (ride != null) {
                    val updatedRide = ride.copy(
                        manualPausedDurationMillis = ride.manualPausedDurationMillis + pausedDuration
                    )
                    rideRepository.updateRide(updatedRide)
                    android.util.Log.d("RideRecordingService",
                        "Stop from paused: accumulated manual pause ${pausedDuration}ms, total=${updatedRide.manualPausedDurationMillis}ms")
                }
                pauseStartTime = 0
            }

            // If stopping from AutoPaused state, accumulate auto-pause duration
            if (currentState is RideRecordingState.AutoPaused && autoPauseStartTime > 0) {
                val autoPausedDuration = System.currentTimeMillis() - autoPauseStartTime
                val ride = rideRepository.getRideById(rideId)
                if (ride != null) {
                    val updatedRide = ride.copy(
                        autoPausedDurationMillis = ride.autoPausedDurationMillis + autoPausedDuration
                    )
                    rideRepository.updateRide(updatedRide)
                    android.util.Log.d("RideRecordingService",
                        "Stop from auto-paused: accumulated auto-pause ${autoPausedDuration}ms, total=${updatedRide.autoPausedDurationMillis}ms")
                }
                autoPauseStartTime = 0
            }

            // Stop GPS tracking
            locationJob?.cancel()

            // Stop duration updates
            durationUpdateJob?.cancel()

            // Stop GPS accuracy observer (T082)
            gpsAccuracyObserverJob?.cancel()

            // Reset current speed to 0.0 on stop (Feature 005)
            (rideRecordingStateRepository as? com.example.bikeredlights.data.repository.RideRecordingStateRepositoryImpl)
                ?.resetCurrentSpeed()

            // Update state to Stopped
            currentState = RideRecordingState.Stopped(rideId)
            rideRecordingStateRepository.updateRecordingState(currentState)

            // Stop foreground service
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * Recover from process death.
     *
     * - Check RideRecordingStateRepository for incomplete ride
     * - Resume GPS tracking if ride was Recording or Paused
     * - Restart foreground service with notification
     */
    private fun recoverFromProcessDeath() {
        serviceScope.launch {
            val state = rideRecordingStateRepository.getCurrentState()

            when (state) {
                is RideRecordingState.Recording -> {
                    // Resume recording
                    currentRideId = state.rideId
                    currentState = state
                    val notification = buildNotification("Recording... (Recovered)")
                    startForegroundService(notification)
                    startLocationTracking(state.rideId)
                    startDurationUpdates(state.rideId)
                    startGpsAccuracyObserver(state.rideId)  // T082
                }
                is RideRecordingState.ManuallyPaused -> {
                    // Resume in paused state
                    currentRideId = state.rideId
                    currentState = state
                    val notification = buildNotification("Paused (Recovered)")
                    startForegroundService(notification)
                }
                is RideRecordingState.AutoPaused -> {
                    // Resume in auto-paused state
                    currentRideId = state.rideId
                    currentState = state
                    val notification = buildNotification("Auto-paused (Recovered)")
                    startForegroundService(notification)
                    startLocationTracking(state.rideId)
                    startGpsAccuracyObserver(state.rideId)  // T082
                }
                else -> {
                    // No active ride, stop service
                    stopSelf()
                }
            }
        }
    }

    /**
     * Start GPS location tracking.
     *
     * Collects location updates from LocationRepository and records track points.
     */
    private fun startLocationTracking(rideId: Long) {
        locationJob?.cancel()

        locationJob = serviceScope.launch {
            locationRepository.getLocationUpdates()
                .catch { exception ->
                    // Handle location errors (e.g., permission denied, GPS disabled)
                    // TODO: Emit error to ViewModel for user notification
                }
                .collect { locationData ->
                    // Get current state
                    val state = rideRecordingStateRepository.getCurrentState()

                    // Determine pause state
                    val isManuallyPaused = state is RideRecordingState.ManuallyPaused
                    val isAutoPaused = state is RideRecordingState.AutoPaused

                    // Record track point
                    recordTrackPointUseCase(
                        rideId = rideId,
                        locationData = locationData,
                        isManuallyPaused = isManuallyPaused,
                        isAutoPaused = isAutoPaused
                    )

                    // Update current speed from GPS (Feature 005)
                    // Use GPS Doppler speed when available (most accurate)
                    // Falls back to 0.0 when speed unavailable (e.g., emulator)
                    val currentSpeed = maxOf(0.0, (locationData.speedMps ?: 0f).toDouble())
                    (rideRecordingStateRepository as? com.example.bikeredlights.data.repository.RideRecordingStateRepositoryImpl)
                        ?.updateCurrentSpeed(currentSpeed)

                    // Check for auto-resume (before pause gate)
                    // This allows auto-resume to execute during AutoPaused state
                    if (isAutoPaused) {
                        checkAutoResume(rideId, currentSpeed)
                    }

                    // Calculate distance if not paused
                    if (!isManuallyPaused && !isAutoPaused) {
                        updateRideDistance(rideId)
                    }
                }
        }
    }

    /**
     * Check if auto-resume should trigger after auto-pause.
     *
     * **Trigger Conditions**:
     * - Current state is AutoPaused
     * - Auto-pause feature is enabled
     * - Speed > 1 km/h (0.278 m/s)
     *
     * **Actions on Auto-Resume**:
     * - Accumulate auto-paused duration to ride
     * - Transition state from AutoPaused → Recording
     * - Reset tracking variables (autoPauseStartTime, lastManualResumeTime, lowSpeedStartTime)
     * - Update notification to "Recording..."
     * - Log debug event with speed, accuracy, rideId
     *
     * @param rideId The ID of the current ride
     * @param currentSpeed Current GPS speed in meters per second
     */
    private suspend fun checkAutoResume(rideId: Long, currentSpeed: Double) {
        // Check if auto-pause feature is enabled
        val autoPauseConfig = try {
            settingsRepository.autoPauseConfig.first()
        } catch (e: Exception) {
            com.example.bikeredlights.domain.model.settings.AutoPauseConfig.default()
        }

        if (!autoPauseConfig.enabled) return

        // Check speed threshold (1 km/h = 0.278 m/s)
        val resumeThreshold = 0.278
        if (currentSpeed < resumeThreshold) return

        // Accumulate auto-pause duration before resuming
        if (autoPauseStartTime > 0) {
            val autoPausedDuration = System.currentTimeMillis() - autoPauseStartTime
            val ride = rideRepository.getRideById(rideId)
            if (ride != null) {
                val updatedRide = ride.copy(
                    autoPausedDurationMillis = ride.autoPausedDurationMillis + autoPausedDuration
                )
                rideRepository.updateRide(updatedRide)
            }
            autoPauseStartTime = 0  // Reset
        }

        // Transition to Recording (automatic resume)
        currentState = RideRecordingState.Recording(rideId)
        rideRecordingStateRepository.updateRecordingState(currentState)

        // Reset tracking variables
        lastManualResumeTime = 0  // Clear grace period
        lowSpeedStartTime = 0  // Reset low speed tracking

        // Update notification
        val notification = buildNotification("Recording...")
        notificationManager.notify(NOTIFICATION_ID, notification)

        // FR-012: Log debug event with timestamp, speed, GPS accuracy, rideId
        android.util.Log.d("RideRecordingService",
            "Auto-resume triggered: rideId=$rideId speed=${currentSpeed}m/s >= threshold=${resumeThreshold}m/s")
    }

    /**
     * Update ride statistics (distance, speeds) from latest track points.
     */
    private suspend fun updateRideDistance(rideId: Long) {
        // Get last two track points
        val trackPoints = trackPointRepository.getTrackPointsForRide(rideId)
        if (trackPoints.size < 2) return

        val lastPoint = trackPoints[trackPoints.size - 1]
        val previousPoint = trackPoints[trackPoints.size - 2]

        // Calculate distance
        val distance = calculateDistanceUseCase(previousPoint, lastPoint)

        // Calculate current speed from GPS data
        // Speed is in meters per second; stationary detection: < 1 km/h (0.278 m/s) = 0.0
        val currentSpeed = if (lastPoint.speedMetersPerSec < 0.278) {
            0.0  // Stationary
        } else {
            lastPoint.speedMetersPerSec
        }

        // Auto-pause detection (T083-T085)
        val autoPauseConfig = try {
            settingsRepository.autoPauseConfig.first()
        } catch (e: Exception) {
            com.example.bikeredlights.domain.model.settings.AutoPauseConfig.default()
        }

        if (autoPauseConfig.enabled) {
            val currentState = rideRecordingStateRepository.getCurrentState()

            // Auto-pause/resume thresholds: 1 km/h = 0.278 m/s
            val pauseThreshold = 0.278  // Speed below which to auto-pause (< 1 km/h)
            val resumeThreshold = 0.278  // Speed above which to auto-resume (> 1 km/h)

            when (currentState) {
                is RideRecordingState.Recording -> {
                    // Check if we're within grace period after manual resume (Bug #5 fix)
                    val timeSinceManualResume = System.currentTimeMillis() - lastManualResumeTime
                    val inGracePeriod = lastManualResumeTime > 0 && timeSinceManualResume < AUTO_PAUSE_GRACE_PERIOD_MS

                    // Bug #9: Time-based auto-pause detection
                    // Track how long speed has been below threshold before triggering auto-pause
                    if (currentSpeed < pauseThreshold && !inGracePeriod) {
                        // Speed is below threshold - start or continue tracking
                        if (lowSpeedStartTime == 0L) {
                            // First time below threshold - start tracking
                            lowSpeedStartTime = System.currentTimeMillis()
                            android.util.Log.d("RideRecordingService",
                                "Low speed detected: speed=$currentSpeed m/s < threshold=$pauseThreshold m/s, starting timer")
                        } else {
                            // Already tracking - check if duration threshold met
                            val lowSpeedDuration = System.currentTimeMillis() - lowSpeedStartTime
                            val autoPauseThresholdMs = autoPauseConfig.getThresholdMs()

                            if (lowSpeedDuration >= autoPauseThresholdMs) {
                                // Duration threshold met - trigger auto-pause
                                autoPauseStartTime = System.currentTimeMillis()
                                lowSpeedStartTime = 0  // Reset tracking

                                // Transition to AutoPaused
                                this.currentState = RideRecordingState.AutoPaused(rideId)
                                rideRecordingStateRepository.updateRecordingState(this.currentState)

                                // Update notification
                                val notification = buildNotification("Auto-paused (low speed)")
                                notificationManager.notify(NOTIFICATION_ID, notification)

                                android.util.Log.d("RideRecordingService",
                                    "Auto-pause triggered: speed=$currentSpeed m/s < threshold=$pauseThreshold m/s for ${lowSpeedDuration}ms >= ${autoPauseThresholdMs}ms")
                            } else {
                                android.util.Log.d("RideRecordingService",
                                    "Low speed continues: ${lowSpeedDuration}ms / ${autoPauseThresholdMs}ms")
                            }
                        }
                    } else if (currentSpeed >= pauseThreshold) {
                        // Speed went above threshold - reset tracking
                        if (lowSpeedStartTime > 0) {
                            android.util.Log.d("RideRecordingService",
                                "Speed increased: speed=$currentSpeed m/s >= threshold=$pauseThreshold m/s, resetting timer")
                            lowSpeedStartTime = 0
                        }
                    } else if (inGracePeriod && currentSpeed < pauseThreshold) {
                        // In grace period - don't track low speed
                        android.util.Log.d("RideRecordingService",
                            "Auto-pause suppressed: in grace period (${timeSinceManualResume}ms / ${AUTO_PAUSE_GRACE_PERIOD_MS}ms)")
                        lowSpeedStartTime = 0  // Reset tracking during grace period
                    }
                }
                // Note: AutoPaused case removed - auto-resume now handled in checkAutoResume()
                // called before this function to ensure it executes during AutoPaused state
                else -> { /* No action for other states */ }
            }
        }

        // Get current ride
        val ride = rideRepository.getRideById(rideId) ?: return

        // Update max speed
        val newMaxSpeed = maxOf(ride.maxSpeedMetersPerSec, currentSpeed)

        // Calculate moving duration (exclude paused time)
        val elapsedDuration = System.currentTimeMillis() - ride.startTime
        val movingDuration = elapsedDuration - ride.manualPausedDurationMillis - ride.autoPausedDurationMillis

        // Calculate average speed (distance / moving time)
        val newDistance = ride.distanceMeters + distance
        val avgSpeed = if (movingDuration > 0) {
            newDistance / (movingDuration / 1000.0)  // Convert ms to seconds
        } else {
            0.0
        }

        // Update ride statistics
        val updatedRide = ride.copy(
            distanceMeters = newDistance,
            maxSpeedMetersPerSec = newMaxSpeed,
            avgSpeedMetersPerSec = avgSpeed
        )

        rideRepository.updateRide(updatedRide)
    }

    /**
     * Start duration updates (timer-based, every 1 second).
     *
     * This fixes Bug #1: Duration not updating in real-time.
     * Updates ride duration fields independently of GPS location changes.
     */
    private fun startDurationUpdates(rideId: Long) {
        durationUpdateJob?.cancel()

        durationUpdateJob = serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(100)  // Update every 100ms for smooth timer display
                updateRideDuration(rideId)
            }
        }
    }

    /**
     * Update ride duration fields in database.
     *
     * Calculates elapsed and moving duration based on current time and pause state.
     * Also updates pause durations in real-time for accurate timer display.
     * Saves to database for real-time UI updates via Flow observation.
     */
    private suspend fun updateRideDuration(rideId: Long) {
        val state = rideRecordingStateRepository.getCurrentState()
        val ride = rideRepository.getRideById(rideId) ?: return

        // Don't update if GPS hasn't initialized yet (startTime = 0)
        if (ride.startTime == 0L) {
            return
        }

        // Calculate elapsed duration (total time since start)
        val elapsedDuration = System.currentTimeMillis() - ride.startTime

        when (state) {
            is RideRecordingState.Recording -> {
                // Active recording: Update elapsed and moving durations
                val movingDuration = elapsedDuration - ride.manualPausedDurationMillis - ride.autoPausedDurationMillis

                val updatedRide = ride.copy(
                    elapsedDurationMillis = elapsedDuration,
                    movingDurationMillis = movingDuration
                )
                rideRepository.updateRide(updatedRide)
            }
            is RideRecordingState.AutoPaused -> {
                // Update auto-pause duration in real-time for timer display
                if (autoPauseStartTime > 0) {
                    val currentPauseDuration = System.currentTimeMillis() - autoPauseStartTime
                    val totalAutoPause = ride.autoPausedDurationMillis + currentPauseDuration
                    val movingDuration = elapsedDuration - ride.manualPausedDurationMillis - totalAutoPause

                    val updatedRide = ride.copy(
                        elapsedDurationMillis = elapsedDuration,
                        movingDurationMillis = movingDuration
                        // Don't update autoPausedDurationMillis here - it's accumulated when resuming
                    )
                    rideRepository.updateRide(updatedRide)
                }
            }
            else -> {
                // ManuallyPaused, Stopped, or Idle: Don't update
                // Manual pause is handled by ViewModel when resuming/stopping
            }
        }
    }

    /**
     * Start GPS accuracy observer (T082).
     *
     * Observes changes to GPS accuracy setting and restarts location tracking
     * when the setting changes mid-ride. This ensures the new update interval
     * takes effect immediately without needing to stop and restart the ride.
     *
     * Bug #6 fix: Use drop(1) to skip initial emission and only react to actual changes.
     *
     * @param rideId ID of the active ride
     */
    private fun startGpsAccuracyObserver(rideId: Long) {
        gpsAccuracyObserverJob?.cancel()

        gpsAccuracyObserverJob = serviceScope.launch {
            settingsRepository.gpsAccuracy
                .distinctUntilChanged()  // Only react to actual changes
                .drop(1)  // Bug #6: Skip initial emission to avoid unnecessary restarts
                .collect { newAccuracy ->
                    // Restart location tracking with new GPS accuracy setting
                    // Only restart if we're currently recording (not paused)
                    val state = rideRecordingStateRepository.getCurrentState()
                    if (state is RideRecordingState.Recording || state is RideRecordingState.AutoPaused) {
                        // Cancel current location job
                        locationJob?.cancel()

                        // Restart with new setting (LocationRepository will read latest setting)
                        startLocationTracking(rideId)

                        android.util.Log.d("RideRecordingService",
                            "GPS accuracy changed to ${newAccuracy.name}, restarted location tracking")
                    }
                }
        }
    }

    /**
     * Start foreground service with notification.
     */
    private fun startForegroundService(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Build foreground notification with actions.
     */
    private fun buildNotification(contentText: String): Notification {
        // Intent to open MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ride Recording")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Add actions based on current state
        when (currentState) {
            is RideRecordingState.Recording -> {
                // Show Pause action
                builder.addAction(
                    0,
                    "Pause",
                    createActionPendingIntent(ACTION_PAUSE_RECORDING)
                )
            }
            is RideRecordingState.ManuallyPaused -> {
                // Show Resume action
                builder.addAction(
                    0,
                    "Resume",
                    createActionPendingIntent(ACTION_RESUME_RECORDING)
                )
            }
            else -> {}
        }

        // Always show Stop action
        builder.addAction(
            0,
            "Stop",
            createActionPendingIntent(ACTION_STOP_RECORDING)
        )

        return builder.build()
    }

    /**
     * Create PendingIntent for notification action.
     */
    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, RideRecordingService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Create notification channel for Android O+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ride Recording",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for active ride recording"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ride_recording_channel"
        private const val NOTIFICATION_ID = 1

        // Bug #5: Grace period after manual resume from auto-pause
        private const val AUTO_PAUSE_GRACE_PERIOD_MS = 30000L  // 30 seconds

        // Service actions
        const val ACTION_START_RECORDING = "com.example.bikeredlights.ACTION_START_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.example.bikeredlights.ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.example.bikeredlights.ACTION_RESUME_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.bikeredlights.ACTION_STOP_RECORDING"

        /**
         * Start ride recording service.
         */
        fun startRecording(context: Context) {
            val intent = Intent(context, RideRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
            }
            context.startForegroundService(intent)
        }

        /**
         * Stop ride recording service.
         */
        fun stopRecording(context: Context) {
            val intent = Intent(context, RideRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }
}
