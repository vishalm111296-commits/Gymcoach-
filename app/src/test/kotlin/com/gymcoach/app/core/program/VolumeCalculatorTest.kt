package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class VolumeCalculatorTest {

    private lateinit var calculator: VolumeCalculator

    @Before
    fun setUp() {
        calculator = VolumeCalculator()
    }

    @Test
    fun `empty set list produces zero volume across all muscles`() {
        val balance = calculator.calculateWeeklyVolume(emptyList(), emptyMap())
        for (muscleVol in balance.asList()) {
            assertEquals(0.0, muscleVol.effectiveWeeklyVolume, 0.001)
            assertEquals(0, muscleVol.rawSetCount)
            assertEquals(0, muscleVol.directSets)
            assertEquals(0, muscleVol.indirectSets)
            assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, muscleVol.status)
        }
    }

    @Test
    fun `uncompleted and warmup sets are filtered out`() {
        val dateMs = LocalDate.of(2026, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val sets = listOf(
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, reps = 10, weight = 50.0, rpe = 8.0, restSeconds = 90, completed = false, setType = 0),
                exerciseId = 101,
                workoutDate = dateMs
            ),
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 2, workoutExerciseId = 1, setNumber = 2, reps = 10, weight = 30.0, rpe = 8.0, restSeconds = 90, completed = true, setType = 1), // Warmup
                exerciseId = 101,
                workoutDate = dateMs
            ),
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 3, workoutExerciseId = 1, setNumber = 3, reps = 10, weight = 60.0, rpe = 8.0, restSeconds = 90, completed = true, setType = 0), // Working set
                exerciseId = 101,
                workoutDate = dateMs
            )
        )

        val map = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val balance = calculator.calculateWeeklyVolume(sets, map)
        assertEquals(1.0, balance.latVolume.effectiveWeeklyVolume, 0.001)
        assertEquals(1, balance.latVolume.rawSetCount)
        assertEquals(1, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
    }

    @Test
    fun `primary secondary and stabilizer weighting applies correctly`() {
        val dateMs = LocalDate.of(2026, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val sets = listOf(
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, reps = 10, weight = 50.0, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                exerciseId = 101,
                workoutDate = dateMs
            )
        )

        val map = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Rear Deltoid", VolumeCalculator.MuscleRole.SECONDARY),
                VolumeCalculator.MuscleAssignment("Core", VolumeCalculator.MuscleRole.STABILIZER)
            )
        )

        val balance = calculator.calculateWeeklyVolume(sets, map)
        assertEquals(1.0, balance.latVolume.effectiveWeeklyVolume, 0.001)
        assertEquals(0.5, balance.rearDeltVolume.effectiveWeeklyVolume, 0.001)
        assertEquals(0.25, balance.coreVolume.effectiveWeeklyVolume, 0.001)

        assertEquals(1, balance.latVolume.directSets)
        assertEquals(1, balance.rearDeltVolume.indirectSets)
        assertEquals(1, balance.coreVolume.stabilizerSets)
    }

    @Test
    fun `weekly volume averages volume across active ISO calendar weeks`() {
        val week1Ms = LocalDate.of(2026, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val week2Ms = LocalDate.of(2026, 9, 8).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

        // 10 sets in Week 1, 6 sets in Week 2 = Total 16 sets, 2 weeks = 8.0 sets/week average
        val sets = mutableListOf<VolumeCalculator.SetWithContext>()
        repeat(10) { i ->
            sets.add(
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(id = i.toLong() + 1, workoutExerciseId = 1, setNumber = i + 1, reps = 10, weight = 50.0, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                    exerciseId = 101,
                    workoutDate = week1Ms
                )
            )
        }
        repeat(6) { i ->
            sets.add(
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(id = i.toLong() + 11, workoutExerciseId = 2, setNumber = i + 1, reps = 10, weight = 50.0, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                    exerciseId = 101,
                    workoutDate = week2Ms
                )
            )
        }

        val map = mapOf(
            101L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = calculator.calculateWeeklyVolume(sets, map)
        assertEquals(8.0, balance.latVolume.effectiveWeeklyVolume, 0.001)
        assertEquals(16, balance.latVolume.rawSetCount)
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    @Test
    fun `classification thresholds match evidence coaching bands`() {
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, calculator.classify(9.9))
        assertEquals(VolumeCalculator.VolumeStatus.MODERATE, calculator.classify(10.0))
        assertEquals(VolumeCalculator.VolumeStatus.MODERATE, calculator.classify(13.9))
        assertEquals(VolumeCalculator.VolumeStatus.OPTIMAL, calculator.classify(14.0))
        assertEquals(VolumeCalculator.VolumeStatus.OPTIMAL, calculator.classify(17.9))
        assertEquals(VolumeCalculator.VolumeStatus.HIGH, calculator.classify(18.0))
        assertEquals(VolumeCalculator.VolumeStatus.HIGH, calculator.classify(21.9))
        assertEquals(VolumeCalculator.VolumeStatus.EXCESSIVE, calculator.classify(22.0))
    }
}
