# Phase 2 — Data Layer Audit

Scope: Room persistence layer for GymCoach v1.
Checked against plan acceptance criteria: schema correctness, DAO contract correctness,
database wiring/migrations, and data-seeding strategy.

Line numbers approximate (from `github_get_file_contents` output; exact line refs
marked `~L`).

Verdict: **FAIL — BLOCKER-grade issues prevent compilation and risk silent data loss on upgrade.**

---

## BLOCKER: Database

### app/src/main/kotlin/com/gymcoach/app/data/local/database/GymCoachDatabase.kt

- `~L60-L78` — **entities array includes `MuscleGroupEnum::class`**
  - Severity: BLOCKER
  - Issue: `MuscleGroupEnum` is an `enum class`, not annotated `@Entity`. Room
    rejects non-entity types in the `entities` list of `@Database` at KAPT/KSP time.
  - Fix: Remove `MuscleGroupEnum::class` from the array. Enum is never a table
    (it is a value type / should be a `TypeConverter` if persisted anywhere).

- `~L60-L78` — **imports/reference to 12 DAO types that do not exist**
  - Severity: BLOCKER (compilation failure)
  - Issue: Imports declare `BodyMeasurementDao`, `EquipmentDao`,
    `ExerciseAliasDao`, `ExerciseEquipmentDao`, `ExerciseMuscleDao`,
    `ExerciseSubstitutionDao`, `FavoriteExerciseDao`, `MuscleDao`,
    `PersonalRecordDao`, `ProgramDao`, `ProgramDayDao`, `ProgramExerciseDao`.
    The `dao/` directory (`github_list` confirmed) contains **only**
    `ExerciseDao.kt` and `WorkoutDao.kt`. The `abstract fun xxxDao()` declarations
    reference 14 DAOs but only 2 compile. `ExerciseMuscleDao`/`ExerciseSubstitutionDao`
    are injected (see SubstitutionEngine.kt) yet absent.
  - Fix: Either implement all twelve `@Dao` interfaces in `dao/`, or remove the
    dangling imports/abstract methods. Cannot ship version 5 with 14 DAO methods.

- `~L109-L160` — **MIGRATION_4_5 creates tables that do NOT match current entities**
  - Severity: BLOCKER (runtime crash on upgrade)
  - Issue: `MIGRATION_4_5` issues `CREATE TABLE` whose column names and shapes
    differ from the `@Entity` schemas, e.g.:
    - `programs (id, name, description, goal, difficulty)` vs `ProgramEntity`
      (adds `user_id`, `split_type`, `duration_weeks`, `days_per_week`,
      `is_active`, `created_at`; FKs/indices missing).
    - `program_days (programId, dayOfWeek, name)` vs `ProgramDayEntity`
      (`program_id` column-name mismatch; `day_number`, `focus`,
      `is_rest_day`, FK, index missing).
    - `personal_records (userId, exerciseId, weight, reps, rpe, date)` vs
      `PersonalRecordEntity` (`user_id`, `weight_kg`, `one_rep_max_kg`,
      `achieved_at`, `notes`, FK, index missing).
    - `body_measurements (userId, date, weight, height, bmi, bodyFatPercent)`
      vs `BodyMeasurementEntity` (every column renamed/reshaped).
    - `muscles (name, primary)` vs `MuscleEntity` (`display_name`,
      `parent_muscle_id`, `body_region`, self-FK, index).
    - `equipment (name)` vs `EquipmentEntity` (`display_name`, `category`).
    - join tables `exercise_aliases/exercise_equipment/exercise_muscles/
      exercise_substitutions/favorite_exercises` use camelCase
      (`exerciseId`, `sourceExerciseId`…) while entities use snake_case
      (`exercise_id`, `original_exercise_id`…).
    - `user_profiles` is created but has **no corresponding `@Entity`** →
      table present in DB but absent from Room schema (double mismatch).
  - Room's `TableInfo` validation runs after migration; the delta is
    non-empty => `IllegalStateException: Migration ... did not properly handle`
    on every upgrading device, at first `open()`/`getOpenHelper()`.
  - Fix: Re-export `schemas/` from a v1→...→v5 incremental run and paste the
    exact `TableInfo` produced DDL into each migration (or delete migrations
    and ship `fallbackToDestructiveMigration` + clear data as intentional beta
    reset, documented).

- `~L104-L107`, `~L109-L160` / `~L79` — **migration chain is NOT contiguous**
  - Severity: BLOCKER (upgrade path)
  - Issue: only `MIGRATION_1_2` and `MIGRATION_4_5` are registered while
    `version = 5`. `MIGRATION_2_3` and `MIGRATION_3_4` are missing. A schema
    at v2 or v3 has no path to v5.
  - Fix: supply `MIGRATION_2_3` and `MIGRATION_3_4`, or document that < v4
    installs are wiped.

- `~L175` — **`fallbackToDestructiveMigration()` is enabled**
  - Severity: HIGH (policy) → BLOCKER in effect given B3/B4
  - Issue: with gaps at 2_3/3_4 and a broken 4_5, the library's fallback path
    fires for *every* upgrader and **deletes the entire DB**. No warning, no
    user opt-in.
  - Fix: remove `fallbackToDestructiveMigration()` (it silently masks migration
    bugs) once real migrations exist; if intentional for v1 beta, guard behind
    an explicit `version < X → clear()` branch and log.

- `~L178-L190` — **seeding lives inside `onCreate` via raw `INSERT`**
  - Severity: MEDIUM (design / idempotency)
  - Issue: `seedExercises` is a non-idempotent bulk `INSERT` (no `OR IGNORE`,
    no uniqueness on `exercises.name`). Runs on every fresh install only
    (Room calls `onCreate` once per DB file), so currently safe-by-accident,
    but diverges from the planned `ExerciseSeeder`/`MuscleSeeder`/
    `EquipmentSeeder` classes which do **not exist** (see Seeder section).
    Also ships `https://example.com/*.jpg` placeholder URLs as real data.
  - Fix: route seeding through dedicated seeders in a transaction; add
    `UNIQUE(name)` or `INSERT OR IGNORE`; replace placeholder URLs before ship.

- `~L80` — **`@Database(exportSchema = false)`**
  - Severity: MEDIUM (tooling / future risk)
  - Issue: schema snapshots are not checked in, so migration diffs are
    authored blind. The B3 mismatches above were invisible precisely because
    export is off.
  - Fix: set `exportSchema = true` and commit `schemas/<version>.json` per
    migration.

- `~L84-L97` — **14 `abstract fun xxxDao()` for only 2 DAOs present**
  - Severity: BLOCKER (duplicate of B1 above)
  - (See import/compile issue.)

---

## HIGH: Entities (schema integrity)

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/ExerciseEntity.kt
- `beginnerVariantId` / `advancedVariantId: Long? = null` (`~L34-L35`)
  - Severity: HIGH
  - Issue: self-references to `exercises.id` but **no `@ForeignKey` declared**.
    Room supports self-referencing FKs; dangling variant IDs are silently
    allowed.
  - Fix: add a `@ForeignKey(entity = ExerciseEntity::class, parentColumns=["id"],
    childColumns=["beginner_variant_id"/"advanced_variant_id"],
    onDelete=CASCADE)` and an `@Index`.

- no `UNIQUE` on `name` (`~L6`)
  - Severity: HIGH
  - Issue: duplicate exercise names permitted → seeder re-run (or a second
    plan→exercise copy) inserts duplicates; alias/FK lookups by name ambiguous.
  - Fix: add `indices = [Index(value=["name"], unique=true)]` to `@Entity`.

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/FavoriteExerciseEntity.kt
- no `UNIQUE(exercise_id, user_id)` (`~L11`)
  - Severity: HIGH
  - Issue: same exercise can be favorited multiple times.
  - Fix: declare `unique = true` on `@Index` combining both columns.

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/MuscleEntity.kt
- self-FK "not supported" comment is wrong; FK missing (`~L15-L16`)
  - Severity: MEDIUM→HIGH
  - Issue: comment states "Room does not support self-ref FK easily" — false;
    Room supports them natively. Without the FK, `parent_muscle_id` can point
    anywhere (integrity gap, no cascade delete of descendants).
  - Fix: add `@ForeignKey(entity = MuscleEntity::class, parentColumns=["id"],
    childColumns=["parent_muscle_id"], onDelete=CASCADE)`; keep the index.

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/ExerciseAliasEntity.kt, ExerciseEquipmentEntity.kt, ExerciseMuscleEntity.kt
- non-unique indices / no composite `UNIQUE` (`~L18`, ExerciseEquipment `~L15`, ExerciseMuscle no unique)
  - Severity: LOW→MED
  - Issue: alias text can repeat; `role` text not validated ("primary"/
    "secondary"/… free string); join tables allow duplicate (exercise,muscle)
    pairs.
  - Fix: make `alias` unique on ExerciseAliasEntity; add composite unique on
    ExerciseEquipmentEntity(exercise_id,equipment_id) and
    ExerciseMuscleEntity(exercise_id,muscle_id).

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/BodyMeasurementEntity.kt
- comment mismatch: `// in cm` above `weight_kg` (`~L10`)
  - Severity: LOW
  - Fix: comment.

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/WorkoutEntity.kt
- non-null `endTime`/`duration` force sentinels for in-progress workout (`~L6-L9`)
  - Severity: MEDIUM
  - Issue: `endTime=0L`, `duration=0L` are indistinguishable from an
    abandoned workout; analytics can't separate "live" vs "zeroed".
  - Fix: make nullable (`endTime: Long?`) or add a `status`/`state` column.
  - No `@ColumnInfo` / snake_case naming (`~L4-L9`)
  - Severity: LOW
  - Issue: only this entity mixes camelCase column names while all other tables
    are snake_case; inconsistent DB surface.
  - No index on `date` (`~L4`)
  - Severity: LOW
  - Issue: every history query `ORDER BY date DESC`.

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/WorkoutSetEntity.kt
- `rpe: Double` non-null, no default (`~L11`)
  - Severity: MEDIUM
  - Issue: RPE is optional in real training; forcing a value (often 0.0)
    corrupts analysis.
  - Fix: `rpe: Double? = null`.
- `setType: Int = 0` magic numbers (`~L13`)
  - Severity: LOW
  - Issue: `0=NORMAL,1=WARMUP,…` comment — no `CHECK` constraint / enum
    converter; typos store invalid ints.
  - Fix: `CHECK(setType IN (0,1,2,3))` or a `@TypeConverters` enum.

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/ProgramEntity.kt
- `isActive: Boolean` not uniquely enforced (`~L13`)
  - Severity: LOW/MEDIUM
  - Issue: multiple rows can be active simultaneously.
  - Fix: application-level guard in a transaction, or a partial unique index
    (SQLite `CREATE UNIQUE INDEX ... WHERE is_active = 1`) — Room 2.x can't
    emit it in `@Entity`; do it in migration.

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/PersonalRecordEntity.kt
- no dedupe / `oneRepMaxKg` stored (not derived) (`~L16`)
  - Severity: LOW/MEDIUM
  - Issue: storing a computed 1RM risks drift from `weight_kg`/`reps`; no
    uniqueness so a new PR for same rep-max can coexist.
  - Fix: derive via Epley formula at read; or store but recompute in `INSERT`.

### app/src/main/kotlin/com/gymcoach/app/data/local/entity/ExerciseEntity.kt — denormalization
- `isFavorite: Boolean` duplicates `favorite_exercises` table; `muscleGroup`/
  `equipment` strings duplicate `exercise_muscles`/`exercise_equipment` (`~L7-L8`)
  - Severity: MEDIUM
  - Issue: two sources of truth → drift. `getFilteredExercises` reads the
    denormalized strings (see DAO §).
  - Fix: pick canonical representation and migrate data out of the redundant
    columns.

---

## HIGH: DAOs (contract + query correctness)

### app/src/main/kotlin/com/gymcoach/app/data/local/dao/ExerciseDao.kt
- `OnConflictStrategy.REPLACE` on `@Insert` (`~L19`)
  - Severity: HIGH
  - Issue: `REPLACE` deletes the existing row **then** inserts. For
    `ExerciseEntity` (PK `id`), re-inserting an existing id cascades to
    `exercise_aliases`, `exercise_muscles`, `exercise_equipment`,
    `favorite_exercises`, `personal_records`, `exercise_substitutions`
    (all `onDelete=CASCADE`) — i.e. a "re-seed"/sync silently nukes the
    relationship graph. Same pattern on:
  - `WorkoutDao.insertWorkout/insertWorkoutExercise/insertWorkoutSet`
    (`~L25`,`~L40`,`~L56`) — same REPLACE⇒CASCADE-delete children risk.
  - Fix: `Insert(onConflict = REPLACE)` only where intentional; otherwise
    `ABORT`/`IGNORE` and rely on the separate `@Update`. At minimum document
    the cascade hazard.

- `getFilteredExercises` reads denormalized columns, ignoring join tables
  (`~L14-L19`)
  - Severity: MEDIUM (consistency)
  - Issue: filters on `muscleGroup`/`difficulty`/`equipment` string columns
    while the normalized graph (`exercise_muscles`, `exercise_equipment`)
    exists but is never queried here. Same exercise listed both ways can
    disagree.
  - Fix: pick canonical model; if joins intended, query them in the DAO.

- no alias/lookup-by-name query (`~L20`)
  - Severity: LOW (scope)
  - Issue: `ExerciseAliasEntity` exists but `ExerciseDao` exposes no
    "exercise by alias" read, so aliases are dead data.
  - Fix: `@Query("SELECT * FROM exercises WHERE id = (SELECT exercise_id FROM exercise_aliases WHERE alias = :alias LIMIT 1)")`.

### app/src/main/kotlin/com/gymcoach/app/data/local/dao/WorkoutDao.kt
- `getAllPersonalRecords` omits `completed = 1` filter (`~L100-L107`)
  - Severity: HIGH
  - Issue: selects from `workout_sets` joined through `workouts` with
    `WHERE 1=1` (i.e. no completion filter). Incomplete/abandoned workout sets
    are counted in PR list — inconsistent with `getPersonalRecordMax`
    (`~L90-L96`) which correctly asserts `w.completed = 1`. User-facing wrong
    data on a "records" screen.
  - Fix: add `AND w.completed = 1`.

- duplicate queries: `getLatestIncompleteWorkout` ≡ `getIncompleteWorkout`
  (`~L35`, `~L184`)
  - Severity: LOW (maintenance)
  - Issue: identical SQL (`ORDER BY date DESC LIMIT 1`, `completed = 0`),
    two method names. Drift risk.
  - Fix: drop one.

- `getCompletedWorkoutsByDurationDesc/Asc` and `*ByVolumeDesc/Asc` duplicate
  the base `getCompletedWorkoutsWithStats` query block (`~L130-L170`)
  - Severity: LOW
  - Issue: six hand-copied `SELECT w.*, SUM…` blocks; any schema change must
    be patched in six places.
  - Fix: keep one canonical `@Relation`/`@Transaction` query + sort in-memory.

- `getMonthlyVolumes` `GROUP BY strftime('%Y-%m')` but `SELECT w.date` bare
  column (`~L130-L137`)
  - Severity: MEDIUM
  - Issue: the returned `date` value comes from an arbitrary row in the group
    (SQLite picks the min `rowid`'s date), not a canonical month boundary →
    chart X labels skew by up to ~30 days.
  - Fix: `SELECT MIN(w.date) AS date, SUM(...) AS volume ... GROUP BY strftime('%Y-%m', w.date)`.

- `getAllWorkoutVolumes` groups by exact `w.date` millis (`~L80-L87`)
  - Severity: LOW/MEDIUM
  - Issue: two workouts in the same day but different milliseconds yield two
    volume points instead of one daily aggregate.
  - Fix: group by `strftime('%Y-%m-%d', w.date / 1000, 'unixepoch', 'localtime')`
    (respecting `date` is a millis UTC epoch).

- `getAverageWorkoutVolume` (`~L175`) computes `AVG(weight*reps)` per **set**
  but is named as if averaging **workout** volume.
  - Severity: LOW
  - Issue: semantically misleading metric; callers assume per-workout.
  - Fix: `AVG(volume)` over the per-workout grouped subquery, or rename.

- `getTotalSetsCount`/`getTotalRepsCount`/`getTotalVolumeSum` use an
  `INNER JOIN ... ON (worksets.workoutExerciseId IN (SELECT id FROM
  workout_exercises WHERE workoutId = workouts.id))` correlated subquery join
  (`~L145-L160`)
  - Severity: LOW
  - Issue: correct but O(n²)-ish and confusing; should be a plain three-way
    join. Also `getTotalRepsCount`/`getTotalVolumeSum` declare nullable
    return where COUNT/SUM could be coalesced.
  - Fix: `FROM workout_sets ws JOIN workout_exercises we ON ws.workoutExerciseId=we.id
    JOIN workouts w ON we.workoutId=w.id WHERE w.completed=1`.

- `searchWorkouts` `LIKE '%' || :query || '%'` (`~L150-L158`)
  - Severity: LOW
  - Issue: user input containing `%` or `_` acts as wildcard (UX, not SQLi
    since parameterized).
  - Fix: escape with `ESCAPE('\')` clause.

- no `@Transaction` writes spanning workout + exercises + sets (`~L1`)
  - Severity: MEDIUM
  - Issue: nothing guarantees an insert of a workout plus its exercises and
    sets is atomic; callers must orchestrate or risk partial writes.
  - Fix: add `@Transaction fun insertFullWorkout(...)`.

- no `@Relation`-based POJOs anywhere (`~entity`)
  - Severity: LOW/medium
  - Issue: all relationships fetched via manual JOIN; acceptable but verbose
    and error-prone (the six duplicated blocks above result).
  - Fix: introduce `@Relation` + `@Transaction` holders for
    `WorkoutWithExercisesAndSets` to de-duplicate.

---

## BLOCKER: Domain code

### app/src/main/kotlin/com/gymcoach/app/core/exercise/SubstitutionEngine.kt
- `exerciseDao.getById(exerciseId)` returns `Flow<ExerciseEntity?>` but is
  used as `val original = exerciseDao.getById(exerciseId) ?: return emptyList()`
  inside a `suspend` (`~L30`)
  - Severity: BLOCKER (compile)
  - Issue: comparing a `Flow` reference for null; then `original.muscleGroup`
    on a `Flow` — type mismatch. Must collect first or DAO should expose a
    `suspend fun getByIdNow(id): ExerciseEntity?`.
  - Fix: `val original = exerciseDao.getByIdSync(exerciseId) ?: return emptyList()`,
    or `.firstOrNull()` on the Flow.

- `exerciseSubstitutionDao.getByExerciseId(exerciseId)` and
  `sub.substituteId` (`~L32-L33`, `~L38`)
  - Severity: BLOCKER (compile)
  - Issue: `ExerciseSubstitutionEntity` has `substituteExerciseId`, not
    `substituteId`; method `getByExerciseId` does not exist on any
    `ExerciseSubstitutionDao` (the interface is absent entirely — see Database
    imports). Compiles only against a DAO that is not checked in.
  - Fix: define `ExerciseSubstitutionDao.getByExerciseId` returning the
    substitution entities; reference `substituteExerciseId`.

- `exerciseDao.getAll().filter { … }` (`~L41`)
  - Severity: BLOCKER (logic)
  - Issue: `getAll()` returns `Flow<List<…>>`; `.filter` operates on the Flow's
    *type* not on emitted lists — never compiles as intended. Should be
    `getAll().map { list -> list.filter { … } }.first()` or a dedicated
    `suspend fun getByMuscleGroup(...)`.
  - Fix: add the DAO query; do not branch logic in the engine.

- unused `exerciseMuscleDao` dependency (`~L19`)
  - Severity: LOW
  - Issue: injected but never referenced (dead dependency / DI surface).

---

## Seeder audit — ExerciseSeeder / MuscleSeeder / EquipmentSeeder

**Verdict: the three planned seeder classes DO NOT EXIST.**

- `github_search_code` for `ExerciseSeeder`, `MuscleSeeder`, `EquipmentSeeder`
  across the repo: 0 hits (incomplete_results:true, total_count:0).
- `core/exercise/` directory listing: contains only `SubstitutionEngine.kt`.
- Therefore: transactional wrapping, seed-version tracking, idempotency, and
  error handling for the *planned* seeders are all N/A / absent.

The only seeding that exists is the inline `GymCoachDatabase.create() →
seedExercises(db)` callback (`~L178-L206`):
- transactional wrapping — Room runs `onCreate` on the IO executor inside the
  `INSERT` statements' own implicit transaction; acceptable, but not explicit
  `@Transaction` and no rollback on partial failure.
- seed version tracking — **absent** (no `schema_version`/seed_version table;
  seeder cannot be re-run after a migration).
- idempotency — **not safe** across future migrations: `INSERT` (not
  `INSERT OR IGNORE`) has no guard; currently tolerable only because
  `onCreate` fires once, but coupled to the missing `UNIQUE(name)` on
  `ExerciseEntity`, a re-seed duplicates rows.
- error handling — **absent**; a single bad seed row (e.g. wrong column count)
  throws `SQLiteException` which surfaces on first DB access as an app crash.
  The 21-row seed array is also hardcoded inline in `Database.kt`, violating
  the planned seeder separation and making column-count drift invisible until
  runtime.

**Required**: extract seeding into `ExerciseSeeder`/`MuscleSeeder`/
`EquipmentSeeder` under `core/exercise/`, wrap each run in `@Transaction`,
introduce a `seed_metadata(key,value)` table for version tracking, switch to
`INSERT OR IGNORE` gated by `UNIQUE` constraints, and fail fast+logged on
constraint violations rather than crashing on first open.

---

## Cross-cutting: environment / concurrency / edge cases (challenge checklist)

1. **Implicit env assumption — single user.** `userId: Long = 1` is hardcoded
   on `BodyMeasurementEntity`, `FavoriteExerciseEntity`, `PersonalRecordEntity`,
   `ProgramEntity`, and (per migration) `user_profiles.userId` is `TEXT`.
   Multi-user is impossible today; `Migration_4_5` even mixes `INTEGER` and
   `TEXT` userId types. BLOCKER for any auth/multi-profile roadmap.
2. **TZ / day boundary.** `getWorkoutsTodayCount(todayStart: Long)` expects
   the caller to compute the epoch-millis start-of-day. If caller treats
   `System.currentTimeMillis()` as local-day midnight without an explicit
   `java.util.TimeZone`, DST transitions shift the bucket (spring-forward drops
   an hour, fall-back double-counts). Document contract or use
   `java.time` `LocalDate.atStartOfDay(zone).toInstant()` at call site.
   Severity: MEDIUM.
3. **Concurrency / Flow vs suspend.** Flows are cold/live; the long-running
   analytics `suspend` calls (`getTotal*`, `getAverage*`) are **not** wrapped
   in `@Transaction`, so a concurrent `@Insert` can change COUNT/SUM mid-flight
   (torn read). Low practical blast radius (stats are approximate anyway) but
   `getPersonalRecordMax` racing an `insertWorkoutSet` can return stale PR.
   Severity: LOW/MEDIUM — wrap read-only stat bundles in `@Transaction` when
   they must be consistent with each other.
4. **Numeric validation.** No `CHECK(weight >= 0)`, `CHECK(reps >= 0)`,
   `CHECK(restSeconds >= 0)`, `CHECK(duration >= 0)` anywhere; malformed
   inserts silently persist `-1` reps. Severity: MEDIUM (trust-boundary gap).
5. **REPLACE-insert data-loss trap (restate).** The `OnConflictStrategy.REPLACE`
   rows in both DAOs are the sharp edge: a sync/resync of one exercise or one
   workout row cascades-deletes its entire child graph without warning.
   Severity: HIGH (re-stated here as the top data-safety finding).

---

## Summary table

| # | File | Severity | Issue |
|---|------|----------|-------|
| B1 | GymCoachDatabase.kt | BLOCKER | `MuscleGroupEnum` registered as entity |
| B2 | GymCoachDatabase.kt | BLOCKER | 12 of 14 DAO interfaces missing from repo |
| B3 | GymCoachDatabase.kt | BLOCKER | `MIGRATION_4_5` schema ≠ entity schema ⇒ crash on open |
| B4 | GymCoachDatabase.kt | BLOCKER | missing `MIGRATION_2_3`, `MIGRATION_3_4` |
| B5 | SubstitutionEngine.kt | BLOCKER | Flow-as-Entity + `substituteId` field + missing DAO methods |
| H1 | ExerciseDao.kt / WorkoutDao.kt | HIGH | `REPLACE` inserts cascade-delete children |
| H2 | WorkoutDao.kt | HIGH | `getAllPersonalRecords` counts incomplete workouts |
| H3 | ExerciseEntity.kt | HIGH | self-FK variants not declared |
| H4 | ExerciseEntity / FavoriteExerciseEntity | HIGH | no uniqueness on names/favorites |
| M1 | DB.create() | MEDIUM | inline seeding, no seed version, example.com URLs |
| M2 | WorkoutEntity.kt | MEDIUM | non-null endTime/duration for in-progress |
| M3 | WorkoutDao.kt | MEDIUM | `getMonthlyVolumes` bare-column date |
| M4 | ExerciseEntity.kt | MEDIUM | denormalized muscleGroup/equipment/isFavorite dual-truth |
| M5 | DAOs | MEDIUM | no `@Transaction` multi-table writes; no `@Relation` POJOs |
| M6 | WorkoutDao.kt | MEDIUM | duplicated stat query templates ×6 |
