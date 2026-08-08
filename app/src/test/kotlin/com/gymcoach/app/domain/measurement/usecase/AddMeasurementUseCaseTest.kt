package com.gymcoach.app.domain.measurement.usecase

import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class AddMeasurementUseCaseTest {

    private lateinit var repository: MeasurementRepository
    private lateinit var useCase: AddMeasurementUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = AddMeasurementUseCase(repository)
    }

    @Test
    fun `invoke with valid data inserts measurement`() = runTest {
        // Given
        val userId = "test_user"
        val type = MeasurementType.WEIGHT
        val value = 70.0
        val notes = "Test notes"

        coEvery { repository.insertMeasurement(any()) } returns Unit

        // When
        val result = useCase(userId, type, value, notes)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.insertMeasurement(any()) }
    }

    @Test
    fun `invoke with negative value returns failure`() = runTest {
        val userId = "test_user"
        val type = MeasurementType.WEIGHT
        val value = -1.0

        val result = useCase(userId, type, value)

        assertTrue(result.isFailure)
        assertEquals("Value must be positive", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.insertMeasurement(any()) }
    }

    @Test
    fun `invoke with body fat percentage over 100 returns failure`() = runTest {
        val userId = "test_user"
        val type = MeasurementType.BODY_FAT
        val value = 101.0

        val result = useCase(userId, type, value)

        assertTrue(result.isFailure)
        assertEquals("Body fat percentage cannot exceed 100%", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.insertMeasurement(any()) }
    }

    @Test
    fun `invoke with unrealistic weight returns failure`() = runTest {
        val userId = "test_user"
        val type = MeasurementType.WEIGHT
        val value = 600.0

        val result = useCase(userId, type, value)

        assertTrue(result.isFailure)
        assertEquals("Weight value unrealistic", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.insertMeasurement(any()) }
    }

    @Test
    fun `invoke with unrealistic measurement returns failure`() = runTest {
        val userId = "test_user"
        val type = MeasurementType.SHOULDERS
        val value = 600.0

        val result = useCase(userId, type, value)

        assertTrue(result.isFailure)
        assertEquals("Measurement value unrealistic", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.insertMeasurement(any()) }
    }

    @Test
    fun `invoke with exception returns failure`() = runTest {
        val userId = "test_user"
        val type = MeasurementType.WEIGHT
        val value = 70.0
        val exception = RuntimeException("Database error")

        coEvery { repository.insertMeasurement(any()) } throws exception

        val result = useCase(userId, type, value)

        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}
