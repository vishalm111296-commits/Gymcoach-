package com.gymcoach.app.presentation.profile

import com.gymcoach.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileViewModelTest {

    @Test
    fun testProfileFormMapping() {
        val original = UserProfile(
            id = 1,
            name = "Test User",
            equipment = "Dumbbell,Flat Bench,Bodyweight"
        )
        val form = ProfileForm.fromProfile(original)
        assertEquals("Test User", form.name)
        assertEquals("Dumbbell,Flat Bench,Bodyweight", form.equipment)

        val profile = form.toProfile()
        assertEquals("Test User", profile.name)
        assertEquals("Dumbbell,Flat Bench,Bodyweight", profile.equipment)
    }

    private fun ProfileForm.toProfile(): UserProfile {
        return UserProfile(
            id = 1,
            name = name,
            age = age,
            gender = gender,
            height = height,
            weight = weight,
            goalWeight = goalWeight,
            currentGoal = "",
            experience = experience,
            trainingStyle = trainingStyle,
            preferredSplit = preferredSplit,
            activityLevel = activityLevel,
            weeklyWorkoutGoal = weeklyWorkoutGoal,
            proteinGoal = proteinGoal,
            caloriesGoal = caloriesGoal,
            units = units,
            avatarUrl = "",
            leanBodyMass = 0.0,
            maintenanceCalories = 0,
            equipment = equipment
        )
    }
}
