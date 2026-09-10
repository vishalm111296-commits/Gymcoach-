package com.gymcoach.app.core.program

import com.gymcoach.app.domain.model.CompletedSetContext
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
        assertTrue("All weekly sets should be 0.0", balance.asList().all { it.weeklyEffectiveSets == 0.0 })
        assertTrue("All statuses should be INSUFFICIENT", balance.asList().all { it.status == VolumeCalculator.VolumeStatus.INSUFFICIENT })
    }

    @Test
    fun `set type filtering strictly includes NORMAL DROP FAILURE completed and excludes WARMUP incomplete`() {
        val now = System.currentTimeMillis()
        val sets = listOf(
            CompletedSetContext(setId = 1, exerciseId = 100, workoutDate = now, weightKg = 50.0, reps = 10, rpe = 8f, completed = true, setType = 0), // NORMAL completed -> INCLUDED
            CompletedSetContext(setId = 2, exerciseId = 100, workoutDate = now, weightKg = 50.0, reps = 10, rpe = 8f, completed = false, setType = 0), // NORMAL incomplete -> EXCLUDED
            CompletedSetContext(setId = 3, exerciseId = 100, workoutDate = now, weightKg = 20.0, reps = 10, rpe = 5f, completed = true, setType = 1), // WARMUP completed -> EXCLUDED
            CompletedSetContext(setId = 4, exerciseId = 100, workoutDate = now, weightKg = 40.0, reps = 8, rpe = 9f, completed = true, setType = 2), // DROP completed -> INCLUDED
            CompletedSetContext(setId = 5, exerciseId = 100, workoutDate = now, weightKg = 50.0, reps = 6, rpe = 10f, completed = true, setType = 3) // FAILURE completed -> INCLUDED
        )
        val muscleMap = mapOf(
            100L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(sets, muscleMap)
        // 3 included primary sets (NORMAL, DROP, FAILURE) = 3.0 effective sets
        assertEquals("Lats effective sets should be 3.0", 3.0, balance.latVolume.weeklyEffectiveSets, 0.001)
        assertEquals("Lats raw direct sets should be 3", 3, balance.latVolume.rawDirectSets)
        assertEquals("Lats raw indirect sets should be 0", 0, balance.latVolume.rawIndirectSets)
    }

    @Test
    fun `deterministic volume weighting example 1 - 3 primary 2 secondary 4 stabilizer`() {
        val now = System.currentTimeMillis()
        val sets = mutableListOf<CompletedSetContext>()
        repeat(3) { i -> sets.add(CompletedSetContext(setId = i + 1L, exerciseId = 101, workoutDate = now, weightKg = 60.0, reps = 10, rpe = 8f, completed = true, setType = 0)) }
        repeat(2) { i -> sets.add(CompletedSetContext(setId = i + 10L, exerciseId = 102, workoutDate = now, weightKg = 50.0, reps = 10, rpe = 8f, completed = true, setType = 0)) }
        repeat(4) { i -> sets.add(CompletedSetContext(setId = i + 20L, exerciseId = 103, workoutDate = now, weightKg = 40.0, reps = 10, rpe = 8f, completed = true, setType = 0)) }

        val muscleMap = mapOf(
            101L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)),
            102L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.SECONDARY)),
            103L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.STABILIZER))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals("Raw direct sets count should be 3", 3, balance.latVolume.rawDirectSets)
        assertEquals("Raw indirect sets count should be 6", 6, balance.latVolume.rawIndirectSets)
        assertEquals("Effective sets should be 3.0 + 1.0 + 1.0 = 5.0", 5.0, balance.latVolume.weeklyEffectiveSets, 0.001)
    }

    @Test
    fun `deterministic volume weighting example 2 - 0 primary 4 secondary 0 stabilizer`() {
        val now = System.currentTimeMillis()
        val sets = List(4) { i ->
            CompletedSetContext(setId = i + 1L, exerciseId = 200, workoutDate = now, weightKg = 30.0, reps = 10, rpe = 8f, completed = true, setType = 0)
        }
        val muscleMap = mapOf(
            200L to listOf(VolumeCalculator.MuscleAssignment("Triceps", VolumeCalculator.MuscleRole.SECONDARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals("Raw direct sets count should be 0", 0, balance.tricepsVolume.rawDirectSets)
        assertEquals("Raw indirect sets count should be 4", 4, balance.tricepsVolume.rawIndirectSets)
        assertEquals("Effective sets should be 4 * 0.5 = 2.0", 2.0, balance.tricepsVolume.weeklyEffectiveSets, 0.001)
    }

    @Test
    fun `deterministic volume weighting example 3 - 0 primary 0 secondary 4 stabilizer`() {
        val now = System.currentTimeMillis()
        val sets = List(4) { i ->
            CompletedSetContext(setId = i + 1L, exerciseId = 300, workoutDate = now, weightKg = 0.0, reps = 30, rpe = 8f, completed = true, setType = 0)
        }
        val muscleMap = mapOf(
            300L to listOf(VolumeCalculator.MuscleAssignment("Core", VolumeCalculator.MuscleRole.STABILIZER))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals("Raw direct sets count should be 0", 0, balance.coreVolume.rawDirectSets)
        assertEquals("Raw indirect sets count should be 4", 4, balance.coreVolume.rawIndirectSets)
        assertEquals("Effective sets should be 4 * 0.25 = 1.0", 1.0, balance.coreVolume.weeklyEffectiveSets, 0.001)
    }

    @Test
    fun `multiple sets up to 12 completed sets are individually counted without grouping or capping`() {
        val now = System.currentTimeMillis()
        val sets = List(12) { i ->
            CompletedSetContext(setId = i + 1L, exerciseId = 400, workoutDate = now, weightKg = 80.0, reps = 8, rpe = 8f, completed = true, setType = 0)
        }
        val muscleMap = mapOf(
            400L to listOf(VolumeCalculator.MuscleAssignment("Quadriceps", VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals("12 completed sets must yield 12.0 effective sets", 12.0, balance.quadricepsVolume.weeklyEffectiveSets, 0.001)
        assertEquals("Raw direct sets should be 12", 12, balance.quadricepsVolume.rawDirectSets)
    }

    @Test
    fun `calculateVtaperBalance provides expected status summary`() {
        val balance = volumeCalculator.calculateWeeklyVolume(emptyList(), emptyMap())
        val vtaper = volumeCalculator.calculateVtaperBalance(balance)
        assertEquals("Low V-taper volume", vtaper.overallBalance)
    }
}
