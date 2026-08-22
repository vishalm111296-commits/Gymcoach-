package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exercise_id")]
)
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    @ColumnInfo(name = "weight_kg") val weightKg: Double = 0.0,
    @ColumnInfo(name = "reps") val reps: Int = 0,
    // Calculated 1RM (e.g. Epley formula)
    @ColumnInfo(name = "one_rep_max_kg") val oneRepMaxKg: Double = 0.0,
    @ColumnInfo(name = "achieved_at") val achievedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "notes") val notes: String = ""
)
