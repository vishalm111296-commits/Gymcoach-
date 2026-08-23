## VOLUMECALCULATOR RECONCILIATION

**Canonical architecture: PR #10 / phase 3 `LoggedSet` pipeline.**

**Reasons (evidence-backed):**
1. Complete producer chain end-to-end (`getLoggedSetsRaw` → `getLoggedSets()`)
2. Zero data-layer coupling in `core.program`
3. Correct set counting (per-set increment, not distinct exercise counting)
4. Minimal DTO matching actual computation ({exerciseId, dateMs, completed, setType})

**Reconciliation obligations (both sides must agree before merge):**

| Item | Current (#10) | Current (#11) | Required Convergence |
|------|--------------|--------------|---------------------|
| Input type | `LoggedSet` (top-level DTO) | `SetWithContext(set, exerciseId, workoutDate)` — nested, entity-coupled | **One input type** — adopt `LoggedSet` |
| Warmup/drop/failure | `setType != "WARMUP"` (DROP+FAILURE counted) | `setType == 0` (only NORMAL counted) | **One policy** — document rationale; likely `!= "WARMUP"` per entity comments |
| Role-credit policy | Primary-only, `credits = 1.0`, `indirectSets = 0`, TODO for secondary | Weighted 1.0/0.5/0.25 per ACSM | **One policy** — restore weighted credits if keeping #11 approach, or keep primary-only with TODO cleared |
| Completed-workout rule | DAO omits `w.completed = 1` (abandoned sets enter pipeline) | N/A (calculator receives pre-filtered list) | **One rule** — add `WHERE w.completed = 1` to `getLoggedSetsRaw` |
| Time window per metric | `weeklySets = avgWeekly.toInt()` (per-week avg); `directSets` cumulative all-time | `directSets`/`indirectSets` from distinct-exercise `.groupBy` | **One metric** — either make `weeklySets` truly weekly (use `avgWeekly`), or make `directSets` weekly too; currently mixed |
| Test parity | Zero tests | 10 tests (but broken assertions, contradict impl) | **One test suite** — fix VolumeCalculatorTest assertions; add phase 3 counterpart |

**Remove duplicated/conflicting representations:** After convergence, remove `SetWithContext` from `VolumeCalculator.kt` (or keep only as internal wrapper if needed). Remove `directSetsByMuscle`/`indirectSetsByMuscle` logic if switching to per-set counting. Ensure only one DTO type (`LoggedSet`) is the home for set data flowing into the calculator.

**Add regression tests:** Minimum: empty input, uncompleted exclusion, warmup exclusion, primary muscle distribution, classification thresholds (INSUFFICIENT < 10, MODERATE 10-20, HIGH > 20), first-ever PR, uncompleted set filtering.