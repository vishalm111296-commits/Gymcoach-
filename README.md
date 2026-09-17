# GymCoach

A modern Android fitness application built with Kotlin, Jetpack Compose, and Clean Architecture.

## Features

- **Exercise Library & Custom Exercises:** Browse preloaded exercises and create, persist, and manage custom exercises with category/muscle filtering.
- **Workout Templates:** Create, duplicate, archive, and start workouts from reusable workout templates (Room v14).
- **Workout Tracking:** Log workouts with sets, reps, weight, RPE, and set types (Warmup, Normal, Drop, Failure).
- **Progression Analytics:** Estimated 1RM trends (Epley formula), PR history tracking, and weekly volume charts per exercise.
- **Rest Timer:** Durable automated rest countdown with background notification controls and process death survival.
- **Camera Form Analysis:** Real-time pose detection and form feedback using bundled MediaPipe models.
- **Progress Dashboard:** Volume history, personal records, V-taper physique scores, and workout statistics.
- **Offline First & Data Export:** Room database (v14) with CSV, Strong-format CSV, and JSON data export via Android Sharesheet.

## Tech Stack

- **Language:** Kotlin (Coroutines + Flow)
- **UI:** Jetpack Compose, Material Design 3
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt
- **Database:** Room (v14 with verified migrations 1..14)
- **Camera & ML:** CameraX, MediaPipe Tasks Vision (bundled offline model)
- **Video:** Media3 ExoPlayer
- **Testing:** 212 unit tests (207 passed, 0 failures, 5 skipped), connected instrumentation tests

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio (Ladybug or newer).
3. Sync Gradle.
4. Build and run on a physical device or emulator.

See [BUILD_GUIDE.md](docs/BUILD_GUIDE.md) for detailed build and release instructions.

## License

N/A
