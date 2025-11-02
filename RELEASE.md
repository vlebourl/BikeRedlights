# BikeRedlights - Release Notes

> **Purpose**: Unified release tracking for all versions, features, and changes
> **Versioning**: Semantic Versioning (MAJOR.MINOR.PATCH)

## Unreleased

_Features and changes completed but not yet released_

### ✨ Features Added
- None yet

### 🐛 Bugs Fixed
- Fixed ProGuard configuration violation (enabled minification per Constitution v1.3.0)

### 💥 Breaking Changes
- None yet

### 📚 Documentation
- **Comprehensive Documentation Review & Fixes** (2025-11-02):
  - Created APK signing documentation (`.specify/docs/apk-signing.md`)
  - Created PR template (`.github/pull_request_template.md`)
  - Enhanced CLAUDE.md with:
    - Git push guidance and cadence recommendations
    - Emulator GPS simulation instructions
    - Version code calculation rules
    - Post-release workflow documentation
    - Dependency scanning process
  - Updated Constitution v1.3.0 with:
    - Branch naming convention (`###-feature-name`)
    - Test requirements by feature type (safety-critical vs non-critical)
    - Branch protection guidelines
  - Updated version references (v1.1.0 → v1.3.0) across all files
  - Added comprehensive ProGuard rules for BikeRedlights dependencies
- Added CLAUDE.md with November 2025 Android development standards
- Ratified BikeRedlights Constitution v1.3.0
- Established project documentation tracking (TODO.md, RELEASE.md)

### 🔧 Internal Changes
- Created CI/CD workflow (`.github/workflows/pr-checks.yml`) for:
  - Kotlin lint checks
  - Debug APK build verification
  - Constitution compliance checks
  - **Note**: Testing is manual (emulator/physical device) per project standards
- Initial project setup with Specify template
- Configured MVVM + Clean Architecture structure
- Set up Material Design 3 Expressive guidelines

---

## v0.0.0 - Buildable Skeleton (2025-11-02)

### Project Skeleton

**Status**: ✅ COMPLETE - Builds, installs, and launches successfully
**Focus**: Establish buildable foundation with all dependencies configured
**APK Size**: 62MB (debug)
**Tested On**: Pixel 9 Pro Emulator (Android 15 / API 35)

### ✨ Features Added
- Buildable Android project with Gradle 8.13 + AGP 8.7.3 + Kotlin 2.0.21
- Jetpack Compose + Material 3 Dynamic Color theme
  - Light and dark mode support
  - Red color scheme for safety (BikeRedlights theme)
  - Welcome screen with "v0.0.0" display
- Clean Architecture folder structure created:
  - `ui/` (theme, screens, components, navigation)
  - `domain/` (models, use cases, repository interfaces)
  - `data/` (repositories, local, remote)
  - `di/` (dependency injection - ready for Hilt)
- All required dependencies configured (Compose, Room, Coroutines, DataStore, WorkManager, Location Services)
- Location permissions declared in manifest
- Application class configured

### 🐛 Known Issues
- **Hilt DI temporarily disabled** due to Gradle plugin compatibility issues
  - Error: `'java.lang.String com.squareup.javapoet.ClassName.canonicalName()'`
  - Root cause: AGP 8.7.3 + Kotlin 2.0.21 + Hilt 2.48 incompatibility
  - **Resolution plan**: Re-enable in v0.1.0 after upstream fix or workaround
  - Code is ready: TODO markers in `BikeRedlightsApplication.kt` and `MainActivity.kt`
- No actual features - placeholder welcome screen only

### 📚 Documentation Added
- **Java 17 requirement documented** in CLAUDE.md (Development Environment section)
- Project governance finalized (Constitution v1.3.0)
- TODO.md and RELEASE.md templates established

### 🛠️ Technology Stack (Configured)
- **Language**: Kotlin 2.0.21
- **Build**: AGP 8.7.3, Gradle 8.13
- **UI**: Jetpack Compose BOM 2024.11.00 + Material Design 3
- **Architecture**: MVVM + Clean Architecture (folder structure ready)
- **Async**: Coroutines 1.9.0 + Flow/StateFlow
- **DI**: Dagger Hilt (dependencies declared, plugin temporarily disabled)
- **Database**: Room 2.6.1
- **Preferences**: DataStore 1.1.1
- **Location**: Play Services Location 21.3.0
- **Testing**: JUnit, MockK 1.13.13, Turbine 1.2.0, Truth 1.4.4

### 🎯 Next Steps (v0.1.0+)
1. Resolve Hilt Gradle plugin compatibility and re-enable DI
2. Implement first MVP feature (speed detection or GPS tracking)
3. Add actual tests (unit + instrumented)

---

## Version History

| Version | Release Date | Status | Notes |
|---------|--------------|--------|-------|
| v0.0.0  | 2025-11-02   | ✅ Released | Buildable skeleton - tested on emulator |
| v0.1.0  | TBD          | Planned | First MVP feature (+ Hilt DI re-enabled) |

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

**Constitution Compliance**: This file satisfies the "Project Documentation Tracking" requirement (Constitution v1.3.0, Development Workflow section).
