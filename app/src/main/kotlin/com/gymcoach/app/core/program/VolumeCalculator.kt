package com.gymcoach.app.core.program

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

    enum class VolumeStatus(val label: String, val ordinalValue: Int) {
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
     * Compile-safe input record for volume math.
     *
     * History: this method previously declared List<WorkoutSetEntity> but read
     * `date`/`exerciseId` properties that do not exist on that entity - the file
     * could not compile at all (latent CI failure). Production code currently
     * derives per-workout muscle volume from domain details (see ProgressViewModel);
     * this API stays available for program analysis once wired.
     */
    data class CompletedSetEntry(
        val exerciseId: Long,
        val date: Long,
        val completed: Boolean,
        val setType: Int // 0=NORMAL, 1=WARMUP, 2=DROP, 3=FAILURE
    )

    /**
     * Calculate weekly volume per muscle group with ISO-week bucketing.
     * Uses primary/secondary/stabilizer weighting (1.0/0.5/0.25).
     *
     * HONESTY NOTE (pinned contract): only weeks that contain at least one
     * logged set participate in the average. A skipped week neither adds
     * volume nor dilutes the denominator - so "average weekly sets" means
     * "average over TRAINED weeks", not calendar weeks. Callers must label
     * it accordingly; do not present it as an all-weeks average.
     */
    fun calculateWeeklyVolume(
        completedSets: List<CompletedSetEntry>,
        exerciseMuscleMap: Map<Long, List<MuscleAssignment>>
    ): TrainingBalance {
        val weekBuckets = mutableMapOf<Int, MutableMap<String, Double>>()

        for (set in completedSets.filter { it.completed && it.setType == 0 }) {
            val weekKey = isoWeekKey(set.date)
            val muscleAssignments = exerciseMuscleMap[set.exerciseId] ?: emptyList()

            for (assignment in muscleAssignments) {
                val credits = assignment.role.credit
                val weekMap = weekBuckets.getOrPut(weekKey) { mutableMapOf() }
                weekMap[assignment.muscleName] = (weekMap[assignment.muscleName] ?: 0.0) + credits
            }
        }

        // Average across weeks that contain training data.
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
            .filter { it.completed && it.setType == 0 }
            .groupBy { it.exerciseId }
            .flatMap { (exId, _) ->
                (exerciseMuscleMap[exId] ?: emptyList())
                    .filter { it.role == MuscleRole.PRIMARY }
                    .map { it.muscleName }
            }
            .groupBy { it }
            .mapValues { (_, v) -> v.size }

        val indirectSetsByMuscle = completedSets
            .filter { it.completed && it.setType == 0 }
            .groupBy { it.exerciseId }
            .flatMap { (exId, _) ->
                (exerciseMuscleMap[exId] ?: emptyList())
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
        val primary = (balance.latVolume.status.ordinalValue + balance.lateralDeltVolume.status.ordinalValue) / 2.0
        val secondary = (balance.rearDeltVolume.status.ordinalValue + balance.upperChestVolume.status.ordinalValue + balance.upperBackVolume.status.ordinalValue) / 3.0
        val text = when {
            primary >= 3.0 && secondary >= 2.0 -> "Good V-taper volume distribution"
            primary >= 2.0 -> "Moderate V-taper focus"
            else -> "Low V-taper volume"
        }
        return VtaperBalance(primary, secondary, text)
    }

    private fun classify(sets: Int): VolumeStatus {
        return when {
            sets < 10 -> VolumeStatus.INSUFFICIENT // < 10 = below evidence band
            sets < 14 -> VolumeStatus.MODERATE      // 10-13 = lower evidence band
            sets < 18 -> VolumeStatus.OPTIMAL       // 14-17 = optimal evidence band
            sets < 22 -> VolumeStatus.HIGH          // 18-21 = upper evidence band
            else -> VolumeStatus.EXCESSIVE          // > 21 = excessive per evidence
        }
    }

    private fun isoWeekKey(dateMs: Long): Int {
        val calendar = java.util.Calendar.getInstance(java.util.Locale.getDefault())
        calendar.timeInMillis = dateMs
        val weekOfYear = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)
        return year * 100 + weekOfYear
    }
}
