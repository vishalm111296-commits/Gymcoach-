# Database Schema

GymCoach uses Room for local persistence.
- Schema evolves through manual `Migration` classes.
- Entities are defined in the `:core:domain` or relevant feature module.
- `GymCoachDatabase` resides in `:core:data` and aggregates all feature-specific DAOs.
- All relationships are modeled to avoid circularity (e.g., Use IDs for cross-feature references).
