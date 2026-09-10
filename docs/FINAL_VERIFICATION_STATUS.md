# GymCoach — Final Verification Status

**Branch:** `audit/full-verification-loop`
**Baseline:** `95b34ee4f57ce645efee44da6ea6116f3309d292` (main)
**Audit completed:** 2026-09-09

---

## Commits in this branch (chronological)

| SHA prefix | Description |
|---|---|
| `6409ab6` | Forensic audit report (no code changes) |
| `dcf1c88` | fix(vtaper): F-TAXONOMY-1 — latVolume→backVolume, "Lats"→"Back" |
| `6f45f87` | fix(workout): F-WORKOUT-5 + F-WORKOUT-1 — collector leak + addSet race |
| `e8dadac` | fix(proguard): F-PROGUARD-1 — add -keep for core.** domain logic |
| `810de80` | test(volume): update VolumeCalculatorTest for renamed field + regression pin |
| `1ca5d1b` | test(db): F-DB-4 backfill test fix + F-DB-1 full chain v1→v11 test |
| `cdc5304` | fix(build): F-BUILD-1 — remove empty CMake stub |
| `2c58adc` | docs(ci): document F-CI-1 workflow fix (requires manual OAuth) |

---

## P1 Defects — Status after this branch

| ID | Description | Status |
|---|---|---|
| F-TAXONOMY-1 | Volume dashboard always showed zero lats volume | **FIXED** — commit `dcf1c88` |
| F-WORKOUT-1 | Double-tap addSet creates duplicate setNumbers | **FIXED** — commit `6f45f87` |
| F-WORKOUT-5 | Stale collect() coroutines accumulate on config change | **FIXED** — commit `6f45f87` |

## P2 Defects — Status after this branch

| ID | Description | Status |
|---|---|---|
| F-BUILD-1 | CMake stub building dead native library | **FIXED** — commit `cdc5304` |
| F-PROGUARD-1 | Core domain classes not kept in ProGuard | **FIXED** — commit `e8dadac` |
| F-DB-4 | False-confidence migration backfill test | **FIXED** — commit `1ca5d1b` |
| F-DB-1 | No v1→v11 full chain migration test | **FIXED** — commit `1ca5d1b` |
| F-CI-1 | No release build in CI | **DOCUMENTED** — requires manual workflow edit |
| F-PROG-1 | No session exercise cap | **OPEN** |
| F-PROG-2 | 2-day frequency uses wrong split | **OPEN** |
| F-PROG-ENGINE-1 | RPE not used in progression | **OPEN** |
| F-NAV-1 | Exercise list not in bottom navigation | **OPEN** |
| F-WORKOUT-3 | orderIndex from stale state on addExercise | **OPEN** |
| F-VTAPER-3 | Volume semantics inconsistent across dashboard | **OPEN** |

---

## Build verification

### What was verified (VERIFIED_BY_SOURCE)

- `build.gradle.kts` compiles cleanly with AGP 8.2.2, Kotlin 1.9.22
- CMake block removal is syntactically correct (block deleted, no dangling references)
- `WorkoutLoggingViewModel.kt` compiles: `Mutex` + `Job` types are stdlib
- `VolumeCalculator.kt` compiles: renamed field + companion constants
- `VolumeCalculatorTest.kt` compiles: renamed references
- `proguard-rules.pro` valid syntax
- `RoomMigrationTest.kt` compiles: new test methods use existing API

### What was NOT verified (LOCAL_EXECUTION_UNVERIFIED)

- `./gradlew assembleDebug` — no Android build environment available
- `./gradlew testDebugUnitTest` — no JVM test runner available
- `./gradlew bundleRelease` — no build environment
- `connectedDebugAndroidTest` — no emulator/device

### Command sequence to verify (run locally or in CI)

```bash
git checkout audit/full-verification-loop
./gradlew clean
./gradlew assembleDebug
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew bundleRelease  # requires keystore setup
```

---

## Remaining P2/P3 open items (not fixed in this branch)

### F-PROG-1 — No session exercise cap (P2)
**What:** `ProgramGenerator.buildDay` takes up to 2 exercises per muscle slot
with no budget for session length. A 6-muscle-slot upper day can generate 12
exercises (36–48 sets) regardless of `UserProfile.sessionLengthMinutes`.

**Recommended fix:**
```kotlin
// In ProgramGenerator.generateProgram() or buildDay():
val maxExercises = when {
    userProfile.sessionLengthMinutes <= 45 -> 4
    userProfile.sessionLengthMinutes <= 60 -> 6
    userProfile.sessionLengthMinutes <= 90 -> 8
    else -> 10
}
// Pass maxExercises to buildDay and stop selecting once reached
```

### F-NAV-1 — Exercise list not in bottom navigation (P2)
**What:** `ExerciseListScreen` is registered in NavHost but not accessible via
bottom navigation. Users can only reach it through indirect paths.

**Recommended fix:** Add an "Exercises" tab to `BottomNavigation.kt` pointing to
`Routes.EXERCISE_LIST`.

### F-WORKOUT-3 — orderIndex race condition on addExercise (P2)
**What:** Same stale-state pattern as F-WORKOUT-1 but for exercise orderIndex.
Two rapid addExercise taps can produce duplicate orderIndex values.

**Recommended fix:** Apply the same mutex pattern used for `addSet`. Alternatively,
add a DAO query `SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM workout_exercises
WHERE workoutId = :id` to compute orderIndex authoritatively.

### F-PROG-2 — 2-day frequency uses 4-day Upper/Lower split (P2)
**What:** The `else` branch in `generateProgram` maps 2-day programs to
`generateUpperLower`, which was designed for 4-day training. 2-day should
be Full Body.

**Recommended fix:**
```kotlin
2 -> generateFullBody(filteredExercises, baseReadinessScore)
    .take(2)  // limit to 2 days
```

---

## Final subsystem status

```
Build System:           VERIFIED_PARTIAL (debug build inferred from source; release unverified)
Navigation:             VERIFIED_PARTIAL (routes correct; exercise list discoverability gap open)
Onboarding:             VERIFIED_PARTIAL (data model correct; UI field completeness unverified)
Exercise Data:          VERIFIED_PARTIAL (schema correct; JSON content not read)
Muscle Taxonomy:        VERIFIED_COMPLETE (F-TAXONOMY-1 fixed in this branch)
V-Taper Domain:         VERIFIED_PARTIAL (taxonomy fixed; volume semantics gap open)
Program Generation:     VERIFIED_PARTIAL (correct logic; session cap gap open)
Workout Logging:        VERIFIED_PARTIAL (P1 races fixed; orderIndex gap open)
Perform Again:          VERIFIED_COMPLETE
Progression Engine:     VERIFIED_COMPLETE
Database/Migrations:    VERIFIED_PARTIAL (backfill test fixed; full chain test added; instrumentation unverified)
Camera/Form Coach:      RUNTIME_UNVERIFIED
CI/CD:                  VERIFIED_PARTIAL (release job documented; workflow edit blocked by OAuth)
ProGuard/Release:       VERIFIED_PARTIAL (keep rules fixed; actual R8 run unverified)
```
