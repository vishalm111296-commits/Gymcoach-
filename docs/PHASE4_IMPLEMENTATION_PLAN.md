# IMPLEMENTATION PLAN: HISTORY & PROGRESS UPGRADES + UI POLISH

## OBJECTIVE
Implement the UX Research "Phase 4" roadmap item (History & Progress Upgrades) by adding a "Perform Again" feature to Workout History and resolving critical UI crash vectors (`!!` usages) alongside a strictly controlled Premium UI redesign of the Presentation Layer.

## NON-GOALS
- Do not modify Room Database schemas, migrations, or DAOs.
- Do not modify the Program Engine, V-Taper math, or Volume Calculator.
- Do not build the unverified Live Camera Wiring.
- Do not change navigation structures or route handling.

## FILES TO CHANGE
1. **Crash Vectors (Safety Pass):**
   - `app/src/main/kotlin/com/gymcoach/app/core/progression/PRDetector.kt`
   - `app/src/main/kotlin/com/gymcoach/app/core/progression/ProgressionEngine.kt`
   - `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutLoggingViewModel.kt`
2. **Premium Redesign (Presentation Layer):**
   - `app/src/main/kotlin/com/gymcoach/app/presentation/home/HomeDashboardScreen.kt`
   - `app/src/main/kotlin/com/gymcoach/app/presentation/home/components/TodayWorkoutCard.kt`
   - `app/src/main/kotlin/com/gymcoach/app/presentation/home/components/VtaperFocusCard.kt`
   - `app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressDashboardScreen.kt`
   - `app/src/main/kotlin/com/gymcoach/app/presentation/progress/components/BodyMeasurementTrend.kt`
   - `app/src/main/kotlin/com/gymcoach/app/presentation/progress/components/StrengthProgressChart.kt`
   - `app/src/main/kotlin/com/gymcoach/app/presentation/detail/ExerciseDetailScreen.kt`
3. **History Upgrades:**
   - `app/src/main/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryDetailScreen.kt` (Add 'Perform Again' button).

## FILES NOT TO CHANGE
- All Room Database related files (`GymCoachDatabase.kt`, `WorkoutDao.kt`, etc.).
- The `GymCoachAppShell.kt` and `GymCoachNavHost.kt`.
- `ProgramGenerator.kt` and `VolumeCalculator.kt`.
- Any existing test files (except to add new test logic if strictly required).

## DATA FLOW
1. **History Upgrades:** The 'Perform Again' button in `WorkoutHistoryDetailScreen` will trigger a navigation event to `Routes.workoutSession(workoutId)` passing the historic ID so the ViewModel can clone and start it.
2. **UI Safety:** Safe operators (`firstOrNull`, `lastOrNull`) will map empty dataset reads into nullable boundaries (e.g., `Double?`), which the Compose View layers will interpret as missing data and render as `--` instead of crashing.

## ARCHITECTURE
All changes are constrained to the `presentation` layer and minor adjustments in the `domain`/use-case layer to avoid `NullPointerException`s. The structural Clean Architecture boundaries remain intact.

## DEPENDENCIES
- Existing `WorkoutRepository` and `ProgressionEngine`.

## DATABASE IMPACT
- **Zero.**

## TEST STRATEGY
- **Baseline Verification:** Re-run `testDebugUnitTest` to guarantee 73/73 tests continue to pass after `PRDetector` and `ProgressionEngine` null-safety changes.
- **Visual/Manual:** Run `assembleDebug` to ensure all `@Composable` files compile and there are no lint warnings regarding the UI structural updates.

## EDGE CASES
- What if a user opens ProgressDashboard on a fresh install? -> The `BodyMeasurementTrend` will gracefully handle a null value without crashing or showing `0.0`.
- What if a user opens a Workout History that was partially corrupted? -> The "Perform Again" button will only clone successfully verified previous workout entities.

## ACCEPTANCE CRITERIA
1. The app compiles without errors.
2. All 73 unit tests pass.
3. No `!!` operators exist in the presentation ViewModels or Composable screens.
4. "Perform Again" button exists in Workout History Detail.
5. Home and Progress Dashboards utilize `DarkCard` elevated containers and `Brush.verticalGradient` styling.

## ROLLBACK STRATEGY
- All implementations will be done on the isolated `feat/premium-redesign-and-crash-fixes` branch starting from the verified `e5e0b1e` HEAD. Rollback is a simple `git reset --hard e5e0b1e`.

## REPAIR-CHAIN STEPS
1. Implement Crash Vectors (Safety Pass). Run tests.
2. Implement History Upgrade ("Perform Again"). Run tests.
3. Implement Premium Redesign slices. Run tests.
