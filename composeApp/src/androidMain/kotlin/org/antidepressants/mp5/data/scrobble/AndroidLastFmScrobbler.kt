package org.antidepressants.mp5.data.scrobble

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.antidepressants.mp5.domain.model.Track
import java.security.MessageDigest

class AndroidLastFmScrobbler(private val context: Context) : Scrobbler {
    companion object {
        private const val API_URL = "https://ws.audioscrobbler.com/2.0/"
        var apiKey: String = ""
        var apiSecret: String = ""
    }
    
    override val name = "Last.fm"
    private var sessionKey: String? = null
    private val httpClient = HttpClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    
    override val isConfigured: Boolean get() = apiKey.isNotBlank() && apiSecret.isNotBlank() && sessionKey != null
    
    override suspend fun updateNowPlaying(track: Track): Result<Unit> {
        android.util.Log.d("LastFm", "updateNowPlaying: ${track.title}. Configured: $isConfigured")
        if (!isConfigured) return Result.failure(Exception("Not configured"))
        val params = sortedMapOf("method" to "track.updateNowPlaying", "api_key" to apiKey, "sk" to (sessionKey ?: ""), "artist" to track.artist, "track" to track.title, "duration" to (track.duration / 1000).toString())
        track.album?.let { params["album"] = it }
        params["api_sig"] = generateSignature(params)
        params["format"] = "json"
        return try {
            val response = httpClient.submitForm(url = API_URL, formParameters = Parameters.build { params.forEach { (k, v) -> append(k, v) } })
            android.util.Log.d("LastFm", "updateNowPlaying Response: ${response.status}")
            Result.success(Unit)
        } catch (e: Exception) { 
            android.util.Log.e("LastFm", "updateNowPlaying Error: ${e.message}")
            Result.failure(e) 
        }
    }
    
    override suspend fun scrobble(track: Track, timestamp: Long): Result<Unit> {
        android.util.Log.d("LastFm", "scrobble: ${track.title}")
        if (!isConfigured) return Result.failure(Exception("Not configured"))
        val params = sortedMapOf("method" to "track.scrobble", "api_key" to apiKey, "sk" to (sessionKey ?: ""), "artist" to track.artist, "track" to track.title, "timestamp" to (timestamp / 1000).toString(), "duration" to (track.duration / 1000).toString())
        track.album?.let { params["album"] = it }
        params["api_sig"] = generateSignature(params)
        params["format"] = "json"
        return try {
            val response = httpClient.submitForm(url = API_URL, formParameters = Parameters.build { params.forEach { (k, v) -> append(k, v) } })
            android.util.Log.d("LastFm", "scrobble Response: ${response.status}")
            Result.success(Unit)
        } catch (e: Exception) { 
            android.util.Log.e("LastFm", "scrobble Error: ${e.message}")
            Result.failure(e) 
        }
    }
    
    override suspend fun getToken(): Result<String> {
        if (apiKey.isBlank()) return Result.failure(Exception("API Key not set"))
        val params = sortedMapOf("method" to "auth.getToken", "api_key" to apiKey)
        params["api_sig"] = generateSignature(params)
        params["format"] = "json"
        
        return try {
            val response = httpClient.get(API_URL) {
                 url.parameters.appendAll(Parameters.build { params.forEach { (k, v) -> append(k, v) } })
            }
            val jsonResponse = response.body<LastFmTokenResponse>()
            Result.success(jsonResponse.token)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getSession(token: String): Result<LastFmSession> {
        if (apiKey.isBlank() || apiSecret.isBlank()) return Result.failure(Exception("API Key or Secret not set"))
        val params = sortedMapOf("method" to "auth.getSession", "api_key" to apiKey, "token" to token)
        params["api_sig"] = generateSignature(params)
        params["format"] = "json"
        
        return try {
            val response = httpClient.get(API_URL) {
                 url.parameters.appendAll(Parameters.build { params.forEach { (k, v) -> append(k, v) } })
            }
            val jsonResponse = response.body<LastFmSessionResponse>()
            sessionKey = jsonResponse.session.key
            Result.success(jsonResponse.session)
        } catch (e: Exception) { Result.failure(e) }
    }
    
    override suspend fun authenticate(apiKey: String, secret: String): Result<String> {
        AndroidLastFmScrobbler.apiKey = apiKey
        AndroidLastFmScrobbler.apiSecret = secret
        return Result.success("https://www.last.fm/api/auth/?api_key=$apiKey")
    }
    
    override suspend fun login(): Result<String> {
        // On Android, we use a simpler flow - open browser and poll
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val tokenResult = getToken()
                if (tokenResult.isFailure) return@withContext Result.failure(Exception(tokenResult.exceptionOrNull()?.message))
                val token = tokenResult.getOrThrow()
                
                val authUrl = "http://www.last.fm/api/auth/?api_key=$apiKey&token=$token"
                
                // Open browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                
                // Poll for session
                var attempts = 0
                val maxAttempts = 60
                
                while (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(2000)
                    attempts++
                    
                    val sessionResult = getSession(token)
                    if (sessionResult.isSuccess) {
                        return@withContext Result.success(sessionResult.getOrThrow().key)
                    }
                }
                
                Result.failure(Exception("Login timed out"))
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override fun setSessionKey(sessionKey: String) { 
        this.sessionKey = sessionKey 
    }
    
    private fun generateSignature(params: Map<String, String>): String {
        val toSign = params.entries.sortedBy { it.key }.joinToString("") { "${it.key}${it.value}" } + apiSecret
        return MessageDigest.getInstance("MD5").digest(toSign.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
