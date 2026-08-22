package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_days",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["program_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("program_id")]
)
data class ProgramDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "program_id") val programId: Long,
    @ColumnInfo(name = "day_number") val dayNumber: Int,
    @ColumnInfo(name = "name") val name: String = "",
    // e.g. "Push", "Pull", "Legs", "Rest"
    @ColumnInfo(name = "focus") val focus: String = "",
    @ColumnInfo(name = "is_rest_day") val isRestDay: Boolean = false
)
