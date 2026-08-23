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

    /**
     * The single resumable workout, if any.
     *
     * Resume requires BOTH conditions:
     *  1. status = 'ACTIVE'  - legacy zombies were backfilled ABANDONED by
     *     MIGRATION_7_8; discarded workouts are marked ABANDONED at runtime;
     *  2. at least one exercise attached - so walking into a session and
     *     immediately backing out cannot manufacture a resumable ghost.
     */
    @Query(
        "SELECT * FROM workouts WHERE status = 'ACTIVE' " +
            "AND id IN (SELECT workoutId FROM workout_exercises) " +
            "ORDER BY date DESC LIMIT 1"
    )
    suspend fun getActiveWorkout(): WorkoutEntity?

    /**
     * Session-start hygiene (skeptic review finding): a user who removes all
     * exercises and then force-quits leaves an ACTIVE row with no content.
     * The resume query already makes such rows unreachable; this reaps them
     * to ABANDONED so they cannot linger as live-state artifacts.
     *
     * @return number of rows transitioned ACTIVE -> ABANDONED.
     */
    @Query(
        "UPDATE workouts SET status = 'ABANDONED' " +
            "WHERE status = 'ACTIVE' " +
            "AND id NOT IN (SELECT workoutId FROM workout_exercises)"
    )
    suspend fun abandonEmptyActiveWorkouts(): Int

    /** Legacy completed=0 lookup. NOT used for resume decisions anymore. */
    @Query("SELECT * FROM workouts WHERE completed = 0 ORDER BY date DESC LIMIT 1")
    suspend fun getLatestIncompleteWorkout(): WorkoutEntity?

    @Deprecated(
        "Duplicate of getLatestIncompleteWorkout; kept for existing DAO callers",
        ReplaceWith("getLatestIncompleteWorkout()")
    )
    @Query("SELECT * FROM workouts WHERE completed = 0 ORDER BY date DESC LIMIT 1")
    suspend fun getIncompleteWorkout(): WorkoutEntity?

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

    // Analytics queries
    @Query("""
        SELECT MAX(ws.weight) 
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND w.completed = 1
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
        WHERE w.completed = 1
        GROUP BY w.date
        ORDER BY w.date ASC
    """)
    suspend fun getAllWorkoutVolumes(): List<DateVolume>

    @Query("SELECT COUNT(*) FROM workouts WHERE completed = 1")
    suspend fun getTotalWorkoutsCount(): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE completed = 1 AND date >= :todayStart")
    suspend fun getWorkoutsTodayCount(todayStart: Long): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE completed = 1 AND date >= :weekStart")
    suspend fun getWorkoutsThisWeekCount(weekStart: Long): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE completed = 1 AND date >= :monthStart")
    suspend fun getWorkoutsThisMonthCount(monthStart: Long): Int

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutId IN (SELECT id FROM workouts WHERE completed = 1)")
    suspend fun getTotalExercisesCount(): Int

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
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
        WHERE w.completed = 1
        GROUP BY w.id
        ORDER BY w.duration ASC
        LIMIT 1
    """)
    suspend fun getShortestWorkout(): WorkoutWithStats?

    @Query("SELECT SUM(duration) FROM workouts WHERE completed = 1")
    suspend fun getTotalTrainingTimeSeconds(): Long?

    @Query("SELECT COUNT(*) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.completed = 1")
    suspend fun getTotalSetsCount(): Int

    @Query("SELECT SUM(reps) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.completed = 1")
    suspend fun getTotalRepsCount(): Int?

    @Query("SELECT SUM(weight * reps) FROM workout_sets INNER JOIN workouts ON workout_sets.workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = workouts.id) WHERE workouts.completed = 1")
    suspend fun getTotalVolumeSum(): Double?

    @Query("""
        SELECT w.date, SUM(ws.reps * ws.weight) as volume
        FROM workouts w
        INNER JOIN workout_exercises we ON we.workoutId = w.id
        INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
        GROUP BY strftime('%Y-%m', w.date)
        ORDER BY w.date ASC
    """)
    // Using SQLite strftime for reliable monthly grouping on Android's Room/SQLite backend.
    suspend fun getMonthlyVolumes(): List<DateVolume>

    @Query("""
        SELECT e.name, SUM(ws.reps) as totalReps
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        INNER JOIN exercises e ON e.id = we.exerciseId
        WHERE w.completed = 1
        GROUP BY we.exerciseId
        ORDER BY totalReps DESC
        LIMIT 5
    """)
    suspend fun getTopMuscleGroups(): List<MuscleGroupStats>

    @Query("SELECT COALESCE(AVG(weight * reps), 0.0) FROM workout_sets ws INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId INNER JOIN workouts w ON w.id = we.workoutId WHERE w.completed = 1")
    suspend fun getAverageWorkoutVolume(): Double

    @Query("SELECT COALESCE(AVG(duration), 0.0) FROM workouts WHERE completed = 1")
    suspend fun getAverageWorkoutDurationSeconds(): Long

    // Workout History queries
    @Query("SELECT * FROM workouts WHERE completed = 1 ORDER BY date DESC")
    fun getCompletedWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE completed = 1 AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getWorkoutsInDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutEntity>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1 AND w.date >= :startDate AND w.date <= :endDate
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    fun getWorkoutsInDateRangeWithStats(startDate: Long, endDate: Long): Flow<List<WorkoutWithStats>>

    @Query("SELECT * FROM workouts WHERE completed = 1 ORDER BY date ASC")
    fun getCompletedWorkoutsAsc(): Flow<List<WorkoutEntity>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
        GROUP BY w.id
        ORDER BY w.duration DESC
    """)
    fun getCompletedWorkoutsByDurationDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
        GROUP BY w.id
        ORDER BY w.duration ASC
    """)
    fun getCompletedWorkoutsByDurationAsc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    fun getCompletedWorkoutsWithStats(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
        GROUP BY w.id
        ORDER BY volume DESC
    """)
    fun getCompletedWorkoutsWithStatsByVolumeDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
        GROUP BY w.id
        ORDER BY volume ASC
    """)
    fun getCompletedWorkoutsWithStatsByVolumeAsc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
        GROUP BY w.id
        ORDER BY w.duration DESC
    """)
    fun getCompletedWorkoutsWithStatsByDurationDesc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1
        GROUP BY w.id
        ORDER BY w.duration ASC
    """)
    fun getCompletedWorkoutsWithStatsByDurationAsc(): Flow<List<WorkoutWithStats>>

    @Query("""
        SELECT w.*, SUM(ws.reps * ws.weight) as volume, COUNT(ws.id) as setCount, SUM(ws.reps) as repCount, COUNT(DISTINCT we.id) as exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.completed = 1 AND (w.notes LIKE '%' || :query || '%' OR EXISTS (
            SELECT 1 FROM workout_exercises we2
            INNER JOIN exercises e ON e.id = we2.exerciseId
            WHERE we2.workoutId = w.id AND e.name LIKE '%' || :query || '%'
        ))
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    suspend fun searchWorkouts(query: String): List<WorkoutWithStats>
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
    val volume: Double?,
    val setCount: Int,
    val repCount: Int?,
    val exerciseCount: Int
)
