package com.gymcoach.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CoachingEngineTest {

    @Test
    fun testRecommendOverloadEmpty() {
        val recommendation = CoachingEngine.recommendOverload("Squat", emptyList(), 8, 12, true)
        assertEquals(0.0, recommendation.recommendedWeight, 0.001)
        assertEquals(8, recommendation.recommendedReps)
    }

    @Test
    fun testRecommendOverloadWeightedProgress() {
        val lastSets = listOf(
            WorkoutSet(completed = true, weight = 100.0, reps = 12, workoutExerciseId = 1, setNumber = 1, rpe = 8.0, restSeconds = 60),
            WorkoutSet(completed = true, weight = 100.0, reps = 12, workoutExerciseId = 1, setNumber = 2, rpe = 8.0, restSeconds = 60)
        )
        val recommendation = CoachingEngine.recommendOverload("Squat", lastSets, 8, 12, true)
        assertEquals(102.5, recommendation.recommendedWeight, 0.001)
        assertEquals(8, recommendation.recommendedReps)
        assertEquals("Target reps hit on all sets. Recommending weight increase.", recommendation.explanation)
    }

    @Test
    fun testRecommendOverloadBodyweightProgress() {
        val lastSets = listOf(
            WorkoutSet(completed = true, weight = 0.0, reps = 12, workoutExerciseId = 1, setNumber = 1, rpe = 8.0, restSeconds = 60)
        )
        val recommendation = CoachingEngine.recommendOverload("Pushup", lastSets, 8, 12, false)
        assertEquals(0.0, recommendation.recommendedWeight, 0.001)
        assertEquals(14, recommendation.recommendedReps)
    }

    @Test
    fun testRecommendOverloadSameWeight() {
        val lastSets = listOf(
            WorkoutSet(completed = true, weight = 100.0, reps = 12, workoutExerciseId = 1, setNumber = 1, rpe = 8.0, restSeconds = 60),
            WorkoutSet(completed = true, weight = 100.0, reps = 10, workoutExerciseId = 1, setNumber = 2, rpe = 8.0, restSeconds = 60)
        )
        val recommendation = CoachingEngine.recommendOverload("Squat", lastSets, 8, 12, true)
        assertEquals(100.0, recommendation.recommendedWeight, 0.001)
        assertEquals(12, recommendation.recommendedReps)
        assertEquals("Work on hitting target rep range of 8-12 across all sets with current weight.", recommendation.explanation)
    }

    @Test
    fun testCalculateE1RM() {
        val e1rm = CoachingEngine.calculateE1RM(100.0, 10)
        assertEquals(133.333, e1rm, 0.001)
    }

    @Test
    fun testFilterByEquipment() {
        val ex1 = Exercise(name = "Pushup", description = "", muscleGroup = "Chest", equipment = "Bodyweight", difficulty = "Beginner")
        val ex2 = Exercise(name = "Bench Press", description = "", muscleGroup = "Chest", equipment = "Barbell", difficulty = "Intermediate")
        val ex3 = Exercise(name = "Dumbbell Fly", description = "", muscleGroup = "Chest", equipment = "Dumbbell", difficulty = "Intermediate")
        val exercises = listOf(ex1, ex2, ex3)
        val allowed = listOf("Barbell")
        val filtered = CoachingEngine.filterByEquipment(exercises, allowed)
        assertEquals(2, filtered.size)
        assertTrue(filtered.contains(ex1))
        assertTrue(filtered.contains(ex2))
    }

    @Test
    fun testCalculateMuscleSetVolumes() {
        val ex1 = Exercise(name = "Squat", description = "", muscleGroup = "Quads", equipment = "Barbell", difficulty = "Intermediate", secondaryMuscles = "Glutes, Hamstrings")
        val wSet1 = WorkoutSet(completed = true, weight = 100.0, reps = 10, workoutExerciseId = 1, setNumber = 1, rpe = 8.0, restSeconds = 60)
        val wSet2 = WorkoutSet(completed = true, weight = 100.0, reps = 10, workoutExerciseId = 1, setNumber = 2, rpe = 8.0, restSeconds = 60)
        val details = WorkoutWithDetails(
            workout = Workout(date = Instant.now(), startTime = Instant.now(), endTime = Instant.now(), duration = 0, notes = "", completed = true),
            exercises = listOf(
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(workoutId = 1, exerciseId = 1, orderIndex = 0),
                    exercise = ex1,
                    sets = listOf(wSet1, wSet2)
                )
            )
        )
        val volumes = CoachingEngine.calculateMuscleSetVolumes(listOf(details))
        assertEquals(2.0, volumes["Quads"] ?: 0.0, 0.001)
        assertEquals(1.0, volumes["Glutes"] ?: 0.0, 0.001)
        assertEquals(1.0, volumes["Hamstrings"] ?: 0.0, 0.001)
    }
}
