package com.gymcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * External-content FTS4 index over [ExerciseEntity].
 * Columns must mirror the referenced content-entity columns exactly.
 * Kept in sync via 'rebuild' after seeding (see ExerciseSeeder).
 */
@Fts4(contentEntity = ExerciseEntity::class)
@Entity(tableName = "exercise_fts")
data class ExerciseFtsEntity(
    val name: String,
    val description: String,
    val muscleGroup: String,
    val equipment: String,
    val difficulty: String,
    val category: String
)
