# Implementation Plan: Core Ride Recording

**Branch**: `002-core-ride-recording` | **Date**: 2025-11-04 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-core-ride-recording/spec.md`

## Summary

Implement GPS-based ride recording with Room database persistence, foreground service for background tracking, and manual/auto-pause functionality. Users can start/stop recording, view live statistics (duration, distance, speeds), pause/resume rides, and review completed rides. Recording survives screen-off and app backgrounding via Android foreground service. All data stored locally in Room database with one-to-many relationship (Ride → TrackPoints). Integrates with existing F2A settings for units, GPS accuracy, and auto-pause configuration.

**Technical Approach**: LifecycleService with FusedLocationProviderClient for GPS tracking, StateFlow for service-ViewModel communication, Room 2.6.1 with KSP for persistence, FLAG_KEEP_SCREEN_ON for screen wake lock, Material 3 UI components with bottom action bar for pause/resume controls.

---

## Technical Context

**Language/Version**: Kotlin 2.0.21
**Primary Dependencies**: Room 2.6.1, Play Services Location 21.3.0, Jetpack Compose BOM 2024.11.00, Kotlin Coroutines 1.9.0
**Storage**: Room database (SQLite), local device only, no cloud sync in v0.3.0
**Testing**: JUnit 4, MockK 1.13.13, Turbine 1.2.0, Truth 1.4.4, Compose UI Test
**Target Platform**: Android 14+ (API 34+), Pixel 9 Pro Emulator for testing
**Project Type**: Mobile (Android)
**Performance Goals**: <100ms GPS processing, 60fps UI, <10% battery/hour, 90%+ test coverage
**Constraints**: Offline-capable, battery-efficient, survives backgrounding, foreground service required
**Scale/Scope**: Single ride at a time, 3600 TrackPoints/hour (High Accuracy mode), 5-7 day implementation

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Architecture Requirements ✅

- **Clean Architecture**: UI → ViewModel → Domain → Data separation enforced
- **MVVM Pattern**: StateFlow-based reactive UI state management
- **Manual DI**: Hilt deferred per Constitution exception for v0.3.0
- **Repository Pattern**: Interfaces in domain, implementations in data layer

### Testing Requirements ✅

- **90%+ Test Coverage**: Safety-critical feature (GPS tracking during cycling) requires extensive testing per Constitution
- **Unit Tests**: ViewModels, UseCases, Repositories all unit tested
- **Instrumented Tests**: Service lifecycle, Room DAOs, UI interactions
- **Emulator Testing**: Mandatory validation before merge (GPX route simulation)

### Accessibility Requirements ✅

- **48dp Touch Targets**: All buttons meet minimum size (Pause/Resume: 48dp × 48dp, FAB: 56dp)
- **TalkBack Support**: ContentDescriptions for all interactive elements
- **Color Contrast**: WCAG AA compliance via Material 3 Dynamic Color
- **Dark Mode**: Full support via Material 3 theming

### Technology Requirements ✅

- **Jetpack Compose**: All new UI (no XML layouts per CLAUDE.md)
- **Material 3 Expressive**: Segmented buttons, filled icon buttons, tonal buttons
- **StateFlow over LiveData**: Modern Android standard (2025)
- **Room with KSP**: Faster than KAPT, project already configured
- **DataStore Integration**: Respects F2A settings (units, GPS accuracy, auto-pause)

### Documentation Requirements ✅

- **Specification**: spec.md complete with 6 user stories, 28 functional requirements, 12 success criteria
- **Research**: research.md complete with 4 parallel research tasks
- **Data Model**: data-model.md complete with Room schema
- **Contracts**: contracts/repository-contracts.md complete
- **Quickstart**: quickstart.md complete with 5-phase implementation plan

**GATE STATUS**: ✅ **PASSED** - All requirements met, no violations

---

## Project Structure

### Documentation (this feature)

```text
specs/002-core-ride-recording/
├── spec.md              # Feature specification (6 user stories, 28 FRs, 12 SCs)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (foreground service, Room, wake lock, manual pause)
├── data-model.md        # Phase 1 output (Ride + TrackPoint entities)
├── quickstart.md        # Phase 1 output (5-phase implementation checklist)
├── contracts/           # Phase 1 output
│   └── repository-contracts.md  # Repository interfaces and service contracts
└── checklists/
    └── requirements.md  # Quality validation checklist
```

### Source Code (repository root)

```text
app/src/main/java/com/example/bikeredlights/
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── Ride.kt                  # NEW: Room entity
│   │   │   └── TrackPoint.kt            # NEW: Room entity
│   │   ├── dao/
│   │   │   ├── RideDao.kt               # NEW: Room DAO
│   │   │   └── TrackPointDao.kt         # NEW: Room DAO
│   │   └── BikeRedlightsDatabase.kt     # NEW: Room database singleton
│   └── repository/
│       ├── RideRepositoryImpl.kt        # NEW: Ride data access
│       ├── TrackPointRepositoryImpl.kt  # NEW: TrackPoint data access
│       ├── LocationRepositoryImpl.kt    # NEW: GPS location access
│       └── RideRecordingStateRepositoryImpl.kt  # NEW: Runtime state sharing
├── domain/
│   ├── model/
│   │   ├── Ride.kt                      # NEW: Domain model (separate from entity)
│   │   ├── TrackPoint.kt                # NEW: Domain model
│   │   └── RideRecordingState.kt        # NEW: Runtime state model
│   ├── repository/
│   │   ├── RideRepository.kt            # NEW: Interface
│   │   ├── TrackPointRepository.kt      # NEW: Interface
│   │   ├── LocationRepository.kt        # NEW: Interface
│   │   └── RideRecordingStateRepository.kt  # NEW: Interface
│   └── usecase/
│       ├── StartRideUseCase.kt          # NEW: Business logic
│       ├── FinishRideUseCase.kt         # NEW: Business logic
│       ├── RecordTrackPointUseCase.kt   # NEW: Business logic
│       └── CalculateDistanceUseCase.kt  # NEW: Haversine formula
├── service/
│   └── RideRecordingService.kt          # NEW: Foreground service with GPS tracking
├── ui/
│   ├── screens/
│   │   └── ride/
│   │       ├── LiveRideScreen.kt        # UPDATED: Add recording UI
│   │       ├── RideReviewScreen.kt      # NEW: Post-ride statistics
│   │       └── RecoveryDialog.kt        # NEW: Incomplete ride recovery
│   ├── components/
│   │   └── ride/
│   │       ├── RideStatistics.kt        # NEW: Stats display
│   │       ├── RideControls.kt          # NEW: Pause/Resume/Stop buttons
│   │       ├── SaveRideDialog.kt        # NEW: Save/discard dialog
│   │       └── KeepScreenOn.kt          # NEW: Wake lock composable
│   ├── viewmodel/
│   │   └── RideRecordingViewModel.kt    # NEW: State management
│   └── navigation/
│       └── AppNavigation.kt             # UPDATED: Add Review screen route
└── MainActivity.kt                      # UPDATED: Database initialization

app/src/test/java/  # Unit tests
├── data/repository/
│   ├── RideRepositoryImplTest.kt        # NEW
│   └── LocationRepositoryImplTest.kt    # NEW
├── domain/usecase/
│   ├── StartRideUseCaseTest.kt          # NEW
│   └── CalculateDistanceUseCaseTest.kt  # NEW
└── ui/viewmodel/
    └── RideRecordingViewModelTest.kt    # NEW

app/src/androidTest/java/  # Instrumented tests
├── data/local/
│   ├── RideDaoTest.kt                   # NEW: Room DAO tests
│   └── TrackPointDaoTest.kt             # NEW
├── service/
│   └── RideRecordingServiceTest.kt      # NEW: Service lifecycle tests
└── ui/screens/
    ├── LiveRideScreenTest.kt            # NEW: Compose UI tests
    └── RideReviewScreenTest.kt          # NEW

app/src/main/AndroidManifest.xml         # UPDATED: Service + permissions
app/build.gradle.kts                     # No changes (Room already configured)
```

**Structure Decision**: Android Clean Architecture with MVVM. Follows existing project structure from v0.1.0 (Speed Tracking) and v0.2.0 (Settings). New directories: `data/local/` for Room entities/DAOs, `service/` for foreground service, `ui/screens/ride/` for ride-specific screens.

---

## Complexity Tracking

> **No violations** - All Constitution requirements satisfied without exceptions.

---

## Phase Breakdown

### Phase 0: Research ✅ COMPLETE

**Duration**: 1 hour (parallel research agents)

**Deliverables**:
- [x] Foreground service architecture research (LifecycleService, StateFlow, notification)
- [x] Room database architecture research (one-to-many, cascade delete, KSP)
- [x] Wake lock management research (FLAG_KEEP_SCREEN_ON, DisposableEffect)
- [x] Manual pause/resume patterns research (bottom action bar, dual time tracking)
- [x] Consolidated findings in `research.md`

---

### Phase 1: Design & Contracts ✅ COMPLETE

**Duration**: 1 hour

**Deliverables**:
- [x] Data model defined (`data-model.md`)
  - Ride entity with 11 fields
  - TrackPoint entity with 9 fields
  - Foreign key with CASCADE delete
  - Validation rules documented
- [x] Repository contracts defined (`contracts/repository-contracts.md`)
  - 4 repository interfaces
  - Service action constants
  - Error handling patterns
- [x] Quickstart guide created (`quickstart.md`)
  - 5-phase implementation checklist
  - Testing strategy
  - Common pitfalls & solutions
- [x] Agent context updated (next step)

---

### Phase 2: Implementation (NOT STARTED - use /speckit.tasks)

**Duration**: 5-7 days

**Approach**: Generate detailed task breakdown with `/speckit.tasks` command. Tasks will cover:
1. Database foundation (Room entities, DAOs, migrations)
2. Repository layer (implementations with error handling)
3. Foreground service (GPS tracking, notifications, lifecycle)
4. State management (ViewModels, StateFlow, use cases)
5. UI implementation (Live screen, Review screen, controls, dialogs)
6. Integration & polish (settings, navigation, edge cases, testing)

**Testing Requirements**:
- 90%+ unit test coverage for ViewModels, UseCases, Repositories
- Instrumented tests for Room DAOs and service lifecycle
- Comprehensive emulator testing with GPX route simulation

---

## Key Technical Decisions Summary

| Area | Decision | Alternatives Rejected |
|------|----------|----------------------|
| Service Type | LifecycleService | Plain Service (no lifecycle coroutines) |
| Communication | StateFlow Repository | LiveData (deprecated), Bound Service (doesn't survive backgrounding) |
| Database | Room 2.6.1 + KSP | Embedded relationship (memory), JSON storage (non-queryable) |
| Location API | FusedLocationProviderClient | PRIORITY_BALANCED (insufficient accuracy) |
| Wake Lock | FLAG_KEEP_SCREEN_ON | PowerManager.WakeLock (requires permission, complex lifecycle) |
| Manual Pause UI | Bottom action bar, separate buttons | Single toggle (accidental stops), disabled states (confusing) |
| GPS During Pause | STOP (manual), CONTINUE (auto) | Always continue (wastes battery) |
| Time Tracking | Dual (elapsed + moving) | Single duration (inaccurate average speed) |

---

## Risk Analysis

### High Risk: Service Process Death

**Risk**: Android may kill foreground service on low memory or aggressive OEMs
**Mitigation**:
- Use START_STICKY for automatic restart
- Persist ride data continuously to Room
- Check for incomplete rides on app launch
- Offer recovery dialog to user
- Document battery optimization settings for users

**Likelihood**: Medium | **Impact**: High | **Priority**: P1

---

### Medium Risk: Battery Drain

**Risk**: High Accuracy GPS (1s interval) drains battery quickly
**Mitigation**:
- Offer Battery Saver mode (4s interval) in settings
- Stop GPS during manual pause
- Display battery warning if < 15% and High Accuracy enabled
- Meet spec target: < 10%/hour

**Likelihood**: Medium | **Impact**: Medium | **Priority**: P2

---

### Medium Risk: GPS Signal Loss

**Risk**: User enters tunnel/building, losing GPS during ride
**Mitigation**:
- Don't crash - continue recording
- Skip TrackPoints with accuracy > 50m
- Show "GPS Signal Lost" indicator on UI
- Gap will appear in future route visualization (acceptable)

**Likelihood**: High | **Impact**: Low | **Priority**: P2

---

### Low Risk: Storage Full

**Risk**: Device storage full, cannot insert TrackPoints
**Mitigation**:
- Catch SQLiteFullException
- Stop recording gracefully
- Show notification: "Storage full - recording stopped"
- Save ride data captured so far

**Likelihood**: Low | **Impact**: Medium | **Priority**: P3

---

## Success Metrics (From Spec SC-001 to SC-012)

- [SC-001] Start ride within 10 seconds of opening app
- [SC-002] Recording survives 5+ minutes screen-locked
- [SC-003] Statistics accurate within 5% vs known routes
- [SC-004] Units conversion accurate within 1%
- [SC-005] Auto-pause triggers within 10 seconds of threshold
- [SC-006] Service survives 30+ minute backgrounded rides
- [SC-007] 90%+ test coverage (ViewModels/UseCases/Repos)
- [SC-008] Battery drain < 10%/hour (High Accuracy GPS)
- [SC-009] Database writes < 100ms (non-blocking UI)
- [SC-010] Stop button responds < 1 second
- [SC-011] GPS signal loss handled gracefully (no crashes)
- [SC-012] Incomplete ride recovery works correctly

---

## Next Steps

1. **Update Agent Context**: Run `.specify/scripts/bash/update-agent-context.sh claude` to add Room database technology
2. **Commit Planning Artifacts**: Commit research.md, data-model.md, contracts/, quickstart.md, plan.md
3. **Generate Tasks**: Run `/speckit.tasks` to create detailed task breakdown (tasks.md)
4. **Begin Implementation**: Start Phase 0 (Database Foundation) per quickstart.md

---

**Planning Phase Complete**: All design artifacts generated. Ready for task generation and implementation.
