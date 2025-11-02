# BikeRedlights - Project TODO

> **Last Updated**: 2025-11-02
> **Purpose**: Unified progress tracking for all features, tasks, and pending work

## 📋 In Progress

_Features currently being developed_

<!-- Example:
### Feature: Speed Detection System
- **Started**: 2025-11-02
- **Status**: Implementation in progress
- **Description**: Implement GPS-based speed detection with configurable thresholds
- **Tasks Remaining**:
  - [ ] Domain layer use case
  - [ ] ViewModel integration
  - [ ] UI composable
  - [x] Repository setup
- **Blockers**: None
-->

_(No features currently in progress)_

---

## 🎯 Planned

_Features planned for upcoming development_

<!-- Example:
### Feature: Red Light Warning System
- **Priority**: P1 - Critical
- **Description**: Alert cyclists when approaching red lights at speed
- **Dependencies**: Speed Detection System
- **Estimated Effort**: 5-7 days
- **Notes**: Core safety feature, requires GPS accuracy validation
-->

_(No features currently planned)_

---

## ✅ Completed

_Features completed and merged_

### v0.0.0 - Buildable Project Skeleton
- **Completed**: 2025-11-02
- **Description**: Established buildable Android project foundation with all dependencies configured
- **Status**: ✅ Builds, installs on emulator, and launches successfully (Pixel 9 Pro / Android 15)
- **Details**:
  - Configured Gradle 8.13 + AGP 8.7.3 + Kotlin 2.0.21
  - Set up Jetpack Compose + Material 3 Dynamic Color theme
  - Created Clean Architecture folder structure (ui, domain, data, di)
  - Configured all required dependencies (Compose, Room, Coroutines, DataStore, WorkManager, Location Services)
  - Added location permissions to manifest
  - Created welcome screen displaying v0.0.0
  - Documented Java 17 requirement in CLAUDE.md
  - **Known Issue**: Hilt DI temporarily disabled due to Gradle plugin compatibility (will fix in v0.1.0)
- **APK Size**: 62MB (debug)
- **Git Tag**: v0.0.0

### Documentation Completeness Review & Fixes
- **Completed**: 2025-11-02
- **Description**: Comprehensive review and systematic fixes of all project documentation and governance files
- **Details**:
  - **P0 Critical Blockers Fixed**:
    - Created APK signing documentation (`.specify/docs/apk-signing.md`)
    - Fixed ProGuard configuration violation (`app/build.gradle.kts`)
    - Added version code calculation rules to CLAUDE.md
    - Created PR template (`.github/pull_request_template.md`)
    - Updated version references (v1.1.0 → v1.3.0) across all files
  - **P1 High Priority Fixed**:
    - Added branch naming convention to constitution
    - Enhanced git push guidance in CLAUDE.md (user-requested)
    - Added emulator GPS simulation instructions to CLAUDE.md
    - Clarified test requirements in constitution (safety-critical vs non-critical)
    - Added post-release workflow documentation to CLAUDE.md
  - **P2 Nice-to-Have Fixed**:
    - Created CI/CD workflow (`.github/workflows/pr-checks.yml`)
    - Added branch protection documentation to constitution
    - Added dependency scanning process to CLAUDE.md
  - **Result**: Documentation is now 100% complete and production-ready

### Initial Project Setup
- **Completed**: 2025-11-02
- **Description**: Project initialization with Specify template, Android standards documentation, and constitution
- **Details**:
  - Created CLAUDE.md with November 2025 Android development standards
  - Ratified BikeRedlights Constitution v1.3.0
  - Established MVVM + Clean Architecture guidelines
  - Configured project structure and governance

---

## 🔄 Deferred

_Features postponed or on hold_

<!-- Example:
### Feature: Social Sharing
- **Deferred**: 2025-11-15
- **Reason**: Low priority, focusing on core safety features first
- **Reconsider**: After v1.0 release
-->

_(No deferred features)_

---

## 📝 Notes

- This file is automatically updated as features progress through the development lifecycle
- User does NOT need to request TODO.md updates; they happen automatically per Constitution
- See RELEASE.md for version-specific feature tracking
- See `.specify/specs/` for detailed feature specifications

---

**Constitution Compliance**: This file satisfies the "Project Documentation Tracking" requirement (Constitution v1.3.0, Development Workflow section).
