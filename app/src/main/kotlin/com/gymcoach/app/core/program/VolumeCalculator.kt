package com.gymcoach.app.core.program

import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LoggedSet(
    val exerciseId: Long,
    val dateMs: Long,
    val completed: Boolean,
    val setType: String
)

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

    /**
     * Calculate weekly volume per muscle group with ISO-week bucketing.
     */
    fun calculateWeeklyVolume(
        completedSets: List<LoggedSet>,
        exerciseMuscleMap: Map<Long, List<MuscleAssignment>>
    ): TrainingBalance {
        val weekBuckets = mutableMapOf<Int, MutableMap<String, Double>>()

        // Filter: completed working sets (setType != "WARMUP")
        val workingSets = completedSets.filter { it.completed && it.setType != "WARMUP" }

        for (set in workingSets) {
            val weekKey = isoWeekKey(set.dateMs)
            val muscleAssignments = exerciseMuscleMap[set.exerciseId] ?: emptyList()

            // TODO: If a secondary-muscle mapping exists in MuscleGroupEnum apply 0.5 credit; currently primary-only
            val primaryAssignments = muscleAssignments.filter { it.role == MuscleRole.PRIMARY }

            for (assignment in primaryAssignments) {
                val credits = 1.0
                val weekMap = weekBuckets.getOrPut(weekKey) { mutableMapOf() }
                weekMap[assignment.muscleName] = (weekMap[assignment.muscleName] ?: 0.0) + credits
            }
        }

        // Average across weeks
        val avgWeekly = mutableMapOf<String, Double>()
        if (weekBuckets.isNotEmpty()) {
            for ((_, weekMap) in weekBuckets) {
                for ((muscle, credits) in weekMap) {
                    avgWeekly[muscle] = (avgWeekly[muscle] ?: 0.0) + credits
                }
            }
            for ((muscle, total) in avgWeekly) {
                avgWeekly[muscle] = total / weekBuckets.size.toDouble()
            }
        }

        // Correct set counting (multiply by the number of sets/occurrences instead of counting distinct exercises)
        val directSetsByMuscle = mutableMapOf<String, Int>()
        for (set in workingSets) {
            val assignments = exerciseMuscleMap[set.exerciseId] ?: emptyList()
            val primaryAssignments = assignments.filter { it.role == MuscleRole.PRIMARY }
            for (assignment in primaryAssignments) {
                directSetsByMuscle[assignment.muscleName] = (directSetsByMuscle[assignment.muscleName] ?: 0) + 1
            }
        }

        fun vol(muscle: String) = MuscleVolume(
            muscleName = muscle,
            weeklySets = (avgWeekly[muscle] ?: 0.0).toInt(),
            directSets = directSetsByMuscle[muscle] ?: 0,
            indirectSets = 0,
            status = classify((avgWeekly[muscle] ?: 0.0).toInt())
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
        val primary = (balance.latVolume.status.level + balance.lateralDeltVolume.status.level) / 2.0
        val secondary = (balance.rearDeltVolume.status.level + balance.upperChestVolume.status.level + balance.upperBackVolume.status.level) / 3.0
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
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = dateMs
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        return year * 100 + weekOfYear
    }
}
