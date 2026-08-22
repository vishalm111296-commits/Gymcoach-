package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(day: ProgramDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<ProgramDayEntity>)

    @Update
    suspend fun update(day: ProgramDayEntity): Int

    @Query("SELECT * FROM program_days WHERE program_id = :programId ORDER BY day_number")
    fun getByProgramId(programId: Long): Flow<List<ProgramDayEntity>>

    @Query("SELECT * FROM program_days WHERE id = :id")
    suspend fun getById(id: Long): ProgramDayEntity?

    @Query("DELETE FROM program_days WHERE program_id = :programId")
    suspend fun deleteByProgramId(programId: Long): Int
}