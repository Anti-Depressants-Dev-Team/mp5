package org.antidepressants.mp5.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.antidepressants.mp5.domain.model.Track
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

/**
 * Desktop audio player implementation using VLCJ (VLC wrapper for Java).
 * 
 * Note: Requires VLC to be installed on the system.
 * Download: https://www.videolan.org/vlc/
 */
class DesktopAudioPlayer : AudioPlayer {
    
    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private var mediaPlayerFactory: MediaPlayerFactory? = null
    private var mediaPlayer: MediaPlayer? = null
    private var positionUpdateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        try {
            initializeVlc()
        } catch (e: Exception) {
            println("[DesktopAudioPlayer] Failed to initialize VLC: ${e.message}")
            println("[DesktopAudioPlayer] Make sure VLC is installed on your system!")
            _playerState.update { 
                it.copy(
                    playbackState = PlaybackState.ERROR,
                    errorMessage = "VLC not found. Please install VLC media player."
                )
            }
        }
    }
    
    private fun initializeVlc() {
        mediaPlayerFactory = MediaPlayerFactory()
        mediaPlayer = mediaPlayerFactory?.mediaPlayers()?.newMediaPlayer()
        
        mediaPlayer?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                _playerState.update { it.copy(playbackState = PlaybackState.PLAYING) }
                startPositionUpdates()
            }
            
            override fun paused(mediaPlayer: MediaPlayer) {
                _playerState.update { it.copy(playbackState = PlaybackState.PAUSED) }
                stopPositionUpdates()
            }
            
            override fun stopped(mediaPlayer: MediaPlayer) {
                _playerState.update { 
                    it.copy(
                        playbackState = PlaybackState.IDLE,
                        currentPosition = 0
                    )
                }
                stopPositionUpdates()
            }
            
            override fun finished(mediaPlayer: MediaPlayer) {
                val currentState = _playerState.value

                // Handle repeat modes
                when (currentState.repeatMode) {
                    RepeatMode.ONE -> {
                        // Repeat current track
                        // Launch coroutine to handle restart asynchronously and avoid callback race conditions
                        scope.launch {
                            delay(100) // Give VLC a moment to settle
                            mediaPlayer.controls().play()
                            delay(50) // Wait for play to engage
                            mediaPlayer.controls().setPosition(0f)
                            _playerState.update { it.copy(currentPosition = 0) }
                        }
                    }
                    RepeatMode.ALL -> {
                        // For now, just replay current track (TODO: playlist support)
                        mediaPlayer.controls().play()
                        _playerState.update { it.copy(currentPosition = 0) }
                    }
                    RepeatMode.OFF -> {
                        // Just mark as ended
                        _playerState.update {
                            it.copy(
                                playbackState = PlaybackState.ENDED,
                                currentPosition = currentState.duration
                            )
                        }
                        stopPositionUpdates()
                    }
                }
            }
            
            override fun error(mediaPlayer: MediaPlayer) {
                _playerState.update { 
                    it.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = "Playback error occurred"
                    )
                }
                stopPositionUpdates()
            }
            
            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                _playerState.update { it.copy(duration = newLength) }
            }
        })
        
        println("[DesktopAudioPlayer] VLC initialized successfully")
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    val position = player.status().time()
                    _playerState.update { it.copy(currentPosition = position) }
                }
                delay(250) // Update every 250ms
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    override suspend fun load(track: Track, streamUrl: String) {
        // Stop any existing playback
        mediaPlayer?.controls()?.stop()
        stopPositionUpdates()
        
        _playerState.update { 
            it.copy(
                currentTrack = track,
                playbackState = PlaybackState.LOADING,
                currentPosition = 0,
                duration = track.duration
            )
        }
        
        try {
            // Use media().play() to load and start playing immediately
            val success = mediaPlayer?.media()?.play(streamUrl) ?: false
            if (success) {
                println("[DesktopAudioPlayer] Playing: ${track.title}")
            } else {
                println("[DesktopAudioPlayer] Failed to play: ${track.title}")
                _playerState.update { 
                    it.copy(playbackState = PlaybackState.ERROR, errorMessage = "Failed to play stream")
                }
            }
        } catch (e: Exception) {
            println("[DesktopAudioPlayer] Load error: ${e.message}")
            _playerState.update { 
                it.copy(playbackState = PlaybackState.ERROR, errorMessage = "Failed to load: ${e.message}")
            }
        }
    }
    
    override fun play() {
        mediaPlayer?.controls()?.play()
    }
    
    override fun pause() {
        mediaPlayer?.controls()?.pause()
    }
    
    override fun stop() {
        mediaPlayer?.controls()?.stop()
    }
    
    override fun seekTo(positionMs: Long) {
        mediaPlayer?.controls()?.setTime(positionMs)
        _playerState.update { it.copy(currentPosition = positionMs) }
    }
    
    override fun setVolume(level: Float) {
        val volumePercent = (level * 100).toInt().coerceIn(0, 100)
        mediaPlayer?.audio()?.setVolume(volumePercent)
        _playerState.update { it.copy(volumeLevel = level) }
    }
    
    override fun setVolumeBoost(multiplier: Float) {
        // VLCJ supports up to 200% volume
        val boostedVolume = (multiplier * 100).toInt().coerceIn(0, 200)
        mediaPlayer?.audio()?.setVolume(boostedVolume)
        _playerState.update { it.copy(volumeBoost = multiplier) }
    }
    
    override fun setShuffleEnabled(enabled: Boolean) {
        _playerState.update { it.copy(isShuffleEnabled = enabled) }
    }
    
    override fun setRepeatMode(mode: RepeatMode) {
        _playerState.update { it.copy(repeatMode = mode) }
    }
    
    override fun release() {
        stopPositionUpdates()
        scope.cancel()
        mediaPlayer?.release()
        mediaPlayerFactory?.release()
        mediaPlayer = null
        mediaPlayerFactory = null
        println("[DesktopAudioPlayer] Released")
    }
}
