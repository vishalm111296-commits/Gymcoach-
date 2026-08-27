1. **Settings Screen Implementation**
   - Use `write_file` to create `app/src/main/kotlin/com/gymcoach/app/presentation/settings/SettingsScreen.kt`.
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt` to include the Settings route.
   - Use `replace_with_git_merge_diff` to wire it into `app/src/main/kotlin/com/gymcoach/app/presentation/profile/ProfileScreen.kt`.
   - Use `list_files` and `read_file` to verify the creation and edits.
   - Run `./gradlew assembleDebug --no-daemon` to verify it compiles.

2. **Workout Summary Celebration Screen**
   - Use `write_file` to create `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutSummaryScreen.kt`.
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt`.
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutSessionScreen.kt` to navigate to this screen instead of Home upon workout completion.
   - Use `list_files` and `read_file` to verify the creation and edits.
   - Verify compilation with `./gradlew assembleDebug --no-daemon`.

3. **Motion and Animations Pass**
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/presentation/home/HomeDashboardScreen.kt` to wrap `TodayWorkoutCard` in `AnimatedVisibility`. (Verified usage of `TodayWorkoutCard` via `grep`).
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt` to add custom `enterTransition` and `exitTransition` animations for navigation composables.
   - Use `read_file` to confirm edits.
   - Check compilation with `./gradlew assembleDebug --no-daemon`.

4. **Workout Experience Modernization**
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutSessionScreen.kt` to replace the `OutlinedTextField` elements (now explicitly verified via `grep -A 80 "val setTypeColor" | tail -n 40` that Weight, Reps, RPE, and Rest are indeed `OutlinedTextField`s) with a `Row` containing a `-` `IconButton`, a `Text` displaying the value, and a `+` `IconButton` for easier gym usage.
   - Use `read_file` to confirm edits.
   - Check compilation with `./gradlew assembleDebug --no-daemon`.

5. **Empty States & Dialog Polish**
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressDashboardScreen.kt` to redefine `EmptyPlaceholder(message: String)` (verified via grep) into a visually appealing `Card` containing an icon and centered text instead of raw `Text`.
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/presentation/progress/components/MeasurementLogDialog.kt` to replace `AlertDialog` (verified via `cat`) with an `@OptIn(ExperimentalMaterial3Api::class)` `ModalBottomSheet`.
   - Use `read_file` to confirm edits.
   - Check compilation with `./gradlew assembleDebug --no-daemon`.

6. **Exercise Visuals Polish**
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/presentation/components/ExerciseItemCard.kt` (verified via `cat`) and `app/src/main/kotlin/com/gymcoach/app/presentation/detail/ExerciseDetailScreen.kt` to use a dynamic color-coded box containing the first character of the exercise name instead of generic Material default icons, giving it a polished typography-based placeholder look.
   - Use `read_file` to confirm edits.
   - Check compilation with `./gradlew assembleDebug --no-daemon`.

7. **Branding: Splash Screen**
   - Use `write_file` to create a `app/src/main/kotlin/com/gymcoach/app/presentation/splash/SplashScreen.kt` Composable with branding text/icon and a fade transition.
   - Use `replace_with_git_merge_diff` to update `app/src/main/kotlin/com/gymcoach/app/ui/MainActivity.kt` and `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt` to set `SplashScreen` as the start destination.
   - Use `list_files` and `read_file` to verify the creation and edits.
   - Run `./gradlew assembleDebug --no-daemon` to confirm the project builds successfully.

8. **Test Full Regression**
   - Run `./gradlew testDebugUnitTest lintDebug --no-daemon` to ensure everything is solid.

9. **Pre-commit Steps**
   - Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.

10. **Final Submission & Report generation**
    - Generate the final evidence-based report per issue instructions and submit.
