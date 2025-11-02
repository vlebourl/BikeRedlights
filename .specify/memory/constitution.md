<!--
Sync Impact Report - Constitution Update
=========================================
Version Change: INITIAL → 1.0.0
Created: 2025-11-02

New Constitution Ratification:
- First constitution for BikeRedlights project
- Based on CLAUDE.md Android development standards (November 2025)
- Aligned with Material Design 3 Expressive guidelines
- Incorporates MVVM + Clean Architecture principles

Principles Established:
1. Modern Android Stack (Kotlin, Jetpack Compose, Material 3)
2. Clean Architecture & MVVM Pattern
3. Compose-First UI Development
4. Test Coverage & Quality Gates
5. Security & Privacy (location data handling)
6. Performance & Battery Efficiency
7. Accessibility & Inclusive Design

Templates Status:
✅ plan-template.md - Reviewed, Constitution Check section aligns
✅ spec-template.md - Reviewed, requirements structure compatible
✅ tasks-template.md - Reviewed, task organization aligns with principles
⚠️ PENDING: Update templates to reference BikeRedlights-specific constraints

Follow-up TODOs:
- None - all placeholders filled

Notes:
- This constitution establishes governance for an Android bike safety app
- Prioritizes user safety, location privacy, and offline-first design
- Enforces latest November 2025 Android development standards
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

### Offline-First Design

**BikeRedlights-Specific Constraint:**
- App MUST function offline; bikes travel through areas with poor connectivity
- Critical features (speed detection, red light warnings) MUST NOT depend on network
- Remote data (if any) MUST be cached locally
- Network failures MUST degrade gracefully

**Rationale:** A red light warning that arrives 10 seconds late due to network latency
could cause an accident. Offline-first is a safety requirement.

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

**Version**: 1.0.0 | **Ratified**: 2025-11-02 | **Last Amended**: 2025-11-02
