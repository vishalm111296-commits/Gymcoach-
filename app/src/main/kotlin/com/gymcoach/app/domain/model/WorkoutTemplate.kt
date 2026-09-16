package com.gymcoach.app.domain.model

data class WorkoutTemplate(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isArchived: Boolean = false,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val exercises: List<TemplateExercise> = emptyList()
)

data class TemplateExercise(
    val id: Long = 0,
    val templateId: Long = 0,
    val exerciseId: Long,
    val exerciseName: String = "",
    val muscleGroup: String = "",
    val equipment: String = "",
    val orderIndex: Int = 0,
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val targetWeightKg: Double = 0.0,
    val targetRpe: Double? = null,
    val restSeconds: Int = 90,
    val notes: String = ""
)
