package com.gymcoach.app.domain.repository

import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.core.program.LoggedSet
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getAllWorkouts(): Flow<List<Workout>>
    fun getWorkoutWithDetails(workoutId: Long): Flow<WorkoutWithDetails?>
    suspend fun getLatestIncompleteWorkout(): Workout?
    suspend fun createWorkout(workout: Workout): Long
    suspend fun updateWorkout(workout: Workout)
    suspend fun deleteWorkout(workoutId: Long)
    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, orderIndex: Int): Long
    suspend fun removeExerciseFromWorkout(workoutExerciseId: Long)
    suspend fun addSetToExercise(workoutExerciseId: Long, set: WorkoutSet): Long
    suspend fun updateSet(set: WorkoutSet)
    suspend fun deleteSet(setId: Long)

    // History
    fun getCompletedWorkouts(): Flow<List<WorkoutWithStats>>
    fun getWorkoutsInDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutWithStats>>
    fun getWorkoutsByVolumeDesc(): Flow<List<WorkoutWithStats>>
    fun getWorkoutsByVolumeAsc(): Flow<List<WorkoutWithStats>>
    fun getWorkoutsByDurationDesc(): Flow<List<WorkoutWithStats>>
    fun getWorkoutsByDurationAsc(): Flow<List<WorkoutWithStats>>
    suspend fun searchWorkouts(query: String): List<WorkoutWithStats>
    suspend fun getIncompleteWorkout(): Workout?

    fun getLoggedSets(): Flow<List<LoggedSet>>
}
