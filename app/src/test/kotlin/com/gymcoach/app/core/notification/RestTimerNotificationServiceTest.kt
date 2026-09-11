package com.gymcoach.app.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RestTimerNotificationServiceTest {

    @Test
    fun testFormatSecondsStandardDurations() {
        assertEquals("00:00", RestTimerNotificationService.formatSeconds(0))
        assertEquals("00:09", RestTimerNotificationService.formatSeconds(9))
        assertEquals("00:45", RestTimerNotificationService.formatSeconds(45))
        assertEquals("01:00", RestTimerNotificationService.formatSeconds(60))
        assertEquals("01:30", RestTimerNotificationService.formatSeconds(90))
        assertEquals("02:00", RestTimerNotificationService.formatSeconds(120))
        assertEquals("03:00", RestTimerNotificationService.formatSeconds(180))
        assertEquals("05:15", RestTimerNotificationService.formatSeconds(315))
        assertEquals("00:00", RestTimerNotificationService.formatSeconds(-10))
    }

    @Test
    fun testCompanionInitialState() {
        assertEquals(0, RestTimerNotificationService.remainingSeconds.value)
        assertFalse(RestTimerNotificationService.isPaused.value)
        assertFalse(RestTimerNotificationService.isRunning.value)
    }

    @Test
    fun testActionConstants() {
        assertEquals("com.gymcoach.app.resttimer.START", RestTimerNotificationService.ACTION_START)
        assertEquals("com.gymcoach.app.resttimer.PAUSE", RestTimerNotificationService.ACTION_PAUSE)
        assertEquals("com.gymcoach.app.resttimer.RESUME", RestTimerNotificationService.ACTION_RESUME)
        assertEquals("com.gymcoach.app.resttimer.CANCEL", RestTimerNotificationService.ACTION_CANCEL)
        assertEquals("com.gymcoach.app.resttimer.COMPLETE", RestTimerNotificationService.ACTION_COMPLETE)
        assertEquals("com.gymcoach.app.resttimer.SKIP", RestTimerNotificationService.ACTION_SKIP)
        assertEquals("com.gymcoach.app.resttimer.PLUS_15", RestTimerNotificationService.ACTION_PLUS_15)
        assertEquals("com.gymcoach.app.resttimer.MINUS_15", RestTimerNotificationService.ACTION_MINUS_15)
    }
}
