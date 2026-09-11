package com.gymcoach.app.core.export

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WorkoutDataExporterTest {

    private val exporter = WorkoutDataExporter()

    @Test
    fun testExportToCsvFormat() {
        val exercise = Exercise(
            id = 1L,
            name = "Barbell Bench Press",
            description = "",
            muscleGroup = "Chest",
            equipment = "barbell",
            difficulty = "Intermediate"
        )
        val now = Instant.now()
        val workout = Workout(
            id = 10L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(3600),
            duration = 3600L,
            notes = "Great pump, felt strong",
            completed = true
        )
        val sets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 5, setNumber = 1, weight = 80.0, reps = 10, rpe = 7.5, restSeconds = 90, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 2, workoutExerciseId = 5, setNumber = 2, weight = 85.0, reps = 8, rpe = 8.5, restSeconds = 90, completed = true, setType = SetType.NORMAL)
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 5, workoutId = 10, exerciseId = 1, orderIndex = 0),
            exercise = exercise,
            sets = sets
        )
        val workoutWithDetails = WorkoutWithDetails(workout = workout, exercises = listOf(we))

        val csv = exporter.exportToCsv(listOf(workoutWithDetails))

        assertTrue(csv.startsWith("Date,Workout ID,Exercise Name,Set Order,Weight (kg),Reps,RPE,Set Type,Rest Seconds,Notes\n"))
        assertTrue(csv.contains("Barbell Bench Press"))
        assertTrue(csv.contains("Workout #10"))
        assertTrue(csv.contains(",1,80.0,10,7.5,NORMAL,90,"))
        assertTrue(csv.contains(",2,85.0,8,8.5,NORMAL,90,"))
        assertTrue(csv.contains("Great pump, felt strong"))
    }

    @Test
    fun testExportToJsonFormat() {
        val exercise = Exercise(
            id = 2L,
            name = "Barbell Squat",
            description = "",
            muscleGroup = "Legs",
            equipment = "barbell",
            difficulty = "Advanced"
        )
        val now = Instant.now()
        val workout = Workout(
            id = 20L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(4200),
            duration = 4200L,
            notes = "Heavy squats",
            completed = true
        )
        val sets = listOf(
            WorkoutSet(id = 10, workoutExerciseId = 12, setNumber = 1, weight = 120.0, reps = 5, rpe = 9.0, restSeconds = 180, completed = true, setType = SetType.NORMAL)
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 12, workoutId = 20, exerciseId = 2, orderIndex = 0),
            exercise = exercise,
            sets = sets
        )
        val workoutWithDetails = WorkoutWithDetails(workout = workout, exercises = listOf(we))

        val json = exporter.exportToJson(listOf(workoutWithDetails))

        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"workoutCount\": 1"))
        assertTrue(json.contains("\"id\": 20"))
        assertTrue(json.contains("\"exerciseName\": \"Barbell Squat\""))
        assertTrue(json.contains("\"weight\": 120"))
        assertTrue(json.contains("\"reps\": 5"))
    }
}
