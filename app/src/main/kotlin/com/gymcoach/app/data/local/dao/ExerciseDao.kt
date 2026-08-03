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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)
}