# Quick Start: Maps Integration

**Feature**: 006-maps-integration
**Target Release**: v0.5.0

---

## 🎯 What You're Building

Add Google Maps visualization to BikeRedlights:
- **Live Tab**: Real-time map with route polyline (55% of screen)
- **Review Screen**: Complete route with start/end markers

**User Value**: Visual context for rides - see where you are and where you've been.

---

## 📋 Prerequisites

Before starting implementation:

1. **Complete Google Cloud Setup**:
   - [ ] Google Cloud project created
   - [ ] Maps SDK for Android API enabled
   - [ ] API key created with Android restriction
   - [ ] Billing account linked (free tier)
   - [ ] API key copied

2. **Verify Feature 1A (Core Recording)**:
   - [ ] Track points being saved to database
   - [ ] Can view completed rides in Review screen
   - [ ] v0.4.2 fully functional

---

## 🚀 Quick Implementation Path (3-4 days)

### Day 1: Setup & Core Components

**Morning** (3 hours):
1. Add dependencies to `libs.versions.toml` and `build.gradle.kts`
2. Add API key to `local.properties` and `AndroidManifest.xml`
3. Add ProGuard rules
4. Gradle sync + verify build

**Afternoon** (4 hours):
5. Create `BikeMap.kt` composable (reusable component)
6. Create `MapUtils.kt` (conversion utilities)
7. Test in Preview with sample data

---

### Day 2: Live Tab Integration

**Morning** (3 hours):
8. Modify `RideRecordingViewModel` (expose track points StateFlow)
9. Start modifying `LiveRideScreen` layout (add map section)

**Afternoon** (4 hours):
10. Finish `LiveRideScreen` map integration
11. Implement camera following and gesture locking
12. Test on emulator with GPS simulation

---

### Day 3: Review Screen + Testing

**Morning** (2 hours):
13. Modify `RideReviewViewModel` (expose track points for selected ride)
14. Replace `MapPlaceholder` with `BikeMap` in `RideReviewScreen`

**Afternoon** (4 hours):
15. Emulator regression testing (Live + Review)
16. Physical device testing (real bike ride)
17. Fix any bugs found

---

### Day 4: Polish & Documentation

**Morning** (2 hours):
18. Performance validation (battery, memory, frame rate)
19. Accessibility testing (TalkBack, touch targets)

**Afternoon** (2 hours):
20. Update TODO.md and RELEASE.md
21. Update CLAUDE.md with Maps setup instructions
22. Final review and sign-off

---

## 🔧 Key Files to Modify

### New Files (Create):
```
app/src/main/java/com/example/bikeredlights/ui/components/map/
├── BikeMap.kt                 # Main reusable map component
└── MapUtils.kt                # Coordinate conversion utilities
```

### Modified Files:
```
gradle/libs.versions.toml      # Add Maps dependencies
app/build.gradle.kts           # Add Maps dependencies + API key config
app/proguard-rules.pro         # Add Maps ProGuard rules
app/src/main/AndroidManifest.xml  # Add API key meta-data

app/src/main/java/com/example/bikeredlights/ui/viewmodel/
├── RideRecordingViewModel.kt  # Add trackPoints StateFlow
└── RideReviewViewModel.kt     # Add trackPoints StateFlow

app/src/main/java/com/example/bikeredlights/ui/screens/ride/
├── LiveRideScreen.kt          # Add map section (Column layout)
└── RideReviewScreen.kt        # Replace MapPlaceholder with BikeMap
```

---

## 📝 Implementation Checklist

### Phase 1: Setup ✅
- [ ] T001: Google Cloud Console setup (interactive guidance)
- [ ] T002: Add dependencies and API key
- [ ] T003: Add ProGuard rules
- [ ] Verify: `./gradlew assembleDebug` succeeds

### Phase 2: Components ✅
- [ ] T004: Create `BikeMap.kt` composable
- [ ] T005: Create `MapUtils.kt` utilities
- [ ] Verify: Preview shows map with sample data

### Phase 3: Live Tab ✅
- [ ] T008: Modify `RideRecordingViewModel`
- [ ] T009: Modify `LiveRideScreen` layout
- [ ] T010: Implement camera following
- [ ] T011: Test on emulator
- [ ] Verify: Map tracks location, polyline grows, gestures locked

### Phase 4: Review Screen ✅
- [ ] T012: Modify `RideReviewViewModel`
- [ ] T013: Replace MapPlaceholder
- [ ] T014: Test on emulator
- [ ] Verify: Complete route displays, start/end markers correct

### Phase 5: Release ✅
- [ ] T015: Physical device testing
- [ ] Update documentation (TODO, RELEASE, CLAUDE)
- [ ] Version bump to v0.5.0 (versionCode = 500)
- [ ] Create pull request
- [ ] Build signed release APK
- [ ] Create GitHub release

---

## 🧪 Quick Testing Guide

### Emulator Testing

**GPS Simulation Setup**:
1. Create `test_route.gpx` file with cycling route
2. Open emulator Extended Controls (...) → Location
3. Load GPX file
4. Set playback speed to 15 km/h
5. Click Play

**Test Scenarios**:
- [ ] Idle state: Map shows current location only
- [ ] Start ride: Camera centers, polyline starts
- [ ] Recording: Polyline grows as simulated movement occurs
- [ ] Gestures locked: Cannot zoom/pan during recording
- [ ] Pause ride: Gestures enabled
- [ ] Stop ride: Polyline clears
- [ ] Review screen: Complete route with markers

### Physical Device Testing (MANDATORY)

**Required Test**:
1. Install debug APK: `./gradlew installDebug`
2. Real bike ride (15-30 minutes)
3. Verify map tracks accurately
4. Measure battery drain (compare to v0.4.2)
5. Check for crashes/ANR events

---

## 🐛 Common Issues & Solutions

### Issue: Map shows blank gray tiles

**Cause**: Invalid or missing API key

**Solution**:
1. Check `local.properties` has `MAPS_API_KEY=...`
2. Check API key enabled in Google Cloud Console
3. Check API key restrictions match package name
4. Rebuild: `./gradlew clean assembleDebug`

---

### Issue: "This app won't run without Google Play Services"

**Cause**: Emulator missing Play Services

**Solution**:
1. Use emulator with Play Store icon (not "Google APIs" only)
2. Or test on physical device

---

### Issue: Polyline not appearing during recording

**Cause**: Track points not flowing to ViewModel

**Solution**:
1. Check `RideRecordingViewModel.trackPoints` is being collected
2. Verify `MapUtils.trackPointsToLatLng()` conversion
3. Check `showPolyline = isRecording` condition
4. Add logging: `Log.d("BikeMap", "Track points: ${trackPoints.size}")`

---

### Issue: App crashes in release build

**Cause**: Missing ProGuard rules for Maps SDK

**Solution**:
1. Verify ProGuard rules added (T003)
2. Test release build: `./gradlew assembleRelease`
3. Check logcat for obfuscation errors

---

## 📚 Quick Reference

### BikeMap Component Usage

**Live Tab**:
```kotlin
BikeMap(
    currentLocation = currentLocation,  // StateFlow from ViewModel
    trackPoints = latLngPoints,         // Converted from TrackPoints
    showPolyline = isRecording,         // Toggle based on state
    cameraFollowsLocation = isRecording,  // Auto-follow during recording
    gesturesEnabled = !isRecording,     // Lock gestures during recording
    onRecenterClick = { /* ... */ }     // Re-center callback
)
```

**Review Screen**:
```kotlin
BikeMap(
    currentLocation = null,             // Not needed
    trackPoints = latLngPoints,         // Complete route
    startMarker = latLngPoints.first(), // Green pin
    endMarker = latLngPoints.last(),    // Red flag
    gesturesEnabled = true              // Allow exploration
)
```

---

### Zoom Levels

| Zoom | View | Use Case |
|------|------|----------|
| 10 | City | Not used |
| 15 | **City block** | **Default (cycling)** |
| 20 | Building | Too zoomed in |

---

### Material 3 Colors

- **Polyline**: `MaterialTheme.colorScheme.primary`
- **Start Marker**: Green (`BitmapDescriptorFactory.HUE_GREEN`)
- **End Marker**: Red (`BitmapDescriptorFactory.HUE_RED`)
- **Current Location**: Blue (`BitmapDescriptorFactory.HUE_AZURE`)

---

## 🎓 Learning Resources

**Essential Reading** (30 minutes):
1. [Maps Compose Quickstart](https://developers.google.com/maps/documentation/android-sdk/maps-compose)
2. [Polylines Guide](https://developers.google.com/maps/documentation/android-sdk/shapes#polylines)

**Sample Code**:
1. [Maps Compose Samples](https://github.com/googlemaps/android-maps-compose/tree/main/app/src/main/java/com/google/maps/android/compose/sample)

---

## ✅ Definition of Done

Before marking feature complete:

**Code**:
- [ ] All files created/modified
- [ ] No compilation errors
- [ ] ProGuard rules added
- [ ] No lint warnings

**Testing**:
- [ ] Emulator testing complete (T011, T014)
- [ ] Physical device testing complete (T015)
- [ ] Performance metrics validated
- [ ] No crashes or memory leaks

**Documentation**:
- [ ] TODO.md updated
- [ ] RELEASE.md updated
- [ ] CLAUDE.md updated
- [ ] spec.md finalized

**Release**:
- [ ] Version bumped to v0.5.0
- [ ] Pull request created
- [ ] Release APK built
- [ ] GitHub release published

---

**Estimated Time**: 3-4 days for full implementation and testing

**Next Feature**: v0.6.0 - Stop Detection Settings (roadmap Phase 3)

---

**Version**: 1.0 | **Created**: 2025-11-08 | **Last Updated**: 2025-11-08
