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

    @Query("SELECT * FROM equipment WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): EquipmentEntity?

    @Query("SELECT COUNT(*) FROM equipment")
    suspend fun count(): Int

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getById(id: Long): EquipmentEntity?
}
