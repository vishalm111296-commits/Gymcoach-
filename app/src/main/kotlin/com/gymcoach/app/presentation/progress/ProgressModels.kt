package com.gymcoach.app.presentation.progress

import java.time.LocalDate

data class MuscleVolumeData(
    val muscleName: String,
    val currentSets: Int,
    val targetMin: Int,
    val targetMax: Int
)

data class ProgressPoint(val date: LocalDate, val value: Double)

data class TrendPoint(val date: LocalDate, val value: Double)

enum class TrendDirection { UP, DOWN, STABLE }

data class PersonalRecordItem(
    val exerciseName: String,
    val achievement: String,
    val date: LocalDate
)

enum class ProgressDateRange(val weeks: Int, val label: String) {
    FOUR_WEEKS(4, "4W"),
    EIGHT_WEEKS(8, "8W"),
    TWELVE_WEEKS(12, "12W")
}
