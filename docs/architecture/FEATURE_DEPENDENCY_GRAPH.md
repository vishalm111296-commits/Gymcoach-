# Feature Dependency Graph

The GymCoach architecture uses a dependency graph where features consume domain-level interfaces.

## Graph
- **Workout Engine**: Depends on `:core:domain`, `:feature:exercise`.
- **Measurement Engine**: Depends on `:core:domain`.
- **Goal Engine**: Depends on `:core:domain`, `:feature:measurement`.
- **Challenge Engine**: Depends on `:core:domain`, `:feature:workout`, `:feature:goal`.
- **Analytics Engine**: Depends on `:core:domain`, `:feature:workout`, `:feature:measurement`.
- **Recommendation Engine**: Depends on `:core:domain`, `:feature:analytics`, `:feature:workout`, `:feature:goal`, `:feature:recovery`.
- **Recovery Engine**: Depends on `:core:domain`, `:feature:workout`.
- **Progress Engine**: Depends on `:core:domain`, `:feature:analytics`.

## Rules
- Direct dependency between two feature modules is forbidden.
- Interactions between features happen via events or domain-level services exposed by `:core:domain`.
- All features must implement their own internal navigation graph, exposed via a common `FeatureNavigation` interface.
