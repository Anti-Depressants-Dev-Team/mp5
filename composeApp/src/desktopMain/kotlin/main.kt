import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.LaunchedEffect
import org.antidepressants.mp5.App
import org.antidepressants.mp5.player.DemoPlayer
import org.antidepressants.mp5.player.DesktopAudioPlayer

fun main() = application {
    // Initialize VLCJ audio player for desktop
    val audioPlayer = try {
        DesktopAudioPlayer()
    } catch (e: Exception) {
        println("[main] Failed to create audio player: ${e.message}")
        null
    }
    
    // Wire audio player to the demo controller
    audioPlayer?.let { player ->
        DemoPlayer.controller.setAudioPlayer(player)
    }

    // Initialize Services
    val discordManager = org.antidepressants.mp5.discord.GlobalDiscordRpc.manager
    
    // Configure scrobblers with saved tokens
    val lastFmScrobbler = org.antidepressants.mp5.data.scrobble.LastFmScrobbler()
    val listenBrainzScrobbler = org.antidepressants.mp5.data.scrobble.ListenBrainzScrobbler()
    
    // Initialize scrobblers with saved settings IMMEDIATELY (not in LaunchedEffect)
    val settings = org.antidepressants.mp5.settings.GlobalSettings.settings
    
    // Last.fm: Set API credentials AND session
    settings.lastFmApiKey?.let { org.antidepressants.mp5.data.scrobble.LastFmScrobbler.apiKey = it }
    settings.lastFmSecret?.let { org.antidepressants.mp5.data.scrobble.LastFmScrobbler.apiSecret = it }
    settings.lastFmSession?.let { 
        System.err.println("[Main] Loading Last.fm session from settings: ${it.take(8)}...")
        lastFmScrobbler.setSessionKey(it) 
    }
    
    // ListenBrainz: Set token
    settings.listenBrainzToken?.let { 
        System.err.println("[Main] Loading ListenBrainz token from settings")
        listenBrainzScrobbler.setSessionKey(it) 
    }
    
    System.err.println("[Main] Last.fm isConfigured: ${lastFmScrobbler.isConfigured}")
    System.err.println("[Main] ListenBrainz isConfigured: ${listenBrainzScrobbler.isConfigured}")
    
    val scrobbleManager = org.antidepressants.mp5.data.scrobble.ScrobbleManager(
        listOf(lastFmScrobbler, listenBrainzScrobbler)
    )

    // Observe player state for services
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val scope = this
        var lastTrackId: String? = null
        var lastState = org.antidepressants.mp5.player.PlaybackState.IDLE
        
        // Connect Discord RPC
        launch(Dispatchers.IO) {
            discordManager.connect()
        }

        DemoPlayer.controller.playerState.collect { state ->
            val track = state.currentTrack
            
            // Discord RPC Update (with thumbnail and progress bar)
            discordManager.updatePresence(track, state.playbackState, state.currentPosition, state.duration)
            
            // Scrobble Updates
            if (track != null) {
                if (track.id != lastTrackId) {
                    scrobbleManager.onTrackStart(track)
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
                    scrobbleManager.onProgress(state.currentPosition)
                }
            } else if (lastTrackId != null) {
                scrobbleManager.onTrackEnd()
                lastTrackId = null
            }
        }
    }
    

    Window(
        onCloseRequest = {
            // Cleanup on close
            audioPlayer?.release()
            exitApplication()
        },
        title = "Psychopath Player",
        // transparent = true, // Enable for custom glass window
        // undecorated = true // Enable for custom controls
    ) {
        // Use App() which wraps AppWindow with Mp5Theme
        App()
    }
}
