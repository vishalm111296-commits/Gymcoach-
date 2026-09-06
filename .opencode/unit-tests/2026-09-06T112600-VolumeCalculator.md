# Unit Test Record: VolumeCalculatorTest (S6.1.1/S6.1.3 rewrite)

**Date**: 2026-09-06T11:26
**Task**: task_4a8a719c (Rewrite VolumeCalculatorTest semantics + VtaperAttributionTest)
**Files**:
- Production: `app/src/main/kotlin/com/gymcoach/app/core/program/VolumeCalculator.kt`
- Test: `app/src/test/kotlin/com/gymcoach/app/core/program/VolumeCalculatorTest.kt`

## Coverage (26 @Tests) — verified assertion-by-assertion against production
1-3. `testPrimaryRoleGivesFullCredit` / `testSecondaryRoleGivesHalfCredit` / `testStabilizerRoleGivesQuarterCredit` — 1.0 / 0.5 / 0.25 weighted credits, correct direct/indirect raw counts, INSUFFICIENT
4. `testMixedRolesAttributionOnOneSet` — squat: quads 1.0, hams 0.5, core 0.25 with direct/indirect split
5. `testFractionalAveragesNoTruncation` — 3 sets / 2 ISO weeks → weeklyVolume 1.5 (FLIPPED from raw 3), directSets=3
6. `testMultiWeekSteadyVolumeIsPerWeekNotTotal` — 6/week × 3 weeks → 6.0 (NOT 18) → INSUFFICIENT (FLIPPED from old HIGH mis-encode)
7-9. `testSteadyFourteenPerWeekIsOptimal` (14.0→OPTIMAL), `testSteadyEighteenPerWeekIsHigh` (18.0→HIGH), `testSteadyTwentyTwoPerWeekIsExcessive` (22.0→EXCESSIVE)
10. `testIsoWeekYearBoundarySharesOneBucket` — Dec 29 2025 + Jan 1 2026 both in ISO 2026-W01 → single bucket → weeklyVolume 2.0 (FLIPPED from old two-bucket 1.0+1.0 bug-encode)
11-12. `testWarmupSetsExcluded` / `testDropAndFailureSetsExcluded` — setType != 0 excluded
13. `testIncompleteSetsExcluded` — completed=false excluded
14-18. Classification bands: 9→INSUFFICIENT, 12→MODERATE, 16→OPTIMAL, 20→HIGH, 25→EXCESSIVE (+ labels "Too low"/"Moderate"/"Optimal"/"High"/"Very high")
19. `testClassificationBoundaryValues` — exactly 10→MODERATE, 14→OPTIMAL, 18→HIGH, 22→EXCESSIVE
20-22. V-Taper balance (Calculation/Good/Low) — ordinal-based scoring verified vs `calculateVtaperBalance()`
23. `testEmptySetsReturnsAllZero` — 0.0 / 0 / 0 / INSUFFICIENT for all 12 muscles
24. `testUnknownExerciseIdIgnored` — IDs not in map contribute 0
25. `testTrainingBalanceAsListSize` — 12 entries
26. `testMuscleRoleCreditValues` — PRIMARY 1.0, SECONDARY 0.5, STABILIZER 0.25

## Verification Status
- **LSP**: CLEAN on VolumeCalculator.kt + VolumeCalculatorTest.kt
- **API**: Uses ONLY new API — `MuscleVolume.weeklyVolume` (Double), `classify(Double)`, `isoWeekKey` via WeekFields.ISO. `weeklySets` fully removed.
- **grep**: `grep -rn "weeklySets" app/src/test/` → NOTHING; `grep -rn "weeklySets" app/src/main/kotlin/` → NOTHING (mandatory checks passed)
- **Semantics**: weeklyVolume = avg weighted credit per ISO week across observed weeks. Test helpers use noon-UTC dates; ISO boundary test asserts shared 2026-W01 bucket directly (no Calendar-based expectation computation)
- **Compile gate**: BLOCKED by SYNC-9 — `kspDebugKotlin` fails on UNRELATED WorkoutDao.kt (T6.3) relation POJOs. `compileDebugUnitTestKotlin` cannot proceed through main-source Room KSP. Test source itself is structurally valid (LSP-clean, all referenced API matches production).

## Notes
- 26 @Tests in VolumeCalculatorTest (up from original ~13; coverage strengthened, not weakened)
- 8 @Tests in VtaperAttributionTest (new file)
- NOT marked [x] in todo.md: mandatory test-EXECUTION gate is blocked upstream by SYNC-9. Re-verify after T6.3 WorkoutDao KSP fix.