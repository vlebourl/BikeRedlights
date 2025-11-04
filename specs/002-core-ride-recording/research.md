# Research: Core Ride Recording (Feature F1A)

**Date**: 2025-11-04
**Feature**: Core Ride Recording (v0.3.0)
**Branch**: 002-core-ride-recording

---

## Summary

This document consolidates research findings for implementing GPS-based ride recording with Room persistence, foreground service architecture, and manual/auto-pause functionality for BikeRedlights cycling app.

---

## 1. Foreground Service Architecture

### Decision: LifecycleService with FusedLocationProviderClient and StateFlow Communication

**Rationale**:
- Lifecycle-aware service with `lifecycleScope` for coroutine management
- StateFlow for reactive communication between Service, Repository, and ViewModel
- Survives screen-off and app backgrounding (critical for cycling use case)
- Complies with Android 14+ foreground service type requirements

### Key Implementation Details

**Service Type & Permissions**:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".service.RideRecordingService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

**Location Request Configuration**:
- High Accuracy: `PRIORITY_HIGH_ACCURACY` with 1000ms interval (GPS required for cycling speed)
- Battery Saver: 4000ms interval (per F2A settings integration)
- `setMaxUpdateDelayMillis(2000L)` for batching (battery optimization)

**Notification Requirements**:
- Priority: `PRIORITY_LOW` / `IMPORTANCE_LOW` (avoid extra battery warning)
- Must be ongoing (`.setOngoing(true)`)
- Update frequency: Every 1 second with current duration/distance
- Actions: "Pause" and "Stop" as `PendingIntent`

**Service-ViewModel Communication Pattern**:
```kotlin
// Singleton Repository with StateFlow
@Singleton
class LocationRepository {
    private val _locationUpdates = MutableStateFlow<Location?>(null)
    val locationUpdates: StateFlow<Location?> = _locationUpdates.asStateFlow()
}

// Service writes to repository
locationCallback.onLocationResult { location ->
    repository.updateLocation(location)
}

// ViewModel observes repository
val locationUpdates = repository.locationUpdates
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
```

**Process Death Handling**:
- `START_STICKY` in `onStartCommand()` (requests restart)
- Persist ride data continuously to Room database
- On service restart, check for incomplete rides (endTime = null) and resume
- Reality: Not guaranteed to survive (user force-stop, OEM battery optimization)

### Alternatives Considered

- **Plain Service**: Rejected - no lifecycle-aware coroutine scopes
- **LiveData**: Rejected - StateFlow is modern Android standard (2025)
- **Bound Service**: Rejected - doesn't survive app backgrounding
- **WorkManager**: Rejected - designed for deferrable tasks, 15-min minimum intervals on Android 12+

### References
- [Foreground Services Overview](https://developer.android.com/develop/background-work/services/foreground-services)
- [Request Location Updates](https://developer.android.com/develop/sensors-and-location/location/request-updates)
- [Android 14 Foreground Service Types](https://developer.android.com/about/versions/14/changes/fgs-types-required)

---

## 2. Room Database Architecture

### Decision: Room 2.6.1 with One-to-Many Relationship + KSP Annotation Processing

**Rationale**:
- KSP is faster than KAPT and the future of Kotlin annotation processing (project already uses KSP)
- Cascade delete (`onDelete = ForeignKey.CASCADE`) ensures data integrity when rides are deleted
- Flow-based queries provide real-time UI updates during recording
- Repository pattern matches project's Clean Architecture standards

### Schema Design

**Ride Entity**:
```kotlin
@Entity(
    tableName = "rides",
    indices = [Index(value = ["startTime"])]
)
data class Ride(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,  // "Ride on Nov 4, 2025"
    val startTime: Long,  // Unix millis
    val endTime: Long?,   // Nullable for incomplete rides
    val elapsedDurationMillis: Long,  // Total time including pauses
    val movingDurationMillis: Long,   // Excluding all pauses
    val manualPausedDurationMillis: Long,
    val autoPausedDurationMillis: Long,
    val distanceMeters: Double,
    val avgSpeedMetersPerSec: Double,
    val maxSpeedMetersPerSec: Double
)
```

**TrackPoint Entity**:
```kotlin
@Entity(
    tableName = "track_points",
    foreignKeys = [ForeignKey(
        entity = Ride::class,
        parentColumns = ["id"],
        childColumns = ["rideId"],
        onDelete = ForeignKey.CASCADE  // Auto-delete TrackPoints when Ride deleted
    )],
    indices = [
        Index(value = ["rideId"]),
        Index(value = ["timestamp"])
    ]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speedMetersPerSec: Double,
    val accuracy: Float,
    val isManuallyPaused: Boolean,
    val isAutoPaused: Boolean
)
```

**Database Version**: 1 (first Room feature)
- Use `.fallbackToDestructiveMigration()` for v1 only
- Remove and implement proper migrations for v2+ (future features)
- Schema exported to `app/schemas/` (already configured in build.gradle.kts)

### DAO Patterns

**Key Operations**:
- `insert(ride: Ride): Long` - Returns generated ride ID
- `insertAll(trackPoints: List<TrackPoint>)` - Batch insert with automatic transaction
- `getAllRidesFlow(): Flow<List<Ride>>` - Reactive query for future history screen
- `getIncompleteRides(): List<Ride>` - Recovery on app launch (endTime = null)

**Performance Optimization**:
- Batch insert TrackPoints (3600 inserts/hour → 1 transaction/batch)
- Indices on foreign keys (rideId) and timestamps for query performance
- Use `distinctUntilChanged()` on Flow to prevent unnecessary UI recompositions

### Alternatives Considered

- **Embedded Relationship**: Rejected - loads ALL TrackPoints into memory every time
- **Manual Deletion**: Rejected - cascade delete is standard SQL feature, more reliable
- **Store as JSON**: Rejected - cannot query individual TrackPoints, violates normalization
- **LiveData**: Rejected - Flow is more composable and recommended by Google

### References
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Define Relationships](https://developer.android.com/training/data-storage/room/relationships)
- [Async Queries with Flow](https://developer.android.com/training/data-storage/room/async-queries)

---

## 3. Wake Lock Management

### Decision: Use `FLAG_KEEP_SCREEN_ON` with `DisposableEffect` (or `Modifier.keepScreenOn()` in Compose 1.9+)

**Rationale**:
- No special permission required (unlike PowerManager.WakeLock)
- Automatic lifecycle management - platform handles cleanup
- Activity-scoped - only works during foreground, automatically releases when backgrounded
- Google-recommended for foreground use cases
- BikeRedlights already has foreground service for background tracking

### Implementation Pattern

**Modern Approach** (Compose 1.9+):
```kotlin
@Composable
fun LiveRideScreen(viewModel: RideRecordingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val modifier = if (uiState.isRecording) {
        Modifier.keepScreenOn()
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Ride stats UI
    }
}
```

**Fallback Approach** (earlier Compose versions):
```kotlin
@Composable
fun KeepScreenOn(enabled: Boolean) {
    if (enabled) {
        val context = LocalContext.current
        DisposableEffect(Unit) {
            val window = (context as Activity).window
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
```

**Automatic Lifecycle Handling**:
- `collectAsStateWithLifecycle()` stops collecting when app backgrounds (Lifecycle.State.STARTED)
- FLAG_KEEP_SCREEN_ON automatically stops preventing screen lock when activity is no longer visible
- Foreground service continues GPS tracking in background
- Wake lock re-applies automatically when returning to foreground

### Alternatives Considered

- **PowerManager.WakeLock**: Rejected - requires WAKE_LOCK permission, complex lifecycle management, prone to orphaned locks, overkill for foreground-only use case
- **View-based keepScreenOn**: Rejected - BikeRedlights uses Jetpack Compose (no XML layouts per CLAUDE.md)

### Battery Impact

- Minimal - screen backlight is primary drain, not wake lock mechanism
- User-controlled - only active when app in foreground AND recording
- Auto-released - stops when backgrounded or recording stops
- Spec target: < 10% battery drain per hour (GPS is primary consumer, not wake lock)

### References
- [Keeping the Device Awake](https://developer.android.com/training/scheduling/wakelock)
- [FLAG_KEEP_SCREEN_ON Documentation](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON)
- [Compose Side Effects](https://developer.android.com/develop/ui/compose/side-effects)

---

## 4. Manual Pause/Resume Functionality

### Decision: Bottom-Aligned Icon Button with State-Based UI and Dual Time Tracking

**Rationale**:
- Separate Pause/Resume from Stop button prevents accidental ride termination (Strava user feedback validates)
- Bottom action bar placement for thumb-friendly one-handed operation while cycling
- Dual time tracking (elapsed time + moving time) aligns with industry standards (Strava, Garmin)
- Manual pause stops GPS for battery saving during extended stops (cafe, lunch)

### UI Design

**Button Placement**:
```
┌─────────────────────────────────┐
│     Live Ride Statistics        │
│   Duration: 00:45:23            │
│   Distance: 12.3 km             │
│   Current: 18.5 km/h            │
│   Average: 22.1 km/h            │
│   Max: 35.7 km/h                │
└─────────────────────────────────┘
┌─────────────────────────────────┐
│  [Pause]           [Stop Ride]  │  ← Bottom Action Bar
└─────────────────────────────────┘
```

**Button Specifications**:
- **Pause Button**: `FilledIconButton` (Material 3)
  - Recording: Container = `surfaceVariant`, Icon = pause symbol
  - Paused: Container = `primaryContainer`, Icon = play/resume symbol
  - Minimum touch target: 48dp × 48dp
  - Spacing from Stop: 16dp minimum

- **Stop Button**: `FilledTonalButton` (Material 3)
  - Container = `errorContainer`, Text = "Stop Ride"
  - Positioned right (harder to reach = safer)

**Visual States**:
- **Paused Banner**: Full-width overlay at top with "Ride Paused" text + icon
- **Frozen Stats**: Duration, distance, speeds all frozen at last values
- **Notification Update**: "Recording Ride" → "Ride Paused", "Pause" action → "Resume" action

### State Management

**Pause State Flow**:
```kotlin
data class RideRecordingState(
    val rideId: Long,
    val startTime: Instant,
    val isManuallyPaused: Boolean = false,
    val manualPauseStartTime: Instant? = null,
    val accumulatedManualPausedDuration: Duration = Duration.ZERO,
    val isAutoPaused: Boolean = false,
    val autoPauseStartTime: Instant? = null,
    val accumulatedAutoPausedDuration: Duration = Duration.ZERO
)
```

**GPS Behavior**:
- **Manual Pause**: GPS location updates **STOP** completely (battery saving)
- **Auto-Pause**: GPS location updates **CONTINUE** (allows automatic resume on movement)
- No TrackPoints inserted during manual pause
- TrackPoints marked with `isManuallyPaused = true` flag for future visualization

**Duration Calculation**:
```kotlin
// Elapsed Time = total time from start to stop (includes all pauses)
val elapsedTime = endTime - startTime

// Moving Time = elapsed time - manual pauses - auto pauses
val movingTime = elapsedTime - accumulatedManualPausedDuration - accumulatedAutoPausedDuration

// Average speed = distance / moving time (industry standard)
val averageSpeed = totalDistance / movingTime.inWholeSeconds
```

### Coordination with Auto-Pause

**Priority Logic**:
- Manual pause takes precedence over auto-pause
- Auto-pause detection disabled while manually paused
- Auto-pause auto-resumes on movement; manual pause requires explicit user action

**Use Cases**:
- **Manual Pause**: Extended cafe stops, lunch breaks, photo sessions (GPS off, battery saving)
- **Auto-Pause**: Traffic lights, brief stops at intersections (GPS on, auto-resume)

### Alternatives Considered

- **Single Toggle Button**: Rejected - high risk of accidental termination, Strava user complaints
- **Disable Pause Based on GPS**: Rejected - Android Health Services warns against this, creates UI/state sync issues
- **Automatic Resume on Movement**: Rejected - defeats purpose of manual pause, battery drain if user walks around cafe
- **Continue GPS During Manual Pause**: Rejected - wastes battery, no benefit for extended stops

### References
- [Android Health Services - Exercise Recording](https://developer.android.com/health-and-fitness/guides/health-services/active-data)
- [Material Design 3 - Buttons](https://m3.material.io/components/all-buttons)
- [Strava Auto-Pause Engineering](https://medium.com/strava-engineering/improving-auto-pause-for-everyone-13f253c66f9e)
- [Garmin Time Metrics](https://forums.garmin.com/apps-software/mobile-apps-web/f/garmin-connect-mobile-andriod/245172/total-time-vs-elapsed-time)

---

## 5. Cross-Cutting Concerns

### Testing Strategy

**Unit Tests**:
- Repository: Test StateFlow emissions with Turbine
- ViewModel: Mock repository, verify service intent creation
- Room DAOs: In-memory database tests with cascade delete validation

**Instrumented Tests**:
- Service lifecycle: Start/stop verification
- Notification: Channel creation, action intents
- Room migrations: MigrationTestHelper for future v2

**Emulator Testing** (MANDATORY per Constitution):
- Location simulation via Extended Controls (GPX route playback)
- Foreground service verification with `adb shell dumpsys activity services`
- Battery/Doze testing with `adb shell dumpsys deviceidle force-idle`
- Process death simulation with `adb shell am kill`
- Permission revocation testing

### Performance Targets

- GPS location updates processed within 100ms
- Database inserts non-blocking (background thread)
- UI updates smooth (60fps minimum)
- Battery drain < 10%/hour with High Accuracy (spec SC-008)
- Notification updates < 100ms for responsiveness

### Error Handling

**SQLiteFullException**: Catch on TrackPoint insert, stop recording, show "Storage full" notification
**SecurityException**: Detect in service on location request, stop recording, show "Permission required" notification
**Process Death**: Service restart checks database for incomplete rides (endTime = null), offers recovery dialog

---

## 6. Technology Stack Summary

| Component | Technology | Version | Notes |
|-----------|-----------|---------|-------|
| Language | Kotlin | 2.0.21 | Already in project |
| UI Framework | Jetpack Compose | BOM 2024.11.00 | Already in project |
| Database | Room | 2.6.1 | Already configured with KSP |
| Location API | Play Services Location | 21.3.0 | Already in project |
| Service Type | LifecycleService | AndroidX | Lifecycle-aware coroutines |
| State Management | StateFlow | Coroutines 1.9.0 | Modern reactive pattern |
| Dependency Injection | Manual DI | N/A | Hilt deferred per Constitution exception |
| Notification | NotificationCompat | AndroidX | Android 13+ POST_NOTIFICATIONS |
| Wake Lock | FLAG_KEEP_SCREEN_ON | Platform | No permission required |

---

## 7. Implementation Phases

**Phase 0: Database Setup** (1 day)
- Define Ride and TrackPoint entities
- Create DAOs with Flow queries
- Implement BikeRedlightsDatabase singleton
- Unit test cascade delete and recovery queries

**Phase 1: Foreground Service** (2 days)
- Implement RideRecordingService with LifecycleService
- Configure FusedLocationProviderClient
- Build notification with Pause/Stop actions
- Implement START_STICKY and process death recovery
- Service lifecycle tests

**Phase 2: State Management** (1 day)
- Create LocationRepository with StateFlow
- Implement RideRecordingViewModel
- Connect service, repository, and ViewModel
- Unit test state flows

**Phase 3: UI Implementation** (1 day)
- Build LiveRideScreen with stats display
- Implement KeepScreenOn composable
- Create Pause/Resume buttons with state-based UI
- Add "Ride Paused" banner
- Compose UI tests

**Phase 4: Integration & Polish** (1 day)
- Settings integration (units, GPS accuracy, auto-pause)
- Save/discard dialog
- Review screen with statistics
- Incomplete ride recovery dialog
- End-to-end emulator testing

**Total Estimate**: 5-7 days (matches spec estimate)

---

## 8. Open Questions Resolved

All clarifications from spec have been resolved through research:

1. **Foreground Service Approach**: ✅ LifecycleService with StateFlow communication
2. **Database Schema**: ✅ One-to-many with cascade delete, dual time tracking
3. **Wake Lock Method**: ✅ FLAG_KEEP_SCREEN_ON via DisposableEffect
4. **Manual Pause UI**: ✅ Bottom action bar with separate Pause and Stop buttons
5. **GPS Behavior During Pause**: ✅ Manual pause stops GPS, auto-pause continues GPS

No additional research or clarifications needed. Ready for Phase 1 (Design & Contracts).

---

**Research Complete**: 2025-11-04
**Next Phase**: Generate data-model.md, contracts/, and quickstart.md
