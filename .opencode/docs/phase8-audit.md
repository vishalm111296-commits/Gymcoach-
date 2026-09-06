# Phase 8 Audit — Analytics / Progress Data Correctness (GymCoach)

- Date: 2026-09-06 (re-run; supersedes the previous partial write of the same file)
- Branch: `phase5-recovery-verified` (working tree incl. uncommitted M-series fixes, HEAD b5fa19c)
- Mode: READ-ONLY forensic audit. No source file was modified.
- Scope: Analytics/Progress surface = `ProgressDashboardScreen` + `ProgressViewModel` + `AnalyticsRepository(Impl)` + `WorkoutDao` analytics queries + `core/` engines they consume + same-class calorie estimate in Workout History detail. `presentation/analytics/` does not exist — verified (`ls` returns empty).
- Severity scale: **P0** = fabricated data presented as real · **P1** = wrong metric/misleading label/broken feature · **P2** = semantics/consistency/efficiency · **P3/INFO** = cosmetic/polish.

## Executive Summary

| # | Finding | Severity | Verdict |
|---|---------|:---:|:---:|
| 1 | "Est. Calories" = `totalVolume * 0.05` fabricated heuristic, on 2 screens | **P0** | FAIL |
| 2 | Weekly-summary bucketing key retains time-of-day → same week SPLIT into multiple rows | **P1** | FAIL |
| 3 | `getWorkoutCounts()` Today/Week/Month boundaries use now-millis / Monday@now-time / 1st@now-time → systematic undercount | **P1** | FAIL |
| 4 | "Avg Volume" = per-SET average (`AVG(weight*reps)`), labeled as workout average | **P1** | FAIL (label) |
| 5 | "Weekly Workouts" stat = number of active weeks, not workouts/week | **P1** | FAIL (label) |
| 6 | Strength selector is a hardcoded 8-item list; 7/8 names don't exist in the seeded DB → empty charts | **P1** | FAIL |
| 7 | Legacy totals (sets/reps/volume/avg-volume/volume-history) include NON-completed sets; window stats filter them → inconsistent | P2 | FAIL |
| 8 | "Exercises" stat = workout_exercise occurrence count, not distinct exercises | P2 | FAIL (semantics) |
| 9 | Monthly volumes bucketed by UTC month while the rest of the app uses local dates → boundary workouts mis-attributed | P2 | PARTIAL |
| 10 | `getTopMuscleGroups` returns per-EXERCISE rows mislabeled as "muscle group distribution" | P2 | FAIL (mislabel) |
| 11 | Dead analytics surface: 5 unused components, unused state fields + 1 unused repo method recomputed every load | P2 | PARTIAL |
| 12 | PRDetector is dead code; its "Bodyweight e1RM" = `reps * 1.5` unlabeled heuristic | P2 | PARTIAL |
| 13 | Charts: index-based x-axis (ignores date gaps); flat-value series drawn at bottom edge; single-point series = blank canvas | P2/P3 | PARTIAL |
| 14 | "0.0%%" double-percent literal on flat Weekly Trend | P2 | FAIL (format) |
| 15 | Volume displayed with unit "kg" though dimension is kg·reps | P2/INFO | PARTIAL |
| 16 | Body-measurement null-vs-zero (0.0 = not measured) — consistent end-to-end | — | **PASS** |
| 17 | All chart series are DAO-backed; no dummy/hardcoded trends, honest empty states | — | **PASS** |
| 18 | Volume formula `reps*weight`, PR best-load, heatmap dates, measurement trends — traced, match queries | — | **PASS** (caveats) |
| 19 | Unit system: app is metric-only; "kg"/"cm" labels consistent app-wide | — | PASS |
| 20 | `estimatedCalories` column exists but is always 0 (assets carry no calorie data) and is never displayed | — | INFO |

---

## A. Fake / placeholder data shown as real

### A1. "Est. Calories" = `totalVolume * 0.05` — P0, FAIL
- `ProgressDashboardScreen.kt:866-869` — `StatCard(label = "Est. Calories", value = "%.0f".format(totalVolume * 0.05))`.
- `WorkoutHistoryDetailScreen.kt:439` — `SummaryStatItem(label = "Est. Calories", value = "%.0f".format(totalVolume * 0.05))`.
- The `0.05` multiplier is an arbitrary, physiology-free heuristic with no calibration, no MET/session context, no user-body-mass input — yet it is rendered as a real metric ("Est. Calories"). `totalVolume` is kg·reps (typically 5,000–200,000); ×0.05 produces a plausible-looking but fabricated number.
- Note: the earlier baseline commit `b5fa19c` removed one fake-calorie instance elsewhere, but these two remain — this audit's global grep (`0\.05|calorie|kcal` across `app/src/main/kotlin`) finds exactly these two.
- Fix direction: delete both stats, or compute from real per-exercise `estimatedCalories` metadata (see INFO-1) with a documented formula.

### A2. Hardcoded Strength-Progression selector — P1, FAIL
- `ProgressDashboardScreen.kt:540-593` — `ExerciseSelector` offers a hardcoded `listOf("Bench Press","Squat","Deadlift","Overhead Press","Barbell Row","Pull-Up","Dumbbell Curl","Tricep Pushdown")` (:579-581).
- The seeded library (assets/exercises/*.json) is dumbbell/bodyweight-centric. Verified name inventory:
  - "Bench Press" → only "Dumbbell Bench Press" / "Dumbbell Incline/Decline/Close-Grip Bench Press"
  - "Squat" → only "Dumbbell Goblet/Split/Sumo/Bulgarian Squat…"  ·  "Deadlift" → only "Dumbbell Romanian Deadlift"
  - "Overhead Press" → "Standing Dumbbell Overhead Press"  ·  "Barbell Row" → no barbell rows at all
  - "Dumbbell Curl" → seed name is "Dumbbell Bicep Curl"  ·  "Tricep Pushdown" → not in the library (cable)
  - Only "Pull-Up" exists verbatim (`bw_pull_up`).
- `strengthPoints` is keyed by exact exercise name (`prByExercise[entry.exercise.name]`, `ProgressViewModel.kt:159-178`) → **7 of 8 offered selections render the "Start logging workouts…" placeholder even when the user has equivalent history** (:304-313).
- Fix direction: derive the selector from the user's actual logged exercise names (e.g., `bestByExerciseDate.keys` or a DISTINCT-name DAO query).

### A3. PRDetector "Bodyweight e1RM" — P2, PARTIAL
- `PRDetector.kt:64-70` — `bodyweightE1RM = bodyweightReps.toDouble() * 1.5 // Simple bodyweight strength proxy` is emitted into the same PR stream as real computed values, with the same `String.format("%.1f", …) + "kg"` presentation (:69). The `1.5` multiplier is unlabeled magic.
- Mitigation: **the whole engine is dead code** (`grep detectPRs/PRDetector` → no callers; `PersonalRecordDao` → no insert call anywhere; `grep personalRecordDao()\.` → no callers). Nothing on screen today shows this value. P2, dormant.

### A4. Nothing else fabricated — PASS
- `ProgressViewModel.load()` (ProgressViewModel.kt:126-272) drives every chart from repository/DAO calls; `AnalyticsRepositoryImpl` (AnalyticsRepositoryImpl.kt:17-139) is a thin mapping layer (no constants, no sample series); `WorkoutDao` analytics queries (WorkoutDao.kt:137-248) are real aggregates. `VolumeLineChart` (:752-803), `StrengthLineChart` (:487-538), `BodyMeasurementTrend` (:130-162) consume state lists directly. `WorkoutSessionScreen` quick presets are real rest-time options (WorkoutSessionScreen.kt:457-463). Verdict: **no dummy chart series anywhere**.

---

## B. Null-vs-zero (0.0 = "not measured") — PASS

- Write: `saveMeasurement` stores `0.0` for un-entered optionals with an explicit documented convention (ProgressViewModel.kt:108-124).
- Read: trends filter `> 0` (:184-202); latest values use `takeIf { it > 0 } → null` (:208-211).
- DAO returns newest first (`BodyMeasurementDao.kt:22` `ORDER BY recorded_at DESC`) → `measurements.firstOrNull()` = latest (ProgressViewModel.kt:204). Correct.
- UI: `BodyMeasurementTrend` renders "Not measured" for null/≤0 and a flat baseline (BodyMeasurementTrend.kt:62, 104-126) — no fabricated trend.
- Dialog: pre-fills only non-null latest values; Save disabled unless weight > 0 (MeasurementLogDialog.kt:37-40, 91-97).
- Empty history → honest "Log your first measurement to see trends" (ProgressDashboardScreen.kt:190).
- **Verdict: PASS, consistent end-to-end (M8 fix intact).**

---

## C. Chart data integrity — each column traced to its DAO query

| Chart/Stat | Formula on screen | Source query | Status |
|---|---|---|---|
| Volume history line | `SUM(reps*weight)` per workout row | `getAllWorkoutVolumes` `GROUP BY w.date` (WorkoutDao.kt:158-167) | ✅ formula; ⚠️ per-workout rows; includes non-completed sets (C-caveat 2) |
| Weekly summary | per-Monday-start bucket sums | `getWeeklySummary` (AnalyticsRepositoryImpl.kt:27-47) | ❌ **week-split bug** (see C1) |
| Monthly summary | `SUM(volume)` per `strftime('%Y-%m', datetime(date/1000,'unixepoch'))` | `getMonthlyVolumes` (WorkoutDao.kt:220-229) | ✅ formula; ⚠️ UTC bucketing (see C2) |
| Strength chart | per-day best weight per exercise, 12-wk window | ViewModel loop (ProgressViewModel.kt:148-178) | ✅ real; ⚠️ exact-name matching; weight-only |
| Muscle Volume bars | set counts (completed only) per `muscleGroup` **category id** | ViewModel loop (:153-160); targets = constants 10/20 (ProgressViewModel.kt:28-29) | ⚠️ aggregates sets, not volume; groups by category, not muscle |
| Recent PRs | best weight+reps per exercise, 12-wk window | ViewModel loop (:158-165) | ✅ real, window-scoped; weight-only (no rep-PR) |
| Totals workouts/sets/reps/volume/time | direct aggregates | WorkoutDao.kt:169-218 | ✅ traceable; ⚠️ totals include non-completed sets (C3) |
| Longest/shortest workout | duration-ordered with stats | WorkoutDao.kt:184-206 | ✅ |
| Today/Week/Month counts | `COUNT(date >= X)` | WorkoutDao.kt:172-179 via `getWorkoutCounts` (AnalyticsRepositoryImpl.kt:122-135) | ❌ boundary timestamps (C4) |
| Avg Volume | `AVG(weight*reps)` **per set** | WorkoutDao.kt:244-245 | ❌ label "Avg Volume" ≠ per-set avg (C5) |
| Avg duration | `AVG(duration)` | WorkoutDao.kt:247-248 | ✅ |
| "Exercises" | `COUNT(*)` workout_exercise rows | WorkoutDao.kt:181-182 | ❌ occurrences ≠ distinct (C6) |
| Muscle-split legacy | `SUM(reps)` per exercise name | `getTopMuscleGroups` (WorkoutDao.kt:231-242) | ❌ mislabeled "muscle groups" (C7); also unrendered |

### C1. Weekly summary splits weeks — P1, FAIL ⚠️ (corrects the previous report's PASS)
`AnalyticsRepositoryImpl.getWeeklySummary` (AnalyticsRepositoryImpl.kt:31-44):
- `calendar.time = Date(dv.date)` → then subtracts days-to-Monday **keeping the workout's own time-of-day** (`calendar.add(DAY_OF_MONTH, -daysToMonday)`), then uses the result as the map key.
- `Date` equality is millisecond equality, so a Wednesday 07:30 workout and a Saturday 17:00 workout in the same ISO week produce **two different keys** (`Mon 07:30` vs `Mon 17:00`) → the week's volume is **split across multiple rows**, each rendered as its own "Week of <Mon>" (ProgressDashboardScreen.kt:371-377). `calculateWeeklyTrend` (ProgressViewModel.kt:277-283) then compares the last two *split* entries → distorted trend.
- Fix: normalize keys to `startOfDay` of Monday (e.g., zero hour/min/sec/ms after computing week start).

### C2. Monthly buckets are UTC; the app is local — P2, PARTIAL
- `getMonthlyVolumes` groups by `strftime('%Y-%m', datetime(w.date / 1000, 'unixepoch'))` which is **UTC** (WorkoutDao.kt:226). A workout at local 2026-09-01 00:30 in a UTC+X zone becomes 2026-08-31 22:30 UTC → bucketed into **August**.
- Every other date computation in the app (adherence, heatmap, trends) uses `ZoneId.systemDefault()`. Labels show "MMM yyyy" (ProgressDashboardScreen.kt:810-813) → user sees the wrong month for boundary workouts.
- Fix: bucket by local-time month (`datetime(date/1000,'unixepoch','localtime')` or compute in Kotlin).

### C3. Totals include non-completed sets — P2, FAIL
- `WorkoutSetEntity` has a `completed: Boolean` (WorkoutSetEntity.kt:28), yet the legacy aggregate queries **never filter `ws.completed`**: `getTotalSetsCount` (WorkoutDao.kt:211-212), `getTotalRepsCount` (:214-215), `getTotalVolumeSum` (:217-218), `getAllWorkoutVolumes` (:158-167), `getAverageWorkoutVolume` (:244-245), `getAllPersonalRecords` (:146-156).
- In contrast, the window-based stats DO filter: `doneSets = entry.sets.filter { it.completed }` (ProgressViewModel.kt:157; also WorkoutHistoryDetailScreen.kt:401-405).
- Result: the dashboard's Sets/Reps/Volume totals and the volume history include every logged set of a completed workout (abandoned/failed sets counted), while Muscle Volume bars and PRs exclude them — **same data, two accounting rules**, inflated totals.

### C4. Today/Week/Month boundary timestamps — P1, FAIL
`getWorkoutCounts` (AnalyticsRepositoryImpl.kt:122-135):
- `today = now.timeInMillis` → `date >= now` → **excludes workouts completed earlier today**. Should be local midnight.
- `week = now.set(DAY_OF_WEEK, MONDAY)` **keeps the current time-of-day** → `date >= Mon@now-time` → excludes Mon 00:00→now. Should be Monday 00:00.
- `month = now.set(DAY_OF_MONTH, 1)` **keeps the current time-of-day** → `date >= 1st@now-time` → excludes 1st 00:00→now. Should be 1st 00:00.
- Cross-screen semantics also disagree: `WorkoutHistoryViewModel` TODAY correctly zeroes to midnight (WorkoutHistoryViewModel.kt:86-93) but THIS_WEEK/THIS_MONTH are **rolling** `now-7d` / `now-30d` (WorkoutHistoryViewModel.kt:95-101) while Progress uses calendar-Monday / calendar-month → same labels, different meanings (verified).
- Fix: compute midnight boundaries in the local zone (`atStartOfDay`), and align semantics across screens.

### C5. "Avg Volume" is a per-set average — P1, FAIL (label)
- `getAverageWorkoutVolume` = `AVG(weight * reps)` over the joined set rows (WorkoutDao.kt:244-245) → mean **per-set** volume. The dashboard labels it "Avg Volume" (ProgressDashboardScreen.kt:254-258) with unit "kg", reading as average per workout.
- Fix: `SUM(weight*reps)/COUNT(DISTINCT workout_id)` or relabel honestly. Also render as kg·reps (see D3).

### C6. "Exercises" = occurrences — P2, FAIL (semantics)
- `getTotalExercisesCount` = `SELECT COUNT(*) FROM workout_exercises WHERE workoutId IN (SELECT id FROM workouts WHERE status='COMPLETED')` (WorkoutDao.kt:181-182) — a muscle done in 5 sessions counts 5. Displayed as "Exercises" (ProgressDashboardScreen.kt:846). Fix: `COUNT(DISTINCT exerciseId)` or relabel "Exercise Sessions".

### C7. "Muscle group distribution" is per-exercise — P2, FAIL (mislabel)
- `getTopMuscleGroups` selects `e.name` (the **exercise name**) with `SUM(ws.reps)`, `GROUP BY we.exerciseId` (WorkoutDao.kt:231-242) → returns top exercises, not muscle groups; mapped to `MuscleGroupStats(name, totalReps)` (AnalyticsRepositoryImpl.kt:82-86).
- Mitigation: `state.muscleGroupDistribution` is **never rendered** (grep → no UI consumer) — dormant, but dangerous if wired up later. Fix: either join `exercises.muscleGroup` / the `muscles` taxonomy tables, or delete.

### C8. Charts: x-axis and flat/single-point rendering — P2/P3, PARTIAL
- Both line charts place points at **even index spacing** (`stepX = width/(n-1)`, ProgressDashboardScreen.kt:518, :783) — ignores real date gaps between sparse sessions → visual time distortion.
- Flat series artifact: `range = (maxVal - minVal).coerceAtLeast(1.0)` (:505, :770) → when all values are equal, `(value-min)/range = 0` → line drawn on the **bottom edge** of the canvas, visually implying zero. `BodyMeasurementTrend` does this correctly (`coerceAtLeast(1e-6)` + midpoint line, BodyMeasurementTrend.kt:135-143).
- Single-point series: `if (data.size < 2) return@Canvas` draws **nothing** while the section shows the chart card — a user with one logged session sees a blank frame, not a hint. (P3)

---

## D. Formatting

- **D1 — "0.0%%" literal (P2).** ProgressDashboardScreen.kt:280 uses the string literal `"\u2022 0.0%%"` (not passed through `format`), so the flat trend renders `• 0.0%%` (two percent signs) while up/down branches show one `%` (:278-279). Fix: `"• 0.0%"` or reuse `"%.1f%%".format(0.0)`.
- **D2 — PR achievement string (PASS).** `"%,.0f kg × %d"` (ProgressViewModel.kt:235) — real data, correct rounding (whole kg), thousands separators. Kg-consistent with a metric-only app.
- **D3 — Volume unit label (P2/INFO).** Volume is dimensionally `kg × reps` (kg·reps); displayed as `"%.1f kg"` for Avg Volume and totals (ProgressDashboardScreen.kt:256, :854) and in the history summary (WorkoutHistoryDetailScreen.kt:438). The "kg" label is dimensionally wrong. Fix: "kg·reps" or drop the unit.
- **D4 — Date labels (INFO).** Charts have **no axis labels/values at all** (unreadable scales). `weekLabel` "Week of MMM dd" (:805-808), `monthLabel` "MMM yyyy" (:810-813) — fine; but `PRCard` renders raw `LocalDate.toString()` ISO-8601 ("2026-09-06", :692) — inconsistent style with the MMM dd labels.
- **D5 — Adherence truncation (P2/INFO).** `(adherence*100).toInt()` (ProgressDashboardScreen.kt:478) truncates rather than rounds (66.7% → 66%). Cosmetic.
- **D6 — Units are metric-only (PASS).** No `SettingsRepository`/unit preference exists anywhere (global grep — none); "kg"/"cm" labels are consistent app-wide. No lb-conversion risk.

---

## E. Dead analytics surface (P2, PARTIAL)

- **Unused components** (no references outside their own file): `CalendarHeatmap.kt`, `MuscleVolumeChart.kt`, `StrengthProgressChart.kt`, `PRListCard.kt`, `TrainingOverviewCard.kt` (all in `presentation/progress/components/`). The live dashboard uses inline `StrengthLineChart`/`VolumeLineChart`/`MuscleVolumeBar`/`PRCard` instead.
- **Unused state recomputed on every load:** `muscleGroupDistribution` and `personalRecords` (ProgressViewModel.kt:38-39, :242-243) — never read by any screen (grep verified).
- **Wasted computation:** `workoutDays` (ProgressViewModel.kt:147-154) is built per load but never rendered (its only consumer, `CalendarHeatmap`, is unused).
- **Unused repository method:** `AnalyticsRepository.getPersonalRecord(exerciseId)` (AnalyticsRepository.kt:8; AnalyticsRepositoryImpl.kt:23-25) — no callers.
- **Dead PR pipeline:** `PRDetector` + `PersonalRecordDao` (`personal_records` table exists in schema v4+) — no callers/inserts anywhere.
- Not fake — but each load wastes 2 aggregate queries + a per-workout map build, and the dead code risks future misuse (see C7, A3).

---

## F. What is CORRECT (PASS list)

1. Body-measurement null-vs-zero convention applied consistently (write, read, UI, dialog, DAO ordering).
2. All rendered charts derive from real completed-workout data; no dummy series, no injected trends, honest empty states.
3. Volume arithmetic `reps*weight` is consistent across `WorkoutDao`, `WorkoutRepositoryImpl` mapper (WorkoutRepositoryImpl.kt:269-284) and `PRDetector.calculateVolume` (PRDetector.kt:81-83).
4. `VolumeCalculator` (working tree, fixed version): weighted credits PRIMARY=1.0/SECONDARY=0.5/STABILIZER=0.25, filters `completed && setType==0`, ISO `WeekFields` bucketing, evidence bands 10/14/18/22 (VolumeCalculator.kt:101, :128, :189-193, :170-178) — PASS.
5. Adherence: Monday-based `LocalDate` comparison, capped at 1.0 (ProgressViewModel.kt:132, :139-140) — PASS.
6. Heatmap grid (did it get used): 12 weeks starting Monday, membership-based cell coloring, no future-day coloring (CalendarHeatmap.kt:84-99) — logic correct, currently dead.
7. `getWeeklySummary` calendar reuse is safe per-iteration (AnalyticsRepositoryImpl.kt:33 resets `calendar.time` each loop) — but see C1 for the keying bug.
8. `getMonthlyVolumes` millis→seconds conversion is correct — but see C2 for UTC bucketing.
9. `formatDuration` (WorkoutHistoryDetailScreen.kt:678-682) and longest/shortest workout cards — correct.
10. `ProgressionEngine` recommendations are rule-based coaching heuristics with human-readable `reason`s (ProgressionEngine.kt:60-121); `calculateEstimated1RM` is the standard Epley formula `w*(1+reps/30)` capped at 12 reps (PRDetector.kt:76-79). Hardcoded `confidence` values (0.5–0.9) are pseudo-precision (INFO).

---

## G. Recommended follow-ups (severity-ordered)

- **P0:** Remove/replace the `volume * 0.05` "Est. Calories" on `ProgressDashboardScreen.kt:867` and `WorkoutHistoryDetailScreen.kt:439`. Prefer real per-exercise `estimatedCalories` metadata or delete the stat.
- **P1:** (a) Fix `getWeeklySummary` Monday keys to `startOfDay` (C1). (b) Fix `getWorkoutCounts` boundaries to local midnight / Monday-00:00 / 1st-00:00 and align Today/Week/Month semantics with WorkoutHistoryViewModel (C4). (c) Replace the hardcoded `ExerciseSelector` with the user's real exercise-name history (A2). (d) Relabel "Avg Volume" (C5) and "Weekly Workouts" (ProgressViewModel.kt:256 → weeks-active; ProgressDashboardScreen.kt:272-276).
- **P2:** Filter `ws.completed` in all legacy aggregate queries (C3); `COUNT(DISTINCT exerciseId)` for "Exercises" (C6); local-time monthly bucketing (C2); fix `getTopMuscleGroups` (C7) or delete; delete dead components/state/method + PRDetector/PersonalRecordDao wiring decision (E); fix "0.0%%" (D1); time-proportional x-axes, flat-series midpoint handling, single-point hint (C8).
- **P3/INFO:** volume unit label kg·reps (D3); PRCard date format (D4); adherence rounding (D5); confidence pseudo-precision (F10); `estimatedCalories` column semantics (always 0, INFO-1: assets carry no calorie data — `ExerciseSeeder` never populates it, ExerciseEntity default 0).

---

*Evidence verified from: `ProgressDashboardScreen.kt`, `ProgressViewModel.kt`, `ProgressModels.kt`, `AnalyticsRepositoryImpl.kt`, `AnalyticsRepository.kt`, `WorkoutDao.kt`, `WorkoutEntity.kt`, `WorkoutSetEntity.kt`, `WorkoutHistoryViewModel.kt`, `WorkoutHistoryDetailScreen.kt`, `WorkoutSessionScreen.kt`, `BodyMeasurementDao.kt`, `BodyMeasurementTrend.kt`, `MeasurementLogDialog.kt`, `CalendarHeatmap.kt`, `MuscleVolumeChart.kt`, `StrengthProgressChart.kt`, `PRListCard.kt`, `TrainingOverviewCard.kt`, `VolumeCalculator.kt`, `PRDetector.kt`, `ProgressionEngine.kt`, `PersonalRecordDao.kt`, `ExerciseRepositoryImpl.kt`, `ExerciseSeeder.kt`, `VtaperAttribution.kt`, plus the seed JSON corpus (14 exercise files, no `calories` key; `muscleGroup` ← category ids: arms/back/chest/core/full_body/legs/shoulders).*