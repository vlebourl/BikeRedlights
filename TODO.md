# BikeRedlights - Project TODO

> **Last Updated**: 2025-11-07
> **Purpose**: Unified progress tracking for all features, tasks, and pending work

## 📋 In Progress

_Features currently being developed_

<!-- Example:
### Feature: Speed Detection System
- **Started**: 2025-11-02
- **Status**: Implementation in progress
- **Description**: Implement GPS-based speed detection with configurable thresholds
- **Tasks Remaining**:
  - [ ] Domain layer use case
  - [ ] ViewModel integration
  - [ ] UI composable
  - [x] Repository setup
- **Blockers**: None
-->

_(No features currently in progress)_

---

## 🎯 Planned

_Features planned for upcoming development_

<!-- Example:
### Feature: Red Light Warning System
- **Priority**: P1 - Critical
- **Description**: Alert cyclists when approaching red lights at speed
- **Dependencies**: Speed Detection System
- **Estimated Effort**: 5-7 days
- **Notes**: Core safety feature, requires GPS accuracy validation
-->

### Bug: Auto-Resume Not Working After Auto-Pause
- **Priority**: P0 - Critical (Safety Issue)
- **Type**: Bug Fix
- **Discovered In**: v0.4.0 real-world ride test (2025-11-07)
- **Description**: Auto-pause correctly triggers when speed < 1 km/h, but does NOT automatically resume recording when speed increases above threshold
- **Current Behavior**: User must manually tap "Resume" button while cycling, requiring phone interaction during ride
- **Expected Behavior**: Should automatically resume recording when speed > 1 km/h (per FR-010 specification from v0.3.0)
- **Impact**: Safety-critical - forces cyclists to interact with phone while moving
- **Investigation Needed**:
  - Auto-resume code exists in `RideRecordingService.kt:530-565`
  - Logic appears correct but not triggering in production environment
  - Potential causes:
    - GPS update frequency issue (HIGH_ACCURACY vs BATTERY_SAVER mode)
    - State transition bug in service lifecycle
    - Race condition between pause/resume logic
    - Grace period logic (30 seconds) preventing immediate resume
- **Related Files**:
  - `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt` (service logic)
  - `app/src/main/java/com/example/bikeredlights/domain/model/settings/AutoPauseConfig.kt` (thresholds)
- **Testing Required**: Physical device testing with real GPS movement patterns
- **Estimated Effort**: 3-5 hours (investigation + fix + testing)
- **Target Release**: v0.4.1 (patch release)

### Bug: Live Current Speed Stuck at 0.0
- **Priority**: P1 - High (UX-Critical)
- **Type**: Bug Fix
- **Discovered In**: v0.4.0 real-world ride test (2025-11-07)
- **Description**: Current speed always displays 0.0 km/h on Live tab during recording, even though max speed and average speed update correctly with real values
- **Current Behavior**: Hardcoded 0.0 value in UI, speed metric is non-functional
- **Expected Behavior**: Display real-time GPS speed from latest track point
- **Impact**: Defeats purpose of live tracking, prevents speed awareness during ride
- **Root Cause**: Hardcoded value in `LiveRideScreen.kt:347-452` with existing TODO comment
- **Solution Architecture** (Clean Architecture pattern):
  1. **Service Layer**: Add `currentSpeedMetersPerSec` to ride state broadcasts
  2. **Repository Layer**: Store current speed in `RideRecordingStateRepository` (StateFlow or DataStore)
  3. **ViewModel Layer**: Expose `currentSpeed: StateFlow<Double>` in `RideRecordingViewModel`
  4. **UI Layer**: Collect StateFlow in `LiveRideScreen` and pass to `RideStatistics` component
- **Related Files**:
  - `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt` (broadcast logic)
  - `app/src/main/java/com/example/bikeredlights/data/repository/RideRecordingStateRepositoryImpl.kt` (state storage)
  - `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt` (expose StateFlow)
  - `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt` (UI integration, lines 347-452)
  - `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt` (component ready, just needs data)
- **Testing Required**: Emulator testing with GPS simulation + physical device testing
- **Estimated Effort**: 2-3 hours
- **Target Release**: v0.4.1 (patch release)

### Enhancement: Prioritize Current Speed Over Elapsed Time in UI
- **Priority**: P2 - Medium (UX Improvement)
- **Type**: Enhancement
- **Discovered In**: v0.4.0 real-world ride test (2025-11-07)
- **Description**: Make current speed the PRIMARY display element (largest/most prominent) and elapsed time SECONDARY
- **Current Layout**: Elapsed time = displayLarge typography (most prominent), current speed = headlineMedium in 2x2 grid with other metrics
- **Proposed Layout**: Current speed = displayLarge (hero metric), elapsed time = headlineMedium (supporting metric)
- **Rationale**:
  - Speed is critical for core safety mission (red light warnings depend on speed awareness)
  - Timer is informational, not safety-critical
  - Aligns UI priority with app purpose (safety > fitness tracking)
- **Dependencies**:
  - **BLOCKED**: Must fix "Live Current Speed Stuck at 0.0" bug first (P1)
  - No point making 0.0 the primary display
- **Related Files**:
  - `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt` (lines 77-133, layout refactoring)
- **Design Considerations**:
  - Material 3 typography scale adjustments
  - Maintain accessibility (minimum touch targets, contrast)
  - Ensure dark mode compatibility
  - Consider tablet/landscape layouts
- **Testing Required**: Visual regression testing on multiple screen sizes and orientations
- **Estimated Effort**: 1-2 hours (UI refactoring only, no logic changes)
- **Target Release**: v0.5.0 (minor feature release after P0/P1 bugs fixed)

---

## ✅ Completed

_Features completed and merged_

### Feature 003: Ride History and List View
- **Completed**: 2025-11-06
- **Description**: Complete ride history management with sorting, filtering, deletion, and detailed statistics
- **Status**: ✅ All 5 user stories complete, fully tested on emulator, released as v0.4.0
- **Details**:
  - **User Story 1: View List of All Rides**
    - Material 3 ride history screen with reactive state management (RideHistoryViewModel + Flow)
    - Custom ride list item cards with swipe-to-delete functionality
    - Empty state view for first-time users with friendly guidance
    - Loading and error state handling with sealed RideHistoryUiState class
    - Automatic list updates via Room Flow queries
  - **User Story 2: View Detailed Ride Statistics**
    - Dedicated ride detail screen with comprehensive statistics (RideDetailScreen)
    - Display metrics: start time, end time, duration, distance, average speed, max speed
    - Unit system support (metric/imperial) from user preferences
    - Top app bar with ride name and back navigation
    - Material 3 card layout for stat grouping with emoji icons
  - **User Story 3: Sort Rides by Multiple Criteria**
    - Sort dialog with 6 sorting options (newest/oldest, longest/shortest distance/duration)
    - Persistent sort preference using DataStore Preferences
    - Real-time list updates when sort changes using `flatMapLatest` operator
    - Sort icon button in top app bar with visual indicator for current selection
  - **User Story 4: Delete Rides**
    - Swipe-to-reveal delete button on ride cards
    - Confirmation dialog before permanent deletion (DeleteConfirmationDialog)
    - Cascade deletion of all associated data (ride entity + all location data points)
    - Automatic list refresh after deletion via Flow
    - Error handling with snackbar feedback
  - **User Story 5: Filter Rides by Date Range**
    - Date range filter dialog with Material 3 DatePickerDialog
    - Custom date range selection (start date + end date)
    - "Clear Filter" option to show all rides
    - Filter icon button in top app bar
    - Real-time list updates when filter changes
    - Filter state maintained during session (not persisted across restarts)
  - **Architecture**:
    - Domain: RideListItem, RideDetailItem, DateRangeFilter, SortPreference models
    - Domain: GetAllRidesUseCase, GetRideDetailUseCase, DeleteRideUseCase
    - Data: Extended RideRepository with 12 new Room queries (6 sort variants + filtering + deletion)
    - UI: RideHistoryViewModel, RideDetailViewModel with reactive Flow state
    - UI: RideHistoryScreen, RideDetailScreen, 5 reusable components (cards, dialogs, empty state)
  - **Quality Assurance**:
    - Fully tested on Pixel 9 Pro emulator (Android 15 / API 35)
    - All 8 test scenarios validated (empty state, sorting, filtering, deletion, detail nav, units, dark mode, rotation)
    - Material 3 theming with dark mode support
    - Accessibility: content descriptions, 48dp touch targets, high contrast
- **Critical Bug Fix**: Fixed sort preference not updating list by using `flatMapLatest` instead of nested `.onEach { collect() }` for proper Flow cancellation
- **Test Coverage**: All user stories validated on emulator
- **Architecture**: MVVM + Clean Architecture with reactive Flow and sealed state classes
- **Git Commits**: 32 commits across 5 user stories + critical bug fix
- **Release**: v0.4.0 (GitHub release with signed 22MB APK)
- **Pull Request**: #4

### Feature 2A: Basic Settings Infrastructure
- **Completed**: 2025-11-04
- **Description**: Implemented comprehensive settings system with DataStore persistence, Material 3 UI, and bottom navigation
- **Status**: ✅ All 3 user stories complete, 57 unit tests passing, 12+ instrumented tests, accessibility validated
- **Details**:
  - **User Story 1: Select Preferred Units (Metric/Imperial)**
    - Domain: UnitsSystem enum, conversion utilities
    - Data: SettingsRepository with DataStore Preferences
    - UI: SegmentedButtonSetting composable, RideTrackingSettingsScreen
    - Persistence: Units selection persists across app restarts
  - **User Story 2: Adjust GPS Accuracy for Battery Life**
    - Domain: GpsAccuracy enum (HIGH_ACCURACY: 1s, BATTERY_SAVER: 4s)
    - Integration: LocationRepository reads GPS accuracy from settings, configures update intervals dynamically
    - UI: GPS Accuracy toggle in Ride & Tracking screen
    - Persistence: GPS accuracy persists and affects location tracking
  - **User Story 3: Enable Auto-Pause for Commutes**
    - Domain: AutoPauseConfig model (enabled: Boolean, thresholdMinutes: Int)
    - UI: ToggleWithPickerSetting composable with threshold picker (1-15 minutes)
    - Persistence: Auto-pause config (toggle + threshold) persists across app restarts
    - Note: Actual pause/resume logic deferred to Feature 1A (Core Ride Recording)
  - **Navigation Infrastructure**:
    - Bottom navigation bar with 3 tabs: Live, Rides, Settings
    - SettingsScreen with navigation to Ride & Tracking detail screen
    - SettingsViewModel with StateFlow for reactive UI updates
  - **Quality Assurance**:
    - 48dp minimum touch targets (WCAG compliant)
    - Comprehensive accessibility (contentDescription, TalkBack support)
    - Material 3 theming with dark mode support
    - Preview compositions for all components
- **Test Coverage**: 57 unit tests, 12+ instrumented tests
- **Architecture**: Clean Architecture (UI → ViewModel → Domain → Data), MVVM pattern
- **Git Commits**: 20+ commits across 6 phases

### v0.0.0 - Buildable Project Skeleton
- **Completed**: 2025-11-02
- **Description**: Established buildable Android project foundation with all dependencies configured
- **Status**: ✅ Builds, installs on emulator, and launches successfully (Pixel 9 Pro / Android 15)
- **Details**:
  - Configured Gradle 8.13 + AGP 8.7.3 + Kotlin 2.0.21
  - Set up Jetpack Compose + Material 3 Dynamic Color theme
  - Created Clean Architecture folder structure (ui, domain, data, di)
  - Configured all required dependencies (Compose, Room, Coroutines, DataStore, WorkManager, Location Services)
  - Added location permissions to manifest
  - Created welcome screen displaying v0.0.0
  - Documented Java 17 requirement in CLAUDE.md
  - **Known Issue**: Hilt DI temporarily disabled due to Gradle plugin compatibility (will fix in v0.1.0)
- **APK Size**: 62MB (debug)
- **Git Tag**: v0.0.0

### Documentation Completeness Review & Fixes
- **Completed**: 2025-11-02
- **Description**: Comprehensive review and systematic fixes of all project documentation and governance files
- **Details**:
  - **P0 Critical Blockers Fixed**:
    - Created APK signing documentation (`.specify/docs/apk-signing.md`)
    - Fixed ProGuard configuration violation (`app/build.gradle.kts`)
    - Added version code calculation rules to CLAUDE.md
    - Created PR template (`.github/pull_request_template.md`)
    - Updated version references (v1.1.0 → v1.3.0) across all files
  - **P1 High Priority Fixed**:
    - Added branch naming convention to constitution
    - Enhanced git push guidance in CLAUDE.md (user-requested)
    - Added emulator GPS simulation instructions to CLAUDE.md
    - Clarified test requirements in constitution (safety-critical vs non-critical)
    - Added post-release workflow documentation to CLAUDE.md
  - **P2 Nice-to-Have Fixed**:
    - Created CI/CD workflow (`.github/workflows/pr-checks.yml`)
    - Added branch protection documentation to constitution
    - Added dependency scanning process to CLAUDE.md
  - **Result**: Documentation is now 100% complete and production-ready

### Initial Project Setup
- **Completed**: 2025-11-02
- **Description**: Project initialization with Specify template, Android standards documentation, and constitution
- **Details**:
  - Created CLAUDE.md with November 2025 Android development standards
  - Ratified BikeRedlights Constitution v1.3.0
  - Established MVVM + Clean Architecture guidelines
  - Configured project structure and governance

---

## 🔄 Deferred

_Features postponed or on hold_

<!-- Example:
### Feature: Social Sharing
- **Deferred**: 2025-11-15
- **Reason**: Low priority, focusing on core safety features first
- **Reconsider**: After v1.0 release
-->

_(No deferred features)_

---

## 📝 Notes

- This file is automatically updated as features progress through the development lifecycle
- User does NOT need to request TODO.md updates; they happen automatically per Constitution
- See RELEASE.md for version-specific feature tracking
- See `.specify/specs/` for detailed feature specifications

---

**Constitution Compliance**: This file satisfies the "Project Documentation Tracking" requirement (Constitution v1.3.0, Development Workflow section).
