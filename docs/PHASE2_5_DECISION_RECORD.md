## ADVERSARIAL REVIEW

**3 CRITICAL findings block "production ready" claim:**

| Finding | Impact | Evidence |
|---------|--------|----------|
| C1: Duplicate ProgressViewModel + ProgressUiState | Branch cannot compile — hard stop | Two different `ProgressUiState` + `ProgressViewModel` in same package; Kotlin redeclaration error |
| C2: ProgramGenerator filters out all exercises | Core promise (plan → train) dead at generation time | Seeder stores lowercase/comma-joined equipment tokens; generator checks Title Case singletons; ALL 68 exercises excluded |
| C3: Room schema crashes on upgrade for existing users | Data intact but unreachable; infinite launch crash | 6 tables have entity/ DDL mismatches; `fallbackToDestructiveMigration` removed (audit P0) → users on v4→5 get `IllegalStateException` on every app launch |

**High findings:**
- H1: sorted-index vs raw-index mismatch → data corruption in workout set editing
- H2: VolumeCalculator "weekly" volume not weekly → incorrect classification
- H3: PRDetector dead code → 18 tests celebrate unreachable path
- H4: Seeder re-seed creates duplicates → accumulated rows on second seed
- H5: Frequency=2 produces 4-day program → adherence math desync
- H6: Room DB unencrypted → health data exposed
- H7: No signing config → cannot ship to Play Store

**Medium/Low findings** address UI/UX gaps, edge cases, and documentation improvements — not blockers for merge but important for product quality.