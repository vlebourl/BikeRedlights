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

**Why This Matters**: Unit tests validate logic, but emulator testing catches Android framework integration issues, UI rendering problems, and runtime behavior that only appears on actual Android. For a safety-critical app like BikeRedlights, this is non-negotiable.

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

---

**This document is a living standard. Update as Android development practices evolve.**
