package com.gymcoach.app.core.timer

import com.gymcoach.app.domain.model.SetType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerManagerTest {

    private lateinit var timerManager: RestTimerManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        timerManager = RestTimerManager()
    }

    @Test
    fun `RestPresets recommended boundary cases and preset values`() {
        assertEquals(30, RestPresets.SHORT)
        assertEquals(60, RestPresets.MEDIUM)
        assertEquals(90, RestPresets.STANDARD)
        assertEquals(120, RestPresets.LONG)
        assertEquals(180, RestPresets.VERY_LONG)

        // SetType WARMUP, DROP, FAILURE
        assertEquals(60, RestPresets.recommended(SetType.WARMUP, 9.9))
        assertEquals(30, RestPresets.recommended(SetType.DROP, 9.9))
        assertEquals(120, RestPresets.recommended(SetType.FAILURE, 5.0))

        // SetType NORMAL boundaries
        assertEquals(180, RestPresets.recommended(SetType.NORMAL, 9.0))
        assertEquals(120, RestPresets.recommended(SetType.NORMAL, 8.9))
        assertEquals(120, RestPresets.recommended(SetType.NORMAL, 7.5))
        assertEquals(90, RestPresets.recommended(SetType.NORMAL, 7.4))
        assertEquals(90, RestPresets.recommended(SetType.NORMAL, 6.0))
        assertEquals(60, RestPresets.recommended(SetType.NORMAL, 5.9))
        assertEquals(60, RestPresets.recommended(SetType.NORMAL, 0.0))
    }

    @Test
    fun `start initializes timer state`() = testScope.runTest {
        timerManager.start(90, this)
        val state = timerManager.state.value
        assertEquals(90, state.timeRemaining)
        assertEquals(90, state.totalDuration)
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
    }

    @Test
    fun `restart cancels previous tick and sets new duration`() = testScope.runTest {
        timerManager.start(45, this)
        assertEquals(45, timerManager.state.value.timeRemaining)

        timerManager.restart(120, this)
        val state = timerManager.state.value
        assertEquals(120, state.timeRemaining)
        assertEquals(120, state.totalDuration)
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
    }

    @Test
    fun `skip resets timer state to idle identical to stop`() = testScope.runTest {
        timerManager.start(90, this)
        assertTrue(timerManager.state.value.isRunning)

        timerManager.skip()
        val state = timerManager.state.value
        assertEquals(0, state.timeRemaining)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(0, state.totalDuration)
    }

    @Test
    fun `pause and resume update isPaused correctly`() = testScope.runTest {
        timerManager.start(60, this)
        timerManager.pause()
        assertTrue(timerManager.state.value.isPaused)

        timerManager.resume()
        assertFalse(timerManager.state.value.isPaused)
    }

    @Test
    fun `pause and resume are no-op when timer is not running`() = testScope.runTest {
        timerManager.pause()
        assertFalse(timerManager.state.value.isPaused)
        assertFalse(timerManager.state.value.isRunning)

        timerManager.resume()
        assertFalse(timerManager.state.value.isPaused)
    }

    @Test
    fun `resume is no-op when timer is running but not paused`() = testScope.runTest {
        timerManager.start(60, this)
        timerManager.resume()
        assertFalse(timerManager.state.value.isPaused)
        assertTrue(timerManager.state.value.isRunning)
    }

    @Test
    fun `stop resets timer state and is idempotent`() = testScope.runTest {
        timerManager.start(60, this)
        timerManager.stop()
        val state = timerManager.state.value
        assertEquals(0, state.timeRemaining)
        assertFalse(state.isRunning)

        // Second stop should not throw
        timerManager.stop()
        assertEquals(0, timerManager.state.value.timeRemaining)
    }

    @Test
    fun `adjust updates remaining time and total duration`() = testScope.runTest {
        timerManager.start(45, this)
        timerManager.adjust(15)
        assertEquals(60, timerManager.state.value.timeRemaining)
        assertEquals(60, timerManager.state.value.totalDuration)

        timerManager.adjust(-20)
        assertEquals(40, timerManager.state.value.timeRemaining)
        assertEquals(60, timerManager.state.value.totalDuration)
    }

    @Test
    fun `adjust to zero stops the timer`() = testScope.runTest {
        timerManager.start(30, this)
        timerManager.adjust(-30)
        assertEquals(0, timerManager.state.value.timeRemaining)
        assertFalse(timerManager.state.value.isRunning)
    }

    @Test
    fun `adjust with negative delta greater than remaining stops timer and resets`() = testScope.runTest {
        timerManager.start(15, this)
        timerManager.adjust(-45)
        assertEquals(0, timerManager.state.value.timeRemaining)
        assertFalse(timerManager.state.value.isRunning)
    }

    @Test
    fun `ticking advances countdown and terminates at zero`() = runTest {
        val manager = RestTimerManager()
        manager.start(3, this)
        assertEquals(3, manager.state.value.timeRemaining)
        assertTrue(manager.state.value.isRunning)

        // Advance 1.5s to cross first 1s delay boundary
        testScheduler.advanceTimeBy(1500L)
        testScheduler.runCurrent()
        assertEquals(2, manager.state.value.timeRemaining)

        // Advance another 1s to cross second delay boundary
        testScheduler.advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(1, manager.state.value.timeRemaining)

        // Advance another 1s to cross final delay boundary
        testScheduler.advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(0, manager.state.value.timeRemaining)
        assertFalse(manager.state.value.isRunning)
    }

    @Test
    fun `ticking halts during pause and resumes on resume`() = runTest {
        val manager = RestTimerManager()
        manager.start(5, this)

        // Advance 1.5s to tick first second
        testScheduler.advanceTimeBy(1500L)
        testScheduler.runCurrent()
        assertEquals(4, manager.state.value.timeRemaining)

        manager.pause()
        testScheduler.advanceTimeBy(3000L)
        testScheduler.runCurrent()
        // Should remain at 4 while paused
        assertEquals(4, manager.state.value.timeRemaining)

        manager.resume()
        testScheduler.advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(3, manager.state.value.timeRemaining)
    }
}
