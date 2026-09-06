package com.gymcoach.app.core.program

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProgramGeneratorVtaperRankingTest {

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var readinessRepository: ReadinessRepository
    private lateinit var generator: ProgramGenerator

    @Before
    fun setUp() {
        exerciseDao = mockk()
        readinessRepository = mockk()
        generator = ProgramGenerator(exerciseDao, EquipmentAvailability(), readinessRepository)
    }

    @Test
    fun testVtaperPrioritizationUsesTheProductionGenerator() = runTest {
        val lowerPriority = ExerciseEntity(
            id = 1,
            name = "Bodyweight Row",
            muscleGroup = "Back",
            equipment = "bodyweight",
            difficulty = "Intermediate",
            vtaperLat = 3
        )
        val higherPriority = ExerciseEntity(
            id = 2,
            name = "Lat-Focused Row",
            muscleGroup = "Back",
            equipment = "bodyweight",
            difficulty = "Intermediate",
            vtaperLat = 9
        )
        coEvery { exerciseDao.getAll() } returns flowOf(listOf(lowerPriority, higherPriority))
        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(null)

        val upperDay = generator.generateProgram(4, "custom", "vtaper").days.first { it.name == "Upper A" }

        assertEquals(2L, upperDay.exercises.first().exerciseId)
    }
}
