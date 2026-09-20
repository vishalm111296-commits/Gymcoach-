package com.gymcoach.app.data.local.dao

import androidx.room.*
import com.gymcoach.app.data.local.entity.NutritionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NutritionLogEntity): Long

    @Update
    suspend fun update(log: NutritionLogEntity)

    @Delete
    suspend fun delete(log: NutritionLogEntity)

    @Query("SELECT * FROM nutrition_logs WHERE date >= :startOfDay AND date < :endOfDay ORDER BY date DESC")
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<NutritionLogEntity>>

    @Query("SELECT * FROM nutrition_logs ORDER BY date DESC LIMIT 90")
    fun getRecentLogs(): Flow<List<NutritionLogEntity>>

    @Query("SELECT SUM(calories) FROM nutrition_logs WHERE date >= :startOfDay AND date < :endOfDay")
    fun getDayCalories(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT SUM(proteinGrams) FROM nutrition_logs WHERE date >= :startOfDay AND date < :endOfDay")
    fun getDayProtein(startOfDay: Long, endOfDay: Long): Flow<Float?>

    @Query("DELETE FROM nutrition_logs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
