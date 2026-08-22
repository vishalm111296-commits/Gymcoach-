# GymCoach Implementation Plan

**Phase:** Foundation
**Date:** 2026-08-22
**Spec:** docs/GYMCOACH_MASTER_SPEC.md

---

## Dependency Graph

```
Week 1: DATA LAYER (ws1-1, ws1-2, ws1-3)
    ↓
Week 1-2: EXERCISE SYSTEM (ws2-1, ws2-2, ws2-3) — parallel with domain models
    ↓
Week 2-3: PROGRAM ENGINE (ws3-1, ws3-2, ws3-3, ws3-4) — depends on data layer
    ↓
Week 3-4: ONBOARDING + HOME (ws4-1, ws4-2, ws4-3) — depends on program engine
    ↓
Week 4-5: TESTING (ws5-1, ws5-2, ws5-3) — continuous, but final pass at end
```

**Parallelization Opportunities:**
- ws2-1, ws2-2, ws2-3 can run in parallel (exercise system components)
- ws3-1, ws3-2, ws3-3, ws3-4 can run in parallel (program engine components)
- ws4-1, ws4-2, ws4-3 can run in parallel (UI components)
- ws5-1, ws5-2, ws5-3 can run in parallel (test components)

---

## Workstream 1: DATA LAYER

### ws1-1: Define Entities
**Files:** `app/src/main/kotlin/com/gymcoach/app/data/local/entity/`
**Task:** Create new entities and extend ExerciseEntity

**New Entities:**
- UserProfileEntity (goal, experience, age, sex, height, weight, training_days, session_length, equipment, preferred_exercises, exercises_to_avoid)
- ProgramEntity (name, description, goal, frequency, created_at)
- ProgramDayEntity (program_id, day_number, name, target_muscles)
- ProgramExerciseEntity (program_day_id, exercise_id, order_index, target_sets, target_reps_min, target_reps_max, target_rpe, rest_seconds)
- PersonalRecordEntity (exercise_id, weight, reps, estimated_1rm, volume, date, workout_id)
- BodyMeasurementEntity (date, weight, waist, chest, shoulders, arm, thigh, body_fat_estimate, photo_url)
- FavoriteExerciseEntity (user_id, exercise_id, added_at)
- ExerciseSubstitutionEntity (exercise_id, substitute_id, preservation_score)
- MuscleEntity (id, name, parent_id, v_taper_relevance)
- EquipmentEntity (id, name, category, gym_available, home_available)
- ExerciseMuscleEntity (exercise_id, muscle_id, role)
- ExerciseEquipmentEntity (exercise_id, equipment_id, role)
- ExerciseAliasEntity (exercise_id, alias)
- MuscleGroupEnum (enum for all muscle groups)

**Extended ExerciseEntity:**
- vtaper_lat: Int (0-10)
- vtaper_lateral_delt: Int (0-10)
- vtaper_upper_chest: Int (0-10)
- vtaper_rear_delt: Int (0-10)
- movement_pattern: String
- image_url: String?
- video_url: String?
- animation_url: String?
- setup_instructions: String
- execution_instructions: String
- breathing_instructions: String
- tempo_guidance: String
- common_mistakes: String
- beginner_variant_id: Long?
- advanced_variant_id: Long?

**Acceptance Criteria:**
- [ ] All entities compile without errors
- [ ] Relationships defined with foreign keys
- [ ] Cascade delete where appropriate
- [ ] Indexes on frequently queried columns

---

### ws1-2: Create Room DAOs
**Files:** `app/src/main/kotlin/com/gymcoach/app/data/local/dao/`
**Task:** Write queries for new entities

**New DAOs:**
- UserProfileDao (insert, update, get, getByGoal)
- ProgramDao (insert, update, delete, getAll, getById, getActiveProgram)
- ProgramDayDao (insert, update, delete, getByProgramId)
- ProgramExerciseDao (insert, update, delete, getByDayId)
- PersonalRecordDao (insert, getByExerciseId, getAll, getLatestPR)
- BodyMeasurementDao (insert, update, getAll, getLatest, getTrend)
- FavoriteExerciseDao (insert, delete, getAll, isFavorite)
- ExerciseSubstitutionDao (getSubstitutes, getByExerciseId)
- MuscleDao (getAll, getByGroup, getVtaperRelevant)
- EquipmentDao (getAll, getByCategory, getGymAvailable, getHomeAvailable)
- ExerciseMuscleDao (getByExerciseId, getByMuscleId)
- ExerciseEquipmentDao (getByExerciseId, getByEquipmentId)
- ExerciseAliasDao (getByExerciseId, search)

**Extended WorkoutDao:**
- getWorkoutStats(from, to)
- getExerciseHistory(exerciseId)
- getMuscleVolumeWeekly(from, to)
- getWeeklyFrequency(from, to)

**Acceptance Criteria:**
- [ ] All DAOs compile without errors
- [ ] Queries return correct types
- [ ] Flow emissions for reactive queries
- [ ] Pagination support where needed

---

### ws1-3: Write v3 Migration
**Files:** `app/src/main/kotlin/com/gymcoach/app/data/local/migration/`
**Task:** SQL script to update SQLite DB structure

**Migration Steps:**
1. Create new tables (user_profiles, programs, program_days, program_exercises, personal_records, body_measurements, favorite_exercises, exercise_substitutions, muscles, equipment, exercise_muscles, exercise_equipment, exercise_aliases)
2. Extend exercises table with new columns
3. Create FTS4 virtual table for exercise search
4. Seed muscle and equipment taxonomies
5. Backfill existing exercises with new metadata

**Acceptance Criteria:**
- [ ] Migration runs without errors
- [ ] Existing data preserved
- [ ] New tables created correctly
- [ ] Foreign keys validated

---

## Workstream 2: EXERCISE SYSTEM

### ws2-1: Implement JSON Seeder
**Files:** `app/src/main/kotlin/com/gymcoach/app/core/exercise/ExerciseSeeder.kt`
**Task:** Parse and insert 200+ exercises from JSON assets

**Exercise Categories (200+ total):**
- Chest: 20 exercises
- Back: 25 exercises
- Lats: 15 exercises
- Shoulders: 20 exercises
- Biceps: 15 exercises
- Triceps: 15 exercises
- Quadriceps: 20 exercises
- Hamstrings: 15 exercises
- Glutes: 15 exercises
- Calves: 10 exercises
- Core: 20 exercises

**JSON Structure per Exercise:**
```json
{
  "name": "Barbell Bench Press",
  "slug": "barbell-bench-press",
  "description": "Compound chest exercise...",
  "category": "Chest",
  "difficulty": "Intermediate",
  "muscles": [
    {"name": "Pectoralis Major", "role": "primary"},
    {"name": "Anterior Deltoid", "role": "secondary"},
    {"name": "Triceps Brachii", "role": "secondary"}
  ],
  "equipment": [
    {"name": "Barbell", "role": "primary"},
    {"name": "Bench", "role": "primary"}
  ],
  "vtaper": {"lat": 0, "lateral_delt": 0, "upper_chest": 8, "rear_delt": 0},
  "movement_pattern": "compound_push",
  "instructions": {
    "setup": "...",
    "execution": "...",
    "breathing": "...",
    "tempo": "2-0-1-0",
    "common_mistakes": "..."
  },
  "aliases": ["bench press", "flat bench", "barbell bench"],
  "tags": ["chest", "compound", "push", "barbell"]
}
```

**Acceptance Criteria:**
- [ ] 200+ exercises seeded on first launch
- [ ] All muscle groups represented
- [ ] All equipment types represented
- [ ] V-taper relevance scores assigned
- [ ] Search aliases work correctly

---

### ws2-2: Create FTS4 Search
**Files:** `app/src/main/kotlin/com/gymcoach/app/core/exercise/ExerciseSearchEngine.kt`
**Task:** Full-text search with filters

**Search Features:**
- Full-text search on name, aliases, tags
- Filters: muscle group, equipment, difficulty, movement pattern, V-taper relevance
- Sort: name, difficulty, V-taper score
- Pagination: 30 results per page
- Favorites and recently used tracking

**Acceptance Criteria:**
- [ ] Search returns relevant results
- [ ] Filters narrow results correctly
- [ ] Pagination works
- [ ] Performance: <100ms for any query

---

### ws2-3: Implement Substitution Engine
**Files:** `app/src/main/kotlin/com/gymcoach/app/core/exercise/SubstitutionEngine.kt`
**Task:** Filter and rank exercise swaps

**Substitution Rules:**
- Preserve training intent (same muscle group, similar movement pattern)
- Respect available equipment
- Rank by: muscle overlap, equipment match, difficulty match
- User can override substitutions

**Example Substitutions:**
- Barbell Bench Press → Dumbbell Bench Press → Machine Chest Press → Push-up
- Lat Pulldown → Pull-up → Assisted Pull-up → Band Pulldown
- Lateral Raise → Cable Lateral Raise → Machine Lateral Raise → Dumbbell Lateral Raise

**Acceptance Criteria:**
- [ ] Substitutions preserve muscle targeting
- [ ] Equipment constraints respected
- [ ] Ranking makes sense
- [ ] User can override

---

## Workstream 3: PROGRAM ENGINE

### ws3-1: Build Program Generator
**Files:** `app/src/main/kotlin/com/gymcoach/app/core/program/ProgramGenerator.kt`
**Task:** Select exercises matching profile split and equipment

**Algorithm:**
1. Determine split based on frequency (3=Full Body, 4=Upper/Lower, 5=PPL/Upper/Lower, 6=PPL×2)
2. For each day, select exercises based on:
   - Target muscle groups for that day
   - Available equipment
   - V-taper relevance (higher score = more likely selected)
   - Difficulty level matching user experience
   - Movement pattern balance (compound before isolation)
3. Assign target sets, reps, and RPE
4. Assign rest periods

**Acceptance Criteria:**
- [ ] Program matches user profile
- [ ] Volume distributed correctly across week
- [ ] Each muscle trained ≥2×/week
- [ ] V-taper priority muscles get more volume

---

### ws3-2: Build Volume Tracker
**Files:** `app/src/main/kotlin/com/gymcoach/app/core/program/VolumeCalculator.kt`
**Task:** Compute set counts, allocate direct/indirect credits

**Volume Calculation:**
- Primary muscle: 1.0 credit per set
- Secondary muscle: 0.5 credit per set
- Stabilizer muscle: 0.25 credit per set
- Weekly volume = sum of all credits per muscle group

**Acceptance Criteria:**
- [ ] Volume calculated correctly
- [ ] Primary/secondary/stabilizer weighted properly
- [ ] Weekly totals accurate

---

### ws3-3: Build Progression Rules
**Files:** `app/src/main/kotlin/com/gymcoach/app/core/progression/ProgressionEngine.kt`
**Task:** Calculate target overload increments from RIR inputs

**Progression Logic:**
- Double progression: when all sets hit top of rep range → increase load
- Load increase: +2-5 kg (or 5-10%)
- Regression: if reps fall below minimum for 2 sessions → reduce load
- Stalling: if progress stalls 3+ weeks → suggest variation

**Acceptance Criteria:**
- [ ] Load recommendations are mathematically correct
- [ ] Regression detected correctly
- [ ] Stalling detected correctly

---

### ws3-4: Build PR Detector
**Files:** `app/src/main/kotlin/com/gymcoach/app/core/progression/PRDetector.kt`
**Task:** Flag sets exceeding personal best

**PR Types:**
- Weight PR: heaviest weight for exercise
- Rep PR: most reps at given weight
- Estimated 1RM PR: highest e1RM (Epley formula)
- Volume PR: highest single-session volume
- Best Set: highest e1RM from single set

**Acceptance Criteria:**
- [ ] PRs mathematically proven from actual data
- [ ] No false positives
- [ ] PR history tracked

---

## Workstream 4: ONBOARDING + HOME

### ws4-1: Create Onboarding Screens
**Files:** `app/src/main/kotlin/com/gymcoach/app/presentation/onboarding/`
**Task:** Collect gender, age, height, split, gear preferences

**Onboarding Steps:**
1. Goals (V-taper, muscle gain, strength, etc.)
2. Experience level (beginner/intermediate/advanced)
3. Personal info (age, sex, height, weight)
4. Training schedule (days/week, session length)
5. Equipment (gym/home/custom)
6. Preferences (preferred exercises, exercises to avoid)

**Acceptance Criteria:**
- [ ] All required data collected
- [ ] Validation on each step
- [ ] Data stored in UserProfileEntity
- [ ] Program generated after completion

---

### ws4-2: Create Home Dashboard
**Files:** `app/src/main/kotlin/com/gymcoach/app/presentation/home/`
**Task:** Draw weekly completion meters, display next workout

**Dashboard Elements:**
- Today's workout (name, target muscles, duration, start button)
- Last session (date, stats, view details)
- Training streak (current streak, recent consistency)
- Recent PR (latest personal record)

**Acceptance Criteria:**
- [ ] Shows today's workout if available
- [ ] Shows last session if exists
- [ ] Shows training streak
- [ ] Shows recent PRs
- [ ] Empty states guide users

---

### ws4-3: Create Empty State UI
**Files:** `app/src/main/kotlin/com/gymcoach/app/presentation/components/`
**Task:** Show friendly layout when history or library is empty

**Empty States:**
- No workouts: "Your training history will appear here after your first completed workout." + [START FIRST WORKOUT]
- No program: "Complete onboarding to generate your personalized program." + [COMPLETE ONBOARDING]
- No exercises: "Exercise library loading..." or "No exercises match your filters."
- No PRs: "Personal records will appear after your first workout."

**Acceptance Criteria:**
- [ ] Every empty state has explanation + CTA
- [ ] No meaningless zeros displayed
- [ ] CTAs lead to correct screens

---

## Workstream 5: TESTING

### ws5-1: Add Core Unit Tests
**Files:** `app/src/test/`
**Task:** Cover progression calculations, seeder integrity, volume logic

**Test Categories:**
- ProgressionEngine tests (load increase, regression, stalling)
- VolumeCalculator tests (primary/secondary/stabilizer weighting)
- PRDetector tests (weight PR, rep PR, e1RM PR)
- ProgramGenerator tests (volume distribution, exercise selection)
- SubstitutionEngine tests (preserving intent, equipment constraints)

**Acceptance Criteria:**
- [ ] All unit tests pass
- [ ] Edge cases covered (zero, overflow, negative)
- [ ] No mocked data in calculations

---

### ws5-2: Add Room Migration Tests
**Files:** `app/src/androidTest/`
**Task:** Execute v2 to v3 upgrades on mock DBs

**Test Scenarios:**
- Fresh install (v3)
- Migration from v2 to v3
- Migration preserves existing data
- New tables created correctly
- Foreign keys validated

**Acceptance Criteria:**
- [ ] All migration tests pass
- [ ] No data loss
- [ ] Schema validated

---

### ws5-3: Add Compose UI Tests
**Files:** `app/src/androidTest/`
**Task:** Verify flow from onboarding options to dashboard load

**Test Scenarios:**
- Onboarding completion triggers program generation
- Home dashboard shows today's workout
- Exercise search returns results
- Workout session logs sets correctly

**Acceptance Criteria:**
- [ ] All UI tests pass
- [ ] Critical user flows covered
- [ ] No crashes

---

## Integration Order

1. **Data Layer (ws1-1, ws1-2, ws1-3)** — Foundation
2. **Exercise System (ws2-1, ws2-2, ws2-3)** — Can parallel with domain models
3. **Program Engine (ws3-1, ws3-2, ws3-3, ws3-4)** — Depends on data layer
4. **Onboarding + Home (ws4-1, ws4-2, ws4-3)** — Depends on program engine
5. **Testing (ws5-1, ws5-2, ws5-3)** — Continuous, final pass at end

**Merge Strategy:**
- Each workstream on isolated branch
- PR review before merge
- Integration tests after merge
- Build verification after each merge

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Main thread lock during seeder | App freeze | Run seeder in background dispatcher |
| Migration crash on older schemas | Data loss | Export schema, write migration tests |
| Exercise data accuracy | Wrong programming | Peer-reviewed sources, manual verification |
| Performance degradation | Slow app | Indexes, pagination, lazy loading |
| Scope creep | Delayed delivery | Strict adherence to foundation phase spec |

---

## Success Criteria

- [ ] Database migration preserves all existing data
- [ ] 200+ exercises seeded with structured metadata
- [ ] Exercise search works with filters
- [ ] Program generates based on user profile
- [ ] Progressive overload provides load recommendations
- [ ] PRs are mathematically proven from actual data
- [ ] Onboarding collects necessary data
- [ ] Home dashboard shows today's workout
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] No fabricated data
- [ ] Conservative recovery language
