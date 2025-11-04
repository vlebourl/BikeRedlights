# DataStore Schema Contract: Basic Settings Infrastructure

**Feature**: 001-settings-infrastructure
**Contract Type**: Data Persistence Layer
**Date**: 2025-11-04
**Version**: 1.0.0

## Overview

This document defines the contract for settings persistence using Jetpack DataStore Preferences. It specifies key names, types, allowed values, defaults, and behavior guarantees.

---

## DataStore Configuration

**DataStore Name**: `user_settings`
**Storage Type**: Preferences DataStore (Protocol Buffers)
**File Location**: `/data/data/com.example.bikeredlights/files/datastore/user_settings.preferences_pb`
**Access Pattern**: Reactive (Flow-based reads)

**Context Creation**:
```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings"
)
```

---

## Schema Definition

### 1. Units System Preference

**Key Name**: `units_system`
**Type**: String
**Purpose**: User's preferred measurement system for speed and distance

**Contract**:
```
Key: "units_system"
Type: String
Allowed Values: "metric" | "imperial"
Default: "metric"
Nullable: No (always has a value or uses default)
```

**Behavior**:
- **On Read**:
  - Key missing → emit `"metric"`
  - Key present with `"metric"` → emit `"metric"`
  - Key present with `"imperial"` → emit `"imperial"`
  - Key present with unknown value → emit `"metric"` (graceful fallback)
- **On Write**:
  - Only accepts `"metric"` or `"imperial"` (enforced by domain layer)
  - Write failures logged but not thrown
  - Atomic write (all-or-nothing)

**Example Read**:
```kotlin
val unitsFlow: Flow<String> = dataStore.data
    .catch { exception ->
        Log.e("Settings", "Error reading units_system", exception)
        emit(emptyPreferences())
    }
    .map { preferences ->
        preferences[stringPreferencesKey("units_system")] ?: "metric"
    }
```

**Example Write**:
```kotlin
suspend fun setUnits(units: String) {
    try {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("units_system")] = units
        }
    } catch (e: Exception) {
        Log.e("Settings", "Error writing units_system", e)
    }
}
```

---

### 2. GPS Accuracy Preference

**Key Name**: `gps_accuracy`
**Type**: String
**Purpose**: User's preferred GPS update frequency (battery vs accuracy trade-off)

**Contract**:
```
Key: "gps_accuracy"
Type: String
Allowed Values: "battery_saver" | "high_accuracy"
Default: "high_accuracy"
Nullable: No (always has a value or uses default)
```

**Behavior**:
- **On Read**:
  - Key missing → emit `"high_accuracy"`
  - Key present with `"battery_saver"` → emit `"battery_saver"`
  - Key present with `"high_accuracy"` → emit `"high_accuracy"`
  - Key present with unknown value → emit `"high_accuracy"` (graceful fallback)
- **On Write**:
  - Only accepts `"battery_saver"` or `"high_accuracy"` (enforced by domain layer)
  - Write failures logged but not thrown
  - Atomic write (all-or-nothing)

**Interval Mapping** (for reference, not part of DataStore schema):
| Stored Value | Location Update Interval |
|--------------|--------------------------|
| `"high_accuracy"` | 1000ms (1 second) |
| `"battery_saver"` | 3000-5000ms (3-5 seconds) |

---

### 3. Auto-Pause Enabled Preference

**Key Name**: `auto_pause_enabled`
**Type**: Boolean
**Purpose**: Whether auto-pause feature is active for ride recording

**Contract**:
```
Key: "auto_pause_enabled"
Type: Boolean
Allowed Values: true | false
Default: false
Nullable: No (Boolean type, always true or false)
```

**Behavior**:
- **On Read**:
  - Key missing → emit `false`
  - Key present with `true` → emit `true`
  - Key present with `false` → emit `false`
- **On Write**:
  - Only accepts `true` or `false` (native Boolean type)
  - Write failures logged but not thrown
  - Atomic write (all-or-nothing)

---

### 4. Auto-Pause Threshold Preference

**Key Name**: `auto_pause_minutes`
**Type**: Int
**Purpose**: Duration (in minutes) rider must be stationary before auto-pause triggers

**Contract**:
```
Key: "auto_pause_minutes"
Type: Int
Allowed Values: 1, 2, 3, 5, 10, 15
Default: 5
Nullable: No (Int type, always has a value)
```

**Behavior**:
- **On Read**:
  - Key missing → emit `5`
  - Key present with valid value (1, 2, 3, 5, 10, 15) → emit value
  - Key present with invalid value (e.g., 4, 0, 16) → emit `5` (graceful fallback)
- **On Write**:
  - Only accepts 1, 2, 3, 5, 10, 15 (validated by `AutoPauseConfig` before write)
  - Write failures logged but not thrown
  - Atomic write (all-or-nothing)

**Validation**: Enforced in domain layer (`AutoPauseConfig.init` block) before DataStore write.

---

## Complete Schema Table

| Key | Type | Default | Allowed Values | Validation Layer |
|-----|------|---------|----------------|------------------|
| `units_system` | String | `"metric"` | `"metric"`, `"imperial"` | Domain (enum mapping) |
| `gps_accuracy` | String | `"high_accuracy"` | `"battery_saver"`, `"high_accuracy"` | Domain (enum mapping) |
| `auto_pause_enabled` | Boolean | `false` | `true`, `false` | Native Boolean type |
| `auto_pause_minutes` | Int | `5` | `1`, `2`, `3`, `5`, `10`, `15` | Domain (`AutoPauseConfig` init) |

---

## Error Handling Contract

### Read Errors

**Scenarios**:
1. DataStore file missing (first launch)
2. DataStore file corrupted
3. I/O error (disk full, permissions)
4. Unexpected data types

**Guaranteed Behavior**:
- ✅ Never throw exceptions to caller
- ✅ Always emit default values
- ✅ Log errors for debugging
- ✅ Flow continues (doesn't terminate)

**Implementation**:
```kotlin
val someSettingFlow: Flow<SomeType> = dataStore.data
    .catch { exception ->
        // Catch-and-recover pattern (DataStore best practice)
        Log.e("SettingsRepository", "Error reading setting", exception)
        emit(emptyPreferences()) // Emit empty → triggers default logic
    }
    .map { preferences ->
        // Map to domain type with default fallback
        preferences[SOME_KEY] ?: DEFAULT_VALUE
    }
```

### Write Errors

**Scenarios**:
1. Disk full (no space for write)
2. I/O error (device storage failure)
3. Unexpected exceptions

**Guaranteed Behavior**:
- ✅ Never throw exceptions to caller
- ✅ Log errors for debugging
- ✅ UI remains responsive (no crashes)
- ✅ Previous value retained (atomic write failure)

**Implementation**:
```kotlin
suspend fun setSomeSetting(value: SomeType) {
    try {
        dataStore.edit { preferences ->
            preferences[SOME_KEY] = value
        }
    } catch (exception: Exception) {
        // Log and continue (graceful degradation)
        Log.e("SettingsRepository", "Error writing setting", exception)
        // Don't rethrow - app continues with previous value
    }
}
```

---

## Concurrency Guarantees

**DataStore Built-In Guarantees**:
- ✅ Thread-safe reads and writes
- ✅ Atomic writes (all-or-nothing)
- ✅ Serialized writes (no race conditions)
- ✅ Reads always return consistent snapshot

**Feature-Specific Guarantees**:
- Multiple rapid writes (e.g., user tapping quickly) → last write wins
- Reads during writes → returns value before write completes
- No partial writes (either all 4 keys updated or none)

**Example: Rapid Button Taps**:
```
Time 0ms: User taps "Imperial" → Write queued
Time 50ms: User taps "Metric" → Write queued
Time 100ms: First write starts → Sets "imperial"
Time 120ms: Second write starts → Sets "metric"
Result: "metric" persisted (last write wins)
```

---

## Performance Contract

**Read Performance**:
- Cold read (first time): < 10ms
- Hot read (cached): < 1ms
- Flow emissions: < 1ms after DataStore change
- No blocking main thread

**Write Performance**:
- Write latency: < 50ms (99th percentile)
- No UI jank (async writes in coroutine scope)
- No batching delays (immediate persistence)

**Memory Footprint**:
- DataStore overhead: ~100KB (library)
- Data size: < 1KB (4 small key-value pairs)
- Flow caching: ~200 bytes per Flow

**Storage Footprint**:
- File size: < 1KB
- Negligible impact on device storage
- No growth over time (fixed schema)

---

## Migration Contract

### v0.2.0 (Initial Release)

**Migration Required**: No
**Behavior**:
- First launch: All keys missing → defaults applied
- No DataMigration implementation needed

### Future Versions (For Reference)

**Adding New Keys**:
- Add key definition to `PreferencesKeys`
- Provide default value in read logic
- No migration needed (backward compatible)

**Renaming Keys**:
- Implement `DataMigration<Preferences>`
- Read old key, write new key, delete old key
- Migration runs automatically on first read after update

**Changing Types**:
- Add new key with new type
- Implement migration to copy old → new
- Delete old key after migration
- Example: `"auto_pause_minutes"` String → Int (not needed, already Int)

**Example Future Migration**:
```kotlin
object SettingsMigrationV2 : DataMigration<Preferences> {
    private val OLD_KEY = stringPreferencesKey("old_units")
    private val NEW_KEY = stringPreferencesKey("units_system")

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        return currentData.contains(OLD_KEY) && !currentData.contains(NEW_KEY)
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        return currentData.toMutablePreferences().apply {
            val oldValue = this[OLD_KEY]
            this[NEW_KEY] = oldValue ?: "metric"
            this.remove(OLD_KEY)
        }
    }

    override suspend fun cleanUp() {
        // No-op (no external cleanup needed)
    }
}
```

---

## Testing Contract

### Unit Tests (Repository Layer)

**Read Tests**:
- ✅ Empty DataStore → emits default values
- ✅ Populated DataStore → emits stored values
- ✅ Corrupted DataStore → emits defaults (error caught)
- ✅ Unknown values → emit defaults (graceful fallback)

**Write Tests**:
- ✅ Write success → value persisted
- ✅ Write failure (mocked I/O error) → logged, no crash
- ✅ Multiple rapid writes → last write wins

**Example Test**:
```kotlin
@Test
fun `empty DataStore emits default units`() = runTest {
    val repository = SettingsRepositoryImpl(testDataStore)
    val units = repository.unitsSystem.first()
    assertThat(units).isEqualTo(UnitsSystem.METRIC)
}
```

### Integration Tests (Instrumented)

**Persistence Tests**:
- ✅ Write → read → value matches
- ✅ Write → restart app → read → value matches
- ✅ Write all 4 keys → restart app → all 4 values match

**Example Test**:
```kotlin
@Test
fun unitsSettingPersistsAcrossAppRestart() = runTest {
    // Write
    repository.setUnitsSystem(UnitsSystem.IMPERIAL)
    delay(100) // Allow DataStore write to complete

    // Restart (simulate by recreating repository with same DataStore)
    val newRepository = SettingsRepositoryImpl(testDataStore)

    // Read
    val units = newRepository.unitsSystem.first()
    assertThat(units).isEqualTo(UnitsSystem.IMPERIAL)
}
```

---

## Backward Compatibility Contract

**v0.2.0 → v0.3.0+**:
- Existing keys: Never removed or renamed without migration
- Existing values: Always readable (no breaking format changes)
- New keys: Added without affecting existing keys
- Defaults: May change in future versions (users see new defaults on reset only)

**Deprecation Policy** (for future):
- Keys deprecated in version X
- Migration provided in version X
- Old keys removed in version X+2 (2 versions later)
- Example: Deprecated in v0.4.0, removed in v0.6.0

---

## Security & Privacy Contract

**Data Classification**: Non-Sensitive
- Settings are user preferences, not personal data
- No PII (personally identifiable information)
- No passwords, tokens, or credentials

**Access Control**:
- App-private storage (no other apps can read)
- No world-readable permissions
- Requires root or ADB backup to access

**Encryption**:
- Not encrypted at rest (not required for non-sensitive data)
- Device encryption protects if enabled by user
- No network transmission (local-only)

**Data Deletion**:
- Deleted on app uninstall
- Deleted on "Clear Data" from app settings
- Not deleted on app update
- Not included in Android Auto Backup (by default)

---

## Summary

**4 Keys Defined**:
1. `units_system` (String): `"metric"` | `"imperial"` (default: `"metric"`)
2. `gps_accuracy` (String): `"battery_saver"` | `"high_accuracy"` (default: `"high_accuracy"`)
3. `auto_pause_enabled` (Boolean): `true` | `false` (default: `false`)
4. `auto_pause_minutes` (Int): `1`, `2`, `3`, `5`, `10`, `15` (default: `5`)

**Guarantees**:
- ✅ Thread-safe reads/writes
- ✅ Atomic writes (all-or-nothing)
- ✅ Graceful error handling (never crash)
- ✅ Default fallbacks (always functional)
- ✅ Fast performance (< 50ms writes, < 1ms reads)

**Contract Version**: 1.0.0
**Breaking Changes**: None allowed without migration
**Stability**: Stable (ready for production)
