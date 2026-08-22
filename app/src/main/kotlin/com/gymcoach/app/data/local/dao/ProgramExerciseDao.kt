package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ProgramExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ProgramExerciseEntity>)

    @Update
    suspend fun update(exercise: ProgramExerciseEntity): Int

    @Query("SELECT * FROM program_exercises WHERE program_day_id = :dayId ORDER BY order_index")
    fun getByDayId(dayId: Long): Flow<List<ProgramExerciseEntity>>

    @Query("SELECT * FROM program_exercises WHERE id = :id")
    suspend fun getById(id: Long): ProgramExerciseEntity?

    @Query("DELETE FROM program_exercises WHERE program_day_id = :dayId")
    suspend fun deleteByDayId(dayId: Long): Int
}