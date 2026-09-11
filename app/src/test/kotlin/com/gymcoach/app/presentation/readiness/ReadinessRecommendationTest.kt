package com.gymcoach.app.presentation.readiness

import com.gymcoach.app.data.local.entity.ReadinessEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessRecommendationTest {

    @Test
    fun `readiness score computes exact average of 4 metrics`() {
        val entry = ReadinessEntity(
            sleepQuality = 4,
            soreness = 3,
            energy = 5,
            motivation = 4
        )
        assertEquals(4.0, entry.readinessScore, 0.001)
    }

    @Test
    fun `optimal readiness suggests full intensity session`() {
        val entry = ReadinessEntity(
            sleepQuality = 4,
            soreness = 4,
            energy = 4,
            motivation = 4
        )
        assertEquals(4.0, entry.readinessScore, 0.001)
        assertEquals("Full intensity session recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `moderate readiness suggests moderate session`() {
        val entry = ReadinessEntity(
            sleepQuality = 3,
            soreness = 3,
            energy = 3,
            motivation = 4
        )
        assertEquals(3.25, entry.readinessScore, 0.001)
        assertEquals("Moderate session recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `low readiness suggests light session or active recovery`() {
        val entry = ReadinessEntity(
            sleepQuality = 2,
            soreness = 2,
            energy = 3,
            motivation = 2
        )
        assertEquals(2.25, entry.readinessScore, 0.001)
        assertEquals("Light session or active recovery recommended", entry.trainingRecommendation)
        assertTrue(entry.isRestDayRecommended)
    }

    @Test
    fun `severely fatigued suggests rest day`() {
        val entry = ReadinessEntity(
            sleepQuality = 1,
            soreness = 1,
            energy = 2,
            motivation = 1
        )
        assertEquals(1.25, entry.readinessScore, 0.001)
        assertEquals("Rest day recommended. Listen to your body.", entry.trainingRecommendation)
        assertTrue(entry.isRestDayRecommended)
    }

    @Test
    fun `maximum score yields 5_0`() {
        val entry = ReadinessEntity(
            sleepQuality = 5,
            soreness = 5,
            energy = 5,
            motivation = 5
        )
        assertEquals(5.0, entry.readinessScore, 0.001)
        assertEquals("Full intensity session recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `minimum score yields 1_0 and flags rest day`() {
        val entry = ReadinessEntity(
            sleepQuality = 1,
            soreness = 1,
            energy = 1,
            motivation = 1
        )
        assertEquals(1.0, entry.readinessScore, 0.001)
        assertEquals("Rest day recommended. Listen to your body.", entry.trainingRecommendation)
        assertTrue(entry.isRestDayRecommended)
    }
}
