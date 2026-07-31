# GymCoach

A modern Android fitness application built with Kotlin, Jetpack Compose, and Clean Architecture.

## Features

- **Exercise Library:** Browse and search preloaded exercises.
- **Workout Tracking:** Log workouts with sets, reps, weight, and RPE.
- **Rest Timer:** Automated rest periods between sets.
- **Camera Form Analysis:** Real-time pose detection and form feedback using MediaPipe.
- **Progress Dashboard:** Volume history, personal records, and workout statistics.
- **Offline First:** Room database for persistent local storage.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material Design 3
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt
- **Database:** Room
- **Camera:** CameraX
- **ML:** MediaPipe Tasks Vision
- **Video:** Media3 ExoPlayer
- **Async:** Kotlin Coroutines + Flow

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio (Ladybug or newer).
3. Sync Gradle.
4. Build and run on a physical device or emulator.

See [BUILD_GUIDE.md](docs/BUILD_GUIDE.md) for detailed build and release instructions.

## License

N/A
