package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "muscles",
    indices = [Index("parent_muscle_id")]
)
data class MuscleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    // Self-referencing FK — enforced at app level; Room does not support self-ref FK easily
    @ColumnInfo(name = "parent_muscle_id") val parentMuscleId: Long? = null,
    @ColumnInfo(name = "body_region") val bodyRegion: String = ""
)
