package org.antidepressants.mp5.data.repository

import org.antidepressants.mp5.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.antidepressants.mp5.domain.model.Playlist
import org.antidepressants.mp5.domain.model.Track

// Placeholder for Android until implemented
actual object GlobalPlaylistRepository {
    actual val instance: PlaylistRepository = object : PlaylistRepository {
        override fun getAllPlaylists(): Flow<List<Playlist>> = emptyFlow()
        override suspend fun getPlaylist(id: String): Playlist? = null
        override suspend fun createPlaylist(name: String, description: String?, isCloud: Boolean): Playlist = 
            throw NotImplementedError("Android playlist repo not implemented")
        override suspend fun updatePlaylist(playlist: Playlist) {}
        override suspend fun deletePlaylist(id: String) {}
        override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {}
        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {}
        override suspend fun reorderTracks(playlistId: String, fromIndex: Int, toIndex: Int) {}
        override suspend fun importPlaylist(playlist: Playlist) {}
    }
}
