| ID | Task | Milestone | Status | Dependencies | Notes |
|----|------|-----------|--------|--------------|-------|
| M1-01 | Create Gradle wrapper and properties | 1 | COMPLETED | - | Generated wrapper files |
| M1-02 | Configure version catalog with all versions | 1 | COMPLETED | - | Versions defined in libs.versions.toml |
| M2-01 | Implement Exercise entity and DAO | 2 | COMPLETED | M2-02 | ExerciseEntity.kt and ExerciseDao.kt |
| M2-02 | Implement GymCoachDatabase with Room | 2 | COMPLETED | M2-01 | Database version 2 with migration logic |
| M2-03 | Implement ExerciseRepository interface and impl | 2 | COMPLETED | M2-01, M2-02 | Injected via Hilt |
| M2-04 | Add Room migrations script | 2 | COMPLETED | M2-02 | Version 1 to 2 migration fixed |
| M2-05 | Implement sample CRUD operations for exercises | 2 | COMPLETED | M2-01, M2-02 | Insert, query, delete verified |
| M3-01 | Exercise list screen (Compose) | 3 | COMPLETED | M2-03 | LazyColumn with search |
| M3-02 | Exercise detail screen | 3 | COMPLETED | M3-01 | Detail view with images |
| M3-03 | Exercise search and filter | 3 | COMPLETED | M3-01 | Filter by muscle group, equipment |
| M3-04 | Offline caching | 3 | COMPLETED | M2-03 | Room-backed cache |
| M4-01 | Workout session model | 4 | COMPLETED | M3-01 | WorkoutSession entity |
| M4-02 | Set logging with rest timers | 4 | COMPLETED | M4-01 | CountDownTimer-based rest |
| M4-03 | Workout history persistence | 4 | COMPLETED | M4-01 | Room DAO for history |
| M5-01 | CameraX preview implementation | 5 | COMPLETED | M4-01 | Preview + capture |
| M5-02 | MediaPipe pose detection workflow | 5 | COMPLETED | M5-01 | Real-time landmark detection |
| M5-03 | Form correction UI integration | 5 | COMPLETED | M5-02 | Overlay feedback on preview |
| M6-01 | Video playback with Media3 | 6 | COMPLETED | M5-01 | ExoPlayer integration |
| M6-02 | Progress charts (Compose) | 6 | COMPLETED | M4-03 | Weight/reps over time |
| M6-03 | History timeline view | 6 | COMPLETED | M4-03 | Chronological workout list |
| P1-05 | Workout History screen with search, filter, sort, delete, detail, resume | 7 | COMPLETED | M6-03 | Full history CRUD + search + filters |
| M7-01 | Performance optimization | 7 | COMPLETED | M6-03 | 60fps verified |
| M7-02 | Accessibility improvements | 7 | COMPLETED | M7-01 | Content descriptions, TalkBack |
| M7-03 | Final testing and bug fixes | 7 | COMPLETED | M7-02 | All tests passing |
