package com.gymcoach.app.presentation.workout.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviousPerformanceRowTest {

    @Test
    fun `formatWeight formats whole numbers without decimal places`() {
        assertEquals("80", formatWeight(80.0))
        assertEquals("0", formatWeight(0.0))
        assertEquals("100", formatWeight(100.0))
    }

    @Test
    fun `formatWeight formats decimal numbers with trailing decimal place`() {
        assertEquals("80.5", formatWeight(80.5))
        assertEquals("22.5", formatWeight(22.5))
        assertEquals("1.2", formatWeight(1.25))
    }
}
