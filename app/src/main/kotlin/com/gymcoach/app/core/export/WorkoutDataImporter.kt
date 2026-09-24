package com.gymcoach.app.core.export

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class ImportedWorkoutData(
    val version: Int,
    val exportedAt: Long,
    val workouts: List<WorkoutWithDetails>
)

@Singleton
class WorkoutDataImporter @Inject constructor() {

    fun parseJson(jsonString: String): Result<ImportedWorkoutData> {
        return try {
            val root = JSONObject(jsonString)

            val version = root.optInt("version", -1)
            if (version != 1) {
                return Result.failure(IllegalArgumentException("Unsupported or missing schema version. Expected 1, got $version"))
            }

            if (!root.has("exportedAt") || !root.has("workouts")) {
                return Result.failure(IllegalArgumentException("Missing required keys: exportedAt or workouts"))
            }

            val exportedAt = root.getLong("exportedAt")
            val workoutsArray = root.getJSONArray("workouts")

            val workoutsList = mutableListOf<WorkoutWithDetails>()

            for (i in 0 until workoutsArray.length()) {
                val wObj = workoutsArray.getJSONObject(i)

                val workoutId = wObj.getLong("id")
                val startTimeMillis = if (wObj.has("startTime")) {
                    wObj.getLong("startTime")
                } else if (wObj.has("date")) {
                    try {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .parse(wObj.getString("date"), Instant::from)
                            .toEpochMilli()
                    } catch (e: Exception) {
                        return Result.failure(IllegalArgumentException("Invalid date format: ${wObj.getString("date")}", e))
                    }
                } else {
                    return Result.failure(IllegalArgumentException("Missing required date or startTime"))
                }
                val endTimeMillis = if (wObj.has("endTime")) wObj.getLong("endTime") else startTimeMillis
                val durationSeconds = wObj.getLong("durationSeconds")
                if (durationSeconds < 0) {
                    return Result.failure(IllegalArgumentException("Invalid durationSeconds $durationSeconds. Must be >= 0"))
                }
                val completed = wObj.getBoolean("completed")
                val notes = if (wObj.has("notes") && !wObj.isNull("notes")) wObj.getString("notes") else ""

                val workout = Workout(
                    id = workoutId,
                    date = Instant.ofEpochMilli(startTimeMillis),
                    startTime = Instant.ofEpochMilli(startTimeMillis),
                    endTime = Instant.ofEpochMilli(endTimeMillis),
                    duration = durationSeconds,
                    notes = notes,
                    completed = completed
                )

                val exercisesArray = wObj.getJSONArray("exercises")
                val exerciseList = mutableListOf<WorkoutExerciseWithSets>()

                for (j in 0 until exercisesArray.length()) {
                    val eObj = exercisesArray.getJSONObject(j)

                    val exerciseId = eObj.getLong("exerciseId")
                    val exerciseName = eObj.getString("exerciseName")
                    if (exerciseName.isBlank()) {
                        return Result.failure(IllegalArgumentException("exerciseName cannot be blank"))
                    }
                    val muscleGroup = eObj.getString("muscleGroup")

                    val exercise = Exercise(
                        id = exerciseId,
                        name = exerciseName,
                        muscleGroup = muscleGroup,
                        description = "",
                        equipment = "",
                        difficulty = ""
                    )

                    val workoutExercise = WorkoutExercise(
                        id = 0,
                        workoutId = workoutId,
                        exerciseId = exerciseId,
                        orderIndex = j
                    )

                    val setsArray = eObj.getJSONArray("sets")
                    val setList = mutableListOf<WorkoutSet>()

                    for (k in 0 until setsArray.length()) {
                        val sObj = setsArray.getJSONObject(k)

                        val setNumber = sObj.getInt("setNumber")
                        if (setNumber < 1) {
                            return Result.failure(IllegalArgumentException("Invalid setNumber $setNumber. Must be >= 1"))
                        }

                        val weight = sObj.getDouble("weight")
                        if (weight < 0.0) {
                            return Result.failure(IllegalArgumentException("Invalid weight $weight. Must be >= 0.0"))
                        }

                        val reps = sObj.getInt("reps")
                        if (reps < 0) {
                            return Result.failure(IllegalArgumentException("Invalid reps $reps. Must be >= 0"))
                        }

                        val rpe = if (sObj.has("rpe") && !sObj.isNull("rpe")) sObj.getDouble("rpe") else 0.0
                        val setCompleted = sObj.getBoolean("completed")

                        val setTypeStr = sObj.getString("setType")
                        val mappedSetTypeStr = if (setTypeStr == "DROPSET") "DROP" else setTypeStr
                        val setType = try {
                            SetType.valueOf(mappedSetTypeStr)
                        } catch (e: IllegalArgumentException) {
                            return Result.failure(IllegalArgumentException("Unknown SetType: $setTypeStr"))
                        }

                        val restSeconds = if (sObj.has("restSeconds") && !sObj.isNull("restSeconds")) sObj.getInt("restSeconds") else 0

                        setList.add(WorkoutSet(
                            id = 0,
                            workoutExerciseId = 0,
                            setNumber = setNumber,
                            weight = weight,
                            reps = reps,
                            rpe = rpe,
                            restSeconds = restSeconds,
                            completed = setCompleted,
                            setType = setType
                        ))
                    }

                    exerciseList.add(WorkoutExerciseWithSets(
                        workoutExercise = workoutExercise,
                        exercise = exercise,
                        sets = setList
                    ))
                }

                workoutsList.add(WorkoutWithDetails(
                    workout = workout,
                    exercises = exerciseList
                ))
            }

            Result.success(ImportedWorkoutData(version, exportedAt, workoutsList))

        } catch (e: JSONException) {
            Result.failure(IllegalArgumentException("Invalid JSON syntax or missing fields: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Error parsing workout data: ${e.message}", e))
        }
    }
}
