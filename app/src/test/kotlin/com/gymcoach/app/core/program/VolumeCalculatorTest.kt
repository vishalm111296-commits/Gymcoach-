package com.gymcoach.app.core.program

import com.gymcoach.app.core.program.VolumeCalculator.MuscleAssignment
import com.gymcoach.app.core.program.VolumeCalculator.MuscleRole
import com.gymcoach.app.core.program.VolumeCalculator.SetWithContext
import com.gymcoach.app.core.program.VolumeCalculator.VolumeStatus
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class VolumeCalculatorTest {

    private val calculator = VolumeCalculator()

    private fun createSet(
        id: Long = 1,
        exerciseId: Long = 1,
        completed: Boolean = true,
        setType: Int = 0,
        daysOffset: Int = 0
    ): SetWithContext {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
        return SetWithContext(
            set = WorkoutSetEntity(
                id = id,
                workoutExerciseId = exerciseId, // workoutExerciseId shouldn't matter much for our mocked SetWithContext, but exerciseId does
                setNumber = 1,
                weight = 100.0,
                reps = 10,
                rpe = 8.0,
                restSeconds = 60,
                completed = completed,
                setType = setType
            ),
            exerciseId = exerciseId,
            workoutDate = calendar.timeInMillis
        )
    }

    @Test
    fun calculateWeeklyVolume_happyPath_directAndIndirectSets() {
        val sets = listOf(
            createSet(id = 1, exerciseId = 1),
            createSet(id = 2, exerciseId = 1),
            createSet(id = 3, exerciseId = 2)
        )

        val muscleMap = mapOf(
            1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY)),
            2L to listOf(
                MuscleAssignment("Lats", MuscleRole.SECONDARY),
                MuscleAssignment("Biceps", MuscleRole.PRIMARY)
            )
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        val lats = balance.latVolume
        // directSets should be 2 (from exercise 1)
        // indirectSets should be 1 (from exercise 2)
        assertEquals(2, lats.directSets)
        assertEquals(1, lats.indirectSets)
        assertEquals(3, lats.weeklySets)

        val biceps = balance.bicepsVolume
        assertEquals(1, biceps.directSets)
        assertEquals(0, biceps.indirectSets)
        assertEquals(1, biceps.weeklySets)
    }

    @Test
    fun calculateWeeklyVolume_excludesIncompleteAndWarmupSets() {
        val sets = listOf(
            createSet(id = 1, exerciseId = 1, completed = true, setType = 0), // Valid
            createSet(id = 2, exerciseId = 1, completed = false, setType = 0), // Incomplete
            createSet(id = 3, exerciseId = 1, completed = true, setType = 1) // Warmup
        )

        val muscleMap = mapOf(
            1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY))
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        val lats = balance.latVolume
        assertEquals(1, lats.directSets)
        assertEquals(1, lats.weeklySets)
    }

    @Test
    fun calculateWeeklyVolume_averagesAcrossMultipleWeeks() {
        // Sets spanning 3 different weeks
        val sets = listOf(
            createSet(id = 1, exerciseId = 1, daysOffset = 0),
            createSet(id = 2, exerciseId = 1, daysOffset = -7), // previous week
            createSet(id = 3, exerciseId = 1, daysOffset = -14), // two weeks ago
            createSet(id = 4, exerciseId = 1, daysOffset = -14)  // same as 3
        )

        val muscleMap = mapOf(
            1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY))
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        val lats = balance.latVolume
        // 4 sets total across 3 distinct weeks. 4 / 3 = 1 (integer division)
        assertEquals(1, lats.directSets)
        assertEquals(1, lats.weeklySets)
    }

    @Test
    fun calculateWeeklyVolume_classifiesVolumeStatus() {
        val sets = mutableListOf<SetWithContext>()
        for (i in 1..25) {
            sets.add(createSet(id = i.toLong(), exerciseId = 1))
        }

        val muscleMap = mapOf(
            1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY))
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        // 25 sets is EXCESSIVE (> 21)
        assertEquals(VolumeStatus.EXCESSIVE, balance.latVolume.status)
    }
}
