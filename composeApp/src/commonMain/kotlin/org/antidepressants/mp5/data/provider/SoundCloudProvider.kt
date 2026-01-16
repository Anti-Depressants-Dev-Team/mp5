package org.antidepressants.mp5.data.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.antidepressants.mp5.domain.model.MusicSource
import org.antidepressants.mp5.domain.model.StreamInfo
import org.antidepressants.mp5.domain.model.Track
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * SoundCloud provider for fallback when YouTube is unavailable.
 * Uses public API endpoints where available.
 */
class SoundCloudProvider : MusicProvider {
    
    override val source: MusicSource = MusicSource.SOUNDCLOUD
    
    override suspend fun search(query: String, limit: Int): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            YouTubeProvider.ensureInitialized() // Shared initialization
            
            val service = ServiceList.SoundCloud
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            
            val items = searchExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .take(limit)
            
            val tracks = items.map { item ->
                Track(
                    id = item.url, // SoundCloud IDs are often URLs in NP
                    title = item.name,
                    artist = item.uploaderName ?: "Unknown Artist",
                    album = null,
                    thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                    duration = item.duration * 1000,
                    source = MusicSource.SOUNDCLOUD
                )
            }
            
            println("[SoundCloudProvider] Found ${tracks.size} results for: $query")
            Result.success(tracks)
        } catch (e: Exception) {
            println("[SoundCloudProvider] Search error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getStream(trackId: String): Result<StreamInfo> = withContext(Dispatchers.IO) {
        try {
            YouTubeProvider.ensureInitialized()
            
            val service = ServiceList.SoundCloud
            val streamExtractor = service.getStreamExtractor(trackId) // trackId is URL
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            val bestStream = audioStreams.firstOrNull()
            
            if (bestStream == null) {
                return@withContext Result.failure(Exception("No audio stream found"))
            }
            
            val streamInfo = StreamInfo(
                trackId = trackId,
                streamUrl = bestStream.content,
                mimeType = bestStream.format?.mimeType,
                bitrate = bestStream.averageBitrate,
                expiresAt = System.currentTimeMillis() + (6 * 60 * 60 * 1000),
                source = MusicSource.SOUNDCLOUD
            )
            
            Result.success(streamInfo)
        } catch (e: Exception) {
            println("[SoundCloudProvider] Stream error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getTrackInfo(trackId: String): Result<Track> = withContext(Dispatchers.IO) {
        try {
            YouTubeProvider.ensureInitialized()
            
            val service = ServiceList.SoundCloud
            val streamExtractor = service.getStreamExtractor(trackId)
            streamExtractor.fetchPage()
            
            val track = Track(
                id = trackId,
                title = streamExtractor.name,
                artist = streamExtractor.uploaderName,
                album = null,
                thumbnailUrl = streamExtractor.thumbnails.firstOrNull()?.url,
                duration = streamExtractor.length * 1000,
                source = MusicSource.SOUNDCLOUD
            )
            
            Result.success(track)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAvailable(): Boolean {
        return true
    }
}
