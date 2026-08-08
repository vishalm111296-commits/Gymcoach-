package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.MeasurementRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insertMeasurement(measurement: MeasurementRecordEntity)

    @Update
    suspend fun updateMeasurement(measurement: MeasurementRecordEntity)

    @Query("SELECT * FROM measurement_records WHERE userId = :userId ORDER BY date DESC")
    fun getMeasurementsForUser(userId: String): Flow<List<MeasurementRecordEntity>>

    @Query("SELECT * FROM measurement_records WHERE userId = :userId AND measurementType = :type ORDER BY date DESC")
    fun getMeasurementsByType(userId: String, type: String): Flow<List<MeasurementRecordEntity>>

    @Query("SELECT * FROM measurement_records WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    fun getLatestMeasurementForUser(userId: String): Flow<MeasurementRecordEntity?>

    @Query("SELECT * FROM measurement_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllMeasurementsForUser(userId: String): Flow<List<MeasurementRecordEntity>>

    @Query("SELECT * FROM measurement_records WHERE date >= :startDate ORDER BY date DESC")
    fun getMeasurementsSince(startDate: Long): Flow<List<MeasurementRecordEntity>>

    @Query("SELECT * FROM measurement_records WHERE measurementType IN (:types) ORDER BY date DESC")
    fun getMeasurementsByTypes(types: List<String>): Flow<List<MeasurementRecordEntity>>

    @Query("DELETE FROM measurement_records WHERE date < :cutoffDate")
    suspend fun deleteOldMeasurements(cutoffDate: Long)
}