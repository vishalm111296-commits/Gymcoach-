package com.gymcoach.app.data.local.dao

import androidx.room.*
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Workouts
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutById(id: Long): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE status = 'ACTIVE' ORDER BY date DESC LIMIT 1")
    suspend fun getLatestIncompleteWorkout(): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    // WorkoutExercises
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity): Long

    @Update
    suspend fun updateWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Delete
    suspend fun deleteWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getWorkoutExerciseById(id: Long): WorkoutExerciseEntity?

    // WorkoutSets
    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    fun getSetsForExercise(workoutExerciseId: Long): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSet(set: WorkoutSetEntity): Long

    @Update
    suspend fun updateWorkoutSet(set: WorkoutSetEntity)

    @Delete
    suspend fun deleteWorkoutSet(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getWorkoutSetById(id: Long): WorkoutSetEntity?

    // Analytics queries — all filter on status = 'COMPLETED' for consistency
    // with WorkoutStatus enum (migration 7→8 backfills status from completed).
    @Query("""
        SELECT MAX(ws.weight) 
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND w.status = 'COMPLETED'
    """)
    suspend fun getPersonalRecordMax(exerciseId: Long): Double?

    @Query("""
        SELECT e.name, MAX(ws.weight) as maxWeight
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN exercises e ON e.id = we.exerciseId
        WHERE 1=1
        GROUP BY we.exerciseId
        ORDER BY maxWeight DESC
    """)
    suspend fun getAllPersonalRecords(): List<ExerciseMaxWeight>

    @Query("""
        SELECT w.date, SUM(ws.reps * ws.weight) as volume
        FROM workouts w
        INNER JOIN workout_exercises we ON we.workoutId = w.id
        INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.date
        ORDER BY w.date ASC
    """)
    suspend fun getAllWorkoutVolumes(): List<DateVolume>

    @Query("SELECT COUNT(*) FROM workouts WHERE status = 'COMPLETED'")
    suspend fun getTotalWorkoutsCount(): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE status = 'COMPLETED' AND date >= :todayStart")
    suspend fun getWorkoutsTodayCount(todayStart: Long): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE status = 'COMPLETED' AND date >= :weekStart")
    suspend fun getWorkoutsThisWeekCount(weekStart: Long): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE status = 'COMPLETED' AND date >= :monthStart")
    suspend fun getWorkoutsThisMonthCount(monthStart: Long): Int

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutId IN (SELECT id FROM workouts WHERE status = 'COMPLETED')")
    suspend fun getTotalExercisesCount(): Int

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration DESC
        LIMIT 1
    """)
    suspend fun getLongestWorkout(): WorkoutWithStats?

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration ASC
        LIMIT 1
    """)
    suspend fun getShortestWorkout(): WorkoutWithStats?

    @Query("SELECT SUM(duration) FROM workouts WHERE status = 'COMPLETED'")
    suspend fun getTotalTrainingTimeSeconds(): Long?

    @Query("SELECT COUNT(*) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.status = 'COMPLETED'")
    suspend fun getTotalSetsCount(): Int

    @Query("SELECT SUM(reps) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.status = 'COMPLETED'")
    suspend fun getTotalRepsCount(): Int?

    @Query("SELECT SUM(weight * reps) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.status = 'COMPLETED'")
    suspend fun getTotalVolumeSum(): Double?

    @Query("""
        SELECT w.date, SUM(ws.reps * ws.weight) as volume
        FROM workouts w
        INNER JOIN workout_exercises we ON we.workoutId = w.id
        INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY CAST(w.date / 2629746000 AS INTEGER)
        ORDER BY w.date ASC
    """)
    // Fixed: w.date is epoch-millis, not a date string. strftime('%Y-%m', w.date) returned
    // '1970-01' for all rows. Now groups by approximate month using integer division.
    // 2629746000 = avg milliseconds per month (365.25/12 * 86400000).
    suspend fun getMonthlyVolumes(): List<DateVolume>

    @Query("""
        SELECT e.name, SUM(ws.reps) as totalReps
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        INNER JOIN exercises e ON e.id = we.exerciseId
        WHERE w.status = 'COMPLETED'
        GROUP BY we.exerciseId
        ORDER BY totalReps DESC
        LIMIT 5
    """)
    suspend fun getTopMuscleGroups(): List<MuscleGroupStats>

    @Query("SELECT COALESCE(AVG(weight * reps), 0.0) FROM workout_sets ws INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId INNER JOIN workouts w ON w.id = we.workoutId WHERE w.status = 'COMPLETED'")
    suspend fun getAverageWorkoutVolume(): Double

    @Query("SELECT COALESCE(AVG(duration), 0.0) FROM workouts WHERE status = 'COMPLETED'")
    suspend fun getAverageWorkoutDurationSeconds(): Long

    // Workout History queries
    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY date DESC")
    fun getCompletedWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getWorkoutsInDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutEntity>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED' AND w.date >= :startDate AND w.date <= :endDate
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    fun getWorkoutsInDateRangeWithStats(startDate: Long, endDate: Long): Flow<List<WorkoutWithStats>>

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY date ASC")
    fun getCompletedWorkoutsAsc(): Flow<List<WorkoutEntity>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration DESC
    """)
    fun getCompletedWorkoutsByDurationDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration ASC
    """)
    fun getCompletedWorkoutsByDurationAsc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    fun getCompletedWorkoutsWithStats(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY volume DESC
    """)
    fun getCompletedWorkoutsWithStatsByVolumeDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY volume ASC
    """)
    fun getCompletedWorkoutsWithStatsByVolumeAsc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration DESC
    """)
    fun getCompletedWorkoutsWithStatsByDurationDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration ASC
    """)
    fun getCompletedWorkoutsWithStatsByDurationAsc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED' AND (w.notes LIKE '%' || :query || '%' OR EXISTS (
            SELECT 1 FROM workout_exercises we2
            INNER JOIN exercises e ON e.id = we2.exerciseId
            WHERE we2.workoutId = w.id AND e.name LIKE '%' || :query || '%'
        ))
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    suspend fun searchWorkouts(query: String): List<WorkoutWithStats>

    @Query("SELECT * FROM workouts WHERE status != 'COMPLETED' ORDER BY date DESC LIMIT 1")
    suspend fun getIncompleteWorkout(): WorkoutEntity?
}

data class DateVolume(
    val date: Long,
    val volume: Double
)

data class ExerciseMaxWeight(
    val name: String,
    val maxWeight: Double
)

data class MuscleGroupStats(
    val name: String,
    val totalReps: Int
)

data class WorkoutWithStats(
    val id: Long,
    val date: Long,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val notes: String,
    val completed: Boolean,
    val volume: Double,
    val setCount: Int,
    val repCount: Int,
    val exerciseCount: Int
)
