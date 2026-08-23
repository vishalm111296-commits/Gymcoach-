package com.gymcoach.app.domain.model

data class Exercise(
    val id: Long = 0,
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
    val vtaperLat: Int = 0,
    val vtaperLateralDelt: Int = 0,
    val vtaperUpperChest: Int = 0,
    val vtaperRearDelt: Int = 0,
    // Movement pattern
    val movementPattern: String = "",
    // Media (nullable, architecture-ready)
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val animationUrl: String? = null,
    // Instructions
    val setupInstructions: String = "",
    val executionInstructions: String = "",
    val breathingInstructions: String = "",
    val tempoGuidance: String = "",
    // Progression variants
    val beginnerVariantId: Long? = null,
    val advancedVariantId: Long? = null
)