# GymCoach — Restore Build Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the GymCoach working tree to a compiling, testable state — removing unwired, unbuildable orphan clusters and repairing the wired core (Exercise Library, Workout Session, History, Progress, Profile, Measurements, Settings) — without adding any new features.

**Architecture:** Two-part strategy. (1) **Delete** the orphaned clusters that are unwired (no NavHost route, no DI binding) and unbuildable (reference types that do not exist): VShape, Search, Goals, Workout Templates, BackupManager, plus 4 unreferenced shared components. (2) **Repair** the wired core by fixing signature/import/syntax errors in dependency order: shared components → data layer + DI → navigation → screens → secondary screens. The committed HEAD (`320d990`) is CI-green; the uncommitted WIP (45 files, 768 compile errors) is what this plan repairs. Nothing new is added; MediaPipe/camera-ML/video features stay out of scope.

**Tech Stack:** Kotlin 1.9.22, Jetpack Compose BOM `2024.02.00` (material3 1.2.0, foundation 1.6.1), Hilt 2.50, Room 2.6.1 (KSP), Navigation Compose, AGP 8.2.2, Gradle 8.4, JUnit 4.13.2 + MockK 1.13.9.

## Global Constraints

- **Build host is aarch64 Linux.** The bundled `aapt2` is x86-only; it runs under a qemu wrapper. **Every** Gradle command MUST pass `-Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2` (a native launcher named exactly `aapt2`; AGP rejects the override path unless the filename ends in `aapt2`).
- **Do not add features.** No MediaPipe inference, no camera ML, no video wiring, no new screens. The camera route stays as the existing raw-preview `CameraPreviewScreen()`; `ExerciseVideoPlayer` is only made to *compile* (still unreferenced).
- **Version floors (verified against resolved artifacts):** Compose BOM `2024.02.00` ⇒ material3 `1.2.0`, foundation `1.6.1`. The `androidx.compose.material3.shapes(...)` top-level function **does not exist** — use `Shapes(...)` constructor + `androidx.compose.foundation.shape.RoundedCornerShape`. The `androidx.compose.foundation.grid.*` package **does not exist** — use `androidx.compose.foundation.lazy.grid.*`.
- **Icons** resolve from `material-icons-extended` 1.6.1. Verified present: `AppRegistration`, `Code`, `DarkMode`, `EmojiEvents`, `History`, `Insights`, `Layers`, `QueryBuilder`, `RestartAlt`, `Scale`, `Terminal`, `VolumeOff`, `CameraAlt`, `Tune`. **Absent:** `Bell` (use `Notifications` — core), `Restart` (use `RestartAlt`), `Builder` (use `QueryBuilder`).
- **Icon extensions require imports.** `Icons.Filled.X` / `Icons.Default.X` are *extension properties* — the fully-qualified path `androidx.compose.material.icons.Icons.Filled.Info` does **not** resolve. Always `import androidx.compose.material.icons.filled.<X>`.
- **Keep** `domain/vshape/model/MeasurementRecord.kt` and `MeasurementType.kt` — the Measurement feature imports them. Everything else in `domain/vshape/` is deleted.
- **Room:** `fallbackToDestructiveMigration()` is active, so removing the `GoalEntity` from the schema is safe (no migration needed).
- **Fast iteration command** (skips KSP/Hilt/Room codegen; runs the Kotlin compiler only):
  `./gradlew :app:compileDebugKotlin -x kspDebugKotlin --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2`
- **Full verification command:**
  `./gradlew :app:assembleDebug --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2`
- **Test command:**
  `./gradlew :app:testDebugUnitTest --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2`
- **Commit discipline:** one commit per task, small and self-contained. Restore point branch created in Task 1 before any change.
- **No claims of completion without evidence:** every "green" claim in the acceptance criteria requires running the stated build/test command and seeing `BUILD SUCCESSFUL` / passing tests.

---

## Root-Cause Analysis

The failure is one uncommitted, never-compiled refactor (the WIP: 20 modified + 31 untracked Kotlin files) layered onto a CI-green commit. Its compile errors fall into four root causes:

1. **Orphaned clusters written against types that were never created.** The VShape feature (5 screens, 1 VM, 1 repository, 8 use-case files) imports `GoalType`, `GoalStatus`, `GoalPriority`, `VShapeDao`, `BodyMeasurement`, `VShapeUseCase`, `VShapeChallengeCompletion` — **none of which exist anywhere**. The `SearchScreen.kt` has no package declaration and no imports. `GoalEntity`'s TypeConverters reference the same nonexistent vshape enums. `BackupManager` imports `androidx.datastore.*`, `kotlinx.serialization.*`, `BuildConfig` — none of which are declared dependencies. These files are *independent* errors: no other code references them, so they cannot be fixed, only removed.
2. **Duplicate declarations.** `GetLatestMeasurementUseCase` is declared both as an interface (`MeasurementUseCases.kt`) and as a class (`GetLatestMeasurementUseCase.kt`) in the same package. `ProfileSettingsViewModel` and `ProfileAnalyticsViewModel` are each declared in *two* files (`ProfileViewModel.kt` + standalone). Two `formatDuration`/`StatItem` functions collide in `presentation.history`.
3. **Missing/incorrect imports and signatures in the wired core.** The WIP rewrote shared components (`ErrorState`, `EmptyState`, `ExerciseItemCard`), screens (`ExerciseListScreen`, `WorkoutHistoryScreen`, `ProgressDashboardScreen`, `MeasurementScreen`), and `Theme.kt` but dropped imports (`Card`, `Column`, `Box`, `size`, `height`, `width`, `semantics`, date-picker APIs, `Shapes`), used APIs that don't exist in this Compose version (`foundation.grid`, `material3.shapes`, `foundation.verticalGrid`), and mis-matched screen call signatures in `GymCoachNavHost`.
4. **Broken ViewModel/DI contracts.** `ProfileViewModel.kt` and both profile VMs miss `import androidx.lifecycle.ViewModel`, so Hilt rejects all `@HiltViewModel` classes and `viewModelScope`/`copy` cascade-fail. `ProfileRepository` has an `@Inject` impl but **no `@Binds`** in `RepositoryModule`. `MeasurementViewModel` calls `.execute(...)` on use cases that expose `operator fun invoke`, and reads `_measurements`/`trends` that are private/nonexistent.

### Cascading vs. independent errors

| Class | Files | How to handle |
|---|---|---|
| **Independent** (unreferenced + unbuildable) | VShape (17), SearchScreen (1), Goals (3), Templates (2), BackupManager (1), DragAndDrop/WorkoutComponents/PlateCalculatorDialog/Accessibility (4) | **Delete** (Tasks 2–5). Zero references verified by grep. |
| **Cascading from missing imports** | ErrorState, EmptyState, ExerciseItemCard, ExerciseVideoPlayer, WorkoutHistoryScreen, ExerciseDetailScreen, Theme, ProgressDashboardScreen, ExerciseListScreen | Fix the import/signature once; all downstream `@Composable`-context and receiver errors in that file clear automatically (Tasks 7–10, 15–18). |
| **Cascading from broken DI/VM contracts** | ProfileViewModel(+Screen+2 standalone VMs), MeasurementViewModel/Screen, SettingsViewModel/Screen | Fix the one root per file (ViewModel import / sealed→data / missing binding / wrong method names); the dozens of `viewModelScope`/`copy`/suspend-context errors clear together (Tasks 20–22). |
| **Cross-file contracts** | GymCoachNavHost ↔ list/history/session/camera/summary screens | Fix the screen signatures first (Tasks 15–18), then rewrite the NavHost call sites (Task 14). |
| **Duplicate declarations** | measurement use-case interfaces, profile VM files, history StatItem/formatDuration | Delete one copy / rename one symbol (Tasks 12, 17, 20). |

**Dependency order rationale:** deletions first (largest independent error mass, ~450 errors in 28 files), because every remaining file's error count is re-measured against a smaller graph. Then the shared components every screen imports, then the data layer + DI (what ViewModels inject), then navigation, then the screens that depend on all of the above, then the full KSP/Hilt/Room build, then tests.

---

## Phase 0 — Baseline & Safety

### Task 1: Verify the toolchain and create a restore point

**Files:** none (repository operations only)

**Problem:** All Gradle commands fail on this aarch64 host unless the x86 `aapt2` is overridden, and the working tree is uncommitted, so any repair mistake is unrecoverable unless we snapshot it first.

**Solution:** Verify the qemu aapt2 wrapper works, reproduce the red build to capture the baseline, and commit the entire pre-repair state to a backup branch.

- [ ] **Step 1: Verify the aapt2 wrapper**
```bash
/opt/android-sdk/qemu/bin/aapt2 version
```
Expected: `Android Asset Packaging Tool (aapt) 2.19-10154469` and exit 0.

- [ ] **Step 2: Reproduce the baseline failure** (this is the "failing test" for the whole plan)
```bash
cd /root/Projects/GymCoach
./gradlew :app:compileDebugKotlin -x kspDebugKotlin --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2 2>&1 | tee /tmp/repair-baseline.log | grep -c "^e: "
```
Expected: `768` (the exact pre-repair error count). Keep `/tmp/repair-baseline.log` — it is the source of truth for "how red were we".

- [ ] **Step 3: Create the restore point**
```bash
cd /root/Projects/GymCoach
git switch -c backup/pre-repair-2026-08-08
git add -A
git commit -m "chore: snapshot of broken WIP state (pre-repair)

Co-Authored-By: Claude <noreply@anthropic.com>"
git switch feature/claude-code-compatibility
```

- [ ] **Step 4: Verify the restore point**
```bash
git log --oneline -1 backup/pre-repair-2026-08-08
git status --porcelain | wc -l
```
Expected: the backup commit is shown, and the working tree is still dirty (44+ entries) on `feature/claude-code-compatibility`.

- [ ] **Step 5: Commit**
```bash
git add -A && git commit -m "chore: baseline verified for build repair

Co-Authored-By: Claude <noreply@anthropic.com>"
```
(If nothing changed in this task, skip this step — there is nothing to commit yet.)

**Dependencies:** none. **Tests:** Steps 1–2. **Acceptance:** wrapper prints version; baseline = 768 errors; `backup/pre-repair-2026-08-08` exists. **Risk:** none (no code touched). **Effort:** ~10 min.

---

## Phase 1 — Remove Orphaned, Unbuildable Clusters

Deleting unwired clusters. Every file here is confirmed (by `grep -rln` across `app/src/main/kotlin`) to have **zero references** from any surviving file. Deleting them removes ~450 errors at once.

### Task 2: Delete the VShape cluster

**Files (delete):**
- `app/src/main/kotlin/com/gymcoach/app/presentation/vshape/VShapeViewModel.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/vshape/screens/VShapeHomeScreen.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/vshape/screens/VShapeAssessmentScreen.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/vshape/screens/VShapeChallengeScreen.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/vshape/screens/VShapePlanScreen.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/vshape/screens/VShapeProgressScreen.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/VShapeAssessmentUseCase.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/MeasurementUseCases.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/VShapeUseCaseImpl.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/VShapeAssessmentUseCase.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/VShapeChallengeUseCaseImpl.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/SaveMeasurementUseCaseImpl.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/MeasurementUseCasesImpl.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/ProgressSnapshot.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/RecoveryLog.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/ChallengesTable.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/ChallengeDay.kt`
- `app/src/main/kotlin/com/gymcoach/app/data/repository/VShapeRepository.kt`

**Files (keep — used by the Measurement feature):**
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/MeasurementRecord.kt`
- `app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/MeasurementType.kt`

**Interfaces:**
- Consumes: nothing (all deleted files are self-referencing).
- Produces: the measurement subsystem continues to import `com.gymcoach.app.domain.vshape.model.MeasurementRecord` / `MeasurementType` — they are untouched.

- [ ] **Step 1: Delete the 18 files**
```bash
cd /root/Projects/GymCoach
git rm -r app/src/main/kotlin/com/gymcoach/app/presentation/vshape
git rm app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/VShapeAssessmentUseCase.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/MeasurementUseCases.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/VShapeUseCaseImpl.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/VShapeAssessmentUseCase.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/VShapeChallengeUseCaseImpl.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/SaveMeasurementUseCaseImpl.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/usecase/impl/MeasurementUseCasesImpl.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/ProgressSnapshot.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/RecoveryLog.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/ChallengesTable.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/ChallengeDay.kt \
       app/src/main/kotlin/com/gymcoach/app/data/repository/VShapeRepository.kt
```
(The `git rm -r .../presentation/vshape` removes the 6 presentation files including the screens; the individual `git rm`s remove the 12 remaining domain/data files. `MeasurementRecord.kt` and `MeasurementType.kt` are **not** deleted.)

- [ ] **Step 2: Verify the keepers survive and no dangling references remain**
```bash
test -f app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/MeasurementRecord.kt && echo "MeasurementRecord OK"
test -f app/src/main/kotlin/com/gymcoach/app/domain/vshape/model/MeasurementType.kt && echo "MeasurementType OK"
grep -rln "ChallengeDay\|ProgressSnapshot\|RecoveryLog\|ChallengesTable\|VShapeRepository\|VShapeViewModel" app/src/main/kotlin || echo "no dangling refs"
```
Expected: both keepers printed, and `no dangling refs`.

- [ ] **Step 3: Verify the error count dropped** (failing-test → passing-test cycle for the deletion)
```bash
./gradlew :app:compileDebugKotlin -x kspDebugKotlin --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2 2>&1 | tee /tmp/repair-t2.log | grep -c "^e: "
```
Expected: fewer than 768, and `grep vshape /tmp/repair-t2.log` prints nothing.

- [ ] **Step 4: Commit**
```bash
git add -A && git commit -m "refactor: remove unbuildable orphaned VShape cluster

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3. **Acceptance:** keepers intact, zero dangling refs, error count drops. **Risk:** low (everything deleted is unwired). **Effort:** ~15 min.

### Task 3: Delete SearchScreen and the orphaned broken shared components

**Files (delete):**
- `app/src/main/kotlin/com/gymcoach/app/ui/SearchScreen.kt` — no package decl, no imports, references nonexistent `SearchViewModel`/`CommunitySearchResult`; it alone breaks the whole main source set.
- `app/src/main/kotlin/com/gymcoach/app/presentation/components/Accessibility.kt` — referenced only by `ProgressDashboardScreen.kt:50` (a broken import that Task 18 removes); its symbols (`Accessibility`, `buttonSemantics`, `screenSemantics`) are used nowhere.
- `app/src/main/kotlin/com/gymcoach/app/presentation/components/DragAndDropComponents.kt` — zero references.
- `app/src/main/kotlin/com/gymcoach/app/presentation/components/WorkoutComponents.kt` — zero references (broken: uses nonexistent `mutableIntStateOf` import path).
- `app/src/main/kotlin/com/gymcoach/app/presentation/components/PlateCalculatorDialog.kt` — zero references (broken: wrong `rememberCollectable` arity).

**Interfaces:**
- Consumes: nothing.
- Produces: nothing — later tasks must NOT import `com.gymcoach.app.presentation.components.Accessibility` (Task 18 removes that import).

- [ ] **Step 1: Delete the 5 files**
```bash
cd /root/Projects/GymCoach
git rm app/src/main/kotlin/com/gymcoach/app/ui/SearchScreen.kt \
       app/src/main/kotlin/com/gymcoach/app/presentation/components/Accessibility.kt \
       app/src/main/kotlin/com/gymcoach/app/presentation/components/DragAndDropComponents.kt \
       app/src/main/kotlin/com/gymcoach/app/presentation/components/WorkoutComponents.kt \
       app/src/main/kotlin/com/gymcoach/app/presentation/components/PlateCalculatorDialog.kt
```

- [ ] **Step 2: Verify no dangling references**
```bash
grep -rln "SearchScreen\|DragAndDropComponents\|WorkoutComponents\|PlateCalculatorDialog\|buttonSemantics\|screenSemantics" app/src/main/kotlin || echo "no dangling refs"
```
Expected: `no dangling refs` (the known `Accessibility` import in `ProgressDashboardScreen.kt:50` will remain — it names `Accessibility` itself; Step 3 confirms the compiler still lists it, and Task 18 removes it).

- [ ] **Step 3: Build** (command from Task 1 Step 2). Expected: error count drops further; the only remaining reference to deleted symbols is `ProgressDashboardScreen.kt:50`'s `Unresolved reference: Accessibility`.

- [ ] **Step 4: Commit**
```bash
git add -A && git commit -m "refactor: remove orphaned broken SearchScreen and shared components

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3. **Acceptance:** zero `SearchScreen`/component references remain in `app/src/main`. **Risk:** low. **Effort:** ~10 min.

### Task 4: Delete the Goal cluster and un-wire it from Room and Hilt

**Files (delete):**
- `app/src/main/kotlin/com/gymcoach/app/data/local/entity/GoalEntity.kt` — its TypeConverters reference nonexistent `domain.vshape.model.GoalType/GoalStatus/GoalPriority`.
- `app/src/main/kotlin/com/gymcoach/app/data/local/dao/GoalDao.kt`
- `app/src/main/kotlin/com/gymcoach/app/data/repository/GoalRepository.kt`

**Files (modify):**
- `app/src/main/kotlin/com/gymcoach/app/data/local/database/GymCoachDatabase.kt`
- `app/src/main/kotlin/com/gymcoach/app/core/di/AppModule.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `GymCoachDatabase` and `AppModule` no longer mention `GoalDao`/`GoalEntity`; Room schema drops the `goals` table (safe via `fallbackToDestructiveMigration()`).

- [ ] **Step 1: Delete the 3 files**
```bash
cd /root/Projects/GymCoach
git rm app/src/main/kotlin/com/gymcoach/app/data/local/entity/GoalEntity.kt \
       app/src/main/kotlin/com/gymcoach/app/data/local/dao/GoalDao.kt \
       app/src/main/kotlin/com/gymcoach/app/data/repository/GoalRepository.kt
```

- [ ] **Step 2: Edit `GymCoachDatabase.kt`** — four removals:
  1. Remove `import com.gymcoach.app.data.local.dao.GoalDao` (line 12)
  2. Remove `import com.gymcoach.app.data.local.entity.GoalEntity` (line 19)
  3. Remove `GoalEntity::class,` from the `entities = [...]` list
  4. Remove `abstract fun goalDao(): GoalDao` from the class body

- [ ] **Step 3: Edit `AppModule.kt`** — two removals:
  1. Remove `import com.gymcoach.app.data.local.dao.GoalDao`
  2. Remove the provider block:
```kotlin
    @Provides
    @Singleton
    fun provideGoalDao(database: GymCoachDatabase): GoalDao = database.goalDao()
```

- [ ] **Step 4: Build** (Task 1 Step 2 command). Expected: error count drops; no `Goal` errors remain.

- [ ] **Step 5: Commit**
```bash
git add -A && git commit -m "refactor: remove unbuildable Goal cluster and drop goals table

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 4. **Acceptance:** no `Goal` references anywhere; Room/Hilt no longer reference the table. **Risk:** low — Goals were unwired, and the destructive-migration fallback tolerates a dropped table. **Effort:** ~15 min.

### Task 5: Delete the Workout Templates cluster and BackupManager

**Files (delete):**
- `app/src/main/kotlin/com/gymcoach/app/domain/repository/WorkoutTemplatesRepository.kt`
- `app/src/main/kotlin/com/gymcoach/app/data/repository/WorkoutTemplatesRepositoryImpl.kt`
- `app/src/main/kotlin/com/gymcoach/app/core/backup/BackupManager.kt` — imports `androidx.datastore.*`, `kotlinx.serialization.*`, `BuildConfig`, none of which are declared dependencies; orphaned (Settings buttons are stubs).

**Interfaces:**
- Consumes: nothing. Note `WorkoutDao.getAllWorkoutTemplates()*` **stay** — they are harmless Room queries used by the templates feature later; do not touch `WorkoutDao`.

- [ ] **Step 1: Delete the 3 files**
```bash
cd /root/Projects/GymCoach
git rm app/src/main/kotlin/com/gymcoach/app/domain/repository/WorkoutTemplatesRepository.kt \
       app/src/main/kotlin/com/gymcoach/app/data/repository/WorkoutTemplatesRepositoryImpl.kt \
       app/src/main/kotlin/com/gymcoach/app/core/backup/BackupManager.kt
```

- [ ] **Step 2: Verify no dangling references**
```bash
grep -rln "WorkoutTemplatesRepository\|BackupManager" app/src/main/kotlin || echo "no dangling refs"
```
Expected: `no dangling refs` (SettingsScreen references strings like `"Manual Backup"`, not the class).

- [ ] **Step 3: Build** (Task 1 Step 2 command). Expected: error count drops further.

- [ ] **Step 4: Commit**
```bash
git add -A && git commit -m "refactor: remove orphaned templates and backup code

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3. **Acceptance:** no template/backup references remain. **Risk:** low. **Effort:** ~10 min.

### Task 6: Phase 1 gate — re-measure the error surface

**Files:** none (verification only)

**Problem:** After deleting 28 files we must confirm the remaining errors are exactly the *repairable* core (shared components, data layer, screens, nav, secondary screens).

- [ ] **Step 1: Run the fast compile**
```bash
cd /root/Projects/GymCoach
./gradlew :app:compileDebugKotlin -x kspDebugKotlin --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2 2>&1 | tee /tmp/repair-t6.log | grep -c "^e: "
```
- [ ] **Step 2: Assert the remaining error set is exactly the repair targets**
```bash
grep "^e: " /tmp/repair-t6.log | grep -icE "vshape|search|goal|template|backup|draganddrop|workoutcomponents|platecalculator|accessibility" 
```
Expected: count = 0 for deleted clusters. Remaining files must be a subset of: `ui/theme/Theme.kt`, `presentation/components/{ErrorState,EmptyState,ExerciseItemCard,ExerciseVideoPlayer}.kt`, `data/**`, `domain/measurement/**`, `presentation/{list,detail,history,progress,profile,measurement,screens,settings,pr,workout}/**`, `ui/GymCoachNavHost.kt`, `core/di/**`, `core/timer/**`.

**Dependencies:** Tasks 2–5. **Tests:** Steps 1–2. **Acceptance:** zero errors in deleted clusters. **Risk:** none. **Effort:** ~10 min.

---

## Phase 2 — Shared Components, Data Layer, DI

Fix in dependency order: the shared components every screen imports, then repositories/DAOs, then measurement-domain dedup, then DI bindings. Each task is small and independently build-verified.

### Task 7: Fix `Theme.kt` (non-existent Material3 API)

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/ui/theme/Theme.kt`

**Problem (verified):** `androidx.compose.material3.shape.RoundedCornerShape` does not exist (`RoundedCornerShape` lives in `androidx.compose.foundation.shape`), and `androidx.compose.material3.shapes` is not a function in material3 1.2.0 (only the `Shapes` class exists). Errors at lines 12–13, 26–29.

**Solution:** fix imports and construct `Shapes` via its class constructor; rename the val to avoid shadowing.

- [ ] **Step 1: Replace the two imports**
```kotlin
-import androidx.compose.material3.shape.RoundedCornerShape
-import androidx.compose.material3.shapes
+import androidx.compose.foundation.shape.RoundedCornerShape
+import androidx.compose.material3.Shapes
```

- [ ] **Step 2: Replace the val definition and its use**
```kotlin
-private val Shapes = shapes(
+private val AppShapes = Shapes(
     small = RoundedCornerShape(4.dp),
     medium = RoundedCornerShape(8.dp),
     large = RoundedCornerShape(12.dp)
 )
```
and inside `GymCoachTheme`'s `MaterialTheme(...)` call:
```kotlin
-        shapes = Shapes,
+        shapes = AppShapes,
```

- [ ] **Step 3: Build** (Task 1 Step 2 command). Expected: `Theme.kt` has zero errors.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/ui/theme/Theme.kt && git commit -m "fix: use valid Material3 Shapes constructor in theme

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3. **Acceptance:** `Theme.kt` compiles. **Risk:** low. **Effort:** ~10 min.

### Task 8: Fix `ErrorState.kt` and `EmptyState.kt`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/components/ErrorState.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/components/EmptyState.kt`

**Problems (verified):** `ErrorState.kt` uses `Column`, `Spacer`, `Modifier.size`, `Modifier.height` without imports (14 errors). `EmptyState.kt` references `androidx.compose.material.icons.Icons.Filled.Info` fully-qualified — icon extensions cannot be referenced fully-qualified; needs imports.

- [ ] **Step 1: `ErrorState.kt` — add the missing foundation imports** (after the existing `androidx.compose.foundation.layout.Box` import):
```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
```

- [ ] **Step 2: `EmptyState.kt` — add icon imports and use the short form**
```kotlin
+import androidx.compose.material.icons.Icons
+import androidx.compose.material.icons.filled.Info
```
and change the default parameter:
```kotlin
-    icon: ImageVector = androidx.compose.material.icons.Icons.Filled.Info
+    icon: ImageVector = Icons.Filled.Info
```

- [ ] **Step 3: Build** (Task 1 Step 2 command). Expected: both files compile.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/components/ErrorState.kt \
        app/src/main/kotlin/com/gymcoach/app/presentation/components/EmptyState.kt && \
git commit -m "fix: restore missing imports in state components

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3. **Acceptance:** both compile. **Risk:** low. **Effort:** ~10 min.

### Task 9: Fix `ExerciseItemCard.kt`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/components/ExerciseItemCard.kt`

**Problem (verified):** bogus import `androidx.compose.foundation.verticalGrid` (line 15, doesn't exist); missing imports `height`, `PaddingValues`, `Card`, `CardDefaults`, `Button` (12 errors).

- [ ] **Step 1: Remove the bogus import**
```kotlin
-import androidx.compose.foundation.verticalGrid
```

- [ ] **Step 2: Add the missing imports** (alphabetical, in the `androidx.compose.foundation.layout.*` and `androidx.compose.material3.*` blocks):
```kotlin
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
```

- [ ] **Step 3: Build** (Task 1 Step 2 command). Expected: `ExerciseItemCard.kt` compiles. If the compiler reports any *further* `ExerciseItemCard.kt` errors, each is a missing import of the same shape — add the corresponding `androidx.compose.material3.*` import and re-run (do not change card behavior).

- [ ] **Step 4: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/components/ExerciseItemCard.kt && \
git commit -m "fix: restore imports in ExerciseItemCard

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3. **Acceptance:** compiles. **Risk:** low. **Effort:** ~10 min.

### Task 10: Fix `ExerciseVideoPlayer.kt` (compile-only; still unreferenced)

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/components/ExerciseVideoPlayer.kt`

**Problem (verified):** missing imports `Column`, `Arrangement`; the `VideoControlBar(...)` call passes named args `currentPosition`/`duration` that the function does not accept (8 errors).

- [ ] **Step 1: Add the two missing imports**
```kotlin
+import androidx.compose.foundation.layout.Arrangement
+import androidx.compose.foundation.layout.Column
```

- [ ] **Step 2: Remove the two invalid named arguments from the `VideoControlBar(...)` call**
```kotlin
         VideoControlBar(
             isPlaying = isPlaying,
             hasEnded = hasEnded,
-            currentPosition = currentPosition,
-            duration = duration,
             onTogglePlayPause = {
```

- [ ] **Step 3: Build** (Task 1 Step 2 command). Expected: `ExerciseVideoPlayer.kt` compiles.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/components/ExerciseVideoPlayer.kt && \
git commit -m "fix: make ExerciseVideoPlayer compile (still unreferenced)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3. **Acceptance:** compiles; the file remains unreferenced (video stays out of v1 scope). **Risk:** low. **Effort:** ~10 min.

### Task 11: Fix the data-layer errors

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/repository/WorkoutRepositoryImpl.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/repository/UserProfileRepositoryImpl.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/local/dao/MeasurementDao.kt`

**Problems (verified):**
1. `WorkoutRepositoryImpl.kt:188` — `getWorkoutWithDetails(workoutId).firstOrNull()` needs `import kotlinx.coroutines.flow.firstOrNull`.
2. `UserProfileRepositoryImpl.kt:16` — `userProfileDao.getUserProfile()` is called without the required `userId` arg (DAO signature: `getUserProfile(userId: Long)`).
3. `MeasurementDao.kt` — `getMeasurementsByTypes(userId: String, types: List<String>)` has an unused `userId` parameter that KSP flags (`Unused parameter: userId`), breaking the KSP phase.

- [ ] **Step 1: `WorkoutRepositoryImpl.kt` — add the flow import**
```kotlin
+import kotlinx.coroutines.flow.firstOrNull
```

- [ ] **Step 2: `UserProfileRepositoryImpl.kt` — pass the id (the app is single-user; id is always `1L`)**
```kotlin
-        return userProfileDao.getUserProfile().map { it?.toDomain() }
+        return userProfileDao.getUserProfile(1L).map { it?.toDomain() }
```

- [ ] **Step 3: `MeasurementDao.kt` — drop the unused parameter** (the query does not filter by `userId`):
```kotlin
-    fun getMeasurementsByTypes(userId: String, types: List<String>): Flow<List<MeasurementRecordEntity>>
+    fun getMeasurementsByTypes(types: List<String>): Flow<List<MeasurementRecordEntity>>
```
Then grep for callers and update them (expected: none call this method today — confirm with the compiler).

- [ ] **Step 4: Build** (Task 1 Step 2 command). Expected: these three files have zero errors.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/data/repository/WorkoutRepositoryImpl.kt \
        app/src/main/kotlin/com/gymcoach/app/data/repository/UserProfileRepositoryImpl.kt \
        app/src/main/kotlin/com/gymcoach/app/data/local/dao/MeasurementDao.kt && \
git commit -m "fix: repair data-layer repository and DAO signatures

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 4. **Acceptance:** three files compile; KSP `Unused parameter` error gone. **Risk:** low. **Effort:** ~10 min.

### Task 12: De-duplicate the measurement use-case layer

**Files (delete):**
- `app/src/main/kotlin/com/gymcoach/app/domain/measurement/usecase/MeasurementUseCases.kt` — declares `SaveMeasurementUseCase`/`GetLatestMeasurementUseCase`/`GetMeasurementsUseCase`/`ValidateMeasurementUseCase` interfaces, and its `GetLatestMeasurementUseCase` redeclares the same name as the concrete class → `Redeclaration` error.
- `app/src/main/kotlin/com/gymcoach/app/domain/measurement/usecase/impl/MeasurementUseCasesImpl.kt` — broken impl of those interfaces (calls `repository.getLatestMeasurement(userId, type)` and `Result.success` which do not exist).

**Files (keep):** the concrete `@Inject` classes the `MeasurementViewModel` actually uses: `AddMeasurementUseCase.kt`, `GetMeasurementsForUserUseCase.kt`, `GetMeasurementsByTypeUseCase.kt`, `GetMeasurementTrendUseCase.kt`, `UpdateMeasurementUseCase.kt`, `GetLatestMeasurementUseCase.kt` (class), plus their `domain/measurement/usecase/model/*` (none) and `domain/repository/MeasurementRepository.kt`.

**Interfaces:**
- Consumes: nothing.
- Produces: package `com.gymcoach.app.domain.measurement.usecase` now contains only the concrete classes; `MeasurementViewModel` keeps importing `AddMeasurementUseCase`, `GetMeasurementsForUserUseCase`, `GetMeasurementTrendUseCase`, `UpdateMeasurementUseCase`.

- [ ] **Step 1: Verify nothing outside the impls references the interfaces**
```bash
cd /root/Projects/GymCoach
grep -rln "SaveMeasurementUseCase\|GetMeasurementsUseCase\|ValidateMeasurementUseCase" app/src/main/kotlin
```
Expected output: only the two files being deleted in this task. (If anything else matches, stop and resolve it before deleting.)

- [ ] **Step 2: Delete the two files**
```bash
git rm app/src/main/kotlin/com/gymcoach/app/domain/measurement/usecase/MeasurementUseCases.kt \
       app/src/main/kotlin/com/gymcoach/app/domain/measurement/usecase/impl/MeasurementUseCasesImpl.kt
```

- [ ] **Step 3: Build** (Task 1 Step 2 command). Expected: `Redeclaration` and impl errors gone.

- [ ] **Step 4: Commit**
```bash
git add -A && git commit -m "refactor: remove duplicate measurement use-case interfaces and impl

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Tasks 2 (vshape impls already deleted), 11. **Tests:** Step 3. **Acceptance:** zero references to the deleted interfaces; `GetLatestMeasurementUseCase` class compiles. **Risk:** low. **Effort:** ~10 min.

### Task 13: Bind `ProfileRepository` in DI

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/core/di/RepositoryModule.kt`

**Problem (verified):** `ProfileViewModel` injects `ProfileRepository`, whose only implementation (`ProfileRepositoryImpl`, `@Inject`) is **not bound** — Hilt fails at the `ProfileViewModel` injection point.

**Interfaces:**
- Consumes: `ProfileRepositoryImpl` (exists), `ProfileRepository` (exists).
- Produces: `ProfileViewModel` (and `ProfileAnalyticsViewModel`) can now inject `ProfileRepository`.

- [ ] **Step 1: Add the imports**
```kotlin
+import com.gymcoach.app.data.repository.ProfileRepositoryImpl
+import com.gymcoach.app.domain.repository.ProfileRepository
```

- [ ] **Step 2: Add the binding** (before the closing brace of `RepositoryModule`):
```kotlin
    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository
```

- [ ] **Step 3: Build** (Task 1 Step 2 command). Expected: `RepositoryModule.kt` compiles. (Hilt's full check runs in Task 23; the Kotlin compiler confirms the `@Binds` signature here.)

- [ ] **Step 4: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/core/di/RepositoryModule.kt && \
git commit -m "fix: bind ProfileRepository in DI

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3 (+ Task 23). **Acceptance:** `@Binds` compiles. **Risk:** low. **Effort:** ~10 min.

---

## Phase 3 — Core Screens & Navigation

Fix the screens that implement the committed product: Exercise Library, Workout Session, History, Progress. **Screen signatures first** (Tasks 15–18), then the NavHost call sites (Task 14) so the NavHost edits reference stable signatures.

### Task 14: Fix `GymCoachNavHost.kt`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt`

**Problem (verified, 10 errors):**
1. `:52` `ExerciseListScreen(onBackClick = ...)` — `ExerciseListScreen` has no `onBackClick`; it requires `navController` and takes `onExerciseClick`/`onHistoryClick`/`onProgressClick`/`onCameraClick`.
2. `:58`, `:70`, `:107` `backStackEntry.getLong(...)` — `NavBackStackEntry` has no `getLong`; use `backStackEntry.arguments?.getLong(...)`.
3. `:64` `WorkoutHistoryScreen(onBackClick = ...)` — the screen requires `onDetailClick` and `onResumeWorkout`.
4. `:77` `navArgument("workoutId") { type = NavType.LongType, defaultValue = "" }` — the comma inside the builder lambda is a syntax error (`Unexpected tokens`), and `defaultValue = ""` is invalid for `LongType`.
5. `:79` `backStackEntry.getString(...)` — same Bundle access problem as (2).
6. `:92` `CameraPreviewScreen(onBackClick = ...)` — `CameraPreviewScreen()` takes no parameters.

**Solution:** rewrite the affected blocks (signatures of the callees are fixed in Tasks 15–18; verify they exist first, then apply).

- [ ] **Step 1: Fix the EXERCISE_LIST composable** (`:51-53`)
```kotlin
        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(
                navController = navController,
                onExerciseClick = { navController.navigate(Routes.exerciseDetail(it)) },
                onHistoryClick = { navController.navigate(Routes.WORKOUT_HISTORY) },
                onProgressClick = { navController.navigate(Routes.PROGRESS) },
                onCameraClick = { navController.navigate(Routes.CAMERA) }
            )
        }
```

- [ ] **Step 2: Fix the EXERCISE_DETAIL argument read** (`:58`)
```kotlin
-            val exerciseId = backStackEntry.getLong("exerciseId", -1)
+            val exerciseId = backStackEntry.arguments?.getLong("exerciseId", -1L) ?: -1L
```

- [ ] **Step 3: Fix the WORKOUT_HISTORY composable** (`:63-65`)
```kotlin
        composable(Routes.WORKOUT_HISTORY) {
            WorkoutHistoryScreen(
                onBackClick = { navController.popBackStack() },
                onDetailClick = { navController.navigate(Routes.workoutHistoryDetail(it)) },
                onResumeWorkout = { navController.navigate(Routes.workoutSession(it)) }
            )
        }
```

- [ ] **Step 4: Fix the WORKOUT_HISTORY_DETAIL argument read** (`:70`)
```kotlin
-            val workoutId = backStackEntry.getLong("workoutId", -1)
+            val workoutId = backStackEntry.arguments?.getLong("workoutId", -1L) ?: -1L
```

- [ ] **Step 5: Fix the WORKOUT_SESSION arguments + read** (`:76-80`)
```kotlin
        composable(
            route = Routes.WORKOUT_SESSION,
            arguments = listOf(
                navArgument("workoutId") {
                    type = NavType.LongType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId")
```

- [ ] **Step 6: Fix the CAMERA composable** (`:92`)
```kotlin
-            CameraPreviewScreen(onBackClick = { navController.popBackStack() })
+            CameraPreviewScreen()
```

- [ ] **Step 7: Fix the WORKOUT_SUMMARY argument read** (`:107`)
```kotlin
-            val workoutId = backStackEntry.getLong("workoutId", -1)
+            val workoutId = backStackEntry.arguments?.getLong("workoutId", -1L) ?: -1L
```

- [ ] **Step 8: Build** (Task 1 Step 2 command). Expected: `GymCoachNavHost.kt` has zero errors. If the compiler reports a remaining param-name mismatch in any callee, fix that screen's signature (it is one of Tasks 15–18) and re-run.

- [ ] **Step 9: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/ui/GymCoachNavHost.kt && \
git commit -m "fix: repair navigation host call sites and argument reads

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Tasks 15–18 (screen signatures). **Tests:** Step 8. **Acceptance:** NavHost compiles. **Risk:** medium — the NavHost is the wiring for the whole app; changes are mechanical and verified by the compiler. **Effort:** ~25 min.

### Task 15: Fix `ExerciseListScreen.kt`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/list/ExerciseListScreen.kt`

**Problem (verified, 28 errors):** missing imports `size`, `height`, `width`, `MaterialTheme`, `Button`, `ModalBottomSheetState`, `com.gymcoach.app.ui.Routes`; undefined `categories` in `NavigationActions`; `items(...)` used inside a non-lazy `ScrollView`.

- [ ] **Step 1: Add missing imports** (in the appropriate existing blocks)
```kotlin
+import androidx.compose.foundation.layout.height
+import androidx.compose.foundation.layout.size
+import androidx.compose.foundation.layout.width
+import androidx.compose.material3.Button
+import androidx.compose.material3.MaterialTheme
+import androidx.compose.material3.ModalBottomSheetState
+import com.gymcoach.app.ui.Routes
```

- [ ] **Step 2: Replace the undefined `categories` iteration in `NavigationActions`** with a static label (the tab row is decorative — `selectedTabIndex = 0`, `onClick = {}`)
```kotlin
-            categories.forEachIndexed { index, category ->
-                Tab(
-                    selected = index == 0,
-                    onClick = {},
-                    text = { Text(category) }
-                )
-            }
+            listOf("All").forEachIndexed { index, category ->
+                Tab(
+                    selected = index == 0,
+                    onClick = {},
+                    text = { Text(category) }
+                )
+            }
```

- [ ] **Step 3: Replace the non-lazy `ScrollView` with `LazyColumn`** in `mainContent` so `items(...)` resolves (the `LazyColumn` import already exists at line 10)
```kotlin
-        ScrollView(
+        LazyColumn(
             modifier = Modifier.fillMaxSize(),
             contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
         ) {
```

- [ ] **Step 4: Build** (Task 1 Step 2 command). Expected: `ExerciseListScreen.kt` has zero errors.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/list/ExerciseListScreen.kt && \
git commit -m "fix: restore imports and list scoping in ExerciseListScreen

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 4. **Acceptance:** compiles. **Risk:** low. **Effort:** ~15 min.

### Task 16: Fix `ExerciseDetailScreen.kt`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/detail/ExerciseDetailScreen.kt`

**Problem (verified, 6 errors):** missing `semantics`/`contentDescription` imports (lines 98–99); `components.Badge(...)` / `components.getDifficultyColor(...)` (lines 301–311) don't resolve — `components` is not a value in Kotlin; the functions are top-level in `com.gymcoach.app.presentation.components`.

- [ ] **Step 1: Add imports**
```kotlin
+import androidx.compose.ui.semantics.contentDescription
+import androidx.compose.ui.semantics.semantics
+import com.gymcoach.app.presentation.components.Badge
+import com.gymcoach.app.presentation.components.getDifficultyColor
```

- [ ] **Step 2: Replace `components.` prefixes** (4 occurrences of `components.Badge(` and 1 of `components.getDifficultyColor(`)
```kotlin
-            components.Badge(
+            Badge(
...
-                backgroundColor = components.getDifficultyColor(difficulty),
+                backgroundColor = getDifficultyColor(difficulty),
```

- [ ] **Step 3: Build** (Task 1 Step 2 command). Expected: `ExerciseDetailScreen.kt` has zero errors.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/detail/ExerciseDetailScreen.kt && \
git commit -m "fix: restore semantics imports and component references in detail screen

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 3. **Acceptance:** compiles. **Risk:** low. **Effort:** ~10 min.

### Task 17: Fix the history screens

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryScreen.kt`

**Problem (verified, 15 errors):**
1. `:91-92` missing `semantics`/`contentDescription` imports.
2. `:233-255` missing date-picker imports (`rememberDateRangePickerState`, `DatePickerDialog`, `DateRangePicker` are `androidx.compose.material3`, experimental).
3. `:369-372`, `:387` — `StatItem(label, value)` is declared **both** as `private` here (`:388`) and `public` in `WorkoutHistoryDetailScreen.kt:383`; the two declarations conflict and make every call ambiguous.
4. `:412` `.padding(padding = ...)` — use positional argument to be version-safe.
5. `:417` `EmptyStateScreen()` references `searchQuery` which is not in its scope.

**Interfaces:**
- Produces: renames the private `StatItem` → `HistoryStatItem`, removing the cross-file conflict so `WorkoutHistoryDetailScreen.kt`'s `StatItem` (line 383) is the only one.

- [ ] **Step 1: Add imports**
```kotlin
+import androidx.compose.material3.DatePickerDialog
+import androidx.compose.material3.DateRangePicker
+import androidx.compose.material3.rememberDateRangePickerState
+import androidx.compose.ui.semantics.contentDescription
+import androidx.compose.ui.semantics.semantics
```
(Add `@OptIn(ExperimentalMaterial3Api::class)` to the composable that hosts the date picker if the compiler reports "This material API is experimental".)

- [ ] **Step 2: Rename the private `StatItem`** (definition `:388` + the 4 call sites `:369-372`) so it no longer collides with the detail screen's:
```kotlin
-private fun StatItem(label: String, value: String) {
+private fun HistoryStatItem(label: String, value: String) {
```
and at each call site `StatItem(label = ...` → `HistoryStatItem(label = ...` (4 sites).

- [ ] **Step 3: Fix the `EmptyStateScreen`** — add a parameter and pass it (call site `:222`):
```kotlin
 @Composable
-private fun EmptyStateScreen() {
+private fun EmptyStateScreen(searchQuery: String) {
```
and at the call site:
```kotlin
-                EmptyStateScreen()
+                EmptyStateScreen(searchQuery = searchQuery)
```

- [ ] **Step 4: Fix the `.padding(padding = ...)` call** (`:412`) to use positional form:
```kotlin
-            .padding(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)),
+            .padding(androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)),
```

- [ ] **Step 5: Build** (Task 1 Step 2 command). Expected: `WorkoutHistoryScreen.kt` and `WorkoutHistoryDetailScreen.kt` (via the removed conflict) have zero errors.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryScreen.kt && \
git commit -m "fix: repair history screen imports and StatItem conflict

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 5. **Acceptance:** history screens compile; `Conflicting overloads` error gone. **Risk:** low. **Effort:** ~20 min.

### Task 18: Fix `ProgressDashboardScreen.kt`

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressDashboardScreen.kt`

**Problem (verified, 47 errors):** broken `Accessibility` import (`:50`); missing `MuscleGroupStats` import (`:69`); missing `Box`, `width`, `size`, grid imports; `onSearch` referenced but not a parameter of `ProgressDataContent` (`:273`); `VolumeLineChart` signature has an unnamed parameter (`:623`); grid sections use the nonexistent `androidx.compose.foundation.grid.GridCells` package.

**Interfaces:**
- Consumes: `ProgressViewModel` (defined at the bottom of this same file) exposes `state: StateFlow<ProgressUiState>`, `onSearchQueryChange(query: String)`, `onRefresh()`. `AnalyticsRepository` exposes `data class MuscleGroupStats(...)` (`domain/repository/AnalyticsRepository.kt:38`).
- Produces: `ProgressDataContent(state, onSearchQueryChange, modifier)`.

- [ ] **Step 1: Fix imports** — remove the broken one, add the missing ones
```kotlin
-import com.gymcoach.app.presentation.components.Accessibility
+import androidx.compose.foundation.layout.Box
+import androidx.compose.foundation.layout.size
+import androidx.compose.foundation.layout.width
+import androidx.compose.foundation.lazy.grid.GridCells
+import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
+import androidx.compose.foundation.lazy.grid.items
+import com.gymcoach.app.domain.repository.MuscleGroupStats
```

- [ ] **Step 2: Fix the `VolumeLineChart` signature** (`:623`) — the parameter needs a name
```kotlin
-@Composable
-private fun VolumeLineChart(
-    List<Pair<Date, Double>>,
+@Composable
+private fun VolumeLineChart(
+    data: List<Pair<Date, Double>>,
     title: String,
     modifier: Modifier = Modifier
 ) {
```

- [ ] **Step 3: Fix the grid sections** — replace the nonexistent package reference in all 4 grid sections (`WeeklySummarySection`, `MonthlySummarySection`, `TopExercisesSection`, `PersonalRecordsSection`)
```kotlin
-                columns = androidx.compose.foundation.grid.GridCells.Fixed(2),
+                columns = GridCells.Fixed(2),
```

- [ ] **Step 4: Fix the `onSearch` wiring** — add a parameter to `ProgressDataContent` and thread it from the caller
```kotlin
 @Composable
 private fun ProgressDataContent(
     state: ProgressUiState,
+    onSearchQueryChange: (String) -> Unit,
     modifier: Modifier = Modifier
 ) {
     Column(modifier = modifier.verticalScroll(rememberScrollState())) {
         SearchField(
             value = state.searchQuery,
-            onValueChange = { onSearch },
+            onValueChange = onSearchQueryChange,
             modifier = Modifier.fillMaxWidth().padding(16.dp)
         )
```
and at the call site (`:199-202`):
```kotlin
                 ProgressDataContent(
                     state = state,
+                    onSearchQueryChange = viewModel::onSearchQueryChange,
                     modifier = Modifier.padding(padding)
                 )
```

- [ ] **Step 5: Build** (Task 1 Step 2 command). Expected: `ProgressDashboardScreen.kt` has zero errors. If the compiler reports any further import gaps in this file, add the matching `androidx.compose.*` import (same shape as Step 1) and re-run.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressDashboardScreen.kt && \
git commit -m "fix: repair progress dashboard imports, chart signature, and search wiring

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Tasks 3 (deleted `Accessibility`), 7–10. **Tests:** Step 5. **Acceptance:** compiles. **Risk:** medium — large file, but each fix is isolated and compiler-verified. **Effort:** ~30 min.

### Task 19: Phase 3 gate — core compiles

**Files:** none (verification only)

**Problem:** Confirm the wired core (Exercise Library, Workout Session, History, Progress, shared components, nav) now compiles as a unit.

- [ ] **Step 1: Run the fast compile**
```bash
cd /root/Projects/GymCoach
./gradlew :app:compileDebugKotlin -x kspDebugKotlin --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2 2>&1 | tee /tmp/repair-t19.log
```
- [ ] **Step 2: Assert zero errors in the core package set**
```bash
grep "^e: " /tmp/repair-t19.log | grep -E "presentation/(list|detail|history|workout|progress|components)/|ui/(GymCoachNavHost|MainActivity|theme)|core/timer|data/repository|domain/repository|domain/model|core/di" || echo "CORE CLEAN"
```
Expected: `CORE CLEAN`. Remaining errors (if any) must be confined to `presentation/{profile,measurement,settings,pr}/`, `domain/measurement/`, `domain/vshape/model/`.

**Dependencies:** Tasks 14–18. **Tests:** Steps 1–2. **Acceptance:** core package set has zero compile errors. **Risk:** none. **Effort:** ~10 min.

---

## Phase 4 — Secondary Screens

The user-approved keep-list screens that sit outside the core product flows: Profile, Measurements, Settings. These run after the foundation is stable (per plan requirement #9).

### Task 20: Fix the Profile subsystem

**Files (modify):**
- `app/src/main/kotlin/com/gymcoach/app/presentation/profile/ProfileViewModel.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/profile/ProfileAnalyticsViewModel.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/profile/ProfileSettingsViewModel.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/profile/ProfileScreen.kt`

**Problem (verified):**
1. `ProfileViewModel.kt` (54 errors) — missing `import androidx.lifecycle.ViewModel` (so `: ViewModel()` and `viewModelScope` fail, Hilt rejects the class); `ProfileUiState` is a `sealed class` but is instantiated directly via `ProfileUiState()` (`:99` "Sealed types cannot be instantiated"); `import com.gymcoach.app.domain.repository.WorkoutStats` is dead (class doesn't exist — remove); **and** it embeds duplicate `ProfileSettingsViewModel` (`:273`) and `ProfileAnalyticsViewModel` (`:349`) that also exist as standalone files → `Redeclaration` errors.
2. `ProfileAnalyticsViewModel.kt` (18 errors) — same `ViewModel` import, same sealed-instantiation (`ProfileAnalyticsUiState()`), same dead `WorkoutStats` import.
3. `ProfileSettingsViewModel.kt` (12 errors) — same `ViewModel` import + sealed `ProfileSettingsState`.
4. `ProfileScreen.kt` (3 errors) — `hiltViewModel()` type mismatch (clears once ProfileViewModel subclasses ViewModel); smart-cast of `state.error`.

**Interfaces:**
- Consumes: `ProfileRepository` (now bound — Task 13), `WorkoutRepository`, `AnalyticsRepository`.
- Produces: `ProfileViewModel` is the only VM in `ProfileViewModel.kt`; the two standalone files own `ProfileSettingsViewModel` / `ProfileAnalyticsViewModel`. `ProfileUiState`, `ProfileSettingsState`, `ProfileAnalyticsUiState` become `data class`es so `ProfileUiState()`/`.copy()` work.

- [ ] **Step 1: `ProfileViewModel.kt` — add lifecycle imports, drop the dead import**
```kotlin
+import androidx.lifecycle.ViewModel
+import androidx.lifecycle.viewModelScope
-import com.gymcoach.app.domain.repository.WorkoutStats
```

- [ ] **Step 2: `ProfileViewModel.kt` — make the state class instantiable**
```kotlin
-sealed class ProfileUiState(
+data class ProfileUiState(
```

- [ ] **Step 3: `ProfileViewModel.kt` — delete the duplicated classes.** Truncate the file at the end of `ProfileViewModel` (the line before the `@HiltViewModel` that introduces `ProfileSettingsViewModel`, currently `:272`). This removes the embedded `ProfileSettingsViewModel`, `ProfileAnalyticsViewModel`, `ProfileSettingsState`, and `ProfileAnalyticsUiState` — all of which live on in their standalone files. Use a text editor to delete from the `@HiltViewModel\nclass ProfileSettingsViewModel` marker through end-of-file.

- [ ] **Step 4: `ProfileAnalyticsViewModel.kt` — same three fixes**
```kotlin
+import androidx.lifecycle.ViewModel
+import androidx.lifecycle.viewModelScope
-import com.gymcoach.app.domain.repository.WorkoutStats
...
-sealed class ProfileAnalyticsUiState(
+data class ProfileAnalyticsUiState(
```

- [ ] **Step 5: `ProfileSettingsViewModel.kt` — same two fixes**
```kotlin
+import androidx.lifecycle.ViewModel
+import androidx.lifecycle.viewModelScope
...
-sealed class ProfileSettingsState(
+data class ProfileSettingsState(
```

- [ ] **Step 6: `ProfileScreen.kt` — fix the smart-cast** (`:70`); read the value into a local first:
```kotlin
-                state.error != null -> ErrorState(message = state.error, onRetry = viewModel::onRetry, modifier = Modifier.fillMaxSize())
+                state.error != null -> ErrorState(message = state.error ?: "", onRetry = viewModel::onRetry, modifier = Modifier.fillMaxSize())
```

- [ ] **Step 7: Fix the save double-write** (data-layer correctness, per plan requirement #4). In `ProfileViewModel.onSaveProfile`, `saveUserProfile(profile)` already upserts via `@Insert(onConflict = REPLACE)`; the follow-up `updateProfile(profile)` is redundant:
```kotlin
                     profileRepository.saveUserProfile(profile)
-                    profileRepository.updateProfile(profile)
```

- [ ] **Step 8: Build** (Task 1 Step 2 command). Expected: all four profile files have zero errors.

- [ ] **Step 9: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/profile/ && \
git commit -m "fix: repair profile ViewModels, state classes, and DI contract

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 13. **Tests:** Step 8 (+ Task 23 for Hilt). **Acceptance:** all four files compile; no duplicate class declarations. **Risk:** medium — the file truncation must remove exactly the duplicated VMs (verify by grep: `ProfileSettingsViewModel` and `ProfileAnalyticsViewModel` must each appear in exactly one file afterwards). **Effort:** ~30 min.

### Task 21: Fix the Measurement subsystem

**Files (modify):**
- `app/src/main/kotlin/com/gymcoach/app/presentation/measurement/screens/MeasurementViewModel.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/measurement/screens/MeasurementScreen.kt`

**Problem (verified):**
1. `MeasurementViewModel.kt` (4 errors) — calls `getMeasurementsForUserUseCase.execute("default_user")`, `addMeasurementUseCase.execute(record)`, `updateMeasurementUseCase.execute(record)`, but the use cases expose `operator fun invoke(...)` and return a `Flow` (not a domain `Result`); the VM also imports the custom `com.gymcoach.app.domain.Result` for the `when` but the use case returns `Flow`.
2. `MeasurementScreen.kt` (32 errors) — **duplicate imports** of `Alignment`, `Modifier`, `dp`, plus explicit `padding`/`fillMaxWidth` imports after `layout.*` wildcards → "Conflicting import" for nearly every symbol; imports nonexistent `components.MeasurementCard`; reads `viewModel.trends` and `trendMap` that don't exist; reads private `_measurements`.

**Interfaces:**
- Consumes: `MeasurementViewModel` exposes `measurements: StateFlow<List<MeasurementRecord>>`, `isLoading`, `error`, and `addMeasurement(record)` / `updateMeasurement(record)`.
- Produces: the VM becomes Flow-based (it already is, in shape); the screen stops referencing `trends`/`trendMap`/`_measurements`.

- [ ] **Step 1: `MeasurementViewModel.kt` — rewrite the body methods to the real use-case API.** Replace the `loadMeasurements`/`addMeasurement`/`updateMeasurement` bodies:
```kotlin
    private fun loadMeasurements() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getMeasurementsForUserUseCase("default_user").collect { records ->
                    _measurements.value = records
                }
            } catch (e: Exception) {
                _error.value = "Failed to load measurements"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addMeasurement(record: MeasurementRecord) {
        viewModelScope.launch {
            try {
                addMeasurementUseCase("default_user", record.measurementType, record.value, record.notes)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add measurement"
            }
            loadMeasurements()
        }
    }

    fun updateMeasurement(record: MeasurementRecord) {
        viewModelScope.launch {
            updateMeasurementUseCase(record)
            loadMeasurements()
        }
    }
```
Remove the now-unused `import com.gymcoach.app.domain.Result` if the compiler flags it as unused (it is unused after this change).

- [ ] **Step 2: `MeasurementScreen.kt` — remove the duplicate import block** (the block starting at the second `import androidx.compose.ui.Alignment` and ending at `import androidx.compose.foundation.layout.fillMaxWidth`). The file's first-imports (`layout.*`, `material3.*`, `runtime.*`, `ui.Alignment`, `ui.Modifier`, `ui.unit.dp`) already cover these symbols; keep only the first occurrence of each. Also **remove** the broken import:
```kotlin
-import com.gymcoach.app.presentation.components.MeasurementCard
```

- [ ] **Step 3: `MeasurementScreen.kt` — replace the broken list body.** The `else` branch currently reads `_measurements.value`, `trendMap`, and calls `MeasurementCard` inside a nested scope. Replace the whole `items(MeasurementType.values()) { type -> ... }` block with a simple per-type list that only uses public VM state:
```kotlin
                        items(MeasurementType.values()) { type ->
                            val latest = measurements
                                .filter { it.measurementType == type }
                                .maxByOrNull { it.date.toEpochMilli() }
                            latest?.let { record ->
                                item {
                                    MeasurementCard(
                                        record = record,
                                        trend = null,
                                        onDelete = {}
                                    )
                                }
                            }
                        }
```
(`MeasurementCard` is the local composable defined at the bottom of this same file (`MeasurementScreen.kt:248`) with signature `fun MeasurementCard(record: MeasurementRecord, trend: MeasurementTrend?, onDelete: () -> Unit)` — the call above matches exactly; the broken import was a mistake, the local one is in scope.)

- [ ] **Step 4: `MeasurementScreen.kt` — drop the `trends` read** (`val trends by viewModel.trends.collectAsState()`), since the VM does not expose trends:
```kotlin
-    val trends by viewModel.trends.collectAsState()
```

- [ ] **Step 5: Build** (Task 1 Step 2 command). Expected: both files compile.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/measurement/ && \
git commit -m "fix: repair measurement ViewModel API usage and screen imports

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Tasks 2, 12. **Tests:** Step 5 (+ Task 23 for Hilt). **Acceptance:** both files compile; no duplicate imports. **Risk:** medium — the VM/use-case mismatch requires matching signatures precisely; the compiler is the oracle. **Effort:** ~30 min.

### Task 22: Fix the Settings subsystem

**Files (modify):**
- `app/src/main/kotlin/com/gymcoach/app/presentation/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/com/gymcoach/app/presentation/settings/SettingsScreen.kt`

**Problem (verified):**
1. `SettingsViewModel.kt` (8 errors) — `Icons.Filled.Bell` and `Icons.Filled.Restart` do not exist (verified against the extended-icons jar); `History` icon import missing; `PackageInfo(versionName = ..., versionCode = ...)` uses the companion's local `PackageInfo` data class which is not in scope in the catch branch (and its later `packageInfo.versionName` cascade-fails); `savePreference(context, key, value: Any)` can't accept `String?`.
2. `SettingsScreen.kt` (17 errors) — bogus `import androidx.compose.foundation.layout.Builder`; same `Bell`/`Restart` icon references; missing `PlateCalculator` and `formatDuration` imports; `Box`/`wrapContentSize` unresolved; param-name mismatches in button-like calls.

**Interfaces:**
- Consumes: `com.gymcoach.app.domain.model.PlateCalculator` (exists, `domain/model/PlateCalculator.kt`), `com.gymcoach.app.presentation.history.formatDuration(Long)`.
- Produces: `SettingsViewModel` exposes `state`, `setNotificationSoundUri(uri: String?)`; icons use valid extended icons.

- [ ] **Step 1: `SettingsViewModel.kt` — fix icon imports**
```kotlin
-import androidx.compose.material.icons.filled.Bell
-import androidx.compose.material.icons.filled.Restart
+import androidx.compose.material.icons.filled.History
+import androidx.compose.material.icons.filled.Notifications
```

- [ ] **Step 2: `SettingsViewModel.kt` — fix the icon references in `SettingsScreenSection`**
```kotlin
-    data object Notifications : SettingsScreenSection(3, "Notifications", Icons.Default.Bell)
-    data object Workout : SettingsScreenSection(4, "Workout", Icons.Default.Restart)
+    data object Notifications : SettingsScreenSection(3, "Notifications", Icons.Default.Notifications)
+    data object Workout : SettingsScreenSection(4, "Workout", Icons.Default.RestartAlt)
```

- [ ] **Step 3: `SettingsViewModel.kt` — fix the version lookup.** Replace the `try/catch` that constructs `PackageInfo(...)` with a null-safe read of the platform `PackageInfo`:
```kotlin
-        val packageInfo = try {
-            context.packageManager.getPackageInfo(context.packageName, 0)
-        } catch (e: PackageManager.NameNotFoundException) {
-            PackageInfo(versionName = "0.1.0", versionCode = 1)
-        }
+        val packageInfo = runCatching {
+            context.packageManager.getPackageInfo(context.packageName, 0)
+        }.getOrNull()
```
and at the usage (`:131-132`):
```kotlin
-            versionName = packageInfo.versionName,
-            versionCode = packageInfo.versionCode
+            versionName = packageInfo?.versionName ?: "0.1.0",
+            versionCode = packageInfo?.versionCode ?: 1
```
Then remove the now-unused companion `data class PackageInfo(...)` and the unused `PackageManager.NameNotFoundException` catch import if flagged.

- [ ] **Step 4: `SettingsViewModel.kt` — make `savePreference` null-safe** (`:201`)
```kotlin
-    private fun savePreference(context: Context, key: String, value: Any) {
+    private fun savePreference(context: Context, key: String, value: Any?) {
         val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
         when (value) {
+            null -> prefs.remove(key)
             is Boolean -> prefs.putBoolean(key, value)
             is Int -> prefs.putInt(key, value)
             is String -> prefs.putString(key, value)
         }
         prefs.apply()
     }
```

- [ ] **Step 5: `SettingsScreen.kt` — remove the bogus import**
```kotlin
-import androidx.compose.foundation.layout.Builder
```

- [ ] **Step 6: `SettingsScreen.kt` — fix icons and add imports.** Apply the same `Bell`→`Notifications`, `Restart`→`RestartAlt` replacements; if `Icons.Filled.Builder` is referenced, change it to `Icons.Filled.QueryBuilder`. Add:
```kotlin
+import com.gymcoach.app.domain.model.PlateCalculator
+import com.gymcoach.app.presentation.history.formatDuration
+import androidx.compose.foundation.layout.Box
+import androidx.compose.foundation.layout.wrapContentSize
```
(For any remaining icon import that the compiler flags, replace it per the Global Constraints icon table; the compiler output names the exact symbol.)

- [ ] **Step 7: Build** (Task 1 Step 2 command). Expected: both settings files compile. Resolve any residual `SettingsScreen.kt` errors with the same rules (param-name mismatch on material3 calls → match the current API; the compiler lists the exact line).

- [ ] **Step 8: Commit**
```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/settings/ && \
git commit -m "fix: repair settings ViewModel and screen imports and icons

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 1. **Tests:** Step 7 (+ Task 23). **Acceptance:** both files compile; no nonexistent icons referenced. **Risk:** medium. **Effort:** ~30 min.

---

## Phase 5 — Full Verification (KSP, Hilt, Room)

### Task 23: Full build green

**Files:** none (verification only)

**Problem:** The fast-iteration commands skip KSP. The real gate is `assembleDebug` — Hilt codegen, Room schema processing, DEX, packaging. This is where the `@HiltViewModel`, `@Binds`, and DAO annotations are actually validated.

- [ ] **Step 1: Run the full build**
```bash
cd /root/Projects/GymCoach
./gradlew :app:assembleDebug --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2 2>&1 | tee /tmp/repair-t23.log
```
- [ ] **Step 2: Assert success**
```bash
grep -E "BUILD SUCCESSFUL|BUILD FAILED" /tmp/repair-t23.log
```
Expected: `BUILD SUCCESSFUL`. If Hilt reports a missing binding or a `@HiltViewModel` issue, fix it in the file named by the error (almost always one of Tasks 13/20/21 files) and re-run. Do **not** declare success until this prints `BUILD SUCCESSFUL`.

- [ ] **Step 3: Verify no regression in the committed core** — confirm the debug APK exists:
```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 4: Commit**
```bash
git add -A && git commit -m "chore: full debug build green (KSP/Hilt/Room verified)

Co-Authored-By: Claude <noreply@anthropic.com>"
```
(If only build outputs changed and they are gitignored, there may be nothing to commit — skip if `git status --porcelain` is empty.)

**Dependencies:** Tasks 7–22. **Tests:** Steps 1–3. **Acceptance:** `BUILD SUCCESSFUL` + APK produced. **Risk:** medium — Hilt may surface binding issues hidden from `compileDebugKotlin`; fix iteratively. **Effort:** ~20 min + iteration.

### Task 24: Repair and run the unit tests

**Files (modify):**
- `app/src/test/kotlin/com/gymcoach/app/domain/measurement/usecase/GetLatestMeasurementUseCaseTest.kt`
- `app/src/test/kotlin/com/gymcoach/app/domain/measurement/usecase/GetMeasurementsForUserUseCaseTest.kt`

**Problem (verified):** both tests use an invalid MockK pattern `mockk { answers { flowOf(...) } }` — `answers` is only available on the stub scope returned by `every`/`coEvery`, not on the `mockk { }` block receiver. `GetMeasurementsForUserUseCaseTest` also calls `assertTrue` without importing it.

**Interfaces:**
- Consumes: `GetLatestMeasurementUseCase` (class, `operator fun invoke(userId: String): Flow<MeasurementRecord?>` — kept in Task 12), `GetMeasurementsForUserUseCase` (class, `operator fun invoke(userId: String): Flow<List<MeasurementRecord>>` — kept).

- [ ] **Step 1: `GetLatestMeasurementUseCaseTest.kt` — replace the invalid stubbing** (both occurrences):
```kotlin
-        val repository = mockk<MeasurementRepository> {
-            answers { flowOf(measurement) }
-        }
+        val repository = mockk<MeasurementRepository>()
+        every { repository.getLatestMeasurementForUser(userId) } returns flowOf(measurement)
```
Add the missing imports at the top: `import io.mockk.every`, and ensure `mockk`/`flowOf`/`runTest` (or `runBlocking` + `first()`) are imported as used by the test body.

- [ ] **Step 2: `GetMeasurementsForUserUseCaseTest.kt` — same fix** for both stubbings:
```kotlin
-        val repository = mockk<MeasurementRepository> {
-            answers { flowOf(listOf(measurement)) }
-        }
+        val repository = mockk<MeasurementRepository>()
+        every { repository.getMeasurementsForUser(userId) } returns flowOf(listOf(measurement))
```
and add the missing `import org.junit.Assert.assertTrue`.

- [ ] **Step 3: Run the unit tests**
```bash
cd /root/Projects/GymCoach
./gradlew :app:testDebugUnitTest --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2 2>&1 | tee /tmp/repair-t24.log
```
- [ ] **Step 4: Assert all tests pass**
```bash
grep -E "BUILD SUCCESSFUL|BUILD FAILED|tests completed|FAILED" /tmp/repair-t24.log
```
Expected: `BUILD SUCCESSFUL` with all tests passing. The 5 existing test files must all pass (`ExerciseRepositoryTest`, `MeasurementRepositoryImplTest`, `AddMeasurementUseCaseTest`, plus the two fixed here).

- [ ] **Step 5: Commit**
```bash
git add app/src/test && \
git commit -m "test: repair broken measurement use-case tests and make suite green

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Tasks 12, 23. **Tests:** Steps 3–4. **Acceptance:** `BUILD SUCCESSFUL` with all 22 existing tests passing. **Risk:** low-medium. **Effort:** ~20 min.

---

## Phase 6 — Missing Critical Data-Flow Tests

### Task 25: Add tests for the progress analytics data flow

**Files (create):**
- `app/src/test/kotlin/com/gymcoach/app/data/repository/AnalyticsRepositoryImplTest.kt`

**Problem:** The Progress dashboard's data flow (`WorkoutDao` → `AnalyticsRepositoryImpl` → `ProgressViewModel`) is entirely untested. The Kotlin-side transforms (`getVolumeHistory`, `getWeeklySummary` Monday-grouping) are pure and testable against a mocked DAO — they are the highest-value missing coverage for the "Progress flow" (plan requirement #8).

**Interfaces:**
- Consumes: `AnalyticsRepositoryImpl(workoutDao)`, `WorkoutDao.getAllWorkoutVolumes(): List<DateVolume>` where `data class DateVolume(val date: Long, val volume: Double)` (`WorkoutDao.kt:313`).
- Produces: `AnalyticsRepositoryImplTest` — verifies epoch-millis → `Date` mapping and week-bucketing.

- [ ] **Step 1: Write the failing test** — create the file:
```kotlin
package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.DateVolume
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AnalyticsRepositoryImplTest {

    private val dao = mockk<WorkoutDao>()

    private fun millis(y: Int, m: Int, d: Int): Long {
        val c = Calendar.getInstance().apply {
            set(y, m - 1, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    @Test
    fun `getVolumeHistory maps DAO rows to date-volume pairs`() = runTest {
        val t1 = millis(2026, 8, 1)
        val t2 = millis(2026, 8, 3)
        coEvery { dao.getAllWorkoutVolumes() } returns listOf(
            DateVolume(date = t1, volume = 100.0),
            DateVolume(date = t2, volume = 250.5)
        )

        val repo = AnalyticsRepositoryImpl(dao)
        val history = repo.getVolumeHistory()

        assertEquals(2, history.size)
        assertEquals(100.0, history[0].second, 0.001)
        assertEquals(250.5, history[1].second, 0.001)
        assertEquals(t1, history[0].first.time)
    }

    @Test
    fun `getWeeklySummary groups all dates into their Monday week`() = runTest {
        // 2026-08-05 is a Wednesday, 2026-08-08 is a Saturday: same week (Monday 2026-08-03)
        val wed = millis(2026, 8, 5)
        val sat = millis(2026, 8, 8)
        // 2026-08-10 is the following Monday
        val nextMon = millis(2026, 8, 10)
        coEvery { dao.getAllWorkoutVolumes() } returns listOf(
            DateVolume(date = wed, volume = 100.0),
            DateVolume(date = sat, volume = 50.0),
            DateVolume(date = nextMon, volume = 200.0)
        )

        val repo = AnalyticsRepositoryImpl(dao)
        val weekly = repo.getWeeklySummary()

        assertEquals(2, weekly.size)
        assertEquals(150.0, weekly[0].second, 0.001) // Wed + Sat bucket
        assertEquals(200.0, weekly[1].second, 0.001) // next Monday bucket
    }
}
```

- [ ] **Step 2: Run the new test and confirm it fails initially** (if the implementation had a bug the test would catch — it should pass, proving the flow works):
```bash
./gradlew :app:testDebugUnitTest --tests "com.gymcoach.app.data.repository.AnalyticsRepositoryImplTest" --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2
```
Expected: `BUILD SUCCESSFUL` (the logic is currently correct for weekly grouping; the test locks it in).

- [ ] **Step 3: Run the full unit-test suite**
```bash
./gradlew :app:testDebugUnitTest --console=plain -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/qemu/bin/aapt2 2>&1 | tee /tmp/repair-t25.log
```
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 4: Commit**
```bash
git add app/src/test/kotlin/com/gymcoach/app/data/repository/AnalyticsRepositoryImplTest.kt && \
git commit -m "test: cover progress analytics volume and weekly grouping

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Dependencies:** Task 24. **Tests:** Steps 2–3. **Acceptance:** new test passes and locks in the Progress data flow. **Risk:** low — pure logic + `coEvery` mock. **Effort:** ~25 min.

---

## Final Acceptance Criteria (whole plan)

1. `./gradlew :app:assembleDebug ...` → **BUILD SUCCESSFUL** (Task 23).
2. `./gradlew :app:testDebugUnitTest ...` → **BUILD SUCCESSFUL**, all 25+ tests green (Tasks 24–25).
3. Error count trajectory (from `/tmp/repair-baseline.log`): 768 → 0.
4. Deleted clusters (VShape, Search, Goals, Templates, Backup, 4 orphan components) produce **zero** compile errors and zero references (Tasks 2–5).
5. The wired core — Exercise Library, Workout Session, History, Progress — compiles as a unit (Task 19) and is unchanged behaviorally (only imports/signatures fixed).
6. Profile, Measurements, Settings compile and their Hilt graphs resolve (Tasks 20–22, 23).
7. No new features added; MediaPipe/camera-ML/video remain unimplemented and unwired (constraint).
8. Every claim above is backed by a stored build log under `/tmp/repair-*.log` and a commit in the feature branch.

## Known risks & mitigations

| Risk | Mitigation |
|---|---|
| Hilt binding gaps surface only in `assembleDebug` (Task 23) | Run it once after Phase 4; fix per error message; the `@Binds` for `ProfileRepository` (Task 13) covers the known gap. |
| Task 20's file truncation could over-delete | Constraint: verify by grep that `ProfileSettingsViewModel` and `ProfileAnalyticsViewModel` each appear in exactly one file after the edit; the restore-point branch (Task 1) allows full revert. |
| A residual per-file import error not enumerated here | The plan's fix pattern is deterministic (add the `androidx.compose.*` import named by the compiler); each Task's build step surfaces it. Re-run the task's build command. |
| Deleting Room entities changes the DB version | `fallbackToDestructiveMigration()` already wipes on version mismatch; removing `goals` needs no version bump (schema list changes trigger the fallback automatically). |
| The Measurement rewrite touches behavior, not just compile | The VM/screen were non-functional (called nonexistent APIs); the rewrite restores them to the intended Flow-based contract. Verify via Task 23 + tests. |

## Scope guardrails (do not do)

- Do **not** implement MediaPipe pose detection, camera ML, or video playback wiring (plan requirement #10; the user's approved scope is "stabilize", not "build").
- Do **not** restore the deleted orphan clusters — they were unwired and unbuildable; reviving them is a separate future plan.
- Do **not** change UI strings, layouts, or behavior of the committed core beyond what is required to compile.
