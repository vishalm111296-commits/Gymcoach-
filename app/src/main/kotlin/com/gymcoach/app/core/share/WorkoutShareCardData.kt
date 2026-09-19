package com.gymcoach.app.core.share

data class ExerciseShareSummary(
    val exerciseName: String,
    val bestSetSummary: String, // e.g. "100.0 kg × 5 reps"
    val totalSetsCount: Int,
    val isPr: Boolean = false
)

data class WorkoutShareCardData(
    val workoutTitle: String,
    val dateFormatted: String,
    val durationFormatted: String,
    val totalVolumeKg: Double,
    val totalSets: Int,
    val totalReps: Int,
    val prCount: Int,
    val topMuscles: List<String>,
    val exercises: List<ExerciseShareSummary>,
    val motivationalQuote: String
)
