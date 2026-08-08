# Entity Relationship Diagram (Textual)

## Core Entities
- **User**: {id, createdAt, preferences}
- **Exercise**: {id, name, description, muscleGroup, equipment, difficulty}
- **Workout**: {id, date, startTime, endTime, duration, notes, completed}
- **WorkoutExercise**: {id, workoutId, exerciseId, orderIndex}
- **WorkoutSet**: {id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed}

## Measurement Engine
- **MeasurementRecord**: {id, userId, type, value, timestamp}
  - Type: enum {SHOULDER, WAIST, WEIGHT, BODY_FAT, CHEST, THIGH, ARM, OTHER}

## Goal Engine
- **Goal**: {id, userId, type, targetValue, startDate, endDate, status, createdAt}
  - Type: enum {V_TAPER_RATIO, WEIGHT_LOSS, MUSCLE_GAIN, STRENGTH_GAIN, ENDURANCE}

## Challenge Engine
- **Challenge**: {id, name, description, category, criteria, startDate, endDate}
- **ChallengeProgress**: {id, userId, challengeId, status, progress, lastUpdated}
  - Status: enum {NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED}

## Recovery Engine
- **RecoveryLog**: {id, userId, date, sleepHours, soreness (1-10), fatigue (1-10), motivation (1-10), notes}

## Progress Engine
- **ProgressSnapshot**: {id, userId, timestamp, metrics (JSON or key-value pairs)}
- **PersonalRecord**: {id, userId, exerciseId, weight, reps, date, context}

## Relationships
- User 1 -- * MeasurementRecord
- User 1 -- * Goal
- User 1 -- * ChallengeProgress
- Challenge 1 -- * ChallengeProgress
- User 1 -- * RecoveryLog
- User 1 -- * ProgressSnapshot
- User 1 -- * PersonalRecord
- Workout 1 -- * WorkoutExercise
- WorkoutExercise 1 -- * WorkoutSet
- Exercise 1 -- * WorkoutExercise

Note: All foreign keys are enforced in Room. Cascading deletes are configured where appropriate.