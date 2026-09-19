package com.gymcoach.app.core.share

import com.gymcoach.app.core.progression.PRDetector.PersonalRecord
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutShareCardBuilder @Inject constructor() {

    fun buildShareData(
        workout: WorkoutWithDetails,
        personalRecords: List<PersonalRecord> = emptyList(),
        workoutTitle: String? = null
    ): WorkoutShareCardData {
        val completedSets = workout.exercises.flatMap { it.sets }.filter { it.completed }
        val totalSets = completedSets.size
        val totalReps = completedSets.sumOf { it.reps }
        val totalVolumeKg = completedSets.sumOf { it.weight * it.reps }

        // Top 3 muscle groups hit
        val muscleScores = LinkedHashMap<String, Int>()
        for (we in workout.exercises) {
            val muscle = we.exercise.muscleGroup.trim()
            if (muscle.isNotEmpty()) {
                val completedCount = we.sets.count { it.completed }
                val count = if (totalSets > 0) completedCount else we.sets.size.coerceAtLeast(1)
                if (count > 0) {
                    muscleScores[muscle] = (muscleScores[muscle] ?: 0) + count
                }
            }
        }
        val topMuscles = muscleScores.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        // Map exercises to summaries
        val exerciseSummaries = workout.exercises.map { we ->
            val exCompletedSets = we.sets.filter { it.completed }
            val candidateSets = if (exCompletedSets.isNotEmpty()) exCompletedSets else we.sets
            val bestSet = candidateSets.maxWithOrNull(
                compareBy<WorkoutSet> { it.weight }.thenBy { it.weight * it.reps }
            )
            val bestSetSummary = bestSet?.let {
                String.format(Locale.US, "%.1f kg × %d reps", it.weight, it.reps)
            } ?: "0.0 kg × 0 reps"

            val isPr = isPrForExercise(we.exercise.id, we.exercise.name, workout, personalRecords)
            val setsCount = if (exCompletedSets.isNotEmpty()) exCompletedSets.size else we.sets.size

            ExerciseShareSummary(
                exerciseName = we.exercise.name,
                bestSetSummary = bestSetSummary,
                totalSetsCount = setsCount,
                isPr = isPr
            )
        }

        // PR count
        val matchingPrs = personalRecords.filter { pr ->
            val dateMatches = (workout.workout.id != 0L && pr.workoutId != 0L && workout.workout.id == pr.workoutId) ||
                matchesDate(pr.date, workout.workout.date) ||
                matchesDate(pr.date, workout.workout.startTime)
            val exMatches = workout.exercises.any { matchesExercise(it.exercise.id, it.exercise.name, pr) }
            dateMatches && exMatches
        }
        val prCount = maxOf(matchingPrs.size, exerciseSummaries.count { it.isPr })

        // Motivational quote
        val motivationalQuote = selectMotivationalQuote(totalVolumeKg, prCount, totalSets)

        // Workout Title
        val title = workoutTitle?.takeIf { it.isNotBlank() }
            ?: workout.workout.notes.lines().firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Workout"

        // Duration formatting
        val durationSeconds = if (workout.workout.duration > 0) {
            workout.workout.duration
        } else {
            (workout.workout.endTime.epochSecond - workout.workout.startTime.epochSecond).coerceAtLeast(0L)
        }
        val durationFormatted = formatDuration(durationSeconds)

        // Date formatting
        val dateFormatted = formatDate(workout.workout.date)

        return WorkoutShareCardData(
            workoutTitle = title,
            dateFormatted = dateFormatted,
            durationFormatted = durationFormatted,
            totalVolumeKg = totalVolumeKg,
            totalSets = totalSets,
            totalReps = totalReps,
            prCount = prCount,
            topMuscles = topMuscles,
            exercises = exerciseSummaries,
            motivationalQuote = motivationalQuote
        )
    }

    fun buildFormattedShareText(data: WorkoutShareCardData): String {
        val sb = StringBuilder()
        sb.appendLine("🏋️ **${data.workoutTitle}**")
        sb.appendLine("📅 ${data.dateFormatted} • ⏱️ ${data.durationFormatted}")
        sb.appendLine("💬 _\"${data.motivationalQuote}\"_")
        sb.appendLine()
        sb.appendLine("📊 **Workout Summary**")
        sb.appendLine("• Total Volume: ${String.format(Locale.US, "%,.1f kg", data.totalVolumeKg)}")
        sb.appendLine("• Total Sets: ${data.totalSets}")
        sb.appendLine("• Total Reps: ${data.totalReps}")
        if (data.prCount > 0) {
            sb.appendLine("• Personal Records: ${data.prCount} 🏆")
        }
        if (data.topMuscles.isNotEmpty()) {
            sb.appendLine("• Target Muscles: ${data.topMuscles.joinToString(", ")}")
        }
        sb.appendLine()
        if (data.exercises.isNotEmpty()) {
            sb.appendLine("💪 **Exercises**")
            data.exercises.forEach { ex ->
                val prBadge = if (ex.isPr) " 🏆 PR!" else ""
                sb.appendLine("• ${ex.exerciseName}: ${ex.bestSetSummary} (${ex.totalSetsCount} sets)$prBadge")
            }
            sb.appendLine()
        }
        sb.append("⚡ Logged with GymCoach")
        return sb.toString()
    }

    private fun selectMotivationalQuote(volumeKg: Double, prCount: Int, totalSets: Int): String {
        return when {
            volumeKg > 10000.0 -> "Titan Volume Unlocked"
            prCount > 0 -> "Personal Records Broken"
            volumeKg > 5000.0 -> "Heavyweight Champion"
            totalSets >= 20 -> "Iron Will & Relentless Grind"
            totalSets >= 10 -> "Solid Work in the Iron Temple"
            totalSets > 0 -> "Every Rep Counts Towards Greatness"
            else -> "Stay Consistent, Stay Strong"
        }
    }

    private fun matchesExercise(exId: Long, exName: String, pr: PersonalRecord): Boolean {
        val idMatch = pr.exerciseId != 0L && exId != 0L && pr.exerciseId == exId
        val nameMatch = pr.exerciseName.isNotBlank() && exName.isNotBlank() &&
            pr.exerciseName.trim().equals(exName.trim(), ignoreCase = true)
        return idMatch || nameMatch
    }

    private fun matchesDate(prDate: Instant, workoutDate: Instant): Boolean {
        if (prDate == workoutDate) return true
        val z1 = prDate.atZone(ZoneId.systemDefault()).toLocalDate()
        val z2 = workoutDate.atZone(ZoneId.systemDefault()).toLocalDate()
        if (z1 == z2) return true
        val u1 = prDate.atZone(ZoneOffset.UTC).toLocalDate()
        val u2 = workoutDate.atZone(ZoneOffset.UTC).toLocalDate()
        return u1 == u2
    }

    private fun isPrForExercise(
        exerciseId: Long,
        exerciseName: String,
        workout: WorkoutWithDetails,
        personalRecords: List<PersonalRecord>
    ): Boolean {
        return personalRecords.any { pr ->
            val exMatches = matchesExercise(exerciseId, exerciseName, pr)
            val dateMatches = (workout.workout.id != 0L && pr.workoutId != 0L && workout.workout.id == pr.workoutId) ||
                matchesDate(pr.date, workout.workout.date) ||
                matchesDate(pr.date, workout.workout.startTime)
            exMatches && dateMatches
        }
    }

    private fun formatDate(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
        return formatter.format(instant.atZone(ZoneId.systemDefault()))
    }

    private fun formatDuration(durationSeconds: Long): String {
        val totalSeconds = durationSeconds.coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    companion object {
        fun buildShareData(
            workout: WorkoutWithDetails,
            personalRecords: List<PersonalRecord> = emptyList(),
            workoutTitle: String? = null
        ): WorkoutShareCardData = WorkoutShareCardBuilder().buildShareData(workout, personalRecords, workoutTitle)

        fun buildFormattedShareText(data: WorkoutShareCardData): String =
            WorkoutShareCardBuilder().buildFormattedShareText(data)
    }
}
