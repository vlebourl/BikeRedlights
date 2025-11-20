# 🚴 Bike Testing Guide - Feature 009 Stop Detection

**Build Date**: November 20, 2025
**Version**: v0.7.0-dev (44% Feature 009 complete)
**Status**: ✅ **READY FOR REAL-WORLD BIKE TESTING**

---

## ✅ What's Working

### Core Stop Detection (Fully Functional)
- ✅ **Speed Monitoring**: Tracks GPS speed in real-time
- ✅ **Stop Detection**: Detects when speed drops below 3 km/h for 15+ seconds
- ✅ **Consecutive Filtering**: Ignores GPS noise with 3-second consecutive threshold checks
- ✅ **Database Persistence**: Automatically saves stop data (location, timestamp, duration)
- ✅ **Live UI Popup**: Shows "🛑 Stop #N" with live duration counter during stops
- ✅ **Auto-Dismiss**: Popup fades out when you start moving again (3 consecutive seconds above threshold)
- ✅ **Settings Integration**: Uses your custom thresholds from Settings screen

### What's Missing (Non-Critical)
- ⚠️ Stop count display on statistics row (Phase 5 - nice-to-have)
- ⚠️ Comprehensive validation tests (Phase 6-8 - quality assurance)

---

## 📱 Installation on Physical Device

### Step 1: Connect Your Android Device

```bash
# Enable Developer Options on your phone:
# Settings → About Phone → Tap "Build Number" 7 times

# Enable USB Debugging:
# Settings → Developer Options → USB Debugging → ON

# Connect phone to computer via USB cable

# Verify connection
adb devices
# Should show: List of devices attached
#              <device_id>    device
```

### Step 2: Install the Debug APK

```bash
cd /Users/vlb/AndroidStudioProjects/BikeRedlights

# Install APK to connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# If app is already installed, use -r to reinstall:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Success Message**: `Success`

---

## 🚴 How to Test on Your Bike

### Test Scenario 1: Basic Stop Detection

1. **Start the app** on your phone
2. **Go to Settings** → Set thresholds:
   - Speed Threshold: `3.0 km/h` (default)
   - Duration Threshold: `15 seconds` (default)
3. **Go to Live tab** → Tap "Start Ride"
4. **Wait for GPS** (green "GPS Ready" indicator at top-right)
5. **Ride normally** for 30 seconds (>10 km/h)
6. **Come to a complete stop** at a traffic light/stop sign
7. **Wait 15+ seconds** without moving
8. **Expected**:
   - Popup appears: "🛑 Stop #1" with duration counter
   - Duration counts up: 0:15, 0:16, 0:17...
9. **Start moving again** (pedal for 5+ seconds)
10. **Expected**: Popup fades out after 3 seconds of movement

### Test Scenario 2: Multiple Stops

1. Continue the same ride
2. Make 2-3 more stops (traffic lights, intersections)
3. **Expected**:
   - Popup shows "🛑 Stop #2", "🛑 Stop #3", etc.
   - Each stop persists to database
4. **Stop the ride** → Save ride
5. **Check database** (see below for inspection)

### Test Scenario 3: Edge Cases

**GPS Noise (brief speed drops)**:
- Ride slowly (5-8 km/h) with fluctuating GPS
- **Expected**: No false stop detection (needs 3 consecutive seconds below threshold)

**Very Short Stops (<15 seconds)**:
- Stop for 10 seconds, then move
- **Expected**: No popup (didn't meet 15s duration threshold)

**Manual Pause During Stop**:
- Stop, wait for popup to appear
- Tap "Pause" button
- **Expected**: Stop ends immediately, data persists to database

---

## 📊 Capturing Comprehensive Logs

### Option 1: Live Logging (While Riding)

**Start logging BEFORE your ride:**

```bash
# Terminal 1: Start continuous logging to file
adb logcat -v time StopDetection:D RideRecordingService:D *:E > ~/bike_test_$(date +%Y%m%d_%H%M%S).txt

# This captures:
# - All StopDetection messages (Debug level)
# - All RideRecordingService messages (Debug level)
# - All Error messages from any component

# Ride your bike with phone in pocket/mount
# Logs are saved continuously to ~/bike_test_YYYYMMDD_HHMMSS.txt
```

**After your ride:**
```bash
# Stop logging: Press Ctrl+C in terminal
# Open log file:
open ~/bike_test_*.txt
```

### Option 2: Retrieve Logs After Ride

**If you forgot to start logging before riding:**

```bash
# Connect phone to computer
adb devices

# Dump recent logs (last ~30 minutes)
adb logcat -d -v time StopDetection:D RideRecordingService:D *:E > ~/bike_test_after_ride.txt

# Open log file
open ~/bike_test_after_ride.txt
```

### Option 3: Real-Time Monitoring (For Desktop Testing)

**Watch logs in real-time:**

```bash
# Terminal 1: Watch stop detection events only
adb logcat -v time | grep --color=always -E "Stop|STOP|Detection|🛑|⚠️|✅|🚀|💾|📊"

# Terminal 2: Watch all debug logs
adb logcat -v time StopDetection:D RideRecordingService:D *:E
```

---

## 🔍 Log Analysis - What to Look For

### Expected Log Patterns

#### **1. Stop Detection Start**
```
11-20 14:32:15.123 D/StopDetection: ⚠️ DETECTION START: Speed dropped below 3.0km/h (current: 2.1km/h)
11-20 14:32:16.234 D/StopDetection: Detection ongoing: 2/3 consecutive seconds below threshold (1.8km/h)
11-20 14:32:17.345 D/StopDetection: Detection ongoing: 3/3 consecutive seconds below threshold (1.5km/h)
11-20 14:32:17.346 D/StopDetection: 3 consecutive seconds confirmed! Duration so far: 2s (threshold: 15s)
```

#### **2. Stop Confirmation** (after 15 seconds)
```
11-20 14:32:30.456 D/StopDetection: 3 consecutive seconds confirmed! Duration so far: 15s (threshold: 15s)
11-20 14:32:30.457 I/StopDetection: 🛑 STOP CONFIRMED! Duration 15s >= threshold 15s
11-20 14:32:30.458 I/StopDetection: 💾 Inserting stop #1 to database - Location: (37.7749, -122.4194)
11-20 14:32:30.512 I/StopDetection: ✅ Stop #1 persisted with ID: 42
11-20 14:32:30.513 D/RideRecordingService: Stop #1 detected (ID: 42)
```

#### **3. During Active Stop** (every GPS update)
```
11-20 14:32:31.567 D/StopDetection: Speed below threshold during active stop: 0.3km/h (still stopped)
11-20 14:32:32.678 D/StopDetection: Speed below threshold during active stop: 0.0km/h (still stopped)
```

#### **4. Stop End** (rider starts moving)
```
11-20 14:33:05.789 D/StopDetection: ✅ Speed above threshold during stop: 4.2km/h - resume counter 1/3
11-20 14:33:06.890 D/StopDetection: ✅ Speed above threshold during stop: 5.8km/h - resume counter 2/3
11-20 14:33:07.901 D/StopDetection: ✅ Speed above threshold during stop: 7.1km/h - resume counter 3/3
11-20 14:33:07.902 I/StopDetection: 🚀 STOP ENDED! 3 consecutive seconds above threshold - rider moving again
11-20 14:33:07.903 I/StopDetection: 📊 Ending stop ID:42 - Duration: 37s
11-20 14:33:07.954 D/RideRecordingService: Stop ended (ID: 42)
```

#### **5. Detection Cancelled** (brief slowdown, but didn't stop)
```
11-20 14:34:10.123 D/StopDetection: ⚠️ DETECTION START: Speed dropped below 3.0km/h (current: 2.5km/h)
11-20 14:34:11.234 D/StopDetection: Detection ongoing: 2/3 consecutive seconds below threshold (2.8km/h)
11-20 14:34:12.345 D/StopDetection: ❌ Detection cancelled: Speed back above threshold (5.2km/h) before stop confirmed
```

### 🚨 Problem Indicators

**❌ No detection logs at all:**
- Check GPS is working (green indicator at top-right)
- Check Settings thresholds are reasonable (not 0 or 999)
- Check ride is RECORDING (not paused or stopped)

**❌ False positives (stops detected while riding):**
- Look for GPS noise patterns in logs (speed oscillating wildly)
- Check actual riding speed vs threshold (maybe riding too slow?)

**❌ Stops not detected:**
- Check duration (did you wait full 15 seconds?)
- Check consecutive seconds (GPS noise breaking the 3-second rule?)
- Look for "❌ Detection cancelled" messages

**❌ Popup not appearing:**
- Check logs show "Stop #N detected"
- Check LiveRideScreen is visible (not in background)
- Check StateFlow updates are happening

---

## 🗄️ Database Inspection (After Ride)

### View Stop Data in Database Inspector

1. **In Android Studio**: View → Tool Windows → App Inspection
2. **Select your device** from dropdown
3. **Navigate to**: Database → BikeRedlightsDatabase → stops table
4. **Click "Live updates"** checkbox
5. **Run queries**:

```sql
-- View all stops for a ride
SELECT * FROM stops WHERE ride_id = <your_ride_id> ORDER BY stop_number;

-- Check stop count
SELECT COUNT(*) FROM stops WHERE ride_id = <your_ride_id>;

-- View stops with durations
SELECT stop_number, start_timestamp, end_timestamp, duration_seconds
FROM stops
WHERE ride_id = <your_ride_id>;
```

### Expected Data Structure

| Column | Example Value | Notes |
|--------|---------------|-------|
| id | 42 | Auto-increment primary key |
| ride_id | 10 | Foreign key to rides table |
| stop_number | 1 | Sequential within ride (1, 2, 3...) |
| latitude | 37.7749 | Stop location (degrees) |
| longitude | -122.4194 | Stop location (degrees) |
| start_timestamp | 1700000000000 | Unix timestamp (ms) |
| end_timestamp | 1700000037000 | Unix timestamp (ms) or NULL if active |
| duration_seconds | 37 | Calculated duration or NULL if active |
| cluster_id | NULL | For Feature 010 (future) |

---

## 🐛 Common Issues & Solutions

### Issue: "adb: device unauthorized"
**Solution**: Check phone screen - accept "Allow USB debugging?" dialog

### Issue: "App not installed" error
**Solution**:
```bash
# Uninstall old version first
adb uninstall com.example.bikeredlights

# Then reinstall
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Issue: GPS not working on device
**Solution**:
- Enable Location Services in phone Settings
- Grant Location permission to BikeRedlights app
- Go outside or near window (GPS needs sky view)

### Issue: Popup not showing during stop
**Solution**:
- Check logs for "Stop #N detected" message
- Verify LiveRideScreen is in foreground
- Check phone isn't in battery saver mode (kills foreground service)

### Issue: Logs show nothing
**Solution**:
```bash
# Verify adb connection
adb devices

# If "device offline", reconnect USB cable

# Test logcat works
adb logcat -d | head -20

# Clear log buffer and try again
adb logcat -c
adb logcat -v time StopDetection:D
```

---

## 📋 Post-Ride Analysis Checklist

After your bike ride, analyze the logs for:

- [ ] **Stop detection accuracy**: Did it detect all real stops?
- [ ] **False positives**: Any stops detected while riding?
- [ ] **Detection timing**: How long after stopping did popup appear?
- [ ] **Resume timing**: How long after moving did popup disappear?
- [ ] **Database persistence**: Do all stops show in Database Inspector?
- [ ] **GPS noise handling**: Were brief speed drops correctly ignored?
- [ ] **Duration accuracy**: Do logged durations match reality?
- [ ] **Stop numbering**: Sequential? (1, 2, 3...)

---

## 🎯 Testing Goals for Tomorrow

1. **Validation**: Does the 15-second threshold feel right in real use?
2. **False Positives**: Any unwanted stop detections during normal riding?
3. **False Negatives**: Any missed stops at intersections/lights?
4. **GPS Noise**: Does the 3-second consecutive filter work well?
5. **UI/UX**: Is the popup visible/readable while riding?
6. **Battery Impact**: How much battery drain during 30-60 min ride?

---

## 📞 Reporting Issues

**Save these files for analysis:**
1. Log file: `~/bike_test_*.txt`
2. Database export: Database Inspector → Export to SQL
3. Ride details: Ride ID, start time, stop count

**Note in your report:**
- Phone model and Android version
- GPS conditions (open road, urban canyon, etc.)
- Riding style (commute, leisure, sport)
- Any unexpected behavior with timestamps

---

**Good luck with your bike test! 🚴‍♂️🛑**
