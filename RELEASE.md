# BikeRedlights - Release Notes

> **Purpose**: Unified release tracking for all versions, features, and changes
> **Versioning**: Semantic Versioning (MAJOR.MINOR.PATCH)

## Unreleased

_Features and changes completed but not yet released_

### ✨ Features Added
- None yet

---

## v0.2.0 - Basic Settings Infrastructure (2025-11-04)

### ⚙️ Settings & Configuration

**Status**: ✅ COMPLETE - Comprehensive settings system with DataStore persistence
**Focus**: User-configurable ride tracking preferences (units, GPS accuracy, auto-pause)
**APK Size**: TBD (release build)
**Tested On**: Pixel 9 Pro Emulator (Android 15 / API 35)

### ✨ Features Added

**Feature 2A: Basic Settings Infrastructure** ([spec](specs/001-settings-infrastructure/spec.md))

- **User Story 1 (P1): Select Preferred Units (Metric/Imperial)** ✅
  - Settings → Ride & Tracking → Units segmented button (Metric/Imperial)
  - Default: Metric (km/h, meters)
  - DataStore persistence: Units selection persists across app restarts
  - Domain: UnitsSystem enum with conversion utilities

- **User Story 2 (P2): Adjust GPS Accuracy for Battery Life** ✅
  - Settings → Ride & Tracking → GPS Accuracy toggle (High Accuracy/Battery Saver)
  - High Accuracy: 1-second GPS updates for real-time tracking
  - Battery Saver: 4-second GPS updates for battery optimization
  - Default: High Accuracy
  - Integration: LocationRepository dynamically configures GPS intervals based on setting
  - DataStore persistence: GPS accuracy selection persists across app restarts

- **User Story 3 (P3): Enable Auto-Pause for Commutes** ✅
  - Settings → Ride & Tracking → Auto-Pause Rides toggle with threshold picker
  - Threshold options: 1, 2, 3, 5, 10, 15 minutes
  - Default: Disabled (5 minutes when enabled)
  - DataStore persistence: Auto-pause config (enabled + threshold) persists across app restarts
  - Note: Actual pause/resume logic deferred to Feature 1A (Core Ride Recording)

**Bottom Navigation Bar** ✅
- Material 3 NavigationBar with 3 tabs: Live, Rides, Settings
- Tab selection preserved across screen changes
- Icons: Compass (Live), List (Rides), Settings

**Settings Architecture** ✅
- SettingsScreen: Main settings menu with navigation cards
- RideTrackingSettingsScreen: Detail screen for Ride & Tracking settings
- SettingsViewModel: StateFlow-based reactive UI state management
- SettingsRepository: DataStore Preferences for key-value persistence
- Domain models: UnitsSystem, GpsAccuracy, AutoPauseConfig with validation

**Reusable Settings UI Components** ✅
- SegmentedButtonSetting: Material 3 segmented button for 2-option choices
- ToggleWithPickerSetting: Toggle switch with conditional dropdown picker
- Accessibility: 48dp minimum touch targets, TalkBack contentDescriptions
- Dark mode support with Material 3 theming

### ✅ Test Coverage
- **Unit Tests**: 57 tests passing
  - Settings domain models and utilities
  - SettingsRepository persistence
  - SettingsViewModel state management
  - All existing tests remain passing
- **Instrumented Tests**: 12+ tests
  - SettingsNavigationTest: UI interactions for all 3 settings
  - SettingsPersistenceTest: DataStore persistence validation across app restarts
- **Emulator Validation**: ✅ Persistence validated for all 3 settings across app restarts

### 🏗️ Architecture
- Clean Architecture: UI → ViewModel → Domain → Data
- MVVM pattern with StateFlow for reactive state
- Manual dependency injection (Hilt deferred to v0.3.0 per Constitution exception)
- DataStore Preferences for local persistence (no network, no database)

### 🐛 Bugs Fixed
- **Auto-Pause Toggle Race Condition**: Fixed toggle not staying enabled when clicked
  - Root cause: Two sequential ViewModel calls reading stale state
  - Solution: Added atomic `setAutoPauseConfig()` method for single-transaction updates
  - Validated on emulator: Toggle now enables correctly and persists across app restarts

### 📦 Files Changed
- **21+ commits** across 6 phases (Setup, Foundation, 3 User Stories, Polish, Bug Fix)
- **Domain layer**: 3 models, 1 utility class
- **Data layer**: SettingsRepository interface + implementation
- **UI layer**: 2 screens, 2 reusable components, ViewModel, navigation integration
- **Test layer**: Unit tests + instrumented tests

---

## v0.1.0 - Real-Time Speed and Location Tracking (2025-11-03)

### 🚴 First MVP Release

**Status**: ✅ COMPLETE - Real-time GPS-based speed tracking
**Focus**: Cycling speedometer with location display and GPS status feedback
**APK Size**: 22MB (release build)
**Tested On**: Pixel 9 Pro Emulator (Android 15 / API 35)

### ✨ Features Added

**Feature 001: Real-Time Speed and Location Tracking** ([spec](specs/001-speed-tracking/spec.md))

**User Story 1 (P1): View Current Speed While Riding** ✅
- Real-time cycling speed display in km/h
- Large, readable typography optimized for at-a-glance viewing
- Stationary detection (<1 km/h shows 0 km/h)
- Speed calculation from GPS with m/s to km/h conversion (×3.6)
- Automatic pause on app background (battery optimization)

**User Story 2 (P2): View Current GPS Position While Riding** ✅
- Latitude/longitude display with 6-decimal precision
- GPS accuracy indicator (±X.X m)
- "Acquiring GPS..." state before first fix
- Coordinate updates in real-time

**User Story 3 (P3): Understand GPS Signal Status** ✅
- Color-coded GPS status indicator:
  - 🔴 Red: GPS Unavailable (indoors, no signal)
  - 🟡 Yellow: Acquiring GPS... (searching for satellites)
  - 🟢 Green: GPS Active (signal acquired with ±X.X m accuracy)
- Accuracy display for Active state

### 🏗️ Architecture

**Clean Architecture with MVVM**:
- **Domain Layer**: LocationData, SpeedMeasurement, GpsStatus models
- **Data Layer**: LocationRepositoryImpl with FusedLocationProviderClient
- **Domain Logic**: TrackLocationUseCase with speed calculation and stationary detection
- **UI Layer**: Jetpack Compose with Material 3
- **State Management**: ViewModel + StateFlow + collectAsStateWithLifecycle

**Key Features**:
- 🔐 Runtime permission handling with rationale dialogs
- ♿ Full accessibility support (TalkBack/semantic content descriptions)
- 🌙 Dark mode compatible (Material3 theme colors)
- 🔄 Configuration change resilient (rotation preserves state)
- 🔋 Battery optimized (1s update interval, pauses when backgrounded)

### ✅ Test Coverage

**90%+ coverage** for safety-critical code (per Constitution requirement):
- **Unit Tests**: 20 tests
  - TrackLocationUseCaseTest: Speed calculation, conversion, stationary threshold
  - SpeedTrackingViewModelTest: State management, permission handling, error handling
- **UI Tests**: 26 tests
  - SpeedTrackingScreenTest (8 tests)
  - LocationDisplayTest (8 tests)
  - GpsStatusIndicatorTest (10 tests)

### 📦 Files Changed
- **34 files**, 5,994 insertions
- **Production code**: 17 files (domain models, repository, use case, UI components, ViewModel)
- **Test code**: 3 unit test files + 3 UI test files
- **Spec files**: 13 documentation files

### 🔧 Technical Details
- FusedLocationProviderClient with PRIORITY_HIGH_ACCURACY
- Location update interval: 1000ms (1 second)
- Lifecycle-aware state collection (Lifecycle.State.STARTED)
- Speed calculation: Haversine distance formula for fallback
- Manual dependency injection (Hilt still disabled awaiting compatibility fix)

### 🐛 Bugs Fixed
- Fixed ProGuard configuration violation (enabled minification per Constitution v1.3.0)

### 💥 Breaking Changes
- None yet

### 📚 Documentation
- **Comprehensive Documentation Review & Fixes** (2025-11-02):
  - Created APK signing documentation (`.specify/docs/apk-signing.md`)
  - Created PR template (`.github/pull_request_template.md`)
  - Enhanced CLAUDE.md with:
    - Git push guidance and cadence recommendations
    - Emulator GPS simulation instructions
    - Version code calculation rules
    - Post-release workflow documentation
    - Dependency scanning process
  - Updated Constitution v1.3.0 with:
    - Branch naming convention (`###-feature-name`)
    - Test requirements by feature type (safety-critical vs non-critical)
    - Branch protection guidelines
  - Updated version references (v1.1.0 → v1.3.0) across all files
  - Added comprehensive ProGuard rules for BikeRedlights dependencies
- Added CLAUDE.md with November 2025 Android development standards
- Ratified BikeRedlights Constitution v1.3.0
- Established project documentation tracking (TODO.md, RELEASE.md)

### 🔧 Internal Changes
- Created CI/CD workflow (`.github/workflows/pr-checks.yml`) for:
  - Kotlin lint checks
  - Debug APK build verification
  - Constitution compliance checks
  - **Note**: Testing is manual (emulator/physical device) per project standards
- Initial project setup with Specify template
- Configured MVVM + Clean Architecture structure
- Set up Material Design 3 Expressive guidelines

---

## v0.0.0 - Buildable Skeleton (2025-11-02)

### Project Skeleton

**Status**: ✅ COMPLETE - Builds, installs, and launches successfully
**Focus**: Establish buildable foundation with all dependencies configured
**APK Size**: 62MB (debug)
**Tested On**: Pixel 9 Pro Emulator (Android 15 / API 35)

### ✨ Features Added
- Buildable Android project with Gradle 8.13 + AGP 8.7.3 + Kotlin 2.0.21
- Jetpack Compose + Material 3 Dynamic Color theme
  - Light and dark mode support
  - Red color scheme for safety (BikeRedlights theme)
  - Welcome screen with "v0.0.0" display
- Clean Architecture folder structure created:
  - `ui/` (theme, screens, components, navigation)
  - `domain/` (models, use cases, repository interfaces)
  - `data/` (repositories, local, remote)
  - `di/` (dependency injection - ready for Hilt)
- All required dependencies configured (Compose, Room, Coroutines, DataStore, WorkManager, Location Services)
- Location permissions declared in manifest
- Application class configured

### 🐛 Known Issues
- **Hilt DI temporarily disabled** due to Gradle plugin compatibility issues
  - Error: `'java.lang.String com.squareup.javapoet.ClassName.canonicalName()'`
  - Root cause: AGP 8.7.3 + Kotlin 2.0.21 + Hilt 2.48 incompatibility
  - **Resolution plan**: Re-enable in v0.1.0 after upstream fix or workaround
  - Code is ready: TODO markers in `BikeRedlightsApplication.kt` and `MainActivity.kt`
- No actual features - placeholder welcome screen only

### 📚 Documentation Added
- **Java 17 requirement documented** in CLAUDE.md (Development Environment section)
- Project governance finalized (Constitution v1.3.0)
- TODO.md and RELEASE.md templates established

### 🛠️ Technology Stack (Configured)
- **Language**: Kotlin 2.0.21
- **Build**: AGP 8.7.3, Gradle 8.13
- **UI**: Jetpack Compose BOM 2024.11.00 + Material Design 3
- **Architecture**: MVVM + Clean Architecture (folder structure ready)
- **Async**: Coroutines 1.9.0 + Flow/StateFlow
- **DI**: Dagger Hilt (dependencies declared, plugin temporarily disabled)
- **Database**: Room 2.6.1
- **Preferences**: DataStore 1.1.1
- **Location**: Play Services Location 21.3.0
- **Testing**: JUnit, MockK 1.13.13, Turbine 1.2.0, Truth 1.4.4

### 🎯 Next Steps (v0.1.0+)
1. Resolve Hilt Gradle plugin compatibility and re-enable DI
2. Implement first MVP feature (speed detection or GPS tracking)
3. Add actual tests (unit + instrumented)

---

## Version History

| Version | Release Date | Status | Notes |
|---------|--------------|--------|-------|
| v0.2.0  | 2025-11-04   | ✅ Released | Basic settings infrastructure with DataStore persistence |
| v0.1.0  | 2025-11-03   | ✅ Released | Real-time speed tracking - first MVP feature |
| v0.0.0  | 2025-11-02   | ✅ Released | Buildable skeleton - tested on emulator |

---

## Release Process

**How releases are tracked:**
1. Features are developed and added to "Unreleased" section
2. When ready to release, create new version section with date
3. Move items from "Unreleased" to the new version section
4. Update version history table
5. Tag release in git: `git tag -a v1.0.0 -m "Release v1.0.0"`
6. Update app/build.gradle.kts version codes

**Version Bumping Rules** (Semantic Versioning):
- **MAJOR (X.0.0)**: Breaking changes, major feature additions, incompatible API changes
- **MINOR (1.X.0)**: New features, backward-compatible additions
- **PATCH (1.0.X)**: Bug fixes, small improvements, backward-compatible fixes

---

**Constitution Compliance**: This file satisfies the "Project Documentation Tracking" requirement (Constitution v1.3.0, Development Workflow section).
