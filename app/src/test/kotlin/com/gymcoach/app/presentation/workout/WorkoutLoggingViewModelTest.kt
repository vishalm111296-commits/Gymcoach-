package com.gymcoach.app.presentation.workout

import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutLoggingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var vm: WorkoutLoggingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        // The elapsed-timer runs a perpetual while(true){ delay(1000) } loop; cancel the VM's
        // scope so runTest teardown terminates instead of spinning on the still-scheduled tick.
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `creating a new workout starts the elapsed timer which ticks with the clock`() =
        runTest(dispatcher.scheduler) {
            val clock = FakeClock(1_000L)
            val repo = FakeWorkoutRepository()
            vm = WorkoutLoggingViewModel(repo, FakeExerciseRepository(), RestTimerManager())
            vm.nowProvider = clock::now

            vm.startNewWorkout()
            runCurrent() // create workout, first timer tick

            assertEquals(0L, vm.elapsedSeconds.value) // now == startTime
            clock.advanceSeconds(2)
            advanceTimeBy(1_000) // advance virtual time to the next tick
            runCurrent() // advanceTimeBy does not run tasks scheduled at exactly the target time
            assertEquals(2L, vm.elapsedSeconds.value)

            // LAST statement: cancel the VM scope so runTest teardown (advanceUntilIdle)
            // sees an idle scheduler instead of the perpetual while(true){ delay(1000) } loop.
            vm.viewModelScope.cancel()
        }

    @Test
    fun `completing a workout marks it completed with the elapsed duration`() =
        runTest(dispatcher.scheduler) {
            val clock = FakeClock(1_000L)
            val repo = FakeWorkoutRepository()
            vm = WorkoutLoggingViewModel(repo, FakeExerciseRepository(), RestTimerManager())
            vm.nowProvider = clock::now

            vm.startNewWorkout()
            runCurrent()

            clock.advanceSeconds(90)
            vm.completeWorkout()
            advanceUntilIdle()

            assertEquals(true, vm.completed.value)
            val stored = repo.savedWorkout
            assertEquals(true, stored?.completed)
            assertEquals(90L, stored?.duration)
            // W2 trigger signal: completedWorkoutId becomes the completed workout's id,
            // which the screen's LaunchedEffect(key = completedWorkoutId.value) reacts to.
            assertEquals(stored?.id, vm.completedWorkoutId.value)

            vm.viewModelScope.cancel()
        }

    @Test
    fun `completed workout timer is frozen and does not keep ticking`() =
        runTest(dispatcher.scheduler) {
            val clock = FakeClock(1_000L)
            val repo = FakeWorkoutRepository()
            vm = WorkoutLoggingViewModel(repo, FakeExerciseRepository(), RestTimerManager())
            vm.nowProvider = clock::now

            vm.startNewWorkout()
            runCurrent()

            clock.advanceSeconds(90)
            vm.completeWorkout()
            advanceUntilIdle() // updateWorkout propagates; the completion guard stops the timer

            clock.advanceSeconds(30)
            advanceTimeBy(1_000) // a stray tick would set elapsed to 120
            assertEquals(90L, vm.elapsedSeconds.value) // frozen at completion

            vm.viewModelScope.cancel()
        }

    private class FakeClock(initialEpochSeconds: Long) {
        private var seconds = initialEpochSeconds
        fun now(): Instant = Instant.ofEpochSecond(seconds)
        fun advanceSeconds(delta: Long) {
            seconds += delta
        }
    }

    private class FakeExerciseRepository : ExerciseRepository {
        override fun getAllExercises(): Flow<List<Exercise>> = flow { emit(emptyList()) }
        override fun getFilteredExercises(muscle: String?, difficulty: String?, equipment: String?): Flow<List<Exercise>> =
            flow { emit(emptyList()) }
        override fun getExerciseById(id: Long): Flow<Exercise?> = flow { emit(null) }
        override suspend fun addExercise(exercise: Exercise) = Unit
        override suspend fun updateExercise(exercise: Exercise) = Unit
        override suspend fun deleteExercise(exercise: Exercise) = Unit
    }

    private class FakeWorkoutRepository : WorkoutRepository {
        var savedWorkout: Workout? = null
        private val workoutFlow = MutableStateFlow<WorkoutWithDetails?>(null)
        private val workouts = mutableListOf<Workout>()
        private var nextId = 1L

        override suspend fun createWorkout(workout: Workout): Long {
            val id = nextId++
            savedWorkout = workout.copy(id = id)
            workouts.add(savedWorkout!!)
            workoutFlow.value = WorkoutWithDetails(savedWorkout!!, emptyList())
            return id
        }

        override fun getWorkoutWithDetails(workoutId: Long): Flow<WorkoutWithDetails?> = workoutFlow

        override suspend fun updateWorkout(workout: Workout) {
            val idx = workouts.indexOfFirst { it.id == workout.id }
            if (idx >= 0) workouts[idx] = workout
            savedWorkout = workout
            workoutFlow.value = WorkoutWithDetails(workout, emptyList())
        }

        override suspend fun getLatestIncompleteWorkout(): Workout? = workouts.lastOrNull { !it.completed }
        override suspend fun getIncompleteWorkout(): Workout? = getLatestIncompleteWorkout()

        override fun getAllWorkouts(): Flow<List<Workout>> = flow { emit(emptyList()) }
        override fun getAllWorkoutTemplates(): Flow<List<Workout>> = flow { emit(emptyList()) }
        override suspend fun getAllWorkoutTemplatesNow(): List<Workout> = emptyList()
        override fun getAllWorkoutsWithDetails(): Flow<List<WorkoutWithDetails>> = flow { emit(emptyList()) }
        override suspend fun deleteWorkout(workoutId: Long) = Unit
        override suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, orderIndex: Int): Long = 0
        override suspend fun removeExerciseFromWorkout(workoutExerciseId: Long) = Unit
        override suspend fun addSetToExercise(workoutExerciseId: Long, set: WorkoutSet): Long = 0
        override suspend fun updateSet(set: WorkoutSet) = Unit
        override suspend fun deleteSet(setId: Long) = Unit
        override fun getCompletedWorkouts(): Flow<List<WorkoutWithStats>> = flow { emit(emptyList()) }
        override fun getWorkoutsInDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutWithStats>> =
            flow { emit(emptyList()) }
        override fun getWorkoutsByVolumeDesc(): Flow<List<WorkoutWithStats>> = flow { emit(emptyList()) }
        override fun getWorkoutsByVolumeAsc(): Flow<List<WorkoutWithStats>> = flow { emit(emptyList()) }
        override fun getWorkoutsByDurationDesc(): Flow<List<WorkoutWithStats>> = flow { emit(emptyList()) }
        override fun getWorkoutsByDurationAsc(): Flow<List<WorkoutWithStats>> = flow { emit(emptyList()) }
        override suspend fun searchWorkouts(query: String): List<WorkoutWithStats> = emptyList()
        override suspend fun getLatestSetForExercise(exerciseId: Long): WorkoutSet? = null
        override suspend fun getBestVolumeSetForExercise(exerciseId: Long): WorkoutSet? = null
        override suspend fun getWorkoutWithDetailsNow(workoutId: Long): WorkoutWithDetails? = null
    }
}
