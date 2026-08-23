# GymCoach Phase 3 Production Verification Baseline

**Repository:** vishalm111266-beep/GymCoach  
**Current Branch:** main (SHA: 06255f36d9cba2f78bdbf7d2f8022ba274bb8e08)  
**Phase 3 Branch:** phase3/p0-stabilization (SHA: 847752fdc9700927742a5dcb48f0254ef53fe5ab)  
**Phase 2 Branch:** phase2/production-hardening (SHA: b514938380f81752ab84f80d938b884058730aa5)  
**PR #10:** "Phase 3: P0 stabilization" — Open, draft, on phase3/p0-stabilization  
**PR #11:** "Phase 2: Compilation fixes, navigation, profile, tests" — Open, draft, on phase2/production-hardening  

---

## Repository State

### Branches
| Branch | SHA | Status |
|--------|-----|--------|
| main | 06255f36d9cba2f78bdbf7d2f8022ba274bb8e08 | Default branch |
| phase3/p0-stabilization | 847752fdc9700927742a5dcb48f0254ef53fe5ab | PR #10 head |
| phase2/production-hardening | b514938380f81752ab84f80d938b884058730aa5 | PR #11 head |
| repair/restore-build | adf291503b2b419f2a4a3318e291cff37ced6f30 | Restore build |
| research/product-benchmark | 3263e87126a3290dfa078dbe5da5dc5e1e238982 | Research |
| +11 more (audit/*, feature/*, fix/*) | — | Various feature/fix branches |

### Open PRs
- **PR #10** — Phase 3 P0 stabilization gate. Verifies next release baseline with real Gradle verdict (build + unit tests + lint). Has known compile blockers marked RED (intentional). Needs conflict resolution on `GymCoachDatabase.kt` and fixes for ExerciseSubstitutionDao, VolumeCalculator.
- **PR #11** — Phase 2 Production Hardening. Fixes column name mismatches (ExerciseSubstitutionDao), VolumeCalculator (SetWithContext), BodyMeasurementDao (ORDER BY recorded_at), navigation wiring, ProfileScreen/ProfileViewModel, AppModule Hilt DI. Has VolumeCalculatorTest (10 tests) and PRDetectorTest (8 tests).

---

## Database Schema (Room)

### Database Version
- **@Database(version = 5, exportSchema = false)**
- Entities: 17 (ExerciseEntity, WorkoutEntity, WorkoutExerciseEntity, WorkoutSetEntity, ProgramEntity, ProgramDayEntity, ProgramExerciseEntity, PersonalRecordEntity, BodyMeasurementEntity, FavoriteExerciseEntity, ExerciseSubstitutionEntity, MuscleEntity, EquipmentEntity, ExerciseMuscleEntity, ExerciseEquipmentEntity, ExerciseAliasEntity, UserProfileEntity)

### Defined Migrations
| Migration | SQL | Status |
|-----------|-----|--------|
| MIGRATION_1_2 | Creates workouts, workout_exercises, workout_sets tables | ✅ Defined in @Database |
| MIGRATION_2_3 | Creates programs, program_days, program_exercises, personal_records, body_measurements, favorite_exercises, exercise_substitutions, muscles, equipment, exercise_muscles, exercise_equipment, exercise_aliases, FTS4 virtual table | ✅ Defined in @Database |

### Missing Migrations
| Required | Status |
|----------|--------|
| MIGRATION_3_4 | ❌ NOT DEFINED |
| MIGRATION_4_5 | ❌ NOT DEFINED |

### Critical Issues
- `fallbackToDestructiveMigration()` is set in Database creation — **destructive on upgrade**, data loss risk
- `exportSchema = false` — schema debugging difficult, no `.json` files generated
- No migration test coverage for 3→4 or 4→5 schema transitions
- Entity schema may not fully match migration SQL (e.g., BodyMeasurementEntity column name mismatches with migration)

### Entity-to-Table Matrix (Key Entities)
| Entity | Table | SQL Type | Nullable | Primary Key | Notes |
|--------|-------|----------|----------|-------------|-------|
| ExerciseEntity | exercises | — | varies | id (auto) | Has vtaper columns, movement_pattern, image_url, video_url, animation_url, setup_execution_breathing_instructions, beginner/advanced variant IDs |
| WorkoutEntity | workouts | — | varies | id (auto) | date, startTime, endTime, duration, notes, completed |
| WorkoutSetEntity | workout_sets | — | varies | id (auto) | workoutExerciseId FK, setType (0=NORMAL, 1=WARMUP, 2=DROP, 3=FAILURE) |
| BodyMeasurementEntity | body_measurements | — | most columns nullable | id (auto) | recorded_at (NOT null), weight_kg (NOT null), others nullable |
| ProgramEntity | programs | — | varies | id (auto) | user_id, name, description, split_type, duration_weeks, days_per_week, difficulty, goal, is_active, created_at |
| ProgramDayEntity | program_days | — | varies | id (auto) | program_id FK, day_number, name, focus, is_rest_day |
| ExerciseSubstitutionEntity | exercise_substitutions | — | varies | id (auto) | original_exercise_id FK, substitute_exercise_id FK, reason |

---

## Critical Compile Blockers (PR #10 - phase3/p0-stabilization)

### 1. ExerciseSubstitutionDao — Nonexistent Columns
- **Issue:** Queries reference `exercise_id`, `substitute_id`, `preservation_score` columns that don't exist in the `exercise_substitutions` table
- **Actual table columns:** `id`, `original_exercise_id`, `substitute_exercise_id`, `reason`, `createdAt`
- **Fix (in PR #11):** Renamed queries to use `original_exercise_id` and `substitute_exercise_id`, removed `preservation_score` ORDER BY
- **Status:** Fix landed in PR #11 (phase2/production-hardening), not yet in PR #10

### 2. VolumeCalculator — Nonexistent Field References
- **Issue:** References `set.date` and `set.exerciseId` absent from `WorkoutSetEntity`
- **Actual WorkoutSetEntity fields:** `id`, `workoutExerciseId`, `setNumber`, `weight`, `reps`, `rpe`, `restSeconds`, `completed`, `setType`
- **Fix (in PR #11):** Replaced with `SetWithContext` data class carrying `(set, exerciseId, workoutDate)`
- **Status:** Fix landed in PR #11, not yet in PR #10

### 3. Duplicate ProgressViewModel
- **Issue:** Suspected duplicate class definitions causing compilation failure
- **Status:** Investigated in PR #11; resolution noted

---

## Test Coverage

| Test Suite | Tests Reported | Status |
|------------|----------------|--------|
| VolumeCalculatorTest | 10 tests | ⚠️ Previous report admits "suspicious/broken assertions" — needs verification |
| PRDetectorTest | 8 tests | ⚠️ Previous report said "8 tests"; actual file needs counting |
| Repository unit tests | 7 tests | ⚠️ Minimal coverage, no device validation |
| No workout persistence tests | 0 | ❌ Admitted missing |
| No ProgramGenerator tests | 0 | ❌ Admitted missing |

---

## CI/CD Pipeline

| Pipeline | Status |
|----------|--------|
| GitHub Actions `.github/workflows/android-build.yml` | Present but lint job is a no-op (echo-only); actual `./gradlew lint` not configured |
| `./gradlew assembleDebug` | Works (per run_log.txt), but environment lacks Android SDK / JDK |
| `./gradlew testDebugUnitTest` | Available but unexecuted in this environment |
| Lint/Unit Test/CI verification | **UNVERIFIED** — environment blocker (no Android SDK/JDK) |

---

## Security / Data Audit

| Area | Finding | Severity |
|------|---------|----------|
| H6 — Unencrypted Room DB | Room database stores body measurements, progress data locally without encryption | MEDIUM |
| H7 — Signing configuration | Release APK is unsigned; only debug artifact produced | MEDIUM |
| Data exposure | No internet permission; all data local-only; no auth flows | LOW |
| Secrets | No API keys, passwords, tokens found in source | LOW |

---

## Known Issues (Deferred, from PR #11)

1. **ProgramDayEntity.focus vs migration `target_muscles` column mismatch** — runtime issue, not compilation
2. **BodyMeasurementEntity schema vs MIGRATION_4_5 table** — runtime mismatch, same root cause as missing migrations
3. **PR #10 takes different VolumeCalculator approach** — needs reconciliation with PR #11's SetWithContext fix
4. **`Color.kt` reconciliation** — `feature/design-system` (#1) has canonical file; branches created independent copies

---

## Production Gate Status (Pre-Fix)

| Gate | Status | Evidence |
|------|--------|----------|
| Repository identity | VERIFIED | vishalm111266-beep/GymCoach, private Kotlin repo |
| PR #10 reviewed | UNVERIFIED | Not yet reviewed with actual evidence |
| PR #11 reviewed | UNVERIFIED | Not yet reviewed with actual evidence |
| Current SHA verified | VERIFIED | 06255f36d9cba2f78bdbf7d2f8022ba274bb8e08 (main) |
| Clean architecture reconciliation | PARTIAL | Entities, DAOs, repository layer present but migration gaps |
| Entity/DAO alignment | PARTIAL | Some column name mismatches fixed in PR #11, remaining in PR #10 |
| Migration tests | ❌ FAIL | No migration tests defined (MIGRATION_3_4, MIGRATION_4_5 missing) |
| VolumeCalculator tests | ⚠️ UNVERIFIED | 10 tests but assertions reported as broken |
| PRDetector tests | ⚠️ UNVERIFIED | 8 tests, actual count needs verification |
| Workout persistence tests | ❌ FAIL | 0 tests exist |
| ProgramGenerator tests | ❌ FAIL | 0 tests exist |
| Navigation verification | ⚠️ UNVERIFIED | Wired in PR #11, needs end-to-end validation |
| Lint | ⚠️ UNVERIFIED | No Android SDK in this environment |
| Unit tests | ⚠️ UNVERIFIED | No Android SDK in this environment |
| Android build | ⚠️ UNVERIFIED | No Android SDK/JDK in this environment |
| CI | ❌ BLOCKED | Environment blocker (no Android SDK/JDK) |
| Security audit | PARTIAL | H6/H7 identified, no secrets found |
| Seed idempotency | UNVERIFIED | Not tested |
| Release configuration | ❌ FAIL | Unsigned release APK, versionName `0.1.0` not `1.0.0` |
| Adversarial review | ❌ NOT DONE | No independent review conducted |
| Review A | ❌ NOT DONE | Not conducted |
| Review B | ❌ NOT DONE | Not conducted |

---

## Summary

The repository is in an **active development state** with two open PRs (#10, #11) that address compilation fixes and P0 stabilization. However:

1. **Build/CI cannot be verified** in this environment (no Android SDK/JDK)
2. **Migration gaps exist** — MIGRATION_3_4 and MIGRATION_4_5 are not defined
3. **Critical compile blockers** from PR #10 (ExerciseSubstitutionDao column references, VolumeCalculator field references) are fixed in PR #11 but not yet merged
4. **Test coverage is minimal** — no workout persistence tests, no ProgramGenerator tests, VolumeCalculatorTest assertions reported as broken
5. **Security findings** — unencrypted Room DB, unsigned release builds
6. **No adversarial review** has been conducted

**Current classification: NOT PRODUCTION-READY** — build verification blocked, migration gaps, insufficient test coverage, security findings unresolved.