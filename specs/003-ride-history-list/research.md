# Research: Ride History and List View

**Feature**: 003-ride-history-list
**Phase**: 0 (Outline & Research)
**Date**: 2025-11-06
**Purpose**: Document architectural decisions, best practices, and patterns for implementing ride history list and detail screens

## Research Questions & Findings

### 1. LazyColumn Performance for Large Lists (1000+ Rides)

**Question**: What are the best practices for implementing performant scrollable lists with 1000+ items in Jetpack Compose?

**Decision**: Use LazyColumn with proper key strategy and avoid heavy computations in item composables

**Rationale**:
- LazyColumn only composes and lays out visible items (virtualization)
- Proper key() usage ensures efficient recomposition when list changes
- Compose's lazy layouts are optimized for infinite scroll scenarios
- Google's Now in Android sample demonstrates this pattern effectively

**Key Implementation Details**:
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize()
) {
    items(
        items = rides,
        key = { ride -> ride.id } // Stable key for efficient updates
    ) { ride ->
        RideListItemCard(ride) // Keep composable lightweight
    }
}
```

**Performance Considerations**:
- Keep RideListItemCard composable stateless and lightweight
- Avoid `remember {}` with expensive lambdas inside item scope
- Use `derivedStateOf` for computed values that depend on ride data
- Format strings (dates, durations) in ViewModel, not in composable

**Alternatives Considered**:
- Manual RecyclerView with ViewBinding: Rejected because project uses Compose-only (CLAUDE.md requirement). LazyColumn provides equivalent or better performance with cleaner code.
- Paging 3 library: Deferred to future version. Current scope supports 1000+ rides comfortably without pagination. Can add if telemetry shows need for 10k+ rides.

---

### 2. Material 3 List Item Patterns

**Question**: What Material 3 components and patterns should be used for list items displaying ride summaries?

**Decision**: Use Material 3 Card with ListItem-style layout (leading icon, three-line text, trailing metadata)

**Rationale**:
- M3 Cards provide elevation, shape, and interaction states (ripple, focus)
- Three-line list item pattern accommodates: title (ride name), subtitle (date), supporting text (stats)
- Aligns with M3 Expressive guidelines for data-dense lists
- Cards enable swipe-to-delete interaction surface

**Implementation Pattern**:
```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onRideClick(ride.id) },
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) {
    Row(modifier = Modifier.padding(16.dp)) {
        // Leading icon (optional - bike icon or ride type indicator)
        Icon(Icons.Default.DirectionsBike, ...)

        Column(modifier = Modifier.weight(1f)) {
            Text(ride.name, style = MaterialTheme.typography.titleMedium)
            Text(ride.date, style = MaterialTheme.typography.bodyMedium)
            Text("${ride.distance} • ${ride.duration}",
                 style = MaterialTheme.typography.bodySmall)
        }

        // Trailing metadata (average speed)
        Text(ride.avgSpeed, style = MaterialTheme.typography.labelLarge)
    }
}
```

**Alternatives Considered**:
- M3 ListItem composable: Available but less flexible for custom layouts. Card + manual layout provides better control for stat display.
- Simple Text rows without Cards: Rejected due to lack of touch feedback and visual hierarchy. Cards provide better UX for tappable items.

---

### 3. Swipe-to-Delete Implementation

**Question**: How should swipe-to-delete be implemented in Compose for ride list items?

**Decision**: Use `SwipeToDismiss` from Material 3 Compose with confirmation dialog

**Rationale**:
- `SwipeToDismiss` is official Material 3 Compose API for swipe gestures
- Provides consistent animation and gesture physics
- Supports both left and right swipe directions
- Integrates with Material 3 dismissible surface patterns

**Implementation Pattern**:
```kotlin
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { dismissValue ->
        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
            // Show delete confirmation dialog
            showDeleteDialog = true
            false // Don't dismiss yet, wait for confirmation
        } else {
            false
        }
    }
)

SwipeToDismissBox(
    state = dismissState,
    backgroundContent = { dismissDirection ->
        val color = when (dismissDirection) {
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
            else -> Color.Transparent
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(color)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(Icons.Default.Delete, "Delete", tint = Color.White)
        }
    }
) {
    RideListItemCard(ride)
}
```

**Safety Considerations**:
- Always show confirmation dialog before actual deletion
- Provide clear warning: "Delete this ride? This cannot be undone."
- Two-button dialog: "Cancel" (default) and "Delete" (destructive color)
- Only delete after explicit confirmation

**Alternatives Considered**:
- Custom gesture detection: Rejected due to complexity and inconsistency with M3 patterns. SwipeToDismiss provides standard UX users expect.
- Long-press menu with delete option: Considered for future enhancement but swipe-to-delete is more discoverable for primary action.

---

### 4. Sort/Filter UI Patterns

**Question**: What UI patterns should be used for sort and filter options in the toolbar?

**Decision**:
- **Sort**: Overflow menu (3-dot icon) with radio group for sort options
- **Filter**: Dedicated filter icon button with bottom sheet dialog for date range selection

**Rationale**:
- Sort is less frequently changed → appropriate for overflow menu
- Filter is more dynamic → deserves dedicated button with badge when active
- Bottom sheet provides space for custom date range picker
- Follows Material 3 patterns for list screen actions

**Sort Implementation**:
```kotlin
IconButton(onClick = { showSortMenu = true }) {
    Icon(Icons.Default.MoreVert, "More options")
}

DropdownMenu(
    expanded = showSortMenu,
    onDismissRequest = { showSortMenu = false }
) {
    DropdownMenuItem(
        text = { Text("Newest First") },
        onClick = { viewModel.setSortPreference(NEWEST_FIRST) },
        leadingIcon = { if (currentSort == NEWEST_FIRST) Icon(Icons.Default.Check, null) }
    )
    DropdownMenuItem(
        text = { Text("Oldest First") },
        onClick = { viewModel.setSortPreference(OLDEST_FIRST) },
        leadingIcon = { if (currentSort == OLDEST_FIRST) Icon(Icons.Default.Check, null) }
    )
    DropdownMenuItem(
        text = { Text("Longest Distance") },
        onClick = { viewModel.setSortPreference(LONGEST_DISTANCE) },
        leadingIcon = { if (currentSort == LONGEST_DISTANCE) Icon(Icons.Default.Check, null) }
    )
    DropdownMenuItem(
        text = { Text("Longest Duration") },
        onClick = { viewModel.setSortPreference(LONGEST_DURATION) },
        leadingIcon = { if (currentSort == LONGEST_DURATION) Icon(Icons.Default.Check, null) }
    )
}
```

**Filter Implementation**:
```kotlin
BadgedBox(
    badge = { if (filterActive) Badge { Text("1") } }
) {
    IconButton(onClick = { showFilterDialog = true }) {
        Icon(Icons.Default.FilterList, "Filter rides")
    }
}

if (showFilterDialog) {
    FilterDialog(
        currentFilter = currentFilter,
        onDismiss = { showFilterDialog = false },
        onApply = { filter -> viewModel.setDateFilter(filter) }
    )
}
```

**Alternatives Considered**:
- Chips for sort/filter at top of list: Rejected due to screen real estate constraints on phone screens. Toolbar actions more compact.
- Full-screen filter activity: Rejected as over-engineered for simple date range selection. Dialog is sufficient.

---

### 5. Date Picker for Custom Range

**Question**: What date picker component should be used for custom date range filtering?

**Decision**: Use Material 3 DateRangePicker from `androidx.compose.material3:material3`

**Rationale**:
- Official M3 component with consistent styling
- Supports date range selection out of the box
- Handles locale formatting and calendar logic
- Modal presentation aligns with filter dialog pattern

**Implementation Pattern**:
```kotlin
val dateRangePickerState = rememberDateRangePickerState()

DatePickerDialog(
    onDismissRequest = { showDatePicker = false },
    confirmButton = {
        TextButton(onClick = {
            val start = dateRangePickerState.selectedStartDateMillis
            val end = dateRangePickerState.selectedEndDateMillis
            if (start != null && end != null) {
                onDateRangeSelected(start, end)
            }
        }) {
            Text("OK")
        }
    },
    dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
            Text("Cancel")
        }
    }
) {
    DateRangePicker(state = dateRangePickerState)
}
```

**Alternatives Considered**:
- Third-party date picker libraries: Rejected to minimize dependencies. Material 3 provides sufficient functionality.
- Manual text input for dates: Rejected due to poor UX and validation complexity. Visual calendar picker is standard.

---

### 6. State Management for List Screens with Filters

**Question**: How should UI state be managed for ride history screen with sort/filter options?

**Decision**: Use sealed class for UI state + separate StateFlows for sort/filter preferences

**Rationale**:
- Sealed class captures loading/success/error states clearly
- Separate StateFlows for preferences enable reactive UI updates
- `collectAsStateWithLifecycle` ensures proper lifecycle handling
- Follows Android recommended ViewModel patterns

**State Classes**:
```kotlin
sealed interface RideHistoryUiState {
    object Loading : RideHistoryUiState
    data class Success(val rides: List<RideListItem>) : RideHistoryUiState
    data class Error(val message: String) : RideHistoryUiState
}

data class RideListItem(
    val id: Long,
    val name: String,
    val date: String,
    val duration: String,
    val distance: String,
    val avgSpeed: String
)
```

**ViewModel Pattern**:
```kotlin
class RideHistoryViewModel @Inject constructor(
    private val getAllRidesUseCase: GetAllRidesUseCase,
    private val getSortPreferenceUseCase: GetSortPreferenceUseCase,
    private val saveSortPreferenceUseCase: SaveSortPreferenceUseCase,
    private val getDateFilterUseCase: GetDateFilterUseCase,
    private val saveDateFilterUseCase: SaveDateFilterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RideHistoryUiState>(Loading)
    val uiState: StateFlow<RideHistoryUiState> = _uiState.asStateFlow()

    val sortPreference = getSortPreferenceUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortPreference.NEWEST_FIRST
    )

    val dateFilter = getDateFilterUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DateRangeFilter.AllTime
    )

    init {
        loadRides()
    }

    private fun loadRides() {
        viewModelScope.launch {
            combine(
                getAllRidesUseCase(),
                sortPreference,
                dateFilter
            ) { rides, sort, filter ->
                // Apply sort and filter, then map to display model
                rides
                    .filter { /* apply date filter */ }
                    .sortedWith( /* apply sort */ )
                    .map { /* map to RideListItem */ }
            }
                .catch { e -> _uiState.value = Error(e.message ?: "Unknown error") }
                .collect { rides -> _uiState.value = Success(rides) }
        }
    }
}
```

**Alternatives Considered**:
- Single StateFlow with nested data class: Rejected due to unnecessary coupling. Separate flows allow independent observation.
- MutableLiveData: Rejected per CLAUDE.md (prefer StateFlow/Flow over LiveData). StateFlow provides better coroutines integration.

---

### 7. Testing Patterns for Compose UI with Lists

**Question**: What testing strategies should be used for Compose screens with LazyColumn?

**Decision**: Multi-layer testing approach:
1. **Unit Tests**: ViewModels, UseCases (MockK, Turbine for Flow testing)
2. **Compose UI Tests**: Screen composables (ComposeTestRule, semantic matchers)
3. **Integration Tests**: Repository + Room DAO (AndroidX Test, in-memory database)
4. **Emulator Tests**: End-to-end manual validation (Constitution requirement)

**ViewModel Test Pattern**:
```kotlin
@Test
fun `when sort preference changes, rides are reordered`() = runTest {
    // Arrange
    val mockRides = listOf(rideA, rideB, rideC)
    whenever(mockGetAllRidesUseCase()).thenReturn(flowOf(mockRides))

    val viewModel = RideHistoryViewModel(...)

    // Act
    viewModel.setSortPreference(SortPreference.OLDEST_FIRST)

    // Assert
    viewModel.uiState.test {
        val state = awaitItem()
        assertTrue(state is Success)
        assertEquals(rideC, (state as Success).rides.first())
    }
}
```

**Compose UI Test Pattern**:
```kotlin
@Test
fun `clicking ride item navigates to detail screen`() {
    composeTestRule.setContent {
        RideHistoryScreen(
            navController = navController,
            viewModel = viewModel
        )
    }

    // Arrange - ensure UI is loaded
    composeTestRule.onNodeWithText("Ride on Nov 4, 2025").assertIsDisplayed()

    // Act
    composeTestRule.onNodeWithText("Ride on Nov 4, 2025").performClick()

    // Assert
    verify { navController.navigate("ride/123") }
}
```

**LazyColumn Testing Considerations**:
- Use semantic properties for testability (testTag, contentDescription)
- Test only visible items (LazyColumn doesn't compose off-screen items in tests)
- Use `performScrollToNode()` to bring items into view before assertions
- Test state changes (loading → success → error) with `waitUntil {}`

**Alternatives Considered**:
- Screenshot testing (Paparazzi/Roborazzi): Considered for future enhancement but not required for MVP. Manual visual QA sufficient initially.
- Espresso for UI tests: Rejected per CLAUDE.md (prefer Compose testing framework for Compose UI). Espresso is legacy for View system.

---

## Architecture Decisions

### Clean Architecture Layer Responsibilities

**Domain Layer**:
- Use Cases: Business logic for get/delete rides, manage preferences
- Models: Ride entity, SortPreference enum, DateRangeFilter sealed class
- Repository Interfaces: Contract for data operations

**Data Layer**:
- Repository Impl: Orchestrate Room queries + DataStore reads
- Room DAO: Query rides with flexible sorting
- DataStore: Persist user preferences (sort, filter)

**UI Layer**:
- ViewModels: Orchestrate use cases, manage UI state, format display data
- Composables: Stateless, receive formatted data, emit events
- Navigation: Coordinate screen transitions

### Dependency Flow

```
UI (Composable)
    ↓ events
ViewModel
    ↓ requests
UseCase
    ↓ data operations
Repository Interface
    ↓ implementation
Repository Impl
    ↓ queries
Room DAO / DataStore
```

**Key Principle**: Dependencies point inward. UI knows about ViewModels, ViewModels know about UseCases, UseCases know about Repository interfaces, but lower layers never reference upper layers.

---

## Technology Stack Confirmation

**UI Framework**: Jetpack Compose (Material 3 components)
- Confirmed via CLAUDE.md requirement: "NO XML layouts for new features"
- All dependencies already in project (Compose BOM 2024.11.00)

**Database**: Room 2.6.1
- Existing Ride and TrackPoint entities
- No schema migration needed for this feature

**Async**: Kotlin Coroutines + Flow
- ViewModel: viewModelScope for coroutine management
- Repository: Return Flow<> for reactive data streams
- UI: collectAsStateWithLifecycle for safe collection

**Dependency Injection**: Hilt
- ViewModels: @HiltViewModel annotation
- UseCases: @Inject constructor injection
- Repository: Provided in existing AppModule

**Preferences**: DataStore (Preferences)
- Existing UserPreferencesRepository to be extended
- Type-safe preference keys using `preferencesKey<T>()`

**Testing**:
- Unit: JUnit 5, MockK (mocking), Turbine (Flow assertions)
- UI: Compose Test (ComposeTestRule, semantic matchers)
- Integration: AndroidX Test, Room in-memory database

**Build System**: Gradle with Kotlin DSL
- Existing build.gradle.kts structure
- JaCoCo for test coverage (already configured)

---

## Best Practices Summary

1. **Performance**: Use LazyColumn with stable keys, keep composables lightweight, format data in ViewModel
2. **Material 3**: Cards for list items, official M3 components (DateRangePicker, SwipeToDismiss), dynamic color support
3. **State Management**: Sealed class for UI state, separate StateFlows for preferences, combine() for reactive data transformation
4. **Testing**: Multi-layer approach (unit/UI/integration/emulator), MockK for mocking, Turbine for Flow testing
5. **Architecture**: Clean Architecture with MVVM, unidirectional data flow, repository pattern for data abstraction
6. **Accessibility**: Semantic properties, content descriptions, WCAG AA contrast, 48dp touch targets
7. **Code Quality**: Small commits (~200 lines), conventional commit messages, 80%+ test coverage, emulator validation

---

## Open Questions / Future Considerations

**Resolved in This Research**:
- All technical unknowns from Technical Context are resolved
- Sort/filter patterns selected
- State management approach defined
- Testing strategy established

**Deferred to Future Versions**:
- **Pagination**: Not needed for 1000 rides, can add Paging 3 if telemetry shows 10k+ rides
- **Search**: Text search by ride name deferred to v0.5.0 (mentioned in spec but P5 priority)
- **Bulk Delete**: Delete multiple rides at once - deferred based on user feedback
- **Export**: Export ride history to GPX/CSV - separate feature entirely (F4+)
- **Cloud Sync**: All data local-only in v0.4.0, sync deferred to future version

**Follow-up Actions**:
- Phase 1: Create data-model.md with detailed entity/model specifications
- Phase 1: Create quickstart.md for local development setup
- Phase 1: Update agent context with new architecture patterns
- Phase 2: Generate tasks.md with implementation breakdown (via /speckit.tasks)
