package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val muscleGroup: String,
    val equipment: String,
    val difficulty: String,
    val secondaryMuscles: String = "",
    val instructions: String = "",
    val tips: String = "",
    val commonMistakes: String = "",
    val safetyNotes: String = "",
    val recommendedRepRange: String = "",
    val recommendedRestTime: String = "",
    val estimatedCalories: Int = 0,
    val category: String = "",
    val tags: String = "",
    val isFavorite: Boolean = false,
    val lastViewed: Long = 0L,
    // V-taper relevance scores (0-10)
    @ColumnInfo(name = "vtaper_lat") val vtaperLat: Int = 0,
    @ColumnInfo(name = "vtaper_lateral_delt") val vtaperLateralDelt: Int = 0,
    @ColumnInfo(name = "vtaper_upper_chest") val vtaperUpperChest: Int = 0,
    @ColumnInfo(name = "vtaper_rear_delt") val vtaperRearDelt: Int = 0,
    // Movement pattern
    @ColumnInfo(name = "movement_pattern") val movementPattern: String = "",
    // Media (nullable, architecture-ready)
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "video_url") val videoUrl: String? = null,
    @ColumnInfo(name = "animation_url") val animationUrl: String? = null,
    // Instructions
    @ColumnInfo(name = "setup_instructions") val setupInstructions: String = "",
    @ColumnInfo(name = "execution_instructions") val executionInstructions: String = "",
    @ColumnInfo(name = "breathing_instructions") val breathingInstructions: String = "",
    @ColumnInfo(name = "tempo_guidance") val tempoGuidance: String = "",
    // Progression variants
    @ColumnInfo(name = "beginner_variant_id") val beginnerVariantId: Long? = null,
    @ColumnInfo(name = "advanced_variant_id") val advancedVariantId: Long? = null
)
