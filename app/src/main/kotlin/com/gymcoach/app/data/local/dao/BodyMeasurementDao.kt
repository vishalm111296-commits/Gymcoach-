package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(measurements: List<BodyMeasurementEntity>)

    @Update
    suspend fun update(measurement: BodyMeasurementEntity): Int

    @Query("SELECT * FROM body_measurements ORDER BY recorded_at DESC")
    fun getAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY recorded_at DESC LIMIT 1")
    fun getLatest(): Flow<BodyMeasurementEntity?>

    @Query("SELECT * FROM body_measurements WHERE id = :id")
    suspend fun getById(id: Long): BodyMeasurementEntity?

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}