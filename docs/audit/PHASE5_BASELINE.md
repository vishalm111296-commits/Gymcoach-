# GymCoach Phase 5 Forensic Baseline

**Generated:** 2026-08-24
**Repository:** vishalm111266-beep/GymCoach
**Main SHA:** 9e6c07e0bf58a460a3cd4d75c06688d76b4b7f2f

---

## Repository Identity

- **Owner:** vishalm111266-beep
- **Repository:** GymCoach
- **Default Branch:** main
- **Main SHA:** 9e6c07e0bf58a460a3cd4d75c06688d76b4b7f2f
- **Working Tree State:** clean (all committed)

---

## Database State

### @Database Annotation

```kotlin
@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        ProgramEntity::class,
        ProgramDayEntity::class,
        ProgramExerciseEntity::class,
        PersonalRecordEntity::class,
        BodyMeasurementEntity::class,
        FavoriteExerciseEntity::class,
        ExerciseSubstitutionEntity::class,
        MuscleEntity::class,
        EquipmentEntity::class,
        ExerciseMuscleEntity::class,
        ExerciseEquipmentEntity::class,
        ExerciseAliasEntity::class
    ],
    version = 5,
    exportSchema = false  // <-- CRITICAL: exportSchema is false
)
abstract class GymCoachDatabase : RoomDatabase() {
    // ... abstract DAO declarations
}
```

### Registered Migrations

| Migration | From → To | SQL Complexity |
|-----------|-----------|----------------|
| MIGRATION_1_2 | 1 → 2 | Creates `workouts`, `workout_exercises`, `workout_sets` tables + indices |
| MIGRATION_2_3 | 2 → 3 | Creates ALL major tables + adds V-taper columns to exercises + FTS4 virtual table |

### Migration Gaps

- **MIGRATION_3_4**: NOT REGISTERED ❌
- **MIGRATION_4_5**: NOT REGISTERED ❌
- **Chain:** 1→2→3 (STOPS at 3, target version is 5) ❌

### fallbackToDestructiveMigration()

```kotlin
// In Database.create():
.fallbackToDestructiveMigration()  // ✅ PRESENT — high risk for fitness app
```

**Risk:** For a personal fitness app containing workout history, sets, PRs, measurements, and profile — accidental destruction during normal upgrade is unacceptable. Room will drop and recreate all tables when a migration path is unavailable.

---

## Entity-First Schema Audit

### ExerciseEntity (table: `exercises`)

| Field | Type | Nullable | Default | Column Name |
|-------|------|----------|---------|-------------|
| id | Long | NO | autoGenerate | `id` |
| name | String | NO | — | `name` |
| description | String | NO | — | `description` |
| muscleGroup | String | NO | — | `muscleGroup` |
| equipment | String | NO | — | `equipment` |
| difficulty | String | NO | — | `difficulty` |
| secondaryMuscles | String | YES | "" | `secondaryMuscles` |
| instructions | String | YES | "" | `instructions` |
| tips | String | YES | "" | `tips` |
| commonMistakes | String | YES | "" | `commonMistakes` |
| safetyNotes | String | YES | "" | `safetyNotes` |
| recommendedRepRange | String | YES | "" | `recommendedRepRange` |
| recommendedRestTime | String | YES | "" | `recommendedRestTime` |
| estimatedCalories | Int | YES | 0 | `estimatedCalories` |
| category | String | YES | "" | `category` |
| tags | String | YES | "" | `tags` |
| isFavorite | Boolean | YES | false | `isFavorite` |
| lastViewed | Long | YES | 0L | `lastViewed` |
| **vtaperLat** | **Int** | **YES** | **0** | **`vtaper_lat`** |
| **vtaperLateralDelt** | **Int** | **YES** | **0** | **`vtaper_lateral_delt`** |
| **vtaperUpperChest** | **Int** | **YES** | **0** | **`vtaper_upper_chest`** |
| **vtaperRearDelt** | **Int** | **YES** | **0** | **`vtaper_rear_delt`** |
| **movementPattern** | **String** | **YES** | "" | **`movement_pattern`** |
| **imageUrl** | **String?** | **YES** | **null** | **`image_url`** |
| **videoUrl** | **String?** | **YES** | **null** | **`video_url`** |
| **animationUrl** | **String?** | **YES** | **null** | **`animation_url`** |
| **setupInstructions** | **String** | **YES** | "" | **`setup_instructions`** |
| **executionInstructions** | **String** | **YES** | "" | **`execution_instructions`** |
| **breathingInstructions** | **String** | **YES** | "" | **`breathing_instructions`** |
| **tempoGuidance** | **String** | **YES** | "" | **`tempo_guidance`** |
| **beginnerVariantId** | **Long?** | **YES** | **null** | **`beginner_variant_id`** |
| **advancedVariantId** | **Long?** | **YES** | **null** | **`advanced_variant_id`** |

### Domain Model vs Entity Alignment ❌

**Exercise (domain)** has 17 fields — **missing all V-taper and variant fields:**

```kotlin
data class Exercise(
    val id: Long = 0,
    val name: String,
    val description: String,
    val muscleGroup: String,
    val equipment: String,
    val difficulty: String,
    val secondaryMuscles: String = "",
    val instructions: String = "",
    val tips: String = "",
    val commonMistakes: String = "",
    val safetyNotes: String = "",
    val recommendedRepRange: String = "",
    val recommendedRestTime: String = "",
    val estimatedCalories: Int = 0,
    val category: String = "",
    val tags: String = "",
    val isFavorite: Boolean = false,
    val lastViewed: Long = 0L  // NOTE: 18 fields, no V-taper
)
```

**Mapping Gap:** `ExerciseEntity.toDomain()` in `ExerciseRepositoryImpl` silently drops all 13 V-taper/variant fields. Same for `Exercise.toEntity()`.

---

## Schema Export

- **exportSchema = false** in @Database annotation
- **No schemaDirectory** configured in `app/build.gradle.kts`
- **No `schemas/` directory** with generated JSON files
- **Inability to verify** room schema matches entity definitions

---

## MigrationTestHelper

- **No `RoomMigrationTest.kt`** exists in the repository
- **No migration tests** that use `MigrationTestHelper`
- **No test** that creates an older database, inserts data, closes it, executes migrations, validates schema, and reopens to read data

---

## Bidirectional Mapping Audit

### ExerciseEntity → Exercise (toDomain)

Fields transferred: id, name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed

**Fields LOST (13):**
- vtaperLat, vtaperLateralDelt, vtaperUpperChest, vtaperRearDelt
- movementPattern
- imageUrl, videoUrl, animationUrl
- setupInstructions, executionInstructions, breathingInstructions, tempoGuidance
- beginnerVariantId, advancedVariantId

### Exercise → ExerciseEntity (toEntity)

Fields transferred: id, name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed

**Fields LOST (13):** Same as above — silently dropped on persist.

---

## Test File Audit

| Test File | Status |
|-----------|--------|
| `ExerciseRepositoryTest.kt` | 7 tests, compile ✅, runs ❌ (no Android env) |
| `ExerciseSeederTest.kt` | Dummy compile check only ❌ |
| `RoomMigrationTest.kt` | NOT CREATED ❌ |
| `VolumeCalculatorTest.kt` | Not found in current repo |
| `PRDetectorTest.kt` | Not found in current repo |

---

## CI / Build Workflow

- **No local build possible** (Android SDK/JDK not available in this environment)
- **GitHub Actions** claimed in prior report but not independently verified
- **Required commands** (from docs): `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleDebug`
- **Android CI** `.github/workflows/` exists but content needs verification

---

## Navigation Audit

### GymCoachNavHost.kt Routes

| Route | Screen |
|-------|--------|
| `exercise_list` | ExerciseListScreen ✅ |
| `exercise_detail/{exerciseId}` | ExerciseDetailScreen ✅ |
| `workout_history` | WorkoutHistoryScreen ✅ |
| `workout_history_detail/{workoutId}` | WorkoutHistoryDetailScreen ✅ |
| `workout_session?workoutId={workoutId}` | WorkoutSessionScreen ✅ |
| `progress` | ProgressDashboardScreen ✅ |
| `camera` | CameraPreviewScreen ✅ |

**Additional routes from PR #11 (not yet merged to main):**
- `onboarding` → OnboardingScreen
- `home` → HomeDashboardScreen
- `profile` → ProfileScreen

---

## Seeder Audit

- **No `ExerciseSeeder` class** found in `app/src/main`
- **seedExercises()** in `GymCoachDatabase.kt` inserts 20+ exercises via raw SQL on DB creation
- **No test** for seeding twice (idempotency)
- **No system exercise duplication protection** documented

---

## Security Audit Status

- **Not yet performed** in this session
- **No secrets** found in source (via quick grep)
- **Cleartext traffic**: `android:allowBackup="true"` without `dataExtractionRules` (documented technical debt)
- **Network access**: No cleartext traffic config present but not explicitly disabled

---

## Release Configuration

- **versionName:** `0.1.0`
- **versionCode:** `1`
- **Play Store:** not configured
- **Signing:** not configured (debug only)
- **firebase/** configs: not present

---

## Summary of Critical Issues

1. ❌ **Migration chain incomplete:** Only 1→2→3 registered, target version 5
2. ❌ **fallbackToDestructiveMigration() present** — data loss risk for fitness app
3. ❌ **exportSchema = false** — no schema verification possible
4. ❌ **Entity↔Domain mapping drops 13 V-taper/variant fields**
5. ❌ **No MigrationTestHelper** — no data preservation validation
6. ❌ **No schema JSON files** forRoom schema audit
7. ⚠️ **Navigation routes** from PR #11 not merged to main
8. ⚠️ ** versionName 0.1.0** not 1.0.0 for production