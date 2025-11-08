# Research: Fix Live Current Speed Display Bug

**Date**: 2025-11-07
**Feature**: [spec.md](./spec.md)
**Branch**: `005-fix-live-speed`

## Executive Summary

Research confirms the bug is a **data plumbing issue**, not a GPS capture problem. GPS speed is already captured and stored in TrackPoint entities but is not exposed to the UI in real-time. The fix requires adding a StateFlow to propagate current speed from Service → Repository → ViewModel → UI following existing architecture patterns.

## Research Findings

### 1. GPS Speed Capture

**Status**: ✅ Already Working

**Location**: `RideRecordingService.kt:407-445, 525-530`

**Current Implementation**:
- GPS speed arrives as `LocationData.speedMps: Float?` from Play Services Location API
- Service already reads and processes speed at line 525-530:
  ```kotlin
  val currentSpeed = if (lastPoint.speedMetersPerSec < 0.278) {
      0.0  // Stationary detection (< 1 km/h)
  } else {
      lastPoint.speedMetersPerSec
  }
  ```
- Speed is stored in `TrackPoint.speedMetersPerSec` for max/average calculations
- **Problem**: Speed is NOT exposed to UI StateFlow

**Decision**: Reuse existing GPS speed capture; add emission to repository StateFlow.

---

### 2. StateFlow Patterns in Existing Architecture

**Status**: ✅ Pattern Identified

**Location**: `RideRecordingStateRepositoryImpl.kt:60-61, 85-87`

**Existing Pattern**:
```kotlin
// Private mutable state (internal updates only)
private val _recordingState = MutableStateFlow<RideRecordingState>(RideRecordingState.Idle)

// Public read-only accessor
override fun getRecordingState(): Flow<RideRecordingState> {
    return _recordingState.asStateFlow()
}
```

**Pattern Benefits**:
- Encapsulation: Internal state is mutable, external observers get read-only Flow
- Reactive: UI updates automatically when state changes
- Thread-safe: StateFlow handles concurrency correctly
- Lifecycle-aware: Survives configuration changes when collected in ViewModel

**Decision**: Add `_currentSpeed = MutableStateFlow(0.0)` with `getCurrentSpeed(): StateFlow<Double>` accessor following this exact pattern.

---

### 3. Configuration Change Handling

**Status**: ✅ Automatic via ViewModel

**ViewModel Survival**: `RideRecordingViewModel.kt:44-51`
- `@HiltViewModel` annotation ensures ViewModel survives screen rotation
- StateFlow automatically retained across configuration changes

**Service State Persistence**: `RideRecordingStateRepositoryImpl.kt:137-176`
- DataStore restoration for ride state after process death
- Current speed is **ephemeral** (not persisted)

**Design Decision**:
- **In-Memory Only**: Current speed should be StateFlow (not DataStore)
  - **Rationale**: Real-time data, resets to 0.0 on service restart
  - **Rationale**: Persisting every GPS update (1-4 per second) is unnecessary overhead
  - **Rationale**: StateFlow provides reactive updates with minimal battery impact
- **Lifecycle**: Speed resets to 0.0 when ride stops or service restarts (expected behavior)

---

### 4. Unit Conversion

**Status**: ✅ Already Implemented

**Location**: `RideRecordingViewModel.kt:378-383`

**Existing Conversion**:
```kotlin
fun convertSpeed(metersPerSec: Double, unitsSystem: UnitsSystem): Double {
    return when (unitsSystem) {
        UnitsSystem.METRIC -> metersPerSec * 3.6      // km/h
        UnitsSystem.IMPERIAL -> metersPerSec * 2.23694  // mph
    }
}
```

**Location 2**: `UnitConversions.kt:39-41, 49-51`
- Conversion factor: `0.621371` for km/h ↔ mph

**Decision**: Reuse `RideRecordingViewModel.convertSpeed()` in UI layer. No new conversion code needed.

---

### 5. Current Ride State Flow (Architecture Trace)

**Service → Repository** (`RideRecordingService.kt:191`):
```kotlin
rideRecordingStateRepository.updateRecordingState(currentState)
```

**Repository → ViewModel** (`RideRecordingViewModel.kt:69-74`):
```kotlin
rideRecordingStateRepository.getRecordingState().collect { recordingState ->
    updateUiStateFromRecordingState(recordingState)
}
```

**ViewModel → UI** (`LiveRideScreen.kt:66`):
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

**Critical Gap** (`LiveRideScreen.kt:350`):
```kotlin
RideStatistics(
    ride = ride,
    currentSpeed = 0.0,  // TODO: Expose current speed from service
    unitsSystem = unitsSystem,
)
```

**Decision**: Follow exact same flow pattern for current speed:
1. Service emits speed to repository
2. Repository exposes StateFlow
3. ViewModel collects and exposes to UI
4. UI collects and passes to RideStatistics composable

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│ GPS Hardware → LocationData.speedMps (already captured)          │
└───────────────────────┬──────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────────┐
│ RideRecordingService.startLocationTracking()                     │
│ - Reads location.speedMps                                        │
│ - Stores in TrackPoint.speedMetersPerSec (✅ working)           │
│ - [NEW] Emits to repository.updateCurrentSpeed(speed)           │
└───────────────────────┬──────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────────┐
│ RideRecordingStateRepository                                     │
│ - [NEW] private val _currentSpeed = MutableStateFlow(0.0)       │
│ - [NEW] fun getCurrentSpeed(): StateFlow<Double>                │
│ - [NEW] suspend fun updateCurrentSpeed(speedMps: Double)        │
└───────────────────────┬──────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────────┐
│ RideRecordingViewModel                                           │
│ - [NEW] val currentSpeed: StateFlow<Double>                     │
│ - [EXISTING] convertSpeed() for unit conversion                 │
└───────────────────────┬──────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────────┐
│ LiveRideScreen                                                   │
│ - [NEW] val currentSpeed by viewModel.currentSpeed.collect()    │
│ - [FIXED] Pass to RideStatistics(currentSpeed = currentSpeed)   │
└───────────────────────┬──────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────────┐
│ RideStatistics Composable                                        │
│ - [NO CHANGE] Already accepts currentSpeed parameter            │
│ - [NO CHANGE] Already converts units for display                │
└──────────────────────────────────────────────────────────────────┘
```

---

## Alternatives Considered

### Alternative 1: Store Current Speed in Ride Entity
**Rejected Because**:
- Would require database write every GPS update (1-4 per second)
- Ride entity represents aggregate statistics, not real-time state
- Unnecessary battery/performance overhead
- StateFlow is the correct tool for ephemeral reactive data

### Alternative 2: Emit Speed Directly from Service to ViewModel
**Rejected Because**:
- Violates Clean Architecture (Service should not know about ViewModel)
- Makes ViewModel untestable (tight coupling to Service)
- Breaks existing pattern (Repository is intermediary layer)
- Would require ViewModel to observe Service directly (anti-pattern)

### Alternative 3: Store Speed in DataStore
**Rejected Because**:
- Persisting every GPS update is unnecessary (speed is ephemeral)
- DataStore reads/writes are async and slower than StateFlow
- Current speed should reset to 0.0 on service restart (expected behavior)
- StateFlow is designed for this exact use case

### Alternative 4: Pass Speed via Intent Broadcast
**Rejected Because**:
- Old Android pattern, not recommended for internal app communication
- StateFlow is more modern, reactive, and lifecycle-aware
- Would require manual registration/unregistration in UI
- Doesn't integrate with Compose lifecycle

---

## Implementation Decisions Summary

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| **State Storage** | StateFlow (in-memory) | Ephemeral real-time data, no persistence needed |
| **Architecture Pattern** | Service → Repository → ViewModel → UI | Follows existing Clean Architecture pattern |
| **GPS Capture** | Reuse existing LocationData.speedMps | Already working, no changes needed |
| **Unit Conversion** | Reuse RideRecordingViewModel.convertSpeed() | Already implemented for max/average speed |
| **Lifecycle** | Speed resets to 0.0 on service restart | Expected behavior for real-time data |
| **Testing Strategy** | Unit tests for Repository/ViewModel, UI tests for LiveRideScreen | Matches existing test patterns |
| **Configuration Changes** | StateFlow survives rotation via ViewModel | Automatic Android behavior |

---

## Files Requiring Modification

### Domain Layer (Interfaces)
1. `domain/repository/RideRecordingStateRepository.kt`
   - Add `getCurrentSpeed(): StateFlow<Double>` method declaration

### Data Layer (Implementation)
2. `data/repository/RideRecordingStateRepositoryImpl.kt`
   - Add `_currentSpeed = MutableStateFlow(0.0)` private state
   - Implement `getCurrentSpeed()` to return `_currentSpeed.asStateFlow()`
   - Add `suspend fun updateCurrentSpeed(speedMps: Double)` internal method

### Service Layer
3. `service/RideRecordingService.kt`
   - In `startLocationTracking()` (line ~430), emit speed updates:
     ```kotlin
     val currentSpeed = (locationData.speedMps ?: 0f).toDouble()
     rideRecordingStateRepository.updateCurrentSpeed(currentSpeed)
     ```

### UI Layer (ViewModel)
4. `ui/viewmodel/RideRecordingViewModel.kt`
   - Expose `currentSpeed: StateFlow<Double>` from repository
   - Use `stateIn()` with `WhileSubscribed(5000)` for lifecycle-aware collection

### UI Layer (Screen)
5. `ui/screens/ride/LiveRideScreen.kt`
   - Collect `currentSpeed` from ViewModel (line ~66)
   - Pass to `RideStatistics()` composable (fix line 350)

### Testing
6. `test/.../RideRecordingStateRepositoryImplTest.kt` (NEW)
   - Test `getCurrentSpeed()` initial value (0.0)
   - Test `updateCurrentSpeed()` updates StateFlow
   - Test multiple rapid updates (simulate GPS)

7. `test/.../RideRecordingViewModelTest.kt` (MODIFY)
   - Test `currentSpeed` StateFlow exposure
   - Test lifecycle behavior (collection/cancellation)

8. `androidTest/.../LiveRideScreenTest.kt` (NEW)
   - Test current speed displays correct value during ride
   - Test speed updates in real-time
   - Test units conversion (metric/imperial)

---

## Next Steps

Proceed to [Phase 1: Data Model & Contracts](./data-model.md) to define the CurrentSpeed state entity and StateFlow contracts between layers.
