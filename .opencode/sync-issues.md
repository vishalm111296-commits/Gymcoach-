# Sync Issues (Unresolved Only)

## SYNC-9
- Severity: CRITICAL (build-blocking — KSP/Room annotation processing fails, blocks ALL unit-test compile/run)
- Files: WorkoutDao.kt (T6.3 bulk-query relation POJOs `WorkoutExerciseWithSetsEntity` / `WorkoutWithExercisesAndSets`)
- Problem: Fresh `./gradlew :app:kspDebugKotlin` (with aapt2 override) FAILS with 5 Room KSP errors, all in WorkoutDao.kt lines 443/453/454/456:
  * 443: "The class must be either @Entity or @DatabaseView." (WorkoutExerciseWithSetsEntity)
  * 453: "...must have a usable public constructor... param:workout -> matched, param:exercises -> unmatched"
  * 454: "Cannot find setter for field."
  * 456: "Cannot find the child entity column `workoutId` in WorkoutExerciseWithSetsEntity."
  Root: nested relation POJO (WorkoutExerciseWithSetsEntity) is not accepted as the child of the top @Relation; Room rejects the nested relation wiring.
  IMPORTANT: The earlier `compileDebugKotlin` "BUILD SUCCESSFUL" was misleading — its `kspDebugKotlin` task was UP-TO-DATE (stale cache), so it did NOT re-validate the Room KSP pass. The current real tree state does NOT pass KSP. This is why the T6.3 verification (which used only behavioral reasoning) missed it.
- Effect: `compileDebugUnitTestKotlin` / `testDebugUnitTest` cannot run → the entire M6 verify/mark gate (incl. T6.2 test run) is blocked until fixed. NOT caused by the S6.1/S6.2 code (ExerciseDao/ProgramGenerator/VolumeCalculator/HomeViewModel changes) — those files are individually correct (S6.2.1 DAO query passed Room KSP with ZERO errors in the same fresh run); the block is isolated to WorkoutDao nested-relation POJOs in T6.3 scope.
- Fix: Rework WorkoutDao nested relations to a Room-supported pattern (e.g., give the top relation POJO a no-arg/default constructor and ensure the nested `@Relation` child POJO is annotated/structured so Room treats it as a valid relation target; or split into two flat @Relation queries and combine in the repository instead of a single nested-relation @Query). Re-verify with a FRESH kspDebugKotlin (delete build/ or rm -rf the ksp cache) — do not trust UP-TO-DATE.
- Status: pending

## SYNC-8 ✅ RESOLVED (2026-09-06 — re-verified: implementation now present and Room-validated)
- Original problem: Worker task_710ebfc2 ("Fix ProgramGenerator primary-muscle matching") previously returned no ProgramGenerator changes (verified at earlier review; S6.2.1/S6.2.2/S6.2.3 all missing).
- Current verified state (working tree, fresh review):
  * S6.2.1 ✅ — `ExerciseDao.getPrimaryMusclesByExercise()` + `PrimaryMuscleRow` present (JOIN exercise_muscles + muscles, role='primary'); table/column names match entities (`exercise_muscles.exercise_id/muscle_id/role`, `muscles.id/name`); Room KSP validated the query with ZERO errors in a fresh `kspDebugKotlin` run.
  * S6.2.2 ✅ — ProgramGenerator.kt: token-based `matchesMuscle` (primary ids + secondary taxonomy tokens + category + vtaper scores), `SLOT_KEY_MUSCLE_IDS`, PRIMARY_BOOST/SECONDARY_BOOST, BACK/CHEST/CORE_MUSCLE_IDS; all generate*/buildDay call sites thread `primaryMusclesByExercise`. Seeder chain verified: taxonomy flattened (subdivision ids = snake_case e.g. biceps/hamstrings/lateral_deltoid), JSON `primary_muscles` → exercise_muscles role='primary', muscles.name = taxonomy id → matches SLOT_KEY_MUSCLE_IDS keys. All 17 slot-key taxonomy ids present in muscle_taxonomy.json.
  * S6.2.3 ✅ — ProgramGeneratorTest.kt: 10 behavioral tests (lateral raise → Lateral Deltoid ranking, curl → Biceps slot never Hamstrings, primary-hamstrings leg curl outranks secondary-only squat, lower_back never Chest, readiness sets/RPE, equipment filtering). Test logic hand-traced; deps (mockk, coroutines-test, junit) present.
  * GATE NOTE: actual `testDebugUnitTest` execution still blocked by SYNC-9 (WorkoutDao KSP). S6.2.2/S6.2.3 final [x] pending green run after SYNC-9 fix.

## SYNC-6
- Severity: CRITICAL (P0 — upgrade from real v1/v2/v3 DB crashes)
- Files: GymCoachDatabase.kt MIGRATION_1_2..10_11, RoomMigrationTest.kt
- Problem (verified against app/schemas exports + Room 2.6.1 MigrationTestHelper source):
  1. v1/v2/v3 exports contain ONLY workouts/workout_exercises/workout_sets (NO `exercises` table). But MIGRATION_2_3 runs `ALTER TABLE exercises ...` → crashes with "no such table: exercises" on a real v2 DB. All the table creation currently in MIGRATION_2_3 must move to MIGRATION_3_4 (the actual v3→v4 boundary where exercises + all tables appear per export 4.json), exercises table created FIRST.
  2. program_days: v4-v8=`focus` only; v9=`focus`+`target_muscles`; v11=`target_muscles` only (no focus). Current MIGRATION_8_9 never adds target_muscles; MIGRATION_10_11 never rebuilds program_days to drop focus → Room validation fails on v4-v8/v9/v10 upgrades.
  3. MIGRATION_1_2 still creates workout_sets WITHOUT the FK `REFERENCES workout_exercises(id) ... ON DELETE CASCADE` (present in v1/v2 exports) → v2 validation fails.
- Attempt 3 (task_f7d09d71) result: FALSE — Worker completed [DONE] in 1m8s with ANALYSIS ONLY. Zero file changes (git diff empty vs b5fa19c), deliverable .opencode/docs/migration-rebase-diff.md never created. All 6 grep acceptance criteria fail: setType in 3_4 not 2_3 (line 265); 14x `ALTER TABLE exercises` still in 2_3 (lines 65-78); FTS still in 6_7 (line 284) not 5_6; status still in 7_8 (line 316) not 6_7; target_muscles 0 occurrences; program_days_new 0 occurrences. RoomMigrationTest untouched (old names/positions; broken migration7To8_statusBackfillLogic at line 165 has no createDatabase; migrate8To9 has no seeding; no 1→11 chain test).
- NEW nuance to resolve during re-do: v3-v11 export defaultValue for setType = None, v9/v10 target_muscles default = None, but spec SQL adds `DEFAULT 0`/`DEFAULT ''`. Room 2.6.1 TableInfo comparison includes defaultValue → potential validation failure unless entities declare matching ColumnInfo(defaultValue=...) or the ALTERs avoid defaults (needs empirical check; for `status` the export default='NOT_STARTED' matches the ALTER so it is safe).
- Fix/Status: Re-dispatch Worker (4th attempt) transcribing .opencode/docs/migration-rebase-spec.md exactly. Re-verify all 6 greps + `@Test >= 13` with required new names + diff doc exists.

## SYNC-4 ✅ RESOLVED
- MIGRATION_11_12 removed, @Database version stays 11 (verified: no MIGRATION_11_12 in file; version = 11).

## RESOLVED ISSUES (archived)

### SYNC-1 ✅ RESOLVED
- VolumeCalculator.directSetsByMuscle/indirectSetsByMuscle now counts individual sets via for-loop (not groupBy exerciseId)
- Fixed in commit b673d7c + subsequent edit

### SYNC-2 ✅ RESOLVED
- HomeViewModel.plannedWeeklySets() now attributes per-exercise via exerciseMuscleMap
- ExerciseRepository injected and _exerciseMuscleMap populated
- Fixed in commit b673d7c

### SYNC-3 ✅ NOTED (out of scope)
- HomeViewModel.buildTrainingBalance uses "Back" for latVolume key, VolumeCalculator uses "Lats"
- Pre-existing naming inconsistency, not introduced by this fix

### SYNC-5 ✅ RESOLVED
- BodyMeasurementEntity nullable vs non-nullable: Worker reverted to non-nullable Double = 0.0 fields, ProgressViewModel.saveMeasurement uses `?: 0.0` to convert null → 0.0, load() uses `takeIf { it > 0 }` to convert 0.0 → null for UI. Convention is consistent across entity/ViewModel/UI layers.

## SYNC-7
- Severity: CRITICAL (V-taper volume bars/muscle-insight all wrong; weighted credits never used)
- Files: VolumeCalculator.kt, HomeViewModel.kt, VolumeCalculatorTest.kt, + MISSING core/home/VtaperAttribution.kt, core/home/VtaperAttributionTest.kt
- Problem: Worker delegate task_c8f7840a returned [DONE] with NO output and NO code changes. All of M6.1 remains at buggy original state:
  * MuscleVolume still has `weeklySets: Int` (raw total) — weighted credits (avgWeekly) computed but unused; classification on raw count (scientifically wrong).
  * isoWeekKey still uses Calendar/Locale (not ISO-8601 weekBasedYear/WeekFields).
  * HomeViewModel still broadcasts day's total to category map; bars/insight always zero.
  * VtaperAttribution.kt + VtaperAttributionTest.kt not created.
  * VolumeCalculatorTest still encodes buggy raw-count semantics (not rewritten to correct).
- Fix: Re-dispatch Worker (resume ses_f8a3be03fffeIBZj6uXn5aY2X1) implementing S6.1.1-S6.1.4 per task spec. Re-verify.
- Status: pending
