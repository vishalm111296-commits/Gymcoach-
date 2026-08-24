package com.gymcoach.app.data.local.dao

import androidx.room.*
import com.gymcoach.app.data.local.entity.ReadinessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadinessDao {
    @Query("SELECT * FROM readiness ORDER BY recorded_at DESC")
    fun getAll(): Flow<List<ReadinessEntity>>

    @Query("SELECT * FROM readiness WHERE id = :id")
    fun getById(id: Long): Flow<ReadinessEntity?>

    @Query("SELECT * FROM readiness ORDER BY recorded_at DESC LIMIT 1")
    fun getLatest(): Flow<ReadinessEntity?>

    @Query("SELECT * FROM readiness WHERE recorded_at BETWEEN :startTime AND :endTime ORDER BY recorded_at DESC")
    fun getInRange(startTime: Long, endTime: Long): Flow<List<ReadinessEntity>>

    @Query("SELECT * FROM readiness WHERE recorded_at > :since ORDER BY recorded_at DESC")
    fun getRecent(since: Long): Flow<List<ReadinessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(readiness: ReadinessEntity): Long

    @Update
    suspend fun update(readiness: ReadinessEntity)

    @Delete
    suspend fun delete(readiness: ReadinessEntity)

    @Query("DELETE FROM readiness WHERE id = :id")
    suspend fun deleteById(id: Long)
}
