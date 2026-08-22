# Fitness App Product Benchmark

Research of 7 modern strength-training applications. Findings focus on
**product principles / UX patterns**, not visual/UI copying. Sources are linked
per fact (official docs, store pages, long-form reviews).

---

## 1. Hevy

Source: <https://www.hevyapp.com/hevy-tutorial/> , <https://www.hevyapp.com/features/how-to-calculate-rpe/> , <https://www.hevyapp.com/> , <https://prpath.app/blog/hevy-app-review-2026.html> , <https://trysoma.app/blog/hevy-app-review/> , <https://himanshuprodesign.medium.com/new-user-onboarding-ux-hevys-activity-tracker-teardown-7b796b912636>

### Onboarding flow
- App lands users on a **routine/library-first flow** rather than a blank log.
  "The easiest way to start is to create a routine." Users can also browse
  Hevy's routine library filtered by experience, goals, equipment.
  (source: tutorial)
- Onboarding is **disparate across web vs mobile**; one teardown notes the web
  landing page leads into a mobile-centric journey with no value-first step
  ("AHA moment" should come *before* credentials). (source: Medium teardown)
- **Deliberate non-prescriptive**: "We don't tell you how or when to work out
  — that's up to you." (source: tutorial)

### Workout execution UX
- **Previous-performance display** is the primary progression aid: the left of
  each exercise shows your last performance on the same set. (tutorial)
- **One-tap set completion** triggers the auto rest timer (no separate timer app).
  Sets are marked complete with a checkmark; RPE/RIR optional (6–10 scale).
  (tutorial, RPE page)
- Set-type tagging per set: warm-up, drop set, to-failure, normal (default).
  Supersets supported; per-exercise rest timers; custom exercise notes.

### Exercise library
- Large, searchable library; **animations + "How to" demonstrational tab**
  with step-by-step form guidance. (tutorial)
- **Custom exercises** supported with image, name, equipment, muscle targets,
  type (weight/reps). Free tier limited to 7 custom exercises; Pro = unlimited.

### Progression logic
- **Hevy Trainer** (Feb 18 2026, Pro-only): algorithmic program generator with
  auto weight progression from session to session based on last sets. NOT a
  chat coach — runs quietly in background. (PRPath)
- **HevyGPT**: ChatGPT integration to build plans from a prompt.
- **RPE is a logged field only** — "doesn't do anything with the data": no
  analytics, no adaptation, no coach feedback. (Soma review)
- Classic Hevy: **no adaptive programming**; tracks what you did, never tells
  you what you *should* do. (Soma review)

### Progress tracking
- Per-exercise: heaviest weight, projected 1RM, best set/session volume, full
  set history graph. Automatic PR detection + live PR notifications. (tutorial, PRPath)
- **Weekly stats**: volume, set count per muscle group, reps, workout calendar.
  Body metrics + weight tracked; progress photo uploads. (tutorial)

### Program management
- **Templates/routines** as reusable workout plans; cycle through templates.
- Free: 4 routines, 3 months history, 7 custom exercises.
- Pro: unlimited routines, full history, Hevy Trainer, HevyGPT, CSV export,
  warmup sets, custom folders, notes/attachments.
- **Community routines** browsable; **routine sharing via link**; coaches share
  programs with clients. (tutorial)

### Empty states
- New users guided to **library of starter routines** rather than a blank slate.
- Critique: onboarding doesn't deliver an "AHA moment" up front; value is gated
  behind creating a routine. (Medium teardown)

### Key differentiators
- **Social layer baked in**: follow friends, home feed of recent workouts,
  discovery feed, likes/comments, profile privacy toggle — lightweight
  accountability. (tutorial)
- Cheapest Pro in category ($2.99/mo, $23.99/yr, $74.99 lifetime) now bundling
  algorithmic programming. (PRPath)
- **Intentionally NOT an AI coach**: app "tells you what happened"; no
  "should I deload?" guidance. (Soma review)

---

## 2. Strong

Source: <https://www.strong.app/> , <https://www.corahealth.app/compare/strong> , <https://www.fitness-tracking.com/reviews/strong/> , <https://lnreview.com/reviews/strong-app-review-2026> , <https://prpath.app/blog/strong-app-review-2026.html>

### Onboarding flow
- **No prescriptive onboarding questions** beyond building your first routine.
  Minimal friction; "open app → log workout → close" ethos.
- Deliberately **no program generation** — you start with a template-free or
  imported routine.

### Workout execution UX
- **Ultra-minimal logging**: ~10s/set. Select exercise, enter weight+reps,
  tap to complete set. (PRPath, LNreview)
- Set completion marks set done; **automatic rest timer** starts immediately
  with notifications when rest ends. (LNreview)
- Auto-calculated **warm-up sets** from working weight.
- **Apple Watch / Wear OS** as first-class: log sets, control timers, track
  entire workout from wrist without phone (v6.2 rebuild, March 2026). (PRPath)
- One-tap set marking: a single tap = completed at target reps; additional taps
  mark failure (mirror of StrongLifts set-circle semantics).

### Exercise library
- **Hundreds of exercises** with animations demonstrating form.
- Custom exercises supported.

### Progression logic
- **None built in.** Previous performance shown next to each exercise so the
  lifter self-regulates progressive overload. (PRPath)
- "Strong is a logger, not a programmer." By-design absence of adaptation.

### Progress tracking
- Per-exercise **PR history** surfaced real-time mid-session; all-time best
  graph with volume trends over months. (Cora)
- **Muscle heat map**, volume per muscle group, total load, 1RM estimates,
  progression graphs. **CSV export** any time (privacy-first). (strong.app)
- Body part measurements, body fat % tracking. (LNreview)

### Program management
- **Routine model**: templates per workout (Push/Pull/Legs, 5x5, etc.).
  Sessions start from a template. Free caps at **3 custom routines**; Pro =
  unlimited.
- No pre-built coaching programs; you supply the program.

### Empty states
- Empty = start a blank routine or pick an exercise from the library.
- No guided "aha moment"; relies on user already knowing a program.

### Key differentiators
- **Native iOS aesthetic**, distraction-free, offline-first, rock-solid
  reliability (5M users, 2014 launch). (Cora, PRPath)
- **Deliberately no AI / no social / no nutrition** — scope minimalism is the
  point. (PRPath)
- Apple Watch + Wear OS integration ahead of newer apps.

---

## 3. Fitbod

Source: <https://fitbod.me/blog/fitbod-algorithm/> , <https://fitbod.me/blog/muscle-recovery/> , <https://lifehacker.com/health/fitbod-app-review> , <https://www.indiehackers.com/post/fitbod-app-review-2026-honest-take-after-real-testing-45d5f07a1b> , <https://aitoolsbakery.com/blog/fitbod-review/>

### Onboarding flow
- Asks **goal, fitness level, available equipment, recent muscle use**. (algorithm blog)
- Generates a first workout immediately from that baseline.

### Workout execution UX
- Exercises come pre-programmed for the session; user executes sets, inputs
  weight/sets/reps.
- **RiR (Reps in Reserve)** logged after each set ("how many reps left in tank").
- **Max Effort Days**: flagged sessions instruct you to do an AMRAP final set
  to feed the 1RM model. (algorithm blog)

### Exercise library
- **800+ exercises**, each with exercise-select scoring.
- No massive public library curation emphasis; focus is on **recovery-compatible**
  selection rather than catalog breadth.

### Progression logic
- **Two engines**: Exercise Selector (what) + Capability Recommender (how much).
  (algorithm blog)
- **Muscle recovery % (0–100) per group** drives selection; 48–72h recovery
  principle (Schoenfeld 2010); fully recovered after 7 days (algorithm function).
  Heat map in Recovery tab; manual adjustment possible. (muscle-recovery blog)
- **Dynamic 1RM** via Epley-style estimate from logged sets, updated every
  session. (algorithm blog)
- **Sets/reps by goal**: Strength = 1–6 reps @85–100% 1RM, 3–5 min rest
  (Grgic 2018); Hypertrophy = 6–12 reps, some 1–5 / 15–25, 10–20 sets/muscle/
  week (Schoenfeld 2017). Cycling across loading zones for long-term overload.
- **Feedback loop**: swaps/favorites/removals retrain the selector; split
  compatibility enforced (e.g., push day excludes leg exercises). (algorithm)
- **Integrations**: Apple Health, Fitbit, Strava feed cardio/activity into
  recovery computation. (algorithm + recovery blogs)
- Critique: personalisation needs **10–15 workouts** before quality; exercise
  rotation **undermines movement-specific progression** (cannot systematically
  add 2.5kg to deadlift if rotated away). (IndieHackers, aitoolsbakery)

### Progress tracking
- **Recovery heat map** (0–100% per muscle) — unique in category.
- 1RM trends, e1RM, volume/intensity charts, weekly summaries.

### Program management
- **None owned by user**: Fitbod generates each session; no saved programs to
  author. (aitoolsbakery)
- 3-workout free trial (then $15.99/mo / $95.99/yr). (IndieHackers)

### Empty states
- Blank → onboad → "get a sensible session without thinking" is the value hook.
- No historical data → algorithm uses baseline questionnaire.

### Key differentiators
- **Recovery-driven, non-linear** selection vs. linear program apps.
- **Equipment-flexible**: toggles for barbell/dumbbell/machine/home/gym;
  session auto-adapts to what's available (hotel gym vs full rack). (IndieHackers)
- "AI" is workout-level pre-session generation, not real-time set-by-set. (IndieHackers)

---

## 4. JEFIT

Source: <https://apps-review.tech/jefit-workout-plan-gym-tracker-review-ios-turn-your-training-into-data-driven-gains/> , <https://etechshout.com/jefit-app-review/> , <https://www.gymbird.com/fitness-apps/jefit-app-review> , <https://dr-muscle.com/jefit-workout-app-review/> , <https://www.finderslist.com/fitness-apps/tools/jefit>

### Onboarding flow
- Create profile → choose goal (cutting/bulking/maintenance) → plan training
  days. Quick ~10-min path to first plan from library. (GymBird)
- Offers **custom plan creation or app-suggested plans** — user picks a path.

### Workout execution UX
- During session: add sets, enter weight+reps, swap exercises, log after each
  set; **detailed rest timer**, interval training support. (Jindal)
- **Activity points system** removes ads — gamified economy.

### Exercise library
- **1,400+ exercises**, HD video + animated demonstrations. (Jindal, GymBird)
- **Rich metadata per exercise**: type, mechanics, equipment, targeted muscle
  groups, step-by-step instructions, difficulty rating. (GymBird)
- Free: ~1,400 moves + guided instructions. (GymBird)

### Progression logic
- **Automatic 1RM estimation** + tracking per lift; progressive overload tools
  to increase load/volume week-over-week. (Jindal)
- Community/Elite programs have **wide quality variance** (anyone can upload);
  no quality control → misleading programs possible. (dr-muscle, etechshout)

### Progress tracking
- **Data-driven**: sets/reps/weights, 1RM trends, weekly performance,
  body composition, progress photos, body measurements. (Jindal, Etechshout)
- Training insights, weekly summaries, graphs. Apple Health sync. (Etechshout)

### Program management
- **Library of 2,000+ programs** (free + Elite); free tier has routines but
  **ad-supported**. (Etechshout)
- Build custom routines: choose exercises, set targets, adjust on the fly.
  Schedule workouts with reminders. (Jindal)
- Elite ($12.99/mo, $69.99/yr): deeper analytics, ad-free, video demos,
  advanced tools. (Etechshout)
- Critique: **too many programs → program-hopping**; quality varies; recent
  updates made navigation slower/less reliable. (Etechshout, dr-muscle)

### Empty states
- Library-first; beginners can pick a routine from the plan database.
- Critique: "sheer volume can feel overwhelming"; 1,300 near-identical moves
  cause **analysis paralysis**. (dr-muscle)

### Key differentiators
- **Massive public database** (1,400+ exercises) + community program sharing.
- **Activity points economy** for ad removal (unique monetisation angle).
- Free version is genuinely functional; Elite targets analytics depth.

---

## 5. Boostcamp

Source: <https://www.boostcamp.app/features> , <https://www.boostcamp.app/> , <https://www.barbend.com/boostcamp-review/> , <https://www.garagegymreviews.com/boostcamp-review> , <https://www.boostcamp.app/coaches/cody-lefever/gzcl-program-gzclp> , <https://www.boostcamp.app/coaches/cody-lefever/jacked-tan-2-0>

### Onboarding flow
- Enter the app → **choose a program** from 11,000+ (filter by goal, experience,
  days/week) OR **AI Program Builder** from a short questionnaire. (barbend)
- Specific program onboarding: GZCLP/J&T2.0 uses **Training Max (TM) input**
  (~90% of 1RM); "be conservative," app shows prior training history to aid RM
  selection with RPE 8–9 guidance. (J&T, GZCLP pages)

### Workout execution UX
- Pick a program, execute the prescribed work, log sets inline.
- **Mid-workout exercise alternatives** (swap to available equipment).
- **Plate calculator**, rest timers, auto rest timer, AMRAP/fail set tagging.
- **Offline mode** for logging + tools without internet (free tier). (barbend)

### Exercise library
- ~500 exercises searchable (per creator tools page); each with video guidance.
- Custom exercise creation supported.

### Progression logic
- **Auto-progression**: working weights/targets update between sessions based
  on logged performance — "tracks all your sets and weights automatically, so
  you always know exactly what to do next." (GZCLP, J&T pages)
- RPE/RIR logging; **RPE-based adjustment option** so the app adjusts weights
  according to effort, not just completion. (GarageGymReviews, features)
- GZCLP mechanics: T1 5×3, T2 3×10, T3 3×15, last-set AMRAP; progression from
  AMRAP reps vs. volume base; T3 adds weight at AMRAP ≥25. (LiftVault)

### Progress tracking
- **PRs + projected 1RM**, max weight, max reps, lifetime bests. (features)
- Weekly Sunday reports, year-end Wrapped recap — free. (features)
- Pro: **Strength Score** (0–100 composite across big lifts), **per-muscle
  volume heatmap**, volume/intensity/1RM trends. (features)

### Program management
- **11,000+ programs**: 130+ coach-designed (Eric Helms, Cody Lefever, Jim
  Wendler, Alex Bromley, Geoffrey Verity Schofield…) + 10,000+ community.
  All free; filters by level/goal/days. (barbend, GGR)
- **Custom Program Builder**: multi-week mesocycles, warmups, supersets/dropsets,
  no cap on free accounts. Fork any program; publish/share via link. (features,
  creator page)
- Pro: 20+ exclusive coach programs, personalized program builder (questionnaire
  → starter periodization plan), advanced analytics. ($14.99/mo, $59.99/yr)
- Note: "Boostcamp has no community or individual coaching" — only program review
  text & sharing. (GGR)

### Empty states
- Solved by **massive curated library** rather than guidance; new lifters pick a
  program by filter. Critique: beginners may struggle without demo videos/
  accountability. (barbend)

### Key differentiators
- **Program library size** (11,000+) is the USP; tracking is the equaliser.
- **Fully functional free core** (tracker + full program library), vs. most
  competitors gating programs behind pay. (GGR)
- Strong **coach-partner model**: official programs authored by named pro
  coaches, hosted free inside the app.

---

## 6. StrongLifts

Source: <https://support.stronglifts.com/article/71-progression> , <https://stronglifts.com/stronglifts-5x5/progress/> , <https://stronglifts.com/stronglifts-5x5/workout-program/> , <https://play.google.com/store/apps/details?id=com.stronglifts.app>

### Onboarding flow
- Enter **gender, weight, strength level** → app **calculates starting weights**
  for the 5 compound lifts and sets increments. (stronglifts.com)
- Program is prescribed: 3x/week, **Workout A** (squat/bench/row) ↔
  **Workout B** (squat/press/deadlift), auto-alternated. (workout-program)

### Workout execution UX
- **Set circles**: one tap = completed set at target reps (5×5). Multiple taps
  on the circle = mark as **failed** set. (support article)
- **Auto rest timer** + vibration when time elapses; **plate calculator**,
  warmup calculator. (Play store, pumpx review)
- Form videos + instructional content for the 5 lifts.

### Exercise library
- **Extremely narrow**: 5 core barbell movements only (+ optional assistance
  via Power Pack). Library = form guides, not a catalog.

### Progression logic (rules engine)
- **Core rule**: completed all sets → +increment (default 5lb) next session on
  that exercise; failed any set → **repeat the weight** (no progression).
  (support article, stronglifts.com)
- **Increment config**: amount AND frequency are independently set — default
  5lb every workout, but user can choose e.g. 2.5lb every 3 workouts, or slower
  linear cadence. (support article)
- **Per-exercise increment sizing**: big lifts (squat/dead) 5–10lb; small lifts
  (bench/press/row) 2.5–5lb; women default to smaller increments. (stronglifts.com)
- **Deload**: fail 3 consecutive sessions on an exercise → weight drops 10%
  (configurable). After ≥1 week off → "Welcome Back" prompt with deload
  slider before resuming. (support article)
- Deadlift 10lb increments taper to 5lb once progress slows; app adjusts.

### Progress tracking
- **Progress graphs** of strength gains over weeks/months per lift. (Play store)
- Linear 5×5 visualization of weekly weight increase. (stronglifts.com)

### Program management
- **One program, fully automated**: A/B alternation, weight math, deload, warmup.
  No program selection. Power Pack ($9.99/mo or $59.99/yr) adds assistance
  exercises, advanced analytics, warmup sets.
- Subscription now required to use (7-day trial); **controversy over legacy
  lifetime purchases being paywalled** (Play store user complaint).

### Empty states
- Solved structurally: **everyone starts with empty bar (45lb)** and builds up.
  No decision paralysis possible — program prescribes the path. (stronglifts.com)

### Key differentiators
- **Single-program dogma**: 5×5 linear progression with a transparent rule
  engine (complete = add; fail = repeat; 3 fails = deload).
- **Start-with-nothing onboarding**: gender/level → starting weights auto;
  removes all early decisions.
- **Plate/tap failure semantics** baked into the set UI itself.

---

## 7. GZCL (Method) apps

Source: <https://apps.apple.com/us/app/gzcl-method-workout-logger/id1517032809> , <https://play.google.com/store/apps/details?id=co.braindead.gzcl> , <https://liftvault.com/programs/powerlifting/gzclp-program-spreadsheets/> , <https://www.boostcamp.app/coaches/cody-lefever/gzcl-program-gzclp> , <https://www.boostcamp.app/coaches/cody-lefever/jacked-tan-2-0>

### Onboarding flow
- Dedicated third-party **GZCL Method Workout Logger**: onboard by selecting
  which **preloaded program** to run. (App Store / Play)
- **Official path**: GZCLP & J&T2.0 now live inside **Boostcamp** (Cody Lefever
  partnership) → onboarding = input **Training Max (~90% 1RM)** for each main
  lift, be conservative, consult prior history for RM targets at RPE 8–9.

### Workout execution UX
- Prescribed tier-based session (T1/T2/T3) loads auto. Log sets including
  warmup/failure/AMRAP tagging; **rest timer per exercise**; notes per set/exercise.
- J&T2.0: work up to a self-selected **Rep Max (RM)** with 1–2 reps in reserve,
  then app auto-calculates %TM for back-off sets; optional AMRAP final set.

### Exercise library
- Focused on the **GZCL movement list**; not a 1,400-piece catalog.
- Exercise images/demos included per lift.

### Progression logic
- **Three-tier AMRAP-driven progression** (LiftVault):
  - T1: 5×3 (last set AMRAP); T2: 3×10 (AMRAP); T3: 3×15 (AMRAP).
  - Progression keyed off **volume base (sets × reps) vs AMRAP achieved**.
  - T3 adds weight when AMRAP ≥25; T1/T2 shift rep schemes when targets missed.
  - Spreadsheet auto-computes; app does the math.
- Boostcamp implementation: **auto-progress weights/targets between sessions**
  from logged performance. (GZCLP/J&T pages)

### Progress tracking
- Per-session lift summary; **volume, intensity, 1RM trends**. (Boostcamp GZCLP)
- Lifetime PR tracking within the GZCL framework.

### Program management
- **Preloaded canonical GZCL programs**: GZCLP, J&T2.0, UHF 5W/9W, VDIP, Rippler
  (both the dedicated app and inside Boostcamp). (App Store/Play, LiftVault)
- Custom tiers: swap lifts within tiers, but keep tier structure.

### Empty states
- New user → pick a program, input TM, start. Recovery/back-off logic is
  **rules-based, not data-dependent** — needs no history to begin.

### Key differentiators
- **Specialised method**: the *only* apps built around the GZCL tier/AMRAP
  progression model rather than generic logging.
- **AMRAP-as-progression-gauge** (not failure as end-state): rep-outs on the
  final set *drive* the next load decision.

---

## Synthesis: Common Patterns & Best Practices

### Universal product principles
1. **Core loop invariant** across all apps: open → see *last performance* →
   log set (single tap/confirmation) → auto rest timer → finish → summary.
   Apps that remove friction at the set-mark step (Strong one-tap, Hevy checkmark,
   StrongLifts set-circles) win the gym experience. (sources: Hevy tutorial,
   PRPath Strong, StrongLifts support)

2. **Previous-performance display is the #1 progressive-overload driver.**
   Every app surfaces the prior set/workout weight+reps adjacent to the input.
   This alone removes "what did I lift last time?" friction.

3. **Progression lives on a spectrum**, and apps pick a point rather than doing
   all points:
   - **None** — lifter self-regulates (Strong, Hevy classic).
   - **Rule-based linear** — complete=add, fail=repeat, 3 fails=deload
     (StrongLifts 5×5; GZCL tier progression).
   - **Coach-program auto-progress** — weights adjust by logged reps/%TM
     (Boostcamp across programs).
   - **Recovery-aware algorithmic** — selection + load by muscle fatigue +
     RiR/1RM model (Fitbod, Hevy Trainer).
   - Trend 2026: linear-rule apps (StrongLifts) keep adding *auto-prog* layers,
     and loggers (Strong) are explicitly choosing NOT to add AI.

4. **Auto-progression is conditional.** The consistent trigger pattern: *if all
   prescribed work was completed → increase; otherwise hold/repeat.* StrongLifts
   formalises it as complete=add / fail=repeat / 3-fails=deload; Boostcamp
   adjusts from AMRAP/RPE; Fitbod from RiR vs target. (sources: StrongLifts
   support, Boostcamp GZCLP, Fitbod algorithm)

5. **Failure has a defined home.** Either a set is "completed" (tapped once) vs
   "failed" (extra tap / RiR=0 / missed target reps). Apps that collapse
   completion+failure into one control (StrongLifts circles, Strong taps) reduce
   decision fatigue mid-set.

6. **Rest timer is non-optional baseline.** Every app includes one; StrongLifts
   adds vibration, Strong ties it to Apple Watch, Hevy per-exercise, Boostcamp
   plate calculator. It's table-stakes for gym UX.

7. **Deload is treated as state-detection.** StrongLifts auto-prompts after 3
   consecutive fails (configurable %) OR after a week off ("Welcome Back").
   Boostcamp uses RPE; Fitbod uses accumulated recovery. Recovery-aware deload
   beats static "every 4 weeks" deloads. (StrongLifts support, Fitbod)

### Onboarding best practices
8. **Decide prescriptiveness vs freedom.** StrongLifts removes all choice (start
   at empty bar, follow the one rule). Hevy/Stax apps offer a **library-first**
   fallback so empty-state = pick a program, not write one. The failure of
   overly-free onboarding is "blank canvas paralysis"; the failure of
   over-prescriptive onboarding is "I didn't sign up to be told what to do."

9. **Collect only what drives the first session.** StrongLifts: gender + weight
   + level → starting weights. Fitbod: goal + level + equipment + recent use →
   first workout. Apps that ask for body metrics/fitness history *without*
   feeding them into week-one value lose users in onboarding.

10. **Value before credentials.** Critique of Hevy's web→mobile handoff:
    "give users value first, then ask for credentials; the AHA moment should
    come ASAP." Library-first or "start at empty bar" beats profile-first.

### Library / content patterns
11. **Breadth without curation causes paralysis.** JEFIT's 1,400+ moves and
    2,000+ community programs → "analysis paralysis" + program-hopping.
    Contrast Boostcamp's *named coach* programs (Eric Helms, Wendler) + filtering
    by level/goal — big catalog + **curated entry points**.

12. **Metadata > count** for exercise discovery. GymBird notes JEFIT shows type,
    mechanics, equipment, muscle targets, difficulty per move — richer
    metadata lets users filter to the ~50 moves they'll actually use.

13. **Video + form guidance is a hygiene factor now**, not a differentiator.
    Strong, Hevy, JEFIT, Boostcamp all ship demo/animations. The differentiator
    is whether form guidance ties to progression (Fitbod RiR coaching,
    StrongLifts start-light philosophy).

### Tracking & metrics
14. **Emphasised metrics converge**: volume, (e)1RM, PRs, session streaks/
    calendar, muscle-group volume split. Fitbod adds a **recovery heatmap**;
    Boostcamp a **Strength Score composite**; Strong the **PR real-time flash**.

15. **Export = trust.** Strong's CSV-any-time, Hevy's Pro CSV export,
    Boostcamp weekly reports — data-exits reduce lock-in anxiety and are
    consistently flagged as table-stakes by reviewers.

16. **Frequency metrics drive behaviour.** Weekly volume, sets-per-muscle,
    calendar streak — shown because they're the leading indicators of consistency
    (the actual leading indicator of results).

### Program management
17. **Templates over authoring for most users.** Strong (routines), Hevy
    (templates), Boostcamp (fork-existing) all default to *start from a
    template*. Only Boostcamp opens full authoring to free users with no cap;
    Strong/Hevy gate template counts.

18. **Program sharing is polarised.** Hevy: lightweight social feed + shared
    routines (coach→client via link). Boostcamp: program link sharing + reviews
    only, **no social feed**. Strong: none. The axis is *social motivation* vs
    *distraction-free logging* — successful apps pick one and commit.

19. **Free tiers are functional gates, not teasers.** Functional ceiling
    identified per app: Hevy (4 routines / 3 months history / 7 custom ex);
    Strong (~3 routines then paywall, iOS); Fitbod (3 workouts); Boostcamp
    (full library + tracker; Pro = analytics); StrongLifts (subscription-gated,
    7-day trial); JEFIT (ad-supported); GZCL Logger (full programs free,
    Boostcamp-hosted GZCLP free).

### Differentiators worth emulating
20. **Algorithmic ≠ conversational.** Hevy Trainer (Feb 2026) and Boostcamp's
    auto-progression are *passive background rules*; PRPath's Atlas is a chat
    coach. The market is splitting into "hands-off auto-prog" vs "chat coach" —
    products that try to be both dilute both.

21. **Specialisation wins on depth.** GZCL Logger (only GZCL tier model),
    StrongLifts (only 5×5), Fitbod (only recovery-aware rotation) each beat
    generalists *within their narrow domain* because the progression logic is
    faithful to the methodology's failure/RIR semantics.

22. **Coach-partner content as a moat.** Boostcamp's differentiator is hosting
    named, respected coaches' (Wendler, Helms, Lefever) complete programs free
    inside the app — turning the product from "a tracker" into "the place my
    program lives."

### Explicit gaps (anti-patterns to avoid)
- **AI that doesn't close the loop**: Hevy's RPE field "doesn't do anything
  with the data" — logging effort without acting on it is busywork, not a
  feature. (Soma review)
- **Un-curated public program pools**: JEFIT's "anyone can upload" → critical
  programming errors, program-hopping. (dr-muscle)
- **Progression that breaks the core lift**: Fitbod rotating deadlift variants
  prevents systematic 2.5 kg/deadlift addition → not a bug for general fitness,
  a hard blocker for strength-specific goals. (aitoolsbakery)
- **Mobile/web parity neglected**: Hevy's disparate onboarding across devices
  wastes the web entry point. (Medium teardown)
