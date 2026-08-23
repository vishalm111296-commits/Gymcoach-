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

    @Query("SELECT * FROM exercise_substitutions WHERE original_exercise_id = :exerciseId")
    fun getByExerciseId(exerciseId: Long): Flow<List<ExerciseSubstitutionEntity>>

    @Query("SELECT * FROM exercise_substitutions WHERE substitute_exercise_id = :substituteId")
    fun getBySubstituteId(substituteId: Long): Flow<List<ExerciseSubstitutionEntity>>

    @Query("DELETE FROM exercise_substitutions WHERE original_exercise_id = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: Long): Int

    @Query("DELETE FROM exercise_substitutions WHERE original_exercise_id = :exerciseId AND substitute_exercise_id = :substituteId")
    suspend fun deleteByExerciseAndSubstitute(exerciseId: Long, substituteId: Long): Int
}
