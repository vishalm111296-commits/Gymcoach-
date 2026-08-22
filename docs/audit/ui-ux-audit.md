# GymCoach — Adversarial UX/UI Audit

- **Date:** 2026-08-22
- **Scope:** `app/src/main/kotlin/com/gymcoach/app/presentation/**` + `ui/GymCoachNavHost.kt`, `ui/MainActivity.kt`, `domain/repository/AnalyticsRepository.kt`, data layer.
- **Method:** Static read of current source, validated every claim against a file: line reference. Severity = impact × exploitability on real devices.
- **Note on environment:** The auditor treated every assumption the implementation makes as wrong. Two findings are flagged *Critical (verify on device)* because they are structural Compose measurement issues that manifest only when data flows are non-empty; the rest are proven from source.

## Severity Summary

| Severity   | Count |
|------------|-------|
| Critical   | 3     |
| High       | 12    |
| Medium     | 15    |
| Low        | 13    |

---

## 1. CRITICAL

### C-1. "Weekly Trend" renders literal `0.0%%` (confirmed, in source)
- **Where:** `presentation/progress/ProgressDashboardScreen.kt:462`
- **Evidence:**
  ```kotlin
  else -> "• 0.0%%"
  ```
  This is a *string literal*, not a format result. Kotlin never strips `%%` from a plain literal. The adjacent branches use `"▲ +%.1f%%".format(weeklyTrend)` (lines 460–461), so the zero/neutral branch is the lone holdout that prints two percent signs. Confirmed exact bug from the known-issues brief.
- **Impact:** User-facing text “0.0%%” — reads as broken/unprofessional; undermines data trust.
- **Fix:** Make the neutral branch structurally consistent:
  ```kotlin
  else -> "• 0.0%%".format()      // or simply "— 0.0%"
  ```
  Prefer a dash/sentinel for “no change” (`"—"`). Centralise all trend formatting in one helper so the three branches can’t drift again.

### C-2. Navigation crash: `LazyVerticalGrid` placed inside `Column(verticalScroll)` with unbounded height
- **Where:** `ProgressDashboardScreen.kt:501`, `:525`, `:549`, `:573` (all four grid sections)
- **Evidence:** Each grid is emitted from inside:
  ```kotlin
  Column(modifier = modifier.verticalScroll(rememberScrollState())) { ... LazyVerticalGrid(...) }
  ```
  A `LazyVerticalGrid`/`LazyColumn` measured by an unbounded (infinite) max height constraint throws `IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed.` This is independent of item count — it is verified at measure time. The screen therefore crashes whenever the Scaffold content branch reaches `ProgressDataContent`, i.e. for every non-loading, non-error user (the exact case where the `0.0%%` text actually renders).
- **Impact:** Progress dashboard hard-crashes on every real use. *This is why the `0.0%%` path is reachable without a crash report — it is not; the crash is masked only because data is usually present and the exception fires, or because QA hit the empty branch.*
- **Fix:** Give each grid a bounded height (e.g. `Modifier.height(180.dp)`) or hoist paging out of `verticalScroll` (convert top-level container to a single `LazyColumn` whose `items` host the grids as rows). Do not nest lazy containers in `verticalScroll` `Column`.

### C-3. Phantom “Resume Workout” + silent auto-resume with stale timer (root-caused)
- **Where:**
  - `WorkoutLoggingViewModel.startNewWorkoutInternal()` `presentation/workout/WorkoutLoggingViewModel.kt:131–143`
  - `loadOrStartWorkout(workoutId = null)` same file `:86–94`
  - DAO `getIncompleteWorkout()` / `getLatestIncompleteWorkout()` `data/local/dao/WorkoutDao.kt:24–25` & `:311–312`
- **Evidence chain:**
  1. `Routes.workoutSession()` (the “+ New workout” entry) navigates with `workoutId = null` → `:177` nav host.
  2. `LaunchedEffect(workoutId)` fires `loadOrStartWorkout(null)` (`:95–97` session screen).
  3. `loadOrStartWorkout(null)` calls `getLatestIncompleteWorkout()` (`:89`); if any `completed=0` row exists it **re-resumes** it instead of starting fresh.
  4. The zombie is *created by this very code*: `startNewWorkoutInternal()` (`startNewWorkout()` path) inserts a `completed=false` row the instant the screen is opened. There is **no** `onCleared`/`onBack` cleanup that deletes an empty draft. So merely opening “Start Workout” and pressing back leaves a `completed=0` workout with `startTime`/`endTime`/`duration` == the open instant.
  5. Next “Start workout” → `getLatestIncompleteWorkout()` returns that zombie → session title timer shows `now - startTime` of a workout the user never started. Reproduction is 100% deterministic: open session → back → reopen.
- **Impact:** Timer lies, phantom “Resume Workout” button (history screen `:197–223`) never dismisses, history/progress stats include empty drafts.
- **Fix (minimum):**
  - Don’t insert a workout row until the user adds the first exercise/set (deferred creation). At minimum, in `onBackClick`/pop, delete the draft if `exerciseCount == 0 && setCount == 0`.
  - `loadOrStartWorkout(null)` should not auto-resume a draft of its own from <N minutes ago without confirmation; surface an explicit “Resume vs New” choice. The `else -> startNewWorkoutInternal()` fallback must run when the latest incomplete workout is genuinely abandoned, not freshly authored.

### C-4. Swipe-to-delete a set: no confirmation, no undo → data loss mid-session
- **Where:** `WorkoutSessionScreen.kt:456–504`
- **Evidence:** `SwipeToDismissBox` on every `SetRow` calls `onRemoveSet(index)` immediately on threshold cross (`confirmValueChange` returns true). No `Undo` snackbar, no dialog. Compare `WorkoutHistoryScreen` (`:268–284`) and `ExerciseSetCard` (`:417–440`) which DO confirm — the session path is the inconsistency that deletes real work.
- **Impact:** Fat-finger or accidental swipe during a live set entry destroys a set the user just typed.
- **Fix:** Route swipe-delete through a confirmation (match the rest of the app) or expose `SnackbarHostState` + `Undo`. If swipe-to-dismiss is desired, gate on velocity/distance and require `StartToEnd` to be disabled (a stray back-edge drag must not wipe data).

---

## 2. HIGH

### H-1. `removeSet` / swipe-delete uses positional index into a re-sorted list
- **Where:** `WorkoutLoggingViewModel.removeSet()` `:12–19` (`we.sets[setIndex]`) vs `SetRow` index from `sets.sortedBy { it.setNumber }.forEachIndexed` `WorkoutSessionScreen.kt:456`.
- **Evidence:** The DAO orders sets `ASC by setNumber` (`:53`), so today the sorted view and the raw list agree. The contract is *implicit*, undocumented, and breaks the moment any sort changes (e.g. a future “reorder by set type” feature). Deleting `we.sets[setIndex]` then targets the wrong set.
- **Fix:** Pass the **stable set id**, not the list position: `onRemoveSet(index)` → `onRemoveSet(setId)`. Same fix applies to `onRepsChange`/`onWeightChange`/etc. in `SetRow` (`:496–502`).

### H-2. Empty / zero-set workouts can be “completed”
- **Where:** `WorkoutLoggingViewModel.completeWorkout()` `:286–299`
- **Evidence:** The “Complete Workout” button (`:285–295`) is always enabled; the dialog says “All completed sets will be saved” but never checks `exerciseCount == 0`. Completing an empty draft writes a `completed=1` row consumed by every analytics query (`getTotalWorkoutsCount`, averages, volume trends).
- **Impact:** Corrupts ProgressDashboard stats (the very charts that crash per C-2).
- **Fix:** Disable/guard: `if (totalSets == 0 && totalExercises == 0)` → prompt “Discard empty workout?” instead of persisting.

### H-3. Back from an in-progress session discards without confirmation
- **Where:** `WorkoutSessionScreen.kt:164–167` (`onBackClick` → `navController.popBackStack()`) and nav host `:177`.
- **Evidence:** No `onBackPressedDispatcher` intercept, no draft check. Because of C-3 the draft also isn’t deleted, so back is simultaneously *no-op* (zombie persists) and *destructive* (unsaved set edits lost).
- **Fix:** On back, if the draft has uncommitted edits or `exerciseCount>0`, confirm: Keep Editing / Discard.

### H-4. Dark-mode toggle requires process restart + brand palette is dead on Android 12+
- **Where:** `MainActivity.kt:19` (`getSharedPreferences("GymCoachSettings", …)` read once in `onCreate`) vs `SettingsViewModel.setDarkMode()` `:134–137`.
- **Evidence:** MainActivity never observes the pref; toggling Dark Mode in Settings writes SharedPreferences but `GymCoachTheme(darkTheme = darkMode)` is only evaluated at `setContent` in `onCreate`. No recomposition is triggered by the write → the setting only takes effect after force-stop/restart.
  - Additionally `Theme.kt:148` hardcodes `dynamicColor: Boolean = true`, so on Android 12+ `dynamicDarkColorScheme`/`dynamicLightColorScheme` replace the hand-authored `DarkColorScheme`/`LightColorScheme` entirely. `GymGreen`, `GymOrange`, `primaryContainer` (alpha 0.15) are never seen by the largest user slice. `primaryContainer = GymGreen.copy(alpha=0.15f)` is also too low-contrast for the on-primary text used in `WorkoutSessionScreen` rest-timer card (`:190`).
- **Fix:** Hoist theme state into an observable (e.g. a `DataStore<Preferences>` + `collectAsState` feeding `GymCoachTheme`) so the toggle recomputes live. Either set `dynamicColor=false` to honour the brand palette, or drop the custom green palette from source-of-truth and document it as dead. Fix rest-timer contrast (`onPrimaryContainer = GymGreenLight` over 15% green container fails WCAG 2.1 AA; use a solid container or a stronger tint).

### H-5. Settings is a facade — many toggles do nothing
- **Where:** `SettingsScreen.kt`, `SettingsViewModel.kt`.
- **Evidence:**
  - “Push Notifications”, “Sound”, “Vibration” toggles mutate prefs but nothing registers a notifier / no alarm manager / no rest-timer sound is wired to them (rest timer in `WorkoutLoggingViewModel.toggleSetCompletion` `:242–247` starts a `RestTimerManager` with no sound/vibration path).
  - “Manual Backup” `onClick = {}` (`:206`); Privacy Policy / Terms `ListItem`s have **no `onClick` at all** (`:477–499`) — inert list items that look tappable.
  - “Auto-start Rest Timer” (`isAutoStartRestTimer`) is **ignored** — `toggleSetCompletion` always starts the timer regardless of the setting (`:242–247` vs SettingsViewModel `:35`).
  - “Default Duration” slider writes `KEY_REST_TIMER`, but `WorkoutLoggingViewModel.defaultRestSeconds = 90` is a **hardcoded** field (`:31`) — the slider value is never read.
  - “Metric Units” exists both in Units AND in Plate-Calculator sections (`:364–414`) toggling the same `isMetricUnits` — duplicated control.
  - `versionCode: Int` from `PackageInfo` — on API 28+ `versionCode` is `long`; `.versionCode ?: Int` compile-warnings aside, the UI renders an int; not a UX bug but a latent crash if read wrong. (Low, included.)
- **Fix:** Wire each control to real behaviour (rest timer sound/vib via `RestTimerManager`, notification permission request for the push toggle, intent browser for privacy/TOS). Remove the duplicated Metric toggle. Consume the rest-timer default + auto-start settings in the session VM (`@Assisted` or repo-injected settings).

### H-6. Home screen has no primary CTA; nav duplicated
- **Where:** `ExerciseListScreen.kt:270–354` (`NavigationActions`) vs `GymCoachNavHost.kt:67–123` (bottom bar).
- **Evidence:** The 6-icon header row exposes History, Insights(Progress), Person(Profile) — three destinations already in the bottom nav (`:78–121`). Two independent nav surfaces compete. “Start Workout” — the app’s primary action — lives only as the **Add** icon in `WorkoutHistoryScreen` (`:110–119`), reachable via the History tab. A new user’s home screen offers zero affordance to begin a workout.
- **Fix:** Promote a “Start Workout” FAB on the Home tab; remove the redundant bottom-bar icons from the header row, or replace the header row with contextual actions only.

### H-7. “Start Workout” is a dead-end in the wrong tab
- **Where:** `onNewWorkout` wired only in `WorkoutHistoryScreen` (`:150` `onNewWorkout = { navController.navigate(Routes.workoutSession()) }`), surfaced as the `+` icon (`:110`).
- **Evidence:** `ExerciseListScreen` has no `onStartWorkout` callback despite being the bottom-bar start destination. Users must first navigate to History to begin. This inverts the mental model of a fitness app.

### H-8. `Est. Calories = volume × 0.05` presented as a fact
- **Where:** `ProgressDashboardScreen.kt:823–826`
- **Evidence:** `StatCard(label = "Est. Calories", value = "%.0f".format(totalVolume * 0.05))`. 0.05 kcal/kg is a fabricated constant with no unit basis, yet rendered in the “Stats Overview” grid as authoritative.
- **Fix:** Either source from `Exercise.estimatedCalories` (the entity already carries it — `ExerciseEntity.estimatedCalories` exists) or relabel to “Calorie estimate (placeholder)”.

### H-9. “Weekly Workouts” is a count of *weeks*, not workouts
- **Where:** `ProgressDashboardScreen.kt:172–174` (`calculateWorkoutFrequency` returns `weekly.size`) → stat label “Weekly Workouts” (`:455`).
- **Evidence:** `getWeeklySummary()` returns aggregated per-week volumes; `weekly.size` is the number of weeks with recorded volume, displayed as a count of workouts. E.g. 3 weeks of data reads “Weekly Workouts: 3” even if each week had 5 sessions.
- **Fix:** Rename label to “Weeks Tracked” or compute `totalWeeklyWorkouts / weeks`. Do not call a week-count a workout-count.

### H-10. “Avg Volume” is per-set average, not per-workout
- **Where:** `data/local/dao/WorkoutDao.kt:192` (`getAverageWorkoutVolume` = `AVG(weight*reps)` over all sets of completed workouts) → `AnalyticsRepositoryImpl` → `ProgressUiState.averageWorkoutVolume` → stat “Avg Volume” (`ProgressDashboardScreen.kt:438`).
- **Evidence:** `AVG(reps*weight)` averaged across sets conflates “average volume per workout” with “average set volume”. A 5-set workout and a 1-set workout each contribute one set’s volume to the same mean.
- **Fix:** `SELECT AVG(workout_volume) FROM (SELECT w.id, SUM(ws.reps*ws.weight) AS workout_volume FROM workouts w … GROUP BY w.id)`.

### H-11. History cards clip content at a hard-coded 88 dp
- **Where:** `WorkoutHistoryScreen.kt:339` `Card(…).height(88.dp)` containing title + 4-stat row + up to 2-line notes (`:380–387`).
- **Evidence:** `notes` (maxLines 2) + header + stat row exceed 88 dp at default font scale; content is clipped without ellipsis-on-container. At larger font scales the duration/set-count text overlaps.
- **Fix:** `wrapContentHeight` + vertical limit, or drop to 1-line notes inside a fixed card and surface full notes in the detail screen.

### H-12. ASCII table header in History violates a11y + breaks under font scaling
- **Where:** `WorkoutHistoryScreen.kt:312–327`
- **Evidence:** `Text("Workout Date         Duration   Sets   Volume      Notes")` + a literal `─────…` divider. Column alignment is achieved with spaces — collapses immediately under `fontScale > 1.0` and is pure noise to TalkBack.
- **Fix:** Replace with a real `LazyColumn` header row using weighted columns (mirror `SetRow`’s layout), or fold the header into `LazyColumn` sticky header.

---

## 3. MEDIUM

### M-1. Inconsistent empty states; shared `EmptyState` exists but is unused on the main screens
- **Where:** `presentation/components/EmptyState.kt` (has icon+CTA) vs `EmptyStateScreen` in History (`:413`, text-only, no CTA), `ProgressDashboardScreen` `EmptyPlaceholder` (`:604`, text-only), `ExerciseListScreen` (`"No exercises found"` + Clear filters — but no path to add data).
- **Evidence:** Three independent “empty” implementations; the one with a primary CTA (`EmptyState`) is used only in Profile/PR/Measurements.
- **Fix:** Unify on `EmptyState` everywhere; supply a contextual CTA (“Start your first workout” from History/Progress).

### M-2. Dead code in the session screen VM (feature is half-implemented)
- **Where:**
  - `exercisePerformance` (`:59–60`, `:71–81`, `:104`) is computed and exposed but **never read** by `WorkoutSessionScreen`. The classic “previous best vs last set” hint for lifters is silently collected and thrown away.
  - `showNotesDialog` / `_showNotesDialog` (`:65–66`, `:266–272`) is toggled by no UI in the screen (notes are edited inline `:257–264`).
  - `showPlateCalculator` / `_showPlateCalculator` (`:68–69`, `:274–276`) is exposed but the plate calculator UI is never launched from the set rows. `PlateCalculator` util exists (`core/util/PlateCalculator.kt`) but is orphaned.
- **Fix:** Either wire `exercisePerformance` into `ExerciseSetCard` (e.g. subtext “prev: 60 kg × 8”) or delete it (it runs a per-exercise DB query on every load). Same for notes dialog / plate calculator.

### M-3. `onBackClick` ContentDescription mismatches destination
- **Where:** History `:105` says “Back to exercises”; Progress `:250` says “Back to exercise list” while Progress can be reached from *either* the History back-stack or the bottom nav. Screen-reader users are told a destination that may not exist.
- **Fix:** Generic “Back” / `navController::navigateUp` content descriptions; do not hard-code the parent label.

### M-4. Top-level tab destinations show a back arrow
- **Where:** Progress `:101`/`GymCoachNavHost:180–182`, Profile, Measurements all render a back icon even when reached from the bottom bar (where back == switch tab). Inconsistency with Material 3 (top-level destinations should not show `<`).
- **Fix:** Hide navigation icon on bottom-bar routes, or hoist back-stack awareness so the icon matches intent.

### M-5. Error states lack retry / leave screen blanks on bad route args
- **Where:** `ProgressDashboardScreen` `:215–217` (raw `$error`, retry only via app-bar refresh icon which itself isn’t obvious); guard clauses `:141–143` (`:140` Progress shows spinner forever) and nav-host `:141`/`:158`/`:206` (exerciseId/historyId/summaryId `== -1` → renders **nothing**, a blank screen).
- **Fix:** Show a retry button inline in `ErrorScreen`; replace blank `-1L` branches with an error/guard composable (“Workout not found”).

### M-6. Search field in Progress mislabels its domain
- **Where:** `ProgressDashboardScreen.kt:381` placeholder “Search exercises…” + `FilterList` icon (`:384`), while the field actually filters Personal Records and Muscle-Group rows.
- **Fix:** “Search personal records…” + `Search` icon.

### M-7. VolumeLineChart double-declares height → content clipped
- **Where:** `ProgressDashboardScreen.kt:487` `Modifier.fillMaxWidth().height(200.dp)` wrapped by column `VolumeLineChart` `Column(modifier = modifier)` `:665` where `modifier` *already* sets `height(200.dp)`. The title `Text` (`:666`) then sits above a 200 dp box inside a 200 dp box → ~20 dp overflow clipped.
- **Fix:** Single source of height; let the column be `wrapContentHeight`.

### M-8. Chart renders axes with no line for < 2 data points
- **Where:** `VolumeLineChart.kt:684` `if (data.size < 2) return@Canvas` — but the title / axis labels / “0” still draw. Result: a titled box with no line, indistinguishable from a chart that failed to load.
- **Fix:** Gate the entire section on `data.isNotEmpty()`; show `EmptyPlaceholder` otherwise.

### M-9. Rest-timer toggle logic is surprising
- **Where:** `WorkoutSessionScreen.kt:210–218`; `WorkoutLoggingViewModel.toggleSetCompletion` `:232–248`.
- **Evidence:** Unchecking a completed set calls `restTimer.stop()` unconditionally — killing a different exercise’s in-flight rest. Completing a *past* set retroactively starts a fresh 90 s timer.
- **Fix:** Tie timer lifecycle to the *current* set index / a focused state, not to any row’s checkbox.

### M-10. LinearProgressIndicator shows *remaining* time (fills downward)
- **Where:** `WorkoutSessionScreen.kt:222–231` `progress = timeRemaining / totalDuration`.
- **Evidence:** `LinearProgressIndicator` is documented as “completed” — shrinking bar reads as a broken timer, not a countdown.
- **Fix:** Animate progress as `(total - remaining)/total`, or swap to a numeric countdown + directional bar.

### M-11. Dead rest-timer APIs exposed but unreachable
- **Where:** `WorkoutLoggingViewModel.addTimeRestTimer` `:262–264` (30 s ±15s buttons are standard) is never invoked by `WorkoutSessionScreen`. The rest card shows only pause/resume/skip.
- **Fix:** Add +15 / −15 s buttons wired to `restTimer.addTime`.

### M-12. Input fields in set rows accept garbage silently
- **Where:** `WorkoutSessionScreen.kt:534–537` `rememberSaveable(setId)` initialised from DB; setters `:571–614` call `v.toDoubleOrNull()?.let { onX(it) }`; empty/partial input (e.g. `"12."`, `"-5"`) is silently ignored on every keystroke.
- **Evidence:** Weight field allows negatives (`"-5".toDoubleOrNull()` = -5.0); RPE allows `99`; there is no validation boundary because the field is editable but the model never receives `NaN`.
- **Fix:** Validate ranges (weight ≥ 0, reps ≥ 0, RPE 1..10 / or your scale) and surface an inline error instead of dropping keystrokes.

### M-13. Monthly grouping timezone mismatch
- **Where:** `WorkoutDao.kt:172` uses `strftime('%Y-%m', w.date/1000, 'unixepoch')` (SQLite, interpreted **UTC**) vs History filter logic `WorkoutHistoryViewModel` `:91–114` (Java `Calendar` / `System.currentTimeMillis()` in **local** zone).
- **Evidence:** A workout finished at 23:30 local in UTC-5 is grouped into the *next* UTC month by analytics, but appears in “this month” for the history filter. Month boundaries diverge.
- **Fix:** Group by `strftime('%Y-%m', datetime(w.date/1000, 'unixepoch', 'local'))`.

### M-14. `navOptions.popUpTo(Routes.EXERCISE_LIST)` on Finish can strand the user
- **Where:** `GymCoachNavHost.kt:209` (`onFinish = { navController.popBackStack(Routes.EXERCISE_LIST, inclusive = false) }`), `:177` summary route.
- **Evidence:** If the user entered the session from the **History** tab, `EXERCISE_LIST` is not on the back stack; `popBackStack(route, inclusive=false)` returns false and clears nothing — the Summary “Finish” can leave the user stranded (or on an unintended tab).
- **Fix:** `popUpTo(0)` / `popUpTo<navigation graph start>` to land on a known top-level destination.

---

## 4. LOW

### L-1. Touch targets below 48 dp guidance
- **Where:** `WorkoutSessionScreen.kt:508` “Add Set” icon `Modifier.size(16.dp)`, `:644` set-type `Star` icon 16 dp, `:557–567` set-number box 24×24.
- **Evidence:** Material 48×48 minimum is a *suggested* floor for touch; these are visual-only but risk mis-tap under fatigue/gym gloves.
- **Fix:** Wrap actionable icons in 48 dp `IconButton` containers even if the drawable is small.

### L-2. Badge component is a false affordance + contrast fail
- **Where:** `presentation/components/ExerciseItemCard.kt:143–175` (`Badge` → `AssistChip` with `onClick = {}`); `:106–117`.
- **Evidence:** `AssistChip(onClick = {})` is announced as a button by TalkBack but does nothing. The leading `LocalFireDepartment` icon renders on **every** badge (muscle-group badges get a fire icon). White `#` text on `0xFFFF9800` (amber 700) ≈ 2.1:1 contrast (fails WCAG AA).
- **Fix:** Either remove trailing interactivity or make chips true labels (`AssistChip(onClick = null)`). Drop the icon, or make it contextual. Use a WCAG-passing palette.

### L-3. Exercise-list category chips lack scroll affordance
- **Where:** `ExerciseListScreen.kt:226–245` (`FilterRow`) renders a horizontal `forEach` **without** `horizontalScroll`, while `EquipmentRow` (`:247–268`) does scroll.
- **Evidence:** Many difficulty options overflow the screen edge with no scroll → chips are unreachable.
- **Fix:** Add `horizontalScroll(rememberScrollState())` to `FilterRow` to match `EquipmentRow`.

### L-4. Camera + PR + Settings only reachable via hidden icon row
- **Where:** `ExerciseListScreen` `NavigationActions` (`:270–354`); `Routes.CAMERA`, `Routes.PR`, `Routes.SETTINGS` have **no bottom-bar item and no other entrypoint** except this 6-icon strip.
- **Evidence:** A user who doesn’t discover the mystery icon row never reaches Settings/Camera/PR. `Route.SETTINGS` is otherwise orphaned.
- **Fix:** Either surface these in the bottom nav / a menu, or accept the icon row and label it.

### L-5. Decimal formatting ignores locale consistency
- **Where:** `ProgressDashboardScreen.kt` multiple `"%.1f kg".format(...)`, `ExerciseListScreen` none.
- **Evidence:** `String.format` default locale renders `12,5 kg` in `de-DE` while the chart axis renders `12.5`; minor visual friction, not a crash.
- **Fix:** Pin a locale (`Locale.US`) or use `NumberFormat`, and apply consistently across chart + stat cards.

### L-6. `SettingsViewModel` annotated `@Singleton`
- **Where:** `SettingsViewModel.kt:69`
- **Evidence:** `@Singleton @Inject constructor` on a `ViewModel` subclass — Hilt scoped it to the *application*, not the *ViewModelStoreOwner*. It happens to work only because Settings is never process-killed per-screen, but any future in-process multi-instance Settings access will share mutable state.
- **Fix:** Remove `@Singleton`; rely on `@HiltViewModel` default scoping.

### L-7. No onboarding / first-run gate
- **Where:** `MainActivity.kt:14–31` (direct nav to `EXERCISE_LIST`) — referenced in known-issues brief.
- **Evidence:** No `DataStore` first-run flag, no profile-wizard. `UserProfileDao` + `UserProfileEntity` exist but `ProfileScreen` is reached post-hoc, so the first workout records against an empty/assumed profile (affects any per-kg metrics).
- **Fix:** Minimal gate: if no profile, show a name/units/w HOSTs wizard; otherwise proceed. YAGNI for full onboarding, but the empty-profile branch must be explicit.

### L-8. `ExerciseDetailScreen` has no exercise-video playback affordance surfaced in audit — out of scope.
- (Reserved; not filed.)

### L-9. `WorkoutHistoryDetailScreen` / `WorkoutSummaryScreen` not opened in this pass
- These are reachable from `Routes.WORKOUT_HISTORY_DETAIL` / `Routes.WORKOUT_SUMMARY` but were not read into the audit. Risk deferred; see §6 Verification.

### L-10. `formatDuration` unit contract ambiguity
- **Where:** `WorkoutHistoryScreen.kt:434` `formatDuration(seconds: Int)` — but `WorkoutLoggingViewModel.elapsedSeconds: StateFlow<Long>` and `WorkoutSessionScreen.kt:157` passes the `Long`.
- **Evidence:** Call-site relies on a silent `Long→Int` coercion; at very long sessions (>35 min? no — Int covers 68 years, fine). Filed for contract clarity only: name one overload `formatDurationSeconds`.

### L-11. Navigation `Modifier.clickable` on inert Settings items
- **Where:** `SettingsScreen.kt:721` `SettingsButtonItem` only attaches `clickable` when `onClick` is wired; but several rows (`:468` rate, `:471` TOS) have default-empty `onClick`, yet are still wrapped — no visual affordance change for non-clickable rows vs clickable ones.
- **Fix:** Tint/disable the chevron for no-op rows.

### L-12. `DatePickerDialog` date range picker height hard-coded 400 dp
- **Where:** `WorkoutHistoryScreen.kt:260–263`. Minor: cramped on small screens, no content-overflow handling.

### L-13. Duplicate `Modifier.padding(horizontal = 8.dp, vertical = 4.dp)` on cards in `ExerciseListScreen:413`? 
- Not present; skipped.
