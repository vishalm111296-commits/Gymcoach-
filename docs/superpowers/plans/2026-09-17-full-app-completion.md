# Full Application Completion Implementation Plan: In-App Program Generator, Universal Bottom Navigation & Interactive Profile

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform GymCoach into a complete, end-to-end production fitness coaching platform by adding:
1. In-App AI Program Generation & Regeneration directly inside `ProgramDetailScreen`.
2. Universal Bottom Navigation across all 5 core app screens (`Home`, `Exercises`, `Program`, `Progress`, `Profile`).
3. Interactive Profile Customization in `ProfileScreen` with automatic synchronization to `BodyMeasurementDao` for bodyweight progress tracking.

**Architecture:**
- `ProgramDetailViewModel` & `ProgramDetailScreen`: Inject `ProgramGenerator` and `UserProfileRepository`. Add `generateAndActivateProgram(frequency: Int, equipmentType: String, goal: String)` and an interactive Obsidian-Volt styled generator bottom sheet.
- Universal Bottom Nav: Integrate `GymCoachBottomNav` in `ExerciseListScreen`, `ProgramDetailScreen`, `ProgressDashboardScreen`, and `ProfileScreen` with standard route matching.
- `ProfileViewModel` & `ProfileScreen`: Add `updateProfile(...)` in `ProfileViewModel` that updates `UserProfileEntity` via `UserProfileRepository` and automatically inserts a `BodyMeasurementEntity` with the new bodyweight to update weight trends. Add "Edit Profile" modal in `ProfileScreen`.
- Automated Tests: Unit tests for `ProgramDetailViewModel` generation and `ProfileViewModel` profile updates. Compose UI tests verifying the generator modal and edit profile bottom sheet.

---

### Task 1: In-App AI Program Generation in `ProgramDetailScreen`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/program/ProgramDetailViewModel.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/program/ProgramDetailScreen.kt`
- Test: `app/src/test/kotlin/com/gymcoach/app/presentation/program/ProgramDetailViewModelTest.kt`

- [ ] **Step 1: Write unit tests in `ProgramDetailViewModelTest.kt` verifying `generateAndActivateProgram` delegates to `ProgramGenerator` and `ProgramRepository`**
- [ ] **Step 2: Update `ProgramDetailViewModel` constructor to inject `ProgramGenerator` and `UserProfileRepository`**
- [ ] **Step 3: Implement `generateAndActivateProgram(frequency: Int, equipmentType: String, goal: String)` in `ProgramDetailViewModel`**
- [ ] **Step 4: Add `GenerateProgramBottomSheet` in `ProgramDetailScreen.kt` with Goal, Frequency, and Equipment selectors**
- [ ] **Step 5: Surface "Generate AI Program" button in both the active program header and empty state**
- [ ] **Step 6: Run tests and verify compilation**

---

### Task 2: Interactive Profile Customization & Body Measurement Sync in `ProfileScreen`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/profile/ProfileScreen.kt`
- Test: `app/src/test/kotlin/com/gymcoach/app/presentation/profile/ProfileViewModelTest.kt`

- [ ] **Step 1: Write unit tests in `ProfileViewModelTest.kt` verifying `updateProfile` persists changes and logs body measurement**
- [ ] **Step 2: Update `ProfileViewModel` to inject `BodyMeasurementDao`**
- [ ] **Step 3: Implement `fun updateProfile(...)` in `ProfileViewModel`**
- [ ] **Step 4: Add "Edit Profile" IconButton in TopAppBar of `ProfileScreen`**
- [ ] **Step 5: Create `EditProfileBottomSheet` with fields for weight, height, age, goal, training frequency, and equipment**
- [ ] **Step 6: Run tests and verify compilation**

---

### Task 3: Universal Bottom Navigation Across All Top-Level Screens

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/list/ExerciseListScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/program/ProgramDetailScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressDashboardScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/profile/ProfileScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt`

- [ ] **Step 1: Add bottom navigation callbacks to `ExerciseListScreen`, `ProgramDetailScreen`, `ProgressDashboardScreen`, and `ProfileScreen`**
- [ ] **Step 2: Wrap screens in `Scaffold` with `bottomBar = { GymCoachBottomNav(currentRoute = ..., onNavigate = ...) }`**
- [ ] **Step 3: Connect top-level route transitions in `GymCoachNavHost.kt`**
- [ ] **Step 4: Run unit and compose tests to verify 0 regressions**

---

### Task 4: Local Verification & GitHub Actions Gate Validation

- [ ] **Step 1: Run `./gradlew testDebugUnitTest` and ensure 100% tests pass**
- [ ] **Step 2: Run `./gradlew assembleDebug` to verify compilation**
- [ ] **Step 3: Commit and push changes to origin main**
- [ ] **Step 4: Monitor CI pipeline and verify 0 artifacts via GitHub API**
