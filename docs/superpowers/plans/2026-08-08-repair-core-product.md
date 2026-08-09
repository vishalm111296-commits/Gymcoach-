# GymCoach Core-Product Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Take the verified repair-state GymCoach (HEAD `9f05948`, debug build green, 24 unit tests green) to a fully functional, internally consistent, tested state by fixing all confirmed CRITICAL/HIGH defects from `docs/audit/AUDIT_REPORT_2026-08-08.md`, without trusting stale v1.0 docs.

**Architecture:** Layered Clean Architecture + MVVM (presentation → domain → data/Room → core). Fixes are minimal and in-place: one-line wiring in `ui/GymCoachNavHost.kt`, targeted ViewModel/Repository/DAO edits, and real-SQL + ViewModel regression tests that fail on the pre-fix code. Dead code is removed, never papered over.

**Tech Stack:** Kotlin 1.9.22, Compose BOM 2024.02, Hilt 2.50, Room 2.6.1 (KSP), coroutines. Tests: JUnit4 + MockK (existing), add Robolectric for real-Room DAO tests, `kotlinx-coroutines-test` for ViewModel tests (already declared), Compose UI tests (androidTest, run on GitHub Actions).

## Global Constraints

- Branch: `repair/restore-build`. Commit per task, only when the task's verification is green. Inspect `git diff` after each batch.
- Every Gradle invocation on this host MUST pass `-Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2`.
- Do not rewrite the app, do not add duplicate repositories/ViewModels/screens, do not invent features.
- Reuse existing implementations when valid (e.g. `WorkoutSummaryScreen`/`WorkoutSummaryViewModel` are valid — wire the route, don't rebuild).
- No secrets in source or memory. Release signing reads keystore paths/passwords from env / `~/.gradle` only.
- Product rule for PRs (D2): only sets with `w.completed = 1 AND ws.completed = 1 AND ws.setType = 0 (NORMAL)`.
- Date rule: all "start of day/week/month" boundaries are **local midnight**; deterministic timestamps in tests must exercise time-of-day (never mask bugs by pinning everything to 00:00).
- Settings: wire every visible toggle to a real consumer or remove it. No inert controls.
- Physical-device claims are NOT made; instrumented tests run via GitHub Actions, reported honestly.

## File Structure (map)

**Modified (app/src/main):**
- `ui/GymCoachNavHost.kt` — N1(done) + W2 summary nav + nav entries for PROFILE/MEASUREMENTS/PR
- `presentation/workout/WorkoutLoggingViewModel.kt` — W1 timer start + injectable clock
- `presentation/workout/WorkoutSessionScreen.kt` — W2 completion nav, W3/W4 confirm dialogs, W5 keyed saveable
- `presentation/history/WorkoutHistoryDetailViewModel.kt` — N3 `.first()`
- `presentation/list/ExerciseListScreen.kt` — L1 nesting, L2 dismissible sheet
- `data/local/dao/WorkoutDao.kt` — D1 monthly SQL, D2 PR SQL
- `data/repository/AnalyticsRepositoryImpl.kt` — D3/D4 boundaries, D5 week key
- `presentation/pr/PRViewModel.kt` — PR set/type filters
- `core/timer/PremiumRestTimerManager.kt` — DELETED (dead, lint blocker)
- `presentation/profile/*`, `presentation/measurement/*`, `presentation/settings/*` — repair per audit §6, §P/S
- `ui/theme/Theme.kt` usage via `MainActivity` — settings dark mode
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — test deps, release strip, signing

**Added (app/src/test, app/src/androidTest):**
- Robolectric real-Room DAO tests (monthly grouping, PR filtering, today count, boundaries)
- ViewModel tests (timer/clock, completion, history detail load, exercise search/filter)
- Compose instrumented tests (search/filter interaction, destructive actions) — CI-run

**Docs:** this plan, `docs/audit/AUDIT_REPORT_2026-08-08.md` (untracked, left alone).

---

## Batch A — Core Workout Flow (depends: N1 already applied + verified green)

### Task 1: W1 — fresh-workout timer starts

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutLoggingViewModel.kt:126-141`
- Test: `app/src/test/kotlin/com/gymcoach/app/presentation/workout/WorkoutLoggingViewModelTest.kt`

**Interfaces:**
- Consumes: `WorkoutRepository`, `ExerciseRepository`, `RestTimerManager`, injectable `now: () -> Instant = Instant::now`
- Produces: `startNewWorkout()` starts the elapsed timer; `elapsedSeconds` ticks from an injectable clock

- [ ] **Step 1:** Write failing test `startNewWorkout_startsTimer_andTicks` — fake repo returns a created `WorkoutWithDetails` (startTime = controllable clock); set Main dispatcher via `Dispatchers.setMain(StandardTestDispatcher())`; call `startNewWorkout()`; advance clock +2s; `advanceTimeBy(2000)`; assert `elapsedSeconds.value == 2L`. Also `completeWorkout_marksCompletedAndCancelsTimer`.
- [ ] **Step 2:** Run → FAIL (fresh-workout path never calls `startWorkoutTimer`; also needs `now` param).
- [ ] **Step 3:** In `WorkoutLoggingViewModel`: add constructor param `private val now: () -> Instant = Instant::now`; replace `Instant.now()` in `startWorkoutTimer` and `startNewWorkoutInternal`/`completeWorkout` with `now()`; add `startWorkoutTimer()` inside the `getWorkoutWithDetails(id).collect { ... }` block in `startNewWorkoutInternal`.
- [ ] **Step 4:** Run test → PASS. Run `:app:testDebugUnitTest`.
- [ ] **Step 5:** `assembleDebug` with `-P` override.
- [ ] **Step 6:** Commit `fix: start elapsed timer when creating a new workout (W1)`.

### Task 2: W2 — completion routes to existing Workout Summary

**Files:**
- Modify: `ui/GymCoachNavHost.kt:95-99`, `presentation/workout/WorkoutSessionScreen.kt:110-128`
- Test: extend `WorkoutLoggingViewModelTest` with `completeWorkout_marksCompleted`

**Interfaces:**
- Consumes: `WorkoutLoggingViewModel.completeWorkout()` sets `completed=true`; `Routes.workoutSummary(id)` (exists)
- Produces: `WorkoutSessionScreen(onCompleted: (Long) -> Unit)`; NavHost navigates to `Routes.workoutSummary(workout.id)`

- [ ] **Step 1:** Verify `WorkoutSummaryScreen` calls `viewModel.setWorkoutId(workoutId)` in a `LaunchedEffect(workoutId)`; if missing, add it. (U3 guard — summary must load metrics when reached.)
- [ ] **Step 2:** In `WorkoutSessionScreen`, replace the inline "Workout Complete!" overlay: add `onCompleted: (Long) -> Unit = {}` param; `LaunchedEffect(completed, currentWorkout)` → when completed and workout id known, call `onCompleted(id)` (fire once). Keep a minimal "View Summary" fallback button calling `onCompleted` too.
- [ ] **Step 3:** In `GymCoachNavHost.kt`, pass `onCompleted = { navController.navigate(Routes.workoutSummary(it)) }`.
- [ ] **Step 4:** Run `:app:testDebugUnitTest` + `assembleDebug` (with `-P`).
- [ ] **Step 5:** Commit `fix: route completed workouts to summary screen (W2)`.

### Task 3: N3 — history detail loads

**Files:**
- Modify: `presentation/history/WorkoutHistoryDetailViewModel.kt:84`
- Test: `app/src/test/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `WorkoutRepository.getWorkoutWithDetails(id): Flow<WorkoutWithDetails?>`
- Produces: `loadWorkout(id)` populates `uiState.workout` from the first emission

- [ ] **Step 1:** Failing test `loadWorkout_populatesWorkout` — fake repo returns a flow emitting `WorkoutWithDetails`; `loadWorkout(1)`; `advanceUntilIdle`; assert `uiState.value.workout != null`.
- [ ] **Step 2:** Run → FAIL (`.value` without collector = null forever).
- [ ] **Step 3:** Replace `...stateIn(viewModelScope, WhileSubscribed(5000), null).value` with `workoutRepository.getWorkoutWithDetails(workoutId).first()` (import `kotlinx.coroutines.flow.first`).
- [ ] **Step 4:** Run test → PASS; `testDebugUnitTest` + `assembleDebug`.
- [ ] **Step 5:** Commit `fix: history detail loads via first() (N3)`.

---

## Batch B — Exercise Library

### Task 4: L1 — search/filter no longer occluded

**Files:**
- Modify: `presentation/list/ExerciseListScreen.kt:73-125,332-359`

- [ ] **Step 1:** Nest `mainContent(...)` inside the top-level `Column` after `SearchField`; give the inner `mainContent` `Column` `Modifier.weight(1f).fillMaxWidth()` and keep `LazyColumn(fillMaxSize())`. Move the `if (showFilterSheet) { FilterBottomSheet(...) }` block inside the same `Column` (after `mainContent`).
- [ ] **Step 2:** `assembleDebug` (with `-P`). (Behavior is layout-only; covered by instrumented test in Task 18.)
- [ ] **Step 3:** Commit `fix: exercise list no longer occludes search and filter (L1)`.

### Task 5: L2 — filter sheet dismissible

**Files:**
- Modify: `presentation/list/ExerciseListScreen.kt:84-105,162-177`

- [ ] **Step 1:** Add `onDismiss: () -> Unit` param to `FilterBottomSheet`; replace `onDismissRequest = { /* automatically dismissed */ }` with `onDismissRequest = onDismiss`; pass `onDismiss = { showFilterSheet = false }` at the call site. Verify sheet already closes on back/scrim via `ModalBottomSheet` default.
- [ ] **Step 2:** `assembleDebug`; run existing unit tests.
- [ ] **Step 3:** Commit `fix: filter sheet is dismissible (L2)`.

---

## Batch C — Analytics Data Correctness (real-SQL tests added in Batch H)

### Task 6: D1 — monthly volume grouping

**Files:**
- Modify: `data/local/dao/WorkoutDao.kt:165-175`
- Test: `app/src/test/kotlin/com/gymcoach/app/data/local/dao/WorkoutDaoTest.kt` (Task 18)

- [ ] **Step 1:** Change `getMonthlyVolumes` SQL: `SELECT (strftime('%s', w.date/1000, 'unixepoch', 'start of month') * 1000) as date, SUM(ws.reps * ws.weight) as volume ... GROUP BY (strftime('%s', w.date/1000, 'unixepoch', 'start of month') * 1000) ORDER BY date ASC`. `DateVolume.date` stays `Long` (month-start millis).
- [ ] **Step 2:** Verify mapper in `AnalyticsRepositoryImpl.getMonthlyVolumes()` (`Date(it.date)`) still valid.
- [ ] **Step 3:** `assembleDebug`.
- [ ] **Step 4:** Commit `fix: monthly volume grouping over epoch millis (D1)`.

### Task 7: D2 — PRs only from completed workouts / completed normal sets

**Files:**
- Modify: `data/local/dao/WorkoutDao.kt:92-101`, `presentation/pr/PRViewModel.kt:71-116`

- [ ] **Step 1:** DAO `getAllPersonalRecords`: add `INNER JOIN workouts w ON w.id = we.workoutId` and `WHERE w.completed = 1 AND ws.completed = 1 AND ws.setType = 0`.
- [ ] **Step 2:** PRViewModel `calculatePRs`: at the top of the workout loop, skip `if (!workout.completed) return@forEach`; in the set loop skip `if (!workoutSet.completed || workoutSet.setType != 0) return@forEach`. (Keeps the 8 PR categories; applies the same product rule.)
- [ ] **Step 3:** `assembleDebug`; run existing tests.
- [ ] **Step 4:** Commit `fix: PRs exclude incomplete workouts and non-working sets (D2)`.

### Task 8: D3/D4 — today/week/month boundaries at local midnight

**Files:**
- Modify: `data/repository/AnalyticsRepositoryImpl.kt:121-134`

- [ ] **Step 1:** Rewrite `getWorkoutCounts`:
  ```kotlin
  val todayStart = Calendar.getInstance().apply { set(HOUR_OF_DAY,0); set(MINUTE,0); set(SECOND,0); set(MILLISECOND,0) }.timeInMillis
  val weekStart = Calendar.getInstance().apply {
      set(HOUR_OF_DAY,0); set(MINUTE,0); set(SECOND,0); set(MILLISECOND,0)
      add(Calendar.DAY_OF_YEAR, -((get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7))
  }.timeInMillis
  val monthStart = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_MONTH, 1); set(HOUR_OF_DAY,0); set(MINUTE,0); set(SECOND,0); set(MILLISECOND,0)
  }.timeInMillis
  ```
- [ ] **Step 2:** `assembleDebug`; run existing `AnalyticsRepositoryImplTest` (still passes — mock DAO).
- [ ] **Step 3:** Commit `fix: workout-count boundaries at local midnight (D3/D4)`.

### Task 9: D5 — weekly summary key normalized to midnight

**Files:**
- Modify: `data/repository/AnalyticsRepositoryImpl.kt:27-47`

- [ ] **Step 1:** In `getWeeklySummary`, before computing `daysToMonday`, zero the time-of-day on `calendar` (HOUR/MINUTE/SECOND/MILLISECOND), then subtract `daysToMonday`. This makes `Mon 09:00` and `Wed 17:00` land on the same Monday-midnight key.
- [ ] **Step 2:** `assembleDebug`.
- [ ] **Step 3:** Commit `fix: weekly summary keyed to Monday midnight (D5)`.

---

## Batch D — Lint / Dead Code

### Task 10: remove dead PremiumRestTimerManager

**Files:**
- Delete: `app/src/main/kotlin/com/gymcoach/app/core/timer/PremiumRestTimerManager.kt`

- [ ] **Step 1:** Confirm no references (grep — verified earlier: only in own file).
- [ ] **Step 2:** `rm` the file; `assembleDebug`; `./gradlew lintDebug -Pandroid.aapt2FromMavenOverride=...` → expect 0 errors (30 dependency warnings remain).
- [ ] **Step 3:** Commit `fix: remove dead PremiumRestTimerManager lint blocker`.

---

## Batch E — Dead Navigation / Existing Modules

### Task 11: PROFILE reachable + functional (P/S1, P/S4, P/S7)

**Files:**
- Modify: `presentation/profile/ProfileViewModel.kt`, `presentation/profile/ProfileScreen.kt`, `ui/GymCoachNavHost.kt`, `presentation/settings/SettingsScreen.kt` (add Profile row)

- [ ] **Step 1:** ProfileViewModel `saveProfile`: reset `isSaving=false` on validation failure (P/S4); seed an empty `profile` (non-null) so Save/Cancel render on first launch (P/S1); make Cancel-edit revert the form (P/S7).
- [ ] **Step 2:** Add `PROFILE` navigation: a "Profile" row in Settings → `navController.navigate(Routes.PROFILE)`.
- [ ] **Step 3:** `assembleDebug`; add/extend ProfileViewModel unit test for the `isSaving` reset.
- [ ] **Step 4:** Commit `fix: profile screen functional and reachable (P/S1/P/S4/P/S7, N2)`.

### Task 12: MEASUREMENTS reachable + functional (P/S2, P/S3, P/S5)

**Files:**
- Modify: `presentation/measurement/screens/MeasurementScreen.kt`, `presentation/measurement/screens/MeasurementViewModel.kt`, `data/local/dao/MeasurementDao.kt` (+ repo/VM delete path), `ui/GymCoachNavHost.kt`, Settings row

- [ ] **Step 1:** Fix dropdown (P/S2): `expanded = selectedType != null` + `onExpandedChange` open/close; un-comment `viewModel.addMeasurement(record)`.
- [ ] **Step 2:** Add `@Delete`/`delete-by-id` to `MeasurementDao`, a repo method, and a VM `deleteMeasurement(id)`; wire the UI delete icon (P/S3).
- [ ] **Step 3:** Fix the crash (P/S5): remove the `LazyColumn`-inside-`verticalScroll` nesting (use a single `LazyColumn` for the list, keep inputs in a header item or plain column).
- [ ] **Step 4:** Add "Measurements" row in Settings → `Routes.MEASUREMENTS`.
- [ ] **Step 5:** `assembleDebug`; add MeasurementViewModel test for add/delete state.
- [ ] **Step 6:** Commit `fix: measurements screen functional and reachable (P/S2/P/S3/P/S5, N2)`.

### Task 13: PR reachable + correct

**Files:**
- Modify: `presentation/pr/PRScreen.kt:45` (L8 gate), `ui/GymCoachNavHost.kt`, Settings row

- [ ] **Step 1:** L8: change the "no data" gate from `highestWeight > 0` to a real empty check (e.g. no recorded sets / all PR values 0 and no workouts).
- [ ] **Step 2:** Add "Personal Records" row in Settings → `Routes.PR`.
- [ ] **Step 3:** `assembleDebug`.
- [ ] **Step 4:** Commit `fix: PR screen reachable and empty-state correct (L8, N2)`.

---

## Batch F — Data Safety / Workout UX

### Task 14: W3/W4 — confirm before destructive set/exercise deletion

**Files:**
- Modify: `presentation/workout/WorkoutSessionScreen.kt:384,403-410`

- [ ] **Step 1:** Add local state `pendingDeleteSet: PendingDelete?` and `pendingRemoveExercise: Long?`. On swipe-away, instead of calling `onRemoveSet`, record the pending index and show an `AlertDialog` ("Delete set X? This cannot be undone."); confirm → `onRemoveSet`, cancel → `dismissState.reset()`. Same pattern for the exercise ✕ (confirm → `onRemoveExercise`).
- [ ] **Step 2:** Keep `SwipeToDismissBox` behavior; only the delete action becomes confirm-gated.
- [ ] **Step 3:** `assembleDebug`; run existing tests.
- [ ] **Step 4:** Commit `fix: confirm destructive set and exercise deletion (W3/W4)`.

### Task 15: W5 — keyed saveable state on dynamic set rows

**Files:**
- Modify: `presentation/workout/WorkoutSessionScreen.kt:478-481`

- [ ] **Step 1:** Key each row's `rememberSaveable` by the set id: `rememberSaveable(set.id) { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }` (same for reps/rpe/rest). Deleting a set no longer shifts saved text onto neighboring rows.
- [ ] **Step 2:** `assembleDebug`.
- [ ] **Step 3:** Commit `fix: key set-row input state by set id (W5)`.

---

## Batch G — Settings

### Task 16: wire or remove every visible setting

**Files:**
- Modify: `presentation/settings/SettingsViewModel.kt`, `presentation/settings/SettingsScreen.kt`, `ui/MainActivity.kt`, `presentation/workout/WorkoutLoggingViewModel.kt` (rest seconds), rest-timer consumers

- [ ] **Step 1:** **Theme**: `MainActivity` reads `isDarkMode` pref and passes `darkTheme = isDarkMode ?: isSystemInDarkTheme()` to `GymCoachTheme` (Theme.kt already accepts `darkTheme`).
- [ ] **Step 2:** **Rest timer duration**: `WorkoutLoggingViewModel` reads `defaultRestTimerSeconds` from SharedPreferences instead of hardcoded `90`.
- [ ] **Step 3:** **Auto-start rest timer**: gate `restTimer.start(...)` in `toggleSetCompletion` on the pref (default on).
- [ ] **Step 4:** **Units (metric/imperial)**: wire into `PlateCalculator` and weight display where a single consumer exists; if a consumer is out of reach without a big change, remove the toggle rather than leave it inert. (Decision documented at execution.)
- [ ] **Step 5:** **Vibration (P/S11)**: reconcile the two vibration fields into one pref consumed by the rest-timer vibrate path (or remove both if the timer never vibrates today).
- [ ] **Step 6:** **No-consumer toggles** (backup — feature deleted in repair, debug mode, set-count, animated transitions): remove the toggle rows.
- [ ] **Step 7:** **Legal/contact rows** (Rate App / Privacy / Terms / Support): disable or replace with an informational `AlertDialog`; not silent no-ops.
- [ ] **Step 8:** `assembleDebug`; add a SettingsViewModel test asserting a toggle writes the pref the consumer reads.
- [ ] **Step 9:** Commit `fix: settings toggles wired to consumers or removed (P/S6)`.

---

## Batch H — Testing Upgrade

### Task 17: real-Room DAO tests (Robolectric)

**Files:**
- Modify: `app/build.gradle.kts` (testOptions + `robolectric`, `androidx.test:core` deps), `gradle/libs.versions.toml`
- Add: `app/src/test/kotlin/com/gymcoach/app/data/local/dao/WorkoutDaoTest.kt`

- [ ] **Step 1:** Add `testOptions { unitTests { isIncludeAndroidResources = true } }`; add `testImplementation(libs.robolectric)` + `testImplementation(libs.androidx.test.core)`; add version entries to catalog.
- [ ] **Step 2:** `WorkoutDaoTest` (with `@RunWith(AndroidJUnit4::class)`, in-memory `GymCoachDatabase`, `allowMainThreadQueries()`), `@Before` inserting: two completed workouts in different months (Jul 15 09:00, Aug 20 17:00) with volumes, one incomplete workout, one completed workout with an uncompleted set and a warmup set:
  - `getMonthlyVolumes_returnsTwoMonths` — assert 2 buckets with correct per-month sums.
  - `getAllPersonalRecords_excludesIncompleteAndNonWorkingSets` — warmup set with weight 150 must NOT appear; completed normal set 100 must.
  - `getWorkoutsTodayCount` + `getWorkoutsThisWeekCount` + `getWorkoutsThisMonthCount` — insert workouts at midnight vs 13:00; assert boundary behavior (this is the real-SQL guard for D3/D4; uses non-midnight timestamps).
- [ ] **Step 3:** Run `:app:testDebugUnitTest` → green. (If Robolectric fails on this host, report honestly and run via GitHub Actions instead — do not fake it.)
- [ ] **Step 4:** Commit `test: real-Room regression tests for analytics SQL (D1/D2/D3/D4)`.

### Task 18: ViewModel tests

**Files:**
- Add: tests under `app/src/test/.../presentation/` for WorkoutLoggingViewModel (Timer/clock, completion, no duplicate timers), WorkoutHistoryDetailViewModel (load), ExerciseViewModel (search/filter state), SettingsViewModel (toggle→pref)

- [ ] **Step 1:** `WorkoutLoggingViewModelTest`: controllable clock; assert new-workout timer ticks, completion marks `completed` + computes duration, resume path doesn't spawn a second timer (`elapsedSeconds` advances, single job).
- [ ] **Step 2:** `WorkoutHistoryDetailViewModelTest.loadWorkout_populatesWorkout` (from Task 3).
- [ ] **Step 3:** `ExerciseViewModelTest`: search text filters list; difficulty/equipment filter state; clear-filters resets.
- [ ] **Step 4:** Run `testDebugUnitTest`; commit `test: ViewModel regression tests (W1, W2, N3, L2 search/filter)`.

### Task 19: instrumented Compose + navigation tests (GitHub Actions-run)

**Files:**
- Add: `app/src/androidTest/kotlin/...` — `ExerciseListSearchFilterTest` (type in search → list updates; open/close filter sheet; clear filters), `WorkoutFlowTest` (start new workout → session shows timer; destructive-set confirm dialog), `NavigationTest` (new workout, summary, profile, measurements, PR routes reachable)
- Modify: `.github/workflows/android-build.yml` — add an instrumented test job (emulator) if not present

- [ ] **Step 1:** Write the instrumented tests (they will not run locally on this host — no emulator).
- [ ] **Step 2:** Add a CI job (or extend existing) to run `connectedDebugAndroidTest` on a hosted emulator.
- [ ] **Step 3:** Commit `test: instrumented Compose and navigation tests (L1/L2/W3/W4/routes)`; note that local execution is environment-blocked, CI is the executor.

---

## Batch I — Release / CI

### Task 20: release build + signing investigation

**Files:**
- Modify: `app/build.gradle.kts:59-67`, `gradle/libs.versions.toml` (if needed), `.github/workflows/android-build.yml`

- [ ] **Step 1:** Check whether NDK 25.1 has an aarch64 `llvm-strip` (`/opt/android-sdk/ndk/*/toolchains/llvm/prebuilt/linux-aarch64/`); if absent, extend the existing workaround to also disable `stripReleaseDebugSymbols` (same `tasks.whenTaskAdded` guard) — documented as a host limitation, not a production fix.
- [ ] **Step 2:** Add release `signingConfig` that reads `signingConfigs { create("release") { storeFile/… from env or gradle.properties } }` (never hardcode secrets; CI uses repository secrets).
- [ ] **Step 3:** Add a GitHub Actions release job: `assembleRelease` + `signingReport` on a hosted runner (no aarch64 issue there) producing a signed APK/AAB upload. Verify `main` CI is green for the current pipeline first.
- [ ] **Step 4:** Attempt `assembleRelease` locally; report the actual result (environmental limitation vs success) honestly.
- [ ] **Step 5:** Commit `build: release strip workaround, env-based signing, CI release job` (only if the change is sound; keep secrets out).

---

## Batch J — UX Polish Audit

### Task 21: targeted polish pass

**Files:** subset of screens, per findings

- [ ] **Step 1:** Verify loading/empty/error states on session, summary, history-detail, PR (U2/U3/L8). Add minimal spinner/empty text where a blank screen is currently possible.
- [ ] **Step 2:** Accessibility basics where free: 48 dp targets on session checkboxes/stars, labels on set fields (contentDescription), card roles.
- [ ] **Step 3:** Only implement polish that doesn't destabilize the repaired core; document what was intentionally skipped.
- [ ] **Step 4:** `assembleDebug` + `testDebugUnitTest`; commit `polish: loading/empty states and a11y basics` (split per screen if large).

---

## Final

### Task 22: full verification + memory update + final report

- [ ] **Step 1:** Run the VERIFICATION GATE flow checks that are runnable on this host (build, unit tests, lint, real-Room tests) and via GitHub Actions (instrumented). Walk the diff of every batch.
- [ ] **Step 2:** Re-read `/storage/3131-6635/Obsidian vault/AI-Memory/00-System/Memory-Rules.md` and `02-Projects/GymCoach.md`; update the existing note with verified state (branch/HEAD, fixed defects, build/test status, remaining risks, doc pointers). Do not create a second file; do not store secrets/logs. Update `02-Projects/_index.md` only if Memory-Rules requires.
- [ ] **Step 3:** Produce the final report (IMPLEMENTATION COMPLETE / FILES CHANGED / BUGS FIXED N1…Lint / TESTS / BUILD / UNIT TESTS / LINT / GITHUB ACTIONS / APK / RELEASE / PHYSICAL DEVICE / MEMORY / REMAINING RISKS / NEXT MILESTONE).

---

## Self-Review

- **Spec coverage:** every numbered defect (N1–N3, W1–W5, L1–L8, D1–D15, P/S1–P/S14, lint, release, settings, tests, CI) maps to a task. N1 pre-done (verified). D6–D15 are documented as lower-severity and addressed where they intersect the fixes (e.g. D8 summary already filters completed sets — verified in `WorkoutSummaryViewModel`); not all are fixed in this pass — remaining LOW/MEDIUM items are listed in the final report as REMAINING RISKS.
- **No placeholders:** every code step is concrete; test steps name the failing assertion.
- **Type consistency:** `DateVolume.date` stays `Long`; `WorkoutSessionScreen(onCompleted: (Long) -> Unit)`; `now: () -> Instant`; `setType: Int (0=NORMAL)`; `PendingDelete` local state.
