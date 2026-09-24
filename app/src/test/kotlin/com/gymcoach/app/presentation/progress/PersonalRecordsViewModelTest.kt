package com.gymcoach.app.presentation.progress

import com.gymcoach.app.data.local.dao.PersonalRecordDao
import com.gymcoach.app.data.local.entity.PersonalRecordWithExercise
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalRecordsViewModelTest {

    private val personalRecordDao = mockk<PersonalRecordDao>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadRecords updates state to Success with sorted records`() = runTest(testDispatcher) {
        val records = listOf(
            PersonalRecordWithExercise(
                id = 1L,
                exerciseId = 101L,
                userId = 1L,
                weightKg = 100.0,
                reps = 5,
                oneRepMaxKg = 116.67,
                achievedAt = 1000L,
                notes = "",
                exerciseName = "Bench Press"
            ),
            PersonalRecordWithExercise(
                id = 2L,
                exerciseId = 102L,
                userId = 1L,
                weightKg = 140.0,
                reps = 3,
                oneRepMaxKg = 154.0,
                achievedAt = 2000L,
                notes = "",
                exerciseName = "Squat"
            )
        )

        every { personalRecordDao.getAllWithExerciseName() } returns flowOf(records)

        val viewModel = PersonalRecordsViewModel(personalRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PersonalRecordsUiState.Success)
        val success = state as PersonalRecordsUiState.Success
        assertEquals(2, success.records.size)
        // Default sort is RECENT -> Squat (achievedAt=2000) first
        assertEquals("Squat", success.records[0].exerciseName)
    }

    @Test
    fun `loadRecords updates state to Empty when no records exist`() = runTest(testDispatcher) {
        every { personalRecordDao.getAllWithExerciseName() } returns flowOf(emptyList())

        val viewModel = PersonalRecordsViewModel(personalRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PersonalRecordsUiState.Empty)
    }

    @Test
    fun `setSortBy sorts by WEIGHT correctly`() = runTest(testDispatcher) {
        val records = listOf(
            PersonalRecordWithExercise(
                id = 1L,
                exerciseId = 101L,
                userId = 1L,
                weightKg = 100.0,
                reps = 5,
                oneRepMaxKg = 116.67,
                achievedAt = 2000L,
                notes = "",
                exerciseName = "Bench Press"
            ),
            PersonalRecordWithExercise(
                id = 2L,
                exerciseId = 102L,
                userId = 1L,
                weightKg = 140.0,
                reps = 3,
                oneRepMaxKg = 154.0,
                achievedAt = 1000L,
                notes = "",
                exerciseName = "Squat"
            )
        )

        every { personalRecordDao.getAllWithExerciseName() } returns flowOf(records)

        val viewModel = PersonalRecordsViewModel(personalRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSortBy(SortBy.WEIGHT)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PersonalRecordsUiState.Success)
        val success = state as PersonalRecordsUiState.Success
        assertEquals(140.0, success.records[0].weightKg, 0.01)
    }

    @Test
    fun `setSortBy sorts by EXERCISE_NAME alphabetically ascending`() = runTest(testDispatcher) {
        val records = listOf(
            PersonalRecordWithExercise(
                id = 1L,
                exerciseId = 101L,
                userId = 1L,
                weightKg = 140.0,
                reps = 3,
                oneRepMaxKg = 154.0,
                achievedAt = 1000L,
                notes = "",
                exerciseName = "Squat"
            ),
            PersonalRecordWithExercise(
                id = 2L,
                exerciseId = 102L,
                userId = 1L,
                weightKg = 100.0,
                reps = 5,
                oneRepMaxKg = 116.67,
                achievedAt = 2000L,
                notes = "",
                exerciseName = "Bench Press"
            )
        )

        every { personalRecordDao.getAllWithExerciseName() } returns flowOf(records)

        val viewModel = PersonalRecordsViewModel(personalRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSortBy(SortBy.EXERCISE_NAME)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PersonalRecordsUiState.Success)
        val success = state as PersonalRecordsUiState.Success
        assertEquals("Bench Press", success.records[0].exerciseName)
        assertEquals("Squat", success.records[1].exerciseName)
    }

    @Test
    fun `database error transitions state to Error`() = runTest(testDispatcher) {
        every { personalRecordDao.getAllWithExerciseName() } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("Database read failure")
        }

        val viewModel = PersonalRecordsViewModel(personalRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PersonalRecordsUiState.Error)
        assertEquals("Database read failure", (state as PersonalRecordsUiState.Error).message)
    }

    @Test
    fun `sortBy stateFlow reflects current SortBy enum`() = runTest(testDispatcher) {
        every { personalRecordDao.getAllWithExerciseName() } returns flowOf(emptyList())

        val viewModel = PersonalRecordsViewModel(personalRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SortBy.RECENT, viewModel.sortBy.value)

        viewModel.setSortBy(SortBy.WEIGHT)
        assertEquals(SortBy.WEIGHT, viewModel.sortBy.value)

        viewModel.setSortBy(SortBy.EXERCISE_NAME)
        assertEquals(SortBy.EXERCISE_NAME, viewModel.sortBy.value)
    }
}
