package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(program: ProgramEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<ProgramEntity>)

    @Update
    suspend fun update(program: ProgramEntity): Int

    @Query("SELECT * FROM programs WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1")
    fun getActiveProgram(): Flow<ProgramEntity?>

    @Query("SELECT * FROM programs ORDER BY created_at DESC")
    fun getAllPrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun getById(id: Long): ProgramEntity?

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}