package org.antidepressants.mp5.player

import android.content.Context
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.antidepressants.mp5.domain.model.Track

@OptIn(UnstableApi::class)
class AndroidAudioPlayer(context: Context) : AudioPlayer {
    
    // Configure larger buffer for streaming stability (especially on emulator)
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            30_000,  // Min buffer (30 seconds)
            60_000,  // Max buffer (60 seconds)  
            2_500,   // Buffer for playback (2.5s)
            5_000    // Buffer for playback after rebuffer (5s)
        )
        .build()
    
    // Audio attributes for music playback
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
    
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .setAudioAttributes(audioAttributes, true)  // true = handle audio focus
        .setWakeMode(C.WAKE_MODE_NETWORK)  // Keep CPU/WiFi awake during playback
        .build()
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private var positionUpdateJob: Job? = null
    
    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val state = when (playbackState) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.LOADING
                    Player.STATE_READY -> if (exoPlayer.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                    Player.STATE_ENDED -> PlaybackState.ENDED
                    else -> PlaybackState.IDLE
                }
                _playerState.update { it.copy(
                    playbackState = state,
                    duration = exoPlayer.duration.coerceAtLeast(0)
                ) }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(
                    playbackState = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                ) }
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("AndroidAudioPlayer", "Playback error: ${error.message}", error)
                _playerState.update { it.copy(
                    playbackState = PlaybackState.ERROR,
                    errorMessage = "Playback error: ${error.message}"
                ) }
            }
        })
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                _playerState.update { it.copy(
                    currentPosition = exoPlayer.currentPosition.coerceAtLeast(0),
                    duration = exoPlayer.duration.coerceAtLeast(0)
                ) }
                delay(250)
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    @OptIn(UnstableApi::class)
    override suspend fun load(track: Track, streamUrl: String) {
        android.util.Log.d("AndroidAudioPlayer", "Loading: ${track.title} - $streamUrl")
        _playerState.update { it.copy(
            currentTrack = track,
            playbackState = PlaybackState.LOADING,
            currentPosition = 0L
        ) }
        
        // ExoPlayer requires main thread access!
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            try {
                val mediaItem = MediaItem.fromUri(streamUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                android.util.Log.e("AndroidAudioPlayer", "Error loading: ${e.message}", e)
                _playerState.update { it.copy(
                    playbackState = PlaybackState.ERROR,
                    errorMessage = "Failed to play: ${e.message}"
                ) }
            }
        }
    }
    
    override fun play() {
        exoPlayer.play()
    }
    
    override fun pause() {
        exoPlayer.pause()
    }
    
    override fun stop() {
        exoPlayer.stop()
        _playerState.update { it.copy(playbackState = PlaybackState.IDLE) }
    }
    
    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playerState.update { it.copy(currentPosition = positionMs) }
    }
    
    override fun setVolume(level: Float) {
        exoPlayer.volume = level.coerceIn(0f, 1f)
        _playerState.update { it.copy(volumeLevel = level) }
    }
    
    override fun setVolumeBoost(multiplier: Float) {
        // Android volume boost using gain control
        // For simplicity, we scale the volume beyond 1.0
        val boostedVolume = (_playerState.value.volumeLevel * multiplier).coerceIn(0f, 2f)
        exoPlayer.volume = boostedVolume.coerceIn(0f, 1f) // ExoPlayer max is 1.0
        _playerState.update { it.copy(volumeBoost = multiplier) }
        // TODO: Use DynamicsProcessing or LoudnessEnhancer for real boost
    }
    
    override fun setShuffleEnabled(enabled: Boolean) {
        exoPlayer.shuffleModeEnabled = enabled
        _playerState.update { it.copy(isShuffleEnabled = enabled) }
    }
    
    override fun setRepeatMode(mode: RepeatMode) {
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        _playerState.update { it.copy(repeatMode = mode) }
    }
    
    override fun release() {
        stopPositionUpdates()
        exoPlayer.release()
    }
}
