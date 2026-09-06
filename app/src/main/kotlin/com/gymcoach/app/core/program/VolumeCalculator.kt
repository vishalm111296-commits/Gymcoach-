package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
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

    data class TrainingBalance(
        val latVolume: MuscleVolume,
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
            latVolume, lateralDeltVolume, rearDeltVolume, upperChestVolume,
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

    /** Workout set enriched with exercise and workout-date context. */
    data class SetWithContext(
        val set: WorkoutSetEntity,
        val exerciseId: Long,
        val workoutDate: Long
    )

    /**
     * Calculate average weekly muscle volume over the distinct ISO weeks represented
     * by completed working sets. Secondary/stabilizer assignments retain their
     * fractional credits rather than being promoted to full sets.
     *
     * Public set counts are rounded from the weighted weekly credits because the
     * existing UI/data contract exposes integer set counts.
     */
    fun calculateWeeklyVolume(
        completedSets: List<SetWithContext>,
        exerciseMuscleMap: Map<Long, List<MuscleAssignment>>
    ): TrainingBalance {
        val workingSets = completedSets.filter { it.set.completed && it.set.setType == 0 }
        val trackedWeeks = workingSets.map { isoWeekKey(it.workoutDate) }.toSet()
        val weeksCount = trackedWeeks.size.coerceAtLeast(1)

        val weightedByMuscle = mutableMapOf<String, Double>()
        val directByMuscle = mutableMapOf<String, Double>()
        val indirectByMuscle = mutableMapOf<String, Double>()

        for (ctx in workingSets) {
            for (assignment in exerciseMuscleMap[ctx.exerciseId].orEmpty()) {
                weightedByMuscle[assignment.muscleName] =
                    (weightedByMuscle[assignment.muscleName] ?: 0.0) + assignment.role.credit

                when (assignment.role) {
                    MuscleRole.PRIMARY -> {
                        directByMuscle[assignment.muscleName] =
                            (directByMuscle[assignment.muscleName] ?: 0.0) + 1.0
                    }
                    MuscleRole.SECONDARY,
                    MuscleRole.STABILIZER -> {
                        indirectByMuscle[assignment.muscleName] =
                            (indirectByMuscle[assignment.muscleName] ?: 0.0) + assignment.role.credit
                    }
                }
            }
        }

        fun average(credits: Double): Double = credits / weeksCount.toDouble()
        fun rounded(credits: Double): Int = kotlin.math.round(average(credits)).toInt()

        fun vol(muscle: String): MuscleVolume {
            val direct = rounded(directByMuscle[muscle] ?: 0.0)
            val indirect = rounded(indirectByMuscle[muscle] ?: 0.0)
            val total = rounded(weightedByMuscle[muscle] ?: 0.0)
            return MuscleVolume(
                muscleName = muscle,
                weeklySets = total,
                directSets = direct,
                indirectSets = indirect,
                status = classify(total)
            )
        }

        return TrainingBalance(
            latVolume = vol("Lats"),
            lateralDeltVolume = vol("Lateral Deltoid"),
            rearDeltVolume = vol("Rear Deltoid"),
            upperChestVolume = vol("Upper Chest"),
            upperBackVolume = vol("Upper Back"),
            bicepsVolume = vol("Biceps"),
            tricepsVolume = vol("Triceps"),
            quadricepsVolume = vol("Quadriceps"),
            hamstringsVolume = vol("Hamstrings"),
            glutesVolume = vol("Glutes"),
            calvesVolume = vol("Calves"),
            coreVolume = vol("Core")
        )
    }

    fun calculateVtaperBalance(balance: TrainingBalance): VtaperBalance {
        val primary = (balance.latVolume.status.ordinal + balance.lateralDeltVolume.status.ordinal) / 2.0
        val secondary = (balance.rearDeltVolume.status.ordinal + balance.upperChestVolume.status.ordinal + balance.upperBackVolume.status.ordinal) / 3.0
        val text = when {
            primary >= 3.0 && secondary >= 2.0 -> "Good V-taper volume distribution"
            primary >= 2.0 -> "Moderate V-taper focus"
            else -> "Low V-taper volume"
        }
        return VtaperBalance(primary, secondary, text)
    }

    private fun classify(sets: Int): VolumeStatus {
        return when {
            sets < 10 -> VolumeStatus.INSUFFICIENT
            sets < 14 -> VolumeStatus.MODERATE
            sets < 18 -> VolumeStatus.OPTIMAL
            sets < 22 -> VolumeStatus.HIGH
            else -> VolumeStatus.EXCESSIVE
        }
    }

    private fun isoWeekKey(dateMs: Long): Int {
        val date = Instant.ofEpochMilli(dateMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR)
        val weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return weekBasedYear * 100 + weekOfYear
    }
}
