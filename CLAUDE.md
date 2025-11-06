# Android Development Standards for BikeRedlights

> **Last Updated:** November 2025
> **Purpose:** This document ensures all code follows modern Android development best practices and latest standards.

## 🎯 Core Principles

### Language & Code Style
- **Kotlin-first**: All new code must be written in Kotlin
- **Immutability**: Prefer `val` over `var` whenever possible
- **Null Safety**: Leverage Kotlin's null safety features; avoid `!!` operator
- **Naming Conventions**:
  - Classes: `PascalCase` (e.g., `BikeRedlightManager`, `MainActivity`)
  - Functions/Variables: `camelCase` (e.g., `getCurrentLocation`, `userSpeed`)
  - Constants: `ALL_CAPS_WITH_UNDERSCORES` (e.g., `MAX_SPEED_THRESHOLD`)
  - Non-public fields: prefix with `m` (e.g., `mUserPreferences`)
  - Static fields: prefix with `s` (e.g., `sInstance`)

## 💻 Development Environment

### Java Development Kit (JDK)

**Required**: Java 17 (OpenJDK 17)

**Installation (macOS with Homebrew)**:
```bash
# Install OpenJDK 17
brew install openjdk@17

# Set environment variables (add to ~/.zshrc or ~/.bash_profile)
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"

# Reload shell configuration
source ~/.zshrc

# Verify installation
java -version  # Should show OpenJDK 17
```

**Note for Intel Macs**: Use `/usr/local/opt/openjdk@17` instead of `/opt/homebrew/opt/openjdk@17`

**Build Configuration**:
- Project uses Java 17 for compilation (`app/build.gradle.kts:44-49`)
- `sourceCompatibility` and `targetCompatibility` set to `JavaVersion.VERSION_17`
- `kotlinOptions.jvmTarget` set to `"17"`

**Why Java 17?**
- Kotlin 2.0+ requires Java 17 minimum
- Android Gradle Plugin 8.7+ recommends Java 17
- Better performance and modern language features

## 🏗️ Architecture Pattern

### MVVM + Clean Architecture
```
┌─────────────────────────────────────┐
│ UI Layer (Jetpack Compose)          │
│ - Composables (stateless)           │
│ - Screen-level state holders        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ ViewModel Layer                     │
│ - UI state management               │
│ - State hoisting                    │
│ - StateFlow/Flow emissions          │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ Domain Layer (Use Cases)            │
│ - Business logic                    │
│ - Pure Kotlin (no Android deps)     │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ Data Layer (Repositories)           │
│ - Data sources coordination         │
│ - Local & remote data               │
└─────────────────────────────────────┘
```

### Key Principles
- **Separation of Concerns**: Each layer has a single responsibility
- **Dependency Rule**: Dependencies point inward (UI → ViewModel → Domain → Data)
- **Unidirectional Data Flow**: State flows down, events flow up
- **Testability**: Each layer can be tested in isolation

## 🛠️ Technology Stack

### Required Libraries
- **UI**: Jetpack Compose (NO XML layouts for new features)
- **Async**: Kotlin Coroutines + Flow/StateFlow
- **DI**: Dagger Hilt
- **Navigation**: Jetpack Navigation Compose
- **Networking**: Retrofit + OkHttp
- **Local DB**: Room
- **Preferences**: DataStore (NOT SharedPreferences)
- **Background Work**: WorkManager
- **Location**: Fused Location Provider API
- **Testing**: JUnit, MockK, Turbine (for Flow testing)

### Avoid
- ❌ XML layouts (except for existing screens during migration)
- ❌ `findViewById()` (use Compose or ViewBinding as fallback)
- ❌ SharedPreferences (use DataStore)
- ❌ AsyncTask (deprecated)
- ❌ LiveData (prefer StateFlow/Flow)

## 🎨 UI/UX Standards (Material Design 3 Expressive)

### Design System
- **Material 3 Expressive** guidelines (November 2025)
- Official reference: [m3.material.io](https://m3.material.io/)
- **7 Core Foundations**: Color, Typography, Shape, Motion, Interaction, Layout, Elevation

### Implementation Requirements
- **Dynamic Color**: Support user wallpaper-based theming
- **Adaptive Layouts**: Responsive design for phones/tablets/foldables
- **Dark Mode**: Always implement both light and dark themes
- **Accessibility**:
  - Minimum touch target: 48dp × 48dp
  - Contrast ratios: WCAG AA compliance
  - Content descriptions for all interactive elements
  - Semantic markup for screen readers

### Motion & Interaction
- Use Material 3's enhanced motion physics
- Provide haptic feedback for important interactions
- Ensure 60fps minimum for animations
- Meaningful transitions between screens

## 📁 Project Structure

```
app/
├── ui/
│   ├── components/          # Reusable composables
│   │   ├── buttons/
│   │   ├── cards/
│   │   └── dialogs/
│   ├── screens/             # Screen-level composables
│   │   ├── home/
│   │   ├── settings/
│   │   └── map/
│   ├── navigation/          # Navigation graphs
│   ├── theme/               # Material 3 theming
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── util/                # UI utilities
├── domain/                  # Business logic
│   ├── model/               # Domain models
│   ├── usecase/             # Use cases
│   └── repository/          # Repository interfaces
├── data/                    # Data layer
│   ├── repository/          # Repository implementations
│   ├── local/               # Room, DataStore
│   ├── remote/              # Retrofit, API
│   └── model/               # Data models (DTOs)
└── di/                      # Hilt modules
    ├── AppModule.kt
    ├── NetworkModule.kt
    └── DatabaseModule.kt
```

## ✨ Jetpack Compose Best Practices

### Performance
- **Keep composables lightweight**: No heavy calculations in composable body
- **Use `remember`**: Cache expensive operations
  ```kotlin
  val expensiveResult = remember(key) {
      performExpensiveCalculation()
  }
  ```
- **Use `derivedStateOf`**: For computed state that depends on other state
  ```kotlin
  val isValid by remember {
      derivedStateOf { email.isNotEmpty() && password.length >= 8 }
  }
  ```
- **Stable types only**: Ensure data classes are stable (immutable, primitives, or other stable types)
- **Defer state reads**: Read state as late as possible to minimize recompositions

### State Management
- **State hoisting**: Lift state to the closest common ancestor
- **Stateless composables**: UI components should receive data via parameters
- **Stateful composables**: Contain `remember` and manage their own state
- **ViewModel for screen state**: Screen-level state lives in ViewModel

### Composition Patterns
- **Slot Pattern**: For flexible, reusable components
  ```kotlin
  @Composable
  fun CustomCard(
      title: String,
      actions: @Composable () -> Unit
  ) { /* ... */ }
  ```
- **Compound Components**: For complex UI patterns
- **Single source of truth**: One place for each piece of state

### Code Organization
```kotlin
// ✅ GOOD: Stateless, reusable
@Composable
fun BikeSpeedDisplay(
    speed: Float,
    modifier: Modifier = Modifier
) { /* ... */ }

// ✅ GOOD: Stateful, manages its own state
@Composable
fun BikeSpeedInput(
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var speed by remember { mutableStateOf(0f) }
    // ...
}

// ✅ GOOD: Screen with ViewModel
@Composable
fun BikeMapScreen(
    viewModel: BikeMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BikeMapContent(uiState)
}
```

## 🧪 Testing Requirements

### Test Coverage Targets
- **Unit Tests**: 80%+ coverage for ViewModels, UseCases, Repositories
- **Integration Tests**: Critical user flows
- **UI Tests**: Main user journeys (Compose testing)

### Testing Stack
- **Unit**: JUnit 5, MockK, Turbine (Flow testing), Truth (assertions)
- **UI**: Compose testing framework
- **Instrumented**: AndroidX Test, Espresso (legacy), Compose UI Test

### Example
```kotlin
@Test
fun `when speed exceeds threshold, red light warning is shown`() = runTest {
    val useCase = DetectRedLightViolationUseCase(fakeRepository)
    val result = useCase(speed = 60f, threshold = 50f)
    assertThat(result).isInstanceOf<ViolationDetected>()
}
```

### Emulator Testing (MANDATORY)

**When Required**: Every completed feature MUST be tested on an Android emulator before merge.

**Setup**:
- Use the latest stable Android emulator (API level matching targetSdk)
- Test on at least one phone form factor (e.g., Pixel 6)
- Enable location simulation for location-dependent features

**Validation Checklist**:
- ✅ App installs successfully via `./gradlew installDebug`
- ✅ Feature UI renders correctly
- ✅ Feature functionality works as expected
- ✅ No runtime crashes or ANR events
- ✅ Location permissions flow works (if applicable)
- ✅ Dark mode displays correctly (toggle in emulator)
- ✅ Rotation handling works (if applicable)
- ✅ Back navigation behaves correctly

**Quick Start Emulator**:
```bash
# List available emulators
emulator -list-avds

# Start emulator (replace with your AVD name)
emulator -avd Pixel_6_API_34 &

# Install debug build
./gradlew installDebug

# View logs
adb logcat | grep BikeRedlights
```

**GPS Location Simulation** (Critical for BikeRedlights):

Emulator → Extended Controls (...) → Location tab

**Option 1: Single Point**:
1. Enter latitude/longitude manually
2. Click "Send" to set location
3. Example: 37.422, -122.084 (Google campus)

**Option 2: GPX/KML Route**:
1. Select "GPX/KML" tab
2. Load GPX file with route points
3. Click "Play" to simulate movement
4. Adjust speed slider for different cycling speeds

**Option 3: ADB Commands**:
```bash
# Send single location (latitude, longitude)
adb emu geo fix -122.084 37.422

# Simulate movement route
adb shell "echo 'geo fix -122.084000 37.422000' | nc localhost 5554"
adb shell "echo 'geo fix -122.085000 37.423000' | nc localhost 5554"
adb shell "echo 'geo fix -122.086000 37.424000' | nc localhost 5554"
```

**Testing Speed Detection**:
1. Create GPX file with points at increasing distances
2. Set playback speed to match cycling speed (10-25 mph)
3. Observe speed detection in app
4. Verify thresholds trigger correctly

**Testing GPS Signal Loss**:
1. Disable location in emulator settings
2. Verify app handles gracefully (doesn't crash)
3. Re-enable and verify app recovers

**Why This Matters**: Unit tests validate logic, but emulator testing catches Android framework integration issues, UI rendering problems, and runtime behavior that only appears on actual Android. For a safety-critical app like BikeRedlights, this is non-negotiable.

### Project Documentation Tracking (MANDATORY)

**Automatic Updates Required**: You MUST update TODO.md and RELEASE.md as features progress. The user does NOT need to explicitly ask for these updates.

**TODO.md - Progress Tracking**:
- **When starting a feature**: Add to "In Progress" section with start date, description, and task checklist
- **During development**: Update status/notes as work progresses, check off completed tasks
- **When complete**: Move to "Completed" section with completion date
- **When deferred**: Move to "Deferred" section with reason

**RELEASE.md - Version Tracking**:
- **When starting a feature**: Add entry to "Unreleased" section with brief description
- **When fixing a bug**: Add to "Bugs Fixed" under "Unreleased"
- **When making breaking change**: Add to "Breaking Changes" with migration guidance
- **When feature complete**: Ensure detailed entry in "Unreleased" with all changes documented
- **When releasing a version**: Move all "Unreleased" items to new version section

**Format Examples**:

TODO.md entry:
```markdown
### Feature: Speed Detection System
- **Started**: 2025-11-02
- **Status**: ViewModel integration in progress
- **Tasks Remaining**:
  - [x] Repository setup
  - [x] Domain layer use case
  - [ ] ViewModel integration
  - [ ] UI composable
- **Blockers**: None
```

RELEASE.md entry:
```markdown
## Unreleased

### ✨ Features Added
- **Speed Detection System**: Real-time GPS-based speed tracking with configurable thresholds
  - Implements Clean Architecture pattern (Repository → UseCase → ViewModel → UI)
  - Battery-efficient location updates using balanced power mode
  - Offline-first design with local processing
```

**Workflow Integration**:
1. Start feature → Update TODO.md and RELEASE.md immediately
2. Make progress → Update TODO.md task checklist
3. Complete feature → Move TODO.md to Completed, finalize RELEASE.md entry
4. Code review → Verify both files are updated (required on checklist)

### Commit Frequency & Size (MANDATORY)

**Small, Regular Commits Required**: NEVER wait for full feature completion to commit.

**Commit Guidelines**:
- ✅ Commit after completing a single file, function, or logical unit
- ✅ Maximum ~200 lines of changes per commit (excluding auto-generated code)
- ✅ Each commit = one logical change or task
- ❌ NEVER accumulate days of work in a single commit
- ❌ NEVER commit entire features at once

**Good Commit Examples**:
```bash
git commit -m "feat(domain): add SpeedThreshold value object with validation"
git commit -m "feat(data): implement LocationRepository with Room integration"
git commit -m "test(domain): add unit tests for SpeedDetectionUseCase"
git commit -m "feat(ui): create SpeedDisplayComposable with preview"
git commit -m "refactor(di): extract location module from AppModule"
```

**Bad Commit Examples**:
```bash
# ❌ Too large, unclear what changed
git commit -m "Complete speed detection feature"

# ❌ Multiple unrelated changes
git commit -m "Add repository, use case, ViewModel, and UI"

# ❌ Too vague
git commit -m "Updates"
```

**Conventional Commit Format**:
```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Formatting, missing semicolons, etc.
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding missing tests
- `chore`: Updating build tasks, package manager configs, etc.

**Scopes**: `domain`, `data`, `ui`, `di`, `test`, `build`, etc.

**Benefits**:
- Easier code review (small diffs)
- Simpler rollback if issues found
- Clear development history
- Better git bisect debugging
- Reduced merge conflicts
- Incremental testing possible

**When to Commit**:
1. After writing a new domain model/entity
2. After implementing a repository method
3. After creating a use case with its tests
4. After building a composable component
5. After adding a Hilt module or dependency
6. After fixing a bug
7. After refactoring a function/class
8. At least every 1-2 hours of work, even if incomplete

**Workflow**:
```bash
# Work on single task
# ... implement LocationRepository ...

# Commit when task is done (even if feature incomplete)
git add app/src/main/java/com/example/bikeredlights/data/repository/LocationRepositoryImpl.kt
git commit -m "feat(data): implement LocationRepository with FusedLocationProvider

- Add battery-efficient location updates
- Implement balanced power mode
- Handle permission denied gracefully"

# Continue with next task
# ... implement use case ...
git commit -m "feat(domain): add SpeedDetectionUseCase with threshold checks"

# And so on...
```

### Pushing to Remote (GitHub) - IMPORTANT

**When to Push**:
- Push regularly throughout development (don't let local work pile up)
- After every 2-5 commits or at end of work session
- Before switching branches or taking breaks
- Before requesting code review
- **CRITICAL**: Before ending work for the day

**First-Time Branch Push**:
```bash
# Create feature branch
git checkout -b 001-speed-detection

# Make some commits
git commit -m "feat(domain): add speed detection use case"
git commit -m "feat(data): implement location repository"

# Push branch to GitHub for the first time (sets upstream tracking)
git push -u origin 001-speed-detection

# Subsequent pushes on this branch
git push
```

**Regular Push Workflow**:
```bash
# After making commits locally
git add .
git commit -m "feat(ui): create speed display composable"

# Push to GitHub immediately
git push

# Or push multiple commits at once
git push origin 001-speed-detection
```

**Why Push Frequently**:
- ✅ Backs up your work to GitHub (protects against local disk failure)
- ✅ Makes work visible to team/collaborators
- ✅ Enables early feedback and code review
- ✅ Allows working from multiple machines
- ✅ Creates audit trail of development progress

**Force Push (Use with EXTREME Caution)**:
```bash
# Only if you're the only developer on the branch
# And you've rebased or amended commits
git push --force-with-lease origin 001-speed-detection

# NEVER force push to main!
```

**Checking Remote Status**:
```bash
# See if you have unpushed commits
git status

# See commit differences
git log origin/001-speed-detection..HEAD

# Verify branch tracking
git branch -vv
```

**Push Cadence Recommendation**:
- Development: Push after every 2-3 commits or 1-2 hours of work
- Before PR: Ensure all commits are pushed
- During review: Push fixes immediately after committing

### Release Pattern & Workflow (MANDATORY)

**Each Specify Session MUST End with a Release**: Every `/speckit.specify` session concludes with a versioned release following semantic versioning.

**Semantic Versioning (vMAJOR.MINOR.PATCH)**:
- **MAJOR (vX.0.0)**: Breaking changes, incompatible API changes, major overhauls
- **MINOR (v1.X.0)**: New features, backward-compatible additions
- **PATCH (v1.0.X)**: Bug fixes, performance improvements, small changes

**Pull Request Workflow**:
1. Work on feature branch (e.g., `001-speed-detection`)
2. Make small, frequent commits throughout development
3. Push branch to GitHub: `git push origin <branch-name>`
4. Create PR with detailed description including:
   - Feature summary and implementation details
   - Link to specification in `.specify/specs/`
   - Emulator testing confirmation
   - Breaking changes (if any)
5. After PR approval and merge to `main`, proceed to release

**Release Steps (After PR Merge)**:
```bash
# 1. Move "Unreleased" items in RELEASE.md to new version section
# Edit RELEASE.md manually

# 2. Update version in app/build.gradle.kts
# Calculate versionCode using formula: MAJOR * 10000 + MINOR * 100 + PATCH
# Examples:
#   v0.1.0 → versionCode = 100
#   v0.2.0 → versionCode = 200
#   v0.2.1 → versionCode = 201
#   v1.0.0 → versionCode = 10000
# Edit app/build.gradle.kts:
#   versionCode = 200  (for v0.2.0)
#   versionName = "0.2.0"

# 3. Commit version bump
git add RELEASE.md app/build.gradle.kts
git commit -m "chore: bump version to v0.2.0"

# 4. Create annotated tag
git tag -a v0.2.0 -m "Release v0.2.0: Speed Detection Feature

- Add GPS-based speed tracking
- Implement configurable speed thresholds
- Battery-efficient location updates"

# 5. Push tag to GitHub
git push origin v0.2.0

# 6. Build signed release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# 7. Create GitHub Release
# - Go to GitHub → Releases → Draft new release
# - Select tag: v0.2.0
# - Title: "v0.2.0 - Speed Detection Feature"
# - Body: Copy from RELEASE.md version section
# - Attach APK: Rename to BikeRedlights-v0.2.0.apk and upload
# - Publish release
```

**Release Checklist**:
- ✅ All tests passing (unit, integration, UI)
- ✅ Emulator testing completed and validated
- ✅ RELEASE.md updated with version section
- ✅ app/build.gradle.kts version codes updated
- ✅ Git tag created with release notes
- ✅ Signed release APK built successfully
- ✅ GitHub Release created with APK attached
- ✅ Release notes match RELEASE.md content

**Version Progression Example**:
- v0.1.0: Initial project setup
- v0.2.0: Speed detection feature (MINOR - new feature)
- v0.3.0: Red light warning system (MINOR - new feature)
- v0.3.1: Fix GPS accuracy bug (PATCH - bug fix)
- v1.0.0: First stable release (MAJOR - production ready)

**Why This Matters**: Structured releases ensure traceability for a safety-critical app. Every feature is properly versioned, documented, and available as a signed APK for installation. The PR workflow enables code review before release.

### Post-Release Workflow

After GitHub Release is published, complete these steps:

**1. Pull Latest Main**:
```bash
# Switch to main and get the latest code
git checkout main
git pull origin main
```

**2. Verify Tag**:
```bash
# List all tags
git tag --list

# Show tag details
git show v0.2.0

# Verify tag is pushed to GitHub
git ls-remote --tags origin
```

**3. Test Installation** (Manual QA):
- Download APK from GitHub Release page
- Install on physical device or clean emulator:
  ```bash
  adb install BikeRedlights-v0.2.0.apk
  ```
- Launch app and verify feature works
- Test upgrade path (if not first release)

**4. Branch Cleanup** (Optional):
```bash
# Delete local feature branch
git branch -d 001-speed-detection

# Delete remote feature branch (optional, keep for audit trail)
git push origin --delete 001-speed-detection
```

**5. Update TODO.md**:
- Ensure feature is moved to "Completed" section
- Add completion date
- Mark any follow-up tasks or technical debt

**6. Announce Release** (if applicable):
- Post to team chat/Slack
- Email stakeholders
- Update project dashboard
- Social media (if public project)

**7. Monitor for Issues**:
- Watch GitHub issues for crash reports
- Check Play Console crash logs (if published to Play Store)
- Monitor user feedback

### Version Code Calculation (Android-Specific)

**Formula**: `versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`

**Why**: Android requires version codes to increase monotonically for app updates. This formula ensures:
- Up to 99 minor versions per major version
- Up to 99 patch versions per minor version
- Clear numeric progression

**Examples**:
| Semantic Version | Calculation | versionCode |
|-----------------|-------------|-------------|
| v0.1.0 | 0 * 10000 + 1 * 100 + 0 | 100 |
| v0.2.0 | 0 * 10000 + 2 * 100 + 0 | 200 |
| v0.2.1 | 0 * 10000 + 2 * 100 + 1 | 201 |
| v0.3.0 | 0 * 10000 + 3 * 100 + 0 | 300 |
| v1.0.0 | 1 * 10000 + 0 * 100 + 0 | 10000 |
| v1.5.3 | 1 * 10000 + 5 * 100 + 3 | 10503 |
| v2.0.0 | 2 * 10000 + 0 * 100 + 0 | 20000 |

**Implementation in `app/build.gradle.kts`**:
```kotlin
defaultConfig {
    applicationId = "com.example.bikeredlights"
    minSdk = 34
    targetSdk = 36

    // Version must increase with each release
    versionCode = 100  // v0.1.0
    versionName = "0.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}
```

**Update Process**:
1. Determine semantic version (v0.2.0)
2. Calculate versionCode: 0 * 10000 + 2 * 100 + 0 = 200
3. Update both values in `app/build.gradle.kts`
4. Commit: `git commit -m "chore: bump version to v0.2.0"`

**Important**:
- versionCode MUST always increase (never decrease or stay same)
- If you need to hotfix an older version, use the next available code
- Android will reject updates if versionCode doesn't increase

## 🚀 Performance Guidelines

### General
- **Layout hierarchy**: Keep flat (avoid nested layouts)
- **Modularization**: Feature modules for build performance
- **ProGuard/R8**: Always enabled for release builds
- **Image optimization**: WebP format, appropriate resolutions
- **Background work**: Use WorkManager, not foreground services unless necessary

### Compose-Specific
- **Avoid unstable types**: Use `@Stable` or `@Immutable` annotations when needed
- **Remember expensive operations**: Network calls, bitmap operations, etc.
- **LazyColumn/LazyRow**: For long lists (never use regular Column/Row)
- **Recomposition tracking**: Use Layout Inspector to debug performance

## 🔒 Security & Privacy

### Location Data (Critical for BikeRedlights)
- **Runtime permissions**: Always request, handle denial gracefully
- **Minimal data collection**: Only collect what's necessary
- **Local processing**: Process speed/location locally when possible
- **Data retention**: Clear old location data regularly
- **User transparency**: Clear privacy policy and data usage explanations

### General
- **No hardcoded secrets**: Use BuildConfig or secure storage
- **HTTPS only**: All network traffic encrypted
- **Input validation**: Sanitize all user inputs
- **Dependency scanning**: Regular security audits

## 📋 Code Review Checklist

Before submitting code, verify:
- ✅ Follows Kotlin coding conventions
- ✅ Uses Jetpack Compose (no new XML layouts)
- ✅ Implements MVVM architecture correctly
- ✅ ViewModels don't hold Context references
- ✅ State is hoisted appropriately
- ✅ Composables are stateless where possible
- ✅ Material 3 theming is used consistently
- ✅ Dark mode works correctly
- ✅ Accessibility features are implemented
- ✅ No memory leaks (check with Profiler)
- ✅ Tests are written and passing
- ✅ **Debug build tested on emulator** (MANDATORY)
- ✅ **TODO.md updated** with current feature status (MANDATORY)
- ✅ **RELEASE.md updated** with feature entry in Unreleased section (MANDATORY)
- ✅ Lint warnings are addressed
- ✅ No new dependencies without justification

## 🛡️ Quality Tools

### Required in Android Studio
- **Kotlin Lint**: Enabled with all checks
- **Compose Lint**: Enabled for performance issues
- **Detekt**: Static code analysis (configure in `detekt.yml`)
- **ktlint**: Code formatting
- **Android Lint**: All warnings addressed

### CI/CD Pipeline
- Automated lint checks
- Unit test execution
- Build verification
- Test coverage reporting

### Dependency Scanning & Updates

**Tool**: GitHub Dependabot (enabled by default in GitHub repositories)

**Monthly Review Process** (Constitution Requirement):

1. **Check Dependabot Alerts**:
   - Go to GitHub → Security → Dependabot alerts
   - Review any security vulnerabilities
   - Prioritize critical/high severity issues

2. **Manual Dependency Check**:
   ```bash
   # Check for outdated dependencies
   ./gradlew dependencyUpdates

   # Or use this plugin in build.gradle.kts:
   # id("com.github.ben-manes.versions") version "0.50.0"
   ```

3. **Update Dependencies**:
   - Edit `gradle/libs.versions.toml`
   - Update version numbers for outdated libraries
   - Focus on security patches first, then feature updates

4. **Test After Updates**:
   ```bash
   # Run all tests
   ./gradlew test

   # Build project
   ./gradlew assembleDebug

   # Test on emulator
   ./gradlew installDebug
   # Manual testing of critical flows
   ```

5. **Commit Updates**:
   ```bash
   git add gradle/libs.versions.toml
   git commit -m "chore(deps): update dependencies

   - Update Kotlin to 1.9.22
   - Update Compose BOM to 2024.01.00
   - Fix security vulnerability in okhttp (CVE-2024-XXXX)

   All tests passing. Tested on emulator."
   ```

**Dependency Update Strategy**:
- **Security patches**: Apply immediately
- **Major version bumps**: Test thoroughly, may require code changes
- **Minor/patch updates**: Apply monthly
- **Breaking changes**: Document in RELEASE.md, plan migration

**Security Scanning**:
```bash
# Check for known vulnerabilities
./gradlew dependencyCheckAnalyze

# Review security report
open build/reports/dependency-check-report.html
```

## 📚 Resources

### Official Documentation
- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

### Learning Resources
- [Now in Android sample app](https://github.com/android/nowinandroid) - Google's reference architecture
- [Compose samples](https://github.com/android/compose-samples)

## 🔄 Versioning & Updates

- **Target SDK**: Latest stable (Android 14+ / API 34+)
- **Compile SDK**: Always latest
- **Min SDK**: Android 8.0 (API 26) minimum recommended for 2025
- **Kotlin**: Latest stable version
- **Compose BOM**: Use Bill of Materials for version alignment
- **Dependency updates**: Monthly review, test thoroughly

---

## 🤖 Notes for Claude

When working on this project:
1. **Always reference this document** before implementing features
2. **Suggest architecture improvements** if you notice violations
3. **Propose modern alternatives** when encountering legacy code
4. **Prioritize user safety** - this app deals with bike safety and location
5. **Think about offline-first** - bikes may go through areas with poor connectivity
6. **Battery efficiency matters** - location tracking can drain battery
7. **Test in various conditions** - different speeds, GPS accuracy levels, lighting conditions
8. **Always test on emulator** - when a feature is "working", install debug build and validate on emulator before considering it complete
9. **Automatically update TODO.md and RELEASE.md** - user does NOT need to ask; this is mandatory for every feature workflow (start, progress, complete)
10. **Commit small and frequently** - NEVER wait for full feature completion; commit after each logical unit (file, function, task) with conventional commit messages
11. **Every specify session ends with a release** - prepare PR, version bump, tag, APK build, and GitHub release per constitution requirements

---

**This document is a living standard. Update as Android development practices evolve.**

## Active Technologies
- Kotlin 2.0.21 with Jetpack Compose + Play Services Location 21.3.0, Jetpack Compose (BOM 2024.11.00), Material 3 (001-speed-tracking)
- None for MVP (no persistence of location data) (001-speed-tracking)
- DataStore Preferences (key-value pairs, local device only) (001-settings-infrastructure)
- Kotlin 2.0.21 + Room 2.6.1, Play Services Location 21.3.0, Jetpack Compose BOM 2024.11.00, Kotlin Coroutines 1.9.0 (002-core-ride-recording)
- Room database (SQLite), local device only, no cloud sync in v0.3.0 (002-core-ride-recording)

## Recent Changes
- 001-speed-tracking: Added Kotlin 2.0.21 with Jetpack Compose + Play Services Location 21.3.0, Jetpack Compose (BOM 2024.11.00), Material 3
