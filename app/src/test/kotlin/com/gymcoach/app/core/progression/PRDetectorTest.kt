package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioral regression suite for PR detection: Epley e1RM math (with the
 * 12-rep cap), per-type PR gates against existing records, and the
 * completed-normal-set filter that keeps warmup junk out of the record books.
 */
class PRDetectorTest {

    private val detector = PRDetector()

    private fun set(
        weight: Double,
        reps: Int,
        completed: Boolean = true,
        setType: Int = 0,
        number: Int = 1
    ) = WorkoutSetEntity(
        id = number.toLong(),
        workoutExerciseId = 1L,
        setNumber = number,
        weight = weight,
        reps = reps,
        rpe = 7.0,
        restSeconds = 90,
        completed = completed,
        setType = setType
    )

    private fun pr(type: PRDetector.PRType, value: Double) = PRDetector.PersonalRecord(
        exerciseId = 1L,
        exerciseName = "Bench",
        type = type,
        value = value,
        details = "",
        date = java.time.Instant.EPOCH,
        workoutId = 0L
    )

    // ---- Epley e1RM ----

    @Test
    fun `epley formula at five reps`() {
        assertEquals(100.0 * (1 + 5.0 / 30.0), detector.calculateEstimated1RM(100.0, 5), 1e-9)
    }

    @Test
    fun `epley caps at twelve reps`() {
        assertEquals(detector.calculateEstimated1RM(100.0, 12), detector.calculateEstimated1RM(100.0, 25), 1e-9)
        assertEquals(140.0, detector.calculateEstimated1RM(100.0, 12), 1e-9)
    }

    @Test
    fun `epley rejects non-positive inputs`() {
        assertEquals(0.0, detector.calculateEstimated1RM(0.0, 5), 1e-9)
        assertEquals(0.0, detector.calculateEstimated1RM(100.0, 0), 1e-9)
        assertEquals(0.0, detector.calculateEstimated1RM(-10.0, 3), 1e-9)
    }

    // ---- Weight PR gate ----

    @Test
    fun `first weighted session sets a weight pr`() {
        val prs = detector.detectPRs(
            exerciseId = 1L, exerciseName = "Press",
            currentSets = listOf(set(weight = 40.0, reps = 8)),
            existingPRs = emptyList(), workoutId = 7L
        )
        assertTrue(prs.any { it.type == PRDetector.PRType.WEIGHT && it.value == 40.0 })
    }

    @Test
    fun `session below existing max fires no weight pr`() {
        val prs = detector.detectPRs(
            exerciseId = 1L, exerciseName = "Press",
            currentSets = listOf(set(weight = 35.0, reps = 8)),
            existingPRs = listOf(pr(PRDetector.PRType.WEIGHT, 40.0)),
            workoutId = 7L
        )
        assertTrue(prs.none { it.type == PRDetector.PRType.WEIGHT })
    }

    @Test
    fun `bodyweight-only session never claims a zero-weight pr`() {
        val prs = detector.detectPRs(
            exerciseId = 1L, exerciseName = "Push-up",
            currentSets = listOf(set(weight = 0.0, reps = 20)),
            existingPRs = emptyList(), workoutId = 7L
        )
        assertTrue(prs.none { it.type == PRDetector.PRType.WEIGHT })
        assertTrue(prs.any { it.type == PRDetector.PRType.REP && it.value == 20.0 })
    }

    // ---- Rep + volume PRs ----

    @Test
    fun `rep pr requires beating prior best`() {
        val base = listOf(set(weight = 30.0, reps = 10))
        withMore(listOf(pr(PRDetector.PRType.REP, 10.0))).let {
            val prs = detector.detectPRs(1L, "Row", base, it, 7L)
            assertTrue(prs.none { r -> r.type == PRDetector.PRType.REP })
        }
        val better = listOf(set(weight = 30.0, reps = 12, number = 2))
        val prs2 = detector.detectPRs(1L, "Row", better, listOf(pr(PRDetector.PRType.REP, 10.0)), 7L)
        assertTrue(prs2.any { it.type == PRDetector.PRType.REP && it.value == 12.0 })
    }

    @Test
    fun `volume pr sums across session sets`() {
        val sets = listOf(
            set(weight = 50.0, reps = 10, number = 1),
            set(weight = 50.0, reps = 8, number = 2)
        )
        val prs = detector.detectPRs(1L, "Row", sets, emptyList(), 7L)
        assertTrue(prs.any { it.type == PRDetector.PRType.VOLUME && it.value == 900.0 })
    }

    // ---- Filtering ----

    @Test
    fun `warmup and incomplete sets are excluded from records`() {
        val sets = listOf(
            set(weight = 60.0, reps = 15, completed = false, number = 1),
            set(weight = 55.0, reps = 15, setType = 1, number = 2),
            set(weight = 40.0, reps = 8, number = 3)
        )
        val prs = detector.detectPRs(1L, "Press", sets, emptyList(), 7L)
        assertTrue(prs.none { it.type == PRDetector.PRType.WEIGHT && it.value > 40.0 })
    }

    private fun withMore(existing: List<PRDetector.PersonalRecord>) = existing

    @Test
    fun `no normal sets means no prs`() {
        val prs = detector.detectPRs(
            1L, "Press",
            listOf(set(weight = 40.0, reps = 8, completed = false)),
            emptyList(), 7L
        )
        assertTrue(prs.isEmpty())
    }
}
