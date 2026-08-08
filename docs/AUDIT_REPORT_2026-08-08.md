# GymCoach — Full Codebase Audit (2026-08-08)

**Auditor:** Independent verification session (no trust of prior reports — every claim compiler- or grep-verified)
**Scope:** Entire `app/src/main/kotlin` working tree, `app/src/test`, Gradle config, CI workflow, committed docs
**Method:** Full Gradle build (`assembleDebug`) executed with a qemu-wrapped aapt2 on this aarch64 host; KSP/Hilt/Room phase and Kotlin phase (`compileDebugKotlin`) both run to completion; CI run history queried via GitHub API; every feature traced screen → ViewModel → Repository → DAO.

---

## 1. Executive Summary

| Verdict | Evidence |
|---|---|
| **The committed HEAD builds and passes CI.** | Last commit `320d990` → GitHub Actions run #94 = **SUCCESS**. The repo contains a working subset (exercise library/detail, workout session/history, progress dashboard). |
| **The current uncommitted working tree does NOT compile.** | `:app:assembleDebug` fails at KSP (Hilt + 4 syntax errors). `:app:compileDebugKotlin -x kspDebugKotlin` reports **768 compile errors across 45 Kotlin files**. |
| **The "reported complete" features (Profile, Measurements, Settings, PR, Search, VShape, Backup, Templates) are all in the broken working tree.** | 31 untracked + 20 modified files; the new screens/use-cases contain the bulk of the 768 errors. |
| **Committed docs claim far more than the code delivers.** | `docs/Summary.md` claims MediaPipe pose detection, exercise images, 60fps polish — none exist in code. |

**Root cause:** The last session did a large uncommitted refactor (added ~45 files, rewrote the nav host, list/history/detail screens) and stopped mid-way. It was never compiled. Nothing in the working tree builds, so none of it can be "working" in any runnable sense.

---

## 2. Build Status (compiler-verified)

```
# KSP / Hilt / Room phase  →  FAILS
e: ProfileViewModel.kt:94/273/349  [Hilt] @HiltViewModel is only supported on types that subclass ViewModel
e: GymCoachNavHost.kt:77:82         Unexpected tokens (use ';' to separate expressions on the same line)
e: VShapeProgressScreen.kt:70-71    Expecting an expression / '->'
e: ProgressDashboardScreen.kt:623   Parameter name expected
e: MeasurementDao.kt:34             Unused parameter: userId

# Kotlin phase (KSP skipped)  →  768 errors / 45 files
# Top offenders:
VShapeChallengeUseCaseImpl.kt   93   VShapeUseCaseImpl.kt        49
ProfileViewModel.kt             54   VShapeProgressScreen.kt     48
SearchScreen.kt                 53   ProgressDashboardScreen.kt  47
MeasurementScreen.kt            32   ExerciseListScreen.kt       28
SaveMeasurementUseCaseImpl.kt   31   MeasurementUseCasesImpl.kt  28
```

Representative errors (all `file:line` verified):
- `WorkoutHistoryScreen.kt:91-92` — `Unresolved reference: semantics / contentDescription` (missing imports)
- `WorkoutHistoryScreen.kt:233-255` — `Unresolved reference: rememberDateRangePickerState / DatePickerDialog / DateRangePicker` (missing date-picker imports)
- `WorkoutHistoryDetailScreen.kt:383` — `Conflicting overloads: StatItem` (public vs private in same package)
- `GymCoachNavHost.kt:52` — `ExerciseListScreen(onBackClick=…)` — screen has no such param
- `GymCoachNavHost.kt:92` — `CameraPreviewScreen(onBackClick=…)` — screen takes no params
- `ProgressDashboardScreen.kt:273` — `onSearch` referenced but undefined in scope
- `ProfileViewModel.kt` — missing `import androidx.lifecycle.ViewModel`; Hilt also needs a `@Binds` for `ProfileRepository` (RepositoryModule binds only Exercise/Workout/Analytics/UserProfile/Measurement)
- `MeasurementScreen.kt` — imports non-existent `components.MeasurementCard`; references `viewModel.trends` and `trendMap` that don't exist
- `SearchScreen.kt` — no package declaration, no imports, references undefined `SearchViewModel` / `CommunitySearchResult`
- `ExerciseListScreen.kt` — `categories` undefined in `NavigationActions`; `items()` called outside a `LazyListScope`
- `VShapeProgressScreen.kt:70` — `when (ratio) { >= 1.5f -> … }` missing subject
- Shared components regressed too: `ErrorState.kt`, `EmptyState.kt`, `ExerciseItemCard.kt`, `ExerciseVideoPlayer.kt`, `Theme.kt` all have errors in the working tree.

**Breakdown of the 45 broken files:** 20 are committed files modified by the WIP (regressed), 25 are new WIP files. The regression is not isolated to new features — it broke the previously-working list, history, detail, progress, nav, and shared components.

---

## 3. Feature Classification

Legend for the current state: **Fully Working** = implemented, wired end-to-end, verified building+running (only true of the committed subset). **Partially Working** = code exists and data-wired but incomplete/buggy at runtime. **Broken** = present but does not compile, is unreachable, or is dead code. **Missing** = no implementation or only stubs. ⚠ = currently non-compiling because of the WIP.

### List 1 — Fully Working (committed & CI-verified at `320d990`)
| Feature | Where | Notes |
|---|---|---|
| Exercise library — browse, search, filter | `presentation/list/ExerciseListScreen.kt` | Client-side search, single-select bottom-sheet filters by muscle/difficulty/equipment. |
| Exercise detail display | `presentation/detail/ExerciseDetailScreen.kt` | Name, muscle, difficulty, equipment, description, instructions, tips, mistakes, safety notes, rep range, rest, calories. |
| Workout session core | `presentation/workout/WorkoutSessionScreen.kt` + `WorkoutLoggingViewModel.kt` | Set-by-set logging (weight/reps/RPE), rest timer, swipe-to-dismiss set, set types (Normal/Warmup/Drop/Failure via star icon). |
| Workout history + history detail | `presentation/history/` | List of completed workouts with volume/duration stats, delete, per-exercise breakdown. |
| Progress dashboard data layer | `data/repository/AnalyticsRepositoryImpl.kt` + `WorkoutDao.kt` | Real SQL aggregates (volume, counts, PRs, longest/shortest), null-guarded. |
| Volume history chart | `ProgressDashboardScreen.kt` `VolumeLineChart` | Custom `Canvas` line chart with grid + dots. Only chart in the app. |

⚠ All of the above are currently **not compiling** because the WIP modified their files (see §2). They worked at the last commit; the WIP broke them.

### List 2 — Partially Working (implemented but incomplete / runtime-buggy)
| Feature | Status detail |
|---|---|
| Progress dashboard screen | Data layer real, but screen has compile errors in WIP; even fixed, it is **not reachable** (NavHost no longer passes `onProgressClick`; `ExerciseListScreen.kt:57` defaults to no-op). |
| PRs (Personal Records) | `PRScreen`/`PRViewModel` are genuinely data-wired (walks every set, 6 categories + recent PRs) but **unreachable** (no `navigate(Routes.PR)` anywhere) and the working-tree screen does not compile. |
| Rest timer | Fully implemented in the session screen, but no persistent notification if app is killed. |
| Analytics | Monthly grouping bug: `getMonthlyVolumes` uses `strftime('%Y-%m', w.date)` where `w.date` is epoch **millis** → buckets are garbage (`WorkoutDao.kt:165-175`). `getWorkoutCounts` can compute the wrong "month" window via `Calendar.set` ordering (`AnalyticsRepositoryImpl.kt:121-134`). |
| Favorite flag | Column `isFavorite` exists on entity/model/seed (all `false`), but **no toggle UI and no DAO query** — dead field. `lastViewed` likewise never written. |
| Theme | Dark + dynamic color work; **zero brand palette** (stock Material3 defaults), Light-only XML base theme, no `values-night`. |

### List 3 — Broken (does not compile / unreachable / dead code)
| Feature | Evidence |
|---|---|
| **Camera form analysis** | Route registered but call site is a compile error (`GymCoachNavHost.kt:92`); nothing navigates to it (`onCameraClick` never wired). `CameraPreviewScreen` is a **bare raw preview** — no `ImageAnalysis`, no MediaPipe, no landmark overlay, no rep counting. `CameraOverlay` never composed. |
| **ML / FormAnalyzer** | `core/ml/FormAnalyzer.kt` is a complete angle-state-machine but has **zero callers** and **no model file** (`app/src/main/assets` does not exist, no `.tflite`/`.task` anywhere). `mediapipe-tasks-vision` is a dependency but **never imported**. |
| **Exercise video player** | `ExerciseVideoPlayer.kt` is a coherent Media3 ExoPlayer implementation but **never instantiated**, has **no data source** (no `videoUrl` field in entity/schema; zero URLs in seed data), and doesn't compile (missing imports). |
| **Exercise detail favorites/video** | No favorite toggle, no video element on detail screen. |
| **Profile subsystem** | `ProfileScreen` + 3 ViewModels in `ProfileViewModel.kt` (missing lifecycle import → Hilt rejects all 3). `ProfileRepository` not bound in DI. VM double-writes (`saveUserProfile` then `updateProfile`). |
| **Measurements** | `MeasurementScreen` doesn't compile (unresolved `components.MeasurementCard`, `viewModel.trends`, `trendMap`). Add/delete/retry are commented-out no-ops. Unreachable via nav. |
| **Settings** | Doesn't compile (17 errors). Backup toggle/button are **stubs** — `onBackupClick = {}` (`SettingsScreen.kt:199`), no persistence, `BackupManager` never called. |
| **Search** | `ui/SearchScreen.kt` has no package/imports, references undefined types; breaks the whole main source set. No route, no caller. |
| **VShape cluster** | All 5 screens + `VShapeViewModel` + `VShapeRepository` + 5 use-case impls: **orphaned** (no NavHost route, no DI binding) and broken (93/49/34/31/28 errors each; screens read `uiState` that the VM never exposes). |
| **Goals** | `GoalEntity`/`GoalDao`/`GoalRepository` — not in DI, referenced-but-undefined model types, 15+9 errors. |
| **Workout templates** | `WorkoutTemplatesRepository(Impl)` — not in DI, never referenced, doesn't compile. |
| **Navigation graph** | `GymCoachNavHost.kt` has 10 errors (syntax error + call-site mismatches). Only one `navigate(...)` exists in the whole app (`ExerciseListScreen.kt:117` → SETTINGS) and its call site is itself broken. Every other route (`PROGRESS`, `MEASUREMENTS`, `CAMERA`, `PROFILE`, `PR`, `HISTORY`, `SESSION`, `SUMMARY`, `DETAIL`) has **zero callers**. |

### List 4 — Missing (absent entirely, or only stubs)
| Feature | Evidence |
|---|---|
| Real-time form feedback / pose detection | No model, no inference, no feedback pipeline. |
| Video content | No URLs, no thumbnails, no images anywhere in seed data. |
| Favorites UX | No UI, no DAO query, no repository method. |
| Exercise images / thumbnails | No image fields or resources (docs claim "detail view with images" — false). |
| Mood / energy / pain logging | Fields exist on `WorkoutEntity` but the summary screen (`WorkoutSummaryViewModel.calculateMetrics`) ignores them; no UI to record them. |
| Previous-set performance display | VM loads `exercisePerformance` (latest + best set per exercise) but **the session screen never renders it**. |
| Backup / restore | Inert UI only; `BackupManager.kt` orphaned, no serialization dependency, no file provider, no export/import. |
| Localization | `strings.xml` contains **one string** (`app_name`); 88 hardcoded `Text("…")` + 63 `contentDescription="…"` literals across screens. |
| Premium (workout templates, plate calculator, goals) | Orphaned/broken code only. |
| Onboarding | No onboarding/empty-state tour. |
| App icon polish / release build | Only debug APK ever built; no release keystore/signing documented for CI; no Play listing. |

---

## 4. Architecture & Tech Stack (verified)

| Layer | Reality |
|---|---|
| Language / UI | Kotlin 1.9.22, Jetpack Compose 1.6.0 (BOM `2024.02.00`), Material3 |
| DI | Hilt 2.50 (HiltViewModel + `@Binds` module binding 5 repos only) |
| Data | Room 2.6.1 (runtime+ktx), single DB `GymCoachDatabase`, **no migrations beyond `fallbackToDestructiveMigration()`** |
| Media | CameraX 1.3.1, MediaPipe 0.10.9 (**unused**), Media3 1.2.1 (ExoPlayer) |
| Other | Coil 2.5.0, Coroutines 1.7.3, Navigation Compose |
| Missing deps | No Retrofit/OkHttp (fine — offline), **no kotlinx-serialization** (blocks real backup), no DataStore/WorkManager, no Compose UI-test deps beyond the stub |
| Build | AGP 8.2.2, Gradle 8.4 wrapper, `minSdk 26 / target 34`, versionName bumped to `1.0.0` in WIP but never built |

**Clean-architecture violations found:**
- `ProfileViewModel.kt` is a 425-line file mixing **three ViewModels** + UI-state classes with Compose imports (dead UI imports), and the missing `ViewModel` import breaks Hilt.
- **Duplicate measurement domain:** `domain/measurement/` and `domain/vshape/` both declare `MeasurementUseCases`, `MeasurementUseCasesImpl`, `SaveMeasurementUseCase`, `GetLatestMeasurementUseCase`, plus two `MeasurementRecord`/`MeasurementType` model sets. The measurement UI imports the vshape models but the measurement use-cases; the vshape impls reference interfaces that don't exist. Both `MeasurementUseCasesImpl` files are compile-broken.
- **Two parallel profile repositories:** `UserProfileRepository` (bound, used by nothing visible) vs `ProfileRepository` (used by ProfileViewModel, **not bound**).
- Dead `/cpp` native config was removed by the WIP (good), but `mediapipe-tasks-vision` (~15 MB APK) remains with zero usages.

---

## 5. Runtime & Data-Correctness Defects (verified by reading)

1. **Room seed race:** `GymCoachDatabase.kt:56-64` seeds 20 exercises asynchronously on `Dispatchers.IO` inside `onCreate` → a fast first query can observe an empty table. `fallbackToDestructiveMigration()` wipes data on any schema change.
2. **Monthly grouping bug** (`WorkoutDao.kt:165-175`): `strftime('%Y-%m', w.date)` on epoch **millis** → wrong century/months.
3. **Workout-count month window bug** (`AnalyticsRepositoryImpl.kt:121-134`): mutating `Calendar` for week then month can set the month window to the previous month's 1st.
4. **NULL aggregates:** handled correctly for totals (nullable + `?:`), but `getLongestWorkout`/`getShortestWorkout` map SQL NULL `SUM` to non-null `Double`/`Int` → silently reads `0.0`/`0` (no crash, wrong data).
5. **`workoutFrequency` = number of week buckets** (not weekly workouts) → mislabeled "Weekly Workouts" card.
6. **`totalWorkouts` dead field** — redundant `COUNT(*)` query per load.
7. **PR empty-state gate** `highestWeight.value > 0.0` hides bodyweight-only users; `mostCalories` dead; no loading state (flicker).
8. **Accessibility:** 63 generic hardcoded `contentDescription`s; icon buttons "Back"/"Pause/Resume"; swipe-to-delete has no announcement; `Accessibility.kt` helpers (`buttonSemantics`/`screenSemantics`), `LargeAccessibleButton`, `AccessibleCheckbox`, `DragAndDropComponents`, `WorkoutComponents` all **unused** (screens re-implement inline).
9. **Rest timer** card is inline in the session screen; the shared `WorkoutComponents.RestTimerDisplay` is orphaned duplicate.

---

## 6. Tests & Quality (verified)

- **5 unit test files** (`app/src/test`, 22 methods, none instrumented):
  - `ExerciseRepositoryTest.kt` — committed, real mapping logic, viable.
  - `MeasurementRepositoryImplTest.kt` — new, real logic, viable.
  - `AddMeasurementUseCaseTest.kt` — new, real validation logic, viable.
  - `GetLatestMeasurementUseCaseTest.kt` / `GetMeasurementsForUserUseCaseTest.kt` — **do not compile** (invalid MockK `mockk { answers { … } }`; missing `assertTrue` import).
- **No** ViewModel tests, no Room instrumentation, no Compose UI tests, no `androidTest` dir. At HEAD only 1 test file existed, so CI "success" exercised almost no test coverage.
- Test source set is currently untracked and unbuildable along with main.

---

## 7. Committed Docs vs Reality

| Claim (docs) | Reality |
|---|---|
| `Summary.md`: "MediaPipe pose detection with real-time landmark overlay" | No MediaPipe usage, no model, raw preview only. |
| "Exercise detail view with images" | No images anywhere. |
| "All milestones completed. Version 1.0 released" | No release tag, no listing; debug APK only; versionName 1.0.0 is unbuilt WIP. |
| `KnownIssues.md`: "MediaPipe may require NDK… pose detection tested on device" | MediaPipe not wired at all. |
| `UX_RESEARCH_GAP_ANALYSIS.md` | Largely stale vs the WIP (already has set types, swipe-to-delete); still useful as competitive reference. |

**Lesson for the project:** prior "verified complete" reports and the committed docs did not match the code. This audit trusts only the compiler and the grep.

---

## 8. Competitive Comparison & Roadmap (premium apps: Strong, Hevy, Fitbod, Alpha Progression, FitNotes)

### Gap: GymCoach vs premium workout apps
| Area | Premium benchmark (Strong/Hevy/Fitbod) | GymCoach today |
|---|---|---|
| Core logging | One-hand gym flow, ghost "previous sets", PR badges, warm-up/drop/failure sets, plate calculator, pinned rest timer | Logging exists; set types + swipe exist; **no previous-set display, no PR badges in-session, no plate calc wired** |
| Library | Thumbnails, sticky headers, multi-select pills, favorites | Plain text list; single-select filter; no favorites |
| Detail | Hero video, muscle heatmaps, history charts, alternatives | Text-only card; no video, no images, no history |
| Progress | Auto body-composition tracking, PR feed, trend charts | One custom volume chart; broken/unreachable screens |
| Structure | Templates, routines, programs, supersets, custom metrics | None working (orphaned/broken templates/goals) |
| Retention | Streaks, achievements, social, reminders | None |
| Reliability | Migration-safe DB, tested release | Destructive migrations, no instrumentation tests, no release build |

### Recommended roadmap (order of operations)
1. **Make it compile** — fix the 768 errors (biggest: delete-or-fix the VShape/Search/goal clusters; add missing imports; fix NavHost). Target: committed-subset green again. *(blocking — nothing else matters until this is done)*
2. **Wire navigation** — single source of truth for routes; connect list → detail/history/progress/camera; add a bottom nav or hamburger to reach Settings/Profile/PR/Measurements.
3. **Data-correctness pass** — fix `strftime` millis, Calendar month window, NULL aggregates, seed race; add Room migrations instead of destructive.
4. **Quality gate** — make the 3 viable test files pass + add Workout/Analytics/ViewModel tests; fix the 2 broken test files; add CI unit-test + lint jobs that block merges.
5. **High-impact UX** (premium parity) — previous-set ghost text + in-session PR badges; exercise favorites (UI+DAO+repo); plate calculator in session; images/thumbnails for library.
6. **Camera/ML** — either remove MediaPipe dep and ship raw-preview "camera timer" honestly, or implement pose detection with a bundled model + overlay + `ImageAnalysis`. Currently it's a broken promise.
7. **i18n + a11y** — extract strings; adopt the shared `Accessibility`/state components; announce swipe actions.
8. **Backup & data portability** — add kotlinx-serialization, file provider, real export/import; replace the stub buttons.
9. **Release** — configure signing, a release build type, and an internal-testing track; create a proper v1.0 tag with the features that actually work.

### Effort heuristics (from the codebase's own commit history: the committed subset took ~3 days / 55 commits)
- Compile fix: **1–2 sessions** (mechanical: imports, signatures, delete orphaned clusters).
- Nav wiring + favorites + previous-set UX: **1–2 sessions**.
- Data correctness + migrations + tests: **1 session**.
- Camera/ML or backup/export: **1+ session each** (real scope).

---

## 9. Bottom Line

- **The repository as committed (`320d990`) is real, builds, and passes CI** — a small but genuine workout tracker (library, logging, history, one chart).
- **The current working tree is not a working app** — 768 compiler errors + failing Hilt/Room; every "reported complete" feature from the last session is unbuilt and, in several cases, fundamentally unwired or stubbed.
- **Do not merge the working tree or release it.** The honest path is: fix compile first, then navigate, then data correctness, then test/CI, then the highest-value UX gaps. Several docs (`Summary.md`, `KnownIssues.md`) must be corrected to match reality.
