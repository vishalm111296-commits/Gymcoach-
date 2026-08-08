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

class GetLatestMeasurementUseCaseTest {

    private lateinit var repository: MeasurementRepository
    private lateinit var useCase: GetLatestMeasurementUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetLatestMeasurementUseCase(repository)
    }

    @Test
    fun `invoke returns latest measurement for user`() = runTest {
        val userId = "test_user"
        val measurement = MeasurementRecord(
            id = 1,
            userId = userId,
            measurementType = MeasurementType.WEIGHT,
            value = 70.0,
            unit = "kg",
            date = Instant.now(),
            createdAt = Instant.now()
        )
        every { repository.getLatestMeasurementForUser(userId) } returns mockk { answers { flowOf(measurement) } }

        val result = useCase(userId).first()
        assertEquals(measurement, result)
    }

    @Test
    fun `invoke returns null if no measurements`() = runTest {
        val userId = "test_user"
        every { repository.getLatestMeasurementForUser(userId) } returns mockk { answers { flowOf(null) } }

        val result = useCase(userId).first()
        assertNull(result)
    }
}