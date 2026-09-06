package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeCalculator @Inject constructor() {

    data class MuscleVolume(
        val muscleName: String,
        /**
         * Average weighted credits per ISO week across the observed weeks.
         *
         * Credit model: PRIMARY=1.0, SECONDARY=0.5, STABILIZER=0.25. The raw
         * direct+indirect set total alone is not evidence-meaningful because the
         * ACSM bands (10/14/18/22 sets/week) are per-week thresholds; this field
         * normalizes multi-week history back to a single average week so that
         * classification compares like with like.
         */
        val weeklyVolume: Double,
        /** Raw completed normal sets in which this muscle was PRIMARY. */
        val directSets: Int,
        /** Raw completed normal sets in which this muscle was SECONDARY or STABILIZER. */
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

    /**
     * Enriched set with workout context needed for volume calculations.
     * WorkoutSetEntity does not store exerciseId or date directly;
     * these come from the parent WorkoutExercise and Workout tables.
     */
    data class SetWithContext(
        val set: WorkoutSetEntity,
        val exerciseId: Long,
        val workoutDate: Long
    )

    /**
     * Calculate weekly volume per muscle group with ISO-week bucketing.
     * Uses primary/secondary/stabilizer weighting (1.0/0.5/0.25) per ACSM evidence.
     *
     * Sets are bucketed into ISO weeks ([isoWeekKey]); each muscle's weighted
     * credits are averaged across the observed ISO weeks into MuscleVolume.weeklyVolume,
     * and classification is applied to that per-week average.
     *
     * @param completedSets sets enriched with exercise ID and workout date context
     * @param exerciseMuscleMap mapping from exercise ID to its muscle assignments
     */
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

        // Weighted credits summed over the observed weeks, then normalized to an
        // average week: this is the source of MuscleVolume.weeklyVolume.
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

        val directSetsByMuscle = mutableMapOf<String, Int>()
        val indirectSetsByMuscle = mutableMapOf<String, Int>()
        for (ctx in completedSets.filter { it.set.completed && it.set.setType == 0 }) {
            val muscleAssignments = exerciseMuscleMap[ctx.exerciseId] ?: emptyList()
            for (assignment in muscleAssignments) {
                when (assignment.role) {
                    MuscleRole.PRIMARY -> directSetsByMuscle[assignment.muscleName] =
                        (directSetsByMuscle[assignment.muscleName] ?: 0) + 1
                    MuscleRole.SECONDARY, MuscleRole.STABILIZER -> indirectSetsByMuscle[assignment.muscleName] =
                        (indirectSetsByMuscle[assignment.muscleName] ?: 0) + 1
                }
            }
        }

        fun vol(muscle: String) = MuscleVolume(
            muscleName = muscle,
            weeklyVolume = avgWeekly[muscle] ?: 0.0,
            directSets = directSetsByMuscle[muscle] ?: 0,
            indirectSets = indirectSetsByMuscle[muscle] ?: 0,
            status = classify(avgWeekly[muscle] ?: 0.0)
        )

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

    /** Evidence bands are per-week; thresholds applied to the averaged weeklyVolume. */
    private fun classify(weeklyVolume: Double): VolumeStatus {
        return when {
            weeklyVolume < 10 -> VolumeStatus.INSUFFICIENT // < 10 = below evidence band
            weeklyVolume < 14 -> VolumeStatus.MODERATE      // 10-13 = lower evidence band
            weeklyVolume < 18 -> VolumeStatus.OPTIMAL       // 14-17 = optimal evidence band
            weeklyVolume < 22 -> VolumeStatus.HIGH          // 18-21 = upper evidence band
            else -> VolumeStatus.EXCESSIVE                  // > 21 = excessive per evidence
        }
    }

    /**
     * ISO-8601 week key: weekBasedYear * 100 + weekOfWeekBasedYear, computed in UTC.
     *
     * The UTC decision: epoch millis describe an absolute instant, and bucketing at
     * UTC keeps the week independent of the device's local time zone — a workout
     * logged late at night locally must not shift into the previous/next ISO week
     * just because the local calendar flipped. WeekFields.ISO gives the true ISO
     * Monday-start, 4-day-minimum week (Calendar.WEEK_OF_YEAR is locale-dependent).
     */
    private fun isoWeekKey(dateMs: Long): Int {
        val weekFields = WeekFields.ISO
        val zoned = Instant.ofEpochMilli(dateMs).atZone(ZoneOffset.UTC)
        return zoned.get(weekFields.weekBasedYear()) * 100 + zoned.get(weekFields.weekOfWeekBasedYear())
    }
}
