package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.MuscleGroupStats
import com.gymcoach.app.domain.repository.PersonalRecord
import com.gymcoach.app.domain.repository.WorkoutCounts
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Date
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : AnalyticsRepository {

    override suspend fun getVolumeHistory(): List<Pair<Date, Double>> {
        return workoutDao.getAllWorkoutVolumes().map {
            Pair(Date(it.date), it.volume)
        }
    }

    override suspend fun getPersonalRecord(exerciseId: Long): Double? {
        return workoutDao.getPersonalRecordMax(exerciseId)
    }

    override suspend fun getWeeklySummary(): List<Pair<Date, Double>> {
        val volumes = workoutDao.getAllWorkoutVolumes()
        val zoneId = ZoneId.systemDefault()
        val grouped = mutableMapOf<Date, Double>()

        for (dv in volumes) {
            val mondayInstant = Instant.ofEpochMilli(dv.date)
                .atZone(zoneId)
                .with(WeekFields.ISO.dayOfWeek(), 1L)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant()
            val weekStart = Date.from(mondayInstant)
            grouped[weekStart] = (grouped[weekStart] ?: 0.0) + dv.volume
        }

        return grouped.toList().sortedBy { it.first }
    }

    override suspend fun getAllPersonalRecords(): List<PersonalRecord> {
        return workoutDao.getAllPersonalRecords().map {
            PersonalRecord(exerciseName = it.name, maxWeight = it.maxWeight)
        }
    }

    override suspend fun getTotalWorkouts(): Int {
        return workoutDao.getTotalWorkoutsCount()
    }

    override suspend fun getTotalSets(): Int {
        return workoutDao.getTotalSetsCount()
    }

    override suspend fun getTotalReps(): Int {
        return workoutDao.getTotalRepsCount() ?: 0
    }

    override suspend fun getTotalVolume(): Double {
        return workoutDao.getTotalVolumeSum() ?: 0.0
    }

    override suspend fun getTotalTrainingTimeMinutes(): Long {
        val totalSeconds = workoutDao.getTotalTrainingTimeSeconds() ?: 0L
        return totalSeconds / 60
    }

    override suspend fun getMonthlyVolumes(): List<Pair<Date, Double>> {
        return workoutDao.getMonthlyVolumes().map {
            Pair(Date(it.date), it.volume)
        }
    }

    override suspend fun getMuscleGroupDistribution(): List<MuscleGroupStats> {
        return workoutDao.getTopMuscleGroups().map {
            MuscleGroupStats(name = it.name, totalReps = it.totalReps)
        }
    }

    override suspend fun getAverageWorkoutVolume(): Double {
        return workoutDao.getAverageWorkoutVolume()
    }

    override suspend fun getAverageWorkoutDurationMinutes(): Long {
        val totalSeconds = workoutDao.getAverageWorkoutDurationSeconds()
        return totalSeconds / 60
    }

    override suspend fun getLongestWorkout(): WorkoutWithStats? {
        val entity = workoutDao.getLongestWorkout()
        return entity?.toDomain()
    }

    override suspend fun getShortestWorkout(): WorkoutWithStats? {
        val entity = workoutDao.getShortestWorkout()
        return entity?.toDomain()
    }

    private fun com.gymcoach.app.data.local.dao.WorkoutWithStats.toDomain() = WorkoutWithStats(
        id = id,
        date = Instant.ofEpochMilli(date),
        startTime = Instant.ofEpochMilli(startTime),
        endTime = Instant.ofEpochMilli(endTime),
        duration = duration,
        notes = notes,
        completed = completed,
        status = status,
        volume = volume,
        setCount = setCount,
        repCount = repCount,
        exerciseCount = exerciseCount
    )

    override suspend fun getWorkoutCounts(): WorkoutCounts {
        val zoneId = ZoneId.systemDefault()
        val nowMs = System.currentTimeMillis()
        val zdt = Instant.ofEpochMilli(nowMs).atZone(zoneId)

        val todayMs = zdt.truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli()
        val weekMs = zdt.with(WeekFields.ISO.dayOfWeek(), 1L).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli()
        val monthMs = zdt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli()

        return WorkoutCounts(
            total = workoutDao.getTotalWorkoutsCount(),
            today = workoutDao.getWorkoutsTodayCount(todayMs),
            week = workoutDao.getWorkoutsThisWeekCount(weekMs),
            month = workoutDao.getWorkoutsThisMonthCount(monthMs)
        )
    }

    override suspend fun getTotalExercises(): Int {
        return workoutDao.getTotalExercisesCount()
    }
}
