# Implementation Plan: Map UX Improvements (v0.6.1 Patch)

**Branch**: `007-map-ux-improvements` | **Date**: 2025-11-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-map-ux-improvements/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This patch release enhances the v0.6.0 map feature with four UX improvements: (1) map bearing rotation to follow rider direction instead of fixed north-up orientation, (2) directional location marker (arrow/bike icon) that rotates with heading, (3) real-time pause counter updates while paused, and (4) granular auto-pause timing options (1s, 2s, 5s, 10s, 15s, 30s). All changes are UI/UX enhancements to existing components without schema changes or new data entities.

**Technical Approach**: Leverage existing Google Maps Compose APIs for map bearing/rotation, implement custom composable for directional marker with rotation transformation, use Kotlin Flow + collectAsStateWithLifecycle for real-time pause counter updates, and extend DataStore Preferences settings with new timing options. All changes are isolated to the UI layer (ViewModels, Composables, Settings).

## Technical Context

**Language/Version**: Kotlin 2.0.21, Java 17 (OpenJDK)
**Primary Dependencies**:
- Jetpack Compose BOM 2024.11.00 (UI framework)
- Maps Compose 4.4.1 (Google Maps integration)
- Play Services Location 21.3.0 (GPS/bearing data)
- Hilt 2.51.1 (dependency injection)
- DataStore Preferences 1.1.1 (settings storage)
- Kotlin Coroutines 1.9.0 + Flow (reactive state management)

**Storage**: DataStore Preferences (auto-pause timing setting), no database changes required
**Testing**: JUnit 5 (unit), Compose UI Test (UI), Turbine (Flow testing), Truth (assertions)
**Target Platform**: Android 14+ (API 34+), Physical device or emulator with GPS/location capabilities
**Project Type**: Mobile (Android) - feature modules within single app
**Performance Goals**:
- Map bearing rotation within 2 seconds of heading change
- Marker rotation within 1 second of heading change
- Pause counter updates every ~1 second without UI lag
- Smooth animations (60fps) for map/marker rotations

**Constraints**:
- GPS bearing data may be unavailable/unreliable when stationary
- Must handle device sleep/background modes for pause counter
- Map rotation must be smooth (no jittery/disorienting jumps)
- Backward compatibility with existing v0.6.0 ride data (no schema changes)
- Battery-efficient (no excessive GPS polling or UI recomposition)

**Scale/Scope**:
- 4 UI components affected (BikeMap, LocationMarker, RideStatistics pause counter, Settings screen)
- 2 ViewModels updated (RideRecordingViewModel for pause counter, SettingsViewModel for timing options)
- 1 settings key added to DataStore
- Estimated ~300-400 LOC changes across 6-8 files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Constitution Status**: The project constitution (`.specify/memory/constitution.md`) is currently a template. Project-specific standards are defined in `/CLAUDE.md`.

**CLAUDE.md Compliance Check**:

| Standard | Requirement | Compliance Status |
|----------|-------------|-------------------|
| Kotlin-first | All new code in Kotlin | ✅ PASS - UI/ViewModel changes only, all Kotlin |
| Jetpack Compose | No new XML layouts | ✅ PASS - Compose-only changes |
| MVVM + Clean Architecture | UI → ViewModel → Domain → Data | ✅ PASS - Changes isolated to UI/ViewModel layer |
| Material 3 Theming | Consistent design system | ✅ PASS - Uses existing theme, no new components |
| Immutability | Prefer `val` over `var` | ✅ PASS - StateFlow/collectAsState pattern |
| Null Safety | Avoid `!!` operator | ✅ PASS - Safe elvis/null checks for bearing data |
| Small commits | <200 LOC per commit | ⚠️ MONITOR - Must break into atomic commits per requirement |
| Emulator testing | Mandatory before merge | ✅ REQUIRED - GPS simulation testing needed |
| Documentation updates | TODO.md + RELEASE.md | ✅ REQUIRED - Patch release documentation |
| DataStore Preferences | Not SharedPreferences | ✅ PASS - Using existing DataStore infrastructure |

**Gate Decision**: ✅ PROCEED - No violations. All changes align with existing architecture patterns. Emulator testing gate applies (GPS bearing simulation required).

## Project Structure

### Documentation (this feature)

```text
specs/007-map-ux-improvements/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output - technology decisions
├── data-model.md        # Phase 1 output - state models and contracts
├── quickstart.md        # Phase 1 output - implementation guide
├── contracts/           # Phase 1 output - ViewModel/component contracts
│   ├── viewmodel-contracts.md
│   └── component-contracts.md
├── checklists/
│   └── requirements.md  # Spec quality validation (completed)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created yet)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/bikeredlights/
├── ui/
│   ├── components/
│   │   ├── map/
│   │   │   ├── BikeMap.kt                    # MODIFY - Add bearing rotation
│   │   │   └── LocationMarker.kt             # MODIFY - Add directional icon + rotation
│   │   └── ride/
│   │       └── RideStatistics.kt             # MODIFY - Real-time pause counter
│   ├── screens/
│   │   ├── ride/
│   │   │   └── LiveRideScreen.kt             # MODIFY - Pass bearing to map
│   │   └── settings/
│   │       └── AutoPauseSettingsScreen.kt    # MODIFY - Update timing options
│   ├── viewmodel/
│   │   ├── RideRecordingViewModel.kt         # MODIFY - Real-time pause counter flow
│   │   └── SettingsViewModel.kt              # MODIFY - New timing options
│   └── theme/
│       └── MapTheme.kt                        # REFERENCE - Existing map styling
├── domain/
│   └── model/
│       └── MapViewState.kt                    # MODIFY - Add bearing field
├── data/
│   └── repository/
│       └── SettingsRepository.kt              # MODIFY - New auto-pause timing key
└── service/
    └── RideRecordingService.kt                # REFERENCE - Existing bearing data source

app/src/test/java/com/example/bikeredlights/
├── ui/viewmodel/
│   ├── RideRecordingViewModelTest.kt          # ADD - Pause counter tests
│   └── SettingsViewModelTest.kt               # ADD - Timing options tests
└── ui/components/
    ├── BikeMapTest.kt                          # ADD - Bearing rotation tests
    └── LocationMarkerTest.kt                   # ADD - Directional marker tests
```

**Structure Decision**: Android single-app architecture with feature modules. All changes are isolated to existing UI/ViewModel/Settings layers. No new database entities, no service/repository restructuring. Follows established MVVM + Clean Architecture pattern with Compose UI.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Status**: ✅ No violations - This section intentionally left empty. All changes comply with project standards and existing architecture patterns.
