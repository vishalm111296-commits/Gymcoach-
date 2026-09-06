package com.gymcoach.app.core.timer

import com.gymcoach.app.domain.model.SetType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerManagerTest {

    @Test
    fun `recommended rest time for WARMUP set is 60 seconds`() {
        val result = RestPresets.recommended(SetType.WARMUP, rpe = 8.0)
        assertEquals(60, result)
    }

    @Test
    fun `recommended rest time for DROP set is 30 seconds`() {
        val result = RestPresets.recommended(SetType.DROP, rpe = 10.0)
        assertEquals(30, result)
    }

    @Test
    fun `recommended rest time for FAILURE set is 120 seconds`() {
        val result = RestPresets.recommended(SetType.FAILURE, rpe = 10.0)
        assertEquals(120, result)
    }

    @Test
    fun `recommended rest time for NORMAL set with RPE 9_0 or higher is 180 seconds`() {
        assertEquals(180, RestPresets.recommended(SetType.NORMAL, rpe = 9.0))
        assertEquals(180, RestPresets.recommended(SetType.NORMAL, rpe = 9.5))
        assertEquals(180, RestPresets.recommended(SetType.NORMAL, rpe = 10.0))
    }

    @Test
    fun `recommended rest time for NORMAL set with RPE between 7_5 and 9_0 is 120 seconds`() {
        assertEquals(120, RestPresets.recommended(SetType.NORMAL, rpe = 7.5))
        assertEquals(120, RestPresets.recommended(SetType.NORMAL, rpe = 8.0))
        assertEquals(120, RestPresets.recommended(SetType.NORMAL, rpe = 8.9))
    }

    @Test
    fun `recommended rest time for NORMAL set with RPE between 6_0 and 7_5 is 90 seconds`() {
        assertEquals(90, RestPresets.recommended(SetType.NORMAL, rpe = 6.0))
        assertEquals(90, RestPresets.recommended(SetType.NORMAL, rpe = 6.5))
        assertEquals(90, RestPresets.recommended(SetType.NORMAL, rpe = 7.4))
    }

    @Test
    fun `recommended rest time for NORMAL set with RPE below 6_0 is 60 seconds`() {
        assertEquals(60, RestPresets.recommended(SetType.NORMAL, rpe = 5.9))
        assertEquals(60, RestPresets.recommended(SetType.NORMAL, rpe = 5.0))
        assertEquals(60, RestPresets.recommended(SetType.NORMAL, rpe = 0.0))
    }

    @Test
    fun `RestTimerManager initial state is default`() {
        val manager = RestTimerManager()
        val state = manager.state.value

        assertEquals(0, state.timeRemaining)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(0, state.totalDuration)
    }

    @Test
    fun `RestTimerManager start initializes state and starts ticking`() = runTest {
        val manager = RestTimerManager()
        manager.start(90, backgroundScope)

        val initialState = manager.state.value
        assertEquals(90, initialState.timeRemaining)
        assertTrue(initialState.isRunning)
        assertFalse(initialState.isPaused)
        assertEquals(90, initialState.totalDuration)

        // Advance 1 second and run pending tasks
        advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(89, manager.state.value.timeRemaining)
        assertTrue(manager.state.value.isRunning)

        // Advance remaining time
        advanceTimeBy(89000L)
        testScheduler.runCurrent()
        assertEquals(0, manager.state.value.timeRemaining)
        assertFalse(manager.state.value.isRunning)
        assertFalse(manager.state.value.isPaused)
    }

    @Test
    fun `RestTimerManager pause and resume control timer ticks`() = runTest {
        val manager = RestTimerManager()
        manager.start(60, backgroundScope)

        advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(59, manager.state.value.timeRemaining)

        manager.pause()
        assertTrue(manager.state.value.isPaused)

        // Advancing time while paused should not reduce timeRemaining
        advanceTimeBy(2000L)
        testScheduler.runCurrent()
        assertEquals(59, manager.state.value.timeRemaining)

        manager.resume()
        assertFalse(manager.state.value.isPaused)

        advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(58, manager.state.value.timeRemaining)
    }

    @Test
    fun `RestTimerManager restart resets timer with new duration`() = runTest {
        val manager = RestTimerManager()
        manager.start(60, backgroundScope)

        advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(59, manager.state.value.timeRemaining)

        manager.restart(120, backgroundScope)
        val newState = manager.state.value
        assertEquals(120, newState.timeRemaining)
        assertEquals(120, newState.totalDuration)
        assertTrue(newState.isRunning)

        advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(119, manager.state.value.timeRemaining)
    }

    @Test
    fun `RestTimerManager stop or skip resets timer state`() = runTest {
        val manager = RestTimerManager()
        manager.start(60, backgroundScope)

        advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(59, manager.state.value.timeRemaining)

        manager.skip()
        val stateAfterSkip = manager.state.value
        assertEquals(0, stateAfterSkip.timeRemaining)
        assertFalse(stateAfterSkip.isRunning)
        assertFalse(stateAfterSkip.isPaused)
        assertEquals(0, stateAfterSkip.totalDuration)
    }
}
