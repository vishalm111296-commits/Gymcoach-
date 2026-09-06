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
        LOW("Low", 0),
        MODERATE("Moderate", 1),
        HIGH("High", 2)
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

    data class SetWithContext(
        val set: WorkoutSetEntity,
        val exerciseId: Long,
        val workoutDate: Long
    )

    /**
     * Calculates credited working-set volume. Secondary and stabilizer work is
     * weighted rather than counted as full direct sets. Weekly buckets use the
     * ISO week-year so dates around New Year remain in the correct training week.
     */
    fun calculateWeeklyVolume(
        completedSets: List<SetWithContext>,
        exerciseMuscleMap: Map<Long, List<MuscleAssignment>>
    ): TrainingBalance {
        val tracked = completedSets.filter { it.set.completed && it.set.setType == 0 }
        val weekBuckets = mutableMapOf<String, MutableMap<String, Double>>()

        tracked.forEach { ctx ->
            val bucket = weekBuckets.getOrPut(isoWeekKey(ctx.workoutDate)) { mutableMapOf() }
            exerciseMuscleMap[ctx.exerciseId].orEmpty().forEach { assignment ->
                bucket[assignment.muscleName] = (bucket[assignment.muscleName] ?: 0.0) + assignment.role.credit
            }
        }

        val averaged = mutableMapOf<String, Double>()
        weekBuckets.values.forEach { week ->
            week.forEach { (muscle, credit) -> averaged[muscle] = (averaged[muscle] ?: 0.0) + credit }
        }
        val weekCount = weekBuckets.size.coerceAtLeast(1)
        averaged.keys.toList().forEach { muscle -> averaged[muscle] = averaged[muscle]!! / weekCount }

        fun roleSets(role: MuscleRole): Map<String, Double> = tracked
            .groupBy { it.exerciseId }
            .flatMap { (exerciseId, sets) ->
                exerciseMuscleMap[exerciseId].orEmpty()
                    .filter { it.role == role }
                    .map { it.muscleName to sets.size.toDouble() }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() / weekCount }

        val direct = roleSets(MuscleRole.PRIMARY)
        val indirect = roleSets(MuscleRole.SECONDARY) + roleSets(MuscleRole.STABILIZER)

        fun vol(muscle: String): MuscleVolume {
            val weighted = averaged[muscle] ?: 0.0
            val directSets = (direct[muscle] ?: 0.0).roundToInt()
            val indirectCredits = (indirect[muscle] ?: 0.0).roundToInt()
            return MuscleVolume(
                muscleName = muscle,
                weeklySets = weighted.roundToInt(),
                directSets = directSets,
                indirectSets = indirectCredits,
                status = classify(weighted)
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
        val primary = (balance.latVolume.weeklySets + balance.lateralDeltVolume.weeklySets) / 2.0
        val secondary = (
            balance.rearDeltVolume.weeklySets +
                balance.upperChestVolume.weeklySets +
                balance.upperBackVolume.weeklySets
            ) / 3.0
        val text = when {
            primary >= 10 && secondary >= 8 -> "Strong V-taper emphasis"
            primary >= 6 -> "Moderate V-taper emphasis"
            else -> "Developing V-taper emphasis"
        }
        return VtaperBalance(primary, secondary, text)
    }

    /**
     * These are descriptive monitoring bands, not universal hypertrophy limits.
     * Individual response, exercise selection, effort and recovery still matter.
     */
    private fun classify(weightedSets: Double): VolumeStatus = when {
        weightedSets < 6.0 -> VolumeStatus.LOW
        weightedSets < 10.0 -> VolumeStatus.MODERATE
        else -> VolumeStatus.HIGH
    }

    private fun isoWeekKey(dateMs: Long): String {
        val date = Instant.ofEpochMilli(dateMs).atZone(ZoneId.systemDefault()).toLocalDate()
        val weekYear = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "$weekYear-W$week"
    }

    private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
}
