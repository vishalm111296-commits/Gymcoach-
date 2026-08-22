# GymCoach Master Specification

**Version:** 1.0
**Date:** 2026-08-22
**Scope:** Foundation Phase — Data Model, Exercise System, Program Engine, Onboarding

---

## 1. Executive Summary

GymCoach is an Android fitness application being transformed from a prototype-level workout tracker into a production-quality personal coaching platform. This specification covers the Foundation Phase: the core data model, exercise system, V-taper program engine, and onboarding flow that everything else depends on.

**Target User Profile:**
- Male, Age 30, Height 170cm
- Goal: Muscular V-shaped/V-taper physique
- Training experience: Beginner/Intermediate
- Default training frequency: 4 days/week

**Key Design Principles:**
1. Evidence-based programming (ACSM 2026, peer-reviewed research)
2. No fake data — every metric mathematically proven from actual workouts
3. Conservative language for recovery ("recent training load suggests...")
4. Scalable architecture supporting 200+ exercises
5. Clean Architecture + MVVM + Hilt + Room (existing stack)

---

## 2. Architecture

### 2.1 Current State
- Clean Architecture with MVVM
- Hilt for DI
- Room for local persistence (v2)
- Jetpack Compose UI
- CameraX + MediaPipe for form analysis
- 4 screens: ExerciseList, ExerciseDetail, WorkoutSession, ProgressDashboard

### 2.2 Target Architecture (Foundation Phase)
```
app/src/main/kotlin/com/gymcoach/app/
├── core/
│   ├── di/                    (existing)
│   ├── ml/                    (existing — FormAnalyzer)
│   ├── timer/                 (existing — RestTimerManager)
│   ├── exercise/              NEW: ExerciseSeeder, ExerciseSearchEngine
│   ├── program/               NEW: ProgramGenerator, VolumeCalculator
│   └── progression/           NEW: ProgressionEngine, PRDetector
├── data/
│   ├── local/
│   │   ├── database/          EXTENDED: GymCoachDatabase v3
│   │   ├── dao/               EXTENDED: new DAOs
│   │   ├── entity/            EXTENDED: new entities
│   │   └── migration/         NEW: MIGRATION_2_3
│   └── repository/            EXTENDED: new repositories
├── domain/
│   ├── model/                 EXTENDED: new domain models
│   └── repository/            EXTENDED: new interfaces
├── presentation/
│   ├── onboarding/            NEW: OnboardingFlow
│   ├── program/               NEW: ProgramScreen
│   ├── home/                  NEW: HomeDashboard
│   ├── exercise/              EXTENDED: enhanced ExerciseDetail
│   ├── workout/               EXTENDED: enhanced WorkoutSession
│   ├── progress/              EXTENDED: enhanced ProgressDashboard
│   └── history/               EXTENDED: WorkoutHistory
└── ui/
    ├── GymCoachNavHost.kt     EXTENDED: new routes
    ├── MainActivity.kt        (existing)
    └── theme/                 (existing)
```

### 2.3 Database Migration (v2 → v3)

**New Tables:**
- `user_profiles` — onboarding data (goal, experience, equipment, training days, etc.)
- `programs` — training program templates
- `program_days` — daily workout structure within a program
- `program_exercises` — exercises within a program day (with target sets/reps)
- `personal_records` — PR tracking per exercise
- `body_measurements` — body weight, measurements, progress photos
- `favorite_exercises` — user favorites
- `exercise_substitutions` — substitution mappings between exercises
- `muscles` — hierarchical muscle taxonomy
- `equipment` — equipment taxonomy
- `exercise_muscles` — exercise-muscle relationships with role (primary/secondary/stabilizer)
- `exercise_equipment` — exercise-equipment relationships
- `exercise_aliases` — search aliases for exercises

**Extended Tables:**
- `exercises` — add vtaper_relevance fields, movement_pattern, media URLs, search_metadata

**Migration Strategy:**
- Use Room auto-migration where possible
- Manual migration for complex schema changes
- Backfill existing exercises with new metadata
- Preserve all existing user data

---

## 3. Exercise System

### 3.1 Exercise Data Model

Each exercise includes:
- **Core:** name, description, category, difficulty (beginner/intermediate/advanced)
- **Muscles:** primary muscles, secondary muscles, stabilizer muscles (via join tables)
- **Equipment:** primary equipment, optional alternatives (via join tables)
- **Movement Pattern:** compound/isolation, push/pull/hinge/squat/carry/core
- **V-Taper Relevance:** lat (0-10), lateral_delt (0-10), upper_chest (0-10), rear_delt (0-10)
- **Media:** image_url, video_url, animation_url (nullable, architecture-ready)
- **Instructions:** setup, execution, breathing, tempo guidance, common_mistakes
- **Search:** aliases, search_tags, full-text search support
- **Progression:** beginner_variant, intermediate_variant, advanced_variant
- **Substitutions:** exercises that can replace this one (preserving training intent)

### 3.2 Muscle Taxonomy

Hierarchical structure:
```
Chest
├── Upper Chest (Clavicular)
├── Middle Chest (Sternal)
└── Lower Chest (Abdominal)
Back
├── Lats (Latissimus Dorsi)
├── Upper Back (Trapezius, Rhomboids)
├── Middle Back (Mid Traps)
└── Lower Back (Erector Spinae)
Shoulders
├── Anterior Deltoid
├── Lateral Deltoid
└── Rear Deltoid
Arms
├── Biceps (Long Head, Short Head, Brachialis)
├── Triceps (Long Head, Lateral Head, Medial Head)
└── Forearms
Legs
├── Quadriceps (Rectus Femoris, Vastus Lateralis, Vastus Medialis, Vastus Intermedius)
├── Hamstrings (Biceps Femoris, Semitendinosus, Semimembranosus)
├── Glutes (Maximus, Medius, Minimus)
└── Calves (Gastrocnemius, Soleus)
Core
├── Rectus Abdominis
├── Obliques
├── Transverse Abdominis
└── Hip Flexors
```

### 3.3 Equipment Taxonomy

Categories:
- **Free Weights:** barbell, dumbbell, kettlebell, EZ-bar, trap bar
- **Machines:** cable, smith machine, leg press, hack squat, leg extension, leg curl
- **Bodyweight:** pull-up bar, dip station, floor, wall
- **Bands:** resistance bands (various tensions)
- **Other:** medicine ball, Swiss ball, foam roller

Availability flags: gym_available, home_available

### 3.4 Exercise Seeding

**Target:** 200+ structured exercises across all muscle groups

**Categories:**
- Chest: 15-20 exercises (bench press variations, flyes, push-ups)
- Back: 20-25 exercises (pull-ups, rows, pulldowns, deadlifts)
- Lats: 10-15 exercises (vertical pulls, horizontal pulls)
- Shoulders: 15-20 exercises (presses, lateral raises, rear delt work)
- Biceps: 10-15 exercises (curls, hammer curls, chin-ups)
- Triceps: 10-15 exercises (pushdowns, extensions, dips)
- Quadriceps: 15-20 exercises (squats, lunges, leg press)
- Hamstrings: 10-15 exercises (deadlifts, leg curls, Romanian deadlifts)
- Glutes: 10-15 exercises (hip thrusts, squats, lunges)
- Calves: 8-10 exercises (standing raises, seated raises, donkey raises)
- Core: 15-20 exercises (planks, crunches, rotations, anti-rotation)

**Seeding Strategy:**
- Versioned JSON asset files in assets/exercises/
- Transactional seed on first launch or version upgrade
- Seed version tracking to avoid redundant reseeding
- Content updates without schema migration

### 3.5 Exercise Search

- Full-text search on name, aliases, tags
- Filters: muscle group, equipment, difficulty, movement pattern, V-taper relevance
- Favorites and recently used tracking
- Paginated results (30 per page)
- Indexed queries for performance

---

## 4. V-Taper Program Engine

### 4.1 Evidence Base

Based on ACSM 2026 Position Stand and peer-reviewed research:

- **Volume:** 10-20 hard sets per muscle group per week for hypertrophy
- **Frequency:** Each muscle trained ≥2×/week
- **Rep Range:** 6-12 RM zone for primary work (30-100% 1RM viable with sufficient effort)
- **Proximity to Failure:** Most working sets at 0-3 RIR; occasional failure on isolation work
- **Progression:** +2-10% load when target reps exceeded by 1-2
- **Recovery:** ~48h between sessions for same muscle group
- **Splits:** Volume delivery scheduling decision; no inherent superiority

### 4.2 V-Taper Priority Mapping

**PRIMARY (highest volume allocation):**
- Lats: vertical pulling emphasis (pull-ups, pulldowns)
- Lateral Deltoids: direct isolation work (lateral raises)

**SECONDARY (moderate volume):**
- Rear Deltoids: horizontal pulling, face pulls, rear delt flyes
- Upper Chest: incline pressing (30-45° angle)
- Upper Back: rows, shrugs, face pulls

**MAINTENANCE (balanced volume):**
- Biceps: curl variations, chin-ups
- Triceps: pushdowns, extensions, dips
- Quadriceps: squats, lunges, leg press
- Hamstrings: deadlifts, leg curls, Romanian deadlifts
- Glutes: hip thrusts, squats, lunges
- Calves: standing/seated raises
- Core: planks, rotations, anti-rotation

### 4.3 Program Generation Algorithm

**Input:**
- Training frequency (3-6 days/week, default 4)
- Available equipment (gym/home/custom)
- Experience level (beginner/intermediate/advanced)
- Session length preference (30-90 minutes)
- Goals (V-taper, muscle gain, strength, etc.)

**Output:**
- Weekly program with daily workout structure
- Exercise selection per day
- Target sets, reps, and RPE for each exercise
- Rest periods between sets
- Warm-up recommendations

**Volume Distribution:**
- V-taper priority muscles: 12-16 sets/week
- Secondary muscles: 10-14 sets/week
- Maintenance muscles: 8-12 sets/week
- Total weekly volume: 100-150 sets across all muscle groups

**Split Options:**
- 3 days: Full Body (A/B/C)
- 4 days: Upper/Lower (A/B/A/B)
- 5 days: Push/Pull/Legs/Upper/Lower
- 6 days: Push/Pull/Legs/Push/Pull/Legs

### 4.4 Exercise Selection Logic

For each muscle group, select exercises based on:
1. Available equipment
2. Movement pattern balance (compound vs isolation)
3. V-taper relevance scoring
4. Difficulty level matching user experience
5. Substitution availability

**Example — Lat Development (Gym):**
1. Pull-ups (primary compound)
2. Lat Pulldown (primary compound)
3. Seated Cable Row (secondary compound)
4. Single-Arm Dumbbell Row (secondary compound)
5. Straight-Arm Pulldown (isolation)

**Example — Lat Development (Home):**
1. Pull-ups (primary compound)
2. Inverted Rows (secondary compound)
3. Resistance Band Pulldowns (secondary compound)
4. Dumbbell Rows (secondary compound)
5. Bodyweight Pullover (isolation)

---

## 5. Progressive Overload Engine

### 5.1 Progression Rules

**Double Progression Model:**
- Target: 3 × 8-12 reps
- When all sets hit 12 reps → increase load by 2-5 kg (or 5-10%)
- New target: 3 × 8-12 reps with new load
- Start at bottom of rep range with new load

**Example:**
```
Set 1: 50kg × 12 ✓
Set 2: 50kg × 12 ✓
Set 3: 50kg × 12 ✓
→ All sets at top of range
→ Next session: 55kg × 8-12
```

**Regression Rules:**
- If reps fall below minimum for 2 consecutive sessions → reduce load by 5-10%
- If user reports excessive fatigue → suggest deload week
- If progress stalls for 3+ weeks → suggest exercise variation

### 5.2 PR Detection

**Types of PRs:**
- **Weight PR:** Heaviest weight lifted for exercise
- **Rep PR:** Most reps at a given weight
- **Estimated 1RM PR:** Highest estimated 1RM (Epley formula)
- **Volume PR:** Highest single-session volume for exercise
- **Best Set:** Highest estimated 1RM from a single set

**Epley Formula:** estimated_1RM = weight × (1 + reps/30)
- Note: Unreliable above 12 reps; cap estimation at 12 reps

### 5.3 Recovery Guidance

**Conservative Language Only:**
- "Recent training load suggests this muscle may still be recovering."
- "You've trained chest heavily in the last 48 hours. Consider waiting before another chest session."
- "Your overall training volume this week is high. Listen to your body."

**NEVER:**
- "Your latissimus is 73% recovered."
- "You need exactly 2.3 days of rest."
- "Your recovery score is 85%."

---

## 6. Onboarding Flow

### 6.1 Data Collection

**Step 1: Goals**
- Muscle gain
- Strength
- Fat loss
- Recomposition
- General fitness
- V-taper physique

**Step 2: Experience Level**
- Complete beginner (never trained)
- Beginner (< 1 year)
- Intermediate (1-3 years)
- Advanced (3+ years)

**Step 3: Personal Info**
- Age
- Sex
- Height (cm)
- Weight (kg)

**Step 4: Training Schedule**
- Training days per week (3-6)
- Session length preference (30-90 minutes)

**Step 5: Equipment**
- Gym (full access)
- Home (limited equipment)
- Custom (select available equipment)

**Step 6: Preferences**
- Preferred exercises (optional)
- Exercises to avoid (optional)
- Any injuries or limitations

### 6.2 Program Generation

After onboarding:
1. Generate personalized 4-week program
2. Show weekly structure
3. Allow exercise substitutions
4. Start with Week 1, Day 1

---

## 7. Home Dashboard

### 7.1 Primary Content

**Today's Workout:**
- Program name (e.g., "Upper Body A")
- Target muscles (e.g., "Lats • Delts • Upper Chest")
- Estimated duration (e.g., "~50-60 min")
- [START WORKOUT] button

**Last Session:**
- Date and workout name
- Key stats (volume, PRs)
- [VIEW DETAILS] link

**Training Streak:**
- Current streak (days/weeks)
- Recent consistency

**Recent PR:**
- Latest personal record
- Exercise name and achievement

### 7.2 Empty State

When no data exists:
- "Welcome to GymCoach"
- "Your training journey starts here"
- "Complete onboarding to generate your personalized program"
- [COMPLETE ONBOARDING] button

When onboarding complete but no workouts:
- "Ready for your first workout?"
- "Today: [Workout Name]"
- [START FIRST WORKOUT] button

---

## 8. Testing Strategy

### 8.1 Database Tests
- In-memory Room tests for all DAOs
- Migration tests (v2 → v3)
- CRUD operations
- Query correctness
- Edge cases (empty DB, duplicate data, cascade deletes)

### 8.2 Domain Logic Tests
- Program generation (correct volume distribution, exercise selection)
- Progression logic (when to increase load, when to maintain)
- Analytics calculations (volume, PRs, estimated 1RM)
- Substitution logic (preserving training intent)

### 8.3 Repository Tests
- Integration tests with in-memory Room
- Flow emissions
- Error handling

### 8.4 ViewModel Tests
- State management
- User interactions
- Loading/error states
- Navigation triggers

### 8.5 Calculation Tests
- Volume = load × reps
- Estimated 1RM = Epley formula
- Weekly frequency calculations
- Muscle group volume calculations
- Edge cases (zero divisions, overflow, negative values)

### 8.6 Integration Tests
- Complete workout flow (start → add exercise → log sets → complete)
- Onboarding to program generation
- Exercise library search and filter

---

## 9. Implementation Order

### Phase 1: Database Foundation (Week 1)
1. Database v3 migration script
2. New entity definitions
3. Extended ExerciseEntity
4. New DAOs
5. Seed data structure

### Phase 2: Domain Models (Week 1-2)
1. Extended Exercise domain model
2. UserProfile model
3. Program models
4. PersonalRecord model
5. BodyMeasurement model

### Phase 3: Repository Layer (Week 2)
1. Exercise repository extensions
2. UserProfile repository
3. Program repository
4. Analytics repository extensions

### Phase 4: Exercise System (Week 2-3)
1. Exercise seeder
2. Exercise search engine
3. Exercise detail enhancements
4. Substitution logic

### Phase 5: Program Engine (Week 3)
1. Volume calculator
2. Program generator
3. Exercise selection algorithm
4. Split templates

### Phase 6: Onboarding (Week 3-4)
1. Onboarding flow UI
2. User profile storage
3. Program generation trigger

### Phase 7: Home Dashboard (Week 4)
1. Today's workout display
2. Training streak
3. Recent PRs
4. Empty states

### Phase 8: Progressive Overload (Week 4-5)
1. Progression engine
2. PR detection
3. Load recommendation

---

## 10. Success Criteria

### Functional
- [ ] Database migration preserves all existing data
- [ ] 200+ exercises seeded with structured metadata
- [ ] Exercise search works with filters
- [ ] Program generates based on user profile
- [ ] Progressive overload provides load recommendations
- [ ] PRs are mathematically proven from actual data
- [ ] Onboarding collects necessary data
- [ ] Home dashboard shows today's workout

### Technical
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Database migrations verified
- [ ] No fabricated data
- [ ] Conservative recovery language
- [ ] Evidence-based programming

### UX
- [ ] Empty states guide users
- [ ] Navigation is logical
- [ ] Workout execution is smooth
- [ ] Progress is visible and meaningful

---

## 11. Known Limitations

1. Camera form analysis (MediaPipe) not in scope for foundation phase
2. Body fat estimates clearly labeled as estimates
3. Recovery guidance is conservative (no precise percentages)
4. Exercise media uses placeholders (architecture-ready)
5. Program generation is rule-based (not AI-powered)

---

## 12. References

- ACSM 2026 Position Stand on Resistance Training
- Schoenfeld et al. 2017 (Volume dose-response)
- Remmert et al. 2024 (Proximity to failure)
- Pallarés et al. 2021 (Range of motion)
- Product benchmark: Hevy, Strong, Fitbod, JEFIT, Boostcamp
