# GymCoach Product Specification

**Version:** 2.0 (Phase 2 — Rebuild)
**Date:** 2026-08-22
**Status:** Approved scope for architecture/data-model design
**Companion docs:** [GYMCOACH_ARCHITECTURE.md](GYMCOACH_ARCHITECTURE.md) · [GYMCOACH_DATA_MODEL.md](GYMCOACH_DATA_MODEL.md) · [research/PRODUCT_BENCHMARK.md](research/PRODUCT_BENCHMARK.md) · [research/SCIENTIFIC_EVIDENCE.md](research/SCIENTIFIC_EVIDENCE.md)

---

## 1. Product Vision

GymCoach is an **offline-first personal strength coach** for Android. It tells the user exactly what to train today, why (evidence-linked), logs every set in under two taps, runs a lock-screen rest timer that chains through supersets, detects every personal record mathematically from logged data, and shows weekly progress toward a V-taper physique.

Three non-negotiables:
1. **No fake data.** Every number shown is computed from actual logged sets or explicit user measurements. Estimates are always labeled as estimates.
2. **Evidence-based programming.** Prescriptions trace to the position stands and meta-analyses catalogued in `docs/research/SCIENTIFIC_EVIDENCE.md`. Where evidence is absent, the app stays silent or uses conservative language.
3. **Works fully offline.** Room database is the source of truth. No account, no network calls.

## 2. Target User

| Attribute | Value |
|---|---|
| Sex / Age | Male, 30 |
| Height | 170 cm |
| Goal | Muscular V-taper (wide lats + lateral delts + upper chest, tight waist) |
| Experience | Beginner–intermediate |
| Default frequency | 4 days/week (3–6 supported) |
| Session length | 30–90 min preference |
| Environment | Home or commercial gym, configurable |

**Default equipment profile:** dumbbells (adjustable), adjustable bench, bodyweight/floor. **Not available unless explicitly added by the user:** cable stack, barbell, machines, pull-up bar, dip station. Every generated program must be executable with only the equipment the user confirmed they own.

## 3. Core Product Loop

The loop below is the product. Every screen exists to serve one step of it.

```
open app
 └─> see TODAY'S WORKOUT (name, target muscles, est. duration, why-this-today)
      └─> understand WHY (volume plan vs evidence band, V-taper priority)
           └─> START SESSION
                └─> LOG SET (weight/reps prefilled from last performance, one tap)
                     └─> REST TIMER starts automatically
                          ├─ lock-screen ongoing notification: Complete Set · Skip · +15s · −15s
                          └─> superset pair? timer chains to antagonist partner exercise
                               └─> PROGRESSION SUGGESTION on next set/exercise
                                    └─> FINISH WORKOUT
                                         └─> SUMMARY (total volume, sets, duration, PRs hit)
                                              └─> PR celebration (only if mathematically earned)
                                                   └─> return HOME
                                                        └─> see WEEKLY PROGRESS (sets/muscle vs 10–20 band, streak)
                                                             └─> (loop repeats tomorrow)
```

Loop acceptance test: a user can complete one full cycle in under 45 minutes of wall-clock time including a 6-exercise session, with zero configuration after onboarding, airplane mode on.

## 4. Required Features & Acceptance Criteria

### PS-1 Onboarding (`PS-1`)
See §6 for flow. **AC:** completing onboarding produces an active 4-week program whose every `program_exercises` row references equipment present in the user's equipment profile; skipping any step is impossible except preferences/injuries.

### PS-2 Home Dashboard (`PS-2`)
Today's workout card (program day name, target muscles, estimated duration, START button), last-session summary, current streak, most recent PR, weekly sets-per-muscle vs the 10–20 hard-set band. **AC:** cold start renders today card <300 ms from Room; airplane-mode safe; empty states per §7.

### PS-3 Session Logging (`PS-3`)
Collapsible exercise cards; set rows with weight/reps steppers; ghost-text of previous performance for same exercise; set types (warm-up, normal, drop, failure); swipe-to-delete with undo; haptic on set completion. **AC:** logging a set = 1 tap on ✓ (values prefilled); works one-handed; no data loss on process death (each completed set persisted immediately).

### PS-4 Rest Timer + Lock-Screen Control (`PS-4`)
Foreground service + ongoing notification with actions: **Complete Set**, **Skip**, **+15 s**, **−15 s**. Timer auto-starts on set completion with per-exercise configured rest; visible on lock screen; survives app close. **AC:** notification countdown updates every second; action buttons mutate running timer without opening app; timer end → vibration/sound (per settings).

### PS-5 Superset Engine (`PS-5`)
Program days may declare superset groups of antagonist pairs (e.g., bench ↔ row, curls ↔ pushdowns). Completing set A starts a short *transition* timer to partner exercise B; after the pair completes both sides, full rest applies. **AC:** chained transitions never dead-end; user can break the chain manually (Skip) without corrupting session state.

### PS-6 Progression Suggestions (`PS-6`)
Double-progression engine: when all prescribed sets reach top of rep range, next session suggests +2–10 % load (smallest increment sensible for implement); two consecutive misses of bottom of range → suggest −5–10 %. Suggestions are advisory and overridable. **AC:** suggestion derivable purely from last N sessions of that exercise; never presented as a guarantee; source rule cited in-app tooltip.

### PS-7 PR Detection (`PS-7`)
Weight PR (heaviest load), rep PR (most reps at a given load), est. 1RM PR (Epley, capped at ≤12 reps, labeled "estimate"), session-volume PR per exercise. **AC:** PR fires only on strictly better measured performance; est. 1RM never displayed as measured; duplicates suppressed.

### PS-8 History & Calendar (`PS-8`)
Chronological workouts with expandable detail; monthly calendar heatmap of training days; "Perform Again" duplication. **AC:** calendar reflects local dates correctly across DST/timezone; deletion cascades cleanly.

### PS-9 Body Measurements & Photos (`PS-9`)
Weight plus tape measurements (waist, chest, shoulders, arms, thighs, neck); trend charts (rolling 7-day average for weight). Progress photos stored **in-app private storage**, encrypted-at-rest directory, path + SHA-256 hash recorded, never in system gallery/media store. **AC:** uninstalling removes photos; photo rows expose no absolute paths in exports; waist trend chart carries disclaimer that abdominal exercise does not spot-reduce waist fat.

### PS-10 Exercise Library (`PS-10`)
200+ seeded exercises; search over name/alias/tags; filters muscle/equipment/difficulty/pattern/V-taper relevance; favorites + recents; substitution suggestions preserving movement pattern and available equipment. Text + static image demos (no video dependency).

### PS-11 Readiness Check-in (`PS-11`)
Pre-workout micro-check (sleep, soreness, motivation, optional resting HR) feeding only conservative language ("recent training load suggests..."). Never numeric recovery percentages. **AC:** check-in can be skipped; outputs are qualitative only.

### PS-12 Settings (`PS-12`)
Units kg/lb (stored canonical kg, display conversion), theme, rest defaults, sound/haptics, data export (JSON), reset. **AC:** unit switch converts all historical displays without mutating stored values.

## 5. Out of Scope (Hard List)

- ❌ No fabricated "AI" scores, readiness scores, muscle-recovery percentages, or V-taper scores. Nothing pseudo-quantified.
- ❌ No fake precision body-fat estimates. User-entered BF% displayed as-is with "self-measured" label; no formula-derived BF% presented as accurate.
- ❌ No spot-reduction claims. Ab work is programmed for musculature, never "to shrink the waist."
- ❌ No testosterone/hormone inference from measurements or lifts.
- ❌ No skeletal-height modification claims (no "grow taller" content).
- ❌ No e1RM presented as measured 1RM; no 1RM testing prescriptions in v1 programs.
- ❌ No social feed, no cloud sync, no accounts (unless user adds sync later — architecture permits, product excludes).
- ❌ No wearable integrations in this phase.
- ❌ No video playback (Media3) — text + images only.
- ❌ Camera form analysis stays unwired (see ARCHITECTURE §8).

## 6. Onboarding Flow

| Step | Collect | Validation |
|---|---|---|
| 1. Goal | V-taper physique (default) · muscle gain · strength · recomposition · general fitness | required |
| 2. Experience | never trained · <1 y · 1–3 y · 3+ y | required |
| 3. Body | sex, age, height cm, weight kg | height 120–230, weight 35–250 |
| 4. Schedule | days/week 3–6 (default 4), session length 30/45/60/90 min | required |
| 5. Equipment | preset Commercial Gym / preset Home-Minimal (dumbbells+bench+bodyweight) / Custom toggles per item | ≥1 usable push+pull capability required |
| 6. Preferences | avoid-list, injuries (free text, advisory) | skippable |
| 7. Generate | builds 4-week program, shows week overview, editable substitutions | — |

All answers persist to `user_profiles` + `equipment_items`. Generation runs locally (rule-based, deterministic given inputs). After step 7 the user lands on Home with Week 1 Day 1 active.

## 7. Empty States

- Fresh install: *"Welcome to GymCoach — your training journey starts here."* [Complete Setup]
- Onboarded, zero workouts: *"Ready for your first workout? Today: {day name}."* [Start First Workout]
- No measurements yet: *"Log your first measurement to start tracking trends."*
- No PRs yet: *"Your first PR is one good session away."*
Copy references and pattern rationale: [PRODUCT_BENCHMARK.md §7](research/PRODUCT_BENCHMARK.md).

## 8. Equipment Profile System

- Canonical equipment taxonomy stored in `equipment_items` (id, key, category, name, available flag, max weight, increments).
- Three presets seed the table: **Commercial Gym** (all available), **Home Minimal** (dumbbells, bench, floor/bodyweight), **Custom** (blank slate).
- Program generator queries availability before selection; substitution resolver falls back through alternatives sharing movement pattern + available equipment.
- Adding/removing equipment mid-program triggers re-validation of remaining scheduled exercises; conflicts surface as substitution prompts, never silent swaps.
- Per-item `max_weight_kg` and `increment_kg` cap progression suggestions (e.g., 2×10 kg adjustables → suggestion granularity respects 2 kg jumps; exhaustion of load headroom suggests rep-range escalation instead).
