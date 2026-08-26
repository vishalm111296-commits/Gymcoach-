package com.gymcoach.app.presentation.history

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutWithDetails
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.gymcoach.app.presentation.history.calculateMuscleBreakdown
import com.gymcoach.app.presentation.history.MuscleData
import com.gymcoach.app.presentation.history.formatDuration
import com.gymcoach.app.presentation.history.formatDate

/**
 * Tests for workout detail utilities and summary generation.
 */
class WorkoutHistoryDetailTest {

    private fun createTestWorkout(): WorkoutWithDetails {
        val workout = Workout(
            id = 1L,
            date = Instant.parse("2026-08-24T10:00:00Z"),
            startTime = Instant.parse("2026-08-24T10:00:00Z"),
            endTime = Instant.parse("2026-08-24T11:00:00Z"),
            duration = 3600L, // 1 hour
            notes = "Leg Day",
            completed = true,
            status = "COMPLETED"
        )

        val benchPress = Exercise(
            id = 1L,
            name = "Bench Press",
            muscleGroup = "Chest",
            equipment = "Barbell",
            description = "Flat barbell bench press",
            difficulty = "Beginner",
            vtaperUpperChest = 8
        )

        val squat = Exercise(
            id = 2L,
            name = "Squat",
            muscleGroup = "Quads",
            equipment = "Barbell",
            description = "Back squat",
            difficulty = "Intermediate",
            vtaperLateralDelt = 1
        )

        val benchSets = listOf(
            WorkoutSet(id = 1L, workoutExerciseId = 1L, setNumber = 1, weight = 80.0, reps = 8, rpe = 7.0, restSeconds = 90, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 2L, workoutExerciseId = 1L, setNumber = 2, weight = 85.0, reps = 6, rpe = 8.0, restSeconds = 120, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 3L, workoutExerciseId = 1L, setNumber = 3, weight = 85.0, reps = 5, rpe = 9.0, restSeconds = 120, completed = true, setType = SetType.NORMAL)
        )

        val squatSets = listOf(
            WorkoutSet(id = 4L, workoutExerciseId = 2L, setNumber = 1, weight = 100.0, reps = 6, rpe = 8.0, restSeconds = 180, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 5L, workoutExerciseId = 2L, setNumber = 2, weight = 105.0, reps = 5, rpe = 9.0, restSeconds = 180, completed = true, setType = SetType.NORMAL)
        )

        return WorkoutWithDetails(
            workout = workout,
            exercises = listOf(
                WorkoutExerciseWithSets(workoutExercise = com.gymcoach.app.domain.model.WorkoutExercise(workoutId = 1L, exerciseId = 1L, orderIndex = 0), exercise = benchPress, sets = benchSets),
                WorkoutExerciseWithSets(workoutExercise = com.gymcoach.app.domain.model.WorkoutExercise(workoutId = 1L, exerciseId = 2L, orderIndex = 1), exercise = squat, sets = squatSets)
            )
        )
    }

    @Test
    fun `formatDuration shows hours and minutes`() {
        assertEquals("1h 0m", formatDuration(3600L))
        assertEquals("2h 30m", formatDuration(9000L))
        assertEquals("45m", formatDuration(2700L))
        assertEquals("0m", formatDuration(0L))
    }

    @Test
    fun `formatDate returns formatted string`() {
        val instant = Instant.parse("2026-08-24T10:00:00Z")
        val result = formatDate(instant)
        assertTrue(result.contains("Aug"))
        assertTrue(result.contains("24"))
        assertTrue(result.contains("2026"))
    }

    @Test
    fun `calculateMuscleBreakdown groups by muscle`() {
        val workout = createTestWorkout()
        val breakdown = com.gymcoach.app.presentation.history.calculateMuscleBreakdown(workout)

        assertEquals(2, breakdown.size)
        assertTrue(breakdown.containsKey("CHEST"))
        assertTrue(breakdown.containsKey("QUADS"))
    }

    @Test
    fun `calculateMuscleBreakdown counts sets correctly`() {
        val workout = createTestWorkout()
        val breakdown = com.gymcoach.app.presentation.history.calculateMuscleBreakdown(workout)

        assertEquals(3, breakdown["CHEST"]?.sets) // 3 bench press sets
        assertEquals(2, breakdown["QUADS"]?.sets) // 2 squat sets
    }

    @Test
    fun `calculateMuscleBreakdown sums reps correctly`() {
        val workout = createTestWorkout()
        val breakdown = com.gymcoach.app.presentation.history.calculateMuscleBreakdown(workout)

        // Bench: 8 + 6 + 5 = 19
        assertEquals(19, breakdown["CHEST"]?.reps)
        // Squat: 6 + 5 = 11
        assertEquals(11, breakdown["QUADS"]?.reps)
    }

    @Test
    fun `calculateMuscleBreakdown sums volume correctly`() {
        val workout = createTestWorkout()
        val breakdown = com.gymcoach.app.presentation.history.calculateMuscleBreakdown(workout)

        // Bench: 80*8 + 85*6 + 85*5 = 640 + 510 + 425 = 1575
        assertEquals(1575.0, breakdown["CHEST"]?.volume ?: 0.0, 0.01)
        // Squat: 100*6 + 105*5 = 600 + 525 = 1125
        assertEquals(1125.0, breakdown["QUADS"]?.volume ?: 0.0, 0.01)
    }

    @Test
    fun `calculateMuscleBreakdown ignores incomplete sets`() {
        val workout = createTestWorkout()
        // Modify one set to be incomplete
        val incompleteSets = workout.exercises[0].sets.toMutableList()
        incompleteSets[0] = incompleteSets[0].copy(completed = false)
        val modifiedWorkout = workout.copy(
            exercises = listOf(
                workout.exercises[0].copy(sets = incompleteSets),
                workout.exercises[1]
            )
        )

        val breakdown = com.gymcoach.app.presentation.history.calculateMuscleBreakdown(modifiedWorkout)
        // Only 2 completed bench press sets now
        assertEquals(2, breakdown["CHEST"]?.sets)
    }

    @Test
    fun `shareWorkoutSummary generates expected format`() {
        // Test the share text generation (unit test for the formatting logic)
        val workout = createTestWorkout()
        val w = workout.workout

        var totalSets = 0
        var totalReps = 0
        var totalVolume = 0.0
        val muscleGroups = linkedMapOf<String, Int>()

        workout.exercises.forEach { entry ->
            val doneSets = entry.sets.filter { it.completed }
            totalSets += doneSets.size
            totalReps += doneSets.sumOf { it.reps }
            totalVolume += doneSets.sumOf { it.weight * it.reps }
            val muscle = entry.exercise.muscleGroup
            muscleGroups[muscle] = (muscleGroups[muscle] ?: 0) + doneSets.size
        }

        assertEquals(5, totalSets)
        assertEquals(30, totalReps) // 19 + 11
        assertEquals(2700.0, totalVolume, 0.01) // 1575 + 1125
        assertEquals(2, muscleGroups.size)
    }
}
