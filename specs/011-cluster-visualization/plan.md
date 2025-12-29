# Implementation Plan: Stop Cluster Visualization

**Branch**: `011-cluster-visualization` | **Date**: 2025-12-29 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/011-cluster-visualization/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Feature 011 adds interactive map visualization for stop clusters created by Feature 010. Users can view all their stop clusters on Google Maps with color-coded markers (green/yellow/red based on cluster size), tap markers to see detailed analytics ("You stopped here 15 times this month"), and apply filters by date range or minimum cluster size. A new "Stops" tab is added to bottom navigation for easy access.

**Technical Approach**: Extends existing Google Maps integration (Feature 006) with new StopsMapScreen, ClusterMapViewModel, and bottom sheet popup. Uses Room queries to fetch clustered stops (cluster_id NOT NULL), calculates aggregate statistics in domain layer, and renders markers with Material 3 color theming. Integrates with existing BikeMap composable and navigation infrastructure.

## Technical Context

**Language/Version**: Kotlin 2.0.21 with Java 17 (OpenJDK)
**Primary Dependencies**:
- Jetpack Compose BOM 2024.11.00 (UI)
- Maps Compose 6.2.0 (map visualization, existing from Feature 006)
- Room 2.6.1 (cluster data queries)
- Hilt 2.51.1 (dependency injection)
- Coroutines 1.9.0 + Flow/StateFlow (reactive data)
- Play Services Maps 19.0.0 (underlying Maps SDK)

**Storage**: Room SQLite database (existing stops table with cluster_id column from Feature 010)

**Testing**: JUnit 5 + MockK 1.13.13 (unit tests), Compose UI Test (integration tests)

**Target Platform**: Android API 34+ (minSdk 34, targetSdk 35)

**Project Type**: Mobile (Android) - MVVM + Clean Architecture

**Performance Goals**:
- Map load time: <2 seconds to display up to 100 cluster markers
- Marker clustering: <100ms to aggregate markers at zoom changes
- Bottom sheet popup: <500ms to open and render up to 50 stop list items
- Filter application: <1 second to update map display
- Map gestures: 60fps pan/zoom responsiveness

**Constraints**:
- Network required: Google Maps tiles need internet connectivity
- GPS permissions: Required for map display (already granted in Feature 006)
- Battery efficiency: Minimize recompositions, cache cluster calculations
- Offline-first data: Cluster data from local Room database (no network calls for stop data)

**Scale/Scope**:
- 50-100 cluster markers on map (typical user after months of rides)
- Up to 500 total stops in database across all clusters
- 4-tab bottom navigation (Live, Rides, Stops, Settings)
- Single new screen: StopsMapScreen with filter controls

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### BikeRedlights Standards (from CLAUDE.md)

✅ **Architecture Pattern**: MVVM + Clean Architecture
- UI Layer: StopsMapScreen composable (stateless)
- ViewModel: ClusterMapViewModel with StateFlow
- Domain: GetClusteredStopsUseCase, CalculateClusterStatsUseCase
- Data: StopRepository queries (existing)
- **Status**: COMPLIANT - follows established pattern from Features 001-010

✅ **Technology Stack**:
- UI: Jetpack Compose (no XML layouts) ✓
- Async: Kotlin Coroutines + Flow/StateFlow ✓
- DI: Dagger Hilt ✓
- Navigation: Navigation Compose (add Stops destination) ✓
- Local DB: Room (query existing stops table) ✓
- Maps: Google Maps Compose 6.2.0 (existing) ✓
- **Status**: COMPLIANT - uses all required libraries

✅ **Material Design 3**:
- Color-coded markers (green/yellow/red using Material 3 palette)
- ModalBottomSheet for cluster details
- Dynamic color theming support
- Dark mode compatible
- Accessibility: 48dp touch targets, content descriptions
- **Status**: COMPLIANT - follows M3 Expressive guidelines

✅ **Testing Requirements** (per CLAUDE.md):
- Unit tests: ViewModel state management, use case logic
- UI tests: Compose testing for map screen, bottom sheet
- Physical device testing: Map rendering, marker interactions
- **Status**: COMPLIANT - standard testing approach

✅ **Code Review Checklist** (from CLAUDE.md):
- Kotlin coding conventions ✓
- Jetpack Compose (no XML) ✓
- MVVM architecture ✓
- State hoisting ✓
- Material 3 theming ✓
- Dark mode support ✓
- Accessibility ✓
- Emulator/device testing ✓
- **Status**: COMPLIANT - all checklist items addressed

### Gate Decision: ✅ PASS

No constitution violations. Feature follows all established patterns and standards.

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
│
├── domain/                          # Business logic layer
│   ├── model/
│   │   ├── ClusterSummary.kt       # NEW: Aggregate cluster stats
│   │   ├── ClusterMarkerData.kt    # NEW: Marker visual properties
│   │   └── StopClusterFilter.kt    # NEW: Date range + min size filter
│   │
│   ├── usecase/
│   │   ├── GetClusteredStopsUseCase.kt       # NEW: Query stops with cluster_id
│   │   ├── CalculateClusterStatsUseCase.kt   # NEW: Aggregate stats per cluster
│   │   ├── FormatClusterAnalyticsUseCase.kt  # NEW: "15 times this month"
│   │   └── CalculateClusterCenterUseCase.kt  # NEW: Average GPS coords
│   │
│   └── repository/
│       └── StopRepository.kt        # MODIFIED: Add clustered stops queries
│
├── data/                            # Data access layer
│   ├── repository/
│   │   └── StopRepositoryImpl.kt   # MODIFIED: Implement cluster queries
│   │
│   └── local/dao/
│       └── StopDao.kt               # MODIFIED: Add cluster query methods
│
├── ui/                              # Presentation layer
│   ├── screens/
│   │   └── clusters/
│   │       ├── StopsMapScreen.kt            # NEW: Main map screen
│   │       └── ClusterDetailBottomSheet.kt  # NEW: Popup with stop list
│   │
│   ├── components/
│   │   └── clusters/
│   │       ├── ClusterMarker.kt             # NEW: Color-coded marker
│   │       ├── ClusterFilterControls.kt     # NEW: Date + size filters
│   │       └── StopListItem.kt              # NEW: Individual stop in list
│   │
│   ├── viewmodel/
│   │   └── ClusterMapViewModel.kt  # NEW: Map state + filter logic
│   │
│   └── navigation/
│       ├── AppNavigation.kt        # MODIFIED: Add Stops destination
│       └── BottomNavDestination.kt # MODIFIED: Add STOPS enum value
│
└── di/
    └── AppModule.kt                 # MODIFIED: Provide new use cases
```

```text
app/src/test/java/com/example/bikeredlights/
├── domain/usecase/
│   ├── GetClusteredStopsUseCaseTest.kt       # NEW
│   ├── CalculateClusterStatsUseCaseTest.kt   # NEW
│   └── FormatClusterAnalyticsUseCaseTest.kt  # NEW
│
└── ui/viewmodel/
    └── ClusterMapViewModelTest.kt   # NEW
```

```text
app/src/androidTest/java/com/example/bikeredlights/
└── ui/screens/
    └── StopsMapScreenTest.kt        # NEW: Compose UI tests
```

**Structure Decision**: Android mobile app following established MVVM + Clean Architecture pattern. New feature adds:
- **Domain layer**: 4 use cases + 3 models for cluster logic
- **Data layer**: Extended StopRepository/Dao with cluster queries
- **UI layer**: 1 screen, 1 bottom sheet, 3 components, 1 ViewModel
- **Navigation**: Modified AppNavigation + BottomNavDestination
- **Tests**: 4 unit test files + 1 UI test file

All code follows existing BikeRedlights structure from Features 001-010.

## Complexity Tracking

No constitution violations to justify. Feature uses established patterns and libraries from previous features.

---

## Planning Completion Status

**Phase 0: Outline & Research** ✅ COMPLETE
- Research findings documented in `research.md`
- All technical unknowns resolved:
  - Marker clustering: Manual aggregation (no runtime clustering library)
  - Bottom sheet: Material 3 ModalBottomSheet with conditional composition
  - 4-tab navigation: Fully supported, icon-only mode for inactive tabs
  - Cluster center: Arithmetic mean of GPS coordinates

**Phase 1: Design & Contracts** ✅ COMPLETE
- Data model documented in `data-model.md`
- Use case contracts documented in `contracts/use-cases.md`
- Repository contracts documented in `contracts/repository.md`
- Developer quickstart guide created in `quickstart.md`
- Agent context updated (CLAUDE.md)

**Phase 2: Task Breakdown** ⏭️ NEXT
- Run `/speckit.tasks` to generate tasks.md

**Constitution Check (Post-Design)**: ✅ PASS
- All decisions align with BikeRedlights architecture standards
- Material Design 3 compliance confirmed
- Performance targets achievable with documented patterns
- No new dependencies required (all libraries already in project)

---

## Planning Artifacts Summary

| Artifact | Location | Status |
|----------|----------|--------|
| Implementation Plan | `plan.md` (this file) | ✅ Complete |
| Research Findings | `research.md` | ✅ Complete |
| Data Model | `data-model.md` | ✅ Complete |
| Use Case Contracts | `contracts/use-cases.md` | ✅ Complete |
| Repository Contracts | `contracts/repository.md` | ✅ Complete |
| Developer Quickstart | `quickstart.md` | ✅ Complete |
| Requirements Checklist | `checklists/requirements.md` | ✅ Complete (from /speckit.specify) |
| Agent Context | `CLAUDE.md` | ✅ Updated |
| Task Breakdown | `tasks.md` | ⏭️ Pending (/speckit.tasks) |

**Ready for**: `/speckit.tasks` command to generate implementation task breakdown
