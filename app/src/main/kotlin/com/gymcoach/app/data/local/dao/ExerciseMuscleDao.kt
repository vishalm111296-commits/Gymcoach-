package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymcoach.app.data.local.entity.ExerciseMuscleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseMuscleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(relation: ExerciseMuscleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(relations: List<ExerciseMuscleEntity>)

    @Query("SELECT * FROM exercise_muscles WHERE exercise_id = :exerciseId")
    fun getByExerciseId(exerciseId: Long): Flow<List<ExerciseMuscleEntity>>

    @Query("SELECT * FROM exercise_muscles WHERE muscle_id = :muscleId")
    fun getByMuscleId(muscleId: Long): Flow<List<ExerciseMuscleEntity>>

    @Query("DELETE FROM exercise_muscles WHERE exercise_id = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: Long): Int

    @Query("DELETE FROM exercise_muscles WHERE exercise_id = :exerciseId AND muscle_id = :muscleId")
    suspend fun deleteByExerciseAndMuscle(exerciseId: Long, muscleId: Long): Int
}