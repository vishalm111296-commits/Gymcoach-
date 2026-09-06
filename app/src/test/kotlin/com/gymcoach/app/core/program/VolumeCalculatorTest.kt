package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class VolumeCalculatorTest {

    private lateinit var calculator: VolumeCalculator

    @Before
    fun setUp() {
        calculator = VolumeCalculator()
    }

    @Test
    fun calculateWeeklyVolume_correctly_counts_direct_and_indirect_sets() {
        val dateMs = Instant.parse("2026-09-01T12:00:00Z").toEpochMilli()
        val set1 = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 80.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0)
        val set2 = WorkoutSetEntity(id = 2, workoutExerciseId = 2, setNumber = 1, weight = 80.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0)

        val completedSets = listOf(
            VolumeCalculator.SetWithContext(set = set1, exerciseId = 101, workoutDate = dateMs),
            VolumeCalculator.SetWithContext(set = set2, exerciseId = 102, workoutDate = dateMs)
        )

        val exerciseMuscleMap = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Rear Deltoid", VolumeCalculator.MuscleRole.SECONDARY)
            ),
            102L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Core", VolumeCalculator.MuscleRole.STABILIZER)
            )
        )

        val balance = calculator.calculateWeeklyVolume(completedSets, exerciseMuscleMap)

        assertEquals(2, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
        assertEquals(0, balance.rearDeltVolume.directSets)
        assertEquals(1, balance.rearDeltVolume.indirectSets)
        assertEquals(0, balance.coreVolume.directSets)
        assertEquals(1, balance.coreVolume.indirectSets)
    }

    @Test
    fun calculateWeeklyVolume_handles_iso_week_bucketing_across_multiple_weeks() {
        val week1Date = Instant.parse("2026-09-01T12:00:00Z").toEpochMilli() // Week A
        val week2Date = Instant.parse("2026-09-15T12:00:00Z").toEpochMilli() // Week B

        val setW1 = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0)
        val setW2 = WorkoutSetEntity(id = 2, workoutExerciseId = 2, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0)

        val completedSets = listOf(
            VolumeCalculator.SetWithContext(set = setW1, exerciseId = 101, workoutDate = week1Date),
            VolumeCalculator.SetWithContext(set = setW2, exerciseId = 102, workoutDate = week2Date)
        )

        val exerciseMuscleMap = mapOf(
            101L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)),
            102L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = calculator.calculateWeeklyVolume(completedSets, exerciseMuscleMap)

        assertEquals(2, balance.latVolume.directSets)
    }

    @Test
    fun calculateVtaperBalance_evaluates_status_properly() {
        val balance = VolumeCalculator.TrainingBalance(
            latVolume = VolumeCalculator.MuscleVolume("Lats", 15, 15, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            lateralDeltVolume = VolumeCalculator.MuscleVolume("Lateral Deltoid", 15, 15, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            rearDeltVolume = VolumeCalculator.MuscleVolume("Rear Deltoid", 18, 18, 0, VolumeCalculator.VolumeStatus.HIGH),
            upperChestVolume = VolumeCalculator.MuscleVolume("Upper Chest", 18, 18, 0, VolumeCalculator.VolumeStatus.HIGH),
            upperBackVolume = VolumeCalculator.MuscleVolume("Upper Back", 18, 18, 0, VolumeCalculator.VolumeStatus.HIGH),
            bicepsVolume = VolumeCalculator.MuscleVolume("Biceps", 10, 10, 0, VolumeCalculator.VolumeStatus.MODERATE),
            tricepsVolume = VolumeCalculator.MuscleVolume("Triceps", 10, 10, 0, VolumeCalculator.VolumeStatus.MODERATE),
            quadricepsVolume = VolumeCalculator.MuscleVolume("Quadriceps", 10, 10, 0, VolumeCalculator.VolumeStatus.MODERATE),
            hamstringsVolume = VolumeCalculator.MuscleVolume("Hamstrings", 10, 10, 0, VolumeCalculator.VolumeStatus.MODERATE),
            glutesVolume = VolumeCalculator.MuscleVolume("Glutes", 10, 10, 0, VolumeCalculator.VolumeStatus.MODERATE),
            calvesVolume = VolumeCalculator.MuscleVolume("Calves", 10, 10, 0, VolumeCalculator.VolumeStatus.MODERATE),
            coreVolume = VolumeCalculator.MuscleVolume("Core", 10, 10, 0, VolumeCalculator.VolumeStatus.MODERATE)
        )

        val vtaperBalance = calculator.calculateVtaperBalance(balance)

        assertEquals("Good V-taper volume distribution", vtaperBalance.overallBalance)
    }
}
