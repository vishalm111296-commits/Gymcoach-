package com.gymcoach.app.domain.model

import java.time.Instant

data class Workout(
    val id: Long = 0,
    val date: Instant,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Long,
    val notes: String,
    val completed: Boolean
)

data class WorkoutExercise(
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val orderIndex: Int
)

enum class SetType {
    NORMAL, WARMUP, DROP, FAILURE
}

data class WorkoutSet(
    val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val restSeconds: Int,
    val completed: Boolean,
    val setType: SetType = SetType.NORMAL
)

data class WorkoutWithDetails(
    val workout: Workout,
    val exercises: List<WorkoutExerciseWithSets>
)

data class WorkoutExerciseWithSets(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise,
    val sets: List<WorkoutSet>
)

data class WorkoutWithStats(
    val id: Long,
    val date: Instant,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Long,
    val notes: String,
    val completed: Boolean,
    val volume: Double,
    val setCount: Int,
    val repCount: Int,
    val exerciseCount: Int
)