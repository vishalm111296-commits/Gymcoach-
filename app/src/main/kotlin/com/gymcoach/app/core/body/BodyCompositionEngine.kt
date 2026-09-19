package com.gymcoach.app.core.body

import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

enum class BodyPart { CHEST, SHOULDERS, WAIST, HIPS, ARMS, THIGHS, CALVES }

data class MetricDelta(
    val currentCm: Double,
    val deltaCm: Double
)

data class BodyMetricTrend(
    val currentWeightKg: Double,
    val previousWeightKg: Double?,
    val weeklyRateKg: Double, // weekly weight change rate
    val currentBodyFatPct: Double?,
    val vTaperRatio: Double?, // shoulders_cm / waist_cm
    val vTaperCategory: String, // 'Athletic', 'Golden Ratio', etc.
    val goldenRatioProximityPct: Float, // Proximity to 1.618
    val circumferences: Map<BodyPart, MetricDelta>,
    val history: List<BodyMeasurementEntity>
)

object BodyCompositionEngine {

    fun calculateTrend(history: List<BodyMeasurementEntity>): BodyMetricTrend {
        val sortedHistory = history.sortedByDescending { it.recordedAt }
        val latest = sortedHistory.firstOrNull()
        val previous = sortedHistory.drop(1).firstOrNull()

        val currentWeightKg = latest?.weightKg ?: 0.0
        val previousWeightKg = previous?.weightKg

        val weeklyRateKg = calculateWeeklyRate(latest, previous)

        val currentBodyFatPct = latest?.bodyFatPct?.takeIf { it > 0.0 }

        val shouldersCm = latest?.shouldersCm ?: 0.0
        val waistCm = latest?.waistCm ?: 0.0

        val vTaperRatio = if (shouldersCm > 0 && waistCm > 0) {
            shouldersCm / waistCm
        } else {
            null
        }

        val vTaperCategory = classifyVTaper(vTaperRatio)
        val goldenRatioProximityPct = calculateGoldenRatioProximity(vTaperRatio)

        val circumferences = calculateCircumferences(latest, previous)

        return BodyMetricTrend(
            currentWeightKg = currentWeightKg,
            previousWeightKg = previousWeightKg,
            weeklyRateKg = weeklyRateKg,
            currentBodyFatPct = currentBodyFatPct,
            vTaperRatio = vTaperRatio,
            vTaperCategory = vTaperCategory,
            goldenRatioProximityPct = goldenRatioProximityPct,
            circumferences = circumferences,
            history = sortedHistory
        )
    }

    private fun calculateWeeklyRate(latest: BodyMeasurementEntity?, previous: BodyMeasurementEntity?): Double {
        if (latest == null || previous == null) return 0.0

        val weightDiff = latest.weightKg - previous.weightKg
        val timeDiffMillis = latest.recordedAt - previous.recordedAt

        if (timeDiffMillis <= 0) return 0.0

        val weeks = timeDiffMillis.toDouble() / TimeUnit.DAYS.toMillis(7)
        if (weeks == 0.0) return 0.0

        return weightDiff / weeks
    }

    private fun classifyVTaper(ratio: Double?): String {
        if (ratio == null) return "Unknown"
        return when {
            ratio < 1.40 -> "Standard Frame"
            ratio < 1.55 -> "Athletic Taper"
            ratio <= 1.65 -> "Golden V-Taper"
            else -> "Heroic Frame"
        }
    }

    private fun calculateGoldenRatioProximity(ratio: Double?): Float {
        if (ratio == null) return 0f
        return max(0f, 1f - (abs(ratio - 1.618) / 0.4f).toFloat())
    }

    private fun calculateCircumferences(
        latest: BodyMeasurementEntity?,
        previous: BodyMeasurementEntity?
    ): Map<BodyPart, MetricDelta> {
        val map = mutableMapOf<BodyPart, MetricDelta>()

        if (latest == null) return map

        fun addDelta(part: BodyPart, current: Double, prev: Double?) {
            if (current > 0) {
                val delta = if (prev != null && prev > 0) current - prev else 0.0
                map[part] = MetricDelta(current, delta)
            }
        }

        addDelta(BodyPart.CHEST, latest.chestCm, previous?.chestCm)
        addDelta(BodyPart.SHOULDERS, latest.shouldersCm, previous?.shouldersCm)
        addDelta(BodyPart.WAIST, latest.waistCm, previous?.waistCm)
        addDelta(BodyPart.HIPS, latest.hipsCm, previous?.hipsCm)

        val currentArms = if (latest.leftArmCm > 0 || latest.rightArmCm > 0) {
            val list = listOf(latest.leftArmCm, latest.rightArmCm).filter { it > 0 }
            if (list.isNotEmpty()) list.average() else 0.0
        } else 0.0

        val prevArms = if (previous != null && (previous.leftArmCm > 0 || previous.rightArmCm > 0)) {
            val list = listOf(previous.leftArmCm, previous.rightArmCm).filter { it > 0 }
            if (list.isNotEmpty()) list.average() else 0.0
        } else null

        addDelta(BodyPart.ARMS, currentArms, prevArms)

        val currentThighs = if (latest.leftThighCm > 0 || latest.rightThighCm > 0) {
            val list = listOf(latest.leftThighCm, latest.rightThighCm).filter { it > 0 }
            if (list.isNotEmpty()) list.average() else 0.0
        } else 0.0

        val prevThighs = if (previous != null && (previous.leftThighCm > 0 || previous.rightThighCm > 0)) {
            val list = listOf(previous.leftThighCm, previous.rightThighCm).filter { it > 0 }
            if (list.isNotEmpty()) list.average() else 0.0
        } else null

        addDelta(BodyPart.THIGHS, currentThighs, prevThighs)

        val currentCalves = if (latest.leftCalfCm > 0 || latest.rightCalfCm > 0) {
            val list = listOf(latest.leftCalfCm, latest.rightCalfCm).filter { it > 0 }
            if (list.isNotEmpty()) list.average() else 0.0
        } else 0.0

        val prevCalves = if (previous != null && (previous.leftCalfCm > 0 || previous.rightCalfCm > 0)) {
            val list = listOf(previous.leftCalfCm, previous.rightCalfCm).filter { it > 0 }
            if (list.isNotEmpty()) list.average() else 0.0
        } else null

        addDelta(BodyPart.CALVES, currentCalves, prevCalves)

        return map
    }
}
