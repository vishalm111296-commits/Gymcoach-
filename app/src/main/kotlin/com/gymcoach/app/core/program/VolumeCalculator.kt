package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeCalculator @Inject constructor() {

    data class MuscleVolume(
        val muscleName: String,
        val weeklySets: Int,
        val directSets: Int,
        val indirectSets: Int,
        val status: VolumeStatus
    )

    enum class VolumeStatus(val label: String, val level: Int) {
        INSUFFICIENT("Too low", 0),
        MODERATE("Moderate", 1),
        HIGH("High", 2),
        OPTIMAL("Optimal", 3),
        EXCESSIVE("Very high", 4)
    }

    /**
     * Weekly training balance across all major muscle groups.
     *
     * NOTE: `backVolume` tracks exercises whose muscleGroup = "Back" in the
     * ExerciseEntity/seed data (the canonical string used by ProgramGenerator).
     * VolumeStatus thresholds are heuristic guidance values based on common
     * evidence-informed ranges (10–20 sets/week), NOT physiologically validated.
     * They provide direction, not precision.
     */
    data class TrainingBalance(
        val backVolume: MuscleVolume,
        val latVolume: MuscleVolume = backVolume,          // replaces former "Lats" — canonical name is "Back"
        val lateralDeltVolume: MuscleVolume,
        val rearDeltVolume: MuscleVolume,
        val upperChestVolume: MuscleVolume,
        val upperBackVolume: MuscleVolume,
        val bicepsVolume: MuscleVolume,
        val tricepsVolume: MuscleVolume,
        val quadricepsVolume: MuscleVolume,
        val hamstringsVolume: MuscleVolume,
        val glutesVolume: MuscleVolume,
        val calvesVolume: MuscleVolume,
        val coreVolume: MuscleVolume
    ) {
        fun asList(): List<MuscleVolume> = listOf(
            backVolume, lateralDeltVolume, rearDeltVolume, upperChestVolume,
            upperBackVolume, bicepsVolume, tricepsVolume, quadricepsVolume,
            hamstringsVolume, glutesVolume, calvesVolume, coreVolume
        )
    }

    data class VtaperBalance(
        val primaryScore: Double,
        val secondaryScore: Double,
        val overallBalance: String
    )

    enum class MuscleRole(val credit: Double) {
        PRIMARY(1.0), SECONDARY(0.5), STABILIZER(0.25)
    }

    data class MuscleAssignment(val muscleName: String, val role: MuscleRole)

    data class SetWithContext(
        val set: WorkoutSetEntity,
        val exerciseId: Long,
        val workoutDate: Long
    )

    // Canonical muscle name constants, matching ExerciseEntity.muscleGroup seed values
    // and ProgramGenerator slot names. Keep in sync with both.
    companion object {
        const val MUSCLE_BACK = "Back"
        const val MUSCLE_LATERAL_DELT = "Lateral Deltoid"
        const val MUSCLE_REAR_DELT = "Rear Deltoid"
        const val MUSCLE_CHEST = "Chest"
        const val MUSCLE_UPPER_BACK = "Upper Back"
        const val MUSCLE_BICEPS = "Biceps"
        const val MUSCLE_TRICEPS = "Triceps"
        const val MUSCLE_QUADRICEPS = "Quadriceps"
        const val MUSCLE_HAMSTRINGS = "Hamstrings"
        const val MUSCLE_GLUTES = "Glutes"
        const val MUSCLE_CALVES = "Calves"
        const val MUSCLE_CORE = "Core"
    }

    fun calculateWeeklyVolume(
        completedSets: List<SetWithContext>,
        exerciseMuscleMap: Map<Long, List<MuscleAssignment>>
    ): TrainingBalance {
        val weekBuckets = mutableMapOf<Int, MutableMap<String, Double>>()

        for (ctx in completedSets.filter { it.set.completed && it.set.setType == 0 }) {
            val weekKey = isoWeekKey(ctx.workoutDate)
            val muscleAssignments = exerciseMuscleMap[ctx.exerciseId] ?: emptyList()

            for (assignment in muscleAssignments) {
                val credits = assignment.role.credit
                val weekMap = weekBuckets.getOrPut(weekKey) { mutableMapOf() }
                weekMap[assignment.muscleName] = (weekMap[assignment.muscleName] ?: 0.0) + credits
            }
        }

        val avgWeekly = mutableMapOf<String, Double>()
        for ((_, weekMap) in weekBuckets) {
            for ((muscle, credits) in weekMap) {
                avgWeekly[muscle] = (avgWeekly[muscle] ?: 0.0) + credits
            }
        }
        if (weekBuckets.isNotEmpty()) {
            for ((muscle, total) in avgWeekly) {
                avgWeekly[muscle] = total / weekBuckets.size.toDouble()
            }
        }

        val directSetsByMuscle = completedSets
            .filter { it.set.completed && it.set.setType == 0 }
            .flatMap { ctx ->
                (exerciseMuscleMap[ctx.exerciseId] ?: emptyList())
                    .filter { it.role == MuscleRole.PRIMARY }
                    .map { it.muscleName }
            }
            .groupBy { it }
            .mapValues { (_, v) -> v.size }

        val indirectSetsByMuscle = completedSets
            .filter { it.set.completed && it.set.setType == 0 }
            .flatMap { ctx ->
                (exerciseMuscleMap[ctx.exerciseId] ?: emptyList())
                    .filter { it.role in setOf(MuscleRole.SECONDARY, MuscleRole.STABILIZER) }
                    .map { it.muscleName }
            }
            .groupBy { it }
            .mapValues { (_, v) -> v.size }

        fun vol(muscle: String) = MuscleVolume(
            muscleName = muscle,
            weeklySets = (directSetsByMuscle[muscle] ?: 0) + (indirectSetsByMuscle[muscle] ?: 0),
            directSets = directSetsByMuscle[muscle] ?: 0,
            indirectSets = indirectSetsByMuscle[muscle] ?: 0,
            status = classify((directSetsByMuscle[muscle] ?: 0) + (indirectSetsByMuscle[muscle] ?: 0))
        )

        // Fix F-TAXONOMY-1: use "Back" (canonical muscleGroup name from seed data and
        // ProgramGenerator) — not "Lats" which never appears in the exercise database.
        return TrainingBalance(
            backVolume = vol(MUSCLE_BACK),
            lateralDeltVolume = vol(MUSCLE_LATERAL_DELT),
            rearDeltVolume = vol(MUSCLE_REAR_DELT),
            upperChestVolume = vol(MUSCLE_CHEST),
            upperBackVolume = vol(MUSCLE_UPPER_BACK),
            bicepsVolume = vol(MUSCLE_BICEPS),
            tricepsVolume = vol(MUSCLE_TRICEPS),
            quadricepsVolume = vol(MUSCLE_QUADRICEPS),
            hamstringsVolume = vol(MUSCLE_HAMSTRINGS),
            glutesVolume = vol(MUSCLE_GLUTES),
            calvesVolume = vol(MUSCLE_CALVES),
            coreVolume = vol(MUSCLE_CORE)
        )
    }

    /**
     * Compute a V-taper balance indicator.
     *
     * Primary score = average VolumeStatus level for Back + Lateral Deltoid (the two
     * muscles most responsible for the V shape).
     * Secondary score = average for Rear Deltoid + Chest + Upper Back.
     *
     * Uses explicit numeric mapping rather than enum ordinals to guard against
     * future enum reordering silently breaking the formula (F-VTAPER-2).
     */
    fun calculateVtaperBalance(balance: TrainingBalance): VtaperBalance {
        fun statusScore(s: VolumeStatus): Double = s.level.toDouble()
        val primary = (statusScore(balance.backVolume.status) + statusScore(balance.lateralDeltVolume.status)) / 2.0
        val secondary = (
            statusScore(balance.rearDeltVolume.status) +
            statusScore(balance.upperChestVolume.status) +
            statusScore(balance.upperBackVolume.status)
        ) / 3.0
        val text = when {
            primary >= 3.0 && secondary >= 2.0 -> "Good V-taper volume distribution"
            primary >= 2.0 -> "Moderate V-taper focus"
            else -> "Low V-taper volume"
        }
        return VtaperBalance(primary, secondary, text)
    }

    private fun classify(sets: Int): VolumeStatus {
        // Heuristic thresholds. Roughly aligned with evidence-based
        // minimum effective volume (10 sets/week) and maximum adaptive
        // volume (~20 sets/week) from current sports science literature.
        // Not validated as precise clinical values.
        return when {
            sets < 10 -> VolumeStatus.INSUFFICIENT
            sets < 14 -> VolumeStatus.MODERATE
            sets < 18 -> VolumeStatus.OPTIMAL
            sets < 22 -> VolumeStatus.HIGH
            else -> VolumeStatus.EXCESSIVE
        }
    }

    private fun isoWeekKey(dateMs: Long): Int {
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = dateMs
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        return year * 100 + weekOfYear
    }
}
