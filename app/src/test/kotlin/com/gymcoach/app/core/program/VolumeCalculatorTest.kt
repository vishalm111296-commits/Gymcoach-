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
                workoutExerciseId = exerciseId,
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
    fun calculateWeeklyVolume_countsDirectAndIndirectSets() {
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

        assertEquals(2, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.indirectSets)
        assertEquals(3, balance.latVolume.weeklySets)
        assertEquals(1, balance.bicepsVolume.directSets)
        assertEquals(0, balance.bicepsVolume.indirectSets)
    }

    @Test
    fun calculateWeeklyVolume_excludesIncompleteAndWarmupSets() {
        val sets = listOf(
            createSet(id = 1, completed = true, setType = 0),
            createSet(id = 2, completed = false, setType = 0),
            createSet(id = 3, completed = true, setType = 1)
        )
        val muscleMap = mapOf(
            1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY))
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(1, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.weeklySets)
    }

    @Test
    fun calculateWeeklyVolume_averagesAcrossTrackedWeeks() {
        val sets = listOf(
            createSet(id = 1, daysOffset = 0),
            createSet(id = 2, daysOffset = -7),
            createSet(id = 3, daysOffset = -14),
            createSet(id = 4, daysOffset = -14)
        )
        val muscleMap = mapOf(
            1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY))
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        // Four sets across three tracked weeks average to one set per week.
        assertEquals(1, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.weeklySets)
    }

    @Test
    fun calculateWeeklyVolume_classifiesExcessiveVolume() {
        val sets = (1..25).map { createSet(id = it.toLong()) }
        val muscleMap = mapOf(
            1L to listOf(MuscleAssignment("Lats", MuscleRole.PRIMARY))
        )

        val balance = calculator.calculateWeeklyVolume(sets, muscleMap)

        assertEquals(VolumeStatus.EXCESSIVE, balance.latVolume.status)
    }
}
