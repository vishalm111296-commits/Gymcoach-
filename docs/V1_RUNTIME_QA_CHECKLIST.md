# GymCoach Version 1.0 — Runtime QA Checklist & Release Readiness Report

- **APK**: `gymcoach-debug-apk` artifact from CI run
  https://github.com/vishalm111266-beep/GymCoach/actions/runs/30713356849 (pre-fix baseline)
- **Commit under test**: `6415477` (pre-fix). All five blockers were verified and fixed in commit `3e23844`
  (CI run `30731302439`: Build ✅ Unit Tests ✅ Android Lint ✅).
- **Validation method**: Static code analysis grounded in the installed build (evidence-first).
  No device/emulator is available in the validation environment, so **every check that requires
  on-screen interaction is marked `DEVICE TEST REQUIRED` and has NOT been executed**. Items that are
  provable from source are marked PASS/FAIL with **confidence** ratings.

---

## Validation status key

| Status | Meaning |
|---|---|
| ✅ PASS (code) | Provable from source; deterministic behavior |
| ❌ FAIL (code) | Provable defect in source that will manifest at runtime |
| 🟡 DEVICE TEST REQUIRED | Requires on-device execution; not run in this environment |
| 🔴 BLOCKER | Prevents the feature from being usable at all |

---

## 1. App Launch

- **Expected behavior**: Cold start shows the Exercise Library screen (list of exercises + search bar + category tabs) without crash. Hilt graph initializes, Room DB opens, theme applies.
- **How to test**: Install APK, tap launcher icon, observe first screen and logcat for `FATAL EXCEPTION`.
- **Status**: 🟡 DEVICE TEST REQUIRED (cold-start itself); ✅ PASS (code) for Hilt/Room wiring.
- **Confidence**: HIGH
- **Possible causes if it fails**:
  - Missing `@HiltAndroidApp` / `@AndroidEntryPoint` wiring → dagger component exception. (Verified present: `GymCoachApplication.kt`, `MainActivity.kt`.)
  - Room schema/entity mismatch → `IllegalStateException`. (Fresh install is fine; see item 20.)
  - `android.hardware.camera.any required=true` in manifest → app cannot be installed on devices without a camera (installer error, not a launch crash).
- **Fix recommendation**: On a camera-less test device, flip the manifest to `required="false"`. Otherwise none needed — app is launch-wired.

## 2. Navigation

- **Expected behavior**: Exercise List → Exercise Detail (by id), → Workout History, History → History Detail, History → Resume session. Back button pops stack to previous screen.
- **How to test**: Tap each entry point; verify routes resolve and no `IllegalArgumentException` for route strings.
- **Status**: ✅ PASS (code) for wired routes after `3e23844`. ⚠️ Prior audit: ❌ FAIL — **two screens were not reachable**: Progress Dashboard and Camera/Form Analysis had **no NavHost route** and **zero call sites** (`ProgressDashboardScreen`, `CameraPreviewScreen`, `FormAnalyzer` were dead code). Both were wired into navigation in `3e23844` (routes `progress`, `camera`; entry icons in Exercise List).
- **Confidence**: HIGH
- **Possible causes if it fails**: Routes exist but no composable references them.
- **Fix recommendation**: Add `Routes.PROGRESS` and `Routes.CAMERA` entries in `GymCoachNavHost.kt` and entry-point buttons (e.g., a bottom nav or top-bar icons).

## 3. Exercise Library

- **Expected behavior**: Shows a searchable, filterable list of preloaded exercises (name, muscle group, difficulty).
- **How to test**: Open app; expect non-empty list.
- **Status**: ❌ FAIL (code) was confirmed — **BLOCKER**. The `exercises` table was **never seeded** (no callback, no assets, no UI insert). **FIXED in `3e23844`**: a `RoomDatabase.Callback.onCreate` in `GymCoachDatabase.kt` seeds the 9 `FormAnalyzer` exercises via raw SQL on first DB creation.
- **Confidence**: HIGH
- **Possible causes if it fails**: No `RoomDatabase.Callback.onCreate` seed, no `assets/` seed file, no hardcoded insert.
- **Fix recommendation**: Add a `RoomDatabase.Callback` (or `createFromAsset`) that inserts the 9 supported exercises (Bicep Curl, Squat, Push-up, Shoulder Press, Lateral Raise, Bent-over Row, Plank, Deadlift, Bench Press) with description/equipment/difficulty on first DB creation.

## 4. Exercise Details

- **Expected behavior**: Tapping an exercise shows description, muscle group, equipment, difficulty; missing exercise shows "Exercise not found."
- **How to test**: Navigate from library.
- **Status**: 🟡 DEVICE TEST REQUIRED for render; ✅ PASS (code) for loading logic (`ExerciseDetailViewModel.loadExercise`, null-handling present). Note `ExerciseVideoPlayer` exists but is **never used** here, so **no demonstration video** is shown.
- **Confidence**: HIGH
- **Possible causes if it fails**: Exercise not seeded (see item 3) → screen always "not found".
- **Fix recommendation**: Seed library (item 3); optionally wire `ExerciseVideoPlayer` to a video URI.

## 5. Workout Session

- **Expected behavior**: User can start a new session, add exercises, add sets, log weight/reps/RPE, mark sets complete, and finish.
- **How to test**: Tap "New Workout" / start a session.
- **Status**: ❌ FAIL (code) was confirmed — **BLOCKER**. No UI navigated to `workout_session` with a null id; the History "＋" was a no-op (`WorkoutHistoryScreen.kt:96`). **FIXED in `3e23844`**: "＋" now calls `onNewWorkout` → `navigate(Routes.workoutSession())`; the existing `startNewWorkout()` path is now reachable.
- **Confidence**: HIGH
- **Possible causes if it fails**: Missing start-workout navigation from Exercise List and/or History.
- **Fix recommendation**: Wire the History "＋" (or add a FAB on the Exercise List) to `navController.navigate(Routes.workoutSession())` (null id). The underlying `WorkoutLoggingViewModel.startNewWorkout()` already exists and is unused.

## 6. Rest Timer

- **Expected behavior**: Marking a set complete starts a countdown (default 90 s) shown in a card; completing/stopping cancels it. When it hits 0 it stops.
- **How to test**: In a session, toggle a set's checkbox.
- **Status**: ✅ PASS (code) for in-process behavior; ❌ FAIL (code) for lifecycle robustness — see below.
- **Confidence**: HIGH
- **Possible causes if it fails**:
  - Timer is a **pure in-memory coroutine** (`RestTimerManager`, a singleton) ticked inside `viewModelScope` of the session VM. **Process death or background kill loses the timer instantly**; no foreground service or WorkManager.
  - `RestTimerManager` is shared across ViewModels; `WorkoutHistoryViewModel.onCleared()` and `WorkoutLoggingViewModel.onCleared()` both call `restTimer.stop()` (`WorkoutHistoryViewModel.kt:181-184`), so leaving History can kill an active session timer.
  - `restSeconds` on sets is always 0 → always the hardcoded 90 s default (`WorkoutLoggingViewModel.kt:173`).
- **Fix recommendation**: Use a foreground service or WorkManager for a process-safe timer; remove `restTimer.stop()` from History VM `onCleared` (only the owning session VM should stop it).

## 7. Workout Save

- **Expected behavior**: "Complete Workout" persists endTime, computed duration, and `completed=true`; set edits persist live.
- **How to test**: Log sets, complete workout, reopen.
- **Status**: ✅ PASS (code). `completeWorkout()` (`WorkoutLoggingViewModel.kt:184-194`) writes the completed entity via `updateWorkout`; every set/reps/weight/RPE edit persists immediately through `WorkoutDao.updateWorkoutSet`. Room insertion with `REPLACE` and FKs (CASCADE) are defined.
- **Confidence**: HIGH (fresh-install schema). MEDIUM for upgrade path — see item 20.
- **Possible causes if it fails**: FK mismatch or migration schema drift on upgrade.
- **Fix recommendation**: None for fresh installs.

## 8. Workout Resume

- **Expected behavior**: An incomplete workout shows a "Resume Workout" button in History; tapping it reloads the session with all logged sets.
- **How to test**: Start session (once item 5 is fixed), add sets, leave, open History.
- **Status**: 🟡 DEVICE TEST REQUIRED (depends on item 5 being fixed first). Loading logic present (`loadOrStartWorkout`, `getWorkoutWithDetails`), but **unreachable without a way to create an incomplete workout**.
- **Confidence**: HIGH (code), MEDIUM (end-to-end untestable until item 5 fixed)
- **Possible causes if it fails**: Only `getLatestIncompleteWorkout()` LIMIT 1 is surfaced — multiple incomplete workouts can't be resumed individually.
- **Fix recommendation**: Fix item 5; consider listing all incomplete workouts.

## 9. Workout History

- **Expected behavior**: Completed workouts listed with date, duration, sets, reps, volume; empty state when none.
- **How to test**: Complete a workout, open History.
- **Status**: ✅ PASS (code) for list + stats via `getCompletedWorkoutsWithStats()`; 🟡 DEVICE TEST REQUIRED for rendering. Minor cosmetic double `.padding(padding)` on the list (`WorkoutHistoryScreen.kt:196`) — harmless.
- **Confidence**: HIGH
- **Possible causes if it fails**: No completed workouts exist (because item 5 blocks creation).
- **Fix recommendation**: Fix item 5; optionally remove duplicate padding.

## 10. Search

- **Expected behavior**: History search filters by workout notes; Library search filters by exercise name.
- **How to test**: Type in each search field.
- **Status**: ✅ PASS (code) for **name** search in Library (`ExerciseViewModel.kt:35`, case-insensitive `contains`). ❌ FAIL (code) for **History**: search matches **notes only** (`WorkoutHistoryViewModel.kt:83`), while the DAO already offers `searchWorkouts()` that also matches **exercise names** (`WorkoutDao.kt:238-251`) — that query is never used.
- **Confidence**: HIGH
- **Possible causes if it fails**: History search misses workouts whose exercise names match the query.
- **Fix recommendation**: Use `WorkoutDao.searchWorkouts()` in the History flow, or extend the in-memory filter to exercise names.

## 11. Filter

- **Expected behavior**: History filter tabs ALL / THIS_WEEK / THIS_MONTH / CUSTOM narrow the list.
- **How to test**: Switch tabs.
- **Status**: ✅ PASS (code) for ALL / THIS_WEEK / THIS_MONTH (`WorkoutHistoryViewModel.kt:87-104`). ❌ FAIL (code) for **CUSTOM**: the tab exists but **no date-picker UI is wired**; `onCustomDateRangeChange` is never called, so CUSTOM behaves as ALL.
- **Confidence**: HIGH
- **Possible causes if it fails**: Filtering works in-memory over the stream; CUSTOM has no bound start/end dates.
- **Fix recommendation**: Add a date-range picker to the CUSTOM tab, or hide the CUSTOM tab until implemented.

## 12. Sort

- **Expected behavior**: Sort dropdown reorders History by NEWEST / OLDEST / VOLUME (asc/desc) / DURATION (asc/desc).
- **How to test**: Open sort menu, choose each option.
- **Status**: ✅ PASS (code). All six options implemented in `WorkoutHistoryViewModel.kt:106-114`. (DAO also exposes per-sort queries, unused — cosmetic redundancy.)
- **Confidence**: HIGH
- **Possible causes if it fails**: None identified.
- **Fix recommendation**: None.

## 13. Delete Workout

- **Expected behavior**: Trash icon on Workout Detail → confirmation dialog → confirm deletes workout and its exercises/sets (FK CASCADE).
- **How to test**: Open a completed workout, tap delete, confirm.
- **Status**: ❌ FAIL (code) was confirmed — **BLOCKER**. `WorkoutHistoryDetailScreen.kt:209` was `viewModel.deleteTarget.collectAsState() != null` — comparing the non-null `State` wrapper made the dialog render permanently. **FIXED in `3e23844`**: condition reads `.value != null`; confirm also navigates back.
- **Confidence**: HIGH
- **Possible causes if it fails**: `State` object vs its `.value` compared to null.
- **Fix recommendation**: Use the already-collected `showDeleteConfirmation` state (or `deleteTarget.collectAsState().value != null`) in the condition.

## 14. Analytics Dashboard

- **Expected behavior**: Overview stats (total workouts, sets, reps, volume, time, avg duration, avg volume, weekly workouts, weekly trend) shown on a dashboard.
- **How to test**: Navigate to the Progress dashboard.
- **Status**: ❌ FAIL (code) was confirmed — **BLOCKER**. `ProgressDashboardScreen` had no NavHost route and no call sites. **FIXED in `3e23844`**: `Routes.PROGRESS` added; reachable via the Insights icon on the Exercise List.
- **Confidence**: HIGH
- **Possible causes if it fails**: No navigation entry point added to `GymCoachNavHost.kt`.
- **Fix recommendation**: Add a `Routes.PROGRESS` route and a UI entry (e.g., bottom navigation or History top-bar chart icon).

## 15. Progress Charts

- **Expected behavior**: Volume History line chart, weekly/monthly summaries, top exercises, personal records render.
- **How to test**: Open the Progress dashboard (see item 14).
- **Status**: ❌ FAIL (code) was confirmed — same root cause as item 14 (screen unreachable). **FIXED in `3e23844`** via the Progress route. The `VolumeLineChart` Canvas implementation itself is sound (empty-state guards present).
- **Confidence**: HIGH
- **Possible causes if it fails**: Unreachable; also empty dataset until workouts are created.
- **Fix recommendation**: Fix item 14; seed data and create workouts for chart content.

## 16. Camera Permission

- **Expected behavior**: First use of camera prompts for CAMERA permission; granting shows preview.
- **How to test**: Open the form-analysis camera screen on a device.
- **Status**: ❌ FAIL (code) was confirmed — **BLOCKER**. `CameraPreviewScreen` (permission flow + CameraX preview) had zero call sites. **FIXED in `3e23844`**: `Routes.CAMERA` added; reachable via the camera icon on the Exercise List. Note: `android.hardware.camera.any required=true` still blocks install on camera-less devices.
- **Confidence**: HIGH
- **Possible causes if it fails**: Permission flow is dead code; the in-manifest requirement also affects installability.
- **Fix recommendation**: Wire the camera screen into navigation (item 2) and set `camera.any required="false"` for broader device support.

## 17. Form Analysis

- **Expected behavior**: Real-time rep counting and form feedback using pose landmarks for 9 exercises.
- **How to test**: Open the camera analysis screen, perform an exercise.
- **Status**: ⚠️ PARTIAL. `FormAnalyzer` (rep counting, phase detection, feedback) is a complete pure-Kotlin state machine with **zero references** — not fed by any camera/MediaPipe pipeline (MediaPipe `tasks-vision 0.10.9` **is** a declared dependency but unused). **Camera preview route added in `3e23844`; the ML pipeline wiring remains open (deferred to 1.1).** `CameraOverlay` and `ExerciseVideoPlayer` are likewise unreferenced.
- **Confidence**: HIGH
- **Possible causes if it fails**: The AI Coach was never connected to a camera frame source.
- **Fix recommendation**: Add MediaPipe Tasks Vision, feed camera frames (ImageAnalysis) into `FormAnalyzer`, map exercise → `ExerciseType`, and surface `AnalysisResult` in the camera screen. This is the largest remaining feature gap.

## 18. Screen Rotation

- **Expected behavior**: Rotating preserves the current screen, entered text, navigation stack, and rest-timer state.
- **How to test**: Rotate on each screen mid-session.
- **Status**: 🟡 DEVICE TEST REQUIRED. Code signals: no `android:configChanges`, so the activity is recreated (correct Compose behavior); ViewModels survive via Hilt; UI fields use `rememberSaveable` (search text, tabs, set weight/reps/RPE, timer-running flag). Timer countdown survives recreation because the coroutine lives in the session VM's scope, which survives recreation.
- **Confidence**: MEDIUM (static), requires device verification
- **Possible causes if it fails**: Layout overflow in landscape on small screens (no landscape-specific layouts); `ExerciseVideoPlayer`'s infinite `while(true)` polling loop is a leaked coroutine but does not affect rotation state.
- **Fix recommendation**: Verify on device; add `Canvas`/chart and table layouts that tolerate portrait constraints in landscape.

## 19. Background / Foreground Lifecycle

- **Expected behavior**: App resumes correctly; a running rest timer continues or recovers after backgrounding/kill.
- **How to test**: Home button mid-session → relaunch; also force-stop → relaunch.
- **Status**: ❌ FAIL (code) for timer survival. Data (logged sets) is safe — every edit persists to Room, so **workout data survives** and can be resumed. But the **rest timer is in-memory only** (item 6): background kill / force-stop resets it; there is no foreground service, WorkManager, or saved deadline to restore from.
- **Confidence**: HIGH
- **Possible causes if it fails**: Process death loses the coroutine ticker; `rememberSaveable` only stores the boolean, not the deadline.
- **Fix recommendation**: Persist a `timerEndTime` in the workout/session record (or WorkManager/FGS) and restore the countdown on relaunch.

## 20. Database Persistence After Restart

- **Expected behavior**: Workouts, exercises, and sets persist across app restarts.
- **How to test**: Complete a workout, force-stop, relaunch, check History.
- **Status**: ✅ PASS (code) for fresh installs — `gymcoach.db` (Room) with 4 tables, FKs (CASCADE), and `MIGRATION_1_2` registered. Exercise seed is missing (item 3), so the library remains empty across restarts.
- **Confidence**: HIGH (fresh install), MEDIUM (upgrade v1→v2)
- **Possible causes if it fails**:
  - `MIGRATION_1_2` (`GymCoachDatabase.kt:29-35`) creates the workout tables **without foreign keys or indices**, and the v1 schema is not defined anywhere in code. If any device ever installed a true v1 DB, the upgrade may fail Room schema validation.
  - No `fallbackToDestructiveMigration` → an unexpected schema mismatch crashes on open.
- **Fix recommendation**: For the release, keep version 2 with the migration but verify on a clean install; consider adding `fallbackToDestructiveMigration()` for dev safety, and add the FK/index DDL to `MIGRATION_1_2`.

---

# Version 1.0 Release Readiness Report

**Audited at commit `6415477`; all five blockers verified at code level and FIXED in commit `3e23844`.**
**Post-fix CI run `30731302439`: Build ✅ Unit Tests ✅ Android Lint ✅.**

## Overall Score: 7.0 / 10 — APPROVED AS INTERNAL RELEASE CANDIDATE (not public release)

| Dimension | Score | Notes |
|---|---|---|
| Build & CI | 9/10 | Green pipeline, APK artifacts produced. Only versionName is `0.1.0`, not `1.0`. |
| Launch/Navigation | 8/10 | All screens now reachable after wiring Progress + Camera routes. |
| Core Logging Flow (items 5–13) | 8/10 | New Workout entry wired; delete dialog fixed. |
| Analytics/Progress (14–15) | 8/10 | Dashboard + charts now reachable. |
| AI Coach / Camera (16–17) | 3/10 | Camera preview + permission reachable; FormAnalyzer ML pipeline not connected. |
| Lifecycle & Persistence (18–20) | 6/10 | Data persists correctly; rest timer not process-safe. |
| Testing | 3/10 | 1 unit test file (7 tests) only; no UI/instrumented tests; device validation not executed in this environment. |

## Blockers — all five CONFIRMED and FIXED in `3e23844`

1. **No way to start a new workout** — History "＋" was a no-op; now wired to `Routes.workoutSession()` (null id).
2. **Exercise library empty** — no seed; now seeded (9 exercises) via `RoomDatabase.Callback` in `GymCoachDatabase.kt`.
3. **Delete-confirmation dialog always shown** on Workout Detail — fixed by comparing `.value != null`.
4. **Progress Dashboard & Progress Charts unreachable** — `Routes.PROGRESS` added.
5. **Camera reachability** — `Routes.CAMERA` added; **FormAnalyzer MediaPipe wiring remains open (1.1)**.

## Remaining runtime issues (post-fix, non-blocking)

- Rest timer is in-memory only — lost on process death / background kill; shared singleton timer can be stopped by History VM `onCleared`.
- FormAnalyzer not connected to live camera frames (MediaPipe pipeline not wired).
- History search matches notes only (DAO's name+notes `searchWorkouts` unused).
- CUSTOM filter tab has no date picker.
- `ExerciseVideoPlayer` defined but unused (no exercise videos in Detail screen).
- `uses-feature camera.any required=true` blocks install on camera-less devices.
- versionName `0.1.0` (should be `1.0.0` for the release).
- `MIGRATION_1_2` lacks FK/index DDL; no `fallbackToDestructiveMigration`.
- No UI/instrumented tests; only 7 repository unit tests.

## Not executed in this environment (device-required)

Items 1 (cold start), 4 (detail render), 5, 8, 9, 18 (rotation) — all interaction-dependent. **Recommend a physical-device pass on a Pixel/API 30+ device before release.**

## Recommendation

Do **not** release publicly as 1.0 yet. The five release blockers are fixed and CI is green; the core logging product is complete. Hold for: physical-device smoke test, `versionName` bump to `1.0.0`, release signing, and a decision on cutting the AI Coach pipeline (largest remaining item) to 1.1.

See `docs/RELEASE_READINESS_REPORT.md` for the detailed post-verification report.
