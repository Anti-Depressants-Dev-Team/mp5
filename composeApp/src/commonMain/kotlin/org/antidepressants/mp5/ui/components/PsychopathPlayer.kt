package org.antidepressants.mp5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.antidepressants.mp5.player.DemoPlayer
import org.antidepressants.mp5.player.PlaybackState
import org.antidepressants.mp5.player.RepeatMode
import org.antidepressants.mp5.settings.GlobalSettings
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.shape.CircleShape

@Composable
fun PsychopathPlayer(
    modifier: Modifier = Modifier
) {
    val playerState by DemoPlayer.controller.playerState.collectAsState()
    val settingsState by GlobalSettings.settings.state.collectAsState()
    val isPlaying = playerState.playbackState == PlaybackState.PLAYING
    
    // Local state for volume
    var volumeBoost by remember { mutableStateOf(settingsState.volumeBoost) }
    var showLyrics by remember { mutableStateOf(false) }
    
    // Calculate progress (0.0 to 1.0)
    val progress = if (playerState.duration > 0) {
        playerState.currentPosition.toFloat() / playerState.duration.toFloat()
    } else 0f
    
    // Lyrics Overlay/Popup
    if (showLyrics) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLyrics = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f) // 90% width
                    .height(500.dp) // Fixed height
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.95f))
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                LyricsView(
                    lyrics = playerState.currentLyrics,
                    currentPosition = playerState.currentPosition,
                    isLoading = playerState.isLoadingLyrics,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Close button
                IconButton(
                    onClick = { showLyrics = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colors.onSurface)
                }
            }
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // More height for glass
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colors.surface.copy(alpha = 0.85f),
                        MaterialTheme.colors.surface.copy(alpha = 0.65f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // === LEFT: Playback Controls ===
        Row(
            modifier = Modifier.width(140.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { DemoPlayer.controller.previous() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious, 
                    "Previous", 
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            IconButton(
                onClick = { DemoPlayer.controller.togglePlayPause() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            IconButton(
                onClick = { DemoPlayer.controller.next() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext, 
                    "Next", 
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // === CENTER: Track Info + Progress ===
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // Track info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        playerState.currentTrack?.title ?: "No track loaded",
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        playerState.currentTrack?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Progress bar with time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatTime(playerState.currentPosition),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
                
                Slider(
                    value = progress,
                    onValueChange = { DemoPlayer.controller.seekTo(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colors.primary,
                        activeTrackColor = MaterialTheme.colors.primary,
                        inactiveTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                    )
                )
                
                Text(
                    formatTime(playerState.duration),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        
        // === RIGHT: Volume, Repeat, Shuffle, Volume Boost ===
        Row(
            modifier = Modifier.width(280.dp), // More width for buttons
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
             // Volume Boost button (quick access!) - moved to left of group for better flow
            Box {
                IconButton(
                    onClick = {
                        // Toggle between 100% and 150% boost
                        val newBoost = if (volumeBoost > 100) 100 else 150
                        volumeBoost = newBoost
                        GlobalSettings.settings.volumeBoost = newBoost
                        DemoPlayer.controller.setVolumeBoost(newBoost / 100f)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        "Volume Boost",
                        tint = if (volumeBoost > 100) 
                            MaterialTheme.colors.primary 
                        else 
                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Boost indicator badge
                if (volumeBoost > 100) {
                    Text(
                        "${volumeBoost}%",
                        style = MaterialTheme.typography.overline,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    )
                }
            }

            // Shuffle button
            IconButton(
                onClick = { DemoPlayer.controller.toggleShuffle() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    "Shuffle",
                    tint = if (playerState.isShuffleEnabled) 
                        MaterialTheme.colors.primary 
                    else 
                        MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Repeat button
            IconButton(
                onClick = { DemoPlayer.controller.cycleRepeatMode() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    when (playerState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    "Repeat",
                    tint = if (playerState.repeatMode != RepeatMode.OFF) 
                        MaterialTheme.colors.primary 
                    else 
                        MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Lyrics Button
            IconButton(
                onClick = { showLyrics = !showLyrics },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Mic, // Or TextSnippet
                    "Lyrics",
                    tint = if (showLyrics) 
                        MaterialTheme.colors.primary 
                    else 
                        MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            
            // Inline Volume Control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(32.dp)
                    .background(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 8.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.scrollDelta.y != 0f }
                                if (change != null) {
                                    val delta = change.scrollDelta
                                    // Scroll down (positive y) -> decrease volume
                                    // Scroll up (negative y) -> increase volume
                                    val adjustment = if (delta.y > 0) -0.05f else 0.05f
                                    val newVolume = (playerState.volumeLevel + adjustment).coerceIn(0f, 1f)
                                    DemoPlayer.controller.setVolume(newVolume)
                                    change.consume()
                                }
                            }
                        }
                    }
            ) {
                // Click to mute/unmute could be added here
                Icon(
                    when {
                        playerState.volumeLevel == 0f -> Icons.Default.VolumeOff
                        playerState.volumeLevel < 0.5f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    },
                    "Volume",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp).clickable {
                        // Simple mute toggle logic could go here
                        if (playerState.volumeLevel > 0) DemoPlayer.controller.setVolume(0f) else DemoPlayer.controller.setVolume(1f)
                    }
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Slider(
                    value = playerState.volumeLevel,
                    onValueChange = { DemoPlayer.controller.setVolume(it) },
                    modifier = Modifier.width(80.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colors.primary,
                        activeTrackColor = MaterialTheme.colors.primary,
                        inactiveTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                    )
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
