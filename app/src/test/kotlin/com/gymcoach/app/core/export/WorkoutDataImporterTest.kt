package com.gymcoach.app.core.export

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class WorkoutDataImporterTest {

    private lateinit var importer: WorkoutDataImporter

    @Before
    fun setup() {
        importer = WorkoutDataImporter()
    }

    @Test
    fun `parseJson returns valid ImportedWorkoutData for well-formed JSON`() {
        val validJson = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workoutCount": 1,
              "workouts": [
                {
                  "id": 100,
                  "date": "2023-01-01 10:00:00",
                  "startTime": 1672567200000,
                  "endTime": 1672570800000,
                  "durationSeconds": 3600,
                  "completed": true,
                  "notes": "Great session",
                  "exercises": [
                    {
                      "exerciseId": 50,
                      "exerciseName": "Bench Press",
                      "muscleGroup": "Chest",
                      "sets": [
                        {
                          "setNumber": 1,
                          "weight": 80.5,
                          "reps": 10,
                          "rpe": 8,
                          "completed": true,
                          "setType": "NORMAL"
                        },
                        {
                          "setNumber": 2,
                          "weight": 80.5,
                          "reps": 8,
                          "rpe": null,
                          "completed": true,
                          "setType": "DROP"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(validJson)

        assertTrue(result.isSuccess)
        val data = result.getOrNull()!!
        assertEquals(1, data.version)
        assertEquals(1672531200000L, data.exportedAt)
        assertEquals(1, data.workouts.size)

        val workoutWithDetails = data.workouts.first()
        val workout = workoutWithDetails.workout
        assertEquals(100L, workout.id)
        assertEquals(Instant.ofEpochMilli(1672567200000L), workout.startTime)
        assertEquals(Instant.ofEpochMilli(1672570800000L), workout.endTime)
        assertEquals(3600L, workout.duration)
        assertTrue(workout.completed)
        assertEquals("Great session", workout.notes)

        assertEquals(1, workoutWithDetails.exercises.size)
        val exercise = workoutWithDetails.exercises.first()
        assertEquals(50L, exercise.exercise.id)
        assertEquals("Bench Press", exercise.exercise.name)
        assertEquals("Chest", exercise.exercise.muscleGroup)

        assertEquals(2, exercise.sets.size)

        val set1 = exercise.sets[0]
        assertEquals(1, set1.setNumber)
        assertEquals(80.5, set1.weight, 0.001)
        assertEquals(10, set1.reps)
        assertEquals(8.0, set1.rpe, 0.001)
        assertTrue(set1.completed)
        assertEquals(SetType.NORMAL, set1.setType)

        val set2 = exercise.sets[1]
        assertEquals(2, set2.setNumber)
        assertEquals(80.5, set2.weight, 0.001)
        assertEquals(8, set2.reps)
        assertEquals(0.0, set2.rpe, 0.001) // null mapped to 0.0
        assertTrue(set2.completed)
        assertEquals(SetType.DROP, set2.setType)
    }

    @Test
    fun `parseJson returns failure for unsupported version`() {
        val json = """
            {
              "version": 2,
              "exportedAt": 1672531200000,
              "workouts": []
            }
        """.trimIndent()

        val result = importer.parseJson(json)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("Unsupported or missing schema version") == true)
    }

    @Test
    fun `parseJson returns failure for missing required fields`() {
        val json = """
            {
              "version": 1
            }
        """.trimIndent() // Missing exportedAt and workouts

        val result = importer.parseJson(json)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("Missing required keys") == true)
    }

    @Test
    fun `parseJson returns failure for invalid setNumber`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "startTime": 0,
                  "endTime": 0,
                  "durationSeconds": 0,
                  "completed": true,
                  "exercises": [
                    {
                      "exerciseId": 1,
                      "exerciseName": "E",
                      "muscleGroup": "M",
                      "sets": [
                        {
                          "setNumber": 0,
                          "weight": 10.0,
                          "reps": 10,
                          "completed": true,
                          "setType": "NORMAL"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid setNumber") == true)
    }

    @Test
    fun `parseJson returns failure for invalid weight`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "startTime": 0,
                  "endTime": 0,
                  "durationSeconds": 0,
                  "completed": true,
                  "exercises": [
                    {
                      "exerciseId": 1,
                      "exerciseName": "E",
                      "muscleGroup": "M",
                      "sets": [
                        {
                          "setNumber": 1,
                          "weight": -5.0,
                          "reps": 10,
                          "completed": true,
                          "setType": "NORMAL"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid weight") == true)
    }

    @Test
    fun `parseJson returns failure for invalid reps`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "startTime": 0,
                  "endTime": 0,
                  "durationSeconds": 0,
                  "completed": true,
                  "exercises": [
                    {
                      "exerciseId": 1,
                      "exerciseName": "E",
                      "muscleGroup": "M",
                      "sets": [
                        {
                          "setNumber": 1,
                          "weight": 10.0,
                          "reps": -1,
                          "completed": true,
                          "setType": "NORMAL"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid reps") == true)
    }

    @Test
    fun `parseJson returns failure for unknown SetType`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "startTime": 0,
                  "endTime": 0,
                  "durationSeconds": 0,
                  "completed": true,
                  "exercises": [
                    {
                      "exerciseId": 1,
                      "exerciseName": "E",
                      "muscleGroup": "M",
                      "sets": [
                        {
                          "setNumber": 1,
                          "weight": 10.0,
                          "reps": 10,
                          "completed": true,
                          "setType": "UNKNOWN_TYPE"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unknown SetType") == true)
    }

    @Test
    fun `parseJson returns failure for corrupted JSON`() {
        val json = "{"

        val result = importer.parseJson(json)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid JSON syntax") == true)
    }

    @Test
    fun `parseJson returns failure for negative durationSeconds`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "startTime": 1672531200000,
                  "endTime": 1672531200000,
                  "durationSeconds": -10,
                  "completed": true,
                  "exercises": []
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid durationSeconds") == true)
    }

    @Test
    fun `parseJson returns failure for blank exerciseName`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "startTime": 1672531200000,
                  "endTime": 1672531200000,
                  "durationSeconds": 60,
                  "completed": true,
                  "exercises": [
                    {
                      "exerciseId": 1,
                      "exerciseName": "   ",
                      "muscleGroup": "Chest",
                      "sets": []
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("exerciseName cannot be blank") == true)
    }

    @Test
    fun `parseJson parses workout using date string when startTime is omitted`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "date": "2026-09-18 10:30:00",
                  "durationSeconds": 1800,
                  "completed": true,
                  "exercises": []
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)
        assertTrue(result.isSuccess)
        val data = result.getOrNull()!!
        assertEquals(1, data.workouts.size)
        assertEquals(1800L, data.workouts[0].workout.duration)
    }

    @Test
    fun `exportToJson and parseJson roundtrip achieves full data fidelity`() {
        val exporter = WorkoutDataExporter()
        val exercise = Exercise(
            id = 42L,
            name = "Overhead Barbell Press",
            description = "",
            muscleGroup = "Shoulders",
            equipment = "barbell",
            difficulty = "Intermediate"
        )
        val startTime = Instant.ofEpochMilli(1700000000000L)
        val endTime = Instant.ofEpochMilli(1700003600000L)
        val workout = Workout(
            id = 77L,
            date = startTime,
            startTime = startTime,
            endTime = endTime,
            duration = 3600L,
            notes = "Strict overhead presses, clean reps",
            completed = true
        )
        val sets = listOf(
            WorkoutSet(
                id = 1,
                workoutExerciseId = 10,
                setNumber = 1,
                weight = 50.0,
                reps = 10,
                rpe = 7.0,
                restSeconds = 90,
                completed = true,
                setType = SetType.WARMUP
            ),
            WorkoutSet(
                id = 2,
                workoutExerciseId = 10,
                setNumber = 2,
                weight = 65.0,
                reps = 6,
                rpe = 9.0,
                restSeconds = 120,
                completed = true,
                setType = SetType.NORMAL
            ),
            WorkoutSet(
                id = 3,
                workoutExerciseId = 10,
                setNumber = 3,
                weight = 45.0,
                reps = 12,
                rpe = 9.5,
                restSeconds = 60,
                completed = true,
                setType = SetType.DROP
            )
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 10, workoutId = 77, exerciseId = 42, orderIndex = 0),
            exercise = exercise,
            sets = sets
        )
        val originalWorkout = WorkoutWithDetails(workout = workout, exercises = listOf(we))

        val exportedJson = exporter.exportToJson(listOf(originalWorkout))
        val importResult = importer.parseJson(exportedJson)

        assertTrue("Import must succeed", importResult.isSuccess)
        val importedData = importResult.getOrNull()!!
        assertEquals(1, importedData.workouts.size)

        val importedWorkoutWithDetails = importedData.workouts[0]
        val importedWorkout = importedWorkoutWithDetails.workout
        assertEquals(originalWorkout.workout.id, importedWorkout.id)
        assertEquals(originalWorkout.workout.startTime, importedWorkout.startTime)
        assertEquals(originalWorkout.workout.endTime, importedWorkout.endTime)
        assertEquals(originalWorkout.workout.duration, importedWorkout.duration)
        assertEquals(originalWorkout.workout.notes, importedWorkout.notes)
        assertEquals(originalWorkout.workout.completed, importedWorkout.completed)

        assertEquals(1, importedWorkoutWithDetails.exercises.size)
        val importedExerciseWithSets = importedWorkoutWithDetails.exercises[0]
        assertEquals(originalWorkout.exercises[0].exercise.name, importedExerciseWithSets.exercise.name)
        assertEquals(originalWorkout.exercises[0].exercise.muscleGroup, importedExerciseWithSets.exercise.muscleGroup)

        assertEquals(3, importedExerciseWithSets.sets.size)
        for (i in 0 until 3) {
            val expectedSet = sets[i]
            val actualSet = importedExerciseWithSets.sets[i]
            assertEquals(expectedSet.setNumber, actualSet.setNumber)
            assertEquals(expectedSet.weight, actualSet.weight, 0.001)
            assertEquals(expectedSet.reps, actualSet.reps)
            assertEquals(expectedSet.rpe, actualSet.rpe, 0.001)
            assertEquals(expectedSet.completed, actualSet.completed)
            assertEquals(expectedSet.setType, actualSet.setType)
            assertEquals(expectedSet.restSeconds, actualSet.restSeconds)
        }
    }

    @Test
    fun `parseJson returns failure on malformed json syntax`() {
        val malformed = "{ version: 1, exportedAt: 12345, workouts: ["
        val result = importer.parseJson(malformed)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid JSON syntax") == true)
    }

    @Test
    fun `parseJson returns failure when version is missing or unsupported`() {
        // Missing version
        val noVersion = """{ "exportedAt": 1000, "workouts": [] }"""
        val result1 = importer.parseJson(noVersion)
        assertTrue(result1.isFailure)
        assertTrue(result1.exceptionOrNull()?.message?.contains("Unsupported or missing schema version") == true)

        // Version 2 (unsupported)
        val v2 = """{ "version": 2, "exportedAt": 1000, "workouts": [] }"""
        val result2 = importer.parseJson(v2)
        assertTrue(result2.isFailure)
        assertTrue(result2.exceptionOrNull()?.message?.contains("Expected 1, got 2") == true)
    }

    @Test
    fun `parseJson returns failure when required exportedAt or workouts is missing`() {
        val noWorkouts = """{ "version": 1, "exportedAt": 1000 }"""
        val result = importer.parseJson(noWorkouts)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Missing required keys") == true)
    }

    @Test
    fun `parseJson parses date string when startTime is omitted and defaults endTime`() {
        val jsonWithDateOnly = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 55,
                  "date": "2023-06-15 10:30:00",
                  "durationSeconds": 1800,
                  "completed": true,
                  "notes": "Morning cardio & push",
                  "exercises": []
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(jsonWithDateOnly)
        assertTrue(result.isSuccess)
        val workout = result.getOrNull()!!.workouts.first().workout
        assertEquals(55L, workout.id)
        assertEquals(1800L, workout.duration)
        assertEquals(workout.startTime, workout.endTime) // endTime defaults to startTime
        assertTrue(workout.startTime.toEpochMilli() > 0)
    }

    @Test
    fun `parseJson defaults restSeconds and rpe when omitted from set json`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "startTime": 1672567200000,
                  "durationSeconds": 600,
                  "completed": true,
                  "exercises": [
                    {
                      "exerciseId": 10,
                      "exerciseName": "Pull Up",
                      "muscleGroup": "Back",
                      "sets": [
                        {
                          "setNumber": 1,
                          "weight": 0.0,
                          "reps": 10,
                          "completed": true,
                          "setType": "NORMAL"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)
        assertTrue(result.isSuccess)
        val set = result.getOrNull()!!.workouts.first().exercises.first().sets.first()
        assertEquals(0, set.restSeconds) // defaulted
        assertEquals(0.0, set.rpe, 0.001) // defaulted
    }

    @Test
    fun `parseJson preserves unicode names and emoji notes`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 88,
                  "startTime": 1672567200000,
                  "durationSeconds": 3600,
                  "completed": true,
                  "notes": "Top séance! 🔥 素晴らしい",
                  "exercises": [
                    {
                      "exerciseId": 99,
                      "exerciseName": "Développé Couché 🏋️",
                      "muscleGroup": "Pectoraux",
                      "sets": [
                        {
                          "setNumber": 1,
                          "weight": 100.0,
                          "reps": 10,
                          "completed": true,
                          "setType": "NORMAL"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)
        assertTrue(result.isSuccess)
        val workout = result.getOrNull()!!.workouts.first()
        assertEquals("Top séance! 🔥 素晴らしい", workout.workout.notes)
        assertEquals("Développé Couché 🏋️", workout.exercises.first().exercise.name)
    }

    @Test
    fun `parseJson handles exercises with empty sets list`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1672531200000,
              "workouts": [
                {
                  "id": 1,
                  "startTime": 1672567200000,
                  "durationSeconds": 1800,
                  "completed": true,
                  "exercises": [
                    {
                      "exerciseId": 10,
                      "exerciseName": "Bench Press",
                      "muscleGroup": "Chest",
                      "sets": []
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = importer.parseJson(json)
        assertTrue(result.isSuccess)
        val workout = result.getOrNull()!!.workouts.first()
        assertEquals(1, workout.exercises.size)
        assertTrue(workout.exercises.first().sets.isEmpty())
    }
}
