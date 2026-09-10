package com.gymcoach.app.core.assessment

import java.util.Locale

/**
 * Represents the assessed V-shape level based on shoulder-to-waist ratio.
 *
 * These thresholds are product heuristics for fitness tracking purposes only.
 * They are NOT clinical diagnoses, medical assessments, or evidence-based
 * anthropometric standards. Do not use for medical decision-making.
 */
enum class VShapeLevel(val label: String) {
    /** Insufficient measurement data to compute a ratio. */
    NOT_ENOUGH_DATA("Not enough data"),
    /** Ratio < 1.30 — early stage, minimal V-taper visible. */
    EARLY("Early"),
    /** Ratio 1.30–1.44 — building foundation, V-taper emerging. */
    BUILDING("Building"),
    /** Ratio 1.45–1.59 — developing V-taper, noticeable taper. */
    DEVELOPING("Developing"),
    /** Ratio >= 1.60 — strong V-taper, well-developed. */
    STRONG("Strong")
}

/**
 * Morphological ratios derived from body measurements.
 *
 * Ratios are computed only when both input measurements are > 0.
 * A null ratio indicates insufficient data for that specific calculation.
 *
 * @param shoulderWaistRatio Shoulders / waist (primary V-shape indicator).
 * @param waistHipRatio Waist / hips (secondary proportion context).
 * @param hasMeasurements True if shoulderWaistRatio is non-null (i.e., both shoulders and waist logged).
 */
data class VShapeMorphology(
    val shoulderWaistRatio: Double?,
    val waistHipRatio: Double?,
    val hasMeasurements: Boolean
)

/**
 * Complete V-shape assessment result.
 *
 * Combines morphological ratios with training balance scores to produce
 * a level classification and actionable insights.
 *
 * @param morphology Computed body measurement ratios.
 * @param trainingPrimaryScore Weekly average primary V-taper muscle credits (lats + lateral delts).
 * @param trainingSecondaryScore Weekly average secondary V-taper muscle credits (upper chest + rear delts).
 * @param level Assessed V-shape level based on shoulder/waist ratio.
 * @param insights Ordered list of actionable guidance strings (never empty).
 */
data class VShapeAssessment(
    val morphology: VShapeMorphology,
    val trainingPrimaryScore: Double,
    val trainingSecondaryScore: Double,
    val level: VShapeLevel,
    val insights: List<String>
)

/**
 * Pure-Kotlin calculator for V-shape assessment.
 *
 * This object contains no Android dependencies and no external state.
 * It is designed to be compiled with plain kotlinc and run on the JVM.
 *
 * Heuristic thresholds (product-defined, NOT clinical):
 * - NOT_ENOUGH_DATA: shouldersCm <= 0 OR waistCm <= 0
 * - EARLY: ratio < 1.30
 * - BUILDING: 1.30 <= ratio < 1.45
 * - DEVELOPING: 1.45 <= ratio < 1.60
 * - STRONG: ratio >= 1.60
 *
 * Insight generation order (all applicable, never empty):
 * 1. If NOT_ENOUGH_DATA: "Log shoulders and waist to assess your V-shape"
 * 2. If trainingPrimaryScore < 2.0: "Add lateral delt and lat volume to drive the V-shape"
 * 3. If trainingSecondaryScore < 2.0: "Upper chest and rear delt volume is underloaded"
 * 4. If trainingPrimaryScore >= 3.0 AND trainingSecondaryScore >= 3.0: "Excellent V-taper training balance"
 * 5. Fallback (data exists, no training flags above): "Keep training balanced across lats, lateral delts, and rear delts"
 *
 * All ratio formatting uses Locale.US for locale-independent output.
 */
object VShapeAssessmentCalculator {

    /**
     * Assesses V-shape from measurements and training scores.
     *
     * @param shouldersCm Shoulder circumference in centimeters.
     * @param waistCm Waist circumference in centimeters.
     * @param hipsCm Hip circumference in centimeters.
     * @param trainingPrimaryScore Average weekly primary V-taper credits (lats + lateral delts).
     * @param trainingSecondaryScore Average weekly secondary V-taper credits (upper chest + rear delts).
     * @return Complete assessment with morphology, level, and insights.
     */
    fun assess(
        shouldersCm: Double,
        waistCm: Double,
        hipsCm: Double,
        trainingPrimaryScore: Double,
        trainingSecondaryScore: Double
    ): VShapeAssessment {

        // Compute ratios — only when both inputs > 0
        val shoulderWaistRatio = if (shouldersCm > 0 && waistCm > 0) {
            shouldersCm / waistCm
        } else null

        val waistHipRatio = if (waistCm > 0 && hipsCm > 0) {
            waistCm / hipsCm
        } else null

        val hasMeasurements = shoulderWaistRatio != null

        val morphology = VShapeMorphology(
            shoulderWaistRatio = shoulderWaistRatio,
            waistHipRatio = waistHipRatio,
            hasMeasurements = hasMeasurements
        )

        // Determine level — smart-cast after null guard (no bang operator,
        // so NOT_ENOUGH_DATA is returned instead of an NPE for missing data)
        val level = when {
            !hasMeasurements -> VShapeLevel.NOT_ENOUGH_DATA
            shoulderWaistRatio == null -> VShapeLevel.NOT_ENOUGH_DATA
            shoulderWaistRatio < 1.30 -> VShapeLevel.EARLY
            shoulderWaistRatio < 1.45 -> VShapeLevel.BUILDING
            shoulderWaistRatio < 1.60 -> VShapeLevel.DEVELOPING
            else -> VShapeLevel.STRONG
        }

        // Build insights in declared order
        val insights = mutableListOf<String>()

        if (!hasMeasurements) {
            insights.add("Log shoulders and waist to assess your V-shape")
        } else {
            if (trainingPrimaryScore < 2.0) {
                insights.add("Add lateral delt and lat volume to drive the V-shape")
            }
            if (trainingSecondaryScore < 2.0) {
                insights.add("Upper chest and rear delt volume is underloaded")
            }
            if (trainingPrimaryScore >= 3.0 && trainingSecondaryScore >= 3.0) {
                insights.add("Excellent V-taper training balance")
            }
            // Fallback when data exists but no training flags triggered
            if (insights.isEmpty()) {
                insights.add("Keep training balanced across lats, lateral delts, and rear delts")
            }
        }

        return VShapeAssessment(
            morphology = morphology,
            trainingPrimaryScore = trainingPrimaryScore,
            trainingSecondaryScore = trainingSecondaryScore,
            level = level,
            insights = insights
        )
    }

    /**
     * Formats a ratio for display using Locale.US (locale-independent).
     *
     * @param ratio The ratio to format, or null.
     * @return Formatted string "X.XX" or "—" if null.
     */
    fun formatRatio(ratio: Double?): String {
        return ratio?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    }
}