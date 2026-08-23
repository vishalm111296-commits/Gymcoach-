## NAVIGATION STATE

**Current state: BROKEN flows, dead ends.**

**CRITICAL navigation issues:**
1. **Duplicate ProgressViewModel** blocks compilation — highest priority
2. **"View Program" navigates to exercise library** — no screen renders saved program days; generated plan is write-only
3. **Start Workout ignores today's program day** — navigates to blank `workout_session` without pre-seeding exercises from today's `ProgramDay`
4. **Enhanced progress experience unreachable** — new chart/heatmap/PR/muscleVolume stack referenced by nothing that renders; dashboard uses legacy VM
5. **Muscle vocabulary fragmentation** — dashboard keys volume by `uppercase()`, generator uses Title Case, VolumeCalculator expects different tokens; any future data join is wrong by construction
6. **Sunday week-boundary bug** — `weekStartMillis()` without pinned first-day-of-week; on en_US, Sunday rolls to *next* Monday → completed-Sunday workouts count as 0 this week
7. **ProfileViewModel bypasses repository** — injects DAO directly; inconsistent architecture; `saveMeasurement` swallows failures

**Required fixes (ordered):**
1. Remove duplicate ProgressViewModel (compile blocker)
2. Wire "Start Workout" to today's `ProgramDay` — pre-seed exercises, navigate to session with workout already configured
3. Create "Program" tab screen that renders saved program days with exercises
4. Fix muscle vocabulary alignment — pick one convention (Title Case or snake_case) and convert all datasets
5. Fix week-boundary — pin first-day-of-week in `weekStartMillis()` or document the convention
6. Make ProfileViewModel use repository layer like all other VMs; display save errors