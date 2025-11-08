# Requirements Checklist: Maps Integration

**Feature**: 006-maps-integration
**Target Release**: v0.5.0
**Created**: 2025-11-08

---

## Functional Requirements

### FR-001: Live Tab Map Display
- [ ] Map occupies 50-60% of screen height on Live tab
- [ ] Map displays in all states (Idle, Recording, Paused, AutoPaused)
- [ ] Map uses Google Maps SDK for Android
- [ ] Map integrates with Material 3 theme (light/dark mode)

### FR-002: Current Location Marker
- [ ] Blue marker shows user's current GPS location
- [ ] Marker updates in real-time during recording
- [ ] Marker visible in all states (Idle, Recording, Paused, AutoPaused)
- [ ] Marker hidden on Review screen

### FR-003: Route Polyline (Live Tab)
- [ ] Polyline shows route traveled from start to current position
- [ ] Polyline grows in real-time as track points added
- [ ] Polyline color uses `MaterialTheme.colorScheme.primary`
- [ ] Polyline width is 8dp
- [ ] Polyline hidden in Idle state
- [ ] Polyline visible in Recording/Paused/AutoPaused states
- [ ] Polyline renders smoothly with 1000+ points

### FR-004: Camera Following
- [ ] Camera auto-centers on current location during recording
- [ ] Camera updates smoothly (1-second animations, no jarring jumps)
- [ ] Camera zoom level fixed at 15f (city block view)
- [ ] Camera following disabled in Idle state
- [ ] Camera following enabled in Recording state

### FR-005: Gesture Locking
- [ ] Map gestures (zoom, pan) locked during active recording
- [ ] Map gestures enabled when paused (manual or auto-pause)
- [ ] Map gestures enabled in Idle state
- [ ] No accidental zoom/pan while riding

### FR-006: Re-center Button
- [ ] FloatingActionButton appears during recording
- [ ] FAB hidden in Idle state
- [ ] FAB positioned bottom-right corner with 16dp padding
- [ ] FAB has MyLocation icon
- [ ] Tapping FAB re-centers camera on current location
- [ ] FAB has 48dp minimum touch target

### FR-007: Review Screen Map
- [ ] Map replaces "Map placeholder" message
- [ ] Map displays complete route polyline
- [ ] Map height is 300dp
- [ ] Map auto-zooms to fit entire route
- [ ] Map has 100px padding around route bounds

### FR-008: Start/End Markers (Review Screen)
- [ ] Green pin marker at first track point (ride start)
- [ ] Red flag marker at last track point (ride end)
- [ ] Markers labeled "Start" and "End"
- [ ] Markers visible on Review screen only

### FR-009: Fallback for Poor GPS
- [ ] Rides with < 2 track points show "No GPS data" message
- [ ] Fallback card matches Material 3 surfaceVariant color
- [ ] Error message explains insufficient GPS coordinates
- [ ] No crash when loading rides without GPS data

### FR-010: Data Flow
- [ ] Track points flow from repository to ViewModel StateFlow
- [ ] UI reacts to StateFlow updates
- [ ] Track points reset when ride stops
- [ ] Memory managed efficiently (< 100MB additional)

---

## Non-Functional Requirements

### NFR-001: Performance
- [ ] Map loads in < 2 seconds on Wi-Fi
- [ ] Polyline renders 1000+ points without lag
- [ ] 60fps maintained during map interactions
- [ ] Battery drain < 5% additional per hour vs. v0.4.2
- [ ] Memory usage < 150MB total app footprint
- [ ] No memory leaks (verified with Android Profiler)

### NFR-002: Offline Support
- [ ] Map works offline with cached tiles
- [ ] Polyline renders offline (client-side, no network)
- [ ] Markers render offline
- [ ] Graceful degradation when tiles unavailable

### NFR-003: Security
- [ ] API key stored in `local.properties` (gitignored)
- [ ] API key not hardcoded in source
- [ ] API key restricted to Android package name
- [ ] SHA-1 certificate fingerprint restriction applied
- [ ] No API key committed to Git

### NFR-004: Material Design 3 Compliance
- [ ] Map integrates with dynamic color scheme
- [ ] Light/dark mode map styles
- [ ] Polyline color uses theme primary color
- [ ] Semantic marker colors (green/red/blue)
- [ ] Elevation and shadows follow M3 guidelines
- [ ] Touch targets ≥ 48dp

### NFR-005: Accessibility
- [ ] BikeMap has semantic contentDescription
- [ ] Re-center FAB has descriptive label
- [ ] Map works with TalkBack enabled
- [ ] High contrast mode supported
- [ ] Screen reader announces map state changes

### NFR-006: Compatibility
- [ ] Works on Android API 34+ (minSdk)
- [ ] Works on Android API 35 (targetSdk)
- [ ] Compatible with Jetpack Compose BOM 2024.11.00
- [ ] Compatible with Play Services Maps 19.0.0+
- [ ] Compatible with Maps Compose 6.2.0+

---

## Architecture Requirements

### AR-001: Clean Architecture
- [ ] UI layer only (no domain/data changes)
- [ ] ViewModels expose track points as StateFlow
- [ ] Reusable BikeMap component
- [ ] No business logic in UI components
- [ ] Follows single responsibility principle

### AR-002: MVVM Pattern
- [ ] ViewModel manages map state
- [ ] UI observes StateFlow reactively
- [ ] No direct repository access from UI
- [ ] State hoisting applied correctly

### AR-003: Separation of Concerns
- [ ] BikeMap.kt: Reusable map display component
- [ ] MapUtils.kt: Coordinate conversion utilities
- [ ] ViewModels: State management only
- [ ] Screens: UI composition only

### AR-004: Reusability
- [ ] BikeMap used in both Live and Review screens
- [ ] Parameters control behavior (not separate components)
- [ ] MapUtils functions pure (no side effects)

---

## Testing Requirements

### TR-001: Unit Tests (Optional for v0.5.0)
- [ ] MapUtils coordinate conversion (optional)
- [ ] MapUtils bounds calculation (optional)

### TR-002: Emulator Tests (MANDATORY)
- [ ] Live tab: Map loads in Idle state
- [ ] Live tab: Polyline grows during GPS simulation
- [ ] Live tab: Camera follows location
- [ ] Live tab: Gestures locked during recording
- [ ] Live tab: Re-center FAB works
- [ ] Review screen: Complete route displays
- [ ] Review screen: Start/end markers positioned correctly
- [ ] Review screen: Auto-zoom fits route
- [ ] Edge case: Rides with < 2 points show fallback
- [ ] Edge case: Screen rotation preserves state
- [ ] Edge case: Dark mode styling correct

### TR-003: Physical Device Tests (MANDATORY)
- [ ] Real bike ride (15-30 minutes minimum)
- [ ] Map tracks location accurately
- [ ] Polyline renders smoothly
- [ ] Battery drain measured (< 5% additional per hour)
- [ ] Cellular data usage reasonable (< 5MB per hour)
- [ ] No crashes or ANR events
- [ ] Memory usage stable

### TR-004: Performance Tests
- [ ] Map load time < 2 seconds
- [ ] Polyline with 1000 points: 60fps
- [ ] Memory leak detection (Android Profiler)
- [ ] Battery profiling (compare v0.4.2 baseline)

### TR-005: Accessibility Tests
- [ ] TalkBack announces map elements
- [ ] Touch targets ≥ 48dp
- [ ] High contrast mode works
- [ ] Screen reader reads FAB label

---

## Documentation Requirements

### DR-001: Specification Files
- [ ] spec.md complete with user stories
- [ ] plan.md complete with implementation plan
- [ ] tasks.md complete with 15 tasks
- [ ] data-model.md complete (no schema changes)
- [ ] research.md complete with technical decisions
- [ ] quickstart.md complete with quick reference

### DR-002: Project Documentation
- [ ] TODO.md updated with Feature 006 entry
- [ ] RELEASE.md updated with v0.5.0 section
- [ ] CLAUDE.md updated with Maps SDK setup instructions
- [ ] API key setup guide for future developers

### DR-003: Code Documentation
- [ ] BikeMap.kt has KDoc comments
- [ ] MapUtils.kt has KDoc comments
- [ ] Complex functions have inline comments
- [ ] Parameters documented with @param tags

---

## Release Requirements

### RR-001: Version Management
- [ ] Version bumped to v0.5.0
- [ ] versionCode = 500 (calculation: 0*10000 + 5*100 + 0)
- [ ] versionName = "0.5.0"
- [ ] Git tag created: `v0.5.0`

### RR-002: Build Artifacts
- [ ] Debug APK builds successfully
- [ ] Release APK builds successfully with ProGuard
- [ ] Signed release APK created
- [ ] APK size < 26MB (debug), < 22MB (release)

### RR-003: Git Management
- [ ] Feature branch: `006-maps-integration`
- [ ] Commits follow conventional commits format
- [ ] Small, frequent commits (not one giant commit)
- [ ] Pull request created
- [ ] PR description includes:
  - Feature summary
  - Link to specification
  - Emulator testing confirmation
  - Physical device testing confirmation
  - Breaking changes (none expected)

### RR-004: GitHub Release
- [ ] Release created on GitHub
- [ ] Title: "v0.5.0 - Maps Integration"
- [ ] Body: Copied from RELEASE.md v0.5.0 section
- [ ] Signed APK attached (renamed to `BikeRedlights-v0.5.0.apk`)
- [ ] Release notes mention Google Maps API key requirement

---

## Google Cloud Requirements

### GC-001: Project Setup
- [ ] Google Cloud project created or selected
- [ ] Billing account linked
- [ ] Free tier confirmed (28,000 map loads/month)

### GC-002: API Configuration
- [ ] Maps SDK for Android API enabled
- [ ] API key created
- [ ] API key restricted to Android apps
- [ ] Package name restriction: `com.example.bikeredlights`
- [ ] SHA-1 fingerprint added (debug + release keystores)

### GC-003: Monitoring
- [ ] API usage dashboard accessible
- [ ] Quota limits monitored
- [ ] Billing alerts configured (optional)

---

## ProGuard Requirements

### PG-001: Configuration
- [ ] ProGuard rules added for Maps SDK
- [ ] ProGuard rules added for Maps Compose
- [ ] No warnings in release build
- [ ] Release APK tested on device

---

## Dependencies Requirements

### DEP-001: Gradle Configuration
- [ ] `play-services-maps:19.0.0` added
- [ ] `maps-compose:6.2.0` added
- [ ] `maps-compose-utils:6.2.0` added
- [ ] Dependencies synced successfully
- [ ] No version conflicts

### DEP-002: Manifest Configuration
- [ ] API key meta-data added to AndroidManifest.xml
- [ ] API key loaded from `local.properties`
- [ ] manifestPlaceholders configured in build.gradle.kts

---

## Acceptance Criteria Summary

**Feature Complete When**:
- ✅ All functional requirements (FR-001 to FR-010) met
- ✅ All non-functional requirements (NFR-001 to NFR-006) met
- ✅ All architecture requirements (AR-001 to AR-004) met
- ✅ All testing requirements (TR-001 to TR-005) met
- ✅ All documentation requirements (DR-001 to DR-003) met
- ✅ All release requirements (RR-001 to RR-004) met
- ✅ All Google Cloud requirements (GC-001 to GC-003) met
- ✅ All ProGuard requirements (PG-001) met
- ✅ All dependency requirements (DEP-001 to DEP-002) met

**Total Requirements**: 84 checkboxes

---

**Version**: 1.0 | **Created**: 2025-11-08 | **Last Updated**: 2025-11-08
