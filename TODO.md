# BikeRedlights - Project TODO

> **Last Updated**: 2025-11-08 (Feature 005: Live Current Speed - Released v0.4.2)
> **Purpose**: Unified progress tracking for all features, tasks, and pending work

## 📋 In Progress

_Features currently being developed_

### Feature 006: Maps Integration (Feature 1B)
- **Started**: 2025-11-08
- **Status**: Specification complete, ready for planning
- **Description**: Add Google Maps SDK integration to display real-time route tracking on Live tab and complete route visualization on Review Screen
- **Tasks**:
  - [x] Create feature specification (spec.md)
  - [x] Validate specification quality (requirements.md checklist)
  - [ ] Generate implementation plan (/speckit.plan)
  - [ ] Implement Google Maps SDK setup
  - [ ] Add map to Live tab with current location marker
  - [ ] Add real-time route polyline to Live tab
  - [ ] Add complete route map to Review Screen
  - [ ] Add start/end markers to Review Screen
  - [ ] Test on emulator with GPS simulation
  - [ ] Test on physical device
  - [ ] Create pull request
  - [ ] Release v0.5.0
- **Blockers**: None
- **Specification**: specs/006-map/spec.md

---

## 🎯 Planned

_Features planned for upcoming development_

_(No planned features currently - all identified enhancements have been implemented)_

---

## ✅ Completed

_Features completed and merged_

### Feature 005: Fix Live Current Speed Display Bug + UI Prioritization
- **Completed**: 2025-11-08
- **Type**: P1 Bug Fix + P2 UX Enhancement (combined in single PR)
- **Description**:
  1. Fix current speed displaying hardcoded 0.0 km/h on Live tab during recording
  2. Prioritize current speed as hero metric (displayLarge) for safety-first design
  3. Add paused time display (manual + auto-pause combined)
  4. Add immobile time placeholder for future tracking
- **Status**: ✅ COMPLETE - Released v0.4.2
- **Implementation Summary**:
  - **Bug Fix (Speed Data Flow)**:
    - Domain layer: Added `getCurrentSpeed(): StateFlow<Double>` to repository interface
    - Data layer: Implemented StateFlow with `updateCurrentSpeed()` and `resetCurrentSpeed()` methods
    - Service layer: Emit GPS speed on every location update, reset on pause/stop
    - ViewModel layer: Expose StateFlow with `stateIn(WhileSubscribed(5000))` for battery optimization
    - UI layer: Collect speed from ViewModel and wire to RideStatistics component
    - Uses GPS Doppler speed (`location.getSpeed()`) when available (most accurate method per Android best practices)
  - **UI Enhancement (Safety-First Layout)**:
    - Current speed now PRIMARY display (displayLarge: 57sp) - hero metric
    - Duration/distance moved to SECONDARY row (headlineMedium: 28sp)
    - Average/max speed in supporting grid (titleLarge: 22sp)
    - Paused time in informational row (manual + auto-pause combined)
    - Immobile time placeholder (future feature for stopped-at-lights tracking)
    - Removed time-of-day display (low value, cluttered UI)
    - Aligns UI priority with safety mission (speed awareness > fitness tracking)
- **Git Commits**:
  - feba18f: docs - specification and tracking
  - ecdf7a7: feat(data) - repository StateFlow implementation
  - ccc7c08: feat(service) - GPS speed emission
  - be34815: feat(viewmodel) - StateFlow exposure
  - 7bdc739: fix(ui) - wire to LiveRideScreen
  - 24bd901: docs - emulator limitation and testing requirements
  - c240657: docs - update TODO.md with draft PR #6 link
  - 16df838: feat(ui) - prioritize current speed as hero metric
  - 117a052: feat(ui) - add paused time display to ride statistics
  - 8092ee6: feat(ui) - add immobile time placeholder to ride statistics
  - b42cf77: docs - update TODO.md with paused and immobile time features
  - 86b992d: chore - bump version to v0.4.2
- **Build Status**: ✅ Release APK built successfully (22MB)
- **Testing**: ✅ Physical device testing completed on real bike ride with GPS
- **Pull Request**: #6 (Merged) - https://github.com/vlebourl/BikeRedlights/pull/6
- **Release**: v0.4.2 - https://github.com/vlebourl/BikeRedlights/releases/tag/v0.4.2
- **Specification**: specs/005-fix-live-speed/spec.md

### Feature 004: Fix Auto-Resume After Auto-Pause
- **Completed**: 2025-11-07
- **Type**: P0 Critical Bug Fix (Safety Issue)
- **Description**: Fixed critical bug where auto-resume does not trigger after auto-pause, forcing cyclists to manually interact with phone while riding
- **Status**: ✅ Core implementation complete, ready for testing
- **Root Cause**: Auto-resume logic was structurally unreachable - trapped inside `updateRideDistance()` function which is only called when NOT paused
- **Solution**: Extracted `checkAutoResume()` function and relocated call to before pause gate, ensuring execution during AutoPaused state
- **Implementation Details**:
  - Created `checkAutoResume()` function (RideRecordingService.kt:440-500, 63 lines)
  - Integrated auto-resume call before pause gate in location update flow (line 434-436)
  - Removed unreachable duplicate code from `updateRideDistance()` (35 lines deleted)
  - Added FR-012 logging with rideId, speed, threshold for debugging
  - Build verified successful (0 compilation errors)
- **Code Changes**:
  - Modified: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt` (~50 LOC)
  - Total diff: +98 lines, -95 lines
- **Testing Status**: ⚠️ Pending physical device validation
  - Unit tests (T008-T016): Not implemented (manual testing preferred for P0 bug fix)
  - Emulator testing: Pending
  - Physical device testing (5 bike rides): Pending - **Required before production deployment**
- **Git Commits**:
  - 26a5fe2 "fix(service): implement auto-resume after auto-pause"
  - Specification: `specs/004-fix-auto-resume/spec.md`
- **Target Release**: v0.4.1 (patch release)
- **Next Steps**: Physical device testing with real GPS movement patterns required

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
