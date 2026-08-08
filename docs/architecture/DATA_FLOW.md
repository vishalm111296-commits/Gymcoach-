# Data Flow

GymCoach follows a unidirectional data flow (UDF) pattern.

## Flow
1. **UI Layer (Compose)**: Displays state, captures user interactions (Events).
2. **Presentation Layer (ViewModel)**: Processes interactions, updates UI State, invokes Use Cases.
3. **Domain Layer (Use Cases)**: Executes business logic, delegates to Repositories.
4. **Data Layer (Repository)**: Abstracts data source (Room/Network).
5. **Persistence Layer (Room)**: Stores/retrieves data, exposes as `Flow` for reactive updates.

## Reactivity
All state flows from the Database through the repository as `Flow` or `StateFlow`, keeping the UI in sync automatically.
- No direct database access from UI.
- No state mutation outside of ViewModels.
- Repositories return immutable models.
