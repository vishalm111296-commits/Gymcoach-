package com.gymcoach.app.core.export

import com.gymcoach.app.domain.model.WorkoutWithDetails
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutDataExporter @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun exportToCsv(workouts: List<WorkoutWithDetails>): String {
        val sb = StringBuilder()
        sb.append("Date,Workout ID,Exercise Name,Set Order,Weight (kg),Reps,RPE,Set Type,Rest Seconds,Notes\n")

        for (workout in workouts) {
            val dateStr = dateFormatter.format(workout.workout.startTime)
            val workoutLabel = "Workout #${workout.workout.id}"
            val notes = escapeCsv(workout.workout.notes)

            for (we in workout.exercises) {
                val exerciseName = escapeCsv(we.exercise.name)
                for (set in we.sets) {
                    val weight = set.weight
                    val reps = set.reps
                    val rpe = if (set.rpe > 0) set.rpe.toString() else ""
                    val setType = set.setType.name
                    val rest = set.restSeconds

                    sb.append(dateStr).append(',')
                        .append(workoutLabel).append(',')
                        .append(exerciseName).append(',')
                        .append(set.setNumber).append(',')
                        .append(weight).append(',')
                        .append(reps).append(',')
                        .append(rpe).append(',')
                        .append(setType).append(',')
                        .append(rest).append(',')
                        .append(notes).append('\n')
                }
            }
        }

        return sb.toString()
    }

    fun exportToJson(workouts: List<WorkoutWithDetails>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("workoutCount", workouts.size)

        val workoutsArray = JSONArray()
        for (w in workouts) {
            val wObj = JSONObject()
            wObj.put("id", w.workout.id)
            wObj.put("date", dateFormatter.format(w.workout.startTime))
            wObj.put("startTime", w.workout.startTime.toEpochMilli())
            wObj.put("endTime", w.workout.endTime.toEpochMilli())
            wObj.put("durationSeconds", w.workout.duration)
            wObj.put("completed", w.workout.completed)
            wObj.put("notes", w.workout.notes)

            val exArray = JSONArray()
            for (e in w.exercises) {
                val eObj = JSONObject()
                eObj.put("exerciseId", e.exercise.id)
                eObj.put("exerciseName", e.exercise.name)
                eObj.put("muscleGroup", e.exercise.muscleGroup)

                val setsArray = JSONArray()
                for (s in e.sets) {
                    val sObj = JSONObject()
                    sObj.put("setNumber", s.setNumber)
                    sObj.put("weight", s.weight)
                    sObj.put("reps", s.reps)
                    sObj.put("rpe", s.rpe)
                    sObj.put("completed", s.completed)
                    sObj.put("setType", s.setType.name)
                    setsArray.put(sObj)
                }
                eObj.put("sets", setsArray)
                exArray.put(eObj)
            }
            wObj.put("exercises", exArray)
            workoutsArray.put(wObj)
        }
        root.put("workouts", workoutsArray)
        return root.toString(2)
    }

    private fun escapeCsv(str: String): String {
        if (str.contains(',') || str.contains('"') || str.contains('\n') || str.contains('\r')) {
            val escaped = str.replace("\"", "\"\"")
            return "\"$escaped\""
        }
        return str
    }
}
