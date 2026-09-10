package com.gymcoach.app.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseVideoPlayerHelpersTest {

    @Test
    fun test_formatTimeZero() {
        assertEquals("0:00", formatTime(0))
    }

    @Test
    fun test_formatTimeSecondsAndMinutes() {
        assertEquals("1:30", formatTime(90_000))
        assertEquals("59:59", formatTime(3_599_000))
    }

    @Test
    fun test_formatTimeNegativeClampedToZero() {
        assertEquals("0:00", formatTime(-5_000))
        assertEquals("0:00", formatTime(-1))
        assertEquals("0:00", formatTime(-100_000))
    }

    @Test
    fun test_isMediaUnavailableNullAndBlank() {
        assertTrue(isMediaUnavailable(null))
        assertTrue(isMediaUnavailable(""))
        assertTrue(isMediaUnavailable("   "))
        assertTrue(isMediaUnavailable("\t\n"))
    }

    @Test
    fun test_isMediaUnavailableHttpUri() {
        assertEquals(false, isMediaUnavailable("https://example.com/v.mp4"))
        assertEquals(false, isMediaUnavailable("android.resource://pkg/raw/v"))
        assertEquals(false, isMediaUnavailable("content://media/external/video/123"))
        assertEquals(false, isMediaUnavailable("file:///sdcard/video.mp4"))
    }
}