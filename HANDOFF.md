# GymCoach — Project Handoff

## 1. Project Overview
GymCoach is an Android fitness application that enables users to log workouts, track progress, and receive real-time form analysis using MediaPipe pose detection. The app follows Clean Architecture with MVVM, Hilt for dependency injection, Room for local persistence, CameraX for camera preview, and Jetpack Compose for the UI.

## 2. Architecture Summary
```
Presentation (Compose + ViewModels)
    ↓ depends on
Domain (Models + Repository Interfaces)
    ↓ depends on
Data (Room DAOs, Entities, Repository Implementations)
    ↓ depends on
Infrastructure (CameraX, MediaPipe, Media3)
```

- **Presentation**: Compose screens and ViewModels. No business logic.
- **Domain**: Pure Kotlin data classes and repository interfaces. No Android dependencies.
- **Data**: Room database, repository implementations, MediaPipe integration.
- **Infrastructure**: CameraX, MediaPipe, Media3 ExoPlayer.

## 3. Technology Stack
| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose, Material Design 3 |
| DI | Hilt |
| Database | Room (SQLite) |
| Camera | CameraX |
| ML | MediaPipe Tasks Vision |
| Video | Media3 ExoPlayer |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle Kotlin DSL |
| Min SDK | 26 |
| Target SDK | 34 |
| Compile SDK | 34 |

## 4. Folder / Package Structure
```
com.gymcoach.app/
├── GymCoachApplication.kt
├── core/
│   ├── di/
│   │   ├── AppModule.kt          — Hilt: Database, DAO, Repository providers
│   │   └── RepositoryModule.kt    — Hilt: Repository interface bindings
│   ├── ml/
│   │   └── FormAnalyzer.kt        — Pose analysis engine (9 exercises)
│   └── timer/
│       └── RestTimerManager.kt    — Rest period countdown timer
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   └── GymCoachDatabase.kt — Room database (v2)
│   │   ├── dao/
│   │   │   ├── ExerciseDao.kt     — Exercise CRUD queries
│   │   │   └── WorkoutDao.kt      — Workout + Sets + Analytics queries
│   │   ├── entity/
│   │   │   ├── ExerciseEntity.kt
│   │   │   ├── WorkoutEntity.kt
│   │   │   ├── WorkoutExerciseEntity.kt
│   │   │   └── WorkoutSetEntity.kt
│   │   └── migration/
│   │       └── (removed — migration logic inlined)
│   └── repository/
│       ├── AnalyticsRepositoryImpl.kt
│       ├── ExerciseRepositoryImpl.kt
│       └── WorkoutRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── Exercise.kt
│   │   └── Workout.kt             — Workout, WorkoutExercise, WorkoutSet, etc.
│   └── repository/
│       ├── AnalyticsRepository.kt
│       ├── ExerciseRepository.kt
│       └── WorkoutRepository.kt
├── presentation/
│   ├── ExerciseViewModel.kt
│   ├── components/
│   │   ├── ExerciseItemCard.kt
│   │   └── ExerciseVideoPlayer.kt
│   ├── camera/
│   │   ├── CameraOverlay.kt
│   │   └── CameraPreviewScreen.kt
│   ├── detail/
│   │   └── ExerciseDetailScreen.kt
│   ├── list/
│   │   └── ExerciseListScreen.kt
│   ├── progress/
│   │   └── ProgressDashboardScreen.kt
│   └── workout/
│       ├── WorkoutLoggingViewModel.kt
│       └── WorkoutSessionScreen.kt
└── ui/
    ├── GymCoachNavHost.kt
    ├── MainActivity.kt
    └── theme/
        └── Theme.kt
```

## 5. Database Schema Overview

### Entities
| Entity | Table | Primary Key |
|--------|-------|-------------|
| `ExerciseEntity` | `exercises` | `id` (auto) |
| `WorkoutEntity` | `workouts` | `id` (auto) |
| `WorkoutExerciseEntity` | `workout_exercises` | `id` (auto) |
| `WorkoutSetEntity` | `workout_sets` | `id` (auto) |

### Relationships
- `workout_exercises.workoutId` → `workouts.id` (CASCADE delete)
- `workout_exercises.exerciseId` → `exercises.id` (CASCADE delete)
- `workout_sets.workoutExerciseId` → `workout_exercises.id` (CASCADE delete)

### Database Version
- Current: **v2**
- Migration from v1 to v2: Inline SQL `CREATE TABLE IF NOT EXISTS` statements in `GymCoachDatabase.create()`.

### Key Queries
- `getAllWorkoutVolumes()`: Daily volume (reps × weight) for completed workouts.
- `getAllPersonalRecords()`: Max weight per exercise across completed workouts.
- `getPersonalRecordMax(exerciseId)`: Max weight for a specific exercise.
- `getMonthlyVolumes()`: Monthly volume grouped by `strftime('%Y-%m', date)`.
- `getTopMuscleGroups()`: Top 5 exercises by total reps.

## 6. Dependency Injection Overview

### Hilt Modules

**`AppModule`** (singleton):
- Provides `GymCoachDatabase`
- Provides `ExerciseDao` and `WorkoutDao`
- Provides `ExerciseRepository` and `WorkoutRepository`

**`RepositoryModule`** (singleton):
- Binds `ExerciseRepositoryImpl` → `ExerciseRepository`
- Binds `WorkoutRepositoryImpl` → `WorkoutRepository`
- Binds `AnalyticsRepositoryImpl` → `AnalyticsRepository`

### Injection Points
- `FormAnalyzer` — constructor injection via `@Inject`
- `RestTimerManager` — `@Singleton` via `@Inject`
- All ViewModels — `@HiltViewModel`
- All repository implementations — `@Inject` constructor

## 7. Navigation Structure
```
ExerciseListScreen → ExerciseDetailScreen
WorkoutSessionScreen (starts new or resumes incomplete workout)
ProgressDashboardScreen
CameraPreviewScreen (launched from workout or exercise detail)
```

- NavHost defined in `GymCoachNavHost.kt`
- Routes: `exercise_list`, `exercise_detail/{exerciseId}`
- Back navigation via `NavHostController.popBackStack()`

## 8. CameraX + MediaPipe Pipeline
```
CameraPreviewScreen (Compose)
    ↓ AndroidView interop
PreviewView (CameraX)
    ↓ ProcessCameraProvider
CameraSelector (FRONT)
    ↓
Preview use case → SurfaceProvider → PreviewView
    ↓ (future)
ImageAnalysis use case → MediaPipe Pose Detection
    ↓
FormAnalyzer.analyze(pose) → AnalysisResult
    ↓
ViewModel → Compose UI feedback
```

### Current State
- Camera preview is fully functional (front camera).
- MediaPipe pose detection is integrated but `FormAnalyzer` does not yet receive live frames from CameraX `ImageAnalysis`. The pipeline is ready for connection.

## 9. FormAnalyzer Design

### Supported Exercises (9)
1. Bicep Curl
2. Squat
3. Push-up
4. Shoulder Press
5. Lateral Raise
6. Bent-over Row
7. Plank
8. Deadlift
9. Bench Press

- Single `FormAnalyzer` class parameterized by `ExerciseType` + `ExerciseConfig`.
- State includes rep counting, phase detection, smoothing, confidence filtering, invalid-movement detection, and time-based plank hold.
- Public API: `analyze(pose, currentTimeMs)`, `reset()`.

## 10. Analytics Pipeline
- `AnalyticsRepository` is the single source for analytics.
- `AnalyticsRepositoryImpl` performs calculations using `WorkoutDao`.
- `ProgressViewModel` maps repository output to `ProgressUiState`.
- `ProgressDashboardScreen` renders stats, charts, PRs, weekly/monthly summaries, and distribution lists.

## 11. Workout Lifecycle
- Start new workout → create `WorkoutEntity` → collect `WorkoutWithDetails` → UI binds.
- Add exercise → insert `WorkoutExerciseEntity`.
- Add set → insert `WorkoutSetEntity`.
- Update reps/weight/RPE → update `WorkoutSetEntity`.
- Complete set toggle → optional rest timer start.
- Complete workout → update `WorkoutEntity` with `endTime`, `duration`, `completed=true`.

## 12. Known Limitations
- MediaPipe pose detection integration exists, but live `ImageAnalysis`→`FormAnalyzer` frame wiring is not implemented in `CameraPreviewScreen`.
- `FormAnalyzer` thresholds are static; no calibration UI exists.
- Workout history screen/search/sort/filter is not implemented as a dedicated screen.
- No UI tests executed (no emulator/device available on the current host; aarch64, no KVM).
- Signing config is not yet set (release APK builds unsigned).
- Accessibility and performance reviews are pending device verification.
- Build-time R8/AGP note: do NOT use `tasks.whenTaskAdded` in `app/build.gradle.kts`; it breaks AGP task wiring for the R8 java-resource transform (issue 277166577), causing `merged_java_res/<variant>/base.jar` to be deleted before R8 reads it. Use `tasks.configureEach` instead (as done for `stripDebugDebugSymbols`).

## 13. Technical Debt
- `MuscleGroupStats` class exists in both domain and data layers; kept as separate DTO/model boundary.
- No automated UI tests.
- No calibration/settings for analyzer thresholds or camera selection.
- No workout history screen with filtering/sorting.

## 14. Build Prerequisites
- Android Studio recommended.
- JDK 17+ available to Gradle/AGP.
- Android SDK with API 34 and build-tools.
- Gradle wrapper is present: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`.

## 15. Testing Checklist
- Verify unit tests run via Gradle.
- Add ViewModel and repository tests.
- Add Compose UI tests for main screens.
- Perform manual device tests for core flows.

## 16. Release Checklist
- Verify Gradle sync.
- Verify debug build. ✔ (assembleDebug green; APK in `app/build/outputs/apk/debug/`)
- Configure signing + `proguard-rules.pro`. (rules present; signing still unset)
- Verify release build. ✔ (assembleRelease green, unsigned APK in `app/build/outputs/apk/release/`)
- Generate signed APK/AAB.
- Upload to Play Console/internal track.

## Build Verification Status (host: aarch64, QEMU bridge for x86-64 SDK tools)
- `assembleDebug`: PASS
- `testDebugUnitTest`: PASS — 27 passed, 12 skipped (Robolectric on aarch64), 0 failures
- `lintDebug`: PASS — 0 errors, 30 warnings
- `assembleRelease`: PASS — after fix of `whenTaskAdded`→`configureEach` (R8 `base.jar` deletion) + `-dontwarn javax.lang.model.*` (AutoValue shaded JavaPoet)
- Device/emulator journey: NOT EXECUTED (no emulator package, no KVM, adb x86-64 binary crashes on this host)

## 17. Future Version 2 Ideas
- Dedicated workout history screen with search, sort, filter, detail, resume.
- Live camera frame analysis wiring from CameraX `ImageAnalysis` into `FormAnalyzer`.
- Calibration flow for `FormAnalyzer` thresholds/camera side.
- User accounts/cloud sync.
- Notifications and reminders.
- Expanded analytics and export/share.
- Tablet/foldable layout optimization.
- Theming and accessibility polish pass.

## First Day Setup
1. Install Android Studio.
2. Install JDK 17+.
3. Install Android SDK API 34 and build-tools.
4. Clone the repository.
5. Open the project in Android Studio.
6. Click Sync Project with Gradle Files.
7. Run on device via Run app.
8. Run tests via Run tests in app.
9. For release builds, configure signing and use Generate Signed Bundle/APK.

## Project Health Assessment
- **Strengths**: Clean Arch + MVVM, consistent DI, Room schema stable, multi-exercise analyzer scaffold, complete progress dashboard, offline-first design.
- **Risks**: No UI/runtime device verification yet; release signing not configured; MediaPipe live-frame wiring not connected.
- **Recommended next steps**: Wire live camera frames to `FormAnalyzer`, implement workout history screen, add UI tests, configure release signing, and verify on a physical device.