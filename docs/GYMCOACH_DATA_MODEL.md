# GymCoach Data Model Specification

**Version:** 5 (canonical target) · **Date:** 2026-08-22 · **Engine:** Room (SQLite)
**Scope:** Full entity catalogue, indexes/FK policy, complete `MIGRATION_4_5`, DAO contracts.
No Kotlin in this doc — DDL/SQL is the contract; entity classes implement it verbatim.

---

## 1. Versioning Policy (Non-Negotiables)

1. `version = 5` stays; all changes land as the single corrected `MIGRATION_4_5` below.
2. **`exportSchema = true` becomes mandatory** — commit generated JSON schemas under `app/schemas/` per version. Without them there is no `MigrationTestHelper` and no way to verify migrations. Current main ships `exportSchema = false`: remediation item R-1.
3. **Remove `fallbackToDestructiveMigration()`** (remediation R-2). Silent user-data erasure is unacceptable for a training-history product.
4. Fresh installs get the canonical schema from `onCreate`; `MIGRATION_4_5` only serves upgrading databases.
5. Every new table/column ships in the same release as the feature reading it. No dead columns except the three documented forward-compat exceptions (`exercises.video_url`, `animation_url`, `tempo_guidance`).

## 2. Current-State Audit (main @ `94eca0f`, why this migration is subtle)

Code today declares `version = 5` but ships a **skeletal `MIGRATION_4_5`** whose DDL does not match the entities Room validates against:

| Table | Shipped migration creates | Entity expects |
|---|---|---|
| `programs` | `(id,name,description,goal,difficulty)` | + `user_id, split_type, duration_weeks, days_per_week, is_active, created_at` |
| `program_days` | `(programId, dayOfWeek, name)` | `program_id, day_number, focus, is_rest_day` |
| `program_exercises` | `(dayId, exerciseId, reps, restTime …)` | `program_day_id, exercise_id, target_reps, rest_seconds, target_weight_kg, notes …` |
| `personal_records` | `(weight, reps, rpe, date)` | `weight_kg, reps, one_rep_max_kg, achieved_at, notes, pr_type` |
| `favorite_exercises` | `(userId, exerciseId, createdAt)` | `exercise_id, user_id, added_at` |
| `exercise_substitutions` | `(sourceExerciseId, targetExerciseId)` | `original_exercise_id, substitute_exercise_id, reason` |
| `muscles` | `(name, primary)` | `name, display_name, parent_muscle_id, body_region` |
| `equipment` | `(name)` | `name, display_name, category` |
| `exercise_muscles` / `exercise_equipment` | no `role` column | `role` required |
| `exercise_aliases` | `(name, exerciseId)` | `exercise_id, alias` |
| all of the above | **zero foreign keys, zero indexes** | FKs + indexes per §4 |

Consequences: any device that ran the shipped `MIGRATION_4_5` holds a schema Room cannot bind → runtime crash. Because the app is **pre-release (zero shipped users)**, the canonical plan is **fix `MIGRATION_4_5` in place** (contingency for stray dev installs: wipe-and-reinstall, documented in §6.3).

Assumed **true v4 baseline** (tables that verifiably predate the skeletal migration): `exercises` (30 core columns, *without* the 15 v-taper/media/instruction columns), `workouts`, `workout_exercises`, `workout_sets` (without `setType`). Nothing else is guaranteed to exist.

## 3. Canonical v5 Entity Catalogue

Notation: PK = primary key autoincrement unless noted. Timestamps = epoch millis INTEGER. Booleans = INTEGER 0/1. All FKs **ON DELETE CASCADE** unless stated.

### Group A — User & Equipment

**A1. `user_profiles` (UserProfile)** — single row (id = 1) after onboarding
- `id` PK · `sex` TEXT NN DEF `'male'` · `age` INTEGER NN · `height_cm` REAL NN DEF 170 · `weight_kg` REAL NN DEF 70
- `goal` TEXT NN DEF `'vtaper'` · `experience_level` TEXT NN DEF `'beginner'`
- `days_per_week` INTEGER NN DEF 4 · `session_minutes` INTEGER NN DEF 60
- `unit` TEXT NN DEF `'kg'` · `onboarded` INTEGER NN DEF 0
- `active_program_id` INTEGER NULL (logical ref to `programs.id`, **no FK** — deleting a program must not delete the profile)
- `seed_version` INTEGER NN DEF 0 · `created_at`, `updated_at` INTEGER NN

**A2. `equipment_items` (EquipmentItem)** — replaces bare `equipment`
- `id` PK · `key` TEXT NN UNIQUE (stable slug, e.g. `dumbbell`, `bench`, `cable_stack`) · `category` TEXT NN · `display_name` TEXT NN
- `available` INTEGER NN DEF 0 · `max_weight_kg` REAL NULL · `increment_kg` REAL NULL · `sort_order` INTEGER NN DEF 0
- Indexes: `key` UNIQUE, `available`

### Group B — Exercise Taxonomy

**B1. `exercises` (Exercise)** — unchanged 30-column shape from main (name, description, muscle_group, equipment, difficulty, secondary_muscles, instructions, tips, common_mistakes, safety_notes, recommended_rep_range, recommended_rest_time, estimated_calories, category, tags, is_favorite, last_viewed, vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt, movement_pattern, image_url, video_url*, animation_url*, setup/execution/breathing_instructions, tempo_guidance*, beginner_variant_id, advanced_variant_id). *forward-compat, never consumed.
- Indexes: `name`, `muscle_group`, `difficulty` (search filters)

**B2. `exercise_media` (ExerciseMedia)** — NEW, canonical media list
- `id` PK · `exercise_id` FK→exercises · `kind` TEXT NN CHECK IN(`'image'`,`'animation'`,`'video'`)
- `url` TEXT NN · `position` INTEGER NN DEF 0 · `caption` TEXT NN DEF `''`
- Index: `exercise_id`. Seeder writes both this table and the legacy `image_url` column until v6 cleanup.

**B3. `exercise_aliases` (alias)** — `id` PK · `exercise_id` FK · `alias` TEXT NN · Indexes: `exercise_id`, `alias`

**B4. `exercise_muscles`** — composite PK (`exercise_id`,`muscle_id`) · `role` TEXT NN CHECK IN(`'primary'`,`'secondary'`,`'stabilizer'`) · FKs CASCADE to exercises/muscles · Index both columns

**B5. `exercise_equipment`** — composite PK (`exercise_id`,`equipment_id`) · `role` TEXT NN DEF `'required'` CHECK IN(`'required'`,`'optional'`) · FKs CASCADE to exercises/equipment_items · Index both

**B6. `exercise_alternatives` (ExerciseAlternative)** — NEW, replaces `exercise_substitutions`
- `id` PK · `source_exercise_id` FK · `target_exercise_id` FK · `reason` TEXT NN DEF `'equipment'` CHECK IN(`'equipment'`,`'injury'`,`'difficulty'`,`'preference'`)
- UNIQUE(`source_exercise_id`,`target_exercise_id`) · Index each column

**B7. `muscles` (Muscle, hierarchical)** — `id` PK · `name` TEXT NN UNIQUE · `display_name` TEXT NN · `parent_muscle_id` INTEGER NULL (self-ref, **app-enforced** — Room self-FK ordering is fragile; index it) · `body_region` TEXT NN DEF `''`

**B8. `favorite_exercises`** — `id` PK · `exercise_id` FK · `user_id` INTEGER NN DEF 1 · `added_at` INTEGER NN · UNIQUE(`exercise_id`,`user_id`)

**B9. `recent_exercises` (RecentExercise)** — NEW — `id` PK · `exercise_id` FK **UNIQUE** · `last_used_at` INTEGER NN. Upsert-on-use semantics (§5 Q6); capped at 20 rows by repository trim.

### Group C — Program

**C1. `programs` (Program)** — `id` PK · `user_id` NN DEF 1 · `name` TEXT NN · `description` TEXT NN DEF `''` · `split_type` TEXT NN DEF `''` · `duration_weeks` INTEGER NN DEF 4 · `days_per_week` INTEGER NN DEF 4 · `difficulty` TEXT NN DEF `'intermediate'` · `goal` TEXT NN DEF `'vtaper'` · `is_active` INTEGER NN DEF 0 · `created_at` INTEGER NN · Index `is_active`

**C2. `program_weeks` (ProgramWeek)** — NEW — `id` PK · `program_id` FK · `week_number` INTEGER NN · `is_deload` INTEGER NN DEF 0 · `notes` TEXT NN DEF `''` · UNIQUE(`program_id`,`week_number`)

**C3. `program_days` (ProgramDay)** — `id` PK · `program_week_id` FK→program_weeks (replaces direct `program_id` — week structure is now first-class) · `day_number` INTEGER NN (within week) · `name` TEXT NN DEF `''` · `focus` TEXT NN DEF `''` · `is_rest_day` INTEGER NN DEF 0 · Index `program_week_id`

**C4. `program_exercises` (ProgramExercise)** — `id` PK · `program_day_id` FK · `exercise_id` FK · `order_index` INTEGER NN DEF 0 · `sets` INTEGER NN DEF 3 · `target_reps` TEXT NN DEF `'8-12'` · `target_rir` REAL NULL DEF 2.0 (NEW — proximity-to-failure target) · `target_weight_kg` REAL NULL DEF 0 · `rest_seconds` INTEGER NN DEF 90 · `superset_group` INTEGER NULL (NEW — pairs antagonist chaining, see ARCHITECTURE §4) · `notes` TEXT NN DEF `''` · Index `program_day_id`, `exercise_id`

### Group D — Logging

**D1. `workouts` (Workout)** — existing camelCase shape kept (`date, startTime, endTime, duration, notes, completed`) to minimise diff + NEW `program_day_id` INTEGER NULL (logical ref, no FK — history must survive program edits) · Index `date`, `program_day_id`

**D2. `workout_exercises` (WorkoutExercise)** — existing (`workout_id` FK CASCADE, `exercise_id` FK CASCADE, `order_index`) + NEW `superset_group` INTEGER NULL (copied from program row at session start) · Index both FKs (existing)

**D3. `workout_sets` (WorkoutSet)** — existing (`workout_exercise_id` FK CASCADE, `set_number`, `weight` REAL NN, `reps` INTEGER NN, `rpe` REAL NN DEF 0, `rest_seconds`, `completed`, `set_type` INTEGER NN DEF 0; enum 0=NORMAL 1=WARMUP 2=DROP 3=FAILURE)
- NEW `rir` REAL NULL — repetitions-in-reserve as logged; NULL = not asked
- NEW `bodyweight_load_pct` REAL NULL — % of bodyweight used as load proxy when external load absent (push-up ≈ 60); tonnage math: `weight * reps * COALESCE(bodyweight_load_pct/100, 1.0)`
- Index `workout_exercise_id` (existing)

**D4. `personal_records` (PersonalRecord)** — `id` PK · `exercise_id` FK · `user_id` NN DEF 1 · `pr_type` TEXT NN DEF `'weight'` CHECK IN(`'weight'`,`'reps'`,`'est_1rm'`,`'session_volume'`) (NEW) · `weight_kg` REAL NN DEF 0 · `reps` INTEGER NN DEF 0 · `one_rep_max_kg` REAL NN DEF 0 · `achieved_at` INTEGER NN · `notes` TEXT NN DEF `''` · Index (`exercise_id`,`pr_type`), `achieved_at`. Uniqueness enforced app-level (latest-wins per type).

### Group E — Body & Wellbeing

**E1. `body_measurements` (BodyMeasurement)** — full existing shape (weight_kg NN, body_fat_pct NULL self-measured only, chest/waist/hips/shoulders/left_arm/right_arm/left_thigh/right_thigh/left_calf/right_calf cm NULL, notes) + NEW `neck_cm` REAL NULL · Index `recorded_at`

**E2. `progress_photos` (ProgressPhoto)** — NEW — `id` PK · `taken_at` INTEGER NN · `file_path` TEXT NN (**relative path inside app-private encrypted dir** — never absolute, never MediaStore) · `sha256` TEXT NN UNIQUE (integrity + dedupe) · `weight_kg` REAL NULL · `notes` TEXT NN DEF `''` · Index `taken_at`

**E3. `readiness_checks` (ReadinessCheck)** — NEW — `id` PK · `checked_at` INTEGER NN · `sleep_quality` INTEGER NULL CHECK 1–5 · `soreness` INTEGER NULL CHECK 0–5 · `motivation` INTEGER NULL CHECK 1–5 · `resting_hr` INTEGER NULL · `skipped` INTEGER NN DEF 0 · `notes` TEXT NN DEF `''` · Index `checked_at`. Feeds qualitative messaging only (SPEC PS-11).

## 4. Integrity Summary

- **CASCADE chains:** delete workout → exercises → sets; delete program → weeks → days → program_exercises; delete exercise → aliases/muscle/equipment joins/alternatives/favorites/recents/media/PR rows. History rows (`workout_exercises.exercise_id`) cascade too — accepted: deleting an exercise is a seed-admin action, exposed nowhere in UI.
- **No-FK logical refs (deliberate):** `user_profiles.active_program_id`, `workouts.program_day_id` — program/profile lifecycle must not orphan-delete training history.
- All FK-carrying tables declare their indices (Room warns otherwise; warning = build error per lint policy).

## 5. DAO Contracts & Key Queries

DAOs: `ExerciseDao`, `WorkoutDao`, `ProgramDao`, `AnalyticsDao` (NEW), `BodyDao` (measurements/photos/readiness), `ProfileDao` (profile/equipment/favorites/recents), `PersonalRecordDao`. Contracts as name + SQL; return-type shaping (nested relations vs flat projections) is implementation detail.

**Q1 — Workout with details** (`WorkoutDao.getWorkoutWithDetails`)
```sql
SELECT w.*, we.id AS workout_exercise_id, we.order_index, we.superset_group,
       e.name AS exercise_name, ws.id AS set_id, ws.set_number, ws.weight, ws.reps,
       ws.rpe, ws.rir, ws.bodyweight_load_pct, ws.set_type, ws.completed
FROM workouts w
LEFT JOIN workout_exercises we ON we.workout_id = w.id
LEFT JOIN exercises e          ON e.id = we.exercise_id
LEFT JOIN workout_sets ws      ON ws.workout_exercise_id = we.id
WHERE w.id = :workoutId
ORDER BY we.order_index ASC, ws.set_number ASC
```
(Equivalent nested `@Relation` model acceptable; flat projection preferred for summary screens.)

**Q2 — Last performance per exercise** (ghost-text prefill; portable — no window functions, keeps minSdk floor low)
```sql
SELECT we.exercise_id, ws.weight, ws.reps, ws.rir, w.date
FROM workout_sets ws
JOIN workout_exercises we ON we.id = ws.workout_exercise_id
JOIN workouts w           ON w.id = we.workout_id
WHERE we.exercise_id IN (:exerciseIds) AND w.completed = 1
  AND w.date = (SELECT MAX(w2.date) FROM workouts w2
                JOIN workout_exercises we2 ON we2.workout_id = w2.id
                WHERE we2.exercise_id = we.exercise_id
                  AND w2.completed = 1 AND w2.date < :beforeDate)
```

**Q3 — Weekly hard-set volume by muscle** (home dashboard band chart)
```sql
SELECT em.muscle_id, m.display_name,
       COUNT(ws.id) AS hard_sets,
       SUM(ws.weight * ws.reps * COALESCE(ws.bodyweight_load_pct / 100.0, 1.0)) AS tonnage_kg
FROM workout_sets ws
JOIN workout_exercises we ON we.id = ws.workout_exercise_id
JOIN workouts w           ON w.id = we.workout_id
JOIN exercise_muscles em  ON em.exercise_id = we.exercise_id AND em.role = 'primary'
JOIN muscles m            ON m.id = em.muscle_id
WHERE w.completed = 1 AND ws.set_type = 0 AND w.date BETWEEN :fromInclusive AND :toInclusive
GROUP BY em.muscle_id
```
(Secondary-muscle variant drops the role filter and halves contribution — domain-layer rule, not SQL.)

**Q4 — PR detection candidates** (`PersonalRecordDao`)
```sql
-- weight PR: heaviest normal/failure set per exercise
SELECT we.exercise_id, MAX(ws.weight) AS best_weight
FROM workout_sets ws JOIN workout_exercises we ON we.id = ws.workout_exercise_id
JOIN workouts w ON w.id = we.workout_id
WHERE w.completed = 1 AND ws.set_type IN (0, 3) AND w.date <= :now
GROUP BY we.exercise_id;

-- rep PR at a given load
SELECT MAX(ws.reps) FROM workout_sets ws
JOIN workout_exercises we ON we.id = ws.workout_exercise_id
WHERE we.exercise_id = :exerciseId AND ws.weight = :weightKg AND ws.set_type IN (0, 3);

-- estimated 1RM (Epley, HARD-CAPPED at 12 reps — see SCIENTIFIC_EVIDENCE §R13)
SELECT we.exercise_id,
       MAX(ws.weight * (1 + MIN(ws.reps, 12) / 30.0)) AS est_1rm
FROM workout_sets ws JOIN workout_exercises we ON we.id = ws.workout_exercise_id
JOIN workouts w ON w.id = we.workout_id
WHERE w.completed = 1 AND ws.set_type IN (0, 3) AND ws.weight > 0
GROUP BY we.exercise_id;

-- session-volume PR (tonnage per exercise per session)
SELECT we.exercise_id, we.workout_id,
       SUM(ws.weight * ws.reps * COALESCE(ws.bodyweight_load_pct / 100.0, 1.0)) AS session_volume
FROM workout_sets ws JOIN workout_exercises we ON we.id = ws.workout_exercise_id
WHERE ws.completed = 1 GROUP BY we.exercise_id, we.workout_id
```
PR rows written only when strictly greater than stored value for that `pr_type`.

**Q5 — Training-day calendar / streak** (`AnalyticsDao`)
```sql
SELECT DISTINCT date(w.date / 1000, 'unixepoch', 'localtime') AS day
FROM workouts w WHERE w.completed = 1 AND w.date BETWEEN :from AND :to ORDER BY day
```

**Q6 — Recents upsert** (`ProfileDao.touchRecent`)
```sql
INSERT INTO recent_exercises (exercise_id, last_used_at) VALUES (:exerciseId, :now)
ON CONFLICT(exercise_id) DO UPDATE SET last_used_at = excluded.last_used_at
```

## 6. Migration Plan

### 6.1 Complete `MIGRATION_4_5` (canonical, replaces shipped version wholesale)

Runs inside Room's migration transaction. Ordered phases:

```sql
-- PHASE A: extend logging tables (true v4 lacked these)
ALTER TABLE workouts ADD COLUMN program_day_id INTEGER NULL;
CREATE INDEX IF NOT EXISTS index_workouts_date ON workouts(date);
CREATE INDEX IF NOT EXISTS index_workouts_program_day_id ON workouts(program_day_id);
ALTER TABLE workout_exercises ADD COLUMN superset_group INTEGER NULL;
ALTER TABLE workout_sets ADD COLUMN rir REAL NULL;
ALTER TABLE workout_sets ADD COLUMN bodyweight_load_pct REAL NULL;
ALTER TABLE workout_sets ADD COLUMN set_type INTEGER NOT NULL DEFAULT 0;
ALTER TABLE body_measurements ADD COLUMN neck_cm REAL NULL;

-- PHASE B: extend exercises (v-taper + media + instructions, matching entity defaults)
ALTER TABLE exercises ADD COLUMN vtaper_lat INTEGER NOT NULL DEFAULT 0;
ALTER TABLE exercises ADD COLUMN vtaper_lateral_delt INTEGER NOT NULL DEFAULT 0;
ALTER TABLE exercises ADD COLUMN vtaper_upper_chest INTEGER NOT NULL DEFAULT 0;
ALTER TABLE exercises ADD COLUMN vtaper_rear_delt INTEGER NOT NULL DEFAULT 0;
ALTER TABLE exercises ADD COLUMN movement_pattern TEXT NOT NULL DEFAULT '';
ALTER TABLE exercises ADD COLUMN image_url TEXT NULL;
ALTER TABLE exercises ADD COLUMN video_url TEXT NULL;
ALTER TABLE exercises ADD COLUMN animation_url TEXT NULL;
ALTER TABLE exercises ADD COLUMN setup_instructions TEXT NOT NULL DEFAULT '';
ALTER TABLE exercises ADD COLUMN execution_instructions TEXT NOT NULL DEFAULT '';
ALTER TABLE exercises ADD COLUMN breathing_instructions TEXT NOT NULL DEFAULT '';
ALTER TABLE exercises ADD COLUMN tempo_guidance TEXT NOT NULL DEFAULT '';
ALTER TABLE exercises ADD COLUMN beginner_variant_id INTEGER NULL;
ALTER TABLE exercises ADD COLUMN advanced_variant_id INTEGER NULL;

-- PHASE C: drop skeletal taxonomy tables from the broken partial migration.
-- Pre-release reality: these hold at most dev-test junk; all are re-seeded.
-- (Each wrapped defensively — see 6.3 for devices that half-ran the old script.)
DROP TABLE IF EXISTS exercise_substitutions;
DROP TABLE IF EXISTS exercise_aliases;
DROP TABLE IF EXISTS exercise_muscles;
DROP TABLE IF EXISTS exercise_equipment;
DROP TABLE IF EXISTS favorite_exercises;
DROP TABLE IF EXISTS personal_records;
DROP TABLE IF EXISTS program_exercises;
DROP TABLE IF EXISTS program_days;
DROP TABLE IF EXISTS programs;
DROP TABLE IF EXISTS muscles;
DROP TABLE IF EXISTS equipment;

-- PHASE D: create canonical taxonomy/program tables (DDL must byte-match Room's
-- generated schema from the entities — verified by MigrationTestHelper, R-3)
CREATE TABLE IF NOT EXISTS user_profiles (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  sex TEXT NOT NULL DEFAULT 'male', age INTEGER NOT NULL DEFAULT 30,
  height_cm REAL NOT NULL DEFAULT 170, weight_kg REAL NOT NULL DEFAULT 70,
  goal TEXT NOT NULL DEFAULT 'vtaper', experience_level TEXT NOT NULL DEFAULT 'beginner',
  days_per_week INTEGER NOT NULL DEFAULT 4, session_minutes INTEGER NOT NULL DEFAULT 60,
  unit TEXT NOT NULL DEFAULT 'kg', onboarded INTEGER NOT NULL DEFAULT 0,
  active_program_id INTEGER NULL, seed_version INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL DEFAULT 0);

CREATE TABLE IF NOT EXISTS equipment_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  key TEXT NOT NULL UNIQUE, category TEXT NOT NULL, display_name TEXT NOT NULL,
  available INTEGER NOT NULL DEFAULT 0, max_weight_kg REAL NULL,
  increment_kg REAL NULL, sort_order INTEGER NOT NULL DEFAULT 0);
CREATE INDEX IF NOT EXISTS index_equipment_items_available ON equipment_items(available);

CREATE TABLE IF NOT EXISTS muscles (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  name TEXT NOT NULL UNIQUE, display_name TEXT NOT NULL,
  parent_muscle_id INTEGER NULL, body_region TEXT NOT NULL DEFAULT '');
CREATE INDEX IF NOT EXISTS index_muscles_parent_muscle_id ON muscles(parent_muscle_id);

CREATE TABLE IF NOT EXISTS exercise_media (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  kind TEXT NOT NULL CHECK(kind IN ('image','animation','video')),
  url TEXT NOT NULL, position INTEGER NOT NULL DEFAULT 0, caption TEXT NOT NULL DEFAULT '');
CREATE INDEX IF NOT EXISTS index_exercise_media_exercise_id ON exercise_media(exercise_id);

CREATE TABLE IF NOT EXISTS exercise_aliases (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  alias TEXT NOT NULL);
CREATE INDEX IF NOT EXISTS index_exercise_aliases_exercise_id ON exercise_aliases(exercise_id);
CREATE INDEX IF NOT EXISTS index_exercise_aliases_alias ON exercise_aliases(alias);

CREATE TABLE IF NOT EXISTS exercise_muscles (
  exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  muscle_id INTEGER NOT NULL REFERENCES muscles(id) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK(role IN ('primary','secondary','stabilizer')),
  PRIMARY KEY(exercise_id, muscle_id));
CREATE INDEX IF NOT EXISTS index_exercise_muscles_muscle_id ON exercise_muscles(muscle_id);

CREATE TABLE IF NOT EXISTS exercise_equipment (
  exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  equipment_id INTEGER NOT NULL REFERENCES equipment_items(id) ON DELETE CASCADE,
  role TEXT NOT NULL DEFAULT 'required' CHECK(role IN ('required','optional')),
  PRIMARY KEY(exercise_id, equipment_id));
CREATE INDEX IF NOT EXISTS index_exercise_equipment_equipment_id ON exercise_equipment(equipment_id);

CREATE TABLE IF NOT EXISTS exercise_alternatives (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  source_exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  target_exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  reason TEXT NOT NULL DEFAULT 'equipment'
    CHECK(reason IN ('equipment','injury','difficulty','preference')),
  UNIQUE(source_exercise_id, target_exercise_id));
CREATE INDEX IF NOT EXISTS index_exercise_alternatives_target ON exercise_alternatives(target_exercise_id);

CREATE TABLE IF NOT EXISTS favorite_exercises (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL DEFAULT 1, added_at INTEGER NOT NULL DEFAULT 0,
  UNIQUE(exercise_id, user_id));

CREATE TABLE IF NOT EXISTS recent_exercises (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  exercise_id INTEGER NOT NULL UNIQUE REFERENCES exercises(id) ON DELETE CASCADE,
  last_used_at INTEGER NOT NULL DEFAULT 0);

CREATE TABLE IF NOT EXISTS programs (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  user_id INTEGER NOT NULL DEFAULT 1, name TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '', split_type TEXT NOT NULL DEFAULT '',
  duration_weeks INTEGER NOT NULL DEFAULT 4, days_per_week INTEGER NOT NULL DEFAULT 4,
  difficulty TEXT NOT NULL DEFAULT 'intermediate', goal TEXT NOT NULL DEFAULT 'vtaper',
  is_active INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL DEFAULT 0);
CREATE INDEX IF NOT EXISTS index_programs_is_active ON programs(is_active);

CREATE TABLE IF NOT EXISTS program_weeks (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  program_id INTEGER NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
  week_number INTEGER NOT NULL, is_deload INTEGER NOT NULL DEFAULT 0,
  notes TEXT NOT NULL DEFAULT '', UNIQUE(program_id, week_number));

CREATE TABLE IF NOT EXISTS program_days (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  program_week_id INTEGER NOT NULL REFERENCES program_weeks(id) ON DELETE CASCADE,
  day_number INTEGER NOT NULL, name TEXT NOT NULL DEFAULT '',
  focus TEXT NOT NULL DEFAULT '', is_rest_day INTEGER NOT NULL DEFAULT 0);
CREATE INDEX IF NOT EXISTS index_program_days_program_week_id ON program_days(program_week_id);

CREATE TABLE IF NOT EXISTS program_exercises (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  program_day_id INTEGER NOT NULL REFERENCES program_days(id) ON DELETE CASCADE,
  exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  order_index INTEGER NOT NULL DEFAULT 0, sets INTEGER NOT NULL DEFAULT 3,
  target_reps TEXT NOT NULL DEFAULT '8-12', target_rir REAL NULL DEFAULT 2.0,
  target_weight_kg REAL NULL DEFAULT 0, rest_seconds INTEGER NOT NULL DEFAULT 90,
  superset_group INTEGER NULL, notes TEXT NOT NULL DEFAULT '');
CREATE INDEX IF NOT EXISTS index_program_exercises_program_day_id ON program_exercises(program_day_id);
CREATE INDEX IF NOT EXISTS index_program_exercises_exercise_id ON program_exercises(exercise_id);

CREATE TABLE IF NOT EXISTS personal_records (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL DEFAULT 1,
  pr_type TEXT NOT NULL DEFAULT 'weight'
    CHECK(pr_type IN ('weight','reps','est_1rm','session_volume')),
  weight_kg REAL NOT NULL DEFAULT 0, reps INTEGER NOT NULL DEFAULT 0,
  one_rep_max_kg REAL NOT NULL DEFAULT 0, achieved_at INTEGER NOT NULL DEFAULT 0,
  notes TEXT NOT NULL DEFAULT '');
CREATE INDEX IF NOT EXISTS index_personal_records_exercise_type
  ON personal_records(exercise_id, pr_type);
CREATE INDEX IF NOT EXISTS index_personal_records_achieved_at ON personal_records(achieved_at);

CREATE TABLE IF NOT EXISTS progress_photos (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  taken_at INTEGER NOT NULL, file_path TEXT NOT NULL,
  sha256 TEXT NOT NULL UNIQUE, weight_kg REAL NULL, notes TEXT NOT NULL DEFAULT '');
CREATE INDEX IF NOT EXISTS index_progress_photos_taken_at ON progress_photos(taken_at);

CREATE TABLE IF NOT EXISTS readiness_checks (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  checked_at INTEGER NOT NULL,
  sleep_quality INTEGER NULL CHECK(sleep_quality IS NULL OR sleep_quality BETWEEN 1 AND 5),
  soreness INTEGER NULL CHECK(soreness IS NULL OR soreness BETWEEN 0 AND 5),
  motivation INTEGER NULL CHECK(motivation IS NULL OR motivation BETWEEN 1 AND 5),
  resting_hr INTEGER NULL, skipped INTEGER NOT NULL DEFAULT 0,
  notes TEXT NOT NULL DEFAULT '');
CREATE INDEX IF NOT EXISTS index_readiness_checks_checked_at ON readiness_checks(checked_at);

-- PHASE E: reseed taxonomy (muscles, equipment_items, exercise_* joins) from
-- versioned JSON assets via the normal seeder path; bump seed_version.
```

Post-migration steps (code, not SQL): run `ExerciseSeeder` transactionally; insert default `equipment_items` presets; verify `PRAGMA foreign_key_check` returns empty in the migration test.

### 6.2 Data-salvage note

True-v4 devices hold no taxonomy data worth keeping (seeds regenerate), so PHASE C loses nothing real. `personal_records` / `favorite_exercises` created by dev installs of the broken migration are discarded likewise. **If the app ever ships publicly before this fix lands, abandon fix-in-place: ship `version = 6` + `MIGRATION_5_6` performing the same PHASE C–E with column-mapped `INSERT SELECT` salvage from the legacy layouts listed in §2.** Decision recorded here so nobody re-derives it.

### 6.3 Verification gates (blocking)

R-1 `exportSchema = true`; commit `app/schemas/**` JSONs.
R-2 Delete `fallbackToDestructiveMigration()`.
R-3 `MigrationTestHelper` test: create v4 DB from exported `4.json`, run `MIGRATION_4_5`, `validateMigratedSchema` against `5.json`; assert preserved workout rows survive PHASE A intact.
R-4 Register ALL entities in `@Database` (incl. the seven new ones) — current main creates `user_profiles` in SQL with no backing entity: drift bug class this policy kills.

## 7. Privacy Notes

- `progress_photos.file_path` stores relative segments only; absolute resolution happens in `core/photo` at read time. JSON export redacts `file_path` + `sha256` unless user opts in.
- `body_measurements.body_fat_pct` is user-entered; UI labels it "self-measured" everywhere (SPEC §5).