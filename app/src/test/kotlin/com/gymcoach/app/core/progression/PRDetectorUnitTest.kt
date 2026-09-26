package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test
    fun `detectPRs ignores performance equal to existing PRs requiring strictly greater value`() {
        val now = Instant.now()
        val e1rm100x10 = detector.calculateEstimated1RM(100.0, 10)
        val existingPRs = listOf(
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.WEIGHT, 100.0, "100kg", now, 1L),
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.REP, 10.0, "10 reps", now, 1L),
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.ESTIMATED_1RM, e1rm100x10, "133.33kg", now, 1L),
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.VOLUME, 2000.0, "2000kg", now, 1L)
        )

        // Exact match with existing PRs (100kg for 10 reps = 1000kg volume, 133.33 e1rm)
        val currentSets = listOf(
            createSet(weight = 100.0, reps = 10, completed = true, setType = 0)
        )

        val prs = detector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Squat",
            currentSets = currentSets,
            existingPRs = existingPRs,
            workoutId = 2L
        )

        assertTrue("No PR should be detected on equal or lesser performance", prs.isEmpty())
    }

    @Test
    fun `detectPRs handles intra-session divergent set winners across weight reps and e1RM`() {
        // Set 1: Heavy low-rep: 120kg x 2 -> e1RM = 120 * (1 + 2/30) = 128kg
        // Set 2: Moderate high-rep: 100kg x 12 -> e1RM = 100 * (1 + 12/30) = 140kg
        val currentSets = listOf(
            createSet(weight = 120.0, reps = 2, completed = true, setType = 0),
            createSet(weight = 100.0, reps = 12, completed = true, setType = 0)
        )

        val prs = detector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            currentSets = currentSets,
            existingPRs = emptyList(),
            workoutId = 5L
        )

        assertEquals(4, prs.size)

        // Weight PR won by Set 1 (120kg)
        val weightPR = prs.first { it.type == PRDetector.PRType.WEIGHT }
        assertEquals(120.0, weightPR.value, 0.01)

        // Rep PR won by Set 2 (12 reps)
        val repPR = prs.first { it.type == PRDetector.PRType.REP }
        assertEquals(12.0, repPR.value, 0.01)

        // Estimated 1RM PR won by Set 2 (140kg, exceeding Set 1's 128kg)
        val e1rmPR = prs.first { it.type == PRDetector.PRType.ESTIMATED_1RM }
        assertEquals(140.0, e1rmPR.value, 0.01)

        // Volume PR sums both sets: 120*2 + 100*12 = 1440kg
        val volumePR = prs.first { it.type == PRDetector.PRType.VOLUME }
        assertEquals(1440.0, volumePR.value, 0.01)
    }

    @Test
    fun `detectPRs detects micro-load fractional improvements over existing PRs`() {
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
            )
        )

        val currentSets = listOf(
            createSet(weight = 100.25, reps = 5, completed = true, setType = 0)
        )

        val prs = detector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            currentSets = currentSets,
            existingPRs = existingPRs,
            workoutId = 2L
        )

        val weightPR = prs.firstOrNull { it.type == PRDetector.PRType.WEIGHT }
        assertNotNull("Micro-load improvement must trigger a WEIGHT PR", weightPR)
        assertEquals(100.25, weightPR!!.value, 0.001)
    }

    @Test
    fun `detectPRs ignores high volume warmup and incomplete sets while keeping completed normal PR`() {
        val mixedSets = listOf(
            createSet(weight = 50.0, reps = 50, completed = true, setType = 1),
            createSet(weight = 110.0, reps = 3, completed = true, setType = 0),
            createSet(weight = 120.0, reps = 10, completed = false, setType = 0)
        )

        val prs = detector.detectPRs(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            currentSets = mixedSets,
            existingPRs = emptyList(),
            workoutId = 3L
        )

        val weightPR = prs.first { it.type == PRDetector.PRType.WEIGHT }
        assertEquals(110.0, weightPR.value, 0.001)

        val repPR = prs.first { it.type == PRDetector.PRType.REP }
        assertEquals(3.0, repPR.value, 0.001)

        val volumePR = prs.first { it.type == PRDetector.PRType.VOLUME }
        assertEquals(330.0, volumePR.value, 0.001)
    }

    @Test
    fun `detectPRs detects bodyweight e1RM proxy with one point five multiplier`() {
        val bwSets = listOf(
            createSet(weight = 0.0, reps = 20, completed = true, setType = 0),
            createSet(weight = 0.0, reps = 15, completed = true, setType = 0)
        )
        val existingBwPR = listOf(
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Pull Up", type = PRDetector.PRType.ESTIMATED_1RM,
                value = 25.0, details = "Bodyweight e1RM: 25.0kg", date = Instant.now(), workoutId = 1L
            )
        )

        // 20 reps * 1.5 = 30.0kg proxy e1RM > 25.0kg -> triggers PR
        val prs = detector.detectPRs(
            exerciseId = 1L, exerciseName = "Pull Up",
            currentSets = bwSets, existingPRs = existingBwPR, workoutId = 2L
        )

        val e1rmPR = prs.firstOrNull { it.type == PRDetector.PRType.ESTIMATED_1RM }
        assertNotNull("Bodyweight proxy e1RM PR should be detected", e1rmPR)
        assertEquals(30.0, e1rmPR!!.value, 0.001)
        assertEquals("Bodyweight e1RM: 30.0kg", e1rmPR.details)
    }

    @Test
    fun `detectPRs returns empty list when performance matches previous PRs without exceeding`() {
        val now = Instant.now()
        val existingPRs = listOf(
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Squat", type = PRDetector.PRType.WEIGHT,
                value = 100.0, details = "100.0kg lifted", date = now, workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Squat", type = PRDetector.PRType.REP,
                value = 10.0, details = "10 reps at 100kg", date = now, workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Squat", type = PRDetector.PRType.VOLUME,
                value = 1000.0, details = "Volume: 1000kg", date = now, workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Squat", type = PRDetector.PRType.ESTIMATED_1RM,
                value = detector.calculateEstimated1RM(100.0, 10), details = "e1RM: 133.3kg", date = now, workoutId = 1L
            )
        )

        // Exactly identical performance: 100kg x 10 reps = 1000kg volume, 133.333kg e1RM
        val matchingSets = listOf(
            createSet(weight = 100.0, reps = 10, completed = true, setType = 0)
        )

        val prs = detector.detectPRs(
            exerciseId = 1L, exerciseName = "Squat",
            currentSets = matchingSets, existingPRs = existingPRs, workoutId = 2L
        )

        assertTrue("Tying an existing PR must strictly not trigger a new PR", prs.isEmpty())
    }

    @Test
    fun `detectPRs detects all 4 PR types simultaneously when sets diverge across heavy and high-rep`() {
        // Set 1: Heavy low-rep (130kg x 2) -> max weight 130kg
        // Set 2: Back-off AMRAP (105kg x 12) -> max reps 12, e1RM = 105 * (1 + 12/30) = 147.0kg
        // Total Volume = 130 * 2 + 105 * 12 = 260 + 1260 = 1520kg
        val currentSets = listOf(
            createSet(weight = 130.0, reps = 2, completed = true, setType = 0),
            createSet(weight = 105.0, reps = 12, completed = true, setType = 0)
        )
        val existingPRs = listOf(
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Bench Press", type = PRDetector.PRType.WEIGHT,
                value = 125.0, details = "125kg", date = Instant.now(), workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Bench Press", type = PRDetector.PRType.REP,
                value = 8.0, details = "8 reps", date = Instant.now(), workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Bench Press", type = PRDetector.PRType.ESTIMATED_1RM,
                value = 140.0, details = "140kg", date = Instant.now(), workoutId = 1L
            ),
            PRDetector.PersonalRecord(
                exerciseId = 1L, exerciseName = "Bench Press", type = PRDetector.PRType.VOLUME,
                value = 1000.0, details = "1000kg", date = Instant.now(), workoutId = 1L
            )
        )

        val prs = detector.detectPRs(
            exerciseId = 1L, exerciseName = "Bench Press",
            currentSets = currentSets, existingPRs = existingPRs, workoutId = 5L
        )

        assertEquals(4, prs.size)
        val weightPR = prs.first { it.type == PRDetector.PRType.WEIGHT }
        assertEquals(130.0, weightPR.value, 0.001)

        val repPR = prs.first { it.type == PRDetector.PRType.REP }
        assertEquals(12.0, repPR.value, 0.001)

        val e1rmPR = prs.first { it.type == PRDetector.PRType.ESTIMATED_1RM }
        assertEquals(147.0, e1rmPR.value, 0.001)

        val volPR = prs.first { it.type == PRDetector.PRType.VOLUME }
        assertEquals(1520.0, volPR.value, 0.001)

        // Verify exercise and workout attribution
        assertTrue(prs.all { it.exerciseId == 1L && it.exerciseName == "Bench Press" && it.workoutId == 5L })
    }

    @Test
    fun `calculateEstimated1RM clamps formula cleanly across supra-12 repetition counts`() {
        val at12 = detector.calculateEstimated1RM(80.0, 12)
        val at15 = detector.calculateEstimated1RM(80.0, 15)
        val at30 = detector.calculateEstimated1RM(80.0, 30)

        // 80 * (1 + 12/30) = 80 * 1.4 = 112.0kg
        assertEquals(112.0, at12, 0.001)
        assertEquals(112.0, at15, 0.001)
        assertEquals(112.0, at30, 0.001)
    }

    @Test
    fun `testMultipleHistoricalPRsSameTypeTakesMaximum verifies comparison against historical ceiling`() {
        val now = Instant.now()
        // Historical log has multiple WEIGHT PRs and VOLUME PRs from older workouts
        val existingPRs = listOf(
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.WEIGHT, 100.0, "100kg", now.minusSeconds(7200), 1L),
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.WEIGHT, 120.0, "120kg", now.minusSeconds(3600), 2L),
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.WEIGHT, 110.0, "110kg", now.minusSeconds(1800), 3L),
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.VOLUME, 1500.0, "1500kg", now.minusSeconds(3600), 2L),
            PRDetector.PersonalRecord(1L, "Squat", PRDetector.PRType.VOLUME, 2500.0, "2500kg", now.minusSeconds(1800), 3L)
        )

        // Performance that beats 100kg and 110kg, but does NOT beat 120kg
        val subPeakSets = listOf(
            createSet(weight = 115.0, reps = 5, completed = true, setType = 0) // vol = 575kg
        )
        val subPeakPRs = detector.detectPRs(1L, "Squat", subPeakSets, existingPRs, 4L)
        assertTrue("Weight of 115kg must not beat historical max of 120kg", subPeakPRs.none { it.type == PRDetector.PRType.WEIGHT })
        assertTrue("Volume of 575kg must not beat historical max of 2500kg", subPeakPRs.none { it.type == PRDetector.PRType.VOLUME })

        // Performance that strictly exceeds the true historical maximum (122.5kg > 120kg)
        val peakSets = listOf(
            createSet(weight = 122.5, reps = 5, completed = true, setType = 0)
        )
        val peakPRs = detector.detectPRs(1L, "Squat", peakSets, existingPRs, 5L)
        val weightPR = peakPRs.firstOrNull { it.type == PRDetector.PRType.WEIGHT }
        assertNotNull("Exceeding 120kg must trigger WEIGHT PR", weightPR)
        assertEquals(122.5, weightPR!!.value, 0.001)
    }

    @Test
    fun `testWeightedSetsSuppressBodyweightProxyE1RM ensures weighted sets dominate e1RM calculation`() {
        // Session with weighted dip and a bodyweight burnout dip set
        // Set 1: Weighted 20kg x 6 -> e1RM = 20 * (1 + 6/30) = 24.0kg (> 0.0)
        // Set 2: Bodyweight 0kg x 25 -> proxy would be 25 * 1.5 = 37.5kg, but bestE1RM > 0.0 must suppress it!
        val mixedSets = listOf(
            createSet(weight = 20.0, reps = 6, completed = true, setType = 0),
            createSet(weight = 0.0, reps = 25, completed = true, setType = 0)
        )

        val prs = detector.detectPRs(
            exerciseId = 10L,
            exerciseName = "Dips",
            currentSets = mixedSets,
            existingPRs = emptyList(),
            workoutId = 8L
        )

        // Must detect normal e1RM (24.0kg), NOT bodyweight proxy (37.5kg)
        val e1rmPRs = prs.filter { it.type == PRDetector.PRType.ESTIMATED_1RM }
        assertEquals("Must have exactly 1 ESTIMATED_1RM PR", 1, e1rmPRs.size)
        assertEquals(24.0, e1rmPRs.first().value, 0.001)
        assertEquals("e1RM: 24.0kg", e1rmPRs.first().details)
    }

    @Test
    fun `testDetailsFormattingAcrossAllPRTypes validates exact presentation strings`() {
        val sets = listOf(
            createSet(weight = 102.5, reps = 8, completed = true, setType = 0)
        )
        // Best weight: 102.5kg
        // Best reps: 8 reps at 102.5kg
        // Volume: 102.5 * 8 = 820kg
        // e1RM: 102.5 * (1 + 8/30) = 102.5 * 1.266667 = 129.8333 -> "e1RM: 129.8kg"
        val prs = detector.detectPRs(
            exerciseId = 2L,
            exerciseName = "Incline Bench",
            currentSets = sets,
            existingPRs = emptyList(),
            workoutId = 12L
        )

        val weightPR = prs.first { it.type == PRDetector.PRType.WEIGHT }
        val repPR = prs.first { it.type == PRDetector.PRType.REP }
        val volumePR = prs.first { it.type == PRDetector.PRType.VOLUME }
        val e1rmPR = prs.first { it.type == PRDetector.PRType.ESTIMATED_1RM }

        assertEquals("102.5kg lifted", weightPR.details)
        assertEquals("8 reps at 102.5kg", repPR.details)
        assertEquals("Volume: 820kg", volumePR.details)
        assertEquals("e1RM: 129.8kg", e1rmPR.details)

        // Test bodyweight detail formatting separately
        val bwSets = listOf(createSet(weight = 0.0, reps = 15, completed = true, setType = 0))
        val bwPRs = detector.detectPRs(
            exerciseId = 3L,
            exerciseName = "Push Up",
            currentSets = bwSets,
            existingPRs = emptyList(),
            workoutId = 13L
        )
        val bwE1rmPR = bwPRs.first { it.type == PRDetector.PRType.ESTIMATED_1RM }
        assertEquals("Bodyweight e1RM: 22.5kg", bwE1rmPR.details)
    }

    @Test
    fun `testRepClampingExactTransitionAt12RepsInE1RMFormula verifies precision boundary`() {
        val load = 100.0
        // 11 reps: 100 * (1 + 11/30) = 136.666666...
        val at11 = detector.calculateEstimated1RM(load, 11)
        assertEquals(136.667, at11, 0.001)

        // 12 reps: 100 * (1 + 12/30) = 140.0
        val at12 = detector.calculateEstimated1RM(load, 12)
        assertEquals(140.0, at12, 0.001)

        // 13 reps: clamped to 12 -> exactly 140.0
        val at13 = detector.calculateEstimated1RM(load, 13)
        assertEquals(140.0, at13, 0.001)

        // 50 reps: clamped to 12 -> exactly 140.0
        val at50 = detector.calculateEstimated1RM(load, 50)
        assertEquals(140.0, at50, 0.001)

        // Exact comparison
        assertEquals(at12, at13, 0.0)
        assertEquals(at12, at50, 0.0)
        assertTrue(at12 > at11)
    }

    @Test
    fun `detectPRs detects bodyweight e1RM PR across multi-set workout when all normal sets are bodyweight`() {
        val currentSets = listOf(
            createSet(weight = 0.0, reps = 15, completed = true, setType = 0),
            createSet(weight = 0.0, reps = 25, completed = true, setType = 0),
            createSet(weight = 0.0, reps = 20, completed = true, setType = 0)
        )
        val prs = detector.detectPRs(
            exerciseId = 6L,
            exerciseName = "Pull Up",
            currentSets = currentSets,
            existingPRs = emptyList(),
            workoutId = 20L
        )

        val e1rmPR = prs.firstOrNull { it.type == PRDetector.PRType.ESTIMATED_1RM }
        assertNotNull("Should detect bodyweight estimated 1RM from highest rep set", e1rmPR)
        assertEquals(37.5, e1rmPR!!.value, 0.001) // 25 reps * 1.5 proxy
        val repPR = prs.firstOrNull { it.type == PRDetector.PRType.REP }
        assertNotNull("Should detect rep PR", repPR)
        assertEquals(25.0, repPR!!.value, 0.001)
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
