package org.antidepressants.mp5.data.scrobble

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.antidepressants.mp5.domain.model.Track
import java.security.MessageDigest

class LastFmScrobbler : Scrobbler {
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
        System.err.println("[LastFm] updateNowPlaying called for ${track.title}. Configured: $isConfigured")
        if (!isConfigured) return Result.failure(Exception("Not configured"))
        val params = sortedMapOf("method" to "track.updateNowPlaying", "api_key" to apiKey, "sk" to (sessionKey ?: ""), "artist" to track.artist, "track" to track.title, "duration" to (track.duration / 1000).toString())
        track.album?.let { params["album"] = it }
        params["api_sig"] = generateSignature(params)
        params["format"] = "json"
        return try {
            val response = httpClient.submitForm(url = API_URL, formParameters = Parameters.build { params.forEach { (k, v) -> append(k, v) } })
            System.err.println("[LastFm] updateNowPlaying Response: ${response.status}")
            if (response.status.value !in 200..299) {
                 System.err.println("[LastFm] Error Body: ${response.body<String>()}")
            }
            Result.success(Unit)
        } catch (e: Exception) { 
            System.err.println("[LastFm] updateNowPlaying Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e) 
        }
    }
    
    override suspend fun scrobble(track: Track, timestamp: Long): Result<Unit> {
        System.err.println("[LastFm] scrobble called for ${track.title}")
        if (!isConfigured) return Result.failure(Exception("Not configured"))
        val params = sortedMapOf("method" to "track.scrobble", "api_key" to apiKey, "sk" to (sessionKey ?: ""), "artist" to track.artist, "track" to track.title, "timestamp" to (timestamp / 1000).toString(), "duration" to (track.duration / 1000).toString())
        track.album?.let { params["album"] = it }
        params["api_sig"] = generateSignature(params)
        params["format"] = "json"
        return try {
            val response = httpClient.submitForm(url = API_URL, formParameters = Parameters.build { params.forEach { (k, v) -> append(k, v) } })
            System.err.println("[LastFm] scrobble Response: ${response.status}")
             if (response.status.value !in 200..299) {
                 System.err.println("[LastFm] Error Body: ${response.body<String>()}")
            }
            Result.success(Unit)
        } catch (e: Exception) { 
            System.err.println("[LastFm] scrobble Exception: ${e.message}")
            e.printStackTrace()
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
            println("[LastFm] Token received: ${jsonResponse.token}")
            Result.success(jsonResponse.token)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getSession(token: String): Result<LastFmSession> {
        if (apiKey.isBlank() || apiSecret.isBlank()) return Result.failure(Exception("API Key or Secret using not set"))
        val params = sortedMapOf("method" to "auth.getSession", "api_key" to apiKey, "token" to token)
        params["api_sig"] = generateSignature(params)
        params["format"] = "json"
        
        return try {
            val response = httpClient.get(API_URL) {
                 url.parameters.appendAll(Parameters.build { params.forEach { (k, v) -> append(k, v) } })
            }
            val jsonResponse = response.body<LastFmSessionResponse>()
            println("[LastFm] Session received for user: ${jsonResponse.session.name}")
            // Update internal state
            sessionKey = jsonResponse.session.key
            Result.success(jsonResponse.session)
        } catch (e: Exception) { Result.failure(e) }
    }
    
    override suspend fun authenticate(apiKey: String, secret: String): Result<String> {
        LastFmScrobbler.apiKey = apiKey
        LastFmScrobbler.apiSecret = secret
        println("[LastFm] Authenticate called with API Key: ${apiKey.take(4)}...")
        return Result.success("https://www.last.fm/api/auth/?api_key=$apiKey")
    }
    
    override fun setSessionKey(sessionKey: String) { 
        println("[LastFm] Session key set: ${sessionKey.take(4)}...")
        this.sessionKey = sessionKey 
    }
    
    override suspend fun login(): Result<String> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Get Token
                val tokenResult = getToken()
                if (tokenResult.isFailure) return@withContext Result.failure(Exception(tokenResult.exceptionOrNull()?.message))
                val token = tokenResult.getOrThrow()
                
                // 2. Construct Auth URL (No Callback)
                val authUrl = "http://www.last.fm/api/auth/?api_key=$apiKey&token=$token"
                println("[LastFb] Opening auth URL for Polling Flow: $authUrl")
                
                // 3. Open Browser
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(java.net.URI(authUrl))
                } else {
                    return@withContext Result.failure(Exception("Desktop browsing not supported"))
                }
                
                // 4. Poll for Session
                // Try every 2 seconds for 2 minutes (60 attempts)
                var attempts = 0
                val maxAttempts = 60
                
                println("[LastFm] Polling for session approval...")
                
                while (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(2000) // Wait 2s
                    attempts++
                    
                    val sessionResult = getSession(token)
                    if (sessionResult.isSuccess) {
                        println("[LastFm] Polling successful! Session obtained.")
                        return@withContext Result.success(sessionResult.getOrThrow().key)
                    } else {
                        // Check if error is specifically "Unauthorized" or just waiting?
                        // Last.fm usually returns error 14 (Unauthorized Token) until approved.
                        // We ignore failure and retry unless it's a network error? 
                        // For simplicity, we just retry until timeout.
                        // println("[LastFm] Poll #$attempts failed: ${sessionResult.exceptionOrNull()?.message}")
                    }
                }
                
                Result.failure(Exception("Login timed out. Please approve in browser faster."))
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun generateSignature(params: Map<String, String>): String {
        val toSign = params.entries.sortedBy { it.key }.joinToString("") { "${it.key}${it.value}" } + apiSecret
        return MessageDigest.getInstance("MD5").digest(toSign.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}


