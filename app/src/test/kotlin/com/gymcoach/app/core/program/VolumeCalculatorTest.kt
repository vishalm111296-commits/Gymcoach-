package com.gymcoach.app.core.program

import com.gymcoach.app.core.program.VolumeCalculator.*
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for VolumeCalculator.
 *
 * Covers: role credit weighting (PRIMARY 1.0 / SECONDARY 0.5 / STABILIZER 0.25),
 * per-ISO-week averaging into weeklyVolume, ISO week bucketing (including the
 * New-Year 2026-W01 boundary), warmup/incomplete set exclusion, per-week evidence
 * band classification (10/14/18/22), and V-Taper balance calculation.
 *
 * Semantics: MuscleVolume.weeklyVolume is the weighted credit average per observed
 * ISO week — NOT the raw total. All classification thresholds are per-week bands.
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
        workoutDate: Long = dateMs(2026, 1, 5) // the Monday of ISO week 2026-W02
    ) = SetWithContext(set = set, exerciseId = exerciseId, workoutDate = workoutDate)

    private fun muscleAssignment(
        muscle: String = "Lats",
        role: MuscleRole = MuscleRole.PRIMARY
    ) = MuscleAssignment(muscleName = muscle, role = role)

    /**
     * Convert year/month/day to epoch millis at noon UTC.
     * Noon UTC keeps the calendar date identical after VolumeCalculator buckets
     * the instant at UTC, independent of the host time zone.
     *
     * java.time (LocalDate/ZoneOffset) mirrors the production isoWeekKey
     * implementation; the tests use no legacy time API.
     */
    private fun dateMs(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atTime(12, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

    // ── 1. Role credit weighting ──────────────────────────────────

    @Test
    fun testPrimaryRoleGivesFullCredit() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = listOf(setContext(set = createSet(), exerciseId = 1L))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // PRIMARY → 1.0 weighted credit per set; 1 set in 1 week → weeklyVolume 1.0
        assertEquals(1.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(1, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
        // 1.0/week < 10 → INSUFFICIENT (level 0)
        assertEquals(VolumeStatus.INSUFFICIENT, balance.latVolume.status)
        assertEquals(0, balance.latVolume.status.level)
    }

    @Test
    fun testSecondaryRoleGivesHalfCredit() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.SECONDARY)))
        val sets = listOf(setContext(set = createSet(), exerciseId = 1L))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // SECONDARY → 0.5 weighted credits per set
        assertEquals(0.5, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(0, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.indirectSets)
        assertEquals(VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    @Test
    fun testStabilizerRoleGivesQuarterCredit() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.STABILIZER)))
        val sets = listOf(setContext(set = createSet(), exerciseId = 1L))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // STABILIZER → 0.25 weighted credits per set
        assertEquals(0.25, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(0, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.indirectSets)
        assertEquals(VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    @Test
    fun testMixedRolesAttributionOnOneSet() {
        // One squat set credits three muscles: quads PRIMARY, hamstrings SECONDARY,
        // core (abs stabilizer role) STABILIZER — each gets its own weighted credit.
        val muscleMap = mapOf(
            1L to listOf(
                muscleAssignment("Quadriceps", MuscleRole.PRIMARY),
                muscleAssignment("Hamstrings", MuscleRole.SECONDARY),
                muscleAssignment("Core", MuscleRole.STABILIZER)
            )
        )
        val sets = listOf(setContext(set = createSet(), exerciseId = 1L))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(1.0, balance.quadricepsVolume.weeklyVolume, 0.001)
        assertEquals(0.5, balance.hamstringsVolume.weeklyVolume, 0.001)
        assertEquals(0.25, balance.coreVolume.weeklyVolume, 0.001)

        assertEquals(1, balance.quadricepsVolume.directSets)
        assertEquals(0, balance.quadricepsVolume.indirectSets)
        assertEquals(0, balance.hamstringsVolume.directSets)
        assertEquals(1, balance.hamstringsVolume.indirectSets)
        assertEquals(0, balance.coreVolume.directSets)
        assertEquals(1, balance.coreVolume.indirectSets)
    }

    // ── 2. Fractional averaging ───────────────────────────────────

    @Test
    fun testFractionalAveragesNoTruncation() {
        // 3 PRIMARY sets across 2 distinct ISO weeks → weeklyVolume = 3/2 = 1.5
        val week1 = dateMs(2026, 1, 5)  // Mon of ISO week 2026-W02
        val week2 = dateMs(2026, 1, 12) // Mon of ISO week 2026-W03
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = listOf(
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week1),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = week2)
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(1.5, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(3, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
        // 1.5/week is still below the evidence band floor
        assertEquals(VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    // ── 3. Multi-week steady state (per-week band semantics) ──────

    @Test
    fun testMultiWeekSteadyVolumeIsPerWeekNotTotal() {
        // 6 PRIMARY sets/week for 3 weeks → weeklyVolume 6.0 (NOT the 18 raw total)
        val weeks = listOf(dateMs(2026, 1, 5), dateMs(2026, 1, 12), dateMs(2026, 1, 19))
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = weeks.flatMap { w ->
            (1..6).map { setContext(set = createSet(), exerciseId = 1L, workoutDate = w) }
        }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(6.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(18, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
        // Per-week band: 6.0/week < 10 → INSUFFICIENT (old code mis-read total 18 → HIGH)
        assertEquals(VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    @Test
    fun testSteadyFourteenPerWeekIsOptimal() {
        // 14 PRIMARY sets/week × 2 weeks → weeklyVolume 14.0 → OPTIMAL band floor
        val weeks = listOf(dateMs(2026, 1, 5), dateMs(2026, 1, 12))
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = weeks.flatMap { w ->
            (1..14).map { setContext(set = createSet(), exerciseId = 1L, workoutDate = w) }
        }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(14.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(28, balance.latVolume.directSets)
        assertEquals(VolumeStatus.OPTIMAL, balance.latVolume.status)
    }

    @Test
    fun testSteadyEighteenPerWeekIsHigh() {
        // 18 PRIMARY sets/week × 2 weeks → weeklyVolume 18.0 → HIGH band (18-21)
        val weeks = listOf(dateMs(2026, 1, 5), dateMs(2026, 1, 12))
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = weeks.flatMap { w ->
            (1..18).map { setContext(set = createSet(), exerciseId = 1L, workoutDate = w) }
        }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(18.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(VolumeStatus.HIGH, balance.latVolume.status)
    }

    @Test
    fun testSteadyTwentyTwoPerWeekIsExcessive() {
        // 22 PRIMARY sets/week × 2 weeks → weeklyVolume 22.0 → EXCESSIVE (>21)
        val weeks = listOf(dateMs(2026, 1, 5), dateMs(2026, 1, 12))
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = weeks.flatMap { w ->
            (1..22).map { setContext(set = createSet(), exerciseId = 1L, workoutDate = w) }
        }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(22.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(VolumeStatus.EXCESSIVE, balance.latVolume.status)
    }

    // ── 4. ISO week year boundary ─────────────────────────────────

    @Test
    fun testIsoWeekYearBoundarySharesOneBucket() {
        // ISO 2026-W01 runs Mon 2025-12-29 .. Sun 2026-01-04. Both dates below are
        // in the SAME ISO week (2026-W01) even though they span calendar years.
        val weekStart = dateMs(2025, 12, 29) // Monday
        val sameWeek = dateMs(2026, 1, 1)    // Thursday
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = listOf(
            setContext(set = createSet(), exerciseId = 1L, workoutDate = weekStart),
            setContext(set = createSet(), exerciseId = 1L, workoutDate = sameWeek)
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        // One bucket (2 sets, 1 week) → weeklyVolume 2.0, no averaging dilution
        assertEquals(2.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(2, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
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
        assertEquals(1.0, balance.latVolume.weeklyVolume, 0.001)
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
        assertEquals(1.0, balance.latVolume.weeklyVolume, 0.001)
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
        assertEquals(1.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(1, balance.latVolume.directSets)
    }

    // ── 7. Classification thresholds (per-week bands) ─────────────

    @Test
    fun testClassificationInsufficient() {
        // < 10 → INSUFFICIENT; 9 sets in a single week → weeklyVolume 9.0
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..9).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(9.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(VolumeStatus.INSUFFICIENT, balance.latVolume.status)
        assertEquals("Too low", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationModerate() {
        // 10-13 → MODERATE; 12 sets in a single week → weeklyVolume 12.0
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..12).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(12.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(VolumeStatus.MODERATE, balance.latVolume.status)
        assertEquals("Moderate", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationOptimal() {
        // 14-17 → OPTIMAL; 16 sets in a single week → weeklyVolume 16.0
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..16).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(16.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(VolumeStatus.OPTIMAL, balance.latVolume.status)
        assertEquals("Optimal", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationHigh() {
        // 18-21 → HIGH; 20 sets in a single week → weeklyVolume 20.0
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..20).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(20.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(VolumeStatus.HIGH, balance.latVolume.status)
        assertEquals("High", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationExcessive() {
        // > 21 → EXCESSIVE; 25 sets in a single week → weeklyVolume 25.0
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))
        val sets = (1..25).map { setContext(set = createSet(), exerciseId = 1L) }

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(25.0, balance.latVolume.weeklyVolume, 0.001)
        assertEquals(VolumeStatus.EXCESSIVE, balance.latVolume.status)
        assertEquals("Very high", balance.latVolume.status.label)
    }

    @Test
    fun testClassificationBoundaryValues() {
        val muscleMap = mapOf(1L to listOf(muscleAssignment("Lats", MuscleRole.PRIMARY)))

        // Exactly 10 → MODERATE
        val sets10 = (1..10).map { setContext(set = createSet(), exerciseId = 1L) }
        assertEquals(10.0, calculator.calculateWeeklyVolume(sets10, muscleMap).latVolume.weeklyVolume, 0.001)
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
            latVolume = MuscleVolume("Lats", 16.0, 16, 0, VolumeStatus.OPTIMAL),
            lateralDeltVolume = MuscleVolume("Lateral Deltoid", 14.0, 14, 0, VolumeStatus.OPTIMAL),
            rearDeltVolume = MuscleVolume("Rear Deltoid", 12.0, 12, 0, VolumeStatus.MODERATE),
            upperChestVolume = MuscleVolume("Upper Chest", 14.0, 14, 0, VolumeStatus.OPTIMAL),
            upperBackVolume = MuscleVolume("Upper Back", 12.0, 12, 0, VolumeStatus.MODERATE),
            bicepsVolume = MuscleVolume("Biceps", 12.0, 12, 0, VolumeStatus.MODERATE),
            tricepsVolume = MuscleVolume("Triceps", 10.0, 10, 0, VolumeStatus.MODERATE),
            quadricepsVolume = MuscleVolume("Quadriceps", 16.0, 16, 0, VolumeStatus.OPTIMAL),
            hamstringsVolume = MuscleVolume("Hamstrings", 14.0, 14, 0, VolumeStatus.OPTIMAL),
            glutesVolume = MuscleVolume("Glutes", 12.0, 12, 0, VolumeStatus.MODERATE),
            calvesVolume = MuscleVolume("Calves", 10.0, 10, 0, VolumeStatus.MODERATE),
            coreVolume = MuscleVolume("Core", 8.0, 8, 0, VolumeStatus.INSUFFICIENT)
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
            latVolume = MuscleVolume("Lats", 20.0, 20, 0, VolumeStatus.HIGH),
            lateralDeltVolume = MuscleVolume("Lateral Deltoid", 22.0, 22, 0, VolumeStatus.EXCESSIVE),
            rearDeltVolume = MuscleVolume("Rear Deltoid", 18.0, 18, 0, VolumeStatus.HIGH),
            upperChestVolume = MuscleVolume("Upper Chest", 16.0, 16, 0, VolumeStatus.OPTIMAL),
            upperBackVolume = MuscleVolume("Upper Back", 18.0, 18, 0, VolumeStatus.HIGH),
            bicepsVolume = MuscleVolume("Biceps", 12.0, 12, 0, VolumeStatus.MODERATE),
            tricepsVolume = MuscleVolume("Triceps", 10.0, 10, 0, VolumeStatus.MODERATE),
            quadricepsVolume = MuscleVolume("Quadriceps", 16.0, 16, 0, VolumeStatus.OPTIMAL),
            hamstringsVolume = MuscleVolume("Hamstrings", 14.0, 14, 0, VolumeStatus.OPTIMAL),
            glutesVolume = MuscleVolume("Glutes", 12.0, 12, 0, VolumeStatus.MODERATE),
            calvesVolume = MuscleVolume("Calves", 10.0, 10, 0, VolumeStatus.MODERATE),
            coreVolume = MuscleVolume("Core", 8.0, 8, 0, VolumeStatus.INSUFFICIENT)
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
            latVolume = MuscleVolume("Lats", 6.0, 6, 0, VolumeStatus.INSUFFICIENT),
            lateralDeltVolume = MuscleVolume("Lateral Deltoid", 4.0, 4, 0, VolumeStatus.INSUFFICIENT),
            rearDeltVolume = MuscleVolume("Rear Deltoid", 4.0, 4, 0, VolumeStatus.INSUFFICIENT),
            upperChestVolume = MuscleVolume("Upper Chest", 6.0, 6, 0, VolumeStatus.INSUFFICIENT),
            upperBackVolume = MuscleVolume("Upper Back", 4.0, 4, 0, VolumeStatus.INSUFFICIENT),
            bicepsVolume = MuscleVolume("Biceps", 4.0, 4, 0, VolumeStatus.INSUFFICIENT),
            tricepsVolume = MuscleVolume("Triceps", 4.0, 4, 0, VolumeStatus.INSUFFICIENT),
            quadricepsVolume = MuscleVolume("Quadriceps", 6.0, 6, 0, VolumeStatus.INSUFFICIENT),
            hamstringsVolume = MuscleVolume("Hamstrings", 4.0, 4, 0, VolumeStatus.INSUFFICIENT),
            glutesVolume = MuscleVolume("Glutes", 4.0, 4, 0, VolumeStatus.INSUFFICIENT),
            calvesVolume = MuscleVolume("Calves", 4.0, 4, 0, VolumeStatus.INSUFFICIENT),
            coreVolume = MuscleVolume("Core", 4.0, 4, 0, VolumeStatus.INSUFFICIENT)
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
            assertEquals(0.0, mv.weeklyVolume, 0.001)
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
            assertEquals(0.0, mv.weeklyVolume, 0.001)
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
