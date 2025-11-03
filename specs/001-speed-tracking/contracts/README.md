# Contracts: Real-Time Speed and Location Tracking

**Feature**: 001-speed-tracking
**Phase**: Phase 1 - Design & Contracts
**Date**: 2025-11-02

## Overview

This directory contains the contract definitions (interfaces and specifications) for the speed tracking feature. These contracts establish the boundaries between architectural layers and serve as the "API" for internal components.

## What Are Contracts?

In Clean Architecture, contracts are interfaces that define:
- **What** functionality a component provides (not **how**)
- Input parameters and output types
- Expected behavior and error conditions
- Performance characteristics and constraints

Contracts enable:
- **Testability**: Mock implementations for unit tests
- **Separation of Concerns**: Domain logic independent of Android framework
- **Replaceability**: Swap implementations without changing consumers
- **Documentation**: Clear expectations for implementers

## Contract Files

### 1. LocationRepository.kt

**Type**: Repository Interface (Domain Layer)

**Purpose**: Abstracts GPS location tracking operations

**Key Method**:
```kotlin
fun getLocationUpdates(): Flow<LocationData>
```

**Consumers**:
- `TrackLocationUseCase` (domain layer)
- Tests: `LocationRepositoryImplTest`, `TrackLocationUseCaseTest`

**Implementations**:
- `LocationRepositoryImpl` (data layer) - Production implementation using FusedLocationProviderClient
- `FakeLocationRepository` (test) - Test double for unit tests

**Contract Guarantees**:
- Returns cold Flow (starts on collect, stops on cancel)
- Emits ~1 location/second when collected
- Throws SecurityException if permission not granted
- Automatically cleans up FusedLocationProviderClient on Flow cancellation

---

### 2. TrackLocationUseCase.kt

**Type**: Use Case (Domain Layer)

**Purpose**: Calculates cycling speed from raw GPS data

**Key Method**:
```kotlin
operator fun invoke(): Flow<SpeedMeasurement>
```

**Consumers**:
- `SpeedTrackingViewModel` (presentation layer)
- Tests: `TrackLocationUseCaseTest`

**Dependencies**:
- `LocationRepository` (injected via constructor)

**Contract Guarantees**:
- Converts m/s to km/h
- Applies stationary threshold (<1 km/h)
- Clamps speed to realistic range (0-100 km/h)
- Determines speed source (GPS vs calculated)
- Maintains previous location for fallback calculation

---

## Architecture Flow

```
┌──────────────────────────────────────────────────────────┐
│  Data Layer                                              │
│  ┌────────────────────────────────────────────┐         │
│  │ LocationRepositoryImpl                     │         │
│  │ (wraps FusedLocationProviderClient)        │         │
│  └───────────────▲────────────────────────────┘         │
└──────────────────┼──────────────────────────────────────┘
                   │ implements
                   │
┌──────────────────┼──────────────────────────────────────┐
│  Domain Layer    │                                       │
│  ┌───────────────▼────────────────────────────┐         │
│  │ LocationRepository (interface/contract)    │         │
│  └───────────────▲────────────────────────────┘         │
│                  │ injected into                         │
│  ┌───────────────┴────────────────────────────┐         │
│  │ TrackLocationUseCase                       │         │
│  │ (business logic: speed calculation)        │         │
│  └───────────────▲────────────────────────────┘         │
└──────────────────┼──────────────────────────────────────┘
                   │ injected into
                   │
┌──────────────────┼──────────────────────────────────────┐
│  Presentation    │                                       │
│  ┌───────────────▼────────────────────────────┐         │
│  │ SpeedTrackingViewModel                     │         │
│  │ (UI state management)                      │         │
│  └───────────────▲────────────────────────────┘         │
└──────────────────┼──────────────────────────────────────┘
                   │ observes
                   │
┌──────────────────┼──────────────────────────────────────┐
│  UI Layer        │                                       │
│  ┌───────────────▼────────────────────────────┐         │
│  │ SpeedTrackingScreen (Jetpack Compose)      │         │
│  │ (renders speed, location, GPS status)      │         │
│  └────────────────────────────────────────────┘         │
└──────────────────────────────────────────────────────────┘
```

## Dependency Rule

**Inward Dependencies Only**:
- UI Layer → Presentation Layer → Domain Layer → Data Layer
- Domain Layer has NO Android dependencies (pure Kotlin)
- Data Layer wraps Android framework (FusedLocationProviderClient)
- Contracts (interfaces) live in Domain Layer
- Implementations live in Data/Presentation/UI layers

**Violations Prohibited**:
- ❌ Domain Layer importing `android.*` packages
- ❌ Data Layer importing UI/Presentation classes
- ❌ ViewModel holding Context or View references

## Testing Strategy

### Unit Tests (No Android Framework)

```kotlin
class TrackLocationUseCaseTest {
    private val fakeRepository = FakeLocationRepository()
    private val useCase = TrackLocationUseCase(fakeRepository)

    @Test
    fun `converts 10 m/s GPS speed to 36 km/h`() = runTest {
        fakeRepository.emitLocation(
            LocationData(speedMps = 10f, ...)
        )

        useCase().test {
            val measurement = awaitItem()
            assertEquals(36f, measurement.speedKmh)
            assertEquals(SpeedSource.GPS, measurement.source)
        }
    }

    @Test
    fun `applies stationary threshold for speeds below 1 km/h`() = runTest {
        fakeRepository.emitLocation(
            LocationData(speedMps = 0.2f, ...) // 0.72 km/h
        )

        useCase().test {
            val measurement = awaitItem()
            assertEquals(0f, measurement.speedKmh)
            assertTrue(measurement.isStationary)
        }
    }
}
```

### Integration Tests (Android Instrumentation)

```kotlin
@RunWith(AndroidJUnit4::class)
class LocationRepositoryImplIntegrationTest {
    @Test
    fun `repository emits location updates when collected`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = LocationRepositoryImpl(context)

        repository.getLocationUpdates()
            .timeout(5.seconds)
            .take(1)
            .test {
                val location = awaitItem()
                assertNotNull(location)
                assertTrue(location.accuracy > 0)
            }
    }
}
```

## Implementation Checklist

- [ ] `LocationRepositoryImpl` implements `LocationRepository`
- [ ] `TrackLocationUseCase` implements speed calculation per contract
- [ ] `FakeLocationRepository` created for tests
- [ ] `LocationRepositoryImplTest` validates repository behavior
- [ ] `TrackLocationUseCaseTest` validates speed calculation logic
- [ ] All contracts documented with KDoc
- [ ] Error handling paths tested (SecurityException, GPS unavailable)
- [ ] Edge cases tested (negative speed, unrealistic values, stationary)

## Contract Evolution

### Version 1 (MVP - Current)
- `LocationRepository.getLocationUpdates()`: Cold Flow emitting LocationData

### Future Versions (Out of Scope for MVP)
- `LocationRepository.getLastKnownLocation()`: Immediate location (no Flow)
- `LocationRepository.requestSingleLocationUpdate()`: One-shot location request
- `LocationRepository.isLocationEnabled()`: Check if GPS is enabled
- `TrackingRepository` (new): Persist location history to Room database

**Breaking Change Policy**:
- New methods: MINOR version bump (backward compatible)
- Changed signatures: MAJOR version bump (breaking change)
- Document migrations in RELEASE.md

## References

- **Clean Architecture**: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- **Dependency Rule**: https://developer.android.com/topic/architecture#recommended-app-arch
- **Repository Pattern**: https://developer.android.com/codelabs/android-room-with-a-view-kotlin
- **Use Case Pattern**: https://proandroiddev.com/why-you-need-use-cases-interactors-142e8a6fe576

---

**Next Steps**: Implement these contracts in the actual source code following the specifications in this directory.
