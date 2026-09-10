package com.gymcoach.app.core.program

import com.gymcoach.app.domain.model.CompletedSetContext
import com.gymcoach.app.domain.model.SetType
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeCalculator @Inject constructor() {

    /**
     * Domain representation of a muscle's training volume.
     *
     * @param muscleName Display/category name of the muscle.
     * @param weeklyEffectiveSets Effective weighted volume (PRIMARY=1.0, SECONDARY=0.5, STABILIZER=0.25).
     * @param rawDirectSets Count of completed direct (PRIMARY) sets.
     * @param rawIndirectSets Count of completed indirect (SECONDARY + STABILIZER) sets.
     * @param status Volume coaching classification band based on effective weighted sets.
     */
    data class MuscleVolume(
        val muscleName: String,
        val weeklyEffectiveSets: Double,
        val rawDirectSets: Int,
        val rawIndirectSets: Int,
        val status: VolumeStatus
    ) {
        /** Alias for weeklyEffectiveSets enforcing EFFECTIVE_WEIGHTED_SETS semantics. */
        val weeklySets: Double get() = weeklyEffectiveSets

        /** Raw unweighted sum of direct and indirect completed sets. */
        val rawTotalSets: Int get() = rawDirectSets + rawIndirectSets

        /** Backward compatibility aliases for raw direct/indirect set counts. */
        val directSets: Int get() = rawDirectSets
        val indirectSets: Int get() = rawIndirectSets
    }

    /** Volume status bands presented as evidence-informed coaching guidance. */
    enum class VolumeStatus(val label: String, val level: Int) {
        INSUFFICIENT("Below target guidance", 0),
        MODERATE("Moderate guidance", 1),
        HIGH("High guidance range", 2),
        OPTIMAL("Target guidance range", 3),
        EXCESSIVE("Above target guidance", 4)
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
     * Calculates weekly volume for completed hypertrophy sets (excluding warmups and incomplete sets).
     *
     * Set-Type Contract:
     * - NORMAL (0), DROP (2), and FAILURE (3) completed sets deliver effective working stimulus
     *   and are INCLUDED in hypertrophy volume calculations.
     * - WARMUP (1) sets are submaximal preparation sets and are EXCLUDED.
     * - Incomplete sets (completed = false) are EXCLUDED.
     *
     * Volume credits per completed working set:
     * - Primary muscle: 1.0 effective set
     * - Secondary muscle: 0.5 effective set
     * - Stabilizer muscle: 0.25 effective set
     */
    fun calculateWeeklyVolume(
        completedSets: List<CompletedSetContext>,
        exerciseMuscleMap: Map<Long, List<MuscleAssignment>>
    ): TrainingBalance {
        // Filter: ONLY completed hypertrophy working sets (completed == true AND setType != SetType.WARMUP)
        val validSets = completedSets.filter { it.isHypertrophyWorkingSet }

        val weekBuckets = mutableMapOf<String, MutableMap<String, Double>>()

        for (ctx in validSets) {
            val weekKey = isoWeekKey(ctx.workoutDate)
            val muscleAssignments = exerciseMuscleMap[ctx.exerciseId] ?: emptyList()

            for (assignment in muscleAssignments) {
                val credits = assignment.role.credit
                val weekMap = weekBuckets.getOrPut(weekKey) { mutableMapOf() }
                weekMap[assignment.muscleName] = (weekMap[assignment.muscleName] ?: 0.0) + credits
            }
        }

        val avgWeeklyEffectiveSets = mutableMapOf<String, Double>()
        if (weekBuckets.isNotEmpty()) {
            for ((_, weekMap) in weekBuckets) {
                for ((muscle, credits) in weekMap) {
                    avgWeeklyEffectiveSets[muscle] = (avgWeeklyEffectiveSets[muscle] ?: 0.0) + credits
                }
            }
            for ((muscle, total) in avgWeeklyEffectiveSets) {
                avgWeeklyEffectiveSets[muscle] = total / weekBuckets.size.toDouble()
            }
        }

        val directSetsByMuscle = validSets
            .flatMap { ctx ->
                (exerciseMuscleMap[ctx.exerciseId] ?: emptyList())
                    .filter { it.role == MuscleRole.PRIMARY }
                    .map { it.muscleName }
            }
            .groupBy { it }
            .mapValues { (_, v) -> v.size }

        val indirectSetsByMuscle = validSets
            .flatMap { ctx ->
                (exerciseMuscleMap[ctx.exerciseId] ?: emptyList())
                    .filter { it.role in setOf(MuscleRole.SECONDARY, MuscleRole.STABILIZER) }
                    .map { it.muscleName }
            }
            .groupBy { it }
            .mapValues { (_, v) -> v.size }

        fun vol(muscle: String): MuscleVolume {
            val effective = avgWeeklyEffectiveSets[muscle] ?: 0.0
            val direct = directSetsByMuscle[muscle] ?: 0
            val indirect = indirectSetsByMuscle[muscle] ?: 0
            return MuscleVolume(
                muscleName = muscle,
                weeklyEffectiveSets = effective,
                rawDirectSets = direct,
                rawIndirectSets = indirect,
                status = classify(effective)
            )
        }

        return TrainingBalance(
            latVolume = vol("Lats"), lateralDeltVolume = vol("Lateral Deltoid"),
            rearDeltVolume = vol("Rear Deltoid"), upperChestVolume = vol("Upper Chest"),
            upperBackVolume = vol("Upper Back"), bicepsVolume = vol("Biceps"),
            tricepsVolume = vol("Triceps"), quadricepsVolume = vol("Quadriceps"),
            hamstringsVolume = vol("Hamstrings"), glutesVolume = vol("Glutes"),
            calvesVolume = vol("Calves"), coreVolume = vol("Core")
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

    private fun classify(effectiveSets: Double): VolumeStatus {
        return when {
            effectiveSets < 10.0 -> VolumeStatus.INSUFFICIENT
            effectiveSets < 14.0 -> VolumeStatus.MODERATE
            effectiveSets < 18.0 -> VolumeStatus.OPTIMAL
            effectiveSets < 22.0 -> VolumeStatus.HIGH
            else -> VolumeStatus.EXCESSIVE
        }
    }

    fun isoWeekKey(dateMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zdt = Instant.ofEpochMilli(dateMs).atZone(zoneId)
        val weekFields = WeekFields.ISO
        val weekOfYear = zdt.get(weekFields.weekOfWeekBasedYear())
        val year = zdt.get(weekFields.weekBasedYear())
        return "%04d-W%02d".format(Locale.US, year, weekOfYear)
    }

    companion object {
        /** Explicit predicate determining whether a set context represents a completed working set for hypertrophy volume. */
        val CompletedSetContext.isHypertrophyWorkingSet: Boolean
            get() = completed && setType != SetType.WARMUP.ordinal
    }
}
