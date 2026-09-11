package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.MuscleGroupStats
import com.gymcoach.app.domain.repository.PersonalRecord
import com.gymcoach.app.domain.repository.WorkoutCounts
import com.gymcoach.app.domain.model.WorkoutWithStats
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
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
        val calendar = GregorianCalendar()

        val grouped = mutableMapOf<Date, Double>()
        for (dv in volumes) {
            calendar.time = Date(dv.date)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysToMonday = when (dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.SUNDAY -> 6
                else -> dayOfWeek - Calendar.MONDAY
            }
            calendar.add(Calendar.DAY_OF_MONTH, -daysToMonday)
            val weekStart = Date(calendar.timeInMillis)

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
        date = java.time.Instant.ofEpochMilli(date),
        startTime = java.time.Instant.ofEpochMilli(startTime),
        endTime = java.time.Instant.ofEpochMilli(endTime),
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
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        val weekCal = (cal.clone() as java.util.Calendar).apply {
            firstDayOfWeek = java.util.Calendar.MONDAY
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        }
        val weekStart = weekCal.timeInMillis

        val monthCal = (cal.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val monthStart = monthCal.timeInMillis

        return WorkoutCounts(
            total = workoutDao.getTotalWorkoutsCount(),
            today = workoutDao.getWorkoutsTodayCount(todayStart),
            week = workoutDao.getWorkoutsThisWeekCount(weekStart),
            month = workoutDao.getWorkoutsThisMonthCount(monthStart)
        )
    }

    override suspend fun getTotalExercises(): Int {
        return workoutDao.getTotalExercisesCount()
    }
}
