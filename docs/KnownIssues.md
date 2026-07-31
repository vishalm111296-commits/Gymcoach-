# Known Issues — V1.0

## Active
- MediaPipe may require NDK configuration on some devices (ARM64 preferred)
- CameraX mock not available for unit tests; form analysis tested on device only
- Responsive layout not fully verified on tablets and foldables
- MediaPipe pose detection accuracy degrades in low-light conditions
- Rest timer notification does not persist if app is killed

## Resolved
- **Room migration conflict (FIXED 2026-07-30).** Migration1to2 was empty; added inline SQL CREATE TABLE statements. Removed duplicate database file.**
- **Room CRUD completeness (FIXED 2026-07-30).** Added missing update() method to ExerciseDao and ExerciseRepository.**
- Gradle-wrapper bootstrapping on fresh clones — fixed with committed wrapper jar
- Database versioning strategy — finalized with Room auto-migrations
- Initial UI layout refinement — addressed in M7 polish pass
- **Gradle wrapper missing (FIXED 2026-07-30).** Restored `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`.

## Workarounds
- For NDK issues: ensure NDK 25.2+ installed via SDK Manager
- For low-light pose detection: use well-lit environment or flash-assisted mode
- For tablet layout: app functional but not optimized; landscape mode works
