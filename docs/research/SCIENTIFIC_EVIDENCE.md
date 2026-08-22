# Scientific Evidence Base for GymCoach Rules

**Date:** 2026-08-22 · **Status:** Expands and supersedes [`fitness-evidence.md`](fitness-evidence.md) (retained as append-only source log; all citations carried forward and re-verified there).

Template per rule: **Finding → Source → Evidence level → Practical implication → Uncertainty → GymCoach implementation.**
Evidence levels: `Position Stand` · `SR/MA` (systematic review/meta-analysis) · `RCT` · `EMG/mechanistic` · `Convention` (standard practice, no comparative trial evidence).

---

## R1. Volume Dose-Response (sets/muscle/week)
- **Finding:** Hypertrophy scales with weekly volume with diminishing returns; ≥10 sets/muscle/week outperforms lower doses; plateau zone ≈18–20+ sets. Minimum effective dose ≥2 sets/exercise.
- **Source:** ACSM Position Stand 2026, MSSE (137 SRs, >30k participants): https://journals.lww.com/acsm-msse/fulltext/2026/04000/american_college_of_sports_medicine_position.21.aspx (PMID 41843416) · Schoenfeld, Ogborn & Krieger 2017, J Sports Sci 35(11):1073–1082, DOI: 10.1080/02640414.2016.1210197
- **Level:** Position Stand + SR/MA
- **Practical implication:** Prescribe 10–20 hard sets/muscle/week; novices start at the low end.
- **Uncertainty:** Individual tolerance varies; joint-specific ceilings (side delts tolerate more) mechanistically plausible, unproven.
- **Implementation:** VolumeCalculator targets 12–16 (V-taper primaries), 10–14 (secondary), 8–12 (maintenance); home dashboard plots sets vs the 10–20 band (DATA_MODEL Q3).

## R2. Training Frequency (≥2×/muscle/week)
- **Finding:** 2×/week beats 1× when volume isn't equated; when volume IS equated, higher frequency adds nothing. Frequency is a volume-delivery tool.
- **Source:** Schoenfeld et al. 2016, Sports Med 46(11):1689–1697, DOI: 10.1007/s40279-016-0543-8 · Schoenfeld, Grgic & Krieger 2019, DOI: 10.1080/02640414.2018.1555906 · Grgic 2019, DOI: 10.1016/j.jsams.2018.09.223 · ACSM 2026 (above)
- **Level:** SR/MA ×3 + Position Stand
- **Practical implication:** Hit each muscle ≥2×/week; total weekly volume is the operative variable.
- **Uncertainty:** None material for prescription purposes.
- **Implementation:** All split templates (full-body A/B/C, upper/lower, PPL variants) guarantee ≥2×/muscle/week at 3–6 days.

## R3. Proximity to Failure / RIR·RPE
- **Finding:** Hypertrophy improves as sets terminate closer to failure; strength gains largely indifferent; failure not required. RIR-anchored RPE scales are acceptably valid for rating proximity.
- **Source:** Remmert et al. 2024, Sports Med 54(9):2209–2231: https://pubmed.ncbi.nlm.nih.gov/38970765/ · Zourdos et al. 2016, JSCR 30(5):1437–1452, DOI: 10.1519/JSC.0000000000001339 · ACSM 2026 (above)
- **Level:** SR/MA (meta-regression) + scale-validation study
- **Practical implication:** Most working sets at ~0–3 RIR; occasional failure fine on isolation; systematic failure training raises fatigue cost without clear premium.
- **Uncertainty:** Remmert authors flag exploratory analysis + RIR-estimation error; effect sizes per RIR are small.
- **Implementation:** `program_exercises.target_rir` defaults 1–3; `workout_sets.rir` optional field; isolation finishers occasionally prescribed to RIR 0. Never displayed as "% effort".

## R4. Load & Hypertrophy
- **Finding:** Across 30–100 % 1RM (volume-equated), hypertrophy is similar when sets are taken sufficiently close to failure; heavier loads (≥80 %) favor strength specifically.
- **Source:** ACSM 2026 (above) · Lasevicius et al. 2018, Eur J Sport Sci, DOI: 10.1080/17461391.2018.1473590
- **Level:** Position Stand + RCT battery
- **Practical implication:** Dumbbell-only training is not inherently limited for hypertrophy; effort + volume matter more than implement.
- **Uncertainty:** Very light loads (<30 %) demand grinding proximity to failure — adherence cost.
- **Implementation:** Rep-range prescriptions 6–12 for compounds, 8–15/12–20 isolations; home-minimal programs rely on R3/R4 together (load headroom → rep escalation, SPEC §8).

## R5. Progressive Overload Rule
- **Finding:** When target repetitions can be exceeded by 1–2, increase load 2–10 %.
- **Source:** ACSM Position Stand 2002, MSSE 34(2):364–380: https://journals.lww.com/acsm-msse/fulltext/2002/02000/progression_models_in_resistance_training_for.27.aspx
- **Level:** Position Stand (consensus rule)
- **Practical implication:** Double progression (rep-range top-out → load jump) is a faithful operationalization.
- **Uncertainty:** **No RCT compares named algorithms** (double progression vs %-1RM linear vs micro-loading) — recorded gap in fitness-evidence.md. Treat algorithm choice as convention, not evidence-differentiated.
- **Implementation:** ProgressionEngine implements double progression (+2–10 % respecting `increment_kg`); regression −5–10 % after 2 consecutive bottom-range misses; suggestions advisory with source-cited tooltip (SPEC PS-6).

## R6. Split vs Full-Body
- **Finding:** No inherent hypertrophy/strength superiority for any split when volume equated; split choice = scheduling preference.
- **Source:** Ramos-Campo et al. 2024, JSCR 38(7):1330–1340, DOI: 10.1519/jsc.0000000000004774 (+ RCTs: Pedersen 2022 DOI: 10.1186/s13102-022-00481-7; Evangelista 2021; Bartolomei 2021 DOI: 10.1519/jsc.0000000000003573)
- **Level:** SR/MA + multiple RCTs
- **Practical implication:** Pick split by adherence + schedule; satisfy R2 volume distribution.
- **Uncertainty:** Trained-population long-term comparisons sparse.
- **Implementation:** Onboarding offers splits by days/week; UI copy says "choose what you'll stick to" — never claims superiority.

## R7. Protein Intake
- **Finding:** ~1.6 g/kg/day maximizes retention of lean mass gains (breakpoint CI spans ~1.03–2.2); position stands recommend 1.4–2.0 g/kg/day; ~0.4 g/kg per meal across ≥4 meals is a practical distribution.
- **Sources:** Morton et al. 2018, Br J Sports Med 52(6):376–384, DOI: 10.1136/bjsports-2017-097608 · Jäger et al. 2017 (ISSN Position Stand), JISSN 14:20, DOI: 10.1186/s12970-017-0177-8 · Schoenfeld & Aragon 2018, DOI: 10.1186/s12970-018-0215-1
- **Level:** SR/MA + Position Stand
- **Practical implication:** For the 70 kg default user: ~100–140 g/day, spread across meals.
- **Uncertainty:** Breakpoint CI wide; per-meal ceiling debated (myth-busted as strict 20–30 g cap, but optimal pulse still unclear).
- **Implementation:** Settings protein calculator: bodyweight × 1.4–2.0 band, labeled "estimate, not a prescription". No food tracking, no macro moralizing.

## R8. Home-Based Resistance Training
- **Finding:** Loads ≥30 % 1RM taken near failure produce comparable hypertrophy (R4); push-up vs bench press show comparable prime-mover EMG at matched relative intensity; dedicated longitudinal home-vs-gym comparative trials were **not located/verified**.
- **Sources:** Lasevicius 2018 (above) · Calatayud et al. 2015, JSCR, DOI: 10.1519/JSC.0000000000000746 · ACSM 2026 setting-agnostic recommendations
- **Level:** RCT + EMG/mechanistic; **comparative gap flagged**
- **Practical implication:** Home-minimal programming is physiologically legitimate; honesty requires not claiming parity "proven" head-to-head.
- **Uncertainty:** Loading progression ceilings without external load; solved practically via leverage manipulation + rep ranges.
- **Implementation:** Home-Minimal preset is first-class; bodyweight tonnage via `bodyweight_load_pct`; no marketing claim of "equal to gym training".

## R9. Range of Motion
- **Finding:** Full ROM superior to partial ROM overall for hypertrophy/strength.
- **Source:** Pallarés et al. 2021, Scand J Med Sci Sports 31(10):1866–1881, DOI: 10.1111/sms.14006 (+ Schoenfeld & Grgic 2020, DOI: 10.1177/2050312120901559)
- **Level:** SR/MA
- **Practical implication:** Cue stretch-position-biased full ROM; shortened-only partials unsupported.
- **Uncertainty:** Lengthened-partials literature promising but individually unverified (recorded gap).
- **Implementation:** Exercise instruction fields emphasize depth/stretch checkpoints; no ROM percentage metrics displayed.

## R10. Tempo / Time Under Tension
- **Finding:** Moderate rep durations (~2–8 s) fine; very slow inferior; controlled vs self-selected tempo showed no significant difference; ACSM: no consistent effect.
- **Sources:** Schoenfeld, Ogborn & Krieger 2015, DOI: 10.1007/s40279-015-0304-0 · Chaves et al. 2020, PeerJ, DOI: 10.7717/peerj.8697 · ACSM 2026
- **Level:** SR/MA + RCT + Position Stand
- **Practical implication:** Loose guardrail (controlled eccentric, no bouncing) beats rigid tempo codes.
- **Uncertainty:** Low stakes either way.
- **Implementation:** `tempo_guidance` rendered as coaching hint, never scored/tracked. No TUT stat anywhere.

## R11. Recovery Spacing
- **Finding:** Early-phase hypertrophy relates to elevated myofibrillar protein synthesis after damage attenuation — supports ~48 h spacing for the same muscle, especially early training.
- **Source:** Damas, Libardi & Ugrinowitsch 2018, Eur J Appl Physiol 118(3):485–500, DOI: 10.1007/s00421-017-3792-9 (+ Damas 2016, DOI: 10.1113/jp272472)
- **Level:** Longitudinal mechanistic review
- **Practical implication:** Splits naturally space muscle groups; consecutive-day same-muscle sessions warrant a soft flag.
- **Uncertainty:** 48 h is heuristic, not threshold law; trained individuals recover faster.
- **Implementation:** ReadinessCheck + conservative copy ONLY ("recent training load suggests…") — never computed recovery percentages (SPEC §5, PS-11).

## R12. Deloads
- **Finding:** Direct RCT evidence shows scheduled one-week deloads did **not** enhance muscular adaptations vs continuous training; practice surveys show widespread coach use anyway.
- **Sources:** Coleman et al. 2024, PeerJ, DOI: 10.7717/peerj.16777 · Pancar et al. 2026, Sci Rep, DOI: 10.1038/s41598-026-40612-5 · surveys: De Marco 2024 DOI: 10.1519/jsc.0000000000004932; Rogerson 2024 DOI: 10.1186/s40798-024-00691-y
- **Level:** RCT ×2 + practice surveys
- **Practical implication:** Deloads are a fatigue-management/adherence tool, not a growth enhancer; never mandatory.
- **Uncertainty:** Long-term trained lifters understudied; fatigue accumulation beyond study lengths unexplored.
- **Implementation:** `program_weeks.is_deload` exists; generator offers (not forces) deload placement; copy cites RCT finding when offered.

## R13. Estimated 1RM Honesty (Epley)
- **Finding:** e1RM formulas estimate from a single set; accuracy degrades with reps; above ~12 reps estimates inflate materially.
- **Source:** Epley, B. (1985), poundage chart convention; widely validated informally — **no modern validation RCT cited as gold standard; labeled Convention**.
- **Level:** Convention
- **Practical implication:** Useful trend metric; useless as measured strength truth; never prescribe training from it as if tested.
- **Uncertainty:** Formula-family differences (Epley/Brzycki) small at low reps, large at high.
- **Implementation:** Epley only, hard-capped at 12 reps (DATA_MODEL Q4), always suffixed "est."; `pr_type='est_1rm'` visually distinct from weight PRs. No 1RM testing days programmed.

## R14. V-Taper Regional Selection (lats · lateral delt · upper chest · rear delt)
- **Finding:** Regional activation/regional growth links exist (pec region grows non-uniformly by bench inclination); lat excitation varies with grip/pulldown geometry; deltoid regional activation varies by raise variant/press. BUT: no controlled trial sets vertical:horizontal pull ratios, no verified single "optimal incline angle", rear-delt→shoulder-width appearance is aesthetic inference only.
- **Sources:** Albarello 2022 DOI: 10.1016/j.jelekin.2022.102722 · Chaves 2020 DOI: 10.70252/fdnb1158 · Andersen 2014 DOI: 10.1097/jsc.0000000000000232 · Padovan 2024 DOI: 10.5114/jhk/185211 · Padovan 2025 DOI: 10.3390/jfmk11010006 · Coratella 2020 DOI: 10.3390/ijerph17176015 · Coratella 2022 DOI: 10.3389/fphys.2022.825880 · McAllister 2013 DOI: 10.1519/jsc.0b013e31824f23ad
- **Level:** EMG/mechanistic (+1 regional-growth ultrasound study)
- **Practical implication:** Prioritize vertical pulls + direct lateral-delt isolation + incline pressing + rear-delt rowing volume — while admitting mechanism-level, not outcome-level, proof.
- **Uncertainty:** Highest of all sections; encoded as *priorities*, never as guaranteed aesthetics.
- **Implementation:** `vtaper_*` relevance scores drive volume allocation only (12–16 sets primaries); UI never promises width gains, only "targets lats/lateral delts per regional-activation research".

## Anti-Pattern Register (product-wide prohibitions)

1. **Spot reduction** — no ab-work-for-waist claims; waist trend charts carry the disclaimer (SPEC PS-9). Classical literature does not support spot reduction.
2. **Skeletal modification** — no height-posture-growth claims, ever.
3. **Fake-precision body fat** — user-entered BF% shown as "self-measured"; no formula-derived BF% presented as accurate; Navy-formula calculators excluded.
4. **Testosterone/hormone inference** — no T-boosting exercise claims, no hormone estimates from lifts or measurements.
5. **e1RM as measured** — covered R13; est. values never rendered in measured-strength contexts (records pages use distinct visual treatment).
6. **Numeric recovery/readiness scores** — R11; qualitative language only.
7. **Unverified constants** — no "2:1 pull ratio", no "30° optimal incline", no mandatory-deload scheduling presented as evidence-based (see Unverified Claims in fitness-evidence.md).
8. **V-taper score / physique scoring** — no composite aesthetic number; progress = measured sets, loads, tapes, photos. Full stop.