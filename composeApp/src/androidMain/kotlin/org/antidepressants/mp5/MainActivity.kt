package org.antidepressants.mp5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private var audioPlayer: org.antidepressants.mp5.player.AndroidAudioPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Audio Player
        audioPlayer = try {
            org.antidepressants.mp5.player.AndroidAudioPlayer(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to create audio player: ${e.message}")
            null
        }
        
        // Wire audio player to the demo controller
        audioPlayer?.let { player ->
            org.antidepressants.mp5.player.DemoPlayer.controller.setAudioPlayer(player)
            
            // Start the music playback service for notification controls
            val serviceIntent = android.content.Intent(this, org.antidepressants.mp5.service.MusicPlaybackService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
        
        // Initialize Scrobblers
        val lastFmScrobbler = org.antidepressants.mp5.data.scrobble.AndroidLastFmScrobbler(this)
        val listenBrainzScrobbler = org.antidepressants.mp5.data.scrobble.AndroidListenBrainzScrobbler()
        
        // Load saved settings
        val settings = org.antidepressants.mp5.settings.GlobalSettings.settings
        settings.lastFmApiKey?.let { org.antidepressants.mp5.data.scrobble.AndroidLastFmScrobbler.apiKey = it }
        settings.lastFmSecret?.let { org.antidepressants.mp5.data.scrobble.AndroidLastFmScrobbler.apiSecret = it }
        settings.lastFmSession?.let { lastFmScrobbler.setSessionKey(it) }
        settings.listenBrainzToken?.let { listenBrainzScrobbler.setSessionKey(it) }
        
        val scrobbleManager = org.antidepressants.mp5.data.scrobble.ScrobbleManager(
            listOf(lastFmScrobbler, listenBrainzScrobbler)
        )
        
        setContent {
            // Observe player state for scrobbling
            LaunchedEffect(Unit) {
                var lastTrackId: String? = null
                var lastState = org.antidepressants.mp5.player.PlaybackState.IDLE
                
                org.antidepressants.mp5.player.DemoPlayer.controller.playerState.collect { state ->
                    val track = state.currentTrack
                    
                    if (track != null) {
                        if (track.id != lastTrackId) {
                            launch(Dispatchers.IO) { scrobbleManager.onTrackStart(track) }
                            lastTrackId = track.id
                        }
                        
                        if (state.playbackState != lastState) {
                            when (state.playbackState) {
                                org.antidepressants.mp5.player.PlaybackState.PAUSED -> scrobbleManager.onPause()
                                org.antidepressants.mp5.player.PlaybackState.PLAYING -> scrobbleManager.onResume()
                                else -> {}
                            }
                            lastState = state.playbackState
                        }
                        
                        if (state.playbackState == org.antidepressants.mp5.player.PlaybackState.PLAYING) {
                            launch(Dispatchers.IO) { scrobbleManager.onProgress(state.currentPosition) }
                        }
                    } else if (lastTrackId != null) {
                        scrobbleManager.onTrackEnd()
                        lastTrackId = null
                    }
                }
            }
            
            App()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioPlayer?.release()
    }
}
