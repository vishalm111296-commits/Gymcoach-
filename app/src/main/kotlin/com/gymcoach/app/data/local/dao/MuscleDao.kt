package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.MuscleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(muscle: MuscleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(muscles: List<MuscleEntity>)

    @Update
    suspend fun update(muscle: MuscleEntity): Int

    @Query("SELECT * FROM muscles ORDER BY group_name, name")
    fun getAll(): Flow<List<MuscleEntity>>

    @Query("SELECT * FROM muscles WHERE group_name = :groupName ORDER BY name")
    fun getByGroup(groupName: String): Flow<List<MuscleEntity>>

    @Query("SELECT * FROM muscles WHERE vtaper_relevance > 0 ORDER BY vtaper_relevance DESC")
    fun getVtaperRelevant(): Flow<List<MuscleEntity>>

    @Query("SELECT * FROM muscles WHERE id = :id")
    suspend fun getById(id: Long): MuscleEntity?
}