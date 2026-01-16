package org.antidepressants.mp5.data.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lyrics provider using the free LRCLIB API (https://lrclib.net/).
 * Supports synced lyrics (LRC).
 */
class LrcLibLyricsProvider : LyricsProvider {

    override val name: String = "LRCLIB"

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun searchLyrics(title: String, artist: String): Result<Lyrics> {
        return try {
            println("[LrcLibLyricsProvider] Searching for: $title by $artist")
            
            // https://lrclib.net/api/get?artist_name=...&track_name=...
            val result: LrcLibResponse = httpClient.get("https://lrclib.net/api/get") {
                parameter("artist_name", artist)
                parameter("track_name", title)
            }.body()

            val syncedLines = parseSyncedLyrics(result.syncedLyrics)

            val lyrics = Lyrics(
                trackTitle = result.trackName ?: title,
                artist = result.artistName ?: artist,
                plainText = result.plainLyrics ?: "No text lyrics found.",
                syncedLyrics = syncedLines,
                source = name
            )
            
             // If we got empty lyrics (sometimes happens), check if valid
            if (lyrics.plainText.isBlank() && lyrics.syncedLyrics.isNullOrEmpty()) {
                 return Result.failure(Exception("No lyrics content found"))
            }

            Result.success(lyrics)
        } catch (e: Exception) {
            println("[LrcLibLyricsProvider] Request failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun isAvailable(): Boolean {
        // LRCLIB is generally always free/available
        return true
    }

    private fun parseSyncedLyrics(lrcContent: String?): List<SyncedLine>? {
        if (lrcContent.isNullOrBlank()) return null
        
        val lines = mutableListOf<SyncedLine>()
        // Regex for [mm:ss.xx]Text
        val regex = Regex("\\[(\\d+):(\\d+(\\.\\d+)?)\\](.*)")
        
        lrcContent.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toDouble()
                val text = match.groupValues[4].trim()
                
                val timeMs = (minutes * 60 * 1000) + (seconds * 1000).toLong()
                if (text.isNotEmpty()) {
                    lines.add(SyncedLine(timeMs, text))
                }
            }
        }
        
        return if (lines.isNotEmpty()) lines else null
    }

    @Serializable
    data class LrcLibResponse(
        val id: Int? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
        val instrumental: Boolean? = false
    )
}
