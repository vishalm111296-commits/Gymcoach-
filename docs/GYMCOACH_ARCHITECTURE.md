# GymCoach Architecture Specification

**Version:** 2.0 (Phase 2 — Rebuild)
**Date:** 2026-08-22
**Stack (fixed):** Kotlin · Jetpack Compose · MVVM · Hilt · Room · Coroutines/Flow · Foreground Service

---

## 1. Layered Clean Architecture

```
presentation (Compose screens, ViewModels, navigation)
      │  StateFlow down / intent callbacks up. No domain types leak into composables.
domain   (use cases, pure domain models, repository INTERFACES, engines)
      │  Pure Kotlin + coroutines. Zero Android framework imports.
data     (Room entities/DAOs/DB, repositories IMPLEMENTATIONS, mappers, seed assets)
```

Rules:
- ViewModels inject use cases (or repositories directly for trivial reads); never DAOs.
- Domain models ≠ Room entities; mapping happens in `data/repository` mappers.
- Single Activity, Compose Navigation. State hoisted per-screen; timers live outside composition (service + repository), UI observes.
- DI: Hilt modules per layer (`DatabaseModule`, `RepositoryModule`, `ServiceModule`).

## 2. Module Map

Single `:app` Gradle module today; package boundaries drawn so extraction into Gradle modules is mechanical. Target structure:

```
core/
  di/            existing Hilt modules
  ml/            FormAnalyzer (UNWIRED — see §8)
  timer/         RestTimerManager (domain clock logic, service-agnostic)
  exercise/      NEW  ExerciseSeeder, SearchEngine, SubstitutionResolver
  program/       NEW  ProgramGenerator, VolumeCalculator, SplitTemplates
  progression/   NEW  ProgressionEngine, PRDetector, RecommendationFormatter
  notification/  NEW  RestTimerService (foreground), NotificationController, channels
  calendar/      NEW  TrainingCalendar (local-date math, streaks, heatmap buckets)
  photo/         NEW  EncryptedPhotoStore (in-app encrypted storage, hashing)
data/            Room v5, DAOs, repositories
domain/          models + repository interfaces + use cases
presentation/    onboarding, home, program, workout, exercise, progress, history, settings
ui/              NavHost, MainActivity, theme
widget/          NEW  TodayWorkoutWidgetProvider
```

Extraction rule for later: a `core/*` package qualifies to become a Gradle module when a second consumer appears; until then packages win (fewest moving parts).

## 3. core/notification — Rest Timer Foreground Service

The rest timer is a **first-class OS citizen**, not an in-app composable:

- `RestTimerService`: started foreground service, typed foreground-service declaration in manifest (targetSdk-compliant, with user-visible rationale on first use).
- Ongoing notification, channel `rest_timer`:
  - Title: exercise + set just completed ("Bench Press · Set 2 done")
  - Countdown text updated every second
  - `visibility = PUBLIC`, `ongoing = true`, shown on **lock screen**
- **Action buttons (4):**
  1. **Complete Set** — marks pending set complete, advances session cursor
  2. **Skip** — cancel remainder, advance cursor, start next rest chain if applicable
  3. **+15 s** — extend remaining
  4. **−15 s** — reduce, floor 0 (expiry fires immediately at 0)
- Actions delivered via pending intents → service mutates timer state held in `RestTimerManager` (singleton, coroutine-based); UI re-subscribes via shared Flow. App kill does not stop service; service stops itself at expiry or Skip.

## 4. Superset Chaining (Antagonist Pairs)

- `program_exercises.superset_group` (int, nullable) groups consecutive paired exercises; pairs declared antagonist-aware in split templates (bench↔row, OHP↔pull-up/pulldown, curl↔pushdown, lunge↔hip thrust).
- Timing model per group member: log set → **transition timer** (default 15–30 s, enough to walk/set up partner station) → partner logs set → back; after BOTH sides of pair finish a round → **full rest** (configured per exercise).
- Implementation: the session state machine emits `RestChain` events (`TRANSITION_TO(exerciseId)` / `FULL_REST(seconds)`); `core/notification` consumes an identical API whether chained or plain — one notification pipeline, two cadences.
- Manual Skip breaks chain gracefully: remaining partner becomes standalone exercise, no orphaned timers.
- Rationale: antagonist supersets maintain performance while roughly halving session wall-clock; treated as scheduling, not a hypertrophy claim (see SCIENTIFIC_EVIDENCE §R6).

## 5. Home-Screen Widget

- Glance-based `TodayWorkoutWidgetProvider`: program day name, target muscles, est. duration, tap → deep link `gymcoach://workout/today` (resolves to session screen; onboarding if uninitialized).
- Refresh triggers: program change, workout completion (explicit refresh, never polling).
- Offline trivially satisfied (direct Room read).

## 6. Offline-First Data Flow

- **Room is the single source of truth.** Repositories expose Flows from DAOs; UI never caches separately.
- Writes go repository → DAO synchronously (suspend); optimistic UI from local write, no network round-trip exists to wait on.
- Seed data (exercises, muscle taxonomy) ships as versioned JSON assets; transactional upsert keyed by `seed_version` in `user_profiles` — content updates without schema migration.
- No Retrofit/OkHttp/Ktor anywhere in v2 scope. If sync is ever added, it plugs in at repository implementations behind existing interfaces (offline remains authoritative, last-write-wins per row with `updated_at`).

## 7. Media Policy

- Exercise demos = **text instructions + static images** (bundled drawables/asset URIs). No Media3/ExoPlayer dependency, no streaming; `video_url` column exists for forward-compat only and is never consumed.
- Image loading: Coil, disk-cached, graceful placeholder (muscle-group glyph) — library renders fully with zero network.

## 8. Camera Form Analysis — Unwired, Documented

Existing `FormAnalyzer` (CameraX + MediaPipe Pose) stays **compiled but unwired**: no navigation entry point, no permissions requested at runtime.

API contract preserved for future wiring:
- Input: RGB frame stream (CameraX `ImageProxy`) + exercise ID
- Output: per-frame joint landmarks → rep-segmented joint-angle series → phase detection + cue list (e.g., "depth below parallel", "bar path drift")
- Integration point when activated: `ExerciseDetail` "Form Check" tab requesting CAMERA permission contextually; results advisory-only, never scored numerically.

Documented here so future wiring needs no archaeology; nothing else in the build depends on it.

## 9. Concurrency & Process-Death Safety

- All DB access via suspend/Flow on background dispatchers (Room-managed executors; never `allowMainThreadQueries`).
- Session writes are incremental: each completed set row committed instantly; workout row finalized on Finish. Process death mid-set loses at most in-progress field state, restored from ViewModel SavedStateHandle.
- Timers survive process death via service; session cursor survives via Room (session state is data, not memory).

## 10. Testing Architecture

- DAO tests: in-memory Room, incl. **migration test v4→v5** using exported schema JSONs (see DATA_MODEL §6).
- Engines (`progression`, `program`, `timer`) are pure-Kotlin JVM unit tested.
- ViewModel tests with faked repositories; Compose UI smoke on critical loop (log set → timer → finish).
