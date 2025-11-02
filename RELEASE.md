# BikeRedlights - Release Notes

> **Purpose**: Unified release tracking for all versions, features, and changes
> **Versioning**: Semantic Versioning (MAJOR.MINOR.PATCH)

## Unreleased

_Features and changes completed but not yet released_

### ✨ Features Added
- None yet

### 🐛 Bugs Fixed
- None yet

### 💥 Breaking Changes
- None yet

### 📚 Documentation
- Added CLAUDE.md with November 2025 Android development standards
- Ratified BikeRedlights Constitution v1.1.0
- Established project documentation tracking (TODO.md, RELEASE.md)

### 🔧 Internal Changes
- Initial project setup with Specify template
- Configured MVVM + Clean Architecture structure
- Set up Material Design 3 Expressive guidelines

---

## v0.1.0 - Initial Setup (2025-11-02)

### Project Initialization

**Status**: Development started
**Focus**: Establishing development standards and project governance

### ✨ Features Added
- Project structure with Android Studio configuration
- Git repository initialization
- Development standards documentation

### 📚 Documentation
- **CLAUDE.md**: Comprehensive Android development standards
  - Kotlin-first development with Jetpack Compose
  - MVVM + Clean Architecture patterns
  - Material Design 3 Expressive compliance
  - Testing requirements (80%+ coverage)
  - Security & privacy guidelines for location data
  - Performance & battery efficiency standards
  - Accessibility requirements (WCAG AA)
  - Emulator testing workflow

- **Constitution v1.1.0**: Project governance and core principles
  - 7 core principles (NON-NEGOTIABLE standards)
  - Android-specific coding standards
  - Development workflow requirements
  - Mandatory emulator testing before merge
  - Automatic TODO.md and RELEASE.md tracking
  - Code review checklist

- **TODO.md**: Unified progress tracking
- **RELEASE.md**: This file - version and feature tracking

### 🏗️ Architecture
- Defined Clean Architecture layers:
  - UI Layer: Jetpack Compose screens and components
  - ViewModel Layer: State management with StateFlow
  - Domain Layer: Pure Kotlin business logic (use cases)
  - Data Layer: Repositories and data sources

- Project structure:
  ```
  app/
  ├── ui/          # Jetpack Compose UI
  ├── domain/      # Business logic
  ├── data/        # Data sources
  └── di/          # Dependency injection
  ```

### 🛠️ Technology Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **Async**: Coroutines + Flow/StateFlow
- **DI**: Dagger Hilt
- **Database**: Room
- **Preferences**: DataStore
- **Testing**: JUnit 5, MockK, Turbine, Compose Test

### 🎯 Next Steps
- Implement core speed detection feature
- Set up GPS location tracking
- Create initial UI theme and components
- Implement red light warning system

---

## Version History

| Version | Release Date | Status | Notes |
|---------|--------------|--------|-------|
| v0.1.0  | 2025-11-02   | In Development | Initial setup and standards |

---

## Release Process

**How releases are tracked:**
1. Features are developed and added to "Unreleased" section
2. When ready to release, create new version section with date
3. Move items from "Unreleased" to the new version section
4. Update version history table
5. Tag release in git: `git tag -a v1.0.0 -m "Release v1.0.0"`
6. Update app/build.gradle.kts version codes

**Version Bumping Rules** (Semantic Versioning):
- **MAJOR (X.0.0)**: Breaking changes, major feature additions, incompatible API changes
- **MINOR (1.X.0)**: New features, backward-compatible additions
- **PATCH (1.0.X)**: Bug fixes, small improvements, backward-compatible fixes

---

**Constitution Compliance**: This file satisfies the "Project Documentation Tracking" requirement (Constitution v1.1.0, Development Workflow section).
