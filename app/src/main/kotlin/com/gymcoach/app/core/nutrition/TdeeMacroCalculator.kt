package com.gymcoach.app.core.nutrition

import kotlin.math.roundToInt

enum class BiologicalSex {
    MALE,
    FEMALE,
    UNSPECIFIED;

    companion object {
        fun fromString(sex: String): BiologicalSex {
            val normalized = sex.trim().lowercase()
            return when {
                normalized.startsWith("m") -> MALE
                normalized.startsWith("f") || normalized.startsWith("w") -> FEMALE
                else -> UNSPECIFIED
            }
        }
    }
}

enum class NutritionGoal(val displayName: String, val calorieMultiplier: Double, val proteinPerKg: Double) {
    AGGRESSIVE_CUT("Aggressive Cut (-25%)", 0.75, 2.4),
    MODERATE_CUT("Fat Loss / Cut (-15%)", 0.85, 2.2),
    RECOMPOSITION("Body Recomposition (0%)", 1.00, 2.2),
    MAINTENANCE("Maintenance (0%)", 1.00, 2.0),
    LEAN_BULK("Lean Muscle Growth (+10%)", 1.10, 2.0),
    AGGRESSIVE_BULK("Aggressive Mass Gain (+18%)", 1.18, 1.8);

    companion object {
        fun fromString(goal: String): NutritionGoal {
            val normalized = goal.trim().lowercase()
            return when {
                normalized.contains("aggressive cut") || normalized.contains("rapid cut") -> AGGRESSIVE_CUT
                normalized.contains("cut") || normalized.contains("fat loss") || normalized.contains("weight loss") -> MODERATE_CUT
                normalized.contains("recomp") -> RECOMPOSITION
                normalized.contains("aggressive bulk") || normalized.contains("mass") -> AGGRESSIVE_BULK
                normalized.contains("bulk") || normalized.contains("muscle") || normalized.contains("hypertrophy") -> LEAN_BULK
                else -> MAINTENANCE
            }
        }
    }
}

enum class ActivityLevel(val multiplier: Double, val displayName: String, val description: String) {
    SEDENTARY(1.20, "Sedentary", "Desk job, little to no exercise (< 2 days/wk)"),
    LIGHTLY_ACTIVE(1.375, "Lightly Active", "Light training 2-3 days/wk"),
    MODERATELY_ACTIVE(1.55, "Moderately Active", "Moderate training 3-5 days/wk"),
    VERY_ACTIVE(1.725, "Very Active", "Hard training 6-7 days/wk"),
    EXTRA_ACTIVE(1.90, "Extra Active", "Athlete / 2x daily or physical job");

    companion object {
        fun inferFromTraining(daysPerWeek: Int, sessionLengthMinutes: Int): ActivityLevel {
            return when {
                daysPerWeek >= 6 || (daysPerWeek >= 5 && sessionLengthMinutes >= 75) -> VERY_ACTIVE
                daysPerWeek in 4..5 || (daysPerWeek == 3 && sessionLengthMinutes >= 60) -> MODERATELY_ACTIVE
                daysPerWeek in 2..3 -> LIGHTLY_ACTIVE
                else -> SEDENTARY
            }
        }
    }
}

enum class BmrFormula {
    MIFFLIN_ST_JEOR,
    KATCH_MCARDLE
}

data class MacroSplit(
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val proteinCalories: Int,
    val carbsCalories: Int,
    val fatCalories: Int,
    val proteinPercent: Float,
    val carbsPercent: Float,
    val fatPercent: Float
)

data class TdeeProfile(
    val bmr: Int,
    val tdee: Int,
    val targetCalories: Int,
    val calorieAdjustment: Int,
    val goal: NutritionGoal,
    val activityLevel: ActivityLevel,
    val formulaUsed: BmrFormula,
    val macroSplit: MacroSplit,
    val fiberGrams: Float,
    val waterMlTarget: Int
)

/**
 * Scientific energy expenditure (BMR / TDEE) and macronutrient allocation engine.
 * Implements Mifflin-St Jeor & Katch-McArdle metabolic calculations with dynamic protein-sparing ratios.
 */
object TdeeMacroCalculator {

    private const val MIN_SAFE_CALORIES = 1200
    private const val MAX_SAFE_CALORIES = 5500

    /**
     * Calculates Basal Metabolic Rate (BMR) using Mifflin-St Jeor formula.
     * Men: BMR = (10 * weight_kg) + (6.25 * height_cm) - (5 * age) + 5
     * Women: BMR = (10 * weight_kg) + (6.25 * height_cm) - (5 * age) - 161
     */
    fun calculateMifflinStJeorBmr(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        sex: BiologicalSex
    ): Double {
        val safeWeight = weightKg.coerceIn(30.0, 300.0)
        val safeHeight = heightCm.coerceIn(100.0, 250.0)
        val safeAge = age.coerceIn(14, 100)

        val base = (10.0 * safeWeight) + (6.25 * safeHeight) - (5.0 * safeAge)
        return when (sex) {
            BiologicalSex.MALE -> base + 5.0
            BiologicalSex.FEMALE -> base - 161.0
            BiologicalSex.UNSPECIFIED -> base - 78.0 // Neutral midpoint
        }
    }

    /**
     * Calculates Basal Metabolic Rate (BMR) using Katch-McArdle formula based on Lean Body Mass.
     * BMR = 370 + (21.6 * LBM_kg)
     */
    fun calculateKatchMcArdleBmr(
        weightKg: Double,
        bodyFatPercentage: Double
    ): Double {
        val safeWeight = weightKg.coerceIn(30.0, 300.0)
        val safeBodyFat = bodyFatPercentage.coerceIn(4.0, 60.0)
        val leanBodyMassKg = safeWeight * (1.0 - (safeBodyFat / 100.0))
        return 370.0 + (21.6 * leanBodyMassKg)
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE).
     */
    fun calculateTdee(bmr: Double, activityLevel: ActivityLevel): Double {
        return bmr * activityLevel.multiplier
    }

    /**
     * Generates a complete TdeeProfile and MacroSplit for a user.
     */
    fun calculate(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        sex: BiologicalSex,
        goal: NutritionGoal,
        activityLevel: ActivityLevel,
        bodyFatPercentage: Double? = null,
        trainingDaysPerWeek: Int = 4,
        sessionLengthMinutes: Int = 60
    ): TdeeProfile {
        val formulaUsed = if (bodyFatPercentage != null && bodyFatPercentage in 4.0..60.0) {
            BmrFormula.KATCH_MCARDLE
        } else {
            BmrFormula.MIFFLIN_ST_JEOR
        }

        val bmrRaw = if (formulaUsed == BmrFormula.KATCH_MCARDLE && bodyFatPercentage != null) {
            calculateKatchMcArdleBmr(weightKg, bodyFatPercentage)
        } else {
            calculateMifflinStJeorBmr(weightKg, heightCm, age, sex)
        }

        val bmr = bmrRaw.roundToInt()
        val tdeeRaw = calculateTdee(bmrRaw, activityLevel)
        val tdee = tdeeRaw.roundToInt()

        // Calorie target based on fitness goal
        val targetCaloriesUnclamped = (tdeeRaw * goal.calorieMultiplier).roundToInt()
        val targetCalories = targetCaloriesUnclamped.coerceIn(MIN_SAFE_CALORIES, MAX_SAFE_CALORIES)
        val calorieAdjustment = targetCalories - tdee

        // Protein calculation (g / kg bodyweight)
        val safeWeight = weightKg.coerceIn(30.0, 300.0)
        val rawProteinGrams = (safeWeight * goal.proteinPerKg).toFloat()
        val proteinGrams = rawProteinGrams.coerceIn(80f, 320f).roundToOneDecimal()
        val proteinCalories = (proteinGrams * 4f).roundToInt()

        // Fat calculation (25% of target energy, clamped to minimum 0.7g/kg for hormonal health)
        val minHealthyFatGrams = (safeWeight * 0.75f).toFloat()
        val targetFatCalories = targetCalories * 0.25f
        val calculatedFatGrams = (targetFatCalories / 9f).coerceAtLeast(minHealthyFatGrams)
        val fatGrams = calculatedFatGrams.coerceIn(40f, 150f).roundToOneDecimal()
        val fatCalories = (fatGrams * 9f).roundToInt()

        // Carbohydrates allocation (remaining calories)
        val remainingCaloriesForCarbs = (targetCalories - proteinCalories - fatCalories).coerceAtLeast(200)
        val carbsGrams = (remainingCaloriesForCarbs / 4f).coerceIn(40f, 700f).roundToOneDecimal()
        val carbsCalories = (carbsGrams * 4f).roundToInt()

        val totalMacroCalories = (proteinCalories + fatCalories + carbsCalories).coerceAtLeast(1)
        val proteinPercent = ((proteinCalories.toFloat() / totalMacroCalories) * 100f).roundToOneDecimal()
        val carbsPercent = ((carbsCalories.toFloat() / totalMacroCalories) * 100f).roundToOneDecimal()
        val fatPercent = ((fatCalories.toFloat() / totalMacroCalories) * 100f).roundToOneDecimal()

        val macroSplit = MacroSplit(
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            proteinCalories = proteinCalories,
            carbsCalories = carbsCalories,
            fatCalories = fatCalories,
            proteinPercent = proteinPercent,
            carbsPercent = carbsPercent,
            fatPercent = fatPercent
        )

        // Fiber target: 14g per 1000 kcal consumed (clamped between 25g and 55g)
        val fiberGrams = ((targetCalories / 1000f) * 14f).coerceIn(25f, 55f).roundToOneDecimal()

        // Daily water intake target: 35ml/kg bodyweight + workout hydration adjustment
        val baseWaterMl = (safeWeight * 35.0).roundToInt()
        val workoutWaterMl = if (trainingDaysPerWeek > 0) ((sessionLengthMinutes / 60.0) * 500.0).roundToInt() else 0
        val waterMlTarget = (baseWaterMl + workoutWaterMl).coerceIn(2000, 5000)

        return TdeeProfile(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            calorieAdjustment = calorieAdjustment,
            goal = goal,
            activityLevel = activityLevel,
            formulaUsed = formulaUsed,
            macroSplit = macroSplit,
            fiberGrams = fiberGrams,
            waterMlTarget = waterMlTarget
        )
    }

    private fun Float.roundToOneDecimal(): Float {
        return (this * 10f).roundToInt() / 10f
    }
}
