package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PRDetectorTest {

    private lateinit var prDetector: PRDetector

    @Before
    fun setUp() {
        prDetector = PRDetector()
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

    // --- calculateEstimated1RM Tests ---

    @Test
    fun `calculateEstimated1RM returns zero when weight is zero or negative`() {
        assertEquals(0.0, prDetector.calculateEstimated1RM(0.0, 10), 0.001)
        assertEquals(0.0, prDetector.calculateEstimated1RM(-50.0, 10), 0.001)
    }

    @Test
    fun `calculateEstimated1RM returns zero when reps are zero or negative`() {
        assertEquals(0.0, prDetector.calculateEstimated1RM(100.0, 0), 0.001)
        assertEquals(0.0, prDetector.calculateEstimated1RM(100.0, -5), 0.001)
    }

    @Test
    fun `calculateEstimated1RM calculates correct value for 1 rep`() {
        // 100 * (1 + 1/30) = 103.3333...
        val expected = 100.0 * (1.0 + 1.0 / 30.0)
        assertEquals(expected, prDetector.calculateEstimated1RM(100.0, 1), 0.001)
    }

    @Test
    fun `calculateEstimated1RM calculates correct value for standard rep ranges`() {
        // 100 * (1 + 10/30) = 133.3333...
        val expected10Reps = 100.0 * (1.0 + 10.0 / 30.0)
        assertEquals(expected10Reps, prDetector.calculateEstimated1RM(100.0, 10), 0.001)

        // 52.5 * (1 + 8/30) = 66.5
        val expectedDecimalWeight = 52.5 * (1.0 + 8.0 / 30.0)
        assertEquals(expectedDecimalWeight, prDetector.calculateEstimated1RM(52.5, 8), 0.001)
    }

    @Test
    fun `calculateEstimated1RM caps reps at 12`() {
        // 100 * (1 + 12/30) = 140.0
        val expectedCap = 100.0 * (1.0 + 12.0 / 30.0)
        assertEquals(expectedCap, prDetector.calculateEstimated1RM(100.0, 12), 0.001)

        // 15 reps should be capped at 12 reps -> 140.0
        assertEquals(expectedCap, prDetector.calculateEstimated1RM(100.0, 15), 0.001)

        // 30 reps should be capped at 12 reps -> 140.0
        assertEquals(expectedCap, prDetector.calculateEstimated1RM(100.0, 30), 0.001)
    }

    // --- calculateVolume Tests ---

    @Test
    fun `calculateVolume returns zero for empty set list`() {
        assertEquals(0.0, prDetector.calculateVolume(emptyList()), 0.001)
    }

    @Test
    fun `calculateVolume correctly sums weight times reps across all sets`() {
        val sets = listOf(
            createSet(weight = 100.0, reps = 5),
            createSet(weight = 100.0, reps = 5),
            createSet(weight = 80.0, reps = 10)
        )
        // (100*5) + (100*5) + (80*10) = 500 + 500 + 800 = 1800.0
        assertEquals(1800.0, prDetector.calculateVolume(sets), 0.001)
    }

    // --- detectPRs Tests ---

    @Test
    fun `detectPRs returns empty list when no completed working sets`() {
        val sets = listOf(
            createSet(weight = 100.0, reps = 10, completed = false),
            createSet(weight = 100.0, reps = 10, completed = true, setType = 1) // Warmup set
        )
        val prs = prDetector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            currentSets = sets,
            existingPRs = emptyList(),
            workoutId = 100L
        )
        assertTrue(prs.isEmpty())
    }

    @Test
    fun `detectPRs detects weight, rep, e1RM, and volume PRs when no existing PRs exist`() {
        val sets = listOf(
            createSet(weight = 100.0, reps = 10)
        )
        val prs = prDetector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            currentSets = sets,
            existingPRs = emptyList(),
            workoutId = 100L
        )

        assertEquals(4, prs.size)
        assertTrue(prs.any { it.type == PRDetector.PRType.WEIGHT && it.value == 100.0 })
        assertTrue(prs.any { it.type == PRDetector.PRType.REP && it.value == 10.0 })
        assertTrue(prs.any { it.type == PRDetector.PRType.ESTIMATED_1RM && it.value > 100.0 })
        assertTrue(prs.any { it.type == PRDetector.PRType.VOLUME && it.value == 1000.0 })
    }

    @Test
    fun `detectPRs only detects PRs that exceed existing records`() {
        val existingPRs = listOf(
            PRDetector.PersonalRecord(
                exerciseId = 1L,
                exerciseName = "Bench Press",
                type = PRDetector.PRType.WEIGHT,
                value = 100.0,
                details = "100.0kg lifted",
                date = Instant.now(),
                workoutId = 50L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L,
                exerciseName = "Bench Press",
                type = PRDetector.PRType.REP,
                value = 12.0,
                details = "12 reps at 90.0kg",
                date = Instant.now(),
                workoutId = 50L
            )
        )

        val sets = listOf(
            createSet(weight = 105.0, reps = 8) // Weight PR (105 > 100), but Reps (8 <= 12) is not a Rep PR
        )

        val prs = prDetector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            currentSets = sets,
            existingPRs = existingPRs,
            workoutId = 100L
        )

        assertTrue(prs.any { it.type == PRDetector.PRType.WEIGHT && it.value == 105.0 })
        assertTrue(prs.none { it.type == PRDetector.PRType.REP })
    }

    @Test
    fun `detectPRs detects bodyweight e1RM PR when weight is zero`() {
        val sets = listOf(
            createSet(weight = 0.0, reps = 20)
        )

        val prs = prDetector.detectPRs(
            exerciseId = 2L,
            exerciseName = "Push-up",
            currentSets = sets,
            existingPRs = emptyList(),
            workoutId = 100L
        )

        // Bodyweight e1RM proxy = reps * 1.5 = 20 * 1.5 = 30.0
        val bwE1RM = prs.firstOrNull { it.type == PRDetector.PRType.ESTIMATED_1RM }
        assertTrue(bwE1RM != null)
        assertEquals(30.0, bwE1RM!!.value, 0.001)
        assertTrue(bwE1RM.details.contains("Bodyweight e1RM"))
    }
}
