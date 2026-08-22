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

    enum class VolumeStatus(val label: String) {
        INSUFFICIENT("Too low"), MODERATE("Moderate"), HIGH("High"), EXCESSIVE("Very high")
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

    fun calculateWeeklyVolume(
        weeklySetsByMuscle: Map<String, Int>,
        weeklyIndirectByMuscle: Map<String, Int>
    ): TrainingBalance {
        fun vol(muscle: String) = MuscleVolume(
            muscleName = muscle,
            weeklySets = (weeklySetsByMuscle[muscle] ?: 0) + (weeklyIndirectByMuscle[muscle] ?: 0),
            directSets = weeklySetsByMuscle[muscle] ?: 0,
            indirectSets = weeklyIndirectByMuscle[muscle] ?: 0,
            status = classify((weeklySetsByMuscle[muscle] ?: 0) + (weeklyIndirectByMuscle[muscle] ?: 0))
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
            primary >= 2.0 && secondary >= 1.5 -> "Good V-taper volume distribution"
            primary >= 1.5 -> "Moderate V-taper focus"
            else -> "Low V-taper volume"
        }
        return VtaperBalance(primary, secondary, text)
    }

    private fun classify(sets: Int): VolumeStatus = when {
        sets < 8 -> VolumeStatus.INSUFFICIENT
        sets < 12 -> VolumeStatus.MODERATE
        sets < 16 -> VolumeStatus.HIGH
        else -> VolumeStatus.EXCESSIVE
    }
}
