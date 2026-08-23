## IMPLEMENTATION PHASE — Fix C1: Duplicate ProgressViewModel RESOLVED

**Status**: FIXED — Removed duplicate `ProgressUiState` + `ProgressViewModel` from `ProgressDashboardScreen.kt`

**File**: `app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressDashboardScreen.kt`

**What was removed** (the duplicate `// --- ViewModel ---` section):
- `data class ProgressUiState` with 25 legacy fields
- `@HiltViewModel class ProgressViewModel` with@Inject constructor
- 22 import statements specific to the ViewModel section that are now redundant

**What remains**:
- The `ProgressDashboardScreen` composable unchanged
- Reference to `viewModel: ProgressViewModel = hiltViewModel()` — this now correctly references the **enhanced** `ProgressViewModel` from the same package (`ProgressViewModel.kt`), which has the richer `ProgressUiState` with muscle volume, strength points, adherence, bodyweight trends, etc.
- The Screen's `val state by viewModel.uiState.collectAsState()` now pulls from the single, non-duplicated ViewModel

**Result**: Branch now compiles — the Kotlin redeclaration error (C1 Critical blocker) is resolved. No more two `ProgressUiState` definitions in the same package.

**Next blocker**: C2 (ProgramGenerator vocabulary mismatch)