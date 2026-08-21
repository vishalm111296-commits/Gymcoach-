# Initial QA Audit

This document establishes the test configurations, edge cases, and QA verification strategies to maintain a 0.0% critical bug rate in production releases.

---

## 1. Unit Testing Strategy & Code Coverage

All core calculations (overload logic, volume attribution, 1RM equations, PR tracking) must be fully unit-tested with $100\%$ code coverage.

### Core Testing Priorities
* **Calculations**: `calculate1RM` must output exact expected values for edge cases (zero weight, null RIR, high rep inputs).
* **PR Detection**: Verification that a set is detected as a PR *only* if it is marked completed, and its estimated 1RM or absolute weight exceeds all historical records for that specific rep target.
* **Volume Attribution**: Verify fractional volume ($0.5$ credit) is correctly calculated for secondary muscle groups and that warmup sets are ignored.

---

## 2. Boundary and Edge Case Matrix

The following edge-case matrix lists boundary conditions, system actions, and recovery actions:

| Category | Input Scenario | Expected System Action | Safety / Recovery |
| :--- | :--- | :--- | :--- |
| **Timer** | App killed or goes to background while rest timer is active. | Timer state persists in memory; Android Notification keeps ticking via Foreground Service. | On app restart, UI matches current notification timestamp. |
| **Overload** | RIR input is set to $10$ (extremely light) or $0$ (failure). | Algorithm adjusts weight recommendation by maximum boundary ($\pm 5\%$). | Prevents recommending unsafe weight increments. |
| **Input validation** | Weight input contains non-numeric strings or symbols (e.g. `80.0..5`). | Input field filters out invalid symbols; UI displays inline error. | Saves button is disabled until format is valid. |
| **1RM Calc** | Rep count input is $0$ or negative. | Formula skips calculation, returns `0.0`. | Prevents division-by-zero crashes. |
| **Database** | Migration from schema version 4 to 5 fails. | Database falls back to destructive migration (if configured) or raises custom upgrade exception. | Pre-flight check verifies migration integrity before production release. |
| **Workout Swap** | No alternative exercises are available for a given muscle group. | Swap engine returns empty list; UI shows placeholder. | Prevents crash; displays descriptive dialog. |

---

## 3. High-Stress Active Lifting QA Verification

Testing in a simulated "active workout" environment ensures UI stability and ease-of-use under physical fatigue.

### Test Cases
1. **Sweaty-Hands Simulation (Rapid Input / Double Taps)**:
   * Rapidly double-tapping set completion checkmarks must not log duplicated entries in the database.
   * Action: Implement UI throttling (debouncing) on all critical button click listeners.
2. **Keyboard Management (One-Handed Entry)**:
   * Ensure pressing the "Next" IME action button in the weight text field shifts focus to reps, and then to RPE, without closing the soft keyboard.
3. **Interrupt Stability**:
   * Simulating phone calls, low battery dialogs, or switching to navigation apps while logging a set. The active session draft must auto-save to Room after every single input field modification.
