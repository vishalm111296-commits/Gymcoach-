package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class VolumeCalculatorTest {

    private lateinit var volumeCalculator: VolumeCalculator

    @Before
    fun setUp() {
        volumeCalculator = VolumeCalculator()
    }

    @Test
    fun `calculateWeeklyVolume computes weighted effective volume and set counts correctly`() {
        val zoneId = ZoneId.systemDefault()
        val dateMs = LocalDateTime.of(2024, 1, 15, 10, 0).atZone(zoneId).toInstant().toEpochMilli()

        // 1 completed working set for exercise 101
        val set1 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
            exerciseId = 101,
            workoutDate = dateMs
        )

        // Exercise 101 gives: Primary to Lats (1.0), Secondary to Biceps (0.5), Stabilizer to Core (0.25)
        val exerciseMuscleMap = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Biceps", VolumeCalculator.MuscleRole.SECONDARY),
                VolumeCalculator.MuscleAssignment("Core", VolumeCalculator.MuscleRole.STABILIZER)
            )
        )

        val result = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(set1),
            exerciseMuscleMap = exerciseMuscleMap
        )

        // Lats: 1 direct set, 1.0 effective volume
        assertEquals(1, result.latVolume.directSets)
        assertEquals(0, result.latVolume.indirectSets)
        assertEquals(1.0, result.latVolume.effectiveWeeklyVolume, 0.001)

        // Biceps: 0 direct, 1 indirect, 0.5 effective volume
        assertEquals(0, result.bicepsVolume.directSets)
        assertEquals(1, result.bicepsVolume.indirectSets)
        assertEquals(0.5, result.bicepsVolume.effectiveWeeklyVolume, 0.001)

        // Core: 0 direct, 1 indirect, 0.25 effective volume
        assertEquals(0, result.coreVolume.directSets)
        assertEquals(1, result.coreVolume.indirectSets)
        assertEquals(0.25, result.coreVolume.effectiveWeeklyVolume, 0.001)
    }

    @Test
    fun `ISO New Year boundary correctly buckets dates belonging to same ISO week`() {
        val zoneId = ZoneId.systemDefault()
        // Dec 30, 2024 (Monday) and Jan 1, 2025 (Wednesday) belong to ISO Week 1 of 2025
        val dec30Ms = LocalDateTime.of(2024, 12, 30, 10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val jan1Ms = LocalDateTime.of(2025, 1, 1, 10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val set1 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
            exerciseId = 101,
            workoutDate = dec30Ms
        )
        val set2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 1, setNumber = 2, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
            exerciseId = 101,
            workoutDate = jan1Ms
        )

        val exerciseMuscleMap = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val result = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(set1, set2),
            exerciseMuscleMap = exerciseMuscleMap
        )

        // Both sets land in 1 ISO week, so average weekly effective volume = 2.0 / 1 week = 2.0
        assertEquals(2.0, result.latVolume.effectiveWeeklyVolume, 0.001)
    }

    @Test
    fun `calculateWeeklyVolume ignores uncompleted and non-working sets`() {
        val zoneId = ZoneId.systemDefault()
        val dateMs = LocalDateTime.of(2024, 1, 15, 10, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Uncompleted set
        val uncompletedSet = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = false, setType = 0),
            exerciseId = 101,
            workoutDate = dateMs
        )
        // Warm-up set (setType = 1)
        val warmupSet = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 1, setNumber = 1, weight = 20.0, reps = 10, rpe = 5.0, restSeconds = 60, completed = true, setType = 1),
            exerciseId = 101,
            workoutDate = dateMs
        )

        val exerciseMuscleMap = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val result = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(uncompletedSet, warmupSet),
            exerciseMuscleMap = exerciseMuscleMap
        )

        assertEquals(0, result.latVolume.weeklySets)
        assertEquals(0.0, result.latVolume.effectiveWeeklyVolume, 0.001)
    }
}
