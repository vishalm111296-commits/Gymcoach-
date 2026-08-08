package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.VShapeDao
import com.gymcoach.app.data.local.entity.BodyMeasurement
import com.gymcoach.app.data.local.entity.VShapeChallengeCompletion
import com.gymcoach.app.domain.vshape.usecase.VShapeUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class VShapeRepository @Inject constructor(
    private val vShapeDao: VShapeDao,
    private val vShapeUseCase: VShapeUseCase
) {
    fun getLatestMeasurement(): Flow<BodyMeasurement?> = vShapeDao.getLatestMeasurement()

    suspend fun insertMeasurement(measurement: BodyMeasurement) {
        vShapeDao.insertMeasurement(measurement)
    }

    suspend fun insertChallengeCompletion(completion: VShapeChallengeCompletion) {
        vShapeDao.insertChallengeCompletion(completion)
    }

    fun getChallengeCompletions(): Flow<List<VShapeChallengeCompletion>> {
        return vShapeDao.getChallengeCompletions()
    }

    fun getCompletedDays(): Flow<Int> {
        return vShapeDao.getCompletedDays()
    }

    // Data access only - no business logic
    suspend fun getMeasurementByDate(date: Long): BodyMeasurement? {
        return vShapeDao.getMeasurements().value?.find { it.date == date }
    }
}