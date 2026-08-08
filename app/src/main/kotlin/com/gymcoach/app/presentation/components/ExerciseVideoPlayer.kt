package com.gymcoach.app.presentation.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var hasEnded by remember { mutableStateOf(false) }

    LaunchedEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
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
            }
        }

        exoPlayer.addListener(listener)

        while (true) {
            kotlinx.coroutines.delay(200L)
            if (exoPlayer.playbackState == Player.STATE_READY) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Video player container
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )

        // Control bar with improved accessibility
        VideoControlBar(
            isPlaying = isPlaying,
            hasEnded = hasEnded,
            onTogglePlayPause = {
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
            }
        )

        VideoProgress(
            currentPosition = currentPosition,
            duration = duration
        )
    }
}

@Composable
private fun VideoControlBar(
    isPlaying: Boolean,
    hasEnded: Boolean,
    onTogglePlayPause: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play / Pause / Replay button with accessible description
        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.height(48.dp).width(48.dp)
        ) {
            Icon(
                imageVector = when {
                    hasEnded -> Icons.Default.Replay
                    isPlaying -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = when {
                    hasEnded -> "Replay video"
                    isPlaying -> "Pause video"
                    else -> "Play video"
                },
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun VideoProgress(
    currentPosition: Long,
    duration: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        androidx.compose.material3.Text(
            text = formatVideoTime(currentPosition) + " / " + formatVideoTime(duration),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatVideoTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}