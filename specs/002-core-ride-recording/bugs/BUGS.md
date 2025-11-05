# Bug Report: Phase 6 - Settings Integration (US5)

**Feature**: F1A - Core Ride Recording (v0.3.0)
**Phase**: Phase 6 - User Story 5 (P2) - Settings Integration
**Status**: ✅ **ALL BUGS FIXED** (as of 2025-11-05)
**Date Reported**: 2025-11-05
**Date Resolved**: 2025-11-05
**Reporter**: Emulator Testing Session

---

## Summary

Phase 6 testing revealed **4 bugs** related to real-time UI updates, pause behavior, permissions, and UX enhancements. All bugs have been successfully resolved:

- ✅ **Bug #1** (CRITICAL): Duration not updating in real-time - **FIXED** (commit e420703)
- ✅ **Bug #2** (CRITICAL): Duration continues when paused - **FIXED** (commit e420703)
- ✅ **Bug #3** (HIGH): Missing permission request UI - **FIXED** (commit b534d23)
- ✅ **Bug #4** (LOW): Missing current time display - **FIXED** (commit e420703)

**Previous Impact**: Users could not see live duration/distance/speed updates during ride recording. The UI only updated when user interacted (pause/resume) or navigated away and back.

**Current Status**: All critical bugs resolved. Real-time updates work correctly, pause behavior is accurate, permissions are requested properly, and current time is displayed.

---

## Bug #1: Duration Not Updating in Real-Time

### Severity
**CRITICAL** - Core feature (real-time statistics) is non-functional

### Description
The ride duration display does not update in real-time while recording is active. Duration stays at "00:00:00" and only updates when user interacts with the UI (pause/resume buttons) or navigates away from the screen and returns.

### Steps to Reproduce
1. Launch app on emulator
2. Grant location permissions: `adb shell pm grant com.example.bikeredlights android.permission.ACCESS_FINE_LOCATION`
3. Tap "Start Ride" button
4. Observe Live view statistics display
5. Wait 30 seconds without interacting
6. **Expected**: Duration increments every second (00:00:01, 00:00:02, etc.)
7. **Actual**: Duration stays at 00:00:00

### User Observations
From emulator testing session (2025-11-05):
> "it stayed at 0, i paused it updated to 00:00:18, i stopped, it updated to 00:00:21"

This confirms duration IS being calculated, but not persisted to database in real-time.

### Root Cause Analysis

#### Service Update Logic (Primary Issue)
**File**: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`

**Lines 324-420**: The Service only updates the database when GPS location changes:

```kotlin
// Line 324: LocationCallback.onLocationResult()
override fun onLocationResult(result: LocationResult) {
    // ...
    updateRideDistance(rideId)  // ← ONLY called on GPS location changes
}
```

**Problem**: In emulator without GPS movement simulation, `onLocationResult()` is never called, so `updateRideDistance()` never executes.

#### Duration Calculation Not Persisted
**Lines 402-420**: Even when `updateRideDistance()` IS called, duration is calculated but NOT saved:

```kotlin
// Line 402: Duration calculated locally
val elapsedDuration = System.currentTimeMillis() - ride.startTime
val movingDuration = elapsedDuration - ride.manualPausedDurationMillis - ride.autoPausedDurationMillis

// Lines 414-420: updatedRide MISSING duration fields
val updatedRide = ride.copy(
    distanceMeters = newDistance,
    maxSpeedMetersPerSec = newMaxSpeed,
    avgSpeedMetersPerSec = avgSpeed
    // ❌ MISSING: elapsedDurationMillis = elapsedDuration
    // ❌ MISSING: movingDurationMillis = movingDuration
)
rideRepository.updateRide(updatedRide)  // ← Only saves distance/speed
```

### Attempted Fixes

#### Fix #1: Flow-Based Observation (PARTIAL SUCCESS)
**Commit**: `c7f8e1a` - "feat(data): add Flow-based ride observation for real-time updates"
**Commit**: `3e6d5f2` - "feat(ui): update ViewModel to observe ride via Flow for real-time stats"

**What was done**:
- Added `getRideByIdFlow()` to RideDao, RideRepository, and RideRepositoryImpl
- Updated RideRecordingViewModel to continuously observe ride via Flow instead of one-time fetch
- Added proper coroutine job cancellation when recording state changes

**Result**: Infrastructure is correct, but exposes the underlying Service issue. Flow works, but there's nothing to observe because Service isn't updating database.

### Proposed Fix

**Strategy**: Timer-based updates instead of event-driven updates

**Implementation**:
1. Add periodic timer (1 second interval) in RideRecordingService
2. Call `updateRideDuration()` every second (new method)
3. Save calculated `elapsedDurationMillis` and `movingDurationMillis` to database
4. Keep existing `updateRideDistance()` for GPS location changes (distance/speed)

**Code changes needed**:
```kotlin
// In RideRecordingService.kt

private var durationUpdateJob: Job? = null

private fun startDurationUpdates(rideId: Long) {
    durationUpdateJob = serviceScope.launch {
        while (isActive) {
            updateRideDuration(rideId)
            delay(1000)  // Update every 1 second
        }
    }
}

private suspend fun updateRideDuration(rideId: Long) {
    val ride = rideRepository.getRideById(rideId) ?: return
    val elapsedDuration = System.currentTimeMillis() - ride.startTime
    val movingDuration = elapsedDuration - ride.manualPausedDurationMillis - ride.autoPausedDurationMillis

    val updatedRide = ride.copy(
        elapsedDurationMillis = elapsedDuration,
        movingDurationMillis = movingDuration
    )
    rideRepository.updateRide(updatedRide)
}
```

**Files to modify**:
- `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`

**Tests to add**:
- Unit test: Verify `updateRideDuration()` calculates correctly
- Instrumented test: Verify database updates every second during recording

---

## Bug #2: Duration Continues When Paused

### Severity
**CRITICAL** - Violates core requirement that paused rides should freeze statistics

### Description
When a ride is manually paused, the duration continues to increment. This results in incorrect total duration calculations.

### Steps to Reproduce
1. Start ride recording
2. Wait 18 seconds
3. Tap "Pause" button
4. Wait 3 seconds while paused
5. Tap "Stop" button
6. **Expected**: Duration shows 00:00:18 (paused time excluded)
7. **Actual**: Duration shows 00:00:21 (paused time included)

### User Observations
From emulator testing session (2025-11-05):
> "it stayed at 0, i paused it updated to 00:00:18, i stopped, it updated to 00:00:21. This shows: 1. it still not updating realtime, 2. the pause isn't pausing the duration counter"

### Root Cause Analysis

**File**: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`

**Line 402**: Duration calculated from `startTime` without considering pause state:

```kotlin
val elapsedDuration = System.currentTimeMillis() - ride.startTime
```

**Problem**: This formula continues counting regardless of whether ride is Recording, ManuallyPaused, or AutoPaused. It doesn't track pause start times or properly accumulate paused durations.

### Missing Functionality
- No `pauseStartTime` tracking when entering paused state
- No `pauseResumeTime` tracking when exiting paused state
- `manualPausedDurationMillis` and `autoPausedDurationMillis` are never updated during ride
- Pause/resume timestamps are only calculated at ride finish (too late)

### Proposed Fix

**Strategy**: Track pause intervals in real-time and exclude from duration calculation

**Implementation**:
1. Add `currentPauseStartTime` field to RideRecordingState
2. When entering ManuallyPaused or AutoPaused:
   - Set `currentPauseStartTime = System.currentTimeMillis()`
   - Stop duration timer (don't call `updateRideDuration()` while paused)
3. When exiting pause (Resume):
   - Calculate `pausedDuration = System.currentTimeMillis() - currentPauseStartTime`
   - Accumulate into `ride.manualPausedDurationMillis` or `ride.autoPausedDurationMillis`
   - Save updated ride to database
   - Resume duration timer
4. Duration calculation should ONLY update when state is Recording (not Paused)

**Code changes needed**:
```kotlin
// In RideRecordingState.kt
sealed class RideRecordingState {
    data class ManuallyPaused(
        val rideId: Long,
        val pauseStartTime: Long  // ← ADD THIS
    ) : RideRecordingState()

    data class AutoPaused(
        val rideId: Long,
        val pauseStartTime: Long  // ← ADD THIS
    ) : RideRecordingState()
}

// In RideRecordingService.kt
private fun handlePause() {
    durationUpdateJob?.cancel()  // Stop duration updates
    val pauseStartTime = System.currentTimeMillis()
    rideRecordingStateRepository.updateState(
        RideRecordingState.ManuallyPaused(rideId, pauseStartTime)
    )
}

private suspend fun handleResume() {
    val state = rideRecordingStateRepository.getCurrentState()
    if (state is RideRecordingState.ManuallyPaused) {
        val pausedDuration = System.currentTimeMillis() - state.pauseStartTime

        val ride = rideRepository.getRideById(state.rideId) ?: return
        val updatedRide = ride.copy(
            manualPausedDurationMillis = ride.manualPausedDurationMillis + pausedDuration
        )
        rideRepository.updateRide(updatedRide)

        startDurationUpdates(state.rideId)  // Resume duration updates
    }
}
```

**Files to modify**:
- `app/src/main/java/com/example/bikeredlights/domain/model/RideRecordingState.kt`
- `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`
- `app/src/main/java/com/example/bikeredlights/data/repository/RideRecordingStateRepositoryImpl.kt`

**Tests to add**:
- Unit test: Verify paused duration accumulates correctly
- Unit test: Verify moving duration excludes paused time
- Instrumented test: Pause for 5 seconds, verify duration doesn't increase
- Instrumented test: Pause, resume, verify total excludes paused interval

---

## Bug #3: Missing Permission Request UI

### Status
✅ **FIXED** (commit b534d23, 2025-11-05)

### Severity
**HIGH** - App crashed on first launch; blocked all functionality

### Description
The app did not request location permissions on first launch. When user tapped "Start Ride", the app crashed with SecurityException because FOREGROUND_SERVICE_LOCATION permission was not granted.

### Steps to Reproduce
1. Fresh install app on emulator
2. Launch app
3. Tap "Start Ride" button
4. **Expected**: Permission request dialog appears
5. **Actual**: App crashes

### Crash Log
```
java.lang.SecurityException: Starting FGS with type location
callerApp=ProcessRecord{...} targetSDK=35 requires permissions:
all of the permissions allOf=true
[android.permission.FOREGROUND_SERVICE_LOCATION]
any of the permissions allOf=false
[android.permission.ACCESS_COARSE_LOCATION,
 android.permission.ACCESS_FINE_LOCATION]
```

### Current Workaround
Manual permission grant via adb:
```bash
adb shell pm grant com.example.bikeredlights android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.example.bikeredlights android.permission.ACCESS_COARSE_LOCATION
```

### Analysis
**File**: `app/src/main/AndroidManifest.xml`

Permissions ARE declared in manifest:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
```

**Problem**: No runtime permission request flow implemented. App assumes permissions are already granted.

### Resolution

**Strategy Implemented**: Permission check in IdleContent composable (lightweight, UI-only approach)

**Approach**: Instead of adding ViewModel state or new screens, implemented permission handling directly in the IdleContent composable using Compose permission APIs. This keeps the logic localized and follows Android best practices.

**Implementation Details** (commit b534d23):

1. **Added permission check helper function**:
```kotlin
private fun hasLocationPermissions(context: Context): Boolean {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fineLocationGranted || coarseLocationGranted
}
```

2. **Implemented permission launcher in IdleContent**:
```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

    if (fineLocationGranted || coarseLocationGranted) {
        onStartRide()  // Start ride immediately
    } else {
        showPermissionDeniedDialog = true  // Show rationale
    }
}
```

3. **Modified "Start Ride" button onClick**:
```kotlin
Button(
    onClick = {
        if (hasLocationPermissions(context)) {
            onStartRide()  // Permissions already granted
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
)
```

4. **Added permission denied dialog**:
```kotlin
if (showPermissionDeniedDialog) {
    AlertDialog(
        onDismissRequest = { showPermissionDeniedDialog = false },
        title = { Text("Location Permission Required") },
        text = {
            Text(
                "BikeRedlights needs location permission to track your ride. " +
                "Please grant location permission in Settings to use ride recording."
            )
        },
        confirmButton = {
            TextButton(onClick = { showPermissionDeniedDialog = false }) {
                Text("OK")
            }
        }
    )
}
```

**Files Modified**:
- `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt`
  - Lines 1-32: Added permission handling imports
  - Lines 144-253: Rewrote IdleContent with permission flow

**Why This Approach**:
- ✅ No new ViewModel state needed (simpler)
- ✅ No new screens/dialogs needed (uses Android's native permission UI)
- ✅ Follows Compose best practices (rememberLauncherForActivityResult)
- ✅ Minimal code changes (localized to one composable)
- ✅ User understands context when permission is requested (at "Start Ride" tap)

**Testing**:
- Revoked permissions via adb
- Installed fresh APK
- App no longer crashes on "Start Ride"
- Android's native permission dialog appears
- Ride starts immediately after granting permissions
- Rationale dialog shows if permissions denied

---

## Bug #4: Missing Current Time Display

### Severity
**LOW** - Enhancement request from user, not blocking

### Description
The Live view does not display the current time (clock). User requested this to help track when ride started and current elapsed time context.

### User Request
From emulator testing session (2025-11-05):
> "we should display the current time as well in the data displayed"

### Proposed Fix

**Strategy**: Add current time ticker to UI state

**Implementation**:
1. Add `currentTime: Flow<Long>` to RideRecordingViewModel that emits every second
2. Display formatted time in LiveRideScreen header (e.g., "11:42 AM")
3. Use Material 3 Typography.labelLarge for time display
4. Position in top-right corner of statistics card

**Code changes needed**:
```kotlin
// In RideRecordingViewModel.kt
val currentTime: StateFlow<Long> = flow {
    while (currentCoroutineContext().isActive) {
        emit(System.currentTimeMillis())
        delay(1000)
    }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

// In LiveRideScreen.kt
val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()

Text(
    text = formatTime(currentTime),  // "11:42 AM"
    style = MaterialTheme.typography.labelLarge,
    modifier = Modifier.align(Alignment.TopEnd)
)
```

**Files to modify**:
- `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`
- `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt`
- `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt` (add formatTime utility)

**Tests to add**:
- UI test: Verify current time displays and updates every second
- UI test: Verify time format is correct (12-hour vs 24-hour based on locale)

---

## Phase 6 Task Status

### Completed Tasks
✅ **T076**: Read UnitsSystem from SettingsRepository in RideRecordingViewModel
✅ **T077**: Implement meters → km/miles conversion in RideRecordingViewModel
✅ **T078**: Implement m/s → km/h/mph conversion in RideRecordingViewModel
✅ **T079**: Update RideStatistics to display units labels (km, km/h, miles, mph)

### Blocked Tasks (Pending Bug Fixes)
❌ **T080-T099**: All remaining Phase 6 tasks blocked by Bug #1 and Bug #2

**Reason**: Cannot test GPS accuracy settings, auto-pause, or units conversion in live recording until real-time updates are fixed. The bugs prevent accurate validation of these features.

---

## Impact Assessment

### User Experience Impact
- **Critical**: Users cannot use app as intended (no live statistics)
- **Frustrating**: Must interact with UI or navigate away to see updates
- **Confusing**: Duration continues when paused (incorrect statistics)
- **Blocking**: App crashes on first launch without manual permission grant

### Development Impact
- **Phase 6 incomplete**: ~19 of 24 tasks blocked by Bug #1 and Bug #2
- **Phase 7+ delayed**: Review screen, edge cases, and polish cannot be tested properly
- **Test coverage**: Cannot achieve 90%+ coverage requirement until bugs fixed
- **Release timeline**: v0.3.0 release delayed until critical bugs resolved

### Technical Debt
- **Architecture decision**: Event-driven vs timer-based updates needs architectural review
- **State management**: Pause state tracking needs refactor
- **Permission handling**: Runtime permission flow needs implementation from scratch

---

## Recommended Priority

**Immediate (This Sprint)**:
1. **Bug #1**: Duration not updating (CRITICAL - core feature broken)
2. **Bug #2**: Duration continues when paused (CRITICAL - data integrity issue)

**Next Sprint**:
3. **Bug #3**: Missing permission request UI (HIGH - crashes on first launch)

**Future Enhancement**:
4. **Bug #4**: Missing current time display (LOW - nice-to-have, not blocking)

---

## Next Steps

1. **Create separate bug fix branch**: `bugfix/002-real-time-updates`
2. **Fix Bug #1**: Implement timer-based duration updates in Service
3. **Fix Bug #2**: Add pause timestamp tracking and exclude from duration
4. **Test fixes on emulator**: Verify real-time updates work without GPS movement
5. **Merge bug fixes**: Back to `002-core-ride-recording` branch
6. **Resume Phase 6**: Complete remaining tasks (T080-T099)
7. **Create separate enhancement branch**: `feat/002-permission-flow` for Bug #3
8. **Defer Bug #4**: Add to Feature 003 or future enhancement backlog

---

## Testing Notes

### Emulator Configuration Used
- **Device**: Pixel 6 emulator (API 35, Android 15)
- **Permissions**: Manually granted via adb (Bug #3 workaround)
- **GPS**: No movement simulation (exposed Bug #1)

### Manual Test Results
- ✅ App launches successfully after permission grant
- ❌ Duration does not update in real-time (stays at 00:00:00)
- ❌ Duration continues when paused (18s → 21s in 3 seconds)
- ⚠️ Distance/speed not tested (no GPS movement in emulator)
- ⚠️ Auto-pause not tested (depends on Bug #2 fix)

---

**Report Complete**: 4 bugs documented with root cause analysis and proposed fixes. Phase 6 marked as partially complete pending bug resolution.
