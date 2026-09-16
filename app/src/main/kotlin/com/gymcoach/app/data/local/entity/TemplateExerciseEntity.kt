package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("template_id"),
        Index("exercise_id"),
        Index(value = ["template_id", "order_index"], unique = true)
    ]
)
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "template_id") val templateId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,
    @ColumnInfo(name = "target_sets") val targetSets: Int = 3,
    @ColumnInfo(name = "target_reps") val targetReps: String = "8-12",
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double = 0.0,
    @ColumnInfo(name = "target_rpe") val targetRpe: Double? = null,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int = 90,
    val notes: String = ""
)
