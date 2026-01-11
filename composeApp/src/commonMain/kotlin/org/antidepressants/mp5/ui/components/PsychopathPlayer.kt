package org.antidepressants.mp5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.antidepressants.mp5.player.DemoPlayer
import org.antidepressants.mp5.player.PlaybackState

@Composable
fun PsychopathPlayer(
    modifier: Modifier = Modifier
) {
    val playerState by DemoPlayer.controller.playerState.collectAsState()
    val isPlaying = playerState.playbackState == PlaybackState.PLAYING
    
    // Calculate progress (0.0 to 1.0)
    val progress = if (playerState.duration > 0) {
        playerState.currentPosition.toFloat() / playerState.duration.toFloat()
    } else 0f
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Track Info
        Column(
            modifier = Modifier.weight(0.3f)
        ) {
            Text(
                playerState.currentTrack?.title ?: "No track loaded",
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                playerState.currentTrack?.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        // Controls
        Column(
            modifier = Modifier.weight(0.4f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { DemoPlayer.controller.previous() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = MaterialTheme.colors.onSurface)
                }
                IconButton(onClick = { DemoPlayer.controller.togglePlayPause() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = { DemoPlayer.controller.next() }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = MaterialTheme.colors.onSurface)
                }
            }
            
            // Playback state indicator
            Text(
                text = when (playerState.playbackState) {
                    PlaybackState.PLAYING -> "▶ Playing"
                    PlaybackState.PAUSED -> "⏸ Paused"
                    PlaybackState.LOADING -> "⏳ Loading..."
                    PlaybackState.IDLE -> "Ready"
                    PlaybackState.ENDED -> "Finished"
                    PlaybackState.ERROR -> "⚠ Error"
                },
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary.copy(alpha = 0.8f)
            )
        }

        // Seek Bar & Time
        Column(
            modifier = Modifier.weight(0.3f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = progress,
                onValueChange = { DemoPlayer.controller.seekTo(it) },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary
                )
            )
            
            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(playerState.currentPosition),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    formatTime(playerState.duration),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
