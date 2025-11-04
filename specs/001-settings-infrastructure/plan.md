# Implementation Plan: Basic Settings Infrastructure

**Branch**: `001-settings-infrastructure` | **Date**: 2025-11-04 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-settings-infrastructure/spec.md`

**Note**: This plan follows SpecKit workflow. Research (Phase 0) resolves technical unknowns, Design (Phase 1) defines data models and contracts, and Tasks (Phase 2) breaks down implementation work.

## Summary

**Primary Requirement**: Create foundational settings infrastructure with three user-configurable preferences (Units, GPS Accuracy, Auto-Pause) persisted to local storage.

**Technical Approach**:
- Material 3 card-based Settings UI with bottom navigation integration
- DataStore Preferences for persistence (already configured in v0.1.0)
- MVVM architecture with reactive StateFlow for settings state
- Clean Architecture: UI → ViewModel → Repository → DataStore
- No external APIs, pure local device storage

**Why This Feature First**: Foundation for Feature 1A (Core Ride Recording) - provides units conversion, GPS configuration, and auto-pause logic required by ride tracking system.

## Technical Context

**Language/Version**: Kotlin 2.0.21
**Primary Dependencies**:
- Jetpack Compose BOM 2024.11.00 (Material 3 UI components)
- DataStore Preferences 1.1.1 (settings persistence)
- Kotlin Coroutines 1.9.0 + Flow (reactive state)
- Navigation Compose 2.8.5 (bottom nav + screen navigation)

**Storage**: DataStore Preferences (key-value pairs, local device only)
**Testing**:
- JUnit 5 for unit tests (SettingsRepository, conversion utilities)
- Compose UI Test for UI tests (navigation, component interactions)
- Instrumented tests for persistence verification (app restart cycles)

**Target Platform**: Android 8.0+ (API 26+), compiles with API 36
**Project Type**: Mobile (Android) - feature module structure
**Performance Goals**:
- Settings UI renders within 100ms
- Settings changes save to DataStore within 50ms
- No UI jank (60fps minimum for all interactions)

**Constraints**:
- No cloud sync (device-local only)
- Settings changes must persist across app restarts 100% of the time
- Must work offline (no network dependency)
- Accessibility: 48dp minimum touch targets, WCAG AA contrast ratios, TalkBack support

**Scale/Scope**:
- 3 settings in v0.2.0 (Units, GPS Accuracy, Auto-Pause)
- 4 DataStore keys initially
- Scalable to 10+ settings (card-based design)
- Settings accessed by all future features (1A, 2B, 3, 4)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### CLAUDE.md Android Development Standards Compliance

**✅ Architecture Pattern (MVVM + Clean Architecture)**
- Settings feature follows: UI (Compose) → ViewModel → Repository → DataStore
- No Android dependencies in domain layer (UnitsSystem, GpsAccuracy enums)
- Separation of concerns: UI for display, ViewModel for state, Repository for persistence
- **Compliance**: Full adherence

**✅ Technology Stack Requirements**
- UI: Jetpack Compose (Material 3) ✅
- Async: Kotlin Coroutines + Flow/StateFlow ✅
- DI: Manual for v0.2.0 (Hilt still disabled per v0.1.0) ✅
- Preferences: DataStore (NOT SharedPreferences) ✅
- Testing: JUnit, Compose UI Test ✅
- **Compliance**: Full adherence

**✅ UI/UX Standards (Material Design 3 Expressive)**
- Card-based settings layout (Material 3 Card component)
- SegmentedButton for mutually exclusive choices (Material 3 guideline)
- Dynamic color support (wallpaper-based theming)
- Dark mode support (Material 3 theme system)
- Accessibility: 48dp touch targets, semantic labels, contrast ratios
- **Compliance**: Full adherence

**✅ Project Structure**
- Follows established structure:
  - `ui/screens/settings/` - Settings screens
  - `ui/components/` - Reusable setting components
  - `ui/navigation/` - Bottom nav integration
  - `domain/model/` - UnitsSystem, GpsAccuracy, AutoPauseConfig models
  - `data/repository/` - SettingsRepository
- **Compliance**: Full adherence

**✅ Testing Requirements (Non-Safety-Critical Feature)**
- Per CLAUDE.md: Non-safety-critical features require unit tests for core logic
- Settings changes don't directly affect user safety (unlike speed detection)
- Required tests:
  - Unit: SettingsRepository, conversion utilities, enum mappings
  - UI: Navigation, component interactions, persistence
  - Integration: Settings reflected in other features (verified in F1A)
- Target: 70%+ coverage for repository and utility code
- **Compliance**: Appropriate test scope for feature criticality

**✅ Commit Frequency & Size**
- Small commits after each logical unit:
  - Commit 1: DataStore keys and enums
  - Commit 2: SettingsRepository
  - Commit 3: Settings ViewModel
  - Commit 4: Settings home screen composable
  - Commit 5: Ride & Tracking detail screen
  - Commit 6: Bottom navigation integration
  - Commit 7: Unit tests
  - Commit 8: UI tests
- Maximum ~200 lines per commit
- **Compliance**: Will follow per Constitution

**✅ Documentation Tracking (Mandatory)**
- TODO.md: Add "Feature 2A: Basic Settings Infrastructure" to In Progress
- RELEASE.md: Add entry to "Unreleased" section
- Updates happen automatically per Constitution requirements
- **Compliance**: Will update in implementation

**✅ Emulator Testing (Mandatory)**
- Feature MUST be tested on emulator before merge
- Validation checklist:
  - Settings tab navigation works
  - All 3 settings can be changed
  - Settings persist across app restart
  - Dark mode works correctly
  - 200% font scaling works without text truncation
  - TalkBack navigation works
- **Compliance**: Will test per Constitution

**✅ Release Pattern (Semantic Versioning)**
- Feature 2A → v0.2.0 (MINOR version bump - new feature)
- Version code: 0 * 10000 + 2 * 100 + 0 = 200
- Pull request → merge to main → tag v0.2.0 → build APK → GitHub release
- **Compliance**: Will follow per Constitution

### Gate Evaluation: ✅ PASS

**All gates satisfied. No violations. Proceed to Phase 0 research.**

## Project Structure

### Documentation (this feature)

```text
specs/001-settings-infrastructure/
├── spec.md              # Feature specification (/speckit.specify output)
├── plan.md              # This file (/speckit.plan output)
├── research.md          # Phase 0 output (technology decisions, patterns)
├── data-model.md        # Phase 1 output (entities, validation, state)
├── quickstart.md        # Phase 1 output (dev setup, testing guide)
├── contracts/           # Phase 1 output (DataStore schema, API contracts)
│   └── datastore-schema.md
├── checklists/          # Quality validation checklists
│   └── requirements.md  # Spec quality checklist (already complete)
└── tasks.md             # Phase 2 output (/speckit.tasks - NOT created yet)
```

### Source Code (repository root)

BikeRedlights uses **Android Clean Architecture** with feature-based organization:

```text
app/src/main/java/com/example/bikeredlights/
├── ui/
│   ├── screens/
│   │   └── settings/              # NEW: Feature 2A screens
│   │       ├── SettingsHomeScreen.kt
│   │       ├── RideTrackingSettingsScreen.kt
│   │       └── SettingsViewModel.kt
│   ├── components/
│   │   └── settings/              # NEW: Reusable settings components
│   │       ├── SettingCard.kt
│   │       ├── SegmentedButtonSetting.kt
│   │       └── ToggleWithPickerSetting.kt
│   ├── navigation/
│   │   ├── BottomNavDestination.kt  # NEW: Bottom nav enum
│   │   └── AppNavigation.kt         # MODIFIED: Add settings routes
│   ├── theme/                     # EXISTING: Material 3 theme
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── MainActivity.kt            # MODIFIED: Integrate bottom nav
│
├── domain/
│   ├── model/
│   │   └── settings/              # NEW: Settings domain models
│   │       ├── UnitsSystem.kt
│   │       ├── GpsAccuracy.kt
│   │       └── AutoPauseConfig.kt
│   └── util/
│       └── UnitConversions.kt     # NEW: km/h ↔ mph, km ↔ miles
│
├── data/
│   ├── repository/
│   │   └── SettingsRepository.kt  # NEW: DataStore operations
│   └── preferences/
│       └── PreferencesKeys.kt     # NEW: DataStore key definitions
│
└── di/
    └── AppModule.kt               # EXISTING: Manual DI (Hilt disabled)

app/src/test/java/com/example/bikeredlights/
├── data/
│   └── repository/
│       └── SettingsRepositoryTest.kt  # NEW: Unit tests
├── domain/
│   └── util/
│       └── UnitConversionsTest.kt     # NEW: Unit tests
└── ui/
    └── screens/
        └── settings/
            └── SettingsViewModelTest.kt  # NEW: Unit tests

app/src/androidTest/java/com/example/bikeredlights/
└── ui/
    └── screens/
        └── settings/
            ├── SettingsNavigationTest.kt     # NEW: UI tests
            └── SettingsPersistenceTest.kt    # NEW: Instrumented tests
```

**Structure Decision**:
- **Clean Architecture with feature modules** (UI → Domain → Data layers)
- Settings isolated to `ui/screens/settings/` and `ui/components/settings/`
- Domain models in `domain/model/settings/` (no Android dependencies)
- Repository pattern for DataStore abstraction
- Reusable components for Future 2B (Stop Detection Settings)
- Bottom navigation shared infrastructure for Features 1A, 2B, 3, 6

**Key Files**:
- **NEW**: 15 new source files (8 UI, 3 domain, 2 data, 2 navigation)
- **MODIFIED**: 2 existing files (MainActivity.kt for bottom nav, AppNavigation.kt for routes)
- **TESTS**: 6 new test files (3 unit, 3 UI/instrumented)

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**No violations detected. This section is not applicable.**

Feature 2A fully complies with CLAUDE.md Android Development Standards. No complexity justifications needed.

---

## Phase 0: Research (Next Steps)

**Objective**: Resolve technical unknowns and document technology decisions.

**Research Tasks**:
1. Material 3 SegmentedButton best practices (2-option mutually exclusive selection)
2. DataStore Preferences error handling patterns (read/write failure recovery)
3. Compose UI testing for settings persistence (app restart simulation)
4. Bottom Navigation integration with existing MainActivity structure
5. Auto-pause logic implementation patterns (timer-based state transitions)

**Output**: `research.md` with:
- Decision: Technology/pattern chosen
- Rationale: Why chosen (backed by official docs or best practices)
- Alternatives considered: What else was evaluated
- Code examples: Minimal working examples for each decision

**Next Command**: Planning workflow will automatically generate `research.md` in Phase 0.

---

## Phase 1: Design & Contracts (Subsequent Steps)

**Objective**: Define data models, API contracts, and quickstart guide.

**Design Artifacts**:
1. **data-model.md**:
   - UnitsSystem enum (METRIC, IMPERIAL)
   - GpsAccuracy enum (BATTERY_SAVER, HIGH_ACCURACY)
   - AutoPauseConfig data class (enabled: Boolean, thresholdMinutes: Int)
   - DataStore key definitions and default values
   - Validation rules (1-15 minute range for auto-pause)

2. **contracts/datastore-schema.md**:
   - DataStore Preferences schema
   - Key names, types, default values
   - Migration strategy (N/A for v0.2.0, but document for future)

3. **quickstart.md**:
   - Developer setup (Android Studio, emulator)
   - How to run feature in isolation
   - How to test settings changes
   - How to verify persistence

**Next Command**: Planning workflow will automatically generate these files in Phase 1.

---

## Phase 2: Task Breakdown (Final Step)

**Objective**: Generate atomic, dependency-ordered tasks for implementation.

**Task Categories** (estimated):
- Setup & Infrastructure (2-3 tasks): DataStore keys, enums, repository
- UI Components (4-5 tasks): SettingCard, SegmentedButton, Toggle+Picker
- Screens (3-4 tasks): Settings home, Ride & Tracking detail, ViewModel
- Navigation (2 tasks): Bottom nav integration, route definitions
- Testing (3-4 tasks): Unit tests, UI tests, persistence tests
- Documentation (2 tasks): Update TODO.md, RELEASE.md

**Estimated Total**: 16-20 atomic tasks (2-3 days of work)

**Next Command**: Run `/speckit.tasks` after Phase 1 completes to generate `tasks.md`.

---

**Planning Status**: Ready for Phase 0 research. Constitution gates passed. No blockers identified.
