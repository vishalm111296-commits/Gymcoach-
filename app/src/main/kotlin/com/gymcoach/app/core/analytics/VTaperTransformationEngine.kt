package com.gymcoach.app.core.analytics

import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class VTaperTier { NOVICE, ATHLETIC, PRIME, GOLDEN }

data class AdonisIndex(
    val currentRatio: Double,
    val targetRatio: Double = 1.618,
    val progressPct: Int,
    val tier: VTaperTier,
    val statusSummary: String
)

data class LimbSymmetry(
    val limbName: String,
    val leftCm: Double,
    val rightCm: Double,
    val symmetryPct: Double,
    val deltaCm: Double,
    val isBalanced: Boolean
)

data class RecompDelta(
    val daysPeriod: Int,
    val waistDeltaCm: Double,
    val shoulderDeltaCm: Double,
    val chestDeltaCm: Double,
    val weightDeltaKg: Double
)

data class VTaperReport(
    val adonisIndex: AdonisIndex,
    val limbSymmetries: List<LimbSymmetry>,
    val recompDelta: RecompDelta?,
    val latestMeasurement: BodyMeasurementEntity?,
    val history: List<BodyMeasurementEntity>
)

@Singleton
class VTaperTransformationEngine @Inject constructor() {

    fun calculateReport(measurements: List<BodyMeasurementEntity>, daysWindow: Int = 90): VTaperReport {
        val sortedHistory = measurements.sortedBy { it.recordedAt }
        val latest = sortedHistory.lastOrNull()

        if (latest == null) {
            return VTaperReport(
                adonisIndex = emptyAdonisIndex(),
                limbSymmetries = emptyList(),
                recompDelta = null,
                latestMeasurement = null,
                history = emptyList()
            )
        }

        val adonisIndex = calculateAdonisIndex(latest)
        val limbSymmetries = calculateLimbSymmetries(latest)
        val recompDelta = calculateRecompDelta(sortedHistory, daysWindow)

        return VTaperReport(
            adonisIndex = adonisIndex,
            limbSymmetries = limbSymmetries,
            recompDelta = recompDelta,
            latestMeasurement = latest,
            history = sortedHistory
        )
    }

    private fun calculateAdonisIndex(latest: BodyMeasurementEntity): AdonisIndex {
        if (latest.waistCm <= 0.0 || latest.shouldersCm <= 0.0) {
            return emptyAdonisIndex()
        }

        val ratio = latest.shouldersCm / latest.waistCm
        val progressPct = min(100, (ratio / 1.618 * 100).roundToInt())

        val tier = when {
            ratio < 1.35 -> VTaperTier.NOVICE
            ratio < 1.50 -> VTaperTier.ATHLETIC
            ratio < 1.618 -> VTaperTier.PRIME
            else -> VTaperTier.GOLDEN
        }

        val summary = when (tier) {
            VTaperTier.NOVICE -> "Novice Tier"
            VTaperTier.ATHLETIC -> "Athletic Tier"
            VTaperTier.PRIME -> "Prime Tier"
            VTaperTier.GOLDEN -> "Golden Tier"
        }

        return AdonisIndex(
            currentRatio = ratio,
            progressPct = progressPct,
            tier = tier,
            statusSummary = summary
        )
    }

    private fun calculateLimbSymmetries(latest: BodyMeasurementEntity): List<LimbSymmetry> {
        val symmetries = mutableListOf<LimbSymmetry>()

        symmetries.add(calculateLimbSymmetry("Arms", latest.leftArmCm, latest.rightArmCm))
        symmetries.add(calculateLimbSymmetry("Thighs", latest.leftThighCm, latest.rightThighCm))
        symmetries.add(calculateLimbSymmetry("Calves", latest.leftCalfCm, latest.rightCalfCm))

        return symmetries.filter { it.leftCm > 0.0 && it.rightCm > 0.0 }
    }

    private fun calculateLimbSymmetry(name: String, left: Double, right: Double): LimbSymmetry {
        val delta = abs(left - right)
        val maxVal = max(left, right)
        val minVal = min(left, right)

        val symmetryPct = if (maxVal > 0.0) (minVal / maxVal) * 100 else 0.0
        val isBalanced = delta <= 0.5

        return LimbSymmetry(
            limbName = name,
            leftCm = left,
            rightCm = right,
            symmetryPct = symmetryPct,
            deltaCm = delta,
            isBalanced = isBalanced
        )
    }

    private fun calculateRecompDelta(sortedHistory: List<BodyMeasurementEntity>, daysWindow: Int): RecompDelta? {
        if (sortedHistory.size < 2) return null

        val latest = sortedHistory.last()
        val latestTime = latest.recordedAt
        val windowMillis = daysWindow * 24L * 60L * 60L * 1000L
        val targetTime = latestTime - windowMillis

        // Find baseline closest to targetTime
        var baseline = sortedHistory.first()
        var minDiff = abs(baseline.recordedAt - targetTime)

        for (i in 1 until sortedHistory.size - 1) { // Exclude latest
            val measurement = sortedHistory[i]
            val diff = abs(measurement.recordedAt - targetTime)
            if (diff < minDiff) {
                minDiff = diff
                baseline = measurement
            }
        }

        if (baseline.id == latest.id) return null

        val actualDaysPeriod = max(1, ((latest.recordedAt - baseline.recordedAt) / (24L * 60L * 60L * 1000L)).toInt())

        return RecompDelta(
            daysPeriod = actualDaysPeriod,
            waistDeltaCm = latest.waistCm - baseline.waistCm,
            shoulderDeltaCm = latest.shouldersCm - baseline.shouldersCm,
            chestDeltaCm = latest.chestCm - baseline.chestCm,
            weightDeltaKg = latest.weightKg - baseline.weightKg
        )
    }

    private fun emptyAdonisIndex() = AdonisIndex(
        currentRatio = 0.0,
        progressPct = 0,
        tier = VTaperTier.NOVICE,
        statusSummary = "No Data"
    )
}
