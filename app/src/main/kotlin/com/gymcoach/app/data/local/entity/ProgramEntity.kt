package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String = "",
    // e.g., "push_pull_legs", "upper_lower", "full_body"
    @ColumnInfo(name = "split_type") val splitType: String = "",
    @ColumnInfo(name = "duration_weeks") val durationWeeks: Int = 0,
    @ColumnInfo(name = "days_per_week") val daysPerWeek: Int = 0,
    @ColumnInfo(name = "difficulty") val difficulty: String = "",
    @ColumnInfo(name = "goal") val goal: String = "",
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
