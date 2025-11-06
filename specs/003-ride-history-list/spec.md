# Feature Specification: Ride History and List View

**Feature Branch**: `003-ride-history-list`
**Created**: 2025-11-06
**Status**: Draft
**Input**: User description: "Feature: Ride History and List View - Implement the ride history list view as defined in the project roadmap. Users need to view previously recorded rides, see summary statistics, filter/sort rides, and access detailed ride information."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View List of All Rides (Priority: P1)

As a cyclist, I want to see a chronological list of all my previously recorded rides so that I can browse my cycling history.

**Why this priority**: This is the foundational capability - users need to see their saved rides before any other functionality (sorting, filtering, details) has value. Without this, the database of rides is invisible to users.

**Independent Test**: Can be fully tested by saving 3-5 rides with different dates/stats, then navigating to History tab and verifying all rides appear in reverse chronological order (newest first) with correct summary information displayed.

**Acceptance Scenarios**:

1. **Given** user has saved rides in the database, **When** user navigates to History tab, **Then** all saved rides appear in a scrollable list sorted by start time (newest first)
2. **Given** ride list is displayed, **When** viewing each ride item, **Then** each item shows ride name, date, total duration, total distance, and average speed
3. **Given** user has more rides than fit on screen, **When** user scrolls down, **Then** list scrolls smoothly and loads additional rides without lag
4. **Given** user has no saved rides, **When** viewing History tab, **Then** an empty state message displays "No rides yet. Start your first ride on the Live tab!"
5. **Given** ride statistics are displayed, **When** user views values, **Then** all statistics respect user's preferred units (Metric or Imperial) from settings

---

### User Story 2 - View Detailed Ride Information (Priority: P2)

As a cyclist, I want to tap on a ride in the list to see complete details about that specific ride so that I can review my performance.

**Why this priority**: After seeing the list of rides, users naturally want to drill down into individual rides for more context. This adds significant value but depends on P1 existing first.

**Independent Test**: Can be fully tested by tapping any ride in the list and verifying navigation to detail screen showing all ride statistics, including max speed and start/end times that aren't shown in the list view.

**Acceptance Scenarios**:

1. **Given** ride list is displayed, **When** user taps on any ride item, **Then** app navigates to Ride Detail screen for that specific ride
2. **Given** Ride Detail screen is open, **When** viewing ride information, **Then** screen displays ride name, start date/time, end date/time, total duration, total distance, average speed, max speed, total paused time, and pause count
3. **Given** Ride Detail screen is open, **When** user taps back button, **Then** user returns to History list at same scroll position
4. **Given** Ride Detail screen is open, **When** viewing statistics, **Then** all values use user's preferred units from settings
5. **Given** Ride Detail screen is open, **When** looking for map, **Then** a placeholder message "Route map coming soon" is displayed (route visualization deferred to future feature)

---

### User Story 3 - Sort Rides (Priority: P3)

As a cyclist, I want to sort my ride list by different criteria (date, distance, duration) so that I can easily find rides based on what's most relevant to me.

**Why this priority**: Sorting enhances findability but is less critical than core viewing functionality. Most users with < 20 rides won't need this, but it becomes valuable as ride count grows.

**Independent Test**: Can be fully tested by saving rides with varied statistics, then selecting different sort options and verifying list reorders correctly. Each sort option works independently.

**Acceptance Scenarios**:

1. **Given** ride list is displayed with multiple rides, **When** user taps sort button in toolbar, **Then** a menu appears with options: "Newest First" (default), "Oldest First", "Longest Distance", "Longest Duration"
2. **Given** sort menu is open, **When** user selects "Oldest First", **Then** list reorders to show rides from earliest to most recent start time
3. **Given** sort menu is open, **When** user selects "Longest Distance", **Then** list reorders to show rides with greatest distance at top
4. **Given** sort menu is open, **When** user selects "Longest Duration", **Then** list reorders to show rides with longest duration at top
5. **Given** user has selected a sort option, **When** user leaves History tab and returns, **Then** sort preference is remembered and list displays in previously selected order

---

### User Story 4 - Delete Rides (Priority: P2)

As a cyclist, I want to delete individual rides from my history so that I can remove test rides or unwanted data.

**Why this priority**: Users need control over their data. Test rides during initial app use or accidental recordings should be removable. This is important for data hygiene but not as critical as viewing rides.

**Independent Test**: Can be fully tested by deleting a single ride via swipe-to-delete or detail screen delete button, verifying ride disappears from list and confirmation prevents accidental deletion.

**Acceptance Scenarios**:

1. **Given** ride list is displayed, **When** user swipes left on a ride item, **Then** a red "Delete" action appears
2. **Given** "Delete" action is visible, **When** user taps delete, **Then** a confirmation dialog appears asking "Delete this ride? This cannot be undone."
3. **Given** delete confirmation dialog is shown, **When** user taps "Delete", **Then** ride is permanently removed from database (including all track points via cascade delete) and disappears from list
4. **Given** delete confirmation dialog is shown, **When** user taps "Cancel", **Then** dialog closes and ride remains in list
5. **Given** Ride Detail screen is open, **When** user taps delete icon in toolbar, **Then** same confirmation dialog appears, and after confirming, user returns to History list with ride removed

---

### User Story 5 - Search/Filter Rides by Date Range (Priority: P4)

As a cyclist, I want to filter my ride history by date range (e.g., last week, last month, custom range) so that I can focus on rides from a specific time period.

**Why this priority**: Useful for users with many rides who want to review specific time periods (e.g., "How much did I ride in October?"). Lower priority because it's a power user feature.

**Independent Test**: Can be fully tested by saving rides across multiple months, then applying date range filters and verifying only rides within the selected period appear.

**Acceptance Scenarios**:

1. **Given** ride list is displayed, **When** user taps filter button in toolbar, **Then** a menu appears with preset options: "All Time" (default), "Last 7 Days", "Last 30 Days", "This Year", "Custom Range"
2. **Given** filter menu is open, **When** user selects "Last 7 Days", **Then** list shows only rides with start time within past 7 days
3. **Given** filter menu is open, **When** user selects "Last 30 Days", **Then** list shows only rides with start time within past 30 days
4. **Given** filter menu is open, **When** user selects "Custom Range", **Then** a date picker dialog appears allowing user to select start and end dates
5. **Given** custom range is selected, **When** user confirms dates, **Then** list shows only rides within that date range and filter button shows badge indicating active filter
6. **Given** a filter is active, **When** user taps filter button and selects "All Time", **Then** filter is cleared and all rides appear again

---

### Edge Cases

- **Empty state**: What happens when user has never saved a ride?
  - Display friendly empty state with icon, "No rides yet" heading, and "Start your first ride on the Live tab!" message
  - List container shows centered content, not broken UI

- **Single ride**: What happens when user has only one ride?
  - List displays single item normally
  - Sorting has no effect (no indicator of sort being applied)
  - All functionality works identically

- **Large number of rides**: What happens when user has 100+ rides?
  - List uses LazyColumn for performance (automatic item windowing - only visible items are composed)
  - Scrolling remains smooth (60fps target)
  - Room Flow automatically updates list reactively without manual pagination
  - Ride count indicator "Showing X rides" deferred to future enhancement (v0.5.0+)

- **Very long ride names**: What happens if user later edits ride name to 100+ characters (future feature)?
  - Ride names truncate with ellipsis in list view (max 2 lines)
  - Full name visible in detail screen
  - No horizontal scrolling or layout breaking

- **Date/time edge cases**: What happens with rides that span midnight or daylight saving time changes?
  - Display ride date as start date/time
  - Duration calculation uses actual elapsed seconds (unaffected by DST)
  - Detail screen shows both start and end timestamps for clarity

- **Concurrent modifications**: What happens if user deletes a ride while database operation is in progress?
  - Use Flow from Room to reactively update list
  - If ride deleted during detail view, gracefully close detail screen
  - Show transient toast "Ride deleted"

- **Statistics precision**: What happens when distance is 0.0 km (stationary ride)?
  - Display "0.0 km" normally (no special handling)
  - Average speed calculation: 0 distance / duration = 0.0 km/h (no division by zero because we use distance as numerator)

- **Sort stability**: What happens when multiple rides have identical values for sort criterion (e.g., same distance)?
  - Use start time as secondary sort for stable ordering
  - Example: If sorting by distance, rides with equal distance are ordered by newest first

- **Filter with no results**: What happens when date filter excludes all rides?
  - Display empty state with message "No rides found in this date range" and button "Clear Filter"
  - Maintain filter selection (don't auto-clear) so user can adjust

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display History tab in bottom navigation with icon and label
- **FR-002**: System MUST query all saved rides from Room database when History tab is opened
- **FR-003**: System MUST display rides in reverse chronological order by default (newest start time first)
- **FR-004**: System MUST show each ride item with: ride name, date (e.g., "Nov 4, 2025"), duration (HH:MM:SS), distance, and average speed
- **FR-005**: System MUST display empty state when no rides exist with helpful message and guidance
- **FR-006**: System MUST use lazy/efficient list rendering for performance with large datasets (LazyColumn provides built-in item windowing - only visible items are composed and rendered)
- **FR-007**: System MUST navigate to Ride Detail screen when user taps ride item
- **FR-008**: System MUST display complete ride details on Ride Detail screen including start/end times, all statistics, and pause information (total paused time, pause count)
- **FR-009**: System MUST show placeholder message for future route map visualization on detail screen
- **FR-010**: System MUST provide sort functionality with options: Newest First, Oldest First, Longest Distance, Longest Duration
- **FR-011**: System MUST persist sort preference when user leaves and returns to History tab
- **FR-012**: System MUST provide swipe-to-delete gesture on ride items revealing delete action
- **FR-013**: System MUST show confirmation dialog before deleting ride with clear warning about permanence
- **FR-014**: System MUST cascade delete all track points when parent ride is deleted (database constraint)
- **FR-015**: System MUST provide delete option in Ride Detail screen toolbar
- **FR-016**: System MUST provide date range filter with preset options: All Time, Last 7 Days, Last 30 Days, This Year, Custom Range
- **FR-017**: System MUST show date picker for custom range filter allowing start and end date selection
- **FR-018**: System MUST show badge or indicator on filter button when filter is active (not "All Time")
- **FR-019**: System MUST respect user's preferred units (Metric or Imperial) for all displayed statistics
- **FR-020**: System MUST truncate long ride names in list view with ellipsis (max 2 lines)
- **FR-021**: System MUST reactively update list when rides are deleted using Flow emissions from database
- **FR-022**: System MUST show transient toast message "Ride deleted" after successful deletion
- **FR-023**: System MUST use stable secondary sort by start time when primary sort values are equal
- **FR-024**: System MUST show appropriate empty state when active filter produces no results with "Clear Filter" action

### Key Entities *(include if feature involves data)*

- **Ride**: Existing entity from F1A (v0.3.0). Contains ride ID, name, start/end timestamps, duration (seconds), distance (meters), average speed (m/s), max speed (m/s), total paused time (seconds), and pause count. One-to-many relationship with TrackPoints.

- **RideListItem**: Display model derived from Ride entity for list view. Includes formatted strings for date, duration, distance, and speeds based on user's unit preferences. Not persisted to database.

- **RideDetailData**: Display model derived from Ride entity for detail screen. Includes all statistics from Ride plus formatted timestamps and pause information. Not persisted to database.

- **SortPreference**: User preference for ride list sort order. Enum values: NEWEST_FIRST, OLDEST_FIRST, LONGEST_DISTANCE, LONGEST_DURATION. Persisted to DataStore.

- **DateRangeFilter**: User preference for ride list date filtering. Contains filter type (ALL_TIME, LAST_7_DAYS, LAST_30_DAYS, THIS_YEAR, CUSTOM) and optional custom start/end dates. Persisted to DataStore.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: User can navigate to History tab and see list of all saved rides within 1 second of tab selection
- **SC-002**: User can tap any ride and navigate to detail screen within 500ms
- **SC-003**: List scrolling maintains 60fps with up to 100 rides loaded
- **SC-004**: Sort operations complete and reorder list within 500ms
- **SC-005**: Filter operations complete and update list within 500ms
- **SC-006**: Swipe-to-delete gesture reveals delete action within 200ms of swipe completion
- **SC-007**: Ride deletion (after confirmation) completes and updates list within 1 second
- **SC-008**: All displayed statistics correctly reflect user's unit preference (Metric/Imperial) with 100% accuracy
- **SC-009**: Empty state displays within 500ms when no rides exist
- **SC-010**: 80%+ test coverage achieved for ViewModels, UseCases, Repositories (per Constitution)
- **SC-011**: Back navigation from detail screen returns to list at same scroll position 100% of the time
- **SC-012**: Filter badge appears/disappears correctly when filter is applied/cleared with 100% accuracy
