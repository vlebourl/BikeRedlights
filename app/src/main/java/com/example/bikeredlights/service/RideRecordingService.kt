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
    private var currentRideId: Long? = null
    private var currentState: RideRecordingState = RideRecordingState.Idle

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
        }
    }

    /**
     * Pause ride recording (manual pause).
     */
    private fun pauseRecording() {
        val rideId = currentRideId ?: return

        serviceScope.launch {
            currentState = RideRecordingState.ManuallyPaused(rideId)
            rideRecordingStateRepository.updateRecordingState(currentState)

            // Update notification
            val notification = buildNotification("Paused")
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Resume ride recording from manual pause.
     */
    private fun resumeRecording() {
        val rideId = currentRideId ?: return

        serviceScope.launch {
            currentState = RideRecordingState.Recording(rideId)
            rideRecordingStateRepository.updateRecordingState(currentState)

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
            // Stop GPS tracking
            locationJob?.cancel()

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

                    // Calculate distance if not paused
                    if (!isManuallyPaused && !isAutoPaused) {
                        updateRideDistance(rideId)
                    }
                }
        }
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
                    // Check if speed dropped below pause threshold
                    if (currentSpeed < pauseThreshold) {
                        // Transition to AutoPaused
                        this.currentState = RideRecordingState.AutoPaused(rideId)
                        rideRecordingStateRepository.updateRecordingState(this.currentState)

                        // Update notification
                        val notification = buildNotification("Auto-paused (low speed)")
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }
                is RideRecordingState.AutoPaused -> {
                    // Check if speed went above resume threshold
                    if (currentSpeed >= resumeThreshold) {
                        // Transition to Recording
                        this.currentState = RideRecordingState.Recording(rideId)
                        rideRecordingStateRepository.updateRecordingState(this.currentState)

                        // Update notification
                        val notification = buildNotification("Recording...")
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // TODO: Use proper icon
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
