package com.gymcoach.app.core.exercise

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

class SubstitutionEngineBenchmark {

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var exerciseSubstitutionDao: ExerciseSubstitutionDao
    private lateinit var equipmentAvailability: EquipmentAvailability
    private lateinit var substitutionEngine: SubstitutionEngine

    @Before
    fun setup() {
        exerciseDao = mockk()
        exerciseMuscleDao = mockk()
        exerciseSubstitutionDao = mockk()
        equipmentAvailability = mockk()

        substitutionEngine = SubstitutionEngine(
            exerciseDao = exerciseDao,
            exerciseMuscleDao = exerciseMuscleDao,
            exerciseSubstitutionDao = exerciseSubstitutionDao,
            equipmentAvailability = equipmentAvailability
        )
    }

    @Test
    fun benchmarkFindSubstitutes() = runBlocking {
        val originalExerciseId = 1L
        val originalExercise = ExerciseEntity(
            id = originalExerciseId,
            name = "Bench Press",
            description = "A compound exercise for chest",
            muscleGroup = "Chest",
            difficulty = "Intermediate",
            equipment = "barbell",
            category = "Compound",
            tags = "compound,push",
            vtaperLat = 0,
            vtaperLateralDelt = 0,
            vtaperUpperChest = 10,
            vtaperRearDelt = 0
        )

        // Mock substitutions
        val numSubstitutions = 1000
        val substitutions = (2L..numSubstitutions + 1L).map { id ->
            ExerciseSubstitutionEntity(originalExerciseId = originalExerciseId, substituteExerciseId = id)
        }

        // Mock substitute exercises
        val substituteExercises = (2L..numSubstitutions + 1L).map { id ->
            ExerciseEntity(
                id = id,
                name = "Substitute $id",
                description = "A substitute compound exercise",
                muscleGroup = "Chest",
                difficulty = "Intermediate",
                equipment = "dumbbell",
                category = "Compound",
                tags = "compound,push",
                vtaperLat = 0,
                vtaperLateralDelt = 0,
                vtaperUpperChest = 5,
                vtaperRearDelt = 0
            )
        }

        // Setup mocks
        every { exerciseDao.getById(originalExerciseId) } returns flowOf(originalExercise)
        every { exerciseSubstitutionDao.getByExerciseId(originalExerciseId) } returns flowOf(substitutions)

        // Simulating the N+1 behavior with slow retrieval
        substituteExercises.forEach { sub ->
            every { exerciseDao.getById(sub.id) } answers {
                Thread.sleep(1) // simulate DB delay
                flowOf(sub)
            }
        }

        // Simulating the optimized single query retrieval
        every { exerciseDao.getByIds(any()) } answers {
            Thread.sleep(5) // Simulate slightly longer DB delay for single query but overall faster than N+1
            flowOf(substituteExercises)
        }

        every { equipmentAvailability.isAvailable(any(), any()) } returns true
        every { exerciseDao.getAll() } returns flowOf(substituteExercises)

        // Warmup
        substitutionEngine.findSubstitutes(originalExerciseId, "gym", maxResults = 10)

        val time = measureTimeMillis {
            substitutionEngine.findSubstitutes(originalExerciseId, "gym", maxResults = 10)
        }

        println("Benchmark time: $time ms")
    }
}
