package com.gymcoach.app.core.program

import com.gymcoach.app.core.program.VolumeCalculator.MuscleAssignment
import com.gymcoach.app.core.program.VolumeCalculator.MuscleRole
import com.gymcoach.app.core.program.VolumeCalculator.SetWithContext
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class VolumeCalculatorWeightedTest {
    private val calculator = VolumeCalculator()

    private fun set(id: Long, exerciseId: Long, date: LocalDate): SetWithContext = SetWithContext(
        set = WorkoutSetEntity(
            id = id,
            workoutExerciseId = exerciseId,
            setNumber = 1,
            weight = 10.0,
            reps = 10,
            rpe = 8.0,
            restSeconds = 90,
            completed = true,
            setType = 0
        ),
        exerciseId = exerciseId,
        workoutDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    @Test
    fun preservesSecondaryAndStabilizerCredits() {
        val date = LocalDate.of(2026, 9, 6)
        val balance = calculator.calculateWeeklyVolume(
            completedSets = listOf(set(1, 1, date), set(2, 2, date)),
            exerciseMuscleMap = mapOf(
                1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)),
                2L to listOf(MuscleAssignment("Lats", MuscleRole.SECONDARY))
            )
        )

        assertEquals(1, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.indirectSets)
        assertEquals(2, balance.latVolume.weeklySets)
    }

    @Test
    fun averagesWeightedCreditsAcrossWeeks() {
        val weekOne = LocalDate.of(2026, 9, 6)
        val weekTwo = LocalDate.of(2026, 8, 30)
        val balance = calculator.calculateWeeklyVolume(
            completedSets = listOf(
                set(1, 1, weekOne),
                set(2, 2, weekOne),
                set(3, 2, weekTwo),
                set(4, 2, weekTwo)
            ),
            exerciseMuscleMap = mapOf(
                1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)),
                2L to listOf(MuscleAssignment("Lats", MuscleRole.SECONDARY))
            )
        )

        // Week 1 = 1 + 0.5; week 2 = 0.5 + 0.5 -> average = 1.25, rounded to 1.
        assertEquals(1, balance.latVolume.weeklySets)
        assertEquals(1, balance.latVolume.indirectSets)
    }

    @Test
    fun isoWeekBoundaryDoesNotSplitNewYearWeek() {
        val dec31 = LocalDate.of(2025, 12, 31)
        val jan1 = LocalDate.of(2026, 1, 1)
        val balance = calculator.calculateWeeklyVolume(
            completedSets = listOf(set(1, 1, dec31), set(2, 1, jan1)),
            exerciseMuscleMap = mapOf(
                1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY))
            )
        )

        assertEquals(2, balance.latVolume.weeklySets)
    }

    @Test
    fun excludesIncompleteAndWarmupSets() {
        val date = LocalDate.of(2026, 9, 6)
        val completed = set(1, 1, date)
        val incomplete = completed.copy(set = completed.set.copy(id = 2, completed = false))
        val warmup = completed.copy(set = completed.set.copy(id = 3, setType = 1))

        val balance = calculator.calculateWeeklyVolume(
            listOf(completed, incomplete, warmup),
            mapOf(1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)))
        )

        assertEquals(1, balance.latVolume.weeklySets)
    }
}
