# Domain Architecture

GymCoach uses a modular, domain-driven design (DDD) approach.
- Each major feature is a Gradle module (`feature:*`).
- `:core:domain` defines shared entities and repository interfaces.
- Communication between modules is handled by dependency injection (Hilt) and domain-layer interfaces.
- Clean Architecture principles ensure the UI, Data, and Domain layers are decoupled.
