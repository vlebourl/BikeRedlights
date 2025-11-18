# Implementation Plan: Stop Detection Settings

**Branch**: `008-stop-detection-settings` | **Date**: 2025-11-18 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/008-stop-detection-settings/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Add a new "Stop Detection" settings card to the Settings home screen with three configurable parameters for future red light detection. This feature extends the existing Settings infrastructure (Feature 002/v0.2.0) to prepare for stop detection implementation (Feature 009) and clustering (Feature 010).

**Primary Requirements**:
- Settings card on Settings home screen with navigation to detail screen
- Three settings: Speed Threshold (1-5 km/h), Duration Threshold (5-30s), Clustering Radius (10-50m)
- DataStore persistence with default values (3 km/h, 15s, 20m)
- Imperial/Metric unit conversion for speed threshold
- Material Design 3 UI consistent with existing settings screens

**Technical Approach**: Extend existing SettingsRepository interface and SettingsViewModel with new StateFlows. Create domain model (StopDetectionConfig) with validation. Add new settings card to SettingsHomeScreen. Create StopDetectionSettingsScreen composable reusing existing SettingCard pattern.

## Technical Context

**Language/Version**: Kotlin 2.0.21 with Java 17 (OpenJDK)
**Primary Dependencies**:
- Jetpack Compose (BOM 2024.11.00) with Material 3 for UI
- Dagger Hilt 2.51.1 for dependency injection
- DataStore Preferences (latest) for settings persistence
- Kotlin Coroutines 1.9.0 for async operations and Flow/StateFlow

**Storage**: DataStore Preferences (key-value pairs, local device only) - existing infrastructure from Feature 002
**Testing**: JUnit 5 for unit tests, Compose UI Test for UI testing, Android Emulator for integration validation
**Target Platform**: Android 8.0+ (API 26+), targetSdk 36, compileSdk 36
**Project Type**: Mobile (Android native app with MVVM + Clean Architecture)
**Performance Goals**:
- Settings screen loads < 100ms
- Setting changes persist < 500ms (Success Criterion SC-007)
- 60 fps UI rendering for smooth navigation and animations
**Constraints**:
- Offline-first: All settings stored and accessed locally (no network required)
- Battery-efficient: DataStore reads/writes are non-blocking
- Material 3 compliance: Consistent theming, dark mode support, accessibility (48dp touch targets)
**Scale/Scope**:
- 3 new settings (speed threshold, duration threshold, clustering radius)
- 1 new settings card on home screen
- 1 new detail screen (StopDetectionSettingsScreen)
- Extends existing SettingsRepository interface with 2 new methods
- Reuses existing SettingCard composable and settings navigation pattern

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: ✅ **PASS** (Constitution file contains template only - no project-specific gates defined)

**Note**: The BikeRedlights project has a template constitution file (`.specify/memory/constitution.md`) that has not been customized with project-specific principles or gates. This is acceptable for this feature as we are following established patterns from CLAUDE.md (Android development standards).

**Alignment with CLAUDE.md Standards**:
- ✅ **Kotlin-first**: All code will be Kotlin 2.0.21
- ✅ **MVVM + Clean Architecture**: Extends existing Repository → ViewModel → UI pattern
- ✅ **Jetpack Compose**: UI implemented in Compose (no XML layouts)
- ✅ **DataStore**: Uses existing DataStore Preferences infrastructure
- ✅ **Material 3**: UI follows Material Design 3 Expressive guidelines
- ✅ **Testing Requirements**: Unit tests for domain models, emulator testing before merge
- ✅ **Small Commits**: Will commit after each logical unit (domain model, repository extension, UI components)
- ✅ **Documentation Updates**: Will update TODO.md and RELEASE.md during development

**Re-evaluation Trigger**: After Phase 1 design (data-model.md and contracts complete), verify no architectural violations introduced.

---

## Constitution Check Re-Evaluation (Post Phase 1)

**Date**: 2025-11-18
**Status**: ✅ **PASS** - No violations introduced during design phase

**Phase 1 Artifacts Review**:
- ✅ **data-model.md**: Domain model with validation (pure Kotlin, no Android dependencies)
- ✅ **contracts/SettingsRepository.kt**: Clean interface extension following existing pattern
- ✅ **contracts/SettingsViewModel.kt**: MVVM pattern with StateFlow
- ✅ **contracts/StopDetectionSettingsScreen.kt**: Stateless Composable, state hoisted to ViewModel
- ✅ **quickstart.md**: Implementation guide with testing checklist

**Architectural Integrity Check**:
- ✅ **Clean Architecture maintained**: Domain → Data → UI layers respected
- ✅ **MVVM pattern followed**: Repository → ViewModel → UI flow intact
- ✅ **Material 3 compliance**: UI contracts specify Material 3 components
- ✅ **Testing requirements met**: Unit tests + Compose UI tests + Emulator testing planned
- ✅ **No new dependencies**: Reuses existing infrastructure (DataStore, Hilt, Compose)

**Complexity Assessment**:
- ✅ **No additional complexity**: All patterns follow Feature 002 established patterns
- ✅ **No new abstractions**: Extends existing Repository and ViewModel interfaces
- ✅ **Reuses UI components**: SettingCard and SegmentedButtonSetting already exist

**Final Verdict**: Design phase complete with no constitutional violations. Ready to proceed to implementation (tasks.md generation via `/speckit.tasks`).

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/bikeredlights/
├── domain/
│   └── model/
│       └── settings/
│           ├── StopDetectionConfig.kt          # NEW: Domain model with validation
│           ├── UnitsSystem.kt                  # EXISTING: Metric/Imperial enum
│           ├── AutoPauseConfig.kt              # EXISTING: Reference pattern
│           └── GpsAccuracy.kt                  # EXISTING: Reference pattern
│
├── data/
│   ├── repository/
│   │   ├── SettingsRepository.kt               # MODIFY: Add stop detection methods
│   │   └── SettingsRepositoryImpl.kt           # MODIFY: Implement new methods
│   └── local/
│       └── datastore/
│           └── PreferencesKeys.kt              # MODIFY: Add DataStore keys
│
├── ui/
│   ├── viewmodel/
│   │   └── SettingsViewModel.kt                # MODIFY: Add stopDetectionConfig StateFlow
│   ├── screens/
│   │   └── settings/
│   │       ├── SettingsHomeScreen.kt           # MODIFY: Add Stop Detection card
│   │       ├── RideTrackingSettingsScreen.kt   # EXISTING: Reference pattern
│   │       └── StopDetectionSettingsScreen.kt  # NEW: Detail screen composable
│   ├── components/
│   │   └── settings/
│   │       ├── SettingCard.kt                  # EXISTING: Reuse for card
│   │       └── SegmentedButtonSetting.kt       # EXISTING: Reuse for controls
│   └── navigation/
│       └── SettingsNavGraph.kt                 # MODIFY: Add stop detection route
│
└── di/
    └── AppModule.kt                             # NO CHANGE: DataStore already injected

app/src/test/java/com/example/bikeredlights/
└── domain/
    └── model/
        └── settings/
            └── StopDetectionConfigTest.kt       # NEW: Unit tests for validation
```

**Structure Decision**: Android mobile app following MVVM + Clean Architecture with three-layer separation:
- **Domain Layer** (`domain/`): Business logic and models (pure Kotlin, no Android dependencies)
- **Data Layer** (`data/`): Repository implementations and DataStore persistence
- **UI Layer** (`ui/`): Jetpack Compose screens, ViewModels, and navigation

This feature extends the existing settings infrastructure established in Feature 002 (v0.2.0). We follow the same patterns:
- Domain model with built-in validation (like `AutoPauseConfig`)
- Repository interface extension with suspend methods and Flow properties
- ViewModel exposing StateFlow for reactive UI updates
- Composable screens reusing existing `SettingCard` and segmented button components

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Status**: No violations - this section is not applicable.

All design decisions follow established patterns from Feature 002 and CLAUDE.md standards. No additional complexity introduced.
