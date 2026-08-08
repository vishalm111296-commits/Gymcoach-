package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.MeasurementDao
import com.gymcoach.app.data.local.entity.MeasurementRecordEntity
import com.gymcoach.app.domain.vshape.model.MeasurementType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class MeasurementRepositoryImplTest {

    private lateinit var dao: MeasurementDao
    private lateinit var repository: MeasurementRepositoryImpl

    @Before
    fun setup() {
        dao = mockk()
        repository = MeasurementRepositoryImpl(dao)
    }

    @Test
    fun `getMeasurementsForUser returns mapped domain models`() = runTest {
        val userId = "test_user"
        val entities = listOf(
            MeasurementRecordEntity(
                id = 1,
                userId = userId,
                measurementType = "WEIGHT",
                value = 70.0,
                unit = "kg",
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )
        every { dao.getMeasurementsForUser(userId) } returns flowOf(entities)

        val result = repository.getMeasurementsForUser(userId).first()
        assertEquals(1, result.size)
        assertEquals(userId, result[0].userId)
        assertEquals(MeasurementType.WEIGHT, result[0].measurementType)
        assertEquals(70.0, result[0].value, 0.001)
    }

    @Test
    fun `getMeasurementsByType returns filtered measurements`() = runTest {
        val userId = "test_user"
        val type = MeasurementType.WEIGHT
        val entities = listOf(
            MeasurementRecordEntity(
                id = 1,
                userId = userId,
                measurementType = "WEIGHT",
                value = 70.0,
                unit = "kg",
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )
        every { dao.getMeasurementsByType(userId, "WEIGHT") } returns flowOf(entities)

        val result = repository.getMeasurementsByType(userId, type).first()
        assertEquals(1, result.size)
        assertEquals(MeasurementType.WEIGHT, result[0].measurementType)
    }

    @Test
    fun `getLatestMeasurementForUser returns single measurement`() = runTest {
        val userId = "test_user"
        val entity = MeasurementRecordEntity(
            id = 1,
            userId = userId,
            measurementType = "WEIGHT",
            value = 70.0,
            unit = "kg",
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        every { dao.getLatestMeasurementForUser(userId) } returns flowOf(entity)

        val result = repository.getLatestMeasurementForUser(userId).first()
        assertEquals(userId, result?.userId)
        assertEquals(MeasurementType.WEIGHT, result?.measurementType)
    }

    @Test
    fun `insertMeasurement converts and inserts entity`() = runTest {
        val measurement = com.gymcoach.app.domain.vshape.model.MeasurementRecord(
            id = 0,
            userId = "test_user",
            measurementType = MeasurementType.WEIGHT,
            value = 70.0,
            unit = "kg",
            date = Instant.now(),
            createdAt = Instant.now()
        )
        coEvery { dao.insertMeasurement(any()) } returns Unit

        repository.insertMeasurement(measurement)
        coVerify { dao.insertMeasurement(any()) }
    }

    @Test
    fun `getTrendForType calculates trend correctly`() = runTest {
        val userId = "test_user"
        val type = MeasurementType.WEIGHT
        val now = Instant.now()
        val yesterday = now.minusSeconds(86400)
        val entities = listOf(
            MeasurementRecordEntity(
                id = 1,
                userId = userId,
                measurementType = "WEIGHT",
                value = 70.0,
                unit = "kg",
                date = yesterday.toEpochMilli(),
                createdAt = yesterday.toEpochMilli()
            ),
            MeasurementRecordEntity(
                id = 2,
                userId = userId,
                measurementType = "WEIGHT",
                value = 72.0,
                unit = "kg",
                date = now.toEpochMilli(),
                createdAt = now.toEpochMilli()
            )
        )
        every { dao.getMeasurementsByType(userId, "WEIGHT") } returns flowOf(entities)

        val result = repository.getTrendForType(userId, type).first()
        assertEquals(2.0, result.absoluteChange, 0.001)
        assertEquals(2.857, result.percentChange, 0.01)
    }

    @Test
    fun `getTrendForType returns zero trend if less than 2 measurements`() = runTest {
        val userId = "test_user"
        val type = MeasurementType.WEIGHT
        val entities = listOf(
            MeasurementRecordEntity(
                id = 1,
                userId = userId,
                measurementType = "WEIGHT",
                value = 70.0,
                unit = "kg",
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )
        every { dao.getMeasurementsByType(userId, "WEIGHT") } returns flowOf(entities)

        val result = repository.getTrendForType(userId, type).first()
        assertEquals(0.0, result.absoluteChange, 0.001)
        assertEquals(0.0, result.percentChange, 0.001)
    }
}