# Training Engine Evidence-Based Audit

- **Target branch:** `main` (@ 94eca0f0299f9791dcdd25463bd0ccb320f6e732)
- **Auditor role:** Skeptic
- **Read-only:** no production code modified. This file is the only artifact added.
- **Method:** static review of the five training-engine files + supporting entity/DAO/database + seed data, cross-checked against the cited literature (sources independently verified, see §Evidence Verification).
- **Line numbers:** exact where the file content allowed counting; approximate (`~`) where only a fetched blob was available.

---

## Evidence Verification (sources cited in `docs/research/fitness-evidence.md`)

| Source | Status | Confirmed claim |
|---|---|---|
| ACSM 2026 Overview of Reviews, MSSE 58(4) Apr 2026, DOI 10.1249/MSS.0000000000003897 | VERIFIED | 137 SRs, >30k participants; hypertrophy enhanced by higher volume (≥10 sets/muscle/wk) and eccentric overload; frequency NOT superior when volume equated; strength enhanced by ≥80%1RM, complete ROM, 2–3 sets, **beginning of session**, ≥2×/wk; failure/equipment/order/periodization NOT consistent modifiers. |
| Schoenfeld, Ogborn & Krieger 2017, J Sports Sci 35(11):1073, DOI 10.1080/02640414.2016.1210197 | VERIFIED | Graded dose-response; ~+0.37%/set; categorical ES 0.307 (<5), 0.378 (5–9), 0.520 (10+). |
| Remmert, Robinson, Pelland, Refalo, Zourdos, Steele & Jukic 2024, Sports Med 54(9):2209, DOI 10.1007/s40279-024-02156-2 | VERIFIED | RIR meta-regression; hypertrophy improves as sets terminated closer to failure (slope ~−0.019/−0.023 per RIR, CI excludes null); **strength** slope contains null (negligible effect). |
| Pallarés 2021 ROM, Scand J Med Sci Sports 31(10):1866, DOI 10.1111/sms.14006 | VERIFIED | Full ROM superior to partial for strength (ES 0.56) and lower-limb hypertrophy (ES 0.88); architecture unchanged. |
| Ramos-Campo 2024 Split vs Full-body, J Strength Cond Res 38(7):1330, DOI 10.1519/JSC.0000000000004774 | VERIFIED | No difference (strength or hypertrophy) between split and full-body when volume equated (k=14, 392 subjects, low heterogeneity). |

Research document faithfully represents these sources **and** correctly self-labels unsupported claims (vertical:horizontal pull ratios, single optimal incline angle, rear-delt aesthetic causality, named-progression algorithm superiority, mandatory deload benefit, spot-reduction).

---

## §0. BLOCKER: main does not compile (engines are dead code, untestable)

**Evidence:** `app/src/main/kotlin/com/gymcoach/app/data/local/database/GymCoachDatabase.kt` (main @ 94eca0f0) imports and declares abstract accessors for **14 DAOs** (`BodyMeasurementDao, EquipmentDao, ExerciseAliasDao, ExerciseEquipmentDao, ExerciseMuscleDao, ExerciseSubstitutionDao, FavoriteExerciseDao, MuscleDao, PersonalRecordDao, ProgramDao, ProgramDayDao, ProgramExerciseDao` + `ExerciseDao, WorkoutDao`). The `…/dao/` directory on `main` contains **only** `ExerciseDao.kt` and `WorkoutDao.kt` (confirmed by tree listing). A branch literally named `repair/restore-build` exists, indicating the team already tripped this.

Concretely the five audited engines cannot type-check against the committed DAO:
- **ProgramGenerator.kt:30** `exerciseDao.getAll()` is assigned into a `List<ExerciseEntity>` (`generateFullBody(exercises: List<…>)`), but `ExerciseDao.getAll()` returns `Flow<List<ExerciseEntity>>` (ExerciseDao.kt:10).
- **SubstitutionEngine.kt** injects `ExerciseMuscleDao` + `ExerciseSubstitutionDao` — files do not exist on main.
- **SubstitutionEngine.kt:17/22/30** `exerciseDao.getById(...)` is used as a nullable entity (`?: return …`) and `.muscleGroup`/`.equipment` read directly, but `ExerciseDao.getById()` returns `Flow<ExerciseEntity?>`.
- Only `exerciseDao.getAll()` (Flow) and `getFilteredExercises` (Flow) exist; no `Flow<List>` is ever collected/`first()`-ed in the engines.

`WorkoutSetEntity.reps: Int` and `weight: Double` are correct; `setType` field (0=NORMAL,1=WARMUP,2=DROP,3=FAILURE, ExerciseSetEntity? workout_sets migration) exists and is ignored everywhere below — see §5.

**Severity:** BLOCKER

**Required fix:** Restore/remove the missing 12 DAO interfaces so the module compiles, or change `ProgramGenerator`/`SubstitutionEngine` to consume `Flow` (e.g. `first()`). Until this lands, **no runtime behavior can be exercised**; every behavioral finding below is proven by static analysis of the algorithm + seed data, not by execution.

---

## §1. ProgramGenerator.kt

Architecture note: seed data (`GymCoachDatabase.seedExercises`) models `equipment` as discrete strings (`Barbell, Bodyweight, Dumbbell, Machine, Cable`); there is **no "Bench" equipment row** and **no equipment inventory** concept. The `vtaper_*` columns are populated (0–10) precisely to rank V-taper relevance — they are never read.

### 1.1 Candidate ranking is alphabetical, not evidence-ranked. Risk: an all-chest day.
- **Location:** `buildDay` L87-105 (candidate selection L90-95).
- **Evidence:** candidates `filter { muscleGroup == muscle || secondaryMuscles.contains(muscle) }` then `.filter { equipment… }` then `.take(2)`; `allExercises` is ordered `ORDER BY LOWER(name)` (ExerciseDao.kt:8). No sort by `vtaper_*`, `category`, `tags`, or movement pattern. Because `secondaryMuscles` is an untokenized comma-joined string, **an isolation/arm/core move whose `secondaryMuscles` lists a torso muscle can displace a compound primary** and the **same exercise can be selected once per muscle-slot** (no de-dup), so e.g. `Bench Press` (secondary `"Triceps, Shoulders"`) is eligible for the Chest, Shoulders, **and** Triceps slots simultaneously.
    - Chest slot (gym): candidates → Bench Press (musGrp=Chest), Push-up, … `take(2)` → **Bench Press, Push-up**.
    - Shoulders slot: eligible incl. Bench Press (secondary contains Shoulders) and Dumbbell Fly (seed `muscleGroup="Shoulders"` — a misclassification of a fly), Lateral Raise, Shoulder Press → alphabetical top-2 → **Dumbbell Fly, Lateral Raise** … but with a slightly different seed set Bench Press re-enters. 
    - Net effect: a single day can contain multiple pressing derivatives and **no lateral-deltoid isolation**, the opposite of the V-taper claim the program advertises.
- **Severity:** HIGH
- **Fix:** tokenize `secondaryMuscles`; rank by `vtaper_*` then `category` (compound>iso) then `tags`; de-duplicate `exerciseId` across slots within a day; assert ≥1 posterior + ≥1 lateral-delt movement when Shoulders present.

### 1.2 Equipment filtering is a magic string; home-gym inventory ignored.
- **Location:** `buildDay` L94 `equipment == "gym" || it.equipment == "Bodyweight" || it.equipment == "Dumbbell"`.
- **Evidence:** caller `generateProgram(…)` receives `equipmentType: String` with **no enum/contract** documenting the value `"gym"`. The target user profile (prompt) is “dumbbells + bodyweight + bench”, yet seed lists no `Bench` equipment; barbell-only moves (Squat, Deadlift, Bent-over Row, Bench Press, Barbell Curl) are silently dropped for the non-`"gym"` branch. `experienceLevel` and `goal` params are accepted and **only echoed into the description string** (L38-43) — not wired to sets/reps/RPE.
- **Severity:** MEDIUM
- **Fix:** replace magic string with a typed `EquipmentSet` flag set; respect a bench as a surface, not resistance type; vary prescription by `goal`/`experienceLevel` (novices: fewer sets, higher RPE buffer; strength goal: heavier singles).

### 1.3 Lower-body / posterior-chain volume collapses for home equipment.
- **Location:** `generateUpperLower` Lower-A `listOf("Quadriceps","Hamstrings","Glutes","Calves")` (L47-49) and `buildDay` L90-95.
- **Evidence:** seed candidates filtered by the home-equipment branch (Bodyweight/Dumbbell):
    - **Hamstrings:** Leg Curl (`Machine`) + Deadlift (`Barbell`) → both excluded → **0 hamstring movements**.
    - **Calves:** Calf Raise (`Machine`) → **0 calves movements**.
    - **Quadriceps:** Squat (`Barbell`), Leg Press/Extension (`Machine`) excluded → only Lunge (`Bodyweight`) survives ⇒ **3 sets/quad/week** (3×/wk×… actually Lower A+B both). Across 2 lower days: 6 sets — **below** the ACSM/Schoenfeld 10+ evidence minimum by ~40%.
- **Severity:** HIGH
- **Fix:** classify exercises by *pattern* + *equipment availability* so dumbbell RDL, single-leg hip thrust, and banded/carry variants fill posterior-chain slots; guarantee ≥10 hard sets/muscle for home configuration or downgrade recommendation confidence.

### 1.4 Rigid, one-size-fits-all prescription with no progression linkage.
- **Location:** `buildDay` L99-100 (`targetSets = 3`, `targetRepsMin = 8`, `targetRepsMax = 12`, `targetRpe = 7.5`, `restSeconds = 90` for **every** exercise).
- **Evidence:** ACSM 2026: strength benefits from 2–3 sets, ≥2×/wk, **exercise order** (compound first) beginning of session — all overridden by alphabetical order, and isolation/leg-days receive 3 sets with no escalation. RPE 7.5 ≈ 2–3 RIR ✓ with Remmert 2024; rest 90s within ACSM 1–2 min ✓. No deload/periodization wiring (research doc itself flags deload as evidence-not-beneficial, so that is *correct*; but the generator has no progression hook into `ProgressionEngine`).
- **Severity:** MEDIUM
- **Fix:** parameterize set/rep/RPE by `goal` (strength: 3–5×3–6 @ 8–9 RPE; hypertrophy: 3×6–12 @ 7–8 RPE); sort compound before isolation; feed `targetRepsMin`/`Max` into `ProgressionEngine` rather than hard-coding 8–12.

### 1.5 Positives
- Split options all satisfy **≥2×/muscle/week** distribution ✓.
- Full-body/U-L/PPL ordering itself is evidence-neutral (Ramos-Campo 2024) ✓.
- Rep range 6–12 and RPE ~7.5 sit inside the supported hypertrophy zone ✓.

---

## §2. VolumeCalculator.kt

### 2.1 The 1.0/0.5/0.25 crediting is implemented but **never applied**.
- **Location:** `MuscleRole` enum (L47-49) and `MuscleAssignment` dataclass (L51).
- **Evidence:** `calculateWeeklyVolume` sums `weeklySetsByMuscle + weeklyIndirectByMuscle` as raw ints — the `credit` field of `MuscleRole` is defined but **no function multiplies by it**. `TrainingBalance` has no call-site on main that weights assignments. Dead abstraction ⇒ indirect volume is double-counted at full weight.
- **Severity:** MEDIUM
- **Fix:** either apply `role.credit * sets` when accumulating, or delete `MuscleRole`/`MuscleAssignment` and document raw-set accounting.

### 2.2 Volume-status thresholds drift from the evidence band.
- **Location:** `classify` L91-96.
- **Evidence:** `<8`→INSUFFICIENT, `8-11`→MODERATE, `12-15`→HIGH, `≥16`→EXCESSIVE. Research doc: evidence band **10–20 hard sets/week** (Schoenfeld 10+ best ES 0.520; ACSM 2026 ≥10). 8–9 sets are therefore labeled MODERATE though **below minimum effective dose**; 16–20 sets are labeled EXCESSIVE though still in the supported band.
- **Severity:** MEDIUM
- **Fix:** INSUFFICIENT <10, MODERATE 10–14, HIGH 15–19, EXCESSIVE ≥20 (matching doc §1’s own “≥18–20 diminishing returns” note).

### 2.3 Weekly-window / date-boundary logic does not exist → check #2 “weekly aggregation” unverifiable.
- **Location:** `calculateWeeklyVolume` accepts pre-summed `Map<String,Int>`; no date filtering anywhere on main (no caller present).
- **Evidence:** the only test file on main is `ExerciseRepositoryTest.kt`; zero callers of VolumeCalculator. Weekly bucketing by calendar boundary (Mon-Sun) is an unstated contract.
- **Severity:** HIGH (unwired)
- **Fix:** add a `WeeklyBucket` that partitions `WorkoutSetEntity` by `Date`/`Instant` into ISO weeks (kotlinx-datetime `instantAtStartOfWeek`), then feed weekly maps in.

### 2.4 Bodyweight volume not modeled (`weight` may be 0 — see §3/§5).
- **Location:** `calculateWeeklyVolume` input contract.
- **Evidence:** for pure bodyweight moves the volume unit is **reps·sets**, not kg — a kg-weighted map silently reads 0.
- **Severity:** LOW (delegated to caller) — see §5 where PRDetector emits 0 kg.

---

## §3. ProgressionEngine.kt

### 3.1 Double progression is correctly implemented.
- **Location:** `calculateProgression` L20-70; rep-target branch L58-68 (maintain) + load branch L33-45 (increase at top-of-range) + regression L46-57.
- **Evidence:** standard two-variable model — progress reps within 8–12 while load stable, add load once all sets reach 12 ✓; ACSM load-increment rule (2–10% once target rep reached) structurally honored by the `calculateIncrease` tiers.
- **Severity:** info / positive.

### 3.2 Load increments breach the 2–10% ACSM rule for light loads / isolation moves.
- **Location:** `calculateIncrease` L72-79.
- **Evidence:** `<20kg → +2kg` ⇒ 5 kg lateral-raise → 7 kg = **+40%** jump; `<50kg → +2.5kg` ⇒ 12.5 kg curl → 15 kg = +20%. ACSM 2026: 2–10% load step. For the home-user target (fixed-step dumbbells, often 1–2.5 kg increments on isolation), jumps land outside the rule and force artificial stalls.
- **Severity:** HIGH
- **Fix:** relative step (e.g. 5% of currentWeight, floored to the smallest available increment, minimum 0.5–1.0 kg) with an exerciser-configurable `minIncrement`.

### 3.3 `currentWeight` taken from the **first** set — warmup contamination.
- **Location:** L28 `val currentWeight = currentSets.firstOrNull()?.weight ?: 0.0`.
- **Evidence:** `setType` enum exists (NORMAL/WARMUP/DROP/FAILURE). First set is frequently the lightest (warmup). Increasing load from a warmup weight is wrong.
- **Severity:** HIGH
- **Fix:** use the **work-set** (setType==NORMAL) median/last working weight.

### 3.4 Equipment ceiling and bodyweight ceiling are not modeled.
- **Location:** `calculateIncrease` on `weight == 0` (bodyweight) → `+2.0 kg`; no `maxAvailableWeight` input.
- **Evidence:** push-up (0 kg) “all top” ⇒ recommended `2.0 kg` — silently recommends weighted vest with no inventory. Limited-dumbbell scenario ⇒ `recommendedWeight` keeps climbing past the heaviest bell, never switching to rep-range expansion / set addition / tempo manipulation (Remmert proximity-to-failure already covered by fixed RPE 7.5, good).
- **Severity:** HIGH
- **Fix:** pass `EquipmentInventory` (max plate/Dumbbell); when `recommendedWeight > max`, emit “extend reps to rep-max, then add a set” instead. Bodyweight exercises: progression via reps/time-under-tension only — never output a kg recommendation.

### 3.5 Reason strings partially explain, but never mention magnitude.
- **Location:** `reason` fields L36, L50, L61.
- **Evidence:** messages name the trigger but not *why the suggested number* (e.g. “Increase weight” without “+5 kg (5% of 95 kg, within ACSM 2–10% step)”).
- **Severity:** LOW
- **Fix:** append magnitude + rule citation to `reason`.

### 3.6 Commit-message claims not delivered.
- **Location:** commit `62674bc2` message: “Implements double progression, **regression detection**, and **stalling detection**.”
- **Evidence:** stalling detection (`allHitTop == false` AND `anyBelowMin == false`) collapses into the `else` “maintain” branch — indistinguishable from a true stall. No separate stalling signal.
- **Severity:** LOW (acceptance-criteria deviation)
- **Fix:** track consecutive “maintain” sessions; surface a `STALLED` confidence tier.

---

## §4. PRDetector.kt

### 4.1 Epley formula correct; 12-rep cap appropriate.
- **Location:** `calculateEstimated1RM` L66-69 `weight * (1 + reps.coerceAtMost(12)/30)`.
- **Evidence:** Epley = weight×(1+reps/30) ✓. Cap at 12 is standard (error balloons past ~12); matches the commit message and the research doc’s caution against >12-rm e1RM.
- **Severity:** info / positive.

### 4.2 Tie handling correct (strict `>`).
- **Location:** `if (maxWeight > (eWPR?.value ?: 0.0))` L34 et al.
- **Evidence:** equal-to-best does **not** register; first-session default `0.0` won’t false-fire ✓.
- **Severity:** info / positive.

### 4.3 REP-PR ignores load context; a 20-rep warmup defeats a 12-rep top set forever.
- **Location:** `bestReps = completed.maxBy { it.reps }` L38.
- **Evidence:** comparison is raw reps only; details string even prints the weight, so a future 30 kg × 12 (e1RM 17.8) is shadowed forever by a 5 kg × 20 (e1RM 5.67). PR “value” is reps, not a comparable strength metric.
- **Severity:** MEDIUM
- **Fix:** REP-PR per fixed load tier, or fold into the BEST_SET/E1RM comparison only.

### 4.4 Warmup/drop sets pollute every PR pool.
- **Location:** `completed = currentSets.filter { it.completed }` L29 — `setType` ignored across WEIGHT/REP/e1RM/VOLUME/BEST_SET L31-55.
- **Evidence:** a `setType == WARMUP` set marked `completed` (routine) counts toward maxWeight and volume, overstating volume PRs and injecting low-weight noise into e1RM pools.
- **Severity:** MEDIUM
- **Fix:** `filter { it.completed && (setType == 0 || setType == 3) }` (exclude warmup; keep drop/failure as legitimate hard sets).

### 4.5 BEST_SET ≡ ESTIMATED_1RM (redundant record type).
- **Location:** L41-44 and L51-54.
- **Evidence:** `bestSetE1RM = maxOf { calculateEstimated1RM }` equals `bestE1RM`; same `e1PR` comparison; both PR types will always fire together — same value, two rows.
- **Severity:** LOW
- **Fix:** drop `BEST_SET` (or redefine as best *raw weight×reps under 12* set, distinct from e1RM).

### 4.6 Bodyweight exercises can never earn WEIGHT/e1RM/VOLUME/BEST_SET PRs.
- **Location:** `calculateEstimated1RM` returns 0 when `weight <= 0`; `calculateVolume` returns 0 kg; WEIGHT `maxWeight > 0.0` comparison.
- **Evidence:** for the push-up/pull-up user (the target equipment mix) only `REP-PR` (§4.1) and possibly `VOLUME` (but 0 kg ⇒ 0 > 0 false ⇒ no volume PR either) can fire. Effectively **single-variable PR tracking** for bodyweight moves.
- **Severity:** HIGH
- **Fix:** for weight-class-null exercises, volume = reps×sets; e1RM proxies = reps-based; or store `isBodyweight` flag and branch scoring.

---

## §5. SubstitutionEngine.kt

### 5.1 Dead dependency `exerciseMuscleDao` is injected but unused (and missing on main — see §0).
- **Location:** ctor L22-25; never referenced in body.
- **Evidence:** `ExerciseMuscleDao` file does not exist on `main`; even if it did the parameter is unused.
- **Severity:** LOW (clean-up)

### 5.2 Scoring does not use the `movementPattern` / `secondaryMuscles` fields; “preserve intent = same muscle + similar movement” is only half-met.
- **Location:** `calculatePreservationScore` L53-64.
- **Evidence:** weights: muscleGroup 40, category 20, equipment 15, difficulty 10, compound/isolation tag 10. `ExerciseEntity` ships a populated `movementPattern` (“Push/ Pull”) column (DB migration, seed rows) — **never consulted**. A Push-dominant pressing move can score 40+ substituting for a hinge/pull because both share `muscleGroup`/`category` loosely.
- **Severity:** MEDIUM
- **Fix:** add movement-pattern match (e.g. +10 when `movementPattern` matches) and subtract on opposing patterns.

### 5.3 Fallback “same muscle group” is a substring match on a free-form `tags`/comma fields; equipment filter ignores case.
- **Location:** L32-35 `muscleGroup == original.muscleGroup`; `substitute.equipment in availableEquipment` (case-sensitive).
- **Evidence:** no `.lowercase()` on either side; an `availableEquipment = ["dumbbell"]` would miss seed `equipment = "Dumbbell"`.
- **Severity:** LOW
- **Fix:** canonicalize equipment tokens to a lowercase set.

### 5.4 Score ceiling 100 unreachable; no normalization.
- **Location:** `coerceAtMost(100)` L64 (max achievable = 95).
- **Evidence:** cosmetic but signals missing weighting math.
- **Severity:** LOW
- **Fix:** reweight components to 25/20/20/10/20 (=95→normalize to /95) or drop the cap.

---

## §6. Acceptance-criteria deviations vs. commit messages

| Commit message claim | Present? | Evidence |
|---|---|---|
| ProgressionEngine: “double progression” | YES (L20-70) | §3.1 |
| …“regression detection” | YES but strict (L46,88 `all < min`) | §3 |
| …“stalling detection” | NO — folds into maintain branch | §3.6 |
| PRDetector “Epley formula capped at 12 reps” | YES | §4.1 |
| “All PRs mathematically proven from actual workout data” | PARTIAL — warmup leakage, bodyweight zeroing, REP-without-load | §4 |
| Volume “with V-taper balance” | Vtaper branch coded (L80-88) but built from status ordinals (0–3) — ordinal averaging is not a validated metric; not evidence-backed | §2.2 + §2.1 |

---

## §7. Summary verdict

**FAIL — BLOCKING.**

The five-engine surface area is conceptually aligned with the research document (V-taper emphasis, 10+ hard sets, 6–12 rep zone, ~2–3 RIR, double progression) but is **not wired, does not compile on `main`, and contains three evidence-conflict bugs rated HIGH** (ProgramGenerator chest-dominant day + home-leg-volume collapse, ProgressionEngine warmup/bodyweight/equipment-ceiling misuse, PRDetector bodyweight dead-zone + warmup leakage). Fix the compile blocker (§0) first; the behavioral items cannot be tested until the module builds.

### Priority fix order
1. **BLOCKER** §0 — restore DAOs / Flow usage so the module compiles.
2. **HIGH** §2.3 + §1.3 + §4.6 — weekly bucketing, home-leg posterior chain, bodyweight PR model.
3. **HIGH** §1.1 + §3.3 + §3.4 — de-dup/compound-first, warmup-set weight sourcing, equipment ceiling.
4. **MEDIUM** §2.2 thresholds, §2.1 credit application, §5.2 movement-pattern scoring, §1.2/1.4 parameterization.
5. Add **one engine test** (assert: no duplicate `exerciseId` per day; e1RM for 12kg×12 = ~19.2 and tie ≠ new PR; +2kg on push-up rejected) — per the audit methodology, a runnable check is required for non-trivial logic.
