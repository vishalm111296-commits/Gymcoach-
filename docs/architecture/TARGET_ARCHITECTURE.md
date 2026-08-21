# TARGET_ARCHITECTURE.md

## Modularity Strategy: Single Module vs Multi-Module

Given the current codebase size and scope, a **Single-Module Package-by-Feature Clean Architecture** is recommended. The theoretical multi-module structure described in previous ADRs added excessive configuration overhead without current benefit. If modularity becomes necessary, transition following this layout.

---

## Target Package Structure

```text
com.gymcoach.app/
│
├── core/
│   ├── di/               # Global Hilt dependency injection configuration
│   ├── ml/               # MediaPipe / CameraX Form Analysis framework
│   ├── timer/            # Central Rest Timer state machine
│   └── util/             # Generic helper utilities
│
├── data/                 # Data Layer (Shared framework implementations)
│   ├── local/
│   │   ├── dao/          # Room DAOs (ExerciseDao, WorkoutDao, etc.)
│   │   ├── database/     # GymCoachDatabase and migration definitions
│   │   └── entity/       # Room database schema entities
│   └── repository/       # Concrete Repository Implementations & Mappers
│
├── domain/               # Domain Layer (Strictly Kotlin - no Android dependencies)
│   ├── model/            # Pure domain models (Workout, Exercise)
│   ├── repository/       # Repository interface contracts
│   └── usecase/          # Optional: Command/Query orchestrators
│
└── presentation/         # Presentation Layer (Jetpack Compose UI)
    ├── components/       # Global UI elements
    └── features/         # Features package-by-feature
        ├── workout/      # WorkoutSession, WorkoutSummary (ViewModel, Screen, State)
        ├── history/      # WorkoutHistory, WorkoutDetail (ViewModel, Screen, State)
        ├── exercise/     # ExerciseList, ExerciseDetail (ViewModel, Screen, State)
        ├── profile/      # UserProfile, Analytics (ViewModel, Screen, State)
        └── measurement/  # Anthropometrics (ViewModel, Screen, State)
```

---

## Architectural Rules & Enforcements

1. **Layer Access Control**:
   - **UI** must only depend on **ViewModels**.
   - **ViewModels** must interact with repositories via **UseCases** (recommended) or directly through **Repository interfaces** (under strict DDD validation). They must not have direct visibility of Room DAOs or Entities.
   - **Domain** must contain no Android references. Data layers must map entities to domain models before emitting data downstream.
2. **Standardization of ViewModel Declarations**:
   - Every ViewModel must reside in its own file (`[Feature]ViewModel.kt`) inside the appropriate feature package. Inline screen definitions are prohibited.
3. **Data Flow**:
   - Strict Unidirectional Data Flow (UDF) must be enforced. ViewModels emit UI State (`StateFlow`) and consume actions. Databases emit updates reactively (`Flow`).
