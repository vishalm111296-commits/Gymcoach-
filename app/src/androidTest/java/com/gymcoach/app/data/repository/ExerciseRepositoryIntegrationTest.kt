package com.gymcoach.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.domain.model.Exercise
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ExerciseRepositoryImpl using real Room database.
 * Tests verify FTS4 search, equipment filtering, and exercise queries work correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: GymCoachDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: ExerciseRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        exerciseDao = db.exerciseDao()
        repository = ExerciseRepositoryImpl(exerciseDao)
    }

    @Test
    fun getAllExercises_returns_seeded_exercises() = runTest {
        val exercises = repository.getAllExercises().first()
        assertTrue("Should have seeded exercises", exercises.size > 0)
    }

    @Test
    fun searchExercises_uses_FTS4_for_text_queries() = runTest {
        // Insert test exercises
        val benchPress = ExerciseEntity(
            name = "Bench Press",
            description = "Chest exercise",
            muscleGroup = "Chest",
            equipment = "barbell",
            difficulty = "Intermediate",
            secondaryMuscles = "Triceps",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 15,
            category = "Powerlifting",
            tags = "compound,push",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 0,
            vtaperLateralDelt = 2,
            vtaperUpperChest = 7,
            vtaperRearDelt = 1,
            movementPattern = "horizontal_push"
        )
        val lateralRaise = ExerciseEntity(
            name = "Lateral Raise",
            description = "Shoulder exercise",
            muscleGroup = "Shoulders",
            equipment = "dumbbell",
            difficulty = "Beginner",
            secondaryMuscles = "Traps",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "12-20",
            recommendedRestTime = "60",
            estimatedCalories = 8,
            category = "Resistance",
            tags = "isolation",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 1,
            vtaperLateralDelt = 10,
            vtaperUpperChest = 0,
            vtaperRearDelt = 2,
            movementPattern = "lateral_raise"
        )
        exerciseDao.insert(benchPress)
        exerciseDao.insert(lateralRaise)

        // Rebuild FTS index
        db.openHelper.writableDatabase.execSQL("INSERT INTO exercise_fts(exercise_fts) VALUES('rebuild')")

        // Search for "bench" - should find Bench Press
        val benchResults = repository.searchExercises("bench").first()
        assertTrue("Should find Bench Press", benchResults.any { it.name.contains("Bench", ignoreCase = true) })

        // Search for "lateral" - should find Lateral Raise
        val lateralResults = repository.searchExercises("lateral").first()
        assertTrue("Should find Lateral Raise", lateralResults.any { it.name.contains("Lateral", ignoreCase = true) })

        // Search for "press" - should find Bench Press
        val pressResults = repository.searchExercises("press").first()
        assertTrue("Should find Bench Press", pressResults.any { it.name.contains("Press", ignoreCase = true) })
    }

    @Test
    fun getFilteredExercises_filters_by_equipment() = runTest {
        val dumbbellEx = ExerciseEntity(
            name = "DB Press",
            description = "",
            muscleGroup = "Chest",
            equipment = "dumbbell",
            difficulty = "Beginner",
            secondaryMuscles = "",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 10,
            category = "Resistance",
            tags = "",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 0,
            vtaperLateralDelt = 2,
            vtaperUpperChest = 5,
            vtaperRearDelt = 1,
            movementPattern = "horizontal_push"
        )
        val barbellEx = ExerciseEntity(
            name = "BB Press",
            description = "",
            muscleGroup = "Chest",
            equipment = "barbell",
            difficulty = "Intermediate",
            secondaryMuscles = "",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 15,
            category = "Powerlifting",
            tags = "",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 0,
            vtaperLateralDelt = 2,
            vtaperUpperChest = 7,
            vtaperRearDelt = 1,
            movementPattern = "horizontal_push"
        )
        exerciseDao.insert(dumbbellEx)
        exerciseDao.insert(barbellEx)

        val dumbbellResults = repository.getFilteredExercises(null, null, "dumbbell").first()
        assertEquals("Should only return dumbbell exercises", 1, dumbbellResults.size)
        assertEquals("dumbbell", dumbbellResults[0].equipment)

        val barbellResults = repository.getFilteredExercises(null, null, "barbell").first()
        assertEquals("Should only return barbell exercises", 1, barbellResults.size)
        assertEquals("barbell", barbellResults[0].equipment)
    }

    @Test
    fun getFilteredExercises_filters_by_difficulty() = runTest {
        val beginnerEx = ExerciseEntity(
            name = "Push-up",
            description = "",
            muscleGroup = "Chest",
            equipment = "bodyweight",
            difficulty = "Beginner",
            secondaryMuscles = "",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "10-20",
            recommendedRestTime = "60",
            estimatedCalories = 10,
            category = "Bodyweight",
            tags = "",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 0,
            vtaperLateralDelt = 1,
            vtaperUpperChest = 5,
            vtaperRearDelt = 1,
            movementPattern = "horizontal_push"
        )
        val advancedEx = ExerciseEntity(
            name = "Archer Push-up",
            description = "",
            muscleGroup = "Chest",
            equipment = "bodyweight",
            difficulty = "Advanced",
            secondaryMuscles = "",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "6-10",
            recommendedRestTime = "60",
            estimatedCalories = 12,
            category = "Bodyweight",
            tags = "",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 0,
            vtaperLateralDelt = 1,
            vtaperUpperChest = 5,
            vtaperRearDelt = 0,
            movementPattern = "horizontal_push"
        )
        exerciseDao.insert(beginnerEx)
        exerciseDao.insert(advancedEx)

        val beginnerResults = repository.getFilteredExercises(null, "Beginner", null).first()
        assertTrue("Should only return beginner exercises", beginnerResults.all { it.difficulty == "Beginner" })

        val advancedResults = repository.getFilteredExercises(null, "Advanced", null).first()
        assertTrue("Should only return advanced exercises", advancedResults.all { it.difficulty == "Advanced" })
    }

    @Test
    fun getFilteredExercises_filters_by_muscle_group() = runTest {
        val chestEx = ExerciseEntity(
            name = "Bench Press",
            description = "",
            muscleGroup = "Chest",
            equipment = "barbell",
            difficulty = "Intermediate",
            secondaryMuscles = "",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 15,
            category = "Powerlifting",
            tags = "",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 0,
            vtaperLateralDelt = 2,
            vtaperUpperChest = 7,
            vtaperRearDelt = 1,
            movementPattern = "horizontal_push"
        )
        val backEx = ExerciseEntity(
            name = "Bent-over Row",
            description = "",
            muscleGroup = "Back",
            equipment = "barbell",
            difficulty = "Intermediate",
            secondaryMuscles = "",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 15,
            category = "Resistance",
            tags = "",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 9,
            vtaperLateralDelt = 0,
            vtaperUpperChest = 0,
            vtaperRearDelt = 5,
            movementPattern = "horizontal_pull"
        )
        exerciseDao.insert(chestEx)
        exerciseDao.insert(backEx)

        val chestResults = repository.getFilteredExercises("Chest", null, null).first()
        assertTrue("Should only return chest exercises", chestResults.all { it.muscleGroup == "Chest" })

        val backResults = repository.getFilteredExercises("Back", null, null).first()
        assertTrue("Should only return back exercises", backResults.all { it.muscleGroup == "Back" })
    }

    @Test
    fun getById_returns_correct_exercise() = runTest {
        val exercise = ExerciseEntity(
            name = "Test Exercise",
            description = "Test",
            muscleGroup = "Chest",
            equipment = "dumbbell",
            difficulty = "Beginner",
            secondaryMuscles = "",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 10,
            category = "Resistance",
            tags = "",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = 0,
            vtaperLateralDelt = 2,
            vtaperUpperChest = 5,
            vtaperRearDelt = 1,
            movementPattern = "horizontal_push"
        )
        val id = exerciseDao.insert(exercise)

        val result = repository.getExerciseById(id).first()
        assertNotNull("Should find exercise by ID", result)
        assertEquals("Test Exercise", result!!.name)
    }
}