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

    @Test
    fun `negative pausedRemainingSeconds is clamped to zero`() {
        val state = DurableTimerState(
            isRunning = true,
            isPaused = true,
            pausedRemainingSeconds = -15
        )
        assertEquals(0, state.calculateRemainingSeconds())
    }

    @Test
    fun `exact millisecond boundary transitions verify integer ceiling math`() {
        val now = 1_000_000L

        // 0 ms -> 0 seconds
        val state0 = DurableTimerState(isRunning = true, isPaused = false, restEndEpochMillis = now)
        assertEquals(0, state0.calculateRemainingSeconds(nowMillis = now))

        // 1 ms -> 1 second ceiling
        val state1 = DurableTimerState(isRunning = true, isPaused = false, restEndEpochMillis = now + 1L)
        assertEquals(1, state1.calculateRemainingSeconds(nowMillis = now))

        // 999 ms -> 1 second ceiling
        val state999 = DurableTimerState(isRunning = true, isPaused = false, restEndEpochMillis = now + 999L)
        assertEquals(1, state999.calculateRemainingSeconds(nowMillis = now))

        // 1000 ms -> exactly 1 second
        val state1000 = DurableTimerState(isRunning = true, isPaused = false, restEndEpochMillis = now + 1000L)
        assertEquals(1, state1000.calculateRemainingSeconds(nowMillis = now))

        // 1001 ms -> 2 seconds ceiling
        val state1001 = DurableTimerState(isRunning = true, isPaused = false, restEndEpochMillis = now + 1001L)
        assertEquals(2, state1001.calculateRemainingSeconds(nowMillis = now))

        // 59999 ms -> 60 seconds
        val state59999 = DurableTimerState(isRunning = true, isPaused = false, restEndEpochMillis = now + 59999L)
        assertEquals(60, state59999.calculateRemainingSeconds(nowMillis = now))

        // 60000 ms -> exactly 60 seconds
        val state60000 = DurableTimerState(isRunning = true, isPaused = false, restEndEpochMillis = now + 60000L)
        assertEquals(60, state60000.calculateRemainingSeconds(nowMillis = now))

        // 60001 ms -> 61 seconds ceiling
        val state60001 = DurableTimerState(isRunning = true, isPaused = false, restEndEpochMillis = now + 60001L)
        assertEquals(61, state60001.calculateRemainingSeconds(nowMillis = now))
    }

    @Test
    fun `long overdue wall-clock expiration returns 0 without underflow`() {
        val now = 100_000L
        val pastEnd = now - (10 * 3600 * 1000L) // 10 hours ago
        val state = DurableTimerState(
            isRunning = true,
            isPaused = false,
            restEndEpochMillis = pastEnd
        )
        assertEquals(0, state.calculateRemainingSeconds(nowMillis = now))
    }

    @Test
    fun `paused timer preserves duration indefinitely across 24 hour elapsed time`() {
        val state = DurableTimerState(
            isRunning = true,
            isPaused = true,
            pausedRemainingSeconds = 45,
            restEndEpochMillis = 50_000L
        )
        val dayLater = 50_000L + (24 * 3600 * 1000L)
        assertEquals(45, state.calculateRemainingSeconds(nowMillis = dayLater))
    }
}
