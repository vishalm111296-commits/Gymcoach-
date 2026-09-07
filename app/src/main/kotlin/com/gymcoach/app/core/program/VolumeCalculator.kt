package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.CanonicalMuscle
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeCalculator @Inject constructor() {

    /**
     * Unified volume summary for a muscle group over an evaluated time window.
     *
     * @param muscleName Display label for the muscle group.
     * @param canonicalMuscle Optional canonical taxonomy enum reference.
     * @param effectiveWeeklyVolume Role-weighted weekly volume (Primary = 1.0, Secondary = 0.5, Stabilizer = 0.25) averaged across active training weeks.
     * @param rawSetCount Total completed working sets directly or indirectly targeting this muscle.
     * @param directSets Total completed working sets where this muscle was the PRIMARY target.
     * @param indirectSets Total completed working sets where this muscle was a SECONDARY target.
     * @param stabilizerSets Total completed working sets where this muscle acted as a STABILIZER.
     * @param weeklyFrequency Average distinct days per week this muscle was trained.
     * @param status Evidence-informed coaching classification band.
     */
    data class MuscleVolume(
        val muscleName: String,
        val canonicalMuscle: CanonicalMuscle? = null,
        val effectiveWeeklyVolume: Double,
        val rawSetCount: Int,
        val directSets: Int,
        val indirectSets: Int,
        val stabilizerSets: Int = 0,
        val weeklyFrequency: Double = 0.0,
        val status: VolumeStatus
    ) {
        /** Backwards-compatible integer property representing rounded effective weekly volume. */
        val weeklySets: Int get() = Math.round(effectiveWeeklyVolume).toInt()

        /** Alternative constructor for backwards-compatibility with planned volume calculations. */
        constructor(
            muscleName: String,
            weeklySets: Int,
            directSets: Int,
            indirectSets: Int,
            status: VolumeStatus
        ) : this(
            muscleName = muscleName,
            canonicalMuscle = CanonicalMuscle.fromIdOrAlias(muscleName),
            effectiveWeeklyVolume = weeklySets.toDouble(),
            rawSetCount = directSets + indirectSets,
            directSets = directSets,
            indirectSets = indirectSets,
            stabilizerSets = 0,
            weeklyFrequency = 0.0,
            status = status
        )
    }

    /**
     * Evidence-informed volume coaching bands.
     * Note: Bands serve as coaching heuristics rather than rigid universal optimal claims.
     */
    enum class VolumeStatus(val label: String, val level: Int, val description: String) {
        INSUFFICIENT("Low volume", 0, "<10 sets/week"),
        MODERATE("Moderate volume", 1, "10-13 sets/week"),
        OPTIMAL("Target volume", 2, "14-17 sets/week"),
        HIGH("High volume", 3, "18-21 sets/week"),
        EXCESSIVE("Very high volume", 4, "22+ sets/week")
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
     * Enriched set with workout context needed for volume calculations.
     */
    data class SetWithContext(
        val set: WorkoutSetEntity,
        val exerciseId: Long,
        val workoutDate: Long
    )

    /**
     * Calculate weekly volume per muscle group with ISO-week bucketing.
     * Uses primary/secondary/stabilizer weighting (1.0/0.5/0.25).
     */
    fun calculateWeeklyVolume(
        completedSets: List<SetWithContext>,
        exerciseMuscleMap: Map<Long, List<MuscleAssignment>>
    ): TrainingBalance {
        val activeSets = completedSets.filter { it.set.completed && it.set.setType == 0 }

        val weekBuckets = mutableMapOf<Int, MutableMap<String, Double>>()
        val directSetsByMuscle = mutableMapOf<String, Int>()
        val indirectSetsByMuscle = mutableMapOf<String, Int>()
        val stabilizerSetsByMuscle = mutableMapOf<String, Int>()
        val activeDaysByMuscle = mutableMapOf<String, MutableSet<Int>>()

        for (ctx in activeSets) {
            val weekKey = isoWeekKey(ctx.workoutDate)
            val cal = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = ctx.workoutDate }
            val yearDay = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)

            val muscleAssignments = exerciseMuscleMap[ctx.exerciseId] ?: emptyList()

            for (assignment in muscleAssignments) {
                val m = assignment.muscleName
                val credits = assignment.role.credit

                val weekMap = weekBuckets.getOrPut(weekKey) { mutableMapOf() }
                weekMap[m] = (weekMap[m] ?: 0.0) + credits

                activeDaysByMuscle.getOrPut(m) { mutableSetOf() }.add(yearDay)

                when (assignment.role) {
                    MuscleRole.PRIMARY -> directSetsByMuscle[m] = (directSetsByMuscle[m] ?: 0) + 1
                    MuscleRole.SECONDARY -> indirectSetsByMuscle[m] = (indirectSetsByMuscle[m] ?: 0) + 1
                    MuscleRole.STABILIZER -> stabilizerSetsByMuscle[m] = (stabilizerSetsByMuscle[m] ?: 0) + 1
                }
            }
        }

        val totalWeeks = maxOf(1, weekBuckets.size)
        val avgWeeklyVolume = mutableMapOf<String, Double>()

        for ((_, weekMap) in weekBuckets) {
            for ((muscle, credits) in weekMap) {
                avgWeeklyVolume[muscle] = (avgWeeklyVolume[muscle] ?: 0.0) + credits
            }
        }
        for ((muscle, total) in avgWeeklyVolume) {
            avgWeeklyVolume[muscle] = total / totalWeeks.toDouble()
        }

        fun vol(muscleName: String, canonical: CanonicalMuscle?): MuscleVolume {
            val effVol = avgWeeklyVolume[muscleName] ?: 0.0
            val direct = directSetsByMuscle[muscleName] ?: 0
            val indirect = indirectSetsByMuscle[muscleName] ?: 0
            val stabilizer = stabilizerSetsByMuscle[muscleName] ?: 0
            val raw = direct + indirect + stabilizer
            val activeDays = activeDaysByMuscle[muscleName]?.size ?: 0
            val frequency = activeDays.toDouble() / totalWeeks.toDouble()

            return MuscleVolume(
                muscleName = muscleName,
                canonicalMuscle = canonical,
                effectiveWeeklyVolume = effVol,
                rawSetCount = raw,
                directSets = direct,
                indirectSets = indirect,
                stabilizerSets = stabilizer,
                weeklyFrequency = frequency,
                status = classify(effVol)
            )
        }

        return TrainingBalance(
            latVolume = vol("Lats", CanonicalMuscle.LATISSIMUS_DORSI),
            lateralDeltVolume = vol("Lateral Deltoid", CanonicalMuscle.LATERAL_DELTOID),
            rearDeltVolume = vol("Rear Deltoid", CanonicalMuscle.REAR_DELTOID),
            upperChestVolume = vol("Upper Chest", CanonicalMuscle.UPPER_CHEST),
            upperBackVolume = vol("Upper Back", CanonicalMuscle.UPPER_BACK),
            bicepsVolume = vol("Biceps", CanonicalMuscle.BICEPS),
            tricepsVolume = vol("Triceps", CanonicalMuscle.TRICEPS),
            quadricepsVolume = vol("Quadriceps", CanonicalMuscle.QUADRICEPS),
            hamstringsVolume = vol("Hamstrings", CanonicalMuscle.HAMSTRINGS),
            glutesVolume = vol("Glutes", CanonicalMuscle.GLUTES),
            calvesVolume = vol("Calves", CanonicalMuscle.CALVES),
            coreVolume = vol("Core", CanonicalMuscle.ABS)
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

    fun classify(effectiveWeeklyVolume: Double): VolumeStatus {
        return when {
            effectiveWeeklyVolume < 10.0 -> VolumeStatus.INSUFFICIENT
            effectiveWeeklyVolume < 14.0 -> VolumeStatus.MODERATE
            effectiveWeeklyVolume < 18.0 -> VolumeStatus.OPTIMAL
            effectiveWeeklyVolume < 22.0 -> VolumeStatus.HIGH
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
