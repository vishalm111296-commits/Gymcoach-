package com.gymcoach.app.data.local.entity

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
    val completed: Boolean
)