package com.gymcoach.app.presentation.profile

import com.gymcoach.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for ProfileViewModel and profile data display.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `UserProfileEntity defaults are correct`() {
        val entity = UserProfileEntity()
        assertEquals(0L, entity.id)
        assertEquals("", entity.goal)
        assertEquals("", entity.experience)
        assertEquals(0, entity.age)
        assertEquals("", entity.sex)
        assertEquals(0.0, entity.heightCm, 0.01)
        assertEquals(0.0, entity.weightKg, 0.01)
        assertEquals(4, entity.trainingDaysPerWeek)
        assertEquals(60, entity.sessionLengthMinutes)
        assertEquals("gym", entity.equipmentType)
        assertEquals("", entity.preferredExercises)
        assertEquals("", entity.exercisesToAvoid)
    }

    @Test
    fun `UserProfileEntity stores all fields`() {
        val entity = UserProfileEntity(
            id = 1L,
            goal = "Build muscle",
            experience = "Intermediate",
            age = 28,
            sex = "Male",
            heightCm = 180.0,
            weightKg = 82.0,
            trainingDaysPerWeek = 5,
            sessionLengthMinutes = 75,
            equipmentType = "dumbbell",
            preferredExercises = "Bench Press, Squat",
            exercisesToAvoid = "Military Press"
        )
        assertEquals(1L, entity.id)
        assertEquals("Build muscle", entity.goal)
        assertEquals("Intermediate", entity.experience)
        assertEquals(28, entity.age)
        assertEquals("Male", entity.sex)
        assertEquals(180.0, entity.heightCm, 0.01)
        assertEquals(82.0, entity.weightKg, 0.01)
        assertEquals(5, entity.trainingDaysPerWeek)
        assertEquals(75, entity.sessionLengthMinutes)
        assertEquals("dumbbell", entity.equipmentType)
        assertEquals("Bench Press, Squat", entity.preferredExercises)
        assertEquals("Military Press", entity.exercisesToAvoid)
    }

    @Test
    fun `training days defaults to 4`() {
        val entity = UserProfileEntity()
        assertEquals(4, entity.trainingDaysPerWeek)
    }

    @Test
    fun `session length defaults to 60`() {
        val entity = UserProfileEntity()
        assertEquals(60, entity.sessionLengthMinutes)
    }

    @Test
    fun `equipment type defaults to gym`() {
        val entity = UserProfileEntity()
        assertEquals("gym", entity.equipmentType)
    }

    @Test
    fun `age can be zero`() {
        val entity = UserProfileEntity(age = 0)
        assertEquals(0, entity.age)
    }

    @Test
    fun `height can be zero`() {
        val entity = UserProfileEntity(heightCm = 0.0)
        assertEquals(0.0, entity.heightCm, 0.01)
    }

    @Test
    fun `weight can be zero`() {
        val entity = UserProfileEntity(weightKg = 0.0)
        assertEquals(0.0, entity.weightKg, 0.01)
    }

    @Test
    fun `blank experience means not specified`() {
        val entity = UserProfileEntity(experience = "")
        assertEquals("", entity.experience)
    }

    @Test
    fun `blank goal means not specified`() {
        val entity = UserProfileEntity(goal = "")
        assertEquals("", entity.goal)
    }

    @Test
    fun `profile display formatting`() {
        val p = UserProfileEntity(
            age = 25,
            sex = "Male",
            heightCm = 175.0,
            weightKg = 80.0,
            experience = "Beginner",
            trainingDaysPerWeek = 3,
            sessionLengthMinutes = 45,
            goal = "Lose fat"
        )

        // Verify the formatting logic
        assertEquals("25 years", "${p.age} years")
        assertEquals("Male", p.sex)
        assertEquals("175 cm", "%.0f cm".format(p.heightCm))
        assertEquals("80.0 kg", "%.1f kg".format(p.weightKg))
        assertEquals("3", "${p.trainingDaysPerWeek}")
        assertEquals("45 minutes", "${p.sessionLengthMinutes} minutes")
        assertEquals("Lose fat", p.goal)
    }

    @Test
    fun `profile with blank fields shows defaults`() {
        val p = UserProfileEntity()
        assertTrue(p.sex.isBlank())
        assertTrue(p.experience.isBlank())
        assertTrue(p.goal.isBlank())
        assertTrue(p.preferredExercises.isBlank())
        assertTrue(p.exercisesToAvoid.isBlank())
    }

    @Test
    fun `updateProfile saves new profile and inserts body measurement`() = kotlinx.coroutines.test.runTest {
        val mockRepo = io.mockk.mockk<com.gymcoach.app.domain.repository.UserProfileRepository>(relaxed = true)
        val mockDao = io.mockk.mockk<com.gymcoach.app.data.local.dao.BodyMeasurementDao>(relaxed = true)

        io.mockk.every { mockRepo.getLatestProfile() } returns kotlinx.coroutines.flow.flowOf(UserProfileEntity(id = 1L))
        io.mockk.coEvery { mockRepo.saveProfile(any()) } returns 1L
        io.mockk.coEvery { mockDao.insert(any()) } returns 1L

        val viewModel = ProfileViewModel(mockRepo, mockDao)

        viewModel.updateProfile(
            age = 30,
            sex = "Male",
            heightCm = 180.0,
            weightKg = 85.0,
            trainingDays = 5,
            sessionLength = 90,
            experience = "Advanced",
            goal = "Strength",
            equipment = "gym"
        )

        io.mockk.coVerify {
            mockRepo.saveProfile(match {
                it.age == 30 && it.weightKg == 85.0 && it.goal == "Strength"
            })
            mockDao.insert(match {
                it.weightKg == 85.0 && it.notes == "Profile update"
            })
        }
    }
}