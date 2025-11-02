# APK Signing Setup for BikeRedlights

> **Purpose**: Configure release APK signing for BikeRedlights to enable distribution via GitHub Releases
> **Security**: This guide includes critical security practices for keystore management

## Overview

Android requires all APKs to be digitally signed before they can be installed. For release builds distributed via GitHub, you must configure a release signing key.

---

## Initial Setup (One-Time)

### Step 1: Generate Release Keystore

```bash
# Navigate to project root
cd /Users/vlb/AndroidStudioProjects/BikeRedlights

# Generate keystore (valid for 10,000 days)
keytool -genkey -v -keystore bikeredlights-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias bikeredlights-release

# You will be prompted for:
# - Keystore password (SAVE THIS SECURELY!)
# - Key password (SAVE THIS SECURELY!)
# - Your name/organization details
# - Confirm with "yes"
```

**Important**: The keystore password and key password should be strong and stored securely (e.g., in 1Password, LastPass, or similar).

### Step 2: Create `keystore.properties` File

Create a file at the project root: `keystore.properties`

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=bikeredlights-release
storeFile=../bikeredlights-release.jks
```

**⚠️ CRITICAL**: This file contains secrets and MUST NOT be committed to git!

### Step 3: Update `.gitignore`

Ensure these entries exist in your `.gitignore`:

```gitignore
# Keystore files (NEVER COMMIT THESE!)
*.jks
*.keystore
keystore.properties

# Release APKs (optional - may want to exclude from repo)
*.apk
```

### Step 4: Update `app/build.gradle.kts`

Add signing configuration to your `app/build.gradle.kts`:

```kotlin
import java.util.Properties
import java.io.FileInputStream

android {
    namespace = "com.example.bikeredlights"
    compileSdk = 36

    // Load keystore properties
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()

    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.bikeredlights"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // Enable ProGuard/R8
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")  // Use release signing
        }
        debug {
            // Debug builds use auto-generated debug keystore
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}
```

### Step 5: Create/Update ProGuard Rules

Ensure `app/proguard-rules.pro` exists with BikeRedlights-specific rules:

```proguard
# BikeRedlights ProGuard Rules

# Keep application class
-keep class com.example.bikeredlights.BikeRedlightsApplication { *; }

# Keep all model classes (data classes used with Room/Retrofit)
-keep class com.example.bikeredlights.domain.model.** { *; }
-keep class com.example.bikeredlights.data.model.** { *; }

# Room database rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Retrofit rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**

# Keep generic signature of Call, Response (R8 full mode strips signatures)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Gson rules (if using Gson)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Location services
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**
```

---

## Building Signed Release APK

### Command Line

```bash
# Build release APK
./gradlew assembleRelease

# Output location:
# app/build/outputs/apk/release/app-release.apk
```

### Verify Signing

```bash
# Check APK signature
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Expected output:
# jar verified.
```

---

## Security Best Practices

### ⚠️ CRITICAL SECURITY RULES

1. **NEVER commit keystore files to git**
   - `bikeredlights-release.jks` MUST stay local
   - Add to `.gitignore` immediately
   - Verify: `git status` should not show keystore files

2. **NEVER commit `keystore.properties`**
   - Contains plaintext passwords
   - Must be in `.gitignore`

3. **Backup keystore securely**
   - Store in password manager (1Password, LastPass, etc.)
   - Keep offline backup on encrypted drive
   - **If you lose the keystore, you cannot update the app!**

4. **Use strong passwords**
   - Minimum 16 characters
   - Mix of letters, numbers, symbols
   - Different password for keystore vs key

5. **Limit keystore access**
   - Only authorized developers should have access
   - Never share via email, Slack, or unencrypted channels

### GitHub Actions / CI/CD

If setting up automated builds:

1. Store keystore as base64 in GitHub Secrets:
   ```bash
   base64 bikeredlights-release.jks | pbcopy
   # Add as GitHub Secret: KEYSTORE_BASE64
   ```

2. Store passwords in GitHub Secrets:
   - `KEYSTORE_PASSWORD`
   - `KEY_PASSWORD`
   - `KEY_ALIAS`

3. Decode in workflow:
   ```yaml
   - name: Decode keystore
     run: |
       echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > bikeredlights-release.jks
   ```

---

## Testing Release Build

### Before Creating GitHub Release

1. **Build signed APK**:
   ```bash
   ./gradlew assembleRelease
   ```

2. **Install on emulator**:
   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

3. **Test functionality**:
   - App launches successfully
   - All features work as expected
   - No crashes or ANR events
   - Location permissions work correctly
   - Dark mode displays correctly

4. **Verify ProGuard didn't break anything**:
   - Test all critical user flows
   - Check for missing classes or methods
   - Review ProGuard mapping file: `app/build/outputs/mapping/release/mapping.txt`

### Debugging ProGuard Issues

If release build crashes but debug build works:

1. **Check logcat for ClassNotFoundException or MethodNotFoundException**

2. **Add keep rules for affected classes**:
   ```proguard
   -keep class com.example.bikeredlights.problematic.Class { *; }
   ```

3. **Use mapping file to deobfuscate stack traces**:
   ```bash
   retrace.sh app/build/outputs/mapping/release/mapping.txt stacktrace.txt
   ```

---

## Troubleshooting

### "keystore.properties not found"

**Solution**: Create `keystore.properties` in project root (see Step 2)

### "Keystore was tampered with, or password was incorrect"

**Solution**: Verify password in `keystore.properties` matches keystore creation password

### "Cannot find signing config 'release'"

**Solution**: Ensure `signingConfigs` block is defined before `buildTypes` in `build.gradle.kts`

### "ProGuard removed my classes"

**Solution**: Add appropriate keep rules to `proguard-rules.pro` (see Step 5)

---

## Version Upgrade Scenario

If you need to update BikeRedlights years from now:

1. **You MUST use the same keystore** to sign the update
2. If keystore is lost, users cannot update (must uninstall/reinstall)
3. This is why keystore backup is critical

---

## Checklist

Before your first release:

- [ ] Keystore generated with strong password
- [ ] `keystore.properties` created with correct values
- [ ] Both files added to `.gitignore`
- [ ] `.gitignore` committed to repository
- [ ] Keystore backed up to secure location (password manager)
- [ ] Passwords documented separately from keystore
- [ ] `app/build.gradle.kts` updated with signing configuration
- [ ] ProGuard rules created/updated
- [ ] Test build: `./gradlew assembleRelease` succeeds
- [ ] Test installation on emulator
- [ ] All features work in release build
- [ ] Verified keystore NOT in git: `git status`

---

**Constitution Compliance**: This document supports the "Release Pattern & Workflow" requirement (Constitution v1.3.0, lines 384-453).
