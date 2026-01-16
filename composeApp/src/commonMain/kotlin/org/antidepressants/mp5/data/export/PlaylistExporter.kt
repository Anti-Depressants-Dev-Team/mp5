package org.antidepressants.mp5.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.antidepressants.mp5.domain.model.Playlist
import org.antidepressants.mp5.domain.model.Track
import org.antidepressants.mp5.domain.model.MusicSource

object PlaylistExporter {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    
    fun exportToJson(playlist: Playlist): String {
        val exportData = ExportedPlaylist(
            name = playlist.name,
            description = playlist.description,
            trackCount = playlist.tracks.size,
            tracks = playlist.tracks.map { ExportedTrack(it.id, it.title, it.artist, it.album, it.duration, it.source.name) },
            exportedAt = System.currentTimeMillis()
        )
        return json.encodeToString(exportData)
    }
    
    fun exportToM3u(playlist: Playlist): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST:${playlist.name}")
        sb.appendLine()
        for (track in playlist.tracks) {
            sb.appendLine("#EXTINF:${track.duration / 1000},${track.artist} - ${track.title}")
            sb.appendLine("https://www.youtube.com/watch?v=${track.id}")
            sb.appendLine()
        }
        return sb.toString()
    }
    
    fun exportAllToJson(playlists: List<Playlist>): String {
        val exportData = ExportedLibrary(
            playlistCount = playlists.size,
            playlists = playlists.map { playlist ->
                ExportedPlaylist(playlist.name, playlist.description, playlist.tracks.size,
                    playlist.tracks.map { ExportedTrack(it.id, it.title, it.artist, it.album, it.duration, it.source.name) },
                    System.currentTimeMillis())
            },
            exportedAt = System.currentTimeMillis()
        )
        return json.encodeToString(exportData)
    }
    
    /**
     * Import a playlist from JSON.
     */
    fun importFromJson(jsonString: String): Result<Playlist> {
        return try {
            val exported = json.decodeFromString<ExportedPlaylist>(jsonString)
            val tracks = exported.tracks.map { t ->
                Track(
                    id = t.id,
                    title = t.title,
                    artist = t.artist,
                    album = t.album,
                    duration = t.duration,
                    thumbnailUrl = null,
                    source = MusicSource.entries.find { it.name == t.source } ?: MusicSource.YOUTUBE
                )
            }
            val playlist = Playlist(
                id = java.util.UUID.randomUUID().toString(),
                name = exported.name,
                description = exported.description,
                tracks = tracks,
                isCloud = false
            )
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import multiple playlists from a library JSON export.
     */
    fun importLibraryFromJson(jsonString: String): Result<List<Playlist>> {
        return try {
            val exported = json.decodeFromString<ExportedLibrary>(jsonString)
            val playlists = exported.playlists.map { ep ->
                val tracks = ep.tracks.map { t ->
                    Track(
                        id = t.id,
                        title = t.title,
                        artist = t.artist,
                        album = t.album,
                        duration = t.duration,
                        thumbnailUrl = null,
                        source = MusicSource.entries.find { it.name == t.source } ?: MusicSource.YOUTUBE
                    )
                }
                Playlist(
                    id = java.util.UUID.randomUUID().toString(),
                    name = ep.name,
                    description = ep.description,
                    tracks = tracks,
                    isCloud = false
                )
            }
            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable data class ExportedLibrary(val playlistCount: Int, val playlists: List<ExportedPlaylist>, val exportedAt: Long)
@Serializable data class ExportedPlaylist(val name: String, val description: String? = null, val trackCount: Int, val tracks: List<ExportedTrack>, val exportedAt: Long)
@Serializable data class ExportedTrack(val id: String, val title: String, val artist: String, val album: String? = null, val duration: Long, val source: String)
