# Engineering Specification

> **Status: V1.0 FINAL — All milestones completed. Released 2026-07-29.**

## Project Overview
GymCoach is a Android application for planning and tracking weight lifting workouts. It provides tools for exercise selection, workout logging, camera-based form analysis, and progress tracking. The app is designed for offline-only usage with no network connectivity, authentication, subscriptions, or analytics.

## Goals
- Provide a clean, modular architecture using Clean Architecture and MVVM pattern.
- Support single-user usage on Android devices.
- Implement offline data persistence with Room.
- Enable camera and MediaPipe pose detection for form analysis.
- Deliver performance-efficient UI with Jetpack Compose and Material3.

## Scope
- Core features: exercise library, workout logging, form analysis, progress charts.
- Out of scope: multi-user collaboration, cloud sync, advanced AI modeling, video tutorials.

## Technology Stack
- Kotlin 1.9.22
- Jetpack Compose 1.6.0 (Material3)
- Hilt 2.50
- Room 2.6.1
- Coroutines 1.7.3
- CameraX 1.3.1
- MediaPipe 0.10.9
- Media3 1.2.1
- Coil 2.5.0

## Architecture
- Clean Architecture with layers: Entity, Repository, Use Cases, Presentation.
- MVVM pattern: ViewModels expose StateFlows, Views are Compose functions.
- Package structure:
  - data (entities, daos, repository interfaces, database)
  - domain (usecases, models, repository contracts)
  - presentation (compose screens, viewmodels, UI components)
  - di (dependency injection modules)

## Dependencies
- Kotlin stdlib 1.9.22
- Jetpack Compose libraries
- Hilt for DI
- Room for persistence
- Coroutines for async
- CameraX for camera
- MediaPipe for pose detection
- Media3 for media playback
- Coil for image loading

## Build Configuration
- Gradle Kotlin DSL
- Version catalog (libs.versions.toml) defines all versions.
- Min SDK 21
- Target JDK 17

## Testing Strategy
- Unit tests for use cases and domain logic.
- Integration tests for repository implementations.
- UI tests for Compose screens using Turbine.
- No Android instrumentation tests for now.

## Performance Requirements
- UI frames at 60fps on mid-tier devices.
- Offline data queries under 100ms.
- Camera preview at 30fps.

## Security Considerations
- No internet permission.
- All data stored locally encrypted optionally.
- No authentication flows.

## Implementation Constraints
- Single user only.
- No subscription model.
- No analytics collection.
- No network calls.

## Milestone 1 Acceptance Criteria (COMPLETED)
- Gradle wrapper and properties configured.
- Version catalog with all versions.
- Core dependencies added.
- Project documentation structure created.
- Basic MainActivity with Compose UI.
- Clean Architecture package skeleton.
- Initial CI/CD config.

## Milestone 2 Acceptance Criteria (COMPLETED)
- Exercise entity, DAO, and Room database implemented.
- Repository interface and implementation with Hilt injection.
- Room migrations configured.
- Sample CRUD operations verified.

## Milestone 3 Acceptance Criteria (COMPLETED)
- Exercise list screen with Compose.
- Exercise detail screen.
- Search and filter functionality.
- Offline caching operational.

## Milestone 4 Acceptance Criteria (COMPLETED)
- Workout session model implemented.
- Set logging with rest timers.
- Workout history persisted in Room.

## Milestone 5 Acceptance Criteria (COMPLETED)
- CameraX preview functional.
- MediaPipe pose detection integrated.
- Form correction UI displayed in real-time.

## Milestone 6 Acceptance Criteria (COMPLETED)
- Video playback via Media3.
- Progress charts rendered with Compose.
- History timeline view functional.

## Milestone 7 Acceptance Criteria (COMPLETED)
- Performance optimized (60fps UI, <100ms queries).
- Accessibility improvements applied.
- Final testing and bug fixes completed.