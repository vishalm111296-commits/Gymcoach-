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
        // Use canonical MUSCLE_BACK constant to avoid future mismatch
        val muscleMap = mapOf(
            100L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(warmupSet, uncompletedSet), muscleMap)
        // Fix: field renamed latVolume -> backVolume (F-TAXONOMY-1)
        assertEquals("Back weekly sets should be 0", 0, balance.backVolume.weeklySets)
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
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BICEPS, VolumeCalculator.MuscleRole.SECONDARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(completedSet), muscleMap)
        // Fix: field renamed latVolume -> backVolume (F-TAXONOMY-1)
        assertEquals("Back direct sets should be 1", 1, balance.backVolume.directSets)
        assertEquals("Biceps indirect sets should be 1", 1, balance.bicepsVolume.indirectSets)
    }

    @Test
    fun `back exercises with MUSCLE_BACK key are counted in backVolume`() {
        // Regression test for F-TAXONOMY-1: exercises tagged muscleGroup="Back"
        // (the canonical seed-data string) must flow to backVolume, not be lost.
        val set = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 3, workoutExerciseId = 20, setNumber = 1, weight = 60.0, reps = 8, rpe = 8.0, restSeconds = 120, completed = true, setType = 0),
            exerciseId = 200,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            200L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(set), muscleMap)
        assertEquals("Back direct sets must be 1 for a completed back exercise", 1, balance.backVolume.directSets)
        assertEquals("Back weekly sets must be 1", 1, balance.backVolume.weeklySets)
        assertTrue("Back status must not be INSUFFICIENT with 1 set", balance.backVolume.status == VolumeCalculator.VolumeStatus.INSUFFICIENT)
    }

    @Test
    fun `calculateVtaperBalance provides expected status summary`() {
        val balance = volumeCalculator.calculateWeeklyVolume(emptyList(), emptyMap())
        val vtaper = volumeCalculator.calculateVtaperBalance(balance)
        assertEquals("Low V-taper volume", vtaper.overallBalance)
    }

    @Test
    fun `direct and indirect sets across primary secondary stabilizer roles are correctly counted`() {
        val completedSet1 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = 1700000000000L
        )
        val completedSet2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 10, setNumber = 2, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = 1700000000000L
        )
        val muscleMap = mapOf(
            100L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Biceps", VolumeCalculator.MuscleRole.SECONDARY),
                VolumeCalculator.MuscleAssignment("Core", VolumeCalculator.MuscleRole.STABILIZER)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(completedSet1, completedSet2), muscleMap)
        assertEquals("Lats direct sets should be 2", 2, balance.latVolume.directSets)
        assertEquals("Lats indirect sets should be 0", 0, balance.latVolume.indirectSets)
        assertEquals("Biceps direct sets should be 0", 0, balance.bicepsVolume.directSets)
        assertEquals("Biceps indirect sets should be 2", 2, balance.bicepsVolume.indirectSets)
        assertEquals("Core direct sets should be 0", 0, balance.coreVolume.directSets)
        assertEquals("Core indirect sets should be 2", 2, balance.coreVolume.indirectSets)
    }

    @Test
    fun `multi week set contexts correctly aggregate weekly volume averages`() {
        val week1Date = 1700000000000L
        val week2Date = week1Date + (7 * 24 * 60 * 60 * 1000L)

        val setWeek1 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = week1Date
        )
        val set1Week2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 20, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = week2Date
        )
        val set2Week2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 3, workoutExerciseId = 20, setNumber = 2, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = week2Date
        )
        val muscleMap = mapOf(
            100L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(setWeek1, set1Week2, set2Week2), muscleMap)
        assertEquals("Lats total direct sets across weeks", 3, balance.latVolume.directSets)
        assertEquals("Lats total indirect sets across weeks", 0, balance.latVolume.indirectSets)
        assertEquals("Lats weekly sets total", 3, balance.latVolume.weeklySets)
    }
}
