package com.gymcoach.app.core.program

import com.gymcoach.app.domain.model.CompletedSetContext
import com.gymcoach.app.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetTypeContractTest {

    private val volumeCalculator = VolumeCalculator()

    @Test
    fun `verify domain SetType working set predicate explicitly includes NORMAL DROP FAILURE and excludes WARMUP`() {
        val now = System.currentTimeMillis()

        val normalSet = CompletedSetContext(setId = 1, exerciseId = 1, workoutDate = now, weightKg = 100.0, reps = 10, rpe = 8f, completed = true, setType = SetType.NORMAL.ordinal)
        val warmupSet = CompletedSetContext(setId = 2, exerciseId = 1, workoutDate = now, weightKg = 50.0, reps = 10, rpe = 5f, completed = true, setType = SetType.WARMUP.ordinal)
        val dropSet = CompletedSetContext(setId = 3, exerciseId = 1, workoutDate = now, weightKg = 80.0, reps = 10, rpe = 9f, completed = true, setType = SetType.DROP.ordinal)
        val failureSet = CompletedSetContext(setId = 4, exerciseId = 1, workoutDate = now, weightKg = 100.0, reps = 8, rpe = 10f, completed = true, setType = SetType.FAILURE.ordinal)
        val incompleteSet = CompletedSetContext(setId = 5, exerciseId = 1, workoutDate = now, weightKg = 100.0, reps = 10, rpe = 8f, completed = false, setType = SetType.NORMAL.ordinal)

        assertEquals(SetType.NORMAL, normalSet.domainSetType)
        assertEquals(SetType.WARMUP, warmupSet.domainSetType)
        assertEquals(SetType.DROP, dropSet.domainSetType)
        assertEquals(SetType.FAILURE, failureSet.domainSetType)

        with(VolumeCalculator.Companion) {
            assertTrue("NORMAL set must be working set", normalSet.isHypertrophyWorkingSet)
            assertFalse("WARMUP set must NOT be working set", warmupSet.isHypertrophyWorkingSet)
            assertTrue("DROP set must be working set", dropSet.isHypertrophyWorkingSet)
            assertTrue("FAILURE set must be working set", failureSet.isHypertrophyWorkingSet)
            assertFalse("Incomplete set must NOT be working set", incompleteSet.isHypertrophyWorkingSet)
        }

        val muscleAssignments = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(
            listOf(normalSet, warmupSet, dropSet, failureSet, incompleteSet),
            muscleAssignments
        )

        // 3 included sets (NORMAL, DROP, FAILURE) = 3.0 effective sets
        assertEquals(3.0, balance.latVolume.weeklyEffectiveSets, 0.001)
        assertEquals(3, balance.latVolume.rawDirectSets)
    }
}
