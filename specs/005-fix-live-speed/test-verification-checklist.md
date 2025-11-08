# Feature 005 - Physical Device Test Verification Checklist

## Test Context
**Date Built**: 2025-11-07
**Device**: Pixel 9 Pro (4B271FDAP008DU)
**Branch**: `005-fix-live-speed`
**PR**: #6 (Draft) - https://github.com/vlebourl/BikeRedlights/pull/6

## Features to Verify

### 1. Live Speed Display Bug Fix (P1 - Critical)
**Expected Behavior**: Current speed should display real GPS speed during ride (not hardcoded 0.0 km/h)

**Questions to Ask**:
- Did the current speed update in real-time during your ride?
- Did it show accurate speeds (compared to bike computer or other GPS app)?
- What was the approximate speed range displayed? (e.g., 0-25 km/h)
- Did speed reset to 0.0 when you stopped or paused?

**Known Limitation**: Uses GPS Doppler speed (requires GPS satellite lock, ~30-60 seconds after starting ride)

---

### 2. Hero Metric UI Layout (P2 - UX Enhancement)
**Expected Behavior**: Current speed should be the largest, most prominent metric on screen

**Questions to Ask**:
- Was current speed easy to read at a glance while riding?
- Did the typography hierarchy make sense? (Speed > Duration/Distance > Avg/Max > Paused/Immobile)
- Was any information missing that you expected to see?
- Was the layout cluttered or confusing?

**Layout Structure**:
```
Row 1: Current Speed (57sp, bold, primary color) - HERO
Row 2: Duration | Distance (28sp) - SECONDARY
Row 3: Average | Max (22sp) - SUPPORTING
Row 4: Paused | Immobile (22sp, de-emphasized) - INFORMATIONAL
```

---

### 3. Paused Time Display
**Expected Behavior**: "Paused" row should show total paused time (manual + auto-pause combined)

**Questions to Ask**:
- Did you manually pause during the ride? If yes, did paused time increment?
- Did auto-pause trigger (speed < 0.5 m/s for 3+ minutes)? If yes, did paused time increment?
- Was the paused time accurate (compared to actual pause duration)?
- After resuming, did paused time stop incrementing?

---

### 4. Immobile Time Placeholder
**Expected Behavior**: "Immobile" should show "00:00:00" (de-emphasized, grayed out)

**Questions to Ask**:
- Did you notice the "Immobile" label?
- Was it obvious this is a placeholder (not yet functional)?
- Did the de-emphasized styling (50% alpha) work well, or was it too subtle/confusing?

---

## General Questions

### GPS & Location
- How long did it take to get GPS lock after starting the ride?
- Did GPS signal drop at any point during the ride?
- Were you riding in urban/suburban/rural areas? (Buildings can affect GPS)

### Performance & Stability
- Did the app crash or freeze at any point?
- Was battery drain acceptable during the ride?
- Did the service notification work correctly (showing ride in progress)?

### Pause/Resume Behavior
- Did manual pause/resume work correctly?
- Did auto-pause trigger when expected?
- Did auto-resume trigger when you started moving again?
- Were there any unexpected pauses or resumes?

### Overall Experience
- Did the new UI layout improve your riding experience?
- Is there anything you'd change about the current speed display?
- Did you encounter any bugs or unexpected behavior?

---

## Next Steps Based on Test Results

### If Test Passed ✅
1. Update TODO.md: Mark feature as tested
2. Update PR #6: Add test observations to description
3. Mark PR ready for review (remove draft status)
4. Merge to main
5. Release v0.4.2 (or v0.5.0 if we consider UI changes a minor bump)

### If Issues Found ❌
1. Document specific issues
2. Prioritize by severity (P0/P1/P2)
3. Create fixes in same branch
4. Rebuild APK and re-test
5. Repeat until all P0/P1 issues resolved

---

## Technical Notes for Next Session

**Commits in PR #6** (total 12):
- feba18f: docs - specification
- ecdf7a7: feat(data) - StateFlow implementation
- ccc7c08: feat(service) - GPS speed emission
- be34815: feat(viewmodel) - StateFlow exposure
- 7bdc739: fix(ui) - wire to LiveRideScreen
- 24bd901: docs - emulator limitation
- c240657: docs - TODO update (draft PR)
- 16df838: feat(ui) - hero metric prioritization
- 7709f48: docs - TODO update (UI enhancement)
- 117a052: feat(ui) - paused time display
- 8092ee6: feat(ui) - immobile placeholder
- b42cf77: docs - TODO update (paused + immobile)

**APK Installed**: `app/build/outputs/apk/debug/app-debug.apk` (built 2025-11-07)

**Data Flow**: GPS → RideRecordingService → RideRecordingStateRepository → RideRecordingViewModel → LiveRideScreen → RideStatistics

**Background Monitoring**: Logcat monitoring may still be running (shell 7be693)
