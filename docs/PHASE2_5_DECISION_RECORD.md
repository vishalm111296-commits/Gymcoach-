## FIX NOW vs FIX BEFORE MERGE vs POSTPONE

### CRITICAL — FIX BEFORE MERGE

| # | Issue | Why Critical |
|---|-------|--------------|
| C1 | **Duplicate ProgressViewModel** — branch cannot compile | Two `ProgressUiState` + `ProgressViewModel` definitions in same package. This is a hard compilation blocker. |
| C2 | **ProgramGenerator filters out all exercises** — zero-exercise programs | The core promise (plan → train) is dead at generation time. User generates a program, gets a blank workout. |
| C3 | **Room schema crashes on upgrade** — 6 tables diverge from MIGRATION_4_5 | Existing users on v4→5 will get `IllegalStateException` on every app launch. Data is intact but unreachable. Since `fallbackToDestructiveMigration` was removed (audit P0), this is an infinite crash. |

### HIGH — FIX BEFORE MERGE

| # | Issue | Why High |
|---|-------|----------|
| H1 | **sorted-index vs raw-index mismatch** in workout set editing | Passes display index instead of `set.id` → silently edits/deletes wrong row in restored workouts |
| H2 | **VolumeCalculator "weekly" volume is not weekly** | `avgWeekly` computed then discarded; `weeklySets` derived from total sets across entire input — 4 weeks inflates to 4× classification |
| H3 | **PRDetector + SetWithContext are dead code** | No production caller; 18 "new unit tests" celebrate unreachable path; PRs dated `Instant.now()` instead of workout date |
| H4 | **Seeder re-seed duplicates library** | No TRUNCATE before inserts; `REPLACE` on exercises by auto-generated ID creates duplicate rows on second seed |
| H5 | **Frequency=2 onboarding produces 4-day program** | `when(frequency)` has no 2-branch → falls to `generateUpperLower` (4 days) while `daysPerWeek=2` stored |
| H6 | **Room DB unencrypted** with health data | Plaintext SQLite accessible via USB debugging/root; body measurements, weights, photos (if added) exposed |
| H7 | **No signing config** for release builds | Cannot ship to Play Store; release APKs unsigned |

### MEDIUM — FIX POST-MERGE OR INCREMENTALLY

| # | Issue | Mitigation |
|---|-------|-----------|
| M1 | **"View Program" goes to the exercise library** | Home nav `program` → `Routes.EXERCISE_LIST`; no screen renders saved program days |
| M2 | **Start Workout ignores today's program day** | `TodayWorkoutCard.onStartClick` navigates to blank `workout_session` — no pre-seeded exercises |
| M3 | **Enhanced progress experience unreachable** | New progress stack (charts, heatmap, PRs, muscle volume) referenced by nothing that renders |
| M4 | **Muscle vocabulary fragmentation** | Dashboard uses `uppercase()`, generator uses Title Case, VolumeCalculator expects different tokens |
| M5 | **Sunday week-boundary bug** | `weekStartMillis()` without pinned first-day-of-week; Sunday rolls to *next* Monday |
| M6 | **ProfileViewModel injects DAO directly** | Bypasses repository layer; `saveMeasurement` swallows failures into `error` state not displayed |
| M7 | **MIGRATION_6_7 FTS4 DDL suspect** | `USING FTS4(name TEXT NOT NULL, ...)` — may fail at migration time for v6 users; untested |
| M8 | **Measurement dialog save silently no-ops** | Invalid weight `toDoubleOrNull() ?: return@Button` — button appears dead, no error message |

### LOW — ADDRESS IF TIME PERMITS

| # | Issue | Notes |
|---|-------|-------|
| L1 | **False-completion accounting** | `prCount` fetched once via `runCatching` — stale until process death |
| L2 | **Home `prCount` stale** | Fetched in `init` — not refreshed on process recreation |
| L3 | **`pickToday` rotates by day-of-year** | Ignores weekday/rest-day alignment — defensible product choice, undocumented |
| L4 | **`updateNotes` last-write-wins race** | Notes typed during finish can be overwritten by `completeWorkout` |
| L5 | **`loadOrStartWorkout` flow leak** | Collects DB flows forever inside `launch`; timer job restarts on every emission |
| L6 | **VolumeCalculatorTest oddity** | Asserts `bicepsVolume == 1` with comment `// 0` — test passes for wrong reason adjacency |
| L7 | **CameraPreviewScreen no navigation** | Potential dead-end screen; exit path unverified |