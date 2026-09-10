# Remediation Plan for GymCoach Repository Audit Findings

1. **Phase 1: Fix F-CI-1 (CI Workflow)**
   - Update `.github/workflows/android-build.yml` with the validated CI workflow from `docs/CI_WORKFLOW_FIX_F-CI-1.md` (including `release-build-verify` R8 check, `unit-tests`, `android-lint`, `build`).
   - Validate YAML structure and triggers.
   - If git push fails due to GitHub token missing `workflows` OAuth scope, keep the corrected file in tree, detail the exact blocker, and provide manual instructions.

2. **Phase 2: Fix Issue #83 (`vtaperRelevance` else-branch)**
   - In `ProgramGenerator.kt`, update `relevantVtaperScore` so the `else` branch returns `0` instead of the sum of all 4 V-taper scores. This prevents V-taper exercises from inappropriately displacing leg/core/arm exercises in non-V-taper muscle slots.
   - Update/add unit tests in `ProgramGeneratorTest.kt` to verify correct score calculation and non-regression.

3. **Phase 3 & 4: Fix F-WORKOUT-1 (`addSet` race condition) and F-WORKOUT-3 (`addExerciseToWorkout` race condition)**
   - Add database-level unique indices to Room entities:
     - `WorkoutSetEntity`: unique index on `(workoutExerciseId, setNumber)`.
     - `WorkoutExerciseEntity`: unique index on `(workoutId, orderIndex)`.
   - Update database version from 11 to 12 in `GymCoachDatabase.kt` and add `MIGRATION_11_12` (creating the unique indices).
   - Add Room schema JSON `12.json` and migration test `migration11To12` in `RoomMigrationTest.kt`.
   - Add authoritative DB-level orderIndex and setNumber calculation queries / atomic methods in `WorkoutDao` / `WorkoutRepositoryImpl`.
   - Add deterministic concurrency unit/integration tests for concurrent `addSet` and `addExerciseToWorkout` calls.

4. **Phase 5: Fix F-TAXONOMY-1 (Muscle Taxonomy & Issue #51)**
   - Fix compilation breakage in `RoomDatabaseClosedLoopIntegrationTest.kt` by updating `latVolume` to `backVolume`.
   - Audit `VolumeCalculator`, `ExerciseEntity`, `ProgramGenerator`, `HomeViewModel`, and all tests for 100% taxonomy consistency (`Back`, `Lateral Deltoid`, `Rear Deltoid`, `Chest`, `Upper Back`, etc.).
   - Verify volume calculation tests for Back, V-taper categories, and zero-volume edge cases.

5. **Phase 6, 7, 8: Verify/Fix Program Generation (F-PROG-2, F-PROG-1, F-PROG-4)**
   - Verify 1-2 day frequency split logic in `ProgramGenerator.kt` (using Full Body split limited to requested days) and add tests in `ProgramGeneratorTest.kt`.
   - Verify session exercise cap (`maxExercisesPerSession`) in `ProgramGenerator.kt` and add tests ensuring workouts are bounded by session duration/cap.
   - Verify Upper A vs Upper B distinct target muscle programming in `generateUpperLower` and add tests verifying non-identical programming.

6. **Phase 9: Verify/Fix F-NAV-1 (Exercises Tab & Navigation)**
   - Confirm `EXERCISE_LIST` route in `GymCoachNavHost` and `BottomNavigation`. Ensure clean navigation flow to Exercise Detail and Camera screens without dead ends.

7. **Phase 10: Verify/Fix F-WORKOUT-5 (Flow Collector Leak in `loadOrStartWorkout`)**
   - Verify `loadOrStartWorkout` in `WorkoutLoggingViewModel.kt` cancels `workoutCollectorJob` before launching a new collector Flow scope. Ensure configuration changes/re-entry do not leak collectors.

8. **Phase 11: Fix F-DB-4 (Migration Test `migration7To8`)**
   - Inspect `migration7To8_statusBackfillWithRealRows` in `RoomMigrationTest.kt`. Ensure realistic pre-migration rows exist before `MIGRATION_7_8` execution and assert post-migration backfill states (`COMPLETED`, `ACTIVE`, `ABANDONED`).

9. **Phase 12: Repository Hygiene**
   - Inspect and delete stray root `./Benchmark.kt` file after confirming no build script/tooling uses it.
   - Delete duplicate source tree directory `./root/` after confirming Gradle source sets only use `app/`.
   - Document placeholder commit `4a10cf0b` and GitHub metadata items as repository hygiene debt.

10. **Phase 13: Pre-commit Steps & Full Regression Verification**
    - Call `pre_commit_instructions` tool and execute required checks.
    - Run Gradle build (`./gradlew assembleDebug`), unit tests (`./gradlew testDebugUnitTest`), and lint/migration tests.
    - Generate final evidence matrix and report.
