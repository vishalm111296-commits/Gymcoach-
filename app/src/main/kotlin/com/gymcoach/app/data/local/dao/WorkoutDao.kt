package com.gymcoach.app.data.local.dao

import androidx.room.*
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutDao {
    @Transaction
    open suspend fun createWorkoutFromHistoryTransaction(
        sourceWorkout: WorkoutEntity,
        sourceExercisesWithSets: List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>
    ): Long {
        val now = System.currentTimeMillis()
        val newWorkoutEntity = sourceWorkout.copy(
            id = 0,
            date = now,
            startTime = now,
            endTime = now,
            duration = 0,
            completed = false,
            status = "ACTIVE"
        )
        val newWorkoutId = insertWorkout(newWorkoutEntity)
        sourceExercisesWithSets.forEach { (exerciseEntity, sets) ->
            val newExerciseEntity = exerciseEntity.copy(
                id = 0,
                workoutId = newWorkoutId
            )
            val newWorkoutExerciseId = insertWorkoutExercise(newExerciseEntity)
            sets.sortedBy { it.setNumber }.forEach { set ->
                val newSetEntity = set.copy(
                    id = 0,
                    workoutExerciseId = newWorkoutExerciseId,
                    completed = false
                )
                insertWorkoutSet(newSetEntity)
            }
        }
        return newWorkoutId
    }
    // Workouts
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    abstract fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    abstract fun getWorkoutById(id: Long): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE status = 'ACTIVE' ORDER BY date DESC LIMIT 1")
    abstract suspend fun getLatestIncompleteWorkout(): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    abstract suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    abstract suspend fun deleteWorkout(workout: WorkoutEntity)

    // WorkoutExercises
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    abstract fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity): Long

    @Update
    abstract suspend fun updateWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Delete
    abstract suspend fun deleteWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    abstract suspend fun getWorkoutExerciseById(id: Long): WorkoutExerciseEntity?

    // WorkoutSets
    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    abstract fun getSetsForExercise(workoutExerciseId: Long): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkoutSet(set: WorkoutSetEntity): Long

    @Update
    abstract suspend fun updateWorkoutSet(set: WorkoutSetEntity)

    @Delete
    abstract suspend fun deleteWorkoutSet(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    abstract suspend fun getWorkoutSetById(id: Long): WorkoutSetEntity?

    // ─── Previous Performance Queries ───────────────────────────────────

    /**
     * Get the last completed workout's date and max weight for a given exercise.
     * Returns the most recent completed workout date + max weight achieved.
     */
    @Query("""
        SELECT w.date, MAX(ws.weight) as maxWeight
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND w.status = 'COMPLETED'
        ORDER BY w.date DESC
        LIMIT 1
    """)
    abstract suspend fun getLastPerformanceForExercise(exerciseId: Long): LastPerformance?

    /**
     * Get the last completed workout's sets for a given exercise.
     * Returns the weight, reps, rpe, and rest from the most recent session.
     */
    @Query("""
        SELECT ws.weight, ws.reps, ws.rpe, ws.restSeconds, ws.setType, w.date
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND w.status = 'COMPLETED'
        ORDER BY w.date DESC, ws.setNumber ASC
        LIMIT 10
    """)
    abstract suspend fun getLastSetsForExercise(exerciseId: Long): List<LastSetData>


    /**
     * Batched version of getLastPerformanceForExercise
     */
    @Query("""
        WITH LastWorkouts AS (
            SELECT we.exerciseId, MAX(w.date) as maxDate
            FROM workouts w
            INNER JOIN workout_exercises we ON we.workoutId = w.id
            WHERE w.status = 'COMPLETED' AND we.exerciseId IN (:exerciseIds)
            GROUP BY we.exerciseId
        )
        SELECT we.exerciseId, w.date, MAX(ws.weight) as maxWeight
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        INNER JOIN LastWorkouts lw ON lw.exerciseId = we.exerciseId AND lw.maxDate = w.date
        WHERE w.status = 'COMPLETED'
        GROUP BY we.exerciseId
    """)
    abstract suspend fun getLastPerformancesForExercises(exerciseIds: List<Long>): List<LastPerformanceWithExercise>

    /**
     * Batched version of getLastSetsForExercise
     */
    @Query("""
        SELECT we.exerciseId, ws.weight, ws.reps, ws.rpe, ws.restSeconds, ws.setType, w.date
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        WHERE w.status = 'COMPLETED' AND we.exerciseId IN (:exerciseIds)
        AND w.date = (
            SELECT MAX(w2.date)
            FROM workouts w2
            INNER JOIN workout_exercises we2 ON we2.workoutId = w2.id
            WHERE w2.status = 'COMPLETED' AND we2.exerciseId = we.exerciseId
        )
        ORDER BY we.exerciseId, ws.setNumber ASC
    """)
    abstract suspend fun getLastSetsForExercises(exerciseIds: List<Long>): List<LastSetDataWithExercise>

    // ─── Analytics Queries ──────────────────────────────────────────────

    @Query("""
        SELECT MAX(ws.weight)
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND w.status = 'COMPLETED'
    """)
    abstract suspend fun getPersonalRecordMax(exerciseId: Long): Double?

    @Query("""
        SELECT e.name, MAX(ws.weight) as maxWeight
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN exercises e ON e.id = we.exerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        WHERE w.status = 'COMPLETED'
        GROUP BY we.exerciseId
        ORDER BY maxWeight DESC
    """)
    abstract suspend fun getAllPersonalRecords(): List<ExerciseMaxWeight>

    @Query("""
        SELECT w.date, SUM(ws.reps * ws.weight) as volume
        FROM workouts w
        INNER JOIN workout_exercises we ON we.workoutId = w.id
        INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.date
        ORDER BY w.date ASC
    """)
    abstract suspend fun getAllWorkoutVolumes(): List<DateVolume>

    @Query("SELECT COUNT(*) FROM workouts WHERE status = 'COMPLETED'")
    abstract suspend fun getTotalWorkoutsCount(): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE status = 'COMPLETED' AND date >= :todayStart")
    abstract suspend fun getWorkoutsTodayCount(todayStart: Long): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE status = 'COMPLETED' AND date >= :weekStart")
    abstract suspend fun getWorkoutsThisWeekCount(weekStart: Long): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE status = 'COMPLETED' AND date >= :monthStart")
    abstract suspend fun getWorkoutsThisMonthCount(monthStart: Long): Int

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutId IN (SELECT id FROM workouts WHERE status = 'COMPLETED')")
    abstract suspend fun getTotalExercisesCount(): Int

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
    abstract suspend fun getLongestWorkout(): WorkoutWithStats?

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
    abstract suspend fun getShortestWorkout(): WorkoutWithStats?

    @Query("SELECT SUM(duration) FROM workouts WHERE status = 'COMPLETED'")
    abstract suspend fun getTotalTrainingTimeSeconds(): Long?

    @Query("SELECT COUNT(*) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.status = 'COMPLETED'")
    abstract suspend fun getTotalSetsCount(): Int

    @Query("SELECT SUM(reps) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.status = 'COMPLETED'")
    abstract suspend fun getTotalRepsCount(): Int?

    @Query("SELECT SUM(weight * reps) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.status = 'COMPLETED'")
    abstract suspend fun getTotalVolumeSum(): Double?

    @Query("""
        SELECT w.date, SUM(ws.reps * ws.weight) as volume
        FROM workouts w
        INNER JOIN workout_exercises we ON we.workoutId = w.id
        INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY strftime('%Y-%m', datetime(w.date / 1000, 'unixepoch'))
        ORDER BY w.date ASC
    """)
    abstract suspend fun getMonthlyVolumes(): List<DateVolume>

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
    abstract suspend fun getTopMuscleGroups(): List<MuscleGroupStats>

    @Query("SELECT COALESCE(AVG(weight * reps), 0.0) FROM workout_sets ws INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId INNER JOIN workouts w ON w.id = we.workoutId WHERE w.status = 'COMPLETED'")
    abstract suspend fun getAverageWorkoutVolume(): Double

    @Query("SELECT COALESCE(AVG(duration), 0.0) FROM workouts WHERE status = 'COMPLETED'")
    abstract suspend fun getAverageWorkoutDurationSeconds(): Long

    // Workout History queries
    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY date DESC")
    abstract fun getCompletedWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    abstract fun getWorkoutsInDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutEntity>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED' AND w.date >= :startDate AND w.date <= :endDate
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    abstract fun getWorkoutsInDateRangeWithStats(startDate: Long, endDate: Long): Flow<List<WorkoutWithStats>>

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY date ASC")
    abstract fun getCompletedWorkoutsAsc(): Flow<List<WorkoutEntity>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration DESC
    """)
    abstract fun getCompletedWorkoutsByDurationDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration ASC
    """)
    abstract fun getCompletedWorkoutsByDurationAsc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    abstract fun getCompletedWorkoutsWithStats(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY volume DESC
    """)
    abstract fun getCompletedWorkoutsWithStatsByVolumeDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY volume ASC
    """)
    abstract fun getCompletedWorkoutsWithStatsByVolumeAsc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration DESC
    """)
    abstract fun getCompletedWorkoutsWithStatsByDurationDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.status = 'COMPLETED'
        GROUP BY w.id
        ORDER BY w.duration ASC
    """)
    abstract fun getCompletedWorkoutsWithStatsByDurationAsc(): Flow<List<WorkoutWithStats>>

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
    abstract suspend fun searchWorkouts(query: String): List<WorkoutWithStats>

    @Query("SELECT * FROM workouts WHERE status = 'ACTIVE' ORDER BY date DESC LIMIT 1")
    abstract suspend fun getIncompleteWorkout(): WorkoutEntity?
}

data class LastPerformance(
    val date: Long,
    val maxWeight: Double
)

data class LastPerformanceWithExercise(
    val exerciseId: Long,
    val date: Long,
    val maxWeight: Double
)

data class LastSetData(
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val restSeconds: Int,
    val setType: Int,
    val date: Long
)

data class LastSetDataWithExercise(
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val restSeconds: Int,
    val setType: Int,
    val date: Long
)

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
    val status: String,
    val volume: Double,
    val setCount: Int,
    val repCount: Int,
    val exerciseCount: Int
)
