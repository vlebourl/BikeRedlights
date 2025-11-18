# Next Feature Prompt for /speckit.specify

> **Purpose**: Prompt for Feature 008 - Red Light Detection & Immobile Time Tracking
> **Created**: 2025-11-18
> **Target Version**: v0.8.0 (MINOR - new safety-critical feature)

## 🚦 Feature Description

**Feature 008: Red Light Detection & Immobile Time Tracking**

Implement the core safety feature that BikeRedlights was designed for: detect when a cyclist is stopped at a red light and track immobile time separately from manual/auto-pause time.

This feature addresses the TODO comment in `RideStatistics.kt:line 97`:
```kotlin
text = "00:00:00", // TODO: Track immobile time (stopped at lights while recording)
```

## 📋 User Story

**As a cyclist**, I want the app to automatically detect when I'm stopped at red lights (immobile while ride is actively recording) and track this time separately from pause time, so I can:
1. See how much time I spend waiting at traffic signals during my commute
2. Understand the impact of red lights on my total ride time
3. Distinguish between intentional breaks (pause) and forced stops (red lights)
4. Get insights into my route efficiency and traffic patterns

## 🎯 Key Requirements

### Functional Requirements

1. **Immobile Detection Logic**
   - Detect when speed drops to 0 km/h (or below threshold like 0.5 km/h) while ride is in "Recording" state
   - Distinguish from "Paused" or "AutoPaused" states (those are intentional breaks)
   - Start immobile timer when stopped, stop when movement resumes
   - Handle rapid stop/start cycles (e.g., stop-and-go traffic)

2. **Data Persistence**
   - Store immobile duration in Room database (Ride entity)
   - Track cumulative immobile time across entire ride
   - Preserve immobile time history for ride statistics and analysis

3. **Real-Time Display**
   - Show live immobile counter on LiveRideScreen while stopped at lights
   - Update counter every second (similar to pause counter from Feature 007)
   - Display cumulative immobile time in ride statistics
   - Show immobile time in RideDetail/RideReview screens after ride completion

4. **User Feedback**
   - Visual indicator when immobile (e.g., different color, icon, or label)
   - Clear distinction between:
     - **Paused**: User manually paused ride
     - **Auto-Paused**: User stopped for extended time (configured threshold)
     - **Immobile**: Stopped at red light/traffic while ride is actively recording

### Non-Functional Requirements

1. **Safety-Critical Design**
   - Minimal battery impact (use existing GPS data)
   - No UI changes required while riding (passive tracking)
   - Reliable detection in various scenarios (urban intersections, stop signs, bike lane stops)

2. **Performance**
   - Use existing location updates (no additional GPS polling)
   - Efficient state transitions (stopped ↔ moving)
   - Minimal memory overhead for immobile time tracking

3. **Architecture**
   - Follow MVVM + Clean Architecture pattern
   - Add domain model for immobile state
   - Update RideRecordingState enum if needed (or use internal state machine)
   - Use StateFlow for reactive UI updates

## 🔍 Edge Cases to Consider

1. **Rapid Stop/Start (Stop-and-Go Traffic)**
   - Should each individual stop be tracked separately?
   - Or should stops within X seconds be merged?
   - Recommendation: Track continuously while speed < threshold, reset timer on movement

2. **Speed Threshold for "Immobile"**
   - 0.0 km/h exactly? (GPS drift may prevent exact 0)
   - 0.5 km/h or 1.0 km/h threshold? (more forgiving for GPS noise)
   - Recommendation: Use 1.0 km/h threshold (same as auto-pause detection logic)

3. **Auto-Pause vs. Immobile**
   - What happens if user is immobile for longer than auto-pause threshold?
   - Should immobile time stop tracking when auto-pause triggers?
   - Recommendation: Auto-pause takes priority - immobile time is only tracked while ride is in "Recording" state

4. **Stationary at Start/End of Ride**
   - Should immobile time count before user presses "Start" or after "End"?
   - Recommendation: No - only track while ride is actively recording

5. **Manual Pause While Immobile**
   - User stops at red light, then manually pauses ride
   - Should immobile time continue or stop?
   - Recommendation: Stop immobile tracking when any pause (manual/auto) is triggered

## 🏗️ Suggested Architecture

### Domain Layer Changes

**New/Updated Models**:
```kotlin
// Update Ride entity
data class Ride(
    // ... existing fields ...
    val immobileDurationSeconds: Long = 0  // NEW: time stopped at lights
)

// Potentially add immobile state tracking
enum class RideRecordingState {
    Idle,
    Recording,
    Immobile,      // NEW: stopped at red light (speed < threshold)
    Paused,
    AutoPaused,
    Stopped
}
// OR keep Recording state and use internal flag in service
```

**New Use Cases**:
- `TrackImmobileTimeUseCase` - Update immobile duration when stopped
- OR extend existing `RecordTrackPointUseCase` to handle immobile time

### Data Layer Changes

**Repository Updates**:
```kotlin
interface RideRepository {
    suspend fun updateImmobileDuration(rideId: Long, durationSeconds: Long)
    // ... existing methods ...
}
```

**Room DAO Updates**:
```kotlin
@Query("UPDATE rides SET immobileDurationSeconds = :duration WHERE id = :rideId")
suspend fun updateImmobileDuration(rideId: Long, duration: Long)
```

### Service Layer Changes

**RideRecordingService Updates**:
- Add immobile timer (similar to pause timer from Feature 007)
- Detect speed < threshold while in Recording state
- Emit immobile state via StateFlow
- Update immobile duration in database

### UI Layer Changes

**RideStatistics Component**:
- Replace TODO comment with actual immobile time display
- Use real-time counter (MM:SS format) similar to pause counter
- Add visual distinction (e.g., different color, icon)

**LiveRideScreen**:
- Show immobile status indicator (optional)
- Display live immobile counter when stopped

**RideDetailScreen & RideReviewScreen**:
- Display total immobile time in statistics

## 📊 Data Model Migration

**Room Database Migration Required**:
```kotlin
// Migration from version 1 to version 2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE rides ADD COLUMN immobileDurationSeconds INTEGER NOT NULL DEFAULT 0")
    }
}
```

## 🧪 Testing Strategy

### Unit Tests
- `TrackImmobileTimeUseCase`: Test immobile detection logic
- `RideRepository`: Test immobile duration updates
- `RideRecordingViewModel`: Test immobile StateFlow emissions

### Emulator Testing (MANDATORY)
1. Start ride, simulate GPS movement → verify no immobile time
2. Stop at simulated intersection → verify immobile timer starts
3. Resume movement → verify immobile timer stops, duration persisted
4. Stop at multiple intersections → verify cumulative immobile time
5. Manual pause while immobile → verify immobile tracking stops
6. Auto-pause trigger while immobile → verify auto-pause takes priority

### Physical Device Testing (Recommended)
- Real bike ride through urban area with traffic lights
- Validate immobile detection accuracy vs. actual red light stops
- Compare immobile time with rider's perception of stops

## 🎨 UI/UX Mockup

**RideStatistics Display (Live Tab)**:
```
┌────────────────────────────────────┐
│ Current Speed: 18.5 km/h           │ ← Hero metric (displayLarge)
├────────────────────────────────────┤
│ Duration: 00:15:30  Distance: 5.2km│ ← Secondary row
├────────────────────────────────────┤
│ Avg: 14.2 km/h    Max: 32.1 km/h  │ ← Supporting grid
├────────────────────────────────────┤
│ ⏸️ Paused: 00:02:15 (manual + auto)│ ← Paused time
│ 🚦 Immobile: 00:03:45 (red lights) │ ← NEW: Immobile time
└────────────────────────────────────┘
```

**Visual States**:
- **Moving**: Speed > 1 km/h, no special indicator
- **Immobile**: Speed ≤ 1 km/h while Recording, show 🚦 icon + live counter
- **Paused**: User/auto-paused, show ⏸️ icon + accumulated time
- **Auto-Paused**: Same as Paused, with auto-pause label

## 🚀 Implementation Phases

### Phase 1: Domain Layer (2-3 tasks)
- Update Ride domain model with immobileDurationSeconds
- Add immobile state tracking (enum or internal flag)
- Create/update use cases for immobile time tracking

### Phase 2: Data Layer (3-4 tasks)
- Add Room database migration (v1 → v2)
- Update RideRepository interface and implementation
- Add DAO query for immobile duration updates
- Test migration on emulator

### Phase 3: Service Layer (4-5 tasks)
- Add immobile detection logic to RideRecordingService
- Implement immobile timer (similar to pause counter)
- Emit immobile state via StateFlow
- Update immobile duration in database on state changes
- Handle edge cases (auto-pause, manual pause, stop-and-go)

### Phase 4: ViewModel Layer (2-3 tasks)
- Expose immobile StateFlow in RideRecordingViewModel
- Add real-time immobile counter (1-second updates)
- Wire immobile duration to UI state

### Phase 5: UI Layer (3-4 tasks)
- Replace TODO in RideStatistics with immobile time display
- Add visual indicator for immobile state (icon, color)
- Update RideDetailScreen to show immobile time
- Update RideReviewScreen to show immobile time

### Phase 6: Testing & Validation (5-6 tasks)
- Write unit tests for immobile detection logic
- Emulator testing with GPS simulation
- Physical device testing (real bike ride)
- Edge case validation (stop-and-go, pause transitions)
- Performance profiling (battery impact)

### Phase 7: Documentation & Release (2-3 tasks)
- Update CLAUDE.md with immobile time feature
- Update TODO.md and RELEASE.md
- Create PR, code review, merge
- Version bump to v0.8.0
- Build release APK and create GitHub Release

**Estimated Total Tasks**: 23-28 tasks

## 📚 References

### Existing Code to Review
- `RideStatistics.kt:97` - TODO comment for immobile time
- `RideRecordingService.kt` - Auto-pause detection logic (reuse for immobile)
- `RideRecordingViewModel.kt` - Pause counter implementation (template for immobile counter)
- `AutoPauseConfig.kt` - Threshold validation (similar for immobile threshold)

### Related Features
- **Feature 004**: Auto-resume logic (similar state transitions)
- **Feature 007**: Real-time pause counter (template for immobile counter)
- **Feature 002**: Core ride recording (extend with immobile tracking)

## 🎯 Success Criteria

1. ✅ Immobile time is tracked accurately when stopped at red lights
2. ✅ Immobile counter updates in real-time on LiveRideScreen
3. ✅ Immobile time persists in Room database
4. ✅ Immobile time displays correctly in RideDetail/RideReview screens
5. ✅ Auto-pause takes priority over immobile tracking
6. ✅ Manual pause stops immobile tracking
7. ✅ No battery impact (uses existing GPS data)
8. ✅ Room database migration succeeds without data loss
9. ✅ All unit tests pass (80%+ coverage)
10. ✅ Emulator testing validates all edge cases
11. ✅ Physical device testing confirms accuracy

## 🔒 Safety-Critical Considerations

**This is a safety-critical feature** - accurate immobile detection is important for:
1. **Route safety insights**: Understanding which routes have excessive red light stops
2. **Ride planning**: Choosing routes with fewer traffic signal delays
3. **Safety awareness**: Recognizing high-stop-density areas

**Critical Requirements**:
- No false positives (classifying movement as immobile)
- No false negatives (missing actual red light stops)
- Robust GPS accuracy handling (noise, drift)
- Battery-efficient implementation

## 🎓 Learning Opportunities

This feature provides experience with:
- Room database migrations (v1 → v2)
- Complex state machine logic (Recording → Immobile transitions)
- Real-time counters with lifecycle awareness
- GPS data interpretation (speed threshold detection)
- Safety-critical feature development
- Physical device validation

## 📝 Prompt for /speckit.specify

**Paste this into Claude Code**:

```
/speckit.specify

Feature 008: Red Light Detection & Immobile Time Tracking

Implement automatic detection and tracking of time spent stopped at red lights (immobile time) during active ride recording. This is the core safety feature BikeRedlights was designed for.

**User Story**: As a cyclist, I want the app to detect when I'm stopped at red lights and track this time separately from pause time, so I can understand how much time I spend waiting at traffic signals and get insights into route efficiency.

**Key Requirements**:
1. Detect when speed drops to ≤1 km/h while ride is in "Recording" state
2. Track immobile duration in real-time (updates every second)
3. Persist immobile time in Room database (add new field to Ride entity)
4. Display immobile counter on LiveRideScreen (similar to pause counter from Feature 007)
5. Show total immobile time in RideDetail and RideReview screens
6. Distinguish between Paused (intentional break), Auto-Paused (long stop), and Immobile (red light stop)
7. Auto-pause takes priority: stop immobile tracking when any pause triggers
8. Room database migration required (v1 → v2) to add immobileDurationSeconds field

**Edge Cases**:
- Rapid stop/start (stop-and-go traffic): Track continuously while speed < threshold
- Auto-pause vs. immobile: Auto-pause takes priority, immobile only while Recording
- Manual pause while immobile: Stop immobile tracking when pause triggered
- GPS drift/noise: Use 1 km/h threshold (not exact 0.0) for robustness

**Architecture**:
- Domain: Update Ride model, add immobile tracking logic
- Data: Room migration (v1 → v2), update RideRepository
- Service: Add immobile detection to RideRecordingService (reuse auto-pause logic)
- ViewModel: Add immobile StateFlow with real-time counter
- UI: Replace TODO in RideStatistics.kt:97 with actual immobile display

**Testing**:
- MANDATORY emulator testing with GPS simulation (multiple red light stops)
- Physical device testing recommended (real bike ride in urban area)
- Validate accuracy vs. actual traffic signal stops

**Safety-Critical**: This feature is critical for route safety insights and ride planning. Requires robust GPS accuracy handling and battery-efficient implementation.

**Target Release**: v0.8.0 (MINOR - new feature)
**Estimated Tasks**: 23-28 tasks across 7 phases
**References**:
- RideStatistics.kt:97 (TODO comment)
- Feature 007 (pause counter implementation - template for immobile counter)
- Feature 004 (auto-resume logic - similar state transitions)
```

---

## 🎉 Why This is the Right Next Feature

1. **Core Mission Alignment**: This is THE feature BikeRedlights was named for - detecting red light stops
2. **Clear TODO**: Explicit TODO comment in codebase (RideStatistics.kt:97)
3. **Natural Progression**: Builds on existing pause/auto-pause infrastructure from Features 004 and 007
4. **User Value**: Provides actionable insights for route planning and safety awareness
5. **Technical Learning**: Room migrations, complex state machines, real-time tracking
6. **Safety-Critical**: Demonstrates handling of safety-critical features with robust testing

---

**Last Updated**: 2025-11-18
**Status**: Ready for /speckit.specify
**Recommended Priority**: P1 (Core Feature)
