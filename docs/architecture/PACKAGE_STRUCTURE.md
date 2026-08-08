# Package Structure

GymCoach uses a modularized Android project structure.

## Structure
- `:app`: Main module. Entry point. Contains `MainActivity` and `GymCoachNavHost`.
- `:core:domain`: Entities, repository interfaces, use cases, models.
- `:core:data`: Room database implementation, repository implementations.
- `:feature:workout`: Workout session, timer, logging UI.
- `:feature:exercise`: Exercise library, search, filtering.
- `:feature:measurement`: Measurement tracking, anthropometrics.
- `:feature:goal`: Goal management, target tracking.
- `:feature:challenge`: Gamification, streak tracking.
- `:feature:analytics`: Data aggregation, charts, trends.
- `:feature:recommendation`: AI/Rules-based workout planning.
- `:feature:recovery`: Recovery logs, soreness management.
- `:feature:progress`: Statistics, personal records, milestones.

## Convention
Each module contains:
- `data`: DAOs, Repositories, Entities (if internal).
- `domain`: UseCases, Models (if internal).
- `presentation`: ViewModels, Composable Screens.
- `di`: Hilt modules.
