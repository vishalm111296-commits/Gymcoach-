package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class VolumeCalculatorTest {

    private lateinit var volumeCalculator: VolumeCalculator

    @Before
    fun setUp() {
        volumeCalculator = VolumeCalculator()
    }

    @Test
    fun `calculateWeeklyVolume computes weighted volume and set counts correctly across ISO weeks`() {
        // Given 2 completed sets on Jan 15 2024 (ISO Week 3 2024) and 1 completed set on Jan 22 2024 (ISO Week 4 2024)
        val zoneId = ZoneId.systemDefault()
        val jan15Ms = LocalDateTime.of(2024, 1, 15, 10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val jan22Ms = LocalDateTime.of(2024, 1, 22, 10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val set1 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
            exerciseId = 101,
            workoutDate = jan15Ms
        )
        val set2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 1, setNumber = 2, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
            exerciseId = 101,
            workoutDate = jan15Ms
        )
        val set3 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 3, workoutExerciseId = 2, setNumber = 1, weight = 20.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
            exerciseId = 102,
            workoutDate = jan22Ms
        )

        val exerciseMuscleMap = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Biceps", VolumeCalculator.MuscleRole.SECONDARY)
            ),
            102L to listOf(
                VolumeCalculator.MuscleAssignment("Lateral Deltoid", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val result = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(set1, set2, set3),
            exerciseMuscleMap = exerciseMuscleMap
        )

        // Lat Volume: 2 direct sets for exercise 101
        assertEquals(2, result.latVolume.directSets)
        assertEquals(0, result.latVolume.indirectSets)
        assertEquals(2, result.latVolume.weeklySets)

        // Biceps Volume: 2 indirect sets (secondary role) for exercise 101
        assertEquals(0, result.bicepsVolume.directSets)
        assertEquals(2, result.bicepsVolume.indirectSets)
        assertEquals(2, result.bicepsVolume.weeklySets)

        // Lateral Delt Volume: 1 direct set for exercise 102
        assertEquals(1, result.lateralDeltVolume.directSets)
        assertEquals(0, result.lateralDeltVolume.indirectSets)
        assertEquals(1, result.lateralDeltVolume.weeklySets)
    }

    @Test
    fun `calculateWeeklyVolume handles empty completed sets gracefully`() {
        val result = volumeCalculator.calculateWeeklyVolume(
            completedSets = emptyList(),
            exerciseMuscleMap = emptyMap()
        )

        assertEquals(0, result.latVolume.weeklySets)
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, result.latVolume.status)
    }

    @Test
    fun `calculateVtaperBalance returns expected balance text based on volume status`() {
        val balance = volumeCalculator.calculateWeeklyVolume(
            completedSets = emptyList(),
            exerciseMuscleMap = emptyMap()
        )

        val vtaper = volumeCalculator.calculateVtaperBalance(balance)
        assertEquals("Low V-taper volume", vtaper.overallBalance)
    }
}
