package com.gymcoach.app.domain.repository

import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getAllWorkouts(): Flow<List<Workout>>
    fun getAllWorkoutTemplates(): Flow<List<Workout>>
    suspend fun getAllWorkoutTemplatesNow(): List<Workout>
    fun getWorkoutWithDetails(workoutId: Long): Flow<WorkoutWithDetails?>
    fun getAllWorkoutsWithDetails(): Flow<List<WorkoutWithDetails>>
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
    suspend fun getLatestSetForExercise(exerciseId: Long): WorkoutSet?
    suspend fun getBestVolumeSetForExercise(exerciseId: Long): WorkoutSet?
    suspend fun getIncompleteWorkout(): Workout?
    suspend fun getWorkoutWithDetailsNow(workoutId: Long): WorkoutWithDetails?
}
