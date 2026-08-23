package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val notes: String,
    val completed: Boolean,
    /**
     * Lifecycle state, see domain.model.WorkoutStatus.
     * Default value MUST match MIGRATION_6_7's ALTER TABLE DEFAULT exactly -
     * Room validates schema identity after migration.
     */
    @ColumnInfo(defaultValue = "NOT_STARTED") val status: String = "NOT_STARTED"
)
