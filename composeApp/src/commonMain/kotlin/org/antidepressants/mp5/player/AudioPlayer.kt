package org.antidepressants.mp5.player

import kotlinx.coroutines.flow.StateFlow
import org.antidepressants.mp5.domain.model.Track

/**
 * Playback state enum.
 */
enum class PlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

/**
 * Current player state.
 */
data class PlayerState(
    val currentTrack: Track? = null,
    val currentPlaylist: List<Track> = emptyList(),
    val currentTrackIndex: Int = -1,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val currentPosition: Long = 0L, // in milliseconds
    val duration: Long = 0L, // in milliseconds
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val volumeLevel: Float = 1.0f, // 0.0 to 1.0 (pre-boost)
    val volumeBoost: Float = 1.0f, // 1.0 to 2.0 (boost multiplier)
    val errorMessage: String? = null
)

/**
 * Repeat mode options.
 */
enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

/**
 * Platform-agnostic audio player interface.
 * Implementations:
 * - Android: AndroidAudioPlayer (Media3/ExoPlayer)
 * - Desktop: DesktopAudioPlayer (VLCJ or MPV)
 */
interface AudioPlayer {
    
    /**
     * Observable player state.
     */
    val playerState: StateFlow<PlayerState>
    
    /**
     * Load and prepare a track for playback.
     */
    suspend fun load(track: Track, streamUrl: String)
    
    /**
     * Start or resume playback.
     */
    fun play()
    
    /**
     * Pause playback.
     */
    fun pause()
    
    /**
     * Stop playback and release resources.
     */
    fun stop()
    
    /**
     * Seek to a specific position.
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long)
    
    /**
     * Set volume level (0.0 to 1.0).
     */
    fun setVolume(level: Float)
    
    /**
     * Set volume boost multiplier (1.0 to 2.0).
     * Uses platform-specific DSP:
     * - Android: DynamicsProcessing or LoudnessEnhancer
     * - Desktop: Internal gain controls
     */
    fun setVolumeBoost(multiplier: Float)
    
    /**
     * Toggle shuffle mode.
     */
    fun setShuffleEnabled(enabled: Boolean)
    
    /**
     * Set repeat mode.
     */
    fun setRepeatMode(mode: RepeatMode)
    
    /**
     * Release all resources.
     */
    fun release()
}
