package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_substitutions",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["original_exercise_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["substitute_exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("original_exercise_id"), Index("substitute_exercise_id")]
)
data class ExerciseSubstitutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "original_exercise_id") val originalExerciseId: Long,
    @ColumnInfo(name = "substitute_exercise_id") val substituteExerciseId: Long,
    // "equipment" | "injury" | "difficulty" | "preference"
    @ColumnInfo(name = "reason") val reason: String = ""
)
