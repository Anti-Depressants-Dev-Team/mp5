package org.antidepressants.mp5.data.provider

import org.antidepressants.mp5.domain.model.MusicSource
import org.antidepressants.mp5.domain.model.StreamInfo
import org.antidepressants.mp5.domain.model.Track

/**
 * Interface for music streaming providers.
 * Implementations: YouTubeProvider, SoundCloudProvider, PipedProvider
 */
interface MusicProvider {
    /**
     * The source identifier for this provider.
     */
    val source: MusicSource

    /**
     * Search for tracks matching the query.
     * @param query Search query string
     * @param limit Maximum number of results
     * @return List of matching tracks
     */
    suspend fun search(query: String, limit: Int = 20): Result<List<Track>>

    /**
     * Get stream URL for a specific track.
     * @param trackId The track identifier
     * @return StreamInfo containing the playable URL
     */
    suspend fun getStream(trackId: String): Result<StreamInfo>

    /**
     * Get track details by ID.
     * @param trackId The track identifier
     * @return Track metadata
     */
    suspend fun getTrackInfo(trackId: String): Result<Track>

    /**
     * Check if this provider is available (network/service status).
     */
    suspend fun isAvailable(): Boolean
}
