package com.gymcoach.app.core.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TdeeMacroCalculatorTest {

    @Test
    fun `calculateMifflinStJeorBmr calculates correct BMR for male`() {
        // 80kg, 180cm, 25 years old male
        // BMR = (10 * 80) + (6.25 * 180) - (5 * 25) + 5 = 800 + 1125 - 125 + 5 = 1805
        val bmr = TdeeMacroCalculator.calculateMifflinStJeorBmr(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 25,
            sex = BiologicalSex.MALE
        )
        assertEquals(1805.0, bmr, 0.01)
    }

    @Test
    fun `calculateMifflinStJeorBmr calculates correct BMR for female`() {
        // 60kg, 165cm, 30 years old female
        // BMR = (10 * 60) + (6.25 * 165) - (5 * 30) - 161 = 600 + 1031.25 - 150 - 161 = 1320.25
        val bmr = TdeeMacroCalculator.calculateMifflinStJeorBmr(
            weightKg = 60.0,
            heightCm = 165.0,
            age = 30,
            sex = BiologicalSex.FEMALE
        )
        assertEquals(1320.25, bmr, 0.01)
    }

    @Test
    fun `calculateKatchMcArdleBmr calculates correct BMR with body fat percentage`() {
        // 80kg with 15% body fat -> Lean Body Mass = 68kg
        // BMR = 370 + (21.6 * 68) = 370 + 1468.8 = 1838.8
        val bmr = TdeeMacroCalculator.calculateKatchMcArdleBmr(
            weightKg = 80.0,
            bodyFatPercentage = 15.0
        )
        assertEquals(1838.8, bmr, 0.01)
    }

    @Test
    fun `calculateTdee multiplies BMR by activity factor accurately`() {
        val bmr = 1800.0
        val sedentaryTdee = TdeeMacroCalculator.calculateTdee(bmr, ActivityLevel.SEDENTARY)
        val moderateTdee = TdeeMacroCalculator.calculateTdee(bmr, ActivityLevel.MODERATELY_ACTIVE)
        val veryActiveTdee = TdeeMacroCalculator.calculateTdee(bmr, ActivityLevel.VERY_ACTIVE)

        assertEquals(2160.0, sedentaryTdee, 0.01)
        assertEquals(2790.0, moderateTdee, 0.01)
        assertEquals(3105.0, veryActiveTdee, 0.01)
    }

    @Test
    fun `inferFromTraining selects correct activity level`() {
        assertEquals(ActivityLevel.VERY_ACTIVE, ActivityLevel.inferFromTraining(6, 60))
        assertEquals(ActivityLevel.VERY_ACTIVE, ActivityLevel.inferFromTraining(5, 90))
        assertEquals(ActivityLevel.MODERATELY_ACTIVE, ActivityLevel.inferFromTraining(4, 60))
        assertEquals(ActivityLevel.MODERATELY_ACTIVE, ActivityLevel.inferFromTraining(3, 60))
        assertEquals(ActivityLevel.LIGHTLY_ACTIVE, ActivityLevel.inferFromTraining(2, 45))
        assertEquals(ActivityLevel.SEDENTARY, ActivityLevel.inferFromTraining(1, 30))
        assertEquals(ActivityLevel.SEDENTARY, ActivityLevel.inferFromTraining(0, 0))
    }

    @Test
    fun `calculate creates full profile for lean bulk`() {
        val profile = TdeeMacroCalculator.calculate(
            weightKg = 75.0,
            heightCm = 178.0,
            age = 26,
            sex = BiologicalSex.MALE,
            goal = NutritionGoal.LEAN_BULK,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            trainingDaysPerWeek = 4,
            sessionLengthMinutes = 60
        )

        assertEquals(BmrFormula.MIFFLIN_ST_JEOR, profile.formulaUsed)
        assertTrue(profile.bmr in 1600..1900)
        assertTrue(profile.tdee in 2500..3000)
        assertTrue(profile.targetCalories > profile.tdee) // Caloric surplus
        assertEquals(NutritionGoal.LEAN_BULK, profile.goal)
        assertTrue(profile.macroSplit.proteinGrams >= 150f)
        assertTrue(profile.macroSplit.fatGrams >= 50f)
        assertTrue(profile.macroSplit.carbsGrams >= 150f)
        assertTrue(profile.fiberGrams in 25f..50f)
        assertTrue(profile.waterMlTarget in 2500..4000)
    }

    @Test
    fun `calculate creates deficit for aggressive cut with high protein preservation`() {
        val profile = TdeeMacroCalculator.calculate(
            weightKg = 90.0,
            heightCm = 185.0,
            age = 30,
            sex = BiologicalSex.MALE,
            goal = NutritionGoal.AGGRESSIVE_CUT,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE
        )

        assertTrue(profile.targetCalories < profile.tdee) // Caloric deficit
        assertTrue(profile.calorieAdjustment < 0)
        // High protein for muscle preservation: 90kg * 2.4 = 216g
        assertEquals(216.0f, profile.macroSplit.proteinGrams, 1.0f)
    }

    @Test
    fun `calculate uses Katch-McArdle when valid body fat is provided`() {
        val profile = TdeeMacroCalculator.calculate(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 28,
            sex = BiologicalSex.MALE,
            goal = NutritionGoal.MAINTENANCE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            bodyFatPercentage = 12.0
        )

        assertEquals(BmrFormula.KATCH_MCARDLE, profile.formulaUsed)
    }

    @Test
    fun `BiologicalSex fromString handles case variations and edge cases`() {
        assertEquals(BiologicalSex.MALE, BiologicalSex.fromString("male"))
        assertEquals(BiologicalSex.MALE, BiologicalSex.fromString("MALE"))
        assertEquals(BiologicalSex.MALE, BiologicalSex.fromString("m"))
        assertEquals(BiologicalSex.FEMALE, BiologicalSex.fromString("female"))
        assertEquals(BiologicalSex.FEMALE, BiologicalSex.fromString("Woman"))
        assertEquals(BiologicalSex.UNSPECIFIED, BiologicalSex.fromString("other"))
        assertEquals(BiologicalSex.UNSPECIFIED, BiologicalSex.fromString(""))
    }

    @Test
    fun `NutritionGoal fromString maps common user goal phrases accurately`() {
        assertEquals(NutritionGoal.AGGRESSIVE_CUT, NutritionGoal.fromString("aggressive cut"))
        assertEquals(NutritionGoal.MODERATE_CUT, NutritionGoal.fromString("cut"))
        assertEquals(NutritionGoal.MODERATE_CUT, NutritionGoal.fromString("fat loss"))
        assertEquals(NutritionGoal.MODERATE_CUT, NutritionGoal.fromString("weight loss"))
        assertEquals(NutritionGoal.RECOMPOSITION, NutritionGoal.fromString("body recomp"))
        assertEquals(NutritionGoal.LEAN_BULK, NutritionGoal.fromString("hypertrophy"))
        assertEquals(NutritionGoal.LEAN_BULK, NutritionGoal.fromString("muscle gain"))
        assertEquals(NutritionGoal.AGGRESSIVE_BULK, NutritionGoal.fromString("mass gain"))
        assertEquals(NutritionGoal.MAINTENANCE, NutritionGoal.fromString("general fitness"))
    }
}
