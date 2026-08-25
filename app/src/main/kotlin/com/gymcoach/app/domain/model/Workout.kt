package com.gymcoach.app.domain.model

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.time.Instant

data class Workout(
    val id: Long = 0,
    val date: Instant,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Long,
    val notes: String,
    val completed: Boolean,
    val status: String = "NOT_STARTED"
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
) {
    fun toEntity(): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = id,
            workoutExerciseId = workoutExerciseId,
            setNumber = setNumber,
            weight = weight,
            reps = reps,
            rpe = rpe,
            restSeconds = restSeconds,
            completed = completed,
            setType = setType.ordinal
        )
    }
}

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
    val status: String = "NOT_STARTED",
    val volume: Double,
    val setCount: Int,
    val repCount: Int,
    val exerciseCount: Int
)