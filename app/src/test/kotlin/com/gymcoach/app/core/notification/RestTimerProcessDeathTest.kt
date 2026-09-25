package com.gymcoach.app.core.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.core.timer.RestTimerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RestTimerProcessDeathTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RestTimerPreferences.clear(context)
    }

    @Test
    fun testDurableTimerStateCalculation() {
        val now = 1_000_000L

        // 1. Running and active
        val runningState = DurableTimerState(
            isRunning = true,
            isPaused = false,
            restEndEpochMillis = now + 45_000L,
            totalDurationSeconds = 60,
            pausedRemainingSeconds = 45
        )
        assertEquals(45, runningState.calculateRemainingSeconds(now))
        assertEquals(25, runningState.calculateRemainingSeconds(now + 20_000L))

        // 2. Running but expired
        assertEquals(0, runningState.calculateRemainingSeconds(now + 50_000L))

        // 3. Paused state (must retain pausedRemainingSeconds regardless of wall clock advance)
        val pausedState = DurableTimerState(
            isRunning = true,
            isPaused = true,
            restEndEpochMillis = now + 45_000L,
            totalDurationSeconds = 60,
            pausedRemainingSeconds = 30
        )
        assertEquals(30, pausedState.calculateRemainingSeconds(now))
        assertEquals(30, pausedState.calculateRemainingSeconds(now + 100_000L))

        // 4. Not running
        val stoppedState = DurableTimerState(isRunning = false)
        assertEquals(0, stoppedState.calculateRemainingSeconds(now))
    }

    @Test
    fun testPreferencesSaveLoadAndClear() {
        val now = System.currentTimeMillis()
        val original = DurableTimerState(
            isRunning = true,
            isPaused = false,
            restEndEpochMillis = now + 60_000L,
            totalDurationSeconds = 90,
            pausedRemainingSeconds = 60,
            nextSetLabel = "Squat Set 3",
            workoutId = 123L
        )

        RestTimerPreferences.save(context, original)
        val loaded = RestTimerPreferences.load(context)

        assertEquals(original.isRunning, loaded.isRunning)
        assertEquals(original.isPaused, loaded.isPaused)
        assertEquals(original.restEndEpochMillis, loaded.restEndEpochMillis)
        assertEquals(original.totalDurationSeconds, loaded.totalDurationSeconds)
        assertEquals(original.pausedRemainingSeconds, loaded.pausedRemainingSeconds)
        assertEquals(original.nextSetLabel, loaded.nextSetLabel)
        assertEquals(original.workoutId, loaded.workoutId)

        RestTimerPreferences.clear(context)
        val cleared = RestTimerPreferences.load(context)
        assertFalse(cleared.isRunning)
        assertEquals(0, cleared.totalDurationSeconds)
    }

    @Test
    fun testRestTimerManagerRestoresStateAfterProcessDeath() = runTest {
        val now = System.currentTimeMillis()
        val activeState = DurableTimerState(
            isRunning = true,
            isPaused = false,
            restEndEpochMillis = now + 60_000L,
            totalDurationSeconds = 90,
            pausedRemainingSeconds = 60,
            nextSetLabel = "Bench Press Set 2",
            workoutId = 77L
        )
        RestTimerPreferences.save(context, activeState)

        // Simulate app launch / ViewModel creation in new process
        val manager = RestTimerManager(context)

        val state = manager.state.value
        assertTrue("Restored timer should be running", state.isRunning)
        assertFalse("Restored timer should not be paused", state.isPaused)
        assertEquals("Total duration should match saved duration", 90, state.totalDuration)
        assertTrue("Remaining seconds should be between 58 and 60", state.timeRemaining in 58..60)
    }

    @Test
    fun testRestTimerManagerClearsExpiredTimerOnProcessRelaunch() = runTest {
        val now = System.currentTimeMillis()
        val expiredState = DurableTimerState(
            isRunning = true,
            isPaused = false,
            restEndEpochMillis = now - 10_000L, // expired 10s ago
            totalDurationSeconds = 60,
            pausedRemainingSeconds = 0,
            nextSetLabel = "Overhead Press Set 1",
            workoutId = 88L
        )
        RestTimerPreferences.save(context, expiredState)

        val manager = RestTimerManager(context)

        val state = manager.state.value
        assertFalse("Expired timer should not be running", state.isRunning)
        assertEquals(0, state.timeRemaining)

        val durable = RestTimerPreferences.load(context)
        assertFalse("Preferences should be cleared for expired timer", durable.isRunning)
    }

    @Test
    fun testRestTimerManagerRestoresPausedTimerAfterProcessDeath() = runTest {
        val now = System.currentTimeMillis()
        val pausedState = DurableTimerState(
            isRunning = true,
            isPaused = true,
            restEndEpochMillis = now + 40_000L,
            totalDurationSeconds = 90,
            pausedRemainingSeconds = 45,
            nextSetLabel = "Incline Dumbbell Press Set 3",
            workoutId = 111L
        )
        RestTimerPreferences.save(context, pausedState)

        val manager = RestTimerManager(context)

        val state = manager.state.value
        assertTrue("Restored timer should be running", state.isRunning)
        assertTrue("Restored timer should be paused", state.isPaused)
        assertEquals("Total duration should match", 90, state.totalDuration)
        assertEquals("Paused remaining seconds should be preserved exactly", 45, state.timeRemaining)
    }

    @Test
    fun testRestTimerManagerStopClearsPreferences() = runTest {
        val now = System.currentTimeMillis()
        val activeState = DurableTimerState(
            isRunning = true,
            isPaused = false,
            restEndEpochMillis = now + 60_000L,
            totalDurationSeconds = 60,
            pausedRemainingSeconds = 60,
            nextSetLabel = "Pullups Set 1",
            workoutId = 222L
        )
        RestTimerPreferences.save(context, activeState)

        val manager = RestTimerManager(context)
        assertTrue(manager.state.value.isRunning)

        manager.stop()

        assertFalse("Manager should be stopped", manager.state.value.isRunning)
        val loaded = RestTimerPreferences.load(context)
        assertFalse("Preferences should be inactive after manager stop", loaded.isRunning)
    }
}
