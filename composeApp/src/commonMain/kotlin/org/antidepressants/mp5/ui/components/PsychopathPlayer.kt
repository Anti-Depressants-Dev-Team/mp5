package org.antidepressants.mp5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import org.antidepressants.mp5.player.DemoPlayer
import org.antidepressants.mp5.player.PlaybackState
import org.antidepressants.mp5.player.RepeatMode
import org.antidepressants.mp5.settings.GlobalSettings

/**
 * YouTube-style player bar layout:
 * LEFT: Playback controls (prev, play/pause, next)
 * CENTER: Track info + progress bar
 * RIGHT: Volume, repeat, shuffle, volume boost
 */
@Composable
fun PsychopathPlayer(
    modifier: Modifier = Modifier
) {
    val playerState by DemoPlayer.controller.playerState.collectAsState()
    val settingsState by GlobalSettings.settings.state.collectAsState()
    val isPlaying = playerState.playbackState == PlaybackState.PLAYING
    
    // Local state for volume popup
    var showVolumePopup by remember { mutableStateOf(false) }
    var volumeBoost by remember { mutableStateOf(settingsState.volumeBoost) }
    
    // Calculate progress (0.0 to 1.0)
    val progress = if (playerState.duration > 0) {
        playerState.currentPosition.toFloat() / playerState.duration.toFloat()
    } else 0f
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 12.dp),
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
            modifier = Modifier.width(180.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    modifier = Modifier.size(22.dp)
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
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Volume button with Popup
            Box {
                IconButton(
                    onClick = { showVolumePopup = !showVolumePopup },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        when {
                            playerState.volumeLevel == 0f -> Icons.Default.VolumeOff
                            playerState.volumeLevel < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        "Volume",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (showVolumePopup) {
                    Popup(
                        alignment = Alignment.TopCenter,
                        onDismissRequest = { showVolumePopup = false }
                    ) {
                        Surface(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colors.surface,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .offset(y = (-150).dp) // Move up above the button
                                .width(48.dp)
                                .height(140.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Slider(
                                    value = playerState.volumeLevel,
                                    onValueChange = { DemoPlayer.controller.setVolume(it) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colors.primary,
                                        activeTrackColor = MaterialTheme.colors.primary,
                                        inactiveTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .width(120.dp)
                                        .rotate(-90f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Volume Boost button (quick access!)
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
                        modifier = Modifier.size(22.dp)
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
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
