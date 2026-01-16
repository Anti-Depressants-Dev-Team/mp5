package org.antidepressants.mp5.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antidepressants.mp5.domain.model.Playlist
import org.antidepressants.mp5.domain.model.Track
import org.antidepressants.mp5.domain.repository.PlaylistRepository
import java.io.File
import java.util.UUID

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

class PersistentPlaylistRepository(
    private val storageDir: File = File(System.getProperty("user.home"), ".mp5").also { it.mkdirs() }
) : PlaylistRepository {
    
    companion object { private const val PLAYLISTS_FILE = "playlists.json" }
    
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    
    init { 
        println("[PersistentPlaylistRepository] Initializing...")
        println("[PersistentPlaylistRepository] Storage Dir: ${storageDir.absolutePath}")
        loadPlaylistsFromDisk() 
    }
    
    private fun loadPlaylistsFromDisk() {
        try {
            val file = File(storageDir, PLAYLISTS_FILE)
            println("[PersistentPlaylistRepository] Loading from: ${file.absolutePath}")
            if (file.exists()) {
                val content = file.readText()
                val loaded = json.decodeFromString<List<Playlist>>(content)
                _playlists.value = loaded
                println("[PersistentPlaylistRepository] Successfully loaded ${loaded.size} playlists")
                loaded.forEach { println(" - Loaded Playlist: ${it.name} (${it.tracks.size} tracks)") }
            } else {
                println("[PersistentPlaylistRepository] No playlists file found at ${file.absolutePath}")
            }
        } catch (e: Exception) { 
            println("[PersistentPlaylistRepository] Load failed: ${e.message}") 
            e.printStackTrace()
        }
    }
    
    private fun savePlaylistsToDisk() {
        scope.launch {
            try {
                File(storageDir, PLAYLISTS_FILE).writeText(json.encodeToString(_playlists.value))
                println("[PersistentPlaylistRepository] Saved ${_playlists.value.size} playlists")
            } catch (e: Exception) { println("[PersistentPlaylistRepository] Save failed: ${e.message}") }
        }
    }
    
    override fun getAllPlaylists(): Flow<List<Playlist>> = _playlists.asStateFlow()
    override suspend fun getPlaylist(id: String): Playlist? = _playlists.value.find { it.id == id }
    
    override suspend fun createPlaylist(name: String, description: String?, isCloud: Boolean): Playlist {
        val newPlaylist = Playlist(UUID.randomUUID().toString(), name, description, null, emptyList(), isCloud)
        _playlists.value = _playlists.value + newPlaylist
        savePlaylistsToDisk()
        return newPlaylist
    }
    
    override suspend fun updatePlaylist(playlist: Playlist) {
        _playlists.value = _playlists.value.map { if (it.id == playlist.id) playlist.copy(updatedAt = System.currentTimeMillis()) else it }
        savePlaylistsToDisk()
    }
    
    override suspend fun deletePlaylist(id: String) {
        _playlists.value = _playlists.value.filter { it.id != id }
        savePlaylistsToDisk()
    }
    
    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        _playlists.value = _playlists.value.map { 
            if (it.id == playlistId) {
                it.copy(tracks = it.tracks + track, updatedAt = System.currentTimeMillis()) 
            } else it 
        }
        savePlaylistsToDisk()
    }
    
    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                val trackToRemove = playlist.tracks.find { it.id == trackId }
                if (trackToRemove != null) {
                    playlist.copy(tracks = playlist.tracks - trackToRemove, updatedAt = System.currentTimeMillis())
                } else playlist
            } else playlist 
        }
        savePlaylistsToDisk()
    }
    
    override suspend fun reorderTracks(playlistId: String, fromIndex: Int, toIndex: Int) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                val mutableTracks = playlist.tracks.toMutableList()
                if (fromIndex in mutableTracks.indices && toIndex in mutableTracks.indices) {
                    val track = mutableTracks.removeAt(fromIndex)
                    mutableTracks.add(toIndex, track)
                    playlist.copy(tracks = mutableTracks, updatedAt = System.currentTimeMillis())
                } else playlist
            } else playlist
        }
        savePlaylistsToDisk()
    }
    
    override suspend fun importPlaylist(playlist: Playlist) {
        // Check if playlist with same ID exists
        val existingIndex = _playlists.value.indexOfFirst { it.id == playlist.id }
        
        if (existingIndex != -1) {
            // Update existng
            val currentList = _playlists.value.toMutableList()
            currentList[existingIndex] = playlist.copy(updatedAt = System.currentTimeMillis())
            _playlists.value = currentList
        } else {
            // Add new
            _playlists.value = _playlists.value + playlist
        }
        savePlaylistsToDisk()
    }
}
