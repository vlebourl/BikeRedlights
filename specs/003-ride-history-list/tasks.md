# Tasks: Ride History and List View

**Input**: Design documents from `/specs/003-ride-history-list/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Tests are REQUIRED per CLAUDE.md Constitution (80%+ coverage for ViewModels, UseCases, Repositories)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Android project structure:
- **Main code**: `app/src/main/java/com/example/bikeredlights/`
- **Unit tests**: `app/src/test/java/com/example/bikeredlights/`
- **UI tests**: `app/src/androidTest/java/com/example/bikeredlights/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create directory structure and base configuration

- [ ] T001 [P] Create ui/components/history directory in app/src/main/java/com/example/bikeredlights/ui/components/history/
- [ ] T002 [P] Create ui/screens/history directory in app/src/main/java/com/example/bikeredlights/ui/screens/history/
- [ ] T003 [P] Create domain/model/display directory in app/src/main/java/com/example/bikeredlights/domain/model/display/
- [ ] T004 [P] Create test directory structure for history feature in app/src/test/java/com/example/bikeredlights/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Display Models (Shared across all stories)

- [ ] T005 [P] Create RideListItem display model in app/src/main/java/com/example/bikeredlights/domain/model/display/RideListItem.kt
- [ ] T006 [P] Create RideDetailData display model in app/src/main/java/com/example/bikeredlights/domain/model/display/RideDetailData.kt

### Settings Models (Shared across US3 and US5)

- [ ] T007 [P] Create SortPreference enum in app/src/main/java/com/example/bikeredlights/domain/model/settings/SortPreference.kt
- [ ] T008 [P] Create DateRangeFilter sealed class in app/src/main/java/com/example/bikeredlights/domain/model/settings/DateRangeFilter.kt

### Utilities (Shared across all stories)

- [ ] T009 Extend FormatUtils with formatDuration, formatDate, formatDateTime methods in app/src/main/java/com/example/bikeredlights/domain/util/FormatUtils.kt
- [ ] T010 Add Ride.toListItem extension function in app/src/main/java/com/example/bikeredlights/domain/model/display/RideListItem.kt
- [ ] T011 Add Ride.toDetailData extension function in app/src/main/java/com/example/bikeredlights/domain/model/display/RideDetailData.kt

### Repository Extensions (Shared - preferences for US3 and US5)

- [ ] T012 Add getSortPreference and setSortPreference methods to UserPreferencesRepository interface in app/src/main/java/com/example/bikeredlights/domain/repository/UserPreferencesRepository.kt
- [ ] T013 Add getDateFilter and setDateFilter methods to UserPreferencesRepository interface in app/src/main/java/com/example/bikeredlights/domain/repository/UserPreferencesRepository.kt
- [ ] T014 Implement getSortPreference and setSortPreference in UserPreferencesRepositoryImpl in app/src/main/java/com/example/bikeredlights/data/preferences/UserPreferencesRepositoryImpl.kt
- [ ] T015 Implement getDateFilter and setDateFilter in UserPreferencesRepositoryImpl in app/src/main/java/com/example/bikeredlights/data/preferences/UserPreferencesRepositoryImpl.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - View List of All Rides (Priority: P1) 🎯 MVP

**Goal**: Display scrollable list of all saved rides with summary statistics in reverse chronological order

**Independent Test**: Save 3-5 rides with different dates/stats, navigate to History tab, verify all rides appear in correct order with formatted statistics

### Tests for User Story 1 (REQUIRED per Constitution)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T016 [P] [US1] Unit test for GetAllRidesUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/GetAllRidesUseCaseTest.kt
- [ ] T017 [P] [US1] Unit test for RideHistoryViewModel in app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModelTest.kt
- [ ] T018 [P] [US1] Compose UI test for RideHistoryScreen in app/src/androidTest/java/com/example/bikeredlights/ui/screens/history/RideHistoryScreenTest.kt

### Implementation for User Story 1

- [ ] T019 [US1] Create GetAllRidesUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/GetAllRidesUseCase.kt
- [ ] T020 [P] [US1] Create RideListItemCard composable in app/src/main/java/com/example/bikeredlights/ui/components/history/RideListItemCard.kt
- [ ] T021 [P] [US1] Create EmptyStateCard composable in app/src/main/java/com/example/bikeredlights/ui/components/history/EmptyStateCard.kt
- [ ] T022 [US1] Create RideHistoryViewModel with UI state management in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModel.kt
- [ ] T023 [US1] Create RideHistoryScreen composable with LazyColumn in app/src/main/java/com/example/bikeredlights/ui/screens/history/RideHistoryScreen.kt
- [ ] T024 [US1] Add History navigation route in app/src/main/java/com/example/bikeredlights/ui/navigation/AppNavigation.kt
- [ ] T025 [US1] Add History tab to bottom navigation in app/src/main/java/com/example/bikeredlights/MainActivity.kt
- [ ] T026 [US1] Provide GetAllRidesUseCase in Hilt AppModule in app/src/main/java/com/example/bikeredlights/di/AppModule.kt
- [ ] T027 [US1] Test on emulator: Navigate to History tab, verify list displays, scroll performance, empty state, unit formatting

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - View Detailed Ride Information (Priority: P2)

**Goal**: Tap on ride item to navigate to detail screen showing complete statistics including pause info

**Independent Test**: Tap any ride in list, verify detail screen shows all statistics (including max speed, start/end times, pause data), back navigation preserves scroll position

### Tests for User Story 2 (REQUIRED per Constitution)

- [ ] T028 [P] [US2] Unit test for GetRideByIdUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/GetRideByIdUseCaseTest.kt
- [ ] T029 [P] [US2] Unit test for RideDetailViewModel in app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideDetailViewModelTest.kt
- [ ] T030 [P] [US2] Compose UI test for RideDetailScreen in app/src/androidTest/java/com/example/bikeredlights/ui/screens/history/RideDetailScreenTest.kt

### Implementation for User Story 2

- [ ] T031 [US2] Create GetRideByIdUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/GetRideByIdUseCase.kt
- [ ] T032 [US2] Create RideDetailViewModel with detail state management in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideDetailViewModel.kt
- [ ] T033 [US2] Create RideDetailScreen composable with stats display in app/src/main/java/com/example/bikeredlights/ui/screens/history/RideDetailScreen.kt
- [ ] T034 [US2] Add ride detail navigation route with rideId parameter in app/src/main/java/com/example/bikeredlights/ui/navigation/AppNavigation.kt
- [ ] T035 [US2] Update RideHistoryScreen to navigate on item click in app/src/main/java/com/example/bikeredlights/ui/screens/history/RideHistoryScreen.kt
- [ ] T036 [US2] Provide GetRideByIdUseCase in Hilt AppModule in app/src/main/java/com/example/bikeredlights/di/AppModule.kt
- [ ] T037 [US2] Test on emulator: Tap ride, verify all detail stats, back navigation, unit formatting, pause stats display

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 4 - Delete Rides (Priority: P2)

**Goal**: Swipe-to-delete rides from list and delete from detail screen with confirmation dialog

**Independent Test**: Swipe ride item, confirm deletion via dialog, verify ride disappears from list and database. Also test delete from detail screen.

**Note**: Implementing P2 User Story 4 before P3 User Story 3 based on priority

### Tests for User Story 4 (REQUIRED per Constitution)

- [ ] T038 [P] [US4] Unit test for DeleteRideUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/DeleteRideUseCaseTest.kt
- [ ] T039 [P] [US4] Update RideHistoryViewModel test to cover delete functionality in app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModelTest.kt
- [ ] T040 [P] [US4] Compose UI test for swipe-to-delete in app/src/androidTest/java/com/example/bikeredlights/ui/screens/history/RideHistoryScreenTest.kt

### Implementation for User Story 4

- [ ] T041 [US4] Create DeleteRideUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/DeleteRideUseCase.kt
- [ ] T042 [P] [US4] Create DeleteConfirmDialog composable in app/src/main/java/com/example/bikeredlights/ui/components/history/DeleteConfirmDialog.kt
- [ ] T043 [US4] Add SwipeToDismissBox to RideListItemCard in app/src/main/java/com/example/bikeredlights/ui/components/history/RideListItemCard.kt
- [ ] T044 [US4] Add deleteRide function to RideHistoryViewModel in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModel.kt
- [ ] T045 [US4] Integrate DeleteConfirmDialog in RideHistoryScreen in app/src/main/java/com/example/bikeredlights/ui/screens/history/RideHistoryScreen.kt
- [ ] T046 [US4] Add delete button and dialog to RideDetailScreen in app/src/main/java/com/example/bikeredlights/ui/screens/history/RideDetailScreen.kt
- [ ] T047 [US4] Add delete functionality to RideDetailViewModel in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideDetailViewModel.kt
- [ ] T048 [US4] Provide DeleteRideUseCase in Hilt AppModule in app/src/main/java/com/example/bikeredlights/di/AppModule.kt
- [ ] T049 [US4] Test on emulator: Swipe-to-delete, confirmation dialog, actual deletion, delete from detail screen, reactive list update

**Checkpoint**: User Stories 1, 2, and 4 should all work independently

---

## Phase 6: User Story 3 - Sort Rides (Priority: P3)

**Goal**: Sort ride list by different criteria (newest/oldest/distance/duration) with persistence

**Independent Test**: Save rides with varied stats, select sort options from menu, verify list reorders correctly, preference persists across sessions

### Tests for User Story 3 (REQUIRED per Constitution)

- [ ] T050 [P] [US3] Unit test for GetSortPreferenceUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/GetSortPreferenceUseCaseTest.kt
- [ ] T051 [P] [US3] Unit test for SaveSortPreferenceUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/SaveSortPreferenceUseCaseTest.kt
- [ ] T052 [P] [US3] Update RideHistoryViewModel test to cover sort functionality in app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModelTest.kt

### Implementation for User Story 3

- [ ] T053 [P] [US3] Create GetSortPreferenceUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/GetSortPreferenceUseCase.kt
- [ ] T054 [P] [US3] Create SaveSortPreferenceUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/SaveSortPreferenceUseCase.kt
- [ ] T055 [P] [US3] Create SortMenuDialog composable in app/src/main/java/com/example/bikeredlights/ui/components/history/SortMenuDialog.kt
- [ ] T056 [US3] Add sort preference Flow to RideHistoryViewModel in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModel.kt
- [ ] T057 [US3] Implement sort logic in loadRides using combine() in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModel.kt
- [ ] T058 [US3] Add sort menu button to RideHistoryScreen toolbar in app/src/main/java/com/example/bikeredlights/ui/screens/history/RideHistoryScreen.kt
- [ ] T059 [US3] Provide sort use cases in Hilt AppModule in app/src/main/java/com/example/bikeredlights/di/AppModule.kt
- [ ] T060 [US3] Test on emulator: Sort menu, all sort options, list reordering, preference persistence, integration with existing features

**Checkpoint**: All user stories except US5 should work independently

---

## Phase 7: User Story 5 - Filter Rides by Date Range (Priority: P4)

**Goal**: Filter ride list by date range (preset and custom) with active filter badge

**Independent Test**: Save rides across multiple months, apply date filters, verify only matching rides appear, filter badge shows when active

### Tests for User Story 5 (REQUIRED per Constitution)

- [ ] T061 [P] [US5] Unit test for GetDateFilterUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/GetDateFilterUseCaseTest.kt
- [ ] T062 [P] [US5] Unit test for SaveDateFilterUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/SaveDateFilterUseCaseTest.kt
- [ ] T063 [P] [US5] Update RideHistoryViewModel test to cover filter functionality in app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModelTest.kt
- [ ] T064 [P] [US5] Unit test for DateRangeFilter.matches() logic in app/src/test/java/com/example/bikeredlights/domain/model/settings/DateRangeFilterTest.kt

### Implementation for User Story 5

- [ ] T065 [P] [US5] Create GetDateFilterUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/GetDateFilterUseCase.kt
- [ ] T066 [P] [US5] Create SaveDateFilterUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/SaveDateFilterUseCase.kt
- [ ] T067 [P] [US5] Create FilterDialog composable with date picker in app/src/main/java/com/example/bikeredlights/ui/components/history/FilterDialog.kt
- [ ] T068 [US5] Add date filter Flow to RideHistoryViewModel in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModel.kt
- [ ] T069 [US5] Implement filter logic in loadRides using combine() in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModel.kt
- [ ] T070 [US5] Add filter button with badge to RideHistoryScreen toolbar in app/src/main/java/com/example/bikeredlights/ui/screens/history/RideHistoryScreen.kt
- [ ] T071 [US5] Provide filter use cases in Hilt AppModule in app/src/main/java/com/example/bikeredlights/di/AppModule.kt
- [ ] T072 [US5] Test on emulator: Filter menu, preset filters, custom date range, filter badge, clear filter, integration with sort

**Checkpoint**: All user stories should now be independently functional

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and final validation

- [ ] T073 [P] Add content descriptions for accessibility to all interactive elements in ui/components/history/
- [ ] T074 [P] Verify 48dp touch targets for all buttons and list items per CLAUDE.md
- [ ] T075 [P] Test dark mode rendering for all history screens
- [ ] T076 [P] Add error handling for database failures in RideHistoryViewModel (SQLiteException, IOException on query - display Snackbar: "Unable to load rides. Pull to refresh.")
- [ ] T077 [P] Add error handling for preferences failures in ViewModels (DataStore IOException on read/write - use default values, log warning, continue gracefully without user notification)
- [ ] T078 [P] Add logging for all user actions (view list, tap ride, sort, filter, delete)
- [ ] T079 Run lint checks and address warnings: ./gradlew lint
- [ ] T080 Run all unit tests and verify 80%+ coverage: ./gradlew test jacocoTestReport
- [ ] T081 Run all UI tests on emulator: ./gradlew connectedAndroidTest
- [ ] T082 Perform comprehensive emulator testing per quickstart.md validation checklist
- [ ] T083 Update TODO.md with feature completion status
- [ ] T084 Update RELEASE.md with v0.4.0 feature entry

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - Can proceed in parallel (if staffed) OR sequentially by priority
  - Priority order: US1 (P1) → US2 (P2) → US4 (P2) → US3 (P3) → US5 (P4)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - No dependencies on other stories ✅ **MVP**
- **User Story 2 (P2)**: Can start after Foundational - Integrates with US1 but independently testable
- **User Story 4 (P2)**: Can start after Foundational - Works with US1 list, independently testable
- **User Story 3 (P3)**: Can start after Foundational - Enhances US1 list, independently testable
- **User Story 5 (P4)**: Can start after Foundational - Enhances US1 list, works with US3 sort, independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD per Constitution)
- Display models before use cases
- Use cases before ViewModels
- ViewModels before composables
- Core implementation before integration
- Story complete and validated before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001-T004) can run in parallel
- All Foundational tasks marked [P] can run in parallel (T005-T008, T010-T011, T012-T013, T014-T015)
- Once Foundational completes, all user stories can start in parallel (if team capacity allows)
- All tests within a user story marked [P] can run in parallel
- Models/composables within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test for GetAllRidesUseCase in app/src/test/.../GetAllRidesUseCaseTest.kt"
Task: "Unit test for RideHistoryViewModel in app/src/test/.../RideHistoryViewModelTest.kt"
Task: "Compose UI test for RideHistoryScreen in app/src/androidTest/.../RideHistoryScreenTest.kt"

# Launch all composables for User Story 1 together (after use case):
Task: "Create RideListItemCard composable in ui/components/history/RideListItemCard.kt"
Task: "Create EmptyStateCard composable in ui/components/history/EmptyStateCard.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T015) - **CRITICAL - blocks all stories**
3. Complete Phase 3: User Story 1 (T016-T027)
4. **STOP and VALIDATE**: Test User Story 1 independently on emulator
5. Deploy/demo if ready → **Minimal Viable Product achieved!**

### Incremental Delivery (Recommended)

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Commit → Deploy/Demo (MVP! 🎯)
3. Add User Story 2 → Test independently → Commit → Deploy/Demo
4. Add User Story 4 → Test independently → Commit → Deploy/Demo
5. Add User Story 3 → Test independently → Commit → Deploy/Demo
6. Add User Story 5 → Test independently → Commit → Deploy/Demo
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T015)
2. Once Foundational is done:
   - **Developer A**: User Story 1 (T016-T027) - **MVP Priority**
   - **Developer B**: User Story 2 (T028-T037) - Can start in parallel
   - **Developer C**: User Story 4 (T038-T049) - Can start in parallel
3. After core stories:
   - **Developer A/B/C**: User Story 3 (T050-T060)
   - **Developer A/B/C**: User Story 5 (T061-T072)
4. Team completes Polish together (T073-T084)

---

## Notes

- **[P] tasks**: Different files, no dependencies - can run in parallel
- **[Story] label**: Maps task to specific user story for traceability
- **Tests REQUIRED**: Constitution mandates 80%+ coverage for ViewModels, UseCases, Repositories
- **TDD approach**: Write tests first, verify they FAIL, then implement
- **Small commits**: ~200 lines per task per CLAUDE.md Constitution
- **Commit format**: Conventional commits (feat/fix/refactor/test/docs)
- **Emulator testing**: MANDATORY per CLAUDE.md before considering feature complete
- **Each user story**: Should be independently completable and testable
- **Stop at checkpoints**: Validate story independently before moving to next
- **Avoid**: Vague tasks, same file conflicts, cross-story dependencies that break independence

---

## Task Summary

**Total Tasks**: 84
**Setup**: 4 tasks
**Foundational**: 11 tasks (BLOCKS all user stories)
**User Story 1 (P1)**: 12 tasks (Tests: 3, Impl: 9) - **MVP**
**User Story 2 (P2)**: 10 tasks (Tests: 3, Impl: 7)
**User Story 4 (P2)**: 12 tasks (Tests: 3, Impl: 9)
**User Story 3 (P3)**: 11 tasks (Tests: 3, Impl: 8)
**User Story 5 (P4)**: 12 tasks (Tests: 4, Impl: 8)
**Polish**: 12 tasks

**Parallel Opportunities**: 31 tasks marked [P] can run concurrently
**MVP Scope**: 27 tasks (Setup + Foundational + US1)
**Estimated MVP Time**: 15-20 hours for solo developer (with TDD)
**Full Feature Time**: 50-60 hours for solo developer (all stories + polish)
