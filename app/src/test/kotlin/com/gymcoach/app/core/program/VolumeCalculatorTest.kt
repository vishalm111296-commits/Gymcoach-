package com.gymcoach.app.core.program

import com.gymcoach.app.core.program.VolumeCalculator.MuscleAssignment
import com.gymcoach.app.core.program.VolumeCalculator.MuscleRole
import com.gymcoach.app.core.program.VolumeCalculator.SetWithContext
import com.gymcoach.app.core.program.VolumeCalculator.VolumeStatus
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class VolumeCalculatorTest {

    private val calculator = VolumeCalculator()
    private val zone = ZoneId.systemDefault()

    /** Millis for a given ISO year/month/day at noon local time, to avoid DST-edge flakiness. */
    private fun dateMs(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(year, month, day, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun createSet(
        id: Long = 1,
        exerciseId: Long = 1,
        completed: Boolean = true,
        setType: Int = 0,
        dateMs: Long
    ): SetWithContext = SetWithContext(
        set = WorkoutSetEntity(
            id = id,
            workoutExerciseId = exerciseId,
            setNumber = 1,
            weight = 100.0,
            reps = 10,
            rpe = 8.0,
            restSeconds = 60,
            completed = completed,
            setType = setType
        ),
        exerciseId = exerciseId,
        workoutDate = dateMs
    )

    @Test
    fun calculateWeeklyVolume_creditsPrimaryRoleAtFullWeight() {
        val monday = dateMs(2026, 3, 2)
        val sets = listOf(createSet(id = 1, exerciseId = 1, dateMs = monday))
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(1.0, balance.latVolume.directSets, 0.0001)
        assertEquals(0.0, balance.latVolume.indirectSets, 0.0001)
        assertEquals(1.0, balance.latVolume.weeklySets, 0.0001)
    }

    @Test
    fun calculateWeeklyVolume_creditsSecondaryRoleAtHalfWeight() {
        val monday = dateMs(2026, 3, 2)
        val sets = listOf(createSet(id = 1, exerciseId = 1, dateMs = monday))
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Upper Back", MuscleRole.SECONDARY)))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(0.0, balance.upperBackVolume.directSets, 0.0001)
        assertEquals(0.5, balance.upperBackVolume.indirectSets, 0.0001)
        assertEquals(0.5, balance.upperBackVolume.weeklySets, 0.0001)
    }

    @Test
    fun calculateWeeklyVolume_creditsStabilizerRoleAtQuarterWeight() {
        val monday = dateMs(2026, 3, 2)
        val sets = listOf(createSet(id = 1, exerciseId = 1, dateMs = monday))
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Core", MuscleRole.STABILIZER)))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(0.0, balance.coreVolume.directSets, 0.0001)
        assertEquals(0.25, balance.coreVolume.indirectSets, 0.0001)
        assertEquals(0.25, balance.coreVolume.weeklySets, 0.0001)
    }

    @Test
    fun calculateWeeklyVolume_averagesFractionallyAcrossTwoWeeks() {
        // 5 primary-role sets across 2 distinct ISO weeks must average to 2.5,
        // not truncate to 2 (this is the defect this suite guards against).
        val week1DayA = dateMs(2026, 3, 2) // Monday, week 10
        val week1DayB = dateMs(2026, 3, 4)
        val week1DayC = dateMs(2026, 3, 5)
        val week2DayA = dateMs(2026, 3, 9) // Monday, week 11
        val week2DayB = dateMs(2026, 3, 11)
        val sets = listOf(
            createSet(id = 1, dateMs = week1DayA),
            createSet(id = 2, dateMs = week1DayB),
            createSet(id = 3, dateMs = week1DayC),
            createSet(id = 4, dateMs = week2DayA),
            createSet(id = 5, dateMs = week2DayB)
        )
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(2.5, balance.latVolume.directSets, 0.0001)
        assertEquals(2.5, balance.latVolume.weeklySets, 0.0001)
    }

    @Test
    fun calculateWeeklyVolume_averagesAcrossThreeTrackedWeeks() {
        val sets = listOf(
            createSet(id = 1, dateMs = dateMs(2026, 3, 2)),  // week 10
            createSet(id = 2, dateMs = dateMs(2026, 3, 9)),  // week 11
            createSet(id = 3, dateMs = dateMs(2026, 3, 16)), // week 12
            createSet(id = 4, dateMs = dateMs(2026, 3, 16))  // week 12
        )
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        // 4 sets across 3 distinct weeks = 1.333..., not truncated to 1.
        assertEquals(4.0 / 3.0, balance.latVolume.directSets, 0.0001)
    }

    @Test
    fun calculateWeeklyVolume_isoWeekDoesNotSplitAcrossYearBoundary() {
        // Dec 31 2025 (Wed) and Jan 1 2026 (Thu) fall in the SAME ISO week
        // (2025-W53). A Calendar.WEEK_OF_YEAR/Calendar.YEAR based key would
        // incorrectly place these in two different week buckets.
        val dec31 = dateMs(2025, 12, 31)
        val jan1 = dateMs(2026, 1, 1)
        val sets = listOf(
            createSet(id = 1, dateMs = dec31),
            createSet(id = 2, dateMs = jan1)
        )
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        // Both sets land in one ISO week, so the average is 2.0 sets/week,
        // not 1.0 (which would happen if incorrectly split into two weeks).
        assertEquals(2.0, balance.latVolume.directSets, 0.0001)
    }

    @Test
    fun calculateWeeklyVolume_excludesIncompleteSets() {
        val monday = dateMs(2026, 3, 2)
        val sets = listOf(
            createSet(id = 1, completed = true, dateMs = monday),
            createSet(id = 2, completed = false, dateMs = monday)
        )
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(1.0, balance.latVolume.directSets, 0.0001)
    }

    @Test
    fun calculateWeeklyVolume_excludesWarmupSets() {
        val monday = dateMs(2026, 3, 2)
        val sets = listOf(
            createSet(id = 1, setType = 0, dateMs = monday), // working set
            createSet(id = 2, setType = 1, dateMs = monday)  // warmup set
        )
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(1.0, balance.latVolume.directSets, 0.0001)
    }

    @Test
    fun calculateWeeklyVolume_countsDirectAndIndirectSetsSeparately() {
        val monday = dateMs(2026, 3, 2)
        val sets = listOf(
            createSet(id = 1, exerciseId = 1, dateMs = monday),
            createSet(id = 2, exerciseId = 1, dateMs = monday),
            createSet(id = 3, exerciseId = 2, dateMs = monday)
        )
        val muscleMap = mapOf(
            1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)),
            2L to listOf(
                MuscleAssignment("Lats", MuscleRole.SECONDARY),
                MuscleAssignment("Biceps", MuscleRole.PRIMARY)
            )
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(2.0, balance.latVolume.directSets, 0.0001)
        assertEquals(0.5, balance.latVolume.indirectSets, 0.0001)
        assertEquals(2.5, balance.latVolume.weeklySets, 0.0001)
        assertEquals(1.0, balance.bicepsVolume.directSets, 0.0001)
        assertEquals(0.0, balance.bicepsVolume.indirectSets, 0.0001)
    }

    private fun setsOnOneDayCount(count: Int): List<SetWithContext> {
        val monday = dateMs(2026, 3, 2)
        return (1..count).map { createSet(id = it.toLong(), dateMs = monday) }
    }

    @Test
    fun classification_belowTenSetsIsInsufficient() {
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(setsOnOneDayCount(9), muscleMap)
        assertEquals(VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    @Test
    fun classification_tenToThirteenSetsIsModerate() {
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(setsOnOneDayCount(10), muscleMap)
        assertEquals(VolumeStatus.MODERATE, balance.latVolume.status)
    }

    @Test
    fun classification_fourteenToSeventeenSetsIsOptimal() {
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(setsOnOneDayCount(14), muscleMap)
        assertEquals(VolumeStatus.OPTIMAL, balance.latVolume.status)
    }

    @Test
    fun classification_eighteenToTwentyOneSetsIsHigh() {
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(setsOnOneDayCount(18), muscleMap)
        assertEquals(VolumeStatus.HIGH, balance.latVolume.status)
    }

    @Test
    fun classification_aboveTwentyOneSetsIsExcessive() {
        val muscleMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(setsOnOneDayCount(25), muscleMap)
        assertEquals(VolumeStatus.EXCESSIVE, balance.latVolume.status)
    }

    @Test
    fun calculateVtaperBalance_optimalScoresHigherThanExcessive() {
        // This guards the fix for the scoring inversion: OPTIMAL must score
        // strictly higher than EXCESSIVE, never the reverse.
        val optimalMap = mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))
        val optimalBalance = calculator.calculateWeeklyVolume(setsOnOneDayCount(15), optimalMap)
        val excessiveBalance = calculator.calculateWeeklyVolume(setsOnOneDayCount(30), optimalMap)

        assertEquals(VolumeStatus.OPTIMAL, optimalBalance.latVolume.status)
        assertEquals(VolumeStatus.EXCESSIVE, excessiveBalance.latVolume.status)

        val optimalScore = calculator.calculateVtaperBalance(
            optimalBalance.copy(lateralDeltVolume = optimalBalance.latVolume)
        ).primaryScore
        val excessiveScore = calculator.calculateVtaperBalance(
            excessiveBalance.copy(lateralDeltVolume = excessiveBalance.latVolume)
        ).primaryScore

        assert(optimalScore > excessiveScore) {
            "Expected OPTIMAL score ($optimalScore) to exceed EXCESSIVE score ($excessiveScore)"
        }
        assertEquals(1.0, optimalScore, 0.0001)
    }
}
