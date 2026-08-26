package com.gymcoach.app.core.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerManagerTest {

    private lateinit var restTimerManager: RestTimerManager

    @Before
    fun setup() {
        restTimerManager = RestTimerManager()
    }

    @Test
    fun `start sets initial state correctly`() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))

        restTimerManager.start(60, testScope)

        val state = restTimerManager.state.value
        assertEquals(60, state.timeRemaining)
        assertEquals(60, state.totalDuration)
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
    }

    @Test
    fun `start ticks down over time`() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))

        restTimerManager.start(60, testScope)

        advanceTimeBy(2500) // 2.5 seconds

        val state = restTimerManager.state.value
        assertEquals(58, state.timeRemaining)
        assertTrue(state.isRunning)
    }

    @Test
    fun `start completes when reaching zero`() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))

        restTimerManager.start(3, testScope)

        advanceTimeBy(3500) // 3.5 seconds

        val state = restTimerManager.state.value
        assertEquals(0, state.timeRemaining)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
    }
}
