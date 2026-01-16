package org.antidepressants.mp5.data.scrobble

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antidepressants.mp5.domain.model.Track

class ListenBrainzScrobbler : Scrobbler {
    companion object {
        private const val API_URL = "https://api.listenbrainz.org/1/"
        var userToken: String = ""
    }
    
    override val name = "ListenBrainz"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val httpClient = HttpClient { install(ContentNegotiation) { json(json) } }
    
    override val isConfigured: Boolean get() = userToken.isNotBlank()
    
    override suspend fun updateNowPlaying(track: Track): Result<Unit> {
        System.err.println("[ListenBrainz] updateNowPlaying called for ${track.title}. Configured: $isConfigured")
        if (!isConfigured) return Result.failure(Exception("Not configured"))
        val payload = ListenBrainzPayload("playing_now", listOf(ListenData(trackMetadata = TrackMetadata(track.artist, track.title, track.album))))
        return submitListen(payload)
    }
    
    override suspend fun scrobble(track: Track, timestamp: Long): Result<Unit> {
        System.err.println("[ListenBrainz] scrobble called for ${track.title}")
        if (!isConfigured) return Result.failure(Exception("Not configured"))
        val payload = ListenBrainzPayload("single", listOf(ListenData((timestamp / 1000).toInt(), TrackMetadata(track.artist, track.title, track.album))))
        return submitListen(payload)
    }
    
    private suspend fun submitListen(payload: ListenBrainzPayload): Result<Unit> = try {
        System.err.println("[ListenBrainz] Submitting: ${payload.listenType} - ${payload.payload.firstOrNull()?.trackMetadata?.trackName}")
        val response = httpClient.post("${API_URL}submit-listens") {
            header("Authorization", "Token $userToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(payload))
        }
        System.err.println("[ListenBrainz] Response: ${response.status}")
        Result.success(Unit)
    } catch (e: Exception) { 
        System.err.println("[ListenBrainz] Error: ${e.message}")
        e.printStackTrace()
        Result.failure(e) 
    }
    
    override suspend fun authenticate(apiKey: String, secret: String): Result<String> {
        userToken = apiKey
        System.err.println("[ListenBrainz] Token set: ${apiKey.take(4)}...")
        return Result.success("Token set")
    }
    
    override fun setSessionKey(sessionKey: String) { 
        System.err.println("[ListenBrainz] Token set: ${sessionKey.take(4)}...")
        userToken = sessionKey 
    }
}

@Serializable data class ListenBrainzPayload(
    @SerialName("listen_type") val listenType: String, 
    val payload: List<ListenData>
)
@Serializable data class ListenData(
    @SerialName("listened_at") val listenedAt: Int? = null, 
    @SerialName("track_metadata") val trackMetadata: TrackMetadata
)
@Serializable data class TrackMetadata(
    @SerialName("artist_name") val artistName: String, 
    @SerialName("track_name") val trackName: String, 
    @SerialName("release_name") val releaseName: String? = null
)
