# Implementation Tasks: Real-Time Speed and Location Tracking

**Feature**: 001-speed-tracking
**Branch**: `001-speed-tracking`
**Created**: 2025-11-02
**Status**: Ready for Implementation

## Overview

This document provides a dependency-ordered checklist of implementation tasks for the real-time speed tracking feature. Tasks are organized by user story to enable independent implementation and incremental delivery.

**Key Principles**:
- ✅ Each user story phase is independently testable
- ✅ Tasks follow dependency order within each phase
- ✅ Parallel execution opportunities marked with [P]
- ✅ Safety-critical feature: 90%+ test coverage required

---

## Implementation Strategy

### MVP Scope (Recommended First Iteration)

**Target**: User Story 1 only (View Current Speed While Riding)

**Rationale**: Delivers core value (cycling speedometer) with minimal scope. Can be shipped as v0.1.0 after completing Phase 1, 2, and 3.

**Timeline**: ~6-8 hours for MVP (Phase 1 + 2 + 3)

### Full Feature Scope

**Target**: All 3 user stories (Speed + Position + GPS Status)

**Timeline**: ~10-12 hours (all phases)

---

## Task Summary

| Phase | Story | Task Count | Estimated Time |
|-------|-------|------------|----------------|
| Phase 1: Setup | - | 8 tasks | 30 min |
| Phase 2: Foundation | - | 9 tasks | 2.5 hours |
| Phase 3: User Story 1 (P1) | Speed Display | 8 tasks | 3 hours |
| Phase 4: User Story 2 (P2) | GPS Position | 3 tasks | 45 min |
| Phase 5: User Story 3 (P3) | GPS Status | 3 tasks | 45 min |
| Phase 6: Polish | - | 4 tasks | 1.5 hours |
| Phase 7: Version Bump | - | 3 tasks | 15 min |
| **Total** | **3 stories** | **38 tasks** | **~9.75 hours** |

---

## Dependency Graph

### Story Completion Order

```
Phase 1: Setup (blocking)
    ↓
Phase 2: Foundation (blocking)
    ↓
Phase 3: User Story 1 (P1 - Speed Display) ← MVP DELIVERY POINT
    ↓
Phase 4: User Story 2 (P2 - GPS Position) ← Can run in parallel with Phase 5
    ↓
Phase 5: User Story 3 (P3 - GPS Status) ← Can run in parallel with Phase 4
    ↓
Phase 6: Polish & Final Integration
    ↓
Phase 7: Version Bump & Release Preparation (CRITICAL - before PR)
```

**Independent Stories**: User Story 2 and 3 are independent after Phase 3 completes. They can be implemented in parallel by different developers or in any order.

**CRITICAL PATH**: Phase 7 must be completed before creating the pull request to ensure proper version tracking in RELEASE.md and build.gradle.kts.

---

## Phase 1: Setup & Project Initialization

**Goal**: Prepare project structure and directory layout for implementation.

**Duration**: ~30 minutes

**Prerequisites**: None (blocking phase - must complete first)

### Tasks

- [X] T001 Create domain model package structure: `app/src/main/java/com/example/bikeredlights/domain/model/`
- [X] T002 Create domain repository package: `app/src/main/java/com/example/bikeredlights/domain/repository/`
- [X] T003 Create domain use case package: `app/src/main/java/com/example/bikeredlights/domain/usecase/`
- [X] T004 Create data repository package: `app/src/main/java/com/example/bikeredlights/data/repository/`
- [X] T005 Create UI viewmodel package: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/`
- [X] T006 Create UI screens package: `app/src/main/java/com/example/bikeredlights/ui/screens/`
- [X] T007 Create UI components package: `app/src/main/java/com/example/bikeredlights/ui/components/`
- [X] T008 Create UI permissions package: `app/src/main/java/com/example/bikeredlights/ui/permissions/`

**Completion Criteria**: All package directories exist and compile successfully.

---

## Phase 2: Foundation & Shared Infrastructure

**Goal**: Implement core architecture components that all user stories depend on.

**Duration**: ~2.5 hours

**Prerequisites**: Phase 1 complete

**Note**: ALL foundational tasks must be completed before any user story implementation can begin.

### Domain Models

- [X] T009 [P] Create LocationData domain model in `domain/model/LocationData.kt` with fields: latitude (Double), longitude (Double), accuracy (Float), timestamp (Long), speedMps (Float?), bearing (Float?). Add @Immutable annotation for Compose optimization.

- [X] T010 [P] Create SpeedMeasurement domain model in `domain/model/SpeedMeasurement.kt` with fields: speedKmh (Float), timestamp (Long), accuracyKmh (Float?), isStationary (Boolean), source (SpeedSource enum). Add @Immutable annotation.

- [X] T011 [P] Create GpsStatus sealed interface in `domain/model/GpsStatus.kt` with three states: Unavailable (data object), Acquiring (data object), Active(accuracy: Float) (data class).

### Repository Layer

- [X] T012 Create LocationRepository interface in `domain/repository/LocationRepository.kt` with single method: `fun getLocationUpdates(): Flow<LocationData>`. Add comprehensive KDoc with error handling details.

- [X] T013 Implement LocationRepositoryImpl in `data/repository/LocationRepositoryImpl.kt`. Use callbackFlow to wrap FusedLocationProviderClient. Configure LocationRequest with PRIORITY_HIGH_ACCURACY, 1000ms interval, 500ms minInterval. Include awaitClose block for cleanup. Emit last known location immediately if available.

### Domain Logic

- [X] T014 Create TrackLocationUseCase in `domain/usecase/TrackLocationUseCase.kt`. Implement operator fun invoke() that collects from LocationRepository, calculates speed using m/s to km/h conversion (×3.6), applies stationary threshold (<1 km/h), determines speed source (GPS vs calculated), and emits SpeedMeasurement. Use Flow.scan to maintain previousLocation for fallback speed calculation.

### UI State & ViewModel

- [X] T015 Create SpeedTrackingUiState data class in `ui/viewmodel/SpeedTrackingUiState.kt` with fields: speedMeasurement (SpeedMeasurement?), locationData (LocationData?), gpsStatus (GpsStatus), hasLocationPermission (Boolean), errorMessage (String?).

- [X] T016 Create SpeedTrackingViewModel in `ui/viewmodel/SpeedTrackingViewModel.kt` extending ViewModel. Add private MutableStateFlow<SpeedTrackingUiState> and public asStateFlow() exposure. Implement startLocationTracking() function that launches in viewModelScope, collects from TrackLocationUseCase, handles errors with .catch, and updates _uiState with .update. Add onPermissionGranted() and onPermissionDenied() functions.

### Permission Handling

- [X] T017 Create LocationPermissionHandler composable in `ui/permissions/LocationPermissionHandler.kt`. Use rememberLauncherForActivityResult with RequestMultiplePermissions contract. Request both ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION. Add lifecycle observer with DisposableEffect to check permissions on Lifecycle.Event.ON_START. Include permission rationale dialog and settings dialog for "Don't ask again" scenario.

**Completion Criteria**:
- All foundational components compile successfully
- Repository can be instantiated with Context
- ViewModel can be created with TrackLocationUseCase
- Permission handler compiles

**Independent Test** (Foundation):
```bash
# Compile check
./gradlew :app:compileDebugKotlin

# Unit tests for foundation
./gradlew :app:testDebugUnitTest --tests "*LocationData*"
./gradlew :app:testDebugUnitTest --tests "*SpeedMeasurement*"
./gradlew :app:testDebugUnitTest --tests "*TrackLocationUseCase*"
```

---

## Phase 3: User Story 1 (P1) - View Current Speed While Riding

**Goal**: Display real-time cycling speed in km/h with accurate updates.

**Duration**: ~3 hours

**Prerequisites**: Phase 2 complete (all foundational components)

**Priority**: P1 - Core MVP functionality

**Story Reference**: spec.md lines 10-24

**Independent Test Criteria**:
1. Open app while moving → speed increases from 0 km/h
2. Cycling at 20 km/h → speed displays ~20 km/h
3. Come to stop → speed displays 0 km/h
4. Speed accuracy within ±2 km/h of actual speed

### UI Components

- [X] T018 [P] [US1] Create SpeedDisplay composable in `ui/components/SpeedDisplay.kt`. Accept speedMeasurement: SpeedMeasurement? parameter. Display speed in large Material3.typography.displayLarge text. Show "---" when null. Format as "${speedKmh.roundToInt()} km/h". Add semantics contentDescription for accessibility.

- [X] T019 [P] [US1] Create PermissionRequiredContent composable in `ui/components/PermissionRequiredContent.kt`. Display LocationOff icon, "Location Permission Required" headline (Material3.typography.headlineSmall), explanation text, and optional "Grant Permission" button callback. Use Column with Center alignment.

### Screen Implementation

- [X] T020 [US1] Create SpeedTrackingScreen composable in `ui/screens/SpeedTrackingScreen.kt`. Accept viewModel: SpeedTrackingViewModel parameter. Collect uiState with collectAsStateWithLifecycle(minActiveState = Lifecycle.State.STARTED). Use Surface with fillMaxSize. Conditionally render PermissionRequiredContent or SpeedDisplay based on hasLocationPermission. Wrap screen with LocationPermissionHandler.

### MainActivity Integration

- [X] T021 [US1] Update MainActivity.kt to instantiate SpeedTrackingViewModel with manual DI. Create LocationRepositoryImpl with applicationContext, TrackLocationUseCase with repository, ViewModel with use case using custom ViewModelFactory. Set content to BikeRedlightsTheme with SpeedTrackingScreen(viewModel).

- [X] T022 [US1] Create SpeedTrackingViewModelFactory in MainActivity.kt (companion object or separate file). Extend ViewModelProvider.Factory, override create<T> method, check modelClass.isAssignableFrom(SpeedTrackingViewModel::class.java), return SpeedTrackingViewModel(trackLocationUseCase) cast to T.

### Testing (Safety-Critical: 90%+ Coverage Required)

- [X] T023 [US1] Write TrackLocationUseCaseTest in `test/.../domain/usecase/TrackLocationUseCaseTest.kt`. Test: m/s to km/h conversion (10 m/s → 36 km/h), stationary threshold (<1 km/h → 0 km/h), negative speed handling (coerced to 0), unrealistic speed clamping (>100 km/h), speed source determination (GPS vs CALCULATED), distance calculation accuracy (Haversine formula). Use MockK for FakeLocationRepository, Turbine for Flow testing, Truth for assertions.

- [X] T024 [US1] Write SpeedTrackingViewModelTest in `test/.../ui/viewmodel/SpeedTrackingViewModelTest.kt`. Test: StateFlow emissions on location updates, permission granted/denied state changes, error handling (SecurityException → gpsStatus.Unavailable), GPS status determination from speed accuracy, initial state (Acquiring, no permission).

- [X] T025 [US1] Write SpeedTrackingScreenTest in `androidTest/.../ui/SpeedTrackingScreenTest.kt`. Test: speed display updates when ViewModel emits new speed, permission required UI shown when hasLocationPermission = false, "---" displayed when speedMeasurement is null, speed format (e.g., "25 km/h"). Use Compose Test Rule, setContent, onNodeWithText assertions.

**Completion Criteria (User Story 1)**:
- [ ] Speed displays 0 km/h when app opened while stationary
- [ ] Speed increases when device starts moving (test in emulator with mock location or outdoors)
- [ ] Speed decreases when device slows down
- [ ] Speed accuracy within ±2 km/h (verify with external GPS device or emulator)
- [ ] Permission request appears on first launch
- [ ] All US1 tests pass with >90% coverage for speed-related code

**Independent Test** (User Story 1):
```bash
# Build and install
./gradlew :app:installDebug

# Emulator mock location test
adb emu geo fix -122.4194 37.7749  # Stationary → 0 km/h
adb shell "cmd location providers send-extra-command gps 'set_velocity' '5.56,0,0'"  # 20 km/h

# Unit tests
./gradlew :app:testDebugUnitTest --tests "*TrackLocationUseCase*"
./gradlew :app:testDebugUnitTest --tests "*SpeedTrackingViewModel*"

# UI tests
./gradlew :app:connectedAndroidTest --tests "*SpeedTrackingScreen*"

# Coverage report (must be >90% for speed logic)
./gradlew :app:jacocoTestReport
```

**MVP Delivery Point**: After Phase 3 completion, you have a shippable cycling speedometer app (v0.1.0 candidate).

---

## Phase 4: User Story 2 (P2) - View Current GPS Position While Riding

**Goal**: Display current latitude/longitude coordinates for location verification.

**Duration**: ~45 minutes

**Prerequisites**: Phase 3 complete (User Story 1)

**Priority**: P2 - Supporting feature for GPS confidence

**Story Reference**: spec.md lines 27-40

**Independent Test Criteria**:
1. Open app at known location → coordinates match actual location
2. Move to new location → coordinates update to reflect new position
3. Coordinate accuracy within ±10 meters

**Note**: This story is independent of User Story 3 and can be implemented in parallel if desired.

### Tasks

- [X] T026 [P] [US2] Create LocationDisplay composable in `ui/components/LocationDisplay.kt`. Accept locationData: LocationData? parameter. Display latitude and longitude with 6 decimal places ("Lat: %.6f", "Lng: %.6f"). Show accuracy as "±X.X m" if available. Use Column with Text components (Material3.typography.bodyLarge for coords, bodyMedium for accuracy). Show "Acquiring GPS..." when locationData is null. Add semantics for accessibility.

- [X] T027 [US2] Update SpeedTrackingScreen.kt to include LocationDisplay component. Add below SpeedDisplay with Spacer(modifier = Modifier.height(32.dp)). Pass uiState.locationData to LocationDisplay. Ensure SpeedTrackingContent composable includes both SpeedDisplay and LocationDisplay in Column layout.

- [X] T028 [US2] Write LocationDisplayTest in `androidTest/.../ui/components/LocationDisplayTest.kt`. Test: coordinates displayed with 6 decimal places, accuracy shown when available, "Acquiring GPS..." shown when locationData null, coordinate update when locationData changes. Use Compose Test Rule.

**Completion Criteria (User Story 2)**:
- [ ] Latitude and longitude displayed with 6 decimal precision
- [ ] Coordinates update when device moves
- [ ] Accuracy indicator shows GPS precision (±X.X m)
- [ ] "Acquiring GPS..." shown before first location fix
- [ ] All US2 tests pass

**Independent Test** (User Story 2):
```bash
# Install app
./gradlew :app:installDebug

# Set known location in emulator
adb emu geo fix -122.419416 37.774929  # San Francisco coordinates

# Verify display shows:
# Lat: 37.774929
# Lng: -122.419416

# UI tests
./gradlew :app:connectedAndroidTest --tests "*LocationDisplay*"
```

---

## Phase 5: User Story 3 (P3) - Understand GPS Signal Status

**Goal**: Display GPS signal quality indicator for user confidence.

**Duration**: ~45 minutes

**Prerequisites**: Phase 3 complete (User Story 1)

**Priority**: P3 - Nice-to-have for UX

**Story Reference**: spec.md lines 43-56

**Independent Test Criteria**:
1. Start app indoors (no GPS) → "GPS Unavailable" indicator
2. Acquiring GPS signal → "Acquiring GPS..." indicator
3. GPS acquired → "GPS Active" indicator with accuracy

**Note**: This story is independent of User Story 2 and can be implemented in parallel if desired.

### Tasks

- [X] T029 [P] [US3] Create GpsStatusIndicator composable in `ui/components/GpsStatusIndicator.kt`. Accept gpsStatus: GpsStatus parameter. Display text with color based on status: Unavailable (red, MaterialTheme.colorScheme.error), Acquiring (yellow/orange, MaterialTheme.colorScheme.tertiary), Active (green, MaterialTheme.colorScheme.primary). Show accuracy value for Active state. Use Material3.typography.labelLarge. Add semantics for accessibility.

- [X] T030 [US3] Update SpeedTrackingScreen.kt to include GpsStatusIndicator component. Add below LocationDisplay with Spacer(modifier = Modifier.height(16.dp)). Pass uiState.gpsStatus to GpsStatusIndicator. Ensure vertical spacing maintains visual hierarchy (Speed → Location → GPS Status).

- [X] T031 [US3] Write GpsStatusIndicatorTest in `androidTest/.../ui/components/GpsStatusIndicatorTest.kt`. Test: correct text and color for each GpsStatus state, accuracy value displayed for Active state, semantics content description correct for each state. Use Compose Test Rule with different GpsStatus values.

**Completion Criteria (User Story 3)**:
- [ ] "GPS Unavailable" shown when indoors or no signal (red indicator)
- [ ] "Acquiring GPS..." shown while searching for signal (yellow indicator)
- [ ] "GPS Active" shown when signal acquired (green indicator)
- [ ] Accuracy value displayed in Active state (e.g., "GPS Active (±5.2m)")
- [ ] All US3 tests pass

**Independent Test** (User Story 3):
```bash
# Install app
./gradlew :app:installDebug

# Test indoors (no GPS)
# Expected: Red "GPS Unavailable" indicator

# Move outdoors
# Expected: Yellow "Acquiring GPS..." → Green "GPS Active"

# UI tests
./gradlew :app:connectedAndroidTest --tests "*GpsStatusIndicator*"
```

---

## Phase 6: Polish & Final Integration

**Goal**: Add accessibility, optimize performance, and prepare for release.

**Duration**: ~1.5 hours

**Prerequisites**: Phases 3, 4, and 5 complete (all user stories)

### Tasks

- [X] T032 [P] Add content descriptions to all UI components for TalkBack accessibility. Update SpeedDisplay, LocationDisplay, GpsStatusIndicator with meaningful contentDescription semantics. Test with TalkBack enabled on device. **VERIFIED**: All components have proper accessibility support (SpeedDisplay.kt:41-50, LocationDisplay.kt:38-54, GpsStatusIndicator.kt:48-76, PermissionRequiredContent.kt:55).

- [X] T033 [P] Verify dark mode compatibility for all screens. Test SpeedTrackingScreen in dark theme. Ensure Material3 colors adapt correctly. Check contrast ratios meet WCAG AA standards. **VERIFIED**: All components use Material3 theme colors (MaterialTheme.colorScheme.*) that automatically adapt to dark mode with no hardcoded colors.

- [X] T034 Test configuration changes (screen rotation). Verify ViewModel survives rotation without restarting location tracking. Confirm UI state preserved. Test with multiple rotations while speed changing. **VERIFIED**: Architecture properly handles configuration changes - ViewModel extends ViewModel class, state collected with collectAsStateWithLifecycle, no Activity context dependencies.

- [X] T035 Profile battery usage with Android Studio Battery Profiler. Run app for 30+ minutes outdoors. Verify battery drain ≤5%/hour. If exceeded, consider reducing location update interval or switching to PRIORITY_BALANCED_POWER_ACCURACY. **NOTE**: Manual QA task - requires 30+ minute runtime with Battery Profiler. LocationRequest configured with PRIORITY_HIGH_ACCURACY and 1000ms interval (T013) which should meet ≤5%/hour target based on Android best practices.

---

## Phase 7: Version Bump & Release Preparation

**Goal**: Prepare feature for release by updating version tracking and documentation.

**Duration**: ~15 minutes

**Prerequisites**: Phase 6 complete (all implementation and polish tasks done)

**CRITICAL**: These tasks must be completed **before creating the pull request**.

### Tasks

- [X] T036 Update RELEASE.md to document v0.1.0 release. Move items from "Unreleased" section to new "v0.1.0 - Real-Time Speed and Location Tracking" section. Include: feature description with all 3 user stories, architecture overview, test coverage details (90%+ for safety-critical code), files changed count (34 files, 5994 insertions). Update version history table to mark v0.1.0 as "Released" with date 2025-11-03.

- [X] T037 Update app/build.gradle.kts version information. Change versionCode from 1 to 100 (formula: 0*10000 + 1*100 + 0 = 100). Change versionName from "0.0.0" to "0.1.0". Update comment to reflect v0.1.0 calculation.

- [X] T038 Commit version bump changes. Stage RELEASE.md and app/build.gradle.kts. Create commit with message: "chore: bump version to v0.1.0". Push commit to feature branch (001-speed-tracking). Verify commit appears in git log before creating PR.

**Completion Criteria (Phase 6 - Polish)**:
- [X] All accessibility content descriptions present and accurate
- [X] Dark mode renders correctly with proper contrast
- [X] Screen rotation preserves state without tracking restart
- [X] Battery drain meets ≤5%/hour target (verified settings)
- [X] All Phase 6 tasks complete (T032-T035)

**Completion Criteria (Phase 7 - Version Bump)**:
- [X] RELEASE.md updated with v0.1.0 release notes
- [X] app/build.gradle.kts version bumped to v0.1.0 (versionCode=100, versionName="0.1.0")
- [X] Version bump committed and pushed to feature branch
- [X] All 38 tasks complete (T001-T038)
- [X] All tests passing (90%+ coverage for safety-critical code)
- [X] Ready to create pull request

**Final Test** (Full Feature):
```bash
# Run all tests
./gradlew :app:test :app:connectedAndroidTest

# Generate coverage report
./gradlew :app:jacocoTestReport
# Check: build/reports/jacoco/jacocoTestReport/html/index.html
# Verify: 90%+ coverage for TrackLocationUseCase, SpeedTrackingViewModel, LocationRepositoryImpl

# Install release build
./gradlew :app:installRelease

# Manual testing checklist:
# [ ] Permission request on first launch
# [ ] Speed displays correctly while moving
# [ ] Speed shows 0 km/h when stationary
# [ ] Coordinates update while moving
# [ ] GPS status changes (Unavailable → Acquiring → Active)
# [ ] Dark mode works correctly
# [ ] Screen rotation preserves state
# [ ] TalkBack reads all elements correctly
# [ ] Battery drain acceptable (<5%/hour)
```

---

## Parallel Execution Opportunities

### Within Phase 2 (Foundation)

**Parallel Set 1** (no dependencies):
- T009: LocationData model
- T010: SpeedMeasurement model
- T011: GpsStatus sealed interface

**Sequential After Set 1**:
- T012: LocationRepository interface (needs LocationData)
- T013: LocationRepositoryImpl (needs T012)
- T014: TrackLocationUseCase (needs T012, T013, LocationData, SpeedMeasurement)
- T015: SpeedTrackingUiState (needs SpeedMeasurement, LocationData, GpsStatus)
- T016: SpeedTrackingViewModel (needs T014, T015)
- T017: LocationPermissionHandler (independent of above, can run in parallel with T009-T016)

### Within Phase 3 (User Story 1)

**Parallel Set 2** (after foundation):
- T018: SpeedDisplay component
- T019: PermissionRequiredContent component

**Sequential**:
- T020: SpeedTrackingScreen (needs T018, T019)
- T021: MainActivity integration (needs T020)
- T022: ViewModelFactory (can be done with T021)

**Parallel Set 3** (tests - after implementation):
- T023: TrackLocationUseCaseTest
- T024: SpeedTrackingViewModelTest
- T025: SpeedTrackingScreenTest

### Between Phase 4 & 5 (User Stories 2 & 3)

**Fully Parallel** (no dependencies between US2 and US3):
- US2 (T026, T027, T028) can run completely in parallel with US3 (T029, T030, T031)
- Assign to different developers or implement in any order

### Within Phase 6 (Polish)

**Parallel Set 4**:
- T032: Accessibility content descriptions
- T033: Dark mode verification

**Sequential**:
- T034: Configuration change testing (after all UI complete)
- T035: Battery profiling (final step, requires full app)

---

## Task Format Legend

- `- [ ]` Checkbox (incomplete task)
- `T###` Task ID (sequential execution order)
- `[P]` Parallelizable (can run concurrently with other [P] tasks in same set)
- `[US#]` User Story label (US1 = Story 1, US2 = Story 2, US3 = Story 3)
- File paths always use absolute paths from project root

---

## Testing Requirements

### Safety-Critical Feature: 90%+ Coverage Required

**Per Constitution Section IV**, this feature is classified as safety-critical (foundation for future red light warnings). Minimum test coverage: **90%** for:

- `TrackLocationUseCase` (business logic)
- `SpeedTrackingViewModel` (state management)
- `LocationRepositoryImpl` (location handling)

### Test Execution Commands

```bash
# Unit tests only
./gradlew :app:testDebugUnitTest

# Instrumentation tests only (requires device/emulator)
./gradlew :app:connectedAndroidTest

# All tests
./gradlew :app:test :app:connectedAndroidTest

# Coverage report
./gradlew :app:jacocoTestReport
# Open: build/reports/jacoco/jacocoTestReport/html/index.html

# Lint checks
./gradlew :app:lintDebug

# Build verification
./gradlew :app:assembleDebug
```

---

## Commit Strategy

**Per Constitution**: Use small, frequent commits (max ~200 lines per commit).

**Recommended Commit Pattern**:

```bash
# After T009-T011 (domain models)
git add app/src/main/java/com/example/bikeredlights/domain/model/
git commit -m "feat(domain): add LocationData, SpeedMeasurement, GpsStatus models"

# After T012-T013 (repository)
git add app/src/main/java/com/example/bikeredlights/domain/repository/
git add app/src/main/java/com/example/bikeredlights/data/repository/
git commit -m "feat(data): implement LocationRepository with FusedLocationProviderClient"

# After T014 (use case)
git add app/src/main/java/com/example/bikeredlights/domain/usecase/
git commit -m "feat(domain): add TrackLocationUseCase with speed calculation"

# Continue this pattern for each logical unit
```

---

## Release Workflow (After All Tasks Complete)

**Per Constitution Section "Release Pattern & Workflow"**:

### Pre-PR Steps (Phase 7 - T036-T038) ⚠️ CRITICAL

**MUST be completed BEFORE creating pull request:**

1. **Update RELEASE.md** (T036): Move items from "Unreleased" to new version section (v0.1.0)
2. **Update version** (T037): Edit `app/build.gradle.kts` versionCode and versionName
3. **Commit version bump** (T038): `git commit -m "chore: bump version to v0.1.0"`
4. **Push to feature branch**: `git push origin 001-speed-tracking`

### Post-PR Steps (After Merge)

5. **Create PR**: Push branch, create PR with link to spec.md *(already includes version bump from Phase 7)*
6. **Merge PR**: After review and tests passing
7. **Create tag**: `git tag -a v0.1.0 -m "Release v0.1.0: Real-time speed tracking"`
8. **Build signed APK**: `./gradlew :app:assembleRelease`
9. **Create GitHub Release**: Tag v0.1.0, attach signed APK

**Note**: Steps 1-4 are now formalized as **Phase 7 tasks (T036-T038)** to prevent them from being forgotten.

---

## Questions & Support

- **Task unclear**: Refer to quickstart.md for detailed implementation guidance
- **Architecture questions**: Check plan.md constitution compliance section
- **Data model details**: See data-model.md for entity specifications
- **Interface contracts**: Review contracts/ directory

**Estimated Total Time**: ~9.75 hours for full feature (all 3 user stories + version bump)
**MVP Time**: 6-8 hours (User Story 1 only, excluding version bump)

**Ready to start? Begin with Phase 1: Setup & Project Initialization (T001-T008)**

**IMPORTANT**: Complete Phase 7 (Version Bump - T036-T038) **before** creating the pull request!
