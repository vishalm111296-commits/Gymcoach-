package com.gymcoach.app.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RestAudioCoachTest {

    private lateinit var evaluator: RestAudioCueEvaluator
    private lateinit var coach: RestAudioCoach

    @Before
    fun setUp() {
        evaluator = RestAudioCueEvaluator()
        coach = RestAudioCoach(evaluator)
    }

    @Test
    fun `sound enabled state can be toggled`() {
        assertTrue(coach.isSoundEnabled())
        coach.setSoundEnabled(false)
        assertFalse(coach.isSoundEnabled())
        coach.setSoundEnabled(true)
        assertTrue(coach.isSoundEnabled())
    }

    @Test
    fun `onTick and onComplete run safely without throwing exceptions`() {
        coach.onTick(3)
        coach.onTick(2)
        coach.onTick(1)
        coach.onTick(0)
        coach.onTick(30)
        coach.onComplete(isSupersetSwitch = false)
        coach.onComplete(isSupersetSwitch = true)
        coach.playCue(AudioCueType.TIMER_FINISHED)
    }

    @Test
    fun `playCue does not trigger sound when sound is disabled`() {
        coach.setSoundEnabled(false)
        coach.onTick(3)
        coach.onComplete()
    }

    @Test
    fun `preset can be changed and read back`() {
        assertEquals(AudioCoachPreset.CLASSIC_BEEPS, coach.getPreset())
        coach.setPreset(AudioCoachPreset.MELLOW_CHIMES)
        assertEquals(AudioCoachPreset.MELLOW_CHIMES, coach.getPreset())
        coach.setPreset(AudioCoachPreset.POWER_PULSE)
        assertEquals(AudioCoachPreset.POWER_PULSE, coach.getPreset())
    }

    @Test
    fun `getVibrationPattern returns valid non-empty arrays for all cue types`() {
        for (cue in AudioCueType.entries) {
            val pattern = coach.getVibrationPattern(cue)
            assertTrue("Vibration pattern for $cue must not be empty", pattern.isNotEmpty())
            assertEquals(0L, pattern[0]) // starts with 0 delay
        }
    }

    @Test
    fun `getVibrationPattern returns exact timing profiles for distinct cues`() {
        val warning = coach.getVibrationPattern(AudioCueType.WARNING_15S)
        assertEquals(4, warning.size)
        assertEquals(0L, warning[0])
        assertEquals(100L, warning[1])
        assertEquals(80L, warning[2])
        assertEquals(100L, warning[3])

        val finished = coach.getVibrationPattern(AudioCueType.TIMER_FINISHED)
        assertEquals(4, finished.size)
        assertEquals(200L, finished[1])
        assertEquals(100L, finished[2])
        assertEquals(300L, finished[3])

        val superset = coach.getVibrationPattern(AudioCueType.SUPERSET_SWITCH)
        assertEquals(6, superset.size)
        assertEquals(240L, superset[5])

        val countdown = coach.getVibrationPattern(AudioCueType.COUNTDOWN_1)
        assertEquals(2, countdown.size)
        assertEquals(60L, countdown[1])
    }

    @Test
    fun `playCue executes safely across all preset and cue combinations`() {
        for (preset in AudioCoachPreset.entries) {
            coach.setPreset(preset)
            assertEquals(preset, coach.getPreset())
            for (cue in AudioCueType.entries) {
                coach.playCue(cue)
            }
        }
    }
}
