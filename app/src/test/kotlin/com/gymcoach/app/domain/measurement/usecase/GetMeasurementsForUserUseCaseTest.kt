package com.gymcoach.app.domain.measurement.usecase

import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class GetMeasurementsForUserUseCaseTest {

    private lateinit var repository: MeasurementRepository
    private lateinit var useCase: GetMeasurementsForUserUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetMeasurementsForUserUseCase(repository)
    }

    @Test
    fun `invoke returns flow of measurements for user`() = runTest {
        val userId = "test_user"
        val measurements = listOf(
            MeasurementRecord(
                id = 1,
                userId = userId,
                measurementType = MeasurementType.WEIGHT,
                value = 70.0,
                unit = "kg",
                date = Instant.now(),
                createdAt = Instant.now()
            )
        )
        every { repository.getMeasurementsForUser(userId) } returns mockk { answers { measurements.asFlow() } }

        val result = useCase(userId).first()
        assertEquals(measurements, result)
    }

    @Test
    fun `invoke returns empty list if no measurements`() = runTest {
        val userId = "test_user"
        every { repository.getMeasurementsForUser(userId) } returns mockk { answers { emptyList().asFlow() } }

        val result = useCase(userId).first()
        assertTrue(result.isEmpty())
    }
}