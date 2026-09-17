# Real-Time PR Detection & Workout Completion Celebration Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement automated Personal Record (PR) detection upon completing a workout session, persist newly unlocked PRs into `PersonalRecordDao`, and replace the plain completion text with a celebration screen displaying metrics (Volume, Duration, Sets, and PRs) with navigation to detailed breakdown.

**Architecture:**
1. In `WorkoutLoggingViewModel`: Inject `PersonalRecordDao` and `PRDetector`.
2. When `completeWorkout()` is invoked:
   - For all exercises with completed working sets, evaluate against historical PRs using `PRDetector.detectPRs`.
   - Persist any new PRs into `PersonalRecordDao`.
   - Build a `WorkoutSummary` model with total volume, elapsed duration, set completion ratio, and list of newly achieved PRs.
   - Expose `workoutSummary: StateFlow<WorkoutSummary?>`.
3. In `WorkoutSessionScreen.kt`:
   - Replace the generic completion box with `WorkoutCompletionView` styled in the dark athletic "Obsidian Volt" palette (`#121316` surface, `#D4FF32` volt PR highlights, `#38BDF8` stats).
   - Display a 2x2 metric grid (Volume, Duration, Sets, PRs) and a list of new PR achievements.
   - Provide "View Full Breakdown" and "Done" action buttons.
   - Add callback `onViewHistoryDetail: (Long) -> Unit`.
4. In `GymCoachNavHost.kt`:
   - Pass `onViewHistoryDetail = { workoutId -> navController.navigate(Routes.workoutHistoryDetail(workoutId)) }` to `WorkoutSessionScreen`.
5. Testing:
   - Add unit tests in `WorkoutLoggingViewModelCollectorTest.kt` verifying PR detection and `workoutSummary` emission upon `completeWorkout()`.
   - Add Compose UI test in `WorkoutSessionScreenTest.kt` verifying `WorkoutCompletionView` renders summary metrics and PR badges.

**Tech Stack:** Jetpack Compose, Room Database, StateFlow, Coroutines, JUnit4, MockK.

## Global Constraints
- Do NOT push any git tags (`v*`).
- Maintain 100% green CI and 0-artifact uploads on main pushes.
- Clean Architecture and MVVM patterns preserved.

---

### Task 1: PR Detection & Summary State in WorkoutLoggingViewModel

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutLoggingViewModel.kt`
- Test: `app/src/test/kotlin/com/gymcoach/app/presentation/workout/WorkoutLoggingViewModelCollectorTest.kt`

**Interfaces:**
- Produces: `data class WorkoutSummary(...)`
- Produces: `val workoutSummary: StateFlow<WorkoutSummary?>`

- [ ] **Step 1: Write unit tests in `WorkoutLoggingViewModelCollectorTest.kt` asserting that `completeWorkout()` generates a `WorkoutSummary` and records PRs**
- [ ] **Step 2: Inject `PersonalRecordDao` and `PRDetector` into `WorkoutLoggingViewModel`**
- [ ] **Step 3: Implement PR detection and `WorkoutSummary` generation in `completeWorkout()`**
- [ ] **Step 4: Run unit tests and verify they pass**
- [ ] **Step 5: Commit changes**

---

### Task 2: Workout Completion Celebration UI & Navigation

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutSessionScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt`
- Test: `app/src/androidTest/java/com/gymcoach/app/presentation/workout/WorkoutSessionScreenTest.kt`

**Interfaces:**
- Consumes: `WorkoutSummary`
- Produces: `onViewHistoryDetail: (Long) -> Unit` in `WorkoutSessionScreen`

- [ ] **Step 1: Create internal `@Composable fun WorkoutCompletionView(summary: WorkoutSummary, onDone: () -> Unit, onViewHistoryDetail: (Long) -> Unit)`**
- [ ] **Step 2: Wire `WorkoutCompletionView` into `WorkoutSessionScreen` when `completed == true`**
- [ ] **Step 3: Connect `onViewHistoryDetail` in `GymCoachNavHost.kt` to `Routes.workoutHistoryDetail(workoutId)`**
- [ ] **Step 4: Write Compose UI test in `WorkoutSessionScreenTest.kt` verifying summary metrics and buttons**
- [ ] **Step 5: Run unit tests and verify build**
- [ ] **Step 6: Commit changes**
