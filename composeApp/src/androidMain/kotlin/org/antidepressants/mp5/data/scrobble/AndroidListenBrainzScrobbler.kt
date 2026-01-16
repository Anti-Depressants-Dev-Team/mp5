package org.antidepressants.mp5.data.scrobble

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antidepressants.mp5.domain.model.Track

class AndroidListenBrainzScrobbler : Scrobbler {
    companion object {
        private const val API_URL = "https://api.listenbrainz.org/1/"
        var userToken: String = ""
    }
    
    override val name = "ListenBrainz"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val httpClient = HttpClient { install(ContentNegotiation) { json(json) } }
    
    override val isConfigured: Boolean get() = userToken.isNotBlank()
    
    override suspend fun updateNowPlaying(track: Track): Result<Unit> {
        android.util.Log.d("ListenBrainz", "updateNowPlaying: ${track.title}. Configured: $isConfigured")
        if (!isConfigured) return Result.failure(Exception("Not configured"))
        val payload = ListenBrainzPayload("playing_now", listOf(ListenData(trackMetadata = TrackMetadata(track.artist, track.title, track.album))))
        return submitListen(payload)
    }
    
    override suspend fun scrobble(track: Track, timestamp: Long): Result<Unit> {
        android.util.Log.d("ListenBrainz", "scrobble: ${track.title}")
        if (!isConfigured) return Result.failure(Exception("Not configured"))
        val payload = ListenBrainzPayload("single", listOf(ListenData((timestamp / 1000).toInt(), TrackMetadata(track.artist, track.title, track.album))))
        return submitListen(payload)
    }
    
    private suspend fun submitListen(payload: ListenBrainzPayload): Result<Unit> = try {
        val response = httpClient.post("${API_URL}submit-listens") {
            header("Authorization", "Token $userToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(payload))
        }
        android.util.Log.d("ListenBrainz", "Response: ${response.status}")
        Result.success(Unit)
    } catch (e: Exception) { 
        android.util.Log.e("ListenBrainz", "Error: ${e.message}")
        Result.failure(e) 
    }
    
    override suspend fun authenticate(apiKey: String, secret: String): Result<String> {
        userToken = apiKey
        return Result.success("Token set")
    }
    
    override fun setSessionKey(sessionKey: String) { 
        userToken = sessionKey 
    }
}

// Data classes for ListenBrainz API (duplicated from desktop since they're in desktopMain)
@kotlinx.serialization.Serializable 
data class ListenBrainzPayload(
    @kotlinx.serialization.SerialName("listen_type") val listenType: String, 
    val payload: List<ListenData>
)

@kotlinx.serialization.Serializable 
data class ListenData(
    @kotlinx.serialization.SerialName("listened_at") val listenedAt: Int? = null, 
    @kotlinx.serialization.SerialName("track_metadata") val trackMetadata: TrackMetadata
)

@kotlinx.serialization.Serializable 
data class TrackMetadata(
    @kotlinx.serialization.SerialName("artist_name") val artistName: String, 
    @kotlinx.serialization.SerialName("track_name") val trackName: String, 
    @kotlinx.serialization.SerialName("release_name") val releaseName: String? = null
)
