package com.gymcoach.app.core.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for RestTimerManager and RestPresets.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerManagerTest {

    private lateinit var manager: RestTimerManager
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        manager = RestTimerManager()
    }

    @Test
    fun `start sets timer to running state`() {
        manager.start(90, testScope)
        val state = manager.state.value
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(90, state.timeRemaining)
        assertEquals(90, state.totalDuration)
    }

    @Test
    fun `pause sets isPaused to true`() {
        manager.start(90, testScope)
        manager.pause()
        assertTrue(manager.state.value.isPaused)
    }

    @Test
    fun `resume sets isPaused to false`() {
        manager.start(90, testScope)
        manager.pause()
        manager.resume()
        assertFalse(manager.state.value.isPaused)
    }

    @Test
    fun `stop clears all state`() {
        manager.start(90, testScope)
        manager.stop()
        val state = manager.state.value
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(0, state.timeRemaining)
        assertEquals(0, state.totalDuration)
    }

    @Test
    fun `restart replaces the running timer`() {
        manager.start(90, testScope)
        manager.restart(60, testScope)
        val state = manager.state.value
        assertEquals(60, state.timeRemaining)
        assertEquals(60, state.totalDuration)
        assertTrue(state.isRunning)
    }

    @Test
    fun `tick decrements timeRemaining`() = runTest {
        manager.start(5, this)
        advanceTimeBy(3001L); runCurrent() // 3 seconds
        assertEquals(2, manager.state.value.timeRemaining)
    }

    @Test
    fun `timer completes when timeRemaining reaches 0`() = runTest {
        manager.start(2, this)
        advanceTimeBy(2001L); runCurrent() // 2 seconds
        assertFalse(manager.state.value.isRunning)
        assertEquals(0, manager.state.value.timeRemaining)
    }

    @Test
    fun `skip stops the timer`() {
        manager.start(90, testScope)
        manager.skip()
        assertFalse(manager.state.value.isRunning)
    }
}

/**
 * Tests for RestPresets recommended rest time logic.
 */
class RestPresetsTest {

    @Test
    fun `warmup sets get 60s rest`() {
        val rest = RestPresets.recommended(
            com.gymcoach.app.domain.model.SetType.WARMUP,
            rpe = 5.0,
            setsCompleted = 1
        )
        assertEquals(60, rest)
    }

    @Test
    fun `drop sets get 30s rest`() {
        val rest = RestPresets.recommended(
            com.gymcoach.app.domain.model.SetType.DROP,
            rpe = 8.0,
            setsCompleted = 3
        )
        assertEquals(30, rest)
    }

    @Test
    fun `failure sets get 120s rest`() {
        val rest = RestPresets.recommended(
            com.gymcoach.app.domain.model.SetType.FAILURE,
            rpe = 10.0,
            setsCompleted = 4
        )
        assertEquals(120, rest)
    }

    @Test
    fun `normal set with RPE 9 gets 180s rest`() {
        val rest = RestPresets.recommended(
            com.gymcoach.app.domain.model.SetType.NORMAL,
            rpe = 9.0,
            setsCompleted = 3
        )
        assertEquals(180, rest)
    }

    @Test
    fun `normal set with RPE 7_5 gets 120s rest`() {
        val rest = RestPresets.recommended(
            com.gymcoach.app.domain.model.SetType.NORMAL,
            rpe = 7.5,
            setsCompleted = 2
        )
        assertEquals(120, rest)
    }

    @Test
    fun `normal set with RPE 6 gets 90s rest`() {
        val rest = RestPresets.recommended(
            com.gymcoach.app.domain.model.SetType.NORMAL,
            rpe = 6.0,
            setsCompleted = 2
        )
        assertEquals(90, rest)
    }

    @Test
    fun `normal set with low RPE gets 60s rest`() {
        val rest = RestPresets.recommended(
            com.gymcoach.app.domain.model.SetType.NORMAL,
            rpe = 4.0,
            setsCompleted = 1
        )
        assertEquals(60, rest)
    }

    @Test
    fun `preset constants are correct`() {
        assertEquals(30, RestPresets.SHORT)
        assertEquals(60, RestPresets.MEDIUM)
        assertEquals(90, RestPresets.STANDARD)
        assertEquals(120, RestPresets.LONG)
        assertEquals(180, RestPresets.VERY_LONG)
    }
}
