## ANALYTICS

**Current state: CALCULATIONS NOT WIRING TO DATA.**

**Volume, reps, sets, duration analytics:**
- **VolumeCalculator** has zero production callers — Progress UI data flow bypasses it entirely on both branches
- **PRDetector** has no production caller — celebrated by 18 unit tests testing dead logic
- **Weekly volume** — `avgWeekly` (per ISO-week credits) computed then discarded; `weeklySets` derived from total sets across entire input, not weekly
- **PR detection** — uses `Instant.now()` for PR dates instead of workout date; bodyweight proxy (`reps*1.5`) cross-compared against Epley 1RM under same `ESTIMATED_1RM` type; first-ever trivial load always a PR
- **Weekly trends** — `calculateWeeklyTrend` requires ≥ 2 data points; returns 0.0 if prev=0; `((curr - prev) / prev) * 100` — crashes if prev=0.0 (division by zero in code, though guarded)
- **Muscle volume** — `TrainingBalance.vol(muscle)` returns `MuscleVolume` with `weeklySets`, `directSets`, `indirectSets`, `status`; but `weeklySets` is not actually weekly; `status` uses wrong counting basis
- **1RM estimation** — Epley formula verified (100×10→133.33), 12-rep cap verified, invalid input guarded; but `PersonalRecord` test model ≠ `PersonalRecordEntity` (DB row has separate `reps`/`estimated_1rm` columns; mapping layer untested)
- **Bodyweight handling** — not explicitly tested; `weight == 0.0` path in PRDetector guarded; `reps×1.5` proxy untested
- **Unit conversion** — no evidence of inch/cm conversion errors; all asset data appears consistent
- **Time-zone errors** — `isoWeekKey` uses `Calendar.WEEK_OF_YEAR` + locale-default `YEAR`; Dec 29–31 can be week 1 of next year while `YEAR` returns old year → bucket collision/duplication
- **Week-boundary errors** — `weekStartMillis()` sets `DAY_OF_WEEK, MONDAY` without pinned first-day-of-week/locale; Sunday counts as 0 this week
- **Month-boundary errors** — no month-aggregation code found; would rely on same `Calendar` patterns

**Verification status:** Analytics calculations exist but are NOT wired to stored schema or UI. No end-to-end verification path.