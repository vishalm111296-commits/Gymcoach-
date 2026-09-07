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
    fun `RestPresets recommended returns expected values per set type and RPE`() {
        assertEquals(60, RestPresets.recommended(SetType.WARMUP, 8.0))
        assertEquals(30, RestPresets.recommended(SetType.DROP, 8.0))
        assertEquals(120, RestPresets.recommended(SetType.FAILURE, 8.0))

        assertEquals(180, RestPresets.recommended(SetType.NORMAL, 9.5))
        assertEquals(120, RestPresets.recommended(SetType.NORMAL, 8.0))
        assertEquals(90, RestPresets.recommended(SetType.NORMAL, 6.5))
        assertEquals(60, RestPresets.recommended(SetType.NORMAL, 5.0))
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
    fun `pause and resume update isPaused correctly`() = testScope.runTest {
        timerManager.start(60, this)
        timerManager.pause()
        assertTrue(timerManager.state.value.isPaused)

        timerManager.resume()
        assertFalse(timerManager.state.value.isPaused)
    }

    @Test
    fun `stop resets timer state`() = testScope.runTest {
        timerManager.start(60, this)
        timerManager.stop()
        val state = timerManager.state.value
        assertEquals(0, state.timeRemaining)
        assertFalse(state.isRunning)
    }
}
