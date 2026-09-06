# Phase 5 Program Engine - Entry Gate Report

## 1. Roadmap & Objective Reconnaissance
Following the resolution of the roadmap conflict in `ROADMAP_FORENSIC_AUDIT.md`, Phase 4 (History/Progress Upgrades) was completed successfully. As explicitly dictated by `docs/GYMCOACH_MASTER_SPEC.md`, the authoritative next milestone is **Phase 5: Program Engine**.

**Phase 5 Objective:**
- Volume Calculator (already exists)
- Program Generator (already exists)
- Exercise Selection Algorithm (already exists)
- Split Templates (already exists)

## 2. Current Program Engine Audit
A thorough inspection of `app/src/main/kotlin/com/gymcoach/app/core/program/ProgramGenerator.kt` reveals:
- **Split Templates:** Exist for 3-Day (Full Body), 4-Day (Upper/Lower), 5-Day (PPL+UL), 6-Day (PPLx2).
- **Exercise Selection Algorithm:** Implemented inside `buildDay()`. It dynamically selects exercises based on required muscles, falling back to a deterministic VTaper score ranking (`relevantVtaperScore`) and difficulty ranking.
- **Equipment Filtering:** `filterByEquipment` properly screens the domain before selection.
- **Volume Calculation:** `VolumeCalculator.kt` properly processes these routines.

**What is MISSING?**
The core engine algorithms are present, but the **Data Integration & UX Flows** connecting the engine to the user's Profile (`UserProfileEntity` -> Goal / Schedule / Equipment -> `ProgramGenerator`) are only partially wired or completely absent from the actual UI setup screens (Phase 6 Onboarding is designated for UI, but the repository integration for generation likely needs tightening). Furthermore:
- The exact persistence pathway from `GeneratedProgram` to `ProgramEntity` via `ProgramRepository` is functional but lacks explicit edge case handling for invalid user profiles (e.g. no equipment selected).
- V-Taper prioritizing is rigidly hardcoded to 3 sets of 8-12 reps per exercise regardless of the muscle group or goal.

## 3. Test Coverage Gap Analysis

| BEHAVIOR | EXISTING TEST | STRENGTH | MISSING CASE |
|----------|---------------|----------|--------------|
| Profile -> Generation | None | N/A | Testing generation off a real Profile setup |
| Equipment Constraints | `ProgramGeneratorTest` | Strong | Edge case: Profile has no equipment mapped to any exercise |
| V-Taper Priority | `ProgramGeneratorTest` | Strong | Ensuring it doesn't starve other muscles entirely |
| Persistence | `ProgramRepositoryIntegrationTest` | Strong | Reloading a persisted active program after generation |
| Determinism | `ProgramGeneratorTest` | Moderate | Identical profile yielding identical DB output |

## 4. Proposed Implementation Slice (If Authorized)
**Slice 1: Integration & Edge Cases**
- **Objective:** Connect `UserProfileRepository` explicitly to `ProgramGenerator` via a `GenerateProgramUseCase`.
- **Action:** Read the user's `experienceLevel`, `goal`, and `equipmentAvailability`. Feed these natively into the generator.
- **Edge Cases:** Handle "No matching exercises for equipment constraint" securely without crashing the program builder.

**Decision Gate / Action:**
As per the strict directive: "For THIS SESSION, the default objective is: RECONNAISSANCE + PHASE 4 EXIT GATE + PHASE 5 PLAN... STOP after the plan." I am executing a **HARD STOP**.
