package com.gymcoach.app.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DurableTimerStateTest {

    @Test
    fun `calculateRemainingSeconds returns zero when not running`() {
        val state = DurableTimerState(
            isRunning = false,
            restEndEpochMillis = 50000L,
            pausedRemainingSeconds = 30
        )
        assertEquals(0, state.calculateRemainingSeconds(nowMillis = 10000L))
    }

    @Test
    fun `calculateRemainingSeconds returns paused remaining when paused`() {
        val state = DurableTimerState(
            isRunning = true,
            isPaused = true,
            restEndEpochMillis = 50000L,
            pausedRemainingSeconds = 45
        )
        // Regardless of current wall-clock time, paused timer holds its remaining duration
        assertEquals(45, state.calculateRemainingSeconds(nowMillis = 10000L))
        assertEquals(45, state.calculateRemainingSeconds(nowMillis = 99999L))
    }

    @Test
    fun `calculateRemainingSeconds accurately computes wall-clock remaining seconds with ceiling`() {
        val now = 100_000L
        val end = now + 45_200L // 45.2 seconds remaining -> 46 seconds ceiling
        val state = DurableTimerState(
            isRunning = true,
            isPaused = false,
            restEndEpochMillis = end,
            totalDurationSeconds = 60
        )

        assertEquals(46, state.calculateRemainingSeconds(nowMillis = now))
        assertEquals(30, state.calculateRemainingSeconds(nowMillis = now + 15_201L))
        assertEquals(1, state.calculateRemainingSeconds(nowMillis = end - 100L))
        assertEquals(0, state.calculateRemainingSeconds(nowMillis = end))
        assertEquals(0, state.calculateRemainingSeconds(nowMillis = end + 5000L))
    }

    @Test
    fun `default values instantiate safe inactive timer`() {
        val state = DurableTimerState()
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(0L, state.restEndEpochMillis)
        assertEquals(0, state.totalDurationSeconds)
        assertEquals(0, state.pausedRemainingSeconds)
        assertEquals("", state.nextSetLabel)
        assertEquals(-1L, state.workoutId)
        assertEquals(0, state.calculateRemainingSeconds())
    }
}
