package org.antidepressants.mp5.data.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.antidepressants.mp5.domain.model.MusicSource
import org.antidepressants.mp5.domain.model.StreamInfo
import org.antidepressants.mp5.domain.model.Track
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * YouTube Music provider using NewPipeExtractor.
 * 
 * Searches YouTube and extracts audio stream URLs for playback.
 * Works on JVM platforms (Desktop, Android with slight modifications).
 */
class YouTubeProvider : MusicProvider {
    
    override val source: MusicSource = MusicSource.YOUTUBE
    
    companion object {
        private var initialized = false
        
        @Synchronized
        fun ensureInitialized() {
            if (!initialized) {
                try {
                    NewPipe.init(DownloaderImpl.getInstance())
                    initialized = true
                    println("[YouTubeProvider] NewPipe initialized successfully")
                } catch (e: Exception) {
                    println("[YouTubeProvider] Failed to initialize NewPipe: ${e.message}")
                }
            }
        }
    }
    
    init {
        ensureInitialized()
    }
    
    override suspend fun search(query: String, limit: Int): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            
            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            
            val items = searchExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .take(limit)
            
            val tracks = items.map { item ->
                Track(
                    id = extractVideoId(item.url),
                    title = item.name,
                    artist = item.uploaderName ?: "Unknown Artist",
                    album = null,
                    thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                    duration = item.duration * 1000, // Convert to ms
                    source = MusicSource.YOUTUBE
                )
            }
            
            println("[YouTubeProvider] Found ${tracks.size} results for: $query")
            Result.success(tracks)
        } catch (e: Exception) {
            println("[YouTubeProvider] Search error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun getStream(trackId: String): Result<StreamInfo> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            
            val url = "https://www.youtube.com/watch?v=$trackId"
            val service = ServiceList.YouTube
            val streamExtractor = service.getStreamExtractor(url)
            streamExtractor.fetchPage()
            
            // Get audio streams, prefer highest quality
            val audioStreams = streamExtractor.audioStreams
            val bestStream = audioStreams
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.firstOrNull()
            
            if (bestStream == null) {
                return@withContext Result.failure(Exception("No audio stream found"))
            }
            
            val streamInfo = StreamInfo(
                trackId = trackId,
                streamUrl = bestStream.content,
                mimeType = bestStream.format?.mimeType,
                bitrate = bestStream.averageBitrate,
                expiresAt = System.currentTimeMillis() + (6 * 60 * 60 * 1000), // ~6 hours
                source = MusicSource.YOUTUBE
            )
            
            println("[YouTubeProvider] Got stream for $trackId: ${bestStream.averageBitrate}kbps")
            Result.success(streamInfo)
        } catch (e: Exception) {
            println("[YouTubeProvider] Stream error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun getTrackInfo(trackId: String): Result<Track> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            
            val url = "https://www.youtube.com/watch?v=$trackId"
            val service = ServiceList.YouTube
            val streamExtractor = service.getStreamExtractor(url)
            streamExtractor.fetchPage()
            
            val track = Track(
                id = trackId,
                title = streamExtractor.name,
                artist = streamExtractor.uploaderName,
                album = null,
                thumbnailUrl = streamExtractor.thumbnails.firstOrNull()?.url,
                duration = streamExtractor.length * 1000, // Convert to ms
                source = MusicSource.YOUTUBE
            )
            
            Result.success(track)
        } catch (e: Exception) {
            println("[YouTubeProvider] Track info error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun isAvailable(): Boolean {
        return try {
            initialized
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get search suggestions for autocomplete.
     */
    suspend fun getSuggestions(query: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            ensureInitialized()

            val service = ServiceList.YouTube
            val suggestionExtractor = service.suggestionExtractor
            val suggestions = suggestionExtractor.suggestionList(query)

            println("[YouTubeProvider] Got ${suggestions.size} suggestions for: $query")
            Result.success(suggestions.take(5)) // Limit to 5 suggestions
        } catch (e: Exception) {
            println("[YouTubeProvider] Suggestions error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun extractVideoId(url: String): String {
        // Extract video ID from YouTube URL
        return url.substringAfter("v=").substringBefore("&")
            .takeIf { it.isNotEmpty() }
            ?: url.substringAfterLast("/").substringBefore("?")
    }
}
