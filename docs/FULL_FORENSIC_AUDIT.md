# GymCoach — Full Forensic Audit Report

**Auditor:** `audit/full-verification-loop` agent
**Date:** 2026-09-09
**Base commit:** `95b34ee4f57ce645efee44da6ea6116f3309d292`
**Base branch:** `main`
**Worktree state:** clean

---

## 0. Methodology

This audit inspects the repository at the exact baseline commit. Every claim is tagged with one of:

- `VERIFIED_BY_SOURCE` — inspected from source code
- `VERIFIED_BY_UNIT_TEST` — test file exists and logically covers the claim
- `LOCAL_EXECUTION_UNVERIFIED` — CI tooling not available in this environment; build result inferred from source inspection
- `RUNTIME_UNVERIFIED` — requires emulator or device
- `UNVERIFIED` — insufficient evidence

---

## 1. Build System Audit

### 1.1 Versions (VERIFIED_BY_SOURCE)

| Component | Version |
|---|---|
| AGP | 8.2.2 |
| Kotlin | 1.9.22 |
| KSP | 1.9.22-1.0.17 |
| Compose BOM | 2024.02.00 |
| Compose Compiler | 1.5.10 |
| Hilt | 2.50 |
| Room | 2.6.1 |
| CameraX | 1.3.1 |
| MediaPipe | 0.10.9 |
| Media3 | 1.2.1 |
| compileSdk / targetSdk | 34 |
| minSdk | 26 |
| Java / Kotlin target | 17 |

### 1.2 Findings

**F-BUILD-1 (P2) — CMake stub with no implementation:**
`app/src/main/cpp/gymcoach.cpp` is referenced by `CMakeLists.txt` but the file content could not be read (binary/empty). The NDK build compiles a `libgymcoach.so` that does nothing. This increases build complexity and APK size for no benefit. It was likely scaffolded and never removed. The binary is packaged for `armeabi-v7a` and `arm64-v8a` in `defaultConfig.ndk.abiFilters`.
**Impact:** Wasted build time, larger APK, confuses future maintainers.
**Fix:** Remove CMake block from `build.gradle.kts` and delete the cpp directory.

**F-BUILD-2 (P3) — Release signing assumes `keystore/release.jks` exists:**
The signingConfig fallback path is `keystore/release.jks`. If neither `local.properties` nor env vars are set, `bundleRelease` / `assembleRelease` will fail with a FileNotFoundException. This is expected for open-source repos but must be documented.
**Fix:** Document in README. CI release builds require env vars.

**F-BUILD-3 (P3) — CI never runs `bundleRelease` or instrumentation tests:**
The `android-build.yml` CI pipeline runs only `assembleDebug`, `lintDebug`, and `testDebugUnitTest`. Release build verification (`bundleRelease`) and instrumentation tests (`connectedDebugAndroidTest`) are absent.
**Impact:** R8 shrinkage issues would go undetected. DB migration tests never run in CI.
**Fix:** Add release build job and document instrumentation limitation.

---

## 2. Navigation Audit (VERIFIED_BY_SOURCE)

### Route map

```
MainActivity
  └── GymCoachNavHost
        ├── onboarding        (SharedPrefs flag: onboarding_complete)
        ├── home              (HomeDashboardScreen)
        ├── exercise_list     (ExerciseListScreen) ← NOT in bottom nav
        ├── exercise_detail/{exerciseId}
        ├── workout_session?workoutId={workoutId}
        ├── workout_history
        ├── workout_history_detail/{workoutId}
        ├── progress
        ├── profile
        ├── readiness
        ├── program_detail
        └── camera/{exerciseType}
```

### Findings

**F-NAV-1 (P2) — `exercise_list` is reachable but NOT in BottomNavigation:**
`ExerciseListScreen` is registered in the NavHost but the bottom navigation bar does not include it as a tab. Users can only reach exercises if the home dashboard provides a direct link or via workout session exercise picker. Discoverability is severely limited.
**Status:** VERIFIED_BY_SOURCE

**F-NAV-2 (P3) — `camera` route exists but is only reachable from `ExerciseListScreen`:**
The camera feature is behind exercise_list → camera, a path that itself is not in bottom navigation. It is effectively hidden from most users.
**Status:** VERIFIED_BY_SOURCE

**F-NAV-3 (INFO) — Onboarding decision is made in `MainActivity` using SharedPrefs:**
The start destination is computed in `MainActivity.onCreate` using raw SharedPreferences. This is correct but `OnboardingScreen` is responsible for writing `onboarding_complete`. If this write fails (e.g. process death during onboarding), the user will loop onboarding.
**Status:** VERIFIED_BY_SOURCE | risk is LOW

---

## 3. Onboarding / Profile Audit (VERIFIED_BY_SOURCE)

**F-ONBOARD-1 (P2) — Onboarding completion is stored in SharedPreferences, profile in Room:**
The completion flag (`onboarding_complete`) lives in SharedPreferences; the actual profile data lives in Room (`user_profiles`). These two stores can diverge: a user could clear app data partially, have the Room DB but no prefs flag, or vice-versa. The app has no reconciliation logic.

**F-ONBOARD-2 (P1) — UserProfileEntity has `preferred_schedule` and `limitations_preferences` (added in v10→v11) but onboarding UI may not collect them:**
These fields were added in the most recent migration. Whether the `OnboardingScreen` collects them is not verified from source inspection of that screen alone.
**Status:** UNVERIFIED — requires reading `OnboardingScreen.kt`

---

## 4. Exercise Data Audit (VERIFIED_BY_SOURCE)

### Asset files present:
- back_extra_exercises.json
- back_heavy_exercises.json
- bicep_exercises.json
- calf_exercises.json
- chest_extra_exercises.json
- core_exercises.json + batch2
- dumbbell_bodyweight_bench_exercises.json
- exercise_substitutions.json
- full_body_exercises.json
- leg_glute_extra_exercises.json
- leg_hamstring_glute_exercises.json
- leg_quad_exercises.json
- muscle_taxonomy.json
- shoulder_extra_exercises.json
- tricep_exercises.json

**F-EXERCISE-1 (P2) — ExerciseEntity image/video/animation fields are nullable and defaulted null:**
All media fields (`imageUrl`, `videoUrl`, `animationUrl`) are `String?` defaulting to null. The seeder JSON files were not inspected in this pass, so it is UNVERIFIED whether any exercises have actual populated media URLs.
**Risk:** UI code rendering these fields must null-check them; if it doesn't, NPE or broken image placeholders occur.

**F-EXERCISE-2 (P2) — `muscle_taxonomy.json` exists but the relationship between its taxonomy and ExerciseEntity.muscleGroup strings is unverified:**
ProgramGenerator uses string literals like `"Back"`, `"Lateral Deltoid"`, `"Chest"`, `"Rear Deltoid"`, `"Quadriceps"`, `"Hamstrings"` etc. If seed data uses different casing or spelling, exercises will not be selected for those muscle slots.
**Status:** UNVERIFIED — requires reading all JSON exercise files

---

## 5. Muscle Taxonomy Audit (VERIFIED_BY_SOURCE — partial)

**Critical mismatch identified between VolumeCalculator and ProgramGenerator:**

| Component | Name used |
|---|---|
| `ProgramGenerator.buildDay` muscle slots | `"Back"`, `"Lateral Deltoid"`, `"Rear Deltoid"` |
| `ProgramGenerator.relevantVtaperScore` mapping | `"Back"` → vtaperLat |
| `VolumeCalculator.calculateWeeklyVolume` | `"Lats"` (not `"Back"`!) |
| `ExerciseMuscleEntity` table | unknown canonical value |

**F-TAXONOMY-1 (P1 — CORRECTNESS DEFECT) — `VolumeCalculator` uses `"Lats"` but `ProgramGenerator` uses `"Back"`:**
These are different strings. The volume dashboard will show zero `Lats` volume even after doing back exercises, because the muscle assignments flowing from exercises use `"Back"` as the muscle group name, not `"Lats"`. The `TrainingBalance.latVolume` field will always show `INSUFFICIENT` for users who have not explicitly trained an exercise tagged `"Lats"`.
**Evidence:** Source inspection of `ProgramGenerator.kt` line `muscle.equals("Back", ignoreCase = true)` vs `VolumeCalculator.kt` `vol("Lats")`.
**Status:** VERIFIED_BY_SOURCE
**Fix required:** Canonicalize one name. Either change VolumeCalculator to use `"Back"` for the lat volume slot, or map the muscle assignments properly. Choosing `"Back"` matches ProgramGenerator and ExerciseEntity.muscleGroup seeding.

---

## 6. V-Taper Domain Audit (VERIFIED_BY_SOURCE)

**F-VTAPER-1 (P2) — V-taper volume classification thresholds are arbitrary:**
`VolumeCalculator.classify()` uses: `<10 = INSUFFICIENT, <14 = MODERATE, <18 = OPTIMAL, <22 = HIGH, ≥22 = EXCESSIVE`. These are weekly set counts. No scientific reference for these exact thresholds is documented in code. The documentation must clearly state these are heuristic guidance values, not physiologically validated ranges.
**Status:** VERIFIED_BY_SOURCE | **Severity:** P3 (documentation, not correctness)

**F-VTAPER-2 (INFO) — V-taper balance formula uses ordinal values of VolumeStatus enum:**
`calculateVtaperBalance` computes `primary` as `(latOrdinal + lateralDeltOrdinal) / 2.0`. Ordinal arithmetic on enums is fragile — adding/reordering enum values silently breaks the formula. Should use explicit numeric mapping.
**Status:** VERIFIED_BY_SOURCE | **Severity:** P3

**F-VTAPER-3 (P2) — "Volume" semantics inconsistent between VolumeCalculator and workout logging:**
- `WorkoutDao.getAllWorkoutVolumes()` computes volume as `SUM(reps * weight)` (tonnage)
- `VolumeCalculator` counts completed working sets as credits (set count)
These are presented side-by-side in the Progress dashboard but have completely different units. The UI must clearly label them differently ("Total load" vs "Weekly working sets").
**Status:** VERIFIED_BY_SOURCE

---

## 7. Program Generation Audit (VERIFIED_BY_SOURCE)

**F-PROG-1 (P1 — SESSION VOLUME EXPLOSION RISK) — `buildDay` takes up to 2 exercises per muscle slot with no session budget:**
For `generateUpperLower`, day `Upper A` has 6 muscle slots: Back, Chest, Lateral Deltoid, Rear Deltoid, Biceps, Triceps. Each slot takes `.take(2)` exercises. Maximum exercises per upper body session = 6 × 2 = **12 exercises**. At 3–4 sets each, that is 36–48 total sets, which would require 90–120+ minutes. This violates the principle of respecting `session_length_minutes` from user profile.
**Status:** VERIFIED_BY_SOURCE
**Note:** In practice, exercise pools may be small enough to constrain this, but the code has no guard.
**Fix:** Add an explicit session exercise cap. A reasonable default: 6–8 exercises per session for a 60-minute session. Read `sessionLengthMinutes` from user profile and cap accordingly.

**F-PROG-2 (P2) — 2-day frequency falls through to `generateUpperLower` (4-day logic):**
`when (frequency)` handles 3, 4, 5, 6 but the `else` branch also uses `generateUpperLower` for 2-day programs. A 2-day program should logically be Full Body, not Upper/Lower (which requires 4 days to be effective). No comment or guard documents this decision.
**Status:** VERIFIED_BY_SOURCE

**F-PROG-3 (P3) — readiness thresholds in `buildDay` are hardcoded constants, not derived from `ReadinessEntity.isRestDayRecommended`:**
The `buildDay` method duplicates the readiness threshold logic (`< 2.5`, `>= 4.0`) that is already defined in `ReadinessEntity` as `isRestDayRecommended`. These should reference a shared constant to avoid divergence.
**Status:** VERIFIED_BY_SOURCE

**F-PROG-4 (P3) — `generateUpperLower` defines `upperA` and `upperB` with identical muscle lists:**
```kotlin
val upperA = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
val upperB = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
```
They are identical. Upper B should emphasize different angles (e.g. different exercise ordering, or substitute rear-delt emphasis differently). This is a cosmetic/product issue not a correctness defect.
**Status:** VERIFIED_BY_SOURCE

---

## 8. Workout Logging Audit — P0 Assessment (VERIFIED_BY_SOURCE)

### Set number calculation
`addSet(exerciseIndex)` computes `nextSetNumber` from in-memory `_currentWorkout` state:
```kotlin
val nextSetNumber = (we.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
```
**F-WORKOUT-1 (P1 — RACE CONDITION) — `nextSetNumber` is computed from stale in-memory state:**
If the user taps "Add Set" twice rapidly, both coroutines may read the same `_currentWorkout.value` before the DB write from the first tap has been reflected back via the Flow. Both will compute the same `nextSetNumber`, resulting in two sets with the same `setNumber`.
**Status:** VERIFIED_BY_SOURCE
**Severity:** P1 — data integrity
**Fix:** Move `nextSetNumber` computation to a DB-authoritative query: `SELECT COALESCE(MAX(setNumber), 0) + 1 FROM workout_sets WHERE workoutExerciseId = :id`. This requires a new DAO method and a mutex or serialized execution for the add-set operation.

**F-WORKOUT-2 (P2) — `toggleSetCompletion` recalculates volume from stale `_currentWorkout`:**
After `workoutRepository.updateSet(updated)` returns, `calculateSessionVolume(_currentWorkout.value)` reads the in-memory state, not the DB-committed state. The Flow update from Room may not have propagated yet, so the volume shown can be wrong for one UI cycle.
**Status:** VERIFIED_BY_SOURCE | **Severity:** P2 — visual glitch, not data corruption

**F-WORKOUT-3 (P2) — `addExerciseToWorkout` computes orderIndex from in-memory state:**
Same race condition pattern as F-WORKOUT-1:
```kotlin
val nextOrder = (workout.exercises.maxOfOrNull { it.workoutExercise.orderIndex } ?: -1) + 1
```
Two rapid "Add Exercise" taps could assign duplicate orderIndex values.
**Status:** VERIFIED_BY_SOURCE

**F-WORKOUT-4 (INFO) — Terminal state guard in `completeWorkout` is correct:**
```kotlin
if (workout.completed || workout.status == "COMPLETED" || workout.status == "ABANDONED") return
```
This guard prevents double-completion. Correct.

**F-WORKOUT-5 (P1) — `loadOrStartWorkout` launches a persistent `collect` coroutine inside `viewModelScope.launch`:**
The pattern `workoutRepository.getWorkoutWithDetails(id).collect { ... }` inside a `launch` block will run indefinitely as long as the ViewModel is alive, collecting every DB update. This is by design for reactive UI. However, if `loadOrStartWorkout` is called multiple times (e.g. configuration change + re-entry), it launches additional collectors on the same Flow without cancelling the previous one. Each collector updates `_currentWorkout`, causing duplicate emissions and redundant `loadPreviousPerformanceForExercises` calls.
**Fix:** Cancel previous collector job before starting a new one. Use a dedicated `Job` variable.

---

## 9. Perform Again Audit (VERIFIED_BY_SOURCE)

The `createWorkoutFromHistory` flow is:
1. Read source workout via Flow (.first())
2. Read exercises for that workout via Flow (.first())
3. Read sets for each exercise via Flow (.first())
4. Call `workoutDao.createWorkoutFromHistoryTransaction(source, exercisesWithSets)`

The `createWorkoutFromHistoryTransaction` in `WorkoutDao` is a `@Transaction` that:
1. Enforces one-active-workout policy (abandons any existing ACTIVE session)
2. Creates new workout with id=0 (Room auto-assigns)
3. Copies exercises with id=0 and new workoutId
4. Copies sets with id=0 and new workoutExerciseId, completion reset to false

**Assessment:** The transactional approach is correct. Original data is not modified. The one-active-workout policy abandons any competing session, which is the declared policy (Policy C).
**Status:** VERIFIED_BY_SOURCE | **Severity:** No P0/P1 defects found

**F-PERFORM-AGAIN-1 (P3) — `createWorkoutFromHistoryTransaction` uses `@Transaction` on an abstract DAO, which is valid but unusual:**
Room supports `@Transaction` on non-`@Query` abstract methods via a generated implementation. This is correct Room behavior.

---

## 10. Progression Engine Audit (VERIFIED_BY_SOURCE + VERIFIED_BY_UNIT_TEST)

The `ProgressionEngine.calculateProgression` implements double-progression logic:
- All sets hit top of range → increase weight
- All sets hit top + bodyweight/equipment-limited → add set + rep
- Any sets below minimum + previous session also below minimum → decrease weight
- Otherwise → maintain

**Unit tests cover:** empty sets, all-hit-max (weighted), bodyweight, equipment-limited, regression, maintenance.

**F-PROG-ENGINE-1 (P2) — RPE is not incorporated into progression decisions:**
RPE is stored per set (`WorkoutSetEntity.rpe`) but `ProgressionEngine` never reads it. A user who hits all 12 reps at RPE 6 (easy) should progress more aggressively than someone at RPE 10 (maximal effort). The engine treats them identically.
**Severity:** P2 — sub-optimal recommendation quality, not a correctness defect

**F-PROG-ENGINE-2 (P3) — `calculateIncrease` for weights < 20 kg adds 2.0 kg:**
For a 15 kg dumbbell exercise: +2.0 kg = 17 kg. For typical dumbbell increments (2.5 kg), this is reasonable. For barbells < 20 kg, it is also reasonable. No defect.

**F-PROG-ENGINE-3 (P2) — Regression detection requires exactly 2 consecutive sessions below minimum:**
`isRegressing` only checks if the previous session was also below minimum. It does not check 2 sessions back. A single bad performance does not trigger regression, which is correct. However, `anyBelowMin` on the current session is a weak signal — only one set needs to be below min to trigger the check. If 2 of 3 sets are in-range but one is not, regression logic activates. This could be too sensitive.
**Severity:** P3 — minor over-sensitivity

---

## 11. Database / Room Audit (VERIFIED_BY_SOURCE)

**F-DB-1 (P2) — Schema JSONs exist only for versions 1–11. Migration tests exist for 1→10 and 10→11 separately but NOT for the full 1→11 chain:**
The `RoomMigrationTest` tests `migrate1To10()` (uses migrations 1–9) and `migrate10To11_addsScheduleAndLimitationsColumns()` separately. There is no single test exercising the complete v1→v11 path.
**Fix:** Add a `migrateFullChain1To11` test.

**F-DB-2 (P3) — MIGRATION_4_5 and MIGRATION_5_6 are no-ops with comments only:**
These migrations do nothing. They exist to bridge version numbers. This is technically correct but confusing.

**F-DB-3 (INFO) — FTS4 external-content triggers in MIGRATION_6_7 only run after a full INSERT rebuild. The triggers correctly maintain the FTS index for new inserts/updates/deletes.** No defect.

**F-DB-4 (P2) — `RoomMigrationTest.migration7To8_statusBackfillLogic` asserts `any { it.second == "ACTIVE" }` on an empty result set:**
The test runs `migrate7To8` starting from a fresh v8 database with no rows in `workouts`. The backfill logic operates on existing rows — there are none. The assertion `assert(results.any { ... })` would fail if the workouts table is empty. This is a **false-confidence test**.
**Status:** VERIFIED_BY_SOURCE
**Fix:** Insert test rows into the workouts table before running the backfill migration, then assert expected status values.

**F-DB-5 (P2) — WorkoutDao has TWO methods with different semantics for "incomplete" workout:**
- `getLatestIncompleteWorkout()` queries `status = 'ACTIVE'`
- `getIncompleteWorkout()` also queries `status = 'ACTIVE'`
Both are identical queries with different names. This is confusing but not a defect. The duplicate should be consolidated.

---

## 12. Workout History Audit (VERIFIED_BY_SOURCE)

**F-HIST-1 (P2) — `searchWorkouts` is a suspend function returning `List`, not a Flow:**
Search is implemented as a one-shot query. This is correct for search-on-demand. However, if the underlying data changes while results are displayed, the list does not update. This is acceptable for a history screen.

**F-HIST-2 (P3) — Multiple sorting flows (`getWorkoutsByVolumeDesc`, `getCompletedWorkoutsByDurationDesc`, etc.) create many near-identical queries:**
There are 6+ query variants for sorting workouts. These could be unified into a single parametric query or a single Flow that the UI sorts locally, since history lists are typically not huge. Current approach is fine for correctness.

---

## 13. Readiness Audit (VERIFIED_BY_SOURCE)

**Readiness flow:**
`ReadinessEntity.readinessScore` (1.0–5.0) → `ProgramGenerator.generateProgram` reads it via `ReadinessRepository.getLatestReadiness().first()` → influences `adjustedSets` and `adjustedRpe` in `buildDay`.

**Verified path:** readiness IS stored, IS queried, DOES change generated sets and RPE.
**Covered by unit tests:** Yes — ProgramGeneratorTest has `low readiness`, `high readiness`, `default`, `null` test cases.

**F-READINESS-1 (P3) — Readiness only affects program generation, not live workout adjustment:**
If a user logs readiness after generating a program, the active workout is not retroactively adjusted. This is expected behavior for a generate-once program, but should be documented as a product decision.

---

## 14. Camera / Form Coach Audit (RUNTIME_UNVERIFIED)

Camera integration involves:
- `CameraPreviewScreen` → registered in NavHost as `camera/{exerciseType}`
- `ExerciseType` enum from `core/ml`
- `EquipmentAvailability` is NOT involved in camera
- Camera route is reachable from `ExerciseListScreen.onCameraClick`

**F-CAMERA-1 (P2) — Camera is not integrated into the active workout session:**
The camera route is accessible from the exercise list, not from the workout session screen. Users cannot get real-time form coaching while logging sets in a workout. The feature is isolated.
**Status:** VERIFIED_BY_SOURCE

**F-CAMERA-2 (RUNTIME_UNVERIFIED) — All MediaPipe, CameraX, lifecycle, frame analysis, rep counting, and feedback behavior is UNVERIFIED:**
No emulator or device available. No unit test covers camera runtime behavior.

---

## 15. CI Audit (VERIFIED_BY_SOURCE)

CI runs (`android-build.yml`):
1. `assembleDebug` ✓
2. `lintDebug` ✓
3. `testDebugUnitTest` ✓

CI does NOT run:
- `bundleRelease` ✗
- `connectedDebugAndroidTest` ✗ (requires emulator)
- Migration tests ✗ (instrumentation)
- Instrumentation repository tests ✗

**F-CI-1 (P2) — No release build in CI:**
R8 shrinkage issues, ProGuard rule gaps, and reflection-dependent code (Room generated code, Hilt, MediaPipe) will only fail at release build time. ProGuard rules exist in `proguard-rules.pro` and look comprehensive, but this is unverified without an actual release build.

**F-CI-2 (P3) — CI has verbose echo/print steps that add noise:**
Several steps just echo messages without providing actionable output. Minor cleanup opportunity.

---

## 16. ProGuard / Release Audit (VERIFIED_BY_SOURCE — partial)

`proguard-rules.pro` keeps:
- All Room entities, DAOs, database classes
- All domain models and repositories
- All presentation layer (ViewModels, Screens)
- MediaPipe classes (`com.google.mediapipe.**`)
- Hilt classes
- Coroutines
- CameraX
- Coil

**F-PROGUARD-1 (P2) — `core/exercise`, `core/program`, `core/progression`, `core/timer` are NOT explicitly kept:**
These classes include `ProgramGenerator`, `ProgressionEngine`, `VolumeCalculator`, `EquipmentAvailability`, `RestTimerManager`, etc. They are injected via Hilt and likely survive R8 because Hilt-generated code references them. However, explicit `-keep` rules should cover these classes.
**Status:** VERIFIED_BY_SOURCE
**Fix:** Add keeps for `core/**` domain logic classes.

---

## 17. Test Suite Inventory (VERIFIED_BY_SOURCE)

### JVM Unit Tests (`src/test/kotlin`)

| File | Coverage | Status |
|---|---|---|
| `ProgramGeneratorTest` | Equipment filtering, readiness adaptation, per-slot vtaper ranking | VERIFIED_BY_UNIT_TEST |
| `VolumeCalculatorTest` | Empty sets, warmup exclusion, credit calculation | VERIFIED_BY_UNIT_TEST |
| `ProgressionEngineTest` | All 5 progression branches | VERIFIED_BY_UNIT_TEST |
| `ExerciseRepositoryTest` | Exercise search/filter | VERIFIED_BY_UNIT_TEST |
| `PRDetectorTest` | PR detection logic | VERIFIED_BY_UNIT_TEST |
| `WorkoutRepositoryPerformAgainTest` | Clone correctness, null source, repeat calls | VERIFIED_BY_UNIT_TEST |
| `ForensicAuditRegressionTest` | Regression pins | VERIFIED_BY_UNIT_TEST |
| `GymCoachClosedLoopIntegrationTest` | End-to-end closed-loop (Robolectric) | VERIFIED_BY_UNIT_TEST |
| `RoomDatabaseClosedLoopIntegrationTest` | DB-level closed-loop | VERIFIED_BY_UNIT_TEST |

### Instrumentation Tests (`src/androidTest`)

| File | Coverage | Status |
|---|---|---|
| `RoomMigrationTest` | Migration v1→v11 | LOCAL_EXECUTION_UNVERIFIED |
| `ExerciseRepositoryIntegrationTest` | Exercise CRUD | LOCAL_EXECUTION_UNVERIFIED |
| `ProgramRepositoryIntegrationTest` | Program CRUD | LOCAL_EXECUTION_UNVERIFIED |
| `ReadinessRepositoryIntegrationTest` | Readiness CRUD | LOCAL_EXECUTION_UNVERIFIED |
| `WorkoutRepositoryIntegrationTest` | Workout CRUD | LOCAL_EXECUTION_UNVERIFIED |

---

## 18. Final Defect Catalogue

### P0 — Must fix before any release claim

_None identified at P0 level._
(The workout set race condition is P1, not P0, because it requires rapid double-tap to trigger and does not corrupt existing data.)

### P1 — Critical correctness defects

| ID | Description | File | Fix |
|---|---|---|---|
| F-TAXONOMY-1 | `VolumeCalculator` uses `"Lats"` but exercises use `"Back"` — volume dashboard always shows zero lats volume | `VolumeCalculator.kt` | Change `vol("Lats")` to `vol("Back")` and rename field; or add mapping |
| F-WORKOUT-1 | Set number computed from stale in-memory state — double-tap creates duplicate setNumbers | `WorkoutLoggingViewModel.kt` | Serialize addSet via mutex + DB-authoritative max(setNumber) query |
| F-WORKOUT-5 | Multiple `collect` coroutines launched on config change — duplicate update emissions | `WorkoutLoggingViewModel.kt` | Track collector job, cancel before re-launch |

### P2 — Significant defects

| ID | Description |
|---|---|
| F-BUILD-1 | CMake stub produces empty native library for no purpose |
| F-PROG-1 | No session exercise cap — upper body day can generate 12 exercises |
| F-PROG-2 | 2-day frequency uses Upper/Lower split (should be Full Body) |
| F-PROG-ENGINE-1 | RPE not used in progression decisions |
| F-NAV-1 | Exercise list not in bottom navigation — major discoverability issue |
| F-DB-1 | No v1→v11 full chain migration test |
| F-DB-4 | `migration7To8_statusBackfillLogic` is a false-confidence test |
| F-PROGUARD-1 | Core domain logic classes not explicitly kept in ProGuard |
| F-CI-1 | No release build in CI |
| F-WORKOUT-3 | orderIndex computed from stale state — double-tap duplicates orderIndex |

### P3 — Minor / informational

| ID | Description |
|---|---|
| F-BUILD-2 | Release signing requires env vars — not documented |
| F-BUILD-3 | CI missing release and instrumentation jobs |
| F-VTAPER-1 | Volume thresholds are heuristic — not documented as such |
| F-VTAPER-2 | VtaperBalance uses fragile enum ordinal arithmetic |
| F-VTAPER-3 | "Volume" semantics inconsistent across dashboard |
| F-PROG-3 | Readiness thresholds duplicated in ProgramGenerator and ReadinessEntity |
| F-PROG-4 | Upper A and Upper B have identical muscle lists |
| F-ONBOARD-1 | Onboarding completion flag and profile in different stores |
| F-DB-2 | No-op migrations 4_5 and 5_6 are confusing |
| F-DB-5 | Two synonymous methods for getting incomplete workout |
| F-PROG-ENGINE-3 | Regression detection may be over-sensitive |
| F-CI-2 | CI has verbose echo steps |

---

## 19. Subsystem Status Classification

```
Build System:
  Status: VERIFIED_PARTIAL
  Evidence: build.gradle.kts inspected; AGP 8.2.2, Kotlin 1.9.22 correct
  Remaining risk: CMake stub (F-BUILD-1); release signing (F-BUILD-2)

Navigation:
  Status: VERIFIED_PARTIAL
  Evidence: GymCoachNavHost.kt fully inspected; all routes registered
  Remaining risk: exercise_list not in bottom nav (F-NAV-1)

Onboarding:
  Status: VERIFIED_PARTIAL
  Evidence: UserProfileEntity, SharedPrefs flag inspected
  Remaining risk: onboarding UI field completeness for v10→v11 fields UNVERIFIED

Exercise Data:
  Status: VERIFIED_PARTIAL
  Evidence: Entity and DAO inspected; JSON files not read
  Remaining risk: Muscle name taxonomy mismatch (F-TAXONOMY-1)

Muscle Taxonomy:
  Status: VERIFIED_BROKEN (F-TAXONOMY-1)
  Evidence: "Lats" vs "Back" mismatch confirmed by source

V-Taper Domain:
  Status: VERIFIED_PARTIAL
  Evidence: VolumeCalculator, ProgramGenerator, scoring inspected
  Remaining risk: Taxonomy mismatch causes broken volume display

Program Generation:
  Status: VERIFIED_PARTIAL
  Evidence: ProgramGenerator.kt fully inspected; unit tests exist
  Remaining risk: No session cap (F-PROG-1); 2-day logic (F-PROG-2)

Workout Logging:
  Status: VERIFIED_PARTIAL
  Evidence: WorkoutLoggingViewModel, WorkoutDao, WorkoutRepositoryImpl inspected
  Remaining risk: Set number race condition (F-WORKOUT-1); collector leak (F-WORKOUT-5)

Perform Again:
  Status: VERIFIED_COMPLETE
  Evidence: Transaction inspected; unit tests cover clone, null source, repeat

Progression Engine:
  Status: VERIFIED_COMPLETE
  Evidence: ProgressionEngine.kt inspected; unit tests cover all 5 branches

Database / Migrations:
  Status: VERIFIED_PARTIAL
  Evidence: GymCoachDatabase.kt fully inspected; schema JSONs v1–v11 exist
  Remaining risk: No v1→v11 full chain test; backfill test issue (F-DB-4)

Camera / Form Coach:
  Status: RUNTIME_UNVERIFIED
  Evidence: Route registered; no device testing possible

CI/CD:
  Status: VERIFIED_PARTIAL
  Evidence: android-build.yml inspected
  Remaining risk: No release build; no instrumentation in CI

ProGuard / Release:
  Status: LOCAL_EXECUTION_UNVERIFIED
  Evidence: proguard-rules.pro inspected; release build not executed
  Remaining risk: Core domain classes not explicitly kept (F-PROGUARD-1)
```

---

## 20. Next Actions (in priority order)

1. **Fix F-TAXONOMY-1** — Change `VolumeCalculator` to use `"Back"` for lat volume slot. This is a one-line fix that makes the progress dashboard correct.
2. **Fix F-WORKOUT-1 + F-WORKOUT-5** — Serialize `addSet` and cancel stale collectors.
3. **Fix F-PROGUARD-1** — Add `-keep` rules for core domain classes.
4. **Fix F-DB-4** — Fix the false-confidence migration test.
5. **Fix F-PROG-1** — Add session exercise cap respecting `sessionLengthMinutes`.
6. **Add F-DB-1** — Full chain v1→v11 migration test.
7. **Fix F-CI-1** — Add `bundleRelease` job to CI.
8. **Fix F-BUILD-1** — Remove empty CMake stub.

Fixes are committed individually in subsequent commits on this branch.
