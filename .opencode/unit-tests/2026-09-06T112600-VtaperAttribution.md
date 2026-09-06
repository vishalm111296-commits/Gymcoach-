# Unit Test Record: VtaperAttribution

**Date**: 2026-09-06T11:26
**Task**: task_4a8a719c (S6.1.3/S6.1.2 test portion — VolumeCalculator + VtaperAttribution tests)
**Files**:
- Production: `app/src/main/kotlin/com/gymcoach/app/core/home/VtaperAttribution.kt`
- Test: `app/src/test/kotlin/com/gymcoach/app/core/home/VtaperAttributionTest.kt`

## Coverage (8 @Tests)
1. `pullUpLikeAttributedToLatsBicepsAndUpperBack` — vtaperLat=10 + back + "biceps, forearms" → Lats, Biceps, Upper Back; NOT Hamstrings
2. `lateralRaiseLikeAttributedToLateralDeltoidOnly` — vtaperLateralDelt=10 + shoulders → exactly `["Lateral Deltoid"]`
3. `squatLikeAttributedToLegsHamstringsAndCore` — legs + "hamstrings, abs, lower_back" → Legs, Hamstrings, Core; NOT Biceps
4. `benchPressLikeAttributedToUpperChest` — chest + vtaperUpperChest=7 → exactly `["Upper Chest"]`
5. `coreExerciseAttributedToCore` — muscleGroup core → contains Core
6. `emptySecondaryAddsNoSpuriousMuscles` — legs + no secondary → exactly `["Legs"]`
7. `coreSourceDoesNotDuplicateCoreEntry` — core + "abs, deep_core" → Core count == 1 (distinct)
8. `allSecondaryTaxonTokensAttributed` — legs + vtaperLat=3 + "quadriceps,triceps,glutes,calves" → all taxa + distinct

## Verification Status
- **LSP**: CLEAN on both VtaperAttribution.kt and VtaperAttributionTest.kt
- **Semantic alignment**: All 8 tests verified against production `VtaperAttribution.contributors()` logic (vtaper scores >0; muscleGroup back/legs/core; secondary taxonomy tokens biceps/triceps/quadriceps/hamstrings/glutes/calves/abs/obliques/deep_core; distinct()).
- **grep**: `grep -rn "weeklySets" app/src/test/` → NOTHING (mandatory check passed)
- **Compile gate**: BLOCKED by SYNC-9 — `kspDebugKotlin` fails on UNRELATED WorkoutDao.kt (T6.3) relation POJOs. Test source itself is structurally valid, but the full `compileDebugUnitTestKotlin`/`testDebugUnitTest` could not be executed because main-source Room KSP fails first.

## Notes
- VtaperAttributionTest is pure JVM Kotlin (JUnit + java.time only), consistent with test patterns.
- These 8 tests reinforce (do not weaken) S6.1.2's per-exercise attribution regression coverage; complement HomeViewModel plannedMuscleVolume usage.
- NOT marked [x] in todo.md: the mandatory test-execution gate is blocked upstream by SYNC-9 (WorkoutDao KSP break). Re-verify once T6.3 KSP is fixed.
