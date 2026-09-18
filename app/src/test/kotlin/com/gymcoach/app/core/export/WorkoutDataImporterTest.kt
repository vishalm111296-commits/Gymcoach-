package com.gymcoach.app.core.export

import com.gymcoach.app.domain.model.SetType
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
}
