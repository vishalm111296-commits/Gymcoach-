package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramGenerator @Inject constructor(
    private val exerciseDao: ExerciseDao
) {
    data class GeneratedProgram(
        val name: String, val description: String, val goal: String,
        val frequency: Int, val days: List<ProgramDay>
    )
    data class ProgramDay(
        val dayNumber: Int, val name: String,
        val targetMuscles: List<String>, val exercises: List<ProgramExercise>
    )
    data class ProgramExercise(
        val exerciseId: Long, val exerciseName: String,
        val targetSets: Int, val targetRepsMin: Int, val targetRepsMax: Int,
        val targetRpe: Double, val restSeconds: Int
    )

    suspend fun generateProgram(
        frequency: Int, equipmentType: String, experienceLevel: String, goal: String
    ): GeneratedProgram {
        val allExercises = exerciseDao.getAll()
        val days = when (frequency) {
            3 -> generateFullBody(allExercises, equipmentType)
            4 -> generateUpperLower(allExercises, equipmentType)
            5 -> generatePPLUpperLower(allExercises, equipmentType)
            6 -> generatePPLDouble(allExercises, equipmentType)
            else -> generateUpperLower(allExercises, equipmentType)
        }
        return GeneratedProgram(
            name = "V-Taper $frequency-Day Program",
            description = "Personalized $frequency-day training program for $goal",
            goal = goal, frequency = frequency, days = days
        )
    }

    private fun generateUpperLower(exercises: List<ExerciseEntity>, equipment: String): List<ProgramDay> {
        val upper1 = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps")
        val lower1 = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves")
        val upper2 = listOf("Back", "Chest", "Shoulders", "Biceps", "Triceps")
        val lower2 = listOf("Hamstrings", "Quadriceps", "Glutes", "Core")
        return listOf(
            buildDay(1, "Upper A", upper1, exercises, equipment),
            buildDay(2, "Lower A", lower1, exercises, equipment),
            buildDay(3, "Upper B", upper2, exercises, equipment),
            buildDay(4, "Lower B", lower2, exercises, equipment)
        )
    }

    private fun generateFullBody(exercises: List<ExerciseEntity>, equipment: String): List<ProgramDay> {
        return listOf(
            buildDay(1, "Full Body A", listOf("Chest", "Back", "Quadriceps", "Shoulders", "Core"), exercises, equipment),
            buildDay(2, "Full Body B", listOf("Back", "Chest", "Hamstrings", "Shoulders", "Biceps"), exercises, equipment),
            buildDay(3, "Full Body C", listOf("Chest", "Back", "Glutes", "Shoulders", "Triceps"), exercises, equipment)
        )
    }

    private fun generatePPLUpperLower(exercises: List<ExerciseEntity>, equipment: String): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Shoulders", "Triceps"), exercises, equipment),
            buildDay(2, "Pull", listOf("Back", "Biceps"), exercises, equipment),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, equipment),
            buildDay(4, "Upper", listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps"), exercises, equipment),
            buildDay(5, "Lower", listOf("Quadriceps", "Hamstrings", "Glutes", "Core"), exercises, equipment)
        )
    }

    private fun generatePPLDouble(exercises: List<ExerciseEntity>, equipment: String): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Shoulders", "Triceps"), exercises, equipment),
            buildDay(2, "Pull", listOf("Back", "Biceps"), exercises, equipment),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, equipment),
            buildDay(4, "Push", listOf("Chest", "Shoulders", "Triceps"), exercises, equipment),
            buildDay(5, "Pull", listOf("Back", "Biceps"), exercises, equipment),
            buildDay(6, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, equipment)
        )
    }

    private fun buildDay(dayNum: Int, name: String, muscles: List<String>, allExercises: List<ExerciseEntity>, equipment: String): ProgramDay {
        val selected = mutableListOf<ProgramExercise>()
        for (muscle in muscles) {
            val candidates = allExercises.filter {
                it.muscleGroup.equals(muscle, ignoreCase = true) ||
                it.secondaryMuscles.contains(muscle, ignoreCase = true)
            }.filter {
                equipment == "gym" || it.equipment == "Bodyweight" || it.equipment == "Dumbbell"
            }.take(2)
            for (ex in candidates) {
                selected.add(ProgramExercise(
                    exerciseId = ex.id, exerciseName = ex.name,
                    targetSets = 3, targetRepsMin = 8, targetRepsMax = 12,
                    targetRpe = 7.5, restSeconds = 90
                ))
            }
        }
        return ProgramDay(dayNum, name, muscles, selected)
    }
}
