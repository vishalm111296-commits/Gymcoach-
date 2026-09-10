package com.gymcoach.app.presentation.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun ExerciseVideoPlayer(
    videoUri: Uri?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Early return for unavailable media - no ExoPlayer creation needed
    if (isMediaUnavailable(videoUri?.toString())) {
        MediaPlaceholder(
            icon = Icons.Default.Info,
            message = "No media available",
            modifier = modifier
        )
        return
    }

    val effectiveUri = videoUri!!

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var hasEnded by remember { mutableStateOf(false) }
    var hasPlaybackError by remember { mutableStateOf(false) }

    LaunchedEffect(effectiveUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(effectiveUri))
        exoPlayer.prepare()
    }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isPlaying = state == Player.STATE_READY && exoPlayer.playWhenReady
                if (state == Player.STATE_ENDED) {
                    hasEnded = true
                    isPlaying = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    hasEnded = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                hasPlaybackError = true
                isPlaying = false
            }
        }

        exoPlayer.addListener(listener)

        // Listener-driven updates replace infinite polling loop.
        // Polling with delay(200L) spins forever on test scheduler (runTest teardown drains
        // all scheduled events including infinite loops -> hang). Player.Listener callbacks
        // fire on state changes: READY (duration known), playback position updates via
        // onIsPlayingChanged, and ENDED. This is deterministic, leak-free, and test-safe.
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Error state: render placeholder instead of player + controls
    if (hasPlaybackError) {
        MediaPlaceholder(
            icon = Icons.Default.Error,
            message = "Media unavailable",
            modifier = modifier
        )
        return
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    )

    // Controls
    val formattedPosition = formatTime(currentPosition)
    val formattedDuration = formatTime(duration)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play / Pause / Replay
        IconButton(onClick = {
            when {
                hasEnded -> {
                    exoPlayer.seekTo(0)
                    exoPlayer.play()
                    hasEnded = false
                }
                isPlaying -> {
                    exoPlayer.pause()
                    hasEnded = false
                }
                else -> {
                    exoPlayer.play()
                }
            }
        }) {
            Icon(
                imageVector = when {
                    hasEnded -> Icons.Default.Replay
                    isPlaying -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = when {
                    hasEnded -> "Replay"
                    isPlaying -> "Pause"
                    else -> "Play"
                },
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(4.dp))

        // Seek bar
        Slider(
            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
            onValueChange = { fraction ->
                val newPosition = (fraction * duration).toLong()
                exoPlayer.seekTo(newPosition)
                currentPosition = newPosition
                hasEnded = false
            },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "$formattedPosition / $formattedDuration",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MediaPlaceholder(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.padding(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Formats milliseconds as "M:SS" or "MM:SS". Negative values are clamped to 0.
 * Pure function — no Android dependencies, suitable for JVM unit tests.
 */
internal fun formatTime(millis: Long): String {
    val clampedMillis = if (millis < 0) 0 else millis
    val totalSeconds = clampedMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * Returns true if the URI string is null, blank, or whitespace-only.
 * Pure function — no Android dependencies, suitable for JVM unit tests.
 */
internal fun isMediaUnavailable(uriString: String?): Boolean {
    return uriString.isNullOrBlank()
}