package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes weekly training volume per muscle group from completed workout
 * sets, using primary/secondary/stabilizer role weighting, and derives a
 * V-taper balance signal from that volume.
 *
 * Volume is bucketed by true ISO week (Mon-Sun, ISO-8601 week-based year)
 * so a week that spans the Dec/Jan boundary is never split into two
 * buckets, then averaged as a fractional (Double) value across the number
 * of distinct weeks actually represented in the input sets. This avoids
 * both integer truncation and the appearance of "weekly volume" that is
 * actually an all-time total.
 */
@Singleton
class VolumeCalculator @Inject constructor() {

    data class MuscleVolume(
        val muscleName: String,
        val weeklySets: Double,
        val directSets: Double,
        val indirectSets: Double,
        val status: VolumeStatus
    )

    enum class VolumeStatus(val label: String, val level: Int) {
        INSUFFICIENT("Too low", 0),
        MODERATE("Moderate", 1),
        OPTIMAL("Optimal", 2),
        HIGH("High", 3),
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

    /**
     * WorkoutSetEntity does not store exerciseId or date directly;
     * these come from the parent WorkoutExercise and Workout tables.
     */
    data class SetWithContext(
        val set: WorkoutSetEntity,
        val exerciseId: Long,
        val workoutDate: Long
    )

    /**
     * Calculate weekly volume per muscle group with true ISO-week bucketing.
     * Uses primary/secondary/stabilizer weighting (1.0/0.5/0.25) per ACSM evidence.
     *
     * Direct (primary-role) and indirect (secondary+stabilizer-role) credits are
     * summed per ISO week per muscle, then averaged across the distinct weeks
     * present in [completedSets] to produce a fractional weekly figure. Warmup
     * sets (setType != 0) and incomplete sets are excluded.
     *
     * @param completedSets sets enriched with exercise ID and workout date context
     * @param exerciseMuscleMap mapping from exercise ID to its muscle assignments
     */
    fun calculateWeeklyVolume(
        completedSets: List<SetWithContext>,
        exerciseMuscleMap: Map<Long, List<MuscleAssignment>>
    ): TrainingBalance {
        val workingSets = completedSets.filter { it.set.completed && it.set.setType == 0 }

        // weekKey -> muscleName -> (direct credits, indirect credits)
        val directByWeek = mutableMapOf<Long, MutableMap<String, Double>>()
        val indirectByWeek = mutableMapOf<Long, MutableMap<String, Double>>()
        val weeksSeen = mutableSetOf<Long>()

        for (ctx in workingSets) {
            val weekKey = isoWeekKey(ctx.workoutDate)
            weeksSeen.add(weekKey)
            val assignments = exerciseMuscleMap[ctx.exerciseId] ?: emptyList()

            for (assignment in assignments) {
                when (assignment.role) {
                    MuscleRole.PRIMARY -> {
                        val bucket = directByWeek.getOrPut(weekKey) { mutableMapOf() }
                        bucket[assignment.muscleName] = (bucket[assignment.muscleName] ?: 0.0) + assignment.role.credit
                    }
                    MuscleRole.SECONDARY, MuscleRole.STABILIZER -> {
                        val bucket = indirectByWeek.getOrPut(weekKey) { mutableMapOf() }
                        bucket[assignment.muscleName] = (bucket[assignment.muscleName] ?: 0.0) + assignment.role.credit
                    }
                }
            }
        }

        val weeksCount = weeksSeen.size.coerceAtLeast(1).toDouble()

        fun averagedTotal(byWeek: Map<Long, Map<String, Double>>, muscle: String): Double {
            val total = byWeek.values.sumOf { it[muscle] ?: 0.0 }
            return total / weeksCount
        }

        val allMuscles = (directByWeek.values.flatMap { it.keys } + indirectByWeek.values.flatMap { it.keys }).toSet()

        fun vol(muscle: String): MuscleVolume {
            val direct = averagedTotal(directByWeek, muscle)
            val indirect = averagedTotal(indirectByWeek, muscle)
            val total = direct + indirect
            return MuscleVolume(
                muscleName = muscle,
                weeklySets = total,
                directSets = direct,
                indirectSets = indirect,
                status = classify(total)
            )
        }

        // Ensure every named muscle in the balance struct resolves even if
        // it never appeared in allMuscles (no sets logged for it yet).
        return TrainingBalance(
            latVolume = vol("Lats"), lateralDeltVolume = vol("Lateral Deltoid"),
            rearDeltVolume = vol("Rear Deltoid"), upperChestVolume = vol("Upper Chest"),
            upperBackVolume = vol("Upper Back"), bicepsVolume = vol("Biceps"),
            tricepsVolume = vol("Triceps"), quadricepsVolume = vol("Quadriceps"),
            hamstringsVolume = vol("Hamstrings"), glutesVolume = vol("Glutes"),
            calvesVolume = vol("Calves"), coreVolume = vol("Core")
        ).also { allMuscles.size } // allMuscles retained for future muscle-list callers; suppress unused warning path
    }

    /**
     * True ISO-8601 week key (week-based year * 100 + week-of-week-based-year).
     * Unlike Calendar.WEEK_OF_YEAR (locale-dependent, can start on Sunday and
     * can split the same ISO week across Dec 31 / Jan 1), this never divides
     * a single ISO week into two buckets.
     */
    private fun isoWeekKey(dateMs: Long): Long {
        val date = Instant.ofEpochMilli(dateMs).atZone(ZoneId.systemDefault()).toLocalDate()
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR)
        return weekBasedYear.toLong() * 100L + week.toLong()
    }

    private fun classify(sets: Double): VolumeStatus {
        return when {
            sets < 10 -> VolumeStatus.INSUFFICIENT // < 10 = below evidence band
            sets < 14 -> VolumeStatus.MODERATE      // 10-13 = lower evidence band
            sets < 18 -> VolumeStatus.OPTIMAL       // 14-17 = optimal evidence band
            sets < 22 -> VolumeStatus.HIGH          // 18-21 = upper evidence band
            else -> VolumeStatus.EXCESSIVE          // > 21 = excessive per evidence
        }
    }

    /**
     * V-taper balance score. Distance-from-OPTIMAL scoring: OPTIMAL scores
     * highest (1.0), and both under-training (INSUFFICIENT) and over-training
     * (EXCESSIVE) are penalized symmetrically, rather than rewarding raw
     * ordinal magnitude (which previously scored EXCESSIVE above OPTIMAL).
     */
    fun calculateVtaperBalance(balance: TrainingBalance): VtaperBalance {
        val optimalLevel = VolumeStatus.OPTIMAL.level
        val maxDistance = maxOf(optimalLevel, VolumeStatus.EXCESSIVE.level - optimalLevel)

        fun bandScore(status: VolumeStatus): Double {
            val distance = kotlin.math.abs(status.level - optimalLevel)
            return 1.0 - (distance.toDouble() / maxDistance.toDouble())
        }

        val primary = (bandScore(balance.latVolume.status) + bandScore(balance.lateralDeltVolume.status)) / 2.0
        val secondary = (
            bandScore(balance.rearDeltVolume.status) +
                bandScore(balance.upperChestVolume.status) +
                bandScore(balance.upperBackVolume.status)
            ) / 3.0

        val text = when {
            primary >= 0.75 && secondary >= 0.5 -> "V-taper focus is well balanced this week."
            primary < 0.5 -> "Lats and side delts need more direct volume for V-taper progress."
            secondary < 0.35 -> "Add rear delt, upper chest, or upper back work to round out the taper."
            else -> "V-taper volume is trending in the right direction."
        }

        return VtaperBalance(primary, secondary, text)
    }
}
