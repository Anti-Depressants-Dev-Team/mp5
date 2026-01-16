package org.antidepressants.mp5.data.scrobble

import org.antidepressants.mp5.domain.model.Track

interface Scrobbler {
    val name: String
    val isConfigured: Boolean
    suspend fun updateNowPlaying(track: Track): Result<Unit>
    suspend fun scrobble(track: Track, timestamp: Long = System.currentTimeMillis()): Result<Unit>
    suspend fun authenticate(apiKey: String, secret: String): Result<String>
    suspend fun getToken(): Result<String> = Result.failure(Exception("Not supported"))
    suspend fun getSession(token: String): Result<LastFmSession> = Result.failure(Exception("Not supported"))
    suspend fun login(): Result<String> = Result.failure(Exception("Login not supported for this provider"))
    fun setSessionKey(sessionKey: String)
}

object GlobalScrobblerManager {
    var scrobblers: List<Scrobbler> = emptyList()
    
    fun getLastFm(): Scrobbler? = scrobblers.find { it.name == "Last.fm" }
}

class ScrobbleManager(private val scrobblers: List<Scrobbler>) {
    init {
        GlobalScrobblerManager.scrobblers = scrobblers
    }
    
    companion object {
        private const val MIN_TRACK_DURATION_MS = 30_000L
        private const val SCROBBLE_THRESHOLD_PERCENT = 0.5f
        private const val SCROBBLE_THRESHOLD_MS = 4 * 60 * 1000L
    }
    
    private var currentTrack: Track? = null
    // ... rest of class remains same
    private var trackStartTime: Long = 0
    private var playedDuration: Long = 0
    private var scrobbled = false
    
    suspend fun onTrackStart(track: Track) {
        currentTrack = track
        trackStartTime = System.currentTimeMillis()
        playedDuration = 0
        scrobbled = false
        
        System.err.println("[ScrobbleManager] Track Started: ${track.title} (Duration: ${track.duration}ms)")
        
        for (scrobbler in scrobblers) {
            if (scrobbler.isConfigured) {
                try {
                    scrobbler.updateNowPlaying(track)
                    System.err.println("[ScrobbleManager] ${scrobbler.name}: Now Playing Sent - ${track.title}")
                } catch (e: Exception) {
                    System.err.println("[ScrobbleManager] ${scrobbler.name} Now Playing failed: ${e.message}")
                }
            } else {
                System.err.println("[ScrobbleManager] ${scrobbler.name}: Skipped (Not Configured)")
            }
        }
    }
    
    fun onPause() {
        if (currentTrack != null && trackStartTime > 0) {
            playedDuration += System.currentTimeMillis() - trackStartTime
            trackStartTime = 0
            System.err.println("[ScrobbleManager] Paused. Played so far: ${playedDuration}ms")
        }
    }
    
    fun onResume() {
        if (currentTrack != null) {
            trackStartTime = System.currentTimeMillis()
            System.err.println("[ScrobbleManager] Resumed")
        }
    }
    
    private var lastLogTime = 0L
    
    suspend fun onProgress(currentPositionMs: Long) {
        val track = currentTrack ?: return
        if (scrobbled || track.duration < MIN_TRACK_DURATION_MS) return
        
        val currentPlayedTime = if (trackStartTime > 0) playedDuration + (System.currentTimeMillis() - trackStartTime) else playedDuration
        
        // Robust logging every 5 seconds
        if (System.currentTimeMillis() - lastLogTime > 5000) {
            System.err.println("[ScrobbleManager] Progress: ${currentPlayedTime}ms / ${track.duration}ms (Threshold: ${track.duration * SCROBBLE_THRESHOLD_PERCENT}ms)")
            lastLogTime = System.currentTimeMillis()
        }
        
        if (currentPlayedTime >= (track.duration * SCROBBLE_THRESHOLD_PERCENT) || currentPlayedTime >= SCROBBLE_THRESHOLD_MS) {
            scrobbled = true
            System.err.println("[ScrobbleManager] Scrobble Threshold Met! (${currentPlayedTime}ms played)")
            for (scrobbler in scrobblers) {
                if (scrobbler.isConfigured) {
                    try {
                        scrobbler.scrobble(track)
                        System.err.println("[ScrobbleManager] ${scrobbler.name}: Scrobbled - ${track.title}")
                    } catch (e: Exception) {
                        System.err.println("[ScrobbleManager] ${scrobbler.name} scrobble failed: ${e.message}")
                    }
                } else {
                     System.err.println("[ScrobbleManager] ${scrobbler.name}: Skipped Scrobble (Not Configured)")
                }
            }
        }
    }
    
    fun onTrackEnd() {
        if (currentTrack != null) {
            System.err.println("[ScrobbleManager] Track Ended: ${currentTrack?.title}")
        }
        currentTrack = null
        trackStartTime = 0
        playedDuration = 0
        scrobbled = false
    }
}

@kotlinx.serialization.Serializable data class LastFmTokenResponse(val token: String)
@kotlinx.serialization.Serializable data class LastFmSessionResponse(val session: LastFmSession)
@kotlinx.serialization.Serializable data class LastFmSession(val name: String, val key: String)
