package com.gymcoach.app.domain.vshape.usecase.impl

import com.gymcoach.app.data.local.entity.VShapeChallengeCompletion
import com.gymcoach.app.data.local.entity.BodyMeasurement
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.Goal
import com.gymcoach.app.domain.vshape.model.Challenge
import com.gymcoach.app.domain.vshape.model.VShapeIndex
import com.gymcoach.app.domain.vshape.model.MuscleBalance
import com.gymcoach.app.domain.vshape.usecase.VShapeAssessmentUseCase
import javax.inject.Inject

class VShapeAssessmentUseCaseImpl @Inject constructor() : VShapeAssessmentUseCase {
    override fun calculateVShapeIndex(shoulderCircumference: Float, waistCircumference: Float): VShapeIndex {
        val ratio = shoulderCircumference / waistCircumference
        val category = when {
            ratio >= 1.5f -> VShapeIndex.Category.EXCELLENT
            ratio >= 1.3f -> VShapeIndex.Category.GOOD
            ratio >= 1.2f -> VShapeIndex.Category.FAIR
            else -> VShapeIndex.Category.POOR
        }
        val targetRatio = 1.4f
        val progress = if (ratio >= targetRatio) 100f else (ratio / targetRatio) * 100f
        
        return VShapeIndex(
            ratio = ratio,
            category = category,
            targetRatio = targetRatio,
            progressPercentage = progress,
            shoulderCircumference = shoulderCircumference,
            waistCircumference = waistCircumference,
            createdAt = java.time.Instant.now()
        )
    }

    override fun calculateMuscleBalance(measurements: List<MeasurementRecord>): MuscleBalance {
        val latWidth = measurements.find { it.measurementType.name == "SHOULDER_WIDTH" }?.value ?: 0.0
        val waistCircumference = measurements.find { it.measurementType.name == "WAIST_CIRCUMFERENCE" }?.value ?: 0.0
        val chestWidth = measurements.find { it.measurementType.name == "CHEST_CIRCUMFERENCE" }?.value ?: 0.0
        val backWidth = measurements.find { it.measurementType.name == "BACK_WIDTH" }?.value ?: 0.0
        
        val shoulderRatio = latWidth / waistCircumference
        val chestRatio = chestWidth / waistCircumference
        val backRatio = backWidth / waistCircumference
        
        return MuscleBalance(
            shoulderRatio = shoulderRatio,
            chestRatio = chestRatio,
            backRatio = backRatio,
            waistCircumference = waistCircumference,
            overallBalanceScore = calculateBalanceScore(listOf(shoulderRatio, chestRatio, backRatio)),
            createdAt = java.time.Instant.now()
        )
    }

    override fun calculateProgress(measurements: List<MeasurementRecord>, challenges: List<VShapeChallengeCompletion>): ProgressMetrics {
        val vShapeIndices = mutableListOf<VShapeIndex>()
        measurements.forEach { measurement ->
            if (measurement.measurementType.name == "SHOULDER_WIDTH" || measurement.measurementType.name == "WAIST_CIRCUMFERENCE") {
                val shoulder = measurements.find { it.measurementType.name == "SHOULDER_WIDTH" }?.value ?: 0.0
                val waist = measurements.find { it.measurementType.name == "WAIST_CIRCUMFERENCE" }?.value ?: 0.0
                if (shoulder > 0 && waist > 0) {
                    vShapeIndices.add(calculateVShapeIndex(shoulder.toFloat(), waist.toFloat()))
                }
            }
        }
        
        val currentIndex = vShapeIndices.lastOrNull()
        val averageProgress = if (vShapeIndices.isNotEmpty()) {
            vShapeIndices.map { it.progressPercentage }.average()
        } else 0.0
        
        val completedDays = challenges.count { it.completed }
        val streak = challenges.filter { it.completed }.maxOfOrNull { it.day } ?: 0
        
        return ProgressMetrics(
            currentVShapeIndex = currentIndex,
            averageProgressPercentage = averageProgress,
            completedDays = completedDays,
            streak = streak,
            totalDays = 30,
            completionRate = if (30 > 0) (completedDays / 30.0 * 100).toFloat() else 0f,
            lastMeasurementDate = measurements.maxOfOrNull { it.date }?.atStartOfDay() ?: java.time.Instant.now(),
            createdAt = java.time.Instant.now()
        )
    }

    override fun generateAssessmentRecommendations(
        vShapeIndex: VShapeIndex,
        muscleBalance: MuscleBalance,
        progressMetrics: ProgressMetrics
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (vShapeIndex.category) {
            VShapeIndex.Category.EXCELLENT -> recommendations.add("Maintain your current training regimen")
            VShapeIndex.Category.GOOD -> recommendations.add("Focus on slight improvements in shoulder width or waist reduction")
            VShapeIndex.Category.FAIR -> recommendations.add("Increase shoulder training volume by 20% and focus on core stability")
            VShapeIndex.Category.POOR -> recommendations.add("Intensive 12-week program focusing on lat development and waist reduction")
        }
        
        if (muscleBalance.shoulderRatio < 1.2) {
            recommendations.add("Increase shoulder and lat exercises")
        }
        
        if (muscleBalance.chestRatio > 1.1) {
            recommendations.add("Reduce chest training volume, focus on back development")
        }
        
        if (progressMetrics.completionRate < 50) {
            recommendations.add("Increase challenge participation - aim for 80% completion rate")
        }
        
        recommendations.add("Schedule re-assessment in 2 weeks")
        
        return recommendations
    }

    override fun calculateTargetTimeline(
        currentIndex: VShapeIndex,
        targetRatio: Float,
        dailyProgressRate: Float
    ): TimelineEstimate {
        val currentRatio = currentIndex.ratio
        val ratioGap = targetRatio - currentRatio
        
        val daysNeeded = if (dailyProgressRate > 0) {
            (ratioGap * 100 / dailyProgressRate).toInt()
        } else {
            999
        }
        
        val weeksNeeded = (daysNeeded / 7).toInt()
        val monthsNeeded = (daysNeeded / 30).toInt()
        
        val confidence = if (daysNeeded <= 60) 0.9f
        else if (daysNeeded <= 120) 0.7f
        else if (daysNeeded <= 180) 0.5f
        else 0.3f
        
        return TimelineEstimate(
            daysNeeded = daysNeeded,
            weeksNeeded = weeksNeeded,
            monthsNeeded = monthsNeeded,
            confidenceLevel = confidence,
            targetRatio = targetRatio,
            currentRatio = currentRatio,
            dailyProgressRate = dailyProgressRate,
            createdAt = java.time.Instant.now()
        )
    }

    private fun calculateBalanceScore(ratios: List<Double>): Double {
        if (ratios.isEmpty()) return 0.0
        val average = ratios.average()
        val variance = ratios.map { (it - average).pow(2) }.average().sqrt()
        return (average * 10 - variance * 5).coerceIn(0.0, 100.0)
    }
}