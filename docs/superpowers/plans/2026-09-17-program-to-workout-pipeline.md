# 1-Click Program-to-Workout Prescription Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the core product loop between Program Generation and Active Workout Logging by allowing users to instantiate a full, prescribed workout session directly from their active program days (on Home Dashboard and Program Detail Screen) with all exercises, sets, target reps, and rest periods pre-populated.

**Architecture:**
1. In `WorkoutDao`: Implement `createWorkoutFromProgramDayTransaction(day: ProgramDayEntity, exercises: List<ProgramExerciseEntity>): Long` that abandons existing incomplete workouts (enforcing single-active-session policy) and inserts a new `WorkoutEntity(sourceType = "PROGRAM", programDayId = day.id)` along with all `WorkoutExerciseEntity` and `WorkoutSetEntity` entries.
2. In `WorkoutRepository` & `WorkoutRepositoryImpl`: Implement `suspend fun createWorkoutFromProgramDay(programDayId: Long): Long?` querying `programDayDao` and `programExerciseDao`.
3. In `ProgramDetailViewModel` & `ProgramDetailScreen`: Provide `startWorkoutForDay(dayId: Long, onCreated: (Long) -> Unit)` and wire the "Start Workout" button per day to pass the specific day's ID.
4. In `HomeViewModel` & `HomeDashboardScreen`: Include `programDayId` in `TodayWorkoutUiModel`, provide `startTodayWorkout(onCreated: (Long) -> Unit)`, and wire the `TodayWorkoutCard` CTA to launch the prescribed workout.
5. In `GymCoachNavHost`: Connect the callbacks to `Routes.workoutSession(workoutId)`.
6. Testing: Add comprehensive unit tests in `WorkoutRepositoryImplTest` and `HomeViewModelTest` / `ProgramDetailViewModelTest`.

**Tech Stack:** Kotlin Coroutines, Room DB transactions, Jetpack Compose, Clean Architecture & Hilt.

## Global Constraints
- Do NOT push any git tags (`v*`).
- Maintain 100% green CI and 0 artifact uploads on main pushes.
- Clean Architecture and MVVM patterns preserved.

---

### Task 1: WorkoutDao Transaction & WorkoutRepository Implementation

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/local/dao/WorkoutDao.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/domain/repository/WorkoutRepository.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/repository/WorkoutRepositoryImpl.kt`
- Test: `app/src/test/kotlin/com/gymcoach/app/data/repository/WorkoutRepositoryImplTest.kt`

**Interfaces:**
- Produces: `WorkoutRepository.createWorkoutFromProgramDay(programDayId: Long): Long?`

- [ ] **Step 1: Write unit test in `WorkoutRepositoryImplTest.kt` for `createWorkoutFromProgramDay`**
- [ ] **Step 2: Add `createWorkoutFromProgramDayTransaction` in `WorkoutDao.kt`**
- [ ] **Step 3: Implement `createWorkoutFromProgramDay` in `WorkoutRepository` and `WorkoutRepositoryImpl`**
- [ ] **Step 4: Verify test passes**
- [ ] **Step 5: Commit changes**

---

### Task 2: Home Dashboard 1-Click Prescribed Workout Launch

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/home/HomeViewModel.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/home/HomeDashboardScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt`

**Interfaces:**
- Consumes: `WorkoutRepository.createWorkoutFromProgramDay`
- Produces: `onStartWorkout: (workoutId: Long?) -> Unit` in `HomeDashboardScreen`

- [ ] **Step 1: Add `programDayId: Long?` to `TodayWorkoutUiModel`**
- [ ] **Step 2: Add `fun startTodayWorkout(onCreated: (Long) -> Unit)` to `HomeViewModel`**
- [ ] **Step 3: Wire `TodayWorkoutCard` in `HomeDashboardScreen` to trigger `startTodayWorkout`**
- [ ] **Step 4: Update `GymCoachNavHost` to pass `workoutId` to `Routes.workoutSession(workoutId)`**
- [ ] **Step 5: Commit changes**

---

### Task 3: Program Detail Screen 1-Click Day Workout Launch

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/program/ProgramDetailScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt`

**Interfaces:**
- Consumes: `WorkoutRepository.createWorkoutFromProgramDay`
- Produces: `startWorkoutForDay(dayId: Long, onCreated: (Long) -> Unit)` in `ProgramDetailViewModel`

- [ ] **Step 1: Add `startWorkoutForDay` in `ProgramDetailViewModel`**
- [ ] **Step 2: Update `ProgramDetailScreen`'s `onStartWorkout: (Long) -> Unit` callback**
- [ ] **Step 3: Update `GymCoachNavHost` to navigate to `Routes.workoutSession(workoutId)`**
- [ ] **Step 4: Verify compilation and tests**
- [ ] **Step 5: Commit changes**
