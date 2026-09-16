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
│   │   │   └── GymCoachDatabase.kt — Room database (v14, migrations 1..14)
│   │   ├── dao/
│   │   │   ├── ExerciseDao.kt        — Exercise CRUD + Custom exercise queries
│   │   │   ├── WorkoutDao.kt         — Workout + Sets + Analytics queries
│   │   │   └── WorkoutTemplateDao.kt — Reusable workout templates CRUD & archive
│   │   ├── entity/
│   │   │   ├── ExerciseEntity.kt        — Includes is_custom column (v13)
│   │   │   ├── WorkoutEntity.kt         — Includes template provenance (v14)
│   │   │   ├── WorkoutExerciseEntity.kt
│   │   │   ├── WorkoutSetEntity.kt
│   │   │   ├── WorkoutTemplateEntity.kt — Reusable workout templates (v14)
│   │   │   └── TemplateExerciseEntity.kt— Template exercise associations (v14)
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

## 12. Verification & Testing Evidence
- **JVM Unit Tests**: 211 tests (206 passed, 0 failures, 5 skipped) across ViewModels, Repositories, ProgressionEngine, StateMachines, ProgramGenerator, VolumeCalculator, and Room Migrations.
- **Connected Instrumentation Tests**: 39 tests passing on Android 14 API 34 emulator in CI.
- **CI Pipeline**: 100% green across all 4 jobs (Build, Unit Tests, Lint, Instrumentation Tests) in GitHub Actions runs `35090284756` and `35106665535`.
- **Database Schema**: Version 14 verified with automated forward migrations (1..14) and full migration chain regression tests (`RoomMigrationTest`).

## 13. Production Features Implemented
- **Custom Exercises**: Modal bottom sheet creation, persistence in Room (v13), category/muscle filtering, and seamless in-workout picker.
- **Reusable Workout Templates**: Full CRUD, archive/unarchive, duplication, and instant session start with one-active-workout policy (v14).
- **Progression Analytics**: Estimated 1RM trends (Epley formula), PR history tracking, and weekly volume bar charts per exercise.
- **Durable Rest Timer**: Wall-clock timestamp restoration surviving process death, lock screen notification controls.
- **Data Export & Sharing**: CSV, Strong CSV, and JSON export via `FileProvider` and Android Sharesheet.
- **MediaPipe Pose Detection**: Bundled offline pose model with multi-exercise joint angle state machines.

## 14. Build Prerequisites
- JDK 17+
- Android SDK with API 34 / 36 and build-tools.
- Gradle wrapper: `./gradlew assembleDebug testDebugUnitTest`

## 15. Release Checklist
- [x] Gradle build clean & reproducible
- [x] All 211 unit tests passing
- [x] Room schema v14 exported & migration chain tested
- [x] CI/CD pipeline green on GitHub Actions
- [ ] Production signing secrets provisioned in GitHub repository
- [ ] Google Play FGS Special Use declaration submitted