package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class PRDetectorUnitTest {

    private lateinit var detector: PRDetector

    @Before
    fun setup() {
        detector = PRDetector()
    }

    @Test
    fun `calculateEstimated1RM with zero or negative inputs returns zero`() {
        assertEquals(0.0, detector.calculateEstimated1RM(0.0, 10), 0.001)
        assertEquals(0.0, detector.calculateEstimated1RM(-50.0, 10), 0.001)
        assertEquals(0.0, detector.calculateEstimated1RM(100.0, 0), 0.001)
        assertEquals(0.0, detector.calculateEstimated1RM(100.0, -5), 0.001)
    }

    @Test
    fun `calculateEstimated1RM calculates accurately and caps at 12 reps`() {
        // 100kg x 1 rep: 100 * (1 + 1/30) = 103.333kg
        val e1rm1 = detector.calculateEstimated1RM(100.0, 1)
        assertEquals(103.333, e1rm1, 0.01)

        // 100kg x 10 reps: 100 * (1 + 10/30) = 133.333kg
        val e1rm10 = detector.calculateEstimated1RM(100.0, 10)
        assertEquals(133.333, e1rm10, 0.01)

        // 100kg x 12 reps: 100 * (1 + 12/30) = 140.0kg
        val e1rm12 = detector.calculateEstimated1RM(100.0, 12)
        assertEquals(140.0, e1rm12, 0.01)

        // 100kg x 20 reps: capped at 12 reps -> same as 12 reps (140.0kg)
        val e1rm20 = detector.calculateEstimated1RM(100.0, 20)
        assertEquals(140.0, e1rm20, 0.01)
    }

    @Test
    fun `calculateVolume sums product of weight and reps`() {
        val sets = listOf(
            createSet(weight = 100.0, reps = 5),
            createSet(weight = 100.0, reps = 5),
            createSet(weight = 80.0, reps = 10)
        )
        val volume = detector.calculateVolume(sets)
        assertEquals(1800.0, volume, 0.001)
    }

    @Test
    fun `detectPRs returns empty list when no completed normal sets exist`() {
        val uncompletedSets = listOf(
            createSet(weight = 100.0, reps = 5, completed = false),
            createSet(weight = 80.0, reps = 10, setType = 1) // warmup
        )
        val result = detector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            currentSets = uncompletedSets,
            existingPRs = emptyList(),
            workoutId = 10L
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectPRs detects weight, rep, 1RM, and volume PRs against existing PRs`() {
        val now = Instant.now()
        val existingPRs = listOf(
            PRDetector.PersonalRecord(
                exerciseId = 1L,
                exerciseName = "Bench Press",
                type = PRDetector.PRType.WEIGHT,
                value = 100.0,
                details = "100.0kg lifted",
                date = now,
                workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L,
                exerciseName = "Bench Press",
                type = PRDetector.PRType.REP,
                value = 8.0,
                details = "8 reps at 90kg",
                date = now,
                workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L,
                exerciseName = "Bench Press",
                type = PRDetector.PRType.VOLUME,
                value = 1500.0,
                details = "Volume: 1500kg",
                date = now,
                workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L,
                exerciseName = "Bench Press",
                type = PRDetector.PRType.ESTIMATED_1RM,
                value = 115.0,
                details = "e1RM: 115.0kg",
                date = now,
                workoutId = 1L
            )
        )

        // New performance: 105kg for 10 reps (exceeds weight, reps, e1RM, and volume)
        val currentSets = listOf(
            createSet(weight = 105.0, reps = 10, completed = true, setType = 0),
            createSet(weight = 100.0, reps = 8, completed = true, setType = 0)
        )

        val prs = detector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            currentSets = currentSets,
            existingPRs = existingPRs,
            workoutId = 10L
        )

        val types = prs.map { it.type }.toSet()
        assertTrue("Must detect WEIGHT PR", types.contains(PRDetector.PRType.WEIGHT))
        assertTrue("Must detect REP PR", types.contains(PRDetector.PRType.REP))
        assertTrue("Must detect ESTIMATED_1RM PR", types.contains(PRDetector.PRType.ESTIMATED_1RM))
        assertTrue("Must detect VOLUME PR", types.contains(PRDetector.PRType.VOLUME))

        val weightPR = prs.first { it.type == PRDetector.PRType.WEIGHT }
        assertEquals(105.0, weightPR.value, 0.001)

        val repPR = prs.first { it.type == PRDetector.PRType.REP }
        assertEquals(10.0, repPR.value, 0.001)
    }

    @Test
    fun `detectPRs detects bodyweight e1RM proxy when weight is zero`() {
        val currentSets = listOf(
            createSet(weight = 0.0, reps = 20, completed = true, setType = 0)
        )
        val prs = detector.detectPRs(
            exerciseId = 5L,
            exerciseName = "Push Up",
            currentSets = currentSets,
            existingPRs = emptyList(),
            workoutId = 15L
        )

        val e1rmPR = prs.firstOrNull { it.type == PRDetector.PRType.ESTIMATED_1RM }
        assertTrue("Should detect bodyweight estimated 1RM", e1rmPR != null)
        assertEquals(30.0, e1rmPR!!.value, 0.001) // 20 reps * 1.5 proxy
    }

    private fun createSet(
        weight: Double,
        reps: Int,
        completed: Boolean = true,
        setType: Int = 0
    ): WorkoutSetEntity = WorkoutSetEntity(
        id = 0,
        workoutExerciseId = 1L,
        setNumber = 1,
        weight = weight,
        reps = reps,
        rpe = 8.0,
        restSeconds = 90,
        completed = completed,
        setType = setType
    )
}
