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

    @Query("SELECT * FROM muscles ORDER BY body_region, name")
    fun getAll(): Flow<List<MuscleEntity>>

    @Query("SELECT * FROM muscles WHERE body_region = :bodyRegion ORDER BY name")
    fun getByBodyRegion(bodyRegion: String): Flow<List<MuscleEntity>>

    @Query("SELECT * FROM muscles WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): MuscleEntity?

    @Query("SELECT COUNT(*) FROM muscles")
    suspend fun count(): Int

    @Query("SELECT * FROM muscles WHERE id = :id")
    suspend fun getById(id: Long): MuscleEntity?
}
