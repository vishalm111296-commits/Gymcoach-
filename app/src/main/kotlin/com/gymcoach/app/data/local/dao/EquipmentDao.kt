package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(equipment: EquipmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(equipment: List<EquipmentEntity>)

    @Update
    suspend fun update(equipment: EquipmentEntity): Int

    @Query("SELECT * FROM equipment ORDER BY category, name")
    fun getAll(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE category = :category ORDER BY name")
    fun getByCategory(category: String): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE gym_available = 1 ORDER BY name")
    fun getGymAvailable(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE home_available = 1 ORDER BY name")
    fun getHomeAvailable(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getById(id: Long): EquipmentEntity?
}