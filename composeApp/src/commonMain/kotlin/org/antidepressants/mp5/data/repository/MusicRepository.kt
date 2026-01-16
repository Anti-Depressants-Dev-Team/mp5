package org.antidepressants.mp5.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.antidepressants.mp5.data.provider.MusicProvider
import org.antidepressants.mp5.data.provider.SoundCloudProvider
import org.antidepressants.mp5.data.provider.YouTubeProvider
import org.antidepressants.mp5.domain.model.MusicSource
import org.antidepressants.mp5.domain.model.StreamInfo
import org.antidepressants.mp5.domain.model.Track

/**
 * Repository capable of fetching music from multiple providers with fallback logic.
 * 
 * Logic:
 * 1. Search: Queries primary provider (YouTube). fallback not strictly necessary for search unless primary is down.
 * 2. GetStream: Tries original source first. If fails, searches for the track on secondary providers.
 */
class MusicRepository {
    
    private val youtubeProvider = YouTubeProvider()
    private val soundCloudProvider = SoundCloudProvider()
    
    // Priority list for searching / availability
    private val providers = listOf(youtubeProvider, soundCloudProvider)
    
    suspend fun search(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        // Try YouTube first
        val ytResult = youtubeProvider.search(query)
        
        if (ytResult.isSuccess && ytResult.getOrNull()?.isNotEmpty() == true) {
            return@withContext ytResult
        }
        
        // If YouTube fails or returns empty, try SoundCloud
        println("[MusicRepository] YouTube search failed/empty. Trying SoundCloud...")
        return@withContext soundCloudProvider.search(query)
    }
    
    suspend fun getStream(track: Track): Result<StreamInfo> = withContext(Dispatchers.IO) {
        // 1. Try fetching from original source
        val provider = getProviderForSource(track.source)
        
        if (provider != null) {
            val result = provider.getStream(track.id)
            if (result.isSuccess) {
                return@withContext result
            }
            println("[MusicRepository] Stream fetch failed for ${track.source}. Error: ${result.exceptionOrNull()?.message}")
        }
        
        // 2. Fallback: Search on other providers
        println("[MusicRepository] Attempting fallback for: ${track.title} - ${track.artist}")
        fallbackStream(track)
    }
    
    private suspend fun fallbackStream(originalTrack: Track): Result<StreamInfo> {
        val query = "${originalTrack.title} ${originalTrack.artist}"
        
        // Try providers that are NOT the original source
        for (provider in providers) {
            if (provider.source == originalTrack.source) continue
            
            println("[MusicRepository] Fallback: Searching on ${provider.source}")
            val searchResult = provider.search(query, limit = 5)
            
            if (searchResult.isSuccess) {
                val match = searchResult.getOrNull()?.firstOrNull() // Simple "first match" logic for now
                if (match != null) {
                    println("[MusicRepository] Fallback match found: ${match.title} (${match.source})")
                    val streamResult = provider.getStream(match.id)
                    if (streamResult.isSuccess) {
                        return Result.success(streamResult.getOrNull()!!)
                    }
                }
            }
        }
        
        return Result.failure(Exception("All fallback attempts failed"))
    }
    
    suspend fun getSuggestions(query: String): Result<List<String>> {
        return youtubeProvider.getSuggestions(query)
    }
    
    private fun getProviderForSource(source: MusicSource): MusicProvider? {
        return when (source) {
            MusicSource.YOUTUBE -> youtubeProvider
            MusicSource.SOUNDCLOUD -> soundCloudProvider
            else -> null
        }
    }
}
