<!--
Sync Impact Report - Constitution Update
=========================================
Version Change: 1.0.1 → 1.2.0
Amended: 2025-11-02

Amendment Summary:
- MINOR update: Added project documentation tracking requirements (TODO.md, RELEASE.md)
- MINOR update: Added commit frequency and size requirements (small, regular commits)
- New mandatory workflows: Progress tracking, release documentation, and incremental commits
- All features must update TODO.md and RELEASE.md automatically
- Commits must be small and frequent, not waiting for full feature completion

Modified Sections:
- Development Workflow: Added "Project Documentation Tracking (NON-NEGOTIABLE)" subsection
- Development Workflow: Added "Commit Frequency & Size (NON-NEGOTIABLE)" subsection
- Code Review Requirements: Added TODO.md, RELEASE.md, and commit frequency verification items

Added Requirements:
- TODO.md: Unified progress tracking for active features and pending work
- RELEASE.md: Unified release notes and feature tracking across versions
- Small commits: Maximum ~200 lines, single logical changes, frequent throughout development
- Conventional commit messages: <type>(<scope>): <subject> format

Principles (Unchanged):
1. Modern Android Stack (Kotlin, Jetpack Compose, Material 3)
2. Clean Architecture & MVVM Pattern
3. Compose-First UI Development
4. Test Coverage & Quality Gates
5. Security & Privacy (location data handling)
6. Performance & Battery Efficiency
7. Accessibility & Inclusive Design

Templates Status:
✅ plan-template.md - No updates required
✅ spec-template.md - No updates required
✅ tasks-template.md - Updated: Added TODO.md/RELEASE.md update tasks to setup and polish phases
✅ CLAUDE.md - Updated: Added "Project Documentation Tracking" section with workflow and examples

Documentation Created:
✅ TODO.md - Created with initial template and structure
✅ RELEASE.md - Created with v0.1.0 initial setup entry

Follow-up TODOs:
- None - all requirements implemented

Notes:
- TODO.md ensures transparent progress tracking without explicit user requests
- RELEASE.md maintains comprehensive feature and version history
- Automatic updates prevent documentation drift
- Rationale: Documentation tracking is essential for project visibility and release management
-->

# BikeRedlights Constitution

## Core Principles

### I. Modern Android Stack (NON-NEGOTIABLE)

**Language & Framework Requirements:**
- Kotlin MUST be used for all new code; no Java except for unavoidable legacy integrations
- Jetpack Compose MUST be used for all UI; XML layouts only permitted for existing screens during migration
- Material Design 3 Expressive MUST be followed for all visual design

**Technology Mandates:**
- Kotlin Coroutines + Flow/StateFlow for asynchronous operations (no RxJava, no callbacks)
- Dagger Hilt for dependency injection
- Room for local database (no raw SQLite)
- DataStore for preferences (SharedPreferences prohibited for new features)
- WorkManager for background tasks
- Jetpack Navigation Compose for navigation

**Rationale:** November 2025 Android standards represent the industry consensus for
maintainable, performant, and modern Android development. Legacy approaches increase
technical debt and reduce code quality.

### II. Clean Architecture & MVVM Pattern (NON-NEGOTIABLE)

**Layer Separation:**
- UI Layer: Jetpack Compose screens and reusable composables (stateless preferred)
- ViewModel Layer: UI state management with StateFlow/Flow, no Android framework dependencies except ViewModel
- Domain Layer: Pure Kotlin business logic (use cases), zero Android dependencies
- Data Layer: Repositories coordinating local/remote data sources

**Dependency Rule:**
- Dependencies MUST point inward: UI → ViewModel → Domain → Data
- Inner layers MUST NOT reference outer layers
- Domain models MUST NOT be polluted with Android or data-layer concerns

**Unidirectional Data Flow:**
- State flows down from ViewModel to UI
- Events flow up from UI to ViewModel
- ViewModels MUST NOT hold Context or View references

**Rationale:** Clean Architecture ensures testability, maintainability, and clear
separation of concerns. MVVM is the Google-recommended pattern for Android and
integrates seamlessly with Jetpack Compose and lifecycle-aware components.

### III. Compose-First UI Development

**State Management:**
- State hoisting MUST be applied: lift state to the closest common ancestor
- Composables SHOULD be stateless where possible; receive data via parameters
- Screen-level state MUST live in ViewModels, not composables
- Use `remember` for expensive calculations, `derivedStateOf` for computed state

**Performance Requirements:**
- Keep composables lightweight; defer heavy operations outside composable body
- Use stable types only; apply `@Stable` or `@Immutable` annotations when needed
- LazyColumn/LazyRow MUST be used for lists; regular Column/Row prohibited for dynamic content
- Recomposition tracking MUST be validated during code review for complex screens

**Composition Patterns:**
- Slot pattern for reusable components with flexible content
- Compound components for complex UI patterns
- Single source of truth for each piece of state

**Rationale:** Jetpack Compose best practices (November 2025) minimize unnecessary
recompositions, improve performance, and create maintainable UI code. Poor Compose
patterns lead to performance degradation and battery drain.

### IV. Test Coverage & Quality Gates

**Coverage Targets:**
- Unit tests: 80%+ coverage for ViewModels, Use Cases, and Repositories
- Integration tests: ALL critical user flows (e.g., speed detection, red light warnings)
- UI tests: Main user journeys using Compose testing framework

**Testing Stack:**
- Unit: JUnit 5, MockK, Turbine (Flow testing), Truth (assertions)
- UI: Compose testing framework (semantics-based testing)
- Instrumented: AndroidX Test, Compose UI Test

**Quality Tools (Mandatory):**
- Kotlin Lint enabled with all checks
- Compose Lint enabled for performance issues
- Detekt for static code analysis
- ktlint for code formatting
- All lint warnings MUST be addressed before merge

**Rationale:** BikeRedlights involves user safety on roads. Insufficient testing could
result in missed red light warnings, putting cyclists at risk. High test coverage is
non-negotiable for safety-critical features.

### V. Security & Privacy (NON-NEGOTIABLE)

**Location Data Handling:**
- Runtime permissions MUST be requested; graceful degradation when denied
- Location data MUST be processed locally when possible (no unnecessary cloud transmission)
- Old location data MUST be cleared regularly (retention policy: 24 hours max)
- Privacy policy MUST be clear and transparent about data usage
- Minimal data collection: collect only what is necessary for red light detection

**General Security:**
- No hardcoded secrets; use BuildConfig or Android Keystore
- All network traffic MUST use HTTPS
- Input validation MUST be performed on all user inputs
- Dependency scanning MUST be performed monthly

**Rationale:** BikeRedlights tracks real-time location, which is highly sensitive data.
Privacy violations could harm users and violate GDPR/CCPA. Location data breaches are
particularly serious for a safety-focused app.

### VI. Performance & Battery Efficiency

**Battery Considerations:**
- Location updates MUST use balanced power mode (not high accuracy unless user opts in)
- Background location tracking MUST be minimized; use geofencing when possible
- Wake locks and foreground services MUST be justified and documented
- Battery usage MUST be monitored and optimized continuously

**Performance Requirements:**
- Layout hierarchies MUST be kept flat
- ProGuard/R8 MUST be enabled for release builds
- Images MUST use WebP format with appropriate resolutions
- ANR (Application Not Responding) events: zero tolerance

**Modularization:**
- Feature modules encouraged for build performance
- Gradle build caching MUST be enabled

**Rationale:** Location tracking is battery-intensive. Poor battery performance will
cause users to uninstall the app, defeating the safety mission. Cyclists need the app
to last for long rides without draining their phone.

### VII. Accessibility & Inclusive Design

**Accessibility Requirements (WCAG AA Compliance):**
- Minimum touch target: 48dp × 48dp for all interactive elements
- Contrast ratios MUST meet WCAG AA standards
- Content descriptions MUST be provided for all interactive elements
- Screen reader support MUST be tested for critical flows
- Text MUST be scalable; no hardcoded text sizes that break with large fonts

**Material Design 3 Compliance:**
- Dynamic Color MUST be supported (adapts to user wallpaper/preferences)
- Dark mode MUST be implemented for all screens
- Adaptive layouts MUST respond to different screen sizes (phones/tablets/foldables)
- Motion physics MUST use Material 3's enhanced motion system

**Rationale:** Cyclists include people with disabilities. Accessibility is not optional.
Material Design 3 provides the framework, but implementation is our responsibility.

## Android-Specific Standards

### Naming Conventions

**Code Style:**
- Classes: `PascalCase` (e.g., `BikeRedlightManager`, `MainActivity`)
- Functions/Variables: `camelCase` (e.g., `getCurrentLocation`, `userSpeed`)
- Constants: `ALL_CAPS_WITH_UNDERSCORES` (e.g., `MAX_SPEED_THRESHOLD`)
- Non-public fields: prefix with `m` (e.g., `mUserPreferences`)
- Static fields: prefix with `s` (e.g., `sInstance`)

**Kotlin Style:**
- Prefer `val` over `var` for immutability
- Leverage Kotlin null safety; avoid `!!` operator (use safe calls or let/run)
- Use data classes for models
- Use sealed classes for state representations

### Project Structure

**Required Directory Layout:**
```
app/
├── ui/
│   ├── components/          # Reusable composables
│   ├── screens/             # Screen-level composables
│   ├── navigation/          # Navigation graphs
│   └── theme/               # Material 3 theming
├── domain/
│   ├── model/               # Domain models
│   ├── usecase/             # Use cases (business logic)
│   └── repository/          # Repository interfaces
├── data/
│   ├── repository/          # Repository implementations
│   ├── local/               # Room, DataStore
│   └── remote/              # Retrofit, API (if applicable)
└── di/                      # Hilt modules
```

**Rationale:** Consistent structure enables rapid onboarding and cross-team collaboration.

## Development Workflow

### Project Documentation Tracking (NON-NEGOTIABLE)

**TODO.md - Progress Tracking:**
- A unified TODO.md file MUST exist at repository root
- MUST be updated automatically when starting, progressing, or completing features
- Format: Organized by status (In Progress, Planned, Completed, Deferred)
- Each entry MUST include: feature name, brief description, current status, start date
- Completed items MUST be moved to "Completed" section with completion date
- User does NOT need to explicitly request TODO.md updates; this is automatic

**RELEASE.md - Version & Feature Tracking:**
- A unified RELEASE.md file MUST exist at repository root
- MUST be updated when features are completed and ready for release
- Format: Organized by version (semantic versioning), with sections for each release
- Each version entry MUST include: version number, release date, features added, bugs fixed, breaking changes
- Unreleased features MUST be tracked in "## Unreleased" section at top
- When a release is cut, move "Unreleased" items to new version section
- User does NOT need to explicitly request RELEASE.md updates; this is automatic

**Automatic Update Triggers:**
- Feature start: Add to TODO.md "In Progress", add to RELEASE.md "Unreleased"
- Feature progress: Update TODO.md status/notes as work progresses
- Feature complete: Move TODO.md to "Completed", ensure RELEASE.md entry is detailed
- Bug fix: Add to RELEASE.md under appropriate section
- Breaking change: Document in RELEASE.md with clear migration guidance

**Rationale:** Unified documentation prevents scattered tracking across tools, ensures
transparency, and maintains project history. Automatic updates eliminate documentation
drift and provide real-time visibility into project status without manual intervention.
This is critical for solo developers and teams alike.

### Commit Frequency & Size (NON-NEGOTIABLE)

**Small, Regular Commits Required:**
- Commits MUST be made regularly throughout development, NOT waiting for full feature completion
- Each commit SHOULD represent a single logical change or task completion
- Commit frequently: after completing a single file, function, or logical unit of work
- Maximum recommended commit size: ~200 lines of changes (excluding auto-generated code)
- NEVER accumulate days of work in a single commit

**Acceptable Commit Granularity Examples:**
- ✅ "Add User domain model with validation"
- ✅ "Implement LocationRepository interface"
- ✅ "Create SpeedDetectionUseCase with tests"
- ✅ "Build SpeedDisplayComposable UI component"
- ✅ "Add Hilt module for location dependencies"
- ❌ "Complete entire speed detection feature" (too large)

**Commit Message Format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: feat, fix, docs, style, refactor, test, chore
**Example**: `feat(domain): add speed detection use case with threshold validation`

**Benefits of Small Commits:**
- Easier code review and understanding
- Simpler rollback if issues found
- Clear history of development progression
- Better git bisect debugging
- Reduced merge conflicts
- Incremental progress visibility

**Rationale:** Large, infrequent commits make code review difficult, hide bugs, complicate
debugging, and prevent incremental testing. For safety-critical BikeRedlights features,
small commits enable catching issues early and maintaining clear development audit trails.

### Code Review Requirements

Before merge, ALL of the following MUST be verified:
- Follows Kotlin coding conventions per CLAUDE.md
- Uses Jetpack Compose (no new XML layouts)
- Implements MVVM architecture correctly
- ViewModels do not hold Context references
- State is hoisted appropriately in composables
- Composables are stateless where possible
- Material 3 theming is used consistently
- Dark mode works correctly
- Accessibility features are implemented and tested
- No memory leaks (verified with Android Profiler)
- Tests are written and passing (if feature requires tests per spec)
- All lint warnings are addressed
- No new dependencies without justification
- **Commits are small and frequent** (reviewed git history)
- **TODO.md updated** with current feature status (MANDATORY)
- **RELEASE.md updated** with feature entry in Unreleased section (MANDATORY)

### Offline-First Design

**BikeRedlights-Specific Constraint:**
- App MUST function offline; bikes travel through areas with poor connectivity
- Critical features (speed detection, red light warnings) MUST NOT depend on network
- Remote data (if any) MUST be cached locally
- Network failures MUST degrade gracefully

**Rationale:** A red light warning that arrives 10 seconds late due to network latency
could cause an accident. Offline-first is a safety requirement.

### Emulator Testing Requirement (NON-NEGOTIABLE)

**Mandatory Validation Step:**
- When a feature reaches "working" status (implementation complete, unit tests passing),
  a debug build MUST be installed and tested on an Android emulator
- Testing MUST verify the feature functions correctly in the Android runtime environment
- Emulator testing MUST be completed before code is considered merge-ready

**Emulator Configuration:**
- Use the latest stable Android emulator (API level matching targetSdk)
- Test on at least one phone form factor (e.g., Pixel 6 or similar)
- Enable location simulation for location-dependent features

**Validation Checklist:**
- App installs successfully
- Feature UI renders correctly
- Feature functionality works as expected
- No runtime crashes or ANR events
- Location permissions flow works (if applicable)
- Dark mode displays correctly (if UI changes included)

**Rationale:** Unit and integration tests validate logic, but emulator testing catches
Android framework integration issues, UI rendering problems, and runtime behavior that
only manifests on actual Android. This is critical for safety features that must work
reliably in real-world conditions.

### Testing Conditions

**BikeRedlights-Specific Testing:**
- Test at various speeds (0-40+ mph / 0-65+ km/h)
- Test with varying GPS accuracy levels (urban canyon, open road, tunnels)
- Test in different lighting conditions (day, night, dusk)
- Test with interrupted location (GPS signal loss)
- Test battery drain over extended sessions (2+ hours)

## Governance

### Amendment Process

1. Proposed amendments MUST be documented with rationale
2. Impact on existing code MUST be assessed
3. Migration plan MUST be provided for breaking changes
4. All team members (or AI assistants) MUST acknowledge the update

### Versioning Policy

Constitution version follows semantic versioning:
- **MAJOR**: Backward-incompatible governance changes or principle removals
- **MINOR**: New principles added or material expansions
- **PATCH**: Clarifications, wording fixes, non-semantic refinements

### Compliance Review

- All PRs MUST verify compliance with this constitution
- Complexity or deviations MUST be explicitly justified (see plan-template.md Complexity Tracking section)
- Use CLAUDE.md as the runtime development guidance file for detailed implementation standards

### Authority

This constitution supersedes all other practices. When CLAUDE.md and the constitution
conflict, the constitution takes precedence. When the constitution is silent, defer to
CLAUDE.md.

**Version**: 1.2.0 | **Ratified**: 2025-11-02 | **Last Amended**: 2025-11-02
