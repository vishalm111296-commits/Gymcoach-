package com.gymcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val restSeconds: Int,
    val completed: Boolean,
    val setType: Int = 0 // 0=NORMAL, 1=WARMUP, 2=DROP, 3=FAILURE
)

fun WorkoutSetEntity.fromLastSetData(data: com.gymcoach.app.data.local.dao.LastSetData): WorkoutSetEntity {
    return WorkoutSetEntity(
        id = 0,
        workoutExerciseId = 0,
        setNumber = 0,
        weight = data.weight,
        reps = data.reps,
        rpe = data.rpe,
        restSeconds = data.restSeconds,
        completed = true,
        setType = data.setType
    )
}

fun com.gymcoach.app.domain.model.WorkoutSet.toEntity(): WorkoutSetEntity {
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