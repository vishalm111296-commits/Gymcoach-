## MIGRATION REPAIR — MANDATORY

**MIGRATION_4_5 is critically broken.** The entity/DB alignment matrix shows 6 tables with mismatches. Cannot simply edit entities and hope — must rewrite migration DDL to match entities, OR rewrite entities to match migration DDL (but this risks data loss).

**Recommended strategy:** Rewrite MIGRATION_4_5 DDL to match the current entity schemas. This preserves existing user data. Key fixes needed:

1. `program_days`: Replace `target_muscles` column with `focus` (TEXT) and `is_rest_day` (INTEGER DEFAULT 0) columns; add INDEX on `program_id`
2. `body_measurements`: Replace `date` → `recorded_at`; replace `arm_cm` → `left_arm_cm`, `right_arm_cm`, `left_thigh_cm`, `right_thigh_cm`, `left_calf_cm`, `right_calf_cm`; add `user_id`, `body_fat_pct`, `chest_cm`, `waist_cm`, `hips_cm`, `shoulders_cm`; remove `photo_url` (or add to entity)
3. `programs`: Add `user_id`, `split_type`, `duration_weeks`, `days_per_week`, `difficulty` columns; replace `frequency` with `days_per_week`
4. `program_exercises`: Add `sets` (INTEGER), `target_reps` (TEXT), `target_weight_kg` (REAL), `notes` (TEXT); add indexes; replace `target_sets`/`target_reps_min-max`/`target_rpe` with proper columns
5. `personal_records`: Add `user_id` column; replace `estimated_1rm` → `one_rep_max_kg`; replace `volume` (remove or keep); replace `workout_id` FK with proper nullable; add `created_at` → `achieved_at`
6. `favorite_exercises`: Add `user_id` column

**Also needed:** Add `room.schemaLocation` KSP arg to `build.gradle.kts` so schema snapshots can be exported and validated.