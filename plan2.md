1. **Fix `dumbbell_shoulder_press` animation**
   - Provide FRONT perspective joints for full body (head, neck, pelvis, shoulder, elbow, wrist left & right). Done in `exercise_animations.json`.
2. **Complete Tier 2 Animations**
   - Provided anatomically realistic stickman skeletons for `incline_dumbbell_press`, `hammer_curl`, `dumbbell_romanian_deadlift`, `face_pull`, `standing_calf_raise`. Done.
3. **Rigid-Body Bone Length Invariant Restoration**
   - Adjusted `overhead_press`, `bench_press`, `biceps_curl`, and others to ensure bone lengths do not distort > 15%. Done.
4. **Kinematic velocity continuity**
   - Replaced piece-wise cubic smoothstep in `ExerciseAnimationModels.kt` with a Catmull-Rom spline implementation for velocity continuity. Done.
5. **Phase badge semantics**
   - Changed "BOTTOM" label to "PEAK / LOCKOUT" with an energetic accent color (`NeonCyan`) for concentric-first exercises (`pull_up`, `overhead_press`, `biceps_curl`, `lat_pulldown`) in `ExerciseAnimationPlayer.kt`. Done.
6. **Create MuscleHighlightCanvas.kt**
   - Created `MuscleHighlightCanvas.kt` to draw body outlines and highlight muscle groups. Done.
7. **Integrate into ExerciseDetailScreen.kt**
   - Replaced generic dumbbell icon with `MuscleHighlightCanvas` in `ExerciseDetailScreen.kt`. Done.
8. **Add Unit Tests**
   - Added tests in `AnimationSystemTest.kt` to verify blank canvas prevention, realistic bone lengths, and correct angle specs. Done.
9. **Pre-commit instructions**
   - Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.
