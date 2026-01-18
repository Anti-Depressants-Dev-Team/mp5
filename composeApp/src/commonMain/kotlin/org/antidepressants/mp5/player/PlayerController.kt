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
    private val lyricsRepository = org.antidepressants.mp5.data.repository.LyricsRepository()
    
    // Loading lock to prevent multiple simultaneous loads
    @Volatile
    private var isLoadingTrack = false
    
    // Queue for next/previous navigation
    private val queuedTracks = mutableListOf<Pair<Track, String>>() // Track + StreamUrl
    private var currentQueueIndex = -1
    
    init {
        restoreState()
    }
    
    private fun restoreState() {
        val settings = org.antidepressants.mp5.settings.GlobalSettings.settings
        
        // Restore volume
        val savedVolume = settings.lastVolume
        _playerState.update { it.copy(volumeLevel = savedVolume) }
        
        // Restore last track
        val savedTrackJson = settings.lastTrackJson
        if (savedTrackJson != null) {
            try {
                val track = kotlinx.serialization.json.Json.decodeFromString<Track>(savedTrackJson)
                println("[PlayerController] Restoring last track: ${track.title}")
                _playerState.update { 
                    it.copy(
                        currentTrack = track,
                        duration = track.duration,
                        playbackState = PlaybackState.IDLE // Don't auto-play, just show it
                    )
                }
                
                // Fetch lyrics for restored track (optional, but nice)
                scope.launch {
                    val lyricResult = lyricsRepository.getLyrics(track.title, track.artist)
                    lyricResult.onSuccess { lyrics ->
                        _playerState.update { it.copy(currentLyrics = lyrics) }
                    }
                }
            } catch (e: Exception) {
                println("[PlayerController] Failed to restore track: ${e.message}")
            }
        }
    }
    
    fun setAudioPlayer(player: AudioPlayer) {
        audioPlayer = player
        // Forward state updates from the actual player, but MERGE with existing state
        // to preserve lyrics and other controller-managed fields
        scope.launch {
            player.playerState.collect { audioState ->
                _playerState.update { current ->
                    current.copy(
                        currentTrack = audioState.currentTrack ?: current.currentTrack,
                        currentPlaylist = audioState.currentPlaylist,
                        currentTrackIndex = audioState.currentTrackIndex,
                        playbackState = audioState.playbackState,
                        currentPosition = audioState.currentPosition,
                        duration = audioState.duration,
                        isShuffleEnabled = audioState.isShuffleEnabled,
                        repeatMode = audioState.repeatMode,
                        volumeLevel = audioState.volumeLevel,
                        volumeBoost = audioState.volumeBoost,
                        errorMessage = audioState.errorMessage
                        // Note: currentLyrics and isLoadingLyrics are NOT overwritten
                    )
                }
            }
        }
    }
    
    fun loadTrack(track: Track, streamUrl: String, autoPlay: Boolean = true) {
        // Prevent multiple simultaneous loads
        if (isLoadingTrack) {
            println("[PlayerController] Already loading a track, skipping: ${track.title}")
            return
        }
        
        isLoadingTrack = true
        
        // Add to queue if not already the current track
        val existing = queuedTracks.indexOfFirst { it.first.id == track.id }
        if (existing == -1) {
            queuedTracks.add(Pair(track, streamUrl))
            currentQueueIndex = queuedTracks.size - 1
        } else {
            currentQueueIndex = existing
        }
        
        _playerState.update {
            it.copy(
                currentTrack = track,
                playbackState = PlaybackState.LOADING
            )
        }
        
        scope.launch {
            // Save to settings
            try {
                 val json = kotlinx.serialization.json.Json.encodeToString(Track.serializer(), track)
                 org.antidepressants.mp5.settings.GlobalSettings.settings.lastTrackJson = json
            } catch (e: Exception) {
                println("[PlayerController] Failed to save track state: ${e.message}")
            }
            
            // Record to play history for "Play Again" feature
            org.antidepressants.mp5.data.history.PlayHistory.recordPlay(track)

            // Reset lyrics
             _playerState.update { it.copy(currentLyrics = null) }
            
            // Launch lyrics fetch (async)
            _playerState.update { it.copy(isLoadingLyrics = true) }
            launch {
                val result = lyricsRepository.getLyrics(track.title, track.artist)
                result.onSuccess { lyrics ->
                    println("[PlayerController] Lyrics found: ${lyrics.source}")
                    _playerState.update { it.copy(currentLyrics = lyrics, isLoadingLyrics = false) }
                }.onFailure {
                    println("[PlayerController] Lyrics not found: ${it.message}")
                    _playerState.update { it.copy(isLoadingLyrics = false) }
                }
            }

            try {
                // load() now handles both loading and playing automatically
                audioPlayer?.load(track, streamUrl)
                // Note: No need to call play() separately - load() starts playback
            } finally {
                isLoadingTrack = false
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
    
    /**
     * Play a track by fetching its stream URL first.
     * Used by "Play Again" feature to replay history entries.
     */
    suspend fun playTrack(track: Track) {
        println("[PlayerController] playTrack: ${track.title}")
        
        // Set loading state
        _playerState.update { it.copy(currentTrack = track, playbackState = PlaybackState.LOADING) }
        
        // Fetch stream URL from provider
        val provider = org.antidepressants.mp5.data.provider.GlobalProvider.provider
        val streamResult = provider.getStream(track.id)
        
        streamResult.fold(
            onSuccess = { streamInfo ->
                println("[PlayerController] Got stream URL, loading...")
                loadTrack(track, streamInfo.streamUrl, autoPlay = true)
            },
            onFailure = { error ->
                println("[PlayerController] Failed to get stream: ${error.message}")
                _playerState.update { 
                    it.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = "Failed to get stream: ${error.message}"
                    )
                }
            }
        )
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
        if (queuedTracks.isEmpty()) {
            println("[PlayerController] No tracks in queue")
            return
        }
        
        val nextIndex = if (_playerState.value.isShuffleEnabled) {
            (0 until queuedTracks.size).random()
        } else {
            (currentQueueIndex + 1) % queuedTracks.size
        }
        
        if (nextIndex == currentQueueIndex && queuedTracks.size > 1) {
            // Skip to different track
            currentQueueIndex = (currentQueueIndex + 1) % queuedTracks.size
        } else {
            currentQueueIndex = nextIndex
        }
        
        val (track, url) = queuedTracks[currentQueueIndex]
        println("[PlayerController] Next track: ${track.title}")
        loadTrackInternal(track, url)
    }
    
    fun previous() {
        if (queuedTracks.isEmpty()) {
            println("[PlayerController] No tracks in queue")
            return
        }
        
        // If we're past 3 seconds, restart current track; otherwise go previous
        if (_playerState.value.currentPosition > 3000) {
            seekTo(0f)
            return
        }
        
        currentQueueIndex = if (currentQueueIndex > 0) {
            currentQueueIndex - 1
        } else {
            queuedTracks.size - 1
        }
        
        val (track, url) = queuedTracks[currentQueueIndex]
        println("[PlayerController] Previous track: ${track.title}")
        loadTrackInternal(track, url)
    }
    
    // Internal load without adding to queue again
    private fun loadTrackInternal(track: Track, streamUrl: String) {
        if (isLoadingTrack) return
        isLoadingTrack = true
        
        _playerState.update {
            it.copy(
                currentTrack = track,
                playbackState = PlaybackState.LOADING
            )
        }
        
        scope.launch {
            try {
                val json = kotlinx.serialization.json.Json.encodeToString(Track.serializer(), track)
                org.antidepressants.mp5.settings.GlobalSettings.settings.lastTrackJson = json
            } catch (e: Exception) {
                println("[PlayerController] Failed to save track state: ${e.message}")
            }

            _playerState.update { it.copy(currentLyrics = null, isLoadingLyrics = true) }
            launch {
                val result = lyricsRepository.getLyrics(track.title, track.artist)
                result.onSuccess { lyrics ->
                    _playerState.update { it.copy(currentLyrics = lyrics, isLoadingLyrics = false) }
                }.onFailure {
                    _playerState.update { it.copy(isLoadingLyrics = false) }
                }
            }

            try {
                audioPlayer?.load(track, streamUrl)
            } finally {
                isLoadingTrack = false
            }
        }
    }
    
    fun setVolume(level: Float) {
        _playerState.update { it.copy(volumeLevel = level) }
        audioPlayer?.setVolume(level)
        org.antidepressants.mp5.settings.GlobalSettings.settings.lastVolume = level
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

// Global singleton for player
object DemoPlayer {
    val controller = PlayerController()
    // Note: No demo track auto-loaded - user picks what to play
}
