# ARCHITECTURE.md

## Overview

GymCoach follows **Clean Architecture** principles with **MVVM** presentation layer.

## Architecture Diagram

```text
Presentation Layer (UI)
    |
    v
ViewModel
    |
    v
Domain Layer (Use Cases / Repositories)
    |
    v
Data Layer (Repository Implementations)
    |
    v
Infrastructure (Room, MediaPipe, CameraX, Media3)
```

## Layer Breakdown

- **Presentation (Compose + ViewModels):**
  - Screens, Composables, ViewModels.
  - Depends only on Domain models and repository interfaces.

- **Domain (Models + Repository Interfaces):**
  - Pure Kotlin data classes.
  - Repository interfaces.
  - No Android framework dependencies.

- **Data (Repository Implementations + DAOs + Entities):**
  - Room DAOs, Entities, and Repository implementations.
  - MediaPipe/CameraX integration.
  - Depends on Android framework.

## Key Technologies
- **Language:** Kotlin
- **UI:** Jetpack Compose with Material Design 3
- **DI:** Hilt
- **Database:** Room
- **Camera:** CameraX
- **ML:** MediaPipe Tasks Vision
- **Video:** Media3 ExoPlayer
- **Async:** Kotlin Coroutines and Flow

## Package Structure

- `core/di/` — Hilt modules
- `core/ml/` — Form analysis logic
- `core/timer/` — Rest timer logic
- `data/local/` — Room database, entities, DAOs
- `data/repository/` — Repository implementations
- `domain/model/` — Domain models
- `domain/repository/` — Repository interfaces
- `presentation/` — ViewModels and Compose screens
- `ui/` — MainActivity and Navigation
