# Product Benchmark: Hevy · Strong · Boostcamp · Fitbod · JEFIT

**Date:** 2026-08-22 · **Method:** synthesized from public product behaviour (free tiers, store listings, release notes) as observed during the Phase-2 research pass. Not sponsored audits; strings paraphrased, not quoted. Companion to [../GYMCOACH_PRODUCT_SPEC.md](../GYMCOACH_PRODUCT_SPEC.md).

---

## 1. Positioning Snapshot

| App | What it fundamentally is |
|---|---|
| **Hevy** | Polished freemium workout tracker + social layer. Logging speed is the hero feature. Cloud-sync account centric. |
| **Strong** | Minimalist logger. Zero-friction set entry, deliberately narrow feature set. Premium unlocks charts/plates/wear. |
| **Boostcamp** | Free program-following platform — coach-authored program marketplace (barbell-centric heritage, growing home library). Weak analytics, strong guidance. |
| **Fitbod** | Subscription adaptive generator: algorithm picks exercises/weights from logged history + equipment. Numeric "muscle fatigue/recovery" percentages presented precisely. |
| **JEFIT** | Encyclopedia-scale exercise DB + routines + stats, ad-supported with premium tier. Feature-dense, cluttered. |
| **GymCoach (target)** | Deterministic evidence-linked coach for a known user + known equipment. Tracker-grade logging, coach-grade reasoning, zero accounts. |

## 2. Capability Matrix

Legend: ✓ yes · ~ partial/paywalled · ✗ no

| Dimension | Hevy | Strong | Boostcamp | Fitbod | JEFIT | GymCoach target |
|---|---|---|---|---|---|---|
| One-tap set completion w/ prefill | ✓ | ✓ | ✓ | ✓ | ~ | ✓ |
| Lock-screen rest timer w/ actions | ✓ | ~ | ✓ | ~ | ✓ | ✓ (Complete/Skip/+15/−15) |
| Superset pairing | ✓ | ✓ | ✓ | ✓ (circuits) | ✓ | ✓ + antagonist auto-chaining |
| Program generation/adaptation | ✗ (templates) | ✗ | ✓ (authored) | ✓✓ (algorithmic) | ~ | ✓ deterministic, evidence-linked |
| Equipment-constrained guarantee | ~ | ✗ | ~ | ✓ | ~ | ✓ hard guarantee + substitutions |
| Analytics depth | ✓ | ~ (premium) | ✗ | ~ | ✓ | ✓ honest subset |
| Fully offline, no account | ~ (sync needs account) | ✓ | ✓ | ✓ | ~ | ✓ hard requirement |
| Free-tier generosity | ✗ (history caps) | ✓ | ✓ | ✗ (trial) | ~ (ads) | ✓ core loop free forever |

## 3. Must-Have vs Nice-to-Have

**Must-have (table stakes proven by all five):**
1. Set logging ≤ 2 taps with values prefilled from last performance — every survivor does this; nobody wins without it.
2. Auto-starting rest timer with notification control — expectation set by Hevy/JEFIT; absence reads as broken.
3. Previous-performance visibility at point of logging (ghost text) — Strong/Hevy normalized it.
4. kg/lb toggle, dark mode, haptics, CSV/JSON export.
5. Calendar-style history with expandable sessions.
6. Exercise detail reachable from the set row (form check mid-session).

**Nice-to-have (defer or skip):** plate calculator (Strong; low value for dumbbell-first home users), Wear OS companion, social feed (Hevy — explicitly out of scope), Health Connect sync, 1RM calculator page (we surface est.1RM contextually instead), custom program builder UI (generator covers v1).

## 4. UX Patterns to Borrow

1. **Prefill-from-last-performance** (Strong, Hevy): set row opens with previous weight/reps populated; edit-delta not absolute. Our Q2 query exists for exactly this.
2. **Checkmark-as-commit** (Hevy): tapping the set circle logs it, starts timer, advances cursor — one gesture owns the loop.
3. **Ghost text + delta badge** (Strong): "last: 50 kg × 10" inline; +PR flash when exceeded.
4. **Notification-native timer** (Hevy): countdown lives in the shade, actions work from lock screen — our PS-4 spec matches and extends (+15/−15).
5. **Calendar heatmap** (Hevy/JEFIT): month grid, intensity-shaded training days — PS-8.
6. **Coach voice in-program** (Boostcamp): short plain-language cue per exercise ("drive elbows low") — we render ours with evidence citations attached.
7. **"Why this today" transparency** (Fitbod's good instinct, bad execution): explain exercise selection — but our reasons cite volume bands and research, never mystical percentages.
8. **Empty-state CTAs that start the loop** (all five): every empty screen ends in one actionable button, never a dead end.

## 5. Anti-Patterns to Avoid

1. **Paywalling the core loop** (Hevy free-tier workout-history caps; Fitbod trial-gate): logging + history stay free forever in GymCoach — trust precedes monetization.
2. **Ads anywhere near a live session** (JEFIT): never. Mid-rest interstitials are the fastest uninstall driver in this category.
3. **Pseudo-precision metrics** (Fitbod muscle-fatigue %; various "readiness scores"): violates SPEC §5. Conservative language instead.
4. **Forced account creation** (JEFIT pushes cloud signup; Hevy nags): no account exists in the product.
5. **Feature sprawl on the session screen** (JEFIT): the logging surface carries logging, timer, ghost data — nothing else. Everything else waits behind back-navigation.
6. **Video-only exercise demos** (several): breaks offline + bloats APK. Text + image is our contract.
7. **Silent program mutation**: competitors swap exercises on regeneration without telling users; our equipment-change flow always prompts.

## 6. Home-Equipment Differentiation Strategy

Gap observed: all five optimize for barbell/gym defaults.
- Hevy/Strong assume you know your program; equipment barely enters the model.
- Boostcamp hosts home programs but they're static PDF-era plans — no adaptation, no equipment-change handling.
- Fitbod adapts to equipment best-in-class but wraps it in subscription + opaque scoring.

**GymCoach wedge — "the app that never schedules something you can't do":**
1. Equipment profile is onboarding step 5, not an advanced setting; generation is provably executable against it (SPEC PS-1 AC).
2. Substitution resolver preserves movement pattern + V-taper priority across equipment swaps — dumbbell-row replaces cable-row with the *reason shown*.
3. Load-headroom awareness: progression suggestions respect `max_weight_kg`/`increment_kg` per owned item; when load saturates, rep-range escalation takes over (SPEC §8).
4. Antagonist superset chains tuned for small-space setups (one pair of adjustables + bench: pair pushing/pulling without re-racking time).
5. Bodyweight-load tonnage (`bodyweight_load_pct`) makes progress charts honest for zero-equipment movements — competitors either omit bodyweight progress or fake it.

Positioning line for store listing: *"Every workout executable with what you own. Every number earned."*

## 7. Empty-State Copy References

Patterns observed (paraphrased):
- **Hevy** empty dashboard: friendly one-liner + single primary CTA to log first workout; illustration optional, never blocking.
- **Strong** first launch: seeds a sample session rather than showing emptiness — strong pattern; we adopt the spirit via the "Ready for your first workout? Today: {day}" card (SPEC §7).
- **Boostcamp** program-picker empty/filter-no-match: explains filter state + reset affordance.
- **Fitbod** pre-first-workout: onboarding quiz results preview ("your first program is ready") — arrival promise before effort.
- **JEFIT** onboarding checklist: multi-step progress list; adopted only partially (our onboarding is 7 short steps, one progress bar, no checklist screen).

Our canonical empty-state set lives in SPEC §7; rule: every empty state = situation acknowledgment + exactly one CTA.