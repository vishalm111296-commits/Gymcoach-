# GymCoach — Full Project Audit Report

**Date:** 2026-08-08
**Branch:** repair/restore-build (HEAD `9f05948`)
**Method:** Independent source verification by the audit lead, plus five parallel deep-dive subagents (exercise library; history/progress; profile/measurements/settings; UI/UX & accessibility; timer/camera/ML/DI). Every CRITICAL/HIGH claim below was re-verified against the actual source by the lead; SQL claims were additionally confirmed empirically with `sqlite3`.

---

## 1. Executive summary

The codebase compiles and the existing 24 unit tests pass, but it is **not in a shippable state**. The audit found **10 critical defects**, three of which break core product flows on a fresh install:

1. **There is no way to start a workout.** The History screen's "Create new workout" button is unwired (`onNewWorkout` defaults to `{}`, the NavHost never passes it), and "Resume Workout" only appears when an incomplete workout already exists. Nothing creates one from the UI, so on a fresh install the entire workout flow is unreachable.
2. **The exercise list's search box and filter button are hidden beneath the exercise list.** The screen emits two sibling `fillMaxSize` nodes; the list draws on top of the search column and steals all touch input. Search and filters are functionally dead.
3. **4 of 12 registered routes have no caller**: `MEASUREMENTS`, `PROFILE`, `PR`, `WORKOUT_SUMMARY`. The Profile, Measurements, and PR screens are unreachable in the running app (the profile/measurement defects below are landmines that detonate the moment anyone wires the routes in).
4. **The history detail screen is always blank** — `getWorkoutWithDetails(...).stateIn(WhileSubscribed(5000), null).value` returns `null` forever because `.value` is read without a collector.
5. **Monthly volume analytics collapse to one bucket** — `GROUP BY strftime('%Y-%m', w.date)` on a millis `Long` returns `NULL` for every row (empirically verified), so all history sums into a single "month".
6. **Personal records are computed from incomplete workouts, uncompleted sets, and warmup sets** — `getAllPersonalRecords()` has no `w.completed = 1` / `ws.completed` / `setType` filters.
7. **"Today" workout count is always 0** — `date >= now` can never be true for a past-dated workout.
8. **Weekly/monthly stats boundaries keep the time-of-day**, so one week/month splits into multiple buckets (and Sunday rolls the week forward into the future).
9. **A brand-new workout's session timer never starts** — `startNewWorkoutInternal()` skips `startWorkoutTimer()`.
10. **Lint fails the build** — 2 errors (dead `PremiumRestTimerManager` calls `Vibrator.vibrate` without the VIBRATE permission).

Downstream of these, the post-workout summary screen is dead code (completion navigates back, never to the summary), settings toggles all persist but nothing consumes them, and the camera screen has no ML integration.

The build environment also has two blockers on this host (aarch64): debug builds require an `android.aapt2FromMavenOverride` to the aarch64 aapt2, and **release builds cannot complete** (`stripReleaseDebugSymbols` runs the NDK's x86_64 `llvm-strip`). Release also has no signing config.

---

## 2. Build baseline (Phase 1)

Verified by running the full pipeline on 2026-08-08. Full logs in `docs/audit/logs/`.

| Check | Result | Evidence |
|---|---|---|
| `assembleDebug` | ✅ PASS | `app-debug.apk` produced (24.3 MB) |
| `testDebugUnitTest` | ✅ PASS | 24 tests, 6 suites, 0 failures |
| `lintDebug` | ❌ FAIL | **2 errors, 30 warnings** |

**Lint errors (fatal):**
- `core/timer/PremiumRestTimerManager.kt:74,77` — `MissingPermission`: `Vibrator.vibrate` requires `android.permission.VIBRATE`, which is not declared. This class is **dead code** (no references outside its own file), so the fix is to delete it (it would also fail DI — no `Vibrator` binding exists). This is the only thing keeping lint red.
- 30 warnings are dependency-update advisories (AGP 8.2.2→9.3.1, Compose BOM 2024.02→2026.06.01, Room 2.6.1→2.8.4, CameraX 1.3.1→1.6.1, etc.). Not defects.

**Environment note (build command):** on this aarch64 host, AGP's default x86_64 `aapt2` daemon cannot start. Every Gradle invocation must pass `-Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2`. The env-var form (`ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride`) is **not** honored; only the `-P` form works.

---

## 3. Room database persistence (Phase 2)

**Schema (DB version 3, `gymcoach_database`):** `users` (single row id=1), `exercises` (seeded, rich metadata incl. `category`, `tags`, `instructions`), `workouts`, `workout_exercises`, `workout_sets`, `measurement_records`. All FKs declared with `CASCADE`. `UserProfile` uses `@Insert(REPLACE)` so the single user row is correctly INSERT-then-REPLACE.

**What is correct:**
- Seed callback fires `onCreate` lazily (after `INSTANCE` is set) — safe.
- `WorkoutRepositoryImpl` flow composition (`getWorkoutWithDetails`) correctly re-queries on table invalidation.
- `WorkoutSet.toEntity()` preserves `id`, so `@Update` actually updates.
- `getLatestMeasurementForUser` LIMIT-1 Flow semantics are correct.

**Defects:**

| # | Severity | Finding | Evidence |
|---|---|---|---|
| P2.1 | **HIGH** | `fallbackToDestructiveMigration()` on a version-3 DB means any future schema bump silently wipes all workouts, measurements, profile, and re-seeds exercises. No migration strategy exists. | `GymCoachDatabase.kt:51` |
| P2.2 | MEDIUM | Divergent private mappers for the same type: `WorkoutRepositoryImpl`'s `ExerciseEntity.toDomain()` maps only 6 of 18 fields (drops instructions/tips/category/etc.). Any feature that reads exercise metadata from a `WorkoutWithDetails` gets empty strings. The exercise-list/detail screens use `ExerciseRepositoryImpl`'s full mapper, so they're unaffected today. | `WorkoutRepositoryImpl.kt:248-255` vs `ExerciseRepositoryImpl.kt:43+` |
| P2.3 | LOW | `deleteWorkout`/`removeExerciseFromWorkout`/`deleteSet` read an entity via `getXById().first()` then delete — works but needlessly Flow-shaped. | `WorkoutRepositoryImpl.kt:108,119,132` |
| P2.4 | LOW | `MeasurementTypeConverter` is annotated `@TypeConverters` on itself (a no-op) and never registered; `measurementType` is stored as TEXT and mapped manually — dead class. | `MeasurementRecordEntity.kt:20-28` |

**CURSOR_MISMATCH investigation:** No `CURSOR_MISMATCH` appears in any run log, error log, or doc in the repo. I checked every entity/DAO mapping manually: all numeric columns are consistently stored as INTEGER/REAL and read into matching Kotlin types; `WorkoutWithStats` intentionally reads a subset of `w.*` (extra cursor columns are ignored by Room). NULL aggregates (`SUM`/`AVG` over zero rows) are read via `cursor.getDouble()/getInt()`, which return `0.0`/`0` for NULL — no crash, just zeros. **There is no actual cursor mismatch today.** The only schema-related data-loss risk is P2.1 (destructive migration).

---

## 4. Navigation routes (Phase 3)

**Full map** (`ui/GymCoachNavHost.kt`, start destination `EXERCISE_LIST`):

```
EXERCISE_LIST ──► EXERCISE_DETAIL, WORKOUT_HISTORY, PROGRESS, CAMERA, SETTINGS
WORKOUT_HISTORY ─► WORKOUT_HISTORY_DETAIL, WORKOUT_SESSION (resume, with id)
(no callers) ────► MEASUREMENTS, PROFILE, PR, WORKOUT_SUMMARY   ← DEAD
```

**Defects:**

| # | Severity | Finding | Evidence |
|---|---|---|---|
| N1 | **CRITICAL** | **No way to start a workout.** Only navigation into `WORKOUT_SESSION` is History's `onResumeWorkout(workout.id)`, and the Resume button renders only when `incompleteWorkout != null` (`WorkoutHistoryScreen.kt:198`). The History "Create new workout" button (`:111`) is wired to `onNewWorkout` which **defaults to `{}` and is never passed** by the NavHost. The no-id session path (`loadOrStartWorkout(null)` → creates a workout) is unreachable. Fix is one line: `onNewWorkout = { navController.navigate(Routes.workoutSession()) }`. | `WorkoutHistoryScreen.kt:77,111`; `GymCoachNavHost.kt:69-75`; `WorkoutLoggingViewModel.kt:45-74` |
| N2 | **CRITICAL** | 4 registered destinations have no caller: `MEASUREMENTS`, `PROFILE`, `PR`, `WORKOUT_SUMMARY`. `WorkoutSummaryScreen/ViewModel`, `MeasurementScreen/ViewModel`, `ProfileScreen/ViewModel`, `PRScreen/ViewModel` are dead code. The only direct `navigate` call in the app is `ExerciseListScreen.kt:122 → SETTINGS`. | `GymCoachNavHost.kt`; grep of all `navigate(`/`Routes.` |
| N3 | **CRITICAL** | **History detail is always blank** (`stateIn(SharingStarted.WhileSubscribed(5000), null).value` never starts the upstream, so `.value` is null forever). The detail screen's Edit is also unwired (`onEditClick` defaults `{}`), so the whole detail page is dead. | `WorkoutHistoryDetailScreen.kt:84`; `GymCoachNavHost.kt:82` |
| N4 | MEDIUM | `exerciseId == -1L` / `workoutId == -1L` (bad deep link) renders a blank destination with no back affordance. | `GymCoachNavHost.kt:64-68,80-83,122-126` |
| N5 | MEDIUM | State restoration is partial: `ExerciseListScreen` restores `textFieldValue`/`tabIndex`/`showFilterSheet` via `rememberSaveable`, but the ViewModel's `searchQuery`/filters are plain `MutableStateFlow` (no `SavedStateHandle`). After process death the visible text and the applied filter disagree. | `ExerciseListScreen.kt:69-71`; `ExerciseViewModel.kt:24-27` |
| N6 | LOW | `WORKOUT_SESSION` accepts a `workoutId` of an already-completed workout with no guard; `completeWorkout()` would re-run and rewrite its duration/endTime. Not reachable today, but a landmine for any future resume flow. | `WorkoutLoggingViewModel.kt:284-295` |

---

## 5. Workout flow end-to-end (Phase 4)

Trace (as designed): Exercise list → (missing entry point, see N1) → WorkoutSession → add exercise → edit sets → complete → summary. Verified against `WorkoutLoggingViewModel`, `WorkoutSessionScreen`, `WorkoutSummaryScreen`.

**What works (once a session is reachable):** add exercise, add/edit sets (weight/reps/RPE/rest/set-type), remove set/exercise, mark-set-complete, notes, pause/resume rest timer, complete workout → `completeWorkout()` writes `endTime`, `duration = now − startTime` (seconds), `completed = true`.

**Defects:**

| # | Severity | Finding | Evidence |
|---|---|---|---|
| W1 | **CRITICAL** | Fresh-workout timer never starts. `startNewWorkoutInternal()` (path for `workoutId == null`) omits `startWorkoutTimer()`, so a new workout shows `00:00` for the whole session. Display-only — final `duration` is recomputed correctly at completion. (Downstream of N1, currently unreachable.) | `WorkoutLoggingViewModel.kt:30-52` (no `startWorkoutTimer`) vs `:103-114` |
| W2 | **CRITICAL** | **Completion never reaches the summary.** `_completed == true` shows an inline full-screen "Workout Complete!" overlay whose button is `onBackClick` (pop back to previous screen). `WORKOUT_SUMMARY`/`WorkoutSummaryScreen`/`WorkoutSummaryViewModel` are dead. | `WorkoutSessionScreen.kt:110-130`; `GymCoachNavHost.kt:118-126` |
| W3 | HIGH | Swipe-to-delete on a set fires immediately on any horizontal swipe with **no confirmation and no undo**; it permanently deletes the set from the DB. High accidental-data-loss risk mid-workout. | `WorkoutSessionScreen.kt:403-410` → `WorkoutLoggingViewModel.kt:210-219` |
| W4 | HIGH | The "Remove Exercise" ✕ button deletes the exercise **and all its sets** with no confirmation. | `WorkoutSessionScreen.kt:384-386` → `:221-228` |
| W5 | MEDIUM | Set-row text fields use positional `rememberSaveable` that never re-syncs from the DB. Deleting a set shifts every subsequent row's position, and a row can inherit the deleted row's saved text (stale/incorrect values shown — and if edited, written back). | `WorkoutSessionScreen.kt:478-481` |
| W6 | MEDIUM | Index-based VM writes: `updateSetField`/`toggleSetCompletion`/`removeSet` read `_currentWorkout.value.exercises[i].sets[j]`. Indices are consistent while the list is stable (DAO orders by `setNumber ASC`, screen sorts the same), but a delete-then-rapid-edit window can hit a stale list. | `WorkoutLoggingViewModel.kt:210-246,297-306` |
| W7 | MEDIUM | 1-second timer tick (and 6 screen-scoped `collectAsState`) recomposes the whole session screen — every set row and text field — each second. | `WorkoutSessionScreen.kt:77-83,137` |
| W8 | MEDIUM | Rest timer settings are not wired: "Default Duration", "Auto-start", "Vibration" only persist to SharedPreferences; the timer always auto-starts at a hardcoded `defaultRestSeconds = 90`. | `WorkoutLoggingViewModel.kt:31,240-245` |
| W9 | MEDIUM | Finish dialog copy says "All completed sets will be saved," implying incomplete sets are dropped — they are kept and counted by analytics. Misleading. | `WorkoutSessionScreen.kt:284` |
| W10 | MEDIUM | `WorkoutHistoryViewModel` injects the singleton `RestTimerManager` only to call `stop()` in `onCleared()` — popping History while a rest runs kills the workout's timer (latent cross-screen interference). | `WorkoutHistoryViewModel.kt:26,191-194` |
| W11 | LOW | `ExerciseVideoPlayer` is dead code (no callers, no URL data source); the 200 ms polling loop is a recomposition smell if ever wired. | `ExerciseVideoPlayer.kt` |

---

## 6. Profile / Measurements / Settings (Phase 5)

**All three screens are on dead routes (N2)** — none are reachable in the running app. Every defect below is verified in source and detonates the moment the routes are wired.

| # | Severity | Finding | Evidence |
|---|---|---|---|
| P/S1 | **CRITICAL** | First-launch profile creation is impossible: Save/Cancel icons render only `if (state.profile != null)` (top-bar actions). No profile is seeded, so a new user can type forever and never persist; `UserProfileDao` INSERT-REPLACE (id=1) is otherwise correct. | `ProfileScreen.kt:41-54`; `GymCoachDatabase.kt:53-61` (seeds exercises only) |
| P/S2 | **CRITICAL** | Add-Measurement is broken in two independent ways: (a) the dropdown `expanded = selectedType != null` with `onExpandedChange = {}` can never open, so a type can never be chosen and the Add button is permanently disabled; (b) even if enabled, the insert is **commented out** (`// viewModel.addMeasurement(record)`). | `MeasurementScreen.kt:124-152,186,193` |
| P/S3 | **CRITICAL** | Individual measurement deletion is structurally impossible: no `@Delete`/delete-by-id in `MeasurementDao`, no repo method, and the UI delete is `onDelete = {}`. | `MeasurementDao.kt` (whole); `MeasurementScreen.kt:107,256-261` |
| P/S4 | **CRITICAL** | `onSaveProfile` validation failure leaves `isSaving = true` forever (the false branch never resets it), locking both Save and Cancel. A fresh form (age=0) hits this on the very first save. | `ProfileViewModel.kt:128-151` |
| P/S5 | **CRITICAL** | `MeasurementScreen` crashes when data exists: `LazyColumn` nested inside a `Column(...verticalScroll(...))` → "measured with an infinity maximum height constraints" `IllegalStateException`. | `MeasurementScreen.kt:66-71` + `:95-98` |
| P/S6 | **HIGH** | **Settings persist but nothing consumes them.** Every toggle (`isDarkMode`, `isMetricUnits`, `plateUnit`, `defaultRestTimerSeconds`, `isSetCountEnabled`, `isAnimatedTransitions`, `isBackupEnabled`, `isDebugMode`, …) writes SharedPreferences and is never read outside the settings package. Dark mode: `Theme.kt:121` hardcodes `isSystemInDarkTheme()`. Every settings control is a user-facing no-op. | `SettingsViewModel.kt:81-132,199-208`; project-wide grep |
| P/S7 | MEDIUM | Cancel-edit doesn't revert the form (`form.profile` is a dead field; unsaved edits stay visible as if saved). | `ProfileViewModel.kt:231-236` |
| P/S8 | MEDIUM | Goal-weight validation rejects weight-loss goals (`goalWeight < weight` → error) and NaN values pass (`"NaN".toDoubleOrNull()` → NaN; `NaN <= 0` is false) and are written to Room. | `ProfileViewModel.kt:157-168,194-196` |
| P/S9 | MEDIUM | `@Update` on a hand-built record with `id = 0` silently no-ops (Room matches by PK); update/trend paths exist in the VM/use-cases but have no UI caller. | `MeasurementRecordEntity.kt:10`; `MeasurementDao.kt:15-16` |
| P/S10 | MEDIUM | Inconsistent user identity: measurements keyed by the literal `"default_user"`, profile by `id = 1L` — no join key for future relations. | `MeasurementViewModel.kt:43,57`; `ProfileViewModel.kt:205` |
| P/S11 | MEDIUM | Two "vibration" fields in settings; `setVibrationEnabled` updates only one, and the Rest-Timer "Vibration" toggle silently changes notification vibration. | `SettingsViewModel.kt:33,40,88,149-152`; `SettingsScreen.kt:137,154` |
| P/S12 | LOW | Measurement error Retry is a no-op (`onRetry = { /* viewModel.refresh() */ }`). | `MeasurementScreen.kt:79` |
| P/S13 | LOW | Dead code: `UserProfileRepository(+Impl)` (duplicate of `ProfileRepository`), `ProfileAnalyticsViewModel`, `ProfileSettingsViewModel` (a second, inconsistent prefs store), `GetMeasurementsByTypeUseCase`, `GetLatestMeasurementUseCase`, `ValidationResult`, 4 unused DAO methods, unused `isNewUser`/`vibrationEnabled`/`selectedSection` state. | grep across app |
| P/S14 | LOW | Profile "units" is stored/editable but never read; per-type units hardcoded; settings units separate and inert. | `ProfileScreen.kt:196-203`; `MeasurementType.kt:3-14` |

---

## 7. Exercise library (Phase 6)

| # | Severity | Finding | Evidence |
|---|---|---|---|
| L1 | **CRITICAL** | **Search box + Filter button are occluded by the exercise list.** The screen emits two sibling `fillMaxSize` nodes — the search `Column` then `mainContent(...)` (its own `Column` + `LazyColumn`). In the NavHost `Box`, the later sibling draws on top and wins touch. The search field and `Tune` filter button cannot be typed into or tapped. (This also makes the filter sheet below unreachable.) | `ExerciseListScreen.kt:73-106` + `:108-125` + `mainContent` at `:340-359` |
| L2 | **CRITICAL** | Filter sheet cannot be dismissed: `onDismissRequest = { /* automatically dismissed */ }` is a no-op and no close button exists; `showFilterSheet` is never set false. (Blocked from opening by L1, but broken either way.) | `ExerciseListScreen.kt:84-105,175-177` |
| L3 | HIGH | "Category" filter has no reachable UI and the DAO never filters the `category` column. The UI's "categories" are actually muscle groups; `filterCategory` would filter `muscleGroup`, not `category`. The seeded `category` column ("Powerlifting", "Bodyweight", …) has no consumer. | `ExerciseListScreen.kt:167-168,270-282,332-359`; `ExerciseViewModel.kt:42-46`; `ExerciseDao.kt:18-25` |
| L4 | HIGH | Favorites are unimplemented: `isFavorite` exists in entity/domain and is copied by mappers, but there is no toggle, no query, no icon, no filter anywhere. | `ExerciseEntity.kt:24`; grep app-wide |
| L5 | HIGH | PRs include incomplete workouts (same root as D2 — see §8). | `PRViewModel.kt:43`; `WorkoutDao.kt:92-101` |
| L6 | MEDIUM | Exercise list has no loading/empty/error state — empty search/filter results show a bare nav row. | `ExerciseListScreen.kt:340-359` |
| L7 | MEDIUM | Search/filter state desync after process death (same as N5). | — |
| L8 | MEDIUM | PR "no data" gate only checks `highestWeight > 0`; a bodyweight-only trainee (weight always 0) forever sees "No personal records yet." | `PRScreen.kt:45` |
| L9 | MEDIUM | Exercise picker (session) ignores the library's search/filters and searches client-side only; search is applied in the ViewModel, not SQL — correct today but won't scale. | `WorkoutLoggingViewModel.kt:33`; `ExerciseViewModel.kt:47-55` |
| L10 | LOW | Difficulty chip row can overflow/clip on narrow screens (`FilterRow` lacks `horizontalScroll`, unlike the equipment row). | `ExerciseListScreen.kt:211-230` |
| L11 | LOW | Dead destinations/state: dead "All" tab (`ScrollableTabRow(0)`), unused `categories`/`selectedCategory` params, blank destination when `exerciseId == -1`. | `ExerciseListScreen.kt:270-282,332-359`; `GymCoachNavHost.kt:60-68` |
| L12 | LOW | `getAllWorkoutsWithDetails` uses `.map { it!! }` — NPE if a workout vanishes between emissions (rare). | `WorkoutRepositoryImpl.kt:89` |

---

## 8. Data-correctness audit (Phase 7)

All SQL verified against source; the two strftime claims and the millis grouping were **empirically confirmed with sqlite3** on 2026-08-08.

| # | Severity | Finding | Evidence / Proof |
|---|---|---|---|
| D1 | **CRITICAL** | **Monthly volumes collapse to one bucket.** `GROUP BY strftime('%Y-%m', w.date)` on a millis `Long`: SQLite treats a bare numeric as a Julian day; realistic millis are out of range → `strftime` returns `NULL` for every row → all completed workouts group into one bucket (lifetime volume, arbitrary date label). Verified: `SELECT quote(strftime('%Y-%m',1723118400000))` → `NULL`; the correct form is `strftime('%Y-%m', w.date/1000, 'unixepoch')`. | `WorkoutDao.kt:165-175` |
| D2 | **CRITICAL** | **PRs from incomplete workouts / uncompleted / warmup sets.** `getAllPersonalRecords()` (`WHERE 1=1`, no join to `workouts`, no `w.completed`, no `ws.completed`, no `setType` filter). Typing 150 kg into a warmup set of an in-progress workout immediately sets a PR. The sibling `PRViewModel.calculatePRs` recomputes the same stats in memory over `getAllWorkouts()` with the same missing filters — two parallel, equally wrong PR implementations. | `WorkoutDao.kt:92-101`; `WorkoutDao.kt:12-13`; `PRViewModel.kt:98-115` |
| D3 | **CRITICAL** | **"Today" workout count is always 0.** `getWorkoutCounts` passes `now` (current wall clock) as `todayStart`; `WorkoutEntity.date` is set at creation (always past) and never advanced → `date >= now` never matches. Also diverges from History's "Today" tab, which correctly uses local midnight. | `AnalyticsRepositoryImpl.kt:121-134`; `WorkoutDao.kt:117-118`; `WorkoutHistoryViewModel.kt:91-99` |
| D4 | **HIGH** | **Weekly/monthly boundaries keep time-of-day.** Week start = `Mon<current-time>` (not Mon 00:00); on a Sunday `DAY_OF_WEEK = MONDAY` rolls **forward** into next week (today excluded). Month start keeps time-of-day (1st excluded before that time). Boundaries only check `>=`, so future-dated rows count too. | `AnalyticsRepositoryImpl.kt:124-127` |
| D5 | **HIGH** | **Weekly summary splits one week into multiple buckets.** `getWeeklySummary` subtracts days-to-Monday but keeps the workout's time-of-day in the key — Mon 09:00 vs Wed 17:00 produce different `Date` keys for the same calendar week. Corrupts weekly totals, `calculateWeeklyTrend`, and `calculateWorkoutFrequency`. (The unit test masks this by using midnight timestamps — see §10.) | `AnalyticsRepositoryImpl.kt:27-47` |
| D6 | HIGH | History "This Week"/"This Month" tabs are rolling 7/30-day windows, inconsistent with the dashboard's calendar semantics. | `WorkoutHistoryViewModel.kt:100-107` |
| D7 | MEDIUM | "Avg Volume" is the mean **per-set** `weight×reps`, labeled and unit-ed as workout volume (`"%.1f kg"` of kg·reps). | `WorkoutDao.kt:190` |
| D8 | MEDIUM | Total sets/reps/volume and every `getCompletedWorkoutsWithStats*` count **all** sets (uncompleted + warmup/drop/failure), inconsistent with the post-workout summary which filters `completed`. | `WorkoutDao.kt:156-163,130-151,203-306`; `WorkoutSummaryViewModel.kt:40-47` |
| D9 | MEDIUM | "Volume History" chart is per-workout volume (grouped by exact millis), indexed x-axis, labeled "weekly" — spacing is arbitrary; same-day workouts are separate points. | `AnalyticsRepositoryImpl.kt:17-21`; `WorkoutDao.kt:103-112`; `ProgressDashboardScreen.kt:631-701` |
| D10 | MEDIUM | "Weekly Workouts" stat is the **count of week-buckets**, not workouts (further inflated by D5). One workout/week for a year → "52". | `ProgressDashboardScreen.kt:166-168` |
| D11 | LOW | `getPersonalRecord(exerciseId)` returns the **latest** set's weight (date DESC), not a max — misnamed dead code. | `AnalyticsRepositoryImpl.kt:23-25` |
| D12 | LOW | `getTopMuscleGroups` groups by `exerciseId` — it's "top exercises by reps", mislabeled as muscle groups. | `WorkoutDao.kt:177-188` |
| D13 | LOW | Search: SQLite `LIKE` is case-insensitive for ASCII (fine), but `%`/`_` in user input act as wildcards; search results are a one-shot `flow{}` that never refreshes on DB change. | `WorkoutDao.kt:294-307`; `WorkoutHistoryViewModel.kt:77-80` |
| D14 | INFO | `SUM(reps)` into Kotlin `Int` (>2.1B wraps); `AVG(duration)` truncated to `Long` before `/60`. Both unreachable in practice. | `WorkoutDao.kt:159-160,193-194` |
| D15 | INFO | No streak feature exists (checklist premise refuted); all date math is local-timezone millis, so the real issues are the boundary bugs above, not UTC/local drift. | grep "streak" → nothing |

---

## 9. UI/UX & accessibility (Phase 8)

Subset of the full UI/UX subagent report (see its output for all 40+ items); the most impactful, all verified:

| # | Severity | Finding | Evidence |
|---|---|---|---|
| U1 | **CRITICAL** | Un-dismissable modal filter sheet (same as L2) — a dead-end trap if ever opened. | `ExerciseListScreen.kt:175-177` |
| U2 | **CRITICAL** | Session screen blank while `currentWorkout == null` (no spinner, no error, no retry; a load failure leaves it blank forever). | `WorkoutSessionScreen.kt:159-248` |
| U3 | **CRITICAL** | Summary screen blank while `metrics == null` (but "Finish" still visible). | `WorkoutSummaryScreen.kt:68-98` |
| U4 | HIGH | Camera permission-denied → permanent black screen with no message, retry, or visible close control. | `CameraPreviewScreen.kt:44-49,31-36`; `GymCoachNavHost.kt:106-108` |
| U5 | HIGH | Whole-row swipe-to-delete coexists with four text fields + 24 dp checkbox + 24 dp star in one row — swipe over a field deletes the set; cramped targets mis-tap. | `WorkoutSessionScreen.kt:412-595` |
| U6 | MEDIUM | 24–40 dp touch targets (checkbox, star, delete, settings rows) — below the 48 dp minimum. | `WorkoutSessionScreen.kt:573,586`; `MeasurementScreen.kt:258`; `SettingsScreen.kt:632` |
| U7 | MEDIUM | Difficulty badges are white-on-orange/green — contrast ≈2.2–2.9:1, fails WCAG AA 4.5:1. | `ExerciseItemCard.kt:115,177-184` |
| U8 | MEDIUM | Unlabeled set text fields for TalkBack (no label/placeholder); cards lack `Role.Button`; search uses placeholder-only labels; PR screen flashes "No personal records yet" during load; detail shows "Exercise not found." during load. | `WorkoutSessionScreen.kt:513-559`; `ExerciseItemCard.kt:53-64`; `PRScreen.kt:45-53`; `ExerciseDetailScreen.kt:258-260` |
| U9 | MEDIUM | Measurement add-dialog input uses `remember` (lost on rotation); measurement screen Retry is a no-op; delete-measurement icon is dead. | `MeasurementScreen.kt:42-45,79,107` |
| U10 | MEDIUM | 1 s timer tick recomposes the whole session screen (same as W7). | — |
| U11 | MEDIUM | Dead/fake controls: `AssistChip` badges with no-op onClick; exercise list has no primary action; dead "All" tab; Settings "Manual Backup"/"Rate App"/"Privacy Policy"/"Terms"/"Contact Support" are enabled no-op rows. | `ExerciseItemCard.kt:150-152`; `SettingsScreen.kt:410-472,696` |
| U12 | LOW | Top bars lack scroll elevation; exercise list cards at 24 dp inset vs 16 dp elsewhere; difficulty row can clip (L10); 200 ms video polling (W11). | — |

---

## 10. Test coverage analysis (Phase 9)

**Current suite (24 tests, 6 files, all passing):**
- `AddMeasurementUseCaseTest` (6), `GetMeasurementsForUserUseCaseTest` (2), `GetLatestMeasurementUseCaseTest` (2) — measurement use-case validation.
- `MeasurementRepositoryImplTest` (6) — mapper correctness with a mock DAO.
- `ExerciseRepositoryTest` (6) — mapper correctness with a mock DAO.
- `AnalyticsRepositoryImplTest` (2) — `getVolumeHistory` mapping and `getWeeklySummary` grouping with a mock DAO.

**Gaps (why the CRITICAL bugs shipped):**
1. **Zero tests execute real SQL.** All repository tests mock the DAO, so `getMonthlyVolumes` (D1), `getAllPersonalRecords` (D2), `getWorkoutsTodayCount` (D3), and the `getCompletedWorkoutsWithStats` family (D8) are never run — the worst data bugs are invisible to the suite.
2. **`getWeeklySummary` test masks the D5 bug**: its `millis()` helper pins every date to 00:00, so the time-of-day-in-key defect never triggers.
3. **Zero ViewModel tests** (WorkoutLogging, Profile, Settings, Measurement, History, PR, Exercise, Summary) — so the `stateIn(...).value` null-load (N3), the stuck `isSaving` (P/S4), the missing timer start (W1), and the profile save-gating (P/S1) are all untested.
4. **Zero Compose UI / instrumented tests** (`app/src/androidTest` is empty) — occlusion (L1), the LazyColumn crash (P/S5), and the un-dismissable sheet (L2) can't be caught.
5. **Zero navigation tests** — dead routes (N2) and the missing start-workout entry (N1) pass CI.

---

## 11. Release build verification (Phase 10)

| Check | Result | Evidence |
|---|---|---|
| `assembleRelease` | ❌ FAILS | `:app:stripReleaseDebugSymbols` → "problem starting process 'command '…/linux-x86_64/bin/llvm-strip''". The NDK ships only an x86_64 `llvm-strip`; the MediaPipe `.so`s need stripping. `build.gradle.kts:63-67` disables only `stripDebugDebugSymbols` (debug), so release is blocked. **Environment limitation**, not an app defect. |
| Signing | ❌ NOT CONFIGURED | No `signingConfig` in `app/build.gradle.kts` — even if release built, the APK/AAB would be unsigned. |
| Debug APK | ✅ | `app-debug.apk` (24.3 MB) produced; requires the `-Pandroid.aapt2FromMavenOverride` workaround on this host. |
| Release deps / R8 | ✅ (compile) | Dependencies resolve; proguard is blunt (`-keep class com.gymcoach.app.**`) so minify would be mostly cosmetic. |

**To release on this host:** add the same strip-disable for `stripReleaseDebugSymbols` (or install an aarch64 NDK / qemu-binfmt for x86_64), and add a signing config + `minifyEnabled` decision. This is a build-environment fix, not app logic.

---

## 12. Removed features (repair-effort context)

The 25-commit `repair/restore-build` branch removed, as intentionally-orphaned clusters: **Backup** (`core/backup/BackupManager`), **Workout templates** (`WorkoutTemplatesRepository*`), the **VShape challenge/assessment/plan** cluster, the **Goal cluster + `goals` table** (DB `dropGoalsTable` DDL), and an orphaned **SearchScreen**. These are gone from the build; the `domain/vshape` package name survives only for measurement models. This context explains several "inert" settings rows (Backup) and dead schema columns (`goals`).

---

## 13. Prioritized remediation plan

**Fix now (blocking — user-visible core flow):**
1. Wire `onNewWorkout` → `navigate(Routes.workoutSession())` in the NavHost (unblocks all workout functionality). *(one line)*
2. Fix the exercise-list layout: put search + list in one `Column` (or `Scaffold`) so the list doesn't occlude the search/filter row; wire `onDismissRequest = { showFilterSheet = false }`.
3. Fix history detail: replace `.stateIn(...).value` with `.firstOrNull()`.
4. SQL data fixes: `getMonthlyVolumes` → `strftime('%Y-%m', w.date/1000, 'unixepoch')`; `getAllPersonalRecords` → join `workouts` + `w.completed = 1` + `ws.completed = 1` + exclude warmup/drop/failure; `getWorkoutCounts` → local-midnight boundaries; `getWeeklySummary` → normalize weekStart to midnight.
5. Lint: delete `PremiumRestTimerManager` (or add `VIBRATE` permission).
6. Start the timer in `startNewWorkoutInternal` (one line: `startWorkoutTimer()`).

**Fix next (feature correctness):**
7. Route completion to the summary screen (or drop the dead summary code).
8. Confirm dialogs for set swipe-delete and remove-exercise.
9. Wire settings to consumers (theme, rest timer, units) or remove the inert toggles.
10. Fix profile/measurement criticals (save gating, `isSaving` reset, measurement insert/delete, LazyColumn nesting) when wiring their routes in.

**Pay down (non-blocking):**
11. Add integration tests against real Room + ViewModel tests (the D1/D2/D3/N3 class of bugs).
12. Divergent `ExerciseEntity` mappers; `fallbackToDestructiveMigration` → explicit migrations.
13. Accessibility: 48 dp targets, field labels, badge contrast, button roles.

---

## 14. Audit artifacts

- Build/test/lint log: `docs/audit/logs/baseline-2026-08-08-build-test-lint.log`
- Release log: `docs/audit/logs/baseline-2026-08-08-release-build.log`
- Lint text report: `app/build/intermediates/lint_intermediate_text_report/debug/lint-results-debug.txt`
- Git state: `docs/audit/logs/baseline-2026-08-08-git-state.txt`
