package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymcoach.app.data.local.entity.ExerciseEquipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseEquipmentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(relation: ExerciseEquipmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(relations: List<ExerciseEquipmentEntity>)

    @Query("SELECT * FROM exercise_equipment WHERE exercise_id = :exerciseId")
    fun getByExerciseId(exerciseId: Long): Flow<List<ExerciseEquipmentEntity>>

    @Query("SELECT * FROM exercise_equipment WHERE equipment_id = :equipmentId")
    fun getByEquipmentId(equipmentId: Long): Flow<List<ExerciseEquipmentEntity>>

    @Query("DELETE FROM exercise_equipment WHERE exercise_id = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: Long): Int

    @Query("DELETE FROM exercise_equipment WHERE exercise_id = :exerciseId AND equipment_id = :equipmentId")
    suspend fun deleteByExerciseAndEquipment(exerciseId: Long, equipmentId: Long): Int
}