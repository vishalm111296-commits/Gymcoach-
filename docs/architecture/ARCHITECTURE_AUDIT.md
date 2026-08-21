# ARCHITECTURE_AUDIT.md

## Repository State & MVVM Reconciliation

GymCoach claims a modular, domain-driven structure, but physically resides entirely within a single monolith module (`:app`). Submodules listed in the existing `PACKAGE_STRUCTURE.md` (e.g., `:core:domain`, `:core:data`, `:feature:workout`) are **hypothetical** and do not exist in `settings.gradle.kts` or the filesystem.

Within `:app`, package nesting simulates clean layers, but boundaries are weak.

### ViewModels
1. **Double Definition in Presentation & Screens**:
   - `WorkoutHistoryDetailViewModel` is declared inside `WorkoutHistoryDetailScreen.kt` rather than its own file, violating the package-by-feature layout.
   - Profile feature split: ViewModels like `ProfileViewModel` and `ProfileAnalyticsViewModel` have separated files, yet their UI layout and models have loose isolation.
2. **ViewModel Hilt Annotations**:
   - ViewModels correctly utilize `@HiltViewModel` and `@Inject constructor`.

### Database Schemas
- **Database File**: `GymCoachDatabase.kt`
- **Current Version**: 5 (Manual migrations `3_4` and `4_5` are defined; older versions fallback to destructive migration).
- **Entities**:
  - `ExerciseEntity`: Stores exercises, seed data, and favorite markers.
  - `WorkoutEntity`: Handles metadata including duration, notes, mood, energy, pain, and template flags.
  - `WorkoutExerciseEntity`: Junction table linking Workouts and Exercises with sorting index.
  - `WorkoutSetEntity`: Represents sets (weight, reps, rpe, type, and status).
  - `UserProfileEntity`: Singular profile table configured for Hilt mapping.
  - `MeasurementRecordEntity`: Stores metrics mapped to domain types.

### Repository Implementations & Core DI Modules
- **Repository Interface Layer**: Resides in `domain/repository/` (`WorkoutRepository`, `ExerciseRepository`, `MeasurementRepository`, `ProfileRepository`, `UserProfileRepository`, `AnalyticsRepository`).
- **Data Repository Implementations**: Resides in `data/repository/` (`WorkoutRepositoryImpl`, `ExerciseRepositoryImpl`, etc.).
- **Dependency Injection Contracts**:
  - `RepositoryModule`: Uses `@Binds` to bind interfaces to repository implementations.
  - `AppModule`: Provides database instance and individual Room DAOs.
- **Leaked Domain Boundaries**: Mappers from entity to domain (and vice versa) are defined directly inside repository implementation classes as private helper extension functions (`WorkoutRepositoryImpl.kt`), preventing domain classes from leaking entity data structures.

### Navigation Structure
- **Host**: `GymCoachNavHost.kt`
- **Routes**: Object definitions containing string-based path configurations. 
- **Type Safety**: Routes pass parameters (e.g., `exerciseId`, `workoutId`) by converting to standard types. Nav graph setup relies on Compose Navigation, and ViewModels are resolved using standard Compose/Hilt integrations (`hiltViewModel()`).

---

## Architectural Discrepancies
1. **Phantom Modularity**: Documentation (`PACKAGE_STRUCTURE.md`, `DOMAIN_ARCHITECTURE.md`) references a modular structure with feature modules (`:feature:*`) and core layers (`:core:*`). In reality, `:app` is a monolith.
2. **Unused UseCases**: While `Measurement` relies on UseCases (`GetMeasurementTrendUseCase`, `AddMeasurementUseCase`, etc.), other screens like `Exercise`, `Profile`, and `Workout` bypass UseCases and inject repositories directly into ViewModels.
3. **Internal ViewModel definitions**: `WorkoutHistoryDetailViewModel` is inside the UI Screen file.
