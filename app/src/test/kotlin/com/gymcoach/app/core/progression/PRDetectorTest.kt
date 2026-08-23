package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class PRDetectorTest {

    private lateinit var detector: PRDetector

    @Before
    fun setUp() {
        detector = PRDetector()
    }

    private fun makeSet(
        weight: Double = 50.0,
        reps: Int = 10,
        completed: Boolean = true,
        setType: Int = 0
    ) = WorkoutSetEntity(
        id = 0,
        workoutExerciseId = 0,
        setNumber = 1,
        weight = weight,
        reps = reps,
        rpe = 7.0,
        restSeconds = 90,
        completed = completed,
        setType = setType
    )

    // --- Estimated 1RM Tests ---

    @Test
    fun `e1RM calculation uses Epley formula capped at 12 reps`() {
        // 100kg x 10 reps = 100 * (1 + 10/30) = 133.3
        val e1rm = detector.calculateEstimated1RM(100.0, 10)
        assertEquals(133.33, e1rm, 0.1)
    }

    @Test
    fun `e1RM caps at 12 reps`() {
        // 100kg x 15 reps should use 12 reps max
        val e1rm = detector.calculateEstimated1RM(100.0, 15)
        assertEquals(140.0, e1rm, 0.1)
    }

    @Test
    fun `e1RM returns 0 for invalid input`() {
        assertEquals(0.0, detector.calculateEstimated1RM(0.0, 10), 0.01)
        assertEquals(0.0, detector.calculateEstimated1RM(100.0, 0), 0.01)
        assertEquals(0.0, detector.calculateEstimated1RM(-50.0, 10), 0.01)
    }

    // --- Volume Tests ---

    @Test
    fun `volume sums weight * reps across all sets`() {
        val sets = listOf(
            makeSet(weight = 50.0, reps = 10),
            makeSet(weight = 60.0, reps = 8),
            makeSet(weight = 40.0, reps = 12)
        )
        // 500 + 480 + 480 = 1460
        assertEquals(1460.0, detector.calculateVolume(sets), 0.01)
    }

    @Test
    fun `volume is 0 for empty sets`() {
        assertEquals(0.0, detector.calculateVolume(emptyList()), 0.01)
    }

    // --- PR Detection Tests ---

    @Test
    fun `detects weight PR when exceeding existing`() {
        val current = listOf(makeSet(weight = 100.0, reps = 5))
        val existing = listOf(
            PRDetector.PersonalRecord(
                1L, "Bench", PRDetector.PRType.WEIGHT, 90.0,
                "", Instant.now(), 1L
            )
        )
        val prs = detector.detectPRs(1L, "Bench", current, existing, 2L)
        val weightPR = prs.find { it.type == PRDetector.PRType.WEIGHT }
        assertNotNull(weightPR)
        assertEquals(100.0, weightPR!!.value, 0.01)
    }

    @Test
    fun `no weight PR when not exceeding existing`() {
        val current = listOf(makeSet(weight = 80.0, reps = 5))
        val existing = listOf(
            PRDetector.PersonalRecord(
                1L, "Bench", PRDetector.PRType.WEIGHT, 90.0,
                "", Instant.now(), 1L
            )
        )
        val prs = detector.detectPRs(1L, "Bench", current, existing, 2L)
        val weightPR = prs.find { it.type == PRDetector.PRType.WEIGHT }
        assertNull(weightPR)
    }

    @Test
    fun `returns empty for uncompleted sets`() {
        val current = listOf(makeSet(completed = false))
        val prs = detector.detectPRs(1L, "Bench", current, emptyList(), 1L)
        assertTrue(prs.isEmpty())
    }

    @Test
    fun `detects first ever PR for new exercise`() {
        val current = listOf(makeSet(weight = 50.0, reps = 10))
        val prs = detector.detectPRs(1L, "Bench", current, emptyList(), 1L)
        // Should detect weight, rep, e1RM, and volume PRs
        assertTrue(prs.isNotEmpty())
        assertTrue(prs.any { it.type == PRDetector.PRType.WEIGHT })
        assertTrue(prs.any { it.type == PRDetector.PRType.VOLUME })
    }
}
