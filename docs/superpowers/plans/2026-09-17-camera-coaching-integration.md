# Camera Form Coaching Integration & UI Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the MediaPipe computer-vision Camera Form Coaching feature directly into the active Workout Session and Exercise Detail screens, and implement real Compose UI testing for workout session exercise components.

**Architecture:** 
1. Provide bidirectional mapping between exercise names and `ExerciseType` enum supported by `FormAnalyzer`.
2. Expose `onCameraClick: (ExerciseType) -> Unit` callbacks in `WorkoutSessionScreen` and `ExerciseDetailScreen`.
3. In `ExerciseDetailScreen`, display a prominent "Live Camera Form Check" button if the exercise is supported.
4. In `WorkoutSessionScreen`'s `ExerciseSetCard`, show a Camera Form Coach icon button when the exercise is supported.
5. Wire both screens in `GymCoachNavHost` to `Routes.camera(exerciseType)`.
6. Make `ExerciseSetCard` accessible for UI tests and implement real Compose UI test assertions in `WorkoutSessionScreenTest.kt`.

**Tech Stack:** Jetpack Compose, CameraX, MediaPipe, Navigation Compose, JUnit4, AndroidX Compose Test.

**Spec:** Connected fitness loop linking exercise execution and instructions directly to real-time CV form coaching.

## Global Constraints
- Do NOT push any git tags (`v*`).
- Maintain 100% green CI and zero artifact emission on main pushes.
- Clean Architecture and MVVM patterns preserved.

---

### Task 1: ExerciseType Name Mapping & Unit Tests

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/core/ml/FormAnalyzer.kt`
- Modify: `app/src/test/kotlin/com/gymcoach/app/core/ml/FormAnalyzerTest.kt`

**Interfaces:**
- Produces: `ExerciseType.fromExerciseName(name: String): ExerciseType?`

- [ ] **Step 1: Write the failing unit test for `ExerciseType.fromExerciseName`**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement `fromExerciseName` in `FormAnalyzer.kt`**
- [ ] **Step 4: Run test to verify it passes**
- [ ] **Step 5: Commit changes**

---

### Task 2: Connect Camera Form Coaching to `ExerciseDetailScreen`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/detail/ExerciseDetailScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt`

**Interfaces:**
- Consumes: `ExerciseType.fromExerciseName`
- Produces: `onCameraClick: (ExerciseType) -> Unit` in `ExerciseDetailScreen`

- [ ] **Step 1: Add `onCameraClick: (ExerciseType) -> Unit` parameter to `ExerciseDetailScreen`**
- [ ] **Step 2: Render Live Camera Form Check button in `ExerciseDetailScreen` when `fromExerciseName(ex.name) != null`**
- [ ] **Step 3: Wire `onCameraClick` in `GymCoachNavHost` to `navController.navigate(Routes.camera(exerciseType))`**
- [ ] **Step 4: Verify compilation**
- [ ] **Step 5: Commit changes**

---

### Task 3: Connect Camera Form Coaching to `WorkoutSessionScreen`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutSessionScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt`

**Interfaces:**
- Consumes: `ExerciseType.fromExerciseName`
- Produces: `onCameraClick: (ExerciseType) -> Unit` in `WorkoutSessionScreen` and `ExerciseSetCard`

- [ ] **Step 1: Add `onCameraClick: (ExerciseType) -> Unit` parameter to `WorkoutSessionScreen` and `ExerciseSetCard`**
- [ ] **Step 2: Show camera icon button on `ExerciseSetCard` when supported by `ExerciseType`**
- [ ] **Step 3: Wire `onCameraClick` in `GymCoachNavHost` for both `Routes.WORKOUT_SESSION` and `Routes.WORKOUT_LEGACY`**
- [ ] **Step 4: Verify compilation**
- [ ] **Step 5: Commit changes**

---

### Task 4: Implement Real Compose UI Test in `WorkoutSessionScreenTest.kt`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutSessionScreen.kt` (ensure `ExerciseSetCard` is package-visible/internal)
- Modify: `app/src/androidTest/java/com/gymcoach/app/presentation/workout/WorkoutSessionScreenTest.kt`

**Interfaces:**
- Consumes: `ExerciseSetCard` composable

- [ ] **Step 1: Make `ExerciseSetCard` `internal` in `WorkoutSessionScreen.kt`**
- [ ] **Step 2: Write Compose UI test in `WorkoutSessionScreenTest.kt` validating instruction expansion, collapsing, and camera button presence**
- [ ] **Step 3: Run JVM unit tests to ensure nothing broken**
- [ ] **Step 4: Verify build with `./gradlew assembleDebug assembleAndroidTest`**
- [ ] **Step 5: Commit changes**
