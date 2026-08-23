package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSubstitutionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(substitution: ExerciseSubstitutionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(substitutions: List<ExerciseSubstitutionEntity>)

    @Query("SELECT * FROM exercise_substitutions WHERE original_exercise_id = :originalExerciseId ORDER BY reason ASC")
    fun getByExerciseId(originalExerciseId: Long): Flow<List<ExerciseSubstitutionEntity>>

    @Query("SELECT * FROM exercise_substitutions WHERE substitute_exercise_id = :substituteExerciseId")
    fun getBySubstituteId(substituteExerciseId: Long): Flow<List<ExerciseSubstitutionEntity>>

    @Query("DELETE FROM exercise_substitutions WHERE original_exercise_id = :originalExerciseId")
    suspend fun deleteByExerciseId(originalExerciseId: Long): Int

    @Query("DELETE FROM exercise_substitutions WHERE original_exercise_id = :originalExerciseId AND substitute_exercise_id = :substituteExerciseId")
    suspend fun deleteByExerciseAndSubstitute(originalExerciseId: Long, substituteExerciseId: Long): Int
}