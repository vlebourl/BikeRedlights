# Feature Specification: Core Ride Recording

**Feature Branch**: `002-core-ride-recording`
**Created**: 2025-11-04
**Status**: Draft
**Input**: User description: "feature f1A"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Start and Stop Recording a Ride (Priority: P1)

As a cyclist, I want to manually start and stop recording my rides so that I can capture accurate GPS tracking data for my cycling sessions.

**Why this priority**: This is the foundational capability of the entire feature. Without the ability to record a ride, no other functionality is possible. This represents the core value proposition.

**Independent Test**: Can be fully tested by tapping "Start Ride" button, waiting 30 seconds, then tapping "Stop Ride". Delivers complete start-to-stop recording flow with save/discard dialog.

**Acceptance Scenarios**:

1. **Given** the app is on the Live tab in idle state, **When** user taps "Start Ride" button, **Then** recording begins, duration counter starts at 00:00:00, and button changes to "Stop Ride"
2. **Given** a ride is currently recording, **When** user taps "Stop Ride" button, **Then** a dialog appears with options "Discard" and "Save"
3. **Given** the save/discard dialog is shown, **When** user taps "Save", **Then** ride data is persisted to database and user navigates to Review screen
4. **Given** the save/discard dialog is shown, **When** user taps "Discard", **Then** ride data is deleted and user returns to Live tab in idle state
5. **Given** a ride is recording, **When** user backgrounds the app or locks screen, **Then** recording continues via foreground service with persistent notification visible

---

### User Story 2 - View Live Ride Statistics (Priority: P2)

As a cyclist, I want to see real-time statistics during my ride (duration, distance, current speed, average speed, max speed) so that I can monitor my performance while cycling.

**Why this priority**: Real-time feedback is essential for cyclists to track their performance. This is the primary user interface during active rides.

**Independent Test**: Can be fully tested by starting a ride and simulating GPS movement on emulator. Stats update every second and display correctly in user's preferred units.

**Acceptance Scenarios**:

1. **Given** a ride is recording, **When** GPS locations are received, **Then** duration counts up in HH:MM:SS format (e.g., "00:12:34")
2. **Given** a ride is recording with GPS movement, **When** distance is calculated, **Then** distance displays in user's preferred units (km or miles) with one decimal place (e.g., "2.5 km")
3. **Given** a ride is recording with GPS movement, **When** current speed updates, **Then** current speed displays in user's preferred units (km/h or mph) with one decimal place (e.g., "18.3 km/h")
4. **Given** a ride has average speed calculated, **When** viewing stats, **Then** average speed = total distance / total duration displays correctly
5. **Given** a ride is recording with speed < 1 km/h, **When** stationary detection activates, **Then** current speed shows "0.0 km/h"
6. **Given** no ride is recording (idle state), **When** viewing Live tab, **Then** all stats display zero ("00:00:00", "0.0 km", "0.0 km/h")

---

### User Story 3 - Review Completed Ride Statistics (Priority: P3)

As a cyclist, I want to review my ride statistics immediately after saving a ride so that I can see my total performance metrics.

**Why this priority**: Post-ride review provides immediate feedback and satisfaction. While important, it's dependent on P1 and P2 being complete.

**Independent Test**: Can be fully tested by completing a ride, tapping Save, and verifying Review screen shows correct statistics. Back button returns to Live tab.

**Acceptance Scenarios**:

1. **Given** user taps "Save" on post-ride dialog, **When** Review screen loads, **Then** screen displays total duration, total distance, average speed, and max speed
2. **Given** Review screen is displayed, **When** user views statistics, **Then** all values use user's preferred units from settings (Metric or Imperial)
3. **Given** Review screen is displayed, **When** user taps back button, **Then** user returns to Live tab in idle state
4. **Given** Review screen is displayed, **When** looking for map, **Then** a placeholder message "Map visualization coming in v0.4.0" is shown (maps deferred to F1B)

---

### User Story 4 - Recording Continues in Background (Priority: P1)

As a cyclist, I want my ride recording to continue when my phone screen is locked or I switch to another app so that I don't lose tracking data during my ride.

**Why this priority**: Critical for safety and usability. Cyclists need to lock their phones while riding, and may need to use navigation or other apps.

**Independent Test**: Can be fully tested by starting a ride, backgrounding app for 2 minutes, then resuming. Ride should show accumulated time and data.

**Acceptance Scenarios**:

1. **Given** a ride is recording, **When** user locks screen, **Then** foreground service continues GPS tracking and notification remains visible
2. **Given** a ride is recording, **When** user switches to another app, **Then** GPS tracking continues and duration keeps incrementing
3. **Given** a ride is recording in background, **When** user views notification, **Then** notification shows "Recording Ride • [duration] • [distance]" with real-time updates
4. **Given** a ride is recording in background, **When** user taps notification, **Then** app opens to Live tab showing current ride stats
5. **Given** a ride is recording in background, **When** user taps "Stop Ride" in notification, **Then** recording stops and save/discard dialog appears

---

### User Story 5 - Settings Integration (Priority: P2)

As a cyclist, I want my ride statistics to respect my preferred units (Metric/Imperial) and GPS accuracy settings so that data is displayed in my preferred format and battery usage matches my needs.

**Why this priority**: Leverages existing F2A infrastructure and ensures consistent user experience. Important for usability but dependent on core recording functionality.

**Independent Test**: Can be fully tested by changing settings mid-ride and verifying stats update to new units without losing data.

**Acceptance Scenarios**:

1. **Given** user has selected Metric in settings, **When** viewing ride stats, **Then** speeds display in km/h and distances in km
2. **Given** user has selected Imperial in settings, **When** viewing ride stats, **Then** speeds display in mph and distances in miles
3. **Given** user has selected High Accuracy GPS, **When** ride is recording, **Then** location updates occur every 1 second
4. **Given** user has selected Battery Saver GPS, **When** ride is recording, **Then** location updates occur every 4 seconds
5. **Given** auto-pause is enabled with 5-minute threshold, **When** speed < 1 km/h for 5 minutes, **Then** ride pauses automatically and "Paused" indicator shows
6. **Given** ride is auto-paused, **When** speed > 1 km/h, **Then** ride resumes automatically and paused duration is not included in total duration

---

### User Story 6 - Screen Stays Awake During Recording (Priority: P3)

As a cyclist, I want my phone screen to stay on while the app is in foreground during recording so that I can glance at my stats without unlocking.

**Why this priority**: Convenience feature that enhances usability. Lower priority as cyclists can still use the app without it.

**Independent Test**: Can be fully tested by starting a ride, leaving app in foreground, and verifying screen doesn't auto-lock after device's normal timeout period.

**Acceptance Scenarios**:

1. **Given** a ride is recording and app is in foreground, **When** device's normal screen timeout period passes, **Then** screen remains on
2. **Given** a ride is recording and app is backgrounded, **When** screen lock timeout occurs, **Then** screen locks normally (foreground service continues tracking)
3. **Given** a ride recording is stopped, **When** app remains in foreground, **Then** wake lock is released and screen can auto-lock normally

---

### Edge Cases

- **GPS signal loss**: What happens when user enters tunnel or building during recording?
  - Recording continues without crashing
  - TrackPoints with accuracy > 50m are not inserted (invalid data)
  - "GPS Signal Lost" indicator shows on Live tab
  - When signal returns, tracking resumes and gap will appear in future route visualization

- **Battery drain during long rides**: How does system handle 3+ hour rides with High Accuracy GPS?
  - User can switch to Battery Saver mode mid-ride in settings
  - Service respects new GPS accuracy setting immediately
  - If battery < 15% and High Accuracy enabled, show warning suggesting Battery Saver mode

- **App process death**: What happens if Android kills app process to free memory?
  - Foreground service protects from most process deaths
  - If service dies, Android restarts it automatically
  - Service re-reads current ride from database on restart
  - Brief gap in tracking may occur (acceptable tradeoff)

- **App crash during recording**: What happens if unhandled exception crashes app?
  - Ride remains in database with endTime = null
  - On next app launch, check for incomplete rides
  - Show dialog: "Recover incomplete ride?" [Discard] [Recover]
  - Recover: Mark complete with last TrackPoint timestamp
  - Discard: Delete ride and track points

- **User forgets to stop recording**: What happens if ride is left running for hours after finishing?
  - Notification remains visible as reminder
  - Auto-pause may trigger if stationary for threshold
  - No automatic stop (user control is paramount)
  - Future feature (F3+): Detect prolonged pause and suggest stopping

- **Permissions revoked mid-ride**: What happens if user revokes location permission in system settings during recording?
  - Service detects SecurityException on next location request
  - Stop recording automatically (cannot continue without permission)
  - Show notification: "Recording stopped - Location permission required"
  - Save ride data captured so far

- **Storage full**: What happens if device storage is full and TrackPoints can't be written?
  - Catch SQLiteFullException on insert
  - Show notification: "Storage full - Recording stopped"
  - Save ride with data captured so far
  - Don't crash or lose existing data

- **Rapid start/stop**: What happens if user taps Start then immediately taps Stop?
  - Minimum ride duration: 5 seconds
  - If stopped before 5s, show toast "Ride too short to save"
  - Auto-discard without prompting dialog

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide "Start Ride" button on Live tab when no ride is actively recording
- **FR-002**: System MUST allow user to stop active recording via "Stop Ride" button or notification action
- **FR-003**: System MUST present save/discard dialog after user stops recording
- **FR-004**: System MUST persist saved rides to local Room database with rides and track_points tables
- **FR-005**: System MUST run foreground service during recording that survives screen-off and app backgrounding
- **FR-006**: System MUST display persistent notification during recording with real-time duration and distance
- **FR-007**: System MUST calculate and display duration in HH:MM:SS format with 1-second updates
- **FR-008**: System MUST calculate distance from GPS coordinates using Haversine formula or distance from Location API
- **FR-009**: System MUST calculate average speed as total distance / total duration (excluding paused time)
- **FR-010**: System MUST track maximum speed achieved during ride
- **FR-011**: System MUST display current speed with stationary detection (< 1 km/h shows as 0)
- **FR-012**: System MUST respect user's units preference (Metric or Imperial) from F2A settings
- **FR-013**: System MUST respect user's GPS accuracy preference (High Accuracy = 1s updates, Battery Saver = 4s updates) from F2A settings
- **FR-014**: System MUST implement auto-pause detection when enabled in F2A settings (speed < 1 km/h for threshold minutes)
- **FR-015**: System MUST exclude paused duration from total ride duration
- **FR-016**: System MUST store rides in base units (meters, meters/second) and convert for display based on settings
- **FR-017**: System MUST provide Review screen after saving ride showing total statistics
- **FR-018**: System MUST acquire wake lock when recording in foreground to keep screen on
- **FR-019**: System MUST release wake lock when recording stops or app is backgrounded
- **FR-020**: System MUST handle location permission denial gracefully with rationale dialog (existing from v0.1.0)
- **FR-021**: System MUST request notification permission on Android 13+ for foreground service
- **FR-022**: System MUST not insert TrackPoints with GPS accuracy worse than 50 meters (invalid data)
- **FR-023**: System MUST enforce minimum ride duration of 5 seconds before allowing save
- **FR-024**: System MUST check for incomplete rides on app launch (endTime = null) and offer recovery
- **FR-025**: System MUST cascade delete TrackPoints when parent Ride is deleted

*Clarifications resolved:*

- **FR-026**: System MUST assign default name to rides in format "Ride on [Month Day, Year]" (e.g., "Ride on Nov 4, 2025"). Ride entity includes name field in database, but editing UI deferred to F3 to keep v0.3.0 focused on core recording.
- **FR-027**: System MUST provide manual pause/resume controls via "Pause" button during recording (separate from auto-pause). Supports cafe stops mid-ride without ending ride. Paused state displayed on Live tab and in notification.
- **FR-028**: System has no maximum ride duration limit. User is trusted to stop recording when finished. Persistent notification serves as reminder. Can add telemetry and limit in future release if users frequently forget to stop.

### Key Entities *(include if feature involves data)*

- **Ride**: Represents a single cycling session with name (auto-generated), start/end timestamps, duration, distance, and speed statistics. Each ride has a one-to-many relationship with TrackPoints forming the GPS route. Name field included in database for future editing (F3), defaults to "Ride on [date]" format.

- **TrackPoint**: Represents a single GPS coordinate captured during a ride at a specific timestamp. Includes latitude, longitude, speed, accuracy, and pause state (both manual and auto-pause). Multiple TrackPoints belong to one Ride (foreign key relationship with cascade delete).

- **RideRecordingState**: Runtime state representing the currently active recording session. Includes ride ID, accumulated statistics, manual pause state, auto-pause state, and references to foreground service. Not persisted to database.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: User can start a ride, see duration counting up, and stop recording within 10 seconds of first opening app
- **SC-002**: Recording continues for at least 5 minutes with screen locked without data loss
- **SC-003**: All ride statistics (duration, distance, speeds) calculate correctly within 5% accuracy compared to known GPS test routes
- **SC-004**: Switching between Metric and Imperial units displays converted values correctly within 1% accuracy
- **SC-005**: Auto-pause triggers within 10 seconds after threshold is reached (e.g., 5 minutes stationary)
- **SC-006**: Foreground service survives app backgrounding and screen-off for 30+ minute rides
- **SC-007**: 90%+ test coverage achieved for ViewModels, UseCases, Repositories (per Constitution requirement for safety-critical features)
- **SC-008**: Battery drain < 10% per hour with High Accuracy GPS on test device (Pixel emulator)
- **SC-009**: Database write operations complete in < 100ms on background thread (non-blocking UI)
- **SC-010**: User can stop recording and see Review screen within 1 second of tapping Stop button
- **SC-011**: App handles GPS signal loss gracefully without crashing or corrupting ride data
- **SC-012**: Incomplete ride recovery on app launch correctly restores or discards ride data
