package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo

/**
 * Projection returned by the JOIN query in [PersonalRecordDao.getAllWithExerciseName].
 * Contains all personal_records fields plus the exercise name from the exercises table.
 */
data class PersonalRecordWithExercise(
    val id: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    val reps: Int,
    @ColumnInfo(name = "one_rep_max_kg") val oneRepMaxKg: Double,
    @ColumnInfo(name = "achieved_at") val achievedAt: Long,
    val notes: String,
    @ColumnInfo(name = "exercise_name") val exerciseName: String
)
