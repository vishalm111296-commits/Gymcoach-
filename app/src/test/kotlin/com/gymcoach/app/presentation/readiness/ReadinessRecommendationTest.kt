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

    @Test
    fun `freshness check correctly identifies today vs past records`() {
        val todayEntry = ReadinessEntity(
            recordedAt = System.currentTimeMillis(),
            sleepQuality = 4,
            soreness = 4,
            energy = 4,
            motivation = 4
        )
        assertTrue(todayEntry.isRecordedToday)

        val pastEntry = ReadinessEntity(
            recordedAt = System.currentTimeMillis() - (48 * 60 * 60 * 1000L), // 2 days ago
            sleepQuality = 1,
            soreness = 1,
            energy = 1,
            motivation = 1
        )
        assertFalse(pastEntry.isRecordedToday)
    }

    @Test
    fun `boundary test - exactly 4_0 yields full intensity`() {
        val entry = ReadinessEntity(
            sleepQuality = 5,
            soreness = 5,
            energy = 3,
            motivation = 3
        )
        assertEquals(4.0, entry.readinessScore, 0.001)
        assertEquals("Full intensity session recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `boundary test - just below 4_0 yields moderate session`() {
        val entry = ReadinessEntity(
            sleepQuality = 4,
            soreness = 4,
            energy = 4,
            motivation = 3
        )
        assertEquals(3.75, entry.readinessScore, 0.001)
        assertEquals("Moderate session recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `boundary test - exactly 3_0 yields moderate session`() {
        val entry = ReadinessEntity(
            sleepQuality = 3,
            soreness = 3,
            energy = 3,
            motivation = 3
        )
        assertEquals(3.0, entry.readinessScore, 0.001)
        assertEquals("Moderate session recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `boundary test - just below 3_0 yields light session without rest day`() {
        val entry = ReadinessEntity(
            sleepQuality = 3,
            soreness = 3,
            energy = 3,
            motivation = 2
        )
        assertEquals(2.75, entry.readinessScore, 0.001)
        assertEquals("Light session or active recovery recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `boundary test - exactly 2_5 does not flag rest day`() {
        val entry = ReadinessEntity(
            sleepQuality = 3,
            soreness = 3,
            energy = 2,
            motivation = 2
        )
        assertEquals(2.5, entry.readinessScore, 0.001)
        assertEquals("Light session or active recovery recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `boundary test - just below 2_5 flags rest day`() {
        val entry = ReadinessEntity(
            sleepQuality = 3,
            soreness = 2,
            energy = 2,
            motivation = 2
        )
        assertEquals(2.25, entry.readinessScore, 0.001)
        assertEquals("Light session or active recovery recommended", entry.trainingRecommendation)
        assertTrue(entry.isRestDayRecommended)
    }

    @Test
    fun `boundary test - exactly 2_0 yields light session with rest day flagged`() {
        val entry = ReadinessEntity(
            sleepQuality = 2,
            soreness = 2,
            energy = 2,
            motivation = 2
        )
        assertEquals(2.0, entry.readinessScore, 0.001)
        assertEquals("Light session or active recovery recommended", entry.trainingRecommendation)
        assertTrue(entry.isRestDayRecommended)
    }

    @Test
    fun `boundary test - just below 2_0 transitions to rest day recommended`() {
        val entry = ReadinessEntity(
            sleepQuality = 2,
            soreness = 2,
            energy = 2,
            motivation = 1
        )
        assertEquals(1.75, entry.readinessScore, 0.001)
        assertEquals("Rest day recommended. Listen to your body.", entry.trainingRecommendation)
        assertTrue(entry.isRestDayRecommended)
    }

    @Test
    fun `default readiness entity provides balanced moderate defaults`() {
        val defaultEntry = ReadinessEntity()
        assertEquals(3, defaultEntry.sleepQuality)
        assertEquals(3, defaultEntry.soreness)
        assertEquals(3, defaultEntry.energy)
        assertEquals(3, defaultEntry.motivation)
        assertEquals(3.0, defaultEntry.readinessScore, 0.001)
        assertEquals("Moderate session recommended", defaultEntry.trainingRecommendation)
        assertFalse(defaultEntry.isRestDayRecommended)
        assertTrue(defaultEntry.isRecordedToday)
    }

    @Test
    fun `asymmetric extreme inputs calculate correct average`() {
        // High sleep & energy (5, 5) but drained soreness & motivation (1, 1)
        val entry = ReadinessEntity(
            sleepQuality = 5,
            soreness = 1,
            energy = 5,
            motivation = 1
        )
        assertEquals(3.0, entry.readinessScore, 0.001)
        assertEquals("Moderate session recommended", entry.trainingRecommendation)
        assertFalse(entry.isRestDayRecommended)
    }

    @Test
    fun `date freshness boundary - tomorrow or past calendar days are not today`() {
        val tomorrow = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val tomorrowEntry = ReadinessEntity(recordedAt = tomorrow)
        assertFalse(tomorrowEntry.isRecordedToday)

        val yesterday = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val yesterdayEntry = ReadinessEntity(recordedAt = yesterday)
        assertFalse(yesterdayEntry.isRecordedToday)
    }
}
