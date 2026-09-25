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

    @Test
    fun `calculateProgression recommends deload when readiness is critically low below 2`() {
        val currentSets = listOf(
            createSet(weight = 100.0, reps = 12),
            createSet(weight = 100.0, reps = 12)
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Squat",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets,
            readinessScore = 1.8
        )

        assertEquals(100.0, result.currentWeight, 0.0)
        assertEquals(90.0, result.recommendedWeight, 0.0) // 100 * 0.9 = 90
        assertEquals(2, result.recommendedSets) // Deload sets (3-1 = 2)
        assertTrue(result.isDeloadRecommended)
        assertFalse(result.isPlateaued)
        assertTrue(result.reason.contains("Readiness is critically low"))
    }

    @Test
    fun `calculateProgression holds weight when readiness is between 2 and 2_5 even if all reps hit max`() {
        val currentSets = listOf(
            createSet(weight = 100.0, reps = 12),
            createSet(weight = 100.0, reps = 12)
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Squat",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets,
            readinessScore = 2.2
        )

        assertEquals(100.0, result.currentWeight, 0.0)
        assertEquals(100.0, result.recommendedWeight, 0.0) // Held at current weight
        assertEquals(3, result.recommendedSets)
        assertFalse(result.isDeloadRecommended)
        assertTrue(result.reason.contains("Reduced readiness"))
    }

    @Test
    fun `calculateProgression detects plateau after 3 sessions at same weight without hitting top reps`() {
        val currentSets = listOf(
            createSet(weight = 80.0, reps = 9),
            createSet(weight = 80.0, reps = 9)
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets,
            consecutiveSessionsAtSameWeight = 3
        )

        assertTrue(result.isPlateaued)
        assertTrue(result.isDeloadRecommended)
        assertEquals(72.0, result.recommendedWeight, 0.0) // 80 * 0.9 = 72
        assertTrue(result.reason.contains("Plateau detected"))
    }

    @Test
    fun `calculateProgression breaks plateau when user reaches top of rep range`() {
        val currentSets = listOf(
            createSet(weight = 80.0, reps = 12),
            createSet(weight = 80.0, reps = 12)
        )

        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets,
            consecutiveSessionsAtSameWeight = 3
        )

        assertFalse(result.isPlateaued)
        assertFalse(result.isDeloadRecommended)
        assertEquals(85.0, result.recommendedWeight, 0.0) // 80 + 5 = 85
        assertTrue(result.reason.contains("Increase weight"))
    }

    @Test
    fun `roundToIncrement rounds weights to standard equipment plate increments`() {
        assertEquals(52.5, progressionEngine.roundToIncrement(52.3, 2.5), 0.001)
        assertEquals(55.0, progressionEngine.roundToIncrement(53.8, 2.5), 0.001)
        assertEquals(12.0, progressionEngine.roundToIncrement(11.1, 2.0), 0.001)
        assertEquals(10.0, progressionEngine.roundToIncrement(10.0, 2.5), 0.001)
    }

    @Test
    fun `parseRepRange parses various valid and fallback formats`() {
        assertEquals(Pair(8, 12), ProgressionEngine.parseRepRange("8-12"))
        assertEquals(Pair(8, 12), ProgressionEngine.parseRepRange("8–12")) // en dash
        assertEquals(Pair(8, 12), ProgressionEngine.parseRepRange("8 to 12"))
        assertEquals(Pair(3, 5), ProgressionEngine.parseRepRange("3 - 5 reps"))
        assertEquals(Pair(8, 10), ProgressionEngine.parseRepRange("10")) // single target
        assertEquals(Pair(1, 1), ProgressionEngine.parseRepRange("1")) // single target clamped at min 1
        assertEquals(Pair(8, 12), ProgressionEngine.parseRepRange("")) // blank default
        assertEquals(Pair(8, 12), ProgressionEngine.parseRepRange("abc")) // corrupt default
    }

    @Test
    fun `calculateProgression heavy weight progression adheres to ACSM 10kg ceiling`() {
        // At 100.0 kg: 100 * 1.05 = 105.0 kg
        val sets100 = listOf(
            createSet(weight = 100.0, reps = 12),
            createSet(weight = 100.0, reps = 12)
        )
        val result100 = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = sets100
        )
        assertEquals(105.0, result100.recommendedWeight, 0.01)

        // At 250.0 kg: 250 * 1.05 = 262.5 kg, capped at 250 + 10 = 260.0 kg (ACSM ceiling)
        val sets250 = listOf(
            createSet(weight = 250.0, reps = 5),
            createSet(weight = 250.0, reps = 5)
        )
        val result250 = progressionEngine.calculateProgression(
            exerciseId = 2L,
            exerciseName = "Deadlift",
            exerciseEquipment = "barbell",
            targetRepsMin = 3,
            targetRepsMax = 5,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = sets250
        )
        assertEquals(260.0, result250.recommendedWeight, 0.01)
    }

    @Test
    fun `calculateProgression light weight progression below 20kg adds 2kg increment`() {
        val sets10 = listOf(
            createSet(weight = 10.0, reps = 12),
            createSet(weight = 10.0, reps = 12)
        )
        val result10 = progressionEngine.calculateProgression(
            exerciseId = 3L,
            exerciseName = "Dumbbell Lateral Raise",
            exerciseEquipment = "dumbbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = sets10
        )
        assertEquals(12.0, result10.recommendedWeight, 0.01)
    }

    @Test
    fun `calculateProgression bodyweight regression maintains 0 weight with coaching advice`() {
        val currentSets = listOf(
            createSet(weight = 0.0, reps = 5) // Below target 8
        )
        val previousSets = listOf(
            createSet(weight = 0.0, reps = 6) // Below target 8
        )
        val result = progressionEngine.calculateProgression(
            exerciseId = 4L,
            exerciseName = "Pull Up",
            exerciseEquipment = "bodyweight",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = previousSets,
            currentSets = currentSets
        )
        assertEquals(0.0, result.recommendedWeight, 0.0)
        assertTrue(result.reason.contains("Focus on form and range of motion"))
    }

    @Test
    fun `calculateProgression bodyweight plateau maintains 0 weight and flags plateau`() {
        val currentSets = listOf(
            createSet(weight = 0.0, reps = 8),
            createSet(weight = 0.0, reps = 9)
        )
        val result = progressionEngine.calculateProgression(
            exerciseId = 5L,
            exerciseName = "Push Up",
            exerciseEquipment = "bodyweight",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = emptyList(),
            currentSets = currentSets,
            consecutiveSessionsAtSameWeight = 3
        )
        assertEquals(0.0, result.recommendedWeight, 0.0)
        assertTrue(result.isPlateaued)
        assertTrue(result.isDeloadRecommended)
        assertTrue(result.reason.contains("Plateau detected"))
    }

    @Test
    fun `calculateProgressionForExercise delegates properly with custom rep range in domain model`() {
        val exercise = com.gymcoach.app.domain.model.Exercise(
            id = 42L,
            name = "Incline Dumbbell Press",
            description = "Upper chest pressing movement",
            muscleGroup = "Chest",
            equipment = "dumbbell",
            difficulty = "Intermediate",
            recommendedRepRange = "6-10"
        )
        val currentSets = listOf(
            createSet(weight = 30.0, reps = 10),
            createSet(weight = 30.0, reps = 10)
        )
        val recommendation = progressionEngine.calculateProgressionForExercise(
            exercise = exercise,
            previousSets = emptyList(),
            currentSets = currentSets
        )

        assertEquals(42L, recommendation.exerciseId)
        assertEquals("Incline Dumbbell Press", recommendation.exerciseName)
        assertEquals(30.0, recommendation.currentWeight, 0.001)
        assertEquals(32.5, recommendation.recommendedWeight, 0.001) // 30.0 + 2.5
        assertEquals("6-10", recommendation.recommendedReps)
        assertFalse(recommendation.isDeloadRecommended)
        assertFalse(recommendation.isPlateaued)
    }

    @Test
    fun `roundToIncrement with zero or negative increment returns weight unchanged`() {
        assertEquals(42.5, progressionEngine.roundToIncrement(42.5, 0.0), 0.001)
        assertEquals(77.3, progressionEngine.roundToIncrement(77.3, -2.5), 0.001)
    }

    @Test
    fun `calculateProgression deload sets are clamped at minimum 2 sets`() {
        val currentSets = listOf(
            createSet(weight = 100.0, reps = 10)
        )
        // targetSets = 2 -> (2 - 1).coerceAtLeast(2) = 2
        val res2 = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Squat",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 2,
            previousSets = emptyList(),
            currentSets = currentSets,
            readinessScore = 1.5
        )
        assertEquals(2, res2.recommendedSets)

        // targetSets = 5 -> 5 - 1 = 4
        val res5 = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Squat",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 5,
            previousSets = emptyList(),
            currentSets = currentSets,
            readinessScore = 1.5
        )
        assertEquals(4, res5.recommendedSets)
    }

    @Test
    fun `isRegressing returns false when previous sets contain only warmups or incomplete sets`() {
        val currentSets = listOf(
            createSet(weight = 80.0, reps = 5) // Below target 8
        )
        val previousWarmupOnly = listOf(
            createSet(weight = 80.0, reps = 5, completed = true, setType = 1), // Warmup
            createSet(weight = 80.0, reps = 5, completed = false, setType = 0)  // Incomplete
        )
        val result = progressionEngine.calculateProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = previousWarmupOnly,
            currentSets = currentSets
        )
        // Because previous normal sets are empty, regression is false and weight is maintained
        assertEquals(80.0, result.recommendedWeight, 0.001)
        assertTrue(result.reason.contains("Maintain current weight"))
    }

    @Test
    fun `calculateProgression handles ACSM weight progression boundaries and 10kg cap`() {
        fun runProgressionWithWeight(weight: Double): Double {
            val sets = listOf(createSet(weight = weight, reps = 12))
            val res = progressionEngine.calculateProgression(
                exerciseId = 1L,
                exerciseName = "Exercise",
                exerciseEquipment = "barbell",
                targetRepsMin = 8,
                targetRepsMax = 12,
                targetSets = 3,
                previousSets = emptyList(),
                currentSets = sets
            )
            return res.recommendedWeight
        }

        // Sub-20kg: +2.0kg increment, rounded to 2.0 multiple
        // 16.0 -> 16.0 + 2.0 = 18.0
        assertEquals(18.0, runProgressionWithWeight(16.0), 0.001)

        // Boundary at 20kg: 20.0 <= weight < 50.0 adds 2.5kg, rounded to 2.5 multiple
        // 20.0 -> 22.5
        assertEquals(22.5, runProgressionWithWeight(20.0), 0.001)
        // 47.5 -> 50.0
        assertEquals(50.0, runProgressionWithWeight(47.5), 0.001)

        // Boundary at 50kg: 50.0 <= weight < 100.0 adds 5.0kg, rounded to 2.5 multiple
        // 50.0 -> 55.0
        assertEquals(55.0, runProgressionWithWeight(50.0), 0.001)
        // 95.0 -> 100.0
        assertEquals(100.0, runProgressionWithWeight(95.0), 0.001)

        // Boundary at 100kg: weight >= 100.0 adds 5% (capped at +10kg)
        // 100.0 -> 100.0 * 1.05 = 105.0
        assertEquals(105.0, runProgressionWithWeight(100.0), 0.001)
        // 150.0 -> 150.0 * 1.05 = 157.5
        assertEquals(157.5, runProgressionWithWeight(150.0), 0.001)
        // 200.0 -> 200.0 * 1.05 = 210.0 (+10.0kg exact cap)
        assertEquals(210.0, runProgressionWithWeight(200.0), 0.001)
        // 300.0 -> 300.0 * 1.05 = 315.0, clamped at 300.0 + 10.0 = 310.0
        assertEquals(310.0, runProgressionWithWeight(300.0), 0.001)
    }

    @Test
    fun `parseRepRange parses various delimiters single integers and handles invalid inputs`() {
        // Hyphen with spaces
        val (min1, max1) = ProgressionEngine.parseRepRange(" 6 - 10 ")
        assertEquals(6, min1)
        assertEquals(10, max1)

        // En-dash delimiter
        val (min2, max2) = ProgressionEngine.parseRepRange("8–12")
        assertEquals(8, min2)
        assertEquals(12, max2)

        // Word 'to' delimiter
        val (min3, max3) = ProgressionEngine.parseRepRange("12 to 15")
        assertEquals(12, min3)
        assertEquals(15, max3)

        // Single integer "10" -> (10 - 2, 10) = (8, 10)
        val (min4, max4) = ProgressionEngine.parseRepRange("10")
        assertEquals(8, min4)
        assertEquals(10, max4)

        // Single integer "1" clamped at 1 -> (1, 1)
        val (min5, max5) = ProgressionEngine.parseRepRange("1")
        assertEquals(1, min5)
        assertEquals(1, max5)

        // Blank string defaults
        val (min6, max6) = ProgressionEngine.parseRepRange("   ", defaultMin = 5, defaultMax = 8)
        assertEquals(5, min6)
        assertEquals(8, max6)

        // Invalid non-digit characters default
        val (min7, max7) = ProgressionEngine.parseRepRange("unlimited", defaultMin = 4, defaultMax = 6)
        assertEquals(4, min7)
        assertEquals(6, max7)
    }

    @Test
    fun `calculateProgression transitions strictly at readiness score boundaries`() {
        val currentSets = listOf(createSet(weight = 100.0, reps = 12)) // Hits max reps

        // Readiness < 2.0 (critically low): deload recommended, weight decreases 10%
        val res19 = progressionEngine.calculateProgression(
            exerciseId = 1L, exerciseName = "Deadlift", exerciseEquipment = "barbell",
            targetRepsMin = 8, targetRepsMax = 12, targetSets = 3,
            previousSets = emptyList(), currentSets = currentSets, readinessScore = 1.9
        )
        assertTrue(res19.isDeloadRecommended)
        assertEquals(90.0, res19.recommendedWeight, 0.001) // 100.0 * 0.9 = 90.0
        assertEquals(2, res19.recommendedSets) // (3 - 1).coerceAtLeast(2) = 2

        // Readiness 2.0 (reduced readiness boundary): holds load, no deload
        val res20 = progressionEngine.calculateProgression(
            exerciseId = 1L, exerciseName = "Deadlift", exerciseEquipment = "barbell",
            targetRepsMin = 8, targetRepsMax = 12, targetSets = 3,
            previousSets = emptyList(), currentSets = currentSets, readinessScore = 2.0
        )
        assertFalse(res20.isDeloadRecommended)
        assertEquals(100.0, res20.recommendedWeight, 0.001)
        assertEquals(3, res20.recommendedSets)
        assertTrue(res20.reason.contains("Reduced readiness (2.0/5.0)"))

        // Readiness 2.49 (still in reduced readiness range): holds load
        val res249 = progressionEngine.calculateProgression(
            exerciseId = 1L, exerciseName = "Deadlift", exerciseEquipment = "barbell",
            targetRepsMin = 8, targetRepsMax = 12, targetSets = 3,
            previousSets = emptyList(), currentSets = currentSets, readinessScore = 2.49
        )
        assertFalse(res249.isDeloadRecommended)
        assertEquals(100.0, res249.recommendedWeight, 0.001)

        // Readiness 2.50 (normal readiness): proceeds to weight progression because all hit top
        val res25 = progressionEngine.calculateProgression(
            exerciseId = 1L, exerciseName = "Deadlift", exerciseEquipment = "barbell",
            targetRepsMin = 8, targetRepsMax = 12, targetSets = 3,
            previousSets = emptyList(), currentSets = currentSets, readinessScore = 2.5
        )
        assertFalse(res25.isDeloadRecommended)
        assertEquals(105.0, res25.recommendedWeight, 0.001) // Weight progresses
    }

    @Test
    fun `calculateProgression handles bodyweight exercise plateau and regression preserving zero load`() {
        val bwSets = listOf(createSet(weight = 0.0, reps = 9)) // Below top 12

        // Plateau: 3 sessions at same weight (0.0) without hitting top
        val plateauRes = progressionEngine.calculateProgression(
            exerciseId = 1L, exerciseName = "Pull Up", exerciseEquipment = "pull-up bar",
            targetRepsMin = 8, targetRepsMax = 12, targetSets = 3,
            previousSets = emptyList(), currentSets = bwSets,
            consecutiveSessionsAtSameWeight = 3
        )
        assertTrue(plateauRes.isPlateaued)
        assertTrue(plateauRes.isDeloadRecommended)
        assertEquals(0.0, plateauRes.recommendedWeight, 0.0) // Must remain 0.0, not negative

        // Regression: reps below min (e.g. 6 reps where min is 8) across 2+ sessions
        val failingCurrentSets = listOf(createSet(weight = 0.0, reps = 6))
        val failingPrevSets = listOf(createSet(weight = 0.0, reps = 7))
        val regressionRes = progressionEngine.calculateProgression(
            exerciseId = 1L, exerciseName = "Pull Up", exerciseEquipment = "pull-up bar",
            targetRepsMin = 8, targetRepsMax = 12, targetSets = 3,
            previousSets = failingPrevSets, currentSets = failingCurrentSets
        )
        assertEquals(0.0, regressionRes.recommendedWeight, 0.0)
        assertTrue(regressionRes.reason.contains("Focus on form and range of motion."))
        assertFalse(regressionRes.reason.contains("Reduce weight."))
    }
}

