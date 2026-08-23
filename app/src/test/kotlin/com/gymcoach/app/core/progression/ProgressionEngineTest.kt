package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Double-progression contract: load increases ONLY when every working set
 * reaches the top of the rep range; equipment-limited and bodyweight moves
 * switch to rep/set progression; two consecutive below-minimum sessions
 * trigger a deload. Warmup/incomplete sets never drive decisions.
 */
class ProgressionEngineTest {

    private val engine = ProgressionEngine(com.gymcoach.app.core.exercise.EquipmentAvailability())

    private fun set(
        weight: Double,
        reps: Int,
        completed: Boolean = true,
        setType: Int = 0,
        number: Int = 1
    ) = WorkoutSetEntity(
        id = number.toLong(),
        workoutExerciseId = 1L,
        setNumber = number,
        weight = weight,
        reps = reps,
        rpe = 7.0,
        restSeconds = 90,
        completed = completed,
        setType = setType
    )

    private fun progress(
        current: List<WorkoutSetEntity>,
        previous: List<WorkoutSetEntity> = emptyList(),
        min: Int = 8,
        max: Int = 12,
        targetSets: Int = 3,
        equipment: String = "Dumbbell"
    ) = engine.calculateProgression(
        exerciseId = 1L,
        exerciseName = "Bench",
        exerciseEquipment = equipment,
        targetRepsMin = min,
        targetRepsMax = max,
        targetSets = targetSets,
        previousSets = previous,
        currentSets = current
    )

    // ---- No data ----

    @Test
    fun `no completed sets yields neutral recommendation`() {
        val rec = progress(emptyList())
        assertEquals(0.0, rec.recommendedWeight, 1e-9)
        assertEquals("No completed working sets yet.", rec.reason)
        assertEquals(0.5, rec.confidence, 1e-9)
    }

    // ---- Weight progression tiers ----

    @Test
    fun `all top-of-range sets increase light weight by 2kg`() {
        val rec = progress((1..3).map { set(weight = 15.0, reps = 12, number = it) })
        assertEquals(17.0, rec.recommendedWeight, 1e-9)
        assertEquals(0.9, rec.confidence, 1e-9)
        assertFalse(rec.isEquipmentLimited)
    }

    @Test
    fun `mid-range dumbbell weight increases by 2_5kg`() {
        val rec = progress((1..3).map { set(weight = 30.0, reps = 12, number = it) })
        assertEquals(32.5, rec.recommendedWeight, 1e-9)
    }

    @Test
    fun `heavy weight increases by 5kg under 100`() {
        val rec = progress((1..3).map { set(weight = 60.0, reps = 12, number = it) })
        assertEquals(65.0, rec.recommendedWeight, 1e-9)
    }

    @Test
    fun `triple-digit weight uses capped 5 percent step`() {
        val rec = progress((1..3).map { set(weight = 100.0, reps = 12, number = it) })
        assertEquals(105.0, rec.recommendedWeight, 1e-9)
    }

    // ---- Bodyweight / equipment-limited paths ----

    @Test
    fun `bodyweight top performance adds reps and a set instead of load`() {
        val rec = progress(
            (1..3).map { set(weight = 0.0, reps = 12, number = it) },
            targetSets = 3
        )
        assertEquals(0.0, rec.recommendedWeight, 1e-9)
        assertEquals(4, rec.recommendedSets)
        assertEquals("8-14", rec.recommendedReps)
        assertTrue(rec.isEquipmentLimited)
    }

    @Test
    fun `barbell at home is equipment limited so load never increases`() {
        val rec = progress(
            (1..3).map { set(weight = 30.0, reps = 12, number = it) },
            equipment = "Barbell"
        )
        assertEquals(30.0, rec.recommendedWeight, 1e-9)
        assertEquals(4, rec.recommendedSets)
        assertTrue(rec.isEquipmentLimited)
    }

    // ---- Regression path ----

    @Test
    fun `two below-min sessions deload by 10 percent`() {
        val prev = (1..3).map { set(weight = 30.0, reps = 6, number = it) }
        val curr = (1..3).map { set(weight = 30.0, reps = 7, number = it) }
        val rec = progress(curr, previous = prev)
        assertEquals(27.0, rec.recommendedWeight, 1e-9)
    }

    @Test
    fun `single weak session after good session maintains weight`() {
        val prev = (1..3).map { set(weight = 30.0, reps = 10, number = it) }
        val curr = (1..2).map { set(weight = 30.0, reps = 7, number = it) }
        val rec = progress(curr, previous = prev)
        assertEquals(30.0, rec.recommendedWeight, 1e-9)
        assertEquals("Maintain current weight and focus on hitting target reps.", rec.reason)
    }

    // ---- Filtering ----

    @Test
    fun `warmup sets do not trigger premature progression`() {
        val current = listOf(
            set(weight = 20.0, reps = 15, setType = 1, number = 1),
            set(weight = 30.0, reps = 12, number = 2),
            set(weight = 30.0, reps = 12, number = 3)
        )
        val rec = progress(current)
        assertEquals(32.5, rec.recommendedWeight, 1e-9)
    }

    @Test
    fun `incomplete sets are ignored entirely`() {
        val rec = progress(listOf(set(weight = 30.0, reps = 12, completed = false)))
        assertEquals("No completed working sets yet.", rec.reason)
    }

    // ---- Partial success maintains ----

    @Test
    fun `one short set blocks load increase`() {
        val rec = progress((1..3).map { set(weight = 30.0, reps = if (it == 3) 11 else 12, number = it) })
        assertEquals(30.0, rec.recommendedWeight, 1e-9)
    }
}
