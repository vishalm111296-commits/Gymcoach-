package com.gymcoach.app.core.audio

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
}
