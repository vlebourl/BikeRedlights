# Implementation Plan: Fix Live Current Speed Display Bug

**Branch**: `005-fix-live-speed` | **Date**: 2025-11-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-fix-live-speed/spec.md`

## Summary

Fix critical bug where current speed displays hardcoded 0.0 instead of real-time GPS speed on Live tab during rides. Technical approach involves creating reactive state flow from Service → Repository → ViewModel → UI to propagate current speed updates captured from GPS location callbacks.

## Technical Context

**Language/Version**: Kotlin 2.0.21, Java 17 (OpenJDK)
**Primary Dependencies**: Jetpack Compose BOM 2024.11.00, Hilt 2.51.1, Kotlin Coroutines 1.9.0, Play Services Location 21.3.0, Room 2.6.1
**Storage**: DataStore Preferences (state storage), Room SQLite (ride persistence)
**Testing**: JUnit 5, MockK, Turbine (Flow testing), Compose UI Test
**Target Platform**: Android 14+ (API 34+), minSdk 26
**Project Type**: Mobile (Android native)
**Performance Goals**: Speed updates within 1-4 seconds (based on GPS accuracy), 60fps UI rendering
**Constraints**: Battery-efficient location updates, offline-capable, survives configuration changes
**Scale/Scope**: Single screen modification (LiveRideScreen), 4-layer architecture change (Service → Repository → ViewModel → UI)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### BikeRedlights Constitution Compliance (from CLAUDE.md)

**Architecture Pattern**: ✅ MVVM + Clean Architecture (UI → ViewModel → Domain → Data)
- **Compliant**: This bug fix follows existing architecture pattern
- **Rationale**: Service captures GPS → Repository stores state → ViewModel exposes StateFlow → UI collects

**Dependency Injection**: ✅ Hilt DI required for all components
- **Compliant**: Existing repositories and ViewModels use Hilt
- **Rationale**: No new DI patterns needed, use existing `@Inject` constructors

**State Management**: ✅ StateFlow/Flow for reactive UI updates
- **Compliant**: Current speed will use StateFlow for reactivity
- **Rationale**: Matches existing pattern (other ride metrics use StateFlow)

**Testing Requirements**: ✅ Unit tests for non-UI layers, Compose tests for UI
- **Compliant**: Will add unit tests for Repository/ViewModel, UI tests for LiveRideScreen
- **Rationale**: 80%+ coverage target for ViewModels/UseCases/Repositories

**Material Design 3**: ✅ All UI components must use Material 3
- **Compliant**: RideStatistics component already exists and is Material 3 compliant
- **Rationale**: No UI component changes needed, only data wiring

**Commit Frequency**: ✅ Small, regular commits (max ~200 LOC per commit)
- **Compliant**: Will commit after each layer (Service → Repository → ViewModel → UI)
- **Rationale**: 4 logical commits for 4-layer changes

**Documentation**: ✅ TODO.md and RELEASE.md must be updated automatically
- **Compliant**: Will update TODO.md (move to In Progress → Completed) and RELEASE.md (add to Unreleased)
- **Rationale**: Constitution requirement for all features

**Emulator Testing**: ✅ MANDATORY before merge
- **Compliant**: Will test on emulator with GPS simulation
- **Rationale**: Safety-critical feature requires physical validation

### Violations/Justifications

*None* - This bug fix aligns with all existing architectural patterns and constitution requirements.

## Project Structure

### Documentation (this feature)

```text
specs/005-fix-live-speed/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (architecture research)
├── data-model.md        # Phase 1 output (CurrentSpeed state model)
├── quickstart.md        # Phase 1 output (developer guide)
├── contracts/           # Phase 1 output (StateFlow contracts)
└── checklists/
    └── requirements.md  # Spec validation checklist (completed)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/bikeredlights/
├── service/
│   └── RideRecordingService.kt         # [MODIFY] Add currentSpeed to broadcast
├── data/
│   └── repository/
│       └── RideRecordingStateRepositoryImpl.kt  # [MODIFY] Add currentSpeed StateFlow
├── domain/
│   └── repository/
│       └── RideRecordingStateRepository.kt  # [MODIFY] Add currentSpeed interface
├── ui/
│   ├── viewmodel/
│   │   └── RideRecordingViewModel.kt   # [MODIFY] Expose currentSpeed StateFlow
│   ├── screens/
│   │   └── ride/
│   │       └── LiveRideScreen.kt       # [MODIFY] Collect currentSpeed and pass to component
│   └── components/
│       └── ride/
│           └── RideStatistics.kt       # [NO CHANGE] Already accepts currentSpeed param
└── BikeRedlightsApplication.kt

app/src/test/java/com/example/bikeredlights/
├── data/
│   └── repository/
│       └── RideRecordingStateRepositoryImplTest.kt  # [NEW] Test currentSpeed state
├── domain/
│   └── usecase/
│       └── [NO NEW TESTS]  # No new use cases for this bug fix
└── ui/
    └── viewmodel/
        └── RideRecordingViewModelTest.kt  # [MODIFY] Test currentSpeed exposure

app/src/androidTest/java/com/example/bikeredlights/
└── ui/
    └── screens/
        └── ride/
            └── LiveRideScreenTest.kt  # [NEW] Test currentSpeed display
```

**Structure Decision**: Android native mobile app with Clean Architecture. This bug fix modifies existing architecture layers (Service → Data → Domain → UI) without adding new components. All changes are within the ride recording feature module.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations* - This bug fix follows all existing architectural patterns and introduces no new complexity.

## Phase 0: Research & Architecture Analysis

**Status**: Completed
**Output**: [research.md](./research.md)

### Research Topics

1. **Current Architecture**: How does ride state currently flow from Service to UI?
2. **GPS Speed Capture**: Where is GPS speed already being captured?
3. **StateFlow Patterns**: What StateFlow patterns are used in existing repositories?
4. **Configuration Changes**: How does the app handle state persistence across screen rotation?
5. **Unit Conversion**: Where are km/h ↔ mph conversions implemented?

### Decisions Made

See [research.md](./research.md) for detailed findings and architectural decisions.

## Phase 1: Data Model & Contracts

**Status**: Completed
**Output**: [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

### Data Model

See [data-model.md](./data-model.md) for:
- CurrentSpeed state entity definition
- StateFlow contract between layers
- State lifecycle (creation, updates, resets)

### API Contracts

See [contracts/](./contracts/) for:
- Repository interface contract (currentSpeed: StateFlow<Double>)
- ViewModel exposure contract
- UI collection pattern

### Developer Quick Start

See [quickstart.md](./quickstart.md) for:
- How to test current speed updates locally
- GPS simulation setup
- Common pitfalls and debugging

## Phase 2: Task Breakdown

**Status**: Not Started
**Command**: Run `/speckit.tasks` to generate [tasks.md](./tasks.md)

The task breakdown will decompose this plan into atomic implementation tasks with dependencies, testing requirements, and estimated effort.
