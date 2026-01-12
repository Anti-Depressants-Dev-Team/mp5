package org.antidepressants.mp5.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.antidepressants.mp5.domain.model.Playlist
import org.antidepressants.mp5.domain.model.Track
import org.antidepressants.mp5.domain.repository.PlaylistRepository
import java.util.UUID

/**
 * In-memory implementation of PlaylistRepository.
 * For MVP - will be replaced with Room Database later.
 */
class InMemoryPlaylistRepository : PlaylistRepository {
    
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    
    override fun getAllPlaylists(): Flow<List<Playlist>> = _playlists.asStateFlow()
    
    override suspend fun getPlaylist(id: String): Playlist? {
        return _playlists.value.find { it.id == id }
    }
    
    override suspend fun createPlaylist(name: String, description: String?, isCloud: Boolean): Playlist {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            isCloud = isCloud,
            tracks = emptyList()
        )
        _playlists.value = _playlists.value + newPlaylist
        return newPlaylist
    }
    
    override suspend fun updatePlaylist(playlist: Playlist) {
        _playlists.value = _playlists.value.map { 
            if (it.id == playlist.id) playlist.copy(updatedAt = System.currentTimeMillis()) else it 
        }
    }
    
    override suspend fun deletePlaylist(id: String) {
        _playlists.value = _playlists.value.filter { it.id != id }
    }
    
    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(
                    tracks = playlist.tracks + track,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                playlist
            }
        }
    }
    
    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(
                    tracks = playlist.tracks.filter { it.id != trackId },
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                playlist
            }
        }
    }
    
    override suspend fun reorderTracks(playlistId: String, fromIndex: Int, toIndex: Int) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                val mutableTracks = playlist.tracks.toMutableList()
                val track = mutableTracks.removeAt(fromIndex)
                mutableTracks.add(toIndex, track)
                playlist.copy(
                    tracks = mutableTracks,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                playlist
            }
        }
    }
}

// Global singleton for demo
object GlobalPlaylistRepository {
    val instance: PlaylistRepository = InMemoryPlaylistRepository()
}
