package com.gymcoach.app.core.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

@Stable
class AnimationController(
    initialDefinition: ExerciseAnimationDefinition? = null,
    initialAutoPlay: Boolean = true
) {
    var definition by mutableStateOf(initialDefinition)
        private set

    var isPlaying by mutableStateOf(initialAutoPlay)
        private set

    var progress by mutableFloatStateOf(0.0f)
        private set

    var speed by mutableFloatStateOf(1.0f)
        private set

    var isLooping by mutableStateOf(true)
        private set

    var isStepMode by mutableStateOf(false)
        private set

    val currentFrame: InterpolatedFrame?
        get() = definition?.interpolateAt(progress)

    val currentPhase: AnimationPhase
        get() = currentFrame?.phase ?: AnimationPhase.START

    val currentCue: String
        get() = currentFrame?.cue.orEmpty()

    fun updateDefinition(newDef: ExerciseAnimationDefinition?) {
        if (definition?.exerciseId != newDef?.exerciseId) {
            definition = newDef
            progress = 0.0f
        }
    }

    fun play() {
        isPlaying = true
        isStepMode = false
    }

    fun pause() {
        isPlaying = false
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun seekTo(newProgress: Float) {
        progress = newProgress.coerceIn(0.0f, 1.0f)
    }

    fun replay() {
        progress = 0.0f
        isPlaying = true
    }

    fun setPlaybackSpeed(newSpeed: Float) {
        speed = newSpeed.coerceIn(0.25f, 2.0f)
    }

    fun setLoop(loop: Boolean) {
        isLooping = loop
    }

    fun nextStep() {
        val def = definition ?: return
        pause()
        isStepMode = true
        val nextKf = def.keyframes.firstOrNull { it.progress > progress + 0.02f }
        progress = if (nextKf != null) {
            nextKf.progress
        } else if (isLooping) {
            def.keyframes.first().progress
        } else {
            def.keyframes.last().progress
        }
    }

    fun previousStep() {
        val def = definition ?: return
        pause()
        isStepMode = true
        val prevKf = def.keyframes.lastOrNull { it.progress < progress - 0.02f }
        progress = if (prevKf != null) {
            prevKf.progress
        } else if (isLooping) {
            def.keyframes.last().progress
        } else {
            def.keyframes.first().progress
        }
    }

    fun advance(deltaTimeMs: Long) {
        val def = definition ?: return
        if (!isPlaying) return

        val duration = def.durationMs.coerceAtLeast(100L)
        val deltaProgress = (deltaTimeMs * speed) / duration.toFloat()
        var newProgress = progress + deltaProgress

        if (newProgress >= 1.0f) {
            if (isLooping) {
                newProgress %= 1.0f
            } else {
                newProgress = 1.0f
                isPlaying = false
            }
        }
        progress = newProgress.coerceIn(0.0f, 1.0f)
    }
}

@Composable
fun rememberAnimationController(
    definition: ExerciseAnimationDefinition?,
    autoPlay: Boolean = true
): AnimationController {
    val controller = remember { AnimationController(definition, autoPlay) }

    LaunchedEffect(definition) {
        controller.updateDefinition(definition)
    }

    // High performance animation loop ticking every ~16ms (60 FPS)
    LaunchedEffect(controller.isPlaying, controller.speed) {
        var lastTime = System.currentTimeMillis()
        while (isActive && controller.isPlaying) {
            delay(16L)
            val now = System.currentTimeMillis()
            val delta = now - lastTime
            lastTime = now
            controller.advance(delta)
        }
    }

    return controller
}
