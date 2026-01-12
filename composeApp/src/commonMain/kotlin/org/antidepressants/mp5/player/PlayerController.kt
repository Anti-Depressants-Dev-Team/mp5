package org.antidepressants.mp5.player

import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.antidepressants.mp5.domain.model.Track

/**
 * Demo audio player controller for quick testing.
 * Manages player state and coordinates with platform-specific implementations.
 */
class PlayerController {
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    // Platform-specific player will be injected
    private var audioPlayer: AudioPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    fun setAudioPlayer(player: AudioPlayer) {
        audioPlayer = player
        // Forward state updates from the actual player
        scope.launch {
            player.playerState.collect { state ->
                _playerState.value = state
            }
        }
    }
    
    fun loadTrack(track: Track, streamUrl: String, autoPlay: Boolean = true) {
        _playerState.update {
            it.copy(
                currentTrack = track,
                playbackState = PlaybackState.LOADING
            )
        }
        scope.launch {
            audioPlayer?.load(track, streamUrl)
            if (autoPlay) {
                // Small delay to ensure media is loaded
                delay(500)
                play()
            }
        }
    }
    
    fun play() {
        if (audioPlayer != null) {
            audioPlayer?.play()
        } else {
            // Demo mode - just update state
            _playerState.update { 
                it.copy(playbackState = PlaybackState.PLAYING)
            }
        }
    }
    
    fun pause() {
        if (audioPlayer != null) {
            audioPlayer?.pause()
        } else {
            // Demo mode
            _playerState.update { 
                it.copy(playbackState = PlaybackState.PAUSED)
            }
        }
    }
    
    fun togglePlayPause() {
        val current = _playerState.value.playbackState
        if (current == PlaybackState.PLAYING) {
            pause()
        } else {
            play()
        }
    }
    
    fun seekTo(progress: Float) {
        val duration = _playerState.value.duration
        val positionMs = (progress * duration).toLong()
        _playerState.update { 
            it.copy(currentPosition = positionMs)
        }
        audioPlayer?.seekTo(positionMs)
    }
    
    fun updatePosition(positionMs: Long) {
        _playerState.update { 
            it.copy(currentPosition = positionMs)
        }
    }
    
    fun updateDuration(durationMs: Long) {
        _playerState.update { 
            it.copy(duration = durationMs)
        }
    }
    
    fun toggleShuffle() {
        _playerState.update { 
            it.copy(isShuffleEnabled = !it.isShuffleEnabled)
        }
        audioPlayer?.setShuffleEnabled(_playerState.value.isShuffleEnabled)
    }
    
    fun cycleRepeatMode() {
        _playerState.update { state ->
            val nextMode = when (state.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            state.copy(repeatMode = nextMode)
        }
        audioPlayer?.setRepeatMode(_playerState.value.repeatMode)
    }
    
    fun next() {
        // TODO: Implement playlist navigation
        println("[PlayerController] Next track requested")
    }
    
    fun previous() {
        // TODO: Implement playlist navigation
        println("[PlayerController] Previous track requested")
    }
    
    fun setVolume(level: Float) {
        _playerState.update { it.copy(volumeLevel = level) }
        audioPlayer?.setVolume(level)
    }
    
    fun setVolumeBoost(multiplier: Float) {
        _playerState.update { it.copy(volumeBoost = multiplier) }
        audioPlayer?.setVolumeBoost(multiplier)
    }
    
    /**
     * Load a demo track for testing with a real audio URL.
     */
    fun loadDemoTrack() {
        val demoTrack = Track(
            id = "demo-1",
            title = "Big Buck Bunny (Audio)",
            artist = "Blender Foundation",
            duration = 596000 // ~10 minutes
        )
        
        // Public domain audio for testing
        val testUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3"
        
        _playerState.update {
            it.copy(
                currentTrack = demoTrack,
                duration = demoTrack.duration,
                playbackState = PlaybackState.IDLE
            )
        }
        
        // If we have a real player, load the URL
        audioPlayer?.let { player ->
            scope.launch {
                player.load(demoTrack, testUrl)
            }
        }
    }
    
    fun release() {
        scope.cancel()
        audioPlayer?.release()
    }
}

// Global singleton for demo purposes
object DemoPlayer {
    val controller = PlayerController()
    
    init {
        controller.loadDemoTrack()
    }
}
