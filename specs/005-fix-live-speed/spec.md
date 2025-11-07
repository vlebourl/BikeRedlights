# Feature Specification: Fix Live Current Speed Display Bug

**Feature Branch**: `005-fix-live-speed`
**Created**: 2025-11-07
**Status**: Draft
**Input**: User description: "Fix Live Current Speed bug"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See Real-Time Speed While Riding (Priority: P1)

As a cyclist using BikeRedlights, I need to see my current speed displayed in real-time on the Live tab during a ride so that I can monitor my speed for safety purposes (red light awareness) and pacing.

**Why this priority**: This is the core safety feature of the app. The Live tab's primary purpose is real-time speed monitoring for red light warnings. Without working speed display, the app fails its core mission. This is blocking all speed-dependent features and creates a confusing user experience where other metrics (max speed, average speed) update but current speed remains frozen at 0.0.

**Independent Test**: Can be fully tested by starting a ride, moving (walking/cycling), and verifying that the current speed value updates in real-time on the Live tab. Delivers immediate value by making the Live tab functional and useful.

**Acceptance Scenarios**:

1. **Given** a ride is not recording, **When** user views Live tab, **Then** current speed displays 0.0 (no GPS data)
2. **Given** a ride is actively recording and user is stationary, **When** user views Live tab, **Then** current speed displays 0.0 or very low speed (< 1 km/h)
3. **Given** a ride is actively recording and user is moving at 15 km/h, **When** user views Live tab, **Then** current speed displays approximately 15 km/h (within GPS accuracy tolerance)
4. **Given** a ride is actively recording and user accelerates from 10 km/h to 20 km/h, **When** user views Live tab, **Then** current speed updates dynamically to reflect the acceleration
5. **Given** a ride is actively recording and user decelerates to a stop, **When** user views Live tab, **Then** current speed decreases and eventually shows 0.0 when stopped
6. **Given** a ride is paused (auto-pause or manual pause), **When** user views Live tab, **Then** current speed displays 0.0 (no tracking during pause)
7. **Given** user has selected Imperial units in settings, **When** viewing current speed during a ride, **Then** speed is displayed in mph instead of km/h

---

### Edge Cases

- What happens when GPS signal is lost temporarily? (Speed should freeze at last known value or display "No GPS" indicator if signal loss exceeds threshold)
- How does the system handle GPS accuracy variations? (Speed should smooth out minor GPS jitter, not display erratic jumps)
- What happens if location permissions are revoked mid-ride? (Speed should display last known value and show permission error)
- How does speed display behave during ride state transitions (start → pause → resume → stop)? (Speed should reset to 0.0 during paused state, resume updating when active)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST capture current speed from GPS location updates during active ride recording
- **FR-002**: System MUST store current speed value in a reactive state mechanism accessible to the UI layer
- **FR-003**: System MUST update current speed display in real-time (within GPS update interval: 1-4 seconds depending on accuracy setting)
- **FR-004**: System MUST display current speed in the user's preferred units (metric: km/h, imperial: mph) based on settings
- **FR-005**: System MUST display 0.0 as current speed when no ride is recording
- **FR-006**: System MUST display 0.0 as current speed when ride is paused (manual or auto-pause)
- **FR-007**: System MUST continue updating current speed display throughout ride state transitions (active recording only)
- **FR-008**: System MUST reset current speed to 0.0 when ride recording stops
- **FR-009**: System MUST handle GPS accuracy variations by displaying speed within reasonable tolerance (±2 km/h for normal GPS conditions)
- **FR-010**: System MUST persist current speed state across configuration changes (screen rotation, app backgrounding during ride)

### Key Entities

- **Current Speed**: Real-time speed value derived from GPS location updates, measured in meters per second (m/s) internally, displayed in km/h or mph based on user preference
  - Sourced from: Latest GPS location update during active ride recording
  - Updated frequency: Every GPS location update (1-4 seconds based on accuracy setting)
  - Reset conditions: Ride pause, ride stop, no ride recording
  - Related to: Ride recording state (active/paused/stopped), GPS accuracy settings, units preference

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Current speed displays real-time values (not 0.0) within 5 seconds of starting a ride and beginning movement
- **SC-002**: Current speed updates are visible to users within 1-4 seconds of speed changes (based on GPS accuracy setting: High Accuracy = 1s, Battery Saver = 4s)
- **SC-003**: Current speed accuracy is within ±10% of actual speed for movements between 5-50 km/h under normal GPS conditions
- **SC-004**: Current speed correctly displays 0.0 when stationary (speed < 1 km/h) or when ride is paused/stopped
- **SC-005**: Current speed survives configuration changes (screen rotation, app backgrounding) without resetting to 0.0 during active rides
- **SC-006**: Users can monitor their speed in real-time during rides without the display being frozen or incorrect

## Assumptions

- GPS location updates are already being captured by the ride recording service
- The existing architecture (Service → Repository → ViewModel → UI) supports reactive state flow using StateFlow or similar mechanisms
- The `RideStatistics` component is already designed to accept and display current speed; it just needs the data source connected
- GPS accuracy settings (High Accuracy: 1s updates, Battery Saver: 4s updates) are already implemented and functional
- Unit conversion utilities (km/h ↔ mph) are already available in the codebase
- The bug is purely a data plumbing issue (missing state flow), not a GPS capture problem (since max/average speeds work correctly)

## Dependencies

- Existing ride recording service must be capturing GPS location updates with speed data
- Existing state management infrastructure (StateFlow/DataStore) must be available
- Existing units preference system must be functional
- Existing `RideStatistics` UI component must support current speed parameter

## Scope

### In Scope

- Capturing current speed from GPS location updates
- Storing current speed in reactive state (StateFlow)
- Exposing current speed through ViewModel
- Displaying current speed in Live tab UI
- Handling current speed during ride state transitions (start/pause/resume/stop)
- Unit conversion (km/h ↔ mph) based on user preference
- Persisting current speed across configuration changes

### Out of Scope

- Speed smoothing algorithms or advanced GPS filtering (beyond basic tolerance)
- Historical speed tracking or speed graphs (separate feature)
- Speed-based alerts or notifications (separate feature: red light warnings)
- Offline speed calculation without GPS (not supported)
- Changing GPS update intervals or accuracy settings (already implemented in Feature 2A)
- UI redesign or layout changes to the Live tab (separate enhancement in TODO.md)
