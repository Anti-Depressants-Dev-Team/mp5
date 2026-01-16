package org.antidepressants.mp5.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.antidepressants.mp5.domain.model.Playlist
import org.antidepressants.mp5.domain.model.Track
import org.antidepressants.mp5.domain.repository.PlaylistRepository
import java.util.UUID

/**
 * In-memory implementation of PlaylistRepository.
 * For MVP - will be replaced with Room Database later.
 */
class InMemoryPlaylistRepository : PlaylistRepository {

    private val _playlists = MutableStateFlow<Map<String, Playlist>>(emptyMap())

    override fun getAllPlaylists(): Flow<List<Playlist>> = _playlists.asStateFlow().map { it.values.toList() }

    override suspend fun getPlaylist(id: String): Playlist? {
        return _playlists.value[id]
    }

    override suspend fun createPlaylist(name: String, description: String?, isCloud: Boolean): Playlist {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            isCloud = isCloud,
            tracks = persistentListOf()
        )
        _playlists.value = _playlists.value + (newPlaylist.id to newPlaylist)
        return newPlaylist
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        if (_playlists.value.containsKey(playlist.id)) {
            _playlists.value = _playlists.value + (playlist.id to playlist.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deletePlaylist(id: String) {
        _playlists.value = _playlists.value - id
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        _playlists.value[playlistId]?.let { playlist ->
            val updatedPlaylist = playlist.copy(
                tracks = playlist.tracks.add(track)
            )
            updatePlaylist(updatedPlaylist)
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        _playlists.value[playlistId]?.let { playlist ->
            val trackToRemove = playlist.tracks.find { it.id == trackId }
            if (trackToRemove != null) {
                val updatedPlaylist = playlist.copy(
                    tracks = playlist.tracks.remove(trackToRemove)
                )
                updatePlaylist(updatedPlaylist)
            }
        }
    }

    override suspend fun reorderTracks(playlistId: String, fromIndex: Int, toIndex: Int) {
        _playlists.value[playlistId]?.let { playlist ->
            val mutableTracks = playlist.tracks.builder()
            val track = mutableTracks.removeAt(fromIndex)
            mutableTracks.add(toIndex, track)
            val updatedPlaylist = playlist.copy(
                tracks = mutableTracks.build()
            )
            updatePlaylist(updatedPlaylist)
        }
    }
}

// Global singleton for demo
object GlobalPlaylistRepository {
    val instance: PlaylistRepository = InMemoryPlaylistRepository()
}
