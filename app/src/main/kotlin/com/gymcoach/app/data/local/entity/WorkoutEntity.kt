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
    @ColumnInfo(defaultValue = "NOT_STARTED") val status: String = "NOT_STARTED",
    @ColumnInfo(name = "template_id") val templateId: Long? = null,
    @ColumnInfo(name = "template_version") val templateVersion: Int? = null,
    @ColumnInfo(name = "source_type", defaultValue = "EMPTY") val sourceType: String = "EMPTY",
    @ColumnInfo(name = "program_day_id") val programDayId: Long? = null
)
