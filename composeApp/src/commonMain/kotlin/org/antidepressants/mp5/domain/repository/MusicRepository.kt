package org.antidepressants.mp5.domain.repository

import org.antidepressants.mp5.data.provider.MusicProvider
import org.antidepressants.mp5.domain.model.MusicSource
import org.antidepressants.mp5.domain.model.StreamInfo
import org.antidepressants.mp5.domain.model.Track

/**
 * Exception thrown when no stream could be obtained from any provider.
 */
class NoStreamAvailableException(
    message: String = "Failed to load stream from all available providers"
) : Exception(message)

/**
 * Music repository implementing fallback logic across multiple providers.
 * 
 * Fallback Order:
 * 1. YouTube (primary, via NewPipeExtractor)
 * 2. SoundCloud (secondary)
 * 3. Piped (tertiary, as YouTube proxy)
 * 
 * @param providers Ordered list of music providers (priority order)
 */
class MusicRepository(
    private val providers: List<MusicProvider>
) {
    
    /**
     * Get a playable stream URL for a track, with automatic fallback.
     * 
     * Tries each provider in order until one succeeds:
     * - Try YouTube → Catch → Try SoundCloud → Catch → Try Piped
     * 
     * @param trackId The track identifier
     * @return Result containing StreamInfo on success, or failure if all providers fail
     */
    suspend fun getStreamUrl(trackId: String): Result<StreamInfo> {
        val errors = mutableListOf<Throwable>()
        
        for (provider in providers) {
            try {
                val result = provider.getStream(trackId)
                if (result.isSuccess) {
                    return result
                }
                result.exceptionOrNull()?.let { errors.add(it) }
            } catch (e: Exception) {
                // Log and continue to next provider
                println("[MusicRepository] ${provider.source} failed: ${e.message}")
                errors.add(e)
            }
        }
        
        return Result.failure(
            NoStreamAvailableException(
                "All providers failed. Errors: ${errors.map { it.message }}"
            )
        )
    }
    
    /**
     * Search for tracks across providers with fallback.
     * Returns results from the first successful provider.
     * 
     * @param query Search query
     * @param limit Maximum results per provider
     * @return List of tracks from the first successful provider
     */
    suspend fun search(query: String, limit: Int = 20): Result<List<Track>> {
        val errors = mutableListOf<Throwable>()
        
        for (provider in providers) {
            try {
                if (!provider.isAvailable()) {
                    println("[MusicRepository] ${provider.source} is not available, skipping")
                    continue
                }
                
                val result = provider.search(query, limit)
                if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
                    return result
                }
                result.exceptionOrNull()?.let { errors.add(it) }
            } catch (e: Exception) {
                println("[MusicRepository] Search failed for ${provider.source}: ${e.message}")
                errors.add(e)
            }
        }
        
        return if (errors.isEmpty()) {
            Result.success(emptyList())
        } else {
            Result.failure(
                NoStreamAvailableException(
                    "Search failed across all providers: ${errors.map { it.message }}"
                )
            )
        }
    }
    
    /**
     * Search across ALL available providers and merge results.
     * Useful for getting a broader selection of tracks.
     * 
     * @param query Search query
     * @param limitPerProvider Maximum results per provider
     * @return Combined list of tracks from all successful providers
     */
    suspend fun searchAllProviders(
        query: String,
        limitPerProvider: Int = 10
    ): List<Track> {
        val allTracks = mutableListOf<Track>()
        
        for (provider in providers) {
            try {
                if (provider.isAvailable()) {
                    val result = provider.search(query, limitPerProvider)
                    result.getOrNull()?.let { tracks ->
                        allTracks.addAll(tracks)
                    }
                }
            } catch (e: Exception) {
                // Continue to next provider
                println("[MusicRepository] Multi-search failed for ${provider.source}: ${e.message}")
            }
        }
        
        // Remove duplicates based on title + artist
        return allTracks.distinctBy { "${it.title.lowercase()}-${it.artist.lowercase()}" }
    }
    
    /**
     * Get track info with fallback to alternative providers.
     * First tries the track's original source, then falls back.
     * 
     * @param trackId Track identifier
     * @param preferredSource Preferred source to try first
     * @return Track information
     */
    suspend fun getTrackInfo(
        trackId: String,
        preferredSource: MusicSource? = null
    ): Result<Track> {
        // Reorder providers to prioritize preferred source
        val orderedProviders = if (preferredSource != null) {
            val preferred = providers.filter { it.source == preferredSource }
            val others = providers.filter { it.source != preferredSource }
            preferred + others
        } else {
            providers
        }
        
        for (provider in orderedProviders) {
            try {
                val result = provider.getTrackInfo(trackId)
                if (result.isSuccess) {
                    return result
                }
            } catch (e: Exception) {
                println("[MusicRepository] GetTrackInfo failed for ${provider.source}: ${e.message}")
            }
        }
        
        return Result.failure(NoStreamAvailableException("Could not get track info from any provider"))
    }
}
