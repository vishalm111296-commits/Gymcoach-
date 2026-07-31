package com.gymcoach.app.domain.repository

import java.util.Date

interface AnalyticsRepository {
    suspend fun getVolumeHistory(): List<Pair<Date, Double>>
    suspend fun getPersonalRecord(exerciseId: Long): Double?
    suspend fun getWeeklySummary(): List<Pair<Date, Double>>
    suspend fun getAllPersonalRecords(): List<PersonalRecord>
    suspend fun getTotalWorkouts(): Int
    suspend fun getTotalSets(): Int
    suspend fun getTotalReps(): Int
    suspend fun getTotalVolume(): Double
    suspend fun getTotalTrainingTimeMinutes(): Long
    suspend fun getMonthlyVolumes(): List<Pair<Date, Double>>
    suspend fun getMuscleGroupDistribution(): List<MuscleGroupStats>
    suspend fun getAverageWorkoutVolume(): Double
    suspend fun getAverageWorkoutDurationMinutes(): Long
}

data class PersonalRecord(
    val exerciseName: String,
    val maxWeight: Double
)

data class MuscleGroupStats(
    val name: String,
    val totalReps: Int
)