# Migration Rebase Spec (generated from app/schemas exports)

> Ground truth = app/schemas/*.json exports. Room validates migrated DBs against these.

## 2_to_3: ALTER workout_sets ADD setType

// v3 field def: {'fieldPath': 'setType', 'columnName': 'setType', 'affinity': 'INTEGER', 'notNull': True}

```sql
ALTER TABLE `workout_sets` ADD COLUMN `setType` INTEGER NOT NULL DEFAULT 0
```

## 3_to_4: CREATE tables (missing from v3), matching v4 export exactly

### exercises
```sql
CREATE TABLE IF NOT EXISTS ``exercises`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `equipment` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `secondaryMuscles` TEXT NOT NULL, `instructions` TEXT NOT NULL, `tips` TEXT NOT NULL, `commonMistakes` TEXT NOT NULL, `safetyNotes` TEXT NOT NULL, `recommendedRepRange` TEXT NOT NULL, `recommendedRestTime` TEXT NOT NULL, `estimatedCalories` INTEGER NOT NULL, `category` TEXT NOT NULL, `tags` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `lastViewed` INTEGER NOT NULL, `vtaper_lat` INTEGER NOT NULL DEFAULT 0, `vtaper_lateral_delt` INTEGER NOT NULL DEFAULT 0, `vtaper_upper_chest` INTEGER NOT NULL DEFAULT 0, `vtaper_rear_delt` INTEGER NOT NULL DEFAULT 0, `movement_pattern` TEXT NOT NULL DEFAULT '', `image_url` TEXT, `video_url` TEXT, `animation_url` TEXT, `setup_instructions` TEXT NOT NULL DEFAULT '', `execution_instructions` TEXT NOT NULL DEFAULT '', `breathing_instructions` TEXT NOT NULL DEFAULT '', `tempo_guidance` TEXT NOT NULL DEFAULT '', `beginner_variant_id` INTEGER, `advanced_variant_id` INTEGER)
```

indices: []

### user_profiles
```sql
CREATE TABLE IF NOT EXISTS ``user_profiles`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `goal` TEXT NOT NULL DEFAULT '', `experience` TEXT NOT NULL DEFAULT '', `age` INTEGER NOT NULL DEFAULT 0, `sex` TEXT NOT NULL DEFAULT '', `height_cm` REAL NOT NULL DEFAULT 0.0, `weight_kg` REAL NOT NULL DEFAULT 0.0, `training_days_per_week` INTEGER NOT NULL DEFAULT 4, `session_length_minutes` INTEGER NOT NULL DEFAULT 60, `equipment_type` TEXT NOT NULL DEFAULT 'gym', `preferred_exercises` TEXT NOT NULL DEFAULT '', `exercises_to_avoid` TEXT NOT NULL DEFAULT '', `created_at` INTEGER NOT NULL DEFAULT 0)
```

indices: []

### programs
```sql
CREATE TABLE IF NOT EXISTS ``programs`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL DEFAULT 1, `name` TEXT NOT NULL DEFAULT '', `description` TEXT NOT NULL DEFAULT '', `split_type` TEXT NOT NULL DEFAULT '', `duration_weeks` INTEGER NOT NULL DEFAULT 0, `days_per_week` INTEGER NOT NULL DEFAULT 0, `difficulty` TEXT NOT NULL DEFAULT '', `goal` TEXT NOT NULL DEFAULT '', `is_active` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL)
```

indices: []

### program_days
```sql
CREATE TABLE IF NOT EXISTS ``program_days`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `program_id` INTEGER NOT NULL, `day_number` INTEGER NOT NULL, `name` TEXT NOT NULL DEFAULT '', `focus` TEXT NOT NULL DEFAULT '', `is_rest_day` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`program_id`) REFERENCES `programs`(`id`) ON DELETE CASCADE)
```

indices: [{"name": "index_program_days_program_id", "unique": false, "columnNames": ["program_id"]}]

### program_exercises
```sql
CREATE TABLE IF NOT EXISTS ``program_exercises`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `program_day_id` INTEGER NOT NULL, `exercise_id` INTEGER NOT NULL, `order_index` INTEGER NOT NULL DEFAULT 0, `sets` INTEGER NOT NULL DEFAULT 3, `target_reps` TEXT NOT NULL DEFAULT '', `target_weight_kg` REAL NOT NULL DEFAULT 0.0, `rest_seconds` INTEGER NOT NULL DEFAULT 90, `notes` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`program_day_id`) REFERENCES `program_days`(`id`) ON DELETE CASCADE, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)
```

indices: [{"name": "index_program_exercises_program_day_id", "unique": false, "columnNames": ["program_day_id"]}, {"name": "index_program_exercises_exercise_id", "unique": false, "columnNames": ["exercise_id"]}]

### personal_records
```sql
CREATE TABLE IF NOT EXISTS ``personal_records`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `user_id` INTEGER NOT NULL DEFAULT 1, `weight_kg` REAL NOT NULL DEFAULT 0.0, `reps` INTEGER NOT NULL DEFAULT 0, `one_rep_max_kg` REAL NOT NULL DEFAULT 0.0, `achieved_at` INTEGER NOT NULL, `notes` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)
```

indices: [{"name": "index_personal_records_exercise_id", "unique": false, "columnNames": ["exercise_id"]}]

### body_measurements
```sql
CREATE TABLE IF NOT EXISTS ``body_measurements`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL DEFAULT 1, `recorded_at` INTEGER NOT NULL, `weight_kg` REAL NOT NULL DEFAULT 0.0, `body_fat_pct` REAL, `chest_cm` REAL, `waist_cm` REAL, `hips_cm` REAL, `shoulders_cm` REAL, `left_arm_cm` REAL, `right_arm_cm` REAL, `left_thigh_cm` REAL, `right_thigh_cm` REAL, `left_calf_cm` REAL, `right_calf_cm` REAL, `notes` TEXT NOT NULL)
```

indices: []

### favorite_exercises
```sql
CREATE TABLE IF NOT EXISTS ``favorite_exercises`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `user_id` INTEGER NOT NULL DEFAULT 1, `added_at` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)
```

indices: [{"name": "index_favorite_exercises_exercise_id", "unique": false, "columnNames": ["exercise_id"]}]

### exercise_substitutions
```sql
CREATE TABLE IF NOT EXISTS ``exercise_substitutions`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `original_exercise_id` INTEGER NOT NULL, `substitute_exercise_id` INTEGER NOT NULL, `reason` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`original_exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`substitute_exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)
```

indices: [{"name": "index_exercise_substitutions_original_exercise_id", "unique": false, "columnNames": ["original_exercise_id"]}, {"name": "index_exercise_substitutions_substitute_exercise_id", "unique": false, "columnNames": ["substitute_exercise_id"]}]

### muscles
```sql
CREATE TABLE IF NOT EXISTS ``muscles`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `display_name` TEXT NOT NULL, `parent_muscle_id` INTEGER, `body_region` TEXT NOT NULL DEFAULT '')
```

indices: [{"name": "index_muscles_parent_muscle_id", "unique": false, "columnNames": ["parent_muscle_id"]}]

### equipment
```sql
CREATE TABLE IF NOT EXISTS ``equipment`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `display_name` TEXT NOT NULL, `category` TEXT NOT NULL DEFAULT '')
```

indices: []

### exercise_muscles
```sql
CREATE TABLE IF NOT EXISTS ``exercise_muscles`` (`exercise_id` INTEGER NOT NULL, `muscle_id` INTEGER NOT NULL, `role` TEXT NOT NULL DEFAULT 'primary', PRIMARY KEY(`exercise_id`, `muscle_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`muscle_id`) REFERENCES `muscles`(`id`) ON DELETE CASCADE)
```

indices: [{"name": "index_exercise_muscles_exercise_id", "unique": false, "columnNames": ["exercise_id"]}, {"name": "index_exercise_muscles_muscle_id", "unique": false, "columnNames": ["muscle_id"]}]

### exercise_equipment
```sql
CREATE TABLE IF NOT EXISTS ``exercise_equipment`` (`exercise_id` INTEGER NOT NULL, `equipment_id` INTEGER NOT NULL, `role` TEXT NOT NULL DEFAULT 'required', PRIMARY KEY(`exercise_id`, `equipment_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`equipment_id`) REFERENCES `equipment`(`id`) ON DELETE CASCADE)
```

indices: [{"name": "index_exercise_equipment_exercise_id", "unique": false, "columnNames": ["exercise_id"]}, {"name": "index_exercise_equipment_equipment_id", "unique": false, "columnNames": ["equipment_id"]}]

### exercise_aliases
```sql
CREATE TABLE IF NOT EXISTS ``exercise_aliases`` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `alias` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)
```

indices: [{"name": "index_exercise_aliases_exercise_id", "unique": false, "columnNames": ["exercise_id"]}, {"name": "index_exercise_aliases_alias", "unique": false, "columnNames": ["alias"]}]

// v4 tables: ['body_measurements', 'equipment', 'exercise_aliases', 'exercise_equipment', 'exercise_muscles', 'exercise_substitutions', 'exercises', 'favorite_exercises', 'muscles', 'personal_records', 'program_days', 'program_exercises', 'programs', 'user_profiles', 'workout_exercises', 'workout_sets', 'workouts']

INDEX parity workouts: v3=[] v4=[] -> ADD in 3_4: []

INDEX parity workout_exercises: v3=[('index_workout_exercises_workoutId', ['workoutId']), ('index_workout_exercises_exerciseId', ['exerciseId'])] v4=[('index_workout_exercises_workoutId', ['workoutId']), ('index_workout_exercises_exerciseId', ['exerciseId'])] -> ADD in 3_4: []

INDEX parity workout_sets: v3=[('index_workout_sets_workoutExerciseId', ['workoutExerciseId'])] v4=[('index_workout_sets_workoutExerciseId', ['workoutExerciseId'])] -> ADD in 3_4: []

## 5_to_6: FTS (present in v6, absent in v5)

```sql
CREATE VIRTUAL TABLE IF NOT EXISTS ``exercise_fts`` USING FTS4(`name` TEXT NOT NULL, `description` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `equipment` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `category` TEXT NOT NULL, content=`exercises`);
INSERT INTO exercise_fts(exercise_fts) VALUES('rebuild');
-- triggers exercises_ai/au/ad: COPY VERBATIM from current MIGRATION_6_7 block
```

## 6_to_7: workouts.status (absent v6, present v7)
// {'fieldPath': 'status', 'columnName': 'status', 'affinity': 'TEXT', 'notNull': True, 'defaultValue': "'NOT_STARTED'"}

```sql
ALTER TABLE `workouts` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'NOT_STARTED';
UPDATE workouts SET status = 'COMPLETED' WHERE completed = 1;
UPDATE workouts SET status = 'ACTIVE' WHERE completed = 0 AND id IN (SELECT workoutId FROM workout_exercises);
UPDATE workouts SET status = 'ABANDONED' WHERE completed = 0 AND status = 'NOT_STARTED';
```

## 7_to_8: NO-OP (exports 7 == 8)

## 8_to_9: program_days.target_muscles + vtaper backfill
// {'fieldPath': 'targetMuscles', 'columnName': 'target_muscles', 'affinity': 'TEXT', 'notNull': True}

```sql
ALTER TABLE `program_days` ADD COLUMN `target_muscles` TEXT NOT NULL DEFAULT '';
UPDATE program_days SET target_muscles = focus;
```
// KEEP the existing vtaper UPDATE statements in current MIGRATION_8_9 verbatim (Bench Press ... Calf Raise).

## 9_to_10: readiness (current MIGRATION_9_10 verbatim)

## 10_to_11 add preferred_schedule: // {'fieldPath': 'preferredSchedule', 'columnName': 'preferred_schedule', 'affinity': 'TEXT', 'notNull': True, 'defaultValue': "''"}
```sql
ALTER TABLE `user_profiles` ADD COLUMN `preferred_schedule` TEXT NOT NULL DEFAULT ''
```

## 10_to_11 add limitations_preferences: // {'fieldPath': 'limitationsPreferences', 'columnName': 'limitations_preferences', 'affinity': 'TEXT', 'notNull': True, 'defaultValue': "''"}
```sql
ALTER TABLE `user_profiles` ADD COLUMN `limitations_preferences` TEXT NOT NULL DEFAULT ''
```

## 10_to_11 rebuild program_days — v11 export has NO focus; name/target_muscles WITHOUT DEFAULT; index_program_days_program_id KEPT

```sql
CREATE TABLE IF NOT EXISTS `program_days_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `program_id` INTEGER NOT NULL, `day_number` INTEGER NOT NULL, `name` TEXT NOT NULL, `target_muscles` TEXT NOT NULL, `is_rest_day` INTEGER NOT NULL, FOREIGN KEY(`program_id`) REFERENCES `programs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
```


INSERT INTO `program_days_new` (id, program_id, day_number, name, target_muscles, is_rest_day)
SELECT id, program_id, day_number, name, target_muscles, is_rest_day FROM `program_days`;
DROP TABLE `program_days`;
ALTER TABLE `program_days_new` RENAME TO `program_days`;
-- v11 export KEEPS this index (columnNames [program_id]):
CREATE INDEX IF NOT EXISTS `index_program_days_program_id` ON `program_days`(`program_id`);


## 3_4 FK-safe creation order

exercises, muscles, equipment, programs, program_days, program_exercises, user_profiles, body_measurements, favorite_exercises, personal_records, exercise_aliases, exercise_equipment, exercise_muscles, exercise_substitutions