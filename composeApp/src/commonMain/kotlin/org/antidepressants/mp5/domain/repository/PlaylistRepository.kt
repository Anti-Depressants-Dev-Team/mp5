package org.antidepressants.mp5.domain.repository

import kotlinx.coroutines.flow.Flow
import org.antidepressants.mp5.domain.model.Playlist
import org.antidepressants.mp5.domain.model.Track

/**
 * Repository for playlist operations.
 * Handles both local and cloud playlists.
 */
interface PlaylistRepository {
    
    /**
     * Get all playlists (local and cloud).
     */
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    /**
     * Get a specific playlist by ID.
     */
    suspend fun getPlaylist(id: String): Playlist?
    
    /**
     * Create a new playlist.
     */
    suspend fun createPlaylist(name: String, description: String? = null, isCloud: Boolean = false): Playlist
    
    /**
     * Update playlist metadata.
     */
    suspend fun updatePlaylist(playlist: Playlist)
    
    /**
     * Delete a playlist.
     */
    suspend fun deletePlaylist(id: String)
    
    /**
     * Add track to playlist.
     */
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)
    
    /**
     * Remove track from playlist.
     */
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    
    /**
     * Reorder tracks in playlist.
     */
    suspend fun reorderTracks(playlistId: String, fromIndex: Int, toIndex: Int)
}
