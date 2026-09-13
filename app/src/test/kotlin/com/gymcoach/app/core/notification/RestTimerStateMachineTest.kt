package com.gymcoach.app.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RestTimerStateMachineTest {

    private lateinit var stateMachine: RestTimerStateMachine
    private var stateChangedCount = 0
    private var completedCount = 0
    private var cancelledCount = 0

    @Before
    fun setUp() {
        stateChangedCount = 0
        completedCount = 0
        cancelledCount = 0
        stateMachine = RestTimerStateMachine(
            onStateChanged = { stateChangedCount++ },
            onComplete = { completedCount++ },
            onCancel = { cancelledCount++ }
        )
    }

    @Test
    fun testInitialState() {
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isPaused.value)
        assertFalse(stateMachine.isRunning.value)
        assertEquals("", stateMachine.nextSetLabel)
        assertEquals(0, stateChangedCount)
    }

    @Test
    fun testStartTimerValidDuration() {
        stateMachine.start(90, "Bench Press Set 2")
        assertEquals(90, stateMachine.remainingSeconds.value)
        assertTrue(stateMachine.isRunning.value)
        assertFalse(stateMachine.isPaused.value)
        assertEquals("Bench Press Set 2", stateMachine.nextSetLabel)
        assertEquals(1, stateChangedCount)
    }

    @Test
    fun testStartTimerZeroOrNegativeDuration() {
        stateMachine.start(0)
        assertFalse(stateMachine.isRunning.value)
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertEquals(1, cancelledCount)

        cancelledCount = 0
        stateMachine.start(-10)
        assertFalse(stateMachine.isRunning.value)
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertEquals(1, cancelledCount)
    }

    @Test
    fun testTickDecrementsTime() {
        stateMachine.start(60)
        stateMachine.tick(59)
        assertEquals(59, stateMachine.remainingSeconds.value)
        assertTrue(stateMachine.isRunning.value)
        assertFalse(stateMachine.isPaused.value)

        stateMachine.tick(58)
        assertEquals(58, stateMachine.remainingSeconds.value)
    }

    @Test
    fun testTickToZeroCompletesTimer() {
        stateMachine.start(2)
        stateMachine.tick(1)
        assertEquals(1, stateMachine.remainingSeconds.value)
        assertEquals(0, completedCount)

        stateMachine.tick(0)
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isRunning.value)
        assertFalse(stateMachine.isPaused.value)
        assertEquals(1, completedCount)
    }

    @Test
    fun testTickIgnoredWhenPausedOrNotRunning() {
        // Not running
        stateMachine.tick(30)
        assertEquals(0, stateMachine.remainingSeconds.value)

        // Paused
        stateMachine.start(45)
        stateMachine.pause()
        assertEquals(45, stateMachine.remainingSeconds.value)
        stateMachine.tick(40)
        assertEquals(45, stateMachine.remainingSeconds.value)
    }

    @Test
    fun testPauseAndResume() {
        stateMachine.start(90)
        stateMachine.pause()
        assertTrue(stateMachine.isPaused.value)
        assertTrue(stateMachine.isRunning.value)
        assertEquals(90, stateMachine.remainingSeconds.value)

        stateMachine.resume()
        assertFalse(stateMachine.isPaused.value)
        assertTrue(stateMachine.isRunning.value)
        assertEquals(90, stateMachine.remainingSeconds.value)
    }

    @Test
    fun testPauseWhenNotRunningIsNoOp() {
        stateMachine.pause()
        assertFalse(stateMachine.isPaused.value)
        assertFalse(stateMachine.isRunning.value)
    }

    @Test
    fun testPauseWhenAlreadyPausedIsNoOp() {
        stateMachine.start(90)
        stateMachine.pause()
        val changeCountBefore = stateChangedCount
        stateMachine.pause()
        assertEquals(changeCountBefore, stateChangedCount)
    }

    @Test
    fun testResumeWhenNotPausedIsNoOp() {
        stateMachine.start(90)
        val changeCountBefore = stateChangedCount
        stateMachine.resume()
        assertEquals(changeCountBefore, stateChangedCount)
    }

    @Test
    fun testResumeWhenNotRunningIsNoOp() {
        stateMachine.resume()
        assertFalse(stateMachine.isRunning.value)
        assertFalse(stateMachine.isPaused.value)
    }

    @Test
    fun testAdjustIncreasesRemainingTime() {
        stateMachine.start(60)
        val result = stateMachine.adjust(15)
        assertEquals(75, result)
        assertEquals(75, stateMachine.remainingSeconds.value)
        assertTrue(stateMachine.isRunning.value)
    }

    @Test
    fun testAdjustDecreasesRemainingTime() {
        stateMachine.start(60)
        val result = stateMachine.adjust(-15)
        assertEquals(45, result)
        assertEquals(45, stateMachine.remainingSeconds.value)
        assertTrue(stateMachine.isRunning.value)
    }

    @Test
    fun testAdjustBelowZeroCancelsTimer() {
        stateMachine.start(10)
        val result = stateMachine.adjust(-15)
        assertEquals(0, result)
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isRunning.value)
        assertEquals(1, cancelledCount)
    }

    @Test
    fun testAdjustExactZeroCancelsTimer() {
        stateMachine.start(15)
        val result = stateMachine.adjust(-15)
        assertEquals(0, result)
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isRunning.value)
        assertEquals(1, cancelledCount)
    }

    @Test
    fun testAdjustWhenNotRunningIsNoOp() {
        val result = stateMachine.adjust(15)
        assertEquals(0, result)
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isRunning.value)
    }

    @Test
    fun testAdjustWhilePausedPreservesPausedState() {
        stateMachine.start(60)
        stateMachine.pause()
        val result = stateMachine.adjust(15)
        assertEquals(75, result)
        assertEquals(75, stateMachine.remainingSeconds.value)
        assertTrue(stateMachine.isPaused.value)
        assertTrue(stateMachine.isRunning.value)
    }

    @Test
    fun testCompleteResetsStateAndTriggersCallback() {
        stateMachine.start(60)
        stateMachine.complete()
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isRunning.value)
        assertFalse(stateMachine.isPaused.value)
        assertEquals(1, completedCount)
    }

    @Test
    fun testCancelResetsStateAndTriggersCallback() {
        stateMachine.start(60)
        stateMachine.cancel()
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isRunning.value)
        assertFalse(stateMachine.isPaused.value)
        assertEquals(1, cancelledCount)
    }

    @Test
    fun testResetClearsEverythingIncludingLabel() {
        stateMachine.start(60, "Squats Set 3")
        stateMachine.pause()
        stateMachine.reset()
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isRunning.value)
        assertFalse(stateMachine.isPaused.value)
        assertEquals("", stateMachine.nextSetLabel)
    }

    @Test
    fun testFormatSeconds() {
        assertEquals("00:00", RestTimerStateMachine.formatSeconds(0))
        assertEquals("00:09", RestTimerStateMachine.formatSeconds(9))
        assertEquals("00:45", RestTimerStateMachine.formatSeconds(45))
        assertEquals("01:00", RestTimerStateMachine.formatSeconds(60))
        assertEquals("01:30", RestTimerStateMachine.formatSeconds(90))
        assertEquals("02:00", RestTimerStateMachine.formatSeconds(120))
        assertEquals("03:00", RestTimerStateMachine.formatSeconds(180))
        assertEquals("05:15", RestTimerStateMachine.formatSeconds(315))
        assertEquals("00:00", RestTimerStateMachine.formatSeconds(-10))
    }

    @Test
    fun testStartWithWorkoutIdAndTotalDuration() {
        stateMachine.start(90, "Bench Press Set 3", workoutId = 42L)
        assertEquals(90, stateMachine.remainingSeconds.value)
        assertEquals(90, stateMachine.totalDurationSeconds.value)
        assertEquals(42L, stateMachine.workoutId)
        assertEquals("Bench Press Set 3", stateMachine.nextSetLabel)
        assertTrue(stateMachine.isRunning.value)
    }

    @Test
    fun testAdjustExpandsTotalDurationWhenExceedingOriginal() {
        stateMachine.start(30, "Curls Set 1", workoutId = 10L)
        assertEquals(30, stateMachine.totalDurationSeconds.value)

        stateMachine.adjust(15)
        assertEquals(45, stateMachine.remainingSeconds.value)
        assertEquals(45, stateMachine.totalDurationSeconds.value)
    }

    @Test
    fun testToDurableStateAndRestoreRunning() {
        val now = 1000000L
        stateMachine.start(60, "Deadlift Set 2", workoutId = 99L)
        val endEpoch = now + 60_000L
        val durable = stateMachine.toDurableState(endEpoch)

        assertEquals(60, durable.totalDurationSeconds)
        assertEquals(99L, durable.workoutId)
        assertEquals("Deadlift Set 2", durable.nextSetLabel)
        assertTrue(durable.isRunning)
        assertFalse(durable.isPaused)

        // Reset state machine
        stateMachine.reset()
        assertEquals(0, stateMachine.remainingSeconds.value)

        // Restore 20 seconds later (40 seconds remaining)
        val restored = stateMachine.restore(durable, nowMillis = now + 20_000L)
        assertTrue(restored)
        assertEquals(40, stateMachine.remainingSeconds.value)
        assertEquals(60, stateMachine.totalDurationSeconds.value)
        assertEquals("Deadlift Set 2", stateMachine.nextSetLabel)
        assertEquals(99L, stateMachine.workoutId)
        assertTrue(stateMachine.isRunning.value)
    }

    @Test
    fun testRestoreExpiredTransitionsToComplete() {
        val now = 1000000L
        stateMachine.start(60, "Deadlift Set 2", workoutId = 99L)
        val endEpoch = now + 60_000L
        val durable = stateMachine.toDurableState(endEpoch)

        stateMachine.reset()

        // Restore after expiration (70 seconds later)
        val restored = stateMachine.restore(durable, nowMillis = now + 70_000L)
        assertFalse(restored)
        assertEquals(0, stateMachine.remainingSeconds.value)
        assertFalse(stateMachine.isRunning.value)
        assertEquals(1, completedCount)
    }

    @Test
    fun testRestorePausedState() {
        val now = 1000000L
        val durable = DurableTimerState(
            isRunning = true,
            isPaused = true,
            restEndEpochMillis = now + 60_000L,
            totalDurationSeconds = 90,
            pausedRemainingSeconds = 45,
            nextSetLabel = "Squat Set 4",
            workoutId = 123L
        )

        val restored = stateMachine.restore(durable, nowMillis = now + 100_000L)
        assertTrue(restored)
        assertEquals(45, stateMachine.remainingSeconds.value)
        assertEquals(90, stateMachine.totalDurationSeconds.value)
        assertTrue(stateMachine.isPaused.value)
        assertTrue(stateMachine.isRunning.value)
        assertEquals("Squat Set 4", stateMachine.nextSetLabel)
        assertEquals(123L, stateMachine.workoutId)
    }
}
