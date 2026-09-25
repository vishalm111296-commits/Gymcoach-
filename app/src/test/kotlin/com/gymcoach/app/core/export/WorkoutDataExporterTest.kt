package com.gymcoach.app.core.export

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import org.json.JSONObject
import org.junit.Assert.assertEquals
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

    @Test
    fun `exportToStrongCsv uses strict 12-column Strong schema`() {
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
            notes = "",
            completed = true
        )
        val sets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 5, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.5, restSeconds = 90, completed = true, setType = SetType.NORMAL)
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 5, workoutId = 10, exerciseId = 1, orderIndex = 0),
            exercise = exercise,
            sets = sets
        )
        val workoutWithDetails = WorkoutWithDetails(workout = workout, exercises = listOf(we))

        val csv = exporter.exportToStrongCsv(listOf(workoutWithDetails))
        val lines = csv.trimEnd().lines()
        assertEquals(
            "Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE",
            lines[0]
        )

        val fields = lines[1].split(",")
        assertEquals(12, fields.size)
        assertEquals("Workout", fields[1])
        assertEquals("60m", fields[2])
        assertEquals("Barbell Bench Press", fields[3])
        assertEquals("1", fields[4])
        assertEquals("100.0", fields[5])
        assertEquals("5", fields[6])
        assertEquals("0", fields[7])
        assertEquals("0", fields[8])
        assertEquals("", fields[9])
        assertEquals("", fields[10])
        assertEquals("8.5", fields[11])
        assertTrue(fields.none { it.startsWith(" ") })
    }

    @Test
    fun testExportToCsvWithCommasQuotesAndUnicode() {
        val exercise = Exercise(
            id = 3L,
            name = "Incline Press, \"Dumbbell\" — 45°",
            description = "",
            muscleGroup = "Upper Chest 🔥",
            equipment = "dumbbell",
            difficulty = "Intermediate"
        )
        val now = Instant.now()
        val workout = Workout(
            id = 30L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(1800),
            duration = 1800L,
            notes = "Felt great,\nsuperset with \"lateral raises\" & curls 🔥",
            completed = true
        )
        val sets = listOf(
            WorkoutSet(id = 15, workoutExerciseId = 22, setNumber = 1, weight = 32.5, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = SetType.NORMAL)
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 22, workoutId = 30, exerciseId = 3, orderIndex = 0),
            exercise = exercise,
            sets = sets
        )
        val workoutWithDetails = WorkoutWithDetails(workout = workout, exercises = listOf(we))

        val csv = exporter.exportToCsv(listOf(workoutWithDetails))
        assertTrue("Exercise name with comma and quotes must be quoted and quotes doubled",
            csv.contains("\"Incline Press, \"\"Dumbbell\"\" — 45°\""))
        assertTrue("Notes with newline, comma and quotes must be properly escaped",
            csv.contains("\"Felt great,\nsuperset with \"\"lateral raises\"\" & curls 🔥\""))
        assertTrue("Unicode emoji preserved", csv.contains("🔥"))

        val json = exporter.exportToJson(listOf(workoutWithDetails))
        assertTrue(json.contains("Upper Chest 🔥"))
        assertTrue(json.contains("Incline Press, \\\"Dumbbell\\\""))
        val rootObj = JSONObject(json)
        val restoredName = rootObj.getJSONArray("workouts")
            .getJSONObject(0)
            .getJSONArray("exercises")
            .getJSONObject(0)
            .getString("exerciseName")
        assertEquals("Incline Press, \"Dumbbell\" — 45°", restoredName)
    }

    @Test
    fun testExportEmptyWorkoutsList() {
        val csv = exporter.exportToCsv(emptyList())
        assertEquals("Date,Workout ID,Exercise Name,Set Order,Weight (kg),Reps,RPE,Set Type,Rest Seconds,Notes\n", csv)

        val strongCsv = exporter.exportToStrongCsv(emptyList())
        assertEquals("Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE\n", strongCsv)

        val json = exporter.exportToJson(emptyList())
        assertTrue(json.contains("\"workoutCount\": 0"))
        assertTrue(json.contains("\"workouts\": []"))
    }

    @Test
    fun `exportToStrongCsv maps SetType to standard Strong tags in Notes column`() {
        val exercise = Exercise(
            id = 5L,
            name = "Overhead Press",
            description = "",
            muscleGroup = "Shoulders",
            equipment = "barbell",
            difficulty = "Intermediate"
        )
        val now = Instant.now()
        val workout = Workout(
            id = 50L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(1800),
            duration = 1800L,
            notes = "Deload OHP",
            completed = true
        )
        val sets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 40.0, reps = 10, rpe = 6.0, restSeconds = 60, completed = true, setType = SetType.WARMUP),
            WorkoutSet(id = 2, workoutExerciseId = 1, setNumber = 2, weight = 60.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 3, workoutExerciseId = 1, setNumber = 3, weight = 45.0, reps = 8, rpe = 9.0, restSeconds = 60, completed = true, setType = SetType.DROP),
            WorkoutSet(id = 4, workoutExerciseId = 1, setNumber = 4, weight = 50.0, reps = 12, rpe = 10.0, restSeconds = 120, completed = true, setType = SetType.FAILURE)
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 1, workoutId = 50, exerciseId = 5, orderIndex = 0),
            exercise = exercise,
            sets = sets
        )
        val workoutWithDetails = WorkoutWithDetails(workout = workout, exercises = listOf(we))

        val csv = exporter.exportToStrongCsv(listOf(workoutWithDetails))
        val lines = csv.trimEnd().lines()
        assertEquals(5, lines.size) // header + 4 sets

        val warmupRow = lines[1].split(",")
        assertEquals("W", warmupRow[9])

        val normalRow = lines[2].split(",")
        assertEquals("", normalRow[9])

        val dropRow = lines[3].split(",")
        assertEquals("D", dropRow[9])

        val failureRow = lines[4].split(",")
        assertEquals("F", failureRow[9])
    }

    @Test
    fun `exportToJson includes restSeconds in set objects`() {
        val exercise = Exercise(
            id = 7L,
            name = "Pull-up",
            description = "",
            muscleGroup = "Back",
            equipment = "bodyweight",
            difficulty = "Intermediate"
        )
        val now = Instant.now()
        val workout = Workout(
            id = 70L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(1200),
            duration = 1200L,
            notes = "",
            completed = true
        )
        val sets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 0.0, reps = 12, rpe = 8.5, restSeconds = 75, completed = true, setType = SetType.NORMAL)
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 1, workoutId = 70, exerciseId = 7, orderIndex = 0),
            exercise = exercise,
            sets = sets
        )
        val workoutWithDetails = WorkoutWithDetails(workout = workout, exercises = listOf(we))

        val json = exporter.exportToJson(listOf(workoutWithDetails))
        val root = JSONObject(json)
        val setObj = root.getJSONArray("workouts")
            .getJSONObject(0)
            .getJSONArray("exercises")
            .getJSONObject(0)
            .getJSONArray("sets")
            .getJSONObject(0)

        assertEquals(75, setObj.getInt("restSeconds"))
    }

    @Test
    fun `exportToCsv with empty list returns header only`() {
        val csv = exporter.exportToCsv(emptyList())
        assertEquals("Date,Workout ID,Exercise Name,Set Order,Weight (kg),Reps,RPE,Set Type,Rest Seconds,Notes\n", csv)
    }

    @Test
    fun `exportToJson with empty list returns valid schema with zero workouts`() {
        val json = exporter.exportToJson(emptyList())
        val root = JSONObject(json)
        assertEquals(1, root.getInt("version"))
        assertEquals(0, root.getInt("workoutCount"))
        assertEquals(0, root.getJSONArray("workouts").length())
    }

    @Test
    fun `exportToCsv escapes quotes commas and newlines in exercise names and notes`() {
        val exercise = Exercise(
            id = 100L,
            name = "Incline Press, \"Paused\"",
            description = "",
            muscleGroup = "Chest",
            equipment = "barbell",
            difficulty = "Intermediate"
        )
        val now = Instant.now()
        val workout = Workout(
            id = 99L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(3600),
            duration = 3600L,
            notes = "Felt great.\nNeed longer rest.",
            completed = true
        )
        val sets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 80.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL)
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 1, workoutId = 99, exerciseId = 100, orderIndex = 0),
            exercise = exercise,
            sets = sets
        )
        val workoutWithDetails = WorkoutWithDetails(workout = workout, exercises = listOf(we))

        val csv = exporter.exportToCsv(listOf(workoutWithDetails))
        // Verify quotes are doubled and field is quoted
        assertTrue(csv.contains("\"Incline Press, \"\"Paused\"\"\""))
        assertTrue(csv.contains("\"Felt great.\nNeed longer rest.\""))
    }

    @Test
    fun `exportToJson preserves Unicode exercise names and emoji in notes`() {
        val exercise = Exercise(
            id = 200L,
            name = "Développé Couché 🏋️",
            description = "",
            muscleGroup = "Poitrine",
            equipment = "barre",
            difficulty = "Avancé"
        )
        val now = Instant.now()
        val workout = Workout(
            id = 150L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(3600),
            duration = 3600L,
            notes = "Séance excellente! 🔥 素晴らしい",
            completed = true
        )
        val we = WorkoutExerciseWithSets(
            workoutExercise = WorkoutExercise(id = 1, workoutId = 150, exerciseId = 200, orderIndex = 0),
            exercise = exercise,
            sets = listOf(
                WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 100.0, reps = 10, rpe = 9.0, restSeconds = 120, completed = true, setType = SetType.NORMAL)
            )
        )
        val json = exporter.exportToJson(listOf(WorkoutWithDetails(workout = workout, exercises = listOf(we))))
        assertTrue(json.contains("Développé Couché 🏋️"))
        assertTrue(json.contains("Séance excellente! 🔥 素晴らしい"))
    }

    @Test
    fun `exportToStrongCsv handles multi-hour durations sub-minute floor and all SetType tags`() {
        val exercise = Exercise(id = 1L, name = "Squat", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "Advanced")
        val now = Instant.now()

        // Workout 1: 2 hours (7200 seconds) -> 120m
        val w1 = Workout(id = 1, date = now, startTime = now, endTime = now.plusSeconds(7200), duration = 7200, notes = "", completed = true)
        val sets1 = listOf(
            WorkoutSet(1, 1, 1, 140.0, 5, 8.0, 180, true, SetType.NORMAL),
            WorkoutSet(2, 1, 2, 70.0, 10, 0.0, 60, true, SetType.WARMUP),
            WorkoutSet(3, 1, 3, 100.0, 12, 10.0, 90, true, SetType.DROP),
            WorkoutSet(4, 1, 4, 120.0, 6, 10.0, 120, true, SetType.FAILURE)
        )
        val we1 = WorkoutExerciseWithSets(WorkoutExercise(1, 1, 1, 0), exercise, sets1)

        // Workout 2: 30 seconds -> coerced to 1m
        val w2 = Workout(id = 2, date = now, startTime = now, endTime = now.plusSeconds(30), duration = 30, notes = "", completed = true)
        val we2 = WorkoutExerciseWithSets(WorkoutExercise(2, 2, 1, 0), exercise, listOf(WorkoutSet(5, 2, 1, 100.0, 5, 8.0, 60, true, SetType.NORMAL)))

        val csv = exporter.exportToStrongCsv(listOf(WorkoutWithDetails(w1, listOf(we1)), WorkoutWithDetails(w2, listOf(we2))))
        val lines = csv.trimEnd().lines()

        // 1 header + 4 sets + 1 set = 6 lines
        assertEquals(6, lines.size)

        // Check durations
        assertTrue(lines[1].contains(",120m,Squat,1,140.0,5,0,0,,")) // Normal tag is empty
        assertTrue(lines[2].contains(",120m,Squat,2,70.0,10,0,0,W,")) // Warmup tag is W
        assertTrue(lines[3].contains(",120m,Squat,3,100.0,12,0,0,D,")) // Drop tag is D
        assertTrue(lines[4].contains(",120m,Squat,4,120.0,6,0,0,F,")) // Failure tag is F
        assertTrue(lines[5].contains(",1m,Squat,1,100.0,5,0,0,,"))    // Sub-minute clamped to 1m
    }
}
