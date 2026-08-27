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

    private lateinit var equipmentAvailability: EquipmentAvailability
    private lateinit var progressionEngine: ProgressionEngine

    @Before
    fun setup() {
        equipmentAvailability = mockk()
        progressionEngine = ProgressionEngine(equipmentAvailability)

        // Default mock behavior
        every { equipmentAvailability.isLimited(any(), any()) } returns false
    }

    private fun createSet(
        weight: Double,
        reps: Int,
        completed: Boolean = true,
        setType: Int = 0
    ) = WorkoutSetEntity(
        id = 0,
        workoutExerciseId = 1,
        setNumber = 1,
        weight = weight,
        reps = reps,
        rpe = 8.0,
        restSeconds = 60,
        completed = completed,
        setType = setType
    )

    @Test
    fun `calculateProgression returns base recommendation when no normal completed sets`() {
        val currentSets = listOf(
            createSet(weight = 50.0, reps = 10, completed = false),
            createSet(weight = 50.0, reps = 10, completed = true, setType = 1) // WARMUP
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Squat",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets
        )

        assertEquals(0.0, result.currentWeight, 0.0)
        assertEquals(0.0, result.recommendedWeight, 0.0)
        assertEquals("8-12", result.recommendedReps)
        assertEquals(3, result.recommendedSets)
        assertEquals("No completed working sets yet.", result.reason)
    }

    @Test
    fun `calculateProgression recommends weight increase when all sets hit max reps`() {
        val currentSets = listOf(
            createSet(weight = 50.0, reps = 12),
            createSet(weight = 50.0, reps = 12),
            createSet(weight = 50.0, reps = 13) // Exceeded max
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Squat",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets
        )

        assertEquals(50.0, result.currentWeight, 0.0)
        assertEquals(55.0, result.recommendedWeight, 0.0) // 50.0 * 1.05 = 52.5, but calculateIncrease for 50 <= x < 100 adds 5.0
        assertEquals("8-12", result.recommendedReps)
        assertEquals(3, result.recommendedSets)
        assertFalse(result.isEquipmentLimited)
    }

    @Test
    fun `calculateProgression recommends set rep progression for bodyweight exercises`() {
        val currentSets = listOf(
            createSet(weight = 0.0, reps = 12),
            createSet(weight = 0.0, reps = 12)
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Pushup",
            exerciseEquipment = "bodyweight",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets
        )

        assertEquals(0.0, result.currentWeight, 0.0)
        assertEquals(0.0, result.recommendedWeight, 0.0)
        assertEquals("8-14", result.recommendedReps) // targetRepsMax + 2
        assertEquals(4, result.recommendedSets) // targetSets + 1
        assertTrue(result.isEquipmentLimited)
    }

    @Test
    fun `calculateProgression recommends set rep progression when equipment is limited`() {
        every { equipmentAvailability.isLimited("dumbbell", "home") } returns true

        val currentSets = listOf(
            createSet(weight = 20.0, reps = 12),
            createSet(weight = 20.0, reps = 12)
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Dumbbell Curl",
            exerciseEquipment = "dumbbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets,
            equipmentType = "home"
        )

        assertEquals(20.0, result.currentWeight, 0.0)
        assertEquals(20.0, result.recommendedWeight, 0.0)
        assertEquals("8-14", result.recommendedReps)
        assertEquals(4, result.recommendedSets)
        assertTrue(result.isEquipmentLimited)
    }

    @Test
    fun `calculateProgression recommends weight decrease when regressing`() {
        val currentSets = listOf(
            createSet(weight = 60.0, reps = 6) // Below min of 8
        )
        val previousSets = listOf(
            createSet(weight = 60.0, reps = 7) // Also below min
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = previousSets,
            currentSets = currentSets
        )

        assertEquals(60.0, result.currentWeight, 0.0)
        assertEquals(54.0, result.recommendedWeight, 0.0) // 60.0 * 0.9 = 54.0
        assertEquals("8-12", result.recommendedReps)
        assertEquals(3, result.recommendedSets)
    }

    @Test
    fun `calculateProgression maintains weight when neither hitting max nor regressing`() {
        val currentSets = listOf(
            createSet(weight = 60.0, reps = 10) // Between 8 and 12
        )
        val previousSets = listOf(
            createSet(weight = 60.0, reps = 9)
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = previousSets,
            currentSets = currentSets
        )

        assertEquals(60.0, result.currentWeight, 0.0)
        assertEquals(60.0, result.recommendedWeight, 0.0) // Maintained
        assertEquals("8-12", result.recommendedReps)
        assertEquals(3, result.recommendedSets)
        assertEquals("Maintain current weight and focus on hitting target reps.", result.reason)
    }
}
