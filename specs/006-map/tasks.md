# Tasks: Maps Integration

**Input**: Design documents from `/specs/006-map/`
**Prerequisites**: plan.md (✅), spec.md (✅), research.md (✅), data-model.md (✅), contracts/ (✅), quickstart.md (✅)

**Tests**: Test tasks are included per Constitution requirements (90%+ coverage for safety-critical features).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android project**: `app/src/main/java/com/example/bikeredlights/`
- **Tests**: `app/src/test/java/` (unit), `app/src/androidTest/java/` (instrumented)
- All paths are absolute from project root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Google Cloud Console setup and project dependencies

- [ ] T001 Create Google Cloud project named "BikeRedlights" via Google Cloud Console
- [ ] T002 Enable billing account and link to project (free trial $300 credit)
- [ ] T003 Enable Maps SDK for Android API in Google Cloud Console
- [ ] T004 Generate API key and copy to secure location
- [ ] T005 Restrict API key by package name (com.example.bikeredlights) + SHA-1 fingerprint (use: `keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore`)
- [ ] T006 Set API restrictions to "Maps SDK for Android" only in Google Cloud Console
- [ ] T007 Create budget alert ($50/month) in Google Cloud Console Billing
- [X] T008 Add Secrets Gradle Plugin to `build.gradle.kts` (project-level): `id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false`
- [X] T009 Add Secrets Gradle Plugin to `app/build.gradle.kts`: `id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")`
- [X] T010 Create `secrets.properties` file in project root with `MAPS_API_KEY=YOUR_API_KEY`
- [X] T011 Verify `secrets.properties` is in `.gitignore`
- [X] T012 Create `local.defaults.properties` with placeholder API key (checked into git)
- [X] T013 Add meta-data to `app/src/main/AndroidManifest.xml`: `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}" />`
- [X] T014 [P] Add Google Maps Compose dependency to `app/build.gradle.kts`: `implementation("com.google.maps.android:maps-compose:6.12.1")`
- [X] T015 [P] Add Play Services Maps dependency to `app/build.gradle.kts`: `implementation("com.google.android.gms:play-services-maps:18.2.0")`
- [X] T016 [P] Add Maps Utils dependency to `app/build.gradle.kts`: `implementation("com.google.maps.android:android-maps-utils:3.8.2")`
- [X] T017 Sync Gradle and verify no errors
- [X] T018 Build debug APK and verify map initialization: `./gradlew assembleDebug && ./gradlew installDebug`

**Checkpoint**: Google Cloud setup complete, dependencies added, basic map can initialize

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain models, use cases, and extension functions that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T019 [P] Create `app/src/main/java/com/example/bikeredlights/domain/model/MapViewState.kt` with CameraPosition, isFollowingUser, mapType, isDarkMode fields
- [X] T020 [P] Create `app/src/main/java/com/example/bikeredlights/domain/model/PolylineData.kt` with points, color, width, geodesic fields
- [X] T021 [P] Create `app/src/main/java/com/example/bikeredlights/domain/model/MarkerData.kt` with MarkerType enum (START, END, CURRENT) and position, type, title, snippet, visible fields
- [X] T022 [P] Create `app/src/main/java/com/example/bikeredlights/domain/model/MapBounds.kt` with bounds, padding, animationDurationMs fields
- [X] T023 [P] Create `app/src/main/java/com/example/bikeredlights/domain/util/TrackPointExtensions.kt` with toLatLngList(), startLocation(), endLocation() extension functions
- [X] T024 [P] Create `app/src/main/java/com/example/bikeredlights/domain/util/LatLngExtensions.kt` with simplifyRoute(toleranceMeters: Double) using PolyUtil.simplify()
- [X] T025 Create `app/src/main/java/com/example/bikeredlights/domain/usecase/GetRoutePolylineUseCase.kt` to convert TrackPoints → PolylineData with simplification
- [X] T026 Create `app/src/main/java/com/example/bikeredlights/domain/usecase/CalculateMapBoundsUseCase.kt` to calculate LatLngBounds for auto-zoom
- [X] T027 Create `app/src/main/java/com/example/bikeredlights/domain/usecase/FormatMapMarkersUseCase.kt` to generate start/end MarkerData from TrackPoints
- [X] T028 [P] Create `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt` composable wrapper for GoogleMap with Material 3 theming (dark mode support via isSystemInDarkTheme())
- [X] T029 [P] Create `app/src/main/java/com/example/bikeredlights/ui/theme/MapTheme.kt` for dark mode style configuration (MapColorScheme.DARK/LIGHT)
- [X] T030 Create extension function `MarkerType.toIcon()` in `MarkerData.kt` to convert MarkerType → BitmapDescriptor (GREEN for START, RED for END, BLUE for CURRENT)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 3 - Google Maps SDK Integration (Priority: P1) 🎯 PREREQUISITE

**Goal**: Integrate Google Maps SDK and verify basic map rendering works correctly before adding route features

**Independent Test**: Build and install app, verify GoogleMap displays on screen without API key errors or blank gray screen. Check logcat for successful initialization.

### Unit Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T031 [P] [US3] Create `app/src/test/java/com/example/bikeredlights/ui/components/map/BikeMapTest.kt` to verify BikeMap composable renders without errors (use ComposeTestRule)

### Implementation for User Story 3

- [X] T032 [US3] Create temporary test screen in `app/src/main/java/com/example/bikeredlights/ui/screens/MapTestScreen.kt` with BikeMap composable
- [X] T033 [US3] Add navigation route to MapTestScreen in `app/src/main/java/com/example/bikeredlights/ui/navigation/AppNavigation.kt`
- [X] T034 [US3] Build debug APK: `./gradlew assembleDebug`
- [X] T035 [US3] Install on emulator: `./gradlew installDebug`
- [X] T036 [US3] Verify map displays correctly (no API key errors, map tiles load, touch gestures work)
- [X] T037 [US3] Check logcat for errors: `adb logcat | grep -E "Maps|ERROR|Exception"`
- [X] T038 [US3] Test dark mode: Enable dark mode on emulator and verify map uses dark style
- [X] T039 [US3] Test rotation: Rotate device and verify map state persists (no reset to 0,0)
- [ ] T040 [US3] Remove MapTestScreen and navigation route (cleanup temporary test code)

**Checkpoint**: At this point, Google Maps SDK should be fully functional and verified

---

## Phase 4: User Story 1 - Real-Time Route Visualization on Live Tab (Priority: P1) 🎯 MVP

**Goal**: Display user's current location on Live tab map with blue marker, show growing route polyline during recording, smooth camera following at city block zoom level (17f)

**Independent Test**: Start ride recording on emulator with GPS simulation, verify blue marker shows current location, polyline grows as simulated route progresses, camera follows smoothly without jarring jumps

### Unit Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T041 [P] [US1] Create `app/src/test/java/com/example/bikeredlights/domain/usecase/GetRoutePolylineUseCaseTest.kt` with tests: returns null when empty, converts TrackPoints to LatLng, simplifies with tolerance, handles single point
- [ ] T042 [P] [US1] Create `app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModelTest.kt` (update existing) to test map state updates: currentRideTrackPoints StateFlow, polylineData StateFlow, userLocation StateFlow
- [ ] T043 [P] [US1] Create `app/src/androidTest/java/com/example/bikeredlights/ui/components/map/RoutePolylineTest.kt` to verify polyline renders with correct color, handles null data gracefully, applies simplification

### Implementation for User Story 1

- [X] T044 [P] [US1] Create `app/src/main/java/com/example/bikeredlights/ui/components/map/RoutePolyline.kt` composable to render Polyline with PolylineData (null-safe)
- [X] T045 [P] [US1] Create `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt` composable to render blue marker at current location (null-safe)
- [X] T046 [US1] Update `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt` to expose currentRideTrackPoints StateFlow from TrackPointRepository
- [X] T047 [US1] Update `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt` to expose polylineData StateFlow by mapping currentRideTrackPoints through GetRoutePolylineUseCase
- [X] T048 [US1] Update `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt` to add BikeMap with userLocation and polylineData
- [X] T049 [US1] Add LaunchedEffect in LiveRideScreen to animate camera following userLocation with CameraPositionState.animate() at zoom 17f, duration 500ms
- [X] T050 [US1] Add LocationMarker composable to LiveRideScreen map content
- [X] T051 [US1] Add RoutePolyline composable to LiveRideScreen map content with red color (Color.Red)
- [X] T052 [US1] Handle case when ride is not recording: only show location marker, no polyline (polylineData will be null)

**Integration Testing for User Story 1**

- [X] T053 [US1] Build and install: `./gradlew assembleDebug && ./gradlew installDebug`
- [X] T054 [US1] Start emulator GPS simulation: Open Extended Controls (...) → Location → Set single point (37.422, -122.084)
- [X] T055 [US1] Open app to Live tab, verify blue marker displays at simulated location within 3 seconds
- [X] T056 [US1] Start ride recording, move GPS location to new point (37.423, -122.085), verify marker updates and polyline starts growing
- [X] T057 [US1] Continue moving GPS location through 5-10 points, verify polyline grows smoothly, camera follows without jumps
- [X] T058 [US1] Verify zoom level stays at city block level (~17f) during movement
- [X] T059 [US1] Stop recording, verify polyline clears on next GPS update (no recording = no polyline)
- [X] T060 [US1] Test performance: Move through 100+ simulated points, verify 60fps panning (no jank, <2s delay for polyline update)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Live tab shows real-time location + route polyline.

---

## Phase 5: User Story 2 - Complete Route Review After Ride (Priority: P2)

**Goal**: Display complete saved ride route on Review Screen map with green start marker, red end marker, and auto-zoom to fit entire route in viewport

**Independent Test**: Save a ride with GPS track points, navigate to Review Screen for that ride, verify complete polyline displays, start/end markers visible with correct colors, auto-zoom fits entire route

### Unit Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T061 [P] [US2] Create `app/src/test/java/com/example/bikeredlights/domain/usecase/CalculateMapBoundsUseCaseTest.kt` with tests: returns null when empty, returns null for single point, calculates bounds for multiple points, applies correct padding
- [ ] T062 [P] [US2] Create `app/src/test/java/com/example/bikeredlights/domain/usecase/FormatMapMarkersUseCaseTest.kt` with tests: returns empty list when empty, returns single START marker for one point, returns START+END for multiple points, formats timestamps correctly
- [ ] T063 [P] [US2] Create `app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideDetailViewModel.kt` (if new) to test: trackPoints StateFlow loads from repository, polylineData StateFlow processes via use case, mapBounds StateFlow calculates correctly, markers StateFlow formats correctly
- [ ] T064 [P] [US2] Create `app/src/androidTest/java/com/example/bikeredlights/ui/components/map/StartEndMarkersTest.kt` to verify markers render at correct positions, start uses green icon, end uses red icon, handles empty list gracefully

### Implementation for User Story 2

- [X] T065 [P] [US2] Create `app/src/main/java/com/example/bikeredlights/ui/components/map/StartEndMarkers.kt` composable to render list of MarkerData with correct icons via MarkerType.toIcon()
- [X] T066 [US2] Create or update `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideDetailViewModel.kt` to expose trackPoints StateFlow from RideRepository.getTrackPointsForRide(rideId)
- [X] T067 [US2] Add polylineData StateFlow to RideDetailViewModel by mapping trackPoints through GetRoutePolylineUseCase
- [X] T068 [US2] Add mapBounds StateFlow to RideDetailViewModel by mapping trackPoints through CalculateMapBoundsUseCase
- [X] T069 [US2] Add markers StateFlow to RideDetailViewModel by mapping trackPoints through FormatMapMarkersUseCase
- [X] T070 [US2] Update `app/src/main/java/com/example/bikeredlights/ui/screens/ride/RideReviewScreen.kt` (or RideDetailScreen) to add BikeMap with polylineData and markers
- [X] T071 [US2] Add LaunchedEffect in Review Screen to animate camera with mapBounds using CameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, padding), durationMs)
- [X] T072 [US2] Add StartEndMarkers composable to Review Screen map content
- [X] T073 [US2] Add RoutePolyline composable to Review Screen map content with Material 3 primary color
- [X] T074 [US2] Handle edge case: single-point route (no bounds, use fixed zoom 17f)
- [X] T075 [US2] Handle edge case: empty track points (show empty state or fallback message)
- [X] T076 [US2] Handle edge case: very long route (100+ km, auto-bounds handles correctly)

**Integration Testing for User Story 2**

- [X] T077 [US2] Build and install: `./gradlew assembleDebug && ./gradlew installDebug`
- [X] T078 [US2] Create test ride with 10+ GPS points using emulator GPS simulation and ride recording
- [X] T079 [US2] Navigate to ride history list, tap on saved ride to open Review Screen
- [X] T080 [US2] Verify complete route polyline displays within 2 seconds
- [X] T081 [US2] Verify green pin marker at start location (first track point)
- [X] T082 [US2] Verify red flag marker at end location (last track point)
- [X] T083 [US2] Verify auto-zoom fits entire route in viewport with appropriate padding (no markers touching screen edges)
- [X] T084 [US2] Test with very short route (<100m): Verify fixed zoom or appropriate bounds
- [X] T085 [US2] Test with very long route (simulate 50+ km): Verify auto-zoom handles without excessive zoom out
- [X] T086 [US2] Test rotation: Rotate device and verify map state persists (bounds recalculated but route still fits)
- [X] T087 [US2] Delete ride, verify no map data remains (test cleanup)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently. Live tab has real-time tracking, Review Screen has complete route visualization.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories, final testing, and documentation

- [ ] T088 [P] Add accessibility content descriptions to BikeMap controls in `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt` (zoom buttons, location FAB)
- [ ] T089 [P] Add accessibility content descriptions to markers in StartEndMarkers.kt and LocationMarker.kt
- [ ] T090 [P] Verify all map components meet 48dp minimum touch target size (Constitution requirement)
- [ ] T091 [P] Test TalkBack support: Enable TalkBack and navigate map controls, verify all elements are accessible
- [ ] T092 Performance profiling: Use Android Studio Profiler to verify <1s map initialization, <2s polyline rendering, 60fps panning
- [ ] T093 Memory profiling: Verify polyline simplification reduces memory (3600 points → 340 points = 90% reduction)
- [ ] T094 Test dark mode across all screens: Toggle dark mode on emulator and verify all map screens use correct theme (MapColorScheme.DARK)
- [ ] T095 Test on physical device: Install on real Android device, take actual bike ride, verify GPS accuracy and smooth rendering
- [ ] T096 Test GPS signal loss: Disable location on emulator, verify app handles gracefully (doesn't crash, shows error state)
- [ ] T097 Test Google Maps API quota: Monitor usage in Google Cloud Console, verify staying within free tier (28,000 map loads/month)
- [ ] T098 [P] Code cleanup: Remove any debug logging, commented code, or temporary test screens
- [ ] T099 [P] Verify all commits follow conventional commit format: `feat(domain): add GetRoutePolylineUseCase`, etc.
- [ ] T100 Run quickstart.md validation: Follow all verification checklists in `specs/006-map/quickstart.md`
- [ ] T101 Update TODO.md: Move feature 006 from "In Progress" to "Completed" with completion date
- [ ] T102 Update RELEASE.md: Move "Maps Integration" from "Unreleased" section to v0.5.0 release section
- [ ] T103 Create pull request with detailed description, link to spec.md, emulator testing confirmation
- [ ] T104 Request code review and address feedback
- [ ] T105 Merge PR to main after approval
- [ ] T106 Update version in `app/build.gradle.kts` to v0.5.0 (versionCode = 500, versionName = "0.5.0")
- [ ] T107 Commit version bump: `git commit -m "chore: bump version to v0.5.0"`
- [ ] T108 Create git tag: `git tag -a v0.5.0 -m "Release v0.5.0: Maps Integration with real-time tracking and route visualization"`
- [ ] T109 Push tag to GitHub: `git push origin v0.5.0`
- [ ] T110 Build signed release APK: `./gradlew assembleRelease`
- [ ] T111 Create GitHub release with APK attached and release notes from RELEASE.md

**Final Checkpoint**: All user stories complete, tested, documented, and ready for release

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion (T001-T018) - BLOCKS all user stories
- **User Story 3 (Phase 3)**: Depends on Foundational (T019-T030) - PREREQUISITE for US1 and US2
- **User Story 1 (Phase 4)**: Depends on Foundational + US3 completion - MVP
- **User Story 2 (Phase 5)**: Depends on Foundational + US3 completion - Can run parallel with US1
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 3 (P1 - SDK Integration)**: PREREQUISITE - Must complete first, verifies SDK works
- **User Story 1 (P1 - Live Tab Map)**: Can start after US3 - No dependencies on US2
- **User Story 2 (P2 - Review Screen Map)**: Can start after US3 - Independent from US1

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD)
- Domain models/use cases before UI components
- Composables before ViewModel updates
- ViewModel updates before screen integration
- Core implementation before edge case handling
- Story complete and tested before moving to next priority

### Parallel Opportunities

**Setup Phase (T001-T018)**:
- T001-T007 (Google Cloud setup): Sequential (each depends on previous)
- T008-T013 (Gradle/secrets setup): Sequential
- T014-T016 (Dependencies): Can run in parallel [P]

**Foundational Phase (T019-T030)**:
- T019-T022 (Domain models): Can run in parallel [P]
- T023-T024 (Extensions): Can run in parallel [P]
- T025-T027 (Use cases): Can run in parallel after T023-T024
- T028-T029 (UI base components): Can run in parallel [P]

**User Story 1 Tests (T041-T043)**:
- All 3 test files can be created in parallel [P]

**User Story 1 Implementation (T044-T052)**:
- T044-T045 (Composables): Can run in parallel [P]
- T046-T047 (ViewModel updates): Sequential (same file)
- T048-T052 (Screen integration): Sequential (same file)

**User Story 2 Tests (T061-T064)**:
- All 4 test files can be created in parallel [P]

**User Story 2 Implementation (T065-T076)**:
- T065-T069 (Composable + ViewModel): T065 can be parallel [P], T066-T069 sequential
- T070-T076 (Screen integration): Sequential (same file)

**Polish Phase (T088-T111)**:
- T088-T091 (Accessibility): Can run in parallel [P]
- T092-T099 (Testing/cleanup): Can run in parallel [P]
- T100-T111 (Documentation/release): Sequential

---

## Parallel Example: User Story 1 Implementation

```bash
# Launch all composables in parallel:
Task T044: "Create RoutePolyline.kt composable"
Task T045: "Create LocationMarker.kt composable"

# Then update ViewModel (sequential, same file):
Task T046: "Add currentRideTrackPoints StateFlow"
Task T047: "Add polylineData StateFlow"

# Then update LiveRideScreen (sequential, same file):
Task T048: "Add BikeMap to LiveRideScreen"
Task T049: "Add camera following LaunchedEffect"
Task T050: "Add LocationMarker to map"
Task T051: "Add RoutePolyline to map"
Task T052: "Handle non-recording state"
```

---

## Implementation Strategy

### MVP First (User Story 3 + User Story 1 Only)

1. Complete Phase 1: Setup (T001-T018) - Google Cloud + dependencies
2. Complete Phase 2: Foundational (T019-T030) - Domain models + base components
3. Complete Phase 3: User Story 3 (T031-T040) - Verify SDK works
4. Complete Phase 4: User Story 1 (T041-T060) - Live tab real-time tracking
5. **STOP and VALIDATE**: Test User Story 1 independently on emulator and physical device
6. **MVP READY**: Live tab map with real-time tracking is fully functional

### Incremental Delivery

1. Complete Setup + Foundational + US3 → SDK verified, foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP! 🎯)
3. Add User Story 2 → Test independently → Deploy/Demo (Review Screen added)
4. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup (Phase 1) together
2. Team completes Foundational (Phase 2) together
3. Team completes US3 (SDK Integration) together - CRITICAL PREREQUISITE
4. Once US3 is done:
   - **Developer A**: User Story 1 (Live tab map)
   - **Developer B**: User Story 2 (Review Screen map)
5. Stories complete and test independently

---

## Task Summary

**Total Tasks**: 111 tasks
- **Setup (Phase 1)**: 18 tasks
- **Foundational (Phase 2)**: 12 tasks
- **User Story 3 (SDK Integration)**: 10 tasks (T031-T040)
- **User Story 1 (Live Tab)**: 20 tasks (T041-T060)
- **User Story 2 (Review Screen)**: 23 tasks (T061-T087)
- **Polish (Phase 6)**: 24 tasks (T088-T111)

**Parallel Opportunities**: 29 tasks marked [P] can run in parallel

**Independent Test Criteria**:
- **US3**: Map displays without errors, tiles load, gestures work
- **US1**: Blue marker shows location, polyline grows during recording, camera follows smoothly
- **US2**: Complete route displays, start/end markers visible, auto-zoom fits route

**Suggested MVP Scope**: Phase 1 + Phase 2 + Phase 3 + Phase 4 = User Story 3 + User Story 1 (Live tab real-time tracking)

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD)
- Commit after each task or logical group (follow CLAUDE.md conventions)
- Stop at any checkpoint to validate story independently
- Follow quickstart.md for detailed implementation guidance
- All file paths are absolute from project root
- Testing is mandatory per Constitution (90%+ coverage for safety-critical features)
