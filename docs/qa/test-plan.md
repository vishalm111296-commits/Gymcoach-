# GymCoach QA Test Plan & Test Matrices

## 1. Test Matrices

### 1.1 Navigation & Routing Matrix
| Test Case ID | Feature under Test | Description | Preconditions | Inputs | Expected Output | Status |
|---|---|---|---|---|---|---|
| NAV-01 | Routing | Navigation from Exercise List to Progress | Fresh app launch | Tap Insights Icon | Navigates to `progress` route. Shows ProgressDashboard. | Pass |
| NAV-02 | Routing | Navigation from Exercise List to Camera | Fresh app launch | Tap Camera Icon | Navigates to `camera` route. Shows CameraPreviewScreen. | Pass |
| NAV-03 | Navigation | Back stack integrity from Detail | In Exercise Detail | Tap Back arrow | Pops stack. Returns to Exercise List. | Pass |
| NAV-04 | Session | Start workout transition | History screen | Tap "＋" button | Starts new workout session. Navigates to `workout_session`. | Pass |

### 1.2 Set-Type Logging Matrix
| Test Case ID | Feature under Test | Description | Inputs | Expected Volume Weight | Expected Set Count |
|---|---|---|---|---|---|
| SET-01 | Normal Set | 1x10 @ 100kg Normal | SetType = NORMAL | $10 \times 100.0 = 1000.0$ | $1$ |
| SET-02 | Warmup Set | 1x10 @ 60kg Warmup | SetType = WARMUP | $0.0$ (Warmup excluded from volume) | $1$ |
| SET-03 | Drop Set | 1x10 @ 80kg Drop | SetType = DROP | $10 \times 80.0 = 800.0$ | $1$ |
| SET-04 | Failure Set | 1x8 @ 100kg Failure | SetType = FAILURE | $8 \times 100.0 = 800.0$ | $1$ |

### 1.3 Persistent Rest Timer Matrix
| Test Case ID | Scenario | Actions | Expected Behavior |
|---|---|---|---|
| TIM-01 | Timer Trigger | Mark set complete | Timer starts. Notification shown if backgrounded. |
| TIM-02 | App Backgrounded | Toggle set, background app for 30s, resume | Timer remaining decreases by 30s. Visual ticker resumes. |
| TIM-03 | Process Death | Force stop app mid-timer, reopen | Remaining time calculated from target epoch. Timer resumes if active. |
| TIM-04 | Timer Cancel | Uncheck set | Rest timer cancels and stops. |

### 1.4 Plate Calculator Matrix
| Test Case ID | Target Weight | Bar Weight | Inventory (per side) | Expected Output (per side) |
|---|---|---|---|---|
| PLA-01 | 100 kg | 20 kg | default | 1x20kg, 1x20kg (40kg per side) |
| PLA-02 | 82.5 kg | 20 kg | default | 1x20kg, 1x10kg, 1x1.25kg (31.25kg per side) |
| PLA-03 | 19.5 kg | 20 kg | default | Null / Weight too low warning |
| PLA-04 | 105 kg | 20 kg | Limit: no 20kg plates | 2x15kg, 1x10kg, 1x2.5kg (42.5kg per side) |

### 1.5 V-Taper Tracking Matrix
| Test Case ID | Shoulder Measure | Waist Measure | Computed Ratio | Target Action |
|---|---|---|---|---|
| VT-01 | 120 cm | 80 cm | 1.50 | Ratio < 1.618. Suggest upper body focus (Lats/Shoulders). |
| VT-02 | 130 cm | 80 cm | 1.625 | Ratio >= 1.618. Suggest maintenance / balanced split. |
| VT-03 | 120 cm | 0 cm | Error | Zero value verification. Display validation error. |

---

## 2. Test Execution Guidelines
- Run unit tests for `PlateCalculator` and `FormAnalyzer` via `./gradlew test`.
- Verify database migrations by upgrading from a clean v1 schema to v5.
- Perform end-to-end device testing for rest timer persistence and MediaPipe camera feed tracking.
