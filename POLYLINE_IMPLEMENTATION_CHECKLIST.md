# Polyline Implementation Checklist

**Purpose**: Step-by-step verification guide for implementing polyline rendering in BikeRedlights
**Last Updated**: November 2025
**Target Version**: v0.5.0+

---

## Pre-Implementation Setup

### Research & Planning
- [ ] Read POLYLINE_QUICK_REFERENCE.md completely
- [ ] Review POLYLINE_RESEARCH.md sections 1-4
- [ ] Understand Douglas-Peucker algorithm concept
- [ ] Verify 10m tolerance is acceptable for bike routes
- [ ] Identify which screens need map visualization

### Project Preparation
- [ ] Create feature branch: `git checkout -b 006-route-polylines`
- [ ] Backup current RideDetailScreen.kt and RideReviewScreen.kt
- [ ] Verify current version in app/build.gradle.kts
- [ ] Review current Material 3 theme colors

---

## Phase 1: Dependencies & Setup

### Add Dependencies

#### gradle/libs.versions.toml
- [ ] Add version entry for googleMapsCompose = "6.1.0"
- [ ] Verify no version conflicts with existing libraries
- [ ] Save and sync Gradle

```toml
[versions]
# ... existing ...
googleMapsCompose = "6.1.0"

[libraries]
# ... existing ...
google-maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "googleMapsCompose" }
```

#### app/build.gradle.kts
- [ ] Add implementation(libs.google.maps.compose)
- [ ] Run `./gradlew clean` to clear cached dependencies
- [ ] Run `./gradlew build` to verify dependencies resolve
- [ ] No dependency conflicts reported

### Verify API Key
- [ ] Google Maps API key is configured in AndroidManifest.xml
- [ ] API key has Maps SDK for Android enabled
- [ ] Test on emulator to confirm Maps loads

---

## Phase 2: Create Utility Functions

### Create PolylineUtils.kt

**File Path**: `app/src/main/java/com/example/bikeredlights/domain/util/PolylineUtils.kt`

- [ ] Create file with package declaration
- [ ] Add import statements (LatLng, TrackPoint, PolyUtil)
- [ ] Implement `toLatLngList()` function
- [ ] Implement `toLatLngListFiltered()` function
- [ ] Implement `simplifyRoute()` function
- [ ] Implement `toSimplifiedPolyline()` function
- [ ] Add KDoc documentation for each function
- [ ] No compilation errors
- [ ] Add to git: `git add app/src/main/java/com/example/bikeredlights/domain/util/PolylineUtils.kt`

### Verification
```kotlin
// Quick manual check in Android Studio
val testPoints = listOf<TrackPoint>(...)
val latLng = testPoints.toLatLngList()  // Should compile
val simplified = latLng.simplifyRoute(10.0)  // Should compile
```

---

## Phase 3: Create Composable Component

### Create RoutePolyline.kt

**File Path**: `app/src/main/java/com/example/bikeredlights/ui/components/route/RoutePolyline.kt`

- [ ] Create directory: `ui/components/route/`
- [ ] Create RoutePolyline.kt file
- [ ] Add package declaration
- [ ] Add imports (Compose, GoogleMap, Material3)
- [ ] Implement RoutePolyline composable
  - [ ] Accept trackPoints parameter
  - [ ] Accept modifier parameter with default
  - [ ] Accept color parameter with Material3 default
  - [ ] Accept width parameter with default 8f
  - [ ] Accept showMarkers parameter with default true
- [ ] Use remember() for simplification
- [ ] Handle empty track points case
- [ ] Calculate initial camera position
- [ ] Add Polyline with geodesic = true
- [ ] Add start marker if showMarkers = true
- [ ] Add end marker if showMarkers = true
- [ ] Add KDoc documentation
- [ ] No compilation errors

### Code Review Checklist
- [ ] Uses remember() for optimization
- [ ] Material 3 colors used (not hardcoded)
- [ ] geodesic = true for accuracy
- [ ] clickable = false for performance
- [ ] Handles edge cases (empty points)
- [ ] Properly scoped with Compose best practices

---

## Phase 4: Unit Tests

### Create PolylineUtilsTest.kt

**File Path**: `app/src/test/java/com/example/bikeredlights/domain/util/PolylineUtilsTest.kt`

#### Test: toLatLngList()
- [ ] Create test function
- [ ] Generate 5 mock TrackPoint objects
- [ ] Call toLatLngList()
- [ ] Assert size matches input
- [ ] Assert first latitude matches
- [ ] Assert first longitude matches
- [ ] Test passes without errors

#### Test: toLatLngListFiltered()
- [ ] Create test function
- [ ] Generate TrackPoints with varying accuracy (10f, 50f, 20f)
- [ ] Call with minAccuracy = 30f
- [ ] Assert only 2 points returned (not 3)
- [ ] Assert filtered correctly (no 50f accuracy point)
- [ ] Test passes

#### Test: simplifyRoute()
- [ ] Create test function
- [ ] Generate 100+ synthetic TrackPoints
- [ ] Convert to LatLng list
- [ ] Call simplifyRoute(10.0)
- [ ] Assert simplified.size < original.size * 0.3 (70% reduction)
- [ ] Assert first point equals original first point
- [ ] Assert last point equals original last point
- [ ] Test passes

#### Test: toSimplifiedPolyline()
- [ ] Create test function
- [ ] Generate 3600 mock TrackPoints (1 hour ride)
- [ ] Call toSimplifiedPolyline(10.0)
- [ ] Assert result size between 300-400 points (85-90% reduction)
- [ ] Verify no exceptions thrown
- [ ] Test passes

### Run All Tests
```bash
./gradlew test
```
- [ ] All tests pass
- [ ] No failures or errors
- [ ] Coverage > 80% for PolylineUtils

---

## Phase 5: Screen Integration

### RideDetailScreen Updates

**File**: `app/src/main/java/com/example/bikeredlights/ui/screens/history/RideDetailScreen.kt`

#### ViewModel Changes
- [ ] Add `trackPoints: Flow<List<TrackPoint>>` to RideDetailViewModel
- [ ] Load track points in loadRide() function from repository
- [ ] Expose trackPoints as StateFlow
- [ ] Handle empty track points case
- [ ] Handle error loading track points

#### UI Integration
- [ ] Locate RideDetailContent() function
- [ ] Find where to insert map (above statistics grid)
- [ ] Add RoutePolyline composable
- [ ] Pass trackPoints from ViewModel
- [ ] Set modifier with .fillMaxWidth().height(300.dp)
- [ ] Set color to MaterialTheme.colorScheme.primary
- [ ] Set showMarkers = true
- [ ] No compilation errors
- [ ] UI preview updates show map

### Test Integration
- [ ] Build debug APK: `./gradlew installDebug`
- [ ] Open RideDetailScreen on emulator
- [ ] Select a ride with 100+ track points
- [ ] Verify map appears
- [ ] Verify polyline renders
- [ ] Verify start/end markers visible
- [ ] Verify no crashes or ANR

---

## Phase 6: Emulator Testing

### Setup Emulator Test Environment
- [ ] Emulator running (API 34+)
- [ ] BikeRedlights debug build installed
- [ ] Sample ride data with 100+ points in database
- [ ] Or create test ride by:
  - [ ] Start live ride
  - [ ] Let it record 50+ points
  - [ ] Save ride
  - [ ] Go to history

### Test: 100-Point Route
- [ ] Navigate to RideDetailScreen
- [ ] Select ride with ~100 track points
- [ ] Map loads quickly (< 2 seconds)
- [ ] Polyline renders in correct color
- [ ] Start and end markers visible
- [ ] Can pan map smoothly
- [ ] Can zoom in/out smoothly
- [ ] Frame rate stays 60fps
- [ ] No jank observed

### Test: 1,000-Point Route
- [ ] Create/find ride with ~1,000 points (10+ minutes)
- [ ] Load RideDetailScreen
- [ ] Verify smooth rendering
- [ ] Pan/zoom responsive
- [ ] No memory spikes (< 100MB)
- [ ] Completion time < 500ms
- [ ] No ANR warnings

### Test: 3,600-Point Route (Full Hour)
- [ ] Create ride with 3,600+ track points
- [ ] Load RideDetailScreen
- [ ] Measure simplification time (should be ~50ms)
- [ ] Verify smooth polyline rendering (no gaps)
- [ ] Confirm points reduced from 3,600 → ~340
- [ ] Memory usage < 50MB overhead
- [ ] No frame rate drops during interaction
- [ ] No crashes or errors

### Test: Edge Cases
- [ ] Ride with 0 track points → no crash, no map
- [ ] Ride with 1 track point → map shows location
- [ ] Ride with 2 track points → simple line renders
- [ ] All pause states (active/manual/auto) → visual distinction

### Test: Dark Mode
- [ ] Enable dark mode in emulator settings
- [ ] Navigate to RideDetailScreen
- [ ] Polyline color visible (light blue, not dark blue)
- [ ] Markers visible
- [ ] No color contrast issues
- [ ] Toggle between light/dark → colors update

### Profiling
- [ ] Open Android Profiler
- [ ] Load 3,600-point route
- [ ] Monitor memory graph
  - [ ] Initial: ~40MB
  - [ ] Peak: < 90MB
  - [ ] After GC: ~50MB
- [ ] Monitor CPU
  - [ ] Peak during rendering: < 30% (main thread)
  - [ ] Normal: < 5%
- [ ] Monitor frame rate
  - [ ] During pan/zoom: 60fps (60 frames/sec)
  - [ ] No drops to 30fps or lower

---

## Phase 7: Integration Testing

### RideReviewScreen Integration (Optional for v0.5.0)
- [ ] Locate MapPlaceholder in RideReviewScreen
- [ ] Consider replacing with RoutePolyline
- [ ] OR update MapPlaceholder comment to reference map feature
- [ ] Defer full integration to v0.6.0 if time constraints

### LiveRideScreen Enhancement (Future)
- [ ] Document API for real-time updates
- [ ] Note that LiveRideScreen can use RoutePolyline
- [ ] Require update() mechanism for live tracking
- [ ] Schedule for v0.7.0 implementation

---

## Phase 8: Code Review & Quality

### Code Style Review
- [ ] All files follow Kotlin naming conventions
- [ ] PascalCase for classes (RoutePolyline)
- [ ] camelCase for functions (toSimplifiedPolyline)
- [ ] No `!!` operator used
- [ ] Null safety properly handled
- [ ] No unused imports
- [ ] kdoc comments on public functions

### Architecture Review
- [ ] Utility functions in domain/util (not UI-dependent)
- [ ] Composable in ui/components (reusable)
- [ ] No business logic in composables
- [ ] Conversion functions are pure (no side effects)
- [ ] Follows MVVM pattern

### Performance Review
- [ ] Simplification uses remember()
- [ ] No expensive operations in composable body
- [ ] Polyline creation optimized
- [ ] No unnecessary recompositions
- [ ] Memory usage within targets
- [ ] Frame rate at 60fps

### Security Review
- [ ] No hardcoded API keys
- [ ] No sensitive data in logs
- [ ] Track points handled securely
- [ ] No unnecessary permissions required

---

## Phase 9: Git Commits

### Commit 1: Dependencies
```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore(deps): add google maps compose library v6.1.0

Adds Google Maps Compose for polyline rendering support.
Prepares for route visualization feature.

- Add googleMapsCompose v6.1.0 to versions
- Add google-maps-compose library
- Sync Gradle and verify no conflicts"
```
- [ ] Commit message follows conventions
- [ ] Files changed: 2
- [ ] Lines added: ~10

### Commit 2: Utility Functions
```bash
git add app/src/main/java/com/example/bikeredlights/domain/util/PolylineUtils.kt
git commit -m "feat(domain): add polyline conversion and simplification utilities

Implements TrackPoint to LatLng conversion and Douglas-Peucker
simplification algorithm for efficient route visualization.

- Add toLatLngList() conversion
- Add toLatLngListFiltered() with accuracy filtering
- Add simplifyRoute() with 10m default tolerance
- Add toSimplifiedPolyline() one-liner
- All functions documented with KDoc"
```
- [ ] Commit message follows conventions
- [ ] Tests written (see Phase 4)
- [ ] Lines added: ~80

### Commit 3: Composable Component
```bash
git add app/src/main/java/com/example/bikeredlights/ui/components/route/RoutePolyline.kt
git commit -m "feat(ui): create reusable RoutePolyline composable

Renders simplified polylines on Google Map with Material 3 theming.
Handles start/end markers and responsive to track point updates.

- Create RoutePolyline composable
- Use remember() for optimization
- Material 3 color integration
- Support for custom width and markers
- Comprehensive documentation"
```
- [ ] Commit message follows conventions
- [ ] Composable thoroughly tested
- [ ] Lines added: ~120

### Commit 4: Tests
```bash
git add app/src/test/java/com/example/bikeredlights/domain/util/PolylineUtilsTest.kt
git commit -m "test(domain): add unit tests for polyline utilities

Comprehensive test coverage for conversion and simplification functions.
Tests validate 80%+ point reduction with 10m tolerance.

- Test toLatLngList() conversion
- Test toLatLngListFiltered() accuracy filtering
- Test simplifyRoute() reduces by 80-90%
- Test toSimplifiedPolyline() pipeline
- All tests passing"
```
- [ ] Commit message follows conventions
- [ ] All tests pass
- [ ] Coverage > 80%
- [ ] Lines added: ~150

### Commit 5: Integration
```bash
git add app/src/main/java/com/example/bikeredlights/ui/screens/history/RideDetailScreen.kt \
        app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideDetailViewModel.kt
git commit -m "feat(ui): integrate polyline route visualization into ride detail screen

Displays route polyline on RideDetailScreen with start/end markers.
Replaces map placeholder with functional map component.

- Load track points in RideDetailViewModel
- Display RoutePolyline in RideDetailScreen
- Show start/end markers
- Handle empty route gracefully
- Tested on emulator with 3600-point routes"
```
- [ ] Commit message follows conventions
- [ ] Emulator testing complete (see Phase 6)
- [ ] No crashes or errors
- [ ] Lines changed: ~30

---

## Phase 10: Pull Request

### PR Description Template
```markdown
## Summary
Add route polyline visualization to BikeRedlights ride detail screens.
Displays GPS track points as simplified polylines on interactive maps.

## Implementation Details
- Convert TrackPoint domain models to Google Maps LatLng coordinates
- Implement Douglas-Peucker polyline simplification (10m tolerance)
- Create reusable RoutePolyline composable with Material 3 theming
- Integrate with RideDetailScreen for viewing completed rides
- Full unit test coverage for conversion and simplification

## Technical Specifications
- Reduces 3,600-point routes to ~340 points (90% reduction)
- Polyline rendering: < 100ms
- Memory overhead: < 50MB
- Frame rate: 60fps during interaction
- Dark mode support: Automatic via Material 3

## Testing
- Unit tests: All passing (conversion, simplification, filtering)
- Emulator tests: Verified 100, 1000, 3600-point routes
- Performance profiling: Memory and CPU within targets
- Accessibility: Dark mode colors validated
- Integration: RideDetailScreen fully functional

## Closes
Closes #007 (Route visualization feature)

## Screenshots / Evidence
(Emulator screenshots showing 3600-point route rendering smoothly)
```

### PR Checklist
- [ ] Title: "feat: add route polyline visualization to ride screens"
- [ ] Description follows template above
- [ ] Link to POLYLINE_RESEARCH.md in description
- [ ] All commits follow conventional format
- [ ] No merge conflicts
- [ ] All tests passing
- [ ] Emulator testing confirmed
- [ ] TODO.md updated with completion
- [ ] RELEASE.md updated for v0.5.0

---

## Phase 11: Post-Merge Tasks

### Documentation Update
- [ ] Update TODO.md: Move Feature to "Completed" section
- [ ] Add completion date: November 2025
- [ ] Update RELEASE.md: Add entry under "Unreleased"
- [ ] Mark all tasks complete in checklist
- [ ] Create commit: `chore: mark feature 006 complete - route polylines`

### Version Bump (If Releasing)
- [ ] Decide on version: v0.5.0
- [ ] Calculate versionCode: 0*10000 + 5*100 + 0 = 500
- [ ] Update app/build.gradle.kts:
  - [ ] versionCode = 500
  - [ ] versionName = "0.5.0"
- [ ] Create commit: `chore: bump version to v0.5.0`

### Release (If Required)
- [ ] Create annotated tag: `git tag -a v0.5.0 -m "..."`
- [ ] Push tag: `git push origin v0.5.0`
- [ ] Build signed APK: `./gradlew assembleRelease`
- [ ] Create GitHub Release with APK attachment

---

## Troubleshooting

### Issue: Gradle Dependency Conflict
**Symptom**: Build fails with version conflict error
**Solution**:
- [ ] Run `./gradlew dependencies` to see dependency tree
- [ ] Check which libraries conflict
- [ ] Update version in libs.versions.toml
- [ ] Run `./gradlew clean build` to verify

### Issue: Map Not Displaying
**Symptom**: Blank space where polyline should be
**Solution**:
- [ ] Verify Google Maps API key in AndroidManifest.xml
- [ ] Check API key has Maps SDK enabled
- [ ] Confirm track points loaded in ViewModel
- [ ] Verify simplifyRoute() not returning empty list
- [ ] Check logcat for specific errors

### Issue: Jank/Frame Drops
**Symptom**: Map interaction causes frame rate drops
**Solution**:
- [ ] Verify polyline simplification applied (not raw points)
- [ ] Check remember() used for simplification
- [ ] Profile with Android Profiler for bottlenecks
- [ ] Increase simplification tolerance (15m instead of 10m)
- [ ] Verify no expensive operations in composable body

### Issue: Wrong Colors in Dark Mode
**Symptom**: Polyline invisible in dark mode
**Solution**:
- [ ] Use MaterialTheme.colorScheme.primary (not hardcoded Color.Blue)
- [ ] Verify theme updated dark color definitions
- [ ] Test with emulator dark mode toggle
- [ ] Check Theme.kt has dark color definitions

### Issue: Test Failures
**Symptom**: PolylineUtilsTest fails
**Solution**:
- [ ] Verify PolyUtil.simplify() available from google-maps-utils
- [ ] Check import statements correct
- [ ] Run individual test to see error message
- [ ] Mock PolyUtil if needed for unit tests
- [ ] Review test expectations match algorithm behavior

---

## Sign-Off

### Feature Complete Checklist
- [ ] All code written and reviewed
- [ ] All tests passing
- [ ] Emulator testing complete
- [ ] Performance targets met
- [ ] Documentation updated
- [ ] PR merged to main
- [ ] Version bumped (if releasing)
- [ ] Git tag created (if releasing)
- [ ] GitHub Release created (if releasing)

### Implementation Sign-Off
- [ ] Feature works as specified
- [ ] No known bugs or limitations
- [ ] Code follows BikeRedlights standards
- [ ] Performance acceptable
- [ ] Ready for v0.5.0 release

**Signed Off By**: [Developer Name]
**Date Completed**: [Date]
**Version**: v0.5.0

---

## Next Feature Planning (v0.6.0)

- [ ] Performance optimization (advanced simplification)
- [ ] Segmented routes (color by pause state)
- [ ] Zoom-to-fit route bounds
- [ ] Integration with LiveRideScreen
- [ ] Real-time polyline updates

---

**Use this checklist to track every step of polyline implementation.**
**Reference POLYLINE_RESEARCH.md or POLYLINE_QUICK_REFERENCE.md as needed.**
