# Reconciliation Wave R — make main compilable & functionally correct

> Executor: orchestrator inline (subagent transport DOWN). Writes via GitHub MCP;
> CI (`android-build.yml` on PR) is the test runner. Payload ceiling ~6KB.

**Goal:** main@`7959fe8a` compiles and ProgramGenerator ranks per muscle slot; verified by green PR checks.

**Stack:** Kotlin 1.9.22 · AGP 8.2.2 · Room **2.6.1 pinned** · JUnit4/MockK/coroutines-test.

## Global Constraints
- Room stays 2.6.1; no androidx.room Gradle plugin; ksp arg stays.
- No new deps; no entity changes; DB version stays 10.
- Every claim needs evidence: green check-run or re-fetched file content.
- Squash-merge only after ALL checks green + Copilot review addressed.

## Tasks
### T1 (done with this commit)
- [x] Branch `reconcile/main-v10`; this plan committed.

### T2 Room API mismatch (compile blocker)
File: `app/src/main/kotlin/com/gymcoach/app/data/local/database/GymCoachDatabase.kt`
- [ ] `.fallbackToDestructiveMigration(false)` -> `.fallbackToDestructiveMigration()`
      (Boolean overload does not exist in Room 2.6.1).
- [ ] Evidence: re-fetch; old call absent, tail intact.

### T3 Equipment separator defect (this commit)
File: `app/src/main/kotlin/com/gymcoach/app/core/exercise/EquipmentAvailability.kt`
- [x] `isLimited`: DB truth stores compounds comma-separated (seeder `joinToString(",")`).
      Normalize `+`->`,`, split `,`, lowercase tokens, drop empty/bodyweight, then any-missing check.
- [x] KDoc updated (canonical `,`, legacy `+` tolerated). Signature unchanged.

### T4 Per-slot V-taper ranking + tests (TDD)
Files: `core/program/ProgramGenerator.kt`; new `app/src/test/kotlin/com/gymcoach/app/core/program/ProgramGeneratorTest.kt`
- [ ] Add `relevantVtaperScore(exercise, muscle)`: Back->vtaperLat, Lateral Deltoid->vtaperLateralDelt,
      Chest->vtaperUpperChest, Rear Deltoid->vtaperRearDelt, else 0.
      buildDay sorts by that (was aggregate sum of all four).
- [ ] Tests (MockK dao.getAll->flowOf, runTest). Fixture:
      A(barbell,Back,lat=10) B(dumbbell,Back,lat=6) C(dumbbell+bench,Chest,uc=9) D(bodyweight,Chest,uc=4).
      1) gym/freq4 Upper A => back-slot=A AND chest-slot=C (aggregate champ must not steal chest slot).
      2) home/freq4 => A excluded, B picked, C retained.
      3) custom(bodyweight-only) => only D-class chest exercise survives.

### T5 Seeder observability (flagged large-payload attempt)
File: `core/exercise/ExerciseSeeder.kt`
- [ ] Silent `catch (_: Exception) { }` x2 -> `catch (e: Exception) { Log.w(TAG, ..., e) }`;
      add TAG; log seeded count after seedExercises.
- [ ] MANDATORY re-fetch integrity check; on truncation restore pristine + retry once, else BLOCKED note.

### T6 PR gate
- [ ] Open PR -> main with matrix/evidence table; request Copilot review (skeptic/security substitute).
- [ ] Poll get_status/get_check_runs; red -> debugger loop <=3. Green -> squash merge -> re-audit main.

## Out of scope (tracked)
Schemas 1/6/7 provenance (back-filled artifacts; no build impact; revisit with MigrationTestHelper wave).
