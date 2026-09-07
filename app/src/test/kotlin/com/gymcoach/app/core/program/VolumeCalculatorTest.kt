package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VolumeCalculatorTest {

    private lateinit var volumeCalculator: VolumeCalculator

    @Before
    fun setUp() {
        volumeCalculator = VolumeCalculator()
    }

    @Test
    fun `empty set list returns zero volume across all muscles`() {
        val balance = volumeCalculator.calculateWeeklyVolume(emptyList(), emptyMap())
        assertTrue("All weekly sets should be 0", balance.asList().all { it.weeklySets == 0 })
        assertTrue("All statuses should be INSUFFICIENT", balance.asList().all { it.status == VolumeCalculator.VolumeStatus.INSUFFICIENT })
    }

    @Test
    fun `uncompleted sets and warmups are excluded from volume`() {
        val warmupSet = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 20.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 1),
            exerciseId = 100,
            workoutDate = System.currentTimeMillis()
        )
        val uncompletedSet = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 10, setNumber = 2, weight = 30.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = false, setType = 0),
            exerciseId = 100,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            100L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(warmupSet, uncompletedSet), muscleMap)
        assertEquals("Lats weekly sets should be 0", 0, balance.latVolume.weeklySets)
    }

    @Test
    fun `completed normal sets add weighted credit`() {
        val completedSet = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 20.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            100L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Biceps", VolumeCalculator.MuscleRole.SECONDARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(completedSet), muscleMap)
        assertEquals("Lats direct sets should be 1", 1, balance.latVolume.directSets)
        assertEquals("Biceps indirect sets should be 1", 1, balance.bicepsVolume.indirectSets)
    }

    @Test
    fun `calculateVtaperBalance provides expected status summary`() {
        val balance = volumeCalculator.calculateWeeklyVolume(emptyList(), emptyMap())
        val vtaper = volumeCalculator.calculateVtaperBalance(balance)
        assertEquals("Low V-taper volume", vtaper.overallBalance)
    }
}
