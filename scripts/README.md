# BikeRedlights Testing Scripts

## GPS Track Playback

Simulate real cycling routes on the Android emulator using GPX files.

### Quick Start

```bash
# 1. Start emulator
emulator -avd Pixel_9_Pro_API_36 &

# 2. Install app
./gradlew installDebug

# 3. Open BikeRedlights and start a ride

# 4. Play GPX track (5x speed recommended)
./scripts/play_gpx_fast.sh test_track.gpx 5
```

### Available Scripts

#### `play_gpx_fast.sh` (Recommended)
Fast and simple - uses `adb emu geo fix`.

**Usage:**
```bash
./scripts/play_gpx_fast.sh <gpx_file> [speed_multiplier]
```

**Examples:**
```bash
# Real-time playback (slow, ~20 minutes for test_track.gpx)
./scripts/play_gpx_fast.sh test_track.gpx 1

# 5x speed (good for testing, ~4 minutes)
./scripts/play_gpx_fast.sh test_track.gpx 5

# 10x speed (quick validation, ~2 minutes)
./scripts/play_gpx_fast.sh test_track.gpx 10
```

#### `play_gpx_emulator.sh`
Uses emulator console (requires authentication).

**Usage:** Same as `play_gpx_fast.sh`

### Test Track

**`test_track.gpx`** - Real cycling ride from Geneva, Switzerland
- 📍 **1,564 GPS points**
- 🚴 **Type:** Road biking
- ⏱️ **Duration:** ~20 minutes (1x speed)
- 📏 **Distance:** ~5-7 km (estimated)
- 🎯 **Perfect for testing:**
  - Speed tracking
  - Auto-pause/resume
  - Stop detection
  - Distance calculation
  - Map route visualization

### What to Test

1. **Speed Tracking**:
   - Current speed updates every second
   - Average/max speed calculated correctly
   - Speed threshold detection

2. **Auto-Pause/Resume**:
   - Pauses when speed < threshold for configured time
   - Auto-resumes when movement detected
   - Manual pause/resume override

3. **Stop Detection** (Feature 009):
   - Stops detected at traffic lights
   - "Stops: N" counter increments
   - Stop popup appears/disappears
   - Stop data saved to database

4. **Distance & Stats**:
   - Total distance matches real ride
   - Moving time vs elapsed time
   - Paused time tracking

5. **Map Visualization** (Feature 006):
   - Route plots correctly on map
   - Current location marker moves
   - Map follows rider position

### Troubleshooting

**GPS not working in app:**
- Grant location permission in Android settings
- Check emulator has location services enabled
- Verify script shows "OK" responses (not "KO")

**Script fails with "more than one device":**
- Use `-s emulator-5554` flag
- Or modify script to specify device

**Emulator console auth required:**
- Use `play_gpx_fast.sh` instead (no auth needed)
- Or set up auth token from `~/.emulator_console_auth_token`

**Speed too fast/slow:**
- Adjust speed multiplier parameter
- Recommended: 5x for testing, 1x for realism

### Creating Your Own Test Tracks

1. **Export from Strava/Garmin/etc:**
   - Download as GPX file
   - Place in repo root

2. **Generate synthetic tracks:**
   ```bash
   # Use GPX editors or online tools
   # Example: gpx.studio, ridewithgps.com
   ```

3. **Play custom track:**
   ```bash
   ./scripts/play_gpx_fast.sh my_custom_track.gpx 5
   ```

### Tips

- **Start app BEFORE playing GPX** - location updates won't be received if app isn't running
- **Use 5-10x speed for testing** - realistic speed takes too long
- **Monitor logcat** for location updates: `adb logcat | grep Location`
- **Take screenshots** during testing: `adb shell screencap -p > screenshot.png`
- **Check database** after test: Use Database Inspector in Android Studio

### Integration with Feature Testing

**Feature 009 (Stop Detection) Test:**
```bash
# 1. Configure stop detection settings (speed: 3 km/h, duration: 15s)
# 2. Start ride in app
# 3. Play GPX at 5x speed
# 4. Observe:
#    - Stop popup appears when stationary
#    - "Stops: N" counter increments
#    - Stop duration updates in real-time
#    - Database contains stop records (check with Inspector)
```

**Feature 006 (Map) Test:**
```bash
# 1. Start ride in app (Live tab)
# 2. Play GPX at 3-5x speed
# 3. Verify:
#    - Blue polyline draws route in real-time
#    - Current location marker moves smoothly
#    - Map camera follows rider position
#    - Route visible on Ride Review after save
```

---

**See also**: `CLAUDE.md` for emulator testing requirements
