# Module Dependencies

GymCoach dependencies follow a Directed Acyclic Graph (DAG) to prevent circular dependencies.
- Features may depend on `:core:domain` and `:core:data`.
- Features do NOT depend on each other directly; they interact through interfaces defined in `:core:domain`.
- `:core:domain` is the most stable module and has zero feature-layer dependencies.
