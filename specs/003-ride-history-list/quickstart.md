# Quickstart: Ride History and List View

**Feature**: 003-ride-history-list
**Phase**: 1 (Design & Contracts)
**Date**: 2025-11-06
**Purpose**: Guide developers through local setup and development workflow for ride history feature

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 17 (OpenJDK)**
   ```bash
   # macOS with Homebrew
   brew install openjdk@17

   # Set environment variables (add to ~/.zshrc or ~/.bash_profile)
   export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
   export JAVA_HOME="/opt/homebrew/opt/openjdk@17"

   # Reload shell
   source ~/.zshrc

   # Verify
   java -version  # Should show OpenJDK 17
   ```

2. **Android Studio** (Latest stable - Hedgehog or newer)
   - Download: https://developer.android.com/studio
   - Includes Android SDK, emulator, and Gradle

3. **Git** (for version control)
   ```bash
   # Verify installation
   git --version
   ```

### Android SDK Requirements

- **compileSdk**: 35
- **targetSdk**: 35
- **minSdk**: 34 (Android 14+)
- **Build Tools**: 35.0.0 (installed automatically by Android Studio)

### Emulator Setup (MANDATORY for Testing)

1. Open Android Studio → Device Manager
2. Create new device:
   - Device: Pixel 6 (or similar)
   - System Image: API Level 34 (Android 14)
   - Ensure Google Play image is selected
3. Start emulator before running app

---

## Project Setup

### 1. Clone Repository

```bash
cd ~/AndroidStudioProjects
git clone https://github.com/vlebourl/BikeRedlights.git
cd BikeRedlights
```

### 2. Checkout Feature Branch

```bash
# Fetch all branches
git fetch --all

# Checkout ride history feature branch
git checkout 003-ride-history-list

# Verify you're on correct branch
git branch  # Should show * 003-ride-history-list
```

### 3. Open in Android Studio

1. Launch Android Studio
2. File → Open → Select `BikeRedlights` directory
3. Wait for Gradle sync to complete (may take 2-5 minutes on first run)
4. Verify no sync errors in Build tab

### 4. Configure JDK in Android Studio

1. Android Studio → Settings (Ctrl+Alt+S on Windows/Linux, Cmd+, on macOS)
2. Build, Execution, Deployment → Build Tools → Gradle
3. Gradle JDK: Select "OpenJDK 17" (should auto-detect from JAVA_HOME)
4. Apply → OK

---

## Build & Run

### Build Project

```bash
# From project root directory
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"

# Clean build
./gradlew clean assembleDebug

# Or from Android Studio: Build → Rebuild Project
```

**Expected Output**:
```
BUILD SUCCESSFUL in 45s
```

### Run Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test file
./gradlew test --tests "RideHistoryViewModelTest"

# Generate test coverage report
./gradlew testDebugUnitTest jacocoTestReport
# Report available at: app/build/reports/jacoco/test/html/index.html
```

### Install on Emulator

```bash
# Ensure emulator is running (check with: adb devices)
~/Library/Android/sdk/platform-tools/adb devices

# Install debug APK
./gradlew installDebug

# Launch app manually or via command:
~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.bikeredlights/.MainActivity
```

---

## Development Workflow

### Creating a New Component

**Example**: Create RideListItemCard composable

1. **Navigate to UI layer**:
   ```
   app/src/main/java/com/example/bikeredlights/ui/components/history/
   ```

2. **Create new file**: `RideListItemCard.kt`

3. **Write composable**:
   ```kotlin
   package com.example.bikeredlights.ui.components.history

   import androidx.compose.material3.Card
   import androidx.compose.runtime.Composable
   import com.example.bikeredlights.domain.model.display.RideListItem

   @Composable
   fun RideListItemCard(
       ride: RideListItem,
       onClick: () -> Unit,
       modifier: Modifier = Modifier
   ) {
       Card(
           modifier = modifier.clickable { onClick() }
       ) {
           // Implement UI
       }
   }
   ```

4. **Write tests**: `app/src/androidTest/java/com/example/bikeredlights/ui/components/history/RideListItemCardTest.kt`

5. **Commit**:
   ```bash
   git add app/src/main/java/com/example/bikeredlights/ui/components/history/RideListItemCard.kt
   git commit -m "feat(ui): create RideListItemCard composable

   - Add Material 3 Card with ride summary
   - Support click interaction
   - Display ride name, date, stats"
   ```

### Creating a New Use Case

**Example**: Create GetAllRidesUseCase

1. **Navigate to domain layer**:
   ```
   app/src/main/java/com/example/bikeredlights/domain/usecase/
   ```

2. **Create new file**: `GetAllRidesUseCase.kt`

3. **Write use case**:
   ```kotlin
   package com.example.bikeredlights.domain.usecase

   import com.example.bikeredlights.domain.model.Ride
   import com.example.bikeredlights.domain.repository.RideRepository
   import kotlinx.coroutines.flow.Flow
   import javax.inject.Inject

   class GetAllRidesUseCase @Inject constructor(
       private val rideRepository: RideRepository
   ) {
       operator fun invoke(): Flow<List<Ride>> {
           return rideRepository.getAllRides()
       }
   }
   ```

4. **Write tests**: `app/src/test/java/com/example/bikeredlights/domain/usecase/GetAllRidesUseCaseTest.kt`

5. **Provide in Hilt module**:
   ```kotlin
   // In AppModule.kt
   @Provides
   fun provideGetAllRidesUseCase(
       rideRepository: RideRepository
   ): GetAllRidesUseCase = GetAllRidesUseCase(rideRepository)
   ```

6. **Commit**:
   ```bash
   git add app/src/main/java/com/example/bikeredlights/domain/usecase/GetAllRidesUseCase.kt
   git commit -m "feat(domain): add GetAllRidesUseCase

   - Return Flow of all rides from repository
   - Enable reactive UI updates when rides change"
   ```

### Creating a New ViewModel

**Example**: Create RideHistoryViewModel

1. **Navigate to UI layer**:
   ```
   app/src/main/java/com/example/bikeredlights/ui/viewmodel/
   ```

2. **Create new file**: `RideHistoryViewModel.kt`

3. **Write ViewModel**:
   ```kotlin
   package com.example.bikeredlights.ui.viewmodel

   import androidx.lifecycle.ViewModel
   import androidx.lifecycle.viewModelScope
   import dagger.hilt.android.lifecycle.HiltViewModel
   import kotlinx.coroutines.flow.StateFlow
   import javax.inject.Inject

   @HiltViewModel
   class RideHistoryViewModel @Inject constructor(
       private val getAllRidesUseCase: GetAllRidesUseCase,
       // ... other use cases
   ) : ViewModel() {

       private val _uiState = MutableStateFlow<RideHistoryUiState>(Loading)
       val uiState: StateFlow<RideHistoryUiState> = _uiState.asStateFlow()

       init {
           loadRides()
       }

       private fun loadRides() {
           // Implementation
       }
   }
   ```

4. **Write tests**: `app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModelTest.kt`
   - Use MockK for mocking use cases
   - Use Turbine for Flow testing

5. **Commit**:
   ```bash
   git add app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideHistoryViewModel.kt
   git commit -m "feat(ui): add RideHistoryViewModel

   - Manage ride list UI state
   - Integrate GetAllRidesUseCase
   - Support sort/filter preferences"
   ```

---

## Testing on Emulator

### GPS Location Simulation

**Critical for BikeRedlights**: Must test with simulated GPS locations

**Method 1: Emulator Extended Controls**
1. Start emulator
2. Click ⋮ (Extended controls)
3. Location tab
4. Enter coordinates manually (e.g., 37.422, -122.084)
5. Click "Send"

**Method 2: ADB Commands**
```bash
# Send single location
~/Library/Android/sdk/platform-tools/adb emu geo fix -122.084 37.422

# Simulate movement (multiple locations)
~/Library/Android/sdk/platform-tools/adb emu geo fix -122.084 37.422
sleep 2
~/Library/Android/sdk/platform-tools/adb emu geo fix -122.085 37.423
sleep 2
~/Library/Android/sdk/platform-tools/adb emu geo fix -122.086 37.424
```

**Method 3: GPX File Playback**
1. Extended Controls → Location → Load GPX/KML
2. Select GPX file with route
3. Click "Play" to simulate movement
4. Adjust speed slider for cycling pace

### Viewing Logs

```bash
# Real-time logs (filtered for BikeRedlights)
~/Library/Android/sdk/platform-tools/adb logcat | grep BikeRedlights

# Specific tags
~/Library/Android/sdk/platform-tools/adb logcat -s RideHistory:* BikeRedlights:*

# Save logs to file
~/Library/Android/sdk/platform-tools/adb logcat -d > logs.txt
```

### Taking Screenshots

```bash
# Capture screenshot from emulator
~/Library/Android/sdk/platform-tools/adb exec-out screencap -p > /tmp/screenshot.png

# Or use Android Studio: View → Tool Windows → Logcat → Camera icon
```

---

## Common Issues & Solutions

### Issue: Gradle Sync Fails with "Java Version" Error

**Solution**:
```bash
# Verify Java version
java -version  # Must show OpenJDK 17

# If wrong version, set JAVA_HOME correctly
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export PATH="$JAVA_HOME/bin:$PATH"

# Restart Android Studio
```

### Issue: Emulator Not Detected

**Solution**:
```bash
# Check emulator status
~/Library/Android/sdk/platform-tools/adb devices

# If offline, restart ADB
~/Library/Android/sdk/platform-tools/adb kill-server
~/Library/Android/sdk/platform-tools/adb start-server
~/Library/Android/sdk/platform-tools/adb devices
```

### Issue: App Crashes on Launch

**Solution**:
```bash
# View crash logs
~/Library/Android/sdk/platform-tools/adb logcat -d -s AndroidRuntime:E *:F

# Check for permission issues (location, notification)
~/Library/Android/sdk/platform-tools/adb shell pm grant com.example.bikeredlights android.permission.ACCESS_FINE_LOCATION
```

### Issue: Database Schema Mismatch

**Solution**:
```bash
# Uninstall app to clear database
~/Library/Android/sdk/platform-tools/adb uninstall com.example.bikeredlights

# Reinstall
./gradlew installDebug
```

### Issue: Compose Preview Not Rendering

**Solution**:
1. Android Studio → File → Invalidate Caches → Invalidate and Restart
2. Ensure `@Preview` annotation is present on composable
3. Check that composable has default parameter values for preview

---

## Code Style & Quality

### Lint Checks

```bash
# Run lint analysis
./gradlew lint

# View report
open app/build/reports/lint-results-debug.html
```

### Code Formatting

```bash
# Auto-format Kotlin files (if ktlint configured)
./gradlew ktlintFormat

# Or use Android Studio: Code → Reformat Code (Ctrl+Alt+L)
```

### Test Coverage

```bash
# Generate coverage report
./gradlew testDebugUnitTest jacocoTestReport

# Open report
open app/build/reports/jacoco/test/html/index.html

# Target: 80%+ coverage for ViewModels, UseCases, Repositories
```

---

## Git Workflow

### Small, Frequent Commits (MANDATORY)

**Rule**: Commit after each logical unit (~200 lines max)

**Good commit pattern**:
```bash
git add app/src/main/java/com/example/bikeredlights/domain/model/display/RideListItem.kt
git commit -m "feat(domain): add RideListItem display model

- Add formatted display fields
- Include startTimeMillis for sorting
- Map from Ride domain model"

git add app/src/main/java/com/example/bikeredlights/domain/usecase/GetAllRidesUseCase.kt
git commit -m "feat(domain): add GetAllRidesUseCase

- Return Flow of all rides
- Enable reactive updates"
```

**Bad commit pattern**:
```bash
# ❌ DON'T DO THIS - too large, unclear changes
git add .
git commit -m "Complete history feature"
```

### Push Frequently

```bash
# Push after 2-5 commits or end of work session
git push origin 003-ride-history-list

# First time pushing branch
git push -u origin 003-ride-history-list
```

### Conventional Commit Format

```
<type>(<scope>): <subject>

[optional body]
```

**Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
**Scopes**: `domain`, `data`, `ui`, `di`, `test`

**Examples**:
- `feat(ui): create RideHistoryScreen with LazyColumn`
- `test(domain): add unit tests for GetAllRidesUseCase`
- `refactor(data): extract formatting logic to FormatUtils`
- `fix(ui): correct swipe-to-delete animation timing`

---

## Next Steps

After local development is complete:

1. **Run Full Test Suite**:
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest  # Runs on emulator
   ```

2. **Validate on Emulator**:
   - Install debug APK
   - Test all user stories from spec.md
   - Verify dark mode, rotation, accessibility

3. **Create Pull Request**:
   ```bash
   # Ensure all commits are pushed
   git push origin 003-ride-history-list

   # Create PR via GitHub UI or gh CLI
   gh pr create --title "feat: Ride History and List View (v0.4.0)" \
                --body "Implements F3 ride history feature per spec"
   ```

4. **After PR Merge**:
   - Follow release workflow (see CLAUDE.md)
   - Bump version to v0.4.0 (versionCode = 400)
   - Create Git tag and GitHub Release
   - Build signed APK

---

## Useful Resources

**Project Documentation**:
- CLAUDE.md: Development standards and constitution
- RELEASE.md: Version history and changelog
- TODO.md: Feature progress tracking
- specs/003-ride-history-list/spec.md: Feature requirements
- specs/003-ride-history-list/plan.md: Implementation plan
- specs/003-ride-history-list/research.md: Architectural decisions

**Android Development**:
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Material 3: https://m3.material.io/
- Room Database: https://developer.android.com/training/data-storage/room
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Hilt DI: https://dagger.dev/hilt/

**Testing**:
- JUnit 5: https://junit.org/junit5/
- MockK: https://mockk.io/
- Turbine: https://github.com/cashapp/turbine
- Compose Testing: https://developer.android.com/jetpack/compose/testing

**BikeRedlights GitHub**:
- Repository: https://github.com/vlebourl/BikeRedlights
- Issues: https://github.com/vlebourl/BikeRedlights/issues
- Releases: https://github.com/vlebourl/BikeRedlights/releases
