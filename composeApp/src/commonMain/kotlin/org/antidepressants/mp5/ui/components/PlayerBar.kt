package org.antidepressants.mp5.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.antidepressants.mp5.player.PlaybackState
import org.antidepressants.mp5.player.PlayerState
import org.antidepressants.mp5.player.RepeatMode

/**
 * Player bar component with glassmorphism effect.
 * Displays current track info, playback controls, and progress.
 * 
 * Works in both TOP (Psychopath) and BOTTOM positions.
 */
@Composable
fun PlayerBar(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isTop: Boolean = true // Psychopath mode indicator
) {
    val isPlaying = playerState.playbackState == PlaybackState.PLAYING
    val progress = if (playerState.duration > 0) {
        playerState.currentPosition.toFloat() / playerState.duration.toFloat()
    } else 0f
    
    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color.Black.copy(alpha = 0.6f),
        shape = if (isTop) {
            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colors.primary.copy(alpha = 0.3f),
                            Color.Transparent,
                            MaterialTheme.colors.primary.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Progress bar at top/bottom based on position
                if (isTop) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerContent(
                            playerState = playerState,
                            isPlaying = isPlaying,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onShuffleToggle = onShuffleToggle,
                            onRepeatToggle = onRepeatToggle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Progress slider
                    Slider(
                        value = animatedProgress,
                        onValueChange = onSeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colors.primary,
                            activeTrackColor = MaterialTheme.colors.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                } else {
                    // Progress slider first for bottom position
                    Slider(
                        value = animatedProgress,
                        onValueChange = onSeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colors.primary,
                            activeTrackColor = MaterialTheme.colors.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerContent(
                            playerState = playerState,
                            isPlaying = isPlaying,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onShuffleToggle = onShuffleToggle,
                            onRepeatToggle = onRepeatToggle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerContent(
    playerState: PlayerState,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playerState.currentTrack?.title ?: "No track playing",
                style = MaterialTheme.typography.body1.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playerState.currentTrack?.artist ?: "",
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Playback controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = onShuffleToggle) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playerState.isShuffleEnabled) {
                        MaterialTheme.colors.primary
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    }
                )
            }
            
            // Previous
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White
                )
            }
            
            // Play/Pause (larger button)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Next
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White
                )
            }
            
            // Repeat
            IconButton(onClick = onRepeatToggle) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = when (playerState.repeatMode) {
                        RepeatMode.OFF -> Color.White.copy(alpha = 0.6f)
                        RepeatMode.ONE -> MaterialTheme.colors.primary
                        RepeatMode.ALL -> MaterialTheme.colors.primary.copy(alpha = 0.8f)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Time display
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatDuration(playerState.currentPosition),
                style = MaterialTheme.typography.caption,
                color = Color.White
            )
            Text(
                text = formatDuration(playerState.duration),
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Format milliseconds to MM:SS string.
 */
private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
