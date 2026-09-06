package com.gymcoach.app.core.progression

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgressionEngineTest {
    private lateinit var equipment: EquipmentAvailability
    private lateinit var engine: ProgressionEngine

    @Before
    fun setup() {
        equipment = mockk()
        engine = ProgressionEngine(equipment)
        every { equipment.isLimited(any(), any()) } returns false
    }

    private fun set(weight: Double, reps: Int, rpe: Double = 8.0, completed: Boolean = true, type: Int = 0) =
        WorkoutSetEntity(
            id = 0, workoutExerciseId = 1, setNumber = 1, weight = weight,
            reps = reps, rpe = rpe, restSeconds = 90, completed = completed, setType = type
        )

    @Test
    fun noCompletedWorkingSetsReturnsBaseTarget() {
        val result = engine.calculateProgression(1, "Curl", "dumbbell", 8, 12, 3, emptyList(), listOf(set(10.0, 10, completed = false), set(10.0, 10, type = 1)))
        assertEquals(0.0, result.currentWeight, 0.0)
        assertEquals(3, result.recommendedSets)
        assertEquals("8-12", result.recommendedReps)
    }

    @Test
    fun increasesLoadWhenTopRangeReachedAtRirOneOrMore() {
        val result = engine.calculateProgression(1, "Curl", "dumbbell", 8, 12, 3, emptyList(), listOf(set(20.0, 12), set(20.0, 12), set(20.0, 12)))
        assertEquals(22.0, result.recommendedWeight, 0.0)
        assertEquals(3, result.recommendedSets)
        assertFalse(result.isEquipmentLimited)
    }

    @Test
    fun doesNotIncreaseLoadWhenSetsAreTakenToFailure() {
        val result = engine.calculateProgression(1, "Curl", "dumbbell", 8, 12, 3, emptyList(), listOf(set(20.0, 12, rpe = 10.0), set(20.0, 12, rpe = 10.0)))
        assertEquals(20.0, result.recommendedWeight, 0.0)
        assertTrue(result.reason.contains("current load"))
    }

    @Test
    fun bodyweightProgressesRepsBeforeAddingSets() {
        val result = engine.calculateProgression(1, "Push-up", "bodyweight", 8, 12, 3, emptyList(), listOf(set(0.0, 12), set(0.0, 12)))
        assertEquals(0.0, result.recommendedWeight, 0.0)
        assertEquals("8-14", result.recommendedReps)
        assertEquals(3, result.recommendedSets)
        assertTrue(result.isEquipmentLimited)
    }

    @Test
    fun limitedLoadUsesRepProgression() {
        every { equipment.isLimited("dumbbell", "home") } returns true
        val result = engine.calculateProgression(1, "Curl", "dumbbell", 8, 12, 3, emptyList(), listOf(set(20.0, 12), set(20.0, 12)), equipmentType = "home")
        assertEquals(20.0, result.recommendedWeight, 0.0)
        assertEquals("8-14", result.recommendedReps)
        assertEquals(3, result.recommendedSets)
        assertTrue(result.isEquipmentLimited)
    }

    @Test
    fun regressionReducesLoadAfterConsecutiveLowSessions() {
        val result = engine.calculateProgression(1, "Press", "dumbbell", 8, 12, 3, listOf(set(20.0, 7)), listOf(set(20.0, 6)))
        assertEquals(18.0, result.recommendedWeight, 0.0)
    }

    @Test
    fun stableMidRangeMaintainsLoad() {
        val result = engine.calculateProgression(1, "Press", "dumbbell", 8, 12, 3, listOf(set(20.0, 9)), listOf(set(20.0, 10)))
        assertEquals(20.0, result.recommendedWeight, 0.0)
        assertEquals(3, result.recommendedSets)
    }
}
