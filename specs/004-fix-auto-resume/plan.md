# Implementation Plan: Fix Auto-Resume After Auto-Pause

**Branch**: `004-fix-auto-resume` | **Date**: 2025-11-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-fix-auto-resume/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

**Primary Requirement**: Fix critical bug where auto-resume does not trigger after auto-pause, forcing cyclists to manually interact with their phone while riding (safety hazard).

**Root Cause**: Auto-resume logic (RideRecordingService.kt:530-565) is structurally unreachable during AutoPaused state due to a conditional gate that prevents `updateRideDistance()` from being called when `isAutoPaused=true`.

**Technical Approach**: Extract auto-resume detection logic into new `checkAutoResume()` function and execute it BEFORE the pause gate in the location update flow. This ensures auto-resume logic runs during AutoPaused state while preserving existing distance calculation behavior.

**Estimated Effort**: 6.5-9.5 hours (including 5 real bike rides for physical device validation)

## Technical Context

**Language/Version**: Kotlin 2.0.21 + Java 17 (OpenJDK 17)
**Primary Dependencies**: Jetpack Compose BOM 2024.11.00, Room 2.6.1, Hilt 2.51.1, Coroutines 1.9.0, Play Services Location 21.3.0
**Storage**: Room database (SQLite) - Ride + TrackPoint entities with pause state flags (no schema changes required)
**Testing**: JUnit 4.13.2, MockK 1.13.13, Turbine 1.2.0 (Flow testing), Truth 1.4.4, AndroidX Test, Compose UI Test
**Target Platform**: Android 14+ (API 34 min, API 35 target/compile)
**Project Type**: Mobile (Android) - Native app with foreground service for GPS tracking
**Performance Goals**: Auto-resume latency <2s (High Accuracy GPS) / <8s (Battery Saver GPS); 60fps UI animations; minimal battery impact
**Constraints**: Must work during service backgrounding, survive process death, handle poor GPS signal (<50m accuracy threshold)
**Scale/Scope**: Single-file bug fix (~50 LOC changes in RideRecordingService.kt) + 6 new test cases (~200 LOC); no UI changes; no new dependencies

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Phase 0: Pre-Research Gates ✅ **PASSED**

| Gate | Requirement | Status | Notes |
|------|-------------|--------|-------|
| **MVVM + Clean Architecture** | Follow 4-layer architecture (UI → ViewModel → Domain → Data) | ✅ Pass | Bug fix in Service layer only; no new layers introduced |
| **Kotlin-first** | All new code in Kotlin | ✅ Pass | Service layer is Kotlin; fix uses Kotlin coroutines |
| **Technology Stack** | Use approved libraries (Compose, Room, Hilt, Coroutines, Location API) | ✅ Pass | No new dependencies; uses existing tech stack |
| **No Deprecated APIs** | Avoid XML layouts, LiveData, SharedPreferences, AsyncTask | ✅ Pass | No UI changes; uses StateFlow/Flow; no deprecated APIs |
| **Testability** | Each layer testable in isolation | ✅ Pass | Service can be tested via instrumented tests; logic is mockable |

### Phase 1: Post-Design Gates (Re-check after implementation plan complete)

| Gate | Requirement | Status | Notes |
|------|-------------|--------|-------|
| **Separation of Concerns** | Each layer has single responsibility | ✅ Pass | `checkAutoResume()` handles state transition only; distance calculation remains separate |
| **Unidirectional Data Flow** | State flows down, events flow up | ✅ Pass | Service updates state → Repository → ViewModel → UI (existing flow preserved) |
| **Testing Requirements** | 80%+ unit test coverage for logic | ✅ Pass | 6 new test cases cover auto-resume scenarios; regression tests for manual pause/resume |
| **Emulator Testing** | Feature tested on emulator before merge | ⏳ Pending | Required: GPS simulation + physical device testing (5 rides) |
| **Documentation** | TODO.md and RELEASE.md auto-updated | ⏳ Pending | Will update during implementation (constitution requirement) |
| **Commit Frequency** | Small, logical commits (max ~200 LOC) | ⏳ Pending | Plan: 1 commit for fix, 1 for tests, 1 for docs |

**Summary**: All pre-research gates passed. No constitution violations. Post-implementation gates will be validated during development.

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
app/
├── src/
│   ├── main/java/com/example/bikeredlights/
│   │   ├── service/
│   │   │   └── RideRecordingService.kt        # PRIMARY FILE TO MODIFY
│   │   ├── data/repository/
│   │   │   └── RideRecordingStateRepositoryImpl.kt  # Used by service
│   │   ├── domain/model/
│   │   │   ├── settings/AutoPauseConfig.kt    # Config model (read-only)
│   │   │   └── RideRecordingState.kt          # State sealed class
│   │   └── di/
│   │       └── AppModule.kt                   # Hilt module (no changes)
│   └── androidTest/java/com/example/bikeredlights/
│       └── service/
│           └── RideRecordingServiceTest.kt    # NEW TEST CASES (6 tests)
│
build.gradle.kts                               # No changes
```

**Structure Decision**: Android mobile app (Option 3 - Mobile). Bug fix isolated to Service layer with corresponding instrumented tests. No UI changes, no new dependencies, no new modules. Clean Architecture preserved: Service layer handles state transitions, Repository layer persists state, ViewModel layer observes state changes.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**No violations detected.** All constitution requirements satisfied.
