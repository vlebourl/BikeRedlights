# Feature Specification: Fix Auto-Resume After Auto-Pause

**Feature Branch**: `004-fix-auto-resume`
**Created**: 2025-11-07
**Status**: Draft
**Input**: User description: "Bug fix: Auto-Resume Not Working (P0 - Critical)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic Resume After Stop (Priority: P1)

As a cyclist, when I stop at a red light or stop sign and the app auto-pauses my ride, I want recording to automatically resume when I start cycling again so that I don't have to manually interact with my phone while riding.

**Why this priority**: This is a critical safety issue. Requiring manual phone interaction while cycling violates the core safety mission of the app and creates a dangerous distraction. Auto-resume is the complement to auto-pause - without it, the auto-pause feature becomes a liability rather than a benefit.

**Independent Test**: Can be fully tested by enabling auto-pause in settings, starting a ride, simulating GPS movement that drops below 1 km/h for the threshold duration (auto-pause triggers), then simulating movement above 1 km/h (auto-resume should trigger automatically). Deliverable: Ride recording resumes without user interaction.

**Acceptance Scenarios**:

1. **Given** auto-pause is enabled and ride has auto-paused due to low speed, **When** GPS detects speed > 1 km/h, **Then** recording automatically resumes within 2 seconds and state changes from "Auto-Paused" to "Recording"
2. **Given** ride has auto-resumed after auto-pause, **When** viewing ride statistics, **Then** auto-paused duration is correctly excluded from total ride duration
3. **Given** ride has auto-resumed, **When** user views notification, **Then** notification text updates from "Auto-Paused" to "Recording..." with current duration and distance
4. **Given** ride has auto-resumed, **When** checking Live tab UI, **Then** "Auto-Paused" indicator disappears and normal recording UI shows with real-time stats updating
5. **Given** auto-pause is disabled in settings, **When** ride speed drops below 1 km/h, **Then** no auto-pause occurs and auto-resume logic is never invoked (feature toggle works correctly)

---

### User Story 2 - Grace Period Respects Movement (Priority: P2)

As a cyclist, when I manually pause my ride (e.g., to check directions), I want the 30-second grace period to prevent auto-pause from immediately triggering after I resume, but I still want auto-resume to work normally if I do get auto-paused later.

**Why this priority**: The grace period is an anti-flicker mechanism to prevent auto-pause from triggering immediately after manual resume. However, the grace period must not interfere with auto-resume functionality. This ensures both features work harmoniously.

**Independent Test**: Can be fully tested by manually pausing a ride, manually resuming, waiting < 30 seconds, stopping for the auto-pause threshold, then moving again. Auto-resume should still work even though manual resume occurred recently.

**Acceptance Scenarios**:

1. **Given** user manually resumes a ride, **When** < 30 seconds have passed and speed drops below threshold for auto-pause duration, **Then** auto-pause is suppressed (grace period working)
2. **Given** grace period is active and ride gets auto-paused anyway (edge case), **When** speed > 1 km/h, **Then** auto-resume still triggers correctly despite grace period
3. **Given** user manually resumes a ride, **When** > 30 seconds have passed, speed drops for auto-pause duration, then speed increases, **Then** auto-pause and auto-resume work normally (grace period expired)
4. **Given** multiple auto-pause/auto-resume cycles occur during a ride, **When** viewing final statistics, **Then** total auto-paused duration accumulates correctly across all cycles

---

### User Story 3 - GPS Accuracy Modes Work with Auto-Resume (Priority: P3)

As a cyclist using Battery Saver GPS mode (4-second updates), I want auto-resume to work reliably even with less frequent GPS updates so that I get battery efficiency without sacrificing auto-resume functionality.

**Why this priority**: Users may prefer Battery Saver mode for longer rides. Auto-resume must work correctly regardless of GPS update frequency to ensure feature parity across accuracy modes.

**Independent Test**: Can be fully tested by setting GPS accuracy to Battery Saver, starting a ride with auto-pause enabled, stopping to trigger auto-pause, then moving again. Auto-resume should trigger within expected latency (< 8 seconds = 2 GPS updates).

**Acceptance Scenarios**:

1. **Given** GPS accuracy set to High Accuracy (1s updates) and ride is auto-paused, **When** speed > 1 km/h, **Then** auto-resume triggers within 2 seconds (2 GPS updates)
2. **Given** GPS accuracy set to Battery Saver (4s updates) and ride is auto-paused, **When** speed > 1 km/h, **Then** auto-resume triggers within 8 seconds (2 GPS updates)
3. **Given** ride is auto-paused, **When** GPS signal is temporarily lost (indoors, tunnel), **Then** auto-resume does not false-trigger on location timeout (requires actual speed > 1 km/h from valid GPS reading)
4. **Given** ride is auto-paused, **When** GPS accuracy is poor (> 50m), **Then** auto-resume still triggers if speed calculation indicates movement > 1 km/h (accuracy threshold for resume should match recording threshold)

---

### Edge Cases

- **Rapid movement fluctuations**: What happens if speed oscillates around 1 km/h threshold (0.9, 1.1, 0.8, 1.2)?
  - Use hysteresis: Auto-pause requires speed < 1 km/h for full threshold duration (e.g., 5 seconds)
  - Auto-resume requires speed > 1 km/h for at least 2 consecutive GPS readings to prevent false triggers
  - This prevents "flapping" between paused and recording states

- **GPS drift while stationary**: What happens if stationary phone reports 0.5-2 km/h due to GPS noise?
  - Use speed threshold with buffer: Auto-resume requires speed > 1.5 km/h to provide hysteresis margin
  - Alternatively: Require movement distance > 10 meters over 2 GPS readings to confirm actual movement
  - Prevents false auto-resume from GPS drift

- **User manually resumes during auto-pause**: What happens if user taps Resume button while auto-paused?
  - Manual resume takes precedence immediately (don't wait for auto-resume)
  - Start grace period timer (30s) to prevent immediate re-auto-pause
  - Auto-resume logic is bypassed for this resume event

- **Auto-pause threshold changes mid-ride**: What happens if user changes auto-pause threshold in settings while auto-paused?
  - New threshold applies to future auto-pause detection only
  - Current auto-pause state is unaffected (don't retroactively re-evaluate)
  - Auto-resume behavior is unaffected by threshold changes (always triggers on speed > 1 km/h)

- **Multiple riders using same device**: What happens if device is passed between cyclists mid-ride?
  - Auto-resume works identically regardless of rider (feature is device-centric, not user-centric)
  - Each auto-pause/resume cycle is recorded as separate track point states in database

- **App backgrounded during auto-pause**: What happens if app is backgrounded while auto-paused and user starts moving?
  - Foreground service continues monitoring GPS updates
  - Auto-resume triggers normally even in background
  - Notification updates from "Auto-Paused" to "Recording..."
  - When user returns to app, UI reflects resumed state

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST automatically resume recording when speed > 1 km/h after auto-pause has been triggered
- **FR-002**: System MUST transition ride state from "Auto-Paused" to "Recording" within 2 seconds of detecting speed above resume threshold (High Accuracy mode) or within 8 seconds (Battery Saver mode)
- **FR-003**: Auto-resume MUST work correctly regardless of GPS accuracy setting (High Accuracy = 1s updates, Battery Saver = 4s updates)
- **FR-004**: Auto-resume MUST update notification text from "Auto-Paused" to "Recording..." when resuming
- **FR-005**: Auto-resume MUST update Live tab UI to remove "Auto-Paused" indicator and resume real-time stats updates
- **FR-006**: System MUST use hysteresis to prevent rapid auto-pause/auto-resume cycles due to GPS noise or speed oscillation around 1 km/h threshold
- **FR-007**: Auto-resume MUST NOT trigger on GPS drift while stationary (require actual movement confirmation via consecutive GPS readings or distance threshold)
- **FR-008**: Manual resume button MUST take precedence over auto-resume (if user manually resumes, bypass auto-resume logic for that event)
- **FR-009**: Grace period (30 seconds after manual resume) MUST NOT prevent auto-resume from working if auto-pause occurs during grace period
- **FR-010**: Auto-resume MUST correctly accumulate auto-paused duration across multiple auto-pause/auto-resume cycles in a single ride
- **FR-011**: Auto-resume logic MUST NOT interfere with manual pause/resume functionality
- **FR-012**: System MUST log auto-resume events for debugging and testing verification (include timestamp, speed, GPS accuracy in logs)

### Key Entities *(include if feature involves data)*

- **TrackPoint** (existing): GPS coordinate points with pause state flags
  - `isAutoPaused: Boolean` - Indicates this point was captured during auto-pause
  - Auto-resume creates new TrackPoint with `isAutoPaused = false` when recording resumes
  - No schema changes required for auto-resume (existing fields support this functionality)

- **Ride** (existing): Aggregate ride statistics
  - `autoPausedDurationMillis: Long` - Total time in auto-paused state (accumulated across cycles)
  - Auto-resume stops accumulating auto-pause duration when recording resumes
  - No schema changes required

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cyclists can stop at intersections and automatically resume recording without touching their phone 100% of the time when speed increases above 1 km/h
- **SC-002**: Auto-resume triggers within 2 seconds on High Accuracy GPS mode and within 8 seconds on Battery Saver mode in 95% of test cases
- **SC-003**: GPS drift while stationary does not cause false auto-resume in > 99% of stationary periods (validated via 100+ stationary test cases)
- **SC-004**: Auto-pause/auto-resume feature reduces manual phone interactions during rides by 90% compared to manual-only pause/resume
- **SC-005**: Zero user reports of "auto-resume not working" after patch release (v0.4.1) in first 30 days
- **SC-006**: Auto-paused duration calculations are accurate within 1 second across 100 test rides with multiple auto-pause/resume cycles
- **SC-007**: Physical device testing with real-world cycling shows auto-resume success rate > 95% across 20+ stop-and-go scenarios (traffic lights, stop signs, congestion)

### Testing Validation

- **TC-001**: Emulator testing with GPS route playback simulating stop-and-go traffic (10 stops, 10 resumes)
- **TC-002**: Physical device testing on real bike rides with auto-pause enabled (minimum 5 rides with documented auto-pause/resume events)
- **TC-003**: Edge case testing: rapid speed oscillation, GPS signal loss/recovery, poor GPS accuracy scenarios
- **TC-004**: Regression testing: Ensure manual pause/resume still works correctly and grace period functions as designed

## Out of Scope

- Changing auto-pause threshold values (5-60 seconds range remains unchanged)
- Changing auto-pause speed threshold (< 1 km/h trigger remains unchanged)
- Changing auto-resume speed threshold (> 1 km/h trigger is the logical complement)
- UI changes beyond fixing the broken auto-resume functionality (no new UI elements)
- Map visualization or route display features
- Alternative pause/resume strategies (e.g., accelerometer-based detection)

## Assumptions

- Auto-pause feature is already implemented and working correctly (confirmed in v0.3.0 release)
- Auto-resume code exists in `RideRecordingService.kt:530-565` but has a bug preventing it from triggering
- GPS location updates continue during auto-pause (foreground service remains active)
- Users have auto-pause enabled in settings (feature is opt-in via toggle)
- Auto-pause threshold is reasonable (5-60 seconds range provides enough time to confirm stationary state)
- Speed calculation from GPS is sufficiently accurate for movement detection (HasSpeed() flag from Android Location API)

## Dependencies

- Feature F2A (Settings Infrastructure): Auto-pause toggle and threshold settings must be functional
- Feature F1A (Core Ride Recording): Ride recording service and GPS tracking must be operational
- Android Location Services: Fused Location Provider API must be accessible and permissions granted
- Room Database: TrackPoint and Ride entities with pause state fields must exist

## Known Constraints

- GPS update frequency limits auto-resume detection latency (Battery Saver mode has 4-second intervals)
- GPS accuracy affects movement detection reliability (poor signal may delay auto-resume)
- Android doze mode may delay GPS updates if device enters deep sleep during auto-pause (foreground service mitigates this)
- Hysteresis requirements may cause perceived latency in auto-resume (trade-off for stability vs responsiveness)

## Risks

- **High Risk**: Auto-resume still doesn't work after fix due to misidentified root cause
  - Mitigation: Comprehensive debugging with physical device and GPS logging before implementing fix
  - Mitigation: Incremental testing with logging at each state transition to isolate exact failure point

- **Medium Risk**: Auto-resume introduces new bugs in manual pause/resume workflow
  - Mitigation: Extensive regression testing of manual pause/resume before release
  - Mitigation: Keep auto-resume logic separate from manual resume logic (separate code paths)

- **Medium Risk**: GPS noise causes auto-resume flapping in edge cases
  - Mitigation: Implement hysteresis (require 2 consecutive readings > 1 km/h or distance > 10m)
  - Mitigation: Physical device testing in various GPS conditions (urban canyon, open road, forest)

- **Low Risk**: Different GPS accuracy modes behave inconsistently
  - Mitigation: Test both High Accuracy and Battery Saver modes with identical scenarios
  - Mitigation: Document expected latency differences in user-facing documentation

## Success Metrics Post-Release

- Zero "auto-resume not working" bug reports in first 30 days after v0.4.1 release
- User reviews mentioning auto-pause/resume feature with positive sentiment (target: > 80% positive)
- Telemetry data showing auto-resume success rate > 95% in production (if telemetry implemented)
- Support ticket volume related to pause/resume functionality decreases by 50% compared to v0.4.0
