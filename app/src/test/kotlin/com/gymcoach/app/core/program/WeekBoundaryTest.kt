package com.gymcoach.app.core.program

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeekBoundaryTest {

    private lateinit var volumeCalculator: VolumeCalculator
    private val zoneId = ZoneId.of("America/New_York")

    @Before
    fun setUp() {
        volumeCalculator = VolumeCalculator()
    }

    @Test
    fun `isoWeekKey correctly groups New Year boundary dates into ISO week-based years`() {
        fun keyFor(year: Int, month: Int, day: Int, hour: Int = 12): String {
            val ms = LocalDate.of(year, month, day)
                .atTime(hour, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
            return volumeCalculator.isoWeekKey(ms, zoneId)
        }

        // Dec 28 & 29, 2024 are in ISO week 52 of 2024
        assertEquals("2024-W52", keyFor(2024, 12, 28))
        assertEquals("2024-W52", keyFor(2024, 12, 29))

        // Dec 30 & 31, 2024 and Jan 1..5, 2025 are in ISO week 01 of 2025
        assertEquals("2025-W01", keyFor(2024, 12, 30))
        assertEquals("2025-W01", keyFor(2024, 12, 31))
        assertEquals("2025-W01", keyFor(2025, 1, 1))
        assertEquals("2025-W01", keyFor(2025, 1, 2))
        assertEquals("2025-W01", keyFor(2025, 1, 3))
        assertEquals("2025-W01", keyFor(2025, 1, 4))

        // Jan 6, 2025 starts ISO week 02 of 2025
        assertEquals("2025-W02", keyFor(2025, 1, 6))
    }

    @Test
    fun `midnight workouts belong to local date and ISO week`() {
        fun keyForTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): String {
            val ms = LocalDate.of(year, month, day)
                .atTime(hour, minute)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
            return volumeCalculator.isoWeekKey(ms, zoneId)
        }

        // 23:59 on Sunday Jan 5, 2025 is ISO week 01 of 2025
        assertEquals("2025-W01", keyForTime(2025, 1, 5, 23, 59))

        // 00:01 on Monday Jan 6, 2025 is ISO week 02 of 2025
        assertEquals("2025-W02", keyForTime(2025, 1, 6, 0, 1))
    }
}
