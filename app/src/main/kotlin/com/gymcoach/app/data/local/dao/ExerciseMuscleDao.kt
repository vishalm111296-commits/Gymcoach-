package com.gymcoach.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymcoach.app.data.local.entity.ExerciseMuscleEntity
import kotlinx.coroutines.flow.Flow

data class ExerciseMuscleWithDetails(
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "muscle_name") val muscleName: String,
    @ColumnInfo(name = "role") val role: String
)

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

    @Query("""
        SELECT em.exercise_id, m.name as muscle_name, em.role
        FROM exercise_muscles em
        INNER JOIN muscles m ON m.id = em.muscle_id
    """)
    fun getAllWithDetails(): Flow<List<ExerciseMuscleWithDetails>>

    @Query("DELETE FROM exercise_muscles WHERE exercise_id = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: Long): Int

    @Query("DELETE FROM exercise_muscles WHERE exercise_id = :exerciseId AND muscle_id = :muscleId")
    suspend fun deleteByExerciseAndMuscle(exerciseId: Long, muscleId: Long): Int
}
