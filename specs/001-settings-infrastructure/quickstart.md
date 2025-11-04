# Quickstart Guide: Basic Settings Infrastructure

**Feature**: 001-settings-infrastructure
**For**: Developers implementing or testing this feature
**Date**: 2025-11-04

## Overview

This guide helps developers quickly set up, build, run, and test the Basic Settings Infrastructure feature in isolation. Follow these steps to verify your implementation or test the feature on an emulator.

---

## Prerequisites

### Required Software

1. **Android Studio**: Latest stable version (Iguana or newer)
   - Download: [https://developer.android.com/studio](https://developer.android.com/studio)

2. **Java 17 (OpenJDK 17)**: Required for Kotlin 2.0.21
   ```bash
   # Install on macOS with Homebrew
   brew install openjdk@17

   # Set environment variables (add to ~/.zshrc or ~/.bash_profile)
   export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
   export JAVA_HOME="/opt/homebrew/opt/openjdk@17"

   # Verify installation
   java -version  # Should show OpenJDK 17
   ```

3. **Android Emulator**: Pixel 6 or similar (API 34+)
   - Create via Android Studio → Device Manager → Create Device
   - System Image: API 34 or 35 (Android 14 or 15)

### Project Dependencies (Already Configured)

All dependencies are in `app/build.gradle.kts`:
- Jetpack Compose BOM 2024.11.00
- DataStore Preferences 1.1.1
- Kotlin Coroutines 1.9.0
- Navigation Compose 2.8.5
- Material 3 (via Compose BOM)

---

## Quick Start (5 Minutes)

### 1. Clone and Open Project

```bash
# If not already cloned
git clone <repo-url> BikeRedlights
cd BikeRedlights

# Switch to feature branch
git checkout 001-settings-infrastructure

# Open in Android Studio
# File → Open → Select BikeRedlights directory
```

### 2. Sync Project

```bash
# In Android Studio terminal
./gradlew sync

# Or: File → Sync Project with Gradle Files
```

### 3. Build Debug APK

```bash
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 4. Start Emulator

```bash
# List available emulators
emulator -list-avds

# Start emulator (replace with your AVD name)
emulator -avd Pixel_6_API_34 &

# Or: Android Studio → Device Manager → Run button
```

### 5. Install and Launch

```bash
# Install APK
./gradlew installDebug

# Or: Drag APK to emulator

# Launch app
adb shell am start -n com.example.bikeredlights/.ui.MainActivity
```

---

## Feature Testing Checklist

### Settings Navigation

**Objective**: Verify Settings tab is accessible and navigates correctly.

**Steps**:
1. Launch app → should land on Live tab (default)
2. Tap "Settings" in bottom navigation (3rd tab)
3. Should see Settings home screen with "Ride & Tracking" card

**Expected Results**:
- ✅ Bottom navigation shows 3 tabs: Live, Rides, Settings
- ✅ Settings tab icon is a gear/cog
- ✅ Ride & Tracking card displays with subtitle "Units, GPS, Auto-pause"

**Screenshot Location**: Capture for documentation

---

### Units Setting

**Objective**: Verify units preference changes and persists.

**Steps**:
1. Navigate to Settings → Tap "Ride & Tracking" card
2. Should see "Units" with Metric/Imperial segmented button
3. Verify "Metric" is selected by default (filled background)
4. Tap "Imperial" → should change to selected state
5. Go back to Settings home
6. Return to Live tab
7. Check speed display → should show "mph" (not "km/h")
8. Close app (swipe from recents)
9. Reopen app → Navigate to Settings → Ride & Tracking
10. Verify "Imperial" is still selected

**Expected Results**:
- ✅ Default: Metric selected
- ✅ Tap Imperial: Selection changes immediately
- ✅ Speed unit changes to "mph" on Live tab
- ✅ Selection persists across app restart

**Test Values**:
- Metric: 25 km/h
- Imperial: 15.5 mph (approximately)

---

### GPS Accuracy Setting

**Objective**: Verify GPS accuracy preference changes and persists.

**Steps**:
1. Navigate to Settings → Ride & Tracking
2. Should see "GPS Accuracy" with Battery Saver/High Accuracy segmented button
3. Verify "High Accuracy" is selected by default
4. Tap "Battery Saver" → should change to selected state
5. Close app and reopen
6. Navigate back to GPS Accuracy setting
7. Verify "Battery Saver" is still selected

**Expected Results**:
- ✅ Default: High Accuracy selected
- ✅ Tap Battery Saver: Selection changes immediately
- ✅ Selection persists across app restart

**Battery Verification** (optional, requires ride recording):
- Start ride with High Accuracy → Monitor battery drain over 10 minutes
- Start ride with Battery Saver → Monitor battery drain over 10 minutes
- Battery Saver should drain 30-50% less GPS power

---

### Auto-Pause Setting

**Objective**: Verify auto-pause toggle and threshold picker.

**Steps**:
1. Navigate to Settings → Ride & Tracking
2. Should see "Auto-Pause Rides" with toggle OFF by default
3. Tap toggle ON → Number picker should appear
4. Verify default threshold is "5 minutes"
5. Tap picker → Should show options: 1, 2, 3, 5, 10, 15
6. Select "3 minutes"
7. Toggle OFF → Picker should still show "3 minutes"
8. Toggle ON again → Still shows "3 minutes" (value persisted)
9. Close app and reopen
10. Navigate back to Auto-Pause setting
11. Verify: Toggle ON, threshold "3 minutes"

**Expected Results**:
- ✅ Default: Toggle OFF, threshold 5 minutes
- ✅ Toggle ON: Picker appears
- ✅ Threshold options: 1, 2, 3, 5, 10, 15 (discrete values only)
- ✅ Threshold persists when toggle OFF
- ✅ Both toggle state and threshold persist across restart

**Auto-Pause Behavior** (requires ride recording, defer to Feature 1A):
- Start ride with auto-pause enabled (3 min)
- Remain stationary (speed < 1 km/h) for 3 minutes
- Should see "⏸️ Paused" indicator on Live tab
- Move (speed > 1 km/h) → Ride auto-resumes

---

### Dark Mode

**Objective**: Verify settings UI works in dark mode.

**Steps**:
1. Enable dark mode in emulator:
   - Settings → Display → Dark theme → ON
   - Or: Quick settings panel → Dark theme toggle
2. Open BikeRedlights app
3. Navigate to Settings → Ride & Tracking
4. Verify all UI elements visible and readable

**Expected Results**:
- ✅ Settings cards have dark background
- ✅ Text is light-colored (white or light gray)
- ✅ Segmented buttons adapt to dark theme
- ✅ Selected state is clearly visible
- ✅ All icons visible (not blending into background)

---

### Accessibility (Font Scaling)

**Objective**: Verify settings UI works at 200% font scaling.

**Steps**:
1. Enable large fonts in emulator:
   - Settings → Accessibility → Font size → Largest
   - Or: Settings → Display → Font size → Largest
2. Open BikeRedlights app
3. Navigate to Settings → Ride & Tracking
4. Verify text doesn't overflow or truncate

**Expected Results**:
- ✅ All labels readable (no truncation with "...")
- ✅ Cards resize to fit larger text
- ✅ No text overlap
- ✅ Segmented button text fits within buttons

---

### TalkBack (Screen Reader)

**Objective**: Verify settings UI is navigable with TalkBack.

**Steps**:
1. Enable TalkBack in emulator:
   - Settings → Accessibility → TalkBack → ON
   - Tutorial will appear (complete or skip)
2. Navigate to Settings tab (swipe right until "Settings" announced, double-tap)
3. Navigate to Ride & Tracking card (swipe right, double-tap)
4. Navigate through each setting (swipe right to hear announcements)

**Expected Announcements**:
- ✅ Settings tab: "Settings"
- ✅ Ride & Tracking card: "Ride and Tracking settings. Tap to configure units, GPS accuracy, and auto-pause."
- ✅ Units: "Units preference. Currently set to Metric."
- ✅ Imperial button: "Imperial. Tap to switch to miles and miles per hour."
- ✅ GPS Accuracy: "GPS accuracy mode. Currently set to High Accuracy."
- ✅ Auto-Pause toggle: "Auto-pause rides. Currently off. Tap to enable."

---

## Testing with ADB Commands

### View Current Settings (DataStore)

```bash
# Pull DataStore file from emulator
adb pull /data/data/com.example.bikeredlights/files/datastore/user_settings.preferences_pb .

# DataStore uses Protocol Buffers (not human-readable directly)
# Use logcat to see settings changes:
adb logcat | grep "SettingsRepository"
```

### Clear Settings (Reset to Defaults)

```bash
# Clear app data (resets DataStore)
adb shell pm clear com.example.bikeredlights

# Or: Settings → Apps → BikeRedlights → Storage → Clear data

# Relaunch app → All settings reset to defaults
```

### Simulate App Restart

```bash
# Stop app
adb shell am force-stop com.example.bikeredlights

# Start app
adb shell am start -n com.example.bikeredlights/.ui.MainActivity

# Or: Swipe app from recents and reopen
```

---

## Debugging Tips

### Settings Not Persisting

**Symptoms**: Changes revert to defaults after app restart

**Checks**:
1. Verify DataStore write completes before app closes:
   ```kotlin
   // Add delay in test to ensure write finishes
   delay(100) // 100ms should be enough for DataStore write
   ```

2. Check logcat for DataStore errors:
   ```bash
   adb logcat | grep -E "SettingsRepository|DataStore"
   ```

3. Verify DataStore file exists:
   ```bash
   adb shell ls /data/data/com.example.bikeredlights/files/datastore/
   # Should see: user_settings.preferences_pb
   ```

### UI Not Updating

**Symptoms**: Change setting but UI doesn't reflect change

**Checks**:
1. Verify ViewModel is using `StateFlow` and UI collects with `collectAsStateWithLifecycle()`:
   ```kotlin
   val uiState by viewModel.uiState.collectAsStateWithLifecycle()
   ```

2. Check Flow emissions in logcat:
   ```kotlin
   settingsRepository.unitsSystem.onEach { units ->
       Log.d("Settings", "Units changed to: $units")
   }.collect()
   ```

3. Verify ViewModel survives configuration changes (rotation):
   ```bash
   # Rotate emulator (Ctrl+F11 on Mac, Ctrl+F12 on Windows)
   # Settings should persist
   ```

### SegmentedButton Not Showing

**Symptoms**: Units or GPS Accuracy setting shows blank or crashes

**Checks**:
1. Verify Material 3 dependency in `app/build.gradle.kts`:
   ```kotlin
   implementation(platform("androidx.compose:compose-bom:2024.11.00"))
   implementation("androidx.compose.material3:material3")
   ```

2. Check import statements:
   ```kotlin
   import androidx.compose.material3.SingleChoiceSegmentedButtonRow
   import androidx.compose.material3.SegmentedButton
   ```

3. Verify Compose version in logcat:
   ```bash
   adb logcat | grep "Compose"
   ```

---

## Performance Testing

### Measure Settings UI Render Time

```bash
# Enable GPU rendering profile in Developer Options
adb shell settings put global debug.hwui.profile visual_bars

# Navigate to Settings screen
# Watch for green bars (< 16ms is 60fps)

# Or use Profiler in Android Studio:
# Run → Profile 'app' → CPU → Record → Navigate to Settings → Stop
```

### Measure DataStore Write Latency

```kotlin
// In SettingsRepository
suspend fun setUnitsSystem(units: UnitsSystem) {
    val startTime = System.currentTimeMillis()
    try {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNITS_SYSTEM] = units.name.lowercase()
        }
        val duration = System.currentTimeMillis() - startTime
        Log.d("SettingsPerf", "DataStore write took ${duration}ms")
    } catch (e: Exception) {
        Log.e("SettingsRepository", "Error writing units", e)
    }
}
```

**Expected**: < 50ms for 99% of writes

---

## Integration with Other Features

### Feature 1A: Core Ride Recording (Upcoming)

**Units Integration**:
- RideTrackingViewModel reads `unitsSystem` from SettingsRepository
- Speed displayed in km/h (Metric) or mph (Imperial)
- Distance displayed in km (Metric) or miles (Imperial)

**GPS Accuracy Integration**:
- LocationRepository reads `gpsAccuracy` from SettingsRepository
- Configures update interval: 1s (High Accuracy) or 3-5s (Battery Saver)

**Auto-Pause Integration**:
- TrackLocationUseCase reads `autoPauseConfig` from SettingsRepository
- Detects speed < 1 km/h for configured threshold
- Emits "paused" state to RideTrackingViewModel
- UI displays "⏸️ Paused" indicator

**Testing**:
- After Feature 1A implemented, verify:
  - Changing units updates live speed display
  - Changing GPS accuracy affects update frequency
  - Auto-pause triggers and resumes correctly

---

## Troubleshooting

### App Crashes on Launch

**Check**:
1. Java 17 installed: `java -version`
2. Gradle sync completed: `./gradlew sync`
3. Clean build: `./gradlew clean && ./gradlew assembleDebug`
4. Logcat for stack trace: `adb logcat | grep AndroidRuntime`

### Emulator Not Starting

**Check**:
1. AVD configured: Android Studio → Device Manager
2. Emulator path: `which emulator` (should be in Android SDK)
3. HAXM installed: Android Studio → SDK Manager → SDK Tools → Intel x86 Emulator Accelerator

### Settings Not Showing in Bottom Nav

**Check**:
1. Bottom navigation implemented in MainActivity
2. Route defined in NavHost: `composable(BottomNavDestination.SETTINGS.route)`
3. Verify branch: `git branch` (should be on `001-settings-infrastructure`)

---

## Next Steps

After verifying Feature 2A (Basic Settings):

1. **Feature 2B (Stop Detection Settings)**: Add "Stop Detection" card to Settings home
2. **Feature 1A (Core Ride Recording)**: Integrate settings (units, GPS accuracy, auto-pause)
3. **Feature 3 (Ride History)**: Use units setting for displaying historical rides

**Related Documentation**:
- [spec.md](spec.md) - Feature specification
- [data-model.md](data-model.md) - Domain models and validation
- [contracts/datastore-schema.md](contracts/datastore-schema.md) - DataStore contract
- [CLAUDE.md](../../CLAUDE.md) - Android development standards

---

## Support

**Issues**: Create GitHub issue with:
- Steps to reproduce
- Expected vs actual behavior
- Logcat output
- Emulator API level

**Questions**: Refer to CLAUDE.md for Android development standards and patterns.

**Quick Reference**:
- Emulator GPS simulation: Emulator → Extended Controls (...) → Location
- Dark mode toggle: Quick settings → Dark theme
- Font size: Settings → Display → Font size
- TalkBack: Settings → Accessibility → TalkBack
