package com.gymcoach.app.data.local.dao

import androidx.room.*
import com.gymcoach.app.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY LOWER(name) ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun getById(id: Long): Flow<ExerciseEntity?>

    @Query("""
        SELECT * FROM exercises 
        WHERE (:muscle IS NULL OR LOWER(muscleGroup) = LOWER(:muscle))
        AND (:difficulty IS NULL OR LOWER(difficulty) = LOWER(:difficulty))
        AND (:equipment IS NULL OR LOWER(equipment) = LOWER(:equipment))
        ORDER BY LOWER(name) ASC
    """)
    fun getFilteredExercises(muscle: String?, difficulty: String?, equipment: String?): Flow<List<ExerciseEntity>>

    // --- Enhanced library queries ---

    @Query("SELECT * FROM exercises WHERE LOWER(muscleGroup) = LOWER(:muscleGroup) ORDER BY LOWER(name) ASC")
    fun getByMuscleGroup(muscleGroup: String): Flow<List<ExerciseEntity>>

    /**
     * Strict equipment filter. Exercises store a comma-separated equipment list;
     * this matches only exact tokens, so "dumbbell" never matches "dumbbell_pair" etc.
     */
    @Query("""
        SELECT * FROM exercises
        WHERE (',' || REPLACE(LOWER(equipment), ' ', '') || ',')
              LIKE ('%,' || LOWER(:equipment) || ',%')
        ORDER BY LOWER(name) ASC
    """)
    fun getByEquipment(equipment: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE LOWER(difficulty) = LOWER(:difficulty) ORDER BY LOWER(name) ASC")
    fun getByDifficulty(difficulty: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE LOWER(movement_pattern) = LOWER(:pattern) ORDER BY LOWER(name) ASC")
    fun getByMovementPattern(pattern: String): Flow<List<ExerciseEntity>>

    /**
     * FTS4 prefix search over name/description/muscleGroup/equipment/category.
     * Prefix '*' is appended here; callers should pass plain alphanumeric terms.
     */
    @Query("""
        SELECT exercises.* FROM exercises
        INNER JOIN exercise_fts ON exercise_fts.rowid = exercises.id
        WHERE exercise_fts MATCH :query || '*'
        ORDER BY LOWER(exercises.name) ASC
    """)
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isFavorite = 1 ORDER BY LOWER(name) ASC")
    fun getFavorites(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE lastViewed > 0 ORDER BY lastViewed DESC LIMIT 20")
    fun getRecentlyUsed(): Flow<List<ExerciseEntity>>

    @Query("""
        SELECT * FROM exercises
        WHERE (vtaper_lat + vtaper_lateral_delt + vtaper_upper_chest + vtaper_rear_delt) > 0
        ORDER BY (vtaper_lat + vtaper_lateral_delt + vtaper_upper_chest + vtaper_rear_delt) DESC
        LIMIT 30
    """)
    fun getVtaperRelevant(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): ExerciseEntity?

/**
 * Bulk fetch of exercises by ids (used for batched workout-detail loading).
 * Room returns an empty list when the id list is empty.
 */
@Query("SELECT * FROM exercises WHERE id IN (:ids)")
suspend fun getByIds(ids: List<Long>): List<ExerciseEntity>

/**
 * All muscle assignments (primary/secondary/stabilizer) from the
 * seeded exercise_muscles table, joined with muscle taxonomy names.
 * Used by VolumeCalculator for weighted volume attribution.
 */
@Query(
    """
    SELECT em.exercise_id AS exerciseId, m.name AS muscleName, em.role AS role
    FROM exercise_muscles em
    INNER JOIN muscles m ON m.id = em.muscle_id
    """
)
suspend fun getAllMuscleAssignments(): List<MuscleAssignmentRow>

/**
 * Authoritative primary-muscle assignments from the seeded `exercise_muscles`
 * relation (role = 'primary'), joined with the taxonomy muscle name.
 * ProgramGenerator matches its muscle slots against these taxonomy ids
 * (e.g. "biceps", "hamstrings", "lateral_deltoid").
 */
@Query(
    """
    SELECT em.exercise_id AS exerciseId, m.name AS muscleName
    FROM exercise_muscles em
    INNER JOIN muscles m ON m.id = em.muscle_id
    WHERE em.role = 'primary'
    """
)
suspend fun getPrimaryMusclesByExercise(): List<PrimaryMuscleRow>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    // --- Writes ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)
}

/** One row of the primary-muscle join: an exercise id and its primary taxonomy muscle name. */
data class PrimaryMuscleRow(
    val exerciseId: Long,
    val muscleName: String
)

/** One row from exercise_muscles join: exercise id, taxonomy muscle name, and role (primary/secondary/stabilizer). */
data class MuscleAssignmentRow(
    val exerciseId: Long,
    val muscleName: String,
    val role: String
)
