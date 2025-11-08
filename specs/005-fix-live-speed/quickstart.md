# Developer Quick Start: Testing Current Speed Fix

**Date**: 2025-11-07
**Feature**: Fix Live Current Speed Display Bug
**Branch**: `005-fix-live-speed`

## Overview

This guide helps developers test the current speed fix locally using Android Emulator GPS simulation or physical device testing. Follow these steps to verify the bug fix works correctly.

## Prerequisites

- Android Studio (latest stable)
- Android Emulator with Google Play (API 34+) OR physical Android device
- BikeRedlights app built from `005-fix-live-speed` branch
- Location permissions granted to the app

---

## Quick Test (Emulator - 5 Minutes)

### 1. Start Emulator

```bash
# List available emulators
emulator -list-avds

# Start emulator (use your AVD name)
emulator -avd Pixel_6_API_34 &
```

### 2. Install Debug Build

```bash
cd /Users/vlb/AndroidStudioProjects/BikeRedlights
./gradlew installDebug
```

### 3. Open Extended Controls

In the emulator window, click the "..." (More) button → **Location** tab.

### 4. Simulate Movement

**Single Point Method** (Quick test):
1. Enter coordinates: Latitude `37.422000`, Longitude `-122.084000`
2. Click **Send**
3. Open BikeRedlights app → Start Ride
4. Change coordinates: Latitude `37.423000`, Longitude `-122.085000`
5. Click **Send** again
6. **Expected Result**: Current speed displays ~10-15 km/h

**GPX Route Method** (Realistic test):
1. Load test GPX file (see [Test Data](#test-data) section)
2. Click **Play** button
3. Adjust speed slider to simulate cycling speed (10-25 km/h)
4. Open BikeRedlights app → Start Ride
5. **Expected Result**: Current speed updates continuously as route plays

---

## Test Data

### GPX Test Route (Cycling Loop)

Create `test-route.gpx` with this content:

```xml
<?xml version="1.0"?>
<gpx version="1.1" creator="BikeRedlights Test">
  <trk>
    <name>Test Cycling Route</name>
    <trkseg>
      <!-- Start point -->
      <trkpt lat="37.422000" lon="-122.084000">
        <ele>10</ele>
        <time>2025-11-07T00:00:00Z</time>
      </trkpt>
      <!-- 100m north (walking speed ~3 km/h) -->
      <trkpt lat="37.422900" lon="-122.084000">
        <ele>10</ele>
        <time>2025-11-07T00:02:00Z</time>
      </trkpt>
      <!-- 200m north (cycling speed ~18 km/h) -->
      <trkpt lat="37.423800" lon="-122.084000">
        <ele>10</ele>
        <time>2025-11-07T00:02:40Z</time>
      </trkpt>
      <!-- 300m east (cycling speed ~22 km/h) -->
      <trkpt lat="37.423800" lon="-122.081300">
        <ele>10</ele>
        <time>2025-11-07T00:03:20Z</time>
      </trkpt>
      <!-- Stop point -->
      <trkpt lat="37.423800" lon="-122.081300">
        <ele>10</ele>
        <time>2025-11-07T00:03:50Z</time>
      </trkpt>
    </trkseg>
  </trk>
</gpx>
```

**Usage**:
1. Save file to your computer
2. Emulator → Extended Controls → Location → Load GPX/KML file
3. Click **Play** to simulate movement

---

## Physical Device Testing (Recommended)

Physical device testing is **mandatory** before merging to main per BikeRedlights constitution.

### Setup

```bash
# Connect device via USB
adb devices  # Verify device listed

# Install debug build
./gradlew installDebug

# Enable location high accuracy
adb shell settings put secure location_mode 3

# Check location providers
adb shell dumpsys location
```

### Test Scenarios

#### Scenario 1: Walking Test (Low Speed)

1. Open BikeRedlights app
2. Start a ride
3. Walk outdoors (or indoors if GPS available)
4. **Expected**: Speed displays 1-5 km/h
5. Stop walking
6. **Expected**: Speed drops to 0.0 km/h within 2-4 seconds

**Why This Matters**: Verifies low-speed detection and stationary threshold (< 1 km/h resets to 0.0).

---

#### Scenario 2: Cycling Test (Normal Speed)

1. Start a ride
2. Cycle at normal pace (15-25 km/h)
3. **Expected**: Speed displays current cycling speed
4. Accelerate to 30+ km/h
5. **Expected**: Speed updates reflect acceleration
6. Brake to stop
7. **Expected**: Speed decreases gradually, then 0.0

**Why This Matters**: Verifies real-world GPS tracking at cycling speeds and smooth updates.

---

#### Scenario 3: Pause/Resume Test

1. Start a ride
2. Cycle to build up speed (20 km/h)
3. **Manually pause** the ride
4. **Expected**: Speed immediately resets to 0.0
5. **Resume** the ride
6. Start cycling again
7. **Expected**: Speed updates resume normally

**Why This Matters**: Verifies speed resets on pause and resumes correctly.

---

#### Scenario 4: Auto-Pause Test

1. Start a ride with auto-pause enabled (Settings → Ride & Tracking → Auto-Pause: ON)
2. Cycle at 20 km/h
3. Stop for 5 seconds (stationary)
4. **Expected**: Ride auto-pauses, speed resets to 0.0
5. Start cycling again
6. **Expected**: Ride auto-resumes, speed updates resume

**Why This Matters**: Verifies speed behavior with auto-pause feature (recently fixed in v0.4.1).

---

#### Scenario 5: Configuration Change Test (Screen Rotation)

1. Start a ride
2. Cycle to build up speed (15 km/h)
3. **Rotate screen** (portrait ↔ landscape)
4. **Expected**: Speed value persists across rotation (does NOT reset to 0.0)
5. **Expected**: UI continues updating with current speed

**Why This Matters**: Verifies StateFlow survives configuration changes via ViewModel.

---

#### Scenario 6: Units Conversion Test

1. Start a ride
2. Cycle at known speed (use bike computer or GPS app as reference)
3. **Metric Mode** (Settings → Units: Metric):
   - **Expected**: Speed displays in km/h
   - Example: 20 km/h
4. **Stop ride**
5. Change to Imperial mode (Settings → Units: Imperial)
6. **Start new ride**, cycle at same speed
7. **Imperial Mode**:
   - **Expected**: Speed displays in mph
   - Example: 12.4 mph (20 km/h ≈ 12.4 mph)

**Why This Matters**: Verifies unit conversion works correctly for both metric and imperial users.

---

## Expected Behavior Summary

| User Action | Expected Current Speed | Update Frequency |
|-------------|----------------------|------------------|
| Ride not started | 0.0 km/h | N/A (static) |
| Ride started, stationary | 0.0 km/h | Every 1-4s (GPS updates) |
| Walking (2-5 km/h) | 2-5 km/h | Every 1-4s |
| Cycling (15-30 km/h) | 15-30 km/h | Every 1-4s |
| Stopped (< 1 km/h) | 0.0 km/h | Every 1-4s |
| Ride paused | 0.0 km/h | Static (no updates) |
| Ride resumed | Resumes updating | Every 1-4s |
| Screen rotation | Value persists | Continues updating |
| Units changed | Value converts | Same frequency |

---

## Debugging Tips

### Issue: Speed Always Shows 0.0

**Possible Causes**:
1. **GPS not enabled**: Check device location settings
2. **No GPS signal**: Test outdoors or near window
3. **Location permissions denied**: Grant in Settings → Apps → BikeRedlights → Permissions
4. **Bug not fixed**: Verify you're running code from `005-fix-live-speed` branch

**Debug Steps**:
```bash
# Check GPS status
adb shell dumpsys location | grep -A 20 "GPS Status"

# Check app permissions
adb shell dumpsys package com.example.bikeredlights | grep -A 5 "android.permission.ACCESS_FINE_LOCATION"

# View app logs (filter for speed-related)
adb logcat | grep -i "speed\|location"
```

---

### Issue: Speed Updates Are Jerky/Erratic

**Possible Causes**:
1. **Poor GPS accuracy**: Test outdoors with clear sky view
2. **GPS Accuracy setting**: Check Settings → Ride & Tracking → GPS Accuracy
   - High Accuracy: 1s updates (smoother)
   - Battery Saver: 4s updates (choppier)
3. **Normal GPS variance**: ±10% speed accuracy is expected

**Expected Behavior**: GPS speed can vary ±2 km/h even when cycling at constant speed. This is normal GPS behavior.

---

### Issue: Speed Doesn't Reset to 0.0 When Stopped

**Possible Causes**:
1. **GPS drift**: Location hardware reports small non-zero speeds even when stationary
2. **Threshold too low**: Speed < 0.278 m/s (< 1 km/h) should reset to 0.0

**Debug Steps**:
```bash
# Check raw GPS speed values in logs
adb logcat | grep "speedMps\|currentSpeed"
```

**Expected**: Service logs should show speed values. If < 0.278 m/s, should reset to 0.0.

---

## Verification Checklist

Before marking bug fix as complete, verify:

- [ ] Speed displays non-zero value when moving (emulator or physical device)
- [ ] Speed updates in real-time (1-4 second intervals)
- [ ] Speed resets to 0.0 when stationary (< 1 km/h)
- [ ] Speed resets to 0.0 when ride paused
- [ ] Speed resumes updating when ride resumed
- [ ] Speed survives screen rotation (configuration change)
- [ ] Speed displays in km/h when units = Metric
- [ ] Speed displays in mph when units = Imperial
- [ ] Speed is accurate within ±10% of actual speed (physical device only)
- [ ] No crashes or ANR events during testing
- [ ] Logcat shows no errors related to speed tracking

---

## Common Pitfalls

### Pitfall 1: Hardcoded 0.0 Still in Code

**Symptom**: Speed always shows 0.0 even after fix.

**Cause**: Forgot to update LiveRideScreen.kt line 350:
```kotlin
// BEFORE (broken)
RideStatistics(
    ride = ride,
    currentSpeed = 0.0,  // ❌ Hardcoded
    unitsSystem = unitsSystem,
)

// AFTER (fixed)
val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
RideStatistics(
    ride = ride,
    currentSpeed = currentSpeed,  // ✅ Reactive
    unitsSystem = unitsSystem,
)
```

---

### Pitfall 2: Not Collecting StateFlow in UI

**Symptom**: Compile error or speed not updating.

**Cause**: Missing `collectAsStateWithLifecycle()`:
```kotlin
// WRONG: Doesn't collect StateFlow
val currentSpeed = viewModel.currentSpeed  // Type: StateFlow<Double>

// CORRECT: Collects into Compose State
val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()  // Type: Double
```

---

### Pitfall 3: Converting Units in Wrong Layer

**Symptom**: Speed displays incorrect values or doesn't respect units setting.

**Cause**: Unit conversion logic in wrong place:
```kotlin
// WRONG: Converting in ViewModel
val currentSpeedKmh: StateFlow<Double> =
    repository.getCurrentSpeed()
        .map { it * 3.6 }  // ❌ Locks units to km/h

// CORRECT: ViewModel exposes raw m/s
val currentSpeed: StateFlow<Double> = repository.getCurrentSpeed()

// UI handles conversion
val displaySpeed = RideRecordingViewModel.convertSpeed(currentSpeed, unitsSystem)
```

---

## Next Steps

After verifying the fix works:

1. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```

2. **Run UI Tests**:
   ```bash
   ./gradlew connectedDebugAndroidTest
   ```

3. **Update Documentation**:
   - TODO.md: Move feature from "Planned" to "In Progress" → "Completed"
   - RELEASE.md: Add to "Unreleased" section

4. **Create PR**:
   ```bash
   git push origin 005-fix-live-speed
   # Create PR on GitHub targeting main branch
   ```

5. **Physical Device Validation** (MANDATORY per constitution):
   - Test all scenarios above on real device
   - Document results in PR description
   - Attach screenshots or video if possible

---

## Support

**Questions?** Check the following resources:
- [Feature Specification](./spec.md) - Requirements and user stories
- [Research Document](./research.md) - Architecture analysis
- [Data Model](./data-model.md) - State entity definition
- [Repository Contract](./contracts/repository-contract.md) - Interface specifications
- [ViewModel Contract](./contracts/viewmodel-contract.md) - UI layer contract

**Issues?** Create GitHub issue with:
- Steps to reproduce
- Expected vs actual behavior
- Android version and device model
- Logcat output (if applicable)
