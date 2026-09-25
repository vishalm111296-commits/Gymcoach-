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
    fun `calculateMifflinStJeorBmr calculates neutral midpoint for UNSPECIFIED sex`() {
        // 80kg, 180cm, 25 years old UNSPECIFIED
        // Base = 800 + 1125 - 125 = 1800
        // UNSPECIFIED = 1800 - 78 = 1722
        val bmr = TdeeMacroCalculator.calculateMifflinStJeorBmr(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 25,
            sex = BiologicalSex.UNSPECIFIED
        )
        assertEquals(1722.0, bmr, 0.01)
    }

    @Test
    fun `calculate clamps extreme body dimensions to safe physiological ranges`() {
        // Test sub-minimum values: weight 10kg, height 50cm, age 5
        val minBmr = TdeeMacroCalculator.calculateMifflinStJeorBmr(
            weightKg = 10.0,
            heightCm = 50.0,
            age = 5,
            sex = BiologicalSex.MALE
        )
        // Clamped: weight=30kg, height=100cm, age=14 -> (10*30)+(6.25*100)-(5*14)+5 = 300+625-70+5 = 860
        assertEquals(860.0, minBmr, 0.01)

        // Test supra-maximum values: weight 500kg, height 300cm, age 120
        val maxBmr = TdeeMacroCalculator.calculateMifflinStJeorBmr(
            weightKg = 500.0,
            heightCm = 300.0,
            age = 120,
            sex = BiologicalSex.MALE
        )
        // Clamped: weight=300kg, height=250cm, age=100 -> (10*300)+(6.25*250)-(5*100)+5 = 3000+1562.5-500+5 = 4067.5
        assertEquals(4067.5, maxBmr, 0.01)
    }

    @Test
    fun `calculateKatchMcArdleBmr clamps extreme body fat percentages`() {
        // Body fat 1% clamped to 4% -> LBM = 80 * 0.96 = 76.8kg -> 370 + (21.6 * 76.8) = 2028.88
        val minFatBmr = TdeeMacroCalculator.calculateKatchMcArdleBmr(weightKg = 80.0, bodyFatPercentage = 1.0)
        assertEquals(2028.88, minFatBmr, 0.01)

        // Body fat 80% clamped to 60% -> LBM = 80 * 0.40 = 32kg -> 370 + (21.6 * 32) = 1061.2
        val maxFatBmr = TdeeMacroCalculator.calculateKatchMcArdleBmr(weightKg = 80.0, bodyFatPercentage = 80.0)
        assertEquals(1061.2, maxFatBmr, 0.01)
    }

    @Test
    fun `calculate enforces absolute safe calorie limits and macro bounds`() {
        // Extreme cut on 30kg person -> target calories must not drop below MIN_SAFE_CALORIES (1200)
        val lowProfile = TdeeMacroCalculator.calculate(
            weightKg = 30.0,
            heightCm = 120.0,
            age = 50,
            sex = BiologicalSex.FEMALE,
            goal = NutritionGoal.AGGRESSIVE_CUT,
            activityLevel = ActivityLevel.SEDENTARY
        )
        assertEquals(1200, lowProfile.targetCalories)
        assertEquals(80.0f, lowProfile.macroSplit.proteinGrams, 0.1f) // Clamped to 80g min
        assertEquals(40.0f, lowProfile.macroSplit.fatGrams, 0.1f)     // Clamped to 40g min
        assertEquals(2000, lowProfile.waterMlTarget)                  // Clamped to 2000ml min
        assertEquals(25.0f, lowProfile.fiberGrams, 0.1f)              // Clamped to 25g min

        // Extreme bulk on 300kg person -> target calories must not exceed MAX_SAFE_CALORIES (5500)
        val highProfile = TdeeMacroCalculator.calculate(
            weightKg = 300.0,
            heightCm = 220.0,
            age = 22,
            sex = BiologicalSex.MALE,
            goal = NutritionGoal.AGGRESSIVE_BULK,
            activityLevel = ActivityLevel.EXTRA_ACTIVE,
            trainingDaysPerWeek = 7,
            sessionLengthMinutes = 120
        )
        assertEquals(5500, highProfile.targetCalories)
        assertEquals(320.0f, highProfile.macroSplit.proteinGrams, 0.1f) // Clamped to 320g max
        assertEquals(150.0f, highProfile.macroSplit.fatGrams, 0.1f)     // Clamped to 150g max
        assertEquals(5000, highProfile.waterMlTarget)                   // Clamped to 5000ml max
        assertEquals(55.0f, highProfile.fiberGrams, 0.1f)               // Clamped to 55g max
    }

    @Test
    fun `calculate handles zero training days without workout water adjustment`() {
        val profile = TdeeMacroCalculator.calculate(
            weightKg = 70.0,
            heightCm = 175.0,
            age = 25,
            sex = BiologicalSex.MALE,
            goal = NutritionGoal.MAINTENANCE,
            activityLevel = ActivityLevel.SEDENTARY,
            trainingDaysPerWeek = 0,
            sessionLengthMinutes = 0
        )
        // 70 * 35ml = 2450ml with 0 workout adjustment
        assertEquals(2450, profile.waterMlTarget)
    }

    @Test
    fun `NutritionGoal fromString maps common user goal phrases accurately`() {
        assertEquals(NutritionGoal.AGGRESSIVE_CUT, NutritionGoal.fromString("aggressive cut"))
        assertEquals(NutritionGoal.MODERATE_CUT, NutritionGoal.fromString("cut"))
        assertEquals(NutritionGoal.MODERATE_CUT, NutritionGoal.fromString("fat loss"))
        assertEquals(NutritionGoal.AGGRESSIVE_BULK, NutritionGoal.fromString("mass gain"))
        assertEquals(NutritionGoal.MAINTENANCE, NutritionGoal.fromString("general fitness"))
    }

    @Test
    fun `calculate falls back to Mifflin-St Jeor when body fat is outside 4 to 60 percent`() {
        // Below 4% (e.g., 3.0%) -> falls back to Mifflin-St Jeor
        val subMinProfile = TdeeMacroCalculator.calculate(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 25,
            sex = BiologicalSex.MALE,
            goal = NutritionGoal.MAINTENANCE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            bodyFatPercentage = 3.0
        )
        assertEquals(BmrFormula.MIFFLIN_ST_JEOR, subMinProfile.formulaUsed)

        // Above 60% (e.g., 65.0%) -> falls back to Mifflin-St Jeor
        val supraMaxProfile = TdeeMacroCalculator.calculate(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 25,
            sex = BiologicalSex.MALE,
            goal = NutritionGoal.MAINTENANCE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            bodyFatPercentage = 65.0
        )
        assertEquals(BmrFormula.MIFFLIN_ST_JEOR, supraMaxProfile.formulaUsed)
    }

    @Test
    fun `inferFromTraining boundary edge cases`() {
        // 5 days, 75 mins -> VERY_ACTIVE
        assertEquals(ActivityLevel.VERY_ACTIVE, ActivityLevel.inferFromTraining(5, 75))
        // 5 days, 74 mins -> MODERATELY_ACTIVE
        assertEquals(ActivityLevel.MODERATELY_ACTIVE, ActivityLevel.inferFromTraining(5, 74))
        // 3 days, 60 mins -> MODERATELY_ACTIVE
        assertEquals(ActivityLevel.MODERATELY_ACTIVE, ActivityLevel.inferFromTraining(3, 60))
        // 3 days, 59 mins -> LIGHTLY_ACTIVE
        assertEquals(ActivityLevel.LIGHTLY_ACTIVE, ActivityLevel.inferFromTraining(3, 59))
        // 2 days, 90 mins -> LIGHTLY_ACTIVE
        assertEquals(ActivityLevel.LIGHTLY_ACTIVE, ActivityLevel.inferFromTraining(2, 90))
        // 1 day, 90 mins -> SEDENTARY
        assertEquals(ActivityLevel.SEDENTARY, ActivityLevel.inferFromTraining(1, 90))
    }

    @Test
    fun `calculate ensures healthy fat floor of 0_75g per kg for endocrine support`() {
        // Person 80kg on aggressive cut: 25% of 1500 kcal would be 375 kcal / 9 = 41.6g,
        // but min healthy fat is 80 * 0.75 = 60.0g
        val cutProfile = TdeeMacroCalculator.calculate(
            weightKg = 80.0,
            heightCm = 160.0,
            age = 45,
            sex = BiologicalSex.FEMALE,
            goal = NutritionGoal.AGGRESSIVE_CUT,
            activityLevel = ActivityLevel.SEDENTARY
        )
        assertTrue("Fat grams should be at least 60.0g floor", cutProfile.macroSplit.fatGrams >= 60.0f)
    }

    @Test
    fun `calculateMifflinStJeorBmr exact clamping boundary thresholds`() {
        // Weight bounds: [30.0, 300.0]
        val bmrWeight29 = TdeeMacroCalculator.calculateMifflinStJeorBmr(29.0, 175.0, 30, BiologicalSex.MALE)
        val bmrWeight30 = TdeeMacroCalculator.calculateMifflinStJeorBmr(30.0, 175.0, 30, BiologicalSex.MALE)
        assertEquals(bmrWeight30, bmrWeight29, 0.001)

        val bmrWeight300 = TdeeMacroCalculator.calculateMifflinStJeorBmr(300.0, 175.0, 30, BiologicalSex.MALE)
        val bmrWeight301 = TdeeMacroCalculator.calculateMifflinStJeorBmr(301.0, 175.0, 30, BiologicalSex.MALE)
        assertEquals(bmrWeight300, bmrWeight301, 0.001)

        // Height bounds: [100.0, 250.0]
        val bmrHeight99 = TdeeMacroCalculator.calculateMifflinStJeorBmr(75.0, 99.0, 30, BiologicalSex.MALE)
        val bmrHeight100 = TdeeMacroCalculator.calculateMifflinStJeorBmr(75.0, 100.0, 30, BiologicalSex.MALE)
        assertEquals(bmrHeight100, bmrHeight99, 0.001)

        val bmrHeight250 = TdeeMacroCalculator.calculateMifflinStJeorBmr(75.0, 250.0, 30, BiologicalSex.MALE)
        val bmrHeight251 = TdeeMacroCalculator.calculateMifflinStJeorBmr(75.0, 251.0, 30, BiologicalSex.MALE)
        assertEquals(bmrHeight250, bmrHeight251, 0.001)

        // Age bounds: [14, 100]
        val bmrAge13 = TdeeMacroCalculator.calculateMifflinStJeorBmr(75.0, 175.0, 13, BiologicalSex.MALE)
        val bmrAge14 = TdeeMacroCalculator.calculateMifflinStJeorBmr(75.0, 175.0, 14, BiologicalSex.MALE)
        assertEquals(bmrAge14, bmrAge13, 0.001)

        val bmrAge100 = TdeeMacroCalculator.calculateMifflinStJeorBmr(75.0, 175.0, 100, BiologicalSex.MALE)
        val bmrAge101 = TdeeMacroCalculator.calculateMifflinStJeorBmr(75.0, 175.0, 101, BiologicalSex.MALE)
        assertEquals(bmrAge100, bmrAge101, 0.001)
    }

    @Test
    fun `calculate macro percentage summing and calorie consistency across all nutrition goals`() {
        for (goal in NutritionGoal.values()) {
            val profile = TdeeMacroCalculator.calculate(
                weightKg = 80.0,
                heightCm = 180.0,
                age = 28,
                sex = BiologicalSex.MALE,
                goal = goal,
                activityLevel = ActivityLevel.MODERATELY_ACTIVE,
                trainingDaysPerWeek = 4,
                sessionLengthMinutes = 60
            )

            val split = profile.macroSplit
            val totalPercent = split.proteinPercent + split.carbsPercent + split.fatPercent
            // Percentages should sum to 100% within rounding tolerance
            assertTrue("Total percentage for $goal should be ~100%, got $totalPercent", totalPercent in 99.0f..101.0f)

            // Direct calorie consistency checks
            assertEquals(Math.round(split.proteinGrams * 4f), split.proteinCalories)
            assertEquals(Math.round(split.fatGrams * 9f), split.fatCalories)
            assertEquals(Math.round(split.carbsGrams * 4f), split.carbsCalories)

            // Ensure non-zero positive macro amounts
            assertTrue("Protein grams should be positive", split.proteinGrams > 0)
            assertTrue("Fat grams should be positive", split.fatGrams > 0)
            assertTrue("Carbs grams should be positive", split.carbsGrams > 0)
        }
    }

    @Test
    fun `calculate hydration target scaling across session lengths and frequencies`() {
        // Base weight 70kg -> base water = 70 * 35 = 2450ml
        // 0 min session or 0 days -> no workout addition -> 2450ml
        val p0 = TdeeMacroCalculator.calculate(
            weightKg = 70.0, heightCm = 175.0, age = 25, sex = BiologicalSex.MALE,
            goal = NutritionGoal.MAINTENANCE, activityLevel = ActivityLevel.SEDENTARY,
            trainingDaysPerWeek = 0, sessionLengthMinutes = 0
        )
        assertEquals(2450, p0.waterMlTarget)

        // 60 min session -> (60/60) * 500 = 500ml addition -> 2450 + 500 = 2950ml
        val p60 = TdeeMacroCalculator.calculate(
            weightKg = 70.0, heightCm = 175.0, age = 25, sex = BiologicalSex.MALE,
            goal = NutritionGoal.MAINTENANCE, activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            trainingDaysPerWeek = 3, sessionLengthMinutes = 60
        )
        assertEquals(2950, p60.waterMlTarget)

        // 90 min session -> (90/60) * 500 = 750ml addition -> 2450 + 750 = 3200ml
        val p90 = TdeeMacroCalculator.calculate(
            weightKg = 70.0, heightCm = 175.0, age = 25, sex = BiologicalSex.MALE,
            goal = NutritionGoal.MAINTENANCE, activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            trainingDaysPerWeek = 4, sessionLengthMinutes = 90
        )
        assertEquals(3200, p90.waterMlTarget)

        // Minimum clamping check (30kg person with 0 workouts -> 30*35=1050ml -> clamped to 2000ml)
        val pMin = TdeeMacroCalculator.calculate(
            weightKg = 30.0, heightCm = 150.0, age = 25, sex = BiologicalSex.FEMALE,
            goal = NutritionGoal.MAINTENANCE, activityLevel = ActivityLevel.SEDENTARY,
            trainingDaysPerWeek = 0, sessionLengthMinutes = 0
        )
        assertEquals(2000, pMin.waterMlTarget)

        // Maximum clamping check (150kg person with 60 min -> 150*35=5250 + 500 = 5750ml -> clamped to 5000ml)
        val pMax = TdeeMacroCalculator.calculate(
            weightKg = 150.0, heightCm = 195.0, age = 25, sex = BiologicalSex.MALE,
            goal = NutritionGoal.MAINTENANCE, activityLevel = ActivityLevel.VERY_ACTIVE,
            trainingDaysPerWeek = 5, sessionLengthMinutes = 60
        )
        assertEquals(5000, pMax.waterMlTarget)
    }

    @Test
    fun `calculate carbohydrate floor preservation during heavy caloric deficits`() {
        // Very low calorie target: ensure carbs are never starved below 40g floor
        val cutProfile = TdeeMacroCalculator.calculate(
            weightKg = 50.0,
            heightCm = 150.0,
            age = 60,
            sex = BiologicalSex.FEMALE,
            goal = NutritionGoal.AGGRESSIVE_CUT,
            activityLevel = ActivityLevel.SEDENTARY
        )
        assertTrue("Carbs should be at least 40g floor", cutProfile.macroSplit.carbsGrams >= 40.0f)
        assertTrue("Carbs calories should be at least 160 kcal", cutProfile.macroSplit.carbsCalories >= 160)
    }
}

