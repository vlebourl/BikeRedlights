# Implementation Plan: Ride History and List View

**Branch**: `003-ride-history-list` | **Date**: 2025-11-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-ride-history-list/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement a comprehensive ride history feature that allows users to view all previously recorded rides in a scrollable list, drill down into detailed ride statistics, sort/filter rides by various criteria, and delete unwanted rides. The feature leverages existing Ride and TrackPoint entities from F1A (v0.3.0) stored in Room database, adds new History tab to bottom navigation, and implements sort/filter preferences using DataStore. Technical approach follows Clean Architecture (MVVM) with Jetpack Compose UI, reactive Flow-based data access, and Material 3 design patterns.

## Technical Context

**Language/Version**: Kotlin 2.0.21, Java 17 (OpenJDK)
**Primary Dependencies**:
- Jetpack Compose BOM 2024.11.00 (Material 3 UI)
- Room 2.6.1 (local database)
- Hilt (dependency injection)
- Kotlin Coroutines 1.9.0 + Flow (async/reactive)
- DataStore Preferences (settings persistence)
- Lifecycle ViewModel Compose (state management)

**Storage**: Room database (existing rides/track_points tables from v0.3.0) + DataStore Preferences (sort/filter settings)
**Testing**: JUnit 5, MockK, Turbine (Flow testing), Compose UI Test, AndroidX Test
**Target Platform**: Android 14+ (API 34+), minSdk 34, targetSdk 35, compileSdk 35
**Project Type**: Mobile (Android single-module app)
**Performance Goals**:
- List load < 1s with 100+ rides
- 60fps scrolling
- Sort/filter operations < 500ms
- Navigation transitions < 500ms

**Constraints**:
- Offline-first (all data local, no sync in v0.4.0)
- Battery efficient (no background processing for this feature)
- Must work seamlessly with existing ride recording (F1A)
- Reactive UI updates when rides deleted/modified

**Scale/Scope**:
- Support 1000+ rides without performance degradation
- 5 user stories (P1-P4 priorities)
- 3 new composable screens (History List, Ride Detail, Filter/Sort dialogs)
- 2 new ViewModels
- 3 new use cases
- 0 new repository implementations (reuse existing RideRepository from F1A)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Based on CLAUDE.md (project development standards):

### ✅ Architecture & Code Quality
- **MVVM + Clean Architecture**: Feature follows established pattern (UI → ViewModel → UseCase → Repository → Data)
- **Kotlin-first**: All code in Kotlin with null safety, immutability preferences
- **Jetpack Compose**: All new UI uses Compose (no XML layouts)
- **Separation of Concerns**: Clear layer boundaries maintained
- **Naming Conventions**: PascalCase classes, camelCase functions/vars, ALL_CAPS constants

### ✅ Testing Requirements
- **80%+ Test Coverage**: Mandatory for ViewModels, UseCases, Repositories
- **Test Types**: Unit (ViewModels/UseCases), Integration (Repository+Room), UI (Compose tests)
- **Emulator Testing**: All features must be tested on Android emulator before merge
- **Test Framework**: JUnit, MockK, Turbine, Compose UI Test

### ✅ UI/UX Standards (Material 3 Expressive)
- **Material 3 Components**: Use M3 components (Card, List, TopAppBar, Dialog)
- **Dynamic Color**: Support user wallpaper-based theming
- **Dark Mode**: Both light and dark theme support required
- **Accessibility**: 48dp touch targets, content descriptions, WCAG AA contrast
- **Motion**: M3 motion physics for transitions and animations

### ✅ Performance & Quality
- **Efficient Lists**: LazyColumn for ride list (render only visible items)
- **Database Best Practices**: Flow-based reactive queries, background threading
- **Memory Management**: No memory leaks (check ViewModels don't hold Context)
- **ProGuard/R8**: Enabled for release builds

### ✅ Development Workflow
- **Small Commits**: Commit after each logical unit (~200 lines max per commit)
- **Commit Format**: Conventional commits (feat/fix/refactor/test/docs)
- **Push Frequently**: After 2-5 commits or end of work session
- **Documentation**: Update TODO.md and RELEASE.md automatically

### ✅ Release Pattern
- **Feature → PR → Review → Merge → Release**: Every feature ends with versioned release
- **Semantic Versioning**: This feature = v0.4.0 (MINOR version bump - new feature)
- **Version Code**: 0*10000 + 4*100 + 0 = 400
- **Release Assets**: Signed APK + GitHub Release with notes

### 🟡 No Constitution Violations Requiring Justification

All aspects of this feature align with established project standards. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/003-ride-history-list/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (UI patterns, list best practices)
├── data-model.md        # Phase 1 output (display models, preferences)
├── quickstart.md        # Phase 1 output (local dev guide)
├── contracts/           # Phase 1 output (not applicable - no external APIs)
├── checklists/
│   └── requirements.md  # Spec validation (already created)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/bikeredlights/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   └── RideDao.kt              # [EXISTING] Already has getAllRides, getRideById methods
│   │   └── entity/
│   │       ├── Ride.kt                 # [EXISTING] From F1A v0.3.0
│   │       └── TrackPoint.kt           # [EXISTING] From F1A v0.3.0
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt # [EXTEND] Add sort/filter preferences
│   └── repository/
│       └── RideRepositoryImpl.kt       # [EXISTING] Already provides getAllRides, deleteRide
│
├── domain/
│   ├── model/
│   │   ├── Ride.kt                     # [EXISTING] Domain model
│   │   ├── settings/
│   │   │   ├── UnitPreference.kt       # [EXISTING] From F2A
│   │   │   ├── SortPreference.kt       # [NEW] Enum for sort options
│   │   │   └── DateRangeFilter.kt      # [NEW] Sealed class for filter options
│   │   └── display/                    # [NEW] Directory for display models
│   │       ├── RideListItem.kt         # [NEW] Display model for list view
│   │       └── RideDetailData.kt       # [NEW] Display model for detail screen
│   ├── repository/
│   │   ├── RideRepository.kt           # [EXISTING] Interface already defines methods
│   │   └── UserPreferencesRepository.kt # [EXTEND] Add sort/filter methods
│   ├── usecase/
│   │   ├── GetAllRidesUseCase.kt       # [NEW] Get rides with sort/filter applied
│   │   ├── GetRideByIdUseCase.kt       # [NEW] Get single ride for detail screen
│   │   ├── DeleteRideUseCase.kt        # [NEW] Delete ride with validation
│   │   ├── GetSortPreferenceUseCase.kt # [NEW] Get current sort setting
│   │   ├── SaveSortPreferenceUseCase.kt # [NEW] Save sort setting
│   │   ├── GetDateFilterUseCase.kt     # [NEW] Get current filter setting
│   │   └── SaveDateFilterUseCase.kt    # [NEW] Save filter setting
│   └── util/
│       └── FormatUtils.kt              # [EXTEND] Add duration/date formatting helpers
│
├── ui/
│   ├── components/
│   │   └── history/                    # [NEW] Directory for history components
│   │       ├── RideListItemCard.kt     # [NEW] Composable for ride list item
│   │       ├── EmptyStateCard.kt       # [NEW] Empty state when no rides
│   │       ├── SortMenuDialog.kt       # [NEW] Sort options dropdown/dialog
│   │       ├── FilterDialog.kt         # [NEW] Date range filter dialog
│   │       └── DeleteConfirmDialog.kt  # [NEW] Delete confirmation dialog
│   ├── screens/
│   │   └── history/                    # [NEW] Directory for history screens
│   │       ├── RideHistoryScreen.kt    # [NEW] Main history list screen
│   │       └── RideDetailScreen.kt     # [NEW] Detail screen for single ride
│   ├── viewmodel/
│   │   ├── RideHistoryViewModel.kt     # [NEW] ViewModel for history list
│   │   └── RideDetailViewModel.kt      # [NEW] ViewModel for detail screen
│   ├── navigation/
│   │   └── AppNavigation.kt            # [EXTEND] Add history routes
│   └── theme/                          # [EXISTING] Material 3 theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── di/
│   └── AppModule.kt                    # [EXTEND] Provide new use cases
│
└── MainActivity.kt                     # [EXTEND] Add History tab to bottom nav

app/src/test/java/com/example/bikeredlights/
├── domain/usecase/
│   ├── GetAllRidesUseCaseTest.kt       # [NEW] Unit tests for use case
│   ├── GetRideByIdUseCaseTest.kt       # [NEW] Unit tests
│   └── DeleteRideUseCaseTest.kt        # [NEW] Unit tests
├── ui/viewmodel/
│   ├── RideHistoryViewModelTest.kt     # [NEW] ViewModel tests with MockK
│   └── RideDetailViewModelTest.kt      # [NEW] ViewModel tests
└── data/repository/
    └── RideRepositoryImplTest.kt       # [EXTEND] Add tests for filter/sort

app/src/androidTest/java/com/example/bikeredlights/
└── ui/screens/history/
    ├── RideHistoryScreenTest.kt        # [NEW] Compose UI tests
    └── RideDetailScreenTest.kt         # [NEW] Compose UI tests
```

**Structure Decision**: This is a mobile Android application following Clean Architecture principles with MVVM pattern. The structure above reflects the established architecture from F1A (v0.3.0) and F2A (v0.2.0). Key decisions:

1. **Reuse Existing Data Layer**: Ride and TrackPoint entities already exist in Room database. RideRepository already provides getAllRides() and deleteRide() methods. No new database schema changes needed.

2. **Extend Preferences**: UserPreferencesRepository already exists for unit preferences (Metric/Imperial). Will add sort and filter preferences to same repository using DataStore.

3. **New Domain Logic**: Create dedicated use cases for ride history operations (get all, get by ID, delete, sort/filter management). This keeps ViewModels thin and testable.

4. **New UI Layer**: History screens are independent from existing Live and Settings screens. Create new directory under ui/screens/history/ with dedicated ViewModels.

5. **Shared Components**: Leverage existing theme, navigation patterns, and Material 3 components from prior features.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations - this section is not applicable.*

