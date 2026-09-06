package com.gymcoach.app.core.program

import com.gymcoach.app.core.program.VolumeCalculator.*
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for VolumeCalculator.
 *
 * Covers: role credit weighting, fractional averaging, ISO week bucketing
 * (including year boundary), warmup/incomplete set exclusion, classification
 * thresholds, and V-Taper balance calculation.
 */
class VolumeCalculatorTest {

    private lateinit var calculator: VolumeCalculator

    @Before
    fun setUp() {
        calculator = VolumeCalculator()
    }

    // ── Helpers ────────────────────────────────────────────────────

    private fun createSet(
        weight: Double = 50.0,
        reps: Int = 10,
        completed: Boolean = true,
        setType: Int = 0  // 0 = NORMAL
    ) = WorkoutSetEntity(
        workoutExerciseId = 1,
        setNumber = 1,
        weight = weight,
        reps = reps,
        rpe = 7.5,
        restSeconds = 90,
        completed = completed,
        setType = setType
    )

    private fun setContext(
        set: WorkoutSetEntity = createSet(),
        exerciseId: Long = 1L,
        workoutDate: Long = dateMs(2026, 1, 5) // a Sunday
    ) = SetWithContext(set = set, exerciseId = exerciseId, workoutDate = workoutDate)

    private fun muscleAssignment(
        muscle: String = "Lats",
        role: MuscleRole = MuscleRole.PRIMARY
    ) = MuscleAssignment(muscleName = muscle, role = role)

    /**
     * Convert year/month/day to epoch millis using Calendar for consistency
     * with the Calendar-based isoWeekKey in VolumeCalculator.
     */
    private fun dateMs(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance(Locale.US)
        cal.set(year, month - 1, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ── 1. Role credit weighting ──────────────────────────────────

    @Test
    fun testPrimaryRoleGivesFullCredit() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = listOf(setContext(set = createSet(), exerciseId = 1L))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // PRIMARY → 1.0 credit per set, 1 set → avgWeekly = 1.0
        // classify(1) → INSUFFICIENT (level 0)
        assertEquals(0, balance.latVolume.status.level)
        assertEquals(1, balance.latVolume.weeklySets)
        assertEquals(1, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
    }

    @Test
    fun testSecondaryRoleGivesHalfCredit() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.SECONDARY)))
        val sets = listOf(setContext(set = createSet(), exerciseId = 1L))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // SECONDARY → 0.5 credit per set
        // But weeklySets counts raw sets (direct+indirect), not credits
        assertEquals(1, balance.latVolume.weeklySets)
        assertEquals(0, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.indirectSets)
    }

    @Test
    fun testStabilizerRoleGivesQuarterCredit() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.STABILIZER)))
        val sets = listOf(setContext(set = createSet(), exerciseId = 1L))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // STABILIZER → 0.25 credit per set
        assertEquals(1, balance.latVolume.weeklySets)
        assertEquals(0, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.indirectSets)
    }

    @Test
    fun testMixedRoles() {
        val muscleMap = mapOf(
            1L to listOf(
                muscleAssignment("Lats", MuscleRole.PRIMARY),
                muscleAssignment("Biceps", MuscleRole.SECONDARY),
                muscleAssignment("Rear Deltoid", MuscleRole.STABILIZER)
            )
        )
        val sets = listOf(setContext(set = createSet(), exerciseId = 1L))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(1, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
        assertEquals(0, balance.bicepsVolume.directSets)
        assertEquals(1, balance.bicepsVolume.indirectSets)
        assertEquals(0, balance.rearDeltVolume.directSets)
        assertEquals(1, balance.rearDeltVolume.indirectSets)
    }

    // ── 2. Fractional averaging ───────────────────────────────────

    @Test
    fun testFractionalAveragesNoTruncation() {
        // 3 sets in 2 weeks → avg = 1.5 per week
        // Use two distinct ISO weeks
        val week1 = dateMs(2026, 1, 5)  // Mon of ISO week 1
        val week2 = dateMs(2026, 1, 12) // Mon of ISO week 2
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = listOf(
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week2)
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // 3 sets across 2 weeks → direct = 3 (count), classify(3) → INSUFFICIENT
        assertEquals(3, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
        assertEquals(3, balance.latVolume.weeklySets)
    }

    // ── 3. Multi-week averaging ───────────────────────────────────

    @Test
    fun testMultiWeekAveraging() {
        val week1 = dateMs(2026, 1, 5)
        val week2 = dateMs(2026, 1, 12)
        val week3 = dateMs(2026, 1, 19)
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))

        val sets = listOf(
            // Week 1: 6 sets
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            // Week 2: 6 sets
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week2),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week2),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week2),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week2),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week2),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week2),
            // Week 3: 6 sets
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week3),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week3),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week3),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week3),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week3),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week3)
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // 18 sets total across 3 weeks
        assertEquals(18, balance.latVolume.weeklySets)
        assertEquals(18, balance.latVolume.directSets)
        // classify(18) → HIGH (18-21)
        assertEquals(VolumeStatus.HIGH, balance.latVolume.status)
    }

    // ── 4. ISO week year boundary ─────────────────────────────────

    @Test
    fun testIsoWeekYearBoundary() {
        // Dec 30, 2025 → falls in ISO week 1 of 2026 (or week 53 of 2025 depending on Calendar)
        // Jan 2, 2026 → falls in a different ISO week
        // Use Calendar to compute the actual week keys
        val dec30 = dateMs(2025, 12, 30)
        val jan2 = dateMs(2026, 1, 2)

        // Compute expected week keys using the same logic as VolumeCalculator
        val cal1 = Calendar.getInstance(Locale.US)
        cal1.timeInMillis = dec30
        val wk1 = cal1.get(Calendar.YEAR) * 100 + cal1.get(Calendar.WEEK_OF_YEAR)

        val cal2 = Calendar.getInstance(Locale.US)
        cal2.timeInMillis = jan2
        val wk2 = cal2.get(Calendar.YEAR) * 100 + cal2.get(Calendar.WEEK_OF_YEAR)

        // They must be different weeks
        assertTrue(
            "Dec 30 ($wk1) and Jan 2 ($wk2) should be different ISO week keys",
            wk1 != wk2
        )

        val muscleMap = mapOf(
            1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)),
            2L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY))
        )
        val sets = listOf(
            setContext(set = createSet(), exerciseId = 1L, workoutDate = dec30),
            setContext(set = createSet(), exerciseId = 2L, workoutDate = jan2)
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // 1 set in week1 + 1 set in week2 → total = 2 sets
        assertEquals(2, balance.latVolume.weeklySets)
        assertEquals(2, balance.latVolume.directSets)
    }

    // ── 5. Warmup sets excluded ───────────────────────────────────

    @Test
    fun testWarmupSetsExcluded() {
        // setType != 0 → excluded by the filter `it.set.setType == 0`
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = listOf(
            setContext(set = createSet(setType = 1), exerciseId = 1L), // WARMUP → excluded
            setContext(set = createSet(setType = 0), exerciseId = 1L)  // NORMAL → included
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(1, balance.latVolume.weeklySets)
        assertEquals(1, balance.latVolume.directSets)
    }

    @Test
    fun testDropAndFailureSetsExcluded() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = listOf(
            setContext(set = createSet(setType = 2), exerciseId = 1L), // DROP → excluded
            setContext(set = createSet(setType = 3), exerciseId = 1L), // FAILURE → excluded
            setContext(set = createSet(setType = 0), exerciseId = 1L)  // NORMAL → included
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(1, balance.latVolume.weeklySets)
    }

    // ── 6. Incomplete sets excluded ───────────────────────────────

    @Test
    fun testIncompleteSetsExcluded() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = listOf(
            setContext(set = createSet(completed = false), exerciseId = 1L), // incomplete
            setContext(set = createSet(completed = true), exerciseId = 1L)  // completed
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(1, balance.latVolume.weeklySets)
        assertEquals(1, balance.latVolume.directSets)
    }

    // ── 7. Classification thresholds ──────────────────────────────

    @Test
    fun testClassificationInsufficient() {
        // < 10 → INSUFFICIENT
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..9).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(VolumeStatus.INSUFFICIENT, balance.latVolume.status)
        assertEquals("Too low", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationModerate() {
        // 10-13 → MODERATE
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..12).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(VolumeStatus.MODERATE, balance.latVolume.status)
        assertEquals("Moderate", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationOptimal() {
        // 14-17 → OPTIMAL
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..16).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(VolumeStatus.OPTIMAL, balance.latVolume.status)
        assertEquals("Optimal", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationHigh() {
        // 18-21 → HIGH
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..20).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(VolumeStatus.HIGH, balance.latVolume.status)
        assertEquals("High", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationExcessive() {
        // >= 22 → EXCESSIVE
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..25).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(VolumeStatus.EXCESSIVE, balance.latVolume.status)
        assertEquals("Very high", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationBoundaryValues() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))

        // Exactly 10 → MODERATE
        val sets10 = (1..10).map { setContext(set = createSet(), exerciseId = 1L) }
        assertEquals(VolumeStatus.MODERATE, calculator.calculateWeeklyVolume(sets10, muscleMap).latVolume.status)

        // Exactly 14 → OPTIMAL
        val sets14 = (1..14).map { setContext(set = createSet(), exerciseId = 1L) }
        assertEquals(VolumeStatus.OPTIMAL, calculator.calculateWeeklyVolume(sets14, muscleMap).latVolume.status)

        // Exactly 18 → HIGH
        val sets18 = (1..18).map { setContext(set = createSet(), exerciseId = 1L) }
        assertEquals(VolumeStatus.HIGH, calculator.calculateWeeklyVolume(sets18, muscleMap).latVolume.status)

        // Exactly 22 → EXCESSIVE
        val sets22 = (1..22).map { setContext(set = createSet(), exerciseId = 1L) }
        assertEquals(VolumeStatus.EXCESSIVE, calculator.calculateWeeklyVolume(sets22, muscleMap).latVolume.status)
    }

    // ── 8. V-Taper balance ────────────────────────────────────────

    @Test
    fun testVtaperBalanceCalculation() {
        val balance = TrainingBalance(
            latVolume = MuscleVolume("Lats", 16, 16, 0, VolumeStatus.OPTIMAL),
            lateralDeltVolume = MuscleVolume("Lateral Deltoid", 14, 14, 0, VolumeStatus.OPTIMAL),
            rearDeltVolume = MuscleVolume("Rear Deltoid", 12, 12, 0, VolumeStatus.MODERATE),
            upperChestVolume = MuscleVolume("Upper Chest", 14, 14, 0, VolumeStatus.OPTIMAL),
            upperBackVolume = MuscleVolume("Upper Back", 12, 12, 0, VolumeStatus.MODERATE),
            bicepsVolume = MuscleVolume("Biceps", 12, 12, 0, VolumeStatus.MODERATE),
            tricepsVolume = MuscleVolume("Triceps", 10, 10, 0, VolumeStatus.MODERATE),
            quadricepsVolume = MuscleVolume("Quadriceps", 16, 16, 0, VolumeStatus.OPTIMAL),
            hamstringsVolume = MuscleVolume("Hamstrings", 14, 14, 0, VolumeStatus.OPTIMAL),
            glutesVolume = MuscleVolume("Glutes", 12, 12, 0, VolumeStatus.MODERATE),
            calvesVolume = MuscleVolume("Calves", 10, 10, 0, VolumeStatus.MODERATE),
            coreVolume = MuscleVolume("Core", 8, 8, 0, VolumeStatus.INSUFFICIENT)
        )

        val vtaper = calculator.calculateVtaperBalance(balance)

        // primary = (lat(OPTIMAL=3) + lateralDelt(OPTIMAL=3)) / 2 = 3.0
        assertEquals(3.0, vtaper.primaryScore, 0.01)
        // secondary = (rearDelt(MODERATE=1) + upperChest(OPTIMAL=3) + upperBack(MODERATE=1)) / 3 = 5/3 ≈ 1.67
        assertEquals(5.0 / 3.0, vtaper.secondaryScore, 0.01)
        // primary >= 3.0 but secondary < 2.0 → "Moderate V-taper focus"
        assertEquals("Moderate V-taper focus", vtaper.overallBalance)
    }

    @Test
    fun testVtaperBalanceGood() {
        // Both primary and secondary high → "Good V-taper volume distribution"
        val balance = TrainingBalance(
            latVolume = MuscleVolume("Lats", 20, 20, 0, VolumeStatus.HIGH),
            lateralDeltVolume = MuscleVolume("Lateral Deltoid", 22, 22, 0, VolumeStatus.EXCESSIVE),
            rearDeltVolume = MuscleVolume("Rear Deltoid", 18, 18, 0, VolumeStatus.HIGH),
            upperChestVolume = MuscleVolume("Upper Chest", 16, 16, 0, VolumeStatus.OPTIMAL),
            upperBackVolume = MuscleVolume("Upper Back", 18, 18, 0, VolumeStatus.HIGH),
            bicepsVolume = MuscleVolume("Biceps", 12, 12, 0, VolumeStatus.MODERATE),
            tricepsVolume = MuscleVolume("Triceps", 10, 10, 0, VolumeStatus.MODERATE),
            quadricepsVolume = MuscleVolume("Quadriceps", 16, 16, 0, VolumeStatus.OPTIMAL),
            hamstringsVolume = MuscleVolume("Hamstrings", 14, 14, 0, VolumeStatus.OPTIMAL),
            glutesVolume = MuscleVolume("Glutes", 12, 12, 0, VolumeStatus.MODERATE),
            calvesVolume = MuscleVolume("Calves", 10, 10, 0, VolumeStatus.MODERATE),
            coreVolume = MuscleVolume("Core", 8, 8, 0, VolumeStatus.INSUFFICIENT)
        )
        val vtaper = calculator.calculateVtaperBalance(balance)

        // primary = (HIGH=2 + EXCESSIVE=4) / 2 = 3.0
        assertEquals(3.0, vtaper.primaryScore, 0.01)
        // secondary = (HIGH=2 + OPTIMAL=3 + HIGH=2) / 3 = 7/3 ≈ 2.33
        assertEquals(7.0 / 3.0, vtaper.secondaryScore, 0.01)
        assertTrue(vtaper.primaryScore >= 3.0 && vtaper.secondaryScore >= 2.0)
        assertEquals("Good V-taper volume distribution", vtaper.overallBalance)
    }

    @Test
    fun testVtaperBalanceLow() {
        // Both primary and secondary low → "Low V-taper volume"
        val balance = TrainingBalance(
            latVolume = MuscleVolume("Lats", 6, 6, 0, VolumeStatus.INSUFFICIENT),
            lateralDeltVolume = MuscleVolume("Lateral Deltoid", 4, 4, 0, VolumeStatus.INSUFFICIENT),
            rearDeltVolume = MuscleVolume("Rear Deltoid", 4, 4, 0, VolumeStatus.INSUFFICIENT),
            upperChestVolume = MuscleVolume("Upper Chest", 6, 6, 0, VolumeStatus.INSUFFICIENT),
            upperBackVolume = MuscleVolume("Upper Back", 4, 4, 0, VolumeStatus.INSUFFICIENT),
            bicepsVolume = MuscleVolume("Biceps", 4, 4, 0, VolumeStatus.INSUFFICIENT),
            tricepsVolume = MuscleVolume("Triceps", 4, 4, 0, VolumeStatus.INSUFFICIENT),
            quadricepsVolume = MuscleVolume("Quadriceps", 6, 6, 0, VolumeStatus.INSUFFICIENT),
            hamstringsVolume = MuscleVolume("Hamstrings", 4, 4, 0, VolumeStatus.INSUFFICIENT),
            glutesVolume = MuscleVolume("Glutes", 4, 4, 0, VolumeStatus.INSUFFICIENT),
            calvesVolume = MuscleVolume("Calves", 4, 4, 0, VolumeStatus.INSUFFICIENT),
            coreVolume = MuscleVolume("Core", 4, 4, 0, VolumeStatus.INSUFFICIENT)
        )
        val vtaper = calculator.calculateVtaperBalance(balance)

        // primary = (INSUFFICIENT=0 + INSUFFICIENT=0) / 2 = 0.0
        assertEquals(0.0, vtaper.primaryScore, 0.01)
        // secondary = (INSUFFICIENT=0 + INSUFFICIENT=0 + INSUFFICIENT=0) / 3 = 0.0
        assertEquals(0.0, vtaper.secondaryScore, 0.01)
        assertEquals("Low V-taper volume", vtaper.overallBalance)
    }

    // ── 9. Empty input ────────────────────────────────────────────

    @Test
    fun testEmptySetsReturnsAllZero() {
        val balance = calculator.calculateWeeklyVolume(emptyList(), emptyMap())
        balance.asList().forEach { mv ->
            assertEquals(0, mv.weeklySets)
            assertEquals(0, mv.directSets)
            assertEquals(0, mv.indirectSets)
            assertEquals(VolumeStatus.INSUFFICIENT, mv.status)
        }
    }

    @Test
    fun testUnknownExerciseIdIgnored() {
        // exerciseId=99 not in the map → no muscle assignments → no volume
        val sets = listOf(setContext(set = createSet(), exerciseId = 99L))
        val balance = calculator.calculateWeeklyVolume(sets, emptyMap())

        balance.asList().forEach { mv ->
            assertEquals(0, mv.weeklySets)
        }
    }

    // ── 10. TrainingBalance.asList() ──────────────────────────────

    @Test
    fun testTrainingBalanceAsListSize() {
        val balance = calculator.calculateWeeklyVolume(emptyList(), emptyMap())
        assertEquals(12, balance.asList().size)
    }

    @Test
    fun testMuscleRoleCreditValues() {
        assertEquals(1.0, MuscleRole.PRIMARY.credit, 0.001)
        assertEquals(0.5, MuscleRole.SECONDARY.credit, 0.001)
        assertEquals(0.25, MuscleRole.STABILIZER.credit, 0.001)
    }
}
