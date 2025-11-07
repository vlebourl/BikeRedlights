# Service Interface Contract: RideRecordingService Auto-Resume

**Date**: 2025-11-07
**Feature**: Fix Auto-Resume Not Working After Auto-Pause
**Spec**: [../spec.md](../spec.md)

## Overview

This contract defines the behavior of the `RideRecordingService` auto-resume functionality. The service is an Android Foreground Service responsible for GPS tracking, ride recording, and state management (including auto-pause/auto-resume).

---

## Service Contract

### Function: `checkAutoResume(rideId: Long, currentSpeed: Double)`

**Visibility**: `private suspend`

**Purpose**: Detect when cyclist starts moving after auto-pause and automatically resume recording

**Preconditions**:
- Service is bound and running
- Current state is `RideRecordingState.AutoPaused(rideId)`
- Auto-pause feature is enabled in settings (`AutoPauseConfig.enabled = true`)
- Location permissions are granted
- `currentSpeed` is valid GPS speed reading (>= 0.0 m/s)

**Inputs**:
| Parameter | Type | Validation | Description |
|-----------|------|------------|-------------|
| `rideId` | `Long` | Must be > 0 | ID of currently active ride |
| `currentSpeed` | `Double` | Must be >= 0.0 | Current GPS speed in meters/second |

**Behavior**:

1. **Check Feature Toggle**:
   ```kotlin
   val autoPauseConfig = settingsRepository.autoPauseConfig.first()
   if (!autoPauseConfig.enabled) return
   ```
   - If auto-pause is disabled, function returns immediately
   - No state transitions occur

2. **Check Speed Threshold**:
   ```kotlin
   val resumeThreshold = 0.278  // 1 km/h in m/s
   if (currentSpeed < resumeThreshold) return
   ```
   - If speed < 1 km/h, function returns (remain auto-paused)
   - **Hysteresis Note**: No time-based hysteresis for auto-resume (immediate trigger)
     - Rationale: Starting movement is intentional; low false positive risk
     - Auto-pause already has time-based hysteresis (5-60 second threshold)

3. **Accumulate Auto-Pause Duration**:
   ```kotlin
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
   ```
   - Calculates time spent in auto-pause (current time - start time)
   - Adds duration to ride's `autoPausedDurationMillis` field
   - Resets `autoPauseStartTime` to 0 (state is no longer auto-paused)

4. **Transition State**:
   ```kotlin
   currentState = RideRecordingState.Recording(rideId)
   rideRecordingStateRepository.updateRecordingState(currentState)
   ```
   - Changes service state from `AutoPaused` to `Recording`
   - Persists state to DataStore (for process death recovery)
   - Emits new state via StateFlow (ViewModel observes)

5. **Reset Tracking Variables**:
   ```kotlin
   lastManualResumeTime = 0  // Clear grace period
   lowSpeedStartTime = 0     // Clear low-speed accumulator
   ```
   - Ensures clean state for future auto-pause detection

6. **Update Notification**:
   ```kotlin
   val notification = buildNotification("Recording...")
   notificationManager.notify(NOTIFICATION_ID, notification)
   ```
   - Changes notification text from "Auto-paused" to "Recording..."
   - Updates duration and distance in notification
   - Keeps notification persistent (foreground service requirement)

7. **Log Event**:
   ```kotlin
   android.util.Log.d("RideRecordingService",
       "Auto-resume triggered: speed=$currentSpeed m/s >= threshold=$resumeThreshold m/s")
   ```
   - Debug log for troubleshooting and testing verification

**Postconditions** (if auto-resume triggered):
- Service state is `RideRecordingState.Recording(rideId)`
- `Ride.autoPausedDurationMillis` increased by auto-pause duration
- Notification text is "Recording..."
- `autoPauseStartTime` is 0
- `lastManualResumeTime` is 0
- `lowSpeedStartTime` is 0
- Distance calculation resumes on next GPS update

**Postconditions** (if auto-resume NOT triggered):
- Service state remains `RideRecordingState.AutoPaused(rideId)`
- No database updates
- Notification text remains "Auto-paused"
- Tracking variables unchanged

**Side Effects**:
- **Database Write**: Updates `rides` table (`autoPausedDurationMillis` field)
- **DataStore Write**: Persists new `RideRecordingState` to Preferences
- **StateFlow Emission**: Emits new state to observers (ViewModel → UI)
- **Notification Update**: Posts notification update via NotificationManager
- **Logging**: Writes to Android logcat

**Error Handling**:
| Error Scenario | Behavior |
|----------------|----------|
| `rideRepository.getRideById()` returns null | Skip duration update, continue with state transition |
| Database write fails (SQLiteException) | Exception propagates; service will retry on next GPS update |
| DataStore write fails (IOException) | Exception propagates; state persists in-memory until next write |
| Notification update fails (SecurityException) | Exception logged; service continues (notification not critical for functionality) |

**Thread Safety**:
- Function is `suspend` and executes on service coroutine scope (`serviceScope`)
- Database operations are sequential (Room DAO methods are suspend functions)
- No shared mutable state; all variables are method-local or synchronized via repository

**Performance**:
- Typical execution time: < 10ms (database read + update + state write)
- Worst case: < 50ms (slow disk I/O)
- Does not block GPS location updates (runs in separate coroutine)

---

## Integration Points

### 1. Location Update Flow

**Call Site**: `startLocationTracking()` function (line ~436 in RideRecordingService.kt)

**Before Fix** (BROKEN):
```kotlin
locationRepository.getLocationUpdates()
    .collect { locationData ->
        // ... record track point ...

        // Calculate distance if not paused
        if (!isManuallyPaused && !isAutoPaused) {
            updateRideDistance(rideId)  // Auto-resume logic trapped inside!
        }
    }
```

**After Fix** (WORKING):
```kotlin
locationRepository.getLocationUpdates()
    .collect { locationData ->
        val state = rideRecordingStateRepository.getCurrentState()
        val isManuallyPaused = state is RideRecordingState.ManuallyPaused
        val isAutoPaused = state is RideRecordingState.AutoPaused

        // 1. Record track point (works during all states)
        recordTrackPointUseCase(
            rideId = rideId,
            locationData = locationData,
            isManuallyPaused = isManuallyPaused,
            isAutoPaused = isAutoPaused
        )

        // 2. NEW: Check for auto-resume (before pause gate)
        if (isAutoPaused) {
            checkAutoResume(rideId, locationData.speedMetersPerSec)
        }

        // 3. Calculate distance if not paused
        if (!isManuallyPaused && !isAutoPaused) {
            updateRideDistance(rideId)
        }
    }
```

**Key Change**: `checkAutoResume()` is called **before** the pause gate, ensuring it executes during `AutoPaused` state.

### 2. State Repository

**Interface**: `RideRecordingStateRepository`

**Methods Used**:
- `getCurrentState(): RideRecordingState` - Get current state synchronously
- `updateRecordingState(state: RideRecordingState)` - Persist new state (suspend)
- `observeRecordingState(): Flow<RideRecordingState>` - Observe state changes (ViewModel uses this)

**Contract**:
- State updates are atomic (no partial writes)
- State persists across process death (DataStore backup)
- State emissions are conflated (only latest state is delivered)

### 3. Ride Repository

**Interface**: `RideRepository`

**Methods Used**:
- `getRideById(id: Long): Ride?` - Fetch ride by ID (suspend)
- `updateRide(ride: Ride)` - Update ride fields (suspend)

**Contract**:
- Returns null if ride ID doesn't exist
- Update is transactional (all fields updated atomically)
- Updates trigger Flow emissions for observers

### 4. Settings Repository

**Interface**: `SettingsRepository`

**Methods Used**:
- `val autoPauseConfig: Flow<AutoPauseConfig>` - Observe auto-pause settings

**Contract**:
- Flow emits current value immediately when collected
- Flow emits new values when settings change
- `.first()` suspends until first value is available

### 5. Notification Manager

**Interface**: `NotificationManager` (Android framework)

**Methods Used**:
- `notify(id: Int, notification: Notification)` - Post or update notification

**Contract**:
- Notification with same ID is replaced (not duplicated)
- Foreground service notification must always be visible
- Notification survives app backgrounding

---

## State Machine Contract

### State Transitions

**Valid Transitions**:
```
Idle → Recording         (user taps Start Ride)
Recording → ManuallyPaused  (user taps Pause)
Recording → AutoPaused   (speed < 1 km/h for threshold seconds) ✅ **WORKS**
Recording → Stopped      (user taps Stop Ride)

ManuallyPaused → Recording  (user taps Resume) ✅ **WORKS**
AutoPaused → Recording   (speed > 1 km/h) ✅ **FIXED BY THIS BUG FIX**

Stopped → Idle          (after save/discard dialog)
```

**Invalid Transitions** (throw IllegalStateException):
- `Idle → AutoPaused` (can't auto-pause without active ride)
- `ManuallyPaused → AutoPaused` (can't auto-pause during manual pause)
- `AutoPaused → ManuallyPaused` (can't manually pause during auto-pause)

### State Invariants

**When state is `AutoPaused(rideId)`**:
- `autoPauseStartTime > 0` (timestamp when auto-pause started)
- `lowSpeedStartTime == 0` (reset when auto-paused)
- Track points are recorded with `isAutoPaused=true`
- Notification text contains "Auto-paused"
- GPS tracking continues (foreground service running)
- Distance calculation is suspended

**When transitioning from `AutoPaused` to `Recording`** (auto-resume):
- `autoPauseStartTime` is reset to 0
- `lastManualResumeTime` is reset to 0 (clear grace period)
- `lowSpeedStartTime` is reset to 0
- `Ride.autoPausedDurationMillis` has been updated
- Notification text changes to "Recording..."
- Distance calculation resumes

---

## Testing Contract

### Unit Test Expectations

**Test 1: Auto-Resume Triggers on Speed Increase**
```kotlin
@Test
fun `when speed exceeds 1 kmh during auto-pause, auto-resume triggers`() = runTest {
    // Setup
    val rideId = 1L
    startRide(rideId)
    triggerAutoPause()  // Enter AutoPaused state

    // Action
    sendLocationUpdate(speed = 1.5 m/s)  // > 1 km/h

    // Assert
    advanceTimeBy(2000)  // Max 2 seconds latency
    val currentState = service.getCurrentState()
    assertTrue(currentState is RideRecordingState.Recording)
    assertEquals(rideId, (currentState as RideRecordingState.Recording).rideId)
}
```

**Test 2: Auto-Resume Accumulates Duration**
```kotlin
@Test
fun `when auto-resume triggers, auto-paused duration is accumulated`() = runTest {
    // Setup
    val rideId = 1L
    startRide(rideId)
    triggerAutoPause()  // Enter AutoPaused state

    // Action
    delay(10000)  // 10 seconds in auto-pause
    sendLocationUpdate(speed = 2.0 m/s)  // Trigger auto-resume

    // Assert
    val ride = rideRepository.getRideById(rideId)
    assertTrue(ride!!.autoPausedDurationMillis in 9900..10100)  // ~10s ±100ms
}
```

**Test 3: Auto-Resume Doesn't Trigger on GPS Drift**
```kotlin
@Test
fun `when speed is below 1 kmh during auto-pause, auto-resume does not trigger`() = runTest {
    // Setup
    val rideId = 1L
    startRide(rideId)
    triggerAutoPause()

    // Action
    repeat(10) {
        sendLocationUpdate(speed = 0.5 m/s)  // < 1 km/h (GPS noise)
        delay(1000)
    }

    // Assert
    val currentState = service.getCurrentState()
    assertTrue(currentState is RideRecordingState.AutoPaused)
}
```

**Test 4: Manual Resume Takes Precedence**
```kotlin
@Test
fun `when user manually resumes during auto-pause, manual resume overrides auto-resume`() = runTest {
    // Setup
    val rideId = 1L
    startRide(rideId)
    triggerAutoPause()

    // Action
    service.resumeRide()  // User taps Resume button

    // Assert
    val currentState = service.getCurrentState()
    assertTrue(currentState is RideRecordingState.Recording)
    // Grace period is active (prevents immediate re-auto-pause)
    assertTrue(service.lastManualResumeTime > 0)
}
```

### Integration Test Expectations

**Emulator GPS Simulation**:
1. Start ride
2. Simulate movement (5 km/h for 30 seconds)
3. Simulate stop (0.5 km/h for 10 seconds) → auto-pause triggers after 5s
4. Simulate movement (15 km/h for 30 seconds) → auto-resume should trigger within 2s
5. Verify:
   - Notification text changes from "Auto-paused" to "Recording..." ✅
   - Live tab UI removes "Auto-Paused" indicator ✅
   - Distance calculation resumes (distance increases) ✅
   - Auto-paused duration = ~5 seconds ✅

---

## Performance Contract

### Latency Targets

| GPS Mode | Update Interval | Auto-Resume Detection Latency | 95th Percentile |
|----------|-----------------|-------------------------------|-----------------|
| High Accuracy | 1 second | < 2 seconds | 1.5 seconds |
| Battery Saver | 4 seconds | < 8 seconds | 6 seconds |

**Measurement Method**: Time from speed increasing above 1 km/h to notification text changing to "Recording..."

### Resource Constraints

| Resource | Constraint | Measurement |
|----------|------------|-------------|
| CPU | < 5% average during auto-resume | Android Profiler |
| Memory | < 1 MB heap allocation per auto-resume cycle | Memory Profiler |
| Disk I/O | < 10 KB per auto-resume (1 database write) | StrictMode disk write detection |
| Battery | No measurable delta vs auto-pause (GPS already running) | Battery Historian |

---

## Backward Compatibility

### Breaking Changes: None

- No API signature changes (new function is `private`)
- No database schema changes
- No ViewModel interface changes
- No UI contract changes

### Migration: None Required

- Fix is transparent to users
- No data migration needed
- Existing auto-paused rides remain compatible

---

## Summary

The `checkAutoResume()` function is a **pure bug fix** with no external API changes. It implements the missing half of the auto-pause/auto-resume state machine, enabling the service to automatically transition from `AutoPaused` to `Recording` when movement is detected.

**Contract Guarantees**:
- ✅ Auto-resume triggers within latency targets (2s High Accuracy, 8s Battery Saver)
- ✅ Auto-paused duration is accurately accumulated
- ✅ State transitions are atomic and crash-safe
- ✅ Notification updates reflect current state
- ✅ Manual resume takes precedence over auto-resume
- ✅ GPS drift does not cause false auto-resumes
- ✅ Zero performance degradation vs current implementation
