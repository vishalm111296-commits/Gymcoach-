# Sync Issues (Unresolved Only)

## SYNC-1
- Severity: HIGH
- Files: `app/src/test/kotlin/.../VolumeCalculatorTest.kt` ↔ `app/src/main/kotlin/.../VolumeCalculator.kt`
- Problem: `VolumeCalculator.directSetsByMuscle` (lines 108-117) uses `groupBy { it.exerciseId }` which counts **distinct exercises**, not individual sets. When multiple sets share the same `exerciseId`, flatMap produces only ONE entry per exercise per muscle. 7 tests assume each set is counted individually but will get exercise counts instead.
- Failing tests (all use `exerciseId = 1L` for multiple sets):
  1. `testFractionalAveragesNoTruncation` — asserts `directSets=3`, will get 1
  2. `testMultiWeekAveraging` — asserts `weeklySets=18, directSets=18, status=HIGH`, will get 1/1/INSUFFICIENT
  3. `testClassificationModerate` — asserts `MODERATE`, will get `INSUFFICIENT`
  4. `testClassificationOptimal` — asserts `OPTIMAL`, will get `INSUFFICIENT`
  5. `testClassificationHigh` — asserts `HIGH`, will get `INSUFFICIENT`
  6. `testClassificationExcessive` — asserts `EXCESSIVE`, will get `INSUFFICIENT`
  7. `testClassificationBoundaryValues` — all 4 boundary assertions wrong
- Fix: Use unique `exerciseId` per set in these tests (e.g., `exerciseId = it.toLong()` or assign distinct IDs 1L, 2L, 3L...) so `groupBy { it.exerciseId }` produces separate groups. OR fix VolumeCalculator to count sets instead of exercises if that is the intended behavior.
- Status: pending

## SYNC-2
- Severity: HIGH
- Files: `app/src/main/kotlin/.../HomeViewModel.kt`
- Problem: `plannedWeeklySets()` (lines 173-186) still broadcasts entire day's total set count to every target muscle instead of per-exercise attribution. The `ExerciseRepository` is injected (line 71) but never used. Worker task `ses_f8ad639c9ffeOGBISOL6gXSbB5` was spawned but fix did not land.
- Fix: Inject ExerciseRepository mapping to attribute sets per-exercise to correct muscle.
- Status: pending
