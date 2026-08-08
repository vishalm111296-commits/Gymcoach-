package com.gymcoach.app.domain.vshape.usecase

import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.VShapeIndex
import com.gymcoach.app.domain.vshape.model.MuscleBalance
import com.gymcoach.app.data.local.entity.VShapeChallengeCompletion
import java.time.Instant

interface VShapeAssessmentUseCase {
    fun calculateVShapeIndex(shoulderCircumference: Float, waistCircumference: Float): VShapeIndex
    fun calculateMuscleBalance(measurements: List<MeasurementRecord>): MuscleBalance
    fun calculateProgress(measurements: List<MeasurementRecord>, challenges: List<VShapeChallengeCompletion>): ProgressMetrics
    fun generateAssessmentRecommendations(
        vShapeIndex: VShapeIndex,
        muscleBalance: MuscleBalance,
        progressMetrics: ProgressMetrics
    ): List<String>
    fun calculateTargetTimeline(
        currentIndex: VShapeIndex,
        targetRatio: Float,
        dailyProgressRate: Float
    ): TimelineEstimate
}