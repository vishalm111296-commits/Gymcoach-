# Architecture Decision Record (ADR-001)

## Status
Approved

## Context
GymCoach needs a long-term scalable architecture for consistent feature development and modularity.

## Decision
- Adopt modularized feature-based architecture.
- Enforce DDD layers (Domain, Data, Presentation).
- Use Hilt for dependency injection.
- Use Room with manual migrations.
- Use unidirectional data flow (UDF) with Compose.

## Consequences
- Requires upfront cost to maintain module boundaries.
- Significantly simplifies feature development and testing in the long run.
