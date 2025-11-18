# Implementation Plan: Stop Detection & Recording

**Branch**: `009-stop-detection` | **Date**: 2025-11-18 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/009-stop-detection/spec.md`

## Summary

Implement real-time stop detection during active rides by monitoring GPS speed against configurable thresholds (speed < 3 km/h, duration > 15s). Detect stops, display live UI feedback popup, persist stop data to Room database, and integrate with settings from Feature 008. This provides the foundational data collection layer for future clustering (Feature 010) and red light location inference.

**Technical Approach**: Extend existing ride recording infrastructure (Feature 002-007) by adding stop detection logic to TrackLocationUseCase, creating new Stop entity in Room database with foreign key to Ride, displaying semi-transparent stop popup composable on Live tab, and reading threshold settings from DataStore via SettingsRepository.

## Technical Context

**Language/Version**: Kotlin 2.0.21, Java 17 (OpenJDK)
**Primary Dependencies**:
- Jetpack Compose BOM 2024.11.00 (UI layer)
- Room 2.6.1 (stop persistence)
- Hilt 2.51.1 (dependency injection)
- Play Services Location 21.3.0 (GPS data source)
- Kotlin Coroutines 1.9.0 (async operations)
- DataStore Preferences (settings integration from Feature 008)

**Storage**: Room SQLite database - New `stops` table with foreign key to existing `rides` table (CASCADE delete)

**Testing**:
- JUnit 5 (unit tests for use cases, ViewModels, repositories)
- MockK (mocking dependencies)
- Turbine (Flow testing for stop detection state)
- Compose UI Test (stop popup composable testing)
- AndroidX Test + Espresso (instrumented tests for database operations)

**Target Platform**: Android 14+ (API 34+), targeting API 36

**Project Type**: Mobile (Android single-module app)

**Performance Goals**:
- Stop detection latency: <1 second from threshold confirmation to UI popup display
- Database insert latency: <100ms for stop record persistence
- UI update frequency: 1Hz (stop duration counter updates every second)
- Battery impact: <2% additional drain per hour compared to Feature 007 baseline
- Memory overhead: <5MB for stop detection state management (in-memory)

**Constraints**:
- Must work offline (no network required for stop detection)
- Must survive app backgrounding (foreground service keeps detection active)
- Must handle GPS signal loss gracefully (no crashes, partial data preserved)
- Must respect manual pause state (no stop detection while ride paused)
- Must use consecutive seconds filtering (3s) to avoid GPS noise false positives

**Scale/Scope**:
- Expected stop count per ride: 5-20 stops for urban commutes
- Expected database growth: ~50 stops per week per active user
- UI complexity: 1 new composable (stop popup), 1 UI state field (stop count), existing Live tab modified
- Database complexity: 1 new table (8 columns), 1 new DAO, 1 new repository
- Domain logic: Stop detection state machine in TrackLocationUseCase, 3 consecutive seconds counters (below/above threshold)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Initial Check (Before Phase 0)

**Status**: ✅ **PASSED** (No constitution file exists - defaulting to Android development standards from CLAUDE.md)

Based on CLAUDE.md project standards:

- ✅ **Kotlin-first**: All new code in Kotlin (Stop entity, StopDao, repository, use case logic)
- ✅ **MVVM + Clean Architecture**: Follows existing pattern (Domain → Data → UI layers)
- ✅ **Jetpack Compose UI**: Stop popup composable (no XML layouts)
- ✅ **Room Database**: Stop persistence using existing Room infrastructure
- ✅ **Hilt DI**: All new classes injected via Hilt
- ✅ **Material Design 3**: Stop popup follows M3 design system (semi-transparent card, typography)
- ✅ **Testing Requirements**: Unit tests for use case logic, instrumented tests for DAO operations
- ✅ **No deprecated APIs**: Using StateFlow (not LiveData), Compose (not XML), DataStore (not SharedPreferences)
- ✅ **Immutability**: Stop entity is immutable data class, domain models use `val`
- ✅ **Null Safety**: Stop nullable fields (endTimestamp, duration during active stop) explicitly handled

### Post-Design Re-Check (After Phase 1)

**Status**: ✅ **PASSED** - Design artifacts confirm compliance

**Data Model Validation**:
- ✅ Stop entity is immutable (only endTimestamp/durationSeconds updated once)
- ✅ Foreign key CASCADE delete implemented correctly (stops auto-deleted with ride)
- ✅ Database indexes created for performance (ride_id, cluster_id, start_timestamp)
- ✅ UNIQUE constraint on (ride_id, stop_number) prevents data corruption
- ✅ Domain models are technology-agnostic (Stop.kt has no Android dependencies)

**Architecture Validation**:
- ✅ StopDao contract defines clear database interface (no implementation details leak)
- ✅ StopRepository follows existing repository pattern (interface in domain, impl in data)
- ✅ Stop detection logic lives in domain layer UseCase, not ViewModel (proper separation)
- ✅ Service owns state machine (survives backgrounding), ViewModel is stateless UI coordinator

**Testing Validation**:
- ✅ Unit tests defined for state machine (pure Kotlin, no Android framework)
- ✅ Instrumented tests defined for DAO (Room testing framework)
- ✅ Test coverage includes edge cases (CASCADE delete, GPS noise, app backgrounding)

**Safety-Critical Validation**:
- ✅ **Data integrity**: CASCADE delete ensures no orphaned stops when ride deleted
- ✅ **GPS noise filtering**: 3-second consecutive threshold crossing prevents false positives (research.md Section 3)
- ✅ **Graceful degradation**: Poor GPS accuracy doesn't crash app (logged, stop detection continues)
- ✅ **State persistence**: Stop data saved immediately on confirmation (survives app kill) - verified in quickstart.md
- ✅ **Memory safety**: No Location objects stored in state (prevents Context leaks) - documented in research.md Section 6

**No violations identified** - design artifacts confirm feature aligns with existing architecture and safety requirements.

## Project Structure

### Documentation (this feature)

```text
specs/009-stop-detection/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification (already created)
├── checklists/
│   └── requirements.md  # Spec validation checklist (already created)
├── research.md          # Phase 0 output (to be created)
├── data-model.md        # Phase 1 output (to be created)
├── quickstart.md        # Phase 1 output (to be created)
├── contracts/           # Phase 1 output (to be created)
│   └── StopDao.kt       # Room DAO contract
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/bikeredlights/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── RideDao.kt                    # Existing
│   │   │   ├── TrackPointDao.kt              # Existing
│   │   │   └── StopDao.kt                    # NEW - Stop CRUD operations
│   │   ├── entity/
│   │   │   ├── RideEntity.kt                 # Existing
│   │   │   ├── TrackPointEntity.kt           # Existing
│   │   │   └── StopEntity.kt                 # NEW - Room entity for stops table
│   │   └── AppDatabase.kt                    # MODIFIED - Add stops table, version bump
│   ├── repository/
│   │   ├── RideRepository.kt                 # Existing
│   │   ├── TrackPointRepository.kt           # Existing
│   │   ├── SettingsRepository.kt             # Existing (Feature 008)
│   │   └── StopRepository.kt                 # NEW - Stop persistence operations
│   └── preferences/                          # Existing (Feature 008 settings)
│
├── domain/
│   ├── model/
│   │   ├── Ride.kt                           # Existing
│   │   ├── TrackPoint.kt                     # Existing
│   │   ├── Stop.kt                           # NEW - Domain model for Stop
│   │   └── StopDetectionState.kt             # NEW - Runtime state (not persisted)
│   ├── repository/
│   │   ├── RideRepository.kt                 # Existing interface
│   │   ├── SettingsRepository.kt             # Existing interface
│   │   └── StopRepository.kt                 # NEW - Repository interface
│   ├── usecase/
│   │   ├── TrackLocationUseCase.kt           # MODIFIED - Add stop detection logic
│   │   ├── SaveRideUseCase.kt                # Existing (no changes needed)
│   │   └── DetectStopUseCase.kt              # NEW (OPTIONAL) - Extract stop logic if TrackLocationUseCase becomes complex
│   └── util/
│       └── StopDetectionUtils.kt             # NEW - Consecutive seconds filtering, duration formatting
│
├── service/
│   └── RideRecordingService.kt               # MODIFIED - Handle stop state on pause/stop ride
│
├── ui/
│   ├── components/
│   │   └── ride/
│   │       ├── SpeedDisplay.kt               # Existing
│   │       ├── RideStatsRow.kt               # MODIFIED - Add stop count display
│   │       └── StopPopup.kt                  # NEW - Semi-transparent stop indicator composable
│   ├── screens/
│   │   └── ride/
│   │       └── LiveRideScreen.kt             # MODIFIED - Integrate StopPopup composable
│   └── viewmodel/
│       └── RideViewModel.kt                  # MODIFIED - Add stop state (count, current stop), emit stop events
│
└── di/
    └── AppModule.kt                          # MODIFIED - Provide StopRepository, bind StopDao

app/src/test/ (unit tests)
└── java/com/example/bikeredlights/
    ├── domain/usecase/
    │   └── DetectStopUseCaseTest.kt          # NEW - Test stop detection logic with various thresholds
    └── data/repository/
        └── StopRepositoryTest.kt             # NEW - Test stop CRUD operations

app/src/androidTest/ (instrumented tests)
└── java/com/example/bikeredlights/
    └── data/local/dao/
        └── StopDaoTest.kt                    # NEW - Test Room queries, CASCADE delete
```

**Structure Decision**: Android single-module architecture following MVVM + Clean Architecture pattern. New stop detection functionality integrates into existing layers:
- **Data Layer**: StopEntity, StopDao, StopRepository (persistence)
- **Domain Layer**: Stop model, StopDetectionState, detection logic in TrackLocationUseCase
- **UI Layer**: StopPopup composable, RideViewModel stop state, LiveRideScreen integration

No new modules needed - feature fits cleanly into existing structure established by Features 001-008.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Status**: N/A - No violations identified. Feature follows established patterns.
